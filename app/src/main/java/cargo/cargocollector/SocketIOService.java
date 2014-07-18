package cargo.cargocollector;

import android.util.Log;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONObject;

/**
 * Created by matt on 7/8/14.
 */
public class SocketIOService {
    private Socket mSocket = null;
    private boolean status;


    public SocketIOService(String socket_url) {
        //Constructor
        status = connectSocket(socket_url);

    }

    public boolean connectSocket(String socket_url) {
        try {
            Log.d("SocketIO", "Connecting to " + socket_url);
            mSocket = IO.socket(socket_url);
            mSocket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    //socket.emit("foo", "hi");
                    //socket.disconnect();
                }

            }).on("event", new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                }

            }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                }

            });
            mSocket.connect();
            return true;
        } catch (Exception e) {
            Log.d("SocketIO", e.getMessage());
            return false;
        }
    }

    public void sendData(JSONObject obj) {
        JSONObject outerObj = new JSONObject();
        try {
            outerObj.put("device_id", "2cda7727f56276e8de548ab2");
            outerObj.put("device_data", obj);
        } catch (Exception e) {
            Log.d("SocketIO", e.getMessage());
        }
        mSocket.emit("event_data", outerObj.toString());
        /*
        try {
            Log.d("SocketIO", "Sending Data: " + outerObj.toString(2));
        } catch (Exception e) {
            Log.d("SocketIO", "Exception: " + e.getMessage());
        }
        */
    }
}
