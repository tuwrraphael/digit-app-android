package digit.digitapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Looper;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;

import digit.digitapp.ble.DigitBleServiceManager;
import digit.digitapp.digitService.DeviceData;
import no.nordicsemi.android.ble.BleManagerCallbacks;
import no.nordicsemi.android.ble.ConnectRequest;
import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanFilter;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DeviceSynchronizationManager {
    private final String deviceId;
    private  final Context applicationContext;
    private final DigitBleServiceManager digitBleServiceManager;
    private final BluetoothLeScannerCompat scanner;
    private final SyncCallback callback;
    private DigitServiceManager digitServiceManager;
    private android.os.Handler mHandler;
    private boolean scanning;
    private  Object scanningLock = new Object();
    private int writeRetryCount;

    public DeviceSynchronizationManager(String deviceId, SyncCallback callback, Context applicationContext) {
        this.deviceId = deviceId;
        this.applicationContext = applicationContext;
        this.callback = callback;
        digitServiceManager = new DigitServiceManager(applicationContext);
        digitBleServiceManager = new DigitBleServiceManager(applicationContext);
        mHandler = new android.os.Handler(Looper.getMainLooper());
        scanner = BluetoothLeScannerCompat.getScanner();
        digitBleServiceManager.setGattCallbacks(new BleManagerCallbacks() {
            @Override
            public void onDeviceConnecting(@androidx.annotation.NonNull BluetoothDevice device) {

            }

            @Override
            public void onDeviceConnected(@androidx.annotation.NonNull BluetoothDevice device) {

            }

            @Override
            public void onDeviceDisconnecting(@androidx.annotation.NonNull BluetoothDevice device) {

            }

            @Override
            public void onDeviceDisconnected(@androidx.annotation.NonNull BluetoothDevice device) {

            }

            @Override
            public void onLinkLossOccurred(@androidx.annotation.NonNull BluetoothDevice device) {

            }

            @Override
            public void onServicesDiscovered(@androidx.annotation.NonNull BluetoothDevice device, boolean optionalServicesFound) {

            }

            @Override
            public void onDeviceReady(@androidx.annotation.NonNull BluetoothDevice device) {
                digitServiceManager.getDeviceData(deviceId, new Callback<DeviceData>() {
                    @Override
                    public void onResponse(Call<DeviceData> call, Response<DeviceData> response) {
                        writeCts(response.body());
                    }
                    @Override
                    public void onFailure(Call<DeviceData> call, Throwable t) {
                        digitServiceManager.log("Could not load device data", 3, callback::failed);
                    }
                });
            }

            @Override
            public void onBondingRequired(@androidx.annotation.NonNull BluetoothDevice device) {

            }

            @Override
            public void onBonded(@androidx.annotation.NonNull BluetoothDevice device) {

            }

            @Override
            public void onBondingFailed(@androidx.annotation.NonNull BluetoothDevice device) {

            }

            @Override
            public void onError(@androidx.annotation.NonNull BluetoothDevice device, @androidx.annotation.NonNull String message, int errorCode) {

            }

            @Override
            public void onDeviceNotSupported(@androidx.annotation.NonNull BluetoothDevice device) {

            }
        });
    }

    public void performSynchronization() {
        writeRetryCount = 0;
        final BluetoothManager bluetoothManager =
                (BluetoothManager) applicationContext.getSystemService(Context.BLUETOOTH_SERVICE);
        final BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        if (!bluetoothAdapter.isEnabled()) {
            digitServiceManager.log("Bluetooth not enabled", 3, callback::failed);
            return;
        }
        final BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceId);
        if (device == null) {
            digitServiceManager.log("Device not bonded", 3, callback::failed);
            return;
        }
        connect(device)
                .fail((dev,status) -> {

                    ScanSettings settings = new ScanSettings.Builder()
                            .setLegacy(false)
                            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                            .setReportDelay(5000)
                            .build();
                    List<ScanFilter> filters = new ArrayList<>();
                    //filters.add(new ScanFilter.Builder().setDeviceAddress(deviceId).build()); TODO fix filter
                    ScanCallback cb = new ScanCb();
                    scanning = true;
                    scanner.startScan(filters, settings, cb);
                    mHandler.postDelayed(() -> {
                        synchronized (scanningLock) {
                            if (scanning) {
                                scanner.stopScan(cb);
                                digitServiceManager.log("Scan unsuccessful", 3, callback::failed);
                            }
                        }
                    }, 3 * 60000);
                }).enqueue();
    }

    private class ScanCb extends ScanCallback {
        @Override
        public void onBatchScanResults(@androidx.annotation.NonNull List<ScanResult> results) {
            Optional<ScanResult> result = results.stream().filter(s -> s.getDevice().getAddress().equals(deviceId))
                    .findAny();
            if (result.isPresent()) {
                synchronized (scanningLock) {
                    scanning = false;
                    scanner.stopScan(this);
                }
                connect(result.get().getDevice())
                        .fail((dev,status) -> digitServiceManager.log("Connect to device after scan failed: " + status, 3, callback::failed)).enqueue();
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            digitServiceManager.log("Scanning failed " + errorCode, 3, callback::failed);
        }
    }

    private ConnectRequest connect(BluetoothDevice device) {
        return digitBleServiceManager.connect(device)
                .retry(3,5000)
                .useAutoConnect(false);
    }

    private void writeCts(final  DeviceData deviceData) {
        digitBleServiceManager.writeCts(Calendar.getInstance())
                .fail((d, status) -> {
                    if (++writeRetryCount > 3) {
                        logWriteFailureAndExit(status, "cts");
                    } else {
                        writeCts(deviceData);
                    }
                })
                .done(d -> writeEvent(deviceData))
                .enqueue();
    }

    private void logWriteFailureAndExit(final int status, final String action) {
        digitBleServiceManager.close();
        digitServiceManager.log("ble:" +action+ " write error; status " + status, 3, callback::failed);
    }

    private void writeEvent(final  DeviceData deviceData) {
        digitBleServiceManager.writeEvent(deviceData)
                .fail((d, status) -> logWriteFailureAndExit(status, "event"))
                .done(d -> writeDirections(deviceData)).enqueue();
    }

    private void writeDirections(final  DeviceData deviceData) {
        digitBleServiceManager.writeDirections(deviceData)
                .fail((d, status) -> logWriteFailureAndExit(status, "directions" ))
                .done(d -> writeLeg(deviceData,0)).enqueue();
    }

    private void writeLeg(final  DeviceData deviceData, int legIndex) {
        if (null != deviceData.getDirections() && null != deviceData.getDirections().getLegs()
                && deviceData.getDirections().getLegs().size() > legIndex) {
            digitBleServiceManager.writeLeg(deviceData, legIndex)
                    .fail((d, status) -> logWriteFailureAndExit(status, "leg-"+legIndex ))
                    .done(d -> writeLeg(deviceData,legIndex+1))
                    .enqueue();
        }
        else {
            digitBleServiceManager.close();
            setSynced();
        }
    }

    private  void setSynced() {
        digitServiceManager.setSynched("deviceSync." + deviceId, new Callback<ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                callback.done();
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                callback.failed();
            }
        });
    }
}