package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/** En Android el vidrio se dibuja con Compose (LiquidGlass); no hay vista nativa. */
actual fun nativeLiquidGlassAvailable(): Boolean = false

@Composable
actual fun NativeGlassTopBar(spec: GlassTopBarSpec, onBack: () -> Unit, onAction: (Int) -> Unit, modifier: Modifier) {}

@Composable
actual fun NativeGlassDock(
    symbols: List<String>,
    labels: List<String>,
    selectedIndex: Int,
    accent: Color,
    darkMode: Boolean,
    onSelect: (Int) -> Unit,
    modifier: Modifier
) {}
