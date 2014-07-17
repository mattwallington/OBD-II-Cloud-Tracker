package cargo.cargocollector;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

/**
 * Created by matt on 6/18/14.
 */
public class ObdService {

    /*
     *  Instance Vars
     */
    private Listener listener;

    private boolean isBusy = false;
    private Queue<String> cmdQueue;

    private BluetoothAdapter btadapter = null;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothSocket socket = null;

    private boolean loopProc = false;
    private boolean queueProc = false;
    private boolean readProc = false;

    public boolean isConnected = false;

    private static interface Listener {
        void onConnected();
        void onReceived(byte[] buffer, int length);
        void onDisconnected();
        void onError(Exception e);
    }

    private static final int BUFFER_SIZE = 1024;

    private InputStream inputStream;
    private OutputStream outputStream;

    private boolean isClosing;
    private byte[] buffer = new byte[BUFFER_SIZE];
    private int curLength;
    private int bytes;

    /*
     *  Methods.
     */
    public ObdService() {

        cmdQueue = new LinkedList<String>();

        //Set bluetooth device and create socket.
        if (createSocket() == false) return;

        //Define the listener methods.
        setListener();

        try {
            //Connect the device through the socket.
            //This will block until it succeeds or throws and exception.
            isClosing = false;
            Log.d("OBD", "Connecting...");
            socket.connect();

            Log.d("OBD", "Connected");
            listener.onConnected();

        } catch (IOException connectException) {
            try {
                cancel();
            } catch (IOException e) {}
            listener.onError(connectException);
            return;
        }

        //Call read thread.
        startReadData();

        //Initiate write queue.
        startQueueProcessor();

        startLoopCommands();
    }

    private boolean createSocket() {
        try {
            btadapter = BluetoothAdapter.getDefaultAdapter();
        } catch (Exception e) {
            Log.d("OBD", "Exception: " + e.getMessage());
            return false;
        }

        if (btadapter == null) {
            //Couldn't get adapter.
            Log.d("OBD", "Couldn't attach to bluetooth adapter.");
            return false;
        }

        try {
            if (!btadapter.isEnabled()) {
                Log.d("OBD", "Bluetooth is not enabled");
                return false;
            }
        } catch (Exception e) {
            Log.d("OBD", "Exception: "+ e.getMessage());
        }

        Set<BluetoothDevice> pairedDevices;
        pairedDevices = btadapter.getBondedDevices();


        for (Iterator<BluetoothDevice> iter = pairedDevices.iterator(); iter.hasNext();) {
            BluetoothDevice device = iter.next();
            Log.d("OBD", device.getName() + ": " + device.getAddress());
        }

        //BT Device: "OBDLink MX" 00:04:3E:30:94:66
        BluetoothDevice device = btadapter.getRemoteDevice("00:04:3E:30:94:66");

        try {
            socket = device.createRfcommSocketToServiceRecord(MY_UUID);
        }
        catch (Exception e) {
            Log.d("OBD", "Error: " + e.getMessage());
            return false;
        }
        return true;
    }

    private void setListener() {
        this.listener = new ObdService.Listener() {
            public void onConnected() {

                try{
                    inputStream = socket.getInputStream();
                    outputStream = new BufferedOutputStream(socket.getOutputStream());

                } catch (IOException e) {
                    if (isClosing)
                        return;

                    listener.onError(e);
                    throw new RuntimeException(e);
                }

                Log.d("OBD", "onConnected() inside");
                try {
                    //Disable echo back.
                    //sendOBDCmd("ATE0\r");
                    queueCommand("ATE0\r");
                    queueCommand("ATZ1\r");
                    queueCommand("ATL11\r");
                    queueCommand("ATH01\r");
                    queueCommand("ATS01\r");
                    //btinst.queueCommand("ATSP0\r");

                    isConnected = true;
                } catch (Exception e) {
                    Log.d("OBD", "Error: " + e.getMessage());
                }
            }
            public void onReceived(byte[] buffer, int length) {

                String data = null;
                try {
                    setIsBusy(false);
                    if (buffer == null) Log.d("OBD", "Buffer null");
                    data = new String(buffer, "US-ASCII").trim();

                    if (data.length() > 5) {
                        String obdcmdresp = data.substring(0,5);

                        if (obdcmdresp.equals("41 0C")){    //RPM
                            if (data.length() > 7) {
                                String strippedString = data.replaceAll("[^0-9A-F]" ,"");
                                if (strippedString.length() == 8) {
                                    int rpm = Integer.parseInt(strippedString.substring(4), 16);
                                    Log.d("OBD", Integer.toString(rpm));
                                }
                            }
                        }
                        else if (obdcmdresp.equals("41 0D")) {  //Speed
                            if (data.length() > 7) {
                                String strippedString = data.replaceAll("[^0-9A-F]", "");
                                if (strippedString.length() == 6) {
                                    int speed = Integer.parseInt(strippedString.substring(4), 16);
                                    Log.d("OBD", "Speed: " + Integer.toString(speed));
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.d("Exception", "Error: " + e.getMessage() + " Command: " + data);
                }
            }
            public void onDisconnected() {
                Log.d("OBD", "onDisconnected()");
                isConnected = false;
            }
            public void onError(Exception e) {
                Log.d("OBD", "Error: " + e.getMessage());
                try {
                    cancel();
                } catch (Exception cancelexcept){
                    Log.d("OBD", "Exception: " + cancelexcept.getMessage());
                }
            }
        };
    }

    private void setIsBusy(boolean status) {
        this.isBusy = status;
    }

    /* Call this from the main Activity to send data to the OBD Device. */
    public void write(byte[] bytes) throws IOException {
        if (outputStream == null) {
            throw new IllegalStateException("Wait connection to be opened");
        }
        outputStream.write(bytes);
        String data = new String(bytes, "US-ASCII");
        //Log.d("Output", "Writing: " + data);
        outputStream.flush();
    }

    /* Will cancel an in-progress connection, and close the socket. */
    public void cancel() throws IOException {
        Log.d("OBD", "Cancelling OBD service");
        isClosing = true;
        loopProc = queueProc = readProc = false;
        try {
            socket.close();
        } catch (Exception e) {
            Log.d("OBD", e.getMessage());
        }
        listener.onDisconnected();

        inputStream = null;
        outputStream = null;
    }

    public void queueCommand(String command) {
        //Log.d("Queue", "Adding command: " + command);
        cmdQueue.add(command);
    }

    /*
     * THREADS
     */
    private void startLoopCommands() {
        loopProc = true;
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d("OBD", "Started loopCommands");
                while (loopProc) {
                    try {
                        //Queue new commands.
                        queueCommand("010D1\r");
                        queueCommand("010C1\r");
                        Thread.sleep(500);
                    } catch (Exception e){
                        Log.d("Exception: Loop", "Error: " + e.getMessage());
                    }
                }
                Log.d("OBD", "Stopping loopCommands");
            }
        });
        thread.start();
    }

    private void startQueueProcessor() {
        queueProc = true;
        //Set up thread here and have it look for new entries in cmdQueue.  Verify that command is finished.
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d("OBD", "Started Queue Processor.");
                String cmd = null;
                while (loopProc) {
                    cmd = null;
                    try {
                        //check if cmdQueue has any commands queued.
                        if (!cmdQueue.isEmpty() && isBusy == false) {
                            //If so, pop one off and run the command.
                            try {
                                setIsBusy(true);
                                cmd = cmdQueue.poll();
                                if (cmd != null) {
                                    write(cmd.getBytes(Charset.forName("US-ASCII")));
                                }

                            } catch (IOException ioe) {
                                Log.d("OBD", "Error: " + ioe.getMessage());
                            }
                        }
                        Thread.sleep(200);
                    } catch (Exception e) {
                        Log.d("OBD " + e.getCause(), "Error: " + e.getMessage() + " Command: " + cmd);
                    }
                }
                Log.d("OBD", "Stopping QueueProcessor");
            }
        });
        thread.start();
    }

    private void startReadData() {
        readProc = true;
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d("OBD", "Started readData");
                //Keep listening to the inputstream until an exception occurs.
                while(readProc) {
                    try {
                        //Read from the inputstream
                        bytes = inputStream.read(buffer, curLength, buffer.length - curLength);
                        if (bytes > 0) {
                            //still reading
                            curLength += bytes;
                        }

                        //check if reading is done.
                        if (curLength > 0) {
                            //reading finished.
                            listener.onReceived(buffer,curLength);
                            curLength = bytes = 0;
                        }
                    } catch (Exception e) {
                        Log.d("OBD", "Error: "+e.getMessage());
                    }
                }
                Log.d("OBD", "Stopping ReadData");
            }
        });
        thread.start();
    }

}
