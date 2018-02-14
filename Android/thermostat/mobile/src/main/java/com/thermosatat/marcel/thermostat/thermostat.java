package com.thermosatat.marcel.thermostat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class thermostat extends AppCompatActivity {

    private static final long SCAN_PERIOD = 10000;  // Stops scanning after 10 seconds.
    private BluetoothAdapter mBluetoothAdapter;     // Bluetooth adapter object
    private static final String TAG = "Scherer";    // debug tag

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_FINE_LOCATION = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thermostat);

        // create a bluetooth adapter object for work with the bluetooth device
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

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
        List<ScanFilter> filters = new ArrayList<>();               // create a filter list for bluetooth Service UUIDs
        ScanSettings settings = new ScanSettings.Builder()          // create bluetooth scan settings
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .build();

        mScanResults = new HashMap<>();
        mScanCallback = new BtleScanCallback(mScanResults);

        mScanCallback = new BtleScanCallback(mScanResults);
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        mBluetoothLeScanner.startScan(filters, settings, mScanCallback);
        mScanning = true;
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
}
