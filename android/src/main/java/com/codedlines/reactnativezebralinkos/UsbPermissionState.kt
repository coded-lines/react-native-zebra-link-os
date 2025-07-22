package com.codedlines.reactnativezebralinkos

import android.app.PendingIntent

object UsbPermissionState {
    var hasPermissionToCommunicate: Boolean = false
    var permissionIntent: PendingIntent? = null
    var onPermissionGranted: (() -> Unit)? = null
}