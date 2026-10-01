package com.eveningoutpost.dexdrip;

import android.content.Intent;
import android.os.Bundle;

import com.eveningoutpost.dexdrip.g5model.FirmwareCapability;
import com.eveningoutpost.dexdrip.g5model.Ob1G5StateMachine;
import com.eveningoutpost.dexdrip.models.Calibration;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.Sensor;
import com.eveningoutpost.dexdrip.models.Treatments;
import com.eveningoutpost.dexdrip.ui.secondary.StopSensorScreen;
import com.eveningoutpost.dexdrip.utilitymodels.AlertPlayer;
import com.eveningoutpost.dexdrip.utilitymodels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.utilitymodels.Inevitable;
import com.eveningoutpost.dexdrip.utilitymodels.NanoStatus;
import com.eveningoutpost.dexdrip.calibrations.PluggableCalibration;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;

import lombok.val;

import static com.eveningoutpost.dexdrip.g5model.Ob1G5StateMachine.shortTxId;
import static com.eveningoutpost.dexdrip.services.Ob1G5CollectionService.getTransmitterID;
import static com.eveningoutpost.dexdrip.xdrip.gs;

/**
 * Stop the active sensor (Track V pass 5, now Compose). The activity keeps the stop side effects
 * and the G6/G7 aware confirmation copy; the screen renders the actions and confirmations.
 */
public class StopSensor extends BaseAppCompatActivity {

    public final ViewModel viewModel = new ViewModel();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Sensor.isActive()) {
            Intent intent = new Intent(this, StartNewSensor.class);
            startActivity(intent);
            finish();
        } else {
            StopSensorScreen.installStopSensor(this);
        }
    }

    /** Confirmation copy, adjusted for collectors that cannot simply restart the sensor. */
    public String stopConfirmMessage() {
        String confirm = gs(R.string.are_you_sure);
        if (!viewModel.resettableCals()) { // Dexcom G6 Firefly or G7
            confirm = gs(R.string.sensor_stop_confirm_norestart);
            if (shortTxId()) { // Dexcom G7
                confirm = gs(R.string.sensor_stop_confirm_really_norestart);
            }
        }
        return confirm;
    }

    public void confirmStop() {
        stop();
        JoH.startActivity(Home.class);
        finish();
    }

    public void confirmResetCalibrations() {
        Calibration.invalidateAllForSensor();
        finish();
    }

    public synchronized static void stop() {
        Sensor.stopSensor();
        Inevitable.task("stop-sensor",1000, Sensor::stopSensor);
        AlertPlayer.getPlayer().stopAlert(xdrip.getAppContext(), true, false);

        JoH.static_toast_long(gs(R.string.sensor_stopped));
        JoH.clearCache();
        LibreAlarmReceiver.clearSensorStats();
        PluggableCalibration.invalidateAllCaches();

        Treatments.sensorStop(null, "Stopped by xDrip");

        Ob1G5StateMachine.stopSensor();
        if (JoH.pratelimit("dex-stop-start", 15)) {
            //
        }

        CollectionServiceStarter.restartCollectionServiceBackground();
        Home.staticRefreshBGCharts();
        NanoStatus.keepFollowerUpdated(false);
    }

    public class ViewModel {
        // This is false only for Dexcom G6 Firefly and G7 as of December 2023.
        // These devices have two common characteristics:
        // 1- xDrip does not maintain a calibration graph for them.  Therefore, resetting calibrations has no impact.
        // 2- They cannot be restarted easily or they cannot be restarted at all.
        // TODO this could be moved to another utility class if the same logic is used elsewhere
        public boolean resettableCals() { // Used on the stop sensor menu.
            return DexCollectionType.getDexCollectionType() != DexCollectionType.DexcomG5
                    || !Pref.getBooleanDefaultFalse("using_g6")
                    || !FirmwareCapability.isTransmitterRawIncapable(getTransmitterID());
        }
    }
}
