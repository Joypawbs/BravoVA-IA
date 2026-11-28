package com.example

import com.example.data.ArcaCatalogData
import com.example.data.BravoCatalogData
import com.example.data.ProductDescriptionProvider
import com.example.model.BRAV_IA_RECIPES
import com.example.model.CatalogSection
import com.example.model.Product
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Pasa los datos de la app (catálogo, secciones, sucursales, recetas y descripciones) a JSON
 * para la app de iOS en Swift (bravova-ios/BravoVa/Resources/Datos). Así no se copia nada a
 * mano: lo que hay en Kotlin es exactamente lo que queda en Swift.
 * Se escribe en composeApp/build/datos-swift/.
 */
class ExportarDatosParaSwift {

    @Test
    fun exportar() {
        val out = File("build/datos-swift").apply { mkdirs() }
        val bravo = BravoCatalogData.BRAVO_PRODUCTS
        val arca = ArcaCatalogData.ARCA_PRODUCTS

        File(out, "catalogo.json").writeText(obj(
            "bravoProducts" to arr(bravo.map(::product)),
            "arcaProducts" to arr(arca.map(::product)),
            "bravoSections" to arr(BravoCatalogData.BRAVO_SECTIONS.map(::section)),
            "arcaSections" to arr(ArcaCatalogData.ARCA_SECTIONS.map(::section)),
            "homeBravoSections" to arr(BravoCatalogData.HOME_BRAVO_SECTIONS.map(::str)),
            "bravoBranches" to arr(BravoCatalogData.BRAVO_BRANCHES.map(::str))
        ))

        File(out, "descripciones.json").writeText(obj(*(bravo + arca).map { p ->
            val d = ProductDescriptionProvider.describe(p)
            p.sku to obj(
                "summary" to str(d.summary),
                "highlights" to arr(d.highlights.map(::str)),
                "tip" to str(d.usageOrStorageTip)
            )
        }.toTypedArray()))

        File(out, "recetas.json").writeText(arr(BRAV_IA_RECIPES.map { r ->
            obj(
                "id" to str(r.id), "name" to str(r.name), "aliases" to arr(r.aliases.map(::str)),
                "kind" to str(r.kind), "servings" to "${r.servings}",
                "prepMinutes" to "${r.prepMinutes}", "cookMinutes" to "${r.cookMinutes}",
                "ingredients" to arr(r.ingredients.map { i ->
                    obj(
                        "amount" to str(i.amount), "name" to str(i.name), "skus" to arr(i.skus.map(::str)),
                        "pantry" to "${i.pantry}", "optional" to "${i.optional}", "unitsToBuy" to "${i.unitsToBuy}"
                    )
                }),
                "steps" to arr(r.steps.map(::str)),
                "serveWith" to (r.serveWith?.let(::str) ?: "null")
            )
        }))

        // Conteos para comparar con el lado de Swift
        val resumen = "productos Bravo=${bravo.size} Arca=${arca.size} secciones Bravo=${BravoCatalogData.BRAVO_SECTIONS.size} " +
            "Arca=${ArcaCatalogData.ARCA_SECTIONS.size} recetas=${BRAV_IA_RECIPES.size}"
        File(out, "resumen.txt").writeText(resumen + "\n")
        println("DATOS_SWIFT $resumen")
        assertTrue(bravo.isNotEmpty() && arca.isNotEmpty())
    }

    private fun product(p: Product) = obj(
        "sku" to str(p.sku), "name" to str(p.name), "price" to "${p.price}", "category" to str(p.category),
        "unit" to str(p.unit), "isBravo" to "${p.isBravo}", "artType" to str(p.artType),
        "colorHex" to str(p.colorHex), "badge" to str(p.badge), "imageUrl" to (p.imageUrl?.let(::str) ?: "null")
    )

    private fun section(s: CatalogSection) = obj("id" to str(s.id), "name" to str(s.name), "iconType" to str(s.iconType))

    private fun obj(vararg fields: Pair<String, String>) =
        fields.joinToString(",", "{", "}") { (k, v) -> "${str(k)}:$v" }

    private fun arr(items: List<String>) = items.joinToString(",", "[", "]")

    private fun str(s: String): String = buildString {
        append('"')
        s.forEach { c ->
            when {
                c == '"' -> append("\\\"")
                c == '\\' -> append("\\\\")
                c == '\n' -> append("\\n")
                c == '\r' -> append("\\r")
                c == '\t' -> append("\\t")
                c < ' ' -> append("\\u%04x".format(c.code))
                else -> append(c)
            }
        }
        append('"')
    }
}
