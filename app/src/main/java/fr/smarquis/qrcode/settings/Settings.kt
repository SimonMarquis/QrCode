package fr.smarquis.qrcode.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

sealed class Settings<In, Out>(
    private val key: Preferences.Key<In>,
) {

    abstract fun Flow<In?>.mapOut(context: Context): Flow<Out>
    abstract fun Out.mapIn(context: Context): In

    fun get(context: Context): Flow<Out> = context.settingsDataStore.data.catch {
        when (it) {
            is IOException -> emit(emptyPreferences())
            else -> throw it
        }
    }.map { it[key] }.mapOut(context)

    suspend fun set(context: Context, out: Out) = context.settingsDataStore.edit { it[key] = out.mapIn(context) }

}
