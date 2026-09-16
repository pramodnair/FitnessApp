package com.example.fitnessapp.ui.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.InputStream

object ImageUtils {

    fun decodeSampledBitmapFromUri(
        context: Context,
        uri: Uri,
        reqWidth: Int = 1024,
        reqHeight: Int = 1024
    ): Bitmap? {
        return try {
            var inputStream: InputStream? = context.contentResolver.openInputStream(uri) ?: return null
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(inputStream, null, boundsOptions)
            inputStream?.close()

            var inSampleSize = 1
            if (boundsOptions.outHeight > reqHeight || boundsOptions.outWidth > reqWidth) {
                val halfHeight = boundsOptions.outHeight / 2
                val halfWidth = boundsOptions.outWidth / 2
                while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                    inSampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val decoded = BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            inputStream.close()

            if (decoded == null) return null

            // Handle EXIF orientation rotation
            val exifStream = context.contentResolver.openInputStream(uri)
            val orientation = if (exifStream != null) {
                val exif = ExifInterface(exifStream)
                val attr = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
                exifStream.close()
                attr
            } else ExifInterface.ORIENTATION_NORMAL

            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(decoded, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(decoded, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(decoded, 270f)
                else -> decoded
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
