package fr.smarquis.qrcode.utils

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.smarquis.qrcode.model.Barcode
import fr.smarquis.qrcode.model.Decoder
import fr.smarquis.qrcode.model.Decoder.MLKit
import fr.smarquis.qrcode.model.Decoder.ZXing
import fr.smarquis.qrcode.settings.SettingsRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertIs

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class DecoderDispatcherTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun TestScope.newDecoderDispatcher(settings: SettingsRepository = newSettings()) = DecoderDispatcher(context, UnconfinedTestDispatcher(this.testScheduler), settings)

    private fun newSettings(decoder: Decoder = newDecoder()) = mockk<SettingsRepository>(relaxed = true) {
        every { this@mockk.decoder } returns flowOf(decoder)
    }

    private fun newSettings(function: () -> Decoder) = mockk<SettingsRepository>(relaxed = true) {
        every { this@mockk.decoder } answers { flowOf(function()) }
    }

    private inline fun <reified T : Decoder> newDecoder(barcode: Barcode? = null): T = mockk(relaxUnitFun = true) {
        every { decode(context = context, uri = any()) } returns barcode
        every { decode(context = context, imageProxy = any()) } returns barcode
    }

    private inline fun <reified T : Decoder> newDecoder(exception: Exception): T = mockk(relaxUnitFun = true) {
        every { decode(context = context, uri = any()) } throws exception
        every { decode(context = context, imageProxy = any()) } throws exception
    }

    @Test
    fun decodeUnsupportedType() = runTest {
        val dispatcher = newDecoderDispatcher()
        assertIs<UnsupportedOperationException>(dispatcher.decodeInternal(Unit).exceptionOrNull())
    }

    @Test
    fun decodeSuccess() = runTest {
        val barcode = mockk<Barcode>()
        val dispatcher = newDecoderDispatcher(newSettings(newDecoder<Decoder>(barcode)))
        assertEquals(
            expected = barcode,
            actual = dispatcher.decodeInternal(mockk<Uri>()).getOrThrow(),
        )
    }

    private object CustomException : Exception()

    @Test
    fun decodeFailureZXing() = runTest {
        val dispatcher = newDecoderDispatcher(newSettings(newDecoder<ZXing>(CustomException)))
        assertEquals(
            expected = CustomException,
            actual = dispatcher.decodeInternal(mockk<Uri>()).exceptionOrNull(),
        )
    }

    @Test
    fun decodeFailureMLKitFallbackWithZXing() = runTest {
        val barcode = mockk<Barcode>()
        val decoders = mutableListOf(
            newDecoder<MLKit>(CustomException),
            newDecoder<ZXing>(barcode),
        )
        val dispatcher = newDecoderDispatcher(newSettings { decoders.removeFirst() })
        assertEquals(
            expected = barcode,
            actual = dispatcher.decodeInternal(mockk<Uri>()).getOrThrow(),
        )
    }

}
