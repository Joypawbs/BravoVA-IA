package com.example.ui.components

import org.jetbrains.compose.resources.stringResource

import com.example.resources.*

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.model.Product

/** Quita tildes y mayúsculas: "Plátano" y "platano" se encuentran igual. */
fun normalizeForSearch(text: String): String {
    val sb = StringBuilder(text.length)
    for (c in text.lowercase()) {
        sb.append(
            when (c) {
                'á', 'à', 'ä', 'â' -> 'a'
                'é', 'è', 'ë', 'ê' -> 'e'
                'í', 'ì', 'ï', 'î' -> 'i'
                'ó', 'ò', 'ö', 'ô' -> 'o'
                'ú', 'ù', 'ü', 'û' -> 'u'
                'ñ' -> 'n'
                else -> c
            }
        )
    }
    return sb.toString().trim()
}

/** Coincide si todas las palabras buscadas aparecen en el nombre, la categoría o la etiqueta. */
fun Product.matchesSearch(query: String): Boolean {
    val words = normalizeForSearch(query).split(' ').filter { it.isNotBlank() }
    if (words.isEmpty()) return false
    val haystack = normalizeForSearch("$name $category $badge")
    return words.all { haystack.contains(it) }
}

/** Búsquedas recientes (las últimas 6, sin repetir), compartidas entre pantallas. */
object RecentSearches {
    val items = mutableStateListOf<String>()
    fun add(query: String) {
        val q = query.trim()
        if (q.length < 2) return
        val nq = normalizeForSearch(q)
        // Si la persona hizo una pausa a mitad de palabra, "como" y "como hag" no deben quedar como
        // búsquedas propias: la búsqueda más larga reemplaza a sus prefijos.
        if (items.any { val ni = normalizeForSearch(it); ni != nq && ni.startsWith(nq) }) return
        items.removeAll { nq.startsWith(normalizeForSearch(it)) }
        items.add(0, q)
        while (items.size > 6) items.removeAt(items.lastIndex)
    }
    fun clear() = items.clear()
}

enum class SearchSort(val labelRes: org.jetbrains.compose.resources.StringResource) {
    RELEVANCE(Res.string.sort_relevance), PRICE_LOW(Res.string.sort_price_low), PRICE_HIGH(Res.string.sort_price_high)
}

/** Precio que paga el cliente. */
val Product.effectivePrice: Int get() = price

/** [offerSkus]: productos que hoy están en oferta (las ofertas del día). */
fun List<Product>.applySearchFilters(onlyOffers: Boolean, sort: SearchSort, offerSkus: Set<String> = emptySet()): List<Product> {
    val base = if (onlyOffers) filter { it.sku in offerSkus } else this
    return when (sort) {
        SearchSort.RELEVANCE -> base
        SearchSort.PRICE_LOW -> base.sortedBy { it.effectivePrice }
        SearchSort.PRICE_HIGH -> base.sortedByDescending { it.effectivePrice }
    }
}

/** Fila de filtros de la búsqueda: "En oferta" y el orden por precio. */
@Composable
fun SearchFilterRow(
    onlyOffers: Boolean,
    onToggleOffers: () -> Unit,
    sort: SearchSort,
    onSortChange: (SearchSort) -> Unit,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val colors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = accent.copy(alpha = 0.16f),
        selectedLabelColor = accent,
        selectedLeadingIconColor = accent
    )
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = onlyOffers,
            onClick = onToggleOffers,
            label = { Text(stringResource(Res.string.filter_offers)) },
            leadingIcon = if (onlyOffers) { { Icon(Icons.Filled.Check, null, Modifier.size(16.dp)) } } else null,
            colors = colors
        )
        SearchSort.entries.forEach { s ->
            FilterChip(
                selected = sort == s,
                onClick = { onSortChange(s) },
                label = { Text(stringResource(s.labelRes)) },
                colors = colors
            )
        }
    }
}

/** Sugerencias al tocar el buscador: recientes primero y luego las más buscadas. */
@Composable
fun SearchSuggestionRow(
    popular: List<String>,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val recent = RecentSearches.items.toList()
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()).padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        recent.forEach { q ->
            SuggestionChip(
                onClick = { onPick(q) },
                label = { Text(q) },
                icon = { Icon(Icons.Filled.History, contentDescription = stringResource(Res.string.search_recent_cd), modifier = Modifier.size(16.dp)) }
            )
        }
        popular.filter { p -> recent.none { normalizeForSearch(it) == normalizeForSearch(p) } }.forEach { q ->
            SuggestionChip(
                onClick = { onPick(q) },
                label = { Text(q) },
                icon = { Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = stringResource(Res.string.search_popular_cd), modifier = Modifier.size(16.dp)) }
            )
        }
    }
}

/**
 * Pedido para abrir la búsqueda desde la barra de pestañas: Inicio enfoca su campo
 * y muestra recientes y populares (la "pantalla de búsqueda" de la 2.0).
 */
object SearchFocusRequest {
    var serial by androidx.compose.runtime.mutableStateOf(0)
        private set
    fun request() { serial++ }
}
