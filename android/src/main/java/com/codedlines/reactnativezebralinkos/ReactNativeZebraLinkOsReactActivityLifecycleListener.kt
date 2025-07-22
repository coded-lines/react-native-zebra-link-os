package com.codedlines.reactnativezebralinkos

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import expo.modules.core.interfaces.ReactActivityLifecycleListener

private const val TAG = "ReactNativeZebraLinkOsModule"

class ReactNativeZebraLinkOsReactActivityLifecycleListener : ReactActivityLifecycleListener {

    private lateinit var appCtx: Context
    private lateinit var actionUsbPermission: String
    private lateinit var filter: IntentFilter
    private var isReceiverRegistered = false

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            if (intent.action == actionUsbPermission) {
                val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                val device: UsbDevice? =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    }

                Log.d(TAG, "USB permission result: granted=$granted, device=$device")
                if (granted && device != null) {
                    UsbPermissionState.hasPermissionToCommunicate = true
                    UsbPermissionState.onPermissionGranted?.invoke()
                }

                UsbPermissionState.onPermissionGranted = null
            }
        }
    }

    override fun onCreate(activity: Activity, savedInstanceState: Bundle?) {
        appCtx = activity.applicationContext

        actionUsbPermission = "${appCtx.packageName}.USB_PERMISSION"
        filter = IntentFilter(actionUsbPermission)

        UsbPermissionState.permissionIntent = PendingIntent.getBroadcast(
            appCtx,
            0,
            Intent(actionUsbPermission),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        registerUsbReceiverIfNeeded()
    }

    override fun onResume(activity: Activity) {
        registerUsbReceiverIfNeeded()
    }

    override fun onPause(activity: Activity) {
        unregisterUsbReceiverIfNeeded()
    }

    override fun onDestroy(activity: Activity) {
        unregisterUsbReceiverIfNeeded()
    }

    private fun registerUsbReceiverIfNeeded() {
        if (!isReceiverRegistered) {
            if (Build.VERSION.SDK_INT >= 33) {
                appCtx.registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                appCtx.registerReceiver(usbReceiver, filter)
            }
            isReceiverRegistered = true
            Log.d(TAG, "USB receiver registered")
        }
    }

    private fun unregisterUsbReceiverIfNeeded() {
        if (isReceiverRegistered) {
            try {
                appCtx.unregisterReceiver(usbReceiver)
                Log.d(TAG, "USB receiver unregistered")
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Receiver not registered: ${e.message}")
            }
            isReceiverRegistered = false
        }
    }
}