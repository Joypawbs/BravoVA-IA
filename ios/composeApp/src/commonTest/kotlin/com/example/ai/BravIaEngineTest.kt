package com.example.ai

import com.example.data.ArcaCatalogData
import com.example.model.BRAV_IA_RECIPES
import com.example.model.Product
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Banco de pruebas de Brav IA. Cada prueba fija una promesa: si alguien cambia el motor y
 * Brav IA vuelve a poder mostrar un precio inventado o un producto que no existe, falla la CI.
 */
class BravIaEngineTest {
    private val catalog = ArcaCatalogData.getAllProducts()
    private val bySku = catalog.associateBy { it.sku }
    private val lookup: (String) -> Product? = { bySku[it] }
    private val bravo = catalog.filter { it.sku.startsWith("BRV") }

    // ---------- Recetario ----------

    @Test
    fun everyRecipeSkuExistsInCatalog() {
        val missing = BRAV_IA_RECIPES.flatMap { r -> r.ingredients.flatMap { i -> i.skus.filter { it !in bySku }.map { "${r.id}:$it" } } }
        assertTrue(missing.isEmpty(), "SKUs del recetario que no existen: $missing")
    }

    @Test
    fun recipeIdsAreUniqueAndHaveSteps() {
        assertEquals(BRAV_IA_RECIPES.size, BRAV_IA_RECIPES.map { it.id }.toSet().size)
        BRAV_IA_RECIPES.forEach { r ->
            assertTrue(r.steps.size >= 2, "${r.id} sin pasos")
            assertTrue(r.ingredients.isNotEmpty(), "${r.id} sin ingredientes")
            assertTrue(r.servings > 0 && r.totalMinutes > 0, "${r.id} sin porciones o tiempo")
        }
    }

    @Test
    fun findsRecipesByWhatPeopleType() {
        assertEquals("sancocho", BravIaEngine.matchRecipe("¿Cómo hago un SANCOCHO de 7 carnes?")?.id)
        assertEquals("mangu", BravIaEngine.matchRecipe("receta de mangú con los tres golpes")?.id)
        assertEquals("habichuelas_con_dulce", BravIaEngine.matchRecipe("quiero hacer habichuelas con dulce")?.id)
        assertEquals("moro_guandules", BravIaEngine.matchRecipe("moro de guandules")?.id)
        assertNull(BravIaEngine.matchRecipe("receta de paella valenciana"))
    }

    @Test
    fun ingredientWordsDoNotOpenRecipesWithoutCookingIntent() {
        // Quien pide un producto no quiere una receta.
        assertNull(BravIaEngine.matchRecipe("necesito arroz y habichuelas"))
        assertNull(BravIaEngine.matchRecipe("quiero comprar pollo"))
        assertNull(BravIaEngine.matchRecipe("tienen salmon?"))
        // Con intención de cocinar, sí.
        assertEquals("arroz_blanco", BravIaEngine.matchRecipe("como hago arroz blanco")?.id)
        assertEquals("pollo_guisado", BravIaEngine.matchRecipe("receta de pollo")?.id)
        assertEquals("salmon_horno", BravIaEngine.matchRecipe("como preparo el salmon")?.id)
        // Los nombres de platos funcionan solos.
        assertEquals("sancocho", BravIaEngine.matchRecipe("sancocho")?.id)
        assertEquals("tostones", BravIaEngine.matchRecipe("hoy quiero tostones")?.id)
    }

    @Test
    fun recipeCartUsesRealProductsAndQuantities() {
        val mangu = BRAV_IA_RECIPES.first { it.id == "mangu" }
        val items = BravIaEngine.recipeCartItems(mangu, lookup)
        assertTrue(items.all { it.sku in bySku })
        assertEquals(4, items.first { it.sku == "BRV-083" }.qty, "el mangú lleva 4 plátanos")
        assertFalse(items.any { it.sku == "BRV-115" }, "la sal es de despensa: no se agrega sola")
    }

    @Test
    fun scalesRecipeForMorePeople() {
        assertEquals(8, BravIaEngine.peopleFromQuery("sancocho para 8 personas"))
        assertNull(BravIaEngine.peopleFromQuery("arroz de 5 lb"))
        val mangu = BRAV_IA_RECIPES.first { it.id == "mangu" } // 4 porciones
        val g = BravIaEngine.ground(RawAiReply("ok", emptyList(), "mangu", null, null), lookup, "BRV", mangu, null, people = 6)
        assertEquals(2, g.recipeMultiplier)
        assertEquals(8, g.items.first { it.sku == "BRV-083" }.qty, "el doble de plátanos")
        assertEquals(1, g.items.first { it.sku == "BRV-096" }.qty, "un cartón de 12 huevos alcanza")
        assertEquals(1, g.items.first { it.sku == "BRV-100" }.qty, "un bloque de mantequilla alcanza")
        assertEquals(2, g.items.first { it.sku == "BRV-004" }.qty, "el salami va por libra")
    }

    @Test
    fun unknownRecipeIsHonestOffline() {
        val r = BravIaEngine.localReply("receta de paella valenciana", bravo, lookup, "BRV", null, false)
        assertTrue(r.text.startsWith("Esa receta no está en mi recetario verificado"), r.text)
        assertNull(r.recipe)
    }

    // ---------- Lectura del JSON ----------

    @Test
    fun parsesJsonEvenWithCodeFences() {
        val raw = "```json\n{\"respuesta\":\"Hola **Ana**\",\"productos\":[{\"sku\":\"brv-053\",\"cantidad\":\"2\",\"motivo\":\"arroz\"}],\"receta_id\":null,\"pregunta\":null,\"sugerencia\":null}\n```"
        val r = assertNotNull(BravIaEngine.parseReply(raw))
        assertEquals("Hola **Ana**", r.answer)
        assertEquals("BRV-053", r.products.single().sku)
        assertEquals(2, r.products.single().qty)
    }

    @Test
    fun brokenJsonReturnsNull() {
        assertNull(BravIaEngine.parseReply("Lo siento, no puedo"))
        assertNull(BravIaEngine.parseReply("{\"respuesta\": \"sin cerrar"))
    }

    @Test
    fun streamingPreviewReadsPartialAnswer() {
        assertEquals("Claro, aquí\ntienes", BravIaEngine.streamingAnswer("{\"respuesta\": \"Claro, aquí\\ntienes"))
        assertEquals("Dijo \"hola\"", BravIaEngine.streamingAnswer("{\"respuesta\":\"Dijo \\\"hola\\\"\",\"productos\":[]}"))
        assertNull(BravIaEngine.streamingAnswer("{\"produc"))
    }

    // ---------- Nunca un dato inventado ----------

    @Test
    fun sanitizeRemovesPricesCaloriesAndSkus() {
        val dirty = """
            • **Arroz Selecto** — 5 Lb — RD$215.00 [BRV-053]
            • **Pollo** (aprox. 250 kcal) por RD$ 1,250
            **Subtotal:** RD$900.00
            Te sale en 900 pesos.
        """.trimIndent()
        val clean = BravIaEngine.sanitize(dirty)
        assertFalse(clean.contains("RD$"), clean)
        assertFalse(clean.contains("kcal"), clean)
        assertFalse(clean.contains("BRV-"), clean)
        assertFalse(clean.contains("Subtotal"), clean)
        assertFalse(clean.contains("900"), clean)
        assertTrue(clean.contains("**Arroz Selecto** — 5 Lb"), clean)
    }

    @Test
    fun groundDropsInventedAndForeignSkus() {
        val reply = RawAiReply(
            answer = "Te recomiendo esto",
            products = listOf(GroundedItem("BRV-999", 1), GroundedItem("ARC-001", 1), GroundedItem("BRV-053", 50), GroundedItem("BRV-053", 1)),
            recipeId = null, question = null, suggestion = null
        )
        val g = BravIaEngine.ground(reply, lookup, "BRV", null, null)
        assertEquals(listOf("BRV-053"), g.items.map { it.sku })
        assertEquals(BravIaEngine.MAX_QTY, g.items.single().qty)
        assertEquals(bySku.getValue("BRV-053").price * BravIaEngine.MAX_QTY, g.total)
    }

    @Test
    fun recipeIngredientsComeFromRecipeBookNotModel() {
        val reply = RawAiReply("Vamos con ese sancocho", listOf(GroundedItem("BRV-001", 1)), "sancocho", null, null)
        val g = BravIaEngine.ground(reply, lookup, "BRV", null, null)
        assertEquals("sancocho", g.recipe?.id)
        assertTrue(g.items.any { it.sku == "BRV-075" }, "pollo del recetario")
        assertTrue(g.items.any { it.sku == "BRV-001" }, "lo extra que agregó el modelo se respeta")
    }

    @Test
    fun unknownRecipeIdIsIgnored() {
        val g = BravIaEngine.ground(RawAiReply("ok", emptyList(), "paella_inventada", null, null), lookup, "BRV", null, null)
        assertNull(g.recipe)
    }

    @Test
    fun budgetIsWarnedNotSilentlyCut() {
        val reply = RawAiReply("ok", listOf(GroundedItem("BRV-141", 3)), null, null, null) // salmón
        val g = BravIaEngine.ground(reply, lookup, "BRV", null, budget = 500)
        assertEquals(1, g.items.size, "no se borran productos")
        assertEquals(g.total - 500, g.budgetExceededBy)
    }

    @Test
    fun readsBudgetWrittenInMessage() {
        assertEquals(1500, BravIaEngine.budgetFromQuery("comida para 4 personas con 1,500 pesos"))
        assertEquals(800, BravIaEngine.budgetFromQuery("tengo un presupuesto de RD$800"))
        assertNull(BravIaEngine.budgetFromQuery("receta para 4 personas"))
        assertNull(BravIaEngine.budgetFromQuery("arroz de 5 lb"))
    }

    // ---------- Contexto y respuesta local ----------

    @Test
    fun candidatesPrioritizeRecipeAndQuery() {
        val recipe = BRAV_IA_RECIPES.first { it.id == "locrio_pollo" }
        val c = BravIaEngine.selectCandidates("locrio de pollo", bravo, recipe)
        assertTrue(c.size <= 60)
        assertTrue(c.any { it.sku == "BRV-082" })
        assertTrue(c.all { it.sku.startsWith("BRV") })
    }

    @Test
    fun promptCarriesNoPersonalDataAndNoPrices() {
        val prompt = BravIaEngine.buildPrompt(
            PromptContext("arroz", "Supermercados Bravo", "Piantini", 25, bravo.take(5), null, emptyList(), null, null, emptyList(), false)
        )
        assertFalse(prompt.contains("RD$" + bravo.first().price))
        assertFalse(prompt.contains("@"))
        assertTrue(prompt.contains(bravo.first().sku))
    }

    @Test
    fun localReplyNeverPretendsToSeeAnImage() {
        val r = BravIaEngine.localReply("mira este producto dañado", bravo, lookup, "BRV", null, hasImage = true)
        assertTrue(r.text.contains("no puedo analizar"), r.text)
        assertTrue(r.items.isEmpty())
    }

    @Test
    fun localReplyGivesVerifiedRecipe() {
        val r = BravIaEngine.localReply("cómo hago tostones", bravo, lookup, "BRV", null, false)
        assertEquals("tostones", r.recipe?.id)
        assertTrue(r.items.isNotEmpty())
    }

    @Test
    fun localReplyAnswersStoreHoursFromFacts() {
        val r = BravIaEngine.localReply("¿a qué hora abren el domingo?", bravo, lookup, "BRV", null, false)
        assertEquals(StoreFacts.HOURS, r.text)
    }

    @Test
    fun plainTextFallbackExtractsSkus() {
        val r = BravIaEngine.fromPlainText("Lleva arroz.\nSKUS: [\"BRV-053\"]")
        assertEquals("BRV-053", r.products.single().sku)
        assertFalse(r.answer.contains("SKUS"))
    }
}
