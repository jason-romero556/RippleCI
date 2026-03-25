package com.example.rippleci

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import com.example.rippleci.ui.theme.RippleCITheme
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import androidx.compose.foundation.layout.* //Sameen added these 5 imports (one was redundant)//
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import com.google.maps.android.compose.GoogleMap //Sameen added these 6 imports//
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.MapProperties //Sameen added this import to enable "current location" in map//
import com.google.accompanist.permissions.* //Sameen added these two imports to enable permission asking
import android.Manifest
import com.google.maps.android.compose.MapUiSettings //Sameen added this import to fix an error

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Test Firebase write
        val db = Firebase.firestore
        val testData = hashMapOf(
            "message" to "RippleCI is connected!",
            "timestamp" to System.currentTimeMillis()
        )
        db.collection("test")
            .add(testData)
            .addOnSuccessListener {
                Log.d("Firebase", "Success!")
            }
            .addOnFailureListener { e ->
                Log.d("Firebase", "Failed: ${e.message}")
            }

        setContent {
            RippleCITheme {
                RippleCIApp()
            }
        }
    }
}

@PreviewScreenSizes
@Composable
fun RippleCIApp() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(it.icon, contentDescription = it.label)
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        // This is the "Content" area of your app
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                // Logic to switch screens based on the button clicked
                when (currentDestination) {
                    AppDestinations.MAP -> {
                        // Call your Map function here
                        MyGoogleMap()
                    }
                    AppDestinations.HOME -> {
                        Greeting(name = "Home Screen")
                    }
                }
            }
        }
    }
}



enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    HOME("Home", Icons.Default.Home),
    MAP("Map", Icons.Default.Place)
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    RippleCITheme {
        Greeting("Android")
    }
}
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MyGoogleMap() {
    // 1. Set up the permission state
    val locationPermissionState = rememberPermissionState(
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    // 2. Zoomed-in starting position (15f is street level)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(34.1621, -119.0435), 15f)
    }

    if (locationPermissionState.status.isGranted) {
        // 3. ONLY show the map with location enabled if permission is granted
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = true),
            uiSettings = MapUiSettings(myLocationButtonEnabled = true)
        )
    } else {
        // 4. Show a button to request permission if we don't have it
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
            Text("We need your location to show where you are on the map.")
            Button(onClick = { locationPermissionState.launchPermissionRequest() }) {
                Text("Grant Permission")
            }
        }
    }
}