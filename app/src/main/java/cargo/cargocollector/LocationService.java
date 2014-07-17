package cargo.cargocollector;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

/**
 * Created by matt on 7/8/14.
 */
public class LocationService implements LocationListener {

    /*  Location */
    private LocationManager locationManager;
    private String provider;
    private static final int MIN_DISTANCE = 1;
    private static final int MIN_TIME = 1000;

    private Context context = null;


    public LocationService(LocationManager locationManager) {
        this.locationManager = locationManager;
        this.context = context;
        activateLocation();
    }

    /*
     *   Location services
     */
    private void activateLocation() {
        //Find out which provider is best to use.  (GPS or network).
        setBestProvider();

        //Get initial location.  May be invalid.
        Location location = locationManager.getLastKnownLocation(provider);

        //Initialize location fields.
        if (location != null) {
            onLocationChanged(location);
        } else {
            Log.d("Location", "location is null");
        }

        locationManager.requestLocationUpdates(provider, MIN_TIME, MIN_DISTANCE, this);

    }

    @Override
    public void onLocationChanged(Location location) {
        //deal with location.
        try {
            String point = Double.toString(location.getLatitude()) + ", " + Double.toString(location.getLongitude());
            Log.d("Location", point);
            //TODO: Do something with location data.

        }
        catch (Exception e) {
            Log.d("Location", e.getMessage());
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        //Not sure what status this is talking about.

    }

    @Override
    public void onProviderEnabled(String provider) {
        setBestProvider();
    }

    @Override
    public void onProviderDisabled(String provider) {
        setBestProvider();
    }

    private void setBestProvider() {
        Criteria criteria = new Criteria();
        provider = locationManager.getBestProvider(criteria, true);
        Log.d("Location", "Location Provider: " + provider);
    }

    public void cancel() {
        Log.d("Location", "Cancelling Location service");
        locationManager.removeUpdates(this);
    }
}
