package digit.digitapp;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.content.ContextCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;

import java.util.Map;

public class DigitFirebaseMessagingService extends FirebaseMessagingService {
    @Override
    public void onNewToken(String s) {
        new PushSubscriptionManager(this.getApplicationContext()).sendToken(s);
        super.onNewToken(s);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Map<String, String> data = remoteMessage.getData();
        if (data.containsKey("actions")) {
            final Context context = getApplicationContext();
            Gson gson = new Gson();
            String[] actions = gson.fromJson(data.get("actions"), String[].class);
            for (String action: actions) {
                Intent i = new Intent(context, DigitSyncService1.class);
                i.putExtra("action", action);
                ContextCompat.startForegroundService(context, i);
            }
        }
        super.onMessageReceived(remoteMessage);
    }
}
