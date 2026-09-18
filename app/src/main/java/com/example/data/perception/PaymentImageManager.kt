package com.example.data.perception

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Handles image persistence and rotation correction in app-private storage.
 * Ensures zero cloud transmission and safe local storage.
 */
class PaymentImageManager(private val context: Context) {

    private val evidenceDir: File
        get() {
            val dir = File(context.filesDir, "payment_evidence")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }

    /**
     * Prepares a target file for CameraX ImageCapture.
     */
    fun createCaptureOutputFile(): File {
        val timestamp = System.currentTimeMillis()
        return File(evidenceDir, "evidence_${timestamp}.jpg")
    }

    /**
     * Copies and normalizes an image selected from the gallery / photo picker into app-private storage.
     * Corrects EXIF rotation if necessary.
     */
    suspend fun saveImageFromUri(uri: Uri): Result<File> = withContext(Dispatchers.IO) {
        try {
            val targetFile = createCaptureOutputFile()
            var inputStream: InputStream? = null
            var exifOrientation = ExifInterface.ORIENTATION_NORMAL

            try {
                // Read EXIF orientation
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val exif = ExifInterface(stream)
                    exifOrientation = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                }

                inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    return@withContext Result.failure(IllegalStateException("Could not open stream for selected image URI"))
                }

                if (exifOrientation == ExifInterface.ORIENTATION_NORMAL || exifOrientation == ExifInterface.ORIENTATION_UNDEFINED) {
                    // Straight copy to avoid lossy recompression
                    FileOutputStream(targetFile).use { output ->
                        inputStream.copyTo(output)
                    }
                } else {
                    // Decode, rotate, and save
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    if (bitmap == null) {
                        return@withContext Result.failure(IllegalStateException("Could not decode image bitmap"))
                    }

                    val rotationDegrees = when (exifOrientation) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                        else -> 0f
                    }

                    val rotatedBitmap = if (rotationDegrees != 0f) {
                        val matrix = Matrix().apply { postRotate(rotationDegrees) }
                        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                    } else {
                        bitmap
                    }

                    FileOutputStream(targetFile).use { output ->
                        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)
                    }

                    if (rotatedBitmap != bitmap) {
                        rotatedBitmap.recycle()
                    }
                    bitmap.recycle()
                }

                Result.success(targetFile)
            } finally {
                inputStream?.close()
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
