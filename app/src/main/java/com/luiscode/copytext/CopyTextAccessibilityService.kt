package com.luiscode.copytext

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.net.Uri
import android.provider.MediaStore
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.os.Build
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors

class CopyTextAccessibilityService : AccessibilityService() {

    companion object {
        const val ACTION_TEST_CAPTURE =
            "com.luiscode.copytext.TEST_ACCESSIBILITY_CAPTURE"
    }

    private val executor =
        Executors.newSingleThreadExecutor()

    private var lastScreenshotUri: Uri? = null
    private var lastScreenshotTimestamp = 0L

    private val screenshotHandler =
        Handler(Looper.getMainLooper())

    private val screenshotChecker =
        object : Runnable {
            override fun run() {
                checkLatestScreenshot()

                screenshotHandler.postDelayed(
                    this,
                    1000
                )
            }
        }

    private val screenshotObserver =
        object : ContentObserver(null) {

            override fun onChange(
                selfChange: Boolean,
                uri: Uri?
            ) {
                super.onChange(
                    selfChange,
                    uri
                )

                if (uri == null) return

                screenshotHandler.post {
                    checkLatestScreenshot()
                }
            }
        }

    private val captureReceiver =
        object : BroadcastReceiver() {

            override fun onReceive(
                context: Context?,
                intent: Intent?
            ) {
                if (
                    intent?.action ==
                    ACTION_TEST_CAPTURE
                ) {
                    captureScreen()
                }
            }
        }

    override fun onServiceConnected() {
        super.onServiceConnected()

        ContextCompat.registerReceiver(
            this,
            captureReceiver,
            IntentFilter(ACTION_TEST_CAPTURE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            screenshotObserver
        )

        screenshotHandler.post(
            screenshotChecker
        )

        Toast.makeText(
            this,
            "CopyText: acessibilidade ativada",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun checkLatestScreenshot() {

        executor.execute {

            try {
                val projection = arrayOf(
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.RELATIVE_PATH,
                    MediaStore.Images.Media.DATE_ADDED
                )

                contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?",
                    arrayOf("%Pictures/Screenshots%"),
                    "${MediaStore.Images.Media.DATE_ADDED} DESC"
                )?.use { cursor ->

                    if (!cursor.moveToFirst()) {
                        return@execute
                    }

                    val idIndex =
                        cursor.getColumnIndex(
                            MediaStore.Images.Media._ID
                        )

                    val nameIndex =
                        cursor.getColumnIndex(
                            MediaStore.Images.Media.DISPLAY_NAME
                        )

                    val pathIndex =
                        cursor.getColumnIndex(
                            MediaStore.Images.Media.RELATIVE_PATH
                        )

                    val dateIndex =
                        cursor.getColumnIndex(
                            MediaStore.Images.Media.DATE_ADDED
                        )

                    if (
                        idIndex < 0 ||
                        nameIndex < 0 ||
                        pathIndex < 0 ||
                        dateIndex < 0
                    ) {
                        return@execute
                    }

                    val id =
                        cursor.getLong(idIndex)

                    val name =
                        cursor.getString(nameIndex)

                    val relativePath =
                        cursor.getString(pathIndex)

                    val dateAdded =
                        cursor.getLong(dateIndex)

                    val uri =
                        Uri.withAppendedPath(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            id.toString()
                        )

                    if (
                        lastScreenshotTimestamp == 0L
                    ) {
                        lastScreenshotTimestamp =
                            dateAdded
                        lastScreenshotUri = uri
                        return@execute
                    }

                    if (
                        dateAdded <=
                        lastScreenshotTimestamp
                    ) {
                        return@execute
                    }

                    if (
                        !name.startsWith(
                            "Screenshot_",
                            ignoreCase = true
                        )
                    ) {
                        return@execute
                    }

                    if (
                        !relativePath.contains(
                            "Screenshots",
                            ignoreCase = true
                        )
                    ) {
                        return@execute
                    }

                    lastScreenshotTimestamp =
                        dateAdded

                    lastScreenshotUri =
                        uri

                    mainExecutor.execute {

                        val intent =
                            Intent(
                                this@CopyTextAccessibilityService,
                                ScreenshotConfirmationActivity::class.java
                            ).apply {
                                flags =
                                    Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP

                                putExtra(
                                    "SCREENSHOT_URI",
                                    uri.toString()
                                )
                            }

                        startActivity(intent)
                    }
                }

            } catch (_: Exception) {
            }
        }
    }

    private fun captureScreen() {

        if (
            Build.VERSION.SDK_INT <
            Build.VERSION_CODES.R
        ) {
            showToast(
                "Captura não suportada neste Android."
            )
            return
        }

        takeScreenshot(
            Display.DEFAULT_DISPLAY,
            executor,
            object : TakeScreenshotCallback {

                override fun onSuccess(
                    screenshot: ScreenshotResult
                ) {

                    val hardwareBuffer =
                        screenshot.hardwareBuffer

                    val hardwareBitmap =
                        Bitmap.wrapHardwareBuffer(
                            hardwareBuffer,
                            screenshot.colorSpace
                        )

                    val bitmap =
                        hardwareBitmap?.copy(
                            Bitmap.Config.ARGB_8888,
                            false
                        )

                    hardwareBuffer.close()

                    if (bitmap == null) {
                        showToast(
                            "Não foi possível criar a imagem."
                        )
                        return
                    }

                    ScreenshotHolder.bitmap =
                        bitmap

                    mainExecutor.execute {

                        val intent =
                            Intent(
                                this@CopyTextAccessibilityService,
                                SelectionActivity::class.java
                            ).apply {
                                flags =
                                    Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP
                            }

                        startActivity(intent)
                    }
                }

                override fun onFailure(
                    errorCode: Int
                ) {
                    showToast(
                        "Falha na captura. Código: $errorCode"
                    )
                }
            }
        )
    }

    private fun showToast(
        message: String
    ) {
        mainExecutor.execute {
            Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onAccessibilityEvent(
        event: AccessibilityEvent?
    ) {
        // Eventos de acessibilidade serão analisados
        // posteriormente para detectar screenshots.
    }

    override fun onInterrupt() {
    }

    override fun onDestroy() {

        try {
            unregisterReceiver(
                captureReceiver
            )
        } catch (_: Exception) {
        }

        try {
            contentResolver.unregisterContentObserver(
                screenshotObserver
            )
        } catch (_: Exception) {
        }

        screenshotHandler.removeCallbacks(
            screenshotChecker
        )

        executor.shutdownNow()

        super.onDestroy()
    }
}
