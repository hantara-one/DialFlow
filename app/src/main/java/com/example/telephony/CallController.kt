package com.example.telephony

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat

sealed class CallInitiationResult {
    data class Success(val mode: String, val number: String) : CallInitiationResult()
    data class PermissionRequired(val permission: String) : CallInitiationResult()
    data class Error(val message: String) : CallInitiationResult()
}

interface CallController {
    fun initiateCall(phoneNumber: String, preferDirectCall: Boolean = true): CallInitiationResult
}

class TelecomCallController(private val context: Context) : CallController {

    override fun initiateCall(phoneNumber: String, preferDirectCall: Boolean): CallInitiationResult {
        val cleanNumber = phoneNumber.trim()
        if (cleanNumber.isEmpty()) {
            return CallInitiationResult.Error("Phone number is empty")
        }

        val encodedNumber = Uri.encode(cleanNumber)
        val uri = Uri.parse("tel:$encodedNumber")

        val hasCallPhonePermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        return try {
            if (preferDirectCall && hasCallPhonePermission) {
                // Direct cellular call initiation
                val callIntent = Intent(Intent.ACTION_CALL, uri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(callIntent)
                CallInitiationResult.Success(mode = "ACTION_CALL (Direct)", number = cleanNumber)
            } else if (preferDirectCall && !hasCallPhonePermission) {
                // Inform user CALL_PHONE permission is needed for direct calling without manual dial tap
                CallInitiationResult.PermissionRequired(Manifest.permission.CALL_PHONE)
            } else {
                // Safe fallback: Opens system dialer with number pre-filled
                val dialIntent = Intent(Intent.ACTION_DIAL, uri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(dialIntent)
                CallInitiationResult.Success(mode = "ACTION_DIAL (System Dialer)", number = cleanNumber)
            }
        } catch (e: SecurityException) {
            CallInitiationResult.Error("Security exception: ${e.localizedMessage ?: "Permission denied"}")
        } catch (e: ActivityNotFoundException) {
            CallInitiationResult.Error("No telephony application available to handle calling")
        } catch (e: Exception) {
            CallInitiationResult.Error("Failed to initiate call: ${e.localizedMessage ?: "Unknown error"}")
        }
    }
}
