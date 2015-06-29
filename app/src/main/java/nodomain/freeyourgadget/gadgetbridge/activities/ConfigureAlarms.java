package nodomain.freeyourgadget.gadgetbridge.activities;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import nodomain.freeyourgadget.gadgetbridge.BluetoothCommunicationService;
import nodomain.freeyourgadget.gadgetbridge.GBAlarm;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.adapter.GBAlarmListAdapter;

import static nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst.PREF_MIBAND_ALARMS;


public class ConfigureAlarms extends ListActivity {

    private GBAlarmListAdapter mGBAlarmListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_configure_alarms);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        mGBAlarmListAdapter = new GBAlarmListAdapter(this, GBAlarm.readAlarmsFromPreferences());

        setListAdapter(mGBAlarmListAdapter);

    }

    @Override
    protected void onResume() {
        super.onResume();

        mGBAlarmListAdapter.setAlarmList(GBAlarm.readAlarmsFromPreferences());
        mGBAlarmListAdapter.notifyDataSetChanged();

        sendAlarmsToDevice();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // back button
                sendAlarmsToDevice();
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void configureAlarm(GBAlarm alarm) {
        Intent startIntent;
        startIntent = new Intent(getApplicationContext(), AlarmDetails.class);
        startIntent.putExtra("alarm", alarm);
        startActivity(startIntent);
    }

    private void sendAlarmsToDevice() {
        Intent startIntent = new Intent(ConfigureAlarms.this, BluetoothCommunicationService.class);
        startIntent.putParcelableArrayListExtra("alarms", mGBAlarmListAdapter.getAlarmList());
        startIntent.setAction(BluetoothCommunicationService.ACTION_SET_ALARMS);
        startService(startIntent);
    }
}
