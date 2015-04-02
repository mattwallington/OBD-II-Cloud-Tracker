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

    private Context mContext = null;
    private NotificationCompat.Builder mBuilder = null;
    private int mCounter = 0;


    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;

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
        mCounter++;
        Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();

        mBuilder = new NotificationCompat.Builder(mContext);
        mBuilder.setSmallIcon(R.drawable.stone);
        mBuilder.setContentTitle("Cargo");
        mBuilder.setContentText(msg);
        NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(mContext.NOTIFICATION_SERVICE);
        notificationManager.notify(mCounter, mBuilder.build());

    }
}
