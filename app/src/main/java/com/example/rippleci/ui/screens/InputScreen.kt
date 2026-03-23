package com.example.rippleci.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.rippleci.logic.EventManager
import kotlinx.coroutines.launch


@Composable
fun InputScreen(navController: NavController){
    var title by remember { mutableStateOf("")}
    var description by remember { mutableStateOf("")}

    val scope = rememberCoroutineScope()


    Column(modifier = Modifier.padding(20.dp)){

        TextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Event title") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Event Description") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = {
            scope.launch {
                EventManager.createEvent(title, description)
                navController.navigate("display")
            }
        }) {
            Text("Create Event")
        }
    }
}