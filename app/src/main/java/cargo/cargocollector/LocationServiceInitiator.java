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
public class LocationServiceInitiator {

    private Context m_context = null;
    private Intent m_intent = null;
    private MainActivity m_activity = null;

    private ServiceConnection m_connection = null;
    private LocationService m_boundService = null;

    private boolean m_isBound = false;


    public LocationServiceInitiator(Context context, MainActivity activity) {
        m_context = context;
        m_activity = activity;
    }

    public LocationServiceInitiator(Context context) {
        m_context = context;
    }

    public void initiateService(Class serviceClass) {
        m_intent = new Intent(m_context, serviceClass);

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
        boolean running = false;
        if (m_boundService != null) {
            running = m_boundService.isRunning();
        } else {
            running = false;
        }
        return running;
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
        Log.d("ServiceBinder", "Create Service Connection()");
        m_connection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder service) {
                m_boundService = ((LocationService.LocationBinder) service).getService();

                Log.d("ServiceBinder", "Service connected: " + className.toString());
                m_isBound = true;
            }

            public void onServiceDisconnected(ComponentName className) {
                Log.d("ServiceBinder", "Service disconnected");
                m_boundService = null;
                m_isBound = false;
            }
        };
    }



}
