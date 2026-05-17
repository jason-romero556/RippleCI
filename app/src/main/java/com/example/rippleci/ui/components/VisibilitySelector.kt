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

val GroupEventVisibilityOptions =
    listOf(
        VisibilityOption("public", "Public", "Anyone can view this event."),
        VisibilityOption("friends", "Friends", "Only friends can view this event."),
        VisibilityOption("members", "Members", "Only group members can view this event."),
        VisibilityOption("attendees", "Attendees", "Only invited or attending people can view this event."),
        VisibilityOption("private", "Private", "Only group leadership can view this event."),
    )

val GroupVisibilityOptions =
    listOf(
        VisibilityOption("public", "Public", "Anyone can find this group."),
        VisibilityOption("friends", "Friends", "Only friends can find this group."),
        VisibilityOption("members", "Members", "Only members can view this group."),
        VisibilityOption("private", "Private", "Only admins can view this group."),
    )

val PastGroupEventsVisibilityOptions =
    listOf(
        VisibilityOption("public", "Public", "Anyone who can open the group can view past events."),
        VisibilityOption("members", "Members", "Only current members can view past events."),
        VisibilityOption("admins", "Admins", "Only the owner and admins can view past events."),
        VisibilityOption("owner", "Owner Only", "Only the owner can view past events."),
    )

val BulletinBoardVisibilityOptions =
    listOf(
        VisibilityOption("public", "Public", "Anyone who can open the group can view the bulletin board."),
        VisibilityOption("members", "Members", "Only current group members can view the bulletin board."),
        VisibilityOption("moderators", "Moderators", "Only the owner and bulletin moderators can view the bulletin board."),
        VisibilityOption("owner", "Owner Only", "Only the owner can view the bulletin board."),
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
