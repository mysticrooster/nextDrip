package com.eveningoutpost.dexdrip;

import android.os.Bundle;

import com.eveningoutpost.dexdrip.ui.secondary.HealthPrivacyScreen;

/** Health Connect privacy explanation, now rendered in Compose (Track V pass 2). */
public class HealthPrivacy extends BaseAppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        HealthPrivacyScreen.installHealthPrivacy(this);
    }
}
