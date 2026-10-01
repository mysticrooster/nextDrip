package com.eveningoutpost.dexdrip;

import java.util.Date;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.preference.PreferenceManager;

import androidx.databinding.ObservableField;

import com.eveningoutpost.dexdrip.models.ActiveBgAlert;
import com.eveningoutpost.dexdrip.models.AlertType;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.services.MissedReadingService;
import com.eveningoutpost.dexdrip.ui.secondary.SnoozeScreen;
import com.eveningoutpost.dexdrip.utilitymodels.AlertPlayer;
import com.eveningoutpost.dexdrip.utilitymodels.Notifications;

import static com.eveningoutpost.dexdrip.xdrip.gs;

/**
 * Alert snooze / disable screen (Track V pass 5, now Compose). The activity keeps the static
 * snooze helpers, the per-type preference actions and the remote snooze; the screen derives its
 * status/visibility state and calls back here.
 */
public class SnoozeActivity extends BaseAppCompatActivity {

    // Snooze types supported by this class
    public enum SnoozeType {
        ALL_ALERTS("alerts_disabled_until"),
        LOW_ALERTS("low_alerts_disabled_until"),
        HIGH_ALERTS("high_alerts_disabled_until")
        ;

        private final String prefKey;
        SnoozeType(String prefKey) {
            this.prefKey = prefKey;
        }

        public String getPrefKey() {
            return prefKey;
        }
    }

    /**
     * Snoozes a given type for the specified number of minutes.
     */
    public static void snoozeForType(long minutes, SnoozeType disableType, SharedPreferences prefs) {
        if (minutes == -1) {
            minutes = infiniteSnoozeValueInMinutes;
        }
        long disableUntil = new Date().getTime() + minutes * 1000 * 60;

        prefs.edit().putLong(disableType.getPrefKey(), disableUntil).apply();

        //check if active bg alert exists and delete it depending on type of alert
        ActiveBgAlert aba = ActiveBgAlert.getOnly();
        if (aba != null) {
            AlertType activeBgAlert = ActiveBgAlert.alertTypegetOnly();
            if (disableType == SnoozeType.ALL_ALERTS
                    || (activeBgAlert.above && disableType == SnoozeType.HIGH_ALERTS)
                    || (!activeBgAlert.above && disableType == SnoozeType.LOW_ALERTS)
            ) {
                //active bg alert exists which is a type that is being disabled so let's remove it completely from the database
                ActiveBgAlert.ClearData();
            }
        }

        if (disableType == SnoozeType.ALL_ALERTS) {
            //disabling all , after the Snooze time set, all alarms will be re-enabled, inclusive low and high bg alarms
            prefs.edit().putLong(SnoozeType.HIGH_ALERTS.getPrefKey(), 0).apply();
            prefs.edit().putLong(SnoozeType.LOW_ALERTS.getPrefKey(), 0).apply();
        }
        recheckAlerts();
    }

    public SharedPreferences prefs;
    public final ObservableField<Integer> refreshTick = new ObservableField<>(0);

    public static final long infiniteSnoozeValueInMinutes = 5256000;//10 years
    public static final int snoozeValues[] = new int []{ 10, 15, 20, 30, 40, 50, 60, 75, 90, 120, 150, 180, 240, 300, 360, 420, 480, 540, 600, 720};

    public static int getSnoozeLocation(int time) {
        for (int i = 0; i < snoozeValues.length; i++) {
            if (time == snoozeValues[i]) {
                return i;
            } else if (time < snoozeValues[i]) {
                // we are in the middle of two, return the smaller
                if (i == 0) {
                    return 0;
                }
                return i - 1;
            }
        }
        return snoozeValues.length - 1;
    }

    public static String getNameFromTime(int time) {
        if (time < 120) {
            return time + " " + gs(R.string.unit_minutes);
        }
        return (time / 60.0) + " " + gs(R.string.unit_hours);
    }

    public static int getTimeFromSnoozeValue(int pickedNumber) {
        return snoozeValues[pickedNumber];
    }

    public static int getDefaultSnooze(boolean above) {
        if (above) {
            return 120;
        }
        return 35;
    }

    /** Still used by the not-yet-migrated EditAlertActivity (V6). */
    public static void SetSnoozePickerValues(android.widget.NumberPicker picker, boolean above, int default_snooze) {
        String[] values = new String[snoozeValues.length];
        for (int i = 0; i < values.length; i++) {
            values[i] = getNameFromTime(snoozeValues[i]);
        }

        picker.setMaxValue(values.length - 1);
        picker.setMinValue(0);
        picker.setDisplayedValues(values);
        picker.setWrapSelectorWheel(false);
        if (default_snooze != 0) {
            picker.setValue(getSnoozeLocation(default_snooze));
        } else {
            picker.setValue(getSnoozeLocation(getDefaultSnooze(above)));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Home.get_holo()) { setTheme(R.style.OldAppThemeNoTitleBar); }
        super.onCreate(savedInstanceState);
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SnoozeScreen.installSnooze(this);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            notifyChanged();
        }
    }

    /** Nudges the Compose screen to recompute its status/visibility state. */
    public void notifyChanged() {
        final Integer current = refreshTick.get();
        refreshTick.set((current == null ? 0 : current) + 1);
    }

    public void snoozeNow(int intValue) {
        AlertPlayer.getPlayer().Snooze(getApplicationContext(), intValue);
        if (ActiveBgAlert.getOnly() != null) {
            startActivity(new Intent(getApplicationContext(), Home.class));
        }
        finish();
    }

    public void disableType(long minutes, SnoozeType type) {
        snoozeForType(minutes, type, prefs);
        notifyChanged();
    }

    public void clearDisabled(SnoozeType type) {
        prefs.edit().putLong(type.getPrefKey(), 0).apply();
        //this is needed to make sure that the missedreading alert will be rechecked, it might have to be raised
        //and if not (ie no missed readings for long enough) then the alarm should be reset because it might have to recheck the missedreading status sooner
        recheckAlerts();
        notifyChanged();
    }

    public void sendRemoteSnooze() {
        JoH.static_toast_short(gs(R.string.remote_snooze));
        AlertPlayer.getPlayer().Snooze(xdrip.getAppContext(), -1);
    }

    public static void recheckAlerts() {
        Notifications.start();
        JoH.startService(MissedReadingService.class); // TODO this should be rate limited or similar as it is polled in various locations leading to excessive cpu
    }
}
