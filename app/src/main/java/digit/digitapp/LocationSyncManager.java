package digit.digitapp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.support.annotation.NonNull;

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

public class LocationSyncManager {
    private  final Context applicationContext;
    private Location sentLocation;

    public LocationSyncManager(Context applicationContext) {
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
                .setNotificationResponsiveness(20000)
                .setExpirationDuration(geofenceRequest.getEnd().getTime() - new Date().getTime())
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT)
                .build());
        return builder.build();
    }

    public void syncLocation (SyncCallback callback) {
        final FusedLocationProviderClient fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(applicationContext);
        final retrofit2.Callback<LocationResponse> cb = new retrofit2.Callback<LocationResponse>() {
            @Override
            public void onResponse(@NonNull Call<LocationResponse> call, Response<LocationResponse> response) {
                if (null != response.body() && null != response.body().getNextUpdateRequiredAt()) {
                    AlarmManager alarmMgr = (AlarmManager)applicationContext.getSystemService(Context.ALARM_SERVICE);
                    Intent intent = new Intent(applicationContext, LocationAlarmReceiver.class);
                    intent.setAction("digit.digitapp.alarms");
                    PendingIntent alarmIntent = PendingIntent.getBroadcast(applicationContext, 0, intent, 0);
                    alarmMgr.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                            response.body().getNextUpdateRequiredAt().getTime(), alarmIntent);
                }
                if (null != response.body() && null != response.body().getRequestGeofence()){
                    GeofencingClient geofencingClient = LocationServices.getGeofencingClient(applicationContext);
                    try {
                        geofencingClient.addGeofences(
                                getGeofencingRequest(response.body().getRequestGeofence(), sentLocation.getLatitude(), sentLocation.getLongitude()),
                                getGeofencePendingIntent())
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        new DigitServiceManager(applicationContext).log("Geofence failed to add", 3, callback::failed);
                                    }
                                })
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {

                                        new DigitServiceManager(applicationContext).log("Geofence added successfully", 0, callback::failed);
                                    }
                                });
                    }
                    catch (SecurityException e) {
                        new DigitServiceManager(applicationContext).log("GeofenceSecurityError", 3, callback::failed);
                    }
                } else {
                    callback.done();
                }
            }

            @Override
            public void onFailure(@NonNull Call<LocationResponse> call, Throwable t) {
                new DigitServiceManager(applicationContext).log("Send location failed " + t.getMessage(), 3, callback::failed);
            }
        };
        try {
            fusedLocationProviderClient.getLastLocation()
                    .addOnSuccessListener(new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(final Location location) {
                            if (null == location || (Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis() - location.getTime()) > (1000 * 30)) {
                                final LocationCallback locationCallback = new LocationCallback() {
                                    @Override
                                    public void onLocationResult(LocationResult locationResult) {
                                        if (locationResult == null) {
                                            new DigitServiceManager(applicationContext).log("Location was null", 3, callback::failed);
                                            return;
                                        }
                                        fusedLocationProviderClient.removeLocationUpdates(this);
                                        sendLocation(locationResult.getLastLocation(), cb);
                                    };
                                };
                                LocationRequest mLocationRequest = new LocationRequest();
                                mLocationRequest.setMaxWaitTime(15000);
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
            new DigitServiceManager(applicationContext).log("Location security error", 3, callback::failed);
        }
    }
}
