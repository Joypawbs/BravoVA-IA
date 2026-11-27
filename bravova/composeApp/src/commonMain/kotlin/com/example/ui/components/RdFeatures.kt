package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PhoneIphone
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.ShoppingBasket
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CheckoutExtras
import com.example.model.DiasporaCurrency
import com.example.model.Product
import com.example.model.RD_BANKS
import com.example.model.RD_DELIVERY_SLOTS
import com.example.model.RdPaymentOption
import com.example.model.displayLabel
import com.example.model.displayDetail
import com.example.model.SEASONAL_COMBOS
import com.example.model.SeasonalCombo
import com.example.platform.PlatformActions
import com.example.platform.PlatformContext
import com.example.platform.URLEncoder
import com.example.platform.sharedPrefs
import com.example.ui.theme.BravoCtaGradient
import com.example.ui.theme.formatRd
import kotlin.math.ceil

/* ------------------------------------------------------------------ */
/*  WhatsApp                                                           */
/* ------------------------------------------------------------------ */

/** Abre WhatsApp con el texto listo para enviar (el usuario elige a quién). */
fun shareOnWhatsApp(context: PlatformContext, text: String): Boolean {
    val encoded = URLEncoder.encode(text).replace("+", "%20")
    return PlatformActions.openUrl(context, "https://wa.me/?text=$encoded")
}

/* ------------------------------------------------------------------ */
/*  Preferencias que se recuerdan entre pedidos                        */
/* ------------------------------------------------------------------ */

object RdPrefs {
    private val prefs by lazy { sharedPrefs("bravova_rd") }

    fun loadExtras(): CheckoutExtras = CheckoutExtras(
        payment = prefs.getString("payment", null)
            ?.let { name -> RdPaymentOption.entries.firstOrNull { it.name == name } }
            ?: RdPaymentOption.CARD,
        transferBank = prefs.getString("transfer_bank", null),
        addressReference = prefs.getString("address_reference", "").orEmpty(),
        ncfType = prefs.getString("ncf_type", "B02") ?: "B02",
        rnc = prefs.getString("rnc", "").orEmpty()
    )

    fun saveExtras(extras: CheckoutExtras) {
        prefs.edit()
            .putString("payment", extras.payment.name)
            .putString("transfer_bank", extras.transferBank)
            .putString("address_reference", extras.addressReference)
            .putString("ncf_type", extras.ncfType)
            .putString("rnc", extras.rnc)
            .apply()
    }
}

/* ------------------------------------------------------------------ */
/*  Checkout: pago, horario, referencia, propina y diáspora            */
/* ------------------------------------------------------------------ */

private fun RdPaymentOption.icon(): ImageVector = when (this) {
    RdPaymentOption.WALLET -> Icons.Outlined.AccountBalanceWallet
    RdPaymentOption.CARD -> Icons.Outlined.CreditCard
    RdPaymentOption.CASH_ON_DELIVERY -> Icons.Outlined.Payments
    RdPaymentOption.POS_ON_DELIVERY -> Icons.Outlined.PointOfSale
    RdPaymentOption.TRANSFER -> Icons.Outlined.AccountBalance
    RdPaymentOption.TPAGO -> Icons.Outlined.PhoneIphone
    RdPaymentOption.AZUL -> Icons.Outlined.CreditCard
}

/** Propina: redondea el total a la siguiente cifra de RD$50. */
fun roundUpTip(totalRd: Double): Int {
    val rounded = ceil(totalRd / 50.0) * 50.0
    val tip = (rounded - totalRd).toInt()
    return if (tip < 10) tip + 50 else tip
}

@Composable
fun RdCheckoutOptionsCard(
    extras: CheckoutExtras,
    onChange: (CheckoutExtras) -> Unit,
    baseTotalRd: Double,
    isDelivery: Boolean,
    darkMode: Boolean,
    accent: Color,
    modifier: Modifier = Modifier
) {
    TranslucentCard(darkMode = darkMode, modifier = modifier.testTag("rd_checkout_options")) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionLabel("¿Cómo pagas?")
            Column(Modifier.clip(RoundedCornerShape(14.dp)).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))) {
                RdPaymentOption.entries.forEachIndexed { index, option ->
                    val selected = option == extras.payment
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (selected) accent.copy(alpha = if (darkMode) 0.16f else 0.06f) else Color.Transparent)
                            .clickable { onChange(extras.copy(payment = option)) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("pay_${option.name.lowercase()}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(option.icon(), contentDescription = null, tint = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(option.displayLabel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(option.displayDetail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        RadioButton(
                            selected = selected,
                            onClick = { onChange(extras.copy(payment = option)) },
                            colors = RadioButtonDefaults.colors(selectedColor = accent)
                        )
                    }
                    if (index < RdPaymentOption.entries.lastIndex) {
                        Box(Modifier.fillMaxWidth().height(0.5.dp).background(MaterialTheme.colorScheme.outlineVariant))
                    }
                }
            }

            AnimatedVisibility(
                visible = extras.payment == RdPaymentOption.TRANSFER,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoiceChips(RD_BANKS, extras.transferBank, accent) { onChange(extras.copy(transferBank = it)) }
                    Text(
                        "Cuenta corriente Bravo S.A. • ${extras.transferBank ?: "elige tu banco"} (demo)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    PillButton(
                        text = if (extras.transferProofAttached) "Comprobante adjuntado" else "Adjuntar comprobante",
                        icon = if (extras.transferProofAttached) Icons.Outlined.CheckCircle else Icons.Outlined.AttachFile,
                        accent = accent,
                        filled = extras.transferProofAttached
                    ) { onChange(extras.copy(transferProofAttached = !extras.transferProofAttached)) }
                }
            }
            AnimatedVisibility(visible = extras.payment == RdPaymentOption.TPAGO) {
                Text(
                    "Al confirmar te llegará la solicitud de pago en tPago para aprobarla (simulado).",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isDelivery) {
                SectionLabel("¿Cuándo lo entregamos?", Icons.Outlined.Schedule)
                ChoiceChips(RD_DELIVERY_SLOTS, extras.deliverySlot, accent) { onChange(extras.copy(deliverySlot = it)) }

                SectionLabel("Referencia para el repartidor")
                OutlinedTextField(
                    value = extras.addressReference,
                    onValueChange = { onChange(extras.copy(addressReference = it.take(160))) },
                    placeholder = { Text("Ej.: frente al colmado, casa verde, 2do piso", style = MaterialTheme.typography.bodySmall) },
                    textStyle = MaterialTheme.typography.bodyMedium,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accent),
                    modifier = Modifier.fillMaxWidth().testTag("address_reference_input")
                )

                val tip = remember(baseTotalRd) { roundUpTip(baseTotalRd) }
                SwitchRow(
                    title = "Redondear para la propina",
                    subtitle = "+${formatRd(tip)} para el repartidor",
                    checked = extras.tipRd > 0,
                    accent = accent
                ) { on -> onChange(extras.copy(tipRd = if (on) tip else 0)) }
            }

            SwitchRow(
                title = "Lo paga un familiar desde el exterior",
                subtitle = "Pago en dólares o euros, entrega aquí en RD",
                checked = extras.diasporaCurrency != null,
                accent = accent,
                icon = Icons.Outlined.Public
            ) { on -> onChange(extras.copy(diasporaCurrency = if (on) DiasporaCurrency.USD else null)) }
            AnimatedVisibility(
                visible = extras.diasporaCurrency != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = extras.diasporaRecipient.orEmpty(),
                        onValueChange = { onChange(extras.copy(diasporaRecipient = it.take(60))) },
                        label = { Text("¿Quién recibe en RD?") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = extras.diasporaPhone.orEmpty(),
                        onValueChange = { onChange(extras.copy(diasporaPhone = it.filter { c -> c.isDigit() || c == '-' }.take(14))) },
                        label = { Text("Teléfono de quien recibe") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    val currency = extras.diasporaCurrency ?: DiasporaCurrency.USD
                    ChoiceChips(DiasporaCurrency.entries.map { it.code }, currency.code, accent) { code ->
                        onChange(extras.copy(diasporaCurrency = DiasporaCurrency.entries.first { it.code == code }))
                    }
                    val foreign = (baseTotalRd + extras.tipRd) / currency.rdPerUnit
                    Text(
                        "Total a pagar: ${currency.symbol}${(foreign * 100).toLong() / 100.0} " +
                            "(referencia ${currency.symbol}1 = RD$${currency.rdPerUnit.toInt()}, demo)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = accent
                    )
                }
            }

            AnimatedVisibility(visible = extras.ncfType == "B01") {
                OutlinedTextField(
                    value = extras.rnc,
                    onValueChange = { onChange(extras.copy(rnc = it.filter { c -> c.isDigit() || c == '-' }.take(13))) },
                    label = { Text("RNC o cédula para el crédito fiscal") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().testTag("rnc_input")
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String, icon: ImageVector? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        icon?.let { Icon(it, contentDescription = null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)) }
        Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ChoiceChips(options: List<String>, selected: String?, accent: Color, onSelect: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(options) { option ->
            val isSelected = option == selected
            Text(
                text = option,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) accent else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (isSelected) accent.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(1.dp, if (isSelected) accent else Color.Transparent, RoundedCornerShape(50))
                    .clickable { onSelect(option) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    accent: Color,
    icon: ImageVector? = null,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        icon?.let {
            Icon(it, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
        }
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = accent, checkedThumbColor = Color.White)
        )
    }
}

@Composable
fun PillButton(
    text: String,
    icon: ImageVector,
    accent: Color,
    filled: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (filled) accent else accent.copy(alpha = 0.10f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = if (filled) Color.White else accent, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, color = if (filled) Color.White else accent, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}

/* ------------------------------------------------------------------ */
/*  Combos de temporada (Inicio)                                       */
/* ------------------------------------------------------------------ */

/** Busca en el catálogo un producto por cada palabra clave del combo. */
fun resolveCombo(combo: SeasonalCombo, catalog: List<Product>): List<Product> {
    val used = mutableSetOf<String>()
    return combo.keywords.mapNotNull { keyword ->
        catalog.firstOrNull { it.sku !in used && it.name.lowercase().contains(keyword) }?.also { used += it.sku }
    }
}

@Composable
fun SeasonalCombosRow(
    catalog: List<Product>,
    accent: Color,
    onAddCombo: (SeasonalCombo, List<Product>) -> Unit
) {
    val resolved = remember(catalog) {
        SEASONAL_COMBOS.map { it to resolveCombo(it, catalog) }.filter { it.second.size >= 3 }
    }
    if (resolved.isEmpty()) return
    Column(Modifier.fillMaxWidth().padding(top = 14.dp)) {
        Text(
            "Combos de temporada",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 14.dp)
        )
        Text(
            "Todo lo que necesitas con un toque",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 14.dp)
        )
        Spacer(Modifier.height(10.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(resolved, key = { it.first.id }) { (combo, products) ->
                val total = products.sumOf { it.price }
                Column(
                    modifier = Modifier
                        .width(230.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(0.8.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                        .padding(12.dp)
                        .testTag("combo_${combo.id}")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(34.dp).clip(CircleShape).background(Brush.linearGradient(BravoCtaGradient)),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Outlined.ShoppingBasket, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp)) }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(combo.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(combo.subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                        products.take(5).forEach { p ->
                            ProductArtworkView(product = p, cornerRadius = 10.dp, modifier = Modifier.size(38.dp).border(1.5.dp, MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp)))
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    PillButton(
                        text = "Agregar ${products.size} • ${formatRd(total)}",
                        icon = Icons.Outlined.ShoppingBasket,
                        accent = accent,
                        filled = true,
                        modifier = Modifier.fillMaxWidth()
                    ) { onAddCombo(combo, products) }
                }
            }
        }
    }
}

/** Texto del pedido para compartir por WhatsApp. */
fun orderWhatsAppText(orderId: String, status: String, total: Double, branch: String, extras: CheckoutExtras?): String = buildString {
    append("🛒 *Pedido BravoVa #$orderId*\n")
    append("Estado: $status\n")
    append("Sucursal: $branch\n")
    append("Total: ${formatRd(total)}\n")
    extras?.let {
        append("Pago: ${it.paymentSummary}\n")
        if (it.deliverySlot.isNotBlank()) append("Entrega: ${it.deliverySlot}\n")
        if (it.addressReference.isNotBlank()) append("Referencia: ${it.addressReference}\n")
    }
    append("\nSíguelo en la app BravoVa.")
}

/** "Avísame cuando baje de precio" en el detalle del producto. */
@Composable
fun PriceAlertRow(active: Boolean, accent: Color, darkMode: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    TranslucentCard(darkMode = darkMode, modifier = modifier.testTag("price_alert_row")) {
        Box(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            SwitchRow(
                title = "Avísame cuando baje de precio",
                subtitle = if (active) "Te llegará un aviso cuando entre en oferta" else "Recibe un aviso si entra en las ofertas del día",
                checked = active,
                accent = accent,
                icon = Icons.Outlined.NotificationsActive
            ) { onToggle() }
        }
    }
}
