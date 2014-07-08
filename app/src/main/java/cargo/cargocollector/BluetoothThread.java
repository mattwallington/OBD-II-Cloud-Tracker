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
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

/**
 * Created by matt on 6/18/14.
 */
public class BluetoothThread extends Thread {

    private Listener listener;

    private boolean isBusy = false;
    private Queue<String> cmdQueue;

    private BluetoothAdapter btadapter = null;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothSocket socket = null;

    public boolean isConnected = false;

    private static interface Listener {
        void onConnected();
        void onReceived(byte[] buffer, int length);
        void onDisconnected();
        void onError(IOException e);
    }

    private static final int BUFFER_SIZE = 1024;

    // Create new instance
    /*
    public static BluetoothThread newInstance(BluetoothSocket socket, Listener listener) {
        BluetoothThread instance = new BluetoothThread(socket, listener);
        instance.start();
        return instance;
    }
    */

    public void setIsBusy(boolean status) {
        this.isBusy = status;
        //Log.d("Status", "Status: " + Boolean.toString(status));
    }

    public boolean getIsBusy() {
        return isBusy;
    }

    public BluetoothThread() {
        //this.socket = socket;
        //this.listener = listener;

        cmdQueue = new LinkedList<String>();

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

        this.listener = new BluetoothThread.Listener() {
            public void onConnected() {
                try {
                    //Disable echo back.
                    //sendOBDCmd("ATE0\r");
                    queueCommand("ATE0\r");
                    queueCommand("ATZ1\r");
                    queueCommand("ATL11\r");
                    queueCommand("ATH01\r");
                    queueCommand("ATS01\r");
                    //btinst.queueCommand("ATSP0\r");

                } catch (Exception e) {
                    Log.d("Exception: OnConnected", "Error: " + e.getMessage());
                }

                isConnected = true;
            }
            public void onReceived(byte[] buffer, int length) {
                String returnVal = new String();
                String data = null;
                try {
                    setIsBusy(false);

                    data = new String(buffer, "US-ASCII").trim();

                    //int speed = Integer.parseInt(data, 16);
                    if (data.length() > 5) {
                        String substr = data.substring(0,5);

                        if (substr.equals("41 0C")){    //RPM
                            if (data.length() > 7) {
                                int rpm = Integer.parseInt(data.substring(6).replaceAll("[^0-9A-F]" ,""), 16);
                                Log.d("RPM", Integer.toString(rpm));
                            }
                        }
                        else if (substr.equals("41 0D")) {  //Speed
                            if (data.length() > 7) {
                                int speed = Integer.parseInt(data.substring(6).replaceAll("[^0-9a-f]" ,""), 16);
                                Log.d("Speed", Integer.toString(speed));
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
            public void onError(IOException e) {
                Log.d("OBD", "Error: " + e.getMessage());
            }
        };
    }

    public void run() {
        try {
            //Connect the device through the socket.
            //This will block until it succeeds or throws and exception.
            isClosing = false;
            socket.connect();
        } catch (IOException connectException) {
            try {
                cancel();
            } catch (IOException e) {}
            listener.onError(connectException);
            return;
        }

        manageConnectedSocket();
    }

    private InputStream inputStream;
    private OutputStream outputStream;

    private boolean isClosing;
    private byte[] buffer = new byte[BUFFER_SIZE];
    private int curLength;
    private int bytes;

    private void manageConnectedSocket() {

        try{
            inputStream = socket.getInputStream();
            outputStream = new BufferedOutputStream(socket.getOutputStream());

            listener.onConnected();

            //Keep listening to the inputstream until an exception occurs.
            while(true) {
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
            }
        } catch (IOException e) {
            if (isClosing)
                return;

            listener.onError(e);
            throw new RuntimeException(e);
        }
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

    public void loopCommands() {
        new Thread(new Runnable() {
            @Override
            public void run() {
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
        }).start();
    }

    public void startQueueProcessor() {
        //Set up thread here and have it look for new entries in cmdQueue.  Verify that command is finished.
        new Thread(new Runnable() {
            @Override
            public void run() {
                String cmd = null;
                int count = 0;
                Log.d("Starting", "Number: " + Math.random());
                while (true) {
                    cmd = null;
                    count = 0;
                    try {
                        //check if cmdQueue has any commands queued.
                        if (!cmdQueue.isEmpty() && getIsBusy() == false) {
                            count =1;
                            //If so, pop one off and run the command.
                            try {
                                count=2;
                                setIsBusy(true);
                                count=3;
                                cmd = cmdQueue.poll();

                                if (cmd != null) {
                                    count=4;
                                    write(cmd.getBytes(Charset.forName("US-ASCII")));
                                    count=5;
                                }

                            } catch (IOException ioe) {
                                count=6;
                                Log.d("Exception: QueueProcessorIO", "Error: " + ioe.getMessage());
                            }
                        }
                        //Thread.sleep(200);
                    } catch (Exception e) {
                        Log.d("Exception: QueueProcessor: " + e.getCause(), "Error: " + e.getMessage() + " Command: " + cmd + " Count: " + Integer.toString(count));
                    }
                }
            }
        }).start();
    }


}
