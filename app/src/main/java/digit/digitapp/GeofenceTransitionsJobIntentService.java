package digit.digitapp;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.JobIntentService;
import android.support.v4.content.ContextCompat;

import com.google.android.gms.location.GeofencingEvent;

import java.util.stream.Collectors;

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
            String ids = geofencingEvent.getTriggeringGeofences().stream()
                    .map(f -> f.getRequestId()).collect(Collectors.joining(", "));
            digitServiceManager.log("Geofence " + ids+" type " +geofencingEvent.getGeofenceTransition(), 0);
            Intent i = new Intent(context, DigitSyncService1.class);
            i.putExtra("action","locationSync");
            ContextCompat.startForegroundService(context, i);
        }
    }
}