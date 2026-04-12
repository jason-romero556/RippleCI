package com.example.rippleci.ui.components

import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp

data class HelpfulLink(
    val label: String,
    val url: String,
)

val defaultHelpfulLinks =
    listOf(
        HelpfulLink(label = "WMC", url = "https://www.csuci.edu/wmc/"),
        HelpfulLink(label = "LRC", url = "https://www.csuci.edu/learningresourcecenter/"),
        HelpfulLink(label = "Advising", url = "https://www.csuci.edu/advising/"),
        HelpfulLink(label = "Cafeteria", url = "https://www.csuci.edu/student-life/dining/plans.htm"),
        HelpfulLink(label = "CAPS", url = "https://www.csuci.edu/caps/"),
        HelpfulLink(label = "DASS", url = "https://www.csuci.edu/dass"),
        HelpfulLink(
            label = "Career Center",
            url = "https://www.csuci.edu/careerdevelopment/index.htm",
        ),
        HelpfulLink(
            label = "Study Room Appointments",
            url = "https://library.csuci.edu/services/study-rooms.htm",
        ),
        HelpfulLink(label = "Virtual Tour", url = "https://go.csuci.edu/VirtualTour"),
        HelpfulLink(
            label = "Directions & Parking",
            url = "https://www.csuci.edu/visit-campus/tours/families-individuals/directions-parking.htm",
        ),
    )

val helpfulLinksMenuTitleStartPadding = 72.dp

@Composable
fun HelpfulLinksMenuButton(
    modifier: Modifier = Modifier,
    links: List<HelpfulLink> = defaultHelpfulLinks,
) {
    val uriHandler = LocalUriHandler.current
    var expanded by remember { mutableStateOf(false) }

    IconButton(
        onClick = { expanded = true },
        modifier = modifier,
    ) {
        Icon(
            imageVector = Icons.Default.Menu,
            contentDescription = "Open helpful links",
        )
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
        modifier = Modifier.statusBarsPadding(),
    ) {
        links.forEach { link ->
            DropdownMenuItem(
                text = { Text(link.label) },
                onClick = {
                    expanded = false
                    uriHandler.openUri(link.url)
                },
            )
        }
    }
}
