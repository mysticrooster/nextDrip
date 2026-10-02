package com.eveningoutpost.dexdrip;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.ObservableField;
import android.util.Log;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.ui.secondary.ImportDbScreen;
import com.eveningoutpost.dexdrip.utilitymodels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.utils.DatabaseUtil;
import com.eveningoutpost.dexdrip.utils.FileUtils;
import com.eveningoutpost.dexdrip.wearintegration.WatchUpdaterService;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.eveningoutpost.dexdrip.Home.startWatchUpdaterService;

public class ImportDatabaseActivity extends BaseAppCompatActivity {
    private final static String TAG = ImportDatabaseActivity.class.getSimpleName();
    public static String menu_name = "Import Database";
    private Handler mHandler;
    private ArrayList<String> databaseNames;
    private ArrayList<File> databases;
    private final static int MY_PERMISSIONS_REQUEST_STORAGE = 132;

    /** Compose bridge: bumped whenever the screen-visible state changes. */
    public final ObservableField<Integer> tick = new ObservableField<>(0);
    public final ObservableField<Boolean> showWarning = new ObservableField<>(true);
    public final ObservableField<String> resultMessage = new ObservableField<>((String) null);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
        final String importit = getIntent().getStringExtra("importit");
        if ((importit != null) && (importit.length() > 0)) {
            showWarning.set(false);
            importDB(new File(importit), this);
        }
        ImportDbScreen.installImportDb(this);
    }

    /** Nudges the Compose screen to recompute its state. */
    public void notifyChanged() {
        final Integer current = tick.get();
        tick.set((current == null ? 0 : current) + 1);
    }

    public void generateDBGui() {
        showWarning.set(false);
        notifyChanged();
        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED && findAllDatabases()) {
            sortDatabasesAlphabetically();
            showDatabasesInList();
        } else if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            JoH.static_toast_long("Need permission for saved files");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_STORAGE);
        } else {
            postImportDB("\'xdrip\' is not a directory... aborting.");
        }
    }

    private void sortDatabasesAlphabetically() {
        Collections.sort(databases, new Comparator<File>() {
            @Override
            public int compare(File lhs, File rhs) {
                //descending sort
                return rhs.getName().compareTo(lhs.getName());
            }
        });
    }

    private boolean findAllDatabases() {
        databases = new ArrayList<>();

        File file = new File(FileUtils.getExternalDir());
        if (!FileUtils.makeSureDirectoryExists(file.getAbsolutePath())) {
            return false;
        }

        // add from "root"
        addAllDatabases(file, databases);

        // add from level below (Andriod usually unzips to a subdirectory)
        File[] subdirectories = file.listFiles(new FileFilter() {
            @Override
            public boolean accept(File path) {
                return path.isDirectory();
            }
        });
        try {
            for (File subdirectory : subdirectories) {
                addAllDatabases(subdirectory, databases);
            }
        } catch (NullPointerException e) {
            // nothing found
        }
        return true;
    }

    private void showDatabasesInList() {
        databaseNames = new ArrayList<>();

        //show found databases in List
        for (File db : databases) {
            databaseNames.add(db.getName());
        }
        notifyChanged();

        if (databaseNames.size() == 0) {
            postImportDB("No databases found.");
        }
    }

    public List<String> getDatabaseNamesSnapshot() {
        return databaseNames == null ? Collections.<String>emptyList() : new ArrayList<>(databaseNames);
    }

    public String databaseNameAt(int position) {
        return (databases != null && position >= 0 && position < databases.size())
                ? databases.get(position).getName() : "";
    }

    private void addAllDatabases(File file, ArrayList<File> databases) {
        File[] files = file.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getPath().endsWith(".sqlite") || pathname.getPath().endsWith(".zip");
            }
        });
        if ((databases != null) && (files != null)) {
            Collections.addAll(databases, files);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_REQUEST_STORAGE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                generateDBGui();
            } else {
                finish();
            }
        }
    }

    public int getDBVersion() {
        // The final legacy ActiveAndroid database version (was AA_DB_VERSION in the manifest).
        return 41;
    }


    public void importDB(int position) {
        importDB(databases.get(position), this);
    }

    private void importDB(File the_file, Activity activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Importing, please wait");
        builder.setMessage("Importing, please wait");
        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.setMessage("Step 1: checking prerequisites");
        dialog.setCancelable(false);
        LoadTask lt = new LoadTask(dialog, the_file);
        lt.execute();
    }

    protected void postImportDB(String result) {

        startWatchUpdaterService(this, WatchUpdaterService.ACTION_RESET_DB, TAG);

        resultMessage.set(result);
        notifyChanged();
    }

    public void returnToHome() {
        Intent intent = new Intent(this, Home.class);
        CollectionServiceStarter.restartCollectionService(getApplicationContext());
        startActivity(intent);
        finish();
    }

    private class LoadTask extends AsyncTask<Void, Void, String> {

        private final AlertDialog statusDialog;
        private File dbFile;

        LoadTask(AlertDialog statusDialog, File dbFile) {
            super();
            this.statusDialog = statusDialog;
            this.dbFile = dbFile;
        }


        protected String doInBackground(Void... args) {
            //Check if db has the correct version:
            File delete_file = null;
            try {

                if (dbFile.getAbsolutePath().endsWith(".zip")) {
                    // uncompress first
                    try (final FileInputStream fileInputStream = new FileInputStream(dbFile.getAbsolutePath());
                         final ZipInputStream zip_stream = new ZipInputStream(new BufferedInputStream(fileInputStream))) {
                        ZipEntry zipEntry = zip_stream.getNextEntry();
                        if ((zipEntry != null) && zipEntry.isDirectory())
                            zipEntry = zip_stream.getNextEntry();
                        if (zipEntry != null) {
                            String filename = zipEntry.getName();
                            if (filename.endsWith(".sqlite")) {
                                String output_filename = dbFile.getAbsolutePath().replaceFirst(".zip$", ".sqlite");
                                try (FileOutputStream fout = new FileOutputStream(output_filename)) {
                                    byte[] buffer = new byte[4096];
                                    int count;
                                    while ((count = zip_stream.read(buffer)) != -1) {
                                        fout.write(buffer, 0, count);
                                    }
                                }
                                dbFile = new File(output_filename);
                                delete_file = dbFile;
                                Log.d(TAG, "New filename: " + output_filename);
                            } else {
                                String msg = "Cant find sqlite in zip file";
                                JoH.static_toast_long(msg);
                                return msg;
                            }
                            zip_stream.closeEntry();
                        } else {
                            String msg = "Invalid ZIP file";
                            JoH.static_toast_long(msg);
                            return msg;
                        }

                    } catch (IOException e) {
                        String msg = "Could not open file";
                        JoH.static_toast_long(msg);
                        return msg;
                    }
                }

                SQLiteDatabase db = SQLiteDatabase.openDatabase(dbFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
                int version = db.getVersion();
                db.close();
                if (getDBVersion() != version) {
                    statusDialog.dismiss();
                    return "Wrong Database version.\n(" + version + " instead of " + getDBVersion() + ")";
                }
            } catch (SQLiteException e){
                statusDialog.dismiss();
                return "Database cannot be opened... aborting.";
            }
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    statusDialog.setMessage("Step 2: exporting current DB");
                }
            });

            String export = DatabaseUtil.saveSql(xdrip.getAppContext(), "b4import");


            if (export == null) {
                statusDialog.dismiss();
                return "Exporting database not successfull... aborting.";
            }

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    statusDialog.setMessage("Step 3: importing DB");
                }
            });

            String result = DatabaseUtil.loadSql(xdrip.getAppContext(), dbFile.getAbsolutePath());
            if (delete_file != null) delete_file.delete();
            statusDialog.dismiss();;
            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            postImportDB(result);

        }
    }
}
