package com.eveningoutpost.dexdrip;

import android.os.Bundle;

import com.eveningoutpost.dexdrip.ui.secondary.CalibrationCheckInScreen;

/** Dexcom calibration check-in request, now rendered in Compose (Track V pass 2). */
public class CalibrationCheckInActivity extends BaseAppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CalibrationCheckInScreen.installCalibrationCheckIn(this);
    }
}
