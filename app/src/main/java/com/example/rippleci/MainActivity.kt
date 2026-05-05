package com.example.rippleci

import android.os.Build
import android.os.Bundle
import android.Manifest
import androidx.core.app.ActivityCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.rippleci.data.UserPresence
import com.example.rippleci.ui.auth.LoginScreen
import com.example.rippleci.ui.main.MainApp
import com.example.rippleci.ui.theme.RippleCITheme
import com.example.rippleci.ui.theme.ThemeViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var presenceHeartbeatJob: Job? = null
    private val themeViewModel: ThemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                1001
            )
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appTheme by remember { derivedStateOf { themeViewModel.appTheme } }
            val darkTheme = themeViewModel.isDarkTheme ?: isSystemInDarkTheme()

            RippleCITheme(appTheme = appTheme, darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                val auth = Firebase.auth
                var currentUserId by remember { mutableStateOf(auth.currentUser?.uid) }

                if (currentUserId != null) {
                    key(currentUserId) {
                        MainApp(
                            themeViewModel = themeViewModel,
                            onSignOut = {
                                currentUserId?.let { userId ->
                                    UserPresence.update(userId, UserPresence.CLOSED)
                                }
                                auth.signOut()
                                currentUserId = null
                            },
                        )
                    }
                } else {
                    LoginScreen(onLoginSuccess = {
                            currentUserId = auth.currentUser?.uid
                        })
                }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        updatePresence(UserPresence.ONLINE)
        startPresenceHeartbeat()
    }

    override fun onStop() {
        updatePresence(UserPresence.MINIMIZED)
        stopPresenceHeartbeat()
        super.onStop()
    }

    private fun startPresenceHeartbeat() {
        stopPresenceHeartbeat()

        val userId = Firebase.auth.currentUser?.uid ?: return
        presenceHeartbeatJob =
            lifecycleScope.launch {
                while (true) {
                    UserPresence.update(userId, UserPresence.ONLINE)
                    delay(60_000L)
                }
            }
    }

    private fun stopPresenceHeartbeat() {
        presenceHeartbeatJob?.cancel()
        presenceHeartbeatJob = null
    }

    private fun updatePresence(status: String) {
        Firebase.auth.currentUser?.uid?.let { userId ->
            UserPresence.update(userId, status)
        }
    }
}
