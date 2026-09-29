package com.eveningoutpost.dexdrip.stats;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.db.AppDatabase;
import com.eveningoutpost.dexdrip.db.dao.BgReadingDao;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError.Log;

import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.xdrip;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Vector;

/**
 * Created by adrian on 30/06/15.
 */
public class DBSearchUtil {

    public static final String CUTOFF = "38";


    public static int noReadingsAboveRange(Context context) {
        Bounds bounds = new Bounds().invoke();

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        boolean mgdl = "mgdl".equals(settings.getString("units", "mgdl"));

        double high = Double.parseDouble(settings.getString("highValue", "170"));
        if (!mgdl) {
            high *= Constants.MMOLL_TO_MGDL;
        }

        int count = dao().countAbove(bounds.start, bounds.stop, Double.parseDouble(CUTOFF), high);
        Log.d("DrawStats", "High count: " + count);
        return count;
    }


    public static List<BgReadingStats> getReadings(boolean ordered) {
        try {
            Bounds bounds = new Bounds().invoke();

            final List<BgReading> rows = ordered
                    ? dao().statsReadingsOrdered(bounds.start, bounds.stop, Double.parseDouble(CUTOFF))
                    : dao().statsReadingsUnordered(bounds.start, bounds.stop, Double.parseDouble(CUTOFF));
            List<BgReadingStats> readings = new Vector<BgReadingStats>();
            for (BgReading row : rows) {
                BgReadingStats reading = new BgReadingStats();
                reading.timestamp = row.timestamp;
                reading.calculated_value = row.calculated_value;
                readings.add(reading);
            }
            return readings;

        } catch (Exception e) {
            JoH.static_toast_long(e.getMessage());
            return null;
        }
    }

    public static List<BgReadingStats> getFilteredReadingsWithFallback(boolean ordered) {
        try {
            Bounds bounds = new Bounds().invoke();

            final List<BgReading> rows = ordered
                    ? dao().statsReadingsOrdered(bounds.start, bounds.stop, Double.parseDouble(CUTOFF))
                    : dao().statsReadingsUnordered(bounds.start, bounds.stop, Double.parseDouble(CUTOFF));
            List<BgReadingStats> readings = new Vector<BgReadingStats>();
            for (BgReading row : rows) {
                BgReadingStats reading = new BgReadingStats();
                reading.timestamp = row.timestamp;
                reading.calculated_value = row.calculated_value;
                if (reading.calculated_value == 0)
                    reading.calculated_value = row.filtered_calculated_value;
                readings.add(reading);
            }
            return readings;

        } catch (Exception e) {
            JoH.static_toast_long(e.getMessage());
            return null;
        }
    }


    public static int noReadingsInRange(Context context) {
        Bounds bounds = new Bounds().invoke();

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        boolean mgdl = "mgdl".equals(settings.getString("units", "mgdl"));

        double high = Double.parseDouble(settings.getString("highValue", "170"));
        double low = Double.parseDouble(settings.getString("lowValue", "70"));
        if (!mgdl) {
            high *= Constants.MMOLL_TO_MGDL;
            low *= Constants.MMOLL_TO_MGDL;

        }
        int count = dao().countIn(bounds.start, bounds.stop, Double.parseDouble(CUTOFF), high, low);
        Log.d("DrawStats", "In count: " + count);

        return count;
    }

    public static int noReadingsBelowRange(Context context) {
        Bounds bounds = new Bounds().invoke();

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        boolean mgdl = "mgdl".equals(settings.getString("units", "mgdl"));

        double low = Double.parseDouble(settings.getString("lowValue", "70"));
        if (!mgdl) {
            low *= Constants.MMOLL_TO_MGDL;

        }
        int count = dao().countBelow(bounds.start, bounds.stop, Double.parseDouble(CUTOFF), low);
        Log.d("DrawStats", "Low count: " + count);

        return count;
    }


    public static long getTodayTimestamp() {
        Calendar date = new GregorianCalendar();
        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);
        return date.getTimeInMillis();
    }

    public static long getYesterdayTimestamp() {
        Calendar date = new GregorianCalendar();
        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);
        date.add(Calendar.DATE, -1);
        return date.getTimeInMillis();
    }

    public static long getXDaysTimestamp(int x) {
        Calendar date = new GregorianCalendar();
        date.add(Calendar.DATE, -x);
        return date.getTimeInMillis();
    }

    private static BgReadingDao dao() {
        return AppDatabase.getInstance(xdrip.getAppContext()).bgReadingDao();
    }

    private static class Bounds {
        private long stop;
        private long start;

        public long getStop() {
            return stop;
        }

        public long getStart() {
            return start;
        }

        public Bounds invoke() {
            stop = System.currentTimeMillis();
            start = System.currentTimeMillis();

            switch (StatsActivity.state) {
                case StatsActivity.TODAY:
                    start = getTodayTimestamp();
                    break;
                case StatsActivity.YESTERDAY:
                    start = getYesterdayTimestamp();
                    stop = getTodayTimestamp();
                    break;
                case StatsActivity.D7:
                    start = getXDaysTimestamp(7);
                    break;
                case StatsActivity.D30:
                    start = getXDaysTimestamp(30);
                    break;
                case StatsActivity.D90:
                    start = getXDaysTimestamp(90);
                    break;
            }
            return this;
        }
    }
}