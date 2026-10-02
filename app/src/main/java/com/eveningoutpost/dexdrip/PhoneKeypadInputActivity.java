package com.eveningoutpost.dexdrip;

import static com.eveningoutpost.dexdrip.Home.startHomeWithExtra;

import android.os.Bundle;
import android.util.Log;

import androidx.databinding.ObservableField;

import com.eveningoutpost.dexdrip.insulin.Insulin;
import com.eveningoutpost.dexdrip.insulin.InsulinManager;
import com.eveningoutpost.dexdrip.insulin.MultipleInsulins;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.ui.secondary.PhoneKeypadScreen;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.wearintegration.WatchUpdaterService;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


/**
 * Adapted from WearDialer which is:
 * <p/>
 * Confirmed as in the public domain by Kartik Arora who also maintains the
 * Potato Library: http://kartikarora.me/Potato-Library
 *
 * Track V (Compose): the custom numeric keypad is replaced by a Material 3 form using the system
 * keyboard. The submit contract is preserved: the activity builds a treatment string and calls
 * {@code Home.startHomeWithExtra(this, WatchUpdaterService.WEARABLE_VOICE_PAYLOAD, mystring)}. The
 * tab semantics (insulin/carbs/blood-test/time, up to three insulin profiles) and the
 * `phone-keypad-treatment-last-tab` persistence are retained.
 */

// jamorham xdrip plus

public class PhoneKeypadInputActivity extends BaseAppCompatActivity {

    private static String currenttab = "insulin-1";
    private static final String LAST_TAB_STORE = "phone-keypad-treatment-last-tab";
    private static final String TAG = "KeypadInput";
    private static Map<String, String> values = new HashMap<String, String>();
    private String bgUnits;
    private final Insulin insulinProfile1;
    private final Insulin insulinProfile2;
    private final Insulin insulinProfile3;

    private final boolean multipleInsulins = MultipleInsulins.isEnabled();

    /** Compose bridge. */
    public final ObservableField<Integer> tick = new ObservableField<>(0);

    public PhoneKeypadInputActivity() {
        insulinProfile1 = InsulinManager.getProfile(0);
        insulinProfile2 = InsulinManager.getProfile(1);
        insulinProfile3 = InsulinManager.getProfile(2);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Pref.getString("units", "mgdl").equals("mgdl")) {
            bgUnits = "mg/dl";
        } else {
            bgUnits = "mmol/l";
        }
        PhoneKeypadScreen.installPhoneKeypad(this);
    }

    public void notifyChanged() {
        final Integer current = tick.get();
        tick.set((current == null ? 0 : current) + 1);
    }

    public static void resetValues() {
        values = new HashMap<String, String>();
    }

    public static String getValue(String tab) {
        if (values.containsKey(tab)) {
            return values.get(tab);
        } else {
            values.put(tab, "");
            return values.get(tab);
        }
    }

    private static String appendValue(String tab, String append) {
        values.put(tab, getValue(tab) + append);
        return values.get(tab);
    }

    public boolean isMultipleInsulins() {
        return multipleInsulins;
    }

    public String getCurrentTab() {
        return currenttab;
    }

    public void setCurrentTab(String tab) {
        currenttab = snapTab(tab);
        notifyChanged();
    }

    private String snapTab(String tab) {
        if (!multipleInsulins && ("insulin-2".equals(tab) || "insulin-3".equals(tab))) {
            return "insulin-1";
        }
        if ("insulin-2".equals(tab) && insulinProfile2 == null) return "insulin-1";
        if ("insulin-3".equals(tab) && insulinProfile3 == null) return "insulin-1";
        return tab;
    }

    public String getCurrentValue() {
        return getValue(currenttab);
    }

    public String getSuffix() {
        switch (currenttab.split("-")[0]) {
            case "insulin":
                String profile = "";
                if (multipleInsulins && currenttab.contains("-")) {
                    final Insulin insulin = insulinProfile(Integer.parseInt(currenttab.split("-")[1]));
                    if (insulin != null) profile = " " + insulin.getName();
                }
                return " " + getString(R.string.units) + profile;
            case "carbs":
                return " g " + getString(R.string.carbs);
            case "bloodtest":
                return " " + bgUnits;
            case "time":
                return " " + getString(R.string.when);
        }
        return "";
    }

    private Insulin insulinProfile(int index) {
        switch (index) {
            case 1: return insulinProfile1;
            case 2: return insulinProfile2;
            case 3: return insulinProfile3;
        }
        return null;
    }

    /** Profile name for the insulin profile selector (1..3), or null if absent. */
    public String getInsulinProfileName(int index) {
        final Insulin insulin = insulinProfile(index);
        return insulin == null ? null : insulin.getName();
    }

    public int getActiveInsulinProfile() {
        if (currenttab.contains("-")) {
            try {
                return Integer.parseInt(currenttab.split("-")[1]);
            } catch (NumberFormatException e) {
                return 1;
            }
        }
        return 1;
    }

    public void selectInsulinProfile(int index) {
        setCurrentTab("insulin-" + index);
    }

    /** Sets the value of the current tab from the system-keyboard text field. */
    public void setCurrentValue(String input) {
        String filtered = input == null ? "" : input.replaceAll("[^0-9.]", "");
        final int dot = filtered.indexOf('.');
        if (dot >= 0) {
            filtered = filtered.substring(0, dot + 1) + filtered.substring(dot + 1).replace(".", "");
        }
        if (filtered.length() > 6) filtered = filtered.substring(0, 6);
        values.put(currenttab, filtered);
        notifyChanged();
    }

    public void clearCurrentValue() {
        values.put(currenttab, "");
        notifyChanged();
    }

    public boolean isNonzeroValueInTab(String tab)
    {
        try
        {
            return (0 != Double.parseDouble(getValue(tab)));
        }
        catch(NumberFormatException e) { return false; }
    }

    public boolean hasAnyValue() {
        return isNonzeroValueInTab("bloodtest")
                || isNonzeroValueInTab("carbs")
                || isNonzeroValueInTab("insulin-1")
                || isNonzeroValueInTab("insulin-2")
                || isNonzeroValueInTab("insulin-3");
    }

    public boolean isInvalidTime() {
        String timeValue = getValue("time");
        if (timeValue.length() == 0) return false; // No time value has been entered.  Then, there is nothing to reject.

        // Normalize HHmm to HH.mm for validation
        if (!timeValue.contains(".") && timeValue.length() >= 3) {
            timeValue = timeValue.substring(0, timeValue.length() - 2) + "." + timeValue.substring(timeValue.length() - 2);
        }

        // Ensure time follows the [H]H.mm format strictly.
        String[] parts = timeValue.split("\\.");
        if (parts.length != 2 || parts[0].isEmpty() || parts[1].length() != 2) return true;

        try {
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            // Validate ranges: 0-23 hours and 0-59 minutes
            return (hours < 0 || hours > 23 || minutes < 0 || minutes > 59);
        } catch (NumberFormatException e) {
            return true;
        }
    }

    public void submitAll() {

        boolean nonzeroBloodValue = isNonzeroValueInTab("bloodtest");
        boolean nonzeroCarbsValue = isNonzeroValueInTab("carbs");
        boolean nonzeroInsulin1Value = isNonzeroValueInTab("insulin-1");
        boolean nonzeroInsulin2Value = isNonzeroValueInTab("insulin-2");
        boolean nonzeroInsulin3Value = isNonzeroValueInTab("insulin-3");

        // The green tick is clickable even when it's hidden, so we might get here
        // without valid data.  Ignore the click if input is incomplete
        if (!nonzeroBloodValue && !nonzeroCarbsValue && !nonzeroInsulin1Value && !nonzeroInsulin2Value && !nonzeroInsulin3Value) {
            Log.d(TAG, "All zero values in tabs - not processing button click");
            return;
        }

        if (isInvalidTime()) {
            return;
        }


        // Add the dot to the time if it is missing
        String timeValue = getValue("time");
        if (timeValue.length() > 2 && !timeValue.contains(".")) {
            timeValue = timeValue.substring(0, timeValue.length() - 2) + "." + timeValue.substring(timeValue.length() - 2);
        }

        String mystring = "";
        double units = 0;
        final DecimalFormat df = new DecimalFormat("0.0#", new DecimalFormatSymbols(Locale.ENGLISH));
        if (timeValue.length() > 0) mystring += timeValue + " time ";
        if (nonzeroBloodValue) mystring += getValue("bloodtest") + " blood ";
        if (nonzeroCarbsValue) {
            String carbValue = String.valueOf(JoH.roundDouble(JoH.tolerantParseDouble(getValue("carbs")), 2)); // Let's round the entered carb value to 2 decimal points
            mystring += carbValue + " g carbs ";
        }
        if (nonzeroInsulin1Value && (insulinProfile1 != null))
        {
            double d = Double.parseDouble(getValue("insulin-1"));
            if (multipleInsulins) {
                mystring += df.format(d) + " " + insulinProfile1.getName() + " ";
            }
            units += d;
        }
        if (multipleInsulins) {
            if (nonzeroInsulin2Value && (insulinProfile2 != null)) {
                double d = Double.parseDouble(getValue("insulin-2"));
                mystring += df.format(d) + " " + insulinProfile2.getName() + " ";
                units += d;
            }
            if (nonzeroInsulin3Value && (insulinProfile3 != null)) {
                double d = Double.parseDouble(getValue("insulin-3"));
                mystring += df.format(d) + " " + insulinProfile3.getName() + " ";
                units += d;
            }
        }
        if (units > 0)
            mystring += df.format(units) + " units ";

        if (mystring.length() > 1) {
            resetValues();
            startHomeWithExtra(this, WatchUpdaterService.WEARABLE_VOICE_PAYLOAD, mystring); // send data to home directly
            finish();
        }
    }

    public void startSpeechRecognition() {
        startHomeWithExtra(getApplicationContext(), Home.START_SPEECH_RECOGNITION, "ok");
        finish();
    }

    public void startTextRecognition() {
        startHomeWithExtra(getApplicationContext(), Home.START_TEXT_RECOGNITION, "ok");
        finish();
    }

    @Override
    protected void onResume() {
        final String savedtab = PersistentStore.getString(LAST_TAB_STORE);
        if (savedtab.length() > 0) currenttab = savedtab;
        currenttab = snapTab(currenttab);
        notifyChanged();
        super.onResume();
    }

    @Override
    protected void onPause() {
        PersistentStore.setString(LAST_TAB_STORE, currenttab);
        super.onPause();
    }
}
