package com.eveningoutpost.dexdrip;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.g5model.FirmwareCapability;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.Calibration;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.Sensor;
import com.eveningoutpost.dexdrip.models.Treatments;
import com.eveningoutpost.dexdrip.services.Ob1G5CollectionService;
import com.eveningoutpost.dexdrip.tables.BgReadingTable;
import com.eveningoutpost.dexdrip.tables.CalibrationDataTable;
import com.eveningoutpost.dexdrip.utilitymodels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.utilitymodels.Experience;
import com.eveningoutpost.dexdrip.stats.StatsActivity;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.eveningoutpost.dexdrip.utils.Preferences;

import java.util.ArrayList;
import java.util.List;

import static com.eveningoutpost.dexdrip.services.Ob1G5CollectionService.getTransmitterID;
import static com.eveningoutpost.dexdrip.utils.DexCollectionType.getBestCollectorHardwareName;

/**
 * Created by Emma Black on 11/5/14.
 */
public class NavDrawerBuilder {
    private List<Calibration> last_two_calibrations = Calibration.latestValid(2);
    private final List<BgReading> last_two_bgReadings = BgReading.latestUnCalculated(2);
    private final List<BgReading> bGreadings_in_last_30_mins = BgReading.last30Minutes();
    private boolean is_active_sensor = Sensor.isActive();
    public final List<Intent> nav_drawer_intents = new ArrayList<>();
    public final List<String> nav_drawer_options = new ArrayList<>();
    public final List<Integer> nav_drawer_icons = new ArrayList<>();

    private static boolean use_note_search = false;

    public NavDrawerBuilder(final Context context) {
        final DexCollectionType collector = DexCollectionType.getDexCollectionType();

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean IUnderstand = prefs.getBoolean("I_understand", false);
        if (!IUnderstand) {
            this.nav_drawer_options.add(context.getString(R.string.settings));
            this.nav_drawer_intents.add(new Intent(context, Preferences.class));
            this.nav_drawer_icons.add(R.drawable.ic_wrench_grey600_48dp);
            return;
        }

        this.nav_drawer_options.add(context.getString(R.string.home_screen));
        this.nav_drawer_intents.add(new Intent(context, Home.class));
        this.nav_drawer_icons.add(R.drawable.ic_launcher);

        if ((is_active_sensor) && (last_two_calibrations != null) && (last_two_calibrations.size() > 0)) {
            this.nav_drawer_options.add(context.getString(R.string.calibration_graph));
            this.nav_drawer_intents.add(new Intent(context, CalibrationGraph.class));
            this.nav_drawer_icons.add(R.drawable.ic_chart_areaspline_grey600_48dp);
        }

        if (prefs.getBoolean("show_data_tables", false)) {
            this.nav_drawer_options.add(context.getString(R.string.bg_data_table));
            this.nav_drawer_intents.add(new Intent(context, BgReadingTable.class));
            this.nav_drawer_icons.add(R.drawable.ic_chart_areaspline_grey600_48dp);
            this.nav_drawer_options.add(context.getString(R.string.calibration_data_table));
            this.nav_drawer_intents.add(new Intent(context, CalibrationDataTable.class));
            this.nav_drawer_icons.add(R.drawable.ic_chart_areaspline_grey600_48dp);
        }

        if (DexCollectionType.isAlwaysNativeCal()) {
            this.nav_drawer_options.add(context.getString(R.string.add_calibration));
            this.nav_drawer_intents.add(new Intent(context, AddCalibration.class));
            this.nav_drawer_icons.add(R.drawable.ic_eyedropper_variant_grey600_24dp);
        } else if (!collector.canNotStartStopOrCal()) {
            // Only if the collector can start/stop sensor and submit calibrations (is not passive)

            if (is_active_sensor) {
                if (!CollectionServiceStarter.isBTShare(context)) {
                    if (last_two_bgReadings.size() > 1 || Ob1G5CollectionService.isG5WantingCalibration()) {
                        if ((last_two_calibrations.size() > 1) && !Ob1G5CollectionService.isG5WantingInitialCalibration()) { // After two successful initial calibrations
                            // TODO tighten this time limit
                            if (bGreadings_in_last_30_mins.size() >= 2) {
                                long time_now = JoH.tsl();
                                if ((time_now - last_two_calibrations.get(0).timestamp < (1000 * 60 * 60))
                                        && !Ob1G5CollectionService.isG5WantingCalibration()) { // Put steps in place to discourage over calibration
                                    this.nav_drawer_options.add(context.getString(R.string.override_calibration));
                                    this.nav_drawer_intents.add(new Intent(context, CalibrationOverride.class));
                                    this.nav_drawer_icons.add(R.drawable.ic_eyedropper_variant_grey600_24dp);
                                } else { // G5, old G6, or Firefly in no-code mode, after initial calibration and long enough after previous calibration
                                    this.nav_drawer_options.add(context.getString(R.string.add_calibration));
                                    this.nav_drawer_intents.add(new Intent(context, AddCalibration.class));
                                    this.nav_drawer_icons.add(R.drawable.ic_eyedropper_variant_grey600_24dp);
                                }
                            } else {  // G5, old G6 or Firefly in no-code mode, not long after a calibration
                                this.nav_drawer_options.add(context.getString(R.string.cannot_calibrate_right_now));
                                this.nav_drawer_intents.add(new Intent(context, Home.class));
                                this.nav_drawer_icons.add(R.drawable.ic_hourglass);
                            }
                        } else { // If there haven't been two initial calibrations
                            if (BgReading.isDataSuitableForDoubleCalibration() || Ob1G5CollectionService.isG5WantingInitialCalibration()) {
                                if ((FirmwareCapability.isTransmitterRawIncapable(getTransmitterID()) && last_two_bgReadings.size() > 1) || FirmwareCapability.isDeviceAltOrAlt2OrAlt3(getTransmitterID()) ) { //A Firefly G6 after third reading or a G7
                                    this.nav_drawer_options.add(context.getString(R.string.add_calibration));
                                    this.nav_drawer_intents.add(new Intent(context, AddCalibration.class));
                                    this.nav_drawer_icons.add(R.drawable.ic_eyedropper_variant_grey600_24dp);
                                } else { // G5 or non-Firefly G6 or Firefly G6 in no-code mode, after warm-up before initial calibration
                                    this.nav_drawer_options.add(context.getString(R.string.initial_calibration));
                                    this.nav_drawer_intents.add(new Intent(context, DoubleCalibrationActivity.class));
                                    this.nav_drawer_icons.add(R.drawable.ic_eyedropper_variant_grey600_24dp);
                                }
                            }
                        }
                    }
                }
                if (!getBestCollectorHardwareName().equals("G7")) { // If we are using G7, there will be no stop sensor option in the menu.
                    this.nav_drawer_options.add(context.getString(R.string.stop_sensor));
                    this.nav_drawer_intents.add(new Intent(context, StopSensor.class));
                    this.nav_drawer_icons.add(R.drawable.power_standby);
                }
            } else {
                this.nav_drawer_options.add(context.getString(R.string.start_sensor));
                this.nav_drawer_intents.add(new Intent(context, StartNewSensor.class));
                this.nav_drawer_icons.add(R.drawable.ic_add_black_36dp);
            }
        }

        if (DexCollectionType.usesBluetoothScan()) {
            this.nav_drawer_options.add(context.getString(R.string.bluetooth_scan));
            this.nav_drawer_intents.add(new Intent(context, BluetoothScan.class));
            this.nav_drawer_icons.add(R.drawable.ic_bluetooth_settings_grey600_48dp);
        }

        //if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
        this.nav_drawer_options.add(context.getString(R.string.system_status));
        this.nav_drawer_intents.add(new Intent(context, MegaStatus.class));
        this.nav_drawer_icons.add(R.drawable.ic_heart_pulse_grey600_24dp);
        //}

        boolean bg_alerts = prefs.getBoolean("bg_alerts_from_main_menu", false);
        if (bg_alerts) {
            this.nav_drawer_options.add(context.getString(R.string.level_alerts));
            this.nav_drawer_intents.add(new Intent(context, AlertList.class));
            this.nav_drawer_icons.add(R.drawable.ic_add_alert_grey600_48dp);
        }

        if (Experience.gotData()) {
            this.nav_drawer_options.add(context.getString(R.string.snooze_alert));
            this.nav_drawer_intents.add(new Intent(context, SnoozeActivity.class));
            this.nav_drawer_icons.add(R.drawable.ic_av_timer_white_36dp);
        }

        if (use_note_search || (Treatments.last() != null)) {
            this.nav_drawer_options.add(context.getString(R.string.note_search));
            this.nav_drawer_intents.add(new Intent(context, NoteSearch.class));
            this.nav_drawer_icons.add(R.drawable.ic_note_plus_grey600_24dp);
            use_note_search = true; // cache
        }

        if (Experience.gotData()) {
            this.nav_drawer_options.add(context.getString(R.string.statistics));
            this.nav_drawer_intents.add(new Intent(context, StatsActivity.class));
            this.nav_drawer_icons.add(R.drawable.ic_chart_areaspline_grey600_48dp);

            this.nav_drawer_options.add(context.getString(R.string.history));
            this.nav_drawer_intents.add(new Intent(context, BGHistory.class));
            this.nav_drawer_icons.add(R.drawable.ic_undo_variant_grey600_24dp);
        }

        this.nav_drawer_options.add(context.getString(R.string.settings));
        this.nav_drawer_intents.add(new Intent(context, Preferences.class));
        this.nav_drawer_icons.add(R.drawable.ic_wrench_grey600_48dp);
    }
}
