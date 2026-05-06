package com.example.rippleci.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class VisibilityOption(
    val value: String,
    val label: String,
    val description: String,
)

val ProfileVisibilityOptions =
    listOf(
        VisibilityOption("public", "Public", "Anyone can view this."),
        VisibilityOption("friends", "Friends", "Only friends can view this."),
        VisibilityOption("private", "Private", "Only you can view this."),
    )

val EventVisibilityOptions =
    listOf(
        VisibilityOption("public", "Public", "Anyone can view this event."),
        VisibilityOption("friends", "Friends", "Only friends can view this event."),
        VisibilityOption("attendees", "Attendees", "Only attendees can view this event."),
        VisibilityOption("private", "Private", "Only you can view this event."),
    )

val GroupVisibilityOptions =
    listOf(
        VisibilityOption("public", "Public", "Anyone can find this group."),
        VisibilityOption("friends", "Friends", "Only friends can find this group."),
        VisibilityOption("members", "Members", "Only members can view this group."),
        VisibilityOption("private", "Private", "Only admins can view this group."),
    )

@Composable
fun VisibilitySelector(
    title: String,
    selectedValue: String,
    options: List<VisibilityOption>,
    onValueChange: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleMedium)

        options.forEach { option ->
            TextButton(
                onClick = { onValueChange(option.value) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = selectedValue == option.value,
                        onClick = { onValueChange(option.value) },
                    )

                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text(option.label)
                        Text(
                            text = option.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }
        }
    }
}
