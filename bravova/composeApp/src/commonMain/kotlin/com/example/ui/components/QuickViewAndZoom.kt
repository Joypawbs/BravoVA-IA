package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.AddShoppingCart
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.Product
import com.example.ui.theme.formatRd

/** Vista rápida (mantener presionado un producto) cuando no hay hoja nativa: hoja Material 3. */
@Composable
fun QuickViewBottomSheet(
    product: Product,
    isFavorite: Boolean,
    accent: Color,
    onDismiss: () -> Unit,
    onAdd: () -> Unit,
    onFavorite: () -> Unit,
    onShare: () -> Unit
) {
    var favorite by remember { mutableStateOf(isFavorite) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ProductArtworkView(product = product, cornerRadius = 20.dp, modifier = Modifier.size(200.dp))
            Text(product.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(product.unit, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatRd(product.price), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = accent)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalIconButton(onClick = { favorite = !favorite; onFavorite() }) {
                    Icon(if (favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, contentDescription = "Favorito")
                }
                Button(
                    onClick = { onAdd(); onDismiss() },
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    modifier = Modifier.weight(1f).height(52.dp)
                ) {
                    Icon(Icons.Outlined.AddShoppingCart, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Agregar al carrito", fontWeight = FontWeight.SemiBold)
                }
                FilledTonalIconButton(onClick = onShare) {
                    Icon(Icons.Outlined.Share, contentDescription = "Compartir")
                }
            }
        }
    }
}

/** Foto a pantalla completa: pellizca para hacer zoom, doble toque para acercar o alejar. */
@Composable
fun ZoomableProductImage(product: Product, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        val shownScale by animateFloatAsState(scale, label = "zoom")
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.92f))) {
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            offset = if (scale == 1f) Offset.Zero else offset + pan
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(onDoubleTap = {
                            if (scale > 1f) { scale = 1f; offset = Offset.Zero } else scale = 2.5f
                        })
                    },
                contentAlignment = Alignment.Center
            ) {
                ProductArtworkView(
                    product = product,
                    cornerRadius = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp)
                        .graphicsLayer {
                            scaleX = shownScale; scaleY = shownScale
                            translationX = offset.x; translationY = offset.y
                        }
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(12.dp)
                    .clip(CircleShape).background(Color.White.copy(alpha = 0.16f))
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = Color.White)
            }
            Text(
                "Pellizca para hacer zoom",
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp)
            )
        }
    }
}
