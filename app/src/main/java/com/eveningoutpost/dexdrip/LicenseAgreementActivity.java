package com.eveningoutpost.dexdrip;

import android.os.Bundle;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.ui.secondary.LicenseAgreementScreen;

/** EULA gate, now rendered in Compose (Track V pass 1). */
public class LicenseAgreementActivity extends BaseAppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            LicenseAgreementScreen.installLicenseAgreement(this);
        } catch (UnsupportedOperationException e) {
            JoH.static_toast_long("Unable to display license agreement? blocked by user? Cannot continue");
            finish();
        }
    }
}
