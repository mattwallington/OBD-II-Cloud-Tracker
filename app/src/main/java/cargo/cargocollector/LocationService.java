package cargo.cargocollector;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.*;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.app.Service;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

/**
 * Created by matt on 7/8/14.
 */
public class LocationService implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener  {

    private IBinder m_binder = null;

    private static final int REQUEST_RESOLVE_ERROR = 1001;
    private static final int UPDATE_INTERVAL = 1000;
    private static final int FASTEST_INTERVAL = 1000;

    private LocationRequest m_locationRequest;
    private LocationListener m_locationListener;
    private GoogleApiClient m_googleApiClient;
    private Intent m_intent;
    private Context m_context;

    private boolean m_isRunning = false;

    public LocationService(Context context) {
        m_context = context;

        m_googleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        m_locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_INTERVAL)
                .setFastestInterval(FASTEST_INTERVAL);
    }

    public void start() {
        if (!m_googleApiClient.isConnected())
            m_googleApiClient.connect();
    }

    /* Begin ConnectionCallbacks callbacks */
    //*******************************************************

    @Override
    public void onConnected(Bundle bundle) {
        Log.d("Location", "Google API client connected.");

        PendingIntent pendingIntent = PendingIntent.getService(m_context, 0, new Intent(m_context, MyLocationHandler.class), PendingIntent.FLAG_UPDATE_CURRENT);
        LocationServices.FusedLocationApi.requestLocationUpdates(m_googleApiClient,m_locationRequest, pendingIntent);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d("Location", "Google API client suspended.");
    }

    /* Begin Connection Failed Listener */
    //********************************************************
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d("Location", "Google API client connection failed.");
    }

    public void cancel() {
        Log.d("Location", "Cancelling location service.");
        if(m_googleApiClient.isConnected())
            m_googleApiClient.disconnect();
    }


}

class MyLocationHandler extends IntentService {

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public MyLocationHandler(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        final android.location.Location location = intent.getParcelableExtra(FusedLocationProviderApi.KEY_LOCATION_CHANGED);
        Log.d("Location", "Lat: " + Double.toString(location.getLatitude()) + " Lng: " + Double.toString(location.getLongitude()) + " Timestamp: " + Long.toString(location.getTime()));
    }
}