package com.eveningoutpost.dexdrip.cgm.ilet.cloud

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig
import com.eveningoutpost.dexdrip.cgm.ilet.protocol.IletChannelIdentity
import org.junit.Before
import org.junit.Test
import org.robolectric.RuntimeEnvironment
import java.io.File

/**
 * Round-trip coverage for the credential store using a passthrough secret box,
 * so the JSON/serialization and "forget" semantics are pinned without depending
 * on the AndroidKeyStore.
 */
class IletCredentialStoreTest : RobolectricTestWithConfig() {

    private val box = object : IletSecretBox {
        override fun seal(plain: ByteArray): ByteArray = plain
        override fun open(sealed: ByteArray): ByteArray = sealed
    }

    private fun context() = RuntimeEnvironment.getApplication()

    @Before
    fun cleanFile() {
        File(context().filesDir, "ilet_credentials.bin").delete()
    }

    private fun store() = IletCredentialStore(context(), box)

    @Test
    fun appUuidIsStableAcrossReloads() {
        val first = store().appUuid()
        check(store().appUuid() == first) { "the app UUID is regenerated on reload" }
    }

    @Test
    fun identityRoundTrips() {
        val s = store()
        val uuid = s.appUuid()
        val identity = IletChannelIdentity.generate(cloudSignature = ByteArray(64), appUuid = uuid)
        s.storeIdentity(identity)

        val loaded = store().loadIdentity()
        check(loaded != null) { "identity did not survive a reload" }
        check(loaded!!.appUuid == uuid)
        check(loaded.privateKey.contentEquals(identity.privateKey))
        check(loaded.nonce.contentEquals(identity.nonce))
        check(loaded.cloudSignature.contentEquals(identity.cloudSignature))
    }

    @Test
    fun clearIdentityKeepsAppUuid() {
        val s = store()
        val uuid = s.appUuid()
        s.storeIdentity(IletChannelIdentity.generate(ByteArray(64), uuid))
        check(s.hasIdentity())

        s.clearIdentity()
        check(!s.hasIdentity()) { "identity was not cleared" }
        check(s.appUuid() == uuid) { "the app UUID must survive clearIdentity" }
    }

    @Test
    fun sessionRoundTripsAndClears() {
        val s = store()
        check(!s.hasSession())
        val session = IletCognitoSession("user@example.com", "id.token", "access", "refresh")
        s.storeSession(session)
        check(store().hasSession())

        val loaded = store().loadSession()!!
        check(loaded.username == "user@example.com")
        check(loaded.refreshToken == "refresh")

        s.clearSession()
        check(!store().hasSession())
    }

    @Test
    fun clearAllWipesEverything() {
        val s = store()
        val uuid = s.appUuid()
        s.storeIdentity(IletChannelIdentity.generate(ByteArray(64), uuid))
        s.storeSession(IletCognitoSession("u", "i", "a", "r"))

        s.clearAll()
        val reloaded = store()
        check(!reloaded.hasIdentity())
        check(!reloaded.hasSession())
        check(reloaded.appUuid() != uuid) { "clearAll should also drop the app UUID" }
    }
}
