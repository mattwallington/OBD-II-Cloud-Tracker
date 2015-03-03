package cargo.cargocollector;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by matt on 7/16/14.
 */
public class BootReceiver extends BroadcastReceiver {
    CollectorServiceInitiator m_collectorServiceInitiator;

    @Override
    public void onReceive(Context context, Intent intent) {

        m_collectorServiceInitiator = new CollectorServiceInitiator(context);

        m_collectorServiceInitiator.initiateService(CollectorService.class);

        m_collectorServiceInitiator.start();
    }
}
