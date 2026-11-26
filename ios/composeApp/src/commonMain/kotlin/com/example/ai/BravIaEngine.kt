package com.example.ai

import com.example.model.BRAV_IA_RECIPES
import com.example.model.Product
import com.example.model.Recipe
import com.example.model.findRecipeById
import com.example.model.findRecipeForQuery
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject

/*
 * Motor de anclaje de Brav IA.
 *
 * Para qué: el modelo de lenguaje es bueno conversando pero inventa números. Aquí la regla es
 * simple: el modelo solo puede (1) conversar, (2) elegir productos por SKU de una lista que le
 * damos y (3) elegir una receta del recetario verificado por su id. Todo lo que es un dato
 * (precio, total, presupuesto, ingredientes, pasos) lo pone la app desde el catálogo.
 *
 * Cómo: el modelo responde en JSON. Este archivo arma el mensaje, lee el JSON aunque venga
 * con basura alrededor, descarta SKUs que no existen, borra del texto cualquier precio o caloría
 * que el modelo haya escrito y calcula el total de verdad. No depende de Android ni de iOS, así
 * que se prueba con pruebas unitarias normales (commonTest).
 */

/** Hechos de la tienda que la app ya muestra en su sección de ayuda. La IA solo puede citar estos. */
object StoreFacts {
    const val HOURS = "Lunes a sábado de 7:00 AM a 10:00 PM. Los domingos está cerrado."
    const val BRAVISIMA = "La Tarjeta Bravísima devuelve RD$1 por cada RD$100 de compra (1 %) y se canjea en el carrito o en caja."
    const val NCF = "Se emiten comprobantes fiscales B01 (crédito fiscal, con RNC) y B02 (consumidor final)."
    const val SUPPORT = "Los reportes de productos dañados o vencidos se registran y los revisa el equipo de servicio al cliente. Brav IA no aprueba reembolsos, reposiciones ni notas de crédito."

    val all: List<String> = listOf(HOURS, BRAVISIMA, NCF, SUPPORT)
}

object BravIaText {
    private val accents = mapOf(
        'á' to 'a', 'é' to 'e', 'í' to 'i', 'ó' to 'o', 'ú' to 'u', 'ü' to 'u', 'ñ' to 'n',
        'à' to 'a', 'è' to 'e', 'ì' to 'i', 'ò' to 'o', 'ù' to 'u'
    )

    /** Minúsculas, sin tildes y sin signos: "¿Cómo hago Mangú?" → "como hago mangu". */
    fun normalize(input: String): String {
        val sb = StringBuilder(input.length)
        for (c in input.lowercase()) {
            val mapped = accents[c] ?: c
            sb.append(if (mapped.isLetterOrDigit()) mapped else ' ')
        }
        return sb.toString().trim().replace(Regex("\\s+"), " ")
    }

    private val stopwords = setOf(
        "que", "como", "para", "con", "una", "uno", "unos", "unas", "los", "las", "del", "por",
        "hago", "hacer", "quiero", "dame", "tienes", "tienen", "necesito", "puedo", "receta",
        "recetas", "algo", "mas", "muy", "este", "esta", "esto", "mis", "tus", "sus", "hay",
        "precio", "cuanto", "cuesta", "favor", "hola", "buenas", "brav", "bravo", "comprar"
    )

    fun keywords(normalized: String): List<String> =
        normalized.split(' ').filter { it.length >= 3 && it !in stopwords }.distinct()
}

/** Un producto que Brav IA recomienda, ya validado contra el catálogo. */
data class GroundedItem(val sku: String, val qty: Int, val reason: String = "")

data class SuggestionDraft(val subject: String, val summary: String)

/** Lo que el modelo devolvió, ya leído pero todavía sin validar. */
data class RawAiReply(
    val answer: String,
    val products: List<GroundedItem>,
    val recipeId: String?,
    val question: String?,
    val suggestion: SuggestionDraft?
)

/** Respuesta final: todo lo que hay aquí se puede mostrar sin miedo a datos inventados. */
data class GroundedReply(
    val text: String,
    val items: List<GroundedItem>,
    val recipe: Recipe?,
    val suggestion: SuggestionDraft?,
    val total: Int,
    val budgetExceededBy: Int?,
    /** Veces que se multiplica la receta para las personas pedidas (1 = porciones base). */
    val recipeMultiplier: Int = 1
)

data class ChatTurn(val fromUser: Boolean, val text: String)

data class PromptContext(
    val query: String,
    val storeName: String,
    val branch: String,
    val etaMinutes: Int?,
    val candidates: List<Product>,
    val matchedRecipe: Recipe?,
    val history: List<ChatTurn>,
    val cartSummary: String?,
    val budget: Int?,
    val frequentProducts: List<String>,
    val hasImage: Boolean
)

object BravIaEngine {

    const val MAX_QTY = 20

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /** Instrucción de sistema: corta, sin datos inventados que el modelo pueda repetir. */
    val SYSTEM_INSTRUCTION: String = """
        Eres Brav IA, la asistente de compras de Supermercados Bravo y El Arca Petshop en República Dominicana.
        Hablas español dominicano neutro, con calidez y en pocas palabras.

        RESPONDE SIEMPRE con un único objeto JSON, sin texto antes ni después:
        {"respuesta": "texto para el cliente", "productos": [{"sku": "BRV-000", "cantidad": 1, "motivo": "para qué sirve"}], "receta_id": null, "pregunta": null, "sugerencia": null}

        REGLAS (no tienen excepciones):
        1. Solo puedes recomendar SKUs que aparezcan en PRODUCTOS DISPONIBLES. Si algo no está en la lista, dilo con honestidad: "no lo tenemos en el catálogo". Nunca inventes productos, marcas ni presentaciones.
        2. En "respuesta" NUNCA escribas precios, montos, totales, descuentos, ahorros, calorías ni datos nutricionales. La app los calcula desde el catálogo y los muestra en tarjetas.
        3. Recetas: si el mensaje trae RECETA VERIFICADA, pon su id en "receta_id" y en "respuesta" escribe solo una introducción de una o dos frases; la app muestra los ingredientes y los pasos verificados. Si el plato no está en el recetario, no inventes cantidades, tiempos, temperaturas ni pasos: dilo y sugiere recetas del RECETARIO con nombre parecido. Puedes recomendar productos para ese plato.
        4. Alergias, condiciones médicas, embarazo o salud de mascotas: no afirmes que un producto es seguro ni libre de alérgenos. Pide revisar la etiqueta y consultar al médico o al veterinario.
        5. Datos de la tienda: usa solo HECHOS DE LA TIENDA. No prometas reembolsos, reposiciones, créditos, descuentos ni tiempos de entrega distintos a los que te damos.
        6. Imágenes: describe solo lo que se ve con claridad. Si una fecha, número o NCF no se lee, dilo.
        7. Temas ajenos a compras, cocina, hogar o mascotas (política, deportes, tareas, programación): rechaza con amabilidad en una frase y ofrece ayuda con la compra.
        8. "cantidad" es el número de unidades del producto que conviene comprar (de 1 a 20).
        9. Si falta un dato clave (para cuántas personas, qué mascota, qué presupuesto), haz una sola pregunta en "pregunta".
        10. Si el cliente da una sugerencia o reporta un problema, llena "sugerencia" con {"asunto": "...", "resumen": "..."}.
        11. No escribas códigos SKU dentro de "respuesta". Usa **negritas** para nombres de productos. Máximo 110 palabras.

        HECHOS DE LA TIENDA:
        ${StoreFacts.all.joinToString("\n        ") { "- $it" }}
    """.trimIndent()

    /* -------------------------------------------------------------- */
    /*  1. Qué productos le enseñamos al modelo                         */
    /* -------------------------------------------------------------- */

    /**
     * Elige hasta [limit] productos de la tienda activa que tienen que ver con la consulta.
     * Por qué no todo el catálogo: 750 líneas diluyen la atención del modelo y cuestan tiempo.
     */
    fun selectCandidates(
        query: String,
        storeProducts: List<Product>,
        recipe: Recipe?,
        extraSkus: List<String> = emptyList(),
        limit: Int = 60
    ): List<Product> {
        val keywords = BravIaText.keywords(BravIaText.normalize(query))
        val recipeSkus = recipe?.ingredients?.flatMap { it.skus }?.toSet().orEmpty()
        val extra = extraSkus.toSet()

        val scored = storeProducts.map { p ->
            val haystack = BravIaText.normalize(p.name + " " + p.category)
            var score = 0
            for (k in keywords) {
                val stem = if (k.length > 4) k.take(k.length - 1) else k // "platanos" ≈ "platano"
                if (haystack.contains(stem)) score += if (BravIaText.normalize(p.name).contains(stem)) 10 else 4
            }
            if (p.sku in recipeSkus) score += 100
            if (p.sku in extra) score += 6
            if (score > 0 && p.isBravo) score += 1
            p to score
        }
        val picked = scored.filter { it.second > 0 }.sortedByDescending { it.second }.map { it.first }.take(limit)
        if (picked.size >= 12) return picked

        // Pocas coincidencias: completamos con productos de las mismas categorías y luego marca Bravo.
        val categories = picked.map { it.category }.toSet()
        val filler = storeProducts.asSequence()
            .filter { it !in picked }
            .sortedWith(compareByDescending<Product> { it.category in categories }.thenByDescending { it.isBravo }.thenBy { it.price })
            .take((limit / 2).coerceAtLeast(12) - picked.size)
            .toList()
        return picked + filler
    }

    /* -------------------------------------------------------------- */
    /*  2. El mensaje que recibe el modelo                              */
    /* -------------------------------------------------------------- */

    fun buildPrompt(ctx: PromptContext): String = buildString {
        appendLine("TIENDA: ${ctx.storeName} — sucursal ${ctx.branch}.")
        ctx.etaMinutes?.let { appendLine("TIEMPO ESTIMADO DE ENTREGA HOY: unos $it minutos (calculado por la app).") }
        ctx.budget?.let { appendLine("PRESUPUESTO DEL CLIENTE: RD$$it (la app comprueba la suma; elige productos sensatos para ese monto).") }
        ctx.cartSummary?.let { appendLine("CARRITO ACTUAL: $it") }
        if (ctx.frequentProducts.isNotEmpty()) appendLine("COMPRA HABITUALMENTE: ${ctx.frequentProducts.joinToString(", ")}")
        if (ctx.hasImage) appendLine("EL CLIENTE ADJUNTÓ UNA IMAGEN.")
        appendLine()

        if (ctx.history.isNotEmpty()) {
            appendLine("CONVERSACIÓN RECIENTE:")
            ctx.history.forEach { t -> appendLine((if (t.fromUser) "Cliente: " else "Brav IA: ") + t.text.take(400).replace('\n', ' ')) }
            appendLine()
        }

        if (ctx.matchedRecipe != null) {
            appendLine("RECETA VERIFICADA: id=\"${ctx.matchedRecipe.id}\" (${ctx.matchedRecipe.name}). Usa este id en receta_id.")
        }
        appendLine("RECETARIO (ids válidos): " + BRAV_IA_RECIPES.joinToString(", ") { "${it.id}=${it.name}" })
        appendLine()

        appendLine("PRODUCTOS DISPONIBLES (SKU | nombre | presentación):")
        ctx.candidates.forEach { p -> appendLine("${p.sku} | ${p.name} | ${p.unit}") }
        appendLine()
        append("MENSAJE DEL CLIENTE: ").append(ctx.query)
    }

    /* -------------------------------------------------------------- */
    /*  3. Leer la respuesta                                           */
    /* -------------------------------------------------------------- */

    /** Lee el JSON del modelo aunque venga dentro de ```json … ``` o con texto alrededor. */
    fun parseReply(raw: String): RawAiReply? {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        val obj = try {
            json.parseToJsonElement(raw.substring(start, end + 1)).jsonObject
        } catch (_: Exception) {
            return null
        }
        val answer = obj.str("respuesta") ?: obj.str("response") ?: return null
        val products = (obj["productos"] as? JsonArray).orEmpty().mapNotNull { el ->
            val o = el as? JsonObject ?: return@mapNotNull null
            val sku = o.str("sku")?.trim()?.uppercase() ?: return@mapNotNull null
            val qtyPrim = o["cantidad"] as? JsonPrimitive
            val qty = qtyPrim?.intOrNull ?: qtyPrim?.doubleOrNull?.toInt() ?: qtyPrim?.contentOrNull?.trim()?.toIntOrNull() ?: 1
            GroundedItem(sku, qty, o.str("motivo").orEmpty())
        }
        val suggestion = (obj["sugerencia"] as? JsonObject)?.let { s ->
            val subject = s.str("asunto")
            val summary = s.str("resumen")
            if (subject.isNullOrBlank() && summary.isNullOrBlank()) null
            else SuggestionDraft(subject.orEmpty().ifBlank { "Sugerencia de cliente" }, summary.orEmpty())
        }
        return RawAiReply(
            answer = answer,
            products = products,
            recipeId = obj.str("receta_id")?.takeIf { it.isNotBlank() && it != "null" },
            question = obj.str("pregunta")?.takeIf { it.isNotBlank() && it != "null" },
            suggestion = suggestion
        )
    }

    private fun JsonObject.str(key: String): String? {
        val el = this[key] ?: return null
        if (el is JsonNull) return null
        return (el as? JsonPrimitive)?.contentOrNull
    }

    /**
     * Mientras el modelo escribe el JSON, saca lo que va del campo "respuesta" para mostrarlo
     * en vivo. Devuelve null si todavía no empezó.
     */
    fun streamingAnswer(partial: String): String? {
        val key = Regex("\"respuesta\"\\s*:\\s*\"").find(partial) ?: return null
        val sb = StringBuilder()
        var i = key.range.last + 1
        while (i < partial.length) {
            val c = partial[i]
            if (c == '\\') {
                if (i + 1 >= partial.length) break
                when (val n = partial[i + 1]) {
                    'n' -> sb.append('\n')
                    't' -> sb.append(' ')
                    '"' -> sb.append('"')
                    '\\' -> sb.append('\\')
                    '/' -> sb.append('/')
                    'u' -> {
                        if (i + 5 >= partial.length) break
                        partial.substring(i + 2, i + 6).toIntOrNull(16)?.let { sb.append(it.toChar()) }
                        i += 4
                    }
                    else -> sb.append(n)
                }
                i += 2
                continue
            }
            if (c == '"') break
            sb.append(c)
            i++
        }
        return sb.toString()
    }

    /* -------------------------------------------------------------- */
    /*  4. Limpiar el texto                                             */
    /* -------------------------------------------------------------- */

    private val skuRegex = Regex("""\s*[\[(]?\s*(?:SKU\s*:?\s*)?\b(?:BRV|ARC)-\d{3}\b\s*[\])]?""", RegexOption.IGNORE_CASE)
    private val moneyRegex = Regex("""\s*[—–\-:]?\s*\(?\s*(?:RD\s?\$|\$|DOP\s?)\s*\d[\d.,]*(?:\s*(?:pesos|c/u|cada uno))?\s*\)?""", RegexOption.IGNORE_CASE)
    private val pesosRegex = Regex("""\s*\(?\b\d[\d.,]*\s*pesos\b\)?""", RegexOption.IGNORE_CASE)
    private val kcalRegex = Regex("""\s*[—–\-:]?\s*\(?\s*~?\d[\d.,]*\s*(?:kcal|calor[ií]as|cal\b)\s*\)?""", RegexOption.IGNORE_CASE)
    private val dataLineRegex = Regex("""^\W*(?:subtotal|total|ahorro|margen|retorno|precio|costo|cashback)\b""", RegexOption.IGNORE_CASE)

    /**
     * Quita del texto del modelo los SKUs, montos en pesos y calorías. Así, aunque el modelo
     * desobedezca la regla 2, en pantalla nunca aparece un precio que no salga del catálogo.
     */
    fun sanitize(text: String): String {
        val lines = text.replace("\r", "").lines().mapNotNull { original ->
            val hadData = moneyRegex.containsMatchIn(original) || kcalRegex.containsMatchIn(original) || pesosRegex.containsMatchIn(original)
            var line = original
                .replace(skuRegex, "")
                .replace(moneyRegex, "")
                .replace(pesosRegex, "")
                .replace(kcalRegex, "")
                .replace(Regex("\\(\\s*\\)"), "")
                .replace(Regex("\\s*[—–]\\s*(?=[—–,.;]|$)"), "")
                .replace(Regex(" {2,}"), " ")
                .trimEnd()
            val visible = line.replace(Regex("[*_•#>\\-\\s:]"), "")
            when {
                hadData && dataLineRegex.containsMatchIn(line) -> null // "Total: RD$900" ya no tiene sentido
                hadData && visible.length < 3 -> null
                else -> line
            }
        }
        return lines.joinToString("\n").replace(Regex("\n{3,}"), "\n\n").trim()
    }

    /* -------------------------------------------------------------- */
    /*  5. Validar contra el catálogo                                   */
    /* -------------------------------------------------------------- */

    /**
     * Convierte la respuesta del modelo en datos seguros:
     * - descarta SKUs que no existen o que son de la otra tienda,
     * - limita las cantidades a 1..20 y une duplicados,
     * - si hay receta (la que pidió la persona o la que eligió el modelo), sus ingredientes mandan,
     * - calcula el total con precios reales y avisa si pasa del presupuesto (sin borrar nada).
     */
    fun ground(
        reply: RawAiReply,
        lookup: (String) -> Product?,
        storePrefix: String,
        matchedRecipe: Recipe?,
        budget: Int?,
        people: Int? = null
    ): GroundedReply {
        val recipe = matchedRecipe ?: findRecipeById(reply.recipeId)
        val multiplier = if (recipe != null && people != null) multiplierFor(recipe, people) else 1
        val modelItems = mergeItems(reply.products.filter { it.sku.startsWith("$storePrefix-") && lookup(it.sku) != null })
        val items = if (recipe != null && storePrefix == "BRV") {
            val fromRecipe = recipeCartItems(recipe, lookup, multiplier)
            // Lo que el modelo agregó aparte de la receta (por ejemplo, una bebida) se respeta.
            mergeItems(fromRecipe + modelItems.filter { m -> fromRecipe.none { it.sku == m.sku } })
        } else {
            modelItems
        }

        var text = sanitize(reply.answer)
        if (recipe != null && text.isBlank()) text = "Aquí tienes la receta verificada de **${recipe.name}**."
        reply.question?.let { q -> text = if (text.isBlank()) sanitize(q) else text.trimEnd() + "\n\n" + sanitize(q) }

        val total = totalOf(items, lookup)
        return GroundedReply(
            text = text,
            items = items,
            recipe = recipe,
            suggestion = reply.suggestion,
            total = total,
            budgetExceededBy = budget?.let { b -> (total - b).takeIf { it > 0 } },
            recipeMultiplier = multiplier
        )
    }

    /** Cuántas veces hay que hacer la receta para [people] personas (redondeando hacia arriba, máx. 5). */
    fun multiplierFor(recipe: Recipe, people: Int): Int =
        ((people + recipe.servings - 1) / recipe.servings).coerceIn(1, 5)

    private val peopleRegex = Regex("""\b(?:para|somos|seremos|de)\s+(\d{1,2})\s*(?:personas|persona|gente|comensales|invitados|porciones)\b""")

    /** "para 8 personas" → 8. */
    fun peopleFromQuery(query: String): Int? =
        peopleRegex.find(BravIaText.normalize(query))?.groupValues?.get(1)?.toIntOrNull()?.takeIf { it in 1..60 }

    fun mergeItems(items: List<GroundedItem>): List<GroundedItem> {
        val bySku = LinkedHashMap<String, GroundedItem>()
        for (it in items) {
            val qty = it.qty.coerceIn(1, MAX_QTY)
            val prev = bySku[it.sku]
            bySku[it.sku] = if (prev == null) it.copy(qty = qty) else prev.copy(qty = (prev.qty + qty).coerceAtMost(MAX_QTY))
        }
        return bySku.values.toList()
    }

    /** Productos a comprar para una receta: ingredientes principales, sin básicos de despensa. */
    fun recipeCartItems(recipe: Recipe, lookup: (String) -> Product?, multiplier: Int = 1): List<GroundedItem> = mergeItems(
        recipe.ingredients
            .filter { !it.pantry && !it.optional }
            .mapNotNull { ing ->
                val sku = ing.skus.firstOrNull { lookup(it) != null } ?: return@mapNotNull null
                GroundedItem(sku, unitsFor(ing, multiplier), ing.name)
            }
    )

    /**
     * Unidades a comprar de un ingrediente cuando la receta se multiplica. Solo crece lo que se
     * compra por unidad (4 plátanos → 8) o por libra (carnes, víveres). Un paquete que ya alcanza
     * (12 huevos, 400 g de mantequilla para 3 cucharadas) se queda en 1.
     */
    fun unitsFor(ing: com.example.model.RecipeIngredient, multiplier: Int): Int {
        if (multiplier <= 1) return ing.unitsToBuy
        val byWeight = Regex("""\b(lb|libra|libras)\b""").containsMatchIn(BravIaText.normalize(ing.amount))
        return if (ing.unitsToBuy > 1 || byWeight) ing.unitsToBuy * multiplier else ing.unitsToBuy
    }

    fun totalOf(items: List<GroundedItem>, lookup: (String) -> Product?): Int =
        items.sumOf { (lookup(it.sku)?.price ?: 0) * it.qty }

    /** Recetas del recetario que usan un producto ("¿qué cocino con esto?"). */
    fun recipesUsing(sku: String): List<Recipe> = BRAV_IA_RECIPES.filter { r -> r.ingredients.any { sku in it.skus } }

    /** Recetas con nombre parecido, para cuando piden un plato que no tenemos verificado. */
    fun similarRecipes(query: String, max: Int = 3): List<Recipe> {
        val words = BravIaText.keywords(BravIaText.normalize(query))
        if (words.isEmpty()) return emptyList()
        return BRAV_IA_RECIPES
            .map { r ->
                val hay = BravIaText.normalize(r.name + " " + r.aliases.joinToString(" ") + " " + r.ingredients.joinToString(" ") { it.name })
                r to words.count { w -> hay.contains(if (w.length > 4) w.dropLast(1) else w) }
            }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(max)
            .map { it.first }
    }

    /**
     * Alias que también son nombres de productos ("arroz", "pollo"): solo abren la receta si la
     * persona quiere cocinar. "Necesito arroz" es una compra, no una receta.
     */
    private val ingredientLikeAliases = setOf(
        "arroz", "pollo", "habichuelas", "habichuelas rojas", "frijoles", "avena", "chinola", "maracuya",
        "salmon", "camarones", "ensalada", "sopa", "espaguetis", "espagueti", "spaghetti", "bacalao", "chivo",
        "maduros", "pizza", "hamburguesa", "hamburguesas", "burger", "burgers", "tacos", "taco", "quesadillas", "burritos"
    )
    private val cookingIntent = Regex(
        """\b(receta|recetas|como (hago|hacer|se hace|preparo|preparar|cocino|cocinar)|preparar|preparo|cocinar|cocino|pasos|paso a paso|ingredientes para|que necesito para|hacer un|hacer una|hacer unos|hacer unas)\b"""
    )

    fun matchRecipe(query: String): Recipe? {
        val n = BravIaText.normalize(query)
        val recipe = findRecipeForQuery(n) ?: return null
        val matchedAlias = recipe.aliases
            .filter { Regex("(^|\\W)${Regex.escape(it)}(\\W|$)").containsMatchIn(n) }
            .maxByOrNull { it.length } ?: return recipe
        return if (matchedAlias in ingredientLikeAliases && !cookingIntent.containsMatchIn(n)) null else recipe
    }

    /* -------------------------------------------------------------- */
    /*  6. Presupuesto escrito en el mensaje                            */
    /* -------------------------------------------------------------- */

    private val budgetRegex = Regex(
        """(?:presupuesto|con|por|de|hasta|maximo|menos de|no mas de)\s*(?:de\s*)?(?:rd\s*\$?|\$)?\s*(\d{1,3}(?:[.,]\d{3})+|\d{3,6})\s*(?:pesos|rd|\$)?""",
        RegexOption.IGNORE_CASE
    )

    /** "algo para 4 personas con 1,500 pesos" → 1500. Solo cuenta si se habla de dinero. */
    fun budgetFromQuery(query: String): Int? {
        val n = BravIaText.normalize(query.replace("$", " $ "))
        val mentionsMoney = query.contains("$") || listOf("peso", "presupuesto", "rd ", "dinero", "gastar").any { n.contains(it) }
        if (!mentionsMoney) return null
        val m = budgetRegex.find(query.lowercase().replace("á", "a").replace("ó", "o")) ?: return null
        return m.groupValues[1].replace(".", "").replace(",", "").toIntOrNull()?.takeIf { it in 100..200_000 }
    }

    /* -------------------------------------------------------------- */
    /*  7. Respuesta local (sin conexión o sin sesión)                  */
    /* -------------------------------------------------------------- */

    /**
     * Respuesta honesta cuando no hay modelo disponible: receta verificada si la hay, si no,
     * productos del catálogo que coinciden con la búsqueda. Nunca finge haber visto una foto.
     */
    fun localReply(
        query: String,
        storeProducts: List<Product>,
        lookup: (String) -> Product?,
        storePrefix: String,
        budget: Int?,
        hasImage: Boolean
    ): GroundedReply {
        val n = BravIaText.normalize(query)
        val recipe = if (storePrefix == "BRV") matchRecipe(query) else null
        if (recipe != null) {
            return ground(RawAiReply("Aquí tienes la receta verificada de **${recipe.name}**.", emptyList(), recipe.id, null, null), lookup, storePrefix, recipe, budget, peopleFromQuery(query))
        }
        val factText = when {
            hasImage -> "Ahora mismo no puedo analizar imágenes porque Brav IA está sin conexión. Inténtalo de nuevo en un momento; tu foto no se ha enviado."
            listOf("horario", "abren", "cierran", "abierto", "domingo").any { n.contains(it) } -> StoreFacts.HOURS
            listOf("bravisima", "puntos", "cashback").any { n.contains(it) } -> StoreFacts.BRAVISIMA
            listOf("ncf", "b01", "b02", "comprobante", "factura", "rnc").any { n.contains(it) } -> StoreFacts.NCF
            listOf("danad", "vencid", "reembolso", "devolucion", "reclamo", "queja", "defectuos").any { n.contains(it) } -> StoreFacts.SUPPORT
            else -> null
        }
        if (factText != null) return GroundedReply(factText, emptyList(), null, null, 0, null)

        val keywords = BravIaText.keywords(n)
        val matches = if (keywords.isEmpty()) emptyList() else selectCandidates(query, storeProducts, null, limit = 6)
            .filter { p -> val h = BravIaText.normalize(p.name + " " + p.category); keywords.any { k -> h.contains(if (k.length > 4) k.dropLast(1) else k) } }
        val similar = if (storePrefix == "BRV") similarRecipes(query) else emptyList()
        val wantsRecipe = cookingIntent.containsMatchIn(n)
        val text = buildString {
            if (wantsRecipe && storePrefix == "BRV") append("Esa receta no está en mi recetario verificado, así que no te doy medidas ni pasos inventados. ")
            if (matches.isNotEmpty()) append("Esto encontré en el catálogo para «${query.trim().take(60)}».")
            else append("No encontré productos que coincidan con «${query.trim().take(60)}». Prueba con otro nombre o revisa el catálogo.")
            if (similar.isNotEmpty()) append("\n\nRecetas verificadas que te pueden servir: ").append(similar.joinToString(", ") { "**${it.name}**" }).append(".")
        }
        val items = matches.map { GroundedItem(it.sku, 1) }
        val total = totalOf(items, lookup)
        return GroundedReply(text, items, null, null, total, budget?.let { b -> (total - b).takeIf { it > 0 } })
    }

    /** Respuesta de un modelo que no devolvió JSON: se usa el texto, limpio, y los SKUs que mencione. */
    fun fromPlainText(raw: String): RawAiReply {
        val skus = Regex("""\b(?:BRV|ARC)-\d{3}\b""", RegexOption.IGNORE_CASE).findAll(raw).map { GroundedItem(it.value.uppercase(), 1) }.toList()
        val withoutSkuLine = raw.lines().filterNot { it.trim().startsWith("SKUS", ignoreCase = true) }.joinToString("\n")
        return RawAiReply(withoutSkuLine, skus, null, null, null)
    }
}
