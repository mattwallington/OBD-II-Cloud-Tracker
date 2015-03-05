package cargo.cargocollector;

import android.location.Location;

/**
 * Snapshot of data to be sent to the server.
 */
public class Snapshot {
    public static Location location = null;
    public static Float speed = null;
    public static Float engine_temp = null;
    public static Float maf = null;
    public static Integer rpm = null;
}
