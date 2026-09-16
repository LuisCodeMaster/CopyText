package com.luiscode.copytext

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.util.DisplayMetrics
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.luiscode.copytext.capture.ScreenCaptureService
import com.luiscode.copytext.capture.ScreenshotResultHolder

class MainActivity : AppCompatActivity() {

    private val screenCaptureLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->

            if (
                result.resultCode != Activity.RESULT_OK ||
                result.data == null
            ) {
                Toast.makeText(
                    this,
                    "Captura de tela cancelada.",
                    Toast.LENGTH_SHORT
                ).show()

                return@registerForActivityResult
            }

            startCaptureService(
                result.resultCode,
                result.data!!
            )
        }

    private val captureReceiver =
        object : BroadcastReceiver() {

            override fun onReceive(
                context: Context?,
                intent: Intent?
            ) {

                if (
                    intent?.action ==
                    ScreenCaptureService.ACTION_CAPTURE_COMPLETE
                ) {

                    val bitmap =
                        ScreenshotResultHolder.bitmap

                    if (bitmap == null) {
                        Toast.makeText(
                            this@MainActivity,
                            "Não foi possível capturar a tela.",
                            Toast.LENGTH_LONG
                        ).show()

                        return
                    }

                    ScreenshotHolder.bitmap = bitmap

                    startActivity(
                        Intent(
                            this@MainActivity,
                            SelectionActivity::class.java
                        )
                    )
                }
            }
        }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        if (
            android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(
                Manifest.permission.READ_MEDIA_IMAGES
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES
                ),
                200
            )
        }

        val captureButton =
            findViewById<Button>(
                R.id.captureButton
            )

        val testAccessibilityButton =
            findViewById<Button>(
                R.id.testAccessibilityButton
            )

        testAccessibilityButton.setOnClickListener {

            val intent =
                Intent(
                    CopyTextAccessibilityService.ACTION_TEST_CAPTURE
                ).setPackage(packageName)

            sendBroadcast(intent)
        }

        captureButton.setOnClickListener {
            requestScreenCapture()
        }

        if (
            intent.getBooleanExtra(
                "START_CAPTURE_FROM_TILE",
                false
            )
        ) {
            window.decorView.post {
                requestScreenCapture()
            }
        }

        val filter =
            IntentFilter(
                ScreenCaptureService.ACTION_CAPTURE_COMPLETE
            )

        ContextCompat.registerReceiver(
            this,
            captureReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private fun requestScreenCapture() {

        val mediaProjectionManager =
            getSystemService(
                Context.MEDIA_PROJECTION_SERVICE
            ) as MediaProjectionManager

        val captureIntent =
            mediaProjectionManager
                .createScreenCaptureIntent()

        screenCaptureLauncher.launch(
            captureIntent
        )
    }

    private fun startCaptureService(
        resultCode: Int,
        data: Intent
    ) {

        val metrics =
            DisplayMetrics()

        @Suppress("DEPRECATION")
        windowManager.defaultDisplay
            .getRealMetrics(metrics)

        val serviceIntent =
            Intent(
                this,
                ScreenCaptureService::class.java
            ).apply {

                action =
                    ScreenCaptureService.ACTION_START

                putExtra(
                    ScreenCaptureService.EXTRA_RESULT_CODE,
                    resultCode
                )

                putExtra(
                    ScreenCaptureService.EXTRA_RESULT_DATA,
                    data
                )

                putExtra(
                    ScreenCaptureService.EXTRA_WIDTH,
                    metrics.widthPixels
                )

                putExtra(
                    ScreenCaptureService.EXTRA_HEIGHT,
                    metrics.heightPixels
                )

                putExtra(
                    ScreenCaptureService.EXTRA_DENSITY,
                    metrics.densityDpi
                )
            }

        ContextCompat.startForegroundService(
            this,
            serviceIntent
        )
    }

    override fun onDestroy() {

        unregisterReceiver(
            captureReceiver
        )

        super.onDestroy()
    }
}
