@file:Suppress("unused")

package fr.smarquis.qrcode.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Dispatchers.Unconfined
import javax.inject.Qualifier
import kotlin.annotation.AnnotationRetention.BINARY

@Qualifier
@Retention(BINARY)
annotation class MainDispatcher

@Qualifier
@Retention(BINARY)
annotation class DefaultDispatcher

@Qualifier
@Retention(BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(BINARY)
annotation class UnconfinedDispatcher


@Module
@InstallIn(SingletonComponent::class)
object CoroutinesModule {

    @Provides
    @MainDispatcher
    fun provideMainDispatcher(): CoroutineDispatcher = Main

    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Default

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = IO

    @Provides
    @UnconfinedDispatcher
    fun provideUnconfinedDispatcher(): CoroutineDispatcher = Unconfined

    @Provides
    fun provideCoroutineDispatcherProvider(): CoroutineDispatcherProvider = object : CoroutineDispatcherProvider {}

}

interface CoroutineDispatcherProvider {
    fun main(): CoroutineDispatcher = Main
    fun default(): CoroutineDispatcher = Default
    fun io(): CoroutineDispatcher = IO
    fun unconfined(): CoroutineDispatcher = Unconfined
}