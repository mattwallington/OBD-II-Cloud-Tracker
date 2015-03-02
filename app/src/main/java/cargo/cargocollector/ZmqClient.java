package cargo.cargocollector;


import android.util.Log;

import org.jeromq.ZMQ;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Created by mattwallington on 3/1/15.
 */
public class ZmqClient {
    private static ZMQ.Context mContext;
    private static ZMQ.Socket mRequester;
    private static String mHost;
    private static String mPort;

    public static void connect(final String data) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                mContext = ZMQ.context(1);

                mRequester = mContext.socket(ZMQ.REQ);

                mRequester.connect("tcp://" + mHost + ":" + mPort);

                mRequester.send(data.getBytes(), 0);

                byte[] reply = mRequester.recv(0);
                Log.d("ZMQ", "Data: " + reply.toString());
            }
        }).start();
    }

    public static void send(final String data) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                mRequester.send(data.getBytes(), 0);

                byte[] reply = mRequester.recv(0);
                Log.d("ZMQ", "Data: " + reply.toString());
            }
        }).start();
    }

    public static byte[] receive() {
        byte[] reply = mRequester.recv(0);
        return reply;
    }

    public static void setCredentials(String host, String port) {
        mHost = host;
        mPort = port;
    }

}
