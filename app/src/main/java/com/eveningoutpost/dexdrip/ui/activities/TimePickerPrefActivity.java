package com.eveningoutpost.dexdrip.ui.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.eveningoutpost.dexdrip.ui.secondary.TimePickerPrefScreen;

/**
 * Start via an intent containing the pref-name for a string preference which stores time of day in
 * seconds 0-86399. The picker is now a Compose Material 3 dialog; it shows the current value and
 * saves the result automatically (Track V pass 1).
 */
public class TimePickerPrefActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Intent intent = getIntent();
        final String prefName = intent == null ? null : intent.getStringExtra("pref-name");
        if (prefName == null) {
            finish();
            return;
        }
        TimePickerPrefScreen.installTimePickerPref(this, prefName);
    }
}
