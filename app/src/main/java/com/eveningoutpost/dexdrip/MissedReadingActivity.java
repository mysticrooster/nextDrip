package com.eveningoutpost.dexdrip;

import android.os.Bundle;

import com.eveningoutpost.dexdrip.ui.secondary.MissedReadingScreen;

/** Missed-reading alert settings, now rendered in Compose (Track V pass 1). */
public class MissedReadingActivity extends BaseAppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MissedReadingScreen.installMissedReading(this);
    }
}
