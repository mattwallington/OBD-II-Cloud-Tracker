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
    private Socket socket = null;


    public SocketIOService(String socket_url) {
        //Constructor
        boolean status = connectSocket(socket_url);

    }

    public boolean connectSocket(String socket_url) {
        try {
            socket = IO.socket(socket_url);
            socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {

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
            socket.connect();
            return true;
        } catch (Exception e) {
            Log.d("SocketIO", e.getMessage());
            return false;
        }
    }

    public void sendData(JSONObject obj) {

        //JSONObject obj = new JSONObject();
        socket.emit("SocketIO", "Sending Data: " + obj);

    }
}
