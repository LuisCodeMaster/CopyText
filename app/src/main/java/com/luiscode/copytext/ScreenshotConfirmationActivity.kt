package com.luiscode.copytext

import android.app.Activity
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class ScreenshotConfirmationActivity : Activity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        val layout =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    48,
                    48,
                    48,
                    48
                )
            }

        val title =
            TextView(this).apply {
                text = "Quer capturar o texto?"
                textSize = 22f

                setPadding(
                    0,
                    0,
                    0,
                    32
                )
            }

        val captureButton =
            Button(this).apply {
                text = "Capturar"
            }

        val cancelButton =
            Button(this).apply {
                text = "Cancelar"
            }

        layout.addView(title)
        layout.addView(captureButton)
        layout.addView(cancelButton)

        setContentView(layout)

        captureButton.setOnClickListener {
            loadScreenshotAndOpenSelection()
        }

        cancelButton.setOnClickListener {
            finish()
        }
    }

    private fun loadScreenshotAndOpenSelection() {

        val uriString =
            intent.getStringExtra("SCREENSHOT_URI")

        if (uriString.isNullOrBlank()) {
            Toast.makeText(
                this,
                "Screenshot não encontrado.",
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

        val uri =
            Uri.parse(uriString)

        try {
            val bitmap =
                contentResolver.openInputStream(uri)?.use { input ->
                    BitmapFactory.decodeStream(input)
                }

            if (bitmap == null) {
                Toast.makeText(
                    this,
                    "Não foi possível carregar o screenshot.",
                    Toast.LENGTH_LONG
                ).show()

                finish()
                return
            }

            ScreenshotHolder.bitmap = bitmap

            startActivity(
                android.content.Intent(
                    this,
                    SelectionActivity::class.java
                )
            )

            finish()

        } catch (exception: Exception) {

            Toast.makeText(
                this,
                "Erro ao carregar screenshot: ${exception.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
