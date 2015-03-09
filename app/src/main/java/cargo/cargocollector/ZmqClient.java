package cargo.cargocollector;


import android.content.Context;
import android.util.Log;

import org.jeromq.ZMQ;
import org.jeromq.ZMQException;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by mattwallington on 3/1/15.
 */
public class ZmqClient {
    private static String TAG;
    private String mServerUri;
    private static final int RCV_TIMEOUT = 10000;
    private ZMQ.Context mZmqContext;
    private LinkedBlockingQueue<byte[]> mQueue;
    private boolean mQuitThread;
    //private ProcessSendQueue mProcessSendQueue;


    public ZmqClient(Context context) {
        String host = null;
        String port = null;

        TAG = this.getClass().getSimpleName();
        mZmqContext = ZMQ.context(1);
        mQueue = new LinkedBlockingQueue<byte[]>();
        mQuitThread = false;

        host = context.getString(R.string.host);
        port = context.getString(R.string.port);

        mServerUri = "tcp://" + host + ":" + port;
        Log.d(TAG, "Server URI: "+mServerUri);
    }


    public void start() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Start ZmqClient thread.");
                try {
                    ZMQ.Socket requester = mZmqContext.socket(ZMQ.REQ);

                    //Set recv timeout
                    requester.setReceiveTimeOut(RCV_TIMEOUT);

                    requester.connect(mServerUri);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e){
                        Log.e(TAG, "Thread interruption exception", e);
                    }

                    byte[] data = null;
                    while(!mQuitThread) {
                        Log.d(TAG, "--->Thread fired.");
                        try {
                            data = mQueue.poll(5000, TimeUnit.MILLISECONDS);
                        } catch (InterruptedException e) {
                            Log.e(TAG, "Queue take interrupted.", e);
                            break;
                        }
                        try {
                            boolean resp = requester.send(data, 0);

                            Log.d(TAG, "Sent.  Resp: "+resp);

                            ZMQ.Msg throwAway = null;
                            try {
                                throwAway = requester.recvMsg(0);
                                Log.d(TAG, "Received: "+throwAway.toString());
                            } catch (ZMQException e) {
                                Log.e(TAG, "ZMQ receive hit timeout.  Restarting socket.");
                                requester.close();
                                requester.connect(mServerUri);
                            }

                        } catch (Exception e) {
                            Log.e(TAG, "Exception sending data.", e);
                            requester.close();
                            mZmqContext.term();
                            mZmqContext = ZMQ.context(1);
                            requester = mZmqContext.socket(ZMQ.REQ);
                            requester.setReceiveTimeOut(RCV_TIMEOUT);
                            requester.connect(mServerUri);
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException ie){
                                Log.e(TAG, "Thread interruption exception", ie);
                            }
                        }
                        Log.v(TAG, "ZmqQueue Length: "+mQueue.size());
                    }
                    Log.d(TAG, "Closing sockets.");
                    requester.close();
                    mZmqContext.term();
                    Log.d(TAG, "END ZmqClient thread.");
                } catch (ZMQException e){
                    Log.e(TAG, "ZMQ Exception", e);
                }

            }
        }).start();
    }

    public void cancel() {
        mQuitThread = true;
    }

    public void send(byte[] msg) {
        try {
            //Log.d(TAG, "Adding msg to queue: " + msg);
            mQueue.offer(msg, 10000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG, "Queue put interrupted.", e);
        }
    }
}
