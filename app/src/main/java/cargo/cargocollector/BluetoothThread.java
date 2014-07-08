package cargo.cargocollector;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by matt on 6/18/14.
 */
public class BluetoothThread extends Thread {

    private Listener listener;

    private boolean isBusy = false;
    private Queue<String> cmdQueue;

    public static interface Listener {
        void onConnected();
        void onReceived(byte[] buffer, int length);
        void onDisconnected();
        void onError(IOException e);
    }

    private static final int BUFFER_SIZE = 1024;
    private BluetoothSocket socket;

    // Create new instance
    public static BluetoothThread newInstance(BluetoothSocket socket, Listener listener) {
        BluetoothThread instance = new BluetoothThread(socket, listener);
        instance.start();
        return instance;
    }

    public void setIsBusy(boolean status) {
        this.isBusy = status;
        //Log.d("Status", "Status: " + Boolean.toString(status));
    }

    public boolean getIsBusy() {
        return isBusy;
    }

    protected BluetoothThread(BluetoothSocket socket, Listener listener) {
        this.socket = socket;
        this.listener = listener;
        cmdQueue = new LinkedList<String>();
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

    public void startQueueProcessor() {
        //Set up thread here and have it look for new entries in cmdQueue.  Verify that command is finished.
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if (getIsBusy() == false) {
                        try {
                            //check if cmdQueue has any commands queued.
                            if (!cmdQueue.isEmpty()) {
                                //If so, pop one off and run the command.
                                try {
                                    setIsBusy(true);
                                    String cmd = cmdQueue.remove();
                                    //Log.d("Sent", cmd);
                                    write(cmd.getBytes(Charset.forName("US-ASCII")));
                                } catch (IOException e) {
                                    Log.d("OBD", "IO Exception: " + e.getMessage());
                                }
                            }
                            //Thread.sleep(200);
                        } catch (Exception e) {
                            Log.d("Exception", e.getMessage());
                        }
                    }
                }
            }
        }).start();
    }
}
