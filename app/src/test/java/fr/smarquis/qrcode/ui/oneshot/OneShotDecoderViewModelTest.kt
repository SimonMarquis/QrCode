package fr.smarquis.qrcode.ui.oneshot

import android.content.Intent
import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import fr.smarquis.qrcode.model.Barcode
import fr.smarquis.qrcode.model.Mode
import fr.smarquis.qrcode.settings.SettingsRepository
import fr.smarquis.qrcode.ui.oneshot.OneShotResult.NotFound
import fr.smarquis.qrcode.utils.DecoderDispatcher
import fr.smarquis.qrcode.utils.MainCoroutineRule
import fr.smarquis.qrcode.utils.getOrAwaitValue
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.fail

@ExperimentalCoroutinesApi
class OneShotDecoderViewModelTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private fun viewModel(
        decoder: DecoderDispatcher = decoder(),
        settings: SettingsRepository = settings(),
        intent: Intent = intent(),
    ) = OneShotDecoderViewModel(
        decoder = decoder,
        settings = settings,
        intent = intent,
    )

    private fun decoder(barcode: Barcode? = mockk()) = mockk<DecoderDispatcher> {
        coEvery { this@mockk.decode(any()) } returns barcode
    }

    private fun settings(mode: Mode = Mode.AUTO) = mockk<SettingsRepository> {
        every { this@mockk.mode } returns flowOf(mode)
    }

    private fun intent(
        action: String = Intent.ACTION_VIEW,
        data: Uri? = mockk(),
        type: String? = "image/*",
    ): Intent = mockk {
        every { this@mockk.action } returns action
        every { this@mockk.data } returns data
        every { this@mockk.type } returns type
    }

    @Test
    fun `do not attempt to decode invalid inputs`() = mainCoroutineRule.runBlockingTest {
        val decoder = mockk<DecoderDispatcher> {
            coEvery { decode(any()) } coAnswers { fail() }
        }
        assertEquals(NotFound, viewModel(decoder = decoder, intent = intent(action = Intent.ACTION_MAIN)).result.getOrAwaitValue())
        assertEquals(NotFound, viewModel(decoder = decoder, intent = intent(data = null)).result.getOrAwaitValue())
        assertEquals(NotFound, viewModel(decoder = decoder, intent = intent(type = null)).result.getOrAwaitValue())
    }

    @Test
    fun `result Found`() = mainCoroutineRule.runBlockingTest {
        /* Given */
        val barcode = mockk<Barcode>()
        val mode = Mode.values().random()
        val viewModel = viewModel(decoder = decoder(barcode), settings = settings(mode))
        /* Then */
        assertEquals(
            expected = OneShotResult.Found(barcode, mode),
            actual = viewModel.result.getOrAwaitValue(),
        )
    }

    @Test
    fun `result NotFound`() = mainCoroutineRule.runBlockingTest {
        assertEquals(NotFound, viewModel(decoder = decoder(null)).result.getOrAwaitValue())
    }


}
