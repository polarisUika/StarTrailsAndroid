package com.startrails.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.startrails.core.model.AppColorScheme
import com.startrails.core.model.BlendMode
import com.startrails.core.model.OutputFormat
import com.startrails.core.model.QualityLevel
import com.startrails.core.model.ThemeMode
import com.startrails.core.model.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "startrails_settings")

@Singleton
class PreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val BLEND_MODE = stringPreferencesKey("default_blend_mode")
        val OUTPUT_FORMAT = stringPreferencesKey("default_output_format")
        val QUALITY = intPreferencesKey("default_quality")
        val RECURSIVE = booleanPreferencesKey("default_recursive")
        val PRIVACY_NOTICE = booleanPreferencesKey("privacy_notice_acknowledged")
        val COLOR_SCHEME = stringPreferencesKey("color_scheme")
    }

    val preferences: Flow<UserPreferences> = context.settingsDataStore.data
        .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
        .map { prefs ->
            UserPreferences(
                themeMode = ThemeMode.fromKey(prefs[Keys.THEME]),
                dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: true,
                defaultBlendMode = BlendMode.fromKey(prefs[Keys.BLEND_MODE]),
                defaultFormat = OutputFormat.fromKey(prefs[Keys.OUTPUT_FORMAT]),
                defaultQuality = QualityLevel.fromQuality(
                    prefs[Keys.QUALITY] ?: QualityLevel.HIGHEST.percent,
                ).percent,
                defaultRecursive = prefs[Keys.RECURSIVE] ?: false,
                privacyNoticeAcknowledged = prefs[Keys.PRIVACY_NOTICE] ?: false,
                colorScheme = AppColorScheme.fromKey(prefs[Keys.COLOR_SCHEME]),
            )
        }

    suspend fun setThemeMode(value: ThemeMode) = edit { it[Keys.THEME] = value.key }

    suspend fun setDynamicColor(value: Boolean) = edit { it[Keys.DYNAMIC_COLOR] = value }

    suspend fun setDefaultBlendMode(value: BlendMode) = edit { it[Keys.BLEND_MODE] = value.key }

    suspend fun setDefaultFormat(value: OutputFormat) = edit { it[Keys.OUTPUT_FORMAT] = value.key }

    suspend fun setDefaultQuality(value: Int) = edit {
        it[Keys.QUALITY] = QualityLevel.fromQuality(value).percent
    }

    suspend fun setDefaultRecursive(value: Boolean) = edit { it[Keys.RECURSIVE] = value }

    suspend fun setPrivacyNoticeAcknowledged(value: Boolean) =
        edit { it[Keys.PRIVACY_NOTICE] = value }

    suspend fun setColorScheme(value: AppColorScheme) = edit { it[Keys.COLOR_SCHEME] = value.key }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.settingsDataStore.edit(block)
    }
}
