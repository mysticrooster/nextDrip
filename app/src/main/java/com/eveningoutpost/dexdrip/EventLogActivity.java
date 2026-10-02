package com.eveningoutpost.dexdrip;

import android.content.Intent;

import androidx.databinding.ObservableArrayList;
import androidx.databinding.ObservableBoolean;
import androidx.databinding.ObservableField;
import androidx.databinding.ObservableList;
import android.os.Bundle;

import android.util.SparseBooleanArray;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.ui.secondary.EventLogScreen;
import com.eveningoutpost.dexdrip.utilitymodels.Inevitable;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utilitymodels.SaveLogs;
import com.eveningoutpost.dexdrip.utilitymodels.SendFeedBack;
import com.eveningoutpost.dexdrip.utils.ExtensionMethods;
import com.eveningoutpost.dexdrip.wearintegration.WatchUpdaterService;

import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.experimental.ExtensionMethod;
import me.tatarka.bindingcollectionadapter2.collections.MergeObservableList;

import static com.eveningoutpost.dexdrip.Home.startWatchUpdaterService;
import static com.eveningoutpost.dexdrip.utils.DexCollectionType.getBestCollectorHardwareName;

/*
 * New style event log viewer
 *
 * Created by jamorham 24/03/2018
 *
 * Track V (Logs, Compose): the activity keeps the streaming refresh, severity/search filters, log
 * packing and wear log sync; the screen renders the filter row, list and actions via the retained
 * [ViewModel] observable lists.
 */
@ExtensionMethod({java.util.Arrays.class, ExtensionMethods.class})
public class EventLogActivity extends BaseAppCompatActivity {

    private static final List<Integer> severitiesList = new ArrayList<>();
    private static final boolean D = false;
    private static final String TAG = EventLogActivity.class.getSimpleName();
    private static final String PREF_SEVERITY_SELECTION = "event-log-severity-enabled-";
    private static final String PREF_LAST_SEARCH = "event-log-last-search-";
    private static int MAX_LOG_PACKAGE_SIZE = 200000;

    static {
        severitiesList.add(1);
        severitiesList.add(2);
        severitiesList.add(3);
        severitiesList.add(5);
        severitiesList.add(6);
    }

    public final ViewModel model = new ViewModel();

    /** Compose bridge: bumped whenever the visible item set or filter changes. */
    public final ObservableField<Integer> tick = new ObservableField<>(0);
    /** Compose bridge: whether the list is scrolled to the top (drives the TOP button). */
    public final ObservableBoolean listAtTop = new ObservableBoolean(true);
    /** Compose bridge: increment to ask the screen to scroll to the top. */
    public final ObservableField<Integer> scrollToTopRequest = new ObservableField<>(0);

    private volatile boolean runRefresh = false;
    private volatile long highest_id = 0;
    private volatile boolean loading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        refreshData();
        getOlderData();
        EventLogScreen.installEventLog(this);
    }

    public void notifyChanged() {
        final Integer current = tick.get();
        tick.set((current == null ? 0 : current) + 1);
    }

    // check if should stream wear logs
    private boolean shouldStreamWearLogs() {
        return Pref.getBooleanDefaultFalse("wear_sync") && Pref.getBooleanDefaultFalse("sync_wear_logs");
    }

    // ask for wear updated logs
    private void getWearData() {
        startWatchUpdaterService(this, WatchUpdaterService.ACTION_SYNC_LOGS, TAG);
    }

    // load in bulk of remaining data
    private void getOlderData() {

        if (model.initial_items.size() == 0) {
            UserError.Log.d(TAG, "No initial items loaded yet to find index from");
            return;
        }
        final long highestId = model.initial_items.get(model.initial_items.size() - 1).getId();

        final Thread t = new Thread(() -> {
            JoH.threadSleep(300);
            // show loading spinner if load time takes > 1s and has non-default filter
            Inevitable.task("show-events-loading-if-slow", 1000, () -> {
                if (loading && !model.isDefaultFilters()) {
                    model.showLoading.set(loading);
                }
            });
            loading = true;
            model.older_items.addAll(UserError.olderThanID(highestId, 100000));
            model.refresh();
            loading = false;
            model.showLoading.set(false);
        }
        );
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startRefresh();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopRefresh();
    }

    // start streaming data
    private synchronized void startRefresh() {
        runRefresh = true;
        new Thread(() -> {
            int c;
            int turbo = 0;

            final boolean streamWearLogs = shouldStreamWearLogs();

            while (runRefresh) {
                if (D) UserError.Log.d(TAG, "refreshing data " + highest_id);
                if (refreshData()) {
                    turbo = 60;
                }
                if (turbo > 0) {
                    // short sleep
                    JoH.threadSleep(100);
                    turbo--;
                } else {
                    if (streamWearLogs && JoH.quietratelimit("stream-wear-logs", 2)) {
                        getWearData();
                    }
                    // long sleep
                    c = 0;
                    while (c < 2 && runRefresh) {
                        JoH.threadSleep(500);
                        c++;
                    }
                }
            }
        }).start();
    }

    // stop streaming data
    private void stopRefresh() {
        runRefresh = false;
    }

    private boolean refreshData() {

        final List<UserError> new_entries = UserError.newerThanID(highest_id, 500);
        if ((new_entries != null) && (new_entries.size() > 0)) {
            final long new_highest = new_entries.get(0).getId();
            UserError.Log.d(TAG, "New streamed data size: " + new_entries.size() + " Highest: " + new_highest);
            if (highest_id == 0) {
                model.newData(new_entries);
            } else {
                model.appendData(new_entries);
            }
            highest_id = new_highest;

            return true;
        }
        return false;
    }

    public synchronized void uploadEventLogs() { // Send events log to JamOrHam
        startActivity(new Intent(getApplicationContext(), SendFeedBack.class).putExtra("generic_text", packLogs()));
    }

    public synchronized void saveEventLog() { // Save events log in mobile storage
        startActivity(new Intent(getApplicationContext(), SaveLogs.class).putExtra("generic_text", packLogs()));
    }

    public void requestScrollToTop() {
        final Integer current = scrollToTopRequest.get();
        scrollToTopRequest.set((current == null ? 0 : current) + 1);
    }

    private String packLogs() { // Prepare current visible logs for upload or local save
        final StringBuilder builder = new StringBuilder(50000);
        builder.append("\n"
                + (model.allSeveritiesEnabled() ? "ALL" : model.whichSeveritiesEnabled()) + (model.getCurrentFilter() != "" ? " Search: " + model.getCurrentFilter() : "") + "\n\n");
        for (UserError item : model.visible) {
            builder.append(item.toString());
            builder.append("\n");
            if (builder.length() > MAX_LOG_PACKAGE_SIZE) {
                JoH.static_toast_long(this, "Could not package up all logs, using most recent");
                builder.append("\n\nOnly the most recent logs have been included to limit the file size.\n");
                break;
            }
        }

        builder.insert(0, JoH.getDeviceDetails() + "\n" + JoH.getVersionDetails() + "\n" + getBestCollectorHardwareName() + "\n===\n" + "\nLog data:\n"); // Adds device, version and collector details before the log.
        builder.append("\n\nCaptured: " + JoH.dateTimeText(JoH.tsl())); // Adds date and time of capture after the log.

        return builder.toString();
    }

    /**
     * View model container - the observable lists/state are retained and bridged into Compose.
     */
    public class ViewModel {

        public final ObservableList<UserError> initial_items = new ObservableArrayList<>();
        public final ObservableList<UserError> older_items = new ObservableArrayList<>();
        public final ObservableList<UserError> streamed_items = new ObservableArrayList<>();
        public final MergeObservableList<UserError> items = new MergeObservableList<UserError>()
                .insertList(streamed_items)
                .insertList(initial_items)
                .insertList(older_items);
        public final ObservableList<UserError> visible = new ObservableArrayList<>();
        public final ObservableBoolean showLoading = new ObservableBoolean(false);
        private final SparseBooleanArray severities = new SparseBooleanArray();
        private String currentFilter = null;

        {
            // persistence load for severities selection
            for (int severity : severitiesList) {
                severities.put(severity, PersistentStore.getBoolean(PREF_SEVERITY_SELECTION + severity, true));
            }
        }

        // populate initial data set
        void newData(List<UserError> newItems) {
            initial_items.clear();
            initial_items.addAll(newItems);
            refresh();
        }

        // append new streamed data to the display
        void appendData(List<UserError> newItems) {
            JoH.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    synchronized (items) {
                        streamed_items.addAll(0, newItems);
                    }
                    refreshNewItems(newItems.size());
                    if (listAtTop.get()) {
                        // If unmoved or already at top then scroll to new values
                        JoH.runOnUiThreadDelayed(() -> requestScrollToTop(), 300);
                    }
                }
            });
        }

        // return the current filter, load from persistent store if not yet loaded
        public String getCurrentFilter() {
            if (currentFilter == null) {
                currentFilter = PersistentStore.getString(PREF_LAST_SEARCH);
            }
            return currentFilter;
        }

        // refresh the display always applying the current filter
        void refresh() {
            JoH.runOnUiThread(() -> filter(getCurrentFilter()));
        }

        // refresh the display always applying the current filter
        void refreshNewItems(final int count) {
            if (count > 0) {
                JoH.runOnUiThread(() -> insertFilteredNewItems(getCurrentFilter(), count));
            }
        }

        // filter has changed on text input, refresh display
        public void filterChanged(String filter) {
            currentFilter = filter.toLowerCase().trim();

            Inevitable.task("event-log-filter-update", 200, () -> {
                refresh();
                PersistentStore.setString(PREF_LAST_SEARCH, currentFilter);
            });
        }

        // is every single severity we filter on enabled
        boolean allSeveritiesEnabled() {
            for (int severity : severitiesList) {
                if (!severity(severity)) return false;
            }
            return true;
        }

        // which severities are enabled (human readable)
        String whichSeveritiesEnabled() {
            String result = "";
            for (int severity : severitiesList) {
                if (severity(severity)) result += severity + " ";
            }
            return result;
        }

        // apply a filter and refresh display
        void filter(final String filter) {
            currentFilter = filter.or(getCurrentFilter()).toLowerCase().trim();
            visible.clear();
            synchronized (items) {
                // skip filter on initial defaults for speed
                if (isDefaultFilters()) {
                    visible.addAll(items);
                } else {
                    for (UserError item : items) {
                        if (filterMatch(item)) {
                            visible.add(item);
                        }
                    }
                }
            }
            notifyChanged();
        }

        // apply filter just to some new items and update accordingly
        private void insertFilteredNewItems(final String filter, int count) {
            currentFilter = filter.or(getCurrentFilter()).toLowerCase().trim();
            int c = 0;
            synchronized (items) {
                for (UserError item : items) {
                    if (filterMatch(item)) {
                        visible.add(0, item);
                    }
                    c++;
                    if (c >= count) break;
                }
            }
            notifyChanged();
        }


        // check if current filter is the out-of-the-box default
        public boolean isDefaultFilters() {
            return ((currentFilter.or("").length() == 0) && allSeveritiesEnabled());
        }

        // check severity enabled and case insensitive contains match using optimized extension method
        public boolean filterMatch(final UserError item) {
            return severities.get(item.severity)
                    && (item.shortError.containsIgnoreCaseF(currentFilter) || (item.message.containsIgnoreCaseF(currentFilter)));
        }


        // whether to show a particular title, eg if it is not a duplicate
        public boolean showThisTitle(UserError item) {

            try {
                final int location = visible.indexOf(item); // cpu intensive?? add cache?
                // if the previous item is the same title then don't show the header
                if (visible.get(location - 1).shortError.equals(item.shortError)) {
                    return false;
                }
            } catch (IndexOutOfBoundsException e) {
                return true;
            }
            return true;
        }

        // is this severity enabled?
        public boolean severity(int i) {
            return severities.get(i);
        }

        // severity checkbox clicked, update store and refresh screen
        public void setSeverity(int i, boolean value) {
            severities.put(i, value);
            refresh();
            PersistentStore.setBoolean(PREF_SEVERITY_SELECTION + i, value);
        }
    }
}
