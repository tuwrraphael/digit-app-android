package digit.digitapp;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.JobIntentService;

import com.google.android.gms.location.GeofencingEvent;

public class GeofenceTransitionsJobIntentService extends JobIntentService {

    private static final int JOB_ID = 573;

    private static final String TAG = "GeofenceTransitionsIS";

    private static final String CHANNEL_ID = "channel_01";

    public static void enqueueWork(Context context, Intent intent) {
        enqueueWork(context, GeofenceTransitionsJobIntentService.class, JOB_ID, intent);
    }

    @Override
    protected void onHandleWork(Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        Context context = this.getApplicationContext();
        DigitServiceManager digitServiceManager = new DigitServiceManager(context);
        if (geofencingEvent.hasError()) {
            digitServiceManager.log("Geofence has errors", 3);
        }
        else {
            digitServiceManager.log("Geofence trigger type " +geofencingEvent.getGeofenceTransition(), 0);
            Intent i = new Intent(context, LocationService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(i);
            } else {
                startService(i);
            }
        }
    }
}