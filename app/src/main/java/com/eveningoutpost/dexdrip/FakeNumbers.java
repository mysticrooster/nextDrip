package com.eveningoutpost.dexdrip;

import android.os.Bundle;

import com.eveningoutpost.dexdrip.ui.secondary.FakeNumbersScreen;

/** Developer test screen, now rendered in Compose (Track V pass 2). */
public class FakeNumbers extends BaseAppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FakeNumbersScreen.installFakeNumbers(this);
    }
}
