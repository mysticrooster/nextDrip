package com.eveningoutpost.dexdrip.eassist;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import androidx.databinding.ObservableArrayList;
import androidx.databinding.ObservableList;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.eveningoutpost.dexdrip.BaseAppCompatActivity;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.ui.secondary.EmergencyAssistScreen;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.Inevitable;
import com.eveningoutpost.dexdrip.utilitymodels.PrefsViewImpl;
import com.eveningoutpost.dexdrip.utils.LocationHelper;
import com.eveningoutpost.dexdrip.xdrip;

import java.util.List;

import static android.provider.ContactsContract.CommonDataKinds.Phone;
import static com.eveningoutpost.dexdrip.eassist.EmergencyAssist.EMERGENCY_ASSIST_PREF;
import static com.eveningoutpost.dexdrip.xdrip.gs;

/*
 * jamorham
 *
 * Display settings page for Emergency Assist Message feature
 *
 * Track V: rendered in Compose (EmergencyAssistScreen); this activity keeps the permission,
 * contact-picker and preference-validation flows.
 */

public class EmergencyAssistActivity extends BaseAppCompatActivity {

    private static final String TAG = EmergencyAssistActivity.class.getSimpleName();
    private static final int CONTACT_REQUEST_CODE = 46912;
    private static final int MY_PERMISSIONS_REQUEST_CONTACTS = 46913;
    private static final int MY_PERMISSIONS_REQUEST_SMS = 46914;

    public final EmergencyAssist model = new EmergencyAssist(EmergencyAssist.Reason.TESTING_FEATURE, Constants.HOUR_IN_MS);
    public final PrefsViewImpl prefs = new PrefsViewImpl();
    public final ContactModel contactModel = new ContactModel();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EmergencyAssistScreen.installEmergencyAssist(this);
        model.getLocation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        askPermissionIfNeeded();
    }

    private void askPermissionIfNeeded() {
        if (prefs.getbool(EMERGENCY_ASSIST_PREF)) {
            if (checkSMSPermission()) {
                checkLocationPermission();
            }
        }
    }

    private void checkLocationPermission() {
        LocationHelper.requestLocationForEmergencyMessage(this);
    }

    private boolean checkContactsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(),
                    Manifest.permission.READ_CONTACTS)
                    != PackageManager.PERMISSION_GRANTED) {
                final Activity activity = this;
                JoH.show_ok_dialog(activity, gs(R.string.please_allow_permission), gs(R.string.need_contacts_permission_to_select_message_recipients), new Runnable() {
                    @Override
                    public void run() {
                        ActivityCompat.requestPermissions(activity,
                                new String[]{Manifest.permission.READ_CONTACTS}, MY_PERMISSIONS_REQUEST_CONTACTS);
                    }
                });
                return false;
            }
        }
        return true;
    }

    private static boolean isSMSPermissionGranted() {
        return (ContextCompat.checkSelfPermission(xdrip.getAppContext(), Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED);
    }

    private synchronized boolean checkSMSPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!isSMSPermissionGranted()) {
                if (JoH.ratelimit("check-sms-permission", 2)) {
                    final Activity activity = this;
                    JoH.show_ok_dialog(activity, gs(R.string.please_allow_permission), "Need SMS permission to send text messages to your emergency contacts."
                            + "\n\n"
                            + "Warning this can cost money at normal telecoms rates!", () -> ActivityCompat.requestPermissions(activity,
                            new String[]{Manifest.permission.SEND_SMS}, MY_PERMISSIONS_REQUEST_SMS));
                }
                return false;
            }
        } else {
            JoH.show_ok_dialog(this, "Needs Android 6+", "This feature is not designed for Android versions < 6\nIf you want this on an older phone please create an issue on the xDrip issue tracker and request it.", null);

            return false;
        }
        return true;
    }

    public String prettyMinutes(String input) {
        if (input == null || input.length() == 0) return "";
        try {
            final int mins = Integer.parseInt(input);
            return JoH.niceTimeScalarShortWithDecimalHours(mins * Constants.MINUTE_IN_MS);

        } catch (NumberFormatException e) {
            return "";
        }

    }

    public void chooseContact() {
        if (checkContactsPermission()) {
            try {
                final Intent intent = new Intent(Intent.ACTION_PICK, Uri.parse("content://contacts"));
                intent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
                startActivityForResult(intent, CONTACT_REQUEST_CODE);
            } catch (ActivityNotFoundException e) {
                JoH.static_toast_long("Device doesn't have a contact picker!?");
            }
        }
    }

    public void testButton() {
        EmergencyAssist.test(EmergencyAssist.Reason.TESTING_FEATURE, Constants.HOUR_IN_MS);
    }

    public void masterEnable() {
        Inevitable.task("ea-master-enable", 100, new Runnable() {
            @Override
            public void run() {
                if (prefs.getbool(EMERGENCY_ASSIST_PREF)) {
                    prefs.setbool(EMERGENCY_ASSIST_PREF, checkSMSPermission() && atLeastOneContact());
                    checkLocationPermission();
                }
            }
        });

    }

    public static void checkPermissionRemoved() {
        if (EmergencyAssist.isEnabled() && !isSMSPermissionGranted()) {
            if (JoH.ratelimit("emergency-start-activity", 30)) {
                JoH.startActivity(EmergencyAssistActivity.class);
            }
            final String msg = "NEED SMS PERMISSION! - EMERGENCY FEATURE CANNOT WORK!!!";
            UserError.Log.wtf(TAG, msg);
            JoH.static_toast_long(msg);
        }
    }

    private boolean atLeastOneContact() {
        final boolean result = contactModel.items.size() > 0;
        if (!result) {
            JoH.static_toast_long("Add at least one contact below to send messages to");
        }
        return result;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent intent) {
        if (intent == null) return;
        if (requestCode == CONTACT_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                final Uri uri = intent.getData();
                final String[] projection = {Phone.NUMBER, Phone.DISPLAY_NAME};
                if (uri != null) {
                    try {
                        final Cursor cursor = getContentResolver().query(uri, projection,
                                null, null, null);
                        if (cursor != null) {
                            cursor.moveToFirst();

                            int numberColumnIndex = cursor.getColumnIndex(Phone.NUMBER);
                            String number = cursor.getString(numberColumnIndex);

                            int nameColumnIndex = cursor.getColumnIndex(Phone.DISPLAY_NAME_PRIMARY);
                            String name = cursor.getString(nameColumnIndex).trim();

                            if (name.length() > 0) {
                                if (number.length() > 5) {
                                    contactModel.add(name, number);
                                } else {
                                    JoH.static_toast_long("Cannot add " + name + " as number is invalid!");
                                }
                            } else {
                                JoH.static_toast_long("Cannot add as name is invalid");
                            }
                            cursor.close();
                        }
                    } catch (Exception e) {
                        final String msg = "Got error trying to read contact information: " + e;
                        UserError.Log.wtf(TAG, msg);
                        JoH.static_toast_long(msg);
                    }
                } else {
                    JoH.static_toast_long("Got null uri trying to read contact");
                }
            }
        }

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_REQUEST_CONTACTS) {
            if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                chooseContact(); // must be the only functionality which calls for permission
            } else {
                JoH.static_toast_long(this, "Cannot choose contact without read contacts permission");
            }
        }

        if (requestCode == MY_PERMISSIONS_REQUEST_SMS) {
            if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                prefs.setbool(EMERGENCY_ASSIST_PREF, true);
            } else {
                JoH.static_toast_long(this, "Cannot send any text messages without SMS permission");
            }
        }

    }

    public void removeContact(EmergencyContact contact) {
        contactModel.remove(contact);
        masterEnable();
    }

    // Contact Model

    public class ContactModel {
        public final ObservableList<EmergencyContact> items = new ObservableArrayList<>();

        ContactModel(List<EmergencyContact> items) {
            this.items.addAll(items);
        }

        ContactModel() {
            this(EmergencyContact.load());
        }

        void add(String name, String number) {
            items.add(new EmergencyContact(name, number));
            EmergencyContact.save(items);
        }

        public void remove(EmergencyContact emergencyContact) {
            items.remove(emergencyContact);
            EmergencyContact.save(items);
        }

    }

}
