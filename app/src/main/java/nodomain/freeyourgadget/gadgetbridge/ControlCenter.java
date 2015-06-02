package nodomain.freeyourgadget.gadgetbridge;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.adapter.GBDeviceAdapter;
import nodomain.freeyourgadget.gadgetbridge.discovery.DiscoveryActivity;
import nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst;

public class ControlCenter extends Activity {

    private static final Logger LOG = LoggerFactory.getLogger(ControlCenter.class);

    public static final String ACTION_QUIT
            = "nodomain.freeyourgadget.gadgetbride.controlcenter.action.quit";

    public static final String ACTION_REFRESH_DEVICELIST
            = "nodomain.freeyourgadget.gadgetbride.controlcenter.action.set_version";

    TextView hintTextView;
    ListView deviceListView;
    GBDeviceAdapter mGBDeviceAdapter;
    private GBDevice selectedDevice = null;

    final List<GBDevice> deviceList = new ArrayList<>();

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case ACTION_QUIT:
                    finish();
                    break;
                case ACTION_REFRESH_DEVICELIST:
                case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
                    refreshPairedDevices();
                    break;
                case GBDevice.ACTION_DEVICE_CHANGED:
                    GBDevice dev = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE);
                    if (dev.getAddress() != null) {
                        int index = deviceList.indexOf(dev); // search by address
                        if (index >= 0) {
                            deviceList.set(index, dev);
                        } else {
                            deviceList.add(dev);
                        }
                    }
                    refreshPairedDevices();

                    if (dev.isConnected() && dev.getFirmwareVersion() == null && !dev.isInitializing()) {
                        LOG.info("device connected, requesting more info");
                        requestDeviceInfo();
                    }
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controlcenter);
        hintTextView = (TextView) findViewById(R.id.hintTextView);
        deviceListView = (ListView) findViewById(R.id.deviceListView);
        mGBDeviceAdapter = new GBDeviceAdapter(this, deviceList);
        deviceListView.setAdapter(this.mGBDeviceAdapter);
        deviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView parent, View v, int position, long id) {
                if (deviceList.get(position).isConnected()) {
                    Intent startIntent = new Intent(ControlCenter.this, AppManagerActivity.class);
                    startActivity(startIntent);
                } else {
                    Intent startIntent = new Intent(ControlCenter.this, BluetoothCommunicationService.class);
                    startIntent.setAction(BluetoothCommunicationService.ACTION_CONNECT);
                    startIntent.putExtra(BluetoothCommunicationService.EXTRA_DEVICE_ADDRESS, deviceList.get(position).getAddress());
                    startService(startIntent);
                }
            }
        });

        registerForContextMenu(deviceListView);

        IntentFilter filterLocal = new IntentFilter();
        filterLocal.addAction(ACTION_QUIT);
        filterLocal.addAction(ACTION_REFRESH_DEVICELIST);
        filterLocal.addAction(GBDevice.ACTION_DEVICE_CHANGED);
        filterLocal.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, filterLocal);

        registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));

        refreshPairedDevices();
        /*
         * Ask for permission to intercept notifications on first run.
         */
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPrefs.getBoolean("firstrun", true)) {
            sharedPrefs.edit().putBoolean("firstrun", false).apply();
            Intent enableIntent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
            startActivity(enableIntent);
        }
        Intent startIntent = new Intent(this, BluetoothCommunicationService.class);
        startIntent.setAction(BluetoothCommunicationService.ACTION_START);
        startService(startIntent);


        if (GB.isBluetoothEnabled() && deviceList.isEmpty()) {
            // start discovery when no devices are present
            startActivity(new Intent(this, DiscoveryActivity.class));
        } else {
            requestDeviceInfo();
        }
    }

    /**
     * Requests information from the {@link BluetoothCommunicationService} about the connection state,
     * firmware info, etc.
     * <p/>
     * Note that this method may cause an implicit device connection (for auto-connectable devices).
     */
    private void requestDeviceInfo() {
        Intent versionInfoIntent = new Intent(ControlCenter.this, BluetoothCommunicationService.class);
        versionInfoIntent.setAction(BluetoothCommunicationService.ACTION_REQUEST_VERSIONINFO);
        startService(versionInfoIntent);
    }


    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        getMenuInflater().inflate(
                R.menu.controlcenter_context, menu);
        AdapterView.AdapterContextMenuInfo acmi = (AdapterView.AdapterContextMenuInfo) menuInfo;
        selectedDevice = deviceList.get(acmi.position);
        menu.setHeaderTitle(selectedDevice.getName());
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.controlcenter_start_sleepmonitor:
                if (selectedDevice != null) {
                    Intent startIntent = new Intent(ControlCenter.this, SleepMonitorActivity.class);
                    startIntent.putExtra("device", selectedDevice);
                    startActivity(startIntent);
                }
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
            case R.id.action_debug:
                Intent debugIntent = new Intent(this, DebugActivity.class);
                startActivity(debugIntent);
                return true;
            case R.id.action_quit:
                Intent stopIntent = new Intent(this, BluetoothCommunicationService.class);
                stopService(stopIntent);

                Intent quitIntent = new Intent(ControlCenter.ACTION_QUIT);
                LocalBroadcastManager.getInstance(this).sendBroadcast(quitIntent);
                return true;
            case R.id.action_discover:
                Intent discoverIntent = new Intent(this, DiscoveryActivity.class);
                startActivity(discoverIntent);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    private void refreshPairedDevices() {
        boolean connected = false;
        List<GBDevice> availableDevices = new ArrayList<>();
        for (GBDevice device : deviceList) {
            if (device.isConnected() || device.isConnecting()) {
                connected = true;
                availableDevices.add(device);
            }
        }

        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();

        if (btAdapter == null) {
            Toast.makeText(this, R.string.bluetooth_is_not_supported_, Toast.LENGTH_SHORT).show();
        } else if (!btAdapter.isEnabled()) {
            Toast.makeText(this, R.string.bluetooth_is_disabled_, Toast.LENGTH_SHORT).show();
        } else {
            Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
            for (BluetoothDevice pairedDevice : pairedDevices) {
                DeviceType deviceDeviceType;
                if (pairedDevice.getName().indexOf("Pebble") == 0) {
                    deviceDeviceType = DeviceType.PEBBLE;
                } else if (pairedDevice.getName().equals("MI")) {
                    deviceDeviceType = DeviceType.MIBAND;
                } else {
                    continue;
                }
                GBDevice device = new GBDevice(pairedDevice.getAddress(), pairedDevice.getName(), deviceDeviceType);
                if (!availableDevices.contains(device)) {
                    availableDevices.add(device);
                }
            }

            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
            String miAddr = sharedPrefs.getString(MiBandConst.PREF_MIBAND_ADDRESS, null);
            if (miAddr != null && miAddr.length() > 0) {
                GBDevice miDevice = new GBDevice(miAddr, "MI", DeviceType.MIBAND);
                if (!availableDevices.contains(miDevice)) {
                    availableDevices.add(miDevice);
                }
            }
            deviceList.retainAll(availableDevices);
            for (GBDevice dev : availableDevices) {
                if (!deviceList.contains(dev)) {
                    deviceList.add(dev);
                }
            }

            if (connected) {
                hintTextView.setText(R.string.tap_connected_device_for_app_mananger);
            } else if (!deviceList.isEmpty()) {
                hintTextView.setText(R.string.tap_a_device_to_connect);
            }
        }
        mGBDeviceAdapter.notifyDataSetChanged();
    }
}
