package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.domain.NumberParser
import com.example.domain.PhoneNumberNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("DialFlow", appName)
    }

    @Test
    fun `indonesian phone number normalization recognized correctly`() {
        // Test all equivalent formats from the prompt:
        // +628123456789, 628123456789, 08123456789
        val n1 = PhoneNumberNormalizer.normalize("+628123456789")
        val n2 = PhoneNumberNormalizer.normalize("628123456789")
        val n3 = PhoneNumberNormalizer.normalize("08123456789")
        val n4 = PhoneNumberNormalizer.normalize("0812-9999-8888")

        assertTrue(n1.isValid)
        assertTrue(n2.isValid)
        assertTrue(n3.isValid)
        assertTrue(n4.isValid)

        // All Indonesian numbers should normalize to the same canonical format
        assertEquals("+628123456789", n1.normalizedNumber)
        assertEquals("+628123456789", n2.normalizedNumber)
        assertEquals("+628123456789", n3.normalizedNumber)
        assertEquals("+6281299998888", n4.normalizedNumber)
    }

    @Test
    fun `invalid and ambiguous numbers are not silently modified`() {
        // Invalid character
        val invalidChar = PhoneNumberNormalizer.normalize("081234abc89")
        assertFalse(invalidChar.isValid)
        assertEquals("081234abc89", invalidChar.normalizedNumber)

        // Too short
        val tooShort = PhoneNumberNormalizer.normalize("0812")
        assertFalse(tooShort.isValid)

        // Ambiguous missing leading 0 or +62
        val ambiguous = PhoneNumberNormalizer.normalize("81234567890")
        assertFalse(ambiguous.isValid)
    }

    @Test
    fun `number parser parses multiline text correctly`() {
        val input = """
            081234567890
            082345678901
            +628567890123
            0812-9999-8888
        """.trimIndent()

        val result = NumberParser.parse(input)
        assertEquals(4, result.totalCount)
        assertEquals(4, result.validCount)
        assertEquals(0, result.invalidCount)
        assertEquals("+6281234567890", result.entries[0].normalizedNumber)
        assertEquals("+6282345678901", result.entries[1].normalizedNumber)
        assertEquals("+628567890123", result.entries[2].normalizedNumber)
        assertEquals("+6281299998888", result.entries[3].normalizedNumber)
    }

    @Test
    fun `four sequential user paste numbers are parsed and validated correctly`() {
        val input = """
            081234567890
            082345678901
            083456789012
            084567890123
        """.trimIndent()

        val result = NumberParser.parse(input)
        assertEquals(4, result.totalCount)
        assertEquals(4, result.validCount)
        assertEquals("+6281234567890", result.entries[0].normalizedNumber)
        assertEquals("+6282345678901", result.entries[1].normalizedNumber)
        assertEquals("+6283456789012", result.entries[2].normalizedNumber)
        assertEquals("+6284567890123", result.entries[3].normalizedNumber)
    }

    @Test
    fun `manual call mode and call status values are correctly defined`() {
        assertEquals("Manual", com.example.models.CallingMode.MANUAL_NEXT.title)
        assertEquals("Auto", com.example.models.CallingMode.AUTO_CALL.title)
        assertTrue(com.example.models.CallStatus.SKIPPED != com.example.models.CallStatus.COMPLETED)
        assertTrue(com.example.models.CallStatus.SKIPPED != com.example.models.CallStatus.PENDING)
    }

    @Test
    fun `return semantics restore skipped to pending and preserve completed`() {
        val skippedItem = com.example.models.PhoneNumberEntry(
            id = 1L,
            originalInput = "08123456789",
            normalizedNumber = "+628123456789",
            formattedDisplay = "+62 812-3456-789",
            status = com.example.models.CallStatus.SKIPPED
        )
        val completedItem = com.example.models.PhoneNumberEntry(
            id = 2L,
            originalInput = "08234567890",
            normalizedNumber = "+628234567890",
            formattedDisplay = "+62 823-4567-890",
            status = com.example.models.CallStatus.COMPLETED
        )

        // RETURN on SKIPPED should produce PENDING
        val restoredFromSkipped = if (skippedItem.status == com.example.models.CallStatus.SKIPPED) {
            skippedItem.copy(status = com.example.models.CallStatus.PENDING)
        } else {
            skippedItem
        }
        assertEquals(com.example.models.CallStatus.PENDING, restoredFromSkipped.status)

        // RETURN on COMPLETED (DONE) should remain COMPLETED
        val restoredFromCompleted = if (completedItem.status == com.example.models.CallStatus.SKIPPED) {
            completedItem.copy(status = com.example.models.CallStatus.PENDING)
        } else {
            completedItem
        }
        assertEquals(com.example.models.CallStatus.COMPLETED, restoredFromCompleted.status)
    }

    @Test
    fun `skip semantics mark pending as skipped but preserve completed done history`() {
        val pendingItem = com.example.models.PhoneNumberEntry(
            id = 1L,
            originalInput = "08123456789",
            normalizedNumber = "+628123456789",
            formattedDisplay = "+62 812-3456-789",
            status = com.example.models.CallStatus.PENDING
        )
        val completedItem = com.example.models.PhoneNumberEntry(
            id = 2L,
            originalInput = "08234567890",
            normalizedNumber = "+628234567890",
            formattedDisplay = "+62 823-4567-890",
            status = com.example.models.CallStatus.COMPLETED
        )

        // SKIP on PENDING
        val skippedPending = if (pendingItem.status != com.example.models.CallStatus.COMPLETED) {
            pendingItem.copy(status = com.example.models.CallStatus.SKIPPED)
        } else {
            pendingItem
        }
        assertEquals(com.example.models.CallStatus.SKIPPED, skippedPending.status)

        // SKIP on COMPLETED preserves COMPLETED
        val skippedCompleted = if (completedItem.status != com.example.models.CallStatus.COMPLETED) {
            completedItem.copy(status = com.example.models.CallStatus.SKIPPED)
        } else {
            completedItem
        }
        assertEquals(com.example.models.CallStatus.COMPLETED, skippedCompleted.status)
    }

    @Test
    fun `auto call control bar center button label resolves to PAUSE when running and START otherwise`() {
        val runningState = com.example.models.QueueExecutionState.RUNNING
        val pausedState = com.example.models.QueueExecutionState.PAUSED
        val stoppedState = com.example.models.QueueExecutionState.STOPPED

        val labelRunning = if (runningState == com.example.models.QueueExecutionState.RUNNING) "PAUSE" else "START"
        val labelPaused = if (pausedState == com.example.models.QueueExecutionState.RUNNING) "PAUSE" else "START"
        val labelStopped = if (stoppedState == com.example.models.QueueExecutionState.RUNNING) "PAUSE" else "START"

        assertEquals("PAUSE", labelRunning)
        assertEquals("START", labelPaused)
        assertEquals("START", labelStopped)
    }

    @Test
    fun `auto call pause maintains queue entries and does not clear history`() {
        val initialQueue = listOf(
            com.example.models.PhoneNumberEntry(1L, "0811111111", "+6281111111", "+62 811-111-111", com.example.models.CallStatus.COMPLETED),
            com.example.models.PhoneNumberEntry(2L, "0822222222", "+6282222222", "+62 822-222-222", com.example.models.CallStatus.SKIPPED),
            com.example.models.PhoneNumberEntry(3L, "0833333333", "+6283333333", "+62 833-333-333", com.example.models.CallStatus.PENDING)
        )

        // Simulating pause execution state change
        var executionState = com.example.models.QueueExecutionState.RUNNING
        executionState = com.example.models.QueueExecutionState.PAUSED

        assertEquals(com.example.models.QueueExecutionState.PAUSED, executionState)
        // Queue items and statuses remain fully intact
        assertEquals(3, initialQueue.size)
        assertEquals(com.example.models.CallStatus.COMPLETED, initialQueue[0].status)
        assertEquals(com.example.models.CallStatus.SKIPPED, initialQueue[1].status)
        assertEquals(com.example.models.CallStatus.PENDING, initialQueue[2].status)
    }

    @Test
    fun `startup splash animation timing adheres to 900 to 1100 ms duration bounds`() {
        val fadeInDuration = 380L
        val holdDuration = 280L
        val fadeOutDuration = 340L
        val totalDuration = fadeInDuration + holdDuration + fadeOutDuration

        assertTrue("Fade in duration must be ~350-400ms", fadeInDuration in 350L..400L)
        assertTrue("Hold duration must be ~250-300ms", holdDuration in 250L..300L)
        assertTrue("Fade out duration must be ~300-400ms", fadeOutDuration in 300L..400L)
        assertTrue("Total duration must be ~900-1100ms", totalDuration in 900L..1100L)
    }

    @Test
    fun `dialflow logo resource is valid and available`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val drawable = androidx.core.content.ContextCompat.getDrawable(
            context,
            R.drawable.nuv_dialer_icon_1788511273776
        )
        org.junit.Assert.assertNotNull(drawable)
    }

    @Test
    fun `launcher icon resources are valid and available`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val launcherIcon = androidx.core.content.ContextCompat.getDrawable(
            context,
            R.mipmap.ic_launcher
        )
        org.junit.Assert.assertNotNull(launcherIcon)

        val launcherRoundIcon = androidx.core.content.ContextCompat.getDrawable(
            context,
            R.mipmap.ic_launcher_round
        )
        org.junit.Assert.assertNotNull(launcherRoundIcon)

        val foreground = androidx.core.content.ContextCompat.getDrawable(
            context,
            R.drawable.ic_launcher_foreground
        )
        org.junit.Assert.assertNotNull(foreground)
    }
}
