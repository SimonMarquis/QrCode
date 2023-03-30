package fr.smarquis.qrcode.ui.multi

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import fr.smarquis.qrcode.model.Barcode
import fr.smarquis.qrcode.model.Decoder
import fr.smarquis.qrcode.model.Decoder.MLKit
import fr.smarquis.qrcode.model.Mode
import fr.smarquis.qrcode.model.Mode.AUTO
import fr.smarquis.qrcode.model.Mode.MANUAL
import fr.smarquis.qrcode.model.Theme
import fr.smarquis.qrcode.model.Theme.DARK
import fr.smarquis.qrcode.model.Theme.SYSTEM
import fr.smarquis.qrcode.settings.SettingsRepository
import fr.smarquis.qrcode.ui.multi.Event.Finish
import fr.smarquis.qrcode.ui.multi.Event.Recreate
import fr.smarquis.qrcode.ui.multi.MultiResult.Empty
import fr.smarquis.qrcode.ui.multi.MultiResult.Found
import fr.smarquis.qrcode.utils.DecoderDispatcher
import fr.smarquis.qrcode.utils.MainCoroutineRule
import fr.smarquis.qrcode.utils.getOrAwaitValue
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@ExperimentalCoroutinesApi
class MultiDecoderViewModelTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule(UnconfinedTestDispatcher())

    private fun viewModel(
        decoder: DecoderDispatcher = decoder(),
        settings: SettingsRepository = settings(),
    ) = MultiDecoderViewModel(
        decoder = decoder,
        settings = settings,
    )

    private fun decoder(barcode: Barcode? = mockk()) = mockk<DecoderDispatcher> {
        coEvery { this@mockk.decode(any()) } returns barcode
    }

    private fun settings(mode: Mode = AUTO, theme: Theme = SYSTEM, decoder: Decoder = MLKit) = mockk<SettingsRepository>(relaxed = true) {
        every { this@mockk.mode } returns flowOf(mode)
        every { this@mockk.theme } returns flowOf(theme)
        every { this@mockk.decoder } returns flowOf(decoder)
    }

    @Test
    fun `MultiResult Found`() = runTest {
        /* Given */
        val barcode = mockk<Barcode>()
        val mode = Mode.values().random()
        val viewModel = viewModel(decoder = decoder(barcode), settings = settings(mode))
        /* When */
        viewModel.processImage(mockk())
        /* Then */
        assertEquals(
            expected = Found(barcode, mode),
            actual = viewModel.results.getOrAwaitValue(),
        )
    }

    @Test
    fun `MultiResult Empty after reset`() = runTest {
        /* Given */
        val viewModel = viewModel()
        /* When */
        viewModel.reset()
        /* Then */
        assertEquals(Empty, viewModel.results.getOrAwaitValue())
    }

    @Test
    fun `no MultiResult when no barcode`() = runTest {
        /* Given */
        val viewModel = viewModel(decoder = decoder(null))
        /* When */
        viewModel.processImage(mockk())
        /* Then */
        assertNull(viewModel.results.value)
    }

    @Test
    fun `onBackPressed trigger Finish in Mode AUTO`() = runTest {
        /* Given */
        val viewModel = viewModel(settings = settings(AUTO))
        /* When */
        viewModel.onBackPressed()
        /* Then */
        assertEquals(Finish, viewModel.events.getOrAwaitValue())
    }

    @Test
    fun `onBackPressed trigger Empty in Mode MANUAL with no result`() = runTest {
        /* Given */
        val viewModel = viewModel(settings = settings(MANUAL))
        /* When */
        viewModel.processImage(mockk())
        viewModel.onBackPressed()
        /* Then */
        assertEquals(Empty, viewModel.results.getOrAwaitValue())
    }

    @Test
    fun `onBackPressed trigger Finish in Mode MANUAL when no result`() = runTest {
        /* Given */
        val viewModel = viewModel(settings = settings(MANUAL))
        /* When */
        viewModel.onBackPressed()
        /* Then */
        assertEquals(Finish, viewModel.events.getOrAwaitValue())
    }

    //region Settings
    @Test
    fun `Updating Mode trigger Reset`() = runTest {
        /* Given */
        val viewModel = viewModel(settings = settings(mode = AUTO))
        /* When */
        viewModel.mode(MANUAL)
        /* Then */
        assertEquals(Empty, viewModel.results.getOrAwaitValue())
    }

    @Test
    fun `Updating Decoder trigger Reset`() = runTest {
        /* Given */
        val viewModel = viewModel(settings = settings(decoder = MLKit))
        /* When */
        viewModel.decoder(Decoder.ZXing)
        /* Then */
        assertEquals(Empty, viewModel.results.getOrAwaitValue())
    }

    @Test
    fun `Updating Theme trigger Recreate`() = runTest {
        /* Given */
        val viewModel = viewModel(settings = settings(theme = SYSTEM))
        /* When */
        viewModel.theme(DARK)
        /* Then */
        assertEquals(Recreate, viewModel.events.getOrAwaitValue())
    }
    //endregion


}
