package com.eveningoutpost.dexdrip.utils.jobs;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.services.DailyIntentService;
import com.eveningoutpost.dexdrip.xdrip;

import java.util.concurrent.TimeUnit;

/**
 * jamorham
 *
 * Scheduled daily job for cleanup / maintenance tasks
 *
 * Should run once per day at the best time period for battery conservation and user experience
 */

public class DailyJob extends Worker {

    public static final String TAG = "xDrip-Daily";

    public DailyJob(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        final long startTime = JoH.tsl();
        DailyIntentService.work();
        final String cellService = !JoH.isLANConnected() ? " (mobile)" : "";
        UserError.Log.uel(TAG, JoH.dateTimeText(JoH.tsl()) + " Job Ran" + cellService + ", duration: " + JoH.niceTimeScalar(JoH.msSince(startTime)));

        return Result.success();
    }

    public static void schedule() {
        if (JoH.pratelimit("daily-job-schedule", 60000)) {
            UserError.Log.uel(TAG, JoH.dateTimeText(JoH.tsl()) + " Job Scheduled");
            final PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(DailyJob.class, 1, TimeUnit.DAYS, 12, TimeUnit.HOURS)
                    .setConstraints(new Constraints.Builder()
                            .setRequiresDeviceIdle(true)
                            .setRequiresBatteryNotLow(true)
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build())
                    .build();
            WorkManager.getInstance(xdrip.getAppContext()).enqueueUniquePeriodicWork(TAG, ExistingPeriodicWorkPolicy.UPDATE, request);
        }
    }
}
