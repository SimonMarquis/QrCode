package fr.smarquis.qrcode

import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode
import com.google.zxing.BarcodeFormat
import com.google.zxing.Result

enum class Format(val value: String? = null) {
    AZTEC("Aztec"),
    CODABAR("CODABAR"),
    CODE_39("Code 39"),
    CODE_93("Code 93"),
    CODE_128("Code 128"),
    DATA_MATRIX("Data Matrix"),
    EAN_8("EAN-8"),
    EAN_13("EAN-13"),
    ITF("ITF"),
    MAXICODE("MaxiCode"),
    PDF_417("PDF417"),
    QR_CODE("QR Code"),
    RSS_14("RSS 14"),
    RSS_EXPANDED("RSS EXPANDED"),
    UPC_A("UPC-A"),
    UPC_E("UPC-E"),
    UNKNOWN,
    ;

    companion object {

        fun of(barcode: FirebaseVisionBarcode): Format = when (barcode.format) {
            FirebaseVisionBarcode.FORMAT_CODE_128 -> CODE_128
            FirebaseVisionBarcode.FORMAT_CODE_39 -> CODE_39
            FirebaseVisionBarcode.FORMAT_CODE_93 -> CODE_93
            FirebaseVisionBarcode.FORMAT_CODABAR -> CODABAR
            FirebaseVisionBarcode.FORMAT_DATA_MATRIX -> DATA_MATRIX
            FirebaseVisionBarcode.FORMAT_EAN_13 -> EAN_13
            FirebaseVisionBarcode.FORMAT_EAN_8 -> EAN_8
            FirebaseVisionBarcode.FORMAT_ITF -> ITF
            FirebaseVisionBarcode.FORMAT_QR_CODE -> QR_CODE
            FirebaseVisionBarcode.FORMAT_UPC_A -> UPC_A
            FirebaseVisionBarcode.FORMAT_UPC_E -> UPC_E
            FirebaseVisionBarcode.FORMAT_PDF417 -> PDF_417
            FirebaseVisionBarcode.FORMAT_AZTEC -> AZTEC
            else -> UNKNOWN
        }

        fun of(result: Result): Format = when (result.barcodeFormat) {
            BarcodeFormat.AZTEC -> AZTEC
            BarcodeFormat.CODABAR -> CODABAR
            BarcodeFormat.CODE_39 -> CODE_39
            BarcodeFormat.CODE_93 -> CODE_93
            BarcodeFormat.CODE_128 -> CODE_128
            BarcodeFormat.DATA_MATRIX -> DATA_MATRIX
            BarcodeFormat.EAN_8 -> EAN_8
            BarcodeFormat.EAN_13 -> EAN_13
            BarcodeFormat.ITF -> ITF
            BarcodeFormat.MAXICODE -> MAXICODE
            BarcodeFormat.PDF_417 -> PDF_417
            BarcodeFormat.QR_CODE -> QR_CODE
            BarcodeFormat.RSS_14 -> RSS_14
            BarcodeFormat.RSS_EXPANDED -> RSS_EXPANDED
            BarcodeFormat.UPC_A -> UPC_A
            BarcodeFormat.UPC_E -> UPC_E
            BarcodeFormat.UPC_EAN_EXTENSION -> UNKNOWN
            else -> UNKNOWN
        }

    }
}