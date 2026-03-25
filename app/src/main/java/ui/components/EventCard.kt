package com.example.rippleci.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
// You need these two imports for the 'by remember' state to work!
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.text.HtmlCompat
import com.example.rippleci.data.SchoolEvent

@Composable
fun EventCard(event: SchoolEvent) {
    // 1. THE CLICK STATE: This remembers if the card is currently expanded or not
    var isExpanded by remember { mutableStateOf(false) }

    // 2. THE DATE FORMATTER: Split the messy string by commas and rebuild it
    val dateParts = event.dateTimeFormatted.split(",")
    val customDateString = if (dateParts.size >= 3) {
        // Grab the first 3 pieces and format them with your requested colon
        "${dateParts[0].trim()} : ${dateParts[1].trim()}, ${dateParts[2].trim()}"
    } else {
        event.dateTimeFormatted // Fallback just in case the format changes
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }, // MAKE IT CLICKABLE! Toggles true/false
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {

            // --- NEW: Clean up the Title HTML here ---
            val cleanTitle = HtmlCompat.fromHtml(
                event.title,
                HtmlCompat.FROM_HTML_MODE_COMPACT
            ).toString()

            Text(
                text = cleanTitle, // Using the clean title
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Use our custom date string
            Text(
                text = customDateString,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!event.location.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))

                    // --- NEW: Clean up the Location HTML here ---
                    val cleanLocation = HtmlCompat.fromHtml(
                        event.location!!,
                        HtmlCompat.FROM_HTML_MODE_COMPACT
                    ).toString()

                    Text(
                        text = cleanLocation, // Using the clean location
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // 4. THE DESCRIPTION REVEAL: Only show this block if isExpanded is true
            AnimatedVisibility(visible = isExpanded) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                    )

                    // Description HTML cleanup
                    val cleanDescription = HtmlCompat.fromHtml(
                        event.description,
                        HtmlCompat.FROM_HTML_MODE_COMPACT
                    ).toString()

                    Text(
                        text = cleanDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}