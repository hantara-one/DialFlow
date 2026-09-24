package com.example

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.example.models.CallStatus
import com.example.models.ObservedCallState
import com.example.models.PhoneNumberEntry
import com.example.models.QueueExecutionState
import com.example.models.QueueSummary
import com.example.telephony.CallObserverSource
import com.example.ui.CurrentCallCard
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
class GreetingScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun dialer_ui_screenshot() {
        val testEntry = PhoneNumberEntry(
            id = 1L,
            originalInput = "0812-9999-8888",
            normalizedNumber = "+6281299998888",
            formattedDisplay = "0812-9999-8888",
            status = CallStatus.CALLING,
            isValid = true,
            orderIndex = 0
        )
        val summary = QueueSummary(
            total = 4,
            completed = 2,
            remaining = 1,
            failed = 0,
            skipped = 1,
            currentNumber = testEntry,
            executionState = QueueExecutionState.RUNNING
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                CurrentCallCard(
                    currentEntry = testEntry,
                    summary = summary,
                    executionState = QueueExecutionState.RUNNING,
                    observedCallState = ObservedCallState.ACTIVE,
                    observerSource = CallObserverSource.TELEPHONY_CALLBACK,
                    countdownRemaining = null,
                    onAdvanceNext = {},
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
    }
}
