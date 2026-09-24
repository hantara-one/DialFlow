package com.example.telephony

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telecom.Call
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.example.models.ObservedCallState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class CallObserverSource {
    NONE,
    TELEPHONY_CALLBACK,
    IN_CALL_SERVICE
}

/**
 * Singleton holder for InCallService events when Android Telecom binds the app as default dialer.
 */
object InCallServiceStateHolder {
    private val _inCallState = MutableStateFlow(ObservedCallState.IDLE)
    val inCallState: StateFlow<ObservedCallState> = _inCallState.asStateFlow()

    private val _isServiceBound = MutableStateFlow(false)
    val isServiceBound: StateFlow<Boolean> = _isServiceBound.asStateFlow()

    private var activeCall: Call? = null

    fun onServiceConnected() {
        _isServiceBound.value = true
    }

    fun onServiceDisconnected() {
        _isServiceBound.value = false
        _inCallState.value = ObservedCallState.IDLE
        activeCall = null
    }

    fun updateCallState(state: ObservedCallState) {
        _inCallState.value = state
    }

    fun setActiveCall(call: Call?) {
        activeCall = call
    }

    fun getActiveCall(): Call? = activeCall
}

interface CallStateObserver {
    val callState: StateFlow<ObservedCallState>
    val observerSource: StateFlow<CallObserverSource>
    fun startObserving()
    fun stopObserving()
}

class TelephonyCallStateObserver(
    private val context: Context,
    private val scope: CoroutineScope
) : CallStateObserver {

    private val telephonyManager =
        context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

    private val _callState = MutableStateFlow(ObservedCallState.IDLE)
    override val callState: StateFlow<ObservedCallState> = _callState.asStateFlow()

    private val _observerSource = MutableStateFlow(CallObserverSource.NONE)
    override val observerSource: StateFlow<CallObserverSource> = _observerSource.asStateFlow()

    private var previousTelephonyState: Int = TelephonyManager.CALL_STATE_IDLE
    private var isObserving = false

    // Android 12+ (API 31+) TelephonyCallback
    private var telephonyCallback: Any? = null

    // Legacy PhoneStateListener (API < 31)
    @Suppress("DEPRECATION")
    private var legacyListener: PhoneStateListener? = null

    init {
        // Observe InCallService state if bound
        scope.launch {
            InCallServiceStateHolder.inCallState.collect { inCallState ->
                if (InCallServiceStateHolder.isServiceBound.value) {
                    _observerSource.value = CallObserverSource.IN_CALL_SERVICE
                    _callState.value = inCallState
                }
            }
        }
    }

    override fun startObserving() {
        if (isObserving) return
        isObserving = true

        // If InCallService is bound, it already takes priority
        if (InCallServiceStateHolder.isServiceBound.value) {
            _observerSource.value = CallObserverSource.IN_CALL_SERVICE
            _callState.value = InCallServiceStateHolder.inCallState.value
            return
        }

        val hasReadPhoneState = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        if (telephonyManager == null) {
            _observerSource.value = CallObserverSource.NONE
            return
        }

        _observerSource.value = CallObserverSource.TELEPHONY_CALLBACK

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            registerTelephonyCallbackApi31(telephonyManager)
        } else {
            registerLegacyListener(telephonyManager)
        }
    }

    private fun registerTelephonyCallbackApi31(tm: TelephonyManager) {
        try {
            val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) {
                    handleTelephonyStateChanged(state)
                }
            }
            telephonyCallback = callback
            context.mainExecutor.let { executor ->
                tm.registerTelephonyCallback(executor, callback)
            }
        } catch (_: SecurityException) {
            // Permission not granted yet
        } catch (_: Exception) {
            // Fallback gracefully
        }
    }

    @Suppress("DEPRECATION")
    private fun registerLegacyListener(tm: TelephonyManager) {
        try {
            val listener = object : PhoneStateListener() {
                @Deprecated("Deprecated in Java")
                override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                    handleTelephonyStateChanged(state)
                }
            }
            legacyListener = listener
            tm.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
        } catch (_: SecurityException) {
            // Permission not granted
        } catch (_: Exception) {
            // Fallback gracefully
        }
    }

    private fun handleTelephonyStateChanged(state: Int) {
        // If InCallService is active, do not override its granular state
        if (InCallServiceStateHolder.isServiceBound.value) {
            return
        }

        val mappedState = when (state) {
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                ObservedCallState.ACTIVE
            }
            TelephonyManager.CALL_STATE_RINGING -> {
                ObservedCallState.RINGING
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                // If previous state was OFFHOOK, it transitioned to IDLE (Call Ended / Disconnected)
                if (previousTelephonyState == TelephonyManager.CALL_STATE_OFFHOOK) {
                    ObservedCallState.DISCONNECTED
                } else {
                    ObservedCallState.IDLE
                }
            }
            else -> ObservedCallState.UNKNOWN
        }

        previousTelephonyState = state
        _callState.value = mappedState

        // If it transitioned to DISCONNECTED, follow up with IDLE after brief moment
        if (mappedState == ObservedCallState.DISCONNECTED) {
            scope.launch {
                kotlinx.coroutines.delay(1000)
                if (_callState.value == ObservedCallState.DISCONNECTED) {
                    _callState.value = ObservedCallState.IDLE
                }
            }
        }
    }

    override fun stopObserving() {
        if (!isObserving) return
        isObserving = false

        telephonyManager?.let { tm ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (telephonyCallback as? TelephonyCallback)?.let { cb ->
                    try {
                        tm.unregisterTelephonyCallback(cb)
                    } catch (_: Exception) {}
                }
                telephonyCallback = null
            } else {
                @Suppress("DEPRECATION")
                legacyListener?.let { listener ->
                    try {
                        tm.listen(listener, PhoneStateListener.LISTEN_NONE)
                    } catch (_: Exception) {}
                }
                legacyListener = null
            }
        }
    }
}
