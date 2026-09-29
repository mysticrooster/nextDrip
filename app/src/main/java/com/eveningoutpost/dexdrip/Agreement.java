package com.eveningoutpost.dexdrip;

import android.os.Bundle;

import com.eveningoutpost.dexdrip.ui.secondary.AgreementScreen;

/** First-run important warning gate, now rendered in Compose (Track V pass 2). */
public class Agreement extends BaseAppCompatActivity {

    public static final String prefmarker = "warning_agreed_to";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AgreementScreen.installAgreement(this);
    }
}
