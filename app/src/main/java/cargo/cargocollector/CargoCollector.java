package cargo.cargocollector;
import android.app.Service;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.IBinder;
import android.util.Log;


/**
 * Created by matt on 6/26/14.
 */
public class CargoCollector extends Service {

    private LocationManager locMan;
    private String provider;

    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("Service", "Service started");

        //Start location manager
        enableLocMan();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("Service", "Service stopped");
    }

    private void enableLocMan() {
        locMan = (LocationManager) getSystemService(LOCATION_SERVICE);

        Criteria criteria = new Criteria();
        provider = locMan.getBestProvider(criteria, false);

        Location location = locMan.getLastKnownLocation(provider);

        String loc = Double.toString(location.getLatitude()) + ", " + Double.toString(location.getLongitude());
        Log.d("Location", loc);

    }

}
