package com.example.telephony

import android.telecom.Call
import android.telecom.InCallService
import com.example.models.ObservedCallState

class NuvInCallService : InCallService() {

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            super.onStateChanged(call, state)
            val mappedState = when (state) {
                Call.STATE_DIALING, Call.STATE_CONNECTING -> ObservedCallState.DIALING
                Call.STATE_RINGING -> ObservedCallState.RINGING
                Call.STATE_ACTIVE -> ObservedCallState.ACTIVE
                Call.STATE_DISCONNECTED -> ObservedCallState.DISCONNECTED
                else -> ObservedCallState.UNKNOWN
            }
            InCallServiceStateHolder.updateCallState(mappedState)
        }
    }

    override fun onCreate() {
        super.onCreate()
        InCallServiceStateHolder.onServiceConnected()
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        InCallServiceStateHolder.setActiveCall(call)
        call.registerCallback(callCallback)

        val initialState = when (call.state) {
            Call.STATE_DIALING, Call.STATE_CONNECTING -> ObservedCallState.DIALING
            Call.STATE_RINGING -> ObservedCallState.RINGING
            Call.STATE_ACTIVE -> ObservedCallState.ACTIVE
            Call.STATE_DISCONNECTED -> ObservedCallState.DISCONNECTED
            else -> ObservedCallState.UNKNOWN
        }
        InCallServiceStateHolder.updateCallState(initialState)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        call.unregisterCallback(callCallback)
        InCallServiceStateHolder.setActiveCall(null)
        InCallServiceStateHolder.updateCallState(ObservedCallState.IDLE)
    }

    override fun onDestroy() {
        super.onDestroy()
        InCallServiceStateHolder.onServiceDisconnected()
    }
}
