package com.example.ui

import android.Manifest
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.R
import com.example.data.DialerPreferences
import com.example.domain.NumberParser
import com.example.domain.PhoneNumberNormalizer
import com.example.domain.QueueEvent
import com.example.models.CallStatus
import com.example.models.CallingMode
import com.example.models.ObservedCallState
import com.example.models.PhoneNumberEntry
import com.example.models.PhoneNumberFormatPreference
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
    val lastImportBatchStats by viewModel.lastImportBatchStats.collectAsStateWithLifecycle()
    val recentlyImportedIds by viewModel.recentlyImportedIds.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val currentLanguage by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val numberFormatPreference by viewModel.numberFormatPreference.collectAsStateWithLifecycle()

    var showInfoDialog by rememberSaveable { mutableStateOf(false) }
    var showSettingsDialog by rememberSaveable { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var highlightedEntryId by remember { mutableStateOf<Long?>(null) }
    var pendingDeleteEntry by remember { mutableStateOf<PhoneNumberEntry?>(null) }

    // Drag and Drop Queue Reordering state
    var draggedItemId by remember { mutableStateOf<Long?>(null) }
    var initialDragIndex by remember { mutableStateOf<Int?>(null) }
    var currentDragIndex by remember { mutableStateOf<Int?>(null) }
    var draggingItemInitialOffset by remember { mutableFloatStateOf(0f) }
    var draggedDistance by remember { mutableFloatStateOf(0f) }
    var currentTouchY by remember { mutableFloatStateOf(0f) }
    var lastVisualOffset by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val spacingPx = with(density) { 16.dp.toPx() }
    val edgeThresholdPx = with(density) { 120.dp.toPx() }

    val displayItems = remember { mutableStateListOf<PhoneNumberEntry>() }
    LaunchedEffect(queueItems) {
        if (draggedItemId == null) {
            val currentIds = displayItems.map { it.id }
            val newIds = queueItems.map { it.id }
            if (currentIds != newIds || displayItems.map { it.status } != queueItems.map { it.status }) {
                val newQueueIds = queueItems.map { it.id }.toSet()
                displayItems.removeAll { it.id !in newQueueIds }
                queueItems.forEachIndexed { index, item ->
                    val currentIndex = displayItems.indexOfFirst { it.id == item.id }
                    if (currentIndex == -1) {
                        if (index <= displayItems.size) {
                            displayItems.add(index, item)
                        } else {
                            displayItems.add(item)
                        }
                    } else {
                        if (currentIndex != index && index < displayItems.size) {
                            val moved = displayItems.removeAt(currentIndex)
                            displayItems.add(index, moved)
                        }
                        if (displayItems[index] != item) {
                            displayItems[index] = item
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(recentlyImportedIds) {
        if (recentlyImportedIds.isNotEmpty() && displayItems.isNotEmpty()) {
            val firstImportedIndex = displayItems.indexOfFirst { it.id in recentlyImportedIds }
            if (firstImportedIndex != -1) {
                val headerOffset = if (searchQuery.isNotBlank()) 6 else 5
                val targetScrollIndex = headerOffset + firstImportedIndex
                listState.animateScrollToItem(targetScrollIndex)
            }
        }
    }

    val draggingItemLayoutInfo by remember {
        derivedStateOf {
            draggedItemId?.let { id ->
                listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == id }
            }
        }
    }

    val currentDragVisualOffset by remember {
        derivedStateOf {
            draggingItemLayoutInfo?.let { item ->
                val offset = (draggingItemInitialOffset + draggedDistance) - item.offset
                lastVisualOffset = offset
                offset
            } ?: lastVisualOffset
        }
    }

    fun checkAndSwapItems() {
        val currentId = draggedItemId ?: return
        val draggedInfo = draggingItemLayoutInfo ?: return
        val visibleItems = listState.layoutInfo.visibleItemsInfo

        val itemSize = draggedInfo.size.toFloat()
        val draggedVisualCenter = draggingItemInitialOffset + draggedDistance + (itemSize / 2f)

        repeat(3) {
            val activeIdx = currentDragIndex ?: return
            var swapped = false

            // Check moving down (swap with next item)
            if (activeIdx < displayItems.lastIndex) {
                val nextItem = displayItems[activeIdx + 1]
                val nextInfo = visibleItems.firstOrNull { it.key == nextItem.id }
                val nextCenter = if (nextInfo != null) {
                    nextInfo.offset + (nextInfo.size / 2f)
                } else {
                    draggedInfo.offset + itemSize + spacingPx + (itemSize / 2f)
                }

                if (draggedVisualCenter > nextCenter) {
                    val item = displayItems.removeAt(activeIdx)
                    displayItems.add(activeIdx + 1, item)
                    currentDragIndex = activeIdx + 1
                    swapped = true
                }
            }

            // Check moving up (swap with previous item)
            if (!swapped && activeIdx > 0) {
                val prevItem = displayItems[activeIdx - 1]
                val prevInfo = visibleItems.firstOrNull { it.key == prevItem.id }
                val prevCenter = if (prevInfo != null) {
                    prevInfo.offset + (prevInfo.size / 2f)
                } else {
                    draggedInfo.offset - spacingPx - (itemSize / 2f)
                }

                if (draggedVisualCenter < prevCenter) {
                    val item = displayItems.removeAt(activeIdx)
                    displayItems.add(activeIdx - 1, item)
                    currentDragIndex = activeIdx - 1
                    swapped = true
                }
            }

            if (!swapped) return
        }
    }

    fun commitDrag() {
        val start = initialDragIndex
        val end = currentDragIndex
        if (start != null && end != null && start != end) {
            viewModel.reorderQueue(start, end)
        }
        draggedItemId = null
        initialDragIndex = null
        currentDragIndex = null
        draggedDistance = 0f
        draggingItemInitialOffset = 0f
        currentTouchY = 0f
        lastVisualOffset = 0f
    }

    // Continuous auto-scroll loop when finger/pointer is near top or bottom edges
    LaunchedEffect(draggedItemId) {
        val currentId = draggedItemId ?: return@LaunchedEffect
        while (draggedItemId == currentId) {
            val layoutInfo = listState.layoutInfo
            val viewportHeight = layoutInfo.viewportSize.height.toFloat()
            if (viewportHeight > 0f) {
                val touchY = currentTouchY

                val scrollDelta = when {
                    // Top edge activation zone: finger is near top of list or extended into header area
                    touchY < edgeThresholdPx -> {
                        val proximity = ((edgeThresholdPx - touchY) / edgeThresholdPx).coerceIn(0f, 1.5f)
                        -((proximity * 18f) + 6f)
                    }
                    // Bottom edge activation zone: finger is near bottom of list or extended into bottom dock
                    touchY > (viewportHeight - edgeThresholdPx) -> {
                        val proximity = ((touchY - (viewportHeight - edgeThresholdPx)) / edgeThresholdPx).coerceIn(0f, 1.5f)
                        ((proximity * 18f) + 6f)
                    }
                    else -> 0f
                }

                if (scrollDelta != 0f) {
                    val scrolled = listState.scrollBy(scrollDelta)
                    if (scrolled != 0f) {
                        checkAndSwapItems()
                    }
                }
            }
            delay(16)
        }
    }
    val showBackToTop by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 2 || (listState.firstVisibleItemIndex == 2 && listState.firstVisibleItemScrollOffset > 100)
        }
    }

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
                    Column {
                        Text(
                            text = "DialFlow",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1D1B20),
                            modifier = Modifier.testTag("app_title")
                        )
                        Text(
                            text = buildAnnotatedString {
                                val prefix = stringResource(R.string.app_subtitle_queue_manager_by).trim()
                                val author = stringResource(R.string.author_name).trim()
                                append(prefix)
                                append(" ")
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                    append(author)
                                }
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF49454F),
                            letterSpacing = 1.2.sp
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.desc_settings_button),
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
                        val hasCallableTarget = (currentEntry != null || queueSummary.remaining > 0) && queueItems.isNotEmpty()
                        val canBeginCall = hasCallableTarget && !isCalling
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
                                            contentDescription = stringResource(R.string.desc_return),
                                            tint = if (canReturn) Color(0xFF49454F) else Color(0xFFCAC4D0)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.action_return),
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
                                            contentDescription = stringResource(R.string.desc_begin_call),
                                            tint = if (canBeginCall) Color.White else Color.White.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.action_begin_call),
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
                                            contentDescription = stringResource(R.string.desc_skip),
                                            tint = if (canSkip) Color(0xFF49454F) else Color(0xFFCAC4D0)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.action_skip),
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
                            queueItems.isNotEmpty() && (queueSummary.remaining > 0 || currentEntry != null)
                        }

                        val centerActionLabel = if (isRunning) stringResource(R.string.action_pause) else stringResource(R.string.action_start)
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
                                            contentDescription = stringResource(R.string.desc_return),
                                            tint = if (canReturn) Color(0xFF49454F) else Color(0xFFCAC4D0)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.action_return),
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
                                            contentDescription = stringResource(R.string.desc_skip),
                                            tint = if (canSkip) Color(0xFF49454F) else Color(0xFFCAC4D0)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.action_skip),
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .pointerInput(displayItems) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { offset ->
                                val hitItem = listState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
                                    val itemTop = item.offset
                                    val itemBottom = item.offset + item.size
                                    offset.y.toInt() in (itemTop - 8)..(itemBottom + 8)
                                }
                                val hitQueueItem = displayItems.firstOrNull { it.id == hitItem?.key }
                                if (hitQueueItem != null && hitItem != null) {
                                    draggedItemId = hitQueueItem.id
                                    val idx = displayItems.indexOfFirst { it.id == hitQueueItem.id }
                                    initialDragIndex = idx
                                    currentDragIndex = idx
                                    draggingItemInitialOffset = hitItem.offset.toFloat()
                                    draggedDistance = 0f
                                    currentTouchY = offset.y
                                    lastVisualOffset = 0f
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                if (draggedItemId != null) {
                                    currentTouchY = change.position.y
                                    draggedDistance += dragAmount.y
                                    checkAndSwapItems()
                                }
                            },
                            onDragEnd = {
                                commitDrag()
                            },
                            onDragCancel = {
                                commitDrag()
                            }
                        )
                    },
                contentPadding = PaddingValues(top = 12.dp, bottom = 84.dp),
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
                                text = stringResource(R.string.paste_phone_numbers),
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
                                    text = stringResource(R.string.auto_format),
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
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone
                            ),
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
                            val count = remember(rawInputText) { NumberParser.previewCount(rawInputText) }
                            val buttonText = if (count > 0) {
                                stringResource(R.string.import_n_numbers, count)
                            } else {
                                stringResource(R.string.import_numbers)
                            }

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
                                    Text(stringResource(R.string.clear), color = Color(0xFF49454F))
                                }
                            }
                        }

                        // Import Confirmation / Stats Banner
                        lastImportStats?.let { stats ->
                            val displayText = lastImportBatchStats?.let { b ->
                                stringResource(R.string.import_stats_format, b.totalCount, b.validCount, b.invalidCount)
                            } ?: stats
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
                                        text = displayText,
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
                                            contentDescription = stringResource(R.string.dismiss),
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
                    numberFormatPreference = numberFormatPreference,
                    onAdvanceNext = { viewModel.advanceNext(success = true) },
                    onCallNext = {
                        if (currentEntry == null && queueItems.isEmpty()) return@CurrentCallCard
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

            // Search Bar positioned between Current Target card and Calling Queue
            item {
                QueueSearchBar(
                    query = searchQuery,
                    onQueryChange = { viewModel.updateSearchQuery(it) },
                    onClear = { viewModel.clearSearchQuery() },
                    modifier = Modifier.testTag("queue_search_bar")
                )
            }

            // Search Results displayed below search bar when query is entered
            if (searchQuery.isNotBlank()) {
                item {
                    SearchResultsSection(
                        query = searchQuery,
                        matchingItems = searchResults,
                        allQueueItems = queueItems,
                        currentEntry = currentEntry,
                        numberFormatPreference = numberFormatPreference,
                        onSetAsCurrent = { entry -> viewModel.setCurrentTarget(entry) },
                        onMoveToNumber = { entry ->
                            val isAlreadyVisible = listState.layoutInfo.visibleItemsInfo.any { it.key == entry.id }
                            highlightedEntryId = entry.id
                            coroutineScope.launch {
                                if (!isAlreadyVisible) {
                                    val queueIndex = queueItems.indexOfFirst { it.id == entry.id }
                                    if (queueIndex != -1) {
                                        val headerCount = if (searchQuery.isNotBlank()) 6 else 5
                                        listState.animateScrollToItem(headerCount + queueIndex)
                                    }
                                }
                                delay(3000)
                                if (highlightedEntryId == entry.id) {
                                    highlightedEntryId = null
                                }
                            }
                        },
                        onDeleteRequest = { entry ->
                            pendingDeleteEntry = entry
                        },
                        onClearSearch = { viewModel.clearSearchQuery() }
                    )
                }
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
                                text = stringResource(R.string.calling_queue),
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
                                        contentDescription = stringResource(R.string.clear_queue),
                                        tint = Color(0xFFB3261E),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(stringResource(R.string.clear), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
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
                                text = stringResource(R.string.no_numbers_in_queue),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = stringResource(R.string.paste_numbers_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            } else {
                items(
                    items = displayItems,
                    key = { it.id }
                ) { entry ->
                    val isDragging = draggedItemId == entry.id
                    val offsetForThisItem = if (isDragging) currentDragVisualOffset else 0f
                    val isRecentlyImported = recentlyImportedIds.contains(entry.id)
                    val itemModifier = if (isDragging) {
                        Modifier.zIndex(10f)
                    } else {
                        Modifier
                            .zIndex(1f)
                            .animateItem(
                                fadeInSpec = tween(
                                    durationMillis = 400,
                                    easing = FastOutSlowInEasing
                                ),
                                fadeOutSpec = tween(
                                    durationMillis = 250
                                ),
                                placementSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )
                    }
                    QueueItemRow(
                        entry = entry,
                        isCurrent = currentEntry?.id == entry.id,
                        isHighlighted = highlightedEntryId == entry.id,
                        isRecentlyImported = isRecentlyImported,
                        isDragging = isDragging,
                        dragOffsetY = offsetForThisItem,
                        numberFormatPreference = numberFormatPreference,
                        onCallNow = { viewModel.callSpecificEntry(entry) },
                        onRetry = { viewModel.retryEntry(entry.id) },
                        onDelete = { viewModel.deleteEntry(entry.id) },
                        onClick = { viewModel.setCurrentTarget(entry) },
                        modifier = itemModifier.testTag("queue_item_${entry.id}")
                    )
                }
            }

            item {
                Spacer(Modifier.height(32.dp))
            }
        }

        // Floating "Back to Top" Queue Button
        AnimatedVisibility(
            visible = showBackToTop,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 20.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        listState.animateScrollToItem(0)
                    }
                },
                modifier = Modifier
                    .testTag("back_to_top_button")
                    .size(44.dp),
                shape = CircleShape,
                containerColor = Color(0xFF6750A4),
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Back to top",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

    // Delete Confirmation Dialog for Search Results / Queue
    pendingDeleteEntry?.let { entryToDelete ->
        AlertDialog(
            onDismissRequest = { pendingDeleteEntry = null },
            title = {
                Text(
                    text = stringResource(R.string.delete_number_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                val displayNum = PhoneNumberNormalizer.getDisplayNumber(entryToDelete, numberFormatPreference)
                Text(
                    text = stringResource(R.string.delete_number_confirm, displayNum),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF49454F)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteEntry(entryToDelete.id)
                        pendingDeleteEntry = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFB3261E),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text(stringResource(R.string.delete), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingDeleteEntry = null },
                    modifier = Modifier.testTag("cancel_delete_button")
                ) {
                    Text(stringResource(R.string.cancel), color = Color(0xFF49454F))
                }
            }
        )
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
            selectedLanguage = currentLanguage,
            numberFormatPreference = numberFormatPreference,
            onCallingModeChange = { viewModel.setCallingMode(it) },
            onUpdateDelay = { viewModel.setAutoAdvanceDelay(it) },
            onLanguageSelected = { viewModel.setLanguage(it) },
            onNumberFormatPreferenceChange = { viewModel.setNumberFormatPreference(it) },
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
            label = stringResource(R.string.metric_total),
            value = summary.total.toString(),
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            label = stringResource(R.string.metric_done),
            value = summary.completed.toString(),
            color = Color(0xFF386A20),
            modifier = Modifier.weight(1f)
        )
        if (summary.skipped > 0) {
            MetricCard(
                label = stringResource(R.string.metric_skipped),
                value = summary.skipped.toString(),
                color = Color(0xFFE65100),
                modifier = Modifier.weight(1f)
            )
        }
        MetricCard(
            label = stringResource(R.string.metric_remain),
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
            label = stringResource(R.string.metric_current),
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
    numberFormatPreference: PhoneNumberFormatPreference = PhoneNumberFormatPreference.LOCAL,
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
        val effectiveEntry = currentEntry ?: if (callingMode == CallingMode.MANUAL_NEXT && queueItems.isNotEmpty()) {
            queueItems.firstOrNull()
        } else null
        val isCalledTarget = effectiveEntry?.status == CallStatus.COMPLETED

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
                        text = stringResource(R.string.current_target),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        color = Color(0xFF49454F)
                    )
                    Spacer(Modifier.height(2.dp))
                    val targetDisplay = if (effectiveEntry != null) {
                        PhoneNumberNormalizer.getDisplayNumber(effectiveEntry, numberFormatPreference)
                    } else {
                        stringResource(R.string.no_active_number)
                    }
                    Text(
                        text = targetDisplay,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (effectiveEntry != null) Color(0xFF1D1B20) else Color(0xFF49454F),
                        modifier = Modifier.testTag("current_number_display")
                    )
                    if (effectiveEntry != null && effectiveEntry.originalInput != targetDisplay) {
                        Text(
                            text = stringResource(R.string.raw_prefix, effectiveEntry.originalInput),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF49454F)
                        )
                    } else if (effectiveEntry != null) {
                        Text(
                            text = if (isCalledTarget) stringResource(R.string.ready_to_recall_sub) else stringResource(R.string.standard_cellular_sub),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF49454F)
                        )
                    }
                }

                // Call & Telephony State Badges
                Column(horizontalAlignment = Alignment.End) {
                    val isCalling = executionState == QueueExecutionState.RUNNING
                    val isReadyStatus = !isCalling && (
                        (callingMode == CallingMode.MANUAL_NEXT && effectiveEntry?.status == CallStatus.PENDING) ||
                        (effectiveEntry?.status != CallStatus.COMPLETED &&
                         effectiveEntry?.status != CallStatus.SKIPPED &&
                         effectiveEntry?.status != CallStatus.FAILED &&
                         executionState != QueueExecutionState.PAUSED &&
                         executionState != QueueExecutionState.STOPPED &&
                         executionState != QueueExecutionState.COMPLETED)
                    )
                    val statusText = when {
                        isCalling && observedCallState == ObservedCallState.ACTIVE -> stringResource(R.string.status_active)
                        isCalling && observedCallState == ObservedCallState.DIALING -> stringResource(R.string.status_dialing)
                        isCalling -> stringResource(R.string.status_calling)
                        effectiveEntry?.status == CallStatus.COMPLETED -> stringResource(R.string.status_called_caps)
                        effectiveEntry?.status == CallStatus.SKIPPED -> stringResource(R.string.status_skipped_caps)
                        effectiveEntry?.status == CallStatus.FAILED -> stringResource(R.string.status_failed_caps)
                        callingMode == CallingMode.MANUAL_NEXT && effectiveEntry?.status == CallStatus.PENDING -> stringResource(R.string.status_ready_caps)
                        executionState == QueueExecutionState.PAUSED -> stringResource(R.string.status_paused_caps)
                        executionState == QueueExecutionState.STOPPED -> stringResource(R.string.status_stopped_caps)
                        executionState == QueueExecutionState.COMPLETED -> stringResource(R.string.status_completed_caps)
                        else -> stringResource(R.string.status_ready_caps)
                    }

                    val badgeBg = when {
                        isCalling -> Color(0xFF6750A4)
                        effectiveEntry?.status == CallStatus.COMPLETED -> Color(0xFFC4EED0)
                        effectiveEntry?.status == CallStatus.SKIPPED -> Color(0xFFFFDCC2)
                        effectiveEntry?.status == CallStatus.FAILED -> Color(0xFFF9DEDC)
                        isReadyStatus -> Color(0xFFEADDFF)
                        executionState == QueueExecutionState.PAUSED -> Color(0xFFEADDFF)
                        executionState == QueueExecutionState.COMPLETED -> Color(0xFFC4EED0)
                        else -> Color(0xFFE8DEF8)
                    }

                    val badgeDot = when {
                        isCalling -> Color.White
                        effectiveEntry?.status == CallStatus.COMPLETED -> Color(0xFF386A20)
                        effectiveEntry?.status == CallStatus.SKIPPED -> Color(0xFFE65100)
                        effectiveEntry?.status == CallStatus.FAILED -> Color(0xFFB3261E)
                        isReadyStatus -> Color(0xFF6750A4)
                        executionState == QueueExecutionState.COMPLETED -> Color(0xFF386A20)
                        else -> Color(0xFF6750A4)
                    }

                    val badgeText = when {
                        isCalling -> Color.White
                        effectiveEntry?.status == CallStatus.COMPLETED -> Color(0xFF1E4620)
                        effectiveEntry?.status == CallStatus.SKIPPED -> Color(0xFF8C3A00)
                        effectiveEntry?.status == CallStatus.FAILED -> Color(0xFF410E0B)
                        isReadyStatus -> Color(0xFF21005D)
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
                        text = stringResource(R.string.telephony_prefix, observedCallState.name),
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
                        text = stringResource(R.string.progress_ratio, summary.completed, summary.total),
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
                (summary.remaining > 0 || currentEntry != null) && (isCooldownActive || executionState == QueueExecutionState.PAUSED)
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
                                        text = if (isCooldownActive) stringResource(R.string.auto_advancing_in, countdownRemaining) else if (isCalledTarget) stringResource(R.string.ready_to_recall_title) else stringResource(R.string.queue_paused_title),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCooldownActive) Color(0xFF49454F) else Color(0xFF21005D)
                                    )
                                    Text(
                                        text = if (isCooldownActive) stringResource(R.string.next_number_dials_auto) else if (isCalledTarget) stringResource(R.string.tap_recall_auto) else stringResource(R.string.n_remaining_in_queue, summary.remaining),
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
                                    text = if (isCooldownActive) stringResource(R.string.btn_call_now) else if (isCalledTarget) stringResource(R.string.btn_recall) else stringResource(R.string.btn_resume),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    // MANUAL_CALL: Wait for user to explicitly press "Call" or "Recall"
                    // Immediate availability, NO cooldown period.
                    val hasCallableTarget = currentEntry != null || summary.remaining > 0
                    val canCall = hasCallableTarget && executionState != QueueExecutionState.RUNNING
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (canCall) Color(0xFFEADDFF) else Color(0xFFF3EDF7),
                        border = BorderStroke(1.dp, if (canCall) Color(0xFFD0BCFF) else Color(0xFFCAC4D0)),
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
                                    tint = if (canCall) Color(0xFF21005D) else Color(0xFF49454F),
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = if (isCalledTarget) stringResource(R.string.ready_to_recall_title) else if (hasCallableTarget) stringResource(R.string.ready_to_call_title) else stringResource(R.string.queue_completed_title),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (canCall) Color(0xFF21005D) else Color(0xFF49454F)
                                    )
                                    Text(
                                        text = if (isCalledTarget) stringResource(R.string.tap_recall_manual) else stringResource(R.string.n_remaining_in_queue, summary.remaining),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 11.sp,
                                        color = Color(0xFF49454F)
                                    )
                                }
                            }

                            Button(
                                onClick = onCallNext,
                                enabled = canCall,
                                shape = RoundedCornerShape(100.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF6750A4),
                                    contentColor = Color.White,
                                    disabledContainerColor = Color(0xFF6750A4).copy(alpha = 0.38f),
                                    disabledContentColor = Color.White.copy(alpha = 0.38f)
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
                                    text = if (isCalledTarget) stringResource(R.string.btn_recall) else stringResource(R.string.btn_call),
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
    isHighlighted: Boolean = false,
    isRecentlyImported: Boolean = false,
    isDragging: Boolean = false,
    dragOffsetY: Float = 0f,
    numberFormatPreference: PhoneNumberFormatPreference = PhoneNumberFormatPreference.LOCAL,
    onCallNow: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = when {
            isDragging -> 1.03f
            isRecentlyImported -> 1.015f
            else -> 1f
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "drag_scale"
    )
    val animatedElevation by animateDpAsState(
        targetValue = when {
            isDragging -> 14.dp
            isHighlighted -> 4.dp
            isRecentlyImported -> 3.dp
            isCurrent -> 2.dp
            else -> 1.dp
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "drag_elevation"
    )
    val animatedBorderWidth by animateDpAsState(
        targetValue = when {
            isDragging -> 2.5.dp
            isHighlighted -> 2.5.dp
            isRecentlyImported -> 1.5.dp
            isCurrent -> 2.dp
            else -> 1.dp
        },
        label = "drag_border_width"
    )
    val containerColor by animateColorAsState(
        targetValue = when {
            isDragging -> Color(0xFFEDE7F6)
            isHighlighted -> Color(0xFFFFF8E1)
            isRecentlyImported -> Color(0xFFF1F8E9)
            isCurrent -> Color(0xFFF3EDF7)
            else -> Color.White
        },
        animationSpec = tween(durationMillis = 500),
        label = "drag_container_color"
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            isDragging -> Color(0xFF6750A4)
            isHighlighted -> Color(0xFFFF8F00)
            isRecentlyImported -> Color(0xFF4CAF50)
            isCurrent -> Color(0xFF6750A4)
            else -> Color(0xFFCAC4D0)
        },
        animationSpec = tween(durationMillis = 500),
        label = "drag_border_color"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationY = dragOffsetY
                scaleX = scale
                scaleY = scale
                shadowElevation = if (isDragging) 24f else 0f
            }
            .clickable(enabled = !isDragging) { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        border = BorderStroke(
            width = animatedBorderWidth,
            color = borderColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = animatedElevation)
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth()
        ) {
            val isWideLayout = maxWidth >= 480.dp

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Drag handle visual affordance
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = stringResource(R.string.desc_hold_and_drag),
                    tint = if (isDragging) Color(0xFF6750A4) else Color(0xFF79747E),
                    modifier = Modifier.size(20.dp)
                )

                // Status Icon symbol matching requirements:
                StatusSymbolIcon(
                    status = entry.status,
                    isCurrent = isCurrent
                )

                // Number information & badges
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 2.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    val display = PhoneNumberNormalizer.getDisplayNumber(entry, numberFormatPreference)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = display,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isCurrent || isHighlighted) FontWeight.Bold else FontWeight.Medium,
                            fontFamily = FontFamily.Monospace,
                            textDecoration = if (entry.status == CallStatus.COMPLETED) TextDecoration.None else null,
                            color = when {
                                !entry.isValid -> Color(0xFFB3261E)
                                isHighlighted -> Color(0xFFE65100)
                                entry.status == CallStatus.COMPLETED -> Color(0xFF1D1B20)
                                else -> Color(0xFF1D1B20)
                            },
                            maxLines = 2,
                            softWrap = true,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        AnimatedVisibility(
                            visible = isRecentlyImported,
                            enter = fadeIn(animationSpec = tween(300)) + expandHorizontally(),
                            exit = fadeOut(animationSpec = tween(400)) + shrinkHorizontally()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFE8F5E9),
                                border = BorderStroke(0.5.dp, Color(0xFF81C784)),
                                modifier = Modifier.testTag("new_import_badge_${entry.id}")
                            ) {
                                Text(
                                    text = stringResource(R.string.badge_new),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32),
                                    fontSize = 9.sp,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                    if (entry.originalInput != display) {
                        Text(
                            text = stringResource(R.string.raw_prefix, entry.originalInput),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF49454F),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (!entry.isValid && entry.validationMessage != null) {
                        Text(
                            text = entry.validationMessage,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFB3261E),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (isDragging) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFEADDFF),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.badge_reordering),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                color = Color(0xFF21005D),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    } else if (isHighlighted) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFFFE082),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.badge_located),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                color = Color(0xFFE65100),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                // Status and Action Controls Area
                if (isWideLayout) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QueueItemStatusChip(status = entry.status)
                        QueueItemActionButtons(
                            entry = entry,
                            onCallNow = onCallNow,
                            onDelete = onDelete
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        QueueItemStatusChip(status = entry.status)
                        QueueItemActionButtons(
                            entry = entry,
                            onCallNow = onCallNow,
                            onDelete = onDelete
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueItemStatusChip(
    status: CallStatus,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(100.dp),
        color = when (status) {
            CallStatus.COMPLETED -> Color(0xFFC4EED0)
            CallStatus.CALLING -> Color(0xFF6750A4)
            CallStatus.SKIPPED -> Color(0xFFE8DEF8)
            CallStatus.FAILED -> Color(0xFFF9DEDC)
            CallStatus.PENDING -> Color(0xFFF3EDF7)
        }
    ) {
        Text(
            text = when (status) {
                CallStatus.COMPLETED -> stringResource(R.string.status_item_called)
                CallStatus.CALLING -> stringResource(R.string.status_item_in_progress)
                CallStatus.SKIPPED -> stringResource(R.string.status_item_skipped)
                CallStatus.FAILED -> stringResource(R.string.status_item_failed)
                CallStatus.PENDING -> stringResource(R.string.status_item_pending)
            },
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = when (status) {
                CallStatus.COMPLETED -> Color(0xFF1E4620)
                CallStatus.CALLING -> Color.White
                CallStatus.SKIPPED -> Color(0xFF49454F)
                CallStatus.FAILED -> Color(0xFF410E0B)
                CallStatus.PENDING -> Color(0xFF49454F)
            },
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun QueueItemActionButtons(
    entry: PhoneNumberEntry,
    onCallNow: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onCallNow,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Phone,
                contentDescription = if (entry.status == CallStatus.COMPLETED) {
                    stringResource(R.string.desc_recall_number, entry.normalizedNumber)
                } else {
                    stringResource(R.string.desc_call_number, entry.normalizedNumber)
                },
                tint = if (entry.status == CallStatus.COMPLETED) Color(0xFF386A20) else Color(0xFF6750A4),
                modifier = Modifier.size(18.dp)
            )
        }
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = stringResource(R.string.desc_delete_entry),
                tint = Color(0xFF49454F),
                modifier = Modifier.size(16.dp)
            )
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
                    contentDescription = stringResource(R.string.desc_calling_current),
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            status == CallStatus.COMPLETED -> {
                // ✓
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(R.string.desc_status_completed),
                    tint = Color(0xFF1E4620),
                    modifier = Modifier.size(18.dp)
                )
            }
            status == CallStatus.SKIPPED -> {
                // ↷
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Redo,
                    contentDescription = stringResource(R.string.desc_status_skipped),
                    tint = Color(0xFF49454F),
                    modifier = Modifier.size(16.dp)
                )
            }
            status == CallStatus.FAILED -> {
                // ✕
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.desc_status_failed),
                    tint = Color(0xFFB3261E),
                    modifier = Modifier.size(16.dp)
                )
            }
            else -> {
                // ○
                Icon(
                    imageVector = Icons.Outlined.Circle,
                    contentDescription = stringResource(R.string.desc_status_pending),
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
            Text(stringResource(R.string.arch_dialog_title), fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.arch_dialog_intro),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(R.string.arch_dialog_standard_mode),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.arch_dialog_default_mode),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (isDefaultDialer) stringResource(R.string.arch_status_active) else stringResource(R.string.arch_status_standard),
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
                    Text(stringResource(R.string.btn_request_default_dialer))
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.btn_close))
                }
            }
        },
        dismissButton = {
            if (!isDefaultDialer) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.btn_keep_standard_mode))
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
    selectedLanguage: String = "en",
    numberFormatPreference: PhoneNumberFormatPreference = PhoneNumberFormatPreference.LOCAL,
    onCallingModeChange: (CallingMode) -> Unit,
    onUpdateDelay: (Int) -> Unit,
    onLanguageSelected: (String) -> Unit = {},
    onNumberFormatPreferenceChange: (PhoneNumberFormatPreference) -> Unit = {},
    onRequestPermissions: () -> Unit,
    onRequestDefaultDialer: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.settings_title), fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Bahasa (Language Selection)
                Text(
                    text = stringResource(R.string.settings_language),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.settings_language_desc),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                LanguageSegmentedToggle(
                    selectedLanguage = selectedLanguage,
                    onLanguageSelected = onLanguageSelected
                )

                HorizontalDivider()

                // 2. Mode Panggilan (Calling Mode Setting)
                Text(
                    text = stringResource(R.string.settings_calling_mode),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.settings_calling_mode_desc),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CallingModeOptionCard(
                        selected = callingMode == CallingMode.AUTO_CALL,
                        title = stringResource(R.string.calling_mode_auto),
                        description = stringResource(R.string.calling_mode_auto_desc),
                        onClick = { onCallingModeChange(CallingMode.AUTO_CALL) },
                        testTag = "calling_mode_auto_call"
                    )
                    CallingModeOptionCard(
                        selected = callingMode == CallingMode.MANUAL_NEXT,
                        title = stringResource(R.string.calling_mode_manual),
                        description = stringResource(R.string.calling_mode_manual_desc),
                        onClick = { onCallingModeChange(CallingMode.MANUAL_NEXT) },
                        testTag = "calling_mode_manual_next"
                    )
                }

                HorizontalDivider()

                // 3. Jeda Lanjut Otomatis (Cooldown Setting - Conditional on Calling Mode)
                if (callingMode == CallingMode.AUTO_CALL) {
                    Text(
                        text = stringResource(R.string.settings_cooldown_label, autoAdvanceDelaySeconds),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.settings_cooldown_desc),
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
                                text = stringResource(R.string.cooldown_disabled),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.cooldown_manual_note),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                HorizontalDivider()

                // Phone Number Format Setting
                Text(
                    text = stringResource(R.string.settings_phone_number_format),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.settings_phone_number_format_desc),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CallingModeOptionCard(
                        selected = numberFormatPreference == PhoneNumberFormatPreference.LOCAL,
                        title = stringResource(R.string.format_local_title),
                        description = stringResource(R.string.format_local_desc),
                        onClick = { onNumberFormatPreferenceChange(PhoneNumberFormatPreference.LOCAL) },
                        testTag = "format_option_local"
                    )
                    CallingModeOptionCard(
                        selected = numberFormatPreference == PhoneNumberFormatPreference.INTERNATIONAL,
                        title = stringResource(R.string.format_international_title),
                        description = stringResource(R.string.format_international_desc),
                        onClick = { onNumberFormatPreferenceChange(PhoneNumberFormatPreference.INTERNATIONAL) },
                        testTag = "format_option_international"
                    )
                }

                HorizontalDivider()

                // 4. Telephony Permissions
                Text(
                    text = stringResource(R.string.settings_telephony_permissions),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(stringResource(R.string.perm_call_phone), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            if (hasCallPhonePermission) stringResource(R.string.perm_granted_direct) else stringResource(R.string.perm_not_granted_fallback),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (hasCallPhonePermission) StatusCompleted else StatusFailed
                        )
                    }
                    if (!hasCallPhonePermission) {
                        OutlinedButton(onClick = onRequestPermissions) {
                            Text(stringResource(R.string.btn_grant), fontSize = 12.sp)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(stringResource(R.string.perm_read_phone_state), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            if (hasReadPhoneStatePermission) stringResource(R.string.perm_granted_monitoring) else stringResource(R.string.perm_not_granted),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (hasReadPhoneStatePermission) StatusCompleted else StatusFailed
                        )
                    }
                    if (!hasReadPhoneStatePermission) {
                        OutlinedButton(onClick = onRequestPermissions) {
                            Text(stringResource(R.string.btn_grant), fontSize = 12.sp)
                        }
                    }
                }

                HorizontalDivider()

                // 5. Default Dialer Role
                Text(
                    text = stringResource(R.string.settings_default_dialer_role),
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
                            if (isDefaultDialer) stringResource(R.string.dialer_role_active) else stringResource(R.string.dialer_role_standard),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            if (isDefaultDialer) stringResource(R.string.dialer_role_active_desc) else stringResource(R.string.dialer_role_standard_desc),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    if (!isDefaultDialer) {
                        Button(onClick = onRequestDefaultDialer) {
                            Text(stringResource(R.string.btn_set), fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_done))
            }
        }
    )
}

@Composable
private fun LanguageSegmentedToggle(
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val isEnSelected = selectedLanguage == "en"
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("language_option_en"),
                shape = RoundedCornerShape(8.dp),
                color = if (isEnSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shadowElevation = if (isEnSelected) 2.dp else 0.dp,
                onClick = { onLanguageSelected("en") }
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🇬🇧 English",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isEnSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isEnSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val isIdSelected = selectedLanguage == "id"
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("language_option_id"),
                shape = RoundedCornerShape(8.dp),
                color = if (isIdSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shadowElevation = if (isIdSelected) 2.dp else 0.dp,
                onClick = { onLanguageSelected("id") }
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🇮🇩 Indonesia",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isIdSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isIdSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
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

/**
 * Search bar positioned between Current Target card and Calling Queue.
 * Supports partial number search and ending digit queries.
 */
@Composable
fun QueueSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            Text(
                text = stringResource(R.string.search_queue_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF49454F).copy(alpha = 0.7f)
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = stringResource(R.string.desc_search_icon),
                tint = Color(0xFF49454F),
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("clear_search_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.desc_clear_search),
                        tint = Color(0xFF49454F),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Phone,
            imeAction = ImeAction.Search
        ),
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = Color(0xFF1D1B20)
        ),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color(0xFFF3EDF7).copy(alpha = 0.5f),
            unfocusedContainerColor = Color(0xFFF3EDF7).copy(alpha = 0.3f),
            focusedBorderColor = Color(0xFF6750A4),
            unfocusedBorderColor = Color(0xFFCAC4D0),
            focusedLeadingIconColor = Color(0xFF6750A4),
            unfocusedLeadingIconColor = Color(0xFF49454F)
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("queue_search_input")
    )
}

/**
 * Temporary search results interface displayed directly below the search bar
 * without altering the underlying queue order.
 */
@Composable
fun SearchResultsSection(
    query: String,
    matchingItems: List<PhoneNumberEntry>,
    allQueueItems: List<PhoneNumberEntry>,
    currentEntry: PhoneNumberEntry?,
    numberFormatPreference: PhoneNumberFormatPreference = PhoneNumberFormatPreference.LOCAL,
    onSetAsCurrent: (PhoneNumberEntry) -> Unit,
    onMoveToNumber: (PhoneNumberEntry) -> Unit,
    onDeleteRequest: (PhoneNumberEntry) -> Unit,
    onClearSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("search_results_container"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFBF8FD)
        ),
        border = BorderStroke(1.dp, Color(0xFFD0BCFF))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.search_results_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF21005D)
                    )
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = Color(0xFFEADDFF)
                    ) {
                        Text(
                            text = "${matchingItems.size}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF21005D),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                TextButton(
                    onClick = onClearSearch,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = stringResource(R.string.clear),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF6750A4)
                    )
                }
            }

            if (matchingItems.isEmpty()) {
                Text(
                    text = stringResource(R.string.search_no_results_for, query),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF49454F),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                matchingItems.forEach { entry ->
                    val queueIndex = allQueueItems.indexOfFirst { it.id == entry.id } + 1
                    val isCurrent = currentEntry?.id == entry.id
                    // Numbers with ALL existing statuses (Pending, Called, Skipped) can be Set as Current
                    val isCallable = true

                    SearchResultItemCard(
                        entry = entry,
                        queueIndex = queueIndex,
                        isCurrent = isCurrent,
                        isCallable = isCallable,
                        numberFormatPreference = numberFormatPreference,
                        onSetAsCurrent = { onSetAsCurrent(entry) },
                        onMoveToNumber = { onMoveToNumber(entry) },
                        onDeleteRequest = { onDeleteRequest(entry) }
                    )
                }
            }
        }
    }
}

/**
 * Individual search result card displaying number, raw input (if present),
 * status, queue position (#27), "Set as Current" button, and 3-dot overflow menu.
 */
@Composable
fun SearchResultItemCard(
    entry: PhoneNumberEntry,
    queueIndex: Int,
    isCurrent: Boolean,
    isCallable: Boolean,
    numberFormatPreference: PhoneNumberFormatPreference = PhoneNumberFormatPreference.LOCAL,
    onSetAsCurrent: () -> Unit,
    onMoveToNumber: () -> Unit,
    onDeleteRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("search_result_${entry.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) Color(0xFFF3EDF7) else Color.White
        ),
        border = BorderStroke(
            width = if (isCurrent) 1.5.dp else 1.dp,
            color = if (isCurrent) Color(0xFF6750A4) else Color(0xFFCAC4D0)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Queue Position Chip (e.g. #27)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isCurrent) Color(0xFF6750A4) else Color(0xFFE8DEF8)
            ) {
                Text(
                    text = if (queueIndex > 0) "#$queueIndex" else "#-",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isCurrent) Color.White else Color(0xFF1D192B),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }

            // Number Information and Status
            Column(modifier = Modifier.weight(1f)) {
                val display = PhoneNumberNormalizer.getDisplayNumber(entry, numberFormatPreference)
                Text(
                    text = display,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF1D1B20)
                )
                if (entry.originalInput != display) {
                    Text(
                        text = stringResource(R.string.raw_prefix, entry.originalInput),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF49454F)
                    )
                }
                // Status badge
                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = when (entry.status) {
                        CallStatus.COMPLETED -> Color(0xFFC4EED0)
                        CallStatus.CALLING -> Color(0xFF6750A4)
                        CallStatus.SKIPPED -> Color(0xFFE8DEF8)
                        CallStatus.FAILED -> Color(0xFFF9DEDC)
                        CallStatus.PENDING -> Color(0xFFF3EDF7)
                    },
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = when (entry.status) {
                            CallStatus.COMPLETED -> stringResource(R.string.status_item_called)
                            CallStatus.CALLING -> stringResource(R.string.status_item_in_progress)
                            CallStatus.SKIPPED -> stringResource(R.string.status_item_skipped)
                            CallStatus.FAILED -> stringResource(R.string.status_item_failed)
                            CallStatus.PENDING -> stringResource(R.string.status_item_pending)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = when (entry.status) {
                            CallStatus.COMPLETED -> Color(0xFF1E4620)
                            CallStatus.CALLING -> Color.White
                            CallStatus.SKIPPED -> Color(0xFF49454F)
                            CallStatus.FAILED -> Color(0xFF410E0B)
                            CallStatus.PENDING -> Color(0xFF49454F)
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            // "Move to number" Action Button (primary visible action)
            Button(
                onClick = onMoveToNumber,
                shape = RoundedCornerShape(100.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6750A4),
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier
                    .height(36.dp)
                    .testTag("move_to_number_${entry.id}")
            ) {
                Text(
                    text = stringResource(R.string.move_to_number),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            // Three-dot Overflow Menu (Set as Current, Delete)
            var menuExpanded by remember { mutableStateOf(false) }
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("search_item_more_${entry.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(
                            R.string.desc_more_options_for,
                            entry.formattedDisplay.ifBlank { entry.normalizedNumber }
                        ),
                        tint = Color(0xFF49454F),
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.testTag("search_item_menu_${entry.id}")
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(R.string.set_as_current),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = Color(0xFF6750A4),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onSetAsCurrent()
                        },
                        enabled = isCallable,
                        modifier = Modifier.testTag("menu_set_as_current_${entry.id}")
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(R.string.delete),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFB3261E)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = Color(0xFFB3261E),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onDeleteRequest()
                        },
                        modifier = Modifier.testTag("menu_delete_${entry.id}")
                    )
                }
            }
        }
    }
}

