package fr.smarquis.qrcode

import android.content.Context
import android.graphics.Rect
import android.os.SystemClock
import android.util.Log
import androidx.annotation.WorkerThread
import com.google.android.gms.tasks.Tasks
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ResultPoint
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.multi.GenericMultipleBarcodeReader
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

        private fun distance(x1: Double, x2: Double, y1: Double, y2: Double): Double {
            return kotlin.math.sqrt((x1 - x2).pow(2.0) + (y1 - y2).pow(2.0))
        }

    }

    abstract fun name(): String

    @Throws(java.lang.Exception::class)
    @WorkerThread
    abstract fun decode(context: Context, frame: Frame): Barcode?

    object MLKit : Decoder() {

        var isAvailable: Boolean = true

        override fun name(): String = "ML Kit"

        private val detector: FirebaseVisionBarcodeDetector by lazy {
            val instance = FirebaseVision.getInstance()
            instance.isStatsCollectionEnabled = false
            instance.getVisionBarcodeDetector(
                FirebaseVisionBarcodeDetectorOptions.Builder()
                    .setBarcodeFormats(FirebaseVisionBarcode.FORMAT_ALL_FORMATS)
                    .build()
            )
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
            val task = detector.detectInImage(
                FirebaseVisionImage.fromByteArray(
                    frame.image,
                    metadata
                )
            )
            val results = Tasks.await(task)
            val vision = results.minBy {
                distance(frame, it.boundingBox ?: return@minBy Double.MAX_VALUE)
            } ?: return null
            val elapsed = SystemClock.elapsedRealtime() - start
            Log.d(TAG, "Found ${results.size} by ${this.name()} in ${elapsed}ms")
            return Barcode.parse(context, vision)
        }
    }

    object ZXing : Decoder() {

        private val multiReader =
            GenericMultipleBarcodeReader(MultiFormatReader())

        override fun name(): String = "ZXing"

        override fun decode(context: Context, frame: Frame): Barcode? {
            val start = SystemClock.elapsedRealtime()
            val width = frame.size.width
            val height = frame.size.height
            val source =
                PlanarYUVLuminanceSource(frame.image, width, height, 0, 0, width, height, false)
            val bitmap = BinaryBitmap(HybridBinarizer(source))

            try {
                val results = multiReader.decodeMultiple(bitmap)
                val result = results.minBy {
                    distance(frame, it.resultPoints ?: return@minBy Double.MAX_VALUE)
                } ?: return null
                val elapsed = SystemClock.elapsedRealtime() - start
                Log.d(TAG, "Found ${results.size} by ${this.name()} in ${elapsed}ms")
                return Barcode.parse(context, result)
            } catch (e: Exception) {
                // Safe to ignore
                // Empty result is reported as an Exception
            }
            return null
        }

    }

}