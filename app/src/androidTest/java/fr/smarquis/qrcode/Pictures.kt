package fr.smarquis.qrcode

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat.PNG
import android.graphics.Color.BLACK
import android.graphics.Color.WHITE
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.zxing.BarcodeFormat
import com.google.zxing.BarcodeFormat.*
import com.google.zxing.EncodeHintType.ERROR_CORRECTION
import com.google.zxing.EncodeHintType.MARGIN
import com.google.zxing.Writer
import com.google.zxing.aztec.AztecWriter
import com.google.zxing.common.BitMatrix
import com.google.zxing.datamatrix.DataMatrixWriter
import com.google.zxing.oned.*
import com.google.zxing.pdf417.PDF417Writer
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.L
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Pictures {

    @Test
    fun useAppContext() {
        assertEquals("fr.smarquis.qrcode", InstrumentationRegistry.getInstrumentation().targetContext.packageName)
        assertEquals("fr.smarquis.qrcode.test", InstrumentationRegistry.getInstrumentation().context.packageName)
    }

    @Test
    fun formats() {
        val hints = mapOf(MARGIN to 0)
        listOf(
            listOf(AztecWriter(), AZTEC, 100, 100, "Hello, World!"),
            listOf(CodaBarWriter(), CODABAR, 300, 100, "A123456A"),
            listOf(Code39Writer(), CODE_39, 300, 100, "1234567890"),
            listOf(Code93Writer(), CODE_93, 300, 100, "1234567890"),
            listOf(Code128Writer(), CODE_128, 300, 100, "1234567890"),
            listOf(DataMatrixWriter(), DATA_MATRIX, 200, 100, "Hello, World!"),
            listOf(EAN8Writer(), EAN_8, 300, 100, "1234567"),
            listOf(EAN13Writer(), EAN_13, 300, 100, "123456789012"),
            listOf(ITFWriter(), ITF, 300, 100, "1234567890"),
            listOf(PDF417Writer(), PDF_417, 500, 100, "Hello, World!"),
            listOf(QRCodeWriter(), QR_CODE, 100, 100, "Hello, World!"),
            listOf(UPCAWriter(), UPC_A, 300, 100, "123456789012"),
            listOf(UPCEWriter(), UPC_E, 300, 100, "1234567"),
        ).forEach { (writer, format, width, height, data) ->
            (writer as Writer).encode(data as String, format as BarcodeFormat, width as Int, height as Int, hints).toBitmap().save(format.name)
        }
    }

    @Test
    fun types() {
        val writer = QRCodeWriter()
        val hints = mapOf(MARGIN to 1, ERROR_CORRECTION to L)
        listOf(
            listOf("Text", "Hello, World!"),
            listOf("WiFi", "WIFI:S:MyWiFi;T:WPA;P:letmein;H:true;;"),
            listOf("Url", "https://example.com"),
            listOf("Sms", "smsto:+1234567890:Hello, World!"),
            listOf("GeoPoint", "geo:48.8566,2.3522?q=Earth"),
            listOf(
                "ContactInfo_vCard",
                "BEGIN:VCARD\nVERSION:3.0\nN:User\nORG:Example\nTITLE:Head\nTEL:+1234567890\nURL:https://example.com\nEMAIL:user@example.com\nADR:Earth\nNOTE:Hello\\, World!\nEND:VCARD",
            ),
            listOf("ContactInfo_MeCard", "MECARD:N:User;ORG:Example;TEL:+1234567890;URL:https\\://example.com;EMAIL:user@example.com;ADR:Earth;NOTE:Hello, World!Head;;"),
            listOf("Email", "mailto:user@example.com"),
            listOf("Phone", "tel:+1234567890"),
            listOf("CalendarEvent", "BEGIN:VEVENT\nSUMMARY:Meeting\nDTSTART:20200101T201400Z\nDTEND:20200101T211400Z\nLOCATION:Earth\nDESCRIPTION:Readme\nEND:VEVENT"),
        ).forEach { (type, data) ->
            writer.encode(data, QR_CODE, 200, 200, hints).toBitmap().save("${QR_CODE}_$type")
        }
    }

    private fun BitMatrix.toBitmap(): Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
        for (x in 0 until width) {
            for (y in 0 until height) {
                setPixel(x, y, if (get(x, y)) BLACK else WHITE)
            }
        }
    }

    private fun Bitmap.save(filename: String, format: Bitmap.CompressFormat = PNG, quality: Int = 100) {
        InstrumentationRegistry.getInstrumentation().targetContext.openFileOutput("$filename.${format.name.lowercase()}", Context.MODE_PRIVATE).use {
            compress(format, quality, it)
        }
    }

}
