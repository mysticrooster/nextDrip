package com.eveningoutpost.dexdrip;

import android.os.Bundle;

import com.eveningoutpost.dexdrip.ui.secondary.DoubleCalibrationScreen;

/** Initial one/two finger-prick calibration, now rendered in Compose (Track V pass 2). */
public class DoubleCalibrationActivity extends BaseAppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DoubleCalibrationScreen.installDoubleCalibration(this);
    }
}
