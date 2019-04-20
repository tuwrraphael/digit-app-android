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
        CountDownLatch syncLatchReady;

        public ServiceHandler(Looper looper, Context context, Context serviceContext) {
            super(looper);
            this.applicationContext = context;
            this.serviceContext = serviceContext;
        }

        private void initializeSyncFinish(int size) {
            syncFinish = new CountDownLatch(size);
            syncLatchReady.countDown();
        }

        @Override
        public void handleMessage(final Message msg) {
            DigitServiceManager digitServiceManager = new DigitServiceManager(applicationContext);
            List<String> failedActions = new ArrayList<String>();
            boolean stop = false;
            while (!stop){
                List<String> startedActions = new ArrayList<String>();
                syncLatchReady = new CountDownLatch(1);
                digitServiceManager.getPendingSyncActions(new retrofit2.Callback<List<SyncAction>>() {
                    @Override
                    public void onResponse(Call<List<SyncAction>> call, Response<List<SyncAction>> response) {
                        if (response.body().isEmpty()) {
                            initializeSyncFinish(0);
                            return;
                        }
                        List<String> distinctActions = response.body().stream().map(s -> s.getId()).distinct().collect(Collectors.toList());
                        initializeSyncFinish(distinctActions.size());
                        for (String id : distinctActions) {
                            SyncCallback callback = new SyncCallback() {
                                @Override
                                public void done() {
                                    syncFinish.countDown();
                                }

                                @Override
                                public void failed() {
                                    failedActions.add(id);
                                    syncFinish.countDown();
                                }
                            };
                            if (failedActions.contains(id)) {
                                // TODO retry, continue for now
                                syncFinish.countDown();
                            }
                            else if ("locationSync".equals(id) || "legacyLocationSync".equals(id)) {
                                startedActions.add(id);
                                new LocationSyncManager(applicationContext).syncLocation(callback);
                            } else if (id.startsWith("deviceSync.")) {
                                startedActions.add(id);
                                String deviceId = id.substring("deviceSync.".length());
                                new DeviceSynchronizationManager(deviceId, callback, serviceContext).performSynchronization();
                            }
                        }
                    }
                    @Override
                    public void onFailure(Call<List<SyncAction>> call, Throwable t) {
                        digitServiceManager.log("Get SyncActions failed" + t.getMessage(), 3, () ->
                                initializeSyncFinish(0)
                        );
                    }
                });
                try {
                    syncLatchReady.await();
                    syncFinish.await();
                    if (startedActions.isEmpty()) {

                    }
                    stop = true; // TODO fix location loop first
                } catch (InterruptedException e) {
                    digitServiceManager.log("Interrupted Exception 2", 3, () ->
                            stopSelf(msg.arg1)
                    );
                }
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