package cargo.cargocollector;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by matt on 7/16/14.
 */
public class BootReceiver extends BroadcastReceiver {
    ServiceInitiator m_initiator;

    @Override
    public void onReceive(Context context, Intent intent) {

        m_initiator = new ServiceInitiator(context);

        m_initiator.initiateService();

        m_initiator.start();
    }
}
