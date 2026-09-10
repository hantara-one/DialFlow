package com.example.ui

import android.Manifest
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.PhoneInTalk
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.QueueEvent
import com.example.models.CallStatus
import com.example.models.CallingMode
import com.example.models.ObservedCallState
import com.example.models.PhoneNumberEntry
import com.example.models.QueueExecutionState
import com.example.models.QueueSummary
import com.example.telephony.CallObserverSource
import com.example.telephony.DialerRoleHelper
import com.example.ui.theme.StatusCalling
import com.example.ui.theme.StatusCompleted
import com.example.ui.theme.StatusFailed
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusSkipped

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NuvDialerScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val queueItems by viewModel.queueItems.collectAsStateWithLifecycle()
    val queueSummary by viewModel.queueSummary.collectAsStateWithLifecycle()
    val currentEntry by viewModel.currentEntry.collectAsStateWithLifecycle()
    val executionState by viewModel.executionState.collectAsStateWithLifecycle()
    val observedCallState by viewModel.observedCallState.collectAsStateWithLifecycle()
    val observerSource by viewModel.observerSource.collectAsStateWithLifecycle()
    val callingMode by viewModel.callingMode.collectAsStateWithLifecycle()
    val autoAdvanceDelay by viewModel.autoAdvanceDelaySeconds.collectAsStateWithLifecycle()
    val countdownRemaining by viewModel.countdownRemaining.collectAsStateWithLifecycle()
    val manualCursorIndex by viewModel.manualCursorIndex.collectAsStateWithLifecycle()
    val isDefaultDialer by viewModel.isDefaultDialer.collectAsStateWithLifecycle()
    val hasCallPhonePermission by viewModel.hasCallPhonePermission.collectAsStateWithLifecycle()
    val hasReadPhoneStatePermission by viewModel.hasReadPhoneStatePermission.collectAsStateWithLifecycle()
    val rawInputText by viewModel.rawInputText.collectAsStateWithLifecycle()
    val lastImportStats by viewModel.lastImportStats.collectAsStateWithLifecycle()

    var showInfoDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Activity launchers for permissions and default dialer role
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        viewModel.refreshTelephonyAndPermissionState()
    }

    val roleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.refreshTelephonyAndPermissionState()
    }

    // Handle background queue events for feedback
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is QueueEvent.CallPlaced -> {
                    snackbarHostState.showSnackbar("Dialing ${event.number} (${event.mode})")
                }
                is QueueEvent.PermissionNeeded -> {
                    snackbarHostState.showSnackbar("CALL_PHONE permission required for direct cellular calls")
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.CALL_PHONE,
                            Manifest.permission.READ_PHONE_STATE
                        )
                    )
                }
                is QueueEvent.Error -> {
                    snackbarHostState.showSnackbar("Error: ${event.message}")
                }
                is QueueEvent.QueueFinished -> {
                    snackbarHostState.showSnackbar("Queue finished! Completed ${event.totalCalls} calls.")
                }
                is QueueEvent.AutoAdvanceCountdown -> {
                    // countdown is shown in the current call card
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFFF7F2FA),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column {
                            Text(
                                text = "DialFlow",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1D1B20),
                                modifier = Modifier.testTag("app_title")
                            )
                            Text(
                                text = "QUEUE MANAGER",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF49454F),
                                letterSpacing = 1.2.sp
                            )
                        }

                        // Dialer Role Pill
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = if (isDefaultDialer) {
                                Color(0xFFE8DEF8)
                            } else {
                                Color(0xFFF3EDF7)
                            },
                            border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .clickable {
                                    val intent = DialerRoleHelper.createRequestDefaultDialerIntent(context)
                                    if (intent != null) {
                                        roleLauncher.launch(intent)
                                    } else {
                                        showInfoDialog = true
                                    }
                                }
                                .testTag("dialer_role_badge")
                        ) {
                            Text(
                                text = if (isDefaultDialer) "Default Dialer" else "Standard Mode",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDefaultDialer) {
                                    Color(0xFF1D192B)
                                } else {
                                    Color(0xFF49454F)
                                },
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Dialer Settings & Permissions",
                            tint = Color(0xFF49454F)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF7F2FA)
                )
            )
        },
        bottomBar = {
            // "Professional Polish" bottom dock matching the design footer
            Surface(
                color = Color(0xFFF3EDF7),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (callingMode == CallingMode.MANUAL_NEXT) {
                        // MANUAL CALL MODE: [ RETURN ] [ BEGIN CALL ] [ SKIP ]
                        val isCalling = executionState == QueueExecutionState.RUNNING
                        val canReturn = (manualCursorIndex ?: 0) > 0 && queueItems.isNotEmpty() && !isCalling
                        val canBeginCall = currentEntry != null && !isCalling
                        val canSkip = currentEntry != null && !isCalling && queueItems.isNotEmpty()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // RETURN button
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, if (canReturn) Color(0xFFCAC4D0) else Color(0xFFCAC4D0).copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable(enabled = canReturn) { viewModel.navigateReturn() }
                                        .testTag("return_queue_button")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.SkipPrevious,
                                            contentDescription = "Return",
                                            tint = if (canReturn) Color(0xFF49454F) else Color(0xFFCAC4D0)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "RETURN",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = if (canReturn) Color(0xFF49454F) else Color(0xFFCAC4D0)
                                )
                            }

                            // BEGIN CALL primary button
                            Column(
                                modifier = Modifier.weight(1.3f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (canBeginCall) Color(0xFF6750A4) else Color(0xFF6750A4).copy(alpha = 0.4f),
                                    shadowElevation = if (canBeginCall) 4.dp else 0.dp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable(enabled = canBeginCall) {
                                            if (!hasCallPhonePermission) {
                                                permissionLauncher.launch(
                                                    arrayOf(
                                                        Manifest.permission.CALL_PHONE,
                                                        Manifest.permission.READ_PHONE_STATE
                                                    )
                                                )
                                            } else {
                                                viewModel.beginCall()
                                            }
                                        }
                                        .testTag("begin_call_button")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Phone,
                                            contentDescription = "Begin Call",
                                            tint = if (canBeginCall) Color.White else Color.White.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "BEGIN CALL",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = if (canBeginCall) Color(0xFF6750A4) else Color(0xFF6750A4).copy(alpha = 0.6f)
                                )
                            }

                            // SKIP button
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, if (canSkip) Color(0xFFCAC4D0) else Color(0xFFCAC4D0).copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable(enabled = canSkip) { viewModel.skipCurrent() }
                                        .testTag("skip_queue_button")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.SkipNext,
                                            contentDescription = "Skip",
                                            tint = if (canSkip) Color(0xFF49454F) else Color(0xFFCAC4D0)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "SKIP",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = if (canSkip) Color(0xFF49454F) else Color(0xFFCAC4D0)
                                )
                            }
                        }
                    } else {
                        // AUTO CALL MODE: [ RETURN ] [ PAUSE / START ] [ SKIP ]
                        val isCalling = executionState == QueueExecutionState.RUNNING && (observedCallState == ObservedCallState.ACTIVE || observedCallState == ObservedCallState.DIALING)
                        val canReturn = (manualCursorIndex ?: 0) > 0 && queueItems.isNotEmpty() && !isCalling
                        val isRunning = executionState == QueueExecutionState.RUNNING
                        val canSkip = currentEntry != null && !isCalling && queueItems.isNotEmpty()

                        val centerButtonEnabled = if (isRunning) {
                            true
                        } else {
                            queueItems.isNotEmpty() && queueSummary.remaining > 0
                        }

                        val centerActionLabel = if (isRunning) "PAUSE" else "START"
                        val centerIcon = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // RETURN button
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, if (canReturn) Color(0xFFCAC4D0) else Color(0xFFCAC4D0).copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable(enabled = canReturn) { viewModel.navigateReturn() }
                                        .testTag("return_queue_button")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.SkipPrevious,
                                            contentDescription = "Return",
                                            tint = if (canReturn) Color(0xFF49454F) else Color(0xFFCAC4D0)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "RETURN",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = if (canReturn) Color(0xFF49454F) else Color(0xFFCAC4D0)
                                )
                            }

                            // PAUSE / START primary center button
                            Column(
                                modifier = Modifier.weight(1.3f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (centerButtonEnabled) Color(0xFF6750A4) else Color(0xFF6750A4).copy(alpha = 0.4f),
                                    shadowElevation = if (centerButtonEnabled) 4.dp else 0.dp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable(enabled = centerButtonEnabled) {
                                            if (isRunning) {
                                                viewModel.pauseQueue()
                                            } else {
                                                if (!hasCallPhonePermission) {
                                                    permissionLauncher.launch(
                                                        arrayOf(
                                                            Manifest.permission.CALL_PHONE,
                                                            Manifest.permission.READ_PHONE_STATE
                                                        )
                                                    )
                                                } else {
                                                    viewModel.startQueue()
                                                }
                                            }
                                        }
                                        .testTag(if (isRunning) "pause_queue_button" else "start_queue_button")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = centerIcon,
                                            contentDescription = centerActionLabel,
                                            tint = if (centerButtonEnabled) Color.White else Color.White.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = centerActionLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = if (centerButtonEnabled) Color(0xFF6750A4) else Color(0xFF6750A4).copy(alpha = 0.6f)
                                )
                            }

                            // SKIP button
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, if (canSkip) Color(0xFFCAC4D0) else Color(0xFFCAC4D0).copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable(enabled = canSkip) { viewModel.skipCurrent() }
                                        .testTag("skip_queue_button")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.SkipNext,
                                            contentDescription = "Skip",
                                            tint = if (canSkip) Color(0xFF49454F) else Color(0xFFCAC4D0)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "SKIP",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = if (canSkip) Color(0xFF49454F) else Color(0xFFCAC4D0)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Number Input & Import Area
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("import_numbers_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Centered and balanced header group: Paste Phone Numbers [AUTO-FORMAT]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Paste Phone Numbers",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF49454F)
                            )
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(100.dp),
                                color = Color(0xFFE8DEF8)
                            ) {
                                Text(
                                    text = "AUTO-FORMAT",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = Color(0xFF1D192B),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Compact text input area with comfortable touch target and typing space
                        OutlinedTextField(
                            value = rawInputText,
                            onValueChange = { viewModel.updateRawInputText(it) },
                            placeholder = {
                                Text(
                                    "081234567890\n082345678901\n083456789012\n084567890123",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    color = Color(0xFF49454F).copy(alpha = 0.6f)
                                )
                            },
                            textStyle = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                color = Color(0xFF1D1B20)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(124.dp)
                                .testTag("number_input_area"),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFF3F3F3),
                                unfocusedContainerColor = Color(0xFFF3F3F3),
                                focusedBorderColor = Color(0xFF6750A4),
                                unfocusedBorderColor = Color(0xFFCAC4D0)
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val count = rawInputText.lines().count { it.isNotBlank() }
                            val buttonText = if (count > 0) "Import $count Numbers" else "Import Numbers"

                            Button(
                                onClick = {
                                    viewModel.importNumbers()
                                },
                                enabled = rawInputText.isNotBlank(),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("import_numbers_button"),
                                shape = RoundedCornerShape(100.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF6750A4),
                                    contentColor = Color.White
                                )
                            ) {
                                Text(buttonText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }

                            if (rawInputText.isNotBlank()) {
                                OutlinedButton(
                                    onClick = { viewModel.updateRawInputText("") },
                                    shape = RoundedCornerShape(100.dp),
                                    border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
                                    modifier = Modifier
                                        .height(44.dp)
                                        .testTag("clear_input_text_button")
                                ) {
                                    Text("Clear", color = Color(0xFF49454F))
                                }
                            }
                        }

                        // Import Confirmation / Stats Banner
                        lastImportStats?.let { stats ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFEADDFF),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("import_stats_banner")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = stats,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF21005D),
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 8.dp, top = 2.dp)
                                    )
                                    IconButton(
                                        onClick = { viewModel.dismissImportStats() },
                                        modifier = Modifier
                                            .size(28.dp)
                                            .testTag("dismiss_import_stats_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Dismiss",
                                            tint = Color(0xFF21005D),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Calling Queue Metrics Summary Bar
            item {
                QueueMetricsBar(
                    summary = queueSummary,
                    queueItems = queueItems,
                    modifier = Modifier.testTag("queue_metrics_bar")
                )
            }

            // 3. Current Number & Status Panel
            item {
                CurrentCallCard(
                    currentEntry = currentEntry,
                    summary = queueSummary,
                    executionState = executionState,
                    observedCallState = observedCallState,
                    observerSource = observerSource,
                    callingMode = callingMode,
                    countdownRemaining = countdownRemaining,
                    queueItems = queueItems,
                    onAdvanceNext = { viewModel.advanceNext(success = true) },
                    onCallNext = {
                        if (!hasCallPhonePermission) {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.CALL_PHONE,
                                    Manifest.permission.READ_PHONE_STATE
                                )
                            )
                        } else {
                            viewModel.callNext()
                        }
                    },
                    modifier = Modifier.testTag("current_call_card")
                )
            }

            // 4. Calling Queue List Header
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF3EDF7),
                    border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Calling Queue",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1D1B20),
                                modifier = Modifier.testTag("calling_queue_header")
                            )
                            Surface(
                                shape = RoundedCornerShape(100.dp),
                                color = Color(0xFFE8DEF8)
                            ) {
                                Text(
                                    text = "${queueItems.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1D192B),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "${queueSummary.completed} / ${queueSummary.total}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF49454F)
                            )
                            if (queueItems.isNotEmpty()) {
                                TextButton(
                                    onClick = { viewModel.clearQueue() },
                                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFB3261E)),
                                    modifier = Modifier.testTag("clear_queue_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Clear Queue",
                                        tint = Color(0xFFB3261E),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("Clear", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // 5. Calling Queue Items
            if (queueItems.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PhoneInTalk,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "No numbers in queue",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = "Paste numbers above and click 'Import Numbers'",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            } else {
                items(
                    items = queueItems,
                    key = { it.id }
                ) { entry ->
                    QueueItemRow(
                        entry = entry,
                        isCurrent = currentEntry?.id == entry.id,
                        onCallNow = { viewModel.callSpecificEntry(entry) },
                        onRetry = { viewModel.retryEntry(entry.id) },
                        onDelete = { viewModel.deleteEntry(entry.id) },
                        modifier = Modifier.testTag("queue_item_${entry.id}")
                    )
                }
            }

            item {
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    // Architecture & Telephony Info Dialog
    if (showInfoDialog) {
        ArchitectureInfoDialog(
            isDefaultDialer = isDefaultDialer,
            onRequestDefaultDialer = {
                val intent = DialerRoleHelper.createRequestDefaultDialerIntent(context)
                if (intent != null) {
                    roleLauncher.launch(intent)
                }
            },
            onDismiss = { showInfoDialog = false }
        )
    }

    // Settings & Permissions Dialog
    if (showSettingsDialog) {
        DialerSettingsDialog(
            hasCallPhonePermission = hasCallPhonePermission,
            hasReadPhoneStatePermission = hasReadPhoneStatePermission,
            isDefaultDialer = isDefaultDialer,
            callingMode = callingMode,
            autoAdvanceDelaySeconds = autoAdvanceDelay,
            onCallingModeChange = { viewModel.setCallingMode(it) },
            onUpdateDelay = { viewModel.setAutoAdvanceDelay(it) },
            onRequestPermissions = {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.CALL_PHONE,
                        Manifest.permission.READ_PHONE_STATE
                    )
                )
            },
            onRequestDefaultDialer = {
                val intent = DialerRoleHelper.createRequestDefaultDialerIntent(context)
                if (intent != null) {
                    roleLauncher.launch(intent)
                }
            },
            onDismiss = { showSettingsDialog = false }
        )
    }
}

/**
 * Metric Cards: Total numbers, Completed, Skipped (if > 0), Remaining, Current number
 */
@Composable
fun QueueMetricsBar(
    summary: QueueSummary,
    modifier: Modifier = Modifier,
    queueItems: List<PhoneNumberEntry> = emptyList()
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        MetricCard(
            label = "TOTAL",
            value = summary.total.toString(),
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            label = "DONE",
            value = summary.completed.toString(),
            color = Color(0xFF386A20),
            modifier = Modifier.weight(1f)
        )
        if (summary.skipped > 0) {
            MetricCard(
                label = "SKIPPED",
                value = summary.skipped.toString(),
                color = Color(0xFFE65100),
                modifier = Modifier.weight(1f)
            )
        }
        MetricCard(
            label = "REMAIN",
            value = summary.remaining.toString(),
            modifier = Modifier.weight(1f)
        )
        val effectiveCurrent = summary.currentNumber ?: if (queueItems.isNotEmpty()) {
            val idx = (summary.completed + summary.skipped).coerceIn(0, queueItems.size - 1)
            queueItems.getOrNull(idx) ?: queueItems.firstOrNull()
        } else null
        val hasActiveCurrent = effectiveCurrent != null
        val currentPosition = effectiveCurrent?.let { curr ->
            val idx = queueItems.indexOfFirst { it.id == curr.id }
            if (idx != -1) "${idx + 1}" else "${curr.orderIndex + 1}"
        } ?: "—"
        MetricCard(
            label = "CURRENT",
            value = currentPosition,
            color = if (hasActiveCurrent) Color.White else Color(0xFF6750A4),
            isHighlighted = hasActiveCurrent,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun MetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF1D1B20),
    isHighlighted: Boolean = false
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted) Color(0xFF6750A4) else Color.White
        ),
        border = if (isHighlighted) null else BorderStroke(1.dp, Color(0xFFCAC4D0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                fontSize = 9.sp,
                letterSpacing = 0.5.sp,
                color = if (isHighlighted) Color.White.copy(alpha = 0.8f) else Color(0xFF49454F)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Current Call & Status Card
 */
@Composable
fun CurrentCallCard(
    currentEntry: PhoneNumberEntry?,
    summary: QueueSummary,
    executionState: QueueExecutionState,
    observedCallState: ObservedCallState,
    observerSource: CallObserverSource,
    callingMode: CallingMode = CallingMode.AUTO_CALL,
    countdownRemaining: Int?,
    queueItems: List<PhoneNumberEntry> = emptyList(),
    onAdvanceNext: () -> Unit = {},
    onCallNext: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Current & Status Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "CURRENT TARGET",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        color = Color(0xFF49454F)
                    )
                    Spacer(Modifier.height(2.dp))
                    val effectiveEntry = currentEntry ?: if (callingMode == CallingMode.MANUAL_NEXT && queueItems.isNotEmpty()) {
                        queueItems.firstOrNull()
                    } else null
                    val targetDisplay = effectiveEntry?.formattedDisplay?.ifBlank { null }
                        ?: effectiveEntry?.normalizedNumber?.ifBlank { null }
                        ?: effectiveEntry?.originalInput?.ifBlank { null }
                        ?: "No active number"
                    Text(
                        text = targetDisplay,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (effectiveEntry != null) Color(0xFF1D1B20) else Color(0xFF49454F),
                        modifier = Modifier.testTag("current_number_display")
                    )
                    if (effectiveEntry != null && effectiveEntry.originalInput != effectiveEntry.formattedDisplay) {
                        Text(
                            text = "Raw: ${effectiveEntry.originalInput}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF49454F)
                        )
                    } else if (effectiveEntry != null) {
                        Text(
                            text = "Standard Cellular • Native Dialer",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF49454F)
                        )
                    }
                }

                // Call & Telephony State Badges
                Column(horizontalAlignment = Alignment.End) {
                    val isCalling = executionState == QueueExecutionState.RUNNING
                    val effectiveEntry = currentEntry ?: if (callingMode == CallingMode.MANUAL_NEXT && queueItems.isNotEmpty()) {
                        queueItems.firstOrNull()
                    } else null
                    val statusText = when {
                        isCalling && observedCallState == ObservedCallState.ACTIVE -> "ACTIVE"
                        isCalling && observedCallState == ObservedCallState.DIALING -> "DIALING"
                        isCalling -> "CALLING"
                        effectiveEntry?.status == CallStatus.COMPLETED -> "DONE"
                        effectiveEntry?.status == CallStatus.SKIPPED -> "SKIPPED"
                        effectiveEntry?.status == CallStatus.FAILED -> "FAILED"
                        callingMode == CallingMode.MANUAL_NEXT && effectiveEntry?.status == CallStatus.PENDING -> "READY"
                        executionState == QueueExecutionState.PAUSED -> "PAUSED"
                        executionState == QueueExecutionState.STOPPED -> "STOPPED"
                        executionState == QueueExecutionState.COMPLETED -> "COMPLETED"
                        else -> "READY"
                    }

                    val badgeBg = when {
                        isCalling -> Color(0xFF6750A4)
                        effectiveEntry?.status == CallStatus.COMPLETED -> Color(0xFFC4EED0)
                        effectiveEntry?.status == CallStatus.SKIPPED -> Color(0xFFFFDCC2)
                        effectiveEntry?.status == CallStatus.FAILED -> Color(0xFFF9DEDC)
                        statusText == "READY" -> Color(0xFFEADDFF)
                        executionState == QueueExecutionState.PAUSED -> Color(0xFFEADDFF)
                        executionState == QueueExecutionState.COMPLETED -> Color(0xFFC4EED0)
                        else -> Color(0xFFE8DEF8)
                    }

                    val badgeDot = when {
                        isCalling -> Color.White
                        effectiveEntry?.status == CallStatus.COMPLETED -> Color(0xFF386A20)
                        effectiveEntry?.status == CallStatus.SKIPPED -> Color(0xFFE65100)
                        effectiveEntry?.status == CallStatus.FAILED -> Color(0xFFB3261E)
                        statusText == "READY" -> Color(0xFF6750A4)
                        executionState == QueueExecutionState.COMPLETED -> Color(0xFF386A20)
                        else -> Color(0xFF6750A4)
                    }

                    val badgeText = when {
                        isCalling -> Color.White
                        effectiveEntry?.status == CallStatus.COMPLETED -> Color(0xFF1E4620)
                        effectiveEntry?.status == CallStatus.SKIPPED -> Color(0xFF8C3A00)
                        effectiveEntry?.status == CallStatus.FAILED -> Color(0xFF410E0B)
                        statusText == "READY" -> Color(0xFF21005D)
                        else -> Color(0xFF1D192B)
                    }

                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = badgeBg
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(badgeDot)
                            )
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = badgeText,
                                modifier = Modifier.testTag("current_status_display")
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Telephony: ${observedCallState.name}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = Color(0xFF49454F),
                        modifier = Modifier.testTag("telephony_state_label")
                    )
                }
            }

            // Progress Bar & Ratio
            val progress = if (summary.total > 0) summary.completed.toFloat() / summary.total.toFloat() else 0f
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(100.dp)),
                    color = Color(0xFF6750A4),
                    trackColor = Color(0xFFF3EDF7)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Progress: ${summary.completed} of ${summary.total}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF49454F)
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF6750A4)
                    )
                }
            }

            // User-controlled Call Next & Cooldown Section
            val isCooldownActive = countdownRemaining != null && countdownRemaining > 0
            val showCallNext = if (callingMode == CallingMode.AUTO_CALL) {
                summary.remaining > 0 && (isCooldownActive || executionState == QueueExecutionState.PAUSED)
            } else {
                currentEntry != null && executionState != QueueExecutionState.RUNNING
            }

            if (showCallNext) {
                if (callingMode == CallingMode.AUTO_CALL) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isCooldownActive) Color(0xFFF3EDF7) else Color(0xFFEADDFF),
                        border = BorderStroke(1.dp, if (isCooldownActive) Color(0xFFCAC4D0) else Color(0xFFD0BCFF)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(if (isCooldownActive) "countdown_banner" else "auto_call_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (isCooldownActive) Icons.Default.Refresh else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = if (isCooldownActive) Color(0xFF49454F) else Color(0xFF21005D),
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = if (isCooldownActive) "Auto-advancing in ${countdownRemaining}s..." else "Queue Paused",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCooldownActive) Color(0xFF49454F) else Color(0xFF21005D)
                                    )
                                    Text(
                                        text = if (isCooldownActive) "Next number dials automatically" else "${summary.remaining} remaining in queue",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 11.sp,
                                        color = Color(0xFF49454F)
                                    )
                                }
                            }

                            Button(
                                onClick = if (isCooldownActive) onAdvanceNext else onCallNext,
                                shape = RoundedCornerShape(100.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF6750A4),
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .height(40.dp)
                                    .testTag(if (isCooldownActive) "skip_cooldown_button" else "resume_queue_button")
                            ) {
                                Icon(
                                    imageVector = if (isCooldownActive) Icons.AutoMirrored.Filled.ArrowForward else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (isCooldownActive) "Call Now" else "Resume",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    // MANUAL_CALL: Wait for user to explicitly press "Begin Call"
                    // Immediate availability, NO cooldown period.
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFEADDFF),
                        border = BorderStroke(1.dp, Color(0xFFD0BCFF)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("begin_call_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = Color(0xFF21005D),
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "Ready to Call",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF21005D)
                                    )
                                    Text(
                                        text = "${summary.remaining} remaining in queue • Tap Begin Call to start",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 11.sp,
                                        color = Color(0xFF49454F)
                                    )
                                }
                            }

                            Button(
                                onClick = onCallNext,
                                shape = RoundedCornerShape(100.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF6750A4),
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .height(40.dp)
                                    .testTag("begin_call_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Begin Call",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Single item in the calling queue list
 * Icons matching prompt:
 * ✓ Completed
 * → Current / Calling
 * ○ Pending
 * ✕ Failed
 * ↷ Skipped
 */
@Composable
fun QueueItemRow(
    entry: PhoneNumberEntry,
    isCurrent: Boolean,
    onCallNow: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) Color(0xFFF3EDF7) else Color.White
        ),
        border = BorderStroke(
            width = if (isCurrent) 2.dp else 1.dp,
            color = if (isCurrent) Color(0xFF6750A4) else Color(0xFFCAC4D0)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrent) 2.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Status Icon symbol matching requirements:
            StatusSymbolIcon(
                status = entry.status,
                isCurrent = isCurrent
            )

            // Number information
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.formattedDisplay.ifBlank { entry.normalizedNumber },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                    fontFamily = FontFamily.Monospace,
                    textDecoration = if (entry.status == CallStatus.COMPLETED) TextDecoration.None else null,
                    color = when {
                        !entry.isValid -> Color(0xFFB3261E)
                        entry.status == CallStatus.COMPLETED -> Color(0xFF1D1B20)
                        else -> Color(0xFF1D1B20)
                    }
                )
                if (entry.originalInput != entry.formattedDisplay) {
                    Text(
                        text = "Raw: ${entry.originalInput}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF49454F)
                    )
                }
                if (!entry.isValid && entry.validationMessage != null) {
                    Text(
                        text = entry.validationMessage,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFB3261E)
                    )
                }
            }

            // Status chip badge matching HTML styling
            Surface(
                shape = RoundedCornerShape(100.dp),
                color = when (entry.status) {
                    CallStatus.COMPLETED -> Color(0xFFC4EED0)
                    CallStatus.CALLING -> Color(0xFF6750A4)
                    CallStatus.SKIPPED -> Color(0xFFE8DEF8)
                    CallStatus.FAILED -> Color(0xFFF9DEDC)
                    CallStatus.PENDING -> Color(0xFFF3EDF7)
                }
            ) {
                Text(
                    text = when (entry.status) {
                        CallStatus.COMPLETED -> "Done"
                        CallStatus.CALLING -> "In Progress"
                        CallStatus.SKIPPED -> "Skipped"
                        CallStatus.FAILED -> "Failed"
                        CallStatus.PENDING -> "Pending"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = when (entry.status) {
                        CallStatus.COMPLETED -> Color(0xFF1E4620)
                        CallStatus.CALLING -> Color.White
                        CallStatus.SKIPPED -> Color(0xFF49454F)
                        CallStatus.FAILED -> Color(0xFF410E0B)
                        CallStatus.PENDING -> Color(0xFF49454F)
                    },
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }

            // Action: Direct Call / Delete
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                if (entry.status == CallStatus.PENDING || entry.status == CallStatus.FAILED || entry.status == CallStatus.SKIPPED) {
                    IconButton(
                        onClick = onCallNow,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Call ${entry.normalizedNumber}",
                            tint = Color(0xFF6750A4),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Delete entry",
                        tint = Color(0xFF49454F),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StatusSymbolIcon(
    status: CallStatus,
    isCurrent: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(
                when {
                    isCurrent || status == CallStatus.CALLING -> Color(0xFF6750A4)
                    status == CallStatus.COMPLETED -> Color(0xFFC4EED0)
                    status == CallStatus.SKIPPED -> Color(0xFFE8DEF8)
                    status == CallStatus.FAILED -> Color(0xFFF9DEDC)
                    else -> Color(0xFFF3EDF7)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        when {
            isCurrent || status == CallStatus.CALLING -> {
                // →
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Calling / Current",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            status == CallStatus.COMPLETED -> {
                // ✓
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Completed",
                    tint = Color(0xFF1E4620),
                    modifier = Modifier.size(18.dp)
                )
            }
            status == CallStatus.SKIPPED -> {
                // ↷
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Redo,
                    contentDescription = "Skipped",
                    tint = Color(0xFF49454F),
                    modifier = Modifier.size(16.dp)
                )
            }
            status == CallStatus.FAILED -> {
                // ✕
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Failed",
                    tint = Color(0xFFB3261E),
                    modifier = Modifier.size(16.dp)
                )
            }
            else -> {
                // ○
                Icon(
                    imageVector = Icons.Outlined.Circle,
                    contentDescription = "Pending",
                    tint = Color(0xFF79747E),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Architecture and Permissions Info Dialog
 */
@Composable
fun ArchitectureInfoDialog(
    isDefaultDialer: Boolean,
    onRequestDefaultDialer: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Android Telephony Architecture", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "DialFlow follows strict native Android telephony rules without questionable workarounds:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "1. Standard App Mode:\n• Initiates outgoing cellular calls using ACTION_CALL (requires CALL_PHONE).\n• TelephonyCallback/TelephonyManager detects call disconnect (OFFHOOK → IDLE transition).\n• Android restricts standard apps from reading fine-grained call states without default dialer status.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "2. Default Dialer Mode (ROLE_DIALER):\n• Unlocks Telecom InCallService.\n• InCallService receives exact real-time call states (DIALING, RINGING, ACTIVE, DISCONNECTED) for automatic sequential calling.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Status: ${if (isDefaultDialer) "Default Dialer Active (InCallService Enabled)" else "Operating in Standard Mode"}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isDefaultDialer) StatusCompleted else StatusSkipped
                )
            }
        },
        confirmButton = {
            if (!isDefaultDialer) {
                Button(onClick = {
                    onDismiss()
                    onRequestDefaultDialer()
                }) {
                    Text("Request Default Dialer")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        },
        dismissButton = {
            if (!isDefaultDialer) {
                TextButton(onClick = onDismiss) {
                    Text("Keep Standard Mode")
                }
            }
        }
    )
}

/**
 * Dialer Settings Dialog
 */
@Composable
fun DialerSettingsDialog(
    hasCallPhonePermission: Boolean,
    hasReadPhoneStatePermission: Boolean,
    isDefaultDialer: Boolean,
    callingMode: CallingMode,
    autoAdvanceDelaySeconds: Int,
    onCallingModeChange: (CallingMode) -> Unit,
    onUpdateDelay: (Int) -> Unit,
    onRequestPermissions: () -> Unit,
    onRequestDefaultDialer: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Settings & Permissions", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Calling Mode Setting
                Text(
                    text = "Calling Mode",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Choose how the dialer advances through your queued numbers.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CallingModeOptionCard(
                        selected = callingMode == CallingMode.AUTO_CALL,
                        title = "Auto",
                        description = "Automatically proceed to the next queued number after the current call ends and cooldown finishes.",
                        onClick = { onCallingModeChange(CallingMode.AUTO_CALL) },
                        testTag = "calling_mode_auto_call"
                    )
                    CallingModeOptionCard(
                        selected = callingMode == CallingMode.MANUAL_NEXT,
                        title = "Manual",
                        description = "Wait for the user to explicitly press the \"Begin Call\" button after the current call ends.",
                        onClick = { onCallingModeChange(CallingMode.MANUAL_NEXT) },
                        testTag = "calling_mode_manual_next"
                    )
                }

                HorizontalDivider()

                // 2. Cooldown Setting (Conditional on Calling Mode)
                if (callingMode == CallingMode.AUTO_CALL) {
                    Text(
                        text = "Auto-Advance Cooldown: $autoAdvanceDelaySeconds s",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Delay after call ends before dialing the next number in queue.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Slider(
                        value = autoAdvanceDelaySeconds.toFloat(),
                        onValueChange = { onUpdateDelay(it.toInt()) },
                        valueRange = 1f..15f,
                        steps = 13,
                        modifier = Modifier.testTag("cooldown_slider")
                    )
                } else {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cooldown_disabled_notice")
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Cooldown Disabled",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "In Manual mode, \"Begin Call\" is immediately available after each call ends without any waiting period.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                HorizontalDivider()

                // 3. Telephony Permissions
                Text(
                    text = "Telephony Permissions",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("CALL_PHONE", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            if (hasCallPhonePermission) "Granted (Direct Call)" else "Not Granted (Dialer fallback)",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (hasCallPhonePermission) StatusCompleted else StatusFailed
                        )
                    }
                    if (!hasCallPhonePermission) {
                        OutlinedButton(onClick = onRequestPermissions) {
                            Text("Grant", fontSize = 12.sp)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("READ_PHONE_STATE", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            if (hasReadPhoneStatePermission) "Granted (Call Monitoring)" else "Not Granted",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (hasReadPhoneStatePermission) StatusCompleted else StatusFailed
                        )
                    }
                    if (!hasReadPhoneStatePermission) {
                        OutlinedButton(onClick = onRequestPermissions) {
                            Text("Grant", fontSize = 12.sp)
                        }
                    }
                }

                HorizontalDivider()

                // 4. Default Dialer Role
                Text(
                    text = "Default Dialer Role",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (isDefaultDialer) "Default Dialer Active" else "Standard App Mode",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            if (isDefaultDialer) "InCallService monitors call lifecycle" else "Tap to set as default phone app",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    if (!isDefaultDialer) {
                        Button(onClick = onRequestDefaultDialer) {
                            Text("Set", fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
private fun CallingModeOptionCard(
    selected: Boolean,
    title: String,
    description: String,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) Color(0xFFEADDFF) else Color(0xFFF3EDF7),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) Color(0xFF6750A4) else Color(0xFFCAC4D0)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RadioButton(
                selected = selected,
                onClick = null
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) Color(0xFF21005D) else MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) Color(0xFF49454F) else MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
