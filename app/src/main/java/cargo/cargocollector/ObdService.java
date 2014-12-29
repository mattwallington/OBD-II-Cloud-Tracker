package cargo.cargocollector;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

/**
 * Created by matt on 6/18/14.
 * Connect to a bluetooth OBD2 reader and pull PIDs on a frequency.
 */
public class ObdService {

    /*
     *  Instance Vars
     */
    private Listener mListener;

    private boolean mIsBusy = false;
    private Queue<String> mCmdQueue;

    private BluetoothAdapter mBtAdapter = null;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothSocket mSocket = null;

    private boolean mLoopProc = false;
    private boolean mQueueProc = false;
    private boolean mReadProc = false;

    public boolean mIsConnected = false;

    private static interface Listener {
        void onConnected();
        void onReceived(byte[] buffer, int length);
        void onDisconnected();
        void onError(Exception e);
    }

    private static final int BUFFER_SIZE = 1024;

    private InputStream mInputStream;
    private OutputStream mOutputStream;

    private boolean mIsClosing;
    private byte[] mBuffer = new byte[BUFFER_SIZE];
    private int mCurLength;
    private int mBytes;

    private SocketIOService mServer;


    /*
     *  Constructor.
     */
    public ObdService(SocketIOService server) {
        mServer = server;
        mCmdQueue = new LinkedList<String>();

        //Set bluetooth device and create socket.
        if (createSocket() == false) return;

        //Define the listener methods.
        setListener();

        try {
            //Connect the device through the socket.
            //This will block until it succeeds or throws and exception.
            mIsClosing = false;
            Log.d("OBD", "Connecting...");
            mSocket.connect();

            Log.d("OBD", "Connected");
            mListener.onConnected();

        } catch (IOException connExcept) {
            try {
                cancel();
            } catch (IOException e) {
                    Log.d("OBD", "Exception: " + e.getMessage());
                }
            mListener.onError(connExcept);
            return;
        }

        //Call read thread.
        startReadData();

        //Create queue thread to send commands to the car on an interval to not overload the car's data bus.
        startQueueProcessor();

        //Throw successive commands at the queue.
        startLoopCommands();
    }

    /*
     * Bind to bluetooth device.
     */
    private boolean createSocket() {
        try {
            mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        } catch (Exception e) {
            Log.d("OBD", "Exception: " + e.getMessage());
            return false;
        }

        if (mBtAdapter == null) {
            //Couldn't get adapter.
            Log.d("OBD", "Couldn't attach to bluetooth adapter.");
            return false;
        }

        try {
            if (!mBtAdapter.isEnabled()) {
                Log.d("OBD", "Bluetooth is not enabled");
                return false;
            }
        } catch (Exception e) {
            Log.d("OBD", "Exception: "+ e.getMessage());
        }

        Set<BluetoothDevice> pairedDevices;
        pairedDevices = mBtAdapter.getBondedDevices();


        for (Iterator<BluetoothDevice> iter = pairedDevices.iterator(); iter.hasNext();) {
            BluetoothDevice device = iter.next();
            Log.d("OBD", device.getName() + ": " + device.getAddress());
        }

        //BT Device: "OBDLink MX" 00:04:3E:30:94:66
        BluetoothDevice device = mBtAdapter.getRemoteDevice("00:04:3E:30:94:66");

        try {
            mSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
        }
        catch (Exception e) {
            Log.d("OBD", "Error: " + e.getMessage());
            return false;
        }
        return true;
    }

    /*
     * Define listener.  Implements connection, data received, disconnected, and error events.
     */
    private void setListener() {
        this.mListener = new ObdService.Listener() {
            public void onConnected() {

                try{
                    mInputStream = mSocket.getInputStream();
                    mOutputStream = new BufferedOutputStream(mSocket.getOutputStream());

                } catch (IOException e) {
                    if (mIsClosing)
                        return;

                    mListener.onError(e);
                    throw new RuntimeException(e);
                }

                Log.d("OBD", "onConnected() inside");

                //Send serial commands to configure car for upcoming comands.
                try {
                    //Disable echo back.
                    //sendOBDCmd("ATE0\r");
                    queueCommand("ATE0\r");
                    queueCommand("ATZ1\r");
                    queueCommand("ATL11\r");
                    queueCommand("ATH01\r");
                    queueCommand("ATS01\r");

                    //Set connection flag.
                    mIsConnected = true;
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
                        String obdCmdResp = data.substring(0,5);

                        if (obdCmdResp.equals("41 0C")){    //RPM
                            if (data.length() > 7) {
                                String strippedString = data.replaceAll("[^0-9A-F]" ,"");
                                if (strippedString.length() == 8) {
                                    int rpm = Integer.parseInt(strippedString.substring(4), 16);
                                    //Log.d("OBD", Integer.toString(rpm));

                                    //Store RPM in JSON object and send to socket server.
                                    JSONObject obj = new JSONObject();
                                    obj.put("RPM", rpm);
                                    mServer.sendData(obj);
                                    Log.d("OBD", "Queue Length: " + mCmdQueue.size());
                                }
                            }
                        }
                        else if (obdCmdResp.equals("41 0D")) {  //Speed
                            if (data.length() > 7) {
                                String strippedString = data.replaceAll("[^0-9A-F]", "");
                                if (strippedString.length() == 6) {
                                    int speed = Integer.parseInt(strippedString.substring(4), 16);
                                    //Log.d("OBD", "Speed: " + Integer.toString(speed));

                                    //Store speed in JSON object and send to socket server.
                                    JSONObject obj = new JSONObject();
                                    obj.put("SPEED", speed);
                                    mServer.sendData(obj);
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

                //Set connection flag.
                mIsConnected = false;
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

    // Flag for blocking sending commands to the car while current command is processing.
    private void setIsBusy(boolean status) {
        this.mIsBusy = status;
    }

    /*
     * Send serial data to the bluetooth device.
     */
    public void write(byte[] bytes) throws IOException {
        if (mOutputStream == null) {
            throw new IllegalStateException("Wait connection to be opened");
        }
        mOutputStream.write(bytes);
        String data = new String(bytes, "US-ASCII");
        //Log.d("Output", "Writing: " + data);
        mOutputStream.flush();
    }

    /*
     * Will cancel an in-progress connection, and close the socket.
     */
    public void cancel() throws IOException {
        Log.d("OBD", "Cancelling OBD service");
        mIsClosing = true;
        mLoopProc = mQueueProc = mReadProc = false;
        try {
            mSocket.close();
        } catch (Exception e) {
            Log.d("OBD", e.getMessage());
        }
        mListener.onDisconnected();

        mInputStream = null;
        mOutputStream = null;
    }

    /*
     * Add a command to the queue.
     */
    public void queueCommand(String command) {
        //Log.d("Queue", "Adding command: " + command);
        mCmdQueue.add(command);
    }

    /*
     * Create a thread to send a series of commands (RPM and Speed) to the car.
     */
    private void startLoopCommands() {
        mLoopProc = true;
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d("OBD", "Started loopCommands");
                while (mLoopProc) {
                    try {
                        //Queue new commands.
                        queueCommand("010D1\r");    // Speed
                        queueCommand("010C1\r");    // RPM
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

    /*
     * Start a thread to process the commands in the queue.
     */
    private void startQueueProcessor() {
        mQueueProc = true;
        //Set up thread here and have it look for new entries in cmdQueue.  Verify that command is finished.
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d("OBD", "Started Queue Processor.");
                String cmd = null;
                while (mQueueProc) {
                    cmd = null;
                    try {
                        //check if cmdQueue has any commands queued and that the car isn't currently processing a command.
                        if (!mCmdQueue.isEmpty() && !mIsBusy) {
                            try {
                                //Toggle the busy flag to true.
                                setIsBusy(true);

                                // Pop command off the queue.
                                cmd = mCmdQueue.poll();
                                if (cmd != null) {
                                    //Send command to bluetooth adapter.
                                    write(cmd.getBytes(Charset.forName("US-ASCII")));
                                }

                            } catch (IOException ioe) {
                                Log.d("OBD", "Error: " + ioe.getMessage());
                            }
                        }
                        //Sleep for 200 ms
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

    /*
     * Read response coming back from car data bus.  Piece data together and when finished, call onReceived.
     */
    private void startReadData() {
        mReadProc = true;
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d("OBD", "Started readData");
                //Keep listening to the inputstream until an exception occurs.
                while(mReadProc) {
                    try {
                        //Read from the inputstream
                        mBytes = mInputStream.read(mBuffer, mCurLength, mBuffer.length - mCurLength);
                        if (mBytes > 0) {
                            //still reading
                            mCurLength += mBytes;
                        }

                        //check if reading is done.
                        if (mCurLength > 0) {
                            //reading finished.
                            mListener.onReceived(mBuffer,mCurLength);
                            mCurLength = mBytes = 0;
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
