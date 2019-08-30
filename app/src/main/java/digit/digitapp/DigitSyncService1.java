package digit.digitapp;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.NotificationCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import digit.digitapp.digitService.SyncAction;
import retrofit2.Call;
import retrofit2.Response;


public class DigitSyncService1 extends Service {
    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;

    private final class ServiceHandler extends Handler {
        private Context applicationContext;
        private Context serviceContext;
        private CountDownLatch syncFinish;

        public ServiceHandler(Looper looper, Context context, Context serviceContext) {
            super(looper);
            this.applicationContext = context;
            this.serviceContext = serviceContext;
        }

        @Override
        public void handleMessage(final Message msg) {
            DigitServiceManager digitServiceManager = new DigitServiceManager(applicationContext);
            syncFinish = new CountDownLatch(1);
            String action = (String)msg.obj;
            SyncCallback callback = new SyncCallback() {
                @Override
                public void done() {
                    syncFinish.countDown();
                }

                @Override
                public void failed() {
                    syncFinish.countDown();
                }
            };
            if ("locationSync".equals(action)) {
                new LocationSyncManager(applicationContext).syncLocation(callback);
            }
            else if( action.startsWith("deviceSync.")) {
                String deviceId = action.substring("deviceSync.".length());
                new DeviceSynchronizationManager(deviceId, callback, serviceContext).performSynchronization();
            }
            try {
                syncFinish.await();
            } catch (InterruptedException e) {
                digitServiceManager.log("Interrupted Exception 2", 3, () ->
                        stopSelf(msg.arg1)
                );
            }
            stopSelf(msg.arg1);
        }
    }

    @Override
    public void onCreate() {
        HandlerThread thread = new HandlerThread("ServiceStartArguments",
                android.os.Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper, this.getApplicationContext(), this);
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

        startForeground(14, notification);

        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.obj = intent.getStringExtra("action");
        mServiceHandler.sendMessage(msg);
        // If we get killed, after returning from here, restart
        return START_REDELIVER_INTENT;
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