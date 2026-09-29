package com.eveningoutpost.dexdrip;

import android.os.Bundle;

import com.eveningoutpost.dexdrip.ui.secondary.CalibrationOverrideScreen;

/** Override the previous calibration, now rendered in Compose (Track V pass 2). */
public class CalibrationOverride extends BaseAppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CalibrationOverrideScreen.installCalibrationOverride(this);
    }
}
