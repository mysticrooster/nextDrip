package com.eveningoutpost.dexdrip;

import android.os.Bundle;

import com.eveningoutpost.dexdrip.ui.secondary.NightscoutBackfillScreen;

/** Nightscout backfill request screen, now rendered in Compose (Track V pass 1). */
public class NightscoutBackfillActivity extends BaseAppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NightscoutBackfillScreen.installNightscoutBackfill(this);
    }
}
