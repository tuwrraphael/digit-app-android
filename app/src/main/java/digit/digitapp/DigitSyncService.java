/*
package digit.digitapp;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.NotificationCompat;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import androidx.annotation.NonNull;
import digit.digitapp.digitService.DeviceData;
import digit.digitapp.digitService.LegData;
import digit.digitapp.digitService.SyncAction;
import no.nordicsemi.android.ble.BleManager;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;


public class DigitSyncService extends Service {
    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;

    private final class ServiceHandler extends Handler {
        private Context applicationContext;
        private BluetoothGatt mBluetoothGatt;

        private static final int STATE_READY = 0;

        private static final int STATE_CONNECTING= 1;

        private static final int STATE_DISCOVERY = 2;

        private static final int STATE_WRITE_CTS = 3;

        private static final int STATE_WRITE_EVENT = 4;

        private static final int STATE_WRITE_DIRECTIONS = 5;

        private static final int STATE_WRITE_LEGS = 6;

        private int state = STATE_READY;

        public ServiceHandler(Looper looper, Context applicationContext) {
            super(looper);
            this.applicationContext = applicationContext;
            digitServiceManager = new DigitServiceManager(applicationContext);
        }
        private DigitServiceManager digitServiceManager;

        final String digitServiceId = "00001523-1212-efde-1523-785fef13d123";
        final String  ctsCharId = "00001805-1212-efde-1523-785fef13d123";
        final String directionsCharId = "00001525-1212-efde-1523-785fef13d123";
        final String eventCharId = "00001524-1212-efde-1523-785fef13d123";
        final String legCharId = "00001526-1212-efde-1523-785fef13d123";

        private static final int subjectLength = 20;
        private static final int ctsLength = 7;
        private static final int directionLength = 20;
        private static final int stopLength = 20;
        private static final int lineLength = 6;

        private BluetoothGattService digitService;
        private ActionFinished finished;
        private DeviceData deviceData;
        private int legIndex = 0;

        private  byte[] FormatCts(Calendar calendar) {
            Date d;
            byte year0 = (byte)(calendar.get(Calendar.YEAR) & 0xFF);
            byte year1 = (byte)((calendar.get(Calendar.YEAR)) >> 8 & 0xFF);
            return  new byte[] {year0,year1,
                    (byte)calendar.get(Calendar.MONTH),
                    (byte)calendar.get(Calendar.DATE),
                    (byte)calendar.get(Calendar.HOUR_OF_DAY),
                    (byte)calendar.get(Calendar.MINUTE),
                    (byte)calendar.get(Calendar.SECOND) };
        }

        private  byte[] FormatCts(Date date) {
            Calendar c = Calendar.getInstance();
            c.setTime(date);
            return  FormatCts(c);
        }

        private String constrain(String str, int length) {
            return str.substring(0,Math.min(str.length(), length));
        }

        private  void syncDevice(final String deviceId, final ActionFinished finished) {
            if (state != STATE_READY) {
                digitServiceManager.log("Last sync must be finished first", 3, finished);
                return;
            }
            this.finished = finished;
            final BluetoothManager bluetoothManager =
                    (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            final BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
            if (null == bluetoothManager || !bluetoothAdapter.isEnabled()) {
                digitServiceManager.log("Bluetooth not enabled", 3, finished);
                return;
            }
            final BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceId);
            if (device == null) {
                digitServiceManager.log("Device not found", 3, finished);
            }
            digitServiceManager.getDeviceData(deviceId, new retrofit2.Callback<DeviceData>() {
                @Override
                public void onResponse(Call<DeviceData> call, Response<DeviceData> response) {
                    if (response.isSuccessful()) {
                        deviceData = response.body();
                        mBluetoothGatt = device.connectGatt(applicationContext, true, mGattCallback);
                        state = STATE_CONNECTING;
                    }
                    else {
                        digitServiceManager.log("Could not access device data: " + response.code(), 3, finished);
                    }
                }

                @Override
                public void onFailure(Call<DeviceData> call, Throwable t) {
                    digitServiceManager.log("Could not access device data", 3, finished);
                }
            });
        }

        private void WriteCts() {
            final BluetoothGattCharacteristic ctsChar = digitService.getCharacteristic(UUID.fromString(ctsCharId));
            ctsChar.setValue(FormatCts(Calendar.getInstance()));
            mBluetoothGatt.writeCharacteristic(ctsChar);
        }

        private void WriteEvent() {
            final BluetoothGattCharacteristic eventChar = digitService.getCharacteristic(UUID.fromString(eventCharId));
            if (null == deviceData.getEvent()) {
                eventChar.setValue(new byte[]{0});
            } else {
                String subject = constrain(deviceData.getEvent().getSubject(), subjectLength-1);
                byte [] subjectBytes = subject.getBytes(StandardCharsets.ISO_8859_1);
                byte[] data = new byte[ctsLength + subjectBytes.length + 1];
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(deviceData.getEvent().getStart());
                System.arraycopy(FormatCts(calendar),0,data, 0,ctsLength);
                System.arraycopy(subjectBytes,0,data, ctsLength,subjectBytes.length);
                data[ctsLength + subjectBytes.length] = '\0';
                eventChar.setValue(data);
            }
            mBluetoothGatt.writeCharacteristic(eventChar);
        }

        private void WriteDirections() {
            final BluetoothGattCharacteristic directionsChar = digitService.getCharacteristic(UUID.fromString(directionsCharId));
            if (null == deviceData.getDirections()) {
                directionsChar.setValue(new byte[]{0});
            } else {
                byte[] data = new byte[2 * ctsLength + 1];
                data[0] = (byte)deviceData.getDirections().getLegs().size();
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(deviceData.getDirections().getDepartureTime());
                System.arraycopy(FormatCts(calendar),0,data, 1,ctsLength);
                calendar.setTime(deviceData.getDirections().getArrivalTime());
                System.arraycopy(FormatCts(calendar),0,data, 1 + ctsLength,ctsLength);
                directionsChar.setValue(data);
            }
            mBluetoothGatt.writeCharacteristic(directionsChar);
        }

        private  void WriteLeg(int legIndex) {
            final BluetoothGattCharacteristic legChar = digitService.getCharacteristic(UUID.fromString(legCharId));
            LegData leg = deviceData.getDirections().getLegs().get(legIndex);
            String line = constrain(leg.getLine(), lineLength - 1);
            String direction = constrain(leg.getDirection(), directionLength - 1);
            String departureStop = constrain(leg.getDepartureStop(), stopLength - 1);
            String arrivalStop = constrain(leg.getArrivalStop(), stopLength - 1);
            byte[] lineBytes = line.getBytes(StandardCharsets.ISO_8859_1);
            byte[] directionBytes = direction.getBytes(StandardCharsets.ISO_8859_1);
            byte[] departureStopBytes = departureStop.getBytes(StandardCharsets.ISO_8859_1);
            byte[] arrivalStopBytes = arrivalStop.getBytes(StandardCharsets.ISO_8859_1);

            byte[] data = new byte[1 + ctsLength +
                    lineBytes.length +
                    directionBytes.length +
                    departureStopBytes.length +
                    arrivalStopBytes.length + 4];
            data[0] = (byte)legIndex;
            System.arraycopy(FormatCts(leg.getDepartureTime()),0,data, 1,ctsLength);
            data[1 + ctsLength] = (byte)lineBytes.length;
            data[1 + ctsLength + 1] = (byte)directionBytes.length;
            data[1 + ctsLength + 2] = (byte)departureStopBytes.length;
            data[1 + ctsLength + 3] = (byte)arrivalStopBytes.length;
            System.arraycopy(lineBytes,0,data, 1 + ctsLength + 4,lineBytes.length);
            System.arraycopy(directionBytes,0,data, 1 + ctsLength + 4 + lineBytes.length,directionBytes.length);
            System.arraycopy(departureStopBytes,0,data,  1 + ctsLength + 4 + lineBytes.length + directionBytes.length,departureStopBytes.length);
            System.arraycopy(arrivalStopBytes,0,data,  1 + ctsLength + 4 + lineBytes.length + directionBytes.length + departureStopBytes.length,arrivalStopBytes.length);
            legChar.setValue(data);
            mBluetoothGatt.writeCharacteristic(legChar);
        }

        private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                if (newState == BluetoothGatt.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                    if (state == STATE_CONNECTING) {
                        state = STATE_DISCOVERY;
                        postDelayed(() -> {
                            if (gatt.getDevice().getBondState() != BluetoothDevice.BOND_BONDING) {
                                gatt.discoverServices();
                            }
                        }, 10000);
                    }
                }
                else if (status != BluetoothGatt.GATT_SUCCESS) {
                    digitServiceManager.log("gatt connect failed " + status, 3, finished);
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (state == STATE_DISCOVERY) {
                        digitService = mBluetoothGatt.getService(UUID.fromString(digitServiceId));
                        if (null == digitService) {
                            state = STATE_READY;
                            digitServiceManager.log("digit service not found", 3, finished);
                        }
                        else {
                            state = STATE_WRITE_CTS;
                            WriteCts();
                        }
                    }
                }
            }

            private  void EndSync() {
                digitService = null;
                mBluetoothGatt.disconnect();
                mBluetoothGatt.close();
                mBluetoothGatt = null;
                state = STATE_READY;
                digitServiceManager.log("synced!", 1, finished);
                finished = null;
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    boolean finished = false;
                    if (state == STATE_WRITE_CTS) {
                        state = STATE_WRITE_EVENT;
                        WriteEvent();
                    }
                    else  if (state == STATE_WRITE_EVENT){
                        state = STATE_WRITE_DIRECTIONS;
                        WriteDirections();
                    }
                    else if (state == STATE_WRITE_DIRECTIONS)
                    {
                        if (null != deviceData.getDirections() && !deviceData.getDirections().getLegs().isEmpty()) {
                            legIndex = 0;
                            state = STATE_WRITE_LEGS;
                            WriteLeg(legIndex);
                        }
                        else {
                            finished = true;
                        }
                    }
                    else if (state == STATE_WRITE_LEGS) {
                        legIndex++;
                        if (legIndex >= deviceData.getDirections().getLegs().size()) {
                            finished = true;
                        }
                        else {
                            WriteLeg(legIndex);
                        }
                    }
                    if (finished) {
                        digitServiceManager.setSynched("deviceSync." + gatt.getDevice().getAddress(), new retrofit2.Callback<ResponseBody>() {
                            @Override
                            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                EndSync();
                            }

                            @Override
                            public void onFailure(Call<ResponseBody> call, Throwable t) {
                                EndSync();
                            }
                        });
                    }
                }
                else {
                    digitServiceManager.log("gatt write failed " + status, 3, finished);
                }
            }
        };

        @Override
        public void handleMessage(final Message msg) {
            final ActionFinished stopSelfAction = new ActionFinished() {
                @Override
                public void finished() {
                    stopSelf();
                }
            };
            digitServiceManager.getPendingSyncActions(new retrofit2.Callback<List<SyncAction>>() {
                @Override
                public void onResponse(Call<List<SyncAction>> call, Response<List<SyncAction>> response) {
                    boolean locationSync = false;
                    List<String> syncDevices = new ArrayList<>();
                    for (SyncAction syncAction : response.body()) {
                        if ("locationSync".equals(syncAction.getId()) || "legacyLocationSync".equals(syncAction.getId())) {
                            locationSync = true;
                        } else if (syncAction.getId().startsWith("deviceSync.")) {
                            String deviceId = syncAction.getId().substring("deviceSync.".length());
                            syncDevices.add(deviceId);
                        }
                    }
                    if (locationSync) {
                        Intent i = new Intent(applicationContext, LocationService.class);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(i);
                        } else {
                            startService(i);
                        }
                    }
                    if (syncDevices.isEmpty()) {
                        stopSelf();
                    }
                    else {
                        syncDevice(syncDevices.get(0),stopSelfAction);
                    }
                }
                @Override
                public void onFailure(Call<List<SyncAction>> call, Throwable t) {
                    digitServiceManager.log("Get SyncActions failed" + t.getMessage(), 3, stopSelfAction);
                }
            });
        }
    }

    @Override
    public void onCreate() {
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
        HandlerThread thread = new HandlerThread("ServiceStartArguments",
                android.os.Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper, this.getApplicationContext());
    }

    private  boolean started = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!started) {
            Message msg = mServiceHandler.obtainMessage();
            msg.arg1 = startId;
            mServiceHandler.sendMessage(msg);
            started = true;
            mServiceHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopSelf();
                }
            }, 5 * 60000);
        }
        else {
            // TODO check again after syncs have completed
        }
        // If we get killed, after returning from here, restart
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

    @Override
    public void onDestroy() {

    }
}*/
