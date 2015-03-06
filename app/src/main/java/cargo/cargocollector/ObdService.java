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
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

/**
 * Service to pull and appropriately route data to and from the car.
 */
public class ObdService {

    private static final String TAG = "OBDService";
    private static final UUID DEVICE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private static final int SEND_CMD_DELAY = 200;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_CONNECTING = 1; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 2;  // now connected to a remote device

    // Member fields
    private final BluetoothAdapter mAdapter;

    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private QueueProcessorThread mQueueProcessorThread;

    private int mState;
    private Context mContext;
    private BluetoothDevice mDevice;

    private Queue<String> mCmdQueue;
    private boolean mIsWriteBusy;    //Keep track of whether the bluetooth device is ready to accept new commands.

    private boolean mStopService;   //Keep track of whether user wants service stopped.


    /**
     * Constructor.
     *
     * @param context The UI Activity Context
     */
    public ObdService(Context context) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mContext = context;
        mCmdQueue = new LinkedList<String>();
        setWriteBusy(false);

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

        //Cancel any connect threads.
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        //Cancel any connected threads.
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        //Cancel any QueueProcessor threads.
        if (mQueueProcessorThread != null) {
            mQueueProcessorThread.cancel();
            mQueueProcessorThread = null;
        }

        //Start connect thread.
        if (mConnectThread == null) {
            mConnectThread = new ConnectThread();
            mConnectThread.start();
        }
        setState(STATE_CONNECTING);
    }

    /**
     * Start the ConnectedThread to begin managing the Bluetooth Connection.
     *
     * @param socket The BluetoothSocket on which the connection was made.
     */
    public synchronized void connected(BluetoothSocket socket) {
        Log.d(TAG, "connected()");

        //Cancel the thread that completed the connection.
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        //Cancel any thread currently running a connection.
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        //Start the thread to manage the connection and perform transmissions.
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        //Start another thread to manage processing the queue of commands to be sent to the device.
        mQueueProcessorThread = new QueueProcessorThread();
        mQueueProcessorThread.start();

        setState(STATE_CONNECTED);
    }

    /**
     * Stop all threads.
     */
    public synchronized void stop() {
        Log.d(TAG, "stop()");
        mStopService = true;        //Do now allow threads to be restarted.

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mQueueProcessorThread != null) {
            mQueueProcessorThread.cancel();
            mQueueProcessorThread = null;
        }

        setState(STATE_NONE);
    }

    /**
     * Write to the ConnectedThread in an unsychronized manner.
     *
     * @param out The bytes to write.
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
        ConnectedThread r;
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        //Perform the write unsynchronized.
        r.write(out);
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
        ObdService.this.start();
    }

    /**
     * Process data coming from device.
     * @param data  Bytes from device.
     * @param length Length of bytes read from device.
     */
    public synchronized void processData(byte[] data, int length) {
        Log.d(TAG, "processData()");

        //Start thread to process data.
        ProcessDataThread thread = new ProcessDataThread(data, length);
        thread.start();
    }

    public void setWriteBusy(boolean isWriteBusy) {
        mIsWriteBusy = isWriteBusy;
    }

    // Thread classes.
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;

        public ConnectThread() {
            BluetoothSocket tmp = null;

            try {
                tmp = mDevice.createRfcommSocketToServiceRecord(DEVICE_UUID);
            } catch (IOException e) {
                Log.e(TAG, "CreateRfcomm Failed.", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread.");

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
                mConnectThread = null;
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

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        private boolean mmStopThread;


        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            mmStopThread = false;

            //Get the input and output streams from the socket.
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "tmp sockets not created.", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes;

            // Continue listening to the InputStream while connected.
            while (!mmStopThread) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);

                    // Start a new thread for processing data.
                    synchronized (this) {
                        processData(buffer, bytes);
                    }
                    setWriteBusy(false);
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    if (!mmStopThread)
                        connectionLost();
                    return;
                }
            }
            Log.i(TAG, "END mConnectedThread");
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write.
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        /**
         * Close the socket
         */
        public void cancel() {
            try {
                Log.d(TAG, "Cancelling mConnectedThread");
                mmStopThread = true;
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connected socket failed", e);
            }
        }

    }

    private class ProcessDataThread extends Thread {
        private byte[] mmData;
        private int mmLength;

        public ProcessDataThread(byte[] data, int length) {
            Log.d(TAG, "create ProcessDataThread");
            mmData = data;
            mmLength = length;
        }

        public void run() {
            Log.i(TAG, "BEGIN ProcessDataThread");

            String strData = null;
            try {
                strData = new String(mmData, "US-ASCII").trim();
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "Exception during conversion from byte[] to string.", e);
                return;
            }

            if (strData.length() > 5) {
                //Log.d(TAG, "Data: "+data);
                String strippedData = strData.replaceAll("[^0-9A-F]" ,"");
                //Log.d(TAG, "Stripped: "+strippedData);
                String command = strippedData.substring(0,4);
                String value = strippedData.substring(4);
                //Log.d(TAG, "Command: " + command);
                switch(command){
                    case ObdCommands.rpm_resp:
                        if (value.length() == 4) {
                            Snapshot.rpm = Integer.parseInt(value, 16) / 4;
                            //Log.d(TAG, "RPM: "+Integer.toString(rpm));
                        }
                        break;
                    case ObdCommands.speed_resp:
                        if (value.length() == 2) {
                            int speed = Integer.parseInt(value, 16);
                            Snapshot.speed = (float)speed;
                            //Log.d(TAG, "Speed: "+Float.toString(Data.speed));
                        }
                        break;
                    case ObdCommands.maf_resp:
                        if (value.length() == 4) {
                            Snapshot.maf = (float)(Integer.parseInt(value, 16)) / 100;
                            //Log.d(TAG, "MAF: "+Float.toString(Data.maf));
                        }
                        break;
                    case ObdCommands.engine_temp_resp:
                        if (value.length() == 2) {
                            Snapshot.engine_temp = (float)(Integer.parseInt(value,16) - 40);
                            //Log.d(TAG, "Engine Temp: "+Float.toString(Data.engine_temp));
                        }
                }
            }
        }
    }

    private class QueueProcessorThread extends Thread {

        private boolean mmStopThread;

        public QueueProcessorThread() {
            Log.d(TAG, "create QueueProcessorThread");
            mmStopThread = false;
        }

        public void run() {
            Log.i(TAG, "BEGIN mQueueProcessorThread");
            String cmd;

            // Continue processing the queue until manually stopped.
            while (!mmStopThread) {
                try {
                    if (!mCmdQueue.isEmpty() && !mIsWriteBusy) {
                        setWriteBusy(true);

                        //Pop next command off the queue.
                        cmd = mCmdQueue.poll();
                        if (cmd != null) {
                            //Send command to the bluetooth adapter.
                            write(cmd.getBytes(Charset.forName("US-ASCII")));
                        }
                    }
                    Thread.sleep(SEND_CMD_DELAY);
                } catch (Exception e) {
                    Log.e(TAG, "Send queued command exception.", e);
                }
            }
            Log.i(TAG, "END mQueueProcessorThread");
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
