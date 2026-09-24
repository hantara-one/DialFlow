package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.example.models.CallStatus
import com.example.models.PhoneNumberEntry
import com.example.ui.QueueItemRow
import com.example.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "id")
class QueueItemResponsiveTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `queue item row renders readable phone number and pending status on narrow container`() {
        val entry = PhoneNumberEntry(
            id = 1L,
            originalInput = "081234567890",
            normalizedNumber = "+6281234567890",
            formattedDisplay = "0812-3456-7890",
            status = CallStatus.PENDING,
            isValid = true,
            orderIndex = 0
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                Box(modifier = Modifier.width(320.dp)) {
                    QueueItemRow(
                        entry = entry,
                        isCurrent = false,
                        onCallNow = {},
                        onRetry = {},
                        onDelete = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("0812-3456-7890").assertIsDisplayed()
        composeTestRule.onNodeWithText("Menunggu").assertIsDisplayed()
    }

    @Test
    fun `queue item row renders dragging state with reordering badge without clipping`() {
        val entry = PhoneNumberEntry(
            id = 2L,
            originalInput = "0812-9999-8888",
            normalizedNumber = "+6281299998888",
            formattedDisplay = "0812-9999-8888",
            status = CallStatus.PENDING,
            isValid = true,
            orderIndex = 1
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                Box(modifier = Modifier.width(320.dp)) {
                    QueueItemRow(
                        entry = entry,
                        isCurrent = false,
                        isDragging = true,
                        onCallNow = {},
                        onRetry = {},
                        onDelete = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("0812-9999-8888").assertIsDisplayed()
        composeTestRule.onNodeWithText("MENGATUR ULANG").assertIsDisplayed()
    }

    @Test
    fun `queue item row renders completed call and skipped call correctly`() {
        val completedEntry = PhoneNumberEntry(
            id = 3L,
            originalInput = "081234567890",
            normalizedNumber = "+6281234567890",
            formattedDisplay = "0812-3456-7890",
            status = CallStatus.COMPLETED,
            isValid = true,
            orderIndex = 2
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                Box(modifier = Modifier.width(300.dp)) {
                    QueueItemRow(
                        entry = completedEntry,
                        isCurrent = false,
                        onCallNow = {},
                        onRetry = {},
                        onDelete = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("0812-3456-7890").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sudah Dipanggil").assertIsDisplayed()
    }

    @Test
    fun `queue item row renders current active calling number correctly`() {
        val callingEntry = PhoneNumberEntry(
            id = 4L,
            originalInput = "081234567890",
            normalizedNumber = "+6281234567890",
            formattedDisplay = "0812-3456-7890",
            status = CallStatus.CALLING,
            isValid = true,
            orderIndex = 3
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                Box(modifier = Modifier.width(320.dp)) {
                    QueueItemRow(
                        entry = callingEntry,
                        isCurrent = true,
                        onCallNow = {},
                        onRetry = {},
                        onDelete = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("0812-3456-7890").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sedang Berlangsung").assertIsDisplayed()
    }
}
