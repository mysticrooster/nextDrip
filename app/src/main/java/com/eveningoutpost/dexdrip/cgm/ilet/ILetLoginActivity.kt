package com.eveningoutpost.dexdrip.cgm.ilet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.eveningoutpost.dexdrip.cgm.ilet.cloud.IletCloudSigner
import com.eveningoutpost.dexdrip.cgm.ilet.cloud.IletCognitoSession
import com.eveningoutpost.dexdrip.cgm.ilet.cloud.IletCognitoAuth
import com.eveningoutpost.dexdrip.cgm.ilet.cloud.IletCredentialStore
import com.eveningoutpost.dexdrip.cgm.ilet.cloud.IletMfaRequired
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletChannelIdentity
import com.eveningoutpost.dexdrip.ui.theme.XdripTheme
import kotlinx.coroutines.launch

/**
 * Compose login/settings host for the iLet integration.
 *
 * Collects the Beta Bionics account credentials, runs Cognito USER_SRP_AUTH
 * (TOTP as an optional second step) and mints the pump credential. Read-only,
 * not medical software.
 */
class ILetLoginActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            XdripTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    ILetLoginScreen(
                        store = IletCredentialStore.getInstance(this),
                        auth = IletCognitoAuth(),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ILetLoginScreen(
    store: IletCredentialStore,
    auth: IletCognitoAuth,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var totp by remember { mutableStateOf("") }
    var mfaRequired by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf(statusText(store)) }
    val scope = rememberCoroutineScope()

    Scaffold(topBar = { TopAppBar(title = { Text("iLet account") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                "Read-only. This is not medical software and cannot deliver insulin or change " +
                    "therapy. Credentials are stored encrypted on this device and never logged.",
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            if (mfaRequired) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = totp,
                    onValueChange = { totp = it },
                    label = { Text("Authenticator code") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(16.dp))

            Button(
                enabled = !busy && email.isNotBlank() && password.isNotBlank(),
                onClick = {
                    busy = true
                    scope.launch {
                        status = try {
                            val session = auth.signIn(email, password, totp.ifBlank { null })
                            store.storeSession(session)
                            mfaRequired = false
                            val identity = mintIdentity(store)
                            "Signed in as ${session.username}. Credential ready for " +
                                identity.identity.take(8) + "..."
                        } catch (e: IletMfaRequired) {
                            mfaRequired = true
                            "Enter your authenticator code and sign in again."
                        } catch (e: Exception) {
                            "Sign in failed: ${e.message}"
                        }
                        busy = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (busy) "Signing in..." else "Sign in") }

            if (busy) {
                Spacer(Modifier.height(12.dp))
                CircularProgressIndicator()
            }

            Spacer(Modifier.height(24.dp))
            Text(status, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    store.clearSession()
                    status = statusText(store)
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Forget session (keep pump credential)") }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    store.clearIdentity()
                    status = statusText(store)
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Forget credential (keep app UUID)") }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    store.clearAll()
                    status = statusText(store)
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Forget everything") }
        }
    }
}

private fun statusText(store: IletCredentialStore): String {
    val summary = store.summary()
    return buildString {
        append("Credential: ")
        append(if (summary["identity"] == true) "present" else "none")
        append("  -  Session: ")
        append(if (summary["session"] == true) (summary["session_user"] ?: "signed in") else "none")
        (summary["app_uuid"] as? String)?.let { append("\nApp UUID: $it") }
    }
}

/** Mint and store the pump credential from the current session. */
private suspend fun mintIdentity(store: IletCredentialStore): IletChannelIdentity {
    val signer = IletCloudSigner(store)
    val draft = IletChannelIdentity.generate(cloudSignature = ByteArray(64), appUuid = store.appUuid())
    val signature = signer.sign(draft.signingInput())
    val identity = IletChannelIdentity(
        appUuid = draft.appUuid,
        privateKey = draft.privateKey,
        nonce = draft.nonce,
        cloudSignature = signature,
    )
    store.storeIdentity(identity)
    return identity
}
