package fr.smarquis.qrcode.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.smarquis.qrcode.model.Decoder
import fr.smarquis.qrcode.model.Decoder.MLKit
import fr.smarquis.qrcode.model.Decoder.ZXing
import fr.smarquis.qrcode.settings.DecoderSetting.mapOut
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class DecoderSettingTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun test(expected: Decoder, value: String?, isMlKitEnabled: Boolean = true) = runTest {
        MLKit.isAvailable = isMlKitEnabled
        assertEquals(
            expected = expected,
            actual = flowOf(value).mapOut(context).single(),
        )
    }

    @Test
    fun `mapOut with null`() {
        test(expected = ZXing, value = null, isMlKitEnabled = false)
        test(expected = MLKit, value = null, isMlKitEnabled = true)
    }

    @Test
    fun `mapOut with empty`() {
        test(expected = ZXing, value = "", isMlKitEnabled = false)
        test(expected = MLKit, value = "", isMlKitEnabled = true)
    }

    @Test
    fun `mapOut with unsupported`() {
        test(expected = ZXing, value = "abc", isMlKitEnabled = false)
        test(expected = MLKit, value = "abc", isMlKitEnabled = true)
    }

    @Test
    fun `mapOut with supported values`() = Decoder::class.sealedSubclasses.mapNotNull { it.objectInstance }.forEach {
        test(expected = ZXing, value = it.name(), isMlKitEnabled = false)
        test(expected = it, value = it.name(), isMlKitEnabled = true)
    }
}
