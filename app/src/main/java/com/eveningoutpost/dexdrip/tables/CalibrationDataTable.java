package com.eveningoutpost.dexdrip.tables;

import android.os.Bundle;

import androidx.databinding.ObservableField;

import com.eveningoutpost.dexdrip.BaseAppCompatActivity;
import com.eveningoutpost.dexdrip.models.Calibration;
import com.eveningoutpost.dexdrip.ui.secondary.CalibrationDataTableScreen;

import java.util.ArrayList;
import java.util.List;


/**
 * Track V (Data tables, Compose) — the calibration data table, reachable from the settings
 * "Your Data" rows (gated by `show_data_tables`). The activity loads the calibrations and keeps the
 * disable-calibration side effect; the drawer shell is dropped in favour of {@code SecondaryScreen}.
 */
public class CalibrationDataTable extends BaseAppCompatActivity {
    private List<Calibration> latest = new ArrayList<>();

    /** Compose bridge: bumped whenever the calibrations change. */
    public final ObservableField<Integer> tick = new ObservableField<>(0);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CalibrationDataTableScreen.installCalibrationDataTable(this);
    }

    @Override
    protected void onResume(){
        super.onResume();
        getData();
    }

    public void notifyChanged() {
        final Integer current = tick.get();
        tick.set((current == null ? 0 : current) + 1);
    }

    private void getData() {
        final List<Calibration> data = Calibration.latest(50);
        latest = data == null ? new ArrayList<>() : data;
        notifyChanged();
    }

    /** Disable a calibration (long-press action). */
    public void disableCalibration(Calibration calibration) {
        if (calibration == null) return;
        calibration.clear_byuuid(calibration.uuid, false);
        notifyChanged();
    }

    public List<Calibration> getCalibrationsSnapshot() {
        return new ArrayList<>(latest);
    }
}
