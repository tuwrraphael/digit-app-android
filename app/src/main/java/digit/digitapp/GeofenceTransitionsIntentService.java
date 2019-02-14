package digit.digitapp;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.google.android.gms.location.GeofencingEvent;

public class GeofenceTransitionsIntentService extends IntentService {

    private final Context context;

    public GeofenceTransitionsIntentService(String name) {
        super(name);
        context = this.getApplicationContext();
    }

    protected void onHandleIntent(Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
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
