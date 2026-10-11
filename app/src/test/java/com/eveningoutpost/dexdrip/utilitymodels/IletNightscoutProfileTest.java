package com.eveningoutpost.dexdrip.utilitymodels;

import static com.google.common.truth.Truth.assertThat;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

/**
 * Unit tests for the pure Nightscout iLet 0 U/hr profile assembly. Verifies that
 * activating the synthetic profile preserves the user's existing store entries and
 * therapy fields (carbratio/sens/target_*) and only overrides the basal schedule.
 */
public class IletNightscoutProfileTest extends RobolectricTestWithConfig {

    private static final String NAME = "iLet 0 U/hr";

    @Test
    public void buildDocument_clonesExistingTherapyFields_andSetsNewDefault() throws Exception {
        final JSONArray basal = new JSONArray().put(new JSONObject()
                .put("time", "00:00").put("value", 1.2).put("timeAsSeconds", 0));
        final JSONArray carb = new JSONArray().put(new JSONObject()
                .put("time", "00:00").put("value", 12).put("timeAsSeconds", 0));
        final JSONArray target = new JSONArray().put(new JSONObject()
                .put("time", "00:00").put("value", 100).put("timeAsSeconds", 0));
        final JSONObject existingEntry = new JSONObject()
                .put("basal", basal)
                .put("carbratio", carb)
                .put("sens", carb)
                .put("target_low", target)
                .put("target_high", target)
                .put("units", "mmol")
                .put("timezone", "Europe/Paris")
                .put("dia", 4);
        final JSONObject store = new JSONObject()
                .put("Default", existingEntry)
                .put("Other", new JSONObject().put("basal", basal));
        final JSONObject doc = new JSONObject()
                .put("defaultProfile", "Default")
                .put("store", store);

        final JSONObject result = NightscoutUploader.buildZeroBasalProfileDocument(doc.toString());

        // New profile is the default, existing entries are preserved.
        assertThat(result.getString("defaultProfile")).isEqualTo(NAME);
        final JSONObject resultStore = result.getJSONObject("store");
        assertThat(resultStore.has("Default")).isTrue();
        assertThat(resultStore.has("Other")).isTrue();
        assertThat(resultStore.has(NAME)).isTrue();

        // Therapy fields are cloned from the prior default profile.
        final JSONObject entry = resultStore.getJSONObject(NAME);
        assertThat(entry.getString("timezone")).isEqualTo("Europe/Paris");
        assertThat(entry.getString("units")).isEqualTo("mmol");
        assertThat(entry.getInt("dia")).isEqualTo(4);
        assertThat(entry.getJSONArray("carbratio").getJSONObject(0).getInt("value")).isEqualTo(12);
        assertThat(entry.getJSONArray("sens").getJSONObject(0).getInt("value")).isEqualTo(12);
        assertThat(entry.getJSONArray("target_low").getJSONObject(0).getInt("value")).isEqualTo(100);
        assertThat(entry.getJSONArray("target_high").getJSONObject(0).getInt("value")).isEqualTo(100);

        // Only the basal schedule is overridden, to a single 0 U/hr entry.
        final JSONArray newBasal = entry.getJSONArray("basal");
        assertThat(newBasal.length()).isEqualTo(1);
        assertThat(newBasal.getJSONObject(0).getInt("value")).isEqualTo(0);
        assertThat(newBasal.getJSONObject(0).getString("time")).isEqualTo("00:00");
    }

    @Test
    public void buildDocument_withoutExistingProfile_isBasalOnly() throws Exception {
        final JSONObject result = NightscoutUploader.buildZeroBasalProfileDocument(null);

        assertThat(result.getString("defaultProfile")).isEqualTo(NAME);
        final JSONObject entry = result.getJSONObject("store").getJSONObject(NAME);
        final JSONArray basal = entry.getJSONArray("basal");
        assertThat(basal.length()).isEqualTo(1);
        assertThat(basal.getJSONObject(0).getInt("value")).isEqualTo(0);
        // No fabricated therapy values.
        assertThat(entry.has("carbratio")).isFalse();
        assertThat(entry.has("sens")).isFalse();
        assertThat(entry.has("target_low")).isFalse();
        assertThat(entry.has("target_high")).isFalse();
    }

    @Test
    public void firstProfileDocument_readsFirstArrayElement() throws Exception {
        final JSONArray list = new JSONArray()
                .put(new JSONObject().put("defaultProfile", "A"))
                .put(new JSONObject().put("defaultProfile", "B"));

        final String first = NightscoutUploader.firstProfileDocument(list.toString());

        assertThat(first).isNotNull();
        assertThat(new JSONObject(first).getString("defaultProfile")).isEqualTo("A");
    }

    @Test
    public void firstProfileDocument_readsSingleObject() throws Exception {
        final String doc = new JSONObject().put("defaultProfile", "Solo").toString();
        assertThat(NightscoutUploader.firstProfileDocument(doc)).isNotNull();
    }

    @Test
    public void firstProfileDocument_handlesEmptyAndNull() throws Exception {
        assertThat(NightscoutUploader.firstProfileDocument(null)).isNull();
        assertThat(NightscoutUploader.firstProfileDocument("[]")).isNull();
        assertThat(NightscoutUploader.firstProfileDocument("")).isNull();
    }
}
