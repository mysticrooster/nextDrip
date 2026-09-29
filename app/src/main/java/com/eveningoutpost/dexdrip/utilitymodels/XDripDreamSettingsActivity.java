package com.eveningoutpost.dexdrip.utilitymodels;

import android.os.Bundle;

import com.eveningoutpost.dexdrip.BaseAppCompatActivity;
import com.eveningoutpost.dexdrip.ui.secondary.DreamSettingsScreen;

/** Daydream/screensaver options, now rendered in Compose (Track V pass 2). */
public class XDripDreamSettingsActivity extends BaseAppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DreamSettingsScreen.installDreamSettings(this);
    }
}
