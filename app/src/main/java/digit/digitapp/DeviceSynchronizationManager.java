package digit.digitapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.support.annotation.NonNull;

import java.util.Calendar;

import digit.digitapp.ble.DigitBleServiceManager;
import no.nordicsemi.android.ble.BleManagerCallbacks;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DeviceSynchronizationManager {
    private final String deviceId;
    private  final Context applicationContext;

    public DeviceSynchronizationManager(String deviceId, Context applicationContext) {
        this.deviceId = deviceId;
        this.applicationContext = applicationContext;
    }

    public void performSynchronization(SyncCallback callback) {
        DigitServiceManager digitServiceManager = new DigitServiceManager(applicationContext);
        final BluetoothManager bluetoothManager =
                (BluetoothManager) applicationContext.getSystemService(Context.BLUETOOTH_SERVICE);
        final BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        if (!bluetoothAdapter.isEnabled()) {
            digitServiceManager.log("Bluetooth not enabled", 3, callback::failed);
            return;
        }
        final BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceId);
        if (device == null) {
            digitServiceManager.log("Device not found", 3, callback::failed);
            return;
        }
        DigitBleServiceManager digitBleServiceManager = new DigitBleServiceManager(applicationContext);
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
                digitBleServiceManager.writeCts(Calendar.getInstance())
                        .done(d -> digitServiceManager.setSynched("deviceSync." + deviceId, new Callback<ResponseBody>() {
                            @Override
                            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                                callback.done();
                            }

                            @Override
                            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                                callback.failed();
                            }
                        }))
                        .fail((d,s) -> callback.failed()).enqueue();
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
        digitBleServiceManager.connect(device)
            .retry(3)
            .timeout(30000)
            .fail((dev,status) -> digitServiceManager.log("Connect to device failed: " + status, 3, callback::failed)).enqueue();
    }
}