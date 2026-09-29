package com.eveningoutpost.dexdrip.utils;

import android.content.Context;
import android.database.Cursor;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateFormat;
import android.widget.Toast;

import com.eveningoutpost.dexdrip.db.AppDatabase;
import com.eveningoutpost.dexdrip.db.LegacyDataImporter;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.xdrip;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.eveningoutpost.dexdrip.utils.FileUtils.getExternalDir;
import static com.eveningoutpost.dexdrip.utils.FileUtils.makeSureDirectoryExists;

/**
 * Save the SQL database to file.
 */
public class DatabaseUtil {

    private static final String TAG = DatabaseUtil.class.getSimpleName();
    private static final int BUFFER_SIZE = 4096;
    private static final Handler handler = new Handler(Looper.getMainLooper());

    private static void toastText(final Context context, final String text) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, text, Toast.LENGTH_LONG).show();
            }
        });
    }

    public static long getDataBaseSizeInBytes() {
        try {
            final File legacyDB = xdrip.getAppContext().getDatabasePath(LegacyDataImporter.LEGACY_DB_NAME);
            final File roomDB = xdrip.getAppContext().getDatabasePath(AppDatabase.DATABASE_NAME);
            return legacyDB.length() + roomDB.length();
        } catch (Exception e) {
            return -1;
        }
    }

    public static String saveSql(Context context) {
        // TecMunky 6/23/17 overload saveSql function to call modified function
        return DatabaseUtil.saveSql(context, "export");
    }

    public static String saveSql(Context context, String prefix) {
        // TecMunky 6/23/17 modify function with added prefix string variable

        FileOutputStream foStream = null;
        ZipOutputStream zipOutputStream = null;
        String zipFilename = null;

        try {
            final String dir = getExternalDir();
            makeSureDirectoryExists(dir);

            final StringBuilder sb = new StringBuilder();
            sb.append(dir);
            //sb.append("/export");
            // TecMunky 6/23/17 replace "/export" with "/" and prefix
            sb.append("/");
            sb.append(prefix);
            final String stamp = DateFormat.format("yyyyMMdd-kkmmss", System.currentTimeMillis()).toString();
            sb.append(stamp);
            sb.append(".zip");
            zipFilename = sb.toString();
            final File sd = Environment.getExternalStorageDirectory();
            if (sd.canWrite()) {
                final File zipOutputFile = new File(zipFilename);
                foStream = new FileOutputStream(zipOutputFile);
                zipOutputStream = new ZipOutputStream(new BufferedOutputStream(foStream));

                // Include both databases so a restore brings back everything.
                AppDatabase.checkpointForBackup();
                boolean wroteAny = false;
                wroteAny |= zipDatabaseFile(context, LegacyDataImporter.LEGACY_DB_NAME, zipOutputStream, prefix + stamp + ".sqlite");
                wroteAny |= zipDatabaseFile(context, AppDatabase.DATABASE_NAME, zipOutputStream, prefix + stamp + "-room.sqlite");

                if (!wroteAny) {
                    toastText(context, "Problem: No current DB found!");
                    Log.d(TAG, "Problem: No current DB found");
                } else if (!zipFilename.contains("b4import")) {
                    Pref.setString("last-saved-database-zip", zipFilename);
                }
            } else {
                toastText(context, "SD card not writable!");
                Log.d(TAG, "SD card not writable!");
                zipFilename = null;
            }

        } catch (IOException e) {
            toastText(context, "SD card not writable!");
            Log.e(TAG, "Exception while writing DB", e);
            zipFilename = null;
        } finally {
            if (zipOutputStream != null) try {
                zipOutputStream.close();
            } catch (IOException e1) {
                Log.e(TAG, "Something went wrong closing: ", e1);
            }
        }
        JoH.clearCache();
        return zipFilename;
    }

    private static boolean zipDatabaseFile(final Context context, final String databaseName,
                                           final ZipOutputStream zipOutputStream, final String entryName) throws IOException {
        final File dbFile = context.getDatabasePath(databaseName);
        if (!dbFile.exists()) {
            return false;
        }
        zipOutputStream.putNextEntry(new ZipEntry(entryName));
        try (FileInputStream in = new FileInputStream(dbFile)) {
            final byte[] buffer = new byte[BUFFER_SIZE];
            int count;
            while ((count = in.read(buffer, 0, BUFFER_SIZE)) != -1) {
                zipOutputStream.write(buffer, 0, count);
            }
        }
        zipOutputStream.closeEntry();
        return true;
    }

    public static String saveSqlUnzipped(Context context) {

        FileInputStream srcStream = null;
        FileChannel src = null;
        FileOutputStream destStream = null;
        FileChannel dst = null;
        String filename = null;

        try {

            final String databaseName = LegacyDataImporter.LEGACY_DB_NAME;

            final String dir = getExternalDir();
            makeSureDirectoryExists(dir);

            final StringBuilder sb = new StringBuilder();
            sb.append(dir);
            sb.append("/export");
            sb.append(DateFormat.format("yyyyMMdd-kkmmss", System.currentTimeMillis()));
            sb.append(".sqlite");

            filename = sb.toString();
            final File sd = Environment.getExternalStorageDirectory();
            if (sd.canWrite()) {
                final File currentDB = context.getDatabasePath(databaseName);
                final File backupDB = new File(filename);
                if (currentDB.exists()) {
                    srcStream = new FileInputStream(currentDB);
                    src = srcStream.getChannel();
                    destStream = new FileOutputStream(backupDB);
                    dst = destStream.getChannel();
                    dst.transferFrom(src, 0, src.size());
                } else {
                    toastText(context, "Problem: No current DB found!");
                    Log.d(TAG, "Problem: No current DB found");
                }
            } else {
                toastText(context, "SD card not writable!");
                Log.d(TAG, "SD card not writable!");
            }

        } catch (IOException e) {
            toastText(context, "SD card not writable!");
            Log.e(TAG, "Exception while writing DB", e);
        } finally {
            if (src != null) try {
                src.close();
            } catch (IOException e1) {
                Log.e(TAG, "Something went wrong closing: ", e1);
            }
            if (destStream != null) try {
                destStream.close();
            } catch (IOException e1) {
                Log.e(TAG, "Something went wrong closing: ", e1);
            }
            if (srcStream != null) try {
                srcStream.close();
            } catch (IOException e1) {
                Log.e(TAG, "Something went wrong closing: ", e1);
            }
            if (dst != null) try {
                dst.close();
            } catch (IOException e1) {
                Log.e(TAG, "Something went wrong closing: ", e1);
            }
        }
        return filename;
    }


    /**
     * Generate a csv that can be imported by SiDiary
     */
    public static String saveCSV(Context context, long from) {

        FileOutputStream foStream = null;
        PrintStream printStream = null;
        ZipOutputStream zipOutputStream = null;
        String zipFilename = null;


        try {

            final String dir = getExternalDir();
            makeSureDirectoryExists(dir);

            final StringBuilder sb = new StringBuilder();
            sb.append(dir);
            sb.append("/exportCSV");
            sb.append(DateFormat.format("yyyyMMdd-kkmmss", System.currentTimeMillis()));
            sb.append(".zip");
            zipFilename = sb.toString();
            final File sd = Environment.getExternalStorageDirectory();
            if (sd.canWrite()) {
                final File zipOutputFile = new File(zipFilename);

                foStream = new FileOutputStream(zipOutputFile);
                zipOutputStream = new ZipOutputStream(new BufferedOutputStream(foStream));
                zipOutputStream.putNextEntry(new ZipEntry("export" + DateFormat.format("yyyyMMdd-kkmmss", System.currentTimeMillis()) + ".csv"));
                printStream = new PrintStream(zipOutputStream);

                //add Treatment and BGlucose Header
                printStream.println("DAY;TIME;UDT_CGMS;BG_LEVEL;CH_GR;BOLUS;REMARK");

                final AppDatabase appDatabase = AppDatabase.getInstance(xdrip.getAppContext());

                // Set all needed Vars
                double value;
                String valueIE;
                String valueCHO;
                String notes;

                long timestamp;

                java.text.DateFormat df = new SimpleDateFormat("dd.MM.yyyy;HH:mm;");
                //df.setTimeZone(TimeZone.getDefault()); did not change the time-slope, so turned it off...

                Date date = new Date();

                //Extract CGMS-Values
                try (Cursor cur = appDatabase.bgReadingDao().exportCursor(from)) {
                    if (cur.moveToFirst()) {
                        do {
                            timestamp = cur.getLong(0);
                            value = cur.getDouble(1);
                            if (value > 13) {
                                date.setTime(timestamp);
                                printStream.println(df.format(date) + Math.round(value) + ";;;;");
                            }
                        } while (cur.moveToNext());
                    }
                }

                //Extract Calibration-BG-Values
                try (Cursor cur = appDatabase.calibrationDao().exportCursor(from)) {
                    if (cur.moveToFirst()) {
                        do {
                            timestamp = cur.getLong(0);
                            value = cur.getDouble(1);
                            if (value > 0) {
                                date.setTime(timestamp);
                                printStream.println(df.format(date) + ";" + Math.round(value) + ";;;");
                            }
                        } while (cur.moveToNext());
                    }
                }

                //Extract Treatment-Values
                try (Cursor cur = appDatabase.treatmentsDao().exportCursor(from)) {
                    if (cur.moveToFirst()) {
                        do {
                            timestamp = cur.getLong(0);
                            valueCHO = cur.getString(1);
                            valueIE = cur.getString(2);
                            notes = cur.getString(3);
                            if (notes == null) notes = "";
                            if (valueIE.equals("0")) valueIE = "";
                            if (valueCHO.equals("0")) valueCHO = "";
                            notes= notes.replaceAll("\n","||"); //convert linefeed to SiDiary conform expression
                            if (!valueIE.equals("") || !valueCHO.equals("") || !notes.equals("")) {
                                date.setTime(timestamp);
                                printStream.println(df.format(date) + ";;" + valueCHO + ";" + valueIE + ";" + notes);
                            }
                        } while (cur.moveToNext());
                    }
                }

                printStream.flush();


            } else {
                toastText(context, "SD card not writable!");
                Log.d(TAG, "SD card not writable!");
            }

        } catch (IOException e) {
            toastText(context, "SD card not writable!");
            Log.e(TAG, "Exception while writing DB", e);
        } finally {
            if (printStream != null) {
                printStream.close();
            }
            if (zipOutputStream != null) try {
                zipOutputStream.close();
            } catch (IOException e1) {
                Log.e(TAG, "Something went wrong closing: ", e1);
            }
        }
        return zipFilename;
    }


    public static String loadSql(Context context, String path) {

        FileInputStream srcStream = null;
        FileChannel src = null;
        FileOutputStream destStream = null;
        FileChannel dst = null;

        String returnString = "";

        try {
            final File replacement = new File(path);
            if (!replacement.exists()) {
                Log.d(TAG, "File does not exist: " + path);
                return "File does not exist: " + path;
            }
            final String databaseName = isRoomDatabase(path) ? AppDatabase.DATABASE_NAME : LegacyDataImporter.LEGACY_DB_NAME;
            File currentDB = context.getDatabasePath(databaseName);
            File currentDBold = context.getDatabasePath(databaseName + ".old");
            File currentDBtmp = context.getDatabasePath(databaseName + ".tmp");

            try {
                currentDBold.delete();
            } catch (Exception e) {
                //
            }
            try {
                currentDBtmp.delete();
            } catch (Exception e) {
                //
            }
            if (currentDB.canWrite()) {
                srcStream = new FileInputStream(replacement);
                src = srcStream.getChannel();
                destStream = new FileOutputStream(currentDBtmp);
                dst = destStream.getChannel();
                dst.transferFrom(src, 0, src.size());
                destStream.flush();
                currentDB.renameTo(currentDBold);
                currentDBtmp.renameTo(currentDB);
                currentDBold.delete();
                returnString = "Successfully imported database";
                if (LegacyDataImporter.LEGACY_DB_NAME.equals(databaseName)) {
                    // A legacy DB was imported; re-run the Room copy on the next launch.
                    LegacyDataImporter.clearImportState();
                }
            } else {
                Log.v(TAG, "loadSql: No Write access");
                returnString = "loadSql: No Write access";
            }
        } catch (IOException e) {
            Log.e(TAG, "Something went wrong importing Database", e);
            returnString = "Something went wrong importing database";


        } finally {
            if (src != null) try {
                src.close();
            } catch (IOException e1) {
                Log.e(TAG, "Something went wrong closing: ", e1);
            }
            if (destStream != null) try {
                destStream.close();
            } catch (IOException e1) {
                Log.e(TAG, "Something went wrong closing: ", e1);
            }
            if (srcStream != null) try {
                srcStream.close();
            } catch (IOException e1) {
                Log.e(TAG, "Something went wrong closing: ", e1);
            }
            if (dst != null) try {
                dst.close();
            } catch (IOException e1) {
                Log.e(TAG, "Something went wrong closing: ", e1);

            }
            JoH.fullDatabaseReset();
            return returnString;
        }
    }

    /**
     * A database file produced by Room contains the {@code room_master_table}; anything else is a
     * legacy ActiveAndroid database.
     */
    private static boolean isRoomDatabase(final String path) {
        try (android.database.sqlite.SQLiteDatabase db = android.database.sqlite.SQLiteDatabase
                .openDatabase(path, null, android.database.sqlite.SQLiteDatabase.OPEN_READONLY);
             android.database.Cursor cursor = db.rawQuery(
                     "SELECT name FROM sqlite_master WHERE type='table' AND name='room_master_table'", null)) {
            return cursor.moveToFirst();
        } catch (Exception e) {
            return false;
        }
    }
}
