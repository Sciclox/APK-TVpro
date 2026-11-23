package com.tvpro.app

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.tvpro.app.navigation.TvNavHost

object PlayerKeyDispatcher {
    var onPlayerKeyEvent: ((KeyEvent) -> Boolean)? = null
}

class MainActivity : ComponentActivity() {
    private var currentActivityIntent by mutableStateOf<Intent?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        currentActivityIntent = intent
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        currentActivityIntent = intent
        setContent {
            TvNavHost(activityIntent = currentActivityIntent)
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val handler = PlayerKeyDispatcher.onPlayerKeyEvent
        if (handler != null && handler.invoke(event)) {
            return true
        }

        return try {
            super.dispatchKeyEvent(event)
        } catch (e: IllegalStateException) {
            if (e.message?.contains("focus system is invalidated") == true) {
                // Safeguard against Compose focus invalidation race conditions during rapid D-pad navigation
                true
            } else {
                throw e
            }
        }
    }
}
