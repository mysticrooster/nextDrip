package com.eveningoutpost.dexdrip.deposit;

import android.os.Bundle;

import com.eveningoutpost.dexdrip.BaseAppCompatActivity;

/** Web Deposit UI, now rendered in Compose (Track V pass 1). */
public class DepositActivity extends BaseAppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DepositScreen.installDeposit(this);
    }
}
