package com.example.ui.components

import com.example.model.Product
import kotlin.math.roundToInt

/** Tipo de medida para comparar precios de productos parecidos. */
enum class UnitKind(val label: String) { WEIGHT("lb"), VOLUME("litro"), COUNT("unidad") }

/** Cantidad del producto en su unidad base: libras, litros o unidades. */
data class UnitQuantity(val kind: UnitKind, val amount: Double)

private val unitRegex = Regex("""^\s*([0-9]+(?:[.,][0-9]+)?)\s*([A-Za-zÁÉÍÓÚáéíóúñÑ]+)""")

/** Interpreta textos como "1 Lb", "400 Gr", "1 Galón", "500 Ml", "12 Rollos". Devuelve null si no se puede. */
fun parseUnit(unit: String): UnitQuantity? {
    val m = unitRegex.find(unit) ?: return null
    val n = m.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
    if (n <= 0) return null
    return when (normalizeForSearch(m.groupValues[2])) {
        "lb", "lbs", "libra", "libras" -> UnitQuantity(UnitKind.WEIGHT, n)
        "gr", "g", "grs", "gramos" -> UnitQuantity(UnitKind.WEIGHT, n / 453.592)
        "kg", "kgs", "kilo", "kilos" -> UnitQuantity(UnitKind.WEIGHT, n * 2.20462)
        "oz", "onz", "onzas" -> UnitQuantity(UnitKind.WEIGHT, n / 16.0)
        "l", "lt", "lts", "litro", "litros" -> UnitQuantity(UnitKind.VOLUME, n)
        "ml" -> UnitQuantity(UnitKind.VOLUME, n / 1000.0)
        "galon", "galones", "gl" -> UnitQuantity(UnitKind.VOLUME, n * 3.78541)
        "und", "unidad", "unidades", "rollos", "rollo", "pack" -> UnitQuantity(UnitKind.COUNT, n)
        else -> null
    }
}

/** Precio por unidad base (RD$ por libra, por litro o por unidad). */
fun Product.unitPrice(): Pair<Double, UnitKind>? {
    val q = parseUnit(unit) ?: return null
    // Un solo artículo ("1 Und") no aporta nada que comparar
    if (q.kind == UnitKind.COUNT && q.amount <= 1.0) return null
    return effectivePrice / q.amount to q.kind
}

fun formatUnitPrice(value: Double, kind: UnitKind): String {
    val cents = (value * 100).roundToInt()
    val whole = cents / 100
    val dec = (cents % 100).toString().padStart(2, '0')
    val wholeStr = whole.toString().reversed().chunked(3).joinToString(",").reversed()
    return "RD$$wholeStr.$dec / ${kind.label}"
}

/**
 * Busca en la misma categoría un producto con la misma medida que salga al menos un 5 % más barato
 * por libra / litro / unidad. Devuelve el producto y cuánto se ahorra (en %).
 */
fun Product.cheaperAlternative(catalog: List<Product>): Pair<Product, Int>? {
    val (mine, kind) = unitPrice() ?: return null
    return catalog.asSequence()
        .filter { it.sku != sku && it.category == category }
        .mapNotNull { p -> p.unitPrice()?.takeIf { it.second == kind }?.let { p to it.first } }
        .filter { (_, price) -> price < mine * 0.95 }
        .minByOrNull { it.second }
        ?.let { (p, price) -> p to ((1 - price / mine) * 100).roundToInt() }
}
