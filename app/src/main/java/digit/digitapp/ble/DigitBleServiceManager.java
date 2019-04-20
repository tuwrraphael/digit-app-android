package digit.digitapp.ble;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import androidx.annotation.NonNull;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import digit.digitapp.digitService.DeviceData;
import digit.digitapp.digitService.LegData;
import no.nordicsemi.android.ble.BleManager;
import no.nordicsemi.android.ble.BleManagerCallbacks;
import no.nordicsemi.android.ble.WriteRequest;
import no.nordicsemi.android.ble.data.Data;

public class DigitBleServiceManager extends BleManager<BleManagerCallbacks> {
    static final String digitServiceId = "00001523-1212-efde-1523-785fef13d123";
    static final String  ctsCharId = "00001805-1212-efde-1523-785fef13d123";
    static final String directionsCharId = "00001525-1212-efde-1523-785fef13d123";
    static final String eventCharId = "00001524-1212-efde-1523-785fef13d123";
    static final String legCharId = "00001526-1212-efde-1523-785fef13d123";

    private static final int subjectLength = 20;
    private static final int ctsLength = 7;
    private static final int directionLength = 20;
    private static final int stopLength = 20;
    private static final int lineLength = 6;


    static final UUID SERVICE_UUID = UUID.fromString(digitServiceId);
    private static final UUID CTS_CHAR_UUID = UUID.fromString(ctsCharId);
    private static final UUID DIRECTIONS_CHAR_UUID = UUID.fromString(directionsCharId);
    private static final UUID EVENT_CHAR_UUID = UUID.fromString(eventCharId);
    private static final UUID LEG_CHAR_UUID = UUID.fromString(legCharId);
    private BluetoothGattCharacteristic ctsChar, directionsChar, eventChar, legChar;

    public DigitBleServiceManager(final Context context) {
        super(context);
    }


    @NonNull
    @Override
    protected BleManagerGattCallback getGattCallback() {
        return mGattCallback;
    }


    private final BleManagerGattCallback mGattCallback = new BleManagerGattCallback() {

        @Override
        protected void initialize() {
            super.initialize();
        }

        @Override
        protected boolean isRequiredServiceSupported(@NonNull final BluetoothGatt gatt) {
            final BluetoothGattService digitService = gatt.getService(SERVICE_UUID);
            if (digitService != null) {
                ctsChar = digitService.getCharacteristic(CTS_CHAR_UUID);
                directionsChar = digitService.getCharacteristic(DIRECTIONS_CHAR_UUID);
                eventChar = digitService.getCharacteristic(EVENT_CHAR_UUID);
                legChar = digitService.getCharacteristic(LEG_CHAR_UUID);
            }
            return ctsChar != null && directionsChar != null && eventChar != null && legChar != null;
        }

        @Override
        protected boolean isOptionalServiceSupported(@NonNull final BluetoothGatt gatt) {
            return true;
        }

        @Override
        protected void onDeviceDisconnected() {
            ctsChar = null;
            directionsChar = null;
            eventChar = null;
            legChar = null;
        }

        @Override
        protected void onDeviceReady() {
            super.onDeviceReady();
        }
    };

    private  byte[] FormatCts(Calendar calendar) {
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

    public WriteRequest writeCts(Calendar calendar) {
        return writeCharacteristic(ctsChar, FormatCts(calendar));
    }

    public WriteRequest writeEvent(DeviceData deviceData) {
        if (null == deviceData.getEvent()) {
           return writeCharacteristic(eventChar,new byte[]{0});
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
            return writeCharacteristic(eventChar,data);
        }
    }

    public WriteRequest writeDirections(DeviceData deviceData) {
        if (null == deviceData.getDirections()) {
            return writeCharacteristic(directionsChar,new byte[]{0});
        } else {
            byte[] data = new byte[2 * ctsLength + 1];
            data[0] = (byte)deviceData.getDirections().getLegs().size();
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(deviceData.getDirections().getDepartureTime());
            System.arraycopy(FormatCts(calendar),0,data, 1,ctsLength);
            calendar.setTime(deviceData.getDirections().getArrivalTime());
            System.arraycopy(FormatCts(calendar),0,data, 1 + ctsLength,ctsLength);
            return writeCharacteristic(directionsChar,data);
        }
    }

    public WriteRequest writeLeg(DeviceData deviceData, int legIndex) {
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
        return writeCharacteristic(legChar,data);
    }

    public  void WriteDeviceData (DeviceData deviceData) {
        writeCts(Calendar.getInstance());
    }
}