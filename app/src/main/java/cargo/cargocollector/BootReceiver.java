package cargo.cargocollector;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

/**
 * Created by matt on 7/16/14.
 */
public class BootReceiver extends BroadcastReceiver {
    public CollectorService m_boundService = null;
    public ServiceConnection m_connection = null;

    @Override
    public void onReceive(Context context, Intent intent) {

        context.startService(new Intent(context, CollectorService.class));
        m_connection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder service) {
                m_boundService = ((CollectorService.CollectorBinder) service).getService();

                Log.d("CollectorService", "Service connected." + className.toString());
            }

            public void onServiceDisconnected(ComponentName className) {
                Log.d("CollectorService", "Service disconnected");
                m_boundService = null;
            }
        };
    }
}
