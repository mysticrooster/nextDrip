package com.eveningoutpost.dexdrip.utils;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.eveningoutpost.dexdrip.GcmActivity;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.cloud.nightlite.NightLiteClient;
import com.eveningoutpost.dexdrip.cloud.nightlite.NightLiteEntry;
import com.eveningoutpost.dexdrip.cloud.nightlite.NightLiteQR;
import com.eveningoutpost.dexdrip.models.DesertSync;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.models.UserError.ExtraLogTags;
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.receiver.InfoContentProvider;
import com.eveningoutpost.dexdrip.services.PlusSyncService;
import com.eveningoutpost.dexdrip.ui.dialog.GenericConfirmDialog;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.watch.thinjam.BlueJay;
import com.eveningoutpost.dexdrip.xdrip;
import com.nightscout.core.barcode.NSBarcodeConfig;

import net.tribe7.common.base.Joiner;

import java.net.URI;
import java.util.Map;

/**
 * Processes a scanned QR/Code-128 payload, ported verbatim from the retired legacy
 * {@code Preferences.onActivityResult} scan handling.
 *
 * <p>Used by the Compose settings host so the scan actions on the Auto Configure and Data Source
 * screens keep working after the legacy settings activity is removed: xDrip settings QR
 * import/wizard, BlueJay pairing, Nightscout cloud config, NightLite config and the Dexcom Share
 * key.
 */
public class QrScanProcessor {

    private static final String TAG = "QrScanProcessor";

    private static byte[] staticKey;

    public static void handle(final Activity activity, final String scanFormat, final String scanContents, final byte[] scanRawBytes) {
        if (scanContents == null) {
            UserError.Log.d(TAG, "No scan results ");
            return;
        }

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);

        if ("QR_CODE".equals(scanFormat)) {

            if (QRcodeUtils.hasDecoderMarker(scanContents)) {
                installxDripPlusPreferencesFromQRCode(activity, prefs, scanContents);
                return;
            }

            try {
                if (BlueJay.processQRCode(scanRawBytes)) {
                    return;
                }
            } catch (Exception e) {
                // meh
            }

            final NSBarcodeConfig barcode = new NSBarcodeConfig(scanContents);
            if (barcode.hasMongoConfig()) {
                if (barcode.getMongoUri().isPresent()) {
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("cloud_storage_mongodb_uri", barcode.getMongoUri().get());
                    editor.putString("cloud_storage_mongodb_collection", barcode.getMongoCollection().or("entries"));
                    editor.putString("cloud_storage_mongodb_device_status_collection", barcode.getMongoDeviceStatusCollection().or("devicestatus"));
                    editor.putBoolean("cloud_storage_mongodb_enable", true);
                    editor.apply();
                }
                if (barcode.hasApiConfig()) {
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("cloud_storage_api_enable", true);
                    editor.putString("cloud_storage_api_base", Joiner.on(' ').join(barcode.getApiUris()));
                    editor.apply();
                }
            }
            if (barcode.hasApiConfig()) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("cloud_storage_api_enable", true);
                editor.putString("cloud_storage_api_base", Joiner.on(' ').join(barcode.getApiUris()));
                editor.apply();
            }

            if (barcode.hasMqttConfig()) {
                if (barcode.getMqttUri().isPresent()) {
                    URI uri = URI.create(barcode.getMqttUri().or(""));
                    if (uri.getUserInfo() != null) {
                        String[] userInfo = uri.getUserInfo().split(":");
                        if (userInfo.length == 2) {
                            String endpoint = uri.getScheme() + "://" + uri.getHost() + ":" + uri.getPort();
                            if (userInfo[0].length() > 0 && userInfo[1].length() > 0) {
                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putString("cloud_storage_mqtt_endpoint", endpoint);
                                editor.putString("cloud_storage_mqtt_user", userInfo[0]);
                                editor.putString("cloud_storage_mqtt_password", userInfo[1]);
                                editor.putBoolean("cloud_storage_mqtt_enable", true);
                                editor.apply();
                            }
                        }
                    }
                }
            }

            try {
                final NightLiteQR barcode2 = new NightLiteQR(scanContents);
                if (barcode2.hasNsLiteConfig()) {
                    UserError.Log.d(TAG, "NightLite QR code detected");
                    if (NightLiteEntry.setApi(barcode2.getApiUris())) {
                        JoH.static_toast_long("NightLite enabled");
                        NightLiteClient.doUpload();
                    }
                }
            } catch (Exception e) {
                UserError.Log.e(TAG, "Error processing NightLite QR code: " + e);
            }

        } else if ("CODE_128".equals(scanFormat)) {
            Log.d(TAG, "Setting serial number to: " + scanContents);
            prefs.edit().putString("share_key", scanContents).apply();
        }
    }

    private static void installxDripPlusPreferencesFromQRCode(final Activity activity, final SharedPreferences prefs, String data) {
        Log.d(TAG, "installing preferences from QRcode");
        try {
            Map<String, String> prefsmap = QRcodeUtils.decodeString(data);
            if (prefsmap != null) {
                if (prefsmap.containsKey(activity.getString(R.string.all_settings_wizard))) {
                    if (prefsmap.containsKey(activity.getString(R.string.wizard_key))
                            && prefsmap.containsKey(activity.getString(R.string.wizard_uuid))) {
                        staticKey = CipherUtils.hexToBytes(prefsmap.get(activity.getString(R.string.wizard_key)));

                        new WebAppHelper(new SettingsSupport.OnServiceTaskCompleted() {
                            @Override
                            public void onTaskCompleted(byte[] result) {
                                if (result.length > 0) {
                                    if ((staticKey == null) || (staticKey.length != 16)) {
                                        toast(activity, "Error processing security key");
                                    } else {
                                        byte[] plainbytes = JoH.decompressBytesToBytes(CipherUtils.decryptBytes(result, staticKey));
                                        staticKey = null;
                                        Log.d(TAG, "Plain bytes size: " + plainbytes.length);
                                        if (plainbytes.length > 0) {
                                            SdcardImportExport.storePreferencesFromBytes(plainbytes, activity.getApplicationContext());
                                        } else {
                                            toast(activity, "Error processing data - empty");
                                        }
                                    }
                                } else {
                                    toast(activity, "Error processing settings - no data - try again?");
                                }
                            }
                        }).executeOnExecutor(xdrip.executor, activity.getString(R.string.wserviceurl) + "/joh-getsw/" + prefsmap.get(activity.getString(R.string.wizard_uuid)));
                    } else {
                        Log.d(TAG, "Incorrectly formatted wizard pref");
                    }
                    return;
                }

                final String sb = SettingsSupport.getMapKeysString(prefsmap);
                final String msg = activity.getString(R.string.import_qr_code_warning) + sb;

                GenericConfirmDialog.show(activity, xdrip.gs(R.string.are_you_sure), msg, () -> {
                    final SharedPreferences.Editor editor = prefs.edit();
                    int changes = 0;
                    for (Map.Entry<String, String> entry : prefsmap.entrySet()) {
                        String key = entry.getKey();
                        String value = entry.getValue();
                        if (value.equals("true") || (value.equals("false"))) {
                            editor.putBoolean(key, Boolean.parseBoolean(value));
                            changes++;
                        } else if (!value.equals("null")) {
                            editor.putString(key, value);
                            changes++;
                        }
                    }
                    editor.apply();
                    ExtraLogTags.readPreference(Pref.getStringDefaultBlank("extra_tags_for_logging"));
                    Toast.makeText(activity.getApplicationContext(), "Loaded " + changes + " preferences from QR code", Toast.LENGTH_LONG).show();
                    PlusSyncService.clearandRestartSyncService(activity.getApplicationContext());
                    DesertSync.settingsChanged(); // refresh
                    InfoContentProvider.ping("pref");
                    if (prefs.getString("dex_collection_method", "").equals("Follower")) {
                        PlusSyncService.clearandRestartSyncService(activity.getApplicationContext());
                        GcmActivity.last_sync_request = 0;
                        GcmActivity.requestBGsync();
                    }
                });

            } else {
                android.util.Log.e(TAG, "Got null prefsmap during decode");
            }
        } catch (Exception e) {
            Log.e(TAG, "Got exception installing preferences");
        }
    }

    private static void toast(final Activity activity, final String msg) {
        try {
            activity.runOnUiThread(() -> Toast.makeText(activity.getApplicationContext(), msg, Toast.LENGTH_SHORT).show());
            android.util.Log.d(TAG, "Toast msg: " + msg);
        } catch (Exception e) {
            android.util.Log.e(TAG, "Couldn't display toast: " + msg);
        }
    }
}
