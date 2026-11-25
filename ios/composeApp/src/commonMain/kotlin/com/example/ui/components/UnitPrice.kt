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

/** Palabras que no dicen qué es el producto (marca, conectores, medidas). */
private val NON_TYPE_WORDS = setOf(
    "bravo", "marca", "exclusiva", "pet", "de", "del", "la", "el", "los", "las", "con", "sin", "y", "en", "para", "a", "al",
    "lb", "lbs", "libra", "libras", "gr", "g", "grs", "gramos", "kg", "kgs", "kilo", "kilos", "oz", "onz", "onzas",
    "l", "lt", "lts", "litro", "litros", "ml", "galon", "galones", "gl", "und", "unidad", "unidades", "rollos",
    "rollo", "pack", "x", "u", "uds"
)

/** Dietas médicas: nunca se sugiere cambiarlas por otra cosa. */
private val THERAPEUTIC_WORDS = setOf(
    "veterinary", "prescription", "vetcare", "veterinario", "veterinaria", "clinico", "dermatologico", "renal",
    "urinary", "hepatic", "diabetic", "hypoallergenic", "gastrointestinal", "hipoalergenico"
)

/** Variantes que hacen a dos productos distintos aunque se llamen parecido (sabor, carne, especie, tipo). */
private val VARIANT_GROUPS = listOf(
    setOf("res", "cerdo", "pollo", "pavo", "pescado", "atun", "salmon", "cordero"),
    setOf("naranja", "chinola", "pina", "manzana", "mango", "uva", "limon", "fresa", "guayaba"),
    setOf("perro", "perros", "dog", "cachorro", "cachorros", "puppy"),
    setOf("gato", "gatos", "gatito", "gatitos", "cat", "kitten"),
    setOf("peces", "pez", "betta"),
    setOf("tortuga", "tortugas"),
    setOf("loro", "loros", "cotorras", "ave", "aves"),
    setOf("hamster", "roedor", "roedores"),
    setOf("entera", "deslactosada", "chocolate", "descremada"),
    setOf("rojas", "negras", "blancas", "pintas"),
    setOf("tinto", "blanco", "rosado", "espumoso"),
    setOf("oliva", "coco", "canola", "vegetal", "maiz", "girasol")
)

/** Palabras que describen el producto, en orden: "Bravo Arroz Premium 10 Lb" → [arroz, premium]. */
fun Product.typeWords(): List<String> =
    normalizeForSearch(name)
        .split(' ', '-', '/', '(', ')', ',', '.', '%', '&', '\'')
        .filter { it.length > 1 && it !in NON_TYPE_WORDS && it.none(Char::isDigit) }

private fun sameKindOfProduct(a: List<String>, b: List<String>): Boolean {
    // Las dos primeras palabras deben coincidir: "arroz premium" con "arroz premium", "pimienta negra" ≠ "pimienta cayena"
    if (a.size < 2 || b.size < 2 || a[0] != b[0] || a[1] != b[1]) return false
    if (a.any { it in THERAPEUTIC_WORDS } || b.any { it in THERAPEUTIC_WORDS }) return false
    return VARIANT_GROUPS.all { g -> a.filter { it in g }.toSet() == b.filter { it in g }.toSet() }
}

/**
 * Busca un producto del MISMO TIPO (pimienta negra con pimienta negra, arroz premium con arroz premium), en la
 * misma categoría y con la misma medida, que salga al menos un 5 % más barato por libra / litro / unidad.
 * Un ahorro mayor al 60 % casi siempre es una comparación injusta, así que se descarta.
 * Devuelve el producto y cuánto se ahorra (en %).
 */
fun Product.cheaperAlternative(catalog: List<Product>): Pair<Product, Int>? {
    val (mine, kind) = unitPrice() ?: return null
    val words = typeWords()
    if (words.size < 2) return null
    return catalog.asSequence()
        .filter { it.sku != sku && it.category == category && sameKindOfProduct(words, it.typeWords()) }
        .mapNotNull { p -> p.unitPrice()?.takeIf { it.second == kind }?.let { p to it.first } }
        .filter { (_, price) -> price < mine * 0.95 && price > mine * 0.40 }
        .minByOrNull { it.second }
        ?.let { (p, price) -> p to ((1 - price / mine) * 100).roundToInt() }
}
