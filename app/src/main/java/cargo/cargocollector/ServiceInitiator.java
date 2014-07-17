package cargo.cargocollector;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.app.ActivityManager;
import android.app.Application;
import android.content.ComponentName;
import android.os.IBinder;
import android.os.IBinder;
import android.util.Log;

/**
 * Created by matt on 7/16/14.
 */
public class ServiceInitiator {

    private Context mContext;
    private Intent mIntent;

    private MainActivity mActivity;

    private ServiceConnection mConnection = null;
    private CollectorService mBoundService = null;
    private boolean mIsBound = false;
    private boolean mIsRunning = false;


    public ServiceInitiator(MainActivity activity, Context context) {
        mContext = context;
        mActivity = activity;
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
        mContext.bindService(mIntent, mConnection, Context.BIND_AUTO_CREATE);
    }

    public void unbind() {
        mContext.unbindService(mConnection);
    }

    private void createServiceConnection() {
        mConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder service) {
                mBoundService = ((CollectorService.CollectorBinder) service).getService();
                Log.d("ServiceBinder", "Service bound");
                //mTvStatus.append("Service bound.\n");
                mIsBound = true;

                mActivity.setButtonStatus(!mBoundService.getIsRunning(), mBoundService.getIsRunning());
                //mStartButton.setEnabled(!mBoundService.getIsRunning());
                //mStopButton.setEnabled(mBoundService.getIsRunning());
            }

            public void onServiceDisconnected(ComponentName className) {
                Log.d("ServiceBinder", "OnServiceDisconnected()");
                //mTvStatus.append("Service Unbound.\n");
                mBoundService = null;
                mIsBound = false;
            }
        };
    }



}
