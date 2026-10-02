package com.eveningoutpost.dexdrip;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.databinding.ObservableField;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.TimePicker;
import android.widget.Toast;

import com.eveningoutpost.dexdrip.models.AlertType;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.ui.dialog.GenericConfirmDialog;
import com.eveningoutpost.dexdrip.ui.secondary.EditAlertScreen;
import com.eveningoutpost.dexdrip.utilitymodels.AlertPlayer;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.watch.thinjam.BlueJayEntry;
import com.eveningoutpost.dexdrip.wearintegration.WatchUpdaterService;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.eveningoutpost.dexdrip.Home.startWatchUpdaterService;
import static com.eveningoutpost.dexdrip.xdrip.gs;

import lombok.val;

/**
 * Track V (Alerts) — the alert editor, migrated in place to Compose. All validation, units
 * conversion, threshold/overlap rules, ringtone/file selection, snooze/pre-snooze side effects and
 * the shared public statics ({@link #unitsConvert2Disp}, {@link #timeFormatString},
 * {@link #shortPath}) are retained here; the screen renders the form and calls back.
 */
public class EditAlertActivity extends BaseAppCompatActivity {

    private static final String TAG = AlertPlayer.class.getSimpleName();

    /** Compose bridge. */
    public final ObservableField<Integer> tick = new ObservableField<>(0);
    public final ObservableField<String> header = new ObservableField<>("");
    public final ObservableField<String> name = new ObservableField<>("");
    public final ObservableField<String> thresholdText = new ObservableField<>("");
    public final ObservableField<String> snoozeText = new ObservableField<>("");
    public final ObservableField<String> reraiseText = new ObservableField<>("");
    public final ObservableField<String> toneText = new ObservableField<>("");
    public final ObservableField<String> startTimeText = new ObservableField<>("");
    public final ObservableField<String> endTimeText = new ObservableField<>("");
    public final ObservableField<Boolean> allDay = new ObservableField<>(true);
    public final ObservableField<Boolean> vibrate = new ObservableField<>(true);
    public final ObservableField<Boolean> disabled = new ObservableField<>(false);
    public final ObservableField<Boolean> overrideSilent = new ObservableField<>(true);
    public final ObservableField<Boolean> forceSpeaker = new ObservableField<>(true);
    public final ObservableField<Boolean> editable = new ObservableField<>(true);
    public final ObservableField<Boolean> removable = new ObservableField<>(false);

    private int startHour = 0;
    private int startMinute = 0;
    private int endHour = 23;
    private int endMinute = 59;

    private String audioPath;

    private boolean doMgdl;
    private String uuid;
    private Context mContext;
    private boolean above;
    private final int REQUEST_CODE_CHOOSE_FILE = 1;
    private final static int MY_PERMISSIONS_REQUEST_STORAGE = 138;

    private final int MIN_ALERT = 40;
    private final int MAX_ALERT = 400;

    public void notifyChanged() {
        final Integer current = tick.get();
        tick.set((current == null ? 0 : current) + 1);
    }

    String getExtra(Bundle savedInstanceState, String paramName, String defaultVal) {
        String newString;
        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if(extras == null) {
                newString= null;
            } else {
                newString= extras.getString(paramName);
            }
        } else {
            newString = (String) savedInstanceState.getSerializable(paramName);
        }
        if (newString != null) {
            return newString;
        } else {
            return defaultVal;
        }
    }

    @Override
    protected void onResume() {
        xdrip.checkForcedEnglish(xdrip.getAppContext());
        super.onResume();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        xdrip.checkForcedEnglish(xdrip.getAppContext());
        super.onCreate(savedInstanceState);
        mContext = this;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        doMgdl = (prefs.getString("units", "mgdl").compareTo("mgdl") == 0);

        uuid = getExtra(savedInstanceState, "uuid", null);
        String status;
        int alertReraise;
        int defaultSnooze;
        if (uuid == null) {
            // This is a new alert
            above = Boolean.parseBoolean(getExtra(savedInstanceState, "above", null));
            allDay.set(true);
            vibrate.set(true);
            disabled.set(false);
            overrideSilent.set(true);
            forceSpeaker.set(true);

            audioPath = "";
            toneText.set(shortPath(audioPath));
            defaultSnooze = SnoozeActivity.getDefaultSnooze(above);
            removable.set(false);
            status = getString(R.string.adding)+" " + (above ? getString(R.string.high) : getString(R.string.low)) + " "+getString(R.string.alert);
            startHour = 0;
            startMinute = 0;
            endHour = 23;
            endMinute = 59;
            alertReraise = 1;
        } else {
            // We are editing an alert
            AlertType alertType = AlertType.get_alert(uuid);
            if(alertType==null) {
                Log.wtf(TAG, "Error editing alert, when that alert does not exist...");
                Intent returnIntent = new Intent();
                setResult(RESULT_CANCELED, returnIntent);
                finish();
                return;
            }

            above = alertType.above;
            name.set(alertType.name);
            thresholdText.set(unitsConvert2Disp(doMgdl, alertType.threshold));
            allDay.set(alertType.all_day);
            vibrate.set(alertType.vibrate);
            disabled.set(!alertType.active);
            overrideSilent.set(alertType.override_silent_mode);
            forceSpeaker.set(alertType.force_speaker);
            defaultSnooze = alertType.default_snooze;
            if(defaultSnooze == 0) {
                defaultSnooze = SnoozeActivity.getDefaultSnooze(above);
            }

            audioPath = getExtra(savedInstanceState, "audioPath" ,alertType.mp3_file);
            toneText.set(shortPath(audioPath));

            status = getString(R.string.editing)+" " + (above ? getString(R.string.high) : getString(R.string.low)) + " "+getString(R.string.alert);
            startHour = AlertType.time2Hours(alertType.start_time_minutes);
            startMinute = AlertType.time2Minutes(alertType.start_time_minutes);
            endHour = AlertType.time2Hours(alertType.end_time_minutes);
            endMinute = AlertType.time2Minutes(alertType.end_time_minutes);
            alertReraise = alertType.minutes_between;
            removable.set(true);

            if(uuid.equals(AlertType.LOW_ALERT_55)) {
                // This is the 55 alert, can not be edited
                editable.set(false);
            }
        }
        reraiseText.set(String.valueOf(alertReraise));
        snoozeText.set(String.valueOf(defaultSnooze));
        header.set(status);
        refreshTimeTexts();

        EditAlertScreen.installEditAlert(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
        outState.putString("uuid", uuid);
        outState.putString("above", String.valueOf(above));
        outState.putString("audioPath", audioPath);
    }

    public static DecimalFormat getNumberFormatter(boolean doMgdl) {
        DecimalFormat df = new DecimalFormat("#");
        if (doMgdl) {
            df.setMaximumFractionDigits(0);
            df.setMinimumFractionDigits(0);
        } else {
            df.setMaximumFractionDigits(1);
            df.setMinimumFractionDigits(1);
        }

        return df;
    }

    public static String unitsConvert2Disp(boolean doMgdl, double threshold) {
        DecimalFormat df = getNumberFormatter(doMgdl);
        if (doMgdl)
            return df.format(threshold);

        return df.format(threshold / Constants.MMOLL_TO_MGDL);
    }

    double unitsConvertFromDisp(double threshold) {
        if(doMgdl ) {
            return threshold;
        } else {
            return threshold * Constants.MMOLL_TO_MGDL;
        }
    }

    private void refreshTimeTexts() {
        startTimeText.set(timeFormatString(mContext, startHour, startMinute));
        endTimeText.set(timeFormatString(mContext, endHour, endMinute));
    }

    // region Compose field setters

    public void setName(String value) { name.set(value); }
    public void setThreshold(String value) { thresholdText.set(value); }
    public void setSnooze(String value) { snoozeText.set(value); }
    public void setReraise(String value) { reraiseText.set(value); }
    public void setAllDay(boolean value) { allDay.set(value); notifyChanged(); }
    public void setVibrate(boolean value) { vibrate.set(value); notifyChanged(); }
    public void setDisabled(boolean value) { disabled.set(value); notifyChanged(); }
    public void setOverrideSilent(boolean value) { overrideSilent.set(value); notifyChanged(); }
    public void setForceSpeaker(boolean value) { forceSpeaker.set(value); notifyChanged(); }

    /** Applies a default-snooze choice from the screen's picker. */
    public void applySnoozeChoice(int minutes) {
        snoozeText.set(String.valueOf(minutes));
        notifyChanged();
    }

    /** Pre-snooze the existing alert by the chosen number of minutes. */
    public void preSnooze(int minutes) {
        if (uuid == null) return;
        AlertPlayer.getPlayer().PreSnooze(getApplicationContext(), uuid, minutes);
    }

    public void pickStartTime() {
        TimePickerDialog mTimePicker = new TimePickerDialog(mContext, AlertDialog.THEME_HOLO_DARK, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                startHour = selectedHour;
                startMinute = selectedMinute;
                refreshTimeTexts();
                notifyChanged();
            }
        }, startHour, startMinute, DateFormat.is24HourFormat(mContext));
        mTimePicker.setTitle(getString(R.string.select_time));
        mTimePicker.show();
    }

    public void pickEndTime() {
        TimePickerDialog mTimePicker = new TimePickerDialog(mContext, AlertDialog.THEME_HOLO_DARK, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                endHour = selectedHour;
                endMinute = selectedMinute;
                refreshTimeTexts();
                notifyChanged();
            }
        }, endHour, endMinute, DateFormat.is24HourFormat(mContext));
        mTimePicker.setTitle(getString(R.string.select_time));
        mTimePicker.show();
    }

    /** Ringtone picker (tone option 0). */
    public void chooseRingtone() {
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.select_tone_for_alerts));
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL);
        startActivityForResult(intent, 999);
    }

    /** Audio-file picker (tone option 1). */
    public void chooseToneFile() {
        if (checkPermissions()) {
            chooseFile();
        }
    }

    /** Revert to the xDrip default tone (tone option 2). */
    public void useDefaultTone() {
        audioPath = "";
        toneText.set(shortPath(audioPath));
        notifyChanged();
    }

    // endregion

    private boolean verifyThreshold(double threshold, boolean allDay, int startTime, int endTime) {
        List<AlertType> lowAlerts = AlertType.getAll(false);
        List<AlertType> highAlerts = AlertType.getAll(true);

        if(threshold < MIN_ALERT || threshold > MAX_ALERT) {
            Toast.makeText(getApplicationContext(), getString(R.string.threshold_has_to_be_between, unitsConvert2Disp(doMgdl, MIN_ALERT), unitsConvert2Disp(doMgdl, MAX_ALERT)),Toast.LENGTH_LONG).show();
            return false;
        }
        // We want to make sure that for each threashold there is only one alert. Otherwise, which file should we play.
        for (AlertType lowAlert : lowAlerts) {
            if(lowAlert.threshold == threshold  && overlapping(lowAlert, allDay, startTime, endTime) && lowAlert.active) {
                if(uuid == null || ! uuid.equals(lowAlert.uuid)){ //new alert or not myself
                    Toast.makeText(getApplicationContext(),
                            getString(R.string.alert_threshold_already_in_use),Toast.LENGTH_LONG).show();
                    return false;
                }
            }
        }
        for (AlertType highAlert : highAlerts) {
            if(highAlert.threshold == threshold  && overlapping(highAlert, allDay, startTime, endTime) && highAlert.active) {
                if(uuid == null || ! uuid.equals(highAlert.uuid)){ //new alert or not myself
                    Toast.makeText(getApplicationContext(),
                            getString(R.string.alert_threshold_already_in_use),Toast.LENGTH_LONG).show();
                    return false;
                }
            }
        }

        // high alerts have to be higher than all low alerts...
        if(above) {
            for (AlertType lowAlert : lowAlerts) {
                if(threshold < lowAlert.threshold  && overlapping(lowAlert, allDay, startTime, endTime) && lowAlert.active) {
                    Toast.makeText(getApplicationContext(),
                            getString(R.string.high_alert_threshold_error),Toast.LENGTH_LONG).show();
                    return false;
                }
            }
        } else {
            // low alert has to be lower than all high alerts
            for (AlertType highAlert : highAlerts) {
                if(threshold > highAlert.threshold  && overlapping(highAlert, allDay, startTime, endTime) && highAlert.active) {
                    Toast.makeText(getApplicationContext(),
                            getString(R.string.low_alert_threshold_error),Toast.LENGTH_LONG).show();
                    return false;
                }
            }
        }

        return true;
    }

    // determines if an alertType is 'at' has a temporal overlap with a new alert with parameters
    // allday, startTime, entTime
    private boolean overlapping(AlertType at, boolean allday, int startTime, int endTime){
        //shortcut: if one is all day, they must overlap
        if(at.all_day || allday) {
            return true;
        }
        int st1 = at.start_time_minutes;
        int st2 = startTime;
        int et1 = at.end_time_minutes;
        int et2 = endTime;

        return  st1 <= st2 && et1 > st2 ||
                st1 <= st2 && (et2 < st2) && et2 > st1 || //2nd timeframe passes midnight
                st2 <= st1 && et2 > st1 ||
                st2 <= st1 && (et1 < st1) && et1 > st2 ||
                (et1 < st1 && et2 < st2); //both timeframes pass midnight -> overlap at least at midnight
    }

    private double parseDouble(String str) {
        try {
            final DecimalFormat numberFormatter = getNumberFormatter(doMgdl);
            return numberFormatter.parse(str).doubleValue();
        } catch (NumberFormatException | ParseException nfe) {
            Log.e(TAG, "Invalid number", nfe);
            Toast.makeText(getApplicationContext(), xdrip.gs(R.string.invalid_number) + " " + str, Toast.LENGTH_LONG).show();
            return Double.NaN;
        }
    }

    // rarely the parseInt can fail, this adds a failsafe in that case
    private int safeGetDefaultSnooze()
    {
        int defaultSnooze;
        try {
            defaultSnooze = parseInt(snoozeText.get());
        } catch (NullPointerException e) {
            Log.wtf(TAG,"Got null pointer exception unboxing parseInt: ",e);
            defaultSnooze = SnoozeActivity.getDefaultSnooze(above);
        }
        return defaultSnooze;
    }

    private Integer parseInt(String str) {
        try {
            return Integer.parseInt(str);
        }
        catch (NumberFormatException nfe) {
            Log.e(TAG, "Invalid number", nfe);
            Toast.makeText(getApplicationContext(), xdrip.gs(R.string.invalid_number) + " " + str, Toast.LENGTH_LONG).show();
            return null;
        }
    }

    public void saveAlert() {
        double threshold;
        try {
            // Check that values are ok.
            threshold = JoH.tolerantParseDouble(thresholdText.get());
            if (Double.isNaN(threshold))
                return;

        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), R.string.error_with_value, Toast.LENGTH_LONG).show();
            return;
        }

        threshold = unitsConvertFromDisp(threshold);

        int alertReraise;
        Integer alterReraiseInt = parseInt(reraiseText.get());
        if(alterReraiseInt ==null)
            return;
        alertReraise = alterReraiseInt;
        int defaultSnooze = safeGetDefaultSnooze();

        if(alertReraise < 1) {
            Toast.makeText(getApplicationContext(), getString(R.string.alert_reraise_value_too_small), Toast.LENGTH_LONG).show();
            return;
        } else if (alertReraise >= defaultSnooze) {
            Toast.makeText(getApplicationContext(), getString(R.string.alert_reraise_value_too_big), Toast.LENGTH_LONG).show();
            return;
        }

        int timeStart = AlertType.toTime(startHour, startMinute);
        int timeEnd = AlertType.toTime(endHour, endMinute);

        boolean isAllDay = allDay.get();
        // if 23:59 was set, we increase it to 24:00
        if(timeStart == AlertType.toTime(23, 59)) {
            timeStart++;
        }
        if(timeEnd == AlertType.toTime(23, 59)) {
            timeEnd++;
        }
        if(timeStart == AlertType.toTime(0, 0) &&
                timeEnd == AlertType.toTime(24, 0)) {
            isAllDay = true;
        }
        if (timeStart == timeEnd && (!isAllDay)) {
            Toast.makeText(getApplicationContext(), getString(R.string.start_and_end_time_same),Toast.LENGTH_LONG).show();
            return;
        }
        boolean isDisabled = disabled.get();
        if(!isDisabled && !verifyThreshold(threshold, isAllDay, timeStart, timeEnd)) {
            return;
        }
        boolean isVibrate = vibrate.get();

        boolean overrideSilentMode = overrideSilent.get();
        boolean forceSpeakerMode = forceSpeaker.get();

        String mp3_file = audioPath;
        if (uuid != null) {
            AlertType.update_alert(uuid, name.get(), above, threshold, isAllDay, alertReraise, mp3_file, timeStart, timeEnd, overrideSilentMode, forceSpeakerMode, defaultSnooze, isVibrate, !isDisabled);
        }  else {
            AlertType.add_alert(null, name.get(), above, threshold, isAllDay, alertReraise, mp3_file, timeStart, timeEnd, overrideSilentMode, forceSpeakerMode, defaultSnooze, isVibrate, !isDisabled);
        }

        startWatchUpdaterService(mContext, WatchUpdaterService.ACTION_SYNC_ALERTTYPE, TAG);
        Intent returnIntent = new Intent();
        setResult(RESULT_OK,returnIntent);
        BlueJayEntry.startWithRefreshIfEnabled();
        finish();
    }

    public void removeAlert() {
        GenericConfirmDialog.show(EditAlertActivity.this, gs(R.string.are_you_sure), gs(R.string.you_cannot_undo_delete_alert),
                () -> { // This, which deletes the alert, will only be executed after confirmation
                    if (uuid == null) {
                        Log.wtf(TAG, "Error remove pressed, while we were adding an alert");
                    } else {
                        AlertType.remove_alert(uuid);
                        startWatchUpdaterService(mContext, WatchUpdaterService.ACTION_SYNC_ALERTTYPE, TAG);
                    }
                    Intent returnIntent = new Intent();
                    setResult(RESULT_OK, returnIntent);
                    finish();
                }
        );
    }

    private void chooseFile()
    {
        final Intent fileIntent = new Intent();
        fileIntent.setType("audio/*");
        fileIntent.setAction(Intent.ACTION_OPEN_DOCUMENT);
        startActivityForResult(Intent.createChooser(fileIntent, getString(R.string.select_file_for_alert)), REQUEST_CODE_CHOOSE_FILE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_REQUEST_STORAGE) {
            if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                chooseFile(); // must be the only functionality which calls for permission
            } else {
                JoH.static_toast_long(this, getString(R.string.cannot_choose_file_without_permission));
            }
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            if (uri != null) {
                audioPath = uri.toString();
                Log.d(TAG, "Selected ringtone audio path: " + audioPath);
                toneText.set(shortPath(audioPath));
                notifyChanged();
            } else {
                if (requestCode == REQUEST_CODE_CHOOSE_FILE) {
                    try {
                        val selectedAudioUri = data.getData();
                        getContentResolver().takePersistableUriPermission(selectedAudioUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

                        String selectedAudioPath = getDisplayNameFromURI(selectedAudioUri);
                        if (selectedAudioPath == null) {
                            //OI FILE Manager
                            selectedAudioPath = selectedAudioUri.getPath();
                        }
                        Log.d(TAG, "Selected audio path: " + selectedAudioPath + " " + selectedAudioUri);
                        audioPath = selectedAudioUri.toString();
                        toneText.set(shortPath(selectedAudioPath));
                        notifyChanged();
                    } catch (Exception e) {
                        JoH.static_toast_long(getString(R.string.problem_with_sound) + " " + e.getMessage());
                    }
                }
            }
        }
    }

    private static String getDisplayNameFromURI(final Uri contentUri) {
        val title = JoH.getFieldFromURI(MediaStore.Audio.Media.TITLE, contentUri);
        if (title == null || contentUri.toString().endsWith(title)) {
            return JoH.getFieldFromURI(MediaStore.Audio.Media.DISPLAY_NAME, contentUri);
        }
        return title;
    }


    static public String timeFormatString(Context context, int hour, int minute) {
        SimpleDateFormat timeFormat24 = new SimpleDateFormat("HH:mm");
        String selected = hour+":" + ((minute<10)?"0":"") + minute;
        if (!DateFormat.is24HourFormat(context)) {
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

    public static boolean isPathRingtone(Context context, String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        Ringtone ringtone = RingtoneManager.getRingtone(context, Uri.parse(path));
        return ringtone != null;
    }

    static String shortPath(final String path) {
        if (path == null) {
            return "";
        }
        if (path.length() == 0) {
            return "xDrip Default";
        }
        if (path.startsWith("content:")) {
            val result = getDisplayNameFromURI(Uri.parse(path));
            if (result != null) return result;
        }
        // This may not actually be used anymore
        if (isPathRingtone(xdrip.getAppContext(), path)) {
            val ringtone = RingtoneManager.getRingtone(xdrip.getAppContext(), Uri.parse(path));
            // Just verified that the ringtone exists... not checking for null
            val result = ringtone.getTitle(xdrip.getAppContext());
            Log.d(TAG,"Ringtone title: "+result);
            return result;
        }

        String[] segments = path.split("/");
        if (segments.length > 1) {
            return segments[segments.length - 1];
        }
        return path;
    }

    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(),
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                final Activity activity = this;
                JoH.show_ok_dialog(activity, gs(R.string.please_allow_permission), gs(R.string.need_storage_permission_to_access_all_ringtones), new Runnable() {
                    @Override
                    public void run() {
                        ActivityCompat.requestPermissions(activity,
                                new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_STORAGE);
                    }
                });
                return false;
            }
        }
        return true;
    }


    public void testAlert() {
        // Check that values are ok.
        double threshold = parseDouble(thresholdText.get());
        if(Double.isNaN(threshold)) {
            JoH.static_toast_long("Threshold number is not valid");
            return;
        }

        threshold = unitsConvertFromDisp(threshold);

        int timeStart = AlertType.toTime(startHour, startMinute);
        int timeEnd = AlertType.toTime(endHour, endMinute);

        boolean isAllDay = allDay.get();
        // if 23:59 was set, we increase it to 24:00
        if(timeStart == AlertType.toTime(23, 59)) {
            timeStart++;
        }
        if(timeEnd == AlertType.toTime(23, 59)) {
            timeEnd++;
        }
        if(timeStart == AlertType.toTime(0, 0) &&
                timeEnd == AlertType.toTime(24, 0)) {
            isAllDay = true;
        }
        if (timeStart == timeEnd && (!isAllDay)) {
            Toast.makeText(getApplicationContext(), getString(R.string.start_and_end_time_same),Toast.LENGTH_LONG).show();
            return;
        }
        if(!verifyThreshold(threshold, isAllDay, timeStart, timeEnd)) {
            return;
        }
        boolean isVibrate = vibrate.get();
        boolean overrideSilentMode = overrideSilent.get();
        boolean forceSpeakerMode = forceSpeaker.get();
        String mp3_file = audioPath;
        try {
            int defaultSnooze = safeGetDefaultSnooze();

            if (Pref.getBooleanDefaultFalse("start_snoozed"))  {
                JoH.static_toast_long(getString(R.string.start_snoozed_enabled));
            } else if (Pref.getStringDefaultBlank("bg_alert_profile").equals("ascending") && Pref.getBoolean("delay_ascending_3min", true)) {
                JoH.static_toast_long(getString(R.string.volume_profile_set_to_ascending_with_delay));
            } else if (Pref.getStringDefaultBlank("bg_alert_profile").equals("Silent")) {
                JoH.static_toast_long(getString(R.string.volume_profile_set_to_silent));
            }

            AlertType.testAlert(name.get(), above, threshold, isAllDay, 1, mp3_file, timeStart, timeEnd, overrideSilentMode, forceSpeakerMode, defaultSnooze, isVibrate, mContext);
        } catch (NullPointerException e) {
            JoH.static_toast_long("Snooze value is not a number - cannot test");
        }
    }
}
