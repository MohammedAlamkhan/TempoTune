package com.alam.tempotune.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.alam.tempotune.R
import com.alam.tempotune.data.ThemePreference

@Composable
internal fun LogoMark(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(34.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF4C5FD5), Color(0xFF8057A8)),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 9.dp, vertical = 8.dp),
        ) {
            val barWidth = size.width * 0.18f
            val gap = (size.width - barWidth * 3f) / 2f
            val heights = listOf(0.46f, 0.88f, 0.64f)
            heights.forEachIndexed { index, fraction ->
                val barHeight = size.height * fraction
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(
                        x = index * (barWidth + gap),
                        y = (size.height - barHeight) / 2f,
                    ),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2f),
                )
            }
        }
    }
}

@Composable
internal fun ThemeMenu(
    current: ThemePreference,
    onThemeChange: (ThemePreference) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = themeIcon(current),
                contentDescription = stringResource(R.string.theme),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            ThemePreference.entries.forEach { preference ->
                val selected = preference == current
                DropdownMenuItem(
                    text = {
                        Text(
                            text = themeLabel(preference),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    onClick = {
                        expanded = false
                        onThemeChange(preference)
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = themeIcon(preference),
                            contentDescription = null,
                            tint = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    },
                    trailingIcon = {
                        if (selected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun themeLabel(preference: ThemePreference): String = when (preference) {
    ThemePreference.SYSTEM -> stringResource(R.string.theme_system)
    ThemePreference.LIGHT -> stringResource(R.string.theme_light)
    ThemePreference.DARK -> stringResource(R.string.theme_dark)
}

private fun themeIcon(preference: ThemePreference): ImageVector = when (preference) {
    ThemePreference.SYSTEM -> Icons.Rounded.Contrast
    ThemePreference.LIGHT -> Icons.Rounded.LightMode
    ThemePreference.DARK -> Icons.Rounded.DarkMode
}
