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

    private Context mContext;
    private Intent mIntent;

    private MainActivity mActivity = null;

    private ServiceConnection mConnection = null;
    private CollectorService mBoundService = null;
    private boolean mIsBound = false;


    public ServiceInitiator(Context context, MainActivity activity) {
        mContext = context;
        mActivity = activity;
    }

    public ServiceInitiator(Context context) {
        mContext = context;
    }

    public void initiateService() {
        mIntent = new Intent(mContext, CollectorService.class);

        //Create a service connection
        createServiceConnection();
    }

    public void start() {
        mContext.startService(mIntent);
    }

    public void stop() {
        mBoundService.onDestroy();
    }

    public boolean isBound() {
        return mIsBound;
    }

    public boolean isRunning() {
        return mBoundService.getIsRunning();
    }

    public void bind() {
        Log.d("Service", "Binding service.");
        mContext.bindService(mIntent, mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    public void unbind() {
        Log.d("Service", "Unbinding service.");
        mContext.unbindService(mConnection);
        mIsBound = false;
    }

    private void createServiceConnection() {
        mConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder service) {
                mBoundService = ((CollectorService.CollectorBinder) service).getService();
                Log.d("ServiceBinder", "Service started");
                mIsBound = true;

                if (mActivity != null)
                    mActivity.setButtonStatus(!mBoundService.getIsRunning(), mBoundService.getIsRunning());
            }

            public void onServiceDisconnected(ComponentName className) {
                Log.d("ServiceBinder", "Service stopped");
                mBoundService = null;
                mIsBound = false;
            }
        };
    }



}
