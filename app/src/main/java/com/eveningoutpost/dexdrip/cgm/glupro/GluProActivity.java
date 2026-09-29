package com.eveningoutpost.dexdrip.cgm.glupro;

import android.os.Bundle;

import com.eveningoutpost.dexdrip.BaseAppCompatActivity;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.ui.secondary.GluProScreen;
import com.eveningoutpost.dexdrip.utilitymodels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.utils.LocationHelper;

import lwld.glucose.profile.iface.State;

/**
 * JamOrHam
 * <p>
 * Glucose Profile device selection activity (Track V pass 3, now Compose).
 */
public class GluProActivity extends BaseAppCompatActivity {

    private static final String TAG = GluProActivity.class.getSimpleName();

    public final ViewModel viewModel = ViewModelProvider.get();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel.setActivityCallback(this::finish);
        GluProScreen.installGluPro(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            if ((LocationHelper.requestLocationForBluetooth(this))
                    && viewModel.lastState.get() == State.INSUFFICIENT_PERMISSIONS) {
                UserError.Log.d(TAG, "Got permission so restarting service");
                CollectionServiceStarter.restartCollectionServiceBackground();
            }
        } catch (Exception e) {
            UserError.Log.e(TAG, "Exception requesting location: " + e.getMessage());
        }
    }

    @Override
    public void onDestroy() {
        viewModel.setActivityCallback(null);
        super.onDestroy();
    }
}
