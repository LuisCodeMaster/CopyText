package com.luiscode.copytext

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.luiscode.copytext.capture.SelectionView
import com.luiscode.copytext.ocr.TextRecognizerManager

class SelectionActivity : AppCompatActivity() {

    private lateinit var screenshotImage: ImageView
    private lateinit var selectionView: SelectionView
    private lateinit var confirmButton: Button

    private lateinit var resultPanel: LinearLayout
    private lateinit var resultText: TextView
    private lateinit var copyButton: Button
    private lateinit var newSelectionButton: Button
    private lateinit var closeButton: Button

    private var screenshot: Bitmap? = null
    private var recognizedText: String = ""

    private lateinit var textRecognizerManager: TextRecognizerManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_selection)

        screenshotImage =
            findViewById(R.id.screenshotImage)

        selectionView =
            findViewById(R.id.selectionView)

        confirmButton =
            findViewById(R.id.confirmButton)

        resultPanel =
            findViewById(R.id.resultPanel)

        resultText =
            findViewById(R.id.resultText)

        copyButton =
            findViewById(R.id.copyButton)

        newSelectionButton =
            findViewById(R.id.newSelectionButton)

        closeButton =
            findViewById(R.id.closeButton)

        textRecognizerManager =
            TextRecognizerManager()

        val bitmap =
            ScreenshotHolder.bitmap

        if (bitmap == null) {
            Toast.makeText(
                this,
                "Nenhuma captura disponível.",
                Toast.LENGTH_SHORT
            ).show()

            finish()
            return
        }

        screenshot = bitmap

        screenshotImage.setImageBitmap(bitmap)

        confirmButton.setOnClickListener {
            confirmSelection()
        }

        copyButton.setOnClickListener {
            copyRecognizedText()
        }

        newSelectionButton.setOnClickListener {
            resetSelection()
        }

        closeButton.setOnClickListener {
            finish()
        }
    }

    private fun confirmSelection() {

        val bitmap =
            screenshot ?: return

        val selectionViewRect =
            selectionView.selectionRect

        if (
            selectionViewRect == null ||
            selectionViewRect.width() < 10 ||
            selectionViewRect.height() < 10
        ) {
            Toast.makeText(
                this,
                "Selecione uma área maior.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val bitmapRect =
            mapSelectionToBitmap(
                selectionViewRect,
                bitmap
            )

        if (
            bitmapRect.width() < 10 ||
            bitmapRect.height() < 10
        ) {
            Toast.makeText(
                this,
                "Área selecionada muito pequena.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        confirmButton.isEnabled = false
        confirmButton.text = "Lendo..."

        val selectedBitmap =
            cropBitmap(
                bitmap,
                bitmapRect
            )

        ScreenshotHolder.selectedBitmap =
            selectedBitmap

        textRecognizerManager.recognize(
            selectedBitmap,

            onSuccess = { text ->

                runOnUiThread {

                    confirmButton.isEnabled = true
                    confirmButton.text = "Extrair texto"

                    if (text.isBlank()) {

                        Toast.makeText(
                            this,
                            "Nenhum texto encontrado.",
                            Toast.LENGTH_LONG
                        ).show()

                        return@runOnUiThread
                    }

                    recognizedText = text

                    resultText.text = text

                    resultPanel.visibility =
                        View.VISIBLE

                    confirmButton.visibility =
                        View.GONE
                }
            },

            onError = { exception ->

                runOnUiThread {

                    confirmButton.isEnabled = true
                    confirmButton.text = "Extrair texto"

                    Toast.makeText(
                        this,
                        "Erro no OCR: ${exception.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )
    }

    private fun mapSelectionToBitmap(
        selection: RectF,
        bitmap: Bitmap
    ): RectF {

        val viewWidth =
            screenshotImage.width.toFloat()

        val viewHeight =
            screenshotImage.height.toFloat()

        if (
            viewWidth <= 0f ||
            viewHeight <= 0f
        ) {
            return RectF(
                0f,
                0f,
                bitmap.width.toFloat(),
                bitmap.height.toFloat()
            )
        }

        val scale =
            minOf(
                viewWidth / bitmap.width,
                viewHeight / bitmap.height
            )

        val displayedWidth =
            bitmap.width * scale

        val displayedHeight =
            bitmap.height * scale

        val offsetX =
            (viewWidth - displayedWidth) / 2f

        val offsetY =
            (viewHeight - displayedHeight) / 2f

        val left =
            ((selection.left - offsetX) / scale)
                .coerceIn(
                    0f,
                    bitmap.width.toFloat()
                )

        val top =
            ((selection.top - offsetY) / scale)
                .coerceIn(
                    0f,
                    bitmap.height.toFloat()
                )

        val right =
            ((selection.right - offsetX) / scale)
                .coerceIn(
                    0f,
                    bitmap.width.toFloat()
                )

        val bottom =
            ((selection.bottom - offsetY) / scale)
                .coerceIn(
                    0f,
                    bitmap.height.toFloat()
                )

        return RectF(
            minOf(left, right),
            minOf(top, bottom),
            maxOf(left, right),
            maxOf(top, bottom)
        )
    }

    private fun cropBitmap(
        bitmap: Bitmap,
        rect: RectF
    ): Bitmap {

        val left =
            rect.left
                .toInt()
                .coerceIn(
                    0,
                    bitmap.width - 1
                )

        val top =
            rect.top
                .toInt()
                .coerceIn(
                    0,
                    bitmap.height - 1
                )

        val right =
            rect.right
                .toInt()
                .coerceIn(
                    left + 1,
                    bitmap.width
                )

        val bottom =
            rect.bottom
                .toInt()
                .coerceIn(
                    top + 1,
                    bitmap.height
                )

        return Bitmap.createBitmap(
            bitmap,
            left,
            top,
            right - left,
            bottom - top
        )
    }

    private fun copyRecognizedText() {

        if (recognizedText.isBlank()) {
            return
        }

        val clipboard =
            getSystemService(
                Context.CLIPBOARD_SERVICE
            ) as ClipboardManager

        val clip =
            ClipData.newPlainText(
                "CopyText",
                recognizedText
            )

        clipboard.setPrimaryClip(clip)

        Toast.makeText(
            this,
            "Texto copiado!",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun resetSelection() {

        resultPanel.visibility =
            View.GONE

        confirmButton.visibility =
            View.VISIBLE

        selectionView.clearSelection()

        recognizedText = ""

        resultText.text = ""
    }

    override fun onDestroy() {

        textRecognizerManager.close()

        super.onDestroy()
    }
}
