package digit.digitapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.v4.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Date;
import java.util.Map;

public class DigitFirebaseMessagingService extends FirebaseMessagingService {
    @Override
    public void onNewToken(String s) {
        new PushSubscriptionManager(this.getApplicationContext()).sendToken(s);
        getSharedPreferences("_", MODE_PRIVATE).edit().putString("fb", s).apply();
        super.onNewToken(s);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Map<String, String> data = remoteMessage.getData();
        final Context context = getApplicationContext();
        if (data.containsKey("Action")) {
            String action = data.get("Action");
            switch (action) {
                case "send_location":
                    final FusedLocationProviderClient fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
                    fusedLocationProviderClient.getLastLocation()
                            .addOnSuccessListener(new OnSuccessListener<Location>() {
                                @Override
                                public void onSuccess(final Location location) {
                                    if (null == location) {
                                        final LocationCallback locationCallback = new LocationCallback() {
                                            @Override
                                            public void onLocationResult(LocationResult locationResult) {
                                                if (locationResult == null) {
                                                    return;
                                                }
                                                digit.digitapp.digitService.Location location1 = new digit.digitapp.digitService.Location();
                                                Location lastLocation = locationResult.getLastLocation();
                                                location1.setAccuracy(lastLocation.getAccuracy());
                                                location1.setLatitude(lastLocation.getLatitude());
                                                location1.setLongitude(lastLocation.getLongitude());
                                                location1.setTimestamp(new Date());
                                                new DigitServiceManager(context).sendLocation(location1);
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
                                        digit.digitapp.digitService.Location location1 = new digit.digitapp.digitService.Location();
                                        location1.setAccuracy(location.getAccuracy());
                                        location1.setLatitude(location.getLatitude());
                                        location1.setLongitude(location.getLongitude());
                                        location1.setTimestamp(new Date());
                                        new DigitServiceManager(context).sendLocation(location1);
                                    }
                                }
                            });
                    break;
                default:
                    break;
            }
        }
        super.onMessageReceived(remoteMessage);
    }

    public static String getToken(Context context) {
        return context.getSharedPreferences("_", MODE_PRIVATE).getString("fb", "empty");
    }
}
