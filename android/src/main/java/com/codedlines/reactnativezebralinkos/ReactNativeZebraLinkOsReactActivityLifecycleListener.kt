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
            if (intent.action != actionUsbPermission) return

            val usbManager = ctx.getSystemService(Context.USB_SERVICE) as UsbManager

            val device: UsbDevice? =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                }

            val grantedExtra = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
            val granted = grantedExtra || (device != null && usbManager.hasPermission(device))

            Log.d(TAG, "USB permission result: granted=$granted, device=$device")

            UsbPermissionState.hasPermissionToCommunicate = granted
            UsbPermissionState.onPermissionResult?.invoke(granted)
            UsbPermissionState.onPermissionResult = null
        }
    }

    override fun onCreate(activity: Activity, savedInstanceState: Bundle?) {
        appCtx = activity.applicationContext

        actionUsbPermission = "${appCtx.packageName}.USB_PERMISSION"
        filter = IntentFilter(actionUsbPermission)

        val flags = when {
            android.os.Build.VERSION.SDK_INT >= 34 ->  // U+
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            android.os.Build.VERSION.SDK_INT >= 31 ->  // S–T
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            else ->
                PendingIntent.FLAG_UPDATE_CURRENT
        }

        UsbPermissionState.permissionIntent = PendingIntent.getBroadcast(
            appCtx,
            0,
            Intent(actionUsbPermission),
            flags
        )

        registerUsbReceiverIfNeeded()
    }

    override fun onResume(activity: Activity) {
        registerUsbReceiverIfNeeded()
    }

    override fun onPause(activity: Activity) {
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