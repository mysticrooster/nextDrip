package com.eveningoutpost.dexdrip.insulin;

import android.os.Bundle;

import com.eveningoutpost.dexdrip.BaseAppCompatActivity;
import com.eveningoutpost.dexdrip.ui.secondary.InsulinProfileScreen;

/** Config insulin profiles, now rendered in Compose (Track V pass 1). */
public class InsulinProfileEditor extends BaseAppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        InsulinProfileScreen.installInsulinProfileEditor(this);
    }
}
