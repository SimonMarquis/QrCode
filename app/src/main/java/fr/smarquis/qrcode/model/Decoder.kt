package fr.smarquis.qrcode.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
import android.graphics.Matrix
import android.graphics.Rect
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.P
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.exifinterface.media.ExifInterface
import androidx.exifinterface.media.ExifInterface.*
import com.google.android.gms.tasks.Tasks
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode.FORMAT_ALL_FORMATS
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.multi.GenericMultipleBarcodeReader
import fr.smarquis.qrcode.utils.TAG
import io.fotoapparat.preview.Frame
import kotlin.math.pow

sealed class Decoder {

    companion object {

        private fun centerX(frame: Frame): Double {
            return when (frame.rotation) {
                90, 270 -> frame.size.height * 0.5
                else -> frame.size.width * 0.5
            }
        }

        private fun centerY(frame: Frame): Double {
            return when (frame.rotation) {
                90, 270 -> frame.size.width * 0.5
                else -> frame.size.height * 0.5
            }
        }

        private fun distance(frame: Frame, rect: Rect): Double {
            val centerX = rect.exactCenterX().toDouble()
            val centerY = rect.exactCenterY().toDouble()
            return distance(centerX(frame), centerX, centerY(frame), centerY)
        }

        private fun distance(frame: Frame, resultPoints: Array<out ResultPoint>): Double {
            val centerX = resultPoints.map { it.x }.average()
            val centerY = resultPoints.map { it.y }.average()
            return distance(centerX(frame), centerX, centerY(frame), centerY)
        }

        private fun distance(bitmap: Bitmap, resultPoints: Array<out ResultPoint>): Double {
            val centerX = resultPoints.map { it.x }.average()
            val centerY = resultPoints.map { it.y }.average()
            return distance(bitmap.width * 0.5, centerX, bitmap.height * 0.5, centerY)
        }

        private fun distance(x1: Double, x2: Double, y1: Double, y2: Double): Double {
            return kotlin.math.sqrt((x1 - x2).pow(2.0) + (y1 - y2).pow(2.0))
        }

    }

    abstract fun name(): String

    @Throws(java.lang.Exception::class)
    @WorkerThread
    abstract fun decode(context: Context, frame: Frame): Barcode?

    @Throws(java.lang.Exception::class)
    @WorkerThread
    abstract fun decode(context: Context, uri: Uri): Barcode?

    object MLKit : Decoder() {

        var isAvailable: Boolean = true

        override fun name(): String = "ML Kit"

        private val detector: FirebaseVisionBarcodeDetector by lazy {
            val instance = FirebaseVision.getInstance()
            instance.isStatsCollectionEnabled = false
            instance.getVisionBarcodeDetector(FirebaseVisionBarcodeDetectorOptions.Builder().setBarcodeFormats(FORMAT_ALL_FORMATS).build())
        }

        override fun decode(context: Context, frame: Frame): Barcode? {
            val start = SystemClock.elapsedRealtime()
            val metadata = FirebaseVisionImageMetadata.Builder()
                .setWidth(frame.size.width)
                .setHeight(frame.size.height)
                .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
                .setRotation(
                    when (frame.rotation) {
                        90 -> FirebaseVisionImageMetadata.ROTATION_90
                        180 -> FirebaseVisionImageMetadata.ROTATION_180
                        270 -> FirebaseVisionImageMetadata.ROTATION_270
                        else -> FirebaseVisionImageMetadata.ROTATION_0
                    }
                )
                .build()
            val task = detector.detectInImage(FirebaseVisionImage.fromByteArray(frame.image, metadata))
            val results = Tasks.await(task)
            val vision = results.minBy {
                distance(frame, it.boundingBox ?: return@minBy Double.MAX_VALUE)
            } ?: return null
            val elapsed = SystemClock.elapsedRealtime() - start
            Log.d(TAG, "Found ${results.size} by ${name()} in ${elapsed}ms")
            return Barcode.parse(context, vision)
        }

        override fun decode(context: Context, uri: Uri): Barcode? {
            val start = SystemClock.elapsedRealtime()
            val task = detector.detectInImage(FirebaseVisionImage.fromFilePath(context, uri))
            val results = Tasks.await(task)
            val vision = results.firstOrNull() ?: return null
            val elapsed = SystemClock.elapsedRealtime() - start
            Log.d(TAG, "Found ${results.size} by ${name()} in ${elapsed}ms")
            return Barcode.parse(context, vision)
        }
    }

    object ZXing : Decoder() {

        private val multiReader = GenericMultipleBarcodeReader(MultiFormatReader())

        override fun name(): String = "ZXing"

        override fun decode(context: Context, frame: Frame): Barcode? {
            val start = SystemClock.elapsedRealtime()
            val width = frame.size.width
            val height = frame.size.height
            val source = PlanarYUVLuminanceSource(frame.image, width, height, 0, 0, width, height, false)
            val bitmap = BinaryBitmap(HybridBinarizer(source))

            try {
                val results = multiReader.decodeMultiple(bitmap)
                val result = results.minBy {
                    distance(frame, it.resultPoints ?: return@minBy Double.MAX_VALUE)
                } ?: return null
                val elapsed = SystemClock.elapsedRealtime() - start
                Log.d(TAG, "Found ${results.size} by ${name()} in ${elapsed}ms")
                return Barcode.parse(context, result)
            } catch (e: Exception) {
                // Safe to ignore
                // Empty result is reported as an Exception
            }
            return null
        }

        override fun decode(context: Context, uri: Uri): Barcode? {
            val start = SystemClock.elapsedRealtime()
            val original = if (SDK_INT >= P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, _, _ -> decoder.allocator = ALLOCATOR_SOFTWARE }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
            val image = context.contentResolver.openInputStream(uri)?.use {
                when (ExifInterface(it).getAttributeInt(TAG_ORIENTATION, ORIENTATION_NORMAL)) {
                    ORIENTATION_ROTATE_90 -> original.rotate(90f)
                    ORIENTATION_ROTATE_180 -> original.rotate(180f)
                    ORIENTATION_ROTATE_270 -> original.rotate(270f)
                    ORIENTATION_FLIP_HORIZONTAL -> original.flip(horizontal = true, vertical = false)
                    ORIENTATION_FLIP_VERTICAL -> original.flip(horizontal = false, vertical = true)
                    /*ORIENTATION_TRANSVERSE, ORIENTATION_TRANSPOSE*/ else -> original
                }
            } ?: original
            if (image != original) {
                original.recycle()
            }
            try {
                val results = multiReader.decodeMultiple(BinaryBitmap(HybridBinarizer(RGBLuminanceSource(image.width, image.height, image.pixels()))))
                val result = results.minBy {
                    distance(image, it.resultPoints ?: return@minBy Double.MAX_VALUE)
                } ?: return null
                val elapsed = SystemClock.elapsedRealtime() - start
                Log.d(TAG, "Found ${results.size} by ${name()} in ${elapsed}ms")
                return Barcode.parse(context, result)
            } catch (e: Exception) {
                // Safe to ignore
                // Empty result is reported as an Exception
            } finally {
                image.recycle()
            }
            return null
        }

        private fun Bitmap.rotate(degrees: Float): Bitmap {
            val matrix = Matrix().apply { postRotate(degrees) }
            return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
        }

        private fun Bitmap.flip(horizontal: Boolean, vertical: Boolean): Bitmap? {
            val matrix = Matrix().apply { postScale(if (horizontal) -1F else 1F, if (vertical) -1F else 1F) }
            return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
        }

        private fun Bitmap.pixels(): IntArray = IntArray(width * height).apply {
            getPixels(this, 0, width, 0, 0, width, height)
        }
    }

}