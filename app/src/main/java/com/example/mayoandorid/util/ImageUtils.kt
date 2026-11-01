package com.example.mayoandorid.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File

object ImageUtils {

    fun bitmapToBase64(bitmap: Bitmap?, quality: Int = 80): String? {
        if (bitmap == null) return null
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        val byteArray = outputStream.toByteArray()
        return "data:image/jpeg;base64," + Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    fun bitmapToPngBase64(bitmap: Bitmap?): String? {
        if (bitmap == null) return null
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val byteArray = outputStream.toByteArray()
        return "data:image/png;base64," + Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    fun base64ToBitmap(base64Str: String?): Bitmap? {
        if (base64Str.isNullOrEmpty()) return null
        return try {
            val cleanStr = if (base64Str.contains(",")) {
                base64Str.substring(base64Str.indexOf(",") + 1)
            } else {
                base64Str
            }
            val decodedBytes = Base64.decode(cleanStr, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun scaleBitmap(bitmap: Bitmap?, maxDimension: Int): Bitmap? {
        if (bitmap == null) return null
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxDimension && height <= maxDimension) {
            return bitmap
        }

        val ratio = width.toFloat() / height.toFloat()
        val targetWidth: Int
        val targetHeight: Int
        if (ratio > 1) {
            targetWidth = maxDimension
            targetHeight = (maxDimension / ratio).toInt()
        } else {
            targetHeight = maxDimension
            targetWidth = (maxDimension * ratio).toInt()
        }
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    fun decodeAndRotateFile(filePath: String, maxDimension: Int = 1024): Bitmap? {
        val file = File(filePath)
        if (!file.exists()) return null

        return try {
            val exif = ExifInterface(filePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            val rotationDegrees = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }

            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(filePath, options)

            var sampleSize = 1
            while (options.outWidth / sampleSize > maxDimension * 2 || options.outHeight / sampleSize > maxDimension * 2) {
                sampleSize *= 2
            }

            options.inJustDecodeBounds = false
            options.inSampleSize = sampleSize

            val decodedBitmap = BitmapFactory.decodeFile(filePath, options) ?: return null
            val scaledBitmap = scaleBitmap(decodedBitmap, maxDimension) ?: decodedBitmap

            if (rotationDegrees != 0) {
                val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.width, scaledBitmap.height, matrix, true)
            } else {
                scaledBitmap
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun uriToBitmap(context: Context, uri: Uri, maxDimension: Int = 1024): Bitmap? {
        return try {
            var rotationDegrees = 0
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val exif = ExifInterface(stream)
                    val orientation = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                    rotationDegrees = when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270
                        else -> 0
                    }
                }
            } catch (ignored: Exception) {}

            val inputBounds = context.contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputBounds, null, options)
            inputBounds?.close()

            var sampleSize = 1
            while (options.outWidth / sampleSize > maxDimension * 2 || options.outHeight / sampleSize > maxDimension * 2) {
                sampleSize *= 2
            }

            options.inJustDecodeBounds = false
            options.inSampleSize = sampleSize

            val input = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(input, null, options)
            input?.close()

            val scaled = scaleBitmap(bitmap, maxDimension) ?: bitmap
            if (scaled != null && rotationDegrees != 0) {
                val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                Bitmap.createBitmap(scaled, 0, 0, scaled.width, scaled.height, matrix, true)
            } else {
                scaled
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}