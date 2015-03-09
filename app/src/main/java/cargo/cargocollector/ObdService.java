package cargo.cargocollector;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Service to pull and appropriately route data to and from the car.
 */
public class ObdService {

    private static String TAG;
    private static final UUID DEVICE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final int SEND_CMD_DELAY = 200;      //Delay 200 ms after sending a command to the car.

    /**
     * Interface for constants that indicate the current connection state
      */
    public interface ConnectionState {
        public static final int NONE = 0;       // we're doing nothing
        public static final int CONNECTING = 1; // now initiating an outgoing connection
        public static final int CONNECTED = 2;  // now connected to a remote device
    }

    /**
     * Interface for constants for referencing threads.
     */
    private interface ObdTasks {
        public static final int CONNECT_DEVICE = 0;
        public static final int MANAGE_DEVICE = 1;
        public static final int QUEUE_PROCESSOR = 2;
        public static final int SEED_COMMANDS = 3;
    }

    // Member fields
    private final BluetoothAdapter mAdapter;

    private ConnectDevice mConnectDevice;
    private ManageDevice mManageDevice;
    private QueueProcessor mQueueProcessor;
    private SeedCommands mSeedCommands;

    private int mState;
    private Context mContext;
    private BluetoothDevice mDevice;

    private BlockingQueue<String> mCmdQueue;

    private boolean mStopService;   //Keep track of whether user wants service stopped.

    /**
     * Constructor.
     *
     * @param context The UI Activity Context
     */
    public ObdService(Context context) {
        TAG = this.getClass().getSimpleName();
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = ConnectionState.NONE;
        mContext = context;
        //mCmdQueue = new LinkedList<String>();
        mCmdQueue = new LinkedBlockingQueue<String>();

        //Select device.
        Set<BluetoothDevice> pairedDevices;
        pairedDevices = mAdapter.getBondedDevices();

        for (Iterator<BluetoothDevice> iter = pairedDevices.iterator(); iter.hasNext(); ){
            BluetoothDevice tmp = iter.next();
            if (tmp.getName().contains("OBDLink")) {
                mDevice = tmp;
            }
        }
    }

    /**
     * Get the current state of the connection.
     */
    public synchronized int getState() { return mState; }

    public synchronized void setState(int state) {
        Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;
    }

    /**
     * Start the OBD service.  Connect to the Bluetooth Device.
     */
    public synchronized void start() {
        Log.d(TAG, "Start()");

        stopTasks(ObdTasks.SEED_COMMANDS, ObdTasks.QUEUE_PROCESSOR, ObdTasks.MANAGE_DEVICE, ObdTasks.CONNECT_DEVICE);

        //Start connect task.
        mConnectDevice = new ConnectDevice();
        mConnectDevice.start();

        setState(ConnectionState.CONNECTING);
    }

    /**
     * Start the ConnectedThread to begin managing the Bluetooth Connection.
     *
     * @param socket The BluetoothSocket on which the connection was made.
     */
    public synchronized void connected(BluetoothSocket socket) {
        Log.d(TAG, "connected()");

        //Cancel the thread running from a previous session.
        stopTasks(ObdTasks.SEED_COMMANDS, ObdTasks.QUEUE_PROCESSOR, ObdTasks.MANAGE_DEVICE);

        //Start the thread to manage the connection and perform transmissions.
        mManageDevice = new ManageDevice(socket);
        mManageDevice.start();

        //Start another thread to manage processing the queue of commands to be sent to the device.
        mQueueProcessor = new QueueProcessor();
        mQueueProcessor.start();

        // Start timer to seed commands into the queue.
        mSeedCommands = new SeedCommands();
        mSeedCommands.start();

        setState(ConnectionState.CONNECTED);
    }

    /**
     * Stop and destroy all running threads.
     */
    public synchronized void cancel() {
        Log.d(TAG, "cancel()");
        mStopService = true;        //Do now allow threads to be restarted.

        stopTasks(ObdTasks.SEED_COMMANDS, ObdTasks.QUEUE_PROCESSOR, ObdTasks.MANAGE_DEVICE, ObdTasks.CONNECT_DEVICE);

        setState(ConnectionState.NONE);
    }

    private void stopTasks(Integer... tasks) {

        for (Integer task: tasks) {
            switch(task) {
                case ObdTasks.CONNECT_DEVICE:
                    if (mConnectDevice != null) {
                        mConnectDevice.cancel();
                        mConnectDevice = null;
                    }
                    break;
                case ObdTasks.MANAGE_DEVICE:
                    if (mManageDevice != null) {
                        mManageDevice.cancel();
                        mManageDevice = null;
                    }
                    break;
                case ObdTasks.QUEUE_PROCESSOR:
                    if (mQueueProcessor != null) {
                        mQueueProcessor.cancel();
                        mQueueProcessor = null;
                    }
                    break;
                case ObdTasks.SEED_COMMANDS:
                    if (mSeedCommands != null) {
                        mSeedCommands.cancel();
                        mSeedCommands = null;
                    }
                    break;
            }
        }
    }

    /**
     * Handle failed socket connection.  Restart the service to attempt reconnection.
     */
    private void connectionFailed() {
        //Start the service over to attempt to connect again.
        if (!mStopService){
            Log.d(TAG, "Restarting ObdService");
            ObdService.this.start();
        }
    }

    /**
     * Handle lost socket connection.  Restart the service to attempt reconnection.
     */
    private void connectionLost() {
        Log.d(TAG, "connectionLost()");
        //Start the service over to attempt to connect again.
        if (!mStopService){
            Log.d(TAG, "Restarting ObdService");
            ObdService.this.start();
        }
    }

    /**
     * Add commands to the queue to be sent to the vehicle.
     * @param commands Commands or command to be sent to vehicle.
     */
    public void sendCommands(String... commands) {
        for(String cmd: commands) {
            try {
                mCmdQueue.offer(cmd + "\r", 10000, TimeUnit.MILLISECONDS);
                Log.v(TAG, "CmdQueue Length: "+mCmdQueue.size());
            } catch (InterruptedException e) {
                Log.e(TAG, "Command put interrupted.", e);
            }
        }
    }

    /**
     * Connect to a bluetooth ELM327 compatable device.
     */
    private class ConnectDevice extends Thread {
        private final BluetoothSocket mmSocket;

        public ConnectDevice() {
            BluetoothSocket tmp = null;

            try {
                tmp = mDevice.createRfcommSocketToServiceRecord(DEVICE_UUID);
            } catch (IOException e) {
                Log.e(TAG, "CreateRfcomm Failed.", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            //Check if service should be stopped.
            if (mStopService)
                return;

            Log.i(TAG, "BEGIN mConnectDevice.");

            try {
                mmSocket.connect();
            } catch (IOException e) {
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }

            synchronized (ObdService.this) {
                mConnectDevice = null;
            }

            //Start the connected thread.
            connected(mmSocket);
        }

        public void cancel() {
            try {
                Log.d(TAG, "Cancelling ConnectThread");
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed.", e);
            }
        }
    }

    /**
     * Once connected to the device, handle the connection (read/writes).
     */
    private class ManageDevice {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private Thread mmReadThread;
        private boolean mmStopThread;


        public ManageDevice(BluetoothSocket socket) {
            Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            mmStopThread = false;
            mmReadThread = null;

            //Get the input and output streams from the socket.
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "tmp sockets not created.", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;

            //Initialize device.
            sendCommands("ATZ", "ATE0", "ATL1", "ATH0", "ATS0");
        }

        public void start() {
            //Check if thread is already running.
            if (mmReadThread != null) {
                Log.d(TAG, "Thread is already running.");
                return;
            }

            //Start read thread.
            readLoop();

        }

        public void readLoop() {
            //Check if service should be stopped.
            if (mStopService)
                return;

            new Thread(new Runnable() {

                @Override
                public void run() {

                    Log.i(TAG, "BEGIN mManageDevice.ReadLoop()");
                    int bytes = 0;

                    int curLen = 0;
                    int totLen = 0;

                    // Continue listening to the InputStream while connected.
                    while (!mmStopThread) {
                        try {
                            // Read from the InputStream in a synchronized manner.
                            byte[] buffer = new byte[1024];
                            synchronized (this) {
                                //bytes = mmInStream.read(buffer);
                                curLen = mmInStream.read(buffer, totLen, buffer.length - totLen);
                            }

                            if (curLen > 0) {
                                //Still reading
                                totLen += curLen;
                            }

                            //Check if reading is done.
                            if (totLen > 0) {
                                //reading finished.
                                //Log.d(TAG, "Calling processData");
                                processData(buffer, totLen);
                                totLen = 0;
                            }

                        } catch (IOException e) {
                            Log.e(TAG, "Socket disconnected while reading.");
                            if (!mmStopThread)
                                connectionLost();
                        }
                    }
                    Log.i(TAG, "END mManageDevice.ReadLoop()");
                }
            }).start();

        }

        private void processData(byte[] buffer, int bytes) {
            String strData;
            try {
                strData = new String(buffer, "US-ASCII").trim();
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "Exception during conversion from byte[] to string.", e);
                return;
            }

            if (strData.length() > 5) {
                //Log.d(TAG, "Data: "+data);
                String strippedData = strData.replaceAll("[^0-9A-F]" ,"");
                //Log.d(TAG, "Stripped: "+strippedData);
                String command;
                String value;
                try {
                    command = strippedData.substring(0,4);
                    value = strippedData.substring(4);
                } catch (StringIndexOutOfBoundsException e) {
                    Log.e(TAG, "Process data substring out of bounds.", e);
                    return;
                }

                switch(command){
                    case ObdCommands.rpm_resp:
                        try {

                            if (value.length() == 4) {
                                Snapshot.rpm = Integer.parseInt(value, 16) / 4;
                                //Log.d(TAG, "RPM: "+Integer.toString(Snapshot.rpm));
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Exception: Process Data - " + e.getMessage());
                        }
                        break;
                    case ObdCommands.speed_resp:
                        try {
                            if (value.length() == 2) {

                                int speed = Integer.parseInt(value, 16);
                                Snapshot.speed = (float) speed;
                                //Log.d(TAG, "Speed: " + Float.toString(Snapshot.speed));
                            }
                        } catch (Exception e){
                            Log.e(TAG, "Exception: Process Data - " + e.getMessage());
                        }
                        break;
                    case ObdCommands.maf_resp:
                        try {
                            if (value.length() == 4) {
                                Snapshot.maf = (float)(Integer.parseInt(value, 16)) / 100;
                                //Log.d(TAG, "MAF: "+Float.toString(Snapshot.maf));
                            }
                        } catch (Exception e){
                            Log.e(TAG, "Exception: Process Data - " + e.getMessage());
                        }
                        break;
                    case ObdCommands.engine_temp_resp:
                        try {
                            if (value.length() == 2) {
                                Snapshot.engine_temp = (float)(Integer.parseInt(value,16) - 40);
                                //Log.d(TAG, "Engine Temp: "+Float.toString(Snapshot.engine_temp));
                            }
                        } catch (Exception e){
                            Log.e(TAG, "Exception: Process Data - " + e.getMessage());
                        }
                        break;
                }
            }
        }

        /**
         * Write to the connected OutStream in a synchronized manner
         *
         * @param buffer The bytes to write.
         */
        public void write(byte[] buffer) {
            synchronized(this) {
                try {
                    //Log.d(TAG, "Sending: "+new String(buffer,"UTF-8"));
                    mmOutStream.write(buffer);
                } catch (IOException e) {
                    Log.e(TAG, "Exception during write", e);
                }
            }
        }

        /**
         * Close the socket
         */
        public void cancel() {
            try {
                Log.d(TAG, "Cancelling mManageDevice");
                mmStopThread = true;
                mmInStream.close();
                mmOutStream.close();
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connected socket failed", e);
            }
        }
    }


    /**
     *  Process commands that need to be sent to the device.
     */
    private class QueueProcessor extends Thread {

        private boolean mmStopThread;

        public QueueProcessor() {
            Log.d(TAG, "create QueueProcessorThread");
            mmStopThread = false;
        }

        public void run() {
            Log.i(TAG, "BEGIN mQueueProcessor");
            String cmd;

            // Continue processing the queue until manually stopped.
            while (!mmStopThread) {
                try {
                    //Pop next command off the queue.
                    cmd = mCmdQueue.poll(3000, TimeUnit.MILLISECONDS);
                    if (cmd != null) {
                        //Send command to the bluetooth adapter.
                        mManageDevice.write(cmd.getBytes(Charset.forName("US-ASCII")));
                    }
                    Thread.sleep(SEND_CMD_DELAY);
                } catch (Exception e) {
                    Log.e(TAG, "Send queued command exception.", e);
                }
            }
            Log.i(TAG, "END mQueueProcessor");
        }

        /**
         * Close the socket
         */
        public void cancel() {
            try {
                Log.d(TAG, "Cancelling QueueProcessorThread.");
                mmStopThread = true;
            } catch (Exception e) {
                Log.e(TAG, "close() of queue processor thread failed.", e);
            }
        }

    }

    private class SeedCommands {
        private Timer mSeedTimer;
        private TimerTask mSeedTask;
        private static final int CMD_INTERVAL = 1000;

        public SeedCommands() {
            mSeedTimer = null;
            mSeedTask = null;
        }

        public void start() {
            Log.d(TAG, "SeedCommands.start()");
            mSeedTimer = new Timer();

            Log.d(TAG, "SeedCommandsTask() initialized.");
            mSeedTask = new TimerTask() {
                public void run() {
                    //Log.d(TAG, "SeedCommandsTask() fired.");
                    sendCommands(ObdCommands.speed, ObdCommands.engine_temp, ObdCommands.maf);
                }
            };

            mSeedTimer.schedule(mSeedTask, 0, CMD_INTERVAL);
        }

        public void cancel() {
            Log.d(TAG, "SeedCommands.stop()");
            if (mSeedTimer != null) {
                mSeedTimer.cancel();
                mSeedTimer = null;
            }
        }

    }

    class ObdCommands {
        public static final String speed = "010D";
        public static final String rpm = "010C";
        public static final String engine_temp = "0105";
        public static final String maf = "0110";

        public static final String speed_resp = "410D";
        public static final String rpm_resp = "410C";
        public static final String engine_temp_resp = "4105";
        public static final String maf_resp = "4110";
    }

}
