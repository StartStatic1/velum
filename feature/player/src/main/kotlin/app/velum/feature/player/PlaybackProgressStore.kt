package app.velum.feature.player

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.velum.core.model.PlaybackProgress
import app.velum.core.model.requireTitleId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal val Context.playbackDataStore: DataStore<Preferences> by preferencesDataStore(name = "playback")

fun newPlaybackProgress(context: Context): PlaybackProgress {
    return DataStorePlaybackProgress(context.playbackDataStore)
}

class DataStorePlaybackProgress(
    private val dataStore: DataStore<Preferences>,
) : PlaybackProgress {
    override fun observe(titleId: String): Flow<Long> {
        val key = progressKey(titleId)
        return dataStore.data.map { prefs -> prefs[key] ?: 0L }
    }

    override suspend fun write(titleId: String, positionMs: Long) {
        val key = progressKey(titleId)
        dataStore.edit { prefs ->
            prefs[key] = positionMs.coerceAtLeast(0L)
        }
    }
}

private fun progressKey(titleId: String): Preferences.Key<Long> {
    requireTitleId(titleId)
    return longPreferencesKey("pos_$titleId")
}
