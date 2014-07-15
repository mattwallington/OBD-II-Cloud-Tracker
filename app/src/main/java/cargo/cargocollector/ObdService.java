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
    private Thread loopProc = null;
    private Thread queueProc = null;
    private Thread readProc = null;

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
        createSocket();

        //Define the listener methods.
        setListener();

        try {
            //Connect the device through the socket.
            //This will block until it succeeds or throws and exception.
            isClosing = false;
            Log.d("Status", "Connecting...");
            socket.connect();

            Log.d("Status", "Connected");
            listener.onConnected();

        } catch (IOException connectException) {
            try {
                cancel();
            } catch (IOException e) {}
            listener.onError(connectException);
            return;
        }

        //Call read thread.
        Log.d("Status", "Starting read processor");
        readProc = readData();

        //Initiate write queue.
        Log.d("Status", "Starting queue processor");
        queueProc = startQueueProcessor();


        Log.d("Status", "Starting loop commands processor");
        loopProc = loopCommands();
    }

    private void createSocket() {
        btadapter = BluetoothAdapter.getDefaultAdapter();
        if (btadapter == null) {
            //Couldn't get adapter.
        }

        if (!btadapter.isEnabled()) {
            Log.d("OBD", "Bluetooth is not enabled");
            return;
        }

        Set<BluetoothDevice> pairedDevices;
        pairedDevices = btadapter.getBondedDevices();

        for (Iterator<BluetoothDevice> iter = pairedDevices.iterator(); iter.hasNext();) {
            BluetoothDevice device = iter.next();
            Log.d("BluetoothDevice", device.getName() + ": " + device.getAddress());

        }

        //BT Device: "OBDLink MX" 00:04:3E:30:94:66
        BluetoothDevice device = btadapter.getRemoteDevice("00:04:3E:30:94:66");

        try {
            socket = device.createRfcommSocketToServiceRecord(MY_UUID);
        }
        catch (Exception e) {
            Log.d("Socket Exception", "Error: " + e.getMessage());
        }
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

                Log.d("Status", "onConnected() inside");
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
                    Log.d("Exception: OnConnected", "Error: " + e.getMessage());
                }
            }
            public void onReceived(byte[] buffer, int length) {

                String data = null;
                try {
                    setIsBusy(false);
                    if (buffer == null) Log.d("Data", "Buffer null");
                    data = new String(buffer, "US-ASCII").trim();

                    if (data.length() > 5) {
                        String obdcmdresp = data.substring(0,5);

                        if (obdcmdresp.equals("41 0C")){    //RPM
                            if (data.length() > 7) {
                                String strippedString = data.replaceAll("[^0-9A-F]" ,"");
                                if (strippedString.length() == 8) {
                                    int rpm = Integer.parseInt(strippedString.substring(4), 16);
                                    Log.d("RPM", Integer.toString(rpm));
                                }
                            }
                        }
                        else if (obdcmdresp.equals("41 0D")) {  //Speed
                            if (data.length() > 7) {
                                String strippedString = data.replaceAll("[^0-9A-F]", "");
                                if (strippedString.length() == 6) {
                                    int speed = Integer.parseInt(strippedString.substring(4), 16);
                                    Log.d("Speed", "Speed: " + Integer.toString(speed));
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
                stopThreads();
            }
        };
    }

    private void setIsBusy(boolean status) {
        this.isBusy = status;
        //Log.d("Status", "Status: " + Boolean.toString(status));
    }

    public void stopThreads() {
        if (loopProc != null) loopProc.interrupt();
        if (queueProc != null) queueProc.interrupt();
        if (readProc != null) readProc.interrupt();
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
        isClosing = true;
        socket.close();
        listener.onDisconnected();

        inputStream = null;
        outputStream = null;
    }

    public void queueCommand(String command) {
        //Log.d("Queue", "Adding command: " + command);
        cmdQueue.add(command);
    }

    private Thread loopCommands() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d("Status", "Started loopCommands");
                while (true) {
                    try {
                        //Queue new commands.
                        queueCommand("010D1\r");
                        queueCommand("010C1\r");
                        Thread.sleep(500);
                    } catch (Exception e){
                        Log.d("Exception: Loop", "Error: " + e.getMessage());
                    }
                }
            }
        });
        thread.start();
        return thread;
    }

    private Thread startQueueProcessor() {
        //Set up thread here and have it look for new entries in cmdQueue.  Verify that command is finished.
        Thread thread = new Thread(new Runnable() {
            @Override//this.socket = socket;
        //this.listener = listener;

            public void run() {
                String cmd = null;
                while (true) {
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
                                Log.d("Exception: QueueProcessorIO", "Error: " + ioe.getMessage());
                            }
                        }
                        Thread.sleep(200);
                    } catch (Exception e) {
                        Log.d("Exception: QueueProcessor: " + e.getCause(), "Error: " + e.getMessage() + " Command: " + cmd);
                    }
                }
            }
        });
        thread.start();
        return thread;
    }

    private Thread readData() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d("Status", "Started readData");
                //Keep listening to the inputstream until an exception occurs.
                while(true) {
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
                        Log.d("Exception: ReadData", "Error: "+e.getMessage());
                    }
                }
            }
        });
        thread.start();
        return thread;
    }


}
