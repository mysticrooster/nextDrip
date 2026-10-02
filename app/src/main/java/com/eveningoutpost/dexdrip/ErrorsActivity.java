package com.eveningoutpost.dexdrip;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import androidx.preference.PreferenceManager;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.databinding.ObservableBoolean;
import androidx.databinding.ObservableField;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.ui.secondary.ErrorsScreen;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;
import com.eveningoutpost.dexdrip.utilitymodels.SendFeedBack;
import com.eveningoutpost.dexdrip.wearintegration.WatchUpdaterService;

import java.util.ArrayList;
import java.util.List;

import static com.eveningoutpost.dexdrip.Home.startWatchUpdaterService;

/**
 * Created by Emma Black on 8/3/15.
 * This is the old logs activity.
 *
 * Track V (Compose): the activity keeps the severity/auto-refresh state, the periodic refresh and
 * the log packaging; the screen renders the severity filter, list and auto-refresh controls.
 */
public class ErrorsActivity extends BaseAppCompatActivity {
    public static final String menu_name = "Errors";
    private static final String TAG = "ErrorView";

    public final ObservableBoolean cbLow = new ObservableBoolean(false);
    public final ObservableBoolean cbMid = new ObservableBoolean(true);
    public final ObservableBoolean cbHigh = new ObservableBoolean(true);
    public final ObservableBoolean cbEl = new ObservableBoolean(true);
    public final ObservableBoolean cbEh = new ObservableBoolean(true);
    public final ObservableBoolean switchAutoRefresh = new ObservableBoolean(false);

    /** Compose bridge: bumped whenever the displayed error list changes. */
    public final ObservableField<Integer> tick = new ObservableField<>(0);

    private List<UserError> errors;
    private final List<UserError> errors_tmp = new ArrayList<>();
    private boolean autoRefresh = false;
    private final Handler handler = new Handler();
    private static final boolean d = false;
    private boolean is_visible = false;
    private SharedPreferences mPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if (mPrefs.getBoolean("wear_sync", false) && mPrefs.getBoolean("sync_wear_logs", false)) {
            startWatchUpdaterService(this, WatchUpdaterService.ACTION_SYNC_LOGS, TAG);
        }

        Intent intent = getIntent();
        if (intent != null) {
            final Bundle bundle = intent.getExtras();
            if (bundle != null) {
                final String str = bundle.getString("events");
                if (str != null) {
                    cbEh.set(true);
                    cbEl.set(PersistentStore.getBoolean("events-userlowcheckbox"));
                    cbMid.set(PersistentStore.getBoolean("events-mediumcheckbox"));
                    cbHigh.set(PersistentStore.getBoolean("events-highcheckbox"));
                    cbLow.set(PersistentStore.getBoolean("events-lowcheckbox"));
                }
            }
        }

        updateErrors();
        ErrorsScreen.installErrors(this);
    }

    /** Nudges the Compose screen to recompute its state. */
    public void notifyChanged() {
        final Integer current = tick.get();
        tick.set((current == null ? 0 : current) + 1);
    }

    @Override
    public void onPause() {
        super.onPause();
        is_visible=false;
        autoRefresh=false;
    }

    @Override
    public void onResume() {
        super.onResume();
        is_visible=true;
        switchAutoRefresh.set(autoRefresh); // turn off after gone in to background
    }

    /** Severity checkbox toggled from the screen (severity 1/2/3/5/6). */
    public void setSeverity(int severity, boolean value) {
        switch (severity) {
            case 1: cbLow.set(value); break;
            case 2: cbMid.set(value); break;
            case 3: cbHigh.set(value); break;
            case 5: cbEl.set(value); break;
            case 6: cbEh.set(value); break;
        }
        updateErrors();
    }

    /** Auto-refresh switch toggled from the screen. */
    public void setAutoRefresh(boolean isChecked) {
        if (isChecked && !autoRefresh) handler.postDelayed(runnable, 1000); // start timer
        autoRefresh = isChecked;
        switchAutoRefresh.set(autoRefresh);

        if (autoRefresh) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            if (mPrefs.getBoolean("wear_sync", false) && mPrefs.getBoolean("sync_wear_logs", false)) {
                startWatchUpdaterService(getApplicationContext(), WatchUpdaterService.ACTION_SYNC_LOGS, TAG);
            }
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        notifyChanged();
    }

    public void uploadLogs() {
        StringBuilder tmp = new StringBuilder(20000);
        tmp.append("The following logs will be sent to the developers: \n\nPlease also include your email address or we will not know who they are from!\n\n");
        for (UserError item : errors) {
            tmp.append(item.toString());
            tmp.append("\n");
            if (tmp.length() > 200000) {
                JoH.static_toast(this, "Could not package up all logs, using most recent", Toast.LENGTH_LONG);
                break;
            }
        }
        startActivity(new Intent(getApplicationContext(), SendFeedBack.class).putExtra("generic_text", tmp.toString()));
    }

    private final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (autoRefresh && is_visible)
            {
                updateErrors(true);
                handler.postDelayed(this, 1000);
            }
        }
    };


    public void updateErrors() {
        updateErrors(false);
    }

    public void updateErrors(boolean from_timer) {
        List<Integer> severitiesList = new ArrayList<>();

        PersistentStore.setBoolean("events-highcheckbox", cbHigh.get());
        PersistentStore.setBoolean("events-mediumcheckbox", cbMid.get());
        PersistentStore.setBoolean("events-lowcheckbox", cbLow.get());
        PersistentStore.setBoolean("events-userlowcheckbox", cbEl.get());
        PersistentStore.setBoolean("events-userhighcheckbox", cbEh.get());

        if (cbHigh.get()) severitiesList.add(3);
        if (cbMid.get()) severitiesList.add(2);
        if (cbLow.get()) severitiesList.add(1);
        if (cbEl.get()) severitiesList.add(5);
        if (cbEh.get()) severitiesList.add(6);
        if(errors == null) {
            errors = UserError.bySeverity(severitiesList.toArray(new Integer[severitiesList.size()]));
            notifyChanged();
        } else {
            if (from_timer) {
                errors_tmp.clear();
                errors_tmp.addAll(UserError.bySeverity(severitiesList.toArray(new Integer[severitiesList.size()])));
                if (errors_tmp.size()!=errors.size())
                {
                    errors.clear();
                    errors.addAll(errors_tmp);
                    notifyChanged();
                    if (d) UserError.Log.d(TAG,"Updating list with new data");
                } else {
                    if (d) UserError.Log.d(TAG,"List sizes the same: "+errors.size());
                }
            } else {
                errors.clear();
                errors.addAll(UserError.bySeverity(severitiesList.toArray(new Integer[severitiesList.size()])));
                notifyChanged();
            }
        }
    }

    public List<UserError> getErrorsSnapshot() {
        return errors == null ? new ArrayList<>() : new ArrayList<>(errors);
    }
}
