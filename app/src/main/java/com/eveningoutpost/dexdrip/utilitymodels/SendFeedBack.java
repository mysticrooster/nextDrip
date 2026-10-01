package com.eveningoutpost.dexdrip.utilitymodels;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.eveningoutpost.dexdrip.BaseAppCompatActivity;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.ui.secondary.SendFeedBackScreen;
import com.eveningoutpost.dexdrip.utils.framework.GzipRequestInterceptor;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.util.concurrent.TimeUnit;

import static com.eveningoutpost.dexdrip.watch.thinjam.BlueJayEntry.isNative;

/**
 * Feedback form (Track V V5, now Compose). The activity keeps the validation toasts, the
 * persisted contact reference and the OkHttp upload; the type/email dialogs are Compose.
 */
public class SendFeedBack extends BaseAppCompatActivity {

    public enum FeedbackAction { IGNORE, CONFIRM_EMAIL, SUBMIT }

    private static final String TAG = "jamorham feedback";
    private static final String FEEDBACK_CONTACT_REFERENCE = "feedback-contact-reference";

    private String type_of_message = "Unknown";
    private String send_url;

    private String log_data = "";
    private String initial_text = "";
    private boolean rating_visible = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        send_url = getString(isNative() ? R.string.qserviceurl : R.string.wserviceurl) + "/joh-feedback";

        Intent intent = getIntent();
        if (intent != null) {
            final Bundle bundle = intent.getExtras();
            if (bundle != null) {
                // TODO this probably should just use generic text method
                final String str = bundle.getString("request_translation");
                if (str != null) {
                    // don't extract string - english only
                    initial_text = "Dear developers, please may I request that you add translation capability for: " + str + "\n\n";
                    type_of_message = "Language request";
                }
                final String str2 = bundle.getString("generic_text");
                if (str2 != null) {
                    log_data = str2;
                    initial_text = log_data.length() > 300 ? "\n\nPlease describe what you think these logs may show. Explain the problem if there is one.\n\nAttached " + log_data.length() + " characters of log data. (hidden)\n\n" : log_data;
                    type_of_message = "Log Push";
                    rating_visible = false;
                }
            }
        }
        SendFeedBackScreen.installSendFeedBack(this);
    }

    public String initialText() {
        return initial_text;
    }

    public String initialContact() {
        return PersistentStore.getString(FEEDBACK_CONTACT_REFERENCE);
    }

    public String initialType() {
        return type_of_message;
    }

    public boolean ratingVisible() {
        return rating_visible;
    }

    public FeedbackAction prepareSend(final String yourtext, final String contact) {
        if (yourtext.length() == 0) {
            toast("No text entered - cannot send blank");
            return FeedbackAction.IGNORE;
        }
        if (contact.length() == 0) {
            toast("Without some contact info we cannot reply");
            return FeedbackAction.CONFIRM_EMAIL;
        }
        return FeedbackAction.SUBMIT;
    }

    public void submit(final String yourtext, final String contact, final float rating, final String type) {
        type_of_message = type;
        final OkHttpClient client = OkHttpWrapper.getClient().newBuilder()
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(new GzipRequestInterceptor())
                .build();

        PersistentStore.setString(FEEDBACK_CONTACT_REFERENCE, contact);
        toast("Sending..");

        try {
            final RequestBody formBody = new FormBody.Builder()
                    .add("contact", contact)
                    .add("body", yourtext + " \n\n===\nType: " + type_of_message + "\nLog data:\n\n" + log_data)  // Adding "Your text" and type to the log
                    .add("rating", String.valueOf(rating))
                    .add("type", type_of_message)
                    .build();
            new Thread(new Runnable() {
                public void run() {
                    try {
                        final Request request = new Request.Builder()
                                .url(send_url)
                                .post(formBody)
                                .build();
                        Log.i(TAG, "Sending feedback request");
                        final Response response = client.newCall(request).execute();
                        if (response.isSuccessful()) {
                            JoH.static_toast_long(response.body().string());
                            log_data = "";
                            //Home.toaststatic("Feedback sent successfully");
                            finish();
                        } else {
                            JoH.static_toast_short("Error sending feedback: " + response.message().toString());
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Got exception in execute: " + e.toString());
                        JoH.static_toast_short("Error with network connection");
                    }
                }
            }).start();
        } catch (Exception e) {
            JoH.static_toast_short(e.getMessage());
            Log.e(TAG, "General exception: " + e.toString());
        }
    }

    private void toast(final String msg) {
        try {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                }
            });
            Log.d(TAG, "Toast msg: " + msg);
        } catch (Exception e) {
            Log.e(TAG, "Couldn't display toast: " + msg);
        }
    }
}
