package digit.digitapp;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import java.util.List;
import java.util.function.Predicate;

import digit.digitapp.digitService.SyncAction;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class DigitSyncService extends Service {
    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;

    private final class ServiceHandler extends Handler {
        private Context applicationContext;

        public ServiceHandler(Looper looper, Context applicationContext) {
            super(looper);
            this.applicationContext = applicationContext;
        }

        @Override
        public void handleMessage(final Message msg) {
            final DigitServiceManager digitServiceManager = new DigitServiceManager(applicationContext);
            final ActionFinished stopSelfAction = new ActionFinished() {
                @Override
                public void finished() {
                    stopSelf();
                }
            };
            digitServiceManager.getPendingSyncActions(new retrofit2.Callback<List<SyncAction>>() {
                @Override
                public void onResponse(Call<List<SyncAction>> call, Response<List<SyncAction>> response) {
                    for (SyncAction syncAction : response.body()) {
                        if (syncAction.getId() == "locationSync" || syncAction.getId() == "legacyLocationSync") {
                            Intent i = new Intent(applicationContext, LocationService.class);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                startForegroundService(i);
                            } else {
                                startService(i);
                            }
                        } else if (syncAction.getId().startsWith("deviceSync.")) {
                            // TODO device sync
                        }
                    }
                    stopSelf();
                }
                @Override
                public void onFailure(Call<List<SyncAction>> call, Throwable t) {
                    new DigitServiceManager(applicationContext).log("Get SyncActions failed" + t.getMessage(), 3, stopSelfAction);
                }
            });
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
                        .setContentTitle(getText(R.string.syncing_notification_title))
                        .setContentText(getText(R.string.syncing_notification_content))
                        .setSmallIcon(R.drawable.ic_stat_onesignal_default)
                        .setContentIntent(pendingIntent)
                        .setTicker(getText(R.string.syncing_notification_content))
                        .build();

        startForeground(13, notification);

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

