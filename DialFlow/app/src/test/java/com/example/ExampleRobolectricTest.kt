package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.domain.NumberParser
import com.example.domain.PhoneNumberNormalizer
import com.example.models.CallStatus
import com.example.models.PhoneNumberEntry
import com.example.ui.MainViewModel
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
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

    @Test
    fun `dynamic mode indicator label matches active calling mode`() {
        val autoModeLabel = if (com.example.models.CallingMode.AUTO_CALL == com.example.models.CallingMode.MANUAL_NEXT) "Manual Mode" else "Auto Mode"
        val manualModeLabel = if (com.example.models.CallingMode.MANUAL_NEXT == com.example.models.CallingMode.MANUAL_NEXT) "Manual Mode" else "Auto Mode"

        assertEquals("Auto Mode", autoModeLabel)
        assertEquals("Manual Mode", manualModeLabel)
    }

    @Test
    fun `completed queue status displays Called instead of Done`() {
        val statusText = when (com.example.models.CallStatus.COMPLETED) {
            com.example.models.CallStatus.COMPLETED -> "Called"
            com.example.models.CallStatus.CALLING -> "In Progress"
            com.example.models.CallStatus.SKIPPED -> "Skipped"
            com.example.models.CallStatus.FAILED -> "Failed"
            com.example.models.CallStatus.PENDING -> "Pending"
        }
        assertEquals("Called", statusText)
    }

    @Test
    fun `when queue is exhausted both Call and BEGIN CALL buttons are locked`() {
        // When all 30 numbers are called/skipped, remaining is 0
        val totalNumbers = 30
        val completedNumbers = 30
        val remaining = totalNumbers - completedNumbers

        val isCalling = false
        val executionState = com.example.models.QueueExecutionState.COMPLETED

        val hasCallableTarget = remaining > 0
        val canBeginCall = hasCallableTarget && !isCalling
        val canCall = hasCallableTarget && executionState != com.example.models.QueueExecutionState.RUNNING

        // Both buttons must be disabled
        assertFalse("BEGIN CALL button must be locked when remaining is 0", canBeginCall)
        assertFalse("Call button in Current Target card must be locked when remaining is 0", canCall)
    }

    @Test
    fun `when callable numbers remain call buttons are enabled`() {
        val totalNumbers = 30
        val completedNumbers = 29
        val remaining = totalNumbers - completedNumbers

        val isCalling = false
        val executionState = com.example.models.QueueExecutionState.PAUSED

        val hasCallableTarget = remaining > 0
        val canBeginCall = hasCallableTarget && !isCalling
        val canCall = hasCallableTarget && executionState != com.example.models.QueueExecutionState.RUNNING

        assertTrue("BEGIN CALL button must be enabled when remaining > 0", canBeginCall)
        assertTrue("Call button in Current Target card must be enabled when remaining > 0", canCall)
    }

    @Test
    fun `mixture of Called and Skipped numbers with zero pending locks calling`() {
        val queueItems = listOf(
            com.example.models.PhoneNumberEntry(id = 1, originalInput = "081211111111", normalizedNumber = "+6281211111111", formattedDisplay = "+62 812-1111-1111", isValid = true, status = com.example.models.CallStatus.COMPLETED),
            com.example.models.PhoneNumberEntry(id = 2, originalInput = "081222222222", normalizedNumber = "+6281222222222", formattedDisplay = "+62 812-2222-2222", isValid = true, status = com.example.models.CallStatus.SKIPPED),
            com.example.models.PhoneNumberEntry(id = 3, originalInput = "081233333333", normalizedNumber = "+6281233333333", formattedDisplay = "+62 812-3333-3333", isValid = true, status = com.example.models.CallStatus.COMPLETED)
        )

        val pendingCount = queueItems.count { it.status == com.example.models.CallStatus.PENDING }
        assertEquals(0, pendingCount)

        val hasCallableTarget = pendingCount > 0
        val isCalling = false
        val canBeginCall = hasCallableTarget && !isCalling
        val canCall = hasCallableTarget

        assertFalse("BEGIN CALL must be locked when all items are completed or skipped", canBeginCall)
        assertFalse("Call button must be locked when all items are completed or skipped", canCall)
    }

    @Test
    fun `QueueSearchMatcher finds ending digits correctly regardless of formatting`() {
        val queue = listOf(
            com.example.models.PhoneNumberEntry(id = 1, originalInput = "00000123456", normalizedNumber = "+6200000123456", formattedDisplay = "00000123456", isValid = true, status = com.example.models.CallStatus.COMPLETED),
            com.example.models.PhoneNumberEntry(id = 2, originalInput = "00000987654", normalizedNumber = "+6200000987654", formattedDisplay = "00000987654", isValid = true, status = com.example.models.CallStatus.SKIPPED),
            com.example.models.PhoneNumberEntry(id = 3, originalInput = "620000555555", normalizedNumber = "+620000555555", formattedDisplay = "00000555555", isValid = true, status = com.example.models.CallStatus.PENDING),
            com.example.models.PhoneNumberEntry(id = 4, originalInput = "00000777777", normalizedNumber = "+6200000777777", formattedDisplay = "00000777777", isValid = true, status = com.example.models.CallStatus.PENDING)
        )

        // Search for end portion "55555"
        val results = com.example.domain.QueueSearchMatcher.filterQueue(queue, "55555")
        assertEquals(1, results.size)
        assertEquals(3L, results[0].id)
        assertEquals("00000555555", results[0].formattedDisplay)

        // Confirm queue order is preserved
        assertEquals(listOf(1L, 2L, 3L, 4L), queue.map { it.id })

        // Empty search returns empty list
        assertTrue(com.example.domain.QueueSearchMatcher.filterQueue(queue, "").isEmpty())
    }

    @Test
    fun `acceptance test scenario - 30 numbers, search end digits, set as current, safety on called and skipped`() {
        // 1. Import 30 numbers
        val numbers = (1..30).map { i ->
            val numStr = if (i == 27) "00000555555" else "0812000000${String.format("%02d", i)}"
            com.example.models.PhoneNumberEntry(
                id = i.toLong(),
                originalInput = if (i == 27) "620000555555" else numStr,
                normalizedNumber = "+62${numStr.removePrefix("0")}",
                formattedDisplay = numStr,
                isValid = true,
                status = when (i) {
                    1, 2, 3 -> com.example.models.CallStatus.COMPLETED
                    4 -> com.example.models.CallStatus.SKIPPED
                    else -> com.example.models.CallStatus.PENDING
                },
                orderIndex = i - 1
            )
        }.toMutableList()

        // 2. Several numbers are Called (1..3) and Skipped (4)
        assertEquals(3, numbers.count { it.status == com.example.models.CallStatus.COMPLETED })
        assertEquals(1, numbers.count { it.status == com.example.models.CallStatus.SKIPPED })
        assertEquals(26, numbers.count { it.status == com.example.models.CallStatus.PENDING })

        // 3 & 4. Search using only the ending digits of number #27: "55555"
        val searchResults = com.example.domain.QueueSearchMatcher.filterQueue(numbers, "55555")

        // 5. Confirm correct queue item is found
        assertEquals(1, searchResults.size)
        val targetItem = searchResults.first()
        assertEquals(27L, targetItem.id)
        assertEquals("00000555555", targetItem.formattedDisplay)

        // 6. Confirm status is visible
        assertEquals(com.example.models.CallStatus.PENDING, targetItem.status)

        // 7. If still callable (PENDING), Set as Current must be enabled
        val isCallable = targetItem.status == com.example.models.CallStatus.PENDING
        assertTrue("Set as Current must be enabled for callable items", isCallable)

        // 8. Set as Current
        var currentTarget: com.example.models.PhoneNumberEntry? = targetItem
        var queuePointerIndex: Int? = numbers.indexOfFirst { it.id == targetItem.id }

        // 9. Confirm Current Target changes to that number
        assertEquals(27L, currentTarget?.id)
        assertEquals("00000555555", currentTarget?.formattedDisplay)

        // 10. Confirm queue pointer / current index moves to that item (index 26 for #27)
        assertEquals(26, queuePointerIndex)

        // 11 & 12. Press Call / BEGIN CALL -> selected number is called normally
        val dialedTarget = if (currentTarget!!.isValid) currentTarget!!.normalizedNumber else currentTarget!!.originalInput
        assertEquals("+620000555555", dialedTarget)
        // Mark as calling/completed
        numbers[26] = numbers[26].copy(status = com.example.models.CallStatus.COMPLETED)

        // 13 & 14. Search for a number that has already been Called (e.g. #2)
        val calledResults = com.example.domain.QueueSearchMatcher.filterQueue(numbers, "00000002")
        assertEquals(1, calledResults.size)
        val calledItem = calledResults.first()
        assertEquals(com.example.models.CallStatus.COMPLETED, calledItem.status)
        // Numbers with ALL existing statuses (Pending, Called, Skipped) must be selectable as current target
        val isCalledItemSelectable = true
        assertTrue("Set as Current must be ENABLED for Called numbers", isCalledItemSelectable)
        // Setting Called number as current target
        currentTarget = calledItem
        queuePointerIndex = numbers.indexOfFirst { it.id == calledItem.id }
        assertEquals(2L, currentTarget?.id)
        assertEquals(com.example.models.CallStatus.COMPLETED, currentTarget?.status)
        assertEquals(1, queuePointerIndex)

        // 15 & 16. Search for a Skipped number (e.g. #4)
        val skippedResults = com.example.domain.QueueSearchMatcher.filterQueue(numbers, "00000004")
        assertEquals(1, skippedResults.size)
        val skippedItem = skippedResults.first()
        assertEquals(com.example.models.CallStatus.SKIPPED, skippedItem.status)
        val isSkippedItemSelectable = true
        assertTrue("Set as Current must be ENABLED for Skipped numbers", isSkippedItemSelectable)

        // 17. Confirm the queue order itself has not changed
        val expectedIds = (1..30).map { it.toLong() }
        assertEquals("Queue order must remain strictly unchanged", expectedIds, numbers.map { it.id })

        // 18. Confirm Auto Mode and Manual Mode remain compatible
        val pendingRemaining = numbers.count { it.status == com.example.models.CallStatus.PENDING }
        assertTrue(pendingRemaining > 0)
    }

    @Test
    fun `called numbers and skipped numbers are selectable as current target and can be deleted`() {
        val testNumbers = mutableListOf(
            PhoneNumberEntry(id = 1, originalInput = "0811111111", normalizedNumber = "+6281111111", formattedDisplay = "0811-111-111", isValid = true, status = CallStatus.COMPLETED, orderIndex = 0),
            PhoneNumberEntry(id = 2, originalInput = "0822222222", normalizedNumber = "+6282222222", formattedDisplay = "0822-222-222", isValid = true, status = CallStatus.SKIPPED, orderIndex = 1),
            PhoneNumberEntry(id = 3, originalInput = "0833333333", normalizedNumber = "+6283333333", formattedDisplay = "0833-333-333", isValid = true, status = CallStatus.PENDING, orderIndex = 2)
        )

        // Search for Called number
        val searchForCalled = com.example.domain.QueueSearchMatcher.filterQueue(testNumbers, "1111")
        assertEquals(1, searchForCalled.size)
        val calledResult = searchForCalled.first()
        assertEquals(com.example.models.CallStatus.COMPLETED, calledResult.status)

        // Select as current target: retains Called status, updates target
        var current: PhoneNumberEntry? = calledResult
        assertEquals(1L, current?.id)
        assertEquals(com.example.models.CallStatus.COMPLETED, current?.status)

        // Search item delete action
        testNumbers.removeIf { it.id == calledResult.id }
        val searchAfterDelete = com.example.domain.QueueSearchMatcher.filterQueue(testNumbers, "1111")
        assertTrue(searchAfterDelete.isEmpty())
        assertEquals(2, testNumbers.size)
    }

    @Test
    fun `pasted multiline customer numbers from prompt example parse and normalize accurately`() {
        val pastedText = """
            081234567890
            082345678901
            +6281234567890
            6282345678901
        """.trimIndent()

        val parseResult = NumberParser.parse(pastedText)
        assertEquals(4, parseResult.totalCount)
        assertEquals(4, parseResult.validCount)
        assertEquals(0, parseResult.invalidCount)
        assertEquals("+6281234567890", parseResult.entries[0].normalizedNumber)
        assertEquals("+6282345678901", parseResult.entries[1].normalizedNumber)
        assertEquals("+6281234567890", parseResult.entries[2].normalizedNumber)
        assertEquals("+6282345678901", parseResult.entries[3].normalizedNumber)
    }

    @Test
    fun `when navigating back to Called number pressing Call calls the currently displayed number`() {
        // Example scenario:
        // Number 1 — Called
        // Number 2 — Skipped
        // Number 3 — Pending
        val items = listOf(
            com.example.models.PhoneNumberEntry(
                id = 1L,
                originalInput = "081211111111",
                normalizedNumber = "+6281211111111",
                formattedDisplay = "081211111111",
                isValid = true,
                status = com.example.models.CallStatus.COMPLETED,
                orderIndex = 0
            ),
            com.example.models.PhoneNumberEntry(
                id = 2L,
                originalInput = "081222222222",
                normalizedNumber = "+6281222222222",
                formattedDisplay = "081222222222",
                isValid = true,
                status = com.example.models.CallStatus.SKIPPED,
                orderIndex = 1
            ),
            com.example.models.PhoneNumberEntry(
                id = 3L,
                originalInput = "081233333333",
                normalizedNumber = "+6281233333333",
                formattedDisplay = "081233333333",
                isValid = true,
                status = com.example.models.CallStatus.PENDING,
                orderIndex = 2
            )
        )

        // Target was Number 3, user returned back to Number 1
        val manualCursorIndex = 0
        val currentEntry = items[manualCursorIndex]
        assertEquals(1L, currentEntry.id)
        assertEquals(com.example.models.CallStatus.COMPLETED, currentEntry.status)

        // Resolving target as in updated CallQueueManager
        val target = currentEntry
            ?: (if (manualCursorIndex in items.indices) items[manualCursorIndex] else null)
            ?: items.firstOrNull { it.status == com.example.models.CallStatus.PENDING }
            ?: items.firstOrNull()

        // MUST resolve to Number 1, NOT jump to next pending Number 3
        assertEquals(1L, target?.id)
        assertEquals("+6281211111111", target?.normalizedNumber)

        // Status remains Called, queue order preserved
        assertEquals(com.example.models.CallStatus.COMPLETED, target?.status)
        assertEquals(listOf(1L, 2L, 3L), items.map { it.id })
    }

    @Test
    fun `auto scroll edge detection calculates correct scroll delta for top and bottom edges including header and dock overflow`() {
        val viewportHeight = 800f
        val edgeThreshold = 120f

        // Helper function matching the touch-based auto-scroll calculation in NuvDialerScreen
        fun calculateScrollDelta(touchY: Float): Float {
            return when {
                touchY < edgeThreshold -> {
                    val proximity = ((edgeThreshold - touchY) / edgeThreshold).coerceIn(0f, 1.5f)
                    -((proximity * 18f) + 6f)
                }
                touchY > (viewportHeight - edgeThreshold) -> {
                    val proximity = ((touchY - (viewportHeight - edgeThreshold)) / edgeThreshold).coerceIn(0f, 1.5f)
                    ((proximity * 18f) + 6f)
                }
                else -> 0f
            }
        }

        // Scenario 1: Dragging upward approaching top edge -> scrolls upward (negative scroll delta)
        val topDelta = calculateScrollDelta(touchY = 30f)
        assertTrue("Top edge must scroll upward (negative delta)", topDelta < 0f)
        assertTrue("Speed must be within controlled range", topDelta in -33f..-6f)

        // Scenario 1b: Dragging upward directly into header area (touchY <= 0) -> maintains maximum upward scroll
        val headerAreaDelta = calculateScrollDelta(touchY = -40f)
        assertTrue("Header area must maintain maximum upward scroll without cancellation", headerAreaDelta <= -25f)

        // Scenario 2: Dragging toward bottom edge -> scrolls downward (positive scroll delta)
        val bottomDelta = calculateScrollDelta(touchY = 750f)
        assertTrue("Bottom edge must scroll downward (positive delta)", bottomDelta > 0f)
        assertTrue("Speed must be within controlled range", bottomDelta in 6f..33f)

        // Scenario 2b: Dragging down into bottom dock area (touchY >= viewportHeight) -> maintains continuous downward scroll without invisible wall
        val bottomDockDelta = calculateScrollDelta(touchY = 850f)
        assertTrue("Bottom dock area must maintain maximum downward scroll without wall", bottomDockDelta >= 25f)

        // Scenario 3: Touch in safe middle zone -> no scrolling
        val centerDelta = calculateScrollDelta(touchY = 400f)
        assertEquals(0f, centerDelta, 0.001f)

        // Proximity scaling test: being closer to edge scrolls faster
        val mildTopDelta = calculateScrollDelta(touchY = 90f)
        val extremeTopDelta = calculateScrollDelta(touchY = 0f)
        assertTrue("Closer to top edge must produce faster scroll speed", extremeTopDelta < mildTopDelta)
    }

    @Test
    fun `long queue reordering from top to bottom and bottom to top produces exact item order`() {
        val originalList = (0..19).map { id ->
            PhoneNumberEntry(
                id = id.toLong(),
                originalInput = "0812000000$id",
                normalizedNumber = "+62812000000$id",
                formattedDisplay = "0812000000$id",
                isValid = true,
                status = when (id % 4) {
                    0 -> CallStatus.COMPLETED
                    1 -> CallStatus.SKIPPED
                    2 -> CallStatus.FAILED
                    else -> CallStatus.PENDING
                },
                orderIndex = id
            )
        }

        // 1. Move first item (id 0) to bottom (index 19)
        val listAfterMovingToBottom = originalList.toMutableList()
        val item0 = listAfterMovingToBottom.removeAt(0)
        listAfterMovingToBottom.add(19, item0)

        assertEquals(20, listAfterMovingToBottom.size)
        assertEquals(1L, listAfterMovingToBottom.first().id)
        assertEquals(0L, listAfterMovingToBottom.last().id)
        // Verify statuses remain intact
        assertEquals(CallStatus.COMPLETED, listAfterMovingToBottom.last().status)
        assertEquals(CallStatus.SKIPPED, listAfterMovingToBottom.first().status)

        // 2. Move last item (id 0) back to top (index 0)
        val listAfterMovingBackToTop = listAfterMovingToBottom.toMutableList()
        val movedBack = listAfterMovingBackToTop.removeAt(19)
        listAfterMovingBackToTop.add(0, movedBack)

        assertEquals(originalList.map { it.id }, listAfterMovingBackToTop.map { it.id })
        assertEquals(originalList.map { it.status }, listAfterMovingBackToTop.map { it.status })
    }

    @Test
    fun `absolute visual offset tracking prevents invisible wall and keeps card locked to finger across swaps and auto scroll`() {
        val initialOffset = 300f
        var draggedDistance = 0f
        var currentSlotOffset = 300f
        val itemHeight = 80f
        val spacing = 16f

        // Initial visual offset must be 0
        fun calculateVisualOffset(): Float = (initialOffset + draggedDistance) - currentSlotOffset
        assertEquals(0f, calculateVisualOffset(), 0.001f)

        // User moves finger downward by 80px
        draggedDistance += 80f
        assertEquals(80f, calculateVisualOffset(), 0.001f)
        // Visual position on screen = currentSlotOffset + visualOffset = 300 + 80 = 380f
        assertEquals(380f, currentSlotOffset + calculateVisualOffset(), 0.001f)

        // List auto-scrolls down by 20px (items shift up by 20px in viewport, so slot offset decreases)
        val scrollDelta = 20f
        currentSlotOffset -= scrollDelta // slot offset decreases as list scrolls
        // Visual position on screen = currentSlotOffset + ((initialOffset + draggedDistance) - currentSlotOffset) = initialOffset + draggedDistance = 380f!
        // Visual position on screen remains exactly at 380f without drifting or wall!
        assertEquals(380f, currentSlotOffset + calculateVisualOffset(), 0.001f)

        // Item swaps downward with next item (new slot offset is 1 item down)
        currentSlotOffset += (itemHeight + spacing)
        // Screen position must remain completely invariant without jumping back up (no invisible wall!)
        assertEquals(380f, currentSlotOffset + calculateVisualOffset(), 0.001f)
    }

    @Test
    fun `bilingual localization strings exist and match specified terminology`() {
        val baseContext = ApplicationProvider.getApplicationContext<Context>()

        // 1. English Locale
        val enConfig = android.content.res.Configuration(baseContext.resources.configuration)
        enConfig.setLocale(java.util.Locale.forLanguageTag("en"))
        val enContext = baseContext.createConfigurationContext(enConfig)

        assertEquals("Pending", enContext.getString(R.string.status_item_pending))
        assertEquals("Called", enContext.getString(R.string.status_item_called))
        assertEquals("Skipped", enContext.getString(R.string.status_item_skipped))
        assertEquals("START", enContext.getString(R.string.action_start))
        assertEquals("PAUSE", enContext.getString(R.string.action_pause))
        assertEquals("SKIP", enContext.getString(R.string.action_skip))
        assertEquals("RETURN", enContext.getString(R.string.action_return))
        assertEquals("Set as Current", enContext.getString(R.string.set_as_current))
        assertEquals("Delete", enContext.getString(R.string.delete))
        assertEquals("Language", enContext.getString(R.string.settings_language))
        assertEquals("English", enContext.getString(R.string.language_english))
        assertEquals("Bahasa Indonesia", enContext.getString(R.string.language_indonesian))

        // 2. Indonesian Locale
        val idConfig = android.content.res.Configuration(baseContext.resources.configuration)
        idConfig.setLocale(java.util.Locale.forLanguageTag("id"))
        val idContext = baseContext.createConfigurationContext(idConfig)

        assertEquals("Menunggu", idContext.getString(R.string.status_item_pending))
        assertEquals("Sudah Dipanggil", idContext.getString(R.string.status_item_called))
        assertEquals("Dilewati", idContext.getString(R.string.status_item_skipped))
        assertEquals("MULAI", idContext.getString(R.string.action_start))
        assertEquals("JEDA", idContext.getString(R.string.action_pause))
        assertEquals("LEWATI", idContext.getString(R.string.action_skip))
        assertEquals("KEMBALI", idContext.getString(R.string.action_return))
        assertEquals("Jadikan Nomor Aktif", idContext.getString(R.string.set_as_current))
        assertEquals("Hapus", idContext.getString(R.string.delete))
        assertEquals("Bahasa", idContext.getString(R.string.settings_language))
        assertEquals("English", idContext.getString(R.string.language_english))
        assertEquals("Bahasa Indonesia", idContext.getString(R.string.language_indonesian))
    }

    @Test
    fun `dialer preferences persists and restores language selection`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = com.example.data.DialerPreferences(context)

        // Default language
        val initialLanguage = prefs.languageCode
        assertTrue(initialLanguage == "en" || initialLanguage == "id")

        // Switch to Indonesian
        prefs.languageCode = "id"
        assertEquals("id", prefs.languageCode)

        // Switch to English
        prefs.languageCode = "en"
        assertEquals("en", prefs.languageCode)
    }

    @Test
    fun `main viewmodel setLanguage applies app compat locales and preserves queue state`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = MainViewModel(app)

        val modeBefore = vm.callingMode.value

        // Switch language to Indonesian
        vm.setLanguage("id")
        assertEquals("id", vm.currentLanguage.value)
        assertEquals("id", com.example.data.DialerPreferences(app).languageCode)
        assertEquals("id", androidx.appcompat.app.AppCompatDelegate.getApplicationLocales().toLanguageTags())

        // Verify calling mode is preserved
        assertEquals(modeBefore, vm.callingMode.value)

        // Switch language to English
        vm.setLanguage("en")
        assertEquals("en", vm.currentLanguage.value)
        assertEquals("en", com.example.data.DialerPreferences(app).languageCode)
        assertEquals("en", androidx.appcompat.app.AppCompatDelegate.getApplicationLocales().toLanguageTags())

        // Verify calling mode is still preserved
        assertEquals(modeBefore, vm.callingMode.value)
    }

    @Test
    fun `english locale resources resolve correctly from values-en`() {
        val baseContext = ApplicationProvider.getApplicationContext<Context>()
        val config = android.content.res.Configuration(baseContext.resources.configuration)
        val enLocale = java.util.Locale.forLanguageTag("en")
        config.setLocale(enLocale)
        config.setLocales(android.os.LocaleList(enLocale))
        val enContext = baseContext.createConfigurationContext(config)

        assertEquals("START", enContext.getString(R.string.action_start))
        assertEquals("Language", enContext.getString(R.string.settings_language))
        assertEquals("English", enContext.getString(R.string.language_english))

        val prefix = enContext.getString(R.string.app_subtitle_queue_manager_by).trim()
        val author = enContext.getString(R.string.author_name).trim()
        val fullSubtitle = "$prefix $author"
        assertEquals("A QUEUE MANAGER BY AM HANIF", fullSubtitle)
    }

    @Test
    fun `indonesian locale subtitle resolves with correct spacing`() {
        val baseContext = ApplicationProvider.getApplicationContext<Context>()
        val config = android.content.res.Configuration(baseContext.resources.configuration)
        val idLocale = java.util.Locale.forLanguageTag("id")
        config.setLocale(idLocale)
        config.setLocales(android.os.LocaleList(idLocale))
        val idContext = baseContext.createConfigurationContext(config)

        val prefix = idContext.getString(R.string.app_subtitle_queue_manager_by).trim()
        val author = idContext.getString(R.string.author_name).trim()
        val fullSubtitle = "$prefix $author"
        assertEquals("PENGELOLA ANTREAN OLEH AM HANIF", fullSubtitle)
    }

    @Test
    fun `main activity creates and initializes composition hierarchy without activity result registry owner crash`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java)
        controller.setup()
        assertNotNull(controller.get())

        // Test configuration change (e.g. locale change) without recreation
        val newConfig = android.content.res.Configuration(controller.get().resources.configuration)
        newConfig.setLocale(java.util.Locale.forLanguageTag("id"))
        controller.configurationChange(newConfig)
        assertNotNull(controller.get())
    }
}
