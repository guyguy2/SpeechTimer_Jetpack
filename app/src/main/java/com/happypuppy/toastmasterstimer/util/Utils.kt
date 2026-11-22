package com.happypuppy.toastmasterstimer.util

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Utility class to get app version information without direct BuildConfig dependency.
 */
object AppVersionInfo {
    fun getVersionName(context: Context): String {
        return try {
            getPackageInfo(context).versionName?.takeIf { it.isNotBlank() } ?: "1.0.0"
        } catch (_: Exception) {
            "1.0.0"
        }
    }

    fun getVersionCode(context: Context): String {
        return try {
            getPackageInfo(context).longVersionCode.toString()
        } catch (_: Exception) {
            "1"
        }
    }

    fun getFullVersionInfo(context: Context): String {
        return "${getVersionName(context)} (${getVersionCode(context)})"
    }

    private fun getPackageInfo(context: Context): PackageInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
    }
}

fun formatTime(seconds: Int): String {
    val safeSeconds = seconds.coerceAtLeast(0)
    val minutes = TimeUnit.SECONDS.toMinutes(safeSeconds.toLong())
    val remainingSeconds = safeSeconds - TimeUnit.MINUTES.toSeconds(minutes)
    return String.format(Locale.US, "%02d:%02d", minutes, remainingSeconds)
}
