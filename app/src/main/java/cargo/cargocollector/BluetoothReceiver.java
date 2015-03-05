package cargo.cargocollector;

import android.app.NotificationManager;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

/**
 * Created by mattwallington on 3/4/15.
 */
public class BluetoothReceiver extends BroadcastReceiver {

    private Context m_context = null;
    private NotificationCompat.Builder m_builder = null;
    private int counter = 0;


    @Override
    public void onReceive(Context context, Intent intent) {
        m_context = context;

        switch(intent.getAction()) {
            case BluetoothDevice.ACTION_FOUND:
                //Toast.makeText(context, "Found", Toast.LENGTH_SHORT).show();
                notify("Found");
                break;
            case BluetoothDevice.ACTION_ACL_CONNECTED:
                //Toast.makeText(context, "ACL Connected", Toast.LENGTH_SHORT).show();
                notify(intent.getAction());
                break;
            case BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED:
                //Toast.makeText(context, "ACL Disconnect Requested", Toast.LENGTH_SHORT).show();
                notify("ACL Disconnect Requested");
                break;
            case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                //Toast.makeText(context, "ACL Disconnected", Toast.LENGTH_SHORT).show();
                notify("ACL Disconnected");
                break;
            default:
                //Toast.makeText(context, "Other: "+intent.getAction(), Toast.LENGTH_SHORT).show();
                notify(intent.getAction());
                break;
        }
    }

    private void notify(String msg) {
        counter++;
        Toast.makeText(m_context, msg, Toast.LENGTH_SHORT).show();

        m_builder = new NotificationCompat.Builder(m_context);
        m_builder.setSmallIcon(R.drawable.common_signin_btn_icon_dark);
        m_builder.setContentTitle("Cargo");
        m_builder.setContentText(msg);
        NotificationManager notificationManager = (NotificationManager) m_context.getSystemService(m_context.NOTIFICATION_SERVICE);
        notificationManager.notify(counter, m_builder.build());

    }
}
