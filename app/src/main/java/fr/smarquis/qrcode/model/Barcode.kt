package fr.smarquis.qrcode.model

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.SystemClock.elapsedRealtime
import android.text.SpannedString
import androidx.core.net.toUri
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.text.scale
import com.google.mlkit.vision.barcode.common.Barcode.*
import com.google.zxing.Result
import fr.smarquis.qrcode.R
import fr.smarquis.qrcode.utils.appendKeyValue
import fr.smarquis.qrcode.utils.isSafeIntent
import java.lang.Double.parseDouble
import java.util.regex.Pattern
import com.google.mlkit.vision.barcode.common.Barcode as VisionBarcode

sealed class Barcode(open val format: Format, open val value: String) {

    companion object {

        fun parse(context: Context, vision: VisionBarcode): Barcode {
            val format = Format.of(vision)
            val value = vision.rawValue.orEmpty()
            return when (vision.valueType) {
                TYPE_URL -> Url(value, format)
                TYPE_SMS -> Sms.parse(value, format) // Don't trust vision.sms
                TYPE_EMAIL -> vision.email?.let { Email(value, format, it.address, it.subject, it.body) }
                TYPE_PHONE -> vision.phone?.let { Phone(value, format, it.number) }
                TYPE_WIFI -> vision.wifi?.let { WiFi(value, format, it.ssid, it.password, it.encryptionType) }
                TYPE_GEO -> vision.geoPoint?.let { GeoPoint(value, format, it.lat, it.lng) }
                TYPE_CALENDAR_EVENT -> vision.calendarEvent?.let { CalendarEvent(value, format, it) }
                TYPE_CONTACT_INFO -> vision.contactInfo?.let { ContactInfo(value, format, it) }
                else -> null
            } ?: fallback(context, value, format)
        }

        fun parse(context: Context, result: Result): Barcode {
            val format = Format.of(result)
            val value = result.text
            val uri = value.toUri()
            val scheme = uri.scheme
            return when (scheme?.lowercase()) {
                "http", "https" -> Url(value, format)
                "geo" -> GeoPoint.parse(value, format)
                else -> null
            } ?: fallback(context, value, format)
        }

        private fun fallback(context: Context, value: String, format: Format): Barcode {
            val url = Url(value, format)
            return if (isSafeIntent(context, url.intent)) {
                url
            } else {
                Text(value, format)
            }
        }
    }

    var timestamp = elapsedRealtime()

    abstract val key: String

    val title: CharSequence by lazy { buildTitle() }

    abstract val details: CharSequence?

    abstract val intent: Intent?

    abstract val icon: Int

    private fun buildTitle(): SpannedString {
        return buildSpannedString {
            append(key)
            if (format != Format.UNKNOWN) {
                append("  ")
                bold {
                    color(Color.LTGRAY) {
                        scale(0.5F) {
                            append(format.value)
                        }
                    }
                }
            }
        }
    }

    data class Text(
        override val value: String,
        override val format: Format,
    ) : Barcode(format, value) {
        override val icon = R.drawable.ic_subject_black_24dp
        override val key: String = "Text"
        override val details: CharSequence? = null
        override val intent: Intent? = null
    }

    data class WiFi(
        override val value: String,
        override val format: Format,
        val ssid: String?,
        val password: String?,
        val encryption: Int,
    ) :
        Barcode(format, value) {
        override val icon = R.drawable.ic_network_wifi_black_24dp
        override val key: String = "WiFi"
        override val details: CharSequence? by lazy {
            buildSpannedString {
                appendKeyValue("ssid", ssid)
                appendKeyValue("password", password)
                appendKeyValue("encryption", encryptionString)
            }
        }
        private val encryptionString: String
            get() =
                when (encryption) {
                    VisionBarcode.WiFi.TYPE_OPEN -> "OPEN"
                    VisionBarcode.WiFi.TYPE_WPA -> "WPA"
                    VisionBarcode.WiFi.TYPE_WEP -> "WEP"
                    else -> "? ($encryption)"
                }

        override val intent: Intent? = null
    }

    data class Url(
        override val value: String,
        override val format: Format,
    ) : Barcode(format, value) {
        override val icon = R.drawable.ic_link_black_24dp
        override val key: String = "Url"
        override val details: CharSequence? = null
        override val intent: Intent? by lazy {
            if (value.isBlank()) null else Intent(Intent.ACTION_VIEW, Uri.parse(value))
        }
    }

    data class Sms(
        override val value: String,
        override val format: Format,
        val phoneNumber: String?,
        val message: String?,
    ) :
        Barcode(format, value) {
        override val icon = R.drawable.ic_sms_black_24dp
        override val key: String = "Sms"
        override val details: CharSequence? by lazy {
            buildSpannedString {
                appendKeyValue("phoneNumber", phoneNumber)
                appendKeyValue("message", message)
            }
        }
        override val intent: Intent? by lazy {
            val scheme = value.toUri().scheme ?: "smsto:"
            val uri = Uri.fromParts(scheme, phoneNumber, null)
            Intent(Intent.ACTION_SENDTO, uri).apply {
                putExtra("sms_body", message)
            }
        }

        companion object {
            fun parse(value: String, format: Format): Sms {
                val ssp = value.toUri().schemeSpecificPart
                val indexOf = ssp.indexOf(":")
                val number = if (indexOf >= 0) ssp.substring(0, indexOf) else ssp
                val message = if (indexOf >= 0) ssp.substring(indexOf + 1) else null
                return Sms(value, format, number, message)
            }
        }
    }

    data class GeoPoint(
        override val value: String,
        override val format: Format,
        val lat: Double?,
        val lng: Double?,
        val altitude: Double? = null,
        val query: String? = parse(value, format)?.query,
    ) : Barcode(format, value) {
        override val icon = R.drawable.ic_location_on_black_24dp
        override val key: String = "Geo"
        override val details: CharSequence? by lazy {
            buildSpannedString {
                appendKeyValue("lat", lat?.toString())
                appendKeyValue("lng", lng?.toString())
                appendKeyValue("altitude", altitude?.toString())
                appendKeyValue("query", query)
            }
        }
        override val intent: Intent? by lazy { Intent(Intent.ACTION_VIEW, Uri.parse(value)) }

        companion object {

            private val PATTERN =
                Pattern.compile("geo:([\\-0-9.]+),([\\-0-9.]+)(?:,([\\-0-9.]+))?(?:\\?(.*))?", Pattern.CASE_INSENSITIVE)

            fun parse(value: String?, format: Format): GeoPoint? {
                val matcher = PATTERN.matcher(value.orEmpty())
                if (!matcher.matches()) {
                    return null
                }

                val query = matcher.group(4)

                val latitude: Double
                val longitude: Double
                val altitude: Double
                try {
                    latitude = parseDouble(matcher.group(1).orEmpty())
                    if (latitude > 90.0 || latitude < -90.0) {
                        return null
                    }
                    longitude = parseDouble(matcher.group(2).orEmpty())
                    if (longitude > 180.0 || longitude < -180.0) {
                        return null
                    }
                    if (matcher.group(3) == null) {
                        altitude = 0.0
                    } else {
                        altitude = parseDouble(matcher.group(3).orEmpty())
                        if (altitude < 0.0) {
                            return null
                        }
                    }
                } catch (ignored: NumberFormatException) {
                    return null
                }
                return GeoPoint(value.orEmpty(), format, latitude, longitude, altitude, query)
            }

        }

    }

    data class ContactInfo(
        override val value: String,
        override val format: Format,
        val contactInfo: VisionBarcode.ContactInfo,
    ) : Barcode(format, value) {
        override val icon = R.drawable.ic_account_circle_black_24dp
        override val key: String = "Contact"
        override val details: CharSequence? = null
        override val intent: Intent? = null
    }

    data class Email(
        override val value: String,
        override val format: Format,
        val address: String?,
        val subject: String?,
        val body: String?,
    ) :
        Barcode(format, value) {
        override val icon = R.drawable.ic_email_black_24dp
        override val key: String = "Email"
        override val details: CharSequence? by lazy {
            buildSpannedString {
                appendKeyValue("address", address)
                appendKeyValue("subject", subject)
                appendKeyValue("body", body)
            }
        }
        override val intent: Intent? by lazy { Intent(Intent.ACTION_VIEW, Uri.parse(value)) }
    }

    data class Phone(
        override val value: String,
        override val format: Format,
        val number: String?,
    ) :
        Barcode(format, value) {
        override val icon = R.drawable.ic_phone_black_24dp
        override val key: String = "Phone"
        override val details: CharSequence? by lazy {
            buildSpannedString {
                appendKeyValue("number", number)
            }
        }
        override val intent: Intent? by lazy { Intent(Intent.ACTION_VIEW, Uri.parse(value)) }
    }

    data class CalendarEvent(
        override val value: String,
        override val format: Format,
        val event: VisionBarcode.CalendarEvent,
    ) : Barcode(format, value) {
        override val icon = R.drawable.ic_event_available_black_24dp
        override val key: String = "Event"
        override val details: CharSequence? by lazy {
            buildSpannedString {
                appendKeyValue("summary", event.summary)
                appendKeyValue("description", event.description)
                appendKeyValue("location", event.location)
                appendKeyValue("organizer", event.organizer)
                appendKeyValue("status", event.status)
                appendKeyValue("start", event.start.print())
                appendKeyValue("end", event.end.print())
            }
        }

        private fun CalendarDateTime?.print(): String? {
            if (this == null) return null
            return "year=$year, month=$month, day=$day, hours=$hours, minutes=$minutes, seconds=$seconds, isUtc=$isUtc"
        }

        override val intent: Intent? = null
    }

}
