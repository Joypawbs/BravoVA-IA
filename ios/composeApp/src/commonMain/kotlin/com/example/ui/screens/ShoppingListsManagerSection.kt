package com.example.ui.screens

import androidx.compose.material.icons.outlined.Share
import com.example.ui.components.shareOnWhatsApp
import com.example.platform.*

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FrequentProductStat
import com.example.model.Product
import com.example.model.ShoppingList
import com.example.ui.components.AnimatedPurchaseButton
import com.example.ui.components.DiagonalMetadataStrip
import com.example.ui.components.HapticFeedbackManager
import com.example.ui.components.HapticStyle
import com.example.ui.components.TranslucentCard
import com.example.ui.components.ProductArtworkView
import com.example.ui.components.performCrispHaptic
import com.example.ui.theme.BravoAmber
import com.example.ui.theme.PlusJakartaSans

private fun formatListMoney(amount: Int): String {
    return NumberFormat.getNumberInstance(Locale.US).format(amount)
}

@Composable
fun BravIaHistoryAutoListHeroCard(
    ordersCount: Int,
    frequentStats: List<FrequentProductStat>,
    isPopulatingAny: Boolean,
    darkMode: Boolean,
    primaryColor: Color,
    onCreateAiPresetList: (title: String, focusHint: String) -> Unit
) {
    val view = currentPlatformView()

    TranslucentCard(
        darkMode = darkMode,
        cornerRadius = 24.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .testTag("bravia_history_lists_hero")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(primaryColor, BravoAmber)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        DiagonalMetadataStrip(
                            items = listOf("BRAV IA • HISTORIAL INTELIGENTE", "$ordersCount órdenes analizadas"),
                            slashColor = primaryColor,
                            fontSizeSp = 10f
                        )
                        Text(
                            text = "Autocompletado según tus compras",
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Text(
                text = "Brav IA analiza los productos que más repites en tus órdenes pasadas y arma o completa tus listas automáticamente.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (frequentStats.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "TUS MÁS COMPRADOS DETECTADOS",
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.5.sp,
                            color = primaryColor
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        frequentStats.take(8).forEach { stat ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                                ) {
                                    Box(modifier = Modifier.size(24.dp)) {
                                        ProductArtworkView(product = stat.product)
                                    }
                                    Column {
                                        Text(
                                            text = stat.product.name,
                                            fontFamily = PlusJakartaSans,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "Comprado ${stat.purchaseCount}x • RD$${stat.product.price}",
                                            fontFamily = PlusJakartaSans,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 10.sp,
                                            color = primaryColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            val presets = listOf(
                Triple("ai_preset_habitual", "✨ Mi Compra Habitual", "Productos más comprados en todo mi historial de órdenes más básicos de despensa"),
                Triple("ai_preset_desayuno", "🍳 Desayunos y Cena", "Mis productos habituales de desayuno y cena dominicana según mi historial"),
                Triple("ai_preset_hogar", "🧹 Reposición Hogar", "Productos de limpieza y hogar basados en mis compras pasadas"),
                Triple("ai_preset_mascota", "🐾 Mascota El Arca", "Alimentos, snacks y cuidado de mascotas según mi historial")
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "GENERAR LISTA INTELIGENTE CON 1 TOQUE",
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 10.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { (tag, label, hint) ->
                        Surface(
                            onClick = {
                                if (!isPopulatingAny) {
                                    HapticFeedbackManager.onAssistantTrigger(view)
                                    val cleanTitle = label.replace("✨ ", "").replace("🍳 ", "").replace("🧹 ", "").replace("🐾 ", "")
                                    onCreateAiPresetList(cleanTitle, hint)
                                }
                            },
                            shape = RoundedCornerShape(999.dp),
                            color = primaryColor.copy(alpha = 0.11f),
                            border = BorderStroke(1.2.dp, primaryColor.copy(alpha = 0.35f)),
                            modifier = Modifier.testTag(tag)
                        ) {
                            Text(
                                text = label,
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                color = primaryColor,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreateShoppingListFormCard(
    darkMode: Boolean,
    primaryColor: Color,
    isPopulatingAny: Boolean,
    onCreateList: (name: String, autoPopulateWithAi: Boolean) -> Unit
) {
    val view = currentPlatformView()
    var newListName by remember { mutableStateOf("") }
    var autoPopulateWithAi by remember { mutableStateOf(true) }

    TranslucentCard(
        darkMode = darkMode,
        cornerRadius = 22.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Crear nueva lista de compras",
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            OutlinedTextField(
                value = newListName,
                onValueChange = { newListName = it },
                placeholder = { Text("Ej: Compra Semanal, Cena Familiar, Despensa...") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("new_list_name_input")
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(primaryColor.copy(alpha = 0.08f))
                    .clickable {
                        performCrispHaptic(view, HapticStyle.TICK)
                        autoPopulateWithAi = !autoPopulateWithAi
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(17.dp)
                    )
                    Column {
                        Text(
                            text = "Autocompletar con Brav IA",
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Llena la lista automáticamente según tu historial de compras",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = autoPopulateWithAi,
                    onCheckedChange = {
                        performCrispHaptic(view, HapticStyle.TICK)
                        autoPopulateWithAi = it
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = primaryColor
                    ),
                    modifier = Modifier.testTag("new_list_ai_switch")
                )
            }

            AnimatedPurchaseButton(
                text = if (autoPopulateWithAi) {
                    "✨ Crear y Autocompletar con Brav IA"
                } else {
                    "+ Crear Lista Personalizada"
                },
                icon = if (autoPopulateWithAi) Icons.Default.AutoAwesome else Icons.Default.Add,
                primaryColor = primaryColor,
                onClick = {
                    val finalName = newListName.trim().ifEmpty {
                        if (autoPopulateWithAi) "Compra Sugerida por Historial" else "Mi Lista Personalizada"
                    }
                    HapticFeedbackManager.onConfirmAction(view)
                    onCreateList(finalName, autoPopulateWithAi)
                    newListName = ""
                },
                modifier = Modifier.fillMaxWidth(),
                testTag = "create_shopping_list_button"
            )
        }
    }
}

@Composable
fun ManagedShoppingListCard(
    shoppingList: ShoppingList,
    isExpanded: Boolean,
    isPopulatingThisList: Boolean,
    allProducts: List<Product>,
    frequentStats: List<FrequentProductStat>,
    productLookup: (String) -> Product?,
    darkMode: Boolean,
    primaryColor: Color,
    onToggleExpand: () -> Unit,
    onLoadListToCart: () -> Unit,
    onPopulateWithAi: () -> Unit,
    onRenameList: (String) -> Unit,
    onDeleteList: () -> Unit,
    onAddProductToList: (String) -> Unit,
    onRemoveProductFromList: (String) -> Unit,
    onAddSingleToCart: (String) -> Unit,
    onOpenProduct: (String) -> Unit
) {
    val view = currentPlatformView()
    val resolvedProducts = remember(shoppingList.skus) {
        shoppingList.skus.mapNotNull { productLookup(it) }
    }
    val totalPrice = remember(resolvedProducts) {
        resolvedProducts.sumOf { it.price }
    }

    var isEditingTitle by remember { mutableStateOf(false) }
    var editedTitle by remember(shoppingList.name) { mutableStateOf(shoppingList.name) }
    var productSearchQuery by remember { mutableStateOf("") }

    val matchingCatalogProducts = remember(productSearchQuery, shoppingList.skus, allProducts) {
        val q = productSearchQuery.trim().lowercase()
        if (q.isEmpty()) {
            emptyList()
        } else {
            allProducts.filter { p ->
                !shoppingList.skus.contains(p.sku) &&
                    ("${p.name} ${p.category} ${p.badge}".lowercase().contains(q))
            }.take(6)
        }
    }

    val historySuggestionsNotYetInList = remember(frequentStats, shoppingList.skus) {
        frequentStats.filter { !shoppingList.skus.contains(it.product.sku) }.take(6)
    }

    TranslucentCard(
        darkMode = darkMode,
        cornerRadius = 22.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .testTag("shopping_list_card_${shoppingList.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (isEditingTitle) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedTextField(
                                value = editedTitle,
                                onValueChange = { editedTitle = it },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    HapticFeedbackManager.onConfirmAction(view)
                                    onRenameList(editedTitle)
                                    isEditingTitle = false
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Guardar nombre",
                                    tint = Color(0xFF059669)
                                )
                            }
                            IconButton(
                                onClick = {
                                    editedTitle = shoppingList.name
                                    isEditingTitle = false
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancelar",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = shoppingList.name,
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (shoppingList.isAiGenerated) {
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = primaryColor.copy(alpha = 0.14f)
                                ) {
                                    Text(
                                        text = "✨ Brav IA",
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 10.sp,
                                        color = primaryColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    DiagonalMetadataStrip(
                        items = listOf(
                            "${resolvedProducts.size} productos",
                            "RD$${formatListMoney(totalPrice)}.00",
                            shoppingList.updatedAtLabel
                        ),
                        slashColor = primaryColor,
                        fontSizeSp = 11f
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(
                        onClick = {
                            performCrispHaptic(view, HapticStyle.TICK)
                            isEditingTitle = !isEditingTitle
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Renombrar lista",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    // Compartir la lista con la familia por WhatsApp
                    val shareContext = currentPlatformContext()
                    IconButton(
                        onClick = {
                            performCrispHaptic(view, HapticStyle.TICK)
                            val lines = shoppingList.skus.mapNotNull(productLookup).joinToString("\n") { "• ${it.name} (${it.unit})" }
                            shareOnWhatsApp(
                                shareContext,
                                "🛒 *${shoppingList.name}* (lista BravoVa)\n$lines\n\n¿Falta algo? Agrégalo y me avisas."
                            )
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("share_shopping_list_${shoppingList.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "Compartir lista por WhatsApp",
                            tint = primaryColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (shoppingList.id != "l1") {
                        IconButton(
                            onClick = {
                                HapticFeedbackManager.onDecrementOrRemove(view, isFullRemove = true)
                                onDeleteList()
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("delete_shopping_list_${shoppingList.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Eliminar lista",
                                tint = Color(0xFFBE123C),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            if (isPopulatingThisList) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = primaryColor.copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.30f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = primaryColor
                        )
                        Text(
                            text = "Brav IA está analizando tu historial de compras para autocompletar esta lista...",
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = primaryColor
                        )
                    }
                }
            } else if (!shoppingList.aiNote.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = primaryColor.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = shoppingList.aiNote,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Miniaturas de productos cuando la lista está cerrada
            if (!isExpanded && resolvedProducts.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            performCrispHaptic(view, HapticStyle.TICK)
                            onToggleExpand()
                        },
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    resolvedProducts.take(5).forEach { prod ->
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            ProductArtworkView(product = prod)
                        }
                    }
                    if (resolvedProducts.size > 5) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = primaryColor.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "+${resolvedProducts.size - 5}",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp,
                                color = primaryColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        HapticFeedbackManager.onAssistantTrigger(view)
                        onPopulateWithAi()
                    },
                    enabled = !isPopulatingThisList,
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.3.dp, primaryColor),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_populate_list_${shoppingList.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (resolvedProducts.isEmpty()) "Autocompletar IA" else "Completar con IA",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.5.sp,
                        color = primaryColor,
                        maxLines = 1
                    )
                }

                AnimatedPurchaseButton(
                    text = "Cargar (${resolvedProducts.size})",
                    icon = Icons.Default.ShoppingCartCheckout,
                    primaryColor = primaryColor,
                    onClick = {
                        HapticFeedbackManager.onAddToCart(view)
                        onLoadListToCart()
                    },
                    modifier = Modifier.weight(1f),
                    testTag = "load_list_to_cart_${shoppingList.id}"
                )
            }

            Surface(
                onClick = {
                    performCrispHaptic(view, HapticStyle.TICK)
                    onToggleExpand()
                },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("toggle_expand_list_${shoppingList.id}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isExpanded) "Ocultar productos y editor" else "Ver y editar productos de la lista (${resolvedProducts.size})",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                    if (resolvedProducts.isEmpty()) {
                        Text(
                            text = "Esta lista está vacía. Toca \"Autocompletar IA\" arriba para llenarla según tu historial de compras o busca productos abajo.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        resolvedProducts.forEach { prod ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f))
                                    .clickable { onOpenProduct(prod.sku) }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(modifier = Modifier.size(38.dp)) {
                                        ProductArtworkView(product = prod)
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = prod.name,
                                            fontFamily = PlusJakartaSans,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        DiagonalMetadataStrip(
                                            items = listOf("RD$${prod.price}.00", prod.unit, prod.category),
                                            slashColor = primaryColor,
                                            fontSizeSp = 10.5f
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(primaryColor.copy(alpha = 0.14f))
                                            .clickable {
                                                HapticFeedbackManager.onAddToCart(view)
                                                onAddSingleToCart(prod.sku)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Agregar al carrito",
                                            tint = primaryColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFFE4E6))
                                            .clickable {
                                                HapticFeedbackManager.onDecrementOrRemove(view, isFullRemove = true)
                                                onRemoveProductFromList(prod.sku)
                                            }
                                            .testTag("remove_sku_${shoppingList.id}_${prod.sku}"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Quitar de la lista",
                                            tint = Color(0xFFBE123C),
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Agregar rápido desde lo que más compras
                    if (historySuggestionsNotYetInList.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "SUGERIDOS DE TU HISTORIAL DE COMPRAS (+ AGREGAR)",
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.sp,
                                color = primaryColor
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                historySuggestionsNotYetInList.forEach { stat ->
                                    Surface(
                                        onClick = {
                                            HapticFeedbackManager.onAddToCart(view)
                                            onAddProductToList(stat.product.sku)
                                        },
                                        shape = RoundedCornerShape(999.dp),
                                        color = primaryColor.copy(alpha = 0.10f),
                                        border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.30f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = null,
                                                tint = primaryColor,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = "${stat.product.name} (${stat.purchaseCount}x)",
                                                fontFamily = PlusJakartaSans,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = primaryColor
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Buscar cualquier producto del catálogo para agregarlo
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(17.dp)
                        )
                        Box(modifier = Modifier.weight(1f)) {
                            if (productSearchQuery.isEmpty()) {
                                Text(
                                    text = "Buscar producto para agregar a \"${shoppingList.name}\"...",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            BasicTextField(
                                value = productSearchQuery,
                                onValueChange = { productSearchQuery = it },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                ),
                                cursorBrush = SolidColor(primaryColor),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("list_product_search_${shoppingList.id}")
                            )
                        }
                        if (productSearchQuery.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Limpiar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { productSearchQuery = "" }
                            )
                        }
                    }

                    if (matchingCatalogProducts.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            matchingCatalogProducts.forEach { prod ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .border(1.dp, primaryColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                        .clickable {
                                            HapticFeedbackManager.onAddToCart(view)
                                            onAddProductToList(prod.sku)
                                            productSearchQuery = ""
                                        }
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(modifier = Modifier.size(28.dp)) {
                                            ProductArtworkView(product = prod)
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = prod.name,
                                                fontFamily = PlusJakartaSans,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "RD$${prod.price}.00 • ${prod.unit}",
                                                fontSize = 10.5.sp,
                                                color = primaryColor,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = primaryColor
                                    ) {
                                        Text(
                                            text = "+ Agregar",
                                            fontFamily = PlusJakartaSans,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 10.5.sp,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
