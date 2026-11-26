package com.example.model

/**
 * Recetario verificado de Brav IA.
 *
 * Para qué: que Brav IA NUNCA invente una receta. Cuando alguien pide un plato, la receta
 * (ingredientes, cantidades, pasos y tiempos) sale de aquí, no del modelo de lenguaje. El modelo
 * solo elige el id de la receta; la app la muestra tal cual.
 *
 * Cómo se armó: versiones tradicionales de cada plato, contrastadas con recetarios dominicanos
 * publicados (Cocina Dominicana / Aunt Clara's Kitchen, Diario Libre, Goya). Los pasos están
 * redactados para BravoVa. Los tiempos y porciones son aproximados y así se indica en pantalla.
 *
 * Cada ingrediente apunta a productos reales del catálogo ([RecipeIngredient.skus], en orden de
 * preferencia). Si un ingrediente no existe en el catálogo, [RecipeIngredient.skus] queda vacío y la
 * app lo muestra como "no disponible en BravoVa" en vez de inventar un producto.
 */
data class RecipeIngredient(
    /** Cantidad para la receta, tal como se mide en la cocina ("2 tazas", "1 lb"). */
    val amount: String,
    val name: String,
    /** Productos del catálogo que sirven, en orden de preferencia. Vacío = no lo vendemos. */
    val skus: List<String> = emptyList(),
    /** Básico de despensa (sal, aceite…): se muestra pero no se agrega solo al carrito. */
    val pantry: Boolean = false,
    val optional: Boolean = false,
    /** Cuántas unidades del producto hacen falta para la receta (por ejemplo 4 plátanos). */
    val unitsToBuy: Int = 1
)

data class Recipe(
    val id: String,
    val name: String,
    /** Formas en que la gente lo pide, sin tildes ni mayúsculas. */
    val aliases: List<String>,
    val kind: String,
    val servings: Int,
    val prepMinutes: Int,
    val cookMinutes: Int,
    val ingredients: List<RecipeIngredient>,
    val steps: List<String>,
    val serveWith: String? = null
) {
    val totalMinutes: Int get() = prepMinutes + cookMinutes
}

private fun ing(
    amount: String,
    name: String,
    vararg skus: String,
    pantry: Boolean = false,
    optional: Boolean = false,
    units: Int = 1
) = RecipeIngredient(amount, name, skus.toList(), pantry, optional, units)

// Básicos que se repiten
private val SAL = ing("Al gusto", "Sal", "BRV-115", pantry = true)
private val ACEITE = ing("3 cucharadas", "Aceite vegetal", "BRV-037", "BRV-252", pantry = true)
private val AJO = ing("4 dientes", "Ajo machacado", "BRV-171", "BRV-245", pantry = true)
private val OREGANO = ing("1 cucharadita", "Orégano", "BRV-243", "BRV-362", pantry = true)
private val SAZON = ing("2 cucharadas", "Sazón líquido", "BRV-241", "BRV-062", pantry = true, optional = true)
private val CEBOLLA = ing("1 mediana", "Cebolla roja picada", "BRV-089", "BRV-169")
private val AJI = ing("1", "Ají cubanela picado", "BRV-172")
private val RECAO = ing("2 ramitas", "Cilantro ancho (recao)", "BRV-175", "BRV-384")
private val PASTA_TOMATE = ing("2 cucharadas", "Pasta de tomate", "BRV-107", "BRV-058")
private val LIMON = ing("1", "Limón", "BRV-177", optional = true)

val BRAV_IA_RECIPES: List<Recipe> = listOf(
    Recipe(
        id = "sancocho", name = "Sancocho dominicano",
        aliases = listOf("sancocho", "sancocho dominicano", "sancocho de 7 carnes", "sancocho de siete carnes", "sancocho de pollo"),
        kind = "Plato fuerte", servings = 8, prepMinutes = 30, cookMinutes = 120,
        ingredients = listOf(
            ing("2 lb", "Pollo en presas", "BRV-075", "BRV-139", "BRV-082"),
            ing("1.5 lb", "Carne de res para guisar", "BRV-077", "BRV-137"),
            ing("1 lb", "Costilla o huesito ahumado de cerdo", "BRV-324", "BRV-079"),
            ing("1 lb", "Yuca", "BRV-085", "BRV-163"),
            ing("1 lb", "Yautía", "BRV-086", "BRV-381"),
            ing("1 lb", "Ñame", "BRV-164", "BRV-382"),
            ing("1 lb", "Auyama", "BRV-166", "BRV-087"),
            ing("3", "Plátanos verdes", "BRV-083", "BRV-380", units = 3),
            ing("1 taza", "Maíz en grano", "BRV-157", "BRV-106", optional = true),
            LIMON, CEBOLLA, AJI, RECAO, AJO, OREGANO, SAZON, SAL, ACEITE
        ),
        steps = listOf(
            "Sazona la res y el cerdo con ajo, orégano, sal y jugo de limón; deja reposar 20 minutos.",
            "En una olla grande calienta el aceite y sofríe la res y el cerdo con la cebolla, el ají y el recao, revolviendo, unos 15 minutos.",
            "Agrega el pollo sazonado y cocina 10 minutos más, tapado, a fuego medio.",
            "Añade unas 12 tazas de agua y cocina a fuego medio hasta que la res esté casi blanda, aproximadamente 45 minutos.",
            "Agrega primero ñame, yautía y la mitad de la auyama; a los 15 minutos, la yuca y el plátano en trozos.",
            "Cuando todo esté blando (unos 30 minutos más), retira algunos trozos de auyama, aplástalos y devuélvelos para espesar.",
            "Rectifica la sal y deja hervir a fuego bajo 10 minutos más. Debe quedar espeso."
        ),
        serveWith = "Arroz blanco y aguacate."
    ),
    Recipe(
        id = "locrio_pollo", name = "Locrio de pollo",
        aliases = listOf("locrio", "locrio de pollo", "arroz con pollo", "locrio dominicano"),
        kind = "Plato fuerte", servings = 4, prepMinutes = 15, cookMinutes = 35,
        ingredients = listOf(
            ing("2 lb", "Pollo en presas pequeñas", "BRV-082", "BRV-075", "BRV-139"),
            ing("2 tazas", "Arroz", "BRV-053", "BRV-101"),
            PASTA_TOMATE, CEBOLLA, AJI, RECAO, AJO, OREGANO, LIMON, SAL, ACEITE
        ),
        steps = listOf(
            "Sazona el pollo con ajo, orégano, sal y limón.",
            "En un caldero calienta el aceite y dora el pollo a fuego medio-alto, unos 10 minutos.",
            "Agrega cebolla, ají, recao y la pasta de tomate; cocina 5 minutos revolviendo.",
            "Añade 3 tazas de agua y deja hervir. Rectifica la sal: el caldo debe quedar un poco salado.",
            "Agrega el arroz lavado y mueve hasta que se seque el agua de la superficie.",
            "Tapa y cocina a fuego bajo unos 15 minutos; voltea el arroz y deja 5 minutos más."
        ),
        serveWith = "Ensalada verde o aguacate."
    ),
    Recipe(
        id = "moro_guandules", name = "Moro de guandules con coco",
        aliases = listOf("moro", "moro de guandules", "moro de guandules con coco", "arroz con guandules", "arroz con gandules"),
        kind = "Acompañante", servings = 6, prepMinutes = 10, cookMinutes = 25,
        ingredients = listOf(
            ing("2 tazas", "Arroz", "BRV-053", "BRV-101"),
            ing("1 lata (15 oz)", "Guandules con coco", "BRV-104", "BRV-370", "BRV-056"),
            PASTA_TOMATE, CEBOLLA, AJI, RECAO, AJO, SAL, ACEITE
        ),
        steps = listOf(
            "En un caldero calienta el aceite y sofríe cebolla, ají, recao y ajo por 2 minutos.",
            "Agrega la pasta de tomate y los guandules con su líquido; cocina 3 minutos.",
            "Añade 2½ tazas de agua, rectifica la sal y deja hervir.",
            "Agrega el arroz lavado y revuelve hasta que el agua baje al nivel del arroz.",
            "Tapa y cocina a fuego bajo unos 15 minutos; voltea y deja 5 minutos más."
        ),
        serveWith = "Pollo guisado o carne de cerdo."
    ),
    Recipe(
        id = "arroz_blanco", name = "Arroz blanco dominicano",
        aliases = listOf("arroz blanco", "arroz", "como hacer arroz", "concon"),
        kind = "Acompañante", servings = 4, prepMinutes = 5, cookMinutes = 25,
        ingredients = listOf(
            ing("2 tazas", "Arroz", "BRV-053", "BRV-101"),
            ing("2 cucharadas", "Aceite vegetal", "BRV-037", "BRV-252"),
            SAL
        ),
        steps = listOf(
            "Lleva a hervir 3 tazas de agua con la sal y el aceite en un caldero.",
            "Agrega el arroz lavado y revuelve hasta que el agua baje al nivel del arroz.",
            "Tapa y cocina a fuego bajo unos 15 minutos.",
            "Voltea el arroz con una cuchara, tapa y deja 5 minutos más. Si quieres concón, déjalo unos minutos extra sin mover."
        )
    ),
    Recipe(
        id = "habichuelas_guisadas", name = "Habichuelas rojas guisadas",
        aliases = listOf("habichuelas", "habichuelas guisadas", "habichuelas rojas", "habichuela guisada", "frijoles"),
        kind = "Acompañante", servings = 6, prepMinutes = 15, cookMinutes = 90,
        ingredients = listOf(
            ing("1 lb", "Habichuelas rojas secas", "BRV-102", "BRV-055"),
            ing("½ lb", "Auyama en cubitos", "BRV-166", "BRV-087"),
            PASTA_TOMATE, CEBOLLA, AJI, RECAO, AJO, OREGANO, SAL, ACEITE
        ),
        steps = listOf(
            "Remoja las habichuelas la noche anterior; escúrrelas.",
            "Hiérvelas en abundante agua hasta que estén blandas, aproximadamente 1 hora (20 minutos en olla de presión).",
            "En otra olla sofríe en aceite la cebolla, el ají, el recao y el ajo; agrega la pasta de tomate.",
            "Añade las habichuelas con 2 tazas de su caldo, la auyama, el orégano y la sal.",
            "Cocina a fuego medio unos 20 minutos, aplastando la auyama, hasta que espese."
        ),
        serveWith = "Arroz blanco."
    ),
    Recipe(
        id = "pollo_guisado", name = "Pollo guisado",
        aliases = listOf("pollo guisado", "pollo", "guiso de pollo", "pollo dominicano"),
        kind = "Plato fuerte", servings = 4, prepMinutes = 20, cookMinutes = 40,
        ingredients = listOf(
            ing("3 lb", "Pollo en presas", "BRV-075", "BRV-139", "BRV-082"),
            ing("1 cucharada", "Azúcar (para dorar)", "BRV-117", "BRV-116", pantry = true),
            PASTA_TOMATE, CEBOLLA, AJI, RECAO, AJO, OREGANO, LIMON, SAL, ACEITE
        ),
        steps = listOf(
            "Lava el pollo con limón y sazónalo con ajo, orégano, sal, cebolla, ají y recao; deja reposar 30 minutos.",
            "Calienta el aceite en un caldero, agrega el azúcar y espera a que tome color caramelo.",
            "Agrega el pollo (sin los vegetales) y dóralo por todos lados, unos 8 minutos.",
            "Incorpora los vegetales del adobo y la pasta de tomate; tapa y cocina 15 minutos.",
            "Añade ½ taza de agua, tapa y cocina a fuego bajo unos 15 minutos, hasta que la salsa espese."
        ),
        serveWith = "Arroz blanco, habichuelas y ensalada: la bandera."
    ),
    Recipe(
        id = "la_bandera", name = "La bandera dominicana",
        aliases = listOf("la bandera", "bandera", "bandera dominicana", "almuerzo dominicano"),
        kind = "Plato fuerte", servings = 4, prepMinutes = 30, cookMinutes = 90,
        ingredients = listOf(
            ing("2 tazas", "Arroz", "BRV-053", "BRV-101"),
            ing("1 lb", "Habichuelas rojas secas", "BRV-102", "BRV-055"),
            ing("3 lb", "Pollo en presas", "BRV-075", "BRV-139"),
            ing("1", "Lechuga", "BRV-189", "BRV-091"),
            ing("1 lb", "Tomate", "BRV-168", "BRV-167"),
            PASTA_TOMATE, CEBOLLA, AJI, RECAO, AJO, OREGANO, SAL, ACEITE
        ),
        steps = listOf(
            "Prepara las habichuelas guisadas (receta «Habichuelas rojas guisadas»).",
            "Mientras se ablandan, prepara el pollo guisado (receta «Pollo guisado»).",
            "Cuando falten 25 minutos, cocina el arroz blanco (receta «Arroz blanco dominicano»).",
            "Corta la lechuga y el tomate y alíñalos con sal, aceite y vinagre o limón.",
            "Sirve el arroz con las habichuelas aparte o por encima, el pollo y la ensalada."
        )
    ),
    Recipe(
        id = "mangu", name = "Mangú con los tres golpes",
        aliases = listOf("mangu", "los tres golpes", "tres golpes", "mangu con los tres golpes", "desayuno dominicano"),
        kind = "Desayuno", servings = 4, prepMinutes = 10, cookMinutes = 30,
        ingredients = listOf(
            ing("4", "Plátanos verdes", "BRV-083", "BRV-380", units = 4),
            ing("3 cucharadas", "Mantequilla", "BRV-100", "BRV-215"),
            ing("1 lb", "Salami dominicano", "BRV-004", "BRV-002"),
            ing("1 lb", "Queso de freír", "BRV-093", "BRV-393"),
            ing("4", "Huevos", "BRV-096", "BRV-097"),
            ing("1", "Cebolla roja en aros", "BRV-089", "BRV-169"),
            ing("2 cucharadas", "Vinagre blanco", "BRV-040", pantry = true),
            SAL, ACEITE
        ),
        steps = listOf(
            "Pela los plátanos, córtalos en 4 y hiérvelos en agua con sal hasta que estén muy blandos, unos 20 minutos.",
            "Mientras tanto, sofríe la cebolla en aros con 1 cucharada de aceite y el vinagre; reserva.",
            "Retira los plátanos y májalos con la mantequilla y un poco del agua de hervir hasta que queden suaves.",
            "Fríe en aceite el salami en ruedas, luego el queso en lascas, y por último los huevos.",
            "Sirve el mangú cubierto con la cebolla, acompañado del salami, el queso y el huevo."
        )
    ),
    Recipe(
        id = "tostones", name = "Tostones",
        aliases = listOf("tostones", "fritos verdes", "platano frito", "patacones"),
        kind = "Acompañante", servings = 4, prepMinutes = 10, cookMinutes = 15,
        ingredients = listOf(
            ing("3", "Plátanos verdes", "BRV-083", "BRV-380", units = 3),
            ing("1½ tazas", "Aceite para freír", "BRV-037", "BRV-252"),
            SAL
        ),
        steps = listOf(
            "Pela los plátanos y córtalos en ruedas de unos 2 cm.",
            "Fríelos en aceite caliente a fuego medio unos 4 minutos, hasta que estén blandos pero sin dorar.",
            "Sácalos y aplástalos con una tostonera o el fondo de un vaso.",
            "Vuelve a freírlos a fuego alto 2–3 minutos, hasta que estén dorados y crujientes.",
            "Escúrrelos en papel y sálalos al momento."
        )
    ),
    Recipe(
        id = "maduros", name = "Plátanos maduros fritos",
        aliases = listOf("maduros", "maduritos", "platanos maduros", "maduros fritos"),
        kind = "Acompañante", servings = 4, prepMinutes = 5, cookMinutes = 10,
        ingredients = listOf(
            ing("3", "Plátanos maduros", "BRV-084", "BRV-162", units = 3),
            ing("1 taza", "Aceite para freír", "BRV-037", "BRV-252")
        ),
        steps = listOf(
            "Pela los plátanos y córtalos en diagonal, de 1 cm de grosor.",
            "Fríelos en aceite a fuego medio unos 3 minutos por lado, hasta que estén dorados.",
            "Escúrrelos en papel absorbente y sirve caliente."
        )
    ),
    Recipe(
        id = "habichuelas_con_dulce", name = "Habichuelas con dulce",
        aliases = listOf("habichuelas con dulce", "habichuela con dulce", "habichuelas dulces", "postre de cuaresma"),
        kind = "Postre", servings = 10, prepMinutes = 15, cookMinutes = 90,
        ingredients = listOf(
            ing("1 lb", "Habichuelas rojas secas", "BRV-102", "BRV-055"),
            ing("1 lb", "Batata en cubitos", "BRV-165"),
            ing("2 latas (315 g c/u)", "Leche evaporada", "BRV-099", units = 2),
            ing("1 lata (395 g)", "Leche condensada", "BRV-360"),
            ing("1 lata (425 g)", "Crema de coco", "BRV-361"),
            ing("1 taza", "Azúcar crema", "BRV-116", "BRV-060"),
            ing("½ taza", "Pasas", "BRV-379"),
            ing("4 astillas", "Canela", "BRV-246"),
            ing("6 granos", "Clavo dulce", "BRV-378", optional = true),
            ing("1 cucharadita", "Vainilla", "BRV-247"),
            ing("Para servir", "Galletas de leche", "BRV-232"),
            SAL
        ),
        steps = listOf(
            "Remoja las habichuelas la noche anterior y hiérvelas con la canela hasta que estén muy blandas, aproximadamente 1 hora.",
            "Licúa las habichuelas con 4 tazas de su agua y cuela para quitar la cáscara.",
            "Lleva el líquido a una olla con la leche evaporada, la crema de coco, el azúcar, el clavo y una pizca de sal; cocina a fuego medio, revolviendo.",
            "Agrega la batata y cocina unos 15 minutos, hasta que esté blanda.",
            "Añade la leche condensada, las pasas y la vainilla; cocina 10 minutos más, revolviendo para que no se pegue.",
            "Retira la canela y el clavo. Sirve frío o tibio con galletas de leche."
        )
    ),
    Recipe(
        id = "chofan", name = "Chofán dominicano",
        aliases = listOf("chofan", "chofan dominicano", "arroz chino", "arroz frito"),
        kind = "Plato fuerte", servings = 4, prepMinutes = 15, cookMinutes = 20,
        ingredients = listOf(
            ing("4 tazas", "Arroz blanco cocido (del día anterior)", "BRV-053", "BRV-101"),
            ing("1 lb", "Pechuga de pollo en cubitos", "BRV-121", "BRV-076"),
            ing("½ lb", "Jamón en cubitos", "BRV-005"),
            ing("2", "Huevos", "BRV-096"),
            ing("1 taza", "Vegetales mixtos", "BRV-155"),
            ing("3 cucharadas", "Salsa para chofán o soya", "BRV-366", "BRV-249"),
            ing("1", "Ají morrón picado", "BRV-173", optional = true),
            AJO, SAL, ACEITE
        ),
        steps = listOf(
            "Bate los huevos, haz una tortilla fina en un sartén, córtala en tiritas y reserva.",
            "En un sartén grande o wok, dora el pollo con aceite y ajo, unos 7 minutos.",
            "Agrega el jamón, los vegetales y el ají morrón; saltea 4 minutos a fuego alto.",
            "Incorpora el arroz frío y la salsa, mezclando a fuego alto hasta que el arroz tome color, unos 5 minutos.",
            "Añade la tortilla en tiras, rectifica la sal y sirve."
        )
    ),
    Recipe(
        id = "espaguetis_dominicanos", name = "Espaguetis a la dominicana",
        aliases = listOf("espaguetis", "espagueti", "spaghetti", "espaguetis a la dominicana", "pasta con salami", "espaguetis con salami"),
        kind = "Plato fuerte", servings = 4, prepMinutes = 10, cookMinutes = 20,
        ingredients = listOf(
            ing("1 paquete (400 g)", "Espaguetis", "BRV-057", "BRV-109"),
            ing("½ lb", "Salami dominicano en cubitos", "BRV-004", "BRV-002"),
            ing("3 cucharadas", "Pasta de tomate", "BRV-107", "BRV-058"),
            ing("½ taza", "Leche evaporada", "BRV-099", optional = true),
            ing("½ taza", "Queso rallado", "BRV-218", optional = true),
            CEBOLLA, AJI, AJO, OREGANO, SAL, ACEITE
        ),
        steps = listOf(
            "Hierve los espaguetis en agua con sal según el paquete, unos 10 minutos; escúrrelos.",
            "En un caldero, dora el salami en aceite unos 4 minutos.",
            "Agrega cebolla, ají y ajo; sofríe 2 minutos. Añade la pasta de tomate, el orégano y ½ taza de agua.",
            "Si usas leche evaporada, agrégala y cocina la salsa 3 minutos.",
            "Incorpora los espaguetis, mezcla bien y sirve con queso rallado."
        )
    ),
    Recipe(
        id = "pastelon_maduro", name = "Pastelón de plátano maduro",
        aliases = listOf("pastelon", "pastelon de platano maduro", "pastelon de maduro", "pastelon de platano"),
        kind = "Plato fuerte", servings = 6, prepMinutes = 20, cookMinutes = 50,
        ingredients = listOf(
            ing("6", "Plátanos maduros", "BRV-084", "BRV-162", units = 6),
            ing("1.5 lb", "Carne molida de res", "BRV-078", "BRV-126"),
            ing("3 cucharadas", "Mantequilla", "BRV-100", "BRV-215"),
            ing("1 lb", "Queso mozzarella o gouda rallado", "BRV-202", "BRV-201"),
            ing("2", "Huevos", "BRV-096"),
            PASTA_TOMATE, CEBOLLA, AJI, AJO, OREGANO, SAL, ACEITE
        ),
        steps = listOf(
            "Hierve los plátanos pelados unos 20 minutos y májalos con la mantequilla.",
            "Sofríe la carne molida con cebolla, ají, ajo, orégano, pasta de tomate y sal hasta que esté cocida, unos 15 minutos.",
            "Precalienta el horno a 180 °C (350 °F) y engrasa un molde.",
            "Coloca una capa de puré de plátano, la carne, el queso y otra capa de plátano.",
            "Unta los huevos batidos por encima y hornea unos 30 minutos, hasta que dore."
        )
    ),
    Recipe(
        id = "res_guisada", name = "Carne de res guisada",
        aliases = listOf("carne guisada", "res guisada", "carne de res guisada", "guiso de res"),
        kind = "Plato fuerte", servings = 4, prepMinutes = 20, cookMinutes = 75,
        ingredients = listOf(
            ing("1.5 lb", "Carne de res para guisar", "BRV-077", "BRV-137"),
            ing("2", "Papas en cubos", "BRV-192", optional = true),
            ing("1", "Zanahoria en ruedas", "BRV-191", optional = true),
            PASTA_TOMATE, CEBOLLA, AJI, RECAO, AJO, OREGANO, LIMON, SAL, ACEITE
        ),
        steps = listOf(
            "Sazona la carne en cubos con ajo, orégano, sal y limón; deja reposar 30 minutos.",
            "Dórala en aceite caliente por todos lados, unos 10 minutos.",
            "Agrega cebolla, ají, recao y pasta de tomate; sofríe 3 minutos.",
            "Añade 2 tazas de agua, tapa y cocina a fuego bajo hasta que la carne esté blanda, alrededor de 1 hora.",
            "Si usas papa y zanahoria, agrégalas en los últimos 20 minutos."
        ),
        serveWith = "Arroz blanco."
    ),
    Recipe(
        id = "chivo_guisado", name = "Chivo guisado",
        aliases = listOf("chivo", "chivo guisado", "chivo liniero"),
        kind = "Plato fuerte", servings = 4, prepMinutes = 20, cookMinutes = 90,
        ingredients = listOf(
            ing("2 lb", "Chivo en presas", "BRV-311", "BRV-129"),
            ing("1 cucharadita", "Orégano", "BRV-362", "BRV-243"),
            PASTA_TOMATE, CEBOLLA, AJI, RECAO, AJO, LIMON, SAL, ACEITE
        ),
        steps = listOf(
            "Lava el chivo con limón y sazónalo con ajo, orégano, sal y los vegetales; deja reposar al menos 1 hora.",
            "Dóralo en aceite caliente, unos 10 minutos.",
            "Agrega la pasta de tomate y 3 tazas de agua.",
            "Tapa y cocina a fuego bajo hasta que esté blando, aproximadamente 1 hora y 20 minutos; agrega agua si hace falta.",
            "Destapa al final para que la salsa espese."
        ),
        serveWith = "Moro o arroz blanco."
    ),
    Recipe(
        id = "bacalao_guisado", name = "Bacalao guisado",
        aliases = listOf("bacalao", "bacalao guisado"),
        kind = "Plato fuerte", servings = 4, prepMinutes = 20, cookMinutes = 25,
        ingredients = listOf(
            ing("1 lb", "Bacalao desalado", "BRV-338", "BRV-147"),
            ing("1 lb", "Tomate", "BRV-167", "BRV-168"),
            ing("1", "Ají morrón", "BRV-173"),
            ing("2 cucharadas", "Aceite de oliva", "BRV-039"),
            CEBOLLA, AJO, PASTA_TOMATE
        ),
        steps = listOf(
            "Si el bacalao no viene desalado, remójalo en agua 12 horas cambiando el agua varias veces.",
            "Hiérvelo 10 minutos, escúrrelo y desmenúzalo.",
            "Sofríe en aceite de oliva la cebolla, el ajo y el ají morrón, 3 minutos.",
            "Agrega el tomate picado y la pasta de tomate; cocina 5 minutos.",
            "Incorpora el bacalao y ½ taza de agua; cocina a fuego bajo 10 minutos."
        ),
        serveWith = "Arroz blanco o víveres hervidos."
    ),
    Recipe(
        id = "pescado_coco", name = "Pescado con coco",
        aliases = listOf("pescado con coco", "pescado al coco", "pescado samana"),
        kind = "Plato fuerte", servings = 4, prepMinutes = 15, cookMinutes = 25,
        ingredients = listOf(
            ing("2 lb", "Filete de pescado blanco", "BRV-326", "BRV-328", "BRV-347"),
            ing("1 lata (13 oz)", "Leche de coco sin azúcar"), // no la vendemos: la crema de coco es dulce
            ing("1", "Ají morrón", "BRV-173"),
            ing("1 lb", "Tomate", "BRV-167"),
            CEBOLLA, AJO, RECAO, LIMON, SAL, ACEITE
        ),
        steps = listOf(
            "Sazona el pescado con sal, ajo y limón.",
            "Sofríe en aceite la cebolla, el ají morrón y el tomate, unos 5 minutos.",
            "Agrega la leche de coco y deja hervir suave 5 minutos.",
            "Coloca el pescado en la salsa, tapa y cocina a fuego bajo 10–12 minutos, sin revolver.",
            "Termina con recao picado."
        ),
        serveWith = "Arroz blanco y tostones."
    ),
    Recipe(
        id = "locrio_salami", name = "Locrio de salami",
        aliases = listOf("locrio de salami", "arroz con salami"),
        kind = "Plato fuerte", servings = 4, prepMinutes = 10, cookMinutes = 30,
        ingredients = listOf(
            ing("1 lb", "Salami dominicano en cubos", "BRV-004", "BRV-002"),
            ing("2 tazas", "Arroz", "BRV-053", "BRV-101"),
            PASTA_TOMATE, CEBOLLA, AJI, RECAO, AJO, SAL, ACEITE
        ),
        steps = listOf(
            "Dora el salami en aceite en un caldero, unos 5 minutos.",
            "Agrega cebolla, ají, recao, ajo y pasta de tomate; sofríe 2 minutos.",
            "Añade 3 tazas de agua y deja hervir; rectifica la sal.",
            "Agrega el arroz y mueve hasta que se seque el agua de la superficie.",
            "Tapa a fuego bajo 15 minutos, voltea y deja 5 minutos más."
        )
    ),
    Recipe(
        id = "sopa_pollo", name = "Sopa de pollo con fideos",
        aliases = listOf("sopa de pollo", "sopa", "sopa con fideos", "caldo de pollo"),
        kind = "Plato fuerte", servings = 6, prepMinutes = 15, cookMinutes = 45,
        ingredients = listOf(
            ing("2 lb", "Pollo en presas", "BRV-082", "BRV-075"),
            ing("1 paquete", "Fideos finos para sopa", "BRV-111"),
            ing("2", "Papas", "BRV-192"),
            ing("2", "Zanahorias", "BRV-191"),
            ing("½ lb", "Auyama", "BRV-166"),
            ing("1 rama", "Apio", "BRV-194", optional = true),
            ing("1", "Cubito de pollo", "BRV-242", optional = true),
            CEBOLLA, AJO, RECAO, SAL
        ),
        steps = listOf(
            "Pon el pollo en una olla con 10 tazas de agua, cebolla, ajo, apio y sal; hierve 20 minutos retirando la espuma.",
            "Agrega la auyama, la papa y la zanahoria en cubos; cocina 15 minutos.",
            "Añade los fideos y el cubito; cocina 6–8 minutos más.",
            "Rectifica la sal y termina con recao picado."
        )
    ),
    Recipe(
        id = "asopao_pollo", name = "Asopao de pollo",
        aliases = listOf("asopao", "asopao de pollo"),
        kind = "Plato fuerte", servings = 6, prepMinutes = 15, cookMinutes = 40,
        ingredients = listOf(
            ing("2 lb", "Pollo en presas", "BRV-082", "BRV-075"),
            ing("1 taza", "Arroz", "BRV-053"),
            ing("1 lb", "Tomate", "BRV-167"),
            PASTA_TOMATE, CEBOLLA, AJI, RECAO, AJO, OREGANO, SAL, ACEITE
        ),
        steps = listOf(
            "Sazona el pollo y dóralo en aceite en una olla, unos 8 minutos.",
            "Agrega cebolla, ají, ajo, tomate y pasta de tomate; sofríe 5 minutos.",
            "Añade 8 tazas de agua y cocina 15 minutos.",
            "Incorpora el arroz lavado y cocina a fuego medio unos 20 minutos, revolviendo de vez en cuando. Debe quedar caldoso.",
            "Termina con recao picado y sirve caliente."
        )
    ),
    Recipe(
        id = "mofongo", name = "Mofongo",
        aliases = listOf("mofongo", "mofongo de chicharron"),
        kind = "Plato fuerte", servings = 4, prepMinutes = 15, cookMinutes = 25,
        ingredients = listOf(
            ing("4", "Plátanos verdes", "BRV-083", "BRV-380", units = 4),
            ing("½ lb", "Chicharrón de cerdo", "BRV-318"),
            ing("6 dientes", "Ajo", "BRV-171"),
            ing("3 cucharadas", "Mantequilla o aceite de oliva", "BRV-100", "BRV-039"),
            ing("1½ tazas", "Aceite para freír", "BRV-037"),
            SAL
        ),
        steps = listOf(
            "Fríe el chicharrón hasta que esté crujiente y desmenúzalo.",
            "Corta los plátanos en ruedas y fríelos a fuego medio unos 8 minutos, hasta que estén blandos y doraditos.",
            "En un pilón, maja el ajo con sal y la mantequilla.",
            "Agrega los plátanos calientes y el chicharrón poco a poco, majando hasta formar una masa.",
            "Dale forma de bola y sirve caliente."
        )
    ),
    Recipe(
        id = "avena", name = "Avena caliente dominicana",
        aliases = listOf("avena", "avena caliente", "avena con leche", "desayuno de avena"),
        kind = "Desayuno", servings = 4, prepMinutes = 5, cookMinutes = 15,
        ingredients = listOf(
            ing("1 taza", "Avena en hojuelas", "BRV-114", "BRV-061"),
            ing("4 tazas", "Leche", "BRV-098", units = 1),
            ing("2 astillas", "Canela", "BRV-246"),
            ing("4 cucharadas", "Azúcar", "BRV-116", "BRV-117"),
            ing("½ cucharadita", "Vainilla", "BRV-247", optional = true),
            ing("Una pizca", "Sal", "BRV-115", pantry = true)
        ),
        steps = listOf(
            "Hierve 2 tazas de agua con la canela y la pizca de sal.",
            "Agrega la avena y cocina a fuego medio 5 minutos, revolviendo.",
            "Incorpora la leche y el azúcar; cocina 5–8 minutos más hasta que espese.",
            "Retira la canela, agrega la vainilla y sirve caliente."
        )
    ),
    Recipe(
        id = "jugo_chinola", name = "Jugo de chinola",
        aliases = listOf("jugo de chinola", "chinola", "jugo de maracuya", "maracuya"),
        kind = "Bebida", servings = 6, prepMinutes = 10, cookMinutes = 0,
        ingredients = listOf(
            ing("1 lb de pulpa o 5 chinolas", "Chinola", "BRV-423", "BRV-182"),
            ing("½ taza", "Azúcar", "BRV-117", "BRV-116"),
            ing("Al gusto", "Hielo", "BRV-430", optional = true)
        ),
        steps = listOf(
            "Si usas chinolas frescas, sácales la pulpa con una cuchara.",
            "Licúa la pulpa con 2 tazas de agua unos segundos y cuela para quitar las semillas.",
            "Agrega 4 tazas de agua más y el azúcar; mezcla bien.",
            "Sirve bien frío con hielo."
        )
    ),
    Recipe(
        id = "arroz_con_leche", name = "Arroz con leche",
        aliases = listOf("arroz con leche"),
        kind = "Postre", servings = 6, prepMinutes = 5, cookMinutes = 40,
        ingredients = listOf(
            ing("1 taza", "Arroz", "BRV-053"),
            ing("1 litro", "Leche", "BRV-098"),
            ing("1 lata (315 g)", "Leche evaporada", "BRV-099"),
            ing("¾ taza", "Azúcar", "BRV-117", "BRV-116"),
            ing("3 astillas", "Canela", "BRV-246"),
            ing("¼ taza", "Pasas", "BRV-379", optional = true),
            ing("1 cucharadita", "Vainilla", "BRV-247"),
            ing("Una pizca", "Sal", "BRV-115", pantry = true)
        ),
        steps = listOf(
            "Cocina el arroz lavado en 2 tazas de agua con la canela y la sal hasta que se seque, unos 15 minutos.",
            "Agrega la leche y la leche evaporada; cocina a fuego bajo 15–20 minutos revolviendo para que no se pegue.",
            "Añade el azúcar, las pasas y la vainilla; cocina 5 minutos más.",
            "Sirve tibio o frío con canela en polvo."
        )
    ),
    Recipe(
        id = "pizza_casera", name = "Pizza casera",
        aliases = listOf("pizza", "pizza casera", "masa de pizza"),
        kind = "Plato fuerte", servings = 4, prepMinutes = 90, cookMinutes = 15,
        ingredients = listOf(
            ing("3 tazas", "Harina de trigo", "BRV-112"),
            ing("1 sobre (7 g)", "Levadura seca"), // no la vendemos
            ing("1 taza", "Salsa para pizza", "BRV-108"),
            ing("½ lb", "Queso mozzarella", "BRV-202"),
            ing("Al gusto", "Salami, jamón o chorizo", "BRV-389", "BRV-005", optional = true),
            ing("2 cucharadas", "Aceite de oliva", "BRV-039"),
            ing("1 cucharadita", "Orégano", "BRV-243", pantry = true),
            ing("1 cucharadita", "Azúcar", "BRV-117", pantry = true),
            SAL
        ),
        steps = listOf(
            "Disuelve la levadura y el azúcar en 1 taza de agua tibia; espera 10 minutos a que espume.",
            "Mezcla con la harina, la sal y el aceite; amasa 10 minutos hasta tener una masa lisa.",
            "Deja reposar tapada en un lugar tibio hasta que doble su tamaño, alrededor de 1 hora.",
            "Precalienta el horno a 230 °C (450 °F). Estira la masa en una bandeja engrasada.",
            "Cubre con salsa, queso, los ingredientes que quieras y orégano; hornea 12–15 minutos."
        )
    ),
    Recipe(
        id = "hamburguesas", name = "Hamburguesas caseras",
        aliases = listOf("hamburguesa", "hamburguesas", "burger", "burgers"),
        kind = "Plato fuerte", servings = 4, prepMinutes = 10, cookMinutes = 15,
        ingredients = listOf(
            ing("4", "Hamburguesas de res", "BRV-134", "BRV-321"),
            ing("4", "Panes de hamburguesa", "BRV-222", "BRV-445"),
            ing("4 lonjas", "Queso cheddar", "BRV-095"),
            ing("4 hojas", "Lechuga", "BRV-091", "BRV-189"),
            ing("1", "Tomate", "BRV-168"),
            ing("½", "Cebolla", "BRV-089", optional = true),
            ing("Al gusto", "Ketchup y mostaza", "BRV-120", "BRV-250", optional = true),
            SAL
        ),
        steps = listOf(
            "Calienta un sartén o parrilla a fuego medio-alto.",
            "Cocina las hamburguesas 4–5 minutos por lado, hasta que estén bien cocidas por dentro.",
            "En el último minuto coloca el queso encima para que se derrita.",
            "Tuesta ligeramente los panes.",
            "Arma con lechuga, tomate, cebolla y salsas al gusto."
        )
    ),
    Recipe(
        id = "tacos", name = "Tacos de carne molida",
        aliases = listOf("tacos", "taco", "burritos", "quesadillas"),
        kind = "Plato fuerte", servings = 4, prepMinutes = 10, cookMinutes = 15,
        ingredients = listOf(
            ing("1.5 lb", "Carne molida de res", "BRV-078", "BRV-126"),
            ing("10", "Tortillas de harina", "BRV-444"),
            ing("1 taza", "Queso rallado", "BRV-095", "BRV-202"),
            ing("1", "Aguacate", "BRV-088", "BRV-178"),
            ing("1", "Tomate", "BRV-168"),
            ing("½ atado", "Cilantro", "BRV-176"),
            CEBOLLA, AJO, LIMON, SAL, ACEITE
        ),
        steps = listOf(
            "Sofríe la cebolla y el ajo en aceite 2 minutos.",
            "Agrega la carne molida con sal y cocina hasta que esté dorada y sin partes rosadas, unos 10 minutos.",
            "Calienta las tortillas en un sartén seco, 30 segundos por lado.",
            "Pica el tomate, el cilantro y el aguacate; exprime el limón.",
            "Rellena las tortillas con carne, queso y los vegetales."
        )
    ),
    Recipe(
        id = "panqueques", name = "Panqueques (hotcakes)",
        aliases = listOf("panqueques", "panqueque", "hotcakes", "pancakes", "panqueca"),
        kind = "Desayuno", servings = 4, prepMinutes = 10, cookMinutes = 15,
        ingredients = listOf(
            ing("1½ tazas", "Harina de trigo", "BRV-112"),
            ing("1¼ tazas", "Leche", "BRV-098"),
            ing("1", "Huevo", "BRV-096"),
            ing("3 cucharadas", "Mantequilla derretida", "BRV-100"),
            ing("2 cucharadas", "Azúcar", "BRV-117"),
            ing("3½ cucharaditas", "Polvo de hornear"), // no lo vendemos
            ing("Al gusto", "Miel", "BRV-033", optional = true),
            ing("Una pizca", "Sal", "BRV-115", pantry = true)
        ),
        steps = listOf(
            "Mezcla la harina, el polvo de hornear, el azúcar y la sal.",
            "Aparte, bate la leche, el huevo y la mantequilla derretida.",
            "Une ambas mezclas sin batir demasiado; está bien si quedan grumitos.",
            "Vierte ¼ de taza de mezcla en un sartén engrasado a fuego medio; voltea cuando salgan burbujas, unos 2 minutos por lado.",
            "Sirve con miel."
        )
    ),
    Recipe(
        id = "camarones_ajillo", name = "Camarones al ajillo",
        aliases = listOf("camarones al ajillo", "camarones", "camarones con ajo"),
        kind = "Plato fuerte", servings = 4, prepMinutes = 10, cookMinutes = 10,
        ingredients = listOf(
            ing("1 lb", "Camarones pelados", "BRV-334", "BRV-142"),
            ing("8 dientes", "Ajo laminado", "BRV-171"),
            ing("¼ taza", "Aceite de oliva", "BRV-039"),
            ing("2 cucharadas", "Mantequilla", "BRV-100", optional = true),
            ing("Una pizca", "Pimienta cayena", "BRV-260", optional = true),
            ing("Unas ramitas", "Cilantro o perejil", "BRV-176", optional = true),
            LIMON, SAL
        ),
        steps = listOf(
            "Seca los camarones y sazónalos con sal.",
            "Calienta el aceite de oliva a fuego medio y dora el ajo laminado 1 minuto, sin quemarlo.",
            "Sube el fuego, agrega los camarones y la cayena; cocina 2 minutos por lado, hasta que estén rosados.",
            "Añade la mantequilla y el jugo de limón; mezcla 30 segundos.",
            "Termina con cilantro picado y sirve enseguida."
        )
    ),
    Recipe(
        id = "salmon_horno", name = "Salmón al horno",
        aliases = listOf("salmon", "salmon al horno", "salmon horneado"),
        kind = "Plato fuerte", servings = 2, prepMinutes = 10, cookMinutes = 15,
        ingredients = listOf(
            ing("1 lb", "Filete de salmón", "BRV-333", "BRV-141"),
            ing("2 cucharadas", "Aceite de oliva", "BRV-039"),
            ing("2 dientes", "Ajo", "BRV-171"),
            ing("½ cucharadita", "Pimienta negra", "BRV-244", pantry = true),
            LIMON, SAL
        ),
        steps = listOf(
            "Precalienta el horno a 200 °C (400 °F).",
            "Coloca el salmón en una bandeja con papel de hornear.",
            "Úntalo con aceite de oliva, ajo picado, sal, pimienta y jugo de limón.",
            "Hornea 12–15 minutos, hasta que se separe fácilmente con un tenedor."
        ),
        serveWith = "Vegetales al vapor o arroz."
    ),
    Recipe(
        id = "ensalada_verde", name = "Ensalada verde dominicana",
        aliases = listOf("ensalada", "ensalada verde", "ensalada de lechuga y tomate"),
        kind = "Acompañante", servings = 4, prepMinutes = 10, cookMinutes = 0,
        ingredients = listOf(
            ing("1", "Lechuga", "BRV-189", "BRV-091"),
            ing("1 lb", "Tomate", "BRV-168"),
            ing("1", "Pepino", "BRV-193", optional = true),
            ing("½", "Cebolla roja en aros", "BRV-089", optional = true),
            ing("1", "Aguacate", "BRV-088", "BRV-178", optional = true),
            ing("2 cucharadas", "Aceite de oliva", "BRV-039", pantry = true),
            ing("1 cucharada", "Vinagre", "BRV-040", pantry = true),
            SAL
        ),
        steps = listOf(
            "Lava y corta la lechuga, el tomate, el pepino y la cebolla.",
            "Mezcla en un bol y agrega el aguacate en lascas.",
            "Aliña justo antes de servir con aceite, vinagre y sal."
        )
    )
)

/** Busca la receta que la persona pidió por su nombre o alias. Prefiere el alias más largo. */
fun findRecipeForQuery(normalizedQuery: String): Recipe? {
    if (normalizedQuery.isBlank()) return null
    return BRAV_IA_RECIPES
        .flatMap { r -> r.aliases.map { alias -> r to alias } }
        .filter { (_, alias) -> Regex("(^|\\W)${Regex.escape(alias)}(\\W|$)").containsMatchIn(normalizedQuery) }
        .maxByOrNull { (_, alias) -> alias.length }
        ?.first
}

fun findRecipeById(id: String?): Recipe? = id?.let { wanted -> BRAV_IA_RECIPES.firstOrNull { it.id == wanted } }
