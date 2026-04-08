package com.example.rippleci

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.example.rippleci.ui.auth.LoginScreen
import com.example.rippleci.ui.main.MainApp
import com.example.rippleci.ui.theme.RippleCITheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RippleCITheme {
                val auth = Firebase.auth
                var isLoggedIn by remember { mutableStateOf(auth.currentUser != null) }
                var currentUserId by remember { mutableStateOf(auth.currentUser?.uid) }

                if (isLoggedIn) {
                    MainApp(onSignOut = {
                        auth.signOut()
                        isLoggedIn = false
                    })
                    if (currentUserId != null) {
                        key(currentUserId) {
                            MainApp(onSignOut = {
                                auth.signOut()
                                currentUserId = null
                            })
                        }
                    } else {
                        LoginScreen(onLoginSuccess = { isLoggedIn = true })
                        LoginScreen(onLoginSuccess = {
                            currentUserId = auth.currentUser?.uid
                        })
                    }
                }
            }
        }
    }
}
