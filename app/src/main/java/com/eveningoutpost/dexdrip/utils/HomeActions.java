package com.eveningoutpost.dexdrip.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.ImportDatabaseActivity;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.profileeditor.DatePickerFragment;
import com.eveningoutpost.dexdrip.profileeditor.ProfileAdapter;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utilitymodels.UpdateActivity;
import com.eveningoutpost.dexdrip.wearintegration.Amazfitservice;
import com.eveningoutpost.dexdrip.wearintegration.WatchUpdaterService;
import com.eveningoutpost.dexdrip.xdrip;

import java.io.File;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Actions that used to live only on the Home overflow menu and are now driven from a settings row.
 *
 * <p>The bodies are the same as the former {@code Home} handlers, but take any {@link Activity} so
 * both the home screen and the settings activity can run them without reaching into {@code Home}.
 */
public class HomeActions {

    private HomeActions() {
    }

    public static void exportDatabase(final Activity activity) {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(activity,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            0);
                    return null;
                } else {
                    return DatabaseUtil.saveSql(activity.getBaseContext());
                }
            }

            @Override
            protected void onPostExecute(String filename) {
                super.onPostExecute(filename);
                if (filename != null) {
                    Home.snackBar(R.string.share, activity.getString(R.string.exported_to) + filename,
                            makeSnackBarUriLauncher(activity, Uri.fromFile(new File(filename)), activity.getString(R.string.share_database)), activity);
                    activity.startActivity(new Intent(xdrip.getAppContext(), SdcardImportExport.class).putExtra("backup", "now").setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                } else {
                    Toast.makeText(activity, R.string.could_not_export_database, Toast.LENGTH_LONG).show();
                }
            }
        }.execute();
    }

    public static void exportCSVasSiDiary(final Activity activity) {
        long from = Pref.getLong("sidiary_last_exportdate", 0);
        final GregorianCalendar date = new GregorianCalendar();
        final DatePickerFragment datePickerFragment = new DatePickerFragment();
        if (from > 0) datePickerFragment.setInitiallySelectedDate(from);
        datePickerFragment.setAllowFuture(false);
        datePickerFragment.setTitle(activity.getString(R.string.sidiary_date_title));
        datePickerFragment.setDateCallback(new ProfileAdapter.DatePickerCallbacks() {
            @Override
            public void onDateSet(int year, int month, int day) {
                date.set(year, month, day);
                date.set(Calendar.HOUR_OF_DAY, 0);
                date.set(Calendar.MINUTE, 0);
                date.set(Calendar.SECOND, 0);
                date.set(Calendar.MILLISECOND, 0);
                new AsyncTask<Void, Void, String>() {
                    @Override
                    protected String doInBackground(Void... params) {
                        int permissionCheck = ContextCompat.checkSelfPermission(activity,
                                Manifest.permission.READ_EXTERNAL_STORAGE);
                        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(activity,
                                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                    0);
                            return null;
                        } else {
                            return DatabaseUtil.saveCSV(activity.getBaseContext(), date.getTimeInMillis());
                        }
                    }

                    @Override
                    protected void onPostExecute(String filename) {
                        super.onPostExecute(filename);
                        if (filename != null) {
                            Pref.setLong("sidiary_last_exportdate", System.currentTimeMillis());
                            Home.snackBar(R.string.share, activity.getString(R.string.exported_to) + filename,
                                    makeSnackBarUriLauncher(activity, Uri.fromFile(new File(filename)), activity.getString(R.string.share_database)), activity);
                        } else {
                            Toast.makeText(activity, activity.getString(R.string.could_not_export_csv_), Toast.LENGTH_LONG).show();
                        }
                    }
                }.execute();
            }
        });
        datePickerFragment.show(activity.getFragmentManager(), "DatePicker");
    }

    public static void checkForUpdate(final Activity activity) {
        if (JoH.ratelimit("manual-update-check", 5)) {
            Toast.makeText(activity, activity.getString(R.string.checking_for_update), Toast.LENGTH_SHORT).show();
            UpdateActivity.last_check_time = -1;
            UpdateActivity.checkForAnUpdate(activity, true);
        }
    }

    public static void resendGlucoseToWatch(Activity activity) {
        WatchUpdaterService.startServiceAndResendData(0);
        if (Pref.getBooleanDefaultFalse("pref_amazfit_enable_key")) {
            Amazfitservice.start("xDrip_synced_SGV_data");
        }
    }

    public static void openSettingsOnWatch(Activity activity) {
        activity.startService(new Intent(activity, WatchUpdaterService.class).setAction(WatchUpdaterService.ACTION_OPEN_SETTINGS));
    }

    public static void resetWearDb(Activity activity) {
        activity.startService(new Intent(activity, WatchUpdaterService.class).setAction(WatchUpdaterService.ACTION_RESET_DB));
    }

    public static void restoreDatabase(Activity activity) {
        activity.startActivity(new Intent(activity, ImportDatabaseActivity.class));
    }

    private static android.view.View.OnClickListener makeSnackBarUriLauncher(final Activity activity, final Uri uri, final String text) {
        return v -> {
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.setType("application/octet-stream");
            activity.startActivity(Intent.createChooser(shareIntent, text));
        };
    }
}
