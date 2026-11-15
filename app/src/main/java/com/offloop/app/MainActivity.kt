package com.offloop.app

import android.content.ComponentName
import android.content.Intent
import android.database.ContentObserver
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.offloop.app.accessibility.OffloopAccessibilityService
import com.offloop.app.ui.HowItWorksScreen
import com.offloop.app.ui.OffloopHome
import com.offloop.app.ui.theme.OffloopTheme

class MainActivity : ComponentActivity() {

    private val serviceEnabled = mutableStateOf(false)

    private val settingsObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            serviceEnabled.value = isDetectionServiceEnabled()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        serviceEnabled.value = isDetectionServiceEnabled()
        contentResolver.registerContentObserver(
            Settings.Secure.getUriFor(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES),
            false,
            settingsObserver
        )

        setContent {
            OffloopTheme {
                var showHowItWorks by rememberSaveable { mutableStateOf(false) }

                if (showHowItWorks) {
                    BackHandler { showHowItWorks = false }
                    HowItWorksScreen(onBack = { showHowItWorks = false })
                } else {
                    OffloopHome(
                        serviceEnabled = serviceEnabled.value,
                        onOpenAccessibilitySettings = ::openAccessibilitySettings,
                        onOpenHowItWorks = { showHowItWorks = true }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        serviceEnabled.value = isDetectionServiceEnabled()
    }

    override fun onDestroy() {
        contentResolver.unregisterContentObserver(settingsObserver)
        super.onDestroy()
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    /**
     * Reads the user's choice rather than the list of bound services, which is
     * briefly empty while the service restarts after an app update.
     */
    private fun isDetectionServiceEnabled(): Boolean {
        val expected = ComponentName(this, OffloopAccessibilityService::class.java)
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.split(':').any { entry ->
            ComponentName.unflattenFromString(entry) == expected
        }
    }
}
