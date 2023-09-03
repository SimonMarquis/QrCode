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
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.camera.core.ImageProxy
import androidx.exifinterface.media.ExifInterface
import androidx.exifinterface.media.ExifInterface.*
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode.FORMAT_ALL_FORMATS
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.multi.GenericMultipleBarcodeReader
import fr.smarquis.qrcode.utils.TAG
import fr.smarquis.qrcode.utils.rotate
import fr.smarquis.qrcode.utils.toLuminance
import kotlin.math.pow
import kotlin.time.DurationUnit.MILLISECONDS
import kotlin.time.ExperimentalTime
import kotlin.time.TimedValue
import kotlin.time.measureTimedValue

sealed class Decoder {

    companion object {

        private fun ImageProxy.centerX(): Double {
            return when (imageInfo.rotationDegrees) {
                90, 270 -> height * 0.5
                else -> width * 0.5
            }
        }

        private fun ImageProxy.centerY(): Double {
            return when (imageInfo.rotationDegrees) {
                90, 270 -> width * 0.5
                else -> height * 0.5
            }
        }

        private fun ImageProxy.distance(rect: Rect): Double {
            val centerX = rect.exactCenterX().toDouble()
            val centerY = rect.exactCenterY().toDouble()
            return distance(centerX(), centerX, centerY(), centerY)
        }

        private fun ImageProxy.distance(resultPoints: Array<out ResultPoint>): Double {
            val centerX = resultPoints.map { it.x }.average()
            val centerY = resultPoints.map { it.y }.average()
            return distance(centerX(), centerX, centerY(), centerY)
        }

        private fun Bitmap.distance(resultPoints: Array<out ResultPoint>): Double {
            val centerX = resultPoints.map { it.x }.average()
            val centerY = resultPoints.map { it.y }.average()
            return distance(width * 0.5, centerX, height * 0.5, centerY)
        }

        private fun distance(x1: Double, x2: Double, y1: Double, y2: Double): Double {
            return kotlin.math.sqrt((x1 - x2).pow(2.0) + (y1 - y2).pow(2.0))
        }

    }

    abstract fun name(): String

    @Throws(java.lang.Exception::class)
    @WorkerThread
    abstract fun decode(context: Context, imageProxy: ImageProxy): Barcode?

    @Throws(java.lang.Exception::class)
    @WorkerThread
    abstract fun decode(context: Context, uri: Uri): Barcode?

    @ExperimentalTime
    internal fun TimedValue<List<Barcode>>.log() = Log.d(TAG, "Found ${value.size} by ${name()} in ${duration.toString(MILLISECONDS)}")

    object MLKit : Decoder() {

        var isAvailable: Boolean = true

        override fun name(): String = "ML Kit"

        private val detector: BarcodeScanner by lazy {
            BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(FORMAT_ALL_FORMATS).build())
        }

        @ExperimentalTime
        @androidx.camera.core.ExperimentalGetImage
        override fun decode(context: Context, imageProxy: ImageProxy): Barcode? = measureTimedValue {
            val image = imageProxy.image ?: return@measureTimedValue emptyList()
            Tasks.await(detector.process(InputImage.fromMediaImage(image, imageProxy.imageInfo.rotationDegrees)))
                .sortedBy {
                    imageProxy.distance(it.boundingBox ?: return@sortedBy Double.MAX_VALUE)
                }.map(Barcode::parse)
        }.also { it.log() }.value.firstOrNull()

        @ExperimentalTime
        override fun decode(context: Context, uri: Uri): Barcode? = measureTimedValue {
            Tasks.await(detector.process(InputImage.fromFilePath(context, uri)))
                .sortedByDescending {
                    it.boundingBox?.run { width() * height() } ?: 0
                }.map(Barcode::parse)
        }.also { it.log() }.value.firstOrNull()
    }

    object ZXing : Decoder() {

        private val multiReader = GenericMultipleBarcodeReader(MultiFormatReader())

        override fun name(): String = "ZXing"

        @ExperimentalTime
        @androidx.camera.core.ExperimentalGetImage
        override fun decode(context: Context, imageProxy: ImageProxy): Barcode? = measureTimedValue {
            val image = imageProxy.image ?: return null
            val luminancePlane = image.toLuminance().rotate(imageProxy.imageInfo.rotationDegrees)
            val source = PlanarYUVLuminanceSource(luminancePlane.byteArray, luminancePlane.width, luminancePlane.height, 0, 0, luminancePlane.width, luminancePlane.height, false)
            val bitmap = BinaryBitmap(HybridBinarizer(source))
            kotlin.runCatching {
                multiReader.decodeMultiple(bitmap)
                    .sortedBy {
                        imageProxy.distance(it.resultPoints ?: return@sortedBy Double.MAX_VALUE)
                    }.map {
                        Barcode.parse(it)
                    }
            }.getOrDefault(emptyList())
        }.also { it.log() }.value.firstOrNull()

        @ExperimentalTime
        override fun decode(context: Context, uri: Uri): Barcode? = measureTimedValue {
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
            if (image != original) original.recycle()
            kotlin.runCatching {
                multiReader.decodeMultiple(BinaryBitmap(HybridBinarizer(RGBLuminanceSource(image.width, image.height, image.pixels()))))
                    .sortedBy {
                        image.distance(it.resultPoints ?: return@sortedBy Double.MAX_VALUE)
                    }.map {
                        Barcode.parse(it)
                    }
            }.getOrDefault(emptyList()).also { image.recycle() }
        }.also { it.log() }.value.firstOrNull()

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
