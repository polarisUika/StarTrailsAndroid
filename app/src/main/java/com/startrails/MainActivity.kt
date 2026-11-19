package com.startrails

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.startrails.core.data.PreferencesRepository
import com.startrails.core.designsystem.theme.StarTrailsTheme
import com.startrails.core.model.UserPreferences
import com.startrails.ui.PrivacyNoticeDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 2001)
        }
        setContent {
            // 初始值为 null，避免偏好尚未读出时用默认值误判为“首次启动”。
            val loadedPreferences by preferencesRepository.preferences
                .collectAsStateWithLifecycle(initialValue = null)
            val preferences = loadedPreferences ?: UserPreferences()
            StarTrailsTheme(preferences) {
                com.startrails.ui.StarTrailsNavHost()
                val loaded = loadedPreferences
                if (loaded != null && !loaded.privacyNoticeAcknowledged) {
                    PrivacyNoticeDialog(
                        onAcknowledge = {
                            lifecycleScope.launch {
                                preferencesRepository.setPrivacyNoticeAcknowledged(true)
                            }
                        },
                    )
                }
            }
        }
    }
}
