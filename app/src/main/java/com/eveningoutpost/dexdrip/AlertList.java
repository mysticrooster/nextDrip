package com.eveningoutpost.dexdrip;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.preference.PreferenceManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.ObservableField;

import com.eveningoutpost.dexdrip.models.AlertType;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.ui.secondary.AlertListScreen;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.eveningoutpost.dexdrip.xdrip.gs;

/**
 * Track V (Alerts, Compose) — the level-alerts list. Low/high alert profiles with add/edit
 * (long-press) navigation. The activity keeps the preference read, the storage-permission warning
 * for custom tones and the edit/result flow; the screen renders the rows.
 */
public class AlertList extends BaseAppCompatActivity {
    boolean doMgdl;
    Context mContext;
    final int ADD_ALERT = 1;
    final int EDIT_ALERT = 2;
    SharedPreferences prefs;

    private List<AlertRow> lowRows = new ArrayList<>();
    private List<AlertRow> highRows = new ArrayList<>();

    /** Compose bridge: bumped whenever the displayed alert rows change. */
    public final ObservableField<Integer> tick = new ObservableField<>(0);

    /** One rendered alert row. */
    public static class AlertRow {
        public final String name;
        public final String threshold;
        public final String time;
        public final String mp3File;
        public final String overrideSilentMode;
        public final String uuid;
        public final boolean active;

        AlertRow(String name, String threshold, String time, String mp3File, String overrideSilentMode, String uuid, boolean active) {
            this.name = name;
            this.threshold = threshold;
            this.time = time;
            this.mp3File = mp3File;
            this.overrideSilentMode = overrideSilentMode;
            this.uuid = uuid;
            this.active = active;
        }
    }

    String stringTimeFromAlert(AlertType alert) {
        if (alert.all_day) {
            return getString(R.string.all_day);
        }
        String start = timeFormatString(AlertType.time2Hours(alert.start_time_minutes), AlertType.time2Minutes(alert.start_time_minutes));
        String end = timeFormatString(AlertType.time2Hours(alert.end_time_minutes), AlertType.time2Minutes(alert.end_time_minutes));
        return start + " - " + end;
    }

    private AlertRow createAlertRow(AlertType alert) {
        String overrideSilentMode = getString(R.string.override_silent_mode);
        if (!alert.override_silent_mode) {
            overrideSilentMode = getString(R.string.no_alert_in_silent_mode);
        }
        return new AlertRow(
                alert.name,
                EditAlertActivity.unitsConvert2Disp(doMgdl, alert.threshold),
                stringTimeFromAlert(alert),
                shortPath(alert.mp3_file),
                overrideSilentMode,
                alert.uuid,
                alert.active);
    }

    private List<AlertRow> createAlerts(boolean above) {
        final List<AlertRow> feedList = new ArrayList<>();
        final List<AlertType> alerts = AlertType.getAll(above);
        for (AlertType alert : alerts) {
            feedList.add(createAlertRow(alert));
        }
        return feedList;
    }

    @Override
    protected void onResume() {
        xdrip.checkForcedEnglish(xdrip.getAppContext());
        super.onResume();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getApplicationContext();
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        doMgdl = (prefs.getString("units", "mgdl").compareTo("mgdl") == 0);

        FillLists();
        AlertListScreen.installAlertList(this);
    }

    public void notifyChanged() {
        final Integer current = tick.get();
        tick.set((current == null ? 0 : current) + 1);
    }

    public void addLowAlert() {
        xdrip.checkForcedEnglish(xdrip.getAppContext());
        Intent myIntent = new Intent(AlertList.this, EditAlertActivity.class);
        myIntent.putExtra("above", "false");
        startActivityForResult(myIntent, ADD_ALERT);
    }

    public void addHighAlert() {
        xdrip.checkForcedEnglish(xdrip.getAppContext());
        Intent myIntent = new Intent(AlertList.this, EditAlertActivity.class);
        myIntent.putExtra("above", "true");
        startActivityForResult(myIntent, ADD_ALERT);
    }

    public void editAlert(String uuid) {
        xdrip.checkForcedEnglish(xdrip.getAppContext());
        Intent myIntent = new Intent(AlertList.this, EditAlertActivity.class);
        myIntent.putExtra("uuid", uuid);
        startActivityForResult(myIntent, EDIT_ALERT);
    }

    public List<AlertRow> getLowRowsSnapshot() {
        return new ArrayList<>(lowRows);
    }

    public List<AlertRow> getHighRowsSnapshot() {
        return new ArrayList<>(highRows);
    }

    void displayWarning() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (!isFinishing()) {
                    new AlertDialog.Builder(AlertList.this)
                            .setTitle(getString(R.string.alert_warning))
                            .setMessage(getString(R.string.no_active_low_alert_warning))
                            .setCancelable(false)
                            .setPositiveButton(
                                    getString(R.string.ok),
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            dialog.cancel();
                                        }
                                    })
                            .create().show();
                }
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!AlertType.activeLowAlertExists()) {
            displayWarning();
        }
        if (requestCode == ADD_ALERT || requestCode == EDIT_ALERT) {
            if (resultCode == RESULT_OK) {
                FillLists();
            }
        }
    }

    void FillLists() {
        lowRows = createAlerts(false);
        highRows = createAlerts(true);
        notifyChanged();
    }

    private String shortPath(final String path) {
        try {
            return EditAlertActivity.shortPath(path);
        } catch (SecurityException e) {
            // need external storage permission?
            checkStoragePermissions(gs(R.string.need_permission_to_access_audio_files));
            return "";
        }
    }

    // TODO this can be centralized
    private final static int MY_PERMISSIONS_REQUEST_STORAGE = 104;

    private boolean checkStoragePermissions(String msg) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(),
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                final Activity activity = this;
                JoH.show_ok_dialog(activity, gs(R.string.please_allow_permission), msg, () -> ActivityCompat.requestPermissions(activity,
                        new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_STORAGE));
                return false;
            }
        }
        return true;
    }

    public String timeFormatString(int hour, int minute) {
        SimpleDateFormat timeFormat24 = new SimpleDateFormat("HH:mm");
        String selected = hour + ":" + ((minute < 10) ? "0" : "") + minute;
        if (!android.text.format.DateFormat.is24HourFormat(mContext)) {
            try {
                Date date = timeFormat24.parse(selected);
                SimpleDateFormat timeFormat12 = new SimpleDateFormat("hh:mm aa");
                return timeFormat12.format(date);
            } catch (final ParseException e) {
                e.printStackTrace();
            }
        }
        return selected;
    }
}
