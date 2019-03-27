package digit.digitapp;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import digit.digitapp.digitService.GeofenceRequest;
import digit.digitapp.digitService.LocationResponse;
import retrofit2.Call;
import retrofit2.Response;


public class LocationService extends Service {
    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;

    private final class ServiceHandler extends Handler {
        private Context applicationContext;
        private Location sentLocation;

        public ServiceHandler(Looper looper, Context applicationContext) {
            super(looper);
            this.applicationContext = applicationContext;
        }

        private  void sendLocation(final Location location, final retrofit2.Callback<LocationResponse> finished) {
            digit.digitapp.digitService.Location location1 = new digit.digitapp.digitService.Location();
            location1.setAccuracy(location.getAccuracy());
            location1.setLatitude(location.getLatitude());
            location1.setLongitude(location.getLongitude());
            location1.setTimestamp(new Date(location.getTime()));
            sentLocation = location;
            new DigitServiceManager(applicationContext).sendLocation(location1,finished);
        }

        private PendingIntent getGeofencePendingIntent() {
            Intent intent = new Intent(applicationContext, GeofenceBroadcastReceiver.class);
            return PendingIntent.getBroadcast(applicationContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        private GeofencingRequest getGeofencingRequest(GeofenceRequest geofenceRequest, double latitude, double longitude) {
            GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
            builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_EXIT);
            builder.addGeofence(new Geofence.Builder()
                    .setRequestId("digitGeofence1")
                    .setCircularRegion(
                            latitude,
                            longitude,
                            70
                    )
                    .setNotificationResponsiveness(60000)
                    .setExpirationDuration(geofenceRequest.getEnd().getTime() - new Date().getTime())
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT)
                    .build());
            return builder.build();
        }

        @Override
        public void handleMessage(final Message msg) {
            final FusedLocationProviderClient fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(applicationContext);
            final ActionFinished finished = new ActionFinished() {
                @Override
                public void finished() {
                    stopSelf();
                }
            };
            final retrofit2.Callback<LocationResponse> cb = new retrofit2.Callback<LocationResponse>() {
                @Override
                public void onResponse(Call<LocationResponse> call, Response<LocationResponse> response) {
                    if (null != response.body().getNextUpdateRequiredAt()) {
                        AlarmManager alarmMgr = (AlarmManager)applicationContext.getSystemService(Context.ALARM_SERVICE);
                        Intent intent = new Intent(applicationContext, LocationAlarmReceiver.class);
                        intent.setAction("digit.digitapp.alarms");
                        PendingIntent alarmIntent = PendingIntent.getBroadcast(applicationContext, 0, intent, 0);
                        alarmMgr.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                                response.body().getNextUpdateRequiredAt().getTime(), alarmIntent);
                    }
                    if (null != response.body().getRequestGeofence()){
                        GeofencingClient geofencingClient = LocationServices.getGeofencingClient(applicationContext);
                        try {
                            geofencingClient.addGeofences(
                                    getGeofencingRequest(response.body().getRequestGeofence(), sentLocation.getLatitude(), sentLocation.getLongitude()),
                                    getGeofencePendingIntent())
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            new DigitServiceManager(applicationContext).log("Geofence failed to add", 3, finished);
                                        }
                                    })
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {

                                            new DigitServiceManager(applicationContext).log("Geofence added successfully", 0, finished);
                                        }
                                    });
                        }
                        catch (SecurityException e) {
                            new DigitServiceManager(applicationContext).log("GeofenceSecurityError", 3, finished);
                        }
                    } else {
                        stopSelf();
                    }
                }

                @Override
                public void onFailure(Call<LocationResponse> call, Throwable t) {
                    new DigitServiceManager(applicationContext).log("Send location failed " + t.getMessage(), 3, finished);
                }
            };
            try {
                fusedLocationProviderClient.getLastLocation()
                    .addOnSuccessListener(new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(final Location location) {
                            if (null == location || (Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis() - location.getTime()) < (1000 *60 * 5)) {
                                final LocationCallback locationCallback = new LocationCallback() {
                                    @Override
                                    public void onLocationResult(LocationResult locationResult) {
                                        if (locationResult == null) {
                                            new DigitServiceManager(applicationContext).log("Location was null", 3, finished);
                                            return;
                                        }
                                        sendLocation(locationResult.getLastLocation(), cb);
                                        fusedLocationProviderClient.removeLocationUpdates(this);
                                    };
                                };
                                LocationRequest mLocationRequest = new LocationRequest();
                                mLocationRequest.setMaxWaitTime(10000);
                                mLocationRequest.setNumUpdates(1);
                                mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                                fusedLocationProviderClient.requestLocationUpdates(mLocationRequest,locationCallback, null);
                            }
                            else {
                                sendLocation(location, cb);
                            }
                        }
                    });            }
            catch (SecurityException e) {
                new DigitServiceManager(applicationContext).log("Location security error", 3, finished);
            }
        }
    }

    @Override
    public void onCreate() {
        HandlerThread thread = new HandlerThread("ServiceStartArguments",
                android.os.Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper, this.getApplicationContext());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification =
                new NotificationCompat.Builder(this, "default")
                        .setContentTitle(getText(R.string.location_notification_title))
                        .setContentText(getText(R.string.location_notification_content))
                        .setSmallIcon(R.drawable.ic_stat_onesignal_default)
                        .setContentIntent(pendingIntent)
                        .setTicker(getText(R.string.location_notification_content))
                        .build();

        startForeground(12, notification);

        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        mServiceHandler.sendMessage(msg);

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

    @Override
    public void onDestroy() {

    }
}

