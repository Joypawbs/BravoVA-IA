package com.example.ui.components

import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
actual fun BravoSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accent: Color,
    modifier: Modifier,
    enabled: Boolean
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accent),
        modifier = modifier
    )
}
