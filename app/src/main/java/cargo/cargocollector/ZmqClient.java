package cargo.cargocollector;


import android.util.Log;

import org.jeromq.ZMQ;

/**
 * Created by mattwallington on 3/1/15.
 */
public class ZmqClient {
    private static ZMQ.Context m_context;
    private static ZMQ.Socket m_requester;
    private static String m_host;
    private static String m_port;

    public static void connect(final String data) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                m_context = ZMQ.context(1);

                m_requester = m_context.socket(ZMQ.REQ);

                m_requester.connect("tcp://" + m_host + ":" + m_port);

                m_requester.send(data.getBytes(), 0);

                byte[] reply = m_requester.recv(0);
                Log.d("ZMQ", "Data: " + reply.toString());
            }
        }).start();
    }

    public static void send(final String data) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                m_requester.send(data.getBytes(), 0);

                byte[] reply = m_requester.recv(0);
                Log.d("ZMQ", "Data: " + reply.toString());
            }
        }).start();
    }

    public static byte[] receive() {
        byte[] reply = m_requester.recv(0);
        return reply;
    }

    public static void setCredentials(String host, String port) {
        m_host = host;
        m_port = port;
    }

}
