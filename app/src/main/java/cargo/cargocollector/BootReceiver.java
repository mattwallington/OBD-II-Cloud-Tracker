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
    public CollectorService mBoundService = null;
    public ServiceConnection mConnection = null;

    @Override
    public void onReceive(Context context, Intent intent) {

        context.startService(new Intent(context, CollectorService.class));
        mConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder service) {
                mBoundService = ((CollectorService.CollectorBinder) service).getService();

                Log.d("CollectorService", "Service connected." + className.toString());
            }

            public void onServiceDisconnected(ComponentName className) {
                Log.d("CollectorService", "Service disconnected");
                mBoundService = null;
            }
        };
    }
}
