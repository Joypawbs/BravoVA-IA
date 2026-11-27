package com.example.ui.components

import com.example.ui.theme.Tone

import com.example.platform.*
import coil3.compose.AsyncImage

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.Product
import com.example.ui.theme.BravoRed
import com.example.ui.theme.PlusJakartaSans

private data class SampleScanChip(
    val sku: String,
    val label: String,
    val ean: String
)

private val SAMPLE_BARCODE_CHIPS = listOf(
    SampleScanChip("BRV-001", "Salami ($499)", "7460112299014"),
    SampleScanChip("BRV-017", "Papel Bravo ($379)", "7460112299021"),
    SampleScanChip("BRV-089", "Pan Viga ($99)", "7460112299038"),
    SampleScanChip("ARC-001", "Alimento Arca ($620)", "7460112299045")
)

@Composable
fun BarcodeScannerModal(
    selectedProduct: Product?,
    onSelectProductSku: (String) -> Unit,
    onAddScannedProduct: (Product) -> Unit,
    onViewDetails: (Product) -> Unit,
    onDismiss: () -> Unit
) {
    val context = currentPlatformContext()
    val view = currentPlatformView()

    var torchEnabled by remember { mutableStateOf(false) }
    var useLiveCamera by remember {
        mutableStateOf(
            hasPermission(context, AppPermission.CAMERA)
        )
    }

    val cameraPermissionLauncher = rememberPermissionLauncher { granted ->
        useLiveCamera = granted
    }

    val infiniteTransition = rememberInfiniteTransition(label = "laserBeam")
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(
            animation = tween(1900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laserProgress"
    )

    val currentEan = remember(selectedProduct) {
        SAMPLE_BARCODE_CHIPS.find { it.sku == selectedProduct?.sku }?.ean ?: "7460112299014"
    }

    com.example.ui.components.SuppressNativeGlass() // el vidrio nativo se dibujaría encima del diálogo
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF090D16).copy(alpha = 0.44f))
                .padding(16.dp)
                .testTag("barcode_scanner_modal"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(ScannerPanel)
                    .border(
                        width = 1.2.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.28f),
                                Color.White.copy(alpha = 0.10f)
                            )
                        ),
                        shape = RoundedCornerShape(28.dp)
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp)) {
                        Text(
                            text = "Escáner Óptico BravoVa",
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Apunta al código de barras EAN-13 del producto",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = {
                                val hasPerm = hasPermission(context, AppPermission.CAMERA)
                                if (hasPerm) {
                                    useLiveCamera = !useLiveCamera
                                } else {
                                    cameraPermissionLauncher.launch(AppPermission.CAMERA)
                                }
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    if (useLiveCamera) BravoRed else Color.White.copy(alpha = 0.14f)
                                )
                                .testTag("scanner_camera_toggle")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Alternar cámara real",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                HapticFeedbackManager.onSelectionTick(view)
                                torchEnabled = !torchEnabled
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    if (torchEnabled) Tone.warningBright else Color.White.copy(alpha = 0.14f)
                                )
                                .testTag("scanner_torch_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlashlightOn,
                                contentDescription = "Linterna",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                HapticFeedbackManager.onNavigationClick(view)
                                onDismiss()
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.14f))
                                .testTag("scanner_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar escáner",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Viewport with CameraX or HD Simulated Feed + HUD Reticle + Laser Beam
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(255.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFF020617))
                ) {
                    if (useLiveCamera) {
                        CameraPreview(
                            torchEnabled = torchEnabled,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (selectedProduct != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            ProductArtworkView(
                                product = selectedProduct,
                                modifier = Modifier.size(150.dp)
                            )
                        }
                    }

                    if (torchEnabled) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFFFDE68A).copy(alpha = 0.28f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                    }

                    // HUD Corner Brackets & Animated Red Laser Line
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(28.dp)
                    ) {
                        val w = size.width
                        val h = size.height
                        val cornerLen = 32.dp.toPx()
                        val stroke = 4.dp.toPx()
                        val bracketColor = Color.White.copy(alpha = 0.9f)

                        drawLine(bracketColor, Offset(0f, 0f), Offset(cornerLen, 0f), stroke, StrokeCap.Round)
                        drawLine(bracketColor, Offset(0f, 0f), Offset(0f, cornerLen), stroke, StrokeCap.Round)
                        drawLine(bracketColor, Offset(w, 0f), Offset(w - cornerLen, 0f), stroke, StrokeCap.Round)
                        drawLine(bracketColor, Offset(w, 0f), Offset(w, cornerLen), stroke, StrokeCap.Round)
                        drawLine(bracketColor, Offset(0f, h), Offset(cornerLen, h), stroke, StrokeCap.Round)
                        drawLine(bracketColor, Offset(0f, h), Offset(0f, h - cornerLen), stroke, StrokeCap.Round)
                        drawLine(bracketColor, Offset(w, h), Offset(w - cornerLen, h), stroke, StrokeCap.Round)
                        drawLine(bracketColor, Offset(w, h), Offset(w, h - cornerLen), stroke, StrokeCap.Round)

                        val laserY = h * laserProgress
                        drawLine(
                            color = BravoRed.copy(alpha = 0.35f),
                            start = Offset(8f, laserY),
                            end = Offset(w - 8f, laserY),
                            strokeWidth = 12f,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = Color(0xFFFF2E38),
                            start = Offset(8f, laserY),
                            end = Offset(w - 8f, laserY),
                            strokeWidth = 3.5f,
                            cap = StrokeCap.Round
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color.Black.copy(alpha = 0.72f),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 10.dp)
                    ) {
                        Text(
                            text = "EAN-13 DETECTADO: $currentEan",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFF34D399),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }

                // Quick Barcode Product Selector Chips
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "PRUEBA RÁPIDA DE PRODUCTOS:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.7.sp,
                        color = Color.White.copy(alpha = 0.65f)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SAMPLE_BARCODE_CHIPS.forEach { chip ->
                            val isSelected = selectedProduct?.sku == chip.sku
                            Surface(
                                onClick = {
                                    HapticFeedbackManager.onBarcodeScanned(view)
                                    onSelectProductSku(chip.sku)
                                },
                                shape = RoundedCornerShape(999.dp),
                                color = if (isSelected) BravoRed else Color.White.copy(alpha = 0.1f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) BravoRed else Color.White.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier.testTag("scanner_chip_${chip.sku}")
                            ) {
                                Text(
                                    text = chip.label,
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }
                }

                // Detected Product Card
                if (selectedProduct != null) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = ScannerCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(ScannerThumb)
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    ProductArtworkView(product = selectedProduct)
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = selectedProduct.name,
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = ScannerText,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${selectedProduct.unit} • SKU ${selectedProduct.sku}",
                                        fontSize = 12.sp,
                                        color = ScannerTextMuted
                                    )
                                    Text(
                                        text = "RD$${selectedProduct.price}.00",
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        color = ScannerPrice
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        HapticFeedbackManager.onNavigationClick(view)
                                        onViewDetails(selectedProduct)
                                    },
                                    shape = RoundedCornerShape(999.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.40f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("scanner_details_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = ScannerText,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Detalles",
                                        fontWeight = FontWeight.Bold,
                                        color = ScannerText
                                    )
                                }

                                Button(
                                    onClick = {
                                        HapticFeedbackManager.onAddToCart(view)
                                        onAddScannedProduct(selectedProduct)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = BravoRed),
                                    shape = RoundedCornerShape(999.dp),
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .testTag("scanner_add_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Agregar",
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching {
                torchEnabled = false
            }
        }
    }
}


// Paleta fija del escáner (siempre oscura, sin depender del tema): contraste WCAG AA en todo el texto.
private val ScannerPanel = Color(0xFF15171C)      // panel
private val ScannerCard = Color(0xFF22252C)       // tarjeta del producto
private val ScannerThumb = Color(0xFF2C3038)      // fondo de la miniatura
private val ScannerText = Color(0xFFF5F6F8)       // títulos: 14.2:1 sobre la tarjeta
private val ScannerTextMuted = Color(0xFFB4BAC6)  // datos secundarios: 7.9:1
private val ScannerPrice = Color(0xFFFF5A52)      // precio rojo Bravo aclarado para fondo oscuro: 5.0:1
