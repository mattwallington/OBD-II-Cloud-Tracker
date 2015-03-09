package cargo.cargocollector;


import android.util.Log;

import org.jeromq.ZMQ;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by mattwallington on 3/1/15.
 */
public class ZmqClient {
    private static String TAG;
    private static final String HOST = "zmq.spartacus.io";
    private static final String PORT = "5555";
    private ZMQ.Context mContext;
    private LinkedBlockingQueue<byte[]> mQueue;
    private boolean mQuitThread;
    //private ProcessSendQueue mProcessSendQueue;


    public ZmqClient() {
        TAG = this.getClass().getSimpleName();
        mContext = ZMQ.context(1);
        mQueue = new LinkedBlockingQueue<byte[]>();
        mQuitThread = false;

        //connect();
    }

    public void connect() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ZMQ.Socket requester = mContext.socket(ZMQ.REQ);
                requester.connect("tcp://" + HOST + ":" + PORT);
            }
        }).start();
    }

    public void start() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Start ZmqClient thread.");
                ZMQ.Socket requester = mContext.socket(ZMQ.REQ);
                requester.connect("tcp://" + HOST + ":" + PORT);
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
                        //String throwAway = requester.recvMsg(0);
                        ZMQ.Msg throwAway = requester.recvMsg(0);
                        //byte[] throwAway = requester.recv(0);

                        Log.d(TAG, "Received: "+throwAway.toString());
                    } catch (Exception e) {
                        Log.e(TAG, "Exception sending data.", e);
                    }

                }
                Log.d(TAG, "Closing sockets.");
                requester.close();
                mContext.term();
                Log.d(TAG, "END ZmqClient thread.");
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
