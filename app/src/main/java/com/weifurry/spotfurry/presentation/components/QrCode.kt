package com.weifurry.spotfurry.presentation.components

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

@Composable
internal fun QrCodeImage(
    content: String,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val qrImage =
        remember(content) {
            createQrBitmap(content = content, pixelSize = QR_BITMAP_SIZE).asImageBitmap()
        }

    Image(
        bitmap = qrImage,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Fit,
        filterQuality = FilterQuality.None
    )
}

private fun createQrBitmap(
    content: String,
    pixelSize: Int
): Bitmap {
    require(content.isNotBlank()) { "QR content must not be blank" }

    val hints =
        mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 1
        )
    val bitMatrix =
        QRCodeWriter().encode(
            content,
            BarcodeFormat.QR_CODE,
            pixelSize,
            pixelSize,
            hints
        )
    val pixels = IntArray(bitMatrix.width * bitMatrix.height)

    for (y in 0 until bitMatrix.height) {
        val rowOffset = y * bitMatrix.width
        for (x in 0 until bitMatrix.width) {
            pixels[rowOffset + x] =
                if (bitMatrix[x, y]) {
                    AndroidColor.BLACK
                } else {
                    AndroidColor.WHITE
                }
        }
    }

    return Bitmap
        .createBitmap(bitMatrix.width, bitMatrix.height, Bitmap.Config.ARGB_8888)
        .apply {
            setPixels(
                pixels,
                0,
                bitMatrix.width,
                0,
                0,
                bitMatrix.width,
                bitMatrix.height
            )
        }
}

private const val QR_BITMAP_SIZE = 512
