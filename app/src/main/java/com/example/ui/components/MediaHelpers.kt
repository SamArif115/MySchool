package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.scale
import com.example.R
import java.io.ByteArrayOutputStream
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

const val MAX_UPLOAD_BYTES = 5 * 1024 * 1024L // 5 MB

data class PickedImageResult(
    val base64: String,
    val fileName: String,
    val originalSizeBytes: Long
)

data class PickedDocumentResult(
    val fileName: String,
    val sizeBytes: Long,
    val mimeType: String
)

fun Context.readFileDisplayNameAndSize(uri: Uri): Pair<String, Long> {
    var name = "dokumen"
    var size = 0L
    contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (nameIdx >= 0) name = cursor.getString(nameIdx) ?: name
            if (sizeIdx >= 0) size = cursor.getLong(sizeIdx)
        }
    }
    if (size <= 0L) {
        contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
            if (afd.length > 0L) size = afd.length
        }
    }
    return name to size
}

/**
 * Reads a picked image from Android Photo Picker, validates PNG/JPG & <= 5 MB,
 * and compresses a clean square/fitted preview Base64 string safe for Firestore document storage.
 */
fun Context.processPickedPngJpgImage(
    uri: Uri,
    maxDimensionPx: Int = 360
): Result<PickedImageResult> = runCatching {
    val mime = (contentResolver.getType(uri) ?: "").lowercase()
    val (rawName, originalSize) = readFileDisplayNameAndSize(uri)
    val lowerName = rawName.lowercase()

    val isPngOrJpg = mime in listOf("image/png", "image/jpeg", "image/jpg") ||
        lowerName.endsWith(".png") || lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")
    if (!isPngOrJpg) {
        throw IllegalArgumentException("Format file tidak didukung. Gunakan gambar PNG atau JPG saja.")
    }

    val rawBytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
        ?: throw IllegalStateException("Gagal membaca berkas gambar yang dipilih.")

    val actualSize = if (originalSize > 0L) originalSize else rawBytes.size.toLong()
    if (actualSize > MAX_UPLOAD_BYTES) {
        val mb = String.format(Locale.US, "%.2f", actualSize.toDouble() / (1024 * 1024))
        throw IllegalArgumentException("Ukuran file ($mb MB) melebihi batas maksimal 5 MB.")
    }

    val decoded = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)
        ?: throw IllegalArgumentException("Berkas gambar PNG/JPG tidak valid.")

    val scaleRatio = max(decoded.width, decoded.height).toFloat() / maxDimensionPx.toFloat()
    var resized = if (scaleRatio > 1f) {
        decoded.scale(
            (decoded.width / scaleRatio).roundToInt().coerceAtLeast(1),
            (decoded.height / scaleRatio).roundToInt().coerceAtLeast(1)
        )
    } else {
        decoded
    }

    val maxBase64Chars = 190_000
    val out = ByteArrayOutputStream()
    val usePng = mime == "image/png" || lowerName.endsWith(".png")
    var b64 = ""
    if (usePng) {
        resized.compress(Bitmap.CompressFormat.PNG, 90, out)
        b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }
    if (b64.isEmpty() || b64.length > maxBase64Chars) {
        for (quality in intArrayOf(82, 68, 52, 38)) {
            out.reset()
            resized.compress(Bitmap.CompressFormat.JPEG, quality, out)
            b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            if (b64.length <= maxBase64Chars) break
        }
    }
    if (b64.length > maxBase64Chars) {
        val fallbackScale = max(resized.width, resized.height).toFloat() / 240f
        if (fallbackScale > 1f) {
            resized = resized.scale(
                (resized.width / fallbackScale).roundToInt().coerceAtLeast(1),
                (resized.height / fallbackScale).roundToInt().coerceAtLeast(1)
            )
        }
        for (quality in intArrayOf(75, 55, 40)) {
            out.reset()
            resized.compress(Bitmap.CompressFormat.JPEG, quality, out)
            b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            if (b64.length <= maxBase64Chars) break
        }
    }

    PickedImageResult(
        base64 = b64,
        fileName = rawName,
        originalSizeBytes = actualSize
    )
}

fun Context.processPickedDocument(uri: Uri): Result<PickedDocumentResult> = runCatching {
    val mime = (contentResolver.getType(uri) ?: "application/octet-stream").lowercase()
    val (rawName, reportedSize) = readFileDisplayNameAndSize(uri)
    val size = if (reportedSize > 0L) {
        reportedSize
    } else {
        contentResolver.openInputStream(uri)?.use { it.readBytes().size.toLong() } ?: 0L
    }
    if (size > MAX_UPLOAD_BYTES) {
        val mb = String.format(Locale.US, "%.2f", size.toDouble() / (1024 * 1024))
        throw IllegalArgumentException("Ukuran dokumen ($mb MB) melebihi batas maksimal 5 MB.")
    }
    PickedDocumentResult(
        fileName = rawName,
        sizeBytes = size,
        mimeType = mime
    )
}

@Composable
fun rememberBase64ImageBitmap(base64: String): ImageBitmap? {
    return remember(base64) {
        if (base64.isBlank()) {
            null
        } else {
            runCatching {
                val clean = base64.substringAfter("base64,", base64)
                val bytes = Base64.decode(clean, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }.getOrNull()
        }
    }
}

@Composable
fun SchoolLogoAvatar(
    logoBase64: String,
    schoolName: String,
    size: Dp = 40.dp,
    shape: Shape = CircleShape,
    containerColor: Color = Color.White,
    modifier: Modifier = Modifier
) {
    val bitmap = rememberBase64ImageBitmap(logoBase64)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "Logo $schoolName",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape),
                contentScale = ContentScale.Crop
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.ic_school_logo),
                contentDescription = "Logo $schoolName",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp)
                    .clip(shape),
                contentScale = ContentScale.Crop
            )
        }
    }
}
