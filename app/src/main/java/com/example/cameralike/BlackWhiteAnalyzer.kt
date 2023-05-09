package com.example.cameralike

import android.graphics.Bitmap
import android.graphics.*
import android.media.Image
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream
import android.graphics.Color;


class BlackWhiteAnalyzer : ImageAnalysis.Analyzer {

    private var lastCapturedImage: ImageProxy? = null

    @ExperimentalGetImage
    override fun analyze(image: ImageProxy) {
        lastCapturedImage = image
        val bitmap = image.image?.toBitmap() ?: return
        val grayBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(grayBitmap)
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

    }
    fun Image.toBitmap(): Bitmap? {
        val nv21Buffer = planes[0].buffer
        val nv21Bytes = ByteArray(nv21Buffer.remaining())
        nv21Buffer.get(nv21Bytes)

        val yuvImage = YuvImage(nv21Bytes, ImageFormat.NV21, width, height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, width, height), 50, out)
        val jpegBytes = out.toByteArray()

        return BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
    }

}