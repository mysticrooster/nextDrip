package com.eveningoutpost.dexdrip.ui.activities;

import android.os.Bundle;

import com.eveningoutpost.dexdrip.BaseAppCompatActivity;
import com.eveningoutpost.dexdrip.ui.secondary.SelectAudioDeviceScreen;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;

/**
 * Live selection of current audio device for vehicle mode (Track V pass 1, now Compose).
 * <p>
 * The static MAC helpers are retained because {@link com.eveningoutpost.dexdrip.utils.HeadsetStateReceiver}
 * reads the stored device.
 */
public class SelectAudioDevice extends BaseAppCompatActivity {

    private static final String PREF_MAC_STORE = "vehicle-mode-audio-mac";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SelectAudioDeviceScreen.installSelectAudioDevice(this);
    }

    public static void setAudioMac(final String mac) {
        if (mac == null || mac.length() == 0) return;
        Pref.setString(PREF_MAC_STORE, mac);
    }

    public static String getAudioMac() {
        return Pref.getString(PREF_MAC_STORE, "<NOT SET>");
    }
}
