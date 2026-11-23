package com.codedlines.reactnativezebralinkos

import android.content.Context
import android.hardware.usb.UsbManager
import android.util.Base64
import android.util.Log
import com.zebra.sdk.printer.SGD
import com.zebra.sdk.printer.ZebraPrinterFactory
import com.zebra.sdk.printer.discovery.DiscoveredPrinter
import com.zebra.sdk.printer.discovery.DiscoveredPrinterUsb
import com.zebra.sdk.printer.discovery.DiscoveryHandler
import com.zebra.sdk.printer.discovery.UsbDiscoverer
import expo.modules.kotlin.Promise
import expo.modules.kotlin.exception.CodedException
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.util.LinkedList

class ReactNativeZebraLinkOsModule : Module() {
    companion object {
        private const val TAG = "ReactNativeZebraLinkOsModule"
    }

    private var pendingUsbPromise: Promise? = null

    private var discoveredPrinterUsb: DiscoveredPrinterUsb? = null
    private lateinit var usbManager: UsbManager

    // Each module class must implement the definition function. The definition consists of components
    // that describes the module's functionality and behavior.
    // See https://docs.expo.dev/modules/module-api for more details about available components.
    override fun definition() = ModuleDefinition {
        // Sets the name of the module that JavaScript code will use to refer to the module. Takes a string as an argument.
        // Can be inferred from module's class name, but it's recommended to set it explicitly for clarity.
        // The module will be accessible from `requireNativeModule('ReactNativeZebraLinkOs')` in JavaScript.
        Name("ReactNativeZebraLinkOs")


        AsyncFunction("usbFindAndConnect") { promise: Promise ->
            Log.d(TAG, "usbFindAndConnect called")

            val reactCtx = appContext.reactContext
            if (reactCtx == null) {
                Log.e(TAG, "usbFindAndConnect: reactContext is null")
                promise.reject(CodedException("NO_CONTEXT"))
                return@AsyncFunction
            }

            val usbManager =
                (reactCtx.getSystemService(Context.USB_SERVICE) as? UsbManager) ?: run {
                    Log.e(TAG, "usbFindAndConnect: UsbManager unavailable")
                    promise.reject(CodedException("NO_USB_MANAGER"))
                    return@AsyncFunction
                }


            val handler = object : DiscoveryHandler {
                val printers = LinkedList<DiscoveredPrinterUsb>()

                @Volatile
                var done = false

                override fun foundPrinter(printer: DiscoveredPrinter) {
                    (printer as? DiscoveredPrinterUsb)?.let {
                        printers.add(it)
                        Log.d(TAG, "Found USB printer: ${it.device.deviceName}")
                    }
                }

                override fun discoveryFinished() {
                    done = true
                    Log.d(TAG, "USB discovery finished")
                }

                override fun discoveryError(message: String) {
                    done = true
                    Log.e(TAG, "Discovery error: $message")
                }
            }

            UsbDiscoverer.findPrinters(reactCtx, handler)
            val start = System.currentTimeMillis()
            while (!handler.done && System.currentTimeMillis() - start < 10_000) {
                Thread.sleep(100)
            }

            if (handler.printers.isEmpty()) {
                Log.e(TAG, "usbFindAndConnect: no printers found")
                promise.reject(CodedException("NO_PRINTER"))
                return@AsyncFunction
            }

            discoveredPrinterUsb = handler.printers.first()
            val device = discoveredPrinterUsb!!.device

            if (!usbManager.hasPermission(device)) {
                val intent = UsbPermissionState.permissionIntent
                if (intent == null) {
                    promise.reject(CodedException("NO_PERMISSION_INTENT"))
                    return@AsyncFunction
                }

                if (pendingUsbPromise != null) {
                    promise.reject(CodedException("PERMISSION_IN_PROGRESS"))
                    return@AsyncFunction
                }

                pendingUsbPromise = promise
                Log.d(TAG, "usbFindAndConnect: requesting USB permission")

                // Listen for the result just for this request
                UsbPermissionState.onPermissionResult = { granted ->
                    val p = pendingUsbPromise
                    pendingUsbPromise = null  // <-- CRUCIAL: always clear!

                    if (granted) {
                        val d = discoveredPrinterUsb!!.device
                        Log.d(TAG, "Permission granted for ${d.deviceName}")
                        p?.resolve(
                            mapOf(
                                "success" to true,
                                "deviceName" to d.deviceName,
                                "vendorId" to d.vendorId,
                                "productId" to d.productId
                            )
                        )
                    } else {
                        Log.d(TAG, "Permission denied/cancelled")
                        p?.reject(CodedException("USB_PERMISSION_DENIED"))
                    }
                }

                usbManager.requestPermission(device, intent)
                return@AsyncFunction
            }


            UsbPermissionState.hasPermissionToCommunicate = true
            Log.d(
                TAG,
                "usbFindAndConnect success: name=${device.deviceName}, vendor=${device.vendorId}, product=${device.productId}"
            )
            promise.resolve(
                mapOf(
                    "success" to true,
                    "deviceName" to device.deviceName,
                    "vendorId" to device.vendorId,
                    "productId" to device.productId
                )
            )
        }

        AsyncFunction("downloadTemplate") { data: String, promise: Promise ->
            Log.d(TAG, "downloadTemplate called (data length=${data.length})")

            if (data.isBlank()) {
                Log.e(TAG, "downloadTemplate: empty payload")
                promise.reject(CodedException("INVALID_DATA"))
                return@AsyncFunction
            }

            Log.d(TAG, "Starting Base64 decode")
            val decodedBytes: ByteArray = try {
                Base64.decode(data, Base64.DEFAULT)
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "downloadTemplate: Base64 decode failed", e)
                promise.reject(CodedException("DECODE_FAILED"))
                return@AsyncFunction
            }
            Log.d(TAG, "Base64 decode finished")

            val conn = discoveredPrinterUsb?.connection
            if (conn == null) {
                Log.e(TAG, "downloadTemplate: no printer connection")
                promise.reject(CodedException("NO_PRINTER"))
                return@AsyncFunction
            }

            try {
                try {
                    conn.open()
                    Log.d(TAG, "Printer connection opened")
                } catch (e: Exception) {
                    Log.e(TAG, "downloadTemplate: open failed", e)
                    promise.reject(CodedException("CONNECTION_OPEN_FAILED"))
                    return@AsyncFunction
                }

                try {
                    Log.d(TAG, "Sending template (${decodedBytes.size} bytes)")
                    conn.write(decodedBytes)
                    Log.d(TAG, "Template sent to printer (${decodedBytes.size} bytes)")
                    promise.resolve("OK")
                } catch (e: IOException) {
                    Log.e(TAG, "downloadTemplate: write failed", e)
                    promise.reject(CodedException("WRITE_FAILED"))
                    return@AsyncFunction
                }
            } finally {
                try {
                    Log.d(TAG, "Closing printer connection")
                    conn.close()
                    Log.d(TAG, "Printer connection closed")
                } catch (e: Exception) {
                    Log.e(TAG, "downloadTemplate: error closing connection", e)
                }
            }
        }


        AsyncFunction("printStoredFormat") { path: String, data: Map<String, String?>, promise: Promise ->
            Log.d(TAG, "printStoredFormat called (path='$path', data entries=${data.size})")

            if (path.isBlank()) {
                Log.e(TAG, "printStoredFormat: empty template path")
                promise.reject(CodedException("INVALID_PATH"))

                return@AsyncFunction
            }

            val conn = discoveredPrinterUsb?.connection
            if (conn == null) {
                Log.e(TAG, "printStoredFormat: no USB printer discovered")
                promise.reject(CodedException("NO_PRINTER"))
                return@AsyncFunction
            }

            val vars = mutableMapOf<Int, String>()
            for ((key, value) in data) {
                val idx = key.toIntOrNull()
                if (idx != null && !value.isNullOrBlank()) {
                    vars[idx] = value
                } else {
                    Log.w(TAG, "Skipping invalid var – key='$key', value='$value'")
                }
            }
            Log.d(TAG, "printStoredFormat vars: $vars")

            try {
                try {
                    conn.open()
                    Log.d(TAG, "Printer connection opened")
                } catch (e: Exception) {
                    Log.e(TAG, "printStoredFormat: open failed", e)
                    promise.reject(CodedException("CONNECTION_OPEN_FAILED"))
                    return@AsyncFunction
                }

                val printer = try {
                    ZebraPrinterFactory.getInstance(conn)
                } catch (e: Exception) {
                    Log.e(TAG, "printStoredFormat: printer init failed", e)
                    promise.reject(CodedException("PRINTER_INIT_FAILED"))
                    return@AsyncFunction
                }

                try {
                    printer.printStoredFormat(path, vars)
                    Log.d(TAG, "printStoredFormat executed successfully")
                    promise.resolve("OK")
                } catch (e: Exception) {
                    Log.e(TAG, "printStoredFormat: print failed", e)
                    promise.reject(CodedException("PRINT_FAILED"))
                }
            } finally {
                try {
                    conn.close()
                    Log.d(TAG, "Printer connection closed")
                } catch (e: Exception) {
                    Log.e(TAG, "printStoredFormat: error closing connection", e)
                }
            }
        }


        AsyncFunction("downloadTtfFont") { path: String, data: String, promise: Promise ->
            Log.d(TAG, "downloadTtfFont called (path='$path', data length=${data.length})")

            if (path.isBlank()) {
                Log.e(TAG, "downloadTtfFont: empty font path")
                promise.reject(CodedException("INVALID_PATH"))
                return@AsyncFunction
            }

            if (data.isBlank()) {
                Log.e(TAG, "downloadTtfFont: empty font data")
                promise.reject(CodedException("INVALID_DATA"))
                return@AsyncFunction
            }

            val fontBytes: ByteArray = try {
                Base64.decode(data, Base64.DEFAULT)
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "downloadTtfFont: Base64 decode failed", e)
                promise.reject(CodedException("DECODE_FAILED"))
                return@AsyncFunction
            }
            Log.d(TAG, "Decoded font size=${fontBytes.size} bytes")

            val conn = discoveredPrinterUsb?.connection
            if (conn == null) {
                Log.e(TAG, "downloadTtfFont: no USB printer discovered")
                promise.reject(CodedException("NO_PRINTER"))
                return@AsyncFunction
            }

            try {
                try {
                    conn.open()
                    Log.d(TAG, "Printer connection opened")
                } catch (e: Exception) {
                    Log.e(TAG, "downloadTtfFont: connection open failed", e)
                    promise.reject(CodedException("CONNECTION_OPEN_FAILED"))
                    return@AsyncFunction
                }

                val printer = try {
                    ZebraPrinterFactory.getLinkOsPrinter(conn)
                } catch (e: Exception) {
                    Log.e(TAG, "downloadTtfFont: printer init failed", e)
                    promise.reject(CodedException("PRINTER_INIT_FAILED"))
                    return@AsyncFunction
                }
                if (printer == null) {
                    Log.e(TAG, "downloadTtfFont: printer is not a Link‑OS device")
                    promise.reject(CodedException("INVALID_PRINTER"))
                    return@AsyncFunction
                }

                try {
                    val inputStream: InputStream = ByteArrayInputStream(fontBytes)
                    printer.downloadTtfFont(inputStream, path)
                    Log.d(TAG, "Font downloaded successfully to '$path'")
                    promise.resolve("OK")
                } catch (e: Exception) {
                    Log.e(TAG, "downloadTtfFont: download failed", e)
                    promise.reject(CodedException("DOWNLOAD_FAILED"))
                    return@AsyncFunction
                }
            } finally {
                try {
                    conn.close()
                    Log.d(TAG, "Printer connection closed")
                } catch (e: Exception) {
                    Log.e(TAG, "downloadTtfFont: error closing connection", e)
                }
            }
        }

        AsyncFunction("printAndConfirm") { base64Data: String, promise: Promise ->
            Log.d(TAG, "printAndConfirm called (data length=${base64Data.length})")

            if (base64Data.isBlank()) {
                promise.reject(CodedException("INVALID_DATA"))
                return@AsyncFunction
            }

            val decodedBytes: ByteArray = try {
                Base64.decode(base64Data, Base64.DEFAULT)
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "printAndConfirm: Base64 decode failed", e)
                promise.reject(CodedException("DECODE_FAILED"))
                return@AsyncFunction
            }

            val conn = discoveredPrinterUsb?.connection
            if (conn == null) {
                promise.reject(CodedException("NO_PRINTER"))
                return@AsyncFunction
            }

            try {
                try {
                    conn.open()
                    Log.d(TAG, "printAndConfirm: connection opened")
                } catch (e: Exception) {
                    Log.e(TAG, "printAndConfirm: open failed", e)
                    promise.reject(CodedException("CONNECTION_OPEN_FAILED"))
                    return@AsyncFunction
                }

                val printer = try {
                    ZebraPrinterFactory.getInstance(conn)
                } catch (e: Exception) {
                    Log.e(TAG, "printAndConfirm: printer init failed", e)
                    promise.reject(CodedException("PRINTER_INIT_FAILED"))
                    return@AsyncFunction
                }

                // Pre-flight: check printer is ready before sending anything
                val preStatus = printer.currentStatus
                Log.d(TAG, "printAndConfirm: pre-flight status: isReadyToPrint=${preStatus.isReadyToPrint}, isPaperOut=${preStatus.isPaperOut}, isHeadOpen=${preStatus.isHeadOpen}, isPaused=${preStatus.isPaused}")
                if (!preStatus.isReadyToPrint) {
                    val reason = when {
                        preStatus.isPaperOut -> "PAPER_OUT"
                        preStatus.isHeadOpen -> "HEAD_OPEN"
                        preStatus.isPaused -> "PAUSED"
                        preStatus.isReceiveBufferFull -> "BUFFER_FULL"
                        else -> "NOT_READY"
                    }
                    Log.e(TAG, "printAndConfirm: printer not ready: $reason")
                    promise.reject(CodedException(reason))
                    return@AsyncFunction
                }

                // Read label counter before printing
                val counterBefore = try {
                    SGD.GET("odometer.total_label_count", conn)?.trim()?.toIntOrNull() ?: -1
                } catch (e: Exception) {
                    Log.w(TAG, "printAndConfirm: could not read counter before: ${e.message}")
                    -1
                }
                Log.d(TAG, "printAndConfirm: counterBefore=$counterBefore")

                // Send the ZPL
                try {
                    conn.write(decodedBytes)
                    Log.d(TAG, "printAndConfirm: ZPL sent (${decodedBytes.size} bytes)")
                } catch (e: IOException) {
                    Log.e(TAG, "printAndConfirm: write failed", e)
                    promise.reject(CodedException("WRITE_FAILED"))
                    return@AsyncFunction
                }

                // Poll status every 300ms until job is out of the buffer or timeout/error
                val timeoutMs = 15_000L
                val pollIntervalMs = 300L
                val start = System.currentTimeMillis()
                var result = "unknown"
                var failReason: String? = null

                while (System.currentTimeMillis() - start < timeoutMs) {
                    Thread.sleep(pollIntervalMs)
                    val s = try {
                        printer.currentStatus
                    } catch (e: Exception) {
                        Log.w(TAG, "printAndConfirm: status poll failed: ${e.message}")
                        continue
                    }

                    Log.d(TAG, "printAndConfirm: poll: isReadyToPrint=${s.isReadyToPrint}, isPaperOut=${s.isPaperOut}, isHeadOpen=${s.isHeadOpen}, isPartialFormatInProgress=${s.isPartialFormatInProgress}")

                    // Detect failure
                    if (s.isPaperOut || s.isHeadOpen || s.isReceiveBufferFull) {
                        result = "failed"
                        failReason = when {
                            s.isPaperOut -> "PAPER_OUT"
                            s.isHeadOpen -> "HEAD_OPEN"
                            s.isReceiveBufferFull -> "BUFFER_FULL"
                            else -> "ERROR"
                        }
                        Log.e(TAG, "printAndConfirm: failure detected: $failReason")
                        // Cancel any remaining jobs in the queue
                        try { conn.write("~JA".toByteArray()) } catch (_: Exception) {}
                        break
                    }

                    // Job done: printer is ready and not mid-format
                    if (s.isReadyToPrint && !s.isPartialFormatInProgress) {
                        result = "done_polling"
                        break
                    }
                }

                // Read label counter after printing
                val counterAfter = try {
                    SGD.GET("odometer.total_label_count", conn)?.trim()?.toIntOrNull() ?: -1
                } catch (e: Exception) {
                    Log.w(TAG, "printAndConfirm: could not read counter after: ${e.message}")
                    -1
                }
                Log.d(TAG, "printAndConfirm: counterAfter=$counterAfter")

                // Determine final result
                val finalResult = when {
                    result == "failed" -> "failed"
                    counterBefore >= 0 && counterAfter > counterBefore -> "confirmed"
                    result == "done_polling" -> "confirmed"
                    else -> "unknown"
                }

                Log.d(TAG, "printAndConfirm: finalResult=$finalResult, counterBefore=$counterBefore, counterAfter=$counterAfter")

                promise.resolve(
                    mapOf(
                        "result" to finalResult,
                        "counterBefore" to counterBefore,
                        "counterAfter" to counterAfter,
                        "error" to failReason,
                    )
                )
            } finally {
                try {
                    conn.close()
                    Log.d(TAG, "printAndConfirm: connection closed")
                } catch (e: Exception) {
                    Log.e(TAG, "printAndConfirm: error closing connection", e)
                }
            }
        }

        AsyncFunction("printBatch") { base64TicketsJson: String, promise: Promise ->
            // Parameter is a JSON-encoded string array — ArrayList<String> is not supported
            // by expo-modules-core's AsyncFunction parameter reflection in this SDK version
            val base64Tickets: ArrayList<String> = try {
                val arr = org.json.JSONArray(base64TicketsJson)
                ArrayList<String>(arr.length()).also { list ->
                    for (i in 0 until arr.length()) list.add(arr.getString(i))
                }
            } catch (e: Exception) {
                Log.e(TAG, "printBatch: JSON parse failed — ${e.message}")
                promise.reject(CodedException("INVALID_DATA"))
                return@AsyncFunction
            }

            Log.d(TAG, "printBatch: START — ${base64Tickets.size} tickets")

            if (base64Tickets.isEmpty()) {
                Log.e(TAG, "printBatch: rejected — empty ticket list")
                promise.reject(CodedException("INVALID_DATA"))
                return@AsyncFunction
            }

            val decodedList: ArrayList<ByteArray> = try {
                base64Tickets.mapTo(ArrayList()) { Base64.decode(it, Base64.DEFAULT) }
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "printBatch: Base64 decode failed", e)
                promise.reject(CodedException("DECODE_FAILED"))
                return@AsyncFunction
            }
            val totalBytes = decodedList.sumOf { it.size }
            Log.d(TAG, "printBatch: decoded ${decodedList.size} tickets, total bytes=$totalBytes")

            val conn = discoveredPrinterUsb?.connection
            if (conn == null) {
                Log.e(TAG, "printBatch: no printer discovered")
                promise.reject(CodedException("NO_PRINTER"))
                return@AsyncFunction
            }

            try {
                try {
                    conn.open()
                    Log.d(TAG, "printBatch: connection opened")
                } catch (e: Exception) {
                    Log.e(TAG, "printBatch: connection open failed", e)
                    promise.reject(CodedException("CONNECTION_OPEN_FAILED"))
                    return@AsyncFunction
                }

                val printer = try {
                    ZebraPrinterFactory.getInstance(conn)
                } catch (e: Exception) {
                    Log.e(TAG, "printBatch: printer init failed", e)
                    promise.reject(CodedException("PRINTER_INIT_FAILED"))
                    return@AsyncFunction
                }

                val counterBefore = try {
                    SGD.GET("odometer.total_label_count", conn)?.trim()?.toIntOrNull() ?: -1
                } catch (e: Exception) {
                    Log.w(TAG, "printBatch: counter read before failed — ${e.message}")
                    -1
                }
                Log.d(TAG, "printBatch: counterBefore=$counterBefore")

                // Burst write — all tickets queued in printer buffer at once
                val writeStart = System.currentTimeMillis()
                Log.d(TAG, "printBatch: burst write starting")
                try {
                    for ((idx, bytes) in decodedList.withIndex()) {
                        conn.write(bytes)
                        Log.d(TAG, "printBatch: wrote ticket ${idx + 1}/${decodedList.size} (${bytes.size} bytes)")
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "printBatch: write failed — ${e.message}")
                    promise.reject(CodedException("WRITE_FAILED"))
                    return@AsyncFunction
                }
                Log.d(TAG, "printBatch: burst write done in ${System.currentTimeMillis() - writeStart}ms — all ${decodedList.size} tickets in printer buffer")

                // Watch the counter increment per label; each tick = one ticket confirmed in FIFO order
                val ticketResults = ArrayList<String>(decodedList.size).apply { repeat(decodedList.size) { add("unknown") } }
                var confirmedCount = 0
                var lastCounter = counterBefore
                val timeoutMs = maxOf(20_000L, decodedList.size.toLong() * 8_000L)
                val pollIntervalMs = 300L
                val pollStart = System.currentTimeMillis()
                var pollIteration = 0
                var exitReason = "timeout"

                Log.d(TAG, "printBatch: poll loop starting — timeoutMs=$timeoutMs, pollIntervalMs=$pollIntervalMs")

                while (confirmedCount < decodedList.size && System.currentTimeMillis() - pollStart < timeoutMs) {
                    Thread.sleep(pollIntervalMs)
                    pollIteration++
                    val elapsed = System.currentTimeMillis() - pollStart

                    // Read counter first — attribute any new prints before checking for errors
                    val currentCounter = try {
                        SGD.GET("odometer.total_label_count", conn)?.trim()?.toIntOrNull() ?: lastCounter
                    } catch (e: Exception) {
                        Log.w(TAG, "printBatch: [poll #$pollIteration +${elapsed}ms] counter read failed — ${e.message}")
                        lastCounter
                    }

                    if (currentCounter != lastCounter) {
                        val newPrints = currentCounter - lastCounter
                        Log.d(TAG, "printBatch: [poll #$pollIteration +${elapsed}ms] counter $lastCounter → $currentCounter (+$newPrints)")
                        repeat(newPrints) {
                            if (confirmedCount < ticketResults.size) {
                                ticketResults[confirmedCount] = "confirmed"
                                Log.d(TAG, "printBatch: ticket index $confirmedCount CONFIRMED (${confirmedCount + 1}/${decodedList.size})")
                                confirmedCount++
                            }
                        }
                        lastCounter = currentCounter
                    } else {
                        Log.d(TAG, "printBatch: [poll #$pollIteration +${elapsed}ms] counter unchanged at $currentCounter, confirmed=$confirmedCount/${decodedList.size}")
                    }

                    if (confirmedCount >= decodedList.size) {
                        exitReason = "all_confirmed"
                        break
                    }

                    val s = try {
                        printer.currentStatus
                    } catch (e: Exception) {
                        Log.w(TAG, "printBatch: [poll #$pollIteration +${elapsed}ms] status read failed — ${e.message}")
                        continue
                    }

                    Log.d(TAG, "printBatch: [poll #$pollIteration +${elapsed}ms] status: ready=${s.isReadyToPrint}, paperOut=${s.isPaperOut}, headOpen=${s.isHeadOpen}, paused=${s.isPaused}, partialFormat=${s.isPartialFormatInProgress}, bufferFull=${s.isReceiveBufferFull}")

                    if (s.isPaperOut || s.isHeadOpen || s.isReceiveBufferFull) {
                        val reason = when {
                            s.isPaperOut -> "PAPER_OUT"
                            s.isHeadOpen -> "HEAD_OPEN"
                            s.isReceiveBufferFull -> "BUFFER_FULL"
                            else -> "ERROR"
                        }
                        Log.e(TAG, "printBatch: hardware error at ticket index $confirmedCount: $reason — cancelling queue")
                        if (confirmedCount < ticketResults.size) {
                            ticketResults[confirmedCount] = reason
                        }
                        try { conn.write("~JA".toByteArray()) } catch (_: Exception) {}
                        exitReason = "hardware_error:$reason"
                        break
                    }
                }

                val pollElapsed = System.currentTimeMillis() - pollStart
                Log.d(TAG, "printBatch: poll loop exited after ${pollIteration} iterations (${pollElapsed}ms), reason=$exitReason, confirmed=$confirmedCount/${decodedList.size}")

                val counterAfter = try {
                    SGD.GET("odometer.total_label_count", conn)?.trim()?.toIntOrNull() ?: lastCounter
                } catch (e: Exception) {
                    Log.w(TAG, "printBatch: final counter read failed — ${e.message}")
                    lastCounter
                }

                Log.d(TAG, "printBatch: DONE — results=$ticketResults, counterBefore=$counterBefore, counterAfter=$counterAfter, diff=${counterAfter - counterBefore}, totalTime=${System.currentTimeMillis() - writeStart}ms")

                promise.resolve(
                    mapOf(
                        "results" to ticketResults,
                        "counterBefore" to counterBefore,
                        "counterAfter" to counterAfter,
                    )
                )
            } finally {
                try {
                    conn.close()
                    Log.d(TAG, "printBatch: connection closed")
                } catch (e: Exception) {
                    Log.e(TAG, "printBatch: error closing connection — ${e.message}")
                }
            }
        }

        AsyncFunction("resetUsbPermission") {
            pendingUsbPromise = null
            UsbPermissionState.onPermissionResult = null
            UsbPermissionState.hasPermissionToCommunicate = false
            Log.d(TAG, "USB permission state reset")
            "OK"
        }
    }

}