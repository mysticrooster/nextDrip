package com.eveningoutpost.dexdrip.cgm.ilet

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.eveningoutpost.dexdrip.BaseAppCompatActivity
import com.eveningoutpost.dexdrip.R
import com.eveningoutpost.dexdrip.cgm.ilet.cloud.IletCloudSigner
import com.eveningoutpost.dexdrip.cgm.ilet.cloud.IletCognitoAuth
import com.eveningoutpost.dexdrip.cgm.ilet.cloud.IletCredentialStore
import com.eveningoutpost.dexdrip.cgm.ilet.cloud.IletMfaRequired
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletChannelIdentity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Login host for the iLet integration. Collects the Beta Bionics account
 * credentials, runs Cognito USER_SRP_AUTH (TOTP as an optional second step) and
 * mints the pump credential. Read-only; secrets are never logged.
 */
class ILetLoginActivity : BaseAppCompatActivity() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // The store decrypts the keystore-backed file in its constructor, so it is
    // loaded on a background dispatcher and every control is gated until then.
    @Volatile
    private var store: IletCredentialStore? = null
    private lateinit var status: TextView
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var totp: EditText
    private lateinit var signIn: Button
    private lateinit var forgetSession: Button
    private lateinit var forgetCredential: Button
    private lateinit var forgetAll: Button
    private var busy: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.ilet_login_title)

        val pad = (16 * resources.displayMetrics.density).toInt()
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }
        column.addView(TextView(this).apply { setText(R.string.ilet_readonly_notice) })

        email = EditText(this).apply {
            setHint(R.string.ilet_email)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }
        password = EditText(this).apply {
            setHint(R.string.ilet_password)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        totp = EditText(this).apply {
            setHint(R.string.ilet_totp)
            inputType = InputType.TYPE_CLASS_NUMBER
            visibility = View.GONE
        }
        signIn = Button(this).apply {
            setText(R.string.ilet_sign_in)
            setOnClickListener { doSignIn() }
        }
        status = TextView(this).apply { text = getString(R.string.ilet_loading) }

        forgetSession = button(R.string.ilet_forget_session) { it.clearSession() }
        forgetCredential = button(R.string.ilet_forget_credential) { it.clearIdentity() }
        forgetAll = button(R.string.ilet_forget_all) { it.clearAll() }

        column.addView(email)
        column.addView(password)
        column.addView(totp)
        column.addView(signIn)
        column.addView(status)
        column.addView(forgetSession)
        column.addView(forgetCredential)
        column.addView(forgetAll)
        setContentView(ScrollView(this).apply { addView(column) })

        setControlsEnabled(false)
        scope.launch {
            val loaded = withContext(Dispatchers.IO) { IletCredentialStore.getInstance(this@ILetLoginActivity) }
            store = loaded
            setControlsEnabled(true)
            status.text = statusText()
        }
    }

    private fun setControlsEnabled(enabled: Boolean) {
        signIn.isEnabled = enabled
        forgetSession.isEnabled = enabled
        forgetCredential.isEnabled = enabled
        forgetAll.isEnabled = enabled
    }

    private fun button(label: Int, action: (IletCredentialStore) -> Unit) = Button(this).apply {
        setText(label)
        setOnClickListener {
            val current = store ?: return@setOnClickListener
            action(current)
            status.text = statusText()
        }
    }

    private fun doSignIn() {
        if (busy?.isActive == true) return
        val current = store ?: return
        val user = email.text.toString().trim()
        val pass = password.text.toString()
        if (user.isEmpty() || pass.isEmpty()) return
        signIn.isEnabled = false
        status.setText(R.string.ilet_signing_in)
        busy = scope.launch {
            status.text = try {
                val session = IletCognitoAuth().signIn(user, pass, totp.text.toString().ifBlank { null })
                current.storeSession(session)
                totp.visibility = View.GONE
                mintIdentity(current)
                getString(R.string.ilet_signed_in, session.username)
            } catch (e: IletMfaRequired) {
                totp.visibility = View.VISIBLE
                getString(R.string.ilet_mfa_needed)
            } catch (e: Exception) {
                getString(R.string.ilet_sign_in_failed, e.message ?: "")
            }
            password.setText("")
            signIn.isEnabled = true
        }
    }

    private fun statusText(): String {
        val summary = store?.summary() ?: return getString(R.string.ilet_loading)
        return buildString {
            append(getString(R.string.ilet_credential_label)).append(' ')
            append(if (summary["identity"] == true) getString(R.string.ilet_present) else getString(R.string.ilet_none))
            append("  -  ").append(getString(R.string.ilet_session_label)).append(' ')
            append(if (summary["session"] == true) (summary["session_user"] ?: getString(R.string.ilet_present)) else getString(R.string.ilet_none))
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    /**
     * Mint and store the pump credential from the current session.
     *
     * The app UUID is bound to the pump, so re-signing the *same* app UUID with
     * a fresh key is rejected by the attest route. Only mint when no credential
     * exists yet; to re-enrol, forget the credential first (which also rotates
     * the app UUID).
     */
    private suspend fun mintIdentity(store: IletCredentialStore): IletChannelIdentity? {
        store.loadIdentity()?.let { return it }
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
}
