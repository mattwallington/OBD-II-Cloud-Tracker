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

            }).on("data", new Emitter.Listener() {

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
        mSocket.emit("data", obj);
    }
}
