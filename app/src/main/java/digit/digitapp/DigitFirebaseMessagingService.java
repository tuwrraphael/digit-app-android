package digit.digitapp;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

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
                    Intent i = new Intent(context, LocationService.class);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(i);
                    } else {
                        startService(i);
                    }
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
