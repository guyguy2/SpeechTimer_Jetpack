package com.happypuppy.toastmasterstimer.util

import android.annotation.SuppressLint
import java.util.concurrent.TimeUnit
import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build

/**
 * Utility class to get app version information without direct BuildConfig dependency
 */
object AppVersionInfo {
    /**
     * Get the version name of the app
     * @param context Application context
     * @return Version name as String
     */
    fun getVersionName(context: Context): String {
        return try {
            val packageInfo = getPackageInfo(context)
            packageInfo.versionName
        } catch (_: Exception) {
            "1.0.0" // Fallback version
        }.toString()
    }

    /**
     * Get the version code of the app
     * @param context Application context
     * @return Version code as String
     */
    fun getVersionCode(context: Context): String {
        return try {
            val packageInfo = getPackageInfo(context)
            packageInfo.longVersionCode.toString()
        } catch (_: Exception) {
            "1" // Fallback version code
        }
    }

    /**
     * Get full version information as a formatted string
     * @param context Application context
     * @return Formatted version string: "versionName (versionCode)"
     */
    fun getFullVersionInfo(context: Context): String {
        return "${getVersionName(context)} (${getVersionCode(context)})"
    }

    private fun getPackageInfo(context: Context): PackageInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                android.content.pm.PackageManager.PackageInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
    }
}

@SuppressLint("DefaultLocale")
fun formatTime(seconds: Int): String {
    val minutes = TimeUnit.SECONDS.toMinutes(seconds.toLong())
    val remainingSeconds = seconds - TimeUnit.MINUTES.toSeconds(minutes)
    return String.format("%02d:%02d", minutes, remainingSeconds)
}