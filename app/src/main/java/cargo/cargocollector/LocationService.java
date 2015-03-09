package cargo.cargocollector;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

/**
 * Created by matt on 7/8/14.
 */
public class LocationService implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener  {

    private static final String TAG = "LocationService";

    private static final int REQUEST_RESOLVE_ERROR = 1001;
    private static final int UPDATE_INTERVAL = 1000;
    private static final int FASTEST_INTERVAL = 1000;

    private LocationListener mLocationListener;
    private GoogleApiClient mGoogleApiClient;
    private Intent mIntent;
    private Context mContext;

    private LocationRequest mLocationRequest;

    private int mCounter;

    private NotificationCompat.Builder mBuilder;

    private boolean mIsRunning = false;

    public LocationService(Context context) {
        mContext = context;

        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_INTERVAL)
                .setFastestInterval(FASTEST_INTERVAL);

        mBuilder = null;
        mCounter = 0;
    }

    public void start() {
        if (!mGoogleApiClient.isConnected())
            mGoogleApiClient.connect();
    }

    /* Begin ConnectionCallbacks callbacks */
    //*******************************************************

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "Google API client connected.");
        FusedLocationProviderApi fusedLocationProviderApi = LocationServices.FusedLocationApi;
        Location curLocation = fusedLocationProviderApi.getLastLocation(mGoogleApiClient);

        //String loc = "1Lat: " + curLocation.getLatitude() + " Lng: " + curLocation.getLongitude() + " Timestamp: " + curLocation.getTime();
        //notification(loc);

        fusedLocationProviderApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "Google API client suspended.");
    }

    /* Begin Connection Failed Listener */
    //********************************************************
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "Google API client connection failed.");
    }

    public void cancel() {
        Log.d(TAG, "Cancelling location service.");
        if(mGoogleApiClient.isConnected())
            mGoogleApiClient.disconnect();
    }

    public void notification(String location) {
        mCounter++;
        Log.d(TAG, location);
        Toast.makeText(mContext, location, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLocationChanged(Location location) {
        String loc = "Lat: " + location.getLatitude() + " Lng: " + location.getLongitude() + " Timestamp: " + location.getTime();
        //notification(loc);
        Snapshot.location = location;
    }
}

