package com.example.apptest

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.net.Uri

object ImagePreprocessor {
    fun loadBitmapFromUri(context: Context, imageUri: Uri): Bitmap {
        val source = ImageDecoder.createSource(context.contentResolver, imageUri)
        return ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
            decoder.isMutableRequired = true
        }
    }

    fun preprocessBitmap(original: Bitmap, targetSize: Int = 224): Bitmap {
        val width = original.width
        val height = original.height

        val scale = minOf(
            targetSize.toFloat() / width,
            targetSize.toFloat() / height
        )

        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()

        val resized = Bitmap.createScaledBitmap(original, newWidth, newHeight, true)

        val output = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(Color.BLACK)

        val left = (targetSize - newWidth) / 2f
        val top = (targetSize - newHeight) / 2f

        canvas.drawBitmap(resized, left, top, null)

        return output
    }

    fun convertBitmapToModelInput(bitmap224: Bitmap): FloatArray {
        val input = FloatArray(224 * 224 * 3)
        var index = 0

        for (y in 0 until 224) {
            for (x in 0 until 224) {
                val pixel = bitmap224.getPixel(x, y)
                val r = ((pixel shr 16) and 0xFF).toFloat()
                val g = ((pixel shr 8) and 0xFF).toFloat()
                val b = (pixel and 0xFF).toFloat()

                input[index++] = r
                input[index++] = g
                input[index++] = b
            }
        }

        return input
    }
}