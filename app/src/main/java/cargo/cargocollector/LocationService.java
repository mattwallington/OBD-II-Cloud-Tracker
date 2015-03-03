package cargo.cargocollector;

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
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

/**
 * Created by matt on 7/8/14.
 */
public class LocationService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private final IBinder m_binder = new LocationBinder();

    private static final int REQUEST_RESOLVE_ERROR = 1001;
    private static final int UPDATE_INTERVAL = 1000;
    private static final int FASTEST_INTERVAL = 1000;

    private LocationRequest m_locationRequest;
    private LocationListener m_locationListener;
    private GoogleApiClient m_googleApiClient;
    private Intent m_intent;

    private boolean m_isRunning = false;

    public LocationService() {
        Log.d("Location", "Location services created.");

        /*
        m_googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        m_locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_INTERVAL)
                .setFastestInterval(FASTEST_INTERVAL);
        */

    }


    public void start() {
        Log.d("Location", "Location services started.");
        if (!m_googleApiClient.isConnected())
            m_googleApiClient.connect();
    }


    public void cancel() {
        Log.d("Location", "Location services disconnected.");
        if (m_googleApiClient.isConnected())
            m_googleApiClient.disconnect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d("Location", "Location services connected.");
        //TODO: Fix broken logic here.  Need a different service for handling location requests.
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, m_intent, 0);
        LocationServices.FusedLocationApi.requestLocationUpdates(m_googleApiClient,m_locationRequest, pendingIntent);

    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d("Location", "Location services connection suspended.  Please reconnect.");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        //TODO: Handle errors.
        Log.d("Location", "Location services connection failed.");
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d("Location", "Lat: "+Double.toString(location.getLatitude())+" Lng: "+Double.toString(location.getLongitude())+" Timestamp: "+Long.toString(location.getTime()));
    }

    public class LocationBinder extends Binder {
        LocationService getService() {
            return LocationService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return m_binder;
        //throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //Created
        Log.d("Service", "onCreate()");
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("Location", "Starting location service!");
        m_isRunning = true;


        return START_STICKY;
    }

    public boolean isRunning() {
        return m_isRunning;
    }
}
