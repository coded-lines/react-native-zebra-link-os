package com.codedlines.reactnativezebralinkos

import android.app.PendingIntent

object UsbPermissionState {
    var hasPermissionToCommunicate: Boolean = false
    var permissionIntent: PendingIntent? = null
    // single callback that always fires with the result
    var onPermissionResult: ((granted: Boolean) -> Unit)? = null
}