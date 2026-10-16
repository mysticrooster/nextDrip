package com.eveningoutpost.dexdrip.cgm.ilet.cloud

import com.eveningoutpost.dexdrip.models.UserError

/**
 * Keeps the Cognito session fresh.
 *
 * The id token is refreshed through the refresh token when it is expired or close to expiry,
 * and the refreshed tokens are persisted. If Cognito rejects the refresh token itself the
 * session is cleared so the UI shows signed-out and the user can sign in again; transient
 * failures (network etc.) leave the session in place to retry on a later cycle.
 */
class IletSessionKeeper(
    private val store: IletCredentialStore,
    private val auth: IletCognitoAuth = IletCognitoAuth(),
) {

    /** Set when the last refresh was rejected and the session was cleared. */
    @Volatile
    var lastRefreshClearedSession: Boolean = false
        private set

    /**
     * Ensure the session is usable, refreshing it when expired or within [skewSeconds] of
     * expiry. Returns true when a valid (existing or refreshed) session is present.
     */
    suspend fun refreshIfNeeded(skewSeconds: Long = REFRESH_SKEW_SECONDS): Boolean {
        val session = store.loadSession() ?: return false
        val now = System.currentTimeMillis() / 1000
        val exp = session.expiresAtEpochSeconds
        if (exp > 0 && now + skewSeconds < exp) return true
        return try {
            val refreshed = auth.refresh(session.refreshToken)
            store.storeSession(
                session.copy(
                    idToken = refreshed.idToken,
                    accessToken = refreshed.accessToken,
                    refreshToken = refreshed.refreshToken.ifEmpty { session.refreshToken },
                ),
            )
            lastRefreshClearedSession = false
            UserError.Log.d(TAG, "refreshed the Cognito session")
            true
        } catch (e: IletAuthError) {
            // Cognito rejected the refresh token: genuinely signed out. Clear the session so
            // the login screen prompts for credentials again.
            lastRefreshClearedSession = true
            store.clearSession()
            UserError.Log.e(TAG, "refresh token rejected; cleared session: ${e.message}")
            false
        } catch (e: Exception) {
            // Transient (network, timeout): keep the session and retry on a later cycle.
            UserError.Log.e(TAG, "session refresh failed, will retry: ${e.message}")
            false
        }
    }

    companion object {
        private const val TAG = "IletSessionKeeper"

        /** Refresh this far ahead of expiry so a later enrolment never uses a stale token. */
        const val REFRESH_SKEW_SECONDS = 300L
    }
}
