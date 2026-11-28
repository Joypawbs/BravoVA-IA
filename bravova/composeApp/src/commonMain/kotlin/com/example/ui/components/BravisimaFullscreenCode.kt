package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.platform.KeepScreenAtFullBrightness

/**
 * Tarjeta Bravísima para la caja: fondo blanco, código Code 128 real a todo lo ancho
 * y brillo al máximo mientras está abierta. Tocar en cualquier lado la cierra.
 */
@Composable
fun BravisimaFullscreenCode(cardNumber: String, memberName: String, onDismiss: () -> Unit) {
    val modules = remember(cardNumber) { Code128.modules(cardNumber) }
    com.example.ui.components.SuppressNativeGlass() // el vidrio nativo se dibujaría encima del diálogo
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        KeepScreenAtFullBrightness()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .testTag("bravisima_fullscreen_code")
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(48.dp)
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = Color(0xFF111827))
            }
            Column(
                modifier = Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Tarjeta Bravísima", fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Color(0xFFE31837))
                Text(memberName, fontSize = 15.sp, color = Color(0xFF374151))
                Spacer(Modifier.height(28.dp))
                // Zona en blanco a los lados (10 módulos) que los lectores necesitan
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .semantics { contentDescription = "Código de barras de la tarjeta $cardNumber" }
                ) {
                    val quiet = 10
                    val unit = size.width / (modules.size + quiet * 2)
                    modules.forEachIndexed { i, isBar ->
                        if (isBar) drawRect(Color.Black, Offset((i + quiet) * unit, 0f), Size(unit + 0.5f, size.height))
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    cardNumber,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    letterSpacing = 2.sp,
                    color = Color(0xFF111827)
                )
                Spacer(Modifier.height(28.dp))
                Text(
                    "Muéstralo en la caja para acumular y canjear tu balance.\nToca en cualquier lugar para cerrar.",
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF6B7280)
                )
            }
        }
    }
}
