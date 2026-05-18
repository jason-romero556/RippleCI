package com.example.rippleci.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A custom button component that follows the style favored in the Map Screen.
 * It features a 9.dp corner radius, a fixed 38.dp height, and a subtle border.
 */
@Composable
fun RippleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color? = null,
    contentColor: Color? = null,
    borderColor: Color? = null,
    content: @Composable RowScope.() -> Unit
) {
    val actualContainerColor = containerColor ?: MaterialTheme.colorScheme.primaryContainer
    val actualContentColor = contentColor ?: MaterialTheme.colorScheme.onPrimaryContainer
    val actualBorderColor = borderColor ?: MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(38.dp),
        shape = MaterialTheme.shapes.small,
        color = if (enabled) actualContainerColor else actualContainerColor.copy(alpha = 0.38f),
        contentColor = if (enabled) actualContentColor else actualContentColor.copy(alpha = 0.38f),
        border = BorderStroke(
            width = 1.dp,
            color = if (enabled) actualBorderColor else actualBorderColor.copy(alpha = 0.12f)
        ),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

/**
 * A convenience version of RippleButton for simple text and optional icon.
 */
@Composable
fun RippleButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    containerColor: Color? = null,
    contentColor: Color? = null,
    borderColor: Color? = null,
    fontWeight: FontWeight = FontWeight.Bold
) {
    RippleButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        containerColor = containerColor,
        contentColor = contentColor,
        borderColor = borderColor
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = fontWeight,
            maxLines = 1
        )
    }
}

/**
 * An outlined version of the Ripple style button.
 */
@Composable
fun RippleOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    RippleButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.75f),
        content = content
    )
}

/**
 * A convenience version of RippleOutlinedButton for simple text and optional icon.
 */
@Composable
fun RippleOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    fontWeight: FontWeight = FontWeight.Medium
) {
    RippleOutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = fontWeight,
            maxLines = 1
        )
    }
}
