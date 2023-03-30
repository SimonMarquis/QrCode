package fr.smarquis.qrcode.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.smarquis.qrcode.model.Mode
import fr.smarquis.qrcode.model.Mode.AUTO
import fr.smarquis.qrcode.settings.ModeSetting.mapOut
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class ModeSettingTest {

    private val context = getApplicationContext<Context>()

    private fun test(expected: Mode, value: String?) = runTest {
        assertEquals(
            expected = expected,
            actual = flowOf(value).mapOut(context).single(),
        )
    }

    @Test
    fun `mapOut with null`() = test(expected = AUTO, value = null)

    @Test
    fun `mapOut with empty`() = test(expected = AUTO, value = "")

    @Test
    fun `mapOut with unsupported`() = test(expected = AUTO, value = "abc")

    @Test
    fun `mapOut with supported values`() = Mode.values().forEach {
        test(expected = it, value = it.name)
    }

}
