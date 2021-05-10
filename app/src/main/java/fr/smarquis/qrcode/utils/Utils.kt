package fr.smarquis.qrcode.utils

import android.content.ClipData.newPlainText
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.Gravity
import android.widget.Toast
import androidx.core.content.getSystemService
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import fr.smarquis.qrcode.R

const val TAG = "QrCode"

fun isGooglePlayServicesAvailable(context: Context): Boolean {
    val instance = GoogleApiAvailability.getInstance()
    return when (instance.isGooglePlayServicesAvailable(context)) {
        ConnectionResult.SUCCESS -> instance.getApkVersion(context) >= instance.getClientVersion(context)
        else -> false
    }
}

fun checkGooglePlayServices(context: Context) {
    val gms = GoogleApiAvailability.getInstance()
    val result = when (gms.isGooglePlayServicesAvailable(context)) {
        ConnectionResult.UNKNOWN -> "UNKNOWN"
        ConnectionResult.SUCCESS -> "SUCCESS"
        ConnectionResult.SERVICE_MISSING -> "SERVICE_MISSING"
        ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED -> "SERVICE_VERSION_UPDATE_REQUIRED"
        ConnectionResult.SERVICE_DISABLED -> "SERVICE_DISABLED"
        ConnectionResult.SIGN_IN_REQUIRED -> "SIGN_IN_REQUIRED"
        ConnectionResult.INVALID_ACCOUNT -> "INVALID_ACCOUNT"
        ConnectionResult.RESOLUTION_REQUIRED -> "RESOLUTION_REQUIRED"
        ConnectionResult.NETWORK_ERROR -> "NETWORK_ERROR"
        ConnectionResult.INTERNAL_ERROR -> "INTERNAL_ERROR"
        ConnectionResult.SERVICE_INVALID -> "SERVICE_INVALID"
        ConnectionResult.DEVELOPER_ERROR -> "DEVELOPER_ERROR"
        ConnectionResult.LICENSE_CHECK_FAILED -> "LICENSE_CHECK_FAILED"
        ConnectionResult.CANCELED -> "CANCELED"
        ConnectionResult.TIMEOUT -> "TIMEOUT"
        ConnectionResult.INTERRUPTED -> "INTERRUPTED"
        ConnectionResult.API_UNAVAILABLE -> "API_UNAVAILABLE"
        ConnectionResult.SIGN_IN_FAILED -> "SIGN_IN_FAILED"
        ConnectionResult.SERVICE_UPDATING -> "SERVICE_UPDATING"
        ConnectionResult.SERVICE_MISSING_PERMISSION -> "SERVICE_MISSING_PERMISSION"
        ConnectionResult.RESTRICTED_PROFILE -> "RESTRICTED_PROFILE"
        else -> "?"
    }
    Log.d(TAG, "GooglePlayServices isGooglePlayServicesAvailable: $result")
    Log.d(TAG, "GooglePlayServices apkVersion: ${gms.getApkVersion(context)}")
    Log.d(TAG, "GooglePlayServices clientVersion: ${gms.getClientVersion(context)}")
    Log.d(TAG, "GooglePlayServices up-to-date: ${gms.getApkVersion(context) >= gms.getClientVersion(context)}")

}

fun SpannableStringBuilder.appendKeyValue(key: String, value: String?) {
    if (value.isNullOrBlank()) {
        return
    }
    if (!isBlank()) {
        append("\n")
    }
    bold {
        append("$key: ")
    }
    append(value)
}

fun isSafeIntent(context: Context, intent: Intent?): Boolean {
    return intent?.let {
        it.resolveActivity(context.packageManager) != null
    } ?: false
}

fun safeStartIntent(context: Context, intent: Intent?): Boolean {
    intent ?: return false
    intent.resolveActivity(context.packageManager) ?: return false
    return runCatching {
        context.startActivity(intent)
    }.isSuccess
}

fun copyToClipboard(context: Context, string: String?, toast: Toast? = null): Toast? {
    val data = newPlainText(context.packageName, string ?: return null)
    toast?.cancel()
    context.getSystemService<ClipboardManager>()?.setPrimaryClip(data)
    val text = buildSpannedString {
        append(context.getString(R.string.copied_to_clipboard))
        append("\n\n")
        bold { append(string) }
    }
    return Toast.makeText(context, text, Toast.LENGTH_LONG).apply { setGravity(Gravity.CENTER, 0, 0) }.also { it.show() }
}