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
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

/**
 * Created by matt on 6/18/14.
 * Connect to a bluetooth OBD2 reader and pull PIDs on a frequency.
 */
public class ObdService {
    /*
     * Constants
     */

    private static final int BUFFER_SIZE = 1024;
    private static final int SEND_CMD_DELAY = 200;  //in ms
    private static final int LOOP_CMD_INTERVAL = 1000;  //in ms
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    /*
     *  Member Vars
     */
    private static Listener s_listener;

    private static boolean s_isBusy = false;
    private static Queue<String> s_cmdQueue;

    private static BluetoothAdapter s_btAdapter = null;

    private static BluetoothSocket s_socket = null;

    private static boolean s_queueProc = false;
    private static boolean s_readProc = false;

    public static boolean s_isConnected = false;
    private static InputStream s_inputStream;
    private static OutputStream s_outputStream;

    private static boolean s_isClosing;

    private static Timer s_loopCmdsTimer;
    private static TimerTask s_loopCmdsTask;

    private static interface Listener {
        void onConnected();
        void onReceived(byte[] buffer, int length);
        void onDisconnected();
        void onError(Exception e);
    }

    /*
     *  Constructor.
     */
    public static void start() {
        s_cmdQueue = new LinkedList<String>();

        //Set bluetooth device and create socket.
        if (!createSocket()) return;

        //Define the s_listener methods.
        setListener();

        try {
            //Connect the device through the socket.
            //This will block until it succeeds or throws and exception.
            s_isClosing = false;
            Log.d("OBD", "Connecting...");
            s_socket.connect();

            Log.d("OBD", "Connected");
            s_listener.onConnected();

        } catch (IOException connExcept) {
            try {
                cancel();
            } catch (IOException e) {
                    Log.d("OBD", "Exception: " + e.getMessage());
                }
            s_listener.onError(connExcept);
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
    private static boolean createSocket() {
        try {
            s_btAdapter = BluetoothAdapter.getDefaultAdapter();
        } catch (Exception e) {
            Log.d("OBD", "Exception: " + e.getMessage());
            return false;
        }

        if (s_btAdapter == null) {
            //Couldn't get adapter.
            Log.d("OBD", "Couldn't attach to bluetooth adapter.");
            return false;
        }

        try {
            if (!s_btAdapter.isEnabled()) {
                Log.d("OBD", "Bluetooth is not enabled");
                return false;
            }
        } catch (Exception e) {
            Log.d("OBD", "Exception: "+ e.getMessage());
        }

        Set<BluetoothDevice> pairedDevices;
        pairedDevices = s_btAdapter.getBondedDevices();

        BluetoothDevice device = null;
        for (Iterator<BluetoothDevice> iter = pairedDevices.iterator(); iter.hasNext();) {
            BluetoothDevice d = iter.next();
            Log.d("OBD", d.getName() + ": " + d.getAddress());
            if (d.getName().contains("OBDLink")) {
                device = d;
            }
        }

        //BT Device: "OBDLink MX" 00:04:3E:30:94:66
        //BluetoothDevice device = s_btAdapter.getRemoteDevice("00:04:3E:30:94:66");

        try {
            s_socket = device.createRfcommSocketToServiceRecord(MY_UUID);
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
    private static void setListener() {
        s_listener = new ObdService.Listener() {
            public void onConnected() {

                try{
                    s_inputStream = s_socket.getInputStream();
                    s_outputStream = new BufferedOutputStream(s_socket.getOutputStream());

                } catch (IOException e) {
                    if (s_isClosing)
                        return;

                    s_listener.onError(e);
                    throw new RuntimeException(e);
                }

                Log.d("OBD", "onConnected() inside");

                //Initialize device.
                try {
                    queueCommands(new String[]{"ATZ","ATE0","ATL1","ATH0","ATS0"});

                    //Set connection flag.
                    s_isConnected = true;
                } catch (Exception e) {
                    Log.d("OBD", "QueueCommands Error: " + e.getMessage());
                }
            }
            public void onReceived(byte[] buffer, int length) {

                String data = null;
                try {
                    setIsBusy(false);
                    if (buffer == null) Log.d("OBD", "Buffer null");
                    data = new String(buffer, "US-ASCII").trim();
                    processData(data);
                } catch (Exception e) {
                    Log.d("Exception", "Error: " + e.getMessage() + " Command: " + data);
                }
            }
            public void onDisconnected() {
                Log.d("OBD", "onDisconnected()");

                //Set connection flag.
                s_isConnected = false;
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

    /*
     * Read response coming back from OBD.  Piece data together and when finished, call onReceived.
     */
    private static void startReadData() {
        s_readProc = true;
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d("OBD", "Started readData");

                int curLen = 0;
                int totLen = 0;

                //Keep listening to the inputstream until an exception occurs.
                while(s_readProc) {
                    try {
                        byte[] buffer = new byte[BUFFER_SIZE];
                        //Read from the inputstream
                        curLen = s_inputStream.read(buffer, totLen, buffer.length - totLen);
                        if (curLen > 0) {
                            //still reading
                            totLen += curLen;
                        }

                        //check if reading is done.
                        if (totLen > 0) {
                            //reading finished.
                            s_listener.onReceived(buffer, totLen);
                            totLen =  0;
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

    /*
     * Parse data to process command responses.
     */
    private static void processData(String data) {
        if (data.length() > 5) {
            //Log.d("OBD", "Data: "+data);
            String strippedData = data.replaceAll("[^0-9A-F]" ,"");
            //Log.d("OBD", "Stripped: "+strippedData);
            String command = strippedData.substring(0,4);
            String value = strippedData.substring(4);
            //Log.d("OBD", "Command: " + command);
            switch(command){
                case ObdCommands.rpm_resp:
                    if (value.length() == 4) {
                        int rpm = Integer.parseInt(value, 16) / 4;
                        //Log.d("OBD", "RPM: "+Integer.toString(rpm));
                    }
                    break;
                case ObdCommands.speed_resp:
                    if (value.length() == 2) {
                        int speed = Integer.parseInt(value, 16);
                        Data.speed = (float)speed;
                        //Log.d("OBD", "Speed: "+Float.toString(Data.speed));
                    }
                    break;
                case ObdCommands.maf_resp:
                    if (value.length() == 4) {
                        Data.maf = (float)(Integer.parseInt(value, 16)) / 100;
                        //Log.d("OBD", "MAF: "+Float.toString(Data.maf));
                    }
                    break;
                case ObdCommands.engine_temp_resp:
                    if (value.length() == 2) {
                        Data.engine_temp = (float)(Integer.parseInt(value,16) - 40);
                        //Log.d("OBD", "Engine Temp: "+Float.toString(Data.engine_temp));
                    }
            }
        }
    }

    // Flag for blocking sending commands to the car while current command is processing.
    private static void setIsBusy(boolean status) {
        s_isBusy = status;
    }

    /*
     * Send serial data to the bluetooth device.
     */
    public static void write(byte[] bytes) throws IOException {
        if (s_outputStream == null) {
            throw new IllegalStateException("Wait connection to be opened");
        }
        s_outputStream.write(bytes);
        s_outputStream.flush();
    }

    /*
     * Will cancel an in-progress connection, and close the socket.
     */
    public static void cancel() throws IOException {
        Log.d("OBD", "Cancelling OBD service");
        s_isClosing = true;
        stopLoopCommands();
        s_queueProc = s_readProc = false;
        try {
            s_socket.close();
        } catch (Exception e) {
            Log.d("OBD", e.getMessage());
        }
        s_listener.onDisconnected();

        s_inputStream = null;
        s_outputStream = null;
    }

    /*
     * Add an array of commands to the queue.
     */
    public static void queueCommands(String[] commands) {
        //Log.d("Queue", "Adding command: " + command);
        for (String cmd: commands) {
            queueCommand(cmd);
        }
    }

    /*
     * Add a single command to the queue.
     */
    public static void queueCommand(String cmd) {
        s_cmdQueue.add(cmd + "\r");
    }

    /*
     * Start a thread to process the commands in the queue.
     */
    private static void startQueueProcessor() {
        s_queueProc = true;
        //Set up thread here and have it look for new entries in s_cmdQueue.  Verify that command is finished.
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d("OBD", "Started Queue Processor.");
                String cmd;
                while (s_queueProc) {
                    cmd = null;
                    try {
                        //check if s_cmdQueue has any commands queued and that the car isn't currently processing a command.
                        if (!s_cmdQueue.isEmpty() && !s_isBusy) {
                            try {
                                //Toggle the busy flag to true.
                                setIsBusy(true);

                                // Pop command off the queue.
                                cmd = s_cmdQueue.poll();
                                if (cmd != null) {
                                    //Send command to bluetooth adapter.
                                    write(cmd.getBytes(Charset.forName("US-ASCII")));
                                }
                            } catch (IOException ioe) {
                                Log.d("OBD", "Error: " + ioe.getMessage());
                            }
                        }
                        Thread.sleep(SEND_CMD_DELAY);
                    } catch (Exception e) {
                        Log.d("OBD " + e.getCause(), "Error: " + e.getMessage() + " Command: " + cmd);
                    }
                }
                Log.d("OBD", "Stopping QueueProcessor");
            }
        }).start();
    }

    private static void startLoopCommands() {
        s_loopCmdsTimer = new Timer();
        initLoopCmdsTask();
        s_loopCmdsTimer.schedule(s_loopCmdsTask, 0, LOOP_CMD_INTERVAL);
    }

    private static void stopLoopCommands() {
        Log.d("OBD", "Stopping Loop Commands");
        if (s_loopCmdsTimer != null) {
            s_loopCmdsTimer.cancel();
            s_loopCmdsTimer = null;
        }
    }

    private static void initLoopCmdsTask() {
        s_loopCmdsTask = new TimerTask() {
            public void run() {
                String[] commands = new String[]{ObdCommands.speed,
                                            ObdCommands.engine_temp,
                                            ObdCommands.maf
                    };
                queueCommands(commands);
            }
        };
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
