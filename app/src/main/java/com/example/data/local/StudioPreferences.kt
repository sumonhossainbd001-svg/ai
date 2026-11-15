package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.AspectRatio
import com.example.domain.model.VideoProviderType
import com.example.domain.model.VideoResolution
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.studioDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "ai_video_studio_prefs"
)

data class StudioSettingsState(
    val creatorName: String = "Creator Studio",
    val defaultProviderId: String = VideoProviderType.RUNWAY.id,
    val autoModeEnabled: Boolean = true,
    val autoFallbackEnabled: Boolean = true,
    val defaultAspectRatioCode: String = AspectRatio.PORTRAIT_9_16.code,
    val defaultResolution: String = VideoResolution.FHD_1080.name,
    val jobNotificationsEnabled: Boolean = true,
    val apiAlertNotificationsEnabled: Boolean = true,
    val themeAccent: String = "violet_blue",
    val localCredits: Int = 120
)

class StudioPreferences(private val context: Context) {

    val settingsFlow: Flow<StudioSettingsState> = context.studioDataStore.data.map { prefs ->
        StudioSettingsState(
            creatorName = prefs[KEY_CREATOR_NAME] ?: "Creator Studio",
            defaultProviderId = prefs[KEY_DEFAULT_PROVIDER] ?: VideoProviderType.RUNWAY.id,
            autoModeEnabled = prefs[KEY_AUTO_MODE] ?: true,
            autoFallbackEnabled = prefs[KEY_AUTO_FALLBACK] ?: true,
            defaultAspectRatioCode = prefs[KEY_DEFAULT_ASPECT] ?: AspectRatio.PORTRAIT_9_16.code,
            defaultResolution = prefs[KEY_DEFAULT_RES] ?: VideoResolution.FHD_1080.name,
            jobNotificationsEnabled = prefs[KEY_JOB_NOTIF] ?: true,
            apiAlertNotificationsEnabled = prefs[KEY_API_NOTIF] ?: true,
            themeAccent = prefs[KEY_THEME_ACCENT] ?: "violet_blue",
            localCredits = prefs[KEY_LOCAL_CREDITS] ?: 120
        )
    }

    suspend fun updateCreatorName(name: String) {
        context.studioDataStore.edit { it[KEY_CREATOR_NAME] = name.trim().ifEmpty { "Creator Studio" } }
    }

    suspend fun setDefaultProvider(providerId: String) {
        context.studioDataStore.edit { it[KEY_DEFAULT_PROVIDER] = providerId }
    }

    suspend fun setAutoMode(enabled: Boolean) {
        context.studioDataStore.edit { it[KEY_AUTO_MODE] = enabled }
    }

    suspend fun setAutoFallback(enabled: Boolean) {
        context.studioDataStore.edit { it[KEY_AUTO_FALLBACK] = enabled }
    }

    suspend fun setDefaultAspectRatio(code: String) {
        context.studioDataStore.edit { it[KEY_DEFAULT_ASPECT] = code }
    }

    suspend fun setDefaultResolution(res: VideoResolution) {
        context.studioDataStore.edit { it[KEY_DEFAULT_RES] = res.name }
    }

    suspend fun setJobNotifications(enabled: Boolean) {
        context.studioDataStore.edit { it[KEY_JOB_NOTIF] = enabled }
    }

    suspend fun setApiNotifications(enabled: Boolean) {
        context.studioDataStore.edit { it[KEY_API_NOTIF] = enabled }
    }

    suspend fun setThemeAccent(accent: String) {
        context.studioDataStore.edit { it[KEY_THEME_ACCENT] = accent }
    }

    suspend fun adjustCredits(delta: Int) {
        context.studioDataStore.edit { prefs ->
            val current = prefs[KEY_LOCAL_CREDITS] ?: 120
            prefs[KEY_LOCAL_CREDITS] = (current + delta).coerceAtLeast(0)
        }
    }

    private companion object {
        val KEY_CREATOR_NAME = stringPreferencesKey("creator_name")
        val KEY_DEFAULT_PROVIDER = stringPreferencesKey("default_provider")
        val KEY_AUTO_MODE = booleanPreferencesKey("auto_mode")
        val KEY_AUTO_FALLBACK = booleanPreferencesKey("auto_fallback")
        val KEY_DEFAULT_ASPECT = stringPreferencesKey("default_aspect")
        val KEY_DEFAULT_RES = stringPreferencesKey("default_res")
        val KEY_JOB_NOTIF = booleanPreferencesKey("job_notif")
        val KEY_API_NOTIF = booleanPreferencesKey("api_notif")
        val KEY_THEME_ACCENT = stringPreferencesKey("theme_accent")
        val KEY_LOCAL_CREDITS = intPreferencesKey("local_credits")
    }
}
