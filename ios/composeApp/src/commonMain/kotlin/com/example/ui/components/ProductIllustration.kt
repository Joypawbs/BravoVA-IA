package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.FlutterDash
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Icecream
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.LunchDining
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.RiceBowl
import androidx.compose.material.icons.filled.SetMeal
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Toys
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WineBar
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.model.Product

/*
 * Ilustración de respaldo para productos sin foto verificada.
 *
 * Para qué: cuando una foto no corresponde al producto se quita (mejor un dibujo honesto que una
 * foto de otra cosa). Antes el respaldo era una caja genérica igual para todo; ahora el ícono dice
 * de qué tipo de producto se trata y la etiqueta se corta por palabras, no a mitad de palabra.
 */

/** Ícono que representa el tipo de producto, por su tienda, su categoría y las palabras de su nombre. */
fun productCategoryIcon(product: Product): ImageVector {
    val n = normalizeForSearch(product.name)
    val c = normalizeForSearch(product.category)
    val words = n.split(' ', '-', '/', '(', ')', ',').filter { it.isNotBlank() }
    // Por palabra, no por subcadena: "res" no coincide con "olores". Las claves cortas (≤ 4 letras)
    // deben ser la palabra entera ("cama" no es "camarones"); las largas, el inicio ("habichuela" → "habichuelas").
    fun matches(word: String, k: String) = if (k.length <= 4) word == k else word.startsWith(k)
    fun any(vararg keys: String) = keys.any { k -> if (' ' in k) n.contains(k) else words.any { matches(it, k) } }
    // La primera palabra (sin la marca) dice qué es: "Waffles belgas dorados", "Batata dulce".
    val lead = words.firstOrNull { it != "bravo" && it != "el" }.orEmpty()
    fun leadIs(vararg keys: String) = keys.any { matches(lead, it) }

    if (product.sku.startsWith("ARC")) {
        return when {
            c.contains("aves") || c.contains("exotico") || any("ave", "aves", "loro", "loros", "perico", "pericos", "periquito", "periquitos", "pez", "peces", "pecera", "acuario", "tortuga", "tortugas", "hamster", "conejo", "conejos", "cobaya", "cobayas", "reptil", "reptiles", "roedor", "roedores", "heno", "viruta", "anticloro") -> Icons.Filled.FlutterDash
            c.contains("veterin") || c.contains("farmacia") || c.contains("salud") ||
                any("pipeta", "antiparasit", "desparasit", "suplemento", "gotas", "jeringa", "venda", "probiotico", "meloxicam", "glucosamina", "condroprotector", "termometro", "hemostatico", "cicatrizante", "solucion", "balsamo", "aditivo", "pasta de malta", "gel energetico") -> Icons.Filled.Medication
            any("cama", "manta", "cojin", "hamaca", "cueva", "iglu") -> Icons.Filled.Bed
            c.contains("juguete") || any("juguete", "pelota", "pelotas", "peluche", "kong", "frisbee", "laser", "varita", "tunel", "rascador", "torre", "clicker", "silbato", "lanzador") -> Icons.Filled.Toys
            any("snack", "golosina", "golosinas", "premio", "premios", "galleta", "galletas", "galletitas", "hueso", "huesito", "huesitos", "oreja", "orejas", "palitos", "tiras", "churu", "dentastix", "greenies") -> Icons.Filled.Cookie
            else -> Icons.Filled.Pets
        }
    }
    return when {
        leadIs("pan", "panes", "waffles", "pudin", "bizcocho", "telera", "croissants", "tostadas") -> Icons.Filled.BakeryDining
        leadIs("galletas", "nachos", "palomitas", "papitas", "platanitos", "yuquitas", "pringles", "doritos", "turron", "gomitas", "mani", "pistachos", "dulce", "mermelada") || n.startsWith("crema de avellanas") || (lead == "papas" && any("bolsa", "pringles")) -> Icons.Filled.Cookie
        leadIs("sal", "azucar", "canela", "vainilla", "pimienta", "pimenton", "oregano", "malagueta", "bija", "alcaparras", "cacao", "sazon") -> Icons.Filled.Grain
        leadIs("salsa", "aceite", "vinagre", "wasakaka", "mayonesa", "ketchup", "mostaza") || n.contains("pasta de tomate") -> Icons.Filled.WaterDrop
        leadIs("batata", "sandia", "melon", "papas") -> Icons.Filled.Eco
        leadIs("kipes", "pastelitos", "croquetas", "empanadas", "tequenos", "pizza", "lasana", "gyoza", "pinchos") -> Icons.Filled.LunchDining
        c.contains("cuidado personal") || any("jabon", "champu", "desodorante", "dental", "cepillo", "enjuague", "protector", "aloe", "rasuradora", "afeitar", "micelar", "algodon", "sanitaria", "sanitarias", "alcohol", "corporal", "acondicionador", "gotero", "afeitadora", "afeitadoras") -> Icons.Filled.Spa
        c.contains("limpieza") || any("cloro", "detergente", "suavizante", "desinfectante", "limpiador", "limpiavidrios", "fundas", "bolsas", "aluminio", "plastico", "pastillas", "ambientador", "desengrasante", "esponja", "papel") -> Icons.Filled.CleaningServices
        c.contains("bebe") || any("panal", "panales") -> Icons.Filled.ChildCare
        any("helado", "helados", "paleta", "paletas") -> Icons.Filled.Icecream
        c.contains("pescaderia") || any("pescado", "camaron", "camarones", "langosta", "pulpo", "calamar", "calamares", "cangrejo", "mejillon", "mejillones", "almeja", "almejas", "vieira", "vieiras", "lambi", "chillo", "salmon", "atun", "bacalao", "sardina", "sardinas", "arenque", "carite", "tilapia", "dorado", "mero", "surimi", "tentaculo", "tentaculos") -> Icons.Filled.SetMeal
        any("huevo", "huevos") -> Icons.Filled.Egg
        any("vino", "cerveza", "ron", "sidra", "cava", "sangria") -> Icons.Filled.WineBar
        any("agua", "jugo", "refresco", "malta", "gatorade", "bebida", "hielo", "pulpa") -> Icons.Filled.LocalDrink
        any("aceite", "vinagre", "salsa", "sazon", "caldo", "wasakaka", "pesto", "mayonesa", "ketchup", "mostaza") -> Icons.Filled.WaterDrop
        any("leche", "yogurt", "queso", "quesos", "natilla", "crema", "mantequilla", "margarina") && !any("galleta", "galletas", "pan", "pastelito", "pastelitos", "tequeno", "tequenos", "pizza", "lasana", "empanada", "empanadas") -> Icons.Filled.Kitchen
        any("arroz", "harina", "trigo", "maiz", "avena", "fideo", "fideos", "pasta", "maicena", "habichuela", "habichuelas", "guandul", "guandules", "garbanzo", "garbanzos") && !any("palomitas") -> Icons.Filled.RiceBowl
        any("pan", "brioche", "croissant", "croissants", "telera", "waffle", "waffles", "bizcocho", "pudin", "tres leches", "tartaleta", "donas", "muffins", "mantecadas", "conconete", "jalao", "tostadas", "casabe", "casabitos") -> Icons.Filled.BakeryDining
        c.contains("snacks") || any("galleta", "galletas", "chocolate", "dulce", "gomitas", "turron", "mani", "nueces", "pistachos", "cajuil", "papitas", "pringles", "nachos", "doritos", "palomitas", "platanitos", "yuquitas", "mermelada", "miel", "pasas", "avellanas", "chicharrones", "barras") -> Icons.Filled.Cookie
        c.contains("carne") || any("kipe", "kipes", "pastelito", "pastelitos", "croqueta", "croquetas", "empanada", "empanadas", "tequeno", "tequenos", "nugget", "nuggets", "pizza", "lasana", "gyoza", "pinchos", "pollo", "res", "cerdo", "chivo", "conejo", "mondongo", "higado", "mollejas", "lomo", "lacon", "patitas", "picana", "pavo") -> Icons.Filled.LunchDining
        c.contains("frutas") || any("platano", "yuca", "yautia", "name", "batata", "mapuey", "papa", "papas", "cebolla", "ajo", "aji", "tomate", "apio", "espinacas", "rabanos", "uvas", "sandia", "melon", "chinola", "limon", "fresa", "fresas", "coco") -> Icons.Filled.Eco
        else -> Icons.Filled.LocalGroceryStore
    }
}

/** Etiqueta corta sin cortar palabras: "FRESCO BOCA CHICA" → "FRESCO BOCA". */
fun shortBadge(badge: String, maxChars: Int = 12): String {
    val words = badge.trim().ifBlank { "BRAVO" }.split(Regex("\\s+"))
    val sb = StringBuilder()
    for (w in words) {
        val next = if (sb.isEmpty()) w else "$sb $w"
        if (next.length > maxChars) break
        sb.clear(); sb.append(next)
    }
    return sb.toString().ifBlank { words.first().take(maxChars) }
}
