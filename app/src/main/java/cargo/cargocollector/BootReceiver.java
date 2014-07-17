package cargo.cargocollector;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by matt on 7/16/14.
 */
public class BootReceiver extends BroadcastReceiver {
    ServiceInitiator initiator;

    @Override
    public void onReceive(Context context, Intent intent) {

        initiator = new ServiceInitiator(context);

        initiator.initiateService();

        initiator.start();
    }
}
