@file:JvmName("SendFeedBackScreen")

package com.eveningoutpost.dexdrip.ui.secondary

import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.eveningoutpost.dexdrip.R
import com.eveningoutpost.dexdrip.utilitymodels.SendFeedBack

private const val TYPE_UNKNOWN = "Unknown"

internal val FEEDBACK_TYPES = listOf("Bug Report", "Compliment", "Question", "Other")

/**
 * Track V V5 — `SendFeedBack`: feedback form (text, contact, rating) with the type and email
 * dialogs in Compose. The activity keeps the validation toasts, the persisted contact and the
 * OkHttp upload. Java entry point: [installSendFeedBack].
 */
fun installSendFeedBack(activity: SendFeedBack) {
    activity.setContent {
        SendFeedBackScreen(
            initialText = activity.initialText(),
            initialContact = activity.initialContact(),
            initialType = activity.initialType(),
            ratingVisible = activity.ratingVisible(),
            prepareSend = { text, contact -> activity.prepareSend(text, contact) },
            onSubmit = { text, contact, rating, type -> activity.submit(text, contact, rating, type) },
            onClose = { activity.finish() },
        )
    }
}

@Composable
internal fun SendFeedBackScreen(
    initialText: String,
    initialContact: String,
    initialType: String,
    ratingVisible: Boolean,
    prepareSend: (String, String) -> SendFeedBack.FeedbackAction,
    onSubmit: (text: String, contact: String, rating: Float, type: String) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    var text by remember { mutableStateOf(initialText) }
    var contact by remember { mutableStateOf(initialContact) }
    var rating by remember { mutableStateOf(0) }
    var type by remember { mutableStateOf(initialType) }
    var showTypeDialog by remember { mutableStateOf(initialType == TYPE_UNKNOWN) }
    var showEmailDialog by remember { mutableStateOf(false) }
    var emailInput by remember { mutableStateOf("") }

    fun proceed() {
        if (type == TYPE_UNKNOWN) {
            showTypeDialog = true
            return
        }
        onSubmit(text, contact, rating.toFloat(), type)
    }

    SecondaryScreen(title = context.getString(R.string.title_activity_send_feed_back), onBack = onClose) {
        Text(
            text = context.getString(R.string.log_confidential_note),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        )
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text(context.getString(R.string.please_enter_your_question_or_comments_here)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .testTag("feedback_text"),
        )
        OutlinedTextField(
            value = contact,
            onValueChange = { contact = it },
            singleLine = true,
            placeholder = { Text(context.getString(R.string.optional_contact_info_here_eg_email)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .testTag("feedback_contact"),
        )
        if (ratingVisible) {
            Text(
                text = context.getString(R.string.please_indicate_what_you_think_of_the_app_generally),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("feedback_rating"),
            ) {
                (1..5).forEach { star ->
                    Text(
                        text = if (star <= rating) "\u2605" else "\u2606",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier
                            .clickable { rating = if (rating == star) 0 else star }
                            .padding(horizontal = 4.dp)
                            .testTag("feedback_star_$star"),
                    )
                }
            }
        }
        Button(
            onClick = {
                when (prepareSend(text, contact)) {
                    SendFeedBack.FeedbackAction.IGNORE -> Unit
                    SendFeedBack.FeedbackAction.CONFIRM_EMAIL -> {
                        emailInput = ""
                        showEmailDialog = true
                    }
                    SendFeedBack.FeedbackAction.SUBMIT -> proceed()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .testTag("feedback_send"),
        ) {
            Text(context.getString(R.string.send_message))
        }
    }

    if (showTypeDialog) {
        AlertDialog(
            onDismissRequest = { showTypeDialog = false },
            title = { Text("Type of feedback?") },
            text = {
                Column {
                    FEEDBACK_TYPES.forEach { option ->
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    type = option
                                    showTypeDialog = false
                                }
                                .padding(vertical = 14.dp)
                                .testTag("feedback_type_$option"),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTypeDialog = false }) {
                    Text(context.getString(R.string.cancel))
                }
            },
        )
    }

    if (showEmailDialog) {
        AlertDialog(
            onDismissRequest = { showEmailDialog = false },
            title = { Text("Please supply email address or other contact reference") },
            text = {
                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.testTag("feedback_email_input"),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showEmailDialog = false
                        contact = emailInput
                        proceed()
                    },
                ) {
                    Text(context.getString(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmailDialog = false }) {
                    Text(context.getString(R.string.cancel))
                }
            },
        )
    }
}
