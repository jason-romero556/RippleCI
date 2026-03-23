package com.example.rippleci.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.rippleci.logic.EventManager
import com.example.rippleci.model.Event
import com.example.rippleci.ui.EventItem

@Composable
fun DisplayScreen(navController: NavController){
    var events by remember { mutableStateOf<List<Event>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        events = EventManager.loadEvents()
    }

    Column(modifier = Modifier.fillMaxSize()){

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        )   {
            Button(onClick = {
                navController.popBackStack()
            }) {
                Text("Back")
            }
            Text("Events", style = MaterialTheme.typography.titleLarge)
        }
        LazyColumn(
            modifier = Modifier.padding(16.dp)
        ) {
            items(events) { event ->
                EventItem(event)
            }
        }
    }
}