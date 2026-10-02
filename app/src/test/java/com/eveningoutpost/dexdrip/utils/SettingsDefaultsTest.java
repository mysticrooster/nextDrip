package com.eveningoutpost.dexdrip.utils;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.xdrip;

import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;

/**
 * Proves that the code-ported {@link SettingsDefaults} reproduce the exact keys, stored types and
 * values the legacy preference XMLs seeded via {@code PreferenceManager.setDefaultValues}.
 *
 * <p>The fixture was captured from the live XMLs before they were deleted.
 */
public class SettingsDefaultsTest extends RobolectricTestWithConfig {

    private SharedPreferences prefs() {
        return PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
    }

    @Before
    public void clearPrefs() {
        prefs().edit().clear().commit();
    }

    @Test
    public void codeDefaultsMatchCapturedXmlDefaults() throws Exception {
        final Map<String, String[]> expected = loadFixture();

        SettingsDefaults.apply(xdrip.getAppContext());

        final Map<String, ?> actual = prefs().getAll();

        assertThat(actual.keySet()).containsExactlyElementsIn(expected.keySet());

        for (final Map.Entry<String, String[]> entry : expected.entrySet()) {
            final String key = entry.getKey();
            final String expectedType = entry.getValue()[0];
            final String expectedValue = entry.getValue()[1];
            final Object value = actual.get(key);
            assertThat(value).isNotNull();
            assertThat(value.getClass().getSimpleName()).isEqualTo(expectedType);
            assertThat(String.valueOf(value)).isEqualTo(expectedValue);
        }
    }

    @Test
    public void applyDoesNotOverwriteExistingValues() {
        prefs().edit().putString("highValue", "222").putBoolean("bluetooth_meter_enabled", true).putInt("speech_speed", 99).commit();

        SettingsDefaults.apply(xdrip.getAppContext());

        assertThat(prefs().getString("highValue", "")).isEqualTo("222");
        assertThat(prefs().getBoolean("bluetooth_meter_enabled", false)).isTrue();
        assertThat(prefs().getInt("speech_speed", 0)).isEqualTo(99);
    }

    private static Map<String, String[]> loadFixture() throws Exception {
        final Map<String, String[]> map = new LinkedHashMap<>();
        try (InputStream is = SettingsDefaultsTest.class.getResourceAsStream("/settings_defaults_fixture.tsv")) {
            assertThat(is).isNotNull();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isEmpty()) continue;
                    final String[] parts = line.split("\t", -1);
                    assertThat(parts.length).isEqualTo(3);
                    map.put(parts[0], new String[]{parts[1], parts[2]});
                }
            }
        }
        assertThat(map).isNotEmpty();
        return map;
    }
}
