package cargo.cargocollector;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

/**
 * Created by matt on 7/16/14.
 * Create Android service.
 */
public class ServiceInitiator {

    private Context m_context;
    private Intent m_intent;

    private MainActivity m_activity = null;

    private ServiceConnection m_connection = null;
    private CollectorService m_boundService = null;
    private boolean m_isBound = false;


    public ServiceInitiator(Context context, MainActivity activity) {
        m_context = context;
        m_activity = activity;
    }

    public ServiceInitiator(Context context) {
        m_context = context;
    }

    public void initiateService() {
        m_intent = new Intent(m_context, CollectorService.class);

        //Create a service connection
        createServiceConnection();
    }

    public void start() {
        m_context.startService(m_intent);
    }

    public void stop() {
        m_boundService.onDestroy();
    }

    public boolean isBound() {
        return m_isBound;
    }

    public boolean isRunning() {
        return m_boundService.getIsRunning();
    }

    public void bind() {
        Log.d("Service", "Binding service.");
        m_context.bindService(m_intent, m_connection, Context.BIND_AUTO_CREATE);
        m_isBound = true;
    }

    public void unbind() {
        Log.d("Service", "Unbinding service.");
        m_context.unbindService(m_connection);
        m_isBound = false;
    }

    private void createServiceConnection() {
        m_connection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder service) {
                m_boundService = ((CollectorService.CollectorBinder) service).getService();
                Log.d("ServiceBinder", "Service started");
                m_isBound = true;

                if (m_activity != null)
                    m_activity.setButtonStatus(!m_boundService.getIsRunning(), m_boundService.getIsRunning());
            }

            public void onServiceDisconnected(ComponentName className) {
                Log.d("ServiceBinder", "Service stopped");
                m_boundService = null;
                m_isBound = false;
            }
        };
    }



}
