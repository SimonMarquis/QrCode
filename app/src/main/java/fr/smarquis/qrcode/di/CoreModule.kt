package fr.smarquis.qrcode.di

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object CoreModule {

    @Provides
    fun provideDefaultSharedPreferences(
        @ApplicationContext appContext: Context
    ): SharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext)

}