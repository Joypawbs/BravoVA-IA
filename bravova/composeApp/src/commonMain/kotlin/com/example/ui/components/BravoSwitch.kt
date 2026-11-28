package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Interruptor de la app. En iPhone es el UISwitch nativo (en iOS 26 trae el vidrio líquido
 * de Apple al tocarlo y arrastrarlo); en Android, el Switch de Material.
 */
@Composable
expect fun BravoSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accent: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
)
