package com.weifurry.spotfurry.data.applemusic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class AppleMusicPairingSessionTest {
    @Test
    fun generatedSessionUsesReadableCodeAndFiveMinuteExpiry() {
        val session =
            AppleMusicPairingSession.create(
                nowEpochMillis = 1_000L,
                random = Random(7)
            )

        assertTrue(session.code.matches(Regex("[2-9A-HJ-NP-Z]{4}-[2-9A-HJ-NP-Z]{4}")))
        assertEquals(301_000L, session.expiresAtEpochMillis)
    }

    @Test
    fun pairingUrlReturnsNullWhenBackendIsMissing() {
        val session =
            AppleMusicPairingSession(
                code = "WXYZ-5678",
                expiresAtEpochMillis = 0L
            )

        assertEquals(null, buildAppleMusicPairingUrl("", session))
    }

    @Test
    fun pairingUrlAddsProviderAndEncodedCode() {
        val session =
            AppleMusicPairingSession(
                code = "ABCD 1234",
                expiresAtEpochMillis = 0L
            )

        assertEquals(
            "https://auth.example.com/pair?code=ABCD+1234&provider=apple_music",
            buildAppleMusicPairingUrl("https://auth.example.com/pair", session)
        )
    }

    @Test
    fun pairingUrlPreservesExistingQueryParameters() {
        val session =
            AppleMusicPairingSession(
                code = "WXYZ-5678",
                expiresAtEpochMillis = 0L
            )

        assertEquals(
            "https://auth.example.com/pair?watch=true&code=WXYZ-5678&provider=apple_music",
            buildAppleMusicPairingUrl("https://auth.example.com/pair?watch=true", session)
        )
    }
}
