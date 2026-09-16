package com.luiscode.copytext

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class ScreenshotConfirmationActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(56, 48, 56, 48)
            setBackgroundColor(Color.WHITE)
        }

        val icon = TextView(this).apply {
            text = "📋"
            textSize = 42f
            gravity = Gravity.CENTER
        }

        val title = TextView(this).apply {
            text = "Copiar texto da tela?"
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(30, 30, 30))
            setPadding(0, 24, 0, 12)
        }

        val description = TextView(this).apply {
            text = "Detectamos uma nova captura de tela.\nVocê pode selecionar uma área para extrair o texto."
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(90, 90, 90))
            setPadding(0, 0, 0, 32)
        }

        val captureButton = Button(this).apply {
            text = "Capturar texto"
            textSize = 16f
            isAllCaps = false
        }

        val cancelButton = Button(this).apply {
            text = "Cancelar"
            textSize = 16f
            isAllCaps = false
        }

        root.addView(
            icon,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        root.addView(
            title,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        root.addView(
            description,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        root.addView(
            captureButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 8
            }
        )

        root.addView(
            cancelButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        setContentView(root)

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

        val uri = Uri.parse(uriString)

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
