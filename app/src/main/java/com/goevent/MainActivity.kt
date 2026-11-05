package com.goevent

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.goevent.ui.components.OfflineBanner
import com.goevent.ui.navigation.MainAppNavigation
import com.goevent.ui.theme.GoEventTheme
import com.goevent.ui.viewmodel.SessionViewModel
import com.goevent.ui.viewmodel.appViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val session = appViewModel { SessionViewModel(it) }
            val state by session.state.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val dark = when (state.theme) {
                "Dark" -> true
                "Light" -> false
                else -> systemDark
            }
            // Keep status/navigation bar icons readable when the in-app theme differs from the phone's.
            LaunchedEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark }
                )
            }
            GoEventTheme(state.theme) {
                Column(Modifier.fillMaxSize()) {
                    OfflineBanner()
                    Box(Modifier.fillMaxWidth().weight(1f)) {
                        MainAppNavigation(session)
                    }
                }
            }
        }
    }
}