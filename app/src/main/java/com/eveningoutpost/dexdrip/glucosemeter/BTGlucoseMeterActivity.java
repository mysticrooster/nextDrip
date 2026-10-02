package com.eveningoutpost.dexdrip.glucosemeter;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;

import android.widget.Toast;

import androidx.databinding.ObservableField;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.eveningoutpost.dexdrip.BaseAppCompatActivity;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.services.BluetoothGlucoseMeter;
import com.eveningoutpost.dexdrip.ui.secondary.BTGlucoseMeterScreen;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utils.LocationHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.eveningoutpost.dexdrip.services.BluetoothGlucoseMeter.start_forget;

/**
 * Created by jamorham on 09/12/2016.
 * Scan, connect and manage pairing state of Bluetooth Glucose meters
 * interacts with BluetoothGlucoseMeter service
 *
 * Track V (Data & admin, Compose): the activity keeps the Bluetooth state machine, the
 * {@link LocalBroadcastManager} receiver and the service calls; the screen renders status and the
 * scanned device list and calls back here.
 */

@TargetApi(18)
public class BTGlucoseMeterActivity extends BaseAppCompatActivity {

    private static final String TAG = BTGlucoseMeterActivity.class.getSimpleName();
    private final ArrayList<MyBluetoothDevice> mLeDevices = new ArrayList<>();

    private BluetoothAdapter bluetooth_adapter;

    private BluetoothManager bluetooth_manager;
    private BroadcastReceiver serviceDataReceiver;

    private boolean first_run = true;

    /** Compose bridge: bumped whenever the visible state changes. */
    public final ObservableField<Integer> tick = new ObservableField<>(0);
    public final ObservableField<String> status = new ObservableField<>("Starting up");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            JoH.static_toast_long("The android version of this device is not compatible with Bluetooth Low Energy");
            finish();
            return;
        }

        bluetooth_manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        bluetooth_adapter = bluetooth_manager.getAdapter();

        if (bluetooth_adapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // get bluetooth ready
        check_and_enable_bluetooth();
        LocationHelper.requestLocationForBluetooth(this);

        serviceDataReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                final String action = intent.getAction();
                UserError.Log.d(TAG, "Got receive:" + action + " :: " + intent.getStringExtra("data"));
                if (action == null) return;
                switch (action) {
                    case BluetoothGlucoseMeter.ACTION_BLUETOOTH_GLUCOSE_METER_SERVICE_UPDATE:
                        status.set(intent.getStringExtra("data"));
                        notifyChanged();
                        break;
                    case BluetoothGlucoseMeter.ACTION_BLUETOOTH_GLUCOSE_METER_NEW_SCAN_DEVICE:
                        addDevice(intent.getStringExtra("data"));
                        break;
                }
            }
        };

        BTGlucoseMeterScreen.installBTGlucoseMeter(this);
    }

    /** Nudges the Compose screen to recompute its state. */
    public void notifyChanged() {
        final Integer current = tick.get();
        tick.set((current == null ? 0 : current) + 1);
    }

    @Override
    protected void onResume() {
        super.onResume();
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothGlucoseMeter.ACTION_BLUETOOTH_GLUCOSE_METER_NEW_SCAN_DEVICE);
        intentFilter.addAction(BluetoothGlucoseMeter.ACTION_BLUETOOTH_GLUCOSE_METER_SERVICE_UPDATE);
        LocalBroadcastManager.getInstance(this).registerReceiver(serviceDataReceiver, intentFilter);
        if (first_run) {
            BluetoothGlucoseMeter.start_service(null);
            first_run = false;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (serviceDataReceiver != null) {
            try {
                LocalBroadcastManager.getInstance(this).unregisterReceiver(serviceDataReceiver);
            } catch (IllegalArgumentException e) {
                UserError.Log.e(TAG, "broadcast receiver not registered", e);
            }
        }
    }

    private void check_and_enable_bluetooth() {
        if (!bluetooth_manager.getAdapter().isEnabled()) {
            if (Pref.getBoolean("automatically_turn_bluetooth_on", true)) {
                JoH.setBluetoothEnabled(getApplicationContext(), true);
                Toast.makeText(this, "Trying to turn Bluetooth on", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Please turn Bluetooth on!", Toast.LENGTH_LONG).show();
            }
        }
    }

    /** Scan button action (previously the action-bar menu item). */
    public void startScan() {
        check_and_enable_bluetooth();
        if (JoH.ratelimit("bluetooth-scan-button", 4)) {
            UserError.Log.d(TAG, "Starting Bluetooth Glucose Meter Service");
            mLeDevices.clear();
            notifyChanged();
            BluetoothGlucoseMeter.start_service(null);
        } else {
            UserError.Log.d(TAG, "Rate limited scan button");
        }
    }

    /** Item tap: connect to the scanned device. */
    public void onDeviceClick(MyBluetoothDevice device) {
        if (device != null) {
            if (JoH.ratelimit("bt-meter-item-clicked", 7)) {
                UserError.Log.d(TAG, "Item Clicked: " + device.address);
                BluetoothGlucoseMeter.start_service(device.address);
            }
        } else {
            UserError.Log.wtf(TAG, "Null pointer on list item click");
        }
    }

    /** Long-press "Disconnect" action. */
    public void disconnectDevice(MyBluetoothDevice device) {
        if (device == null) return;
        if (Pref.getStringDefaultBlank("selected_bluetooth_meter_address").equals(device.address)) {
            Pref.setString("selected_bluetooth_meter_address", "");
            notifyChanged();
            JoH.static_toast_long("Disconnected!");
            BluetoothGlucoseMeter.start_service(null);
        } else {
            JoH.static_toast_short("Not connected to this device!");
        }
    }

    /** Long-press "Forget Pair" action. */
    public void forgetDevice(MyBluetoothDevice device) {
        if (device != null) {
            start_forget(device.address);
        }
    }

    public List<MyBluetoothDevice> getDeviceSnapshot() {
        return Collections.unmodifiableList(new ArrayList<>(mLeDevices));
    }

    public String getSelectedAddress() {
        return Pref.getString("selected_bluetooth_meter_address", "");
    }

    private synchronized void addDevice(String data) {
        if (data == null) return;
        final MyBluetoothDevice device = new MyBluetoothDevice(data);
        for (MyBluetoothDevice existing : mLeDevices) {
            if (existing.address.equals(device.address)) {
                // update if pairing state changes
                existing.pairstate = device.pairstate;
                existing.name = device.name;
                notifyChanged();
                return;
            }
        }
        mLeDevices.add(device);
        notifyChanged();
        UserError.Log.d(TAG, "New list device added - data set changed");
    }

    public static class MyBluetoothDevice {
        public final String address;
        public String name;
        public int pairstate;

        public MyBluetoothDevice(String data) {
            // parse fixed format data string
            String[] stra = data.split("\\^");
            this.address = stra[0];
            this.pairstate = Integer.parseInt(stra[1]);
            if (stra.length > 2) {
                this.name = stra[2];
            } else {
                this.name = ""; // unnamed
            }
        }

        public boolean isBonded() {
            return pairstate == BluetoothDevice.BOND_BONDED;
        }
    }
}
