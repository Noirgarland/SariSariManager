package com.example.sarisari.manager.core.util

import android.content.Context
import android.content.Intent

/** Restarts the app so a staged restore is applied before the database opens. */
fun Context.restartApp() {
    val launch = packageManager.getLaunchIntentForPackage(packageName) ?: return
    startActivity(Intent.makeRestartActivityTask(launch.component))
    Runtime.getRuntime().exit(0)
}
