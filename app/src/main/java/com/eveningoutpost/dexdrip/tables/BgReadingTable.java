package com.eveningoutpost.dexdrip.tables;

import android.os.Bundle;

import androidx.databinding.ObservableField;

import com.eveningoutpost.dexdrip.BaseAppCompatActivity;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.ui.secondary.BgReadingTableScreen;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import lombok.val;


/**
 * Track V (Data tables, Compose) — the BG raw-data table, reachable from the settings "Your Data"
 * rows (gated by `show_data_tables`). The activity loads the readings and keeps the flag-for-stats
 * side effect; the drawer shell is dropped in favour of {@code SecondaryScreen}.
 */
public class BgReadingTable extends BaseAppCompatActivity {
    private List<BgReading> latest = new ArrayList<>();
    private int missing;
    private int backfilled;
    private int total;

    /** Compose bridge: bumped whenever the readings or subtitle change. */
    public final ObservableField<Integer> tick = new ObservableField<>(0);
    public final ObservableField<String> subtitle = new ObservableField<>("");

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BgReadingTableScreen.installBgReadingTable(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getData();
    }

    public void notifyChanged() {
        final Integer current = tick.get();
        tick.set((current == null ? 0 : current) + 1);
    }

    private void getData() {
        try {
            latest = BgReading.latest(5000);
            if (latest == null) latest = new ArrayList<>();
            parseDataForStats(latest);
            if (total > 0) {
                subtitle.set(String.format(Locale.getDefault(), "%d in 24h, bf:%d%% mis:%d", total, ((backfilled * 100) / total), missing));
            }
            notifyChanged();
        } catch (NullPointerException e) {
            //
        }
    }

    private void parseDataForStats(List<BgReading> list) {
        long cutoff = JoH.tsl() - Constants.DAY_IN_MS;
        long oldest = 0;
        total = 0;
        backfilled = 0;
        missing = 0;
        for (val item : list) {
            if (item.timestamp < cutoff) break;
            oldest = item.timestamp;
            total++;
            if (item.source_info != null && item.source_info.contains("Backfill")) {
                backfilled++;
            }
        }

        if (total > 0) {
            val expectedReadings = (JoH.tsl() - oldest) / DexCollectionType.getCurrentSamplePeriod();
            missing = (int) (expectedReadings - total);
        }

    }

    /** Flag/unflag a reading for statistics (long-press action). */
    public void flagReading(BgReading bgReading, boolean ignore) {
        if (bgReading == null) return;
        bgReading.ignoreForStats = ignore;
        bgReading.save();
        if (Pref.getBooleanDefaultFalse("wear_sync")) {
            BgReading.pushBgReadingSyncToWatch(bgReading, false);
        }
        notifyChanged();
    }

    public List<BgReading> getReadingsSnapshot() {
        return new ArrayList<>(latest);
    }
}
