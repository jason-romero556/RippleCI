package com.example.rippleci

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
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
import androidx.core.app.ActivityCompat
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

    // These are mutable so onNewIntent can update them and Compose recomposes
    private val navigateTo = mutableStateOf<String?>(null)
    private val conversationId = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                1001
            )
        }

        // Read extras from the initial launch intent
        handleIntent(intent)

        setContent {
            val appTheme by remember { derivedStateOf { themeViewModel.appTheme } }
            val systemDark = isSystemInDarkTheme()
            val darkTheme = themeViewModel.isDarkTheme ?: systemDark

            RippleCITheme(appTheme = appTheme, darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var user by remember { mutableStateOf(Firebase.auth.currentUser) }

                    if (user != null) {
                        MainApp(
                            themeViewModel = themeViewModel,
                            onSignOut = {
                                Firebase.auth.signOut()
                                user = null
                            },
                            navigateTo = navigateTo.value,
                            conversationId = conversationId.value
                        )
                    } else {
                        LoginScreen(
                            onLoginSuccess = {
                                user = Firebase.auth.currentUser
                            }
                        )
                    }
                }
            }
        }
    }

    // Called when app is already running and a notification is tapped
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        navigateTo.value = intent?.getStringExtra("navigate_to")
        conversationId.value = intent?.getStringExtra("conversationId")
    }

    override fun onStart() {
        super.onStart()
        val uid = Firebase.auth.currentUser?.uid ?: return
        presenceHeartbeatJob = lifecycleScope.launch {
            while (true) {
                UserPresence.update(uid, UserPresence.ONLINE)
                delay(30_000)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        presenceHeartbeatJob?.cancel()
        val uid = Firebase.auth.currentUser?.uid ?: return
        lifecycleScope.launch {
            UserPresence.update(uid, UserPresence.CLOSED)
        }
    }
}