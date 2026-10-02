package com.eveningoutpost.dexdrip.utils;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;

import org.junit.Before;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;

/**
 * Behavioural tests for the non-UI settings API extracted from the retired legacy
 * {@code Preferences} activity.
 */
public class SettingsSupportTest extends RobolectricTestWithConfig {

    @Before
    public void clearPrefs() {
        Pref.getInstance().edit().clear().commit();
    }

    @Test
    public void isNumeric_matchesLegacySemantics() {
        assertThat(SettingsSupport.isNumeric("123")).isTrue();
        assertThat(SettingsSupport.isNumeric("1.5")).isTrue();
        assertThat(SettingsSupport.isNumeric("1e3")).isTrue();
        assertThat(SettingsSupport.isNumeric("NaN")).isTrue();
        assertThat(SettingsSupport.isNumeric("abc")).isFalse();
        assertThat(SettingsSupport.isNumeric("")).isFalse();
        assertThat(SettingsSupport.isNumeric("3.5 4.5")).isFalse();
    }

    @Test
    public void applyPrefSettingRange_clampsAboveMaxMgdl() {
        Pref.setString("units", "mgdl");
        Pref.setString("test_range_key", "500");

        SettingsSupport.applyPrefSettingRange("test_range_key", "170", SettingsSupport.MIN_GLUCOSE_INPUT, SettingsSupport.MAX_GLUCOSE_INPUT);

        assertThat(Pref.getString("test_range_key", "")).isEqualTo("400.0");
    }

    @Test
    public void applyPrefSettingRange_clampsBelowMinMgdl() {
        Pref.setString("units", "mgdl");
        Pref.setString("test_range_key", "10");

        SettingsSupport.applyPrefSettingRange("test_range_key", "170", SettingsSupport.MIN_GLUCOSE_INPUT, SettingsSupport.MAX_GLUCOSE_INPUT);

        assertThat(Pref.getString("test_range_key", "")).isEqualTo("40.0");
    }

    @Test
    public void applyPrefSettingRange_leavesInRangeValueUntouched() {
        Pref.setString("units", "mgdl");
        Pref.setString("test_range_key", "200");

        SettingsSupport.applyPrefSettingRange("test_range_key", "170", SettingsSupport.MIN_GLUCOSE_INPUT, SettingsSupport.MAX_GLUCOSE_INPUT);

        assertThat(Pref.getString("test_range_key", "")).isEqualTo("200");
    }

    @Test
    public void applyPrefSettingRange_convertsMgdlDefaultForMmolUser() {
        Pref.setString("units", "mmol");
        // Setting equals the mg/dL default but is being read as mmol -> converted to mmol.
        Pref.setString("test_range_key", "170");

        SettingsSupport.applyPrefSettingRange("test_range_key", "170", SettingsSupport.MIN_GLUCOSE_INPUT, SettingsSupport.MAX_GLUCOSE_INPUT);

        assertThat(Pref.getString("test_range_key", "")).isEqualTo(JoH.qs(170 * Constants.MGDL_TO_MMOLL, 1));
    }

    @Test
    public void handleUnitsChange_convertsMgdlToMmol() {
        Pref.setString("units", "mgdl");
        Pref.setString("saved_profile_list_json", "[]");
        Pref.setString("highValue", "180");
        Pref.setString("lowValue", "80");
        Pref.setString("profile_insulin_sensitivity_default", "54");
        Pref.setString("plus_target_range", "100");
        Pref.setString("persistent_high_threshold", "170");
        Pref.setString("forecast_low_threshold", "70");

        SettingsSupport.handleUnitsChange("mmol");

        assertThat(Pref.getString("highValue", "")).isEqualTo(JoH.qs(180 * Constants.MGDL_TO_MMOLL, 1));
        assertThat(Pref.getString("lowValue", "")).isEqualTo(JoH.qs(80 * Constants.MGDL_TO_MMOLL, 1));
        assertThat(Pref.getString("profile_insulin_sensitivity_default", "")).isEqualTo(JoH.qs(54 * Constants.MGDL_TO_MMOLL, 2));
        assertThat(Pref.getString("plus_target_range", "")).isEqualTo(JoH.qs(100 * Constants.MGDL_TO_MMOLL, 1));
        assertThat(Pref.getString("persistent_high_threshold", "")).isEqualTo(JoH.qs(170 * Constants.MGDL_TO_MMOLL, 1));
        assertThat(Pref.getString("forecast_low_threshold", "")).isEqualTo(JoH.qs(70 * Constants.MGDL_TO_MMOLL, 1));
    }

    @Test
    public void getMapKeysString_returnsSortedKeysOnePerLine() {
        final Map<String, Object> map = new LinkedHashMap<>();
        map.put("zebra", 1);
        map.put("alpha", 2);
        map.put("middle", 3);

        assertThat(SettingsSupport.getMapKeysString(map)).isEqualTo("alpha\nmiddle\nzebra\n");
    }
}
