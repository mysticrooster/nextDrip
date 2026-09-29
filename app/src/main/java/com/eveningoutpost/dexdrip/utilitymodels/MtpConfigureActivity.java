package com.eveningoutpost.dexdrip.utilitymodels;

import android.os.Bundle;

import com.eveningoutpost.dexdrip.BaseAppCompatActivity;
import com.eveningoutpost.dexdrip.ui.secondary.MtpConfigureScreen;

/** USB-OTG MTP configuration assistant, now rendered in Compose (Track V pass 3). */
public class MtpConfigureActivity extends BaseAppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MtpConfigureScreen.installMtpConfigure(this);
    }
}
