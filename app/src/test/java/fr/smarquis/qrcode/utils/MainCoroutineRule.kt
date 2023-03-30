package fr.smarquis.qrcode.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Sets the main coroutines dispatcher to a [StandardTestDispatcher] for unit testing.
 * A [StandardTestDispatcher] provides control over the execution of coroutines.
 * Alternatively, you can use an [UnconfinedTestDispatcher].
 *
 * Declare it as a JUnit Rule:
 *
 * ```
 * @get:Rule
 * var mainCoroutineRule = MainCoroutineRule()
 * ```
 *
 * Then, use `runTest` to execute your tests.
 */
@ExperimentalCoroutinesApi
class MainCoroutineRule(val dispatcher: TestDispatcher = StandardTestDispatcher()) : TestWatcher() {

    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)

    override fun finished(description: Description) = Dispatchers.resetMain()

}
