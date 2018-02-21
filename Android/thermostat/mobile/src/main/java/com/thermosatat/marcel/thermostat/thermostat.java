package com.thermosatat.marcel.thermostat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import android.bluetooth.BluetoothGattCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;



public class thermostat extends AppCompatActivity {


    public static String SERVICE_STRING             = "6E400001-B5A3-F393-E0A9-E50E24DCCA9E";
    public static UUID SERVICE_UUID                 = UUID.fromString(SERVICE_STRING);

//    public static String CHARACTERISTIC_ECHO_STRING = "6E400002-B5A3-F393-E0A9-E50E24DCCA9E";
//    public static UUID CHARACTERISTIC_UUID_TX       = UUID.fromString(CHARACTERISTIC_ECHO_STRING);

//    public static String CHARACTERISTIC_TIME_STRING = "6E400003-B5A3-F393-E0A9-E50E24DCCA9E";
//    public static UUID CHARACTERISTIC_UUID_RX       = UUID.fromString(CHARACTERISTIC_TIME_STRING);

    private String BLUETOOTH_MAC = "30:AE:A4:20:2C:9A";
    private static final long SCAN_PERIOD = 10000;      // Stops scanning after 10 seconds.
    private BluetoothAdapter mBluetoothAdapter;         // Bluetooth adapter object
    private static final String TAG = "Scherer";        // debug tag
    private boolean mScanning = false;                  // flag is scanning is active
    private Map<String, BluetoothDevice> mScanResults;  // map of found bluetooth device
    private BluetoothLeScanner mBluetoothLeScanner;     // bluetooth low energy scanner object
    private Handler mHandler;                           // handler for stop scanning after time delay
    private Button button_start;                        // button object to start scan
    private ScanCallback mScanCallback;
    private boolean mConnected;                         // flag if device connected
    private BluetoothGatt mGatt;



    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_FINE_LOCATION = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thermostat);

        button_start = (Button)findViewById(R.id.button_start);

        // create a bluetooth adapter object for work with the bluetooth device
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        /* action button "now" */
        button_start.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if(mScanning == true){
                    stopScan();
                }
                else if(mConnected == true){
                    disconnectGattServer();
                }
                else {
                    startScan();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // check if bluetooth is available on the device
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            finish();
        }
    }

    // function for start scanning
    private void startScan() {
        // if no permission or scanning is active
        if (!hasPermissions() || mScanning) {
            return;
        }

        button_start.setText("Stop Scan");

        mScanResults = new HashMap<>();
        mScanCallback = new BtleScanCallback();
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        List<ScanFilter> filters = new ArrayList<>();               // create a filter list for bluetooth Service UUIDs
        ScanFilter scanFilter = new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(SERVICE_UUID))
                .build();
        //filters.add(scanFilter);
        ScanSettings settings = new ScanSettings.Builder()          // create bluetooth scan settings
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .build();

        mBluetoothLeScanner.startScan(filters, settings, mScanCallback);

        mHandler = new Handler();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                stopScan();
            }
        }, SCAN_PERIOD);

        mScanning = true;
        Log.d(TAG, "start scanning ...");
    }

    /* function to stop BLE device scann */
    private void stopScan() {
        button_start.setText("Start Scan");
        if (mScanning && mBluetoothAdapter != null && mBluetoothAdapter.isEnabled() && mBluetoothLeScanner != null) {
            mBluetoothLeScanner.stopScan(mScanCallback);
            scanComplete();
        }
        Log.d(TAG, "stop scanning ...");
        mScanning = false;
        mHandler = null;
    }

    /* function where list all find BLE devices in logfile */
    private void scanComplete() {
        if (mScanResults.isEmpty()) {
            Log.d(TAG, "list empty ...");
            return;
        }
        Log.d(TAG, "call scanComplete");
        for (String deviceAddress : mScanResults.keySet()) {
            if (deviceAddress.equals(BLUETOOTH_MAC)){
                BluetoothDevice device = mScanResults.get(deviceAddress);
                Log.d(TAG, "device for conection: " + deviceAddress);
                connectDevice(device);
            }
        }
    }

    // the bluetooth scan callback
    private class BtleScanCallback extends ScanCallback {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            addScanResult(result);
        }
        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult result : results) {
                addScanResult(result);
            }
        }
        @Override
        public void onScanFailed(int errorCode) {
            Log.e(TAG, "BLE Scan Failed with code " + errorCode);
        }
        private void addScanResult(ScanResult result) {
            BluetoothDevice device = result.getDevice();
            String deviceAddress = device.getAddress();
            mScanResults.put(deviceAddress, device);
        }
    };

    private void connectDevice(BluetoothDevice device) {
        Log.d(TAG,"connect to device: " + device.toString());
        GattClientCallback gattClientCallback = new GattClientCallback();
        mGatt = device.connectGatt(this, false, gattClientCallback);
    }

    public void logError(String msg) {
        Log.d(TAG,"Error: " + msg);
    }

    public void setConnected(boolean connected) {
        button_start.setText("Disconnect");
        mConnected = connected;
    }

    public void disconnectGattServer() {
        Log.d(TAG,"Closing Gatt connection");
        mConnected = false;
        if (mGatt != null) {
            mGatt.disconnect();
            mGatt.close();
        }
        button_start.setText("start scan");
    }

    // check if user has permisson
    private boolean hasPermissions() {
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            requestBluetoothEnable();
            return false;
        } else if (!hasLocationPermissions()) {
            requestLocationPermission();
            return false;
        }
        return true;
    }

    // request to start bluetooth
    private void requestBluetoothEnable() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        Log.d(TAG, "Requested user enables Bluetooth. Try starting the scan again.");
    }

    // check is user has location permission
    private boolean hasLocationPermissions() {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    // request for location permission
    private void requestLocationPermission() {
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_FINE_LOCATION);
    }

    private class GattClientCallback extends BluetoothGattCallback {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.d(TAG,"onConnectionStateChange newState: " + newState);

            if (status == BluetoothGatt.GATT_FAILURE) {
                Log.d(TAG,"Connection Gatt failure status " + status);
                disconnectGattServer();
                return;
            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                // handle anything not SUCCESS as failure
                logError("Connection not GATT sucess status " + status);
                disconnectGattServer();
                return;
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG,"Connected to device " + gatt.getDevice().getAddress());
                setConnected(true);
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                logError("Disconnected from device");
                disconnectGattServer();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Device service discovery unsuccessful, status " + status);
                return;
            }

            List<BluetoothGattCharacteristic> matchingCharacteristics = BluetoothUtils.findCharacteristics(gatt);
            if (matchingCharacteristics.isEmpty()) {
                logError("Unable to find characteristics.");
                return;
            }
        }
    }
}
