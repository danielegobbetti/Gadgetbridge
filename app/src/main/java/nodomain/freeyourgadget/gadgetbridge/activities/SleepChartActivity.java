package nodomain.freeyourgadget.gadgetbridge.activities;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.content.LocalBroadcastManager;
import android.view.MenuItem;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.BarLineChartBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.ControlCenter;
import nodomain.freeyourgadget.gadgetbridge.GBActivitySample;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.R;


public class SleepChartActivity extends Activity {
    private static final float Y_VALUE_DEEP_SLEEP = 0.01f;
    private static final float Y_VALUE_LIGHT_SLEEP = 0.016f;

    private static final class ActivityKind {
        public final byte type;
        public final String label;
        public final Integer color;

        public ActivityKind(byte type, String label, Integer color) {
            this.type = type;
            this.label = label;
            this.color = color;
        }
    }

    public static final String ACTION_REFRESH
            = "nodomain.freeyourgadget.gadgetbride.chart.action.refresh";
    protected static final Logger LOG = LoggerFactory.getLogger(SleepChartActivity.class);

    private BarLineChartBase mChart;

    private int mSmartAlarmFrom = -1;
    private int mSmartAlarmTo = -1;
    private int mTimestampFrom = -1;
    private int mSmartAlarmGoneOff = -1;
    private GBDevice mGBDevice = null;

    private ActivityKind akActivity = new ActivityKind(GBActivitySample.TYPE_UNKNOWN, "Activity", Color.rgb(89, 178, 44));
    private ActivityKind akLightSleep = new ActivityKind(GBActivitySample.TYPE_LIGHT_SLEEP, "Light Sleep", Color.rgb(182, 191, 255));
    private ActivityKind akDeepSleep = new ActivityKind(GBActivitySample.TYPE_DEEP_SLEEP, "Deep Sleep", Color.rgb(76, 90, 255));

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ControlCenter.ACTION_QUIT)) {
                finish();
            } else if (action.equals(ACTION_REFRESH)) {
                // TODO: use LimitLines to visualize smart alarms?
                mSmartAlarmFrom = intent.getIntExtra("smartalarm_from", -1);
                mSmartAlarmTo = intent.getIntExtra("smartalarm_to", -1);
                mTimestampFrom = intent.getIntExtra("recording_base_timestamp", -1);
                mSmartAlarmGoneOff = intent.getIntExtra("alarm_gone_off", -1);
                refresh();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mGBDevice = extras.getParcelable("device");
        }

        getActionBar().setDisplayHomeAsUpEnabled(true);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ControlCenter.ACTION_QUIT);
        filter.addAction(ACTION_REFRESH);

        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, filter);

//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
//                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_sleepmonitor2);

        mChart = (BarLineChartBase) findViewById(R.id.sleepchart2);
        mChart.setBackgroundColor(Color.rgb(24, 22, 24));
        mChart.setDescriptionColor(Color.WHITE);

        // if enabled, the chart will always start at zero on the y-axis

        // disable value highlighting
        mChart.setHighlightEnabled(false);

        // enable touch gestures
        mChart.setTouchEnabled(true);

        // enable scaling and dragging
        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(true);

        // if disabled, scaling can be done on x- and y-axis separately
//        mChart.setPinchZoom(true);

        mChart.setDrawGridBackground(false);

//        tf = Typeface.createFromAsset(getAssets(), "OpenSans-Regular.ttf");

        XAxis x = mChart.getXAxis();
        x.setDrawLabels(true);
        x.setDrawGridLines(false);
//        x.setTypeface(tf);
        x.setEnabled(true);
        x.setTextColor(Color.WHITE);
        x.setDrawLimitLinesBehindData(true);

        YAxis y = mChart.getAxisLeft();
        y.setDrawGridLines(false);
//        y.setDrawLabels(false);
        // TODO: make fixed max value optional
        y.setAxisMaxValue(1f);
        y.setDrawTopYLabelEntry(false);
        y.setTextColor(Color.WHITE);

//        y.setTypeface(tf);
//        y.setLabelCount(5);
        y.setEnabled(true);

        YAxis yAxisRight = mChart.getAxisRight();
        yAxisRight.setDrawGridLines(false);
        yAxisRight.setEnabled(false);
        yAxisRight.setDrawLabels(false);
        yAxisRight.setDrawTopYLabelEntry(false);
        yAxisRight.setTextColor(Color.WHITE);

        refresh();

        mChart.getLegend().setTextColor(Color.WHITE);
//        mChart.getLegend().setEnabled(false);
//
//        mChart.animateXY(2000, 2000);

        // dont forget to refresh the drawing
        mChart.invalidate();
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    private byte getProvider(GBDevice device) {
        byte provider = -1;
        switch (device.getType()) {
            case MIBAND:
                provider = GBActivitySample.PROVIDER_MIBAND;
                break;
            case PEBBLE:
                provider = GBActivitySample.PROVIDER_PEBBLE_MORPHEUZ; // FIXME
                break;
        }
        return provider;
    }

    private ArrayList<GBActivitySample> getSamples(GBDevice device, int tsFrom, int tsTo) {
        if (tsFrom == -1) {
            long ts = System.currentTimeMillis();
            tsFrom = (int) ((ts / 1000) - (24 * 60 * 60) & 0xffffffff); // -24 hours
        }

        byte provider = getProvider(device);
        return GBApplication.getActivityDatabaseHandler().getGBActivitySamples(tsFrom, tsTo, provider);
    }

    private ArrayList<GBActivitySample> getTestSamples(GBDevice device, int tsFrom, int tsTo) {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2015, Calendar.JUNE, 10, 6, 40);
        // ignore provided date ranges
        tsTo = (int) ((cal.getTimeInMillis() / 1000) & 0xffffffff);
        tsFrom = tsTo - (24 * 60 * 60);

        byte provider = getProvider(device);
        return GBApplication.getActivityDatabaseHandler().getGBActivitySamples(tsFrom, tsTo, provider);
    }

    private void refresh() {
        if (mGBDevice == null) {
            return;
        }

//        ArrayList<GBActivitySample> samples = getTestSamples(mGBDevice, -1, -1);
        ArrayList<GBActivitySample> samples = getSamples(mGBDevice, -1, -1);

        Calendar cal = Calendar.getInstance();
        cal.clear();
        Date date;
        String dateStringFrom = "";
        String dateStringTo = "";

        LOG.info("number of samples:" + samples.size());
        if (samples.size() > 1) {
            float movement_divisor;
            boolean annotate = true;
            boolean use_steps_as_movement;
            switch (getProvider(mGBDevice)) {
                case GBActivitySample.PROVIDER_MIBAND:
                    movement_divisor = 256.0f;
                    use_steps_as_movement = true;
                    break;
                default: // Morpheuz
                    movement_divisor = 5000.0f;
                    use_steps_as_movement = false;
                    break;
            }

            byte last_type = GBActivitySample.TYPE_UNKNOWN;

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");
            SimpleDateFormat annotationDateFormat = new SimpleDateFormat("HH:mm");

            int numEntries = samples.size();
            List<String> xLabels = new ArrayList<>(numEntries);
//            List<BarEntry> deepSleepEntries = new ArrayList<>(numEntries / 4);
//            List<BarEntry> lightSleepEntries = new ArrayList<>(numEntries / 4);
            List<BarEntry> activityEntries = new ArrayList<>(numEntries);
            List<Integer> colors = new ArrayList<>(numEntries); // this is kinda inefficient...

            for (int i = 0; i < numEntries; i++) {
                GBActivitySample sample = samples.get(i);
                byte type = sample.getType();

                // determine start and end dates
                if (i == 0) {
                    cal.setTimeInMillis(sample.getTimestamp() * 1000L); // make sure it's converted to long
                    date = cal.getTime();
                    dateStringFrom = dateFormat.format(date);
                } else if (i == samples.size() - 1) {
                    cal.setTimeInMillis(sample.getTimestamp() * 1000L); // same here
                    date = cal.getTime();
                    dateStringTo = dateFormat.format(date);
                }

                short movement = sample.getIntensity();

                float value;
                if (type == GBActivitySample.TYPE_DEEP_SLEEP) {
//                    value = Y_VALUE_DEEP_SLEEP;
                    value = ((float) movement) / movement_divisor;
                    value += Y_VALUE_DEEP_SLEEP;
                    activityEntries.add(createEntry(value, i));
                    colors.add(akDeepSleep.color);
                } else {
                    if (type == GBActivitySample.TYPE_LIGHT_SLEEP) {
                        value = ((float) movement) / movement_divisor;
//                        value += Y_VALUE_LIGHT_SLEEP;
//                        value = Math.min(1.0f, Y_VALUE_LIGHT_SLEEP);
                        activityEntries.add(createEntry(value, i));
                        colors.add(akLightSleep.color);
                    } else {
                        byte steps = sample.getSteps();
                        if (use_steps_as_movement && steps != 0) {
                            // I'm not sure using steps for this is actually a good idea
                            movement = steps;
                        }
                        value = ((float) movement) / movement_divisor;
                        activityEntries.add(createEntry(value, i));
                        colors.add(akActivity.color);
                    }
                }

                String xLabel = "";
                if (annotate) {
                    cal.setTimeInMillis(sample.getTimestamp() * 1000L);
                    date = cal.getTime();
                    String dateString = annotationDateFormat.format(date);
                    xLabel = dateString;
//                    if (last_type != type) {
//                        if (isSleep(last_type) && !isSleep(type)) {
//                            // woken up
//                            LimitLine line = new LimitLine(i, dateString);
//                            line.enableDashedLine(8, 8, 0);
//                            line.setTextColor(Color.WHITE);
//                            line.setTextSize(15);
//                            mChart.getXAxis().addLimitLine(line);
//                        } else if (!isSleep(last_type) && isSleep(type)) {
//                            // fallen asleep
//                            LimitLine line = new LimitLine(i, dateString);
//                            line.enableDashedLine(8, 8, 0);
//                            line.setTextSize(15);
//                            line.setTextColor(Color.WHITE);
//                            mChart.getXAxis().addLimitLine(line);
//                        }
//                    }
                    last_type = type;
                }
                xLabels.add(xLabel);
            }

            mChart.getXAxis().setValues(xLabels);

//            BarDataSet deepSleepSet = createDeepSleepSet(deepSleepEntries, "Deep Sleep");
//            BarDataSet lightSleepSet = createLightSleepSet(lightSleepEntries, "Light Sleep");
            BarDataSet activitySet = createActivitySet(activityEntries, colors, "Activity");

            ArrayList<BarDataSet> dataSets = new ArrayList<>();
//            dataSets.add(deepSleepSet);
//            dataSets.add(lightSleepSet);
            dataSets.add(activitySet);

            // create a data object with the datasets
            BarData data = new BarData(xLabels, dataSets);
            data.setGroupSpace(0);

            mChart.setDescription(getString(R.string.sleep_activity_date_range, dateStringFrom, dateStringTo));
//            mChart.setDescriptionPosition(?, ?);
            // set data

            setupLegend(mChart);

            mChart.setData(data);

            mChart.animateX(1000, Easing.EasingOption.EaseInOutQuart);

//            textView.setText(dateStringFrom + " to " + dateStringTo);
        }
    }

    private boolean isSleep(byte type) {
        return type == GBActivitySample.TYPE_DEEP_SLEEP || type == GBActivitySample.TYPE_LIGHT_SLEEP;
    }

    private void setupLegend(BarLineChartBase chart) {
        List<Integer> legendColors = new ArrayList<>(3);
        List<String> legendLabels = new ArrayList<>(3);
        legendColors.add(akActivity.color);
        legendLabels.add(akActivity.label);
        legendColors.add(akLightSleep.color);
        legendLabels.add(akLightSleep.label);
        legendColors.add(akDeepSleep.color);
        legendLabels.add(akDeepSleep.label);
        chart.getLegend().setColors(legendColors);
        chart.getLegend().setLabels(legendLabels);
    }

    private BarEntry createEntry(float value, int index) {
        return new BarEntry(value, index);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private BarDataSet createActivitySet(List<BarEntry> values, List<Integer> colors, String label) {
        BarDataSet set1 = new BarDataSet(values, label);
        set1.setColors(colors);
//        set1.setDrawCubic(true);
//        set1.setCubicIntensity(0.2f);
//        //set1.setDrawFilled(true);
//        set1.setDrawCircles(false);
//        set1.setLineWidth(2f);
//        set1.setCircleSize(5f);
//        set1.setFillColor(ColorTemplate.getHoloBlue());
        set1.setDrawValues(false);
//        set1.setHighLightColor(Color.rgb(128, 0, 255));
//        set1.setColor(Color.rgb(89, 178, 44));
        set1.setValueTextColor(Color.WHITE);
        return set1;
    }

    private BarDataSet createDeepSleepSet(List<BarEntry> values, String label) {
        BarDataSet set1 = new BarDataSet(values, label);
//        set1.setDrawCubic(true);
//        set1.setCubicIntensity(0.2f);
//        //set1.setDrawFilled(true);
//        set1.setDrawCircles(false);
//        set1.setLineWidth(2f);
//        set1.setCircleSize(5f);
//        set1.setFillColor(ColorTemplate.getHoloBlue());
        set1.setDrawValues(false);
//        set1.setHighLightColor(Color.rgb(244, 117, 117));
//        set1.setColor(Color.rgb(76, 90, 255));
        set1.setValueTextColor(Color.WHITE);
        return set1;
    }

    private BarDataSet createLightSleepSet(List<BarEntry> values, String label) {
        BarDataSet set1 = new BarDataSet(values, label);

//        set1.setDrawCubic(true);
//        set1.setCubicIntensity(0.2f);
//        //set1.setDrawFilled(true);
//        set1.setDrawCircles(false);
//        set1.setLineWidth(2f);
//        set1.setCircleSize(5f);
//        set1.setFillColor(ColorTemplate.getHoloBlue());
        set1.setDrawValues(false);
//        set1.setHighLightColor(Color.rgb(244, 117, 117));
//        set1.setColor(Color.rgb(182, 191, 255));
        set1.setValueTextColor(Color.WHITE);
//        set1.setColor(Color.CYAN);
        return set1;
    }
}
