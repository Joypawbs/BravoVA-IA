package com.example.ui.components

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import com.example.platform.PlatformInfo
import com.example.ui.theme.BravoOrange
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.TextButton
import androidx.compose.material3.IconButton
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * Barra superior estilo Liquid Glass (como la app de Claude en iOS 26):
 * piezas de vidrio separadas que flotan sobre el contenido:
 *   [ ◀ ]   [   Título / subtítulo   ]   [ acciones ]
 * En iOS 26 se dibuja con el vidrio nativo de Apple (SwiftUI .glassEffect);
 * en Android y en iOS anteriores, con el componente LiquidGlass de Compose.
 */

/** Una acción de la barra. [symbol] es el SF Symbol para iOS; [icon] se usa en Compose. */
data class GlassBarAction(
    val symbol: String,
    val icon: ImageVector?,
    val label: String,
    val text: String? = null,
    val badge: String? = null,
    val highlighted: Boolean = false,
    val tint: Color? = null,
    val testTag: String = "",
    val onClick: (() -> Unit)? = null
)

/** Datos de la barra que viajan a la vista nativa. */
data class GlassTopBarSpec(
    val title: String,
    val subtitle: String,
    val subtitleColor: Color,
    val hasBack: Boolean,
    val backLabel: String,
    val actions: List<GlassBarAction>,
    val accent: Color,
    val darkMode: Boolean
)

/** True cuando la plataforma dibuja el vidrio de forma nativa (iOS 26 o superior). */
expect fun nativeLiquidGlassAvailable(): Boolean

@Composable
expect fun NativeGlassTopBar(spec: GlassTopBarSpec, onBack: () -> Unit, onAction: (Int) -> Unit, modifier: Modifier)

@Composable
expect fun NativeGlassDock(
    symbols: List<String>,
    labels: List<String>,
    selectedIndex: Int,
    accent: Color,
    darkMode: Boolean,
    onSelect: (Int) -> Unit,
    modifier: Modifier,
    minimized: Boolean = false,
    onSearch: () -> Unit = {}
)

/** Barra del carrito de vidrio nativo (iOS 26+), teñida con el color de la marca. */
@Composable
expect fun NativeGlassCartBar(count: Int, total: String, accent: Color, darkMode: Boolean, onTap: () -> Unit, modifier: Modifier)

/** Aviso flotante (toast) de vidrio nativo (iOS 26+). */
@Composable
expect fun NativeGlassToast(text: String, symbol: String, accent: Color, darkMode: Boolean, modifier: Modifier)

/**
 * Funciones del sistema que dependen de la plataforma:
 * vista rápida de producto (hoja nativa en iOS), y pedido en vivo
 * (Live Activity / Dynamic Island en iOS, notificación de progreso en Android).
 */
expect object PlatformExtras {
    /** Devuelve false si la plataforma no tiene hoja nativa (se usa la de Compose). */
    fun presentQuickView(
        title: String,
        subtitle: String,
        price: String,
        imageUrl: String,
        isFavorite: Boolean,
        accentArgb: Int,
        onAdd: () -> Unit,
        onFavorite: () -> Unit,
        onShare: () -> Unit
    ): Boolean
    fun startDeliveryActivity(orderId: String, branch: String, stage: String, etaMinutes: Int, progress: Double)
    fun updateDeliveryActivity(stage: String, etaMinutes: Int, progress: Double)
    fun endDeliveryActivity()
}

internal fun Color.argbInt(): Int = toArgb()

@Composable
fun GlassTopBar(
    darkMode: Boolean,
    accent: Color,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    subtitleColor: Color? = null,
    onBack: (() -> Unit)? = null,
    backLabel: String = "Volver",
    backTestTag: String = "",
    actions: List<GlassBarAction> = emptyList()
) {
    val spec = GlassTopBarSpec(
        title = title,
        subtitle = subtitle.orEmpty(),
        subtitleColor = subtitleColor ?: accent,
        hasBack = onBack != null,
        backLabel = backLabel,
        actions = actions,
        accent = accent,
        darkMode = darkMode
    )
    if (nativeLiquidGlassAvailable()) {
        NativeGlassTopBar(
            spec = spec,
            onBack = { onBack?.invoke() },
            onAction = { i -> actions.getOrNull(i)?.onClick?.invoke() },
            // Desvanecido detrás de la barra (como el "scroll edge effect" de iOS 26)
            modifier = modifier
                .fillMaxWidth()
                .background(
                    // Más opaco arriba para que el contenido que pasa por detrás no compita con el título
                    Brush.verticalGradient(
                        0f to MaterialTheme.colorScheme.background.copy(alpha = 0.97f),
                        0.55f to MaterialTheme.colorScheme.background.copy(alpha = 0.86f),
                        1f to MaterialTheme.colorScheme.background.copy(alpha = 0f)
                    )
                )
                .padding(top = 4.dp)
                .height(60.dp)
        )
        return
    }
    if (PlatformInfo.isAndroid) {
        MaterialTopBar(title, subtitle, subtitleColor ?: accent, accent, onBack, backLabel, backTestTag, actions, modifier)
        return
    }
    val onSurface = MaterialTheme.colorScheme.onSurface
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (onBack != null) {
            LiquidGlass(darkMode = darkMode, accent = accent, cornerRadius = 24.dp, elevation = 8.dp,
                modifier = Modifier.size(48.dp).clickable { onBack() }.then(if (backTestTag.isNotEmpty()) Modifier.testTag(backTestTag) else Modifier)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = backLabel, tint = onSurface, modifier = Modifier.align(Alignment.Center).size(21.dp))
            }
        }
        LiquidGlass(darkMode = darkMode, accent = accent, cornerRadius = 24.dp, elevation = 8.dp,
            modifier = Modifier.weight(1f).height(48.dp)) {
            Column(
                modifier = Modifier.align(Alignment.Center).padding(horizontal = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!subtitle.isNullOrEmpty()) {
                    Text(subtitle, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = subtitleColor ?: accent, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        if (actions.isNotEmpty()) {
            val single = actions.size == 1 && actions[0].text == null
            LiquidGlass(darkMode = darkMode, accent = accent, cornerRadius = 24.dp, elevation = 8.dp,
                modifier = Modifier.height(48.dp).then(if (single) Modifier.width(48.dp) else Modifier.widthIn(min = 48.dp))) {
                Row(
                    modifier = Modifier.align(Alignment.Center).padding(horizontal = if (single) 0.dp else 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    actions.forEach { a ->
                        val color = a.tint ?: if (a.highlighted) accent else onSurface
                        Box(
                            modifier = Modifier
                                .height(40.dp)
                                .widthIn(min = 40.dp)
                                .clip(CircleShape)
                                .then(if (a.highlighted) Modifier.background(accent.copy(alpha = 0.18f)) else Modifier)
                                .then(if (a.onClick != null) Modifier.clickable { a.onClick.invoke() } else Modifier)
                                .then(if (a.testTag.isNotEmpty()) Modifier.testTag(a.testTag) else Modifier)
                                .padding(horizontal = if (a.text != null) 8.dp else 0.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (a.icon != null) Icon(a.icon, contentDescription = a.label, tint = color, modifier = Modifier.size(20.dp))
                                if (a.text != null) Text(a.text, fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold, color = color, maxLines = 1)
                            }
                            if (a.badge != null) {
                                Box(
                                    modifier = Modifier.align(Alignment.TopEnd).offset(x = 2.dp, y = 2.dp)
                                        .size(16.dp).clip(CircleShape).background(accent),
                                    contentAlignment = Alignment.Center
                                ) { Text(a.badge, color = Color.White, fontSize = 8.5.sp, fontWeight = FontWeight.Black) }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.width(0.dp))
    }
}

/**
 * Cabecera de Android: barra Material 3 centrada, con la línea degradada bajo el título
 * que usa la app oficial.
 */
@Composable
private fun MaterialTopBar(
    title: String,
    subtitle: String?,
    subtitleColor: Color,
    accent: Color,
    onBack: (() -> Unit)?,
    backLabel: String,
    backTestTag: String,
    actions: List<GlassBarAction>,
    modifier: Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 4.dp)
            .height(64.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(52.dp)) {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = if (backTestTag.isNotEmpty()) Modifier.testTag(backTestTag) else Modifier
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = backLabel)
                }
            }
        }
        val titleAlpha by animateFloatAsState(if (title.isBlank()) 0f else 1f, tween(220), label = "bar_title_alpha")
        var lastTitle by remember { mutableStateOf(title) }
        if (title.isNotBlank()) lastTitle = title
        Column(
            modifier = Modifier.weight(1f).graphicsLayer { alpha = titleAlpha },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = lastTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = subtitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Box(
                    Modifier
                        .padding(top = 4.dp)
                        .width(28.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Brush.horizontalGradient(listOf(BravoOrange, accent)))
                )
            }
        }
        Row(
            modifier = Modifier.widthIn(min = 52.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            actions.forEach { action ->
                val tint = action.tint ?: if (action.highlighted) accent else MaterialTheme.colorScheme.onSurface
                val clickModifier = if (action.testTag.isNotEmpty()) Modifier.testTag(action.testTag) else Modifier
                if (action.text != null) {
                    TextButton(onClick = { action.onClick?.invoke() }, modifier = clickModifier) {
                        action.icon?.let { Icon(it, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp)) }
                        Spacer(Modifier.width(4.dp))
                        Text(action.text, color = tint, style = MaterialTheme.typography.labelLarge)
                    }
                } else {
                    IconButton(onClick = { action.onClick?.invoke() }, modifier = clickModifier) {
                        BadgedBox(badge = {
                            if (!action.badge.isNullOrBlank()) Badge { Text(action.badge) }
                        }) {
                            action.icon?.let { Icon(it, contentDescription = action.label, tint = tint) }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Título grande (estilo iOS) que se muestra arriba de la pantalla antes de hacer scroll.
 * Al bajar, la pantalla cambia a la barra compacta y el título pasa a la píldora.
 */
@Composable
fun LargeTitle(
    title: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Box(
            Modifier
                .padding(top = 2.dp)
                .width(36.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(50))
                .background(Brush.horizontalGradient(listOf(BravoOrange, accent)))
        )
    }
}
