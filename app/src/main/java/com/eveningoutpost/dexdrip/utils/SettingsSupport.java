package com.eveningoutpost.dexdrip.utils;

import static com.eveningoutpost.dexdrip.models.JoH.showNotification;
import static com.eveningoutpost.dexdrip.models.JoH.tolerantParseDouble;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.OUT_OF_RANGE_GLUCOSE_ENTRY_ID;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.Profile;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.profileeditor.ProfileEditor;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.xdrip;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Non-UI settings support API extracted from the retired legacy {@code Preferences} activity.
 *
 * These methods are used by services, migrations, QR export and the Compose settings host, so they
 * must remain available after the legacy {@code android.preference} UI is removed.
 */
public class SettingsSupport {

    private static final String TAG = "jamorham SettingsSupport";

    public static final double MIN_GLUCOSE_INPUT = 40; // The smallest acceptable input glucose value in mg/dL
    public static final double MAX_GLUCOSE_INPUT = 400; // The largest acceptable input glucose value in mg/dL

    public interface OnServiceTaskCompleted {
        void onTaskCompleted(byte[] result);
    }

    /** Returns the sorted keys of a preference map, one per line. */
    public static String getMapKeysString(final Map<String, ?> prefsmap) {
        final StringBuilder sb = new StringBuilder();
        final List<String> keyList = new ArrayList<>(prefsmap.keySet());
        Collections.sort(keyList);
        for (final String entry : keyList) {
            sb.append(entry);
            sb.append("\n");
        }
        return sb.toString();
    }

    public static Boolean getBooleanPreferenceViaContextWithoutException(Context context, String key, Boolean defaultValue) {
        try {
            return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(key, defaultValue);
        } catch (ClassCastException ex) {
            return defaultValue;
        }
    }

    public static boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    /** Correct a preference glucose setting if the value is out of range. */
    public static void applyPrefSettingRange(String pref_key, String def, Double min, Double max) {
        final int notificationId = OUT_OF_RANGE_GLUCOSE_ENTRY_ID;
        String mySettingString = Pref.getString(pref_key, def);
        final boolean doMgdl = (Pref.getString("units", "mgdl").equals("mgdl"));
        double mySettingMgdl = doMgdl ? tolerantParseDouble(mySettingString) : tolerantParseDouble(mySettingString) * Constants.MMOLL_TO_MGDL; // The preference value in mg/dL
        if (mySettingMgdl > max) { // If the preference value is greater than max
            if (!doMgdl && mySettingString.equals(def)) { // If the setting value in mmol/L is the same as the default, which is in mg/dL, we correct the value next.
                // This will only happen if user has chosen mmol/L and updates to a version that has a new preference setting with default in mg/dL
                UserError.Log.d(TAG, "Setting  " + pref_key + "  to default converted to mmol/L");
                Pref.setString(pref_key, JoH.qs(tolerantParseDouble(def) * Constants.MGDL_TO_MMOLL, 1)); // Set the preference to the default value converted to mmol/L
            } else { // The preference has been set to a value greater than the max allowed.  Let's fix it and notify.
                // This will only happen if user has entered a preference setting value out of range before the listener range limit update has been merged.
                mySettingString = doMgdl ? max + "" : JoH.qs(max * Constants.MGDL_TO_MMOLL, 1) + "";
                Pref.setString(pref_key, mySettingString); // Set the preference to max
                UserError.Log.uel(TAG, xdrip.gs(R.string.pref_was_greater_than_max, pref_key)); // Inform the user that xDrip is changing the setting value
                showNotification(pref_key, xdrip.gs(R.string.setting_pref_to_max), null, notificationId, null, false, false, null, null, null, true);
            }
        } else if (mySettingMgdl < min) { // If the preference value is less than min, correct it and notify.
            // This will only happen if user has entered a preference setting value out of range before the listener range limit update has been merged.
            mySettingString = doMgdl ? min + "" : JoH.qs(min * Constants.MGDL_TO_MMOLL, 1) + "";
            Pref.setString(pref_key, mySettingString); // Set the preference to min
            UserError.Log.uel(TAG, xdrip.gs(R.string.pref_was_less_than_min, pref_key)); // Inform the user that xDrip is changing the setting value
            showNotification(pref_key, xdrip.gs(R.string.setting_pref_to_min), null, notificationId, null, false, false, null, null, null, true);
        }
    }

    /**
     * Non-UI port of the legacy {@code handleUnitsChange(Preference, Object, AllPrefsFragment)} unit
     * change side effect: converts the stored high/low/insulin-sensitivity/target/persistent-high/
     * forecast-low values when switching between mg/dL and mmol/L.
     */
    public static void handleUnitsChange(String newUnits) {
        try {
            final SharedPreferences preferences = Pref.getInstance();

            final double highVal = Double.parseDouble(preferences.getString("highValue", "0"));
            final double lowVal = Double.parseDouble(preferences.getString("lowValue", "0"));
            final double default_insulin_sensitivity = Double.parseDouble(preferences.getString("profile_insulin_sensitivity_default", "54"));
            final double default_target_glucose = Double.parseDouble(preferences.getString("plus_target_range", "100"));
            final double persistent_high_Val = Double.parseDouble(preferences.getString("persistent_high_threshold", "0"));
            final double forecast_low_Val = Double.parseDouble(preferences.getString("forecast_low_threshold", "0"));

            if (newUnits.equals("mgdl")) {
                if (highVal < 36) {
                    ProfileEditor.convertData(Constants.MMOLL_TO_MGDL);
                    preferences.edit().putString("highValue", Long.toString(Math.round(highVal * Constants.MMOLL_TO_MGDL))).apply();
                    preferences.edit().putString("profile_insulin_sensitivity_default", Long.toString(Math.round(default_insulin_sensitivity * Constants.MMOLL_TO_MGDL))).apply();
                    preferences.edit().putString("plus_target_range", Long.toString(Math.round(default_target_glucose * Constants.MMOLL_TO_MGDL))).apply();
                    Profile.invalidateProfile();
                }
                if (persistent_high_Val < 36) {
                    ProfileEditor.convertData(Constants.MMOLL_TO_MGDL);
                    preferences.edit().putString("persistent_high_threshold", Long.toString(Math.round(persistent_high_Val * Constants.MMOLL_TO_MGDL))).apply();
                    Profile.invalidateProfile();
                }
                if (forecast_low_Val < 36) {
                    ProfileEditor.convertData(Constants.MMOLL_TO_MGDL);
                    preferences.edit().putString("forecast_low_threshold", Long.toString(Math.round(forecast_low_Val * Constants.MMOLL_TO_MGDL))).apply();
                    Profile.invalidateProfile();
                }
                if (lowVal < 36) {
                    ProfileEditor.convertData(Constants.MMOLL_TO_MGDL);
                    preferences.edit().putString("lowValue", Long.toString(Math.round(lowVal * Constants.MMOLL_TO_MGDL))).apply();
                    preferences.edit().putString("profile_insulin_sensitivity_default", Long.toString(Math.round(default_insulin_sensitivity * Constants.MMOLL_TO_MGDL))).apply();
                    preferences.edit().putString("plus_target_range", Long.toString(Math.round(default_target_glucose * Constants.MMOLL_TO_MGDL))).apply();
                    Profile.invalidateProfile();
                }
            } else {
                if (highVal > 35) {
                    ProfileEditor.convertData(Constants.MGDL_TO_MMOLL);
                    preferences.edit().putString("highValue", JoH.qs(highVal * Constants.MGDL_TO_MMOLL, 1)).apply();
                    preferences.edit().putString("profile_insulin_sensitivity_default", JoH.qs(default_insulin_sensitivity * Constants.MGDL_TO_MMOLL, 2)).apply();
                    preferences.edit().putString("plus_target_range", JoH.qs(default_target_glucose * Constants.MGDL_TO_MMOLL, 1)).apply();
                    Profile.invalidateProfile();
                }
                if (persistent_high_Val > 35) {
                    ProfileEditor.convertData(Constants.MGDL_TO_MMOLL);
                    preferences.edit().putString("persistent_high_threshold", JoH.qs(persistent_high_Val * Constants.MGDL_TO_MMOLL, 1)).apply();
                    Profile.invalidateProfile();
                }
                if (forecast_low_Val > 35) {
                    ProfileEditor.convertData(Constants.MGDL_TO_MMOLL);
                    preferences.edit().putString("forecast_low_threshold", JoH.qs(forecast_low_Val * Constants.MGDL_TO_MMOLL, 1)).apply();
                    Profile.invalidateProfile();
                }
                if (lowVal > 35) {
                    ProfileEditor.convertData(Constants.MGDL_TO_MMOLL);
                    preferences.edit().putString("lowValue", JoH.qs(lowVal * Constants.MGDL_TO_MMOLL, 1)).apply();
                    preferences.edit().putString("profile_insulin_sensitivity_default", JoH.qs(default_insulin_sensitivity * Constants.MGDL_TO_MMOLL, 2)).apply();
                    preferences.edit().putString("plus_target_range", JoH.qs(default_target_glucose * Constants.MGDL_TO_MMOLL, 1)).apply();
                    Profile.invalidateProfile();
                }
            }
            Profile.reloadPreferences(preferences);
        } catch (Exception e) {
            Log.e(TAG, "Got excepting processing high/low value preferences: " + e.toString());
        }
    }
}
