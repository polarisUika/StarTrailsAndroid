package com.startrails.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.startrails.core.data.PreferencesRepository
import com.startrails.core.model.AppColorScheme
import com.startrails.core.model.BlendMode
import com.startrails.core.model.OutputFormat
import com.startrails.core.model.ThemeMode
import com.startrails.core.model.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: PreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserPreferences())
    val uiState: StateFlow<UserPreferences> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { preferences.preferences.collect { _uiState.value = it } }
    }

    fun setTheme(value: ThemeMode) = update { preferences.setThemeMode(value) }

    fun setDynamicColor(value: Boolean) = update { preferences.setDynamicColor(value) }

    fun setColorScheme(value: AppColorScheme) = update { preferences.setColorScheme(value) }

    fun setBlendMode(value: BlendMode) = update { preferences.setDefaultBlendMode(value) }

    fun setFormat(value: OutputFormat) = update { preferences.setDefaultFormat(value) }

    fun setQuality(value: Int) = update { preferences.setDefaultQuality(value) }

    fun setRecursive(value: Boolean) = update { preferences.setDefaultRecursive(value) }

    private fun update(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
