package digit.digitapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.content.ContextCompat;

public class LocationAlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent i = new Intent(context, DigitSyncService1.class);
        i.putExtra("action", "locationSync");
        ContextCompat.startForegroundService(context, i);
    }
}
