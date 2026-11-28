import Foundation

/*
 * Motor de anclaje de Brav IA (traducido de BravIaEngine.kt).
 *
 * El modelo solo puede conversar, elegir productos por SKU de una lista que le damos y elegir una
 * receta verificada por su id. Precios, totales, presupuesto, ingredientes y pasos los pone la app
 * desde el catálogo. Lo que el modelo escriba de más (precios, calorías, SKUs) se borra del texto.
 */

enum StoreFacts {
    static let hours = "Lunes a sábado de 7:00 AM a 10:00 PM. Los domingos está cerrado."
    // Reglas oficiales del Bravísimo Club (superbravo.com.do/bravisimo-club)
    static let bravisima = "Con la membresía Bravísima ganas 1 punto por cada RD$100 de compra; cada punto vale RD$1 y se canjea desde 100 puntos. Los puntos de un año vencen el 31 de marzo del año siguiente."
    static let ncf = "Se emiten comprobantes fiscales B01 (crédito fiscal, con RNC) y B02 (consumidor final)."
    static let support = "Los reportes de productos dañados o vencidos se registran y los revisa el equipo de servicio al cliente. Brav IA no aprueba reembolsos, reposiciones ni notas de crédito."
    static let all = [hours, bravisima, ncf, support]
}

enum BravIaText {
    private static let accents: [Character: Character] = [
        "á": "a", "é": "e", "í": "i", "ó": "o", "ú": "u", "ü": "u", "ñ": "n",
        "à": "a", "è": "e", "ì": "i", "ò": "o", "ù": "u"
    ]

    /// Minúsculas, sin tildes y sin signos: "¿Cómo hago Mangú?" → "como hago mangu".
    static func normalize(_ input: String) -> String {
        var out = ""
        out.reserveCapacity(input.count)
        for c in input.lowercased() {
            let m = accents[c] ?? c
            out.append(m.isLetter || m.isNumber ? m : " ")
        }
        return out.split(separator: " ", omittingEmptySubsequences: true).joined(separator: " ")
    }

    private static let stopwords: Set<String> = [
        "que", "como", "para", "con", "una", "uno", "unos", "unas", "los", "las", "del", "por",
        "hago", "hacer", "quiero", "dame", "tienes", "tienen", "necesito", "puedo", "receta",
        "recetas", "algo", "mas", "muy", "este", "esta", "esto", "mis", "tus", "sus", "hay",
        "precio", "cuanto", "cuesta", "favor", "hola", "buenas", "brav", "bravo", "comprar"
    ]

    static func keywords(_ normalized: String) -> [String] {
        var seen = Set<String>()
        return normalized.split(separator: " ").map(String.init)
            .filter { $0.count >= 3 && !stopwords.contains($0) && seen.insert($0).inserted }
    }

    /// "platanos" ≈ "platano"
    static func stem(_ w: String) -> String { w.count > 4 ? String(w.dropLast()) : w }
}

struct GroundedItem: Hashable {
    let sku: String
    var qty: Int
    var reason: String = ""
}

struct SuggestionDraft: Hashable {
    let subject: String
    let summary: String
}

struct RawAiReply {
    let answer: String
    let products: [GroundedItem]
    let recipeId: String?
    let question: String?
    let suggestion: SuggestionDraft?
}

/// Respuesta final: todo lo que hay aquí se puede mostrar sin miedo a datos inventados.
struct GroundedReply {
    var text: String
    var items: [GroundedItem]
    var recipe: Recipe?
    var suggestion: SuggestionDraft?
    var total: Int
    var budgetExceededBy: Int?
    var recipeMultiplier: Int = 1
}

struct ChatTurn {
    let fromUser: Bool
    let text: String
}

struct PromptContext {
    var query: String
    var storeName: String
    var branch: String
    var etaMinutes: Int?
    var candidates: [Product]
    var matchedRecipe: Recipe?
    var history: [ChatTurn]
    var cartSummary: String?
    var budget: Int?
    var frequentProducts: [String]
    var hasImage: Bool
}

enum BravIaEngine {
    static let maxQty = 20
    static var recipes: [Recipe] { Catalog.shared.recipes }

    static let systemInstruction = """
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
    \(StoreFacts.all.map { "- \($0)" }.joined(separator: "\n"))
    """

    static let responseSchema = """
    {"type":"object","properties":{"respuesta":{"type":"string"},"productos":{"type":"array","items":{"type":"object","properties":{"sku":{"type":"string"},"cantidad":{"type":"integer","minimum":1,"maximum":\(maxQty)},"motivo":{"type":"string"}},"required":["sku","cantidad"]}},"receta_id":{"type":"string"},"pregunta":{"type":"string"},"sugerencia":{"type":"object","properties":{"asunto":{"type":"string"},"resumen":{"type":"string"}}}},"required":["respuesta","productos"]}
    """

    // MARK: 1. Qué productos le enseñamos al modelo

    static func selectCandidates(query: String, storeProducts: [Product], recipe: Recipe?,
                                 extraSkus: [String] = [], limit: Int = 60) -> [Product] {
        let keywords = BravIaText.keywords(BravIaText.normalize(query))
        let recipeSkus = Set(recipe?.ingredients.flatMap(\.skus) ?? [])
        let extra = Set(extraSkus)
        let scored: [(Product, Int)] = storeProducts.map { p in
            let name = BravIaText.normalize(p.name)
            let hay = BravIaText.normalize(p.name + " " + p.category)
            var score = 0
            for k in keywords {
                let s = BravIaText.stem(k)
                if hay.contains(s) { score += name.contains(s) ? 10 : 4 }
            }
            if recipeSkus.contains(p.sku) { score += 100 }
            if extra.contains(p.sku) { score += 6 }
            if score > 0 && p.isBravo { score += 1 }
            return (p, score)
        }
        let picked = scored.filter { $0.1 > 0 }
            .enumerated().sorted { a, b in a.element.1 != b.element.1 ? a.element.1 > b.element.1 : a.offset < b.offset }
            .prefix(limit).map { $0.element.0 }
        if picked.count >= 12 { return picked }

        let pickedSkus = Set(picked.map(\.sku))
        let categories = Set(picked.map(\.category))
        let fillerCount = max(limit / 2, 12) - picked.count
        let filler = storeProducts.filter { !pickedSkus.contains($0.sku) }
            .enumerated().sorted { a, b in
                let (x, y) = (a.element, b.element)
                let cx = categories.contains(x.category), cy = categories.contains(y.category)
                if cx != cy { return cx }
                if x.isBravo != y.isBravo { return x.isBravo }
                if x.price != y.price { return x.price < y.price }
                return a.offset < b.offset
            }
            .prefix(max(fillerCount, 0)).map(\.element)
        return picked + filler
    }

    // MARK: 2. El mensaje que recibe el modelo

    static func buildPrompt(_ ctx: PromptContext) -> String {
        var s = "TIENDA: \(ctx.storeName) — sucursal \(ctx.branch).\n"
        if let eta = ctx.etaMinutes { s += "TIEMPO ESTIMADO DE ENTREGA HOY: unos \(eta) minutos (calculado por la app).\n" }
        if let b = ctx.budget { s += "PRESUPUESTO DEL CLIENTE: RD$\(b) (la app comprueba la suma; elige productos sensatos para ese monto).\n" }
        if let c = ctx.cartSummary { s += "CARRITO ACTUAL: \(c)\n" }
        if !ctx.frequentProducts.isEmpty { s += "COMPRA HABITUALMENTE: \(ctx.frequentProducts.joined(separator: ", "))\n" }
        if ctx.hasImage { s += "EL CLIENTE ADJUNTÓ UNA IMAGEN.\n" }
        s += "\n"
        if !ctx.history.isEmpty {
            s += "CONVERSACIÓN RECIENTE:\n"
            for t in ctx.history {
                s += (t.fromUser ? "Cliente: " : "Brav IA: ") + String(t.text.prefix(400)).replacingOccurrences(of: "\n", with: " ") + "\n"
            }
            s += "\n"
        }
        if let r = ctx.matchedRecipe { s += "RECETA VERIFICADA: id=\"\(r.id)\" (\(r.name)). Usa este id en receta_id.\n" }
        s += "RECETARIO (ids válidos): " + recipes.map { "\($0.id)=\($0.name)" }.joined(separator: ", ") + "\n\n"
        s += "PRODUCTOS DISPONIBLES (SKU | nombre | presentación):\n"
        for p in ctx.candidates { s += "\(p.sku) | \(p.name) | \(p.unit)\n" }
        s += "\nMENSAJE DEL CLIENTE: " + ctx.query
        return s
    }

    // MARK: 3. Leer la respuesta

    /// Lee el JSON del modelo aunque venga dentro de ```json … ``` o con texto alrededor.
    static func parseReply(_ raw: String) -> RawAiReply? {
        guard let start = raw.firstIndex(of: "{"), let end = raw.lastIndex(of: "}"), start < end,
              let data = String(raw[start...end]).data(using: .utf8),
              let obj = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let answer = str(obj["respuesta"]) ?? str(obj["response"]) else { return nil }
        let products: [GroundedItem] = (obj["productos"] as? [Any] ?? []).compactMap { el in
            guard let o = el as? [String: Any],
                  let sku = str(o["sku"])?.trimmingCharacters(in: .whitespaces).uppercased() else { return nil }
            let qty: Int
            switch o["cantidad"] {
            case let n as NSNumber: qty = n.intValue
            case let t as String: qty = Int(t.trimmingCharacters(in: .whitespaces)) ?? 1
            default: qty = 1
            }
            return GroundedItem(sku: sku, qty: qty, reason: str(o["motivo"]) ?? "")
        }
        var suggestion: SuggestionDraft?
        if let s = obj["sugerencia"] as? [String: Any] {
            let subject = str(s["asunto"]) ?? "", summary = str(s["resumen"]) ?? ""
            if !(subject.isBlank && summary.isBlank) {
                suggestion = SuggestionDraft(subject: subject.isBlank ? "Sugerencia de cliente" : subject, summary: summary)
            }
        }
        return RawAiReply(answer: answer, products: products,
                          recipeId: str(obj["receta_id"]).flatMap { $0.isBlank || $0 == "null" ? nil : $0 },
                          question: str(obj["pregunta"]).flatMap { $0.isBlank || $0 == "null" ? nil : $0 },
                          suggestion: suggestion)
    }

    private static func str(_ v: Any?) -> String? {
        switch v {
        case let s as String: return s
        case let n as NSNumber: return n.stringValue
        default: return nil
        }
    }

    /// Mientras el modelo escribe el JSON, saca lo que va del campo "respuesta" para mostrarlo en vivo.
    static func streamingAnswer(_ partial: String) -> String? {
        guard let r = partial.range(of: #""respuesta"\s*:\s*""#, options: .regularExpression) else { return nil }
        let chars = Array(partial[r.upperBound...])
        var out = ""
        var i = 0
        while i < chars.count {
            let c = chars[i]
            if c == "\\" {
                guard i + 1 < chars.count else { break }
                let n = chars[i + 1]
                switch n {
                case "n": out.append("\n")
                case "t": out.append(" ")
                case "u":
                    guard i + 5 < chars.count else { return out }
                    if let v = UInt32(String(chars[(i + 2)...(i + 5)]), radix: 16), let u = Unicode.Scalar(v) {
                        out.unicodeScalars.append(u)
                    }
                    i += 4
                default: out.append(n)
                }
                i += 2
                continue
            }
            if c == "\"" { break }
            out.append(c)
            i += 1
        }
        return out
    }

    // MARK: 4. Limpiar el texto

    private static let skuRx = rx(#"\s*[\[(]?\s*(?:SKU\s*:?\s*)?\b(?:BRV|ARC)-\d{3}\b\s*[\])]?"#)
    private static let moneyRx = rx(#"\s*[—–\-:]?\s*\(?\s*(?:RD\s?\$|\$|DOP\s?)\s*\d[\d.,]*(?:\s*(?:pesos|c/u|cada uno))?\s*\)?"#)
    private static let pesosRx = rx(#"\s*\(?\b\d[\d.,]*\s*pesos\b\)?"#)
    private static let kcalRx = rx(#"\s*[—–\-:]?\s*\(?\s*~?\d[\d.,]*\s*(?:kcal|calor[ií]as|cal\b)\s*\)?"#)
    private static let dataLineRx = rx(#"^\W*(?:subtotal|total|ahorro|margen|retorno|precio|costo|cashback)\b"#)

    /// Quita del texto los SKUs, montos en pesos y calorías que el modelo haya escrito.
    static func sanitize(_ text: String) -> String {
        let lines: [String] = text.replacingOccurrences(of: "\r", with: "")
            .components(separatedBy: "\n").compactMap { original in
                let hadData = moneyRx.matches(original) || kcalRx.matches(original) || pesosRx.matches(original)
                var line = original
                for r in [skuRx, moneyRx, pesosRx, kcalRx] { line = r.replace(line, with: "") }
                line = rx(#"\(\s*\)"#).replace(line, with: "")
                line = rx(#"\s*[—–]\s*(?=[—–,.;]|$)"#).replace(line, with: "")
                line = rx(" {2,}").replace(line, with: " ")
                while line.last?.isWhitespace == true { line.removeLast() }
                let visible = rx(#"[*_•#>\-\s:]"#).replace(line, with: "")
                if hadData && dataLineRx.matches(line) { return nil }
                if hadData && visible.count < 3 { return nil }
                return line
            }
        return rx("\n{3,}").replace(lines.joined(separator: "\n"), with: "\n\n")
            .trimmingCharacters(in: .whitespacesAndNewlines)
    }

    // MARK: 5. Validar contra el catálogo

    static func ground(_ reply: RawAiReply, lookup: (String) -> Product?, storePrefix: String,
                       matchedRecipe: Recipe?, budget: Int?, people: Int? = nil) -> GroundedReply {
        let recipe = matchedRecipe ?? Self.recipe(id: reply.recipeId)
        let multiplier = (recipe != nil && people != nil) ? multiplierFor(recipe!, people: people!) : 1
        let modelItems = mergeItems(reply.products.filter { $0.sku.hasPrefix("\(storePrefix)-") && lookup($0.sku) != nil })
        let items: [GroundedItem]
        if let recipe, storePrefix == "BRV" {
            let fromRecipe = recipeCartItems(recipe, lookup: lookup, multiplier: multiplier)
            items = mergeItems(fromRecipe + modelItems.filter { m in !fromRecipe.contains { $0.sku == m.sku } })
        } else {
            items = modelItems
        }
        var text = sanitize(reply.answer)
        if let recipe, text.isBlank { text = "Aquí tienes la receta verificada de **\(recipe.name)**." }
        if let q = reply.question { text = text.isBlank ? sanitize(q) : text + "\n\n" + sanitize(q) }
        let total = totalOf(items, lookup: lookup)
        return GroundedReply(text: text, items: items, recipe: recipe, suggestion: reply.suggestion, total: total,
                             budgetExceededBy: budget.flatMap { b in total > b ? total - b : nil },
                             recipeMultiplier: multiplier)
    }

    static func multiplierFor(_ recipe: Recipe, people: Int) -> Int {
        min(max((people + recipe.servings - 1) / recipe.servings, 1), 5)
    }

    private static let peopleRx = rx(#"\b(?:para|somos|seremos|de)\s+(\d{1,2})\s*(?:personas|persona|gente|comensales|invitados|porciones)\b"#)

    static func peopleFromQuery(_ query: String) -> Int? {
        guard let v = peopleRx.firstGroup(BravIaText.normalize(query)).flatMap(Int.init), (1...60).contains(v) else { return nil }
        return v
    }

    static func mergeItems(_ items: [GroundedItem]) -> [GroundedItem] {
        var order: [String] = []
        var bySku: [String: GroundedItem] = [:]
        for it in items {
            let qty = min(max(it.qty, 1), maxQty)
            if var prev = bySku[it.sku] {
                prev.qty = min(prev.qty + qty, maxQty)
                bySku[it.sku] = prev
            } else {
                order.append(it.sku)
                var copy = it
                copy.qty = qty
                bySku[it.sku] = copy
            }
        }
        return order.compactMap { bySku[$0] }
    }

    static func recipeCartItems(_ recipe: Recipe, lookup: (String) -> Product?, multiplier: Int = 1) -> [GroundedItem] {
        mergeItems(recipe.ingredients.filter { !$0.pantry && !$0.optional }.compactMap { ing in
            guard let sku = ing.skus.first(where: { lookup($0) != nil }) else { return nil }
            return GroundedItem(sku: sku, qty: unitsFor(ing, multiplier: multiplier), reason: ing.name)
        })
    }

    /// Solo crece lo que se compra por unidad o por libra; un paquete que ya alcanza se queda en 1.
    static func unitsFor(_ ing: RecipeIngredient, multiplier: Int) -> Int {
        guard multiplier > 1 else { return ing.unitsToBuy }
        let byWeight = rx(#"\b(lb|libra|libras)\b"#).matches(BravIaText.normalize(ing.amount))
        return (ing.unitsToBuy > 1 || byWeight) ? ing.unitsToBuy * multiplier : ing.unitsToBuy
    }

    static func totalOf(_ items: [GroundedItem], lookup: (String) -> Product?) -> Int {
        items.reduce(0) { $0 + (lookup($1.sku)?.price ?? 0) * $1.qty }
    }

    static func recipe(id: String?) -> Recipe? {
        guard let id else { return nil }
        return recipes.first { $0.id == id }
    }

    static func recipesUsing(sku: String) -> [Recipe] {
        recipes.filter { r in r.ingredients.contains { $0.skus.contains(sku) } }
    }

    static func similarRecipes(_ query: String, max: Int = 3) -> [Recipe] {
        let words = BravIaText.keywords(BravIaText.normalize(query))
        guard !words.isEmpty else { return [] }
        return recipes.map { r -> (Recipe, Int) in
            let hay = BravIaText.normalize(r.name + " " + r.aliases.joined(separator: " ") + " " + r.ingredients.map(\.name).joined(separator: " "))
            return (r, words.filter { hay.contains(BravIaText.stem($0)) }.count)
        }
        .filter { $0.1 > 0 }
        .enumerated().sorted { a, b in a.element.1 != b.element.1 ? a.element.1 > b.element.1 : a.offset < b.offset }
        .prefix(max).map { $0.element.0 }
    }

    private static let ingredientLikeAliases: Set<String> = [
        "arroz", "pollo", "habichuelas", "habichuelas rojas", "frijoles", "avena", "chinola", "maracuya",
        "salmon", "camarones", "ensalada", "sopa", "espaguetis", "espagueti", "spaghetti", "bacalao", "chivo",
        "maduros", "pizza", "hamburguesa", "hamburguesas", "burger", "burgers", "tacos", "taco", "quesadillas", "burritos"
    ]
    private static let cookingIntent = rx(#"\b(receta|recetas|como (hago|hacer|se hace|preparo|preparar|cocino|cocinar)|preparar|preparo|cocinar|cocino|pasos|paso a paso|ingredientes para|que necesito para|hacer un|hacer una|hacer unos|hacer unas)\b"#)

    private static func aliasMatches(_ alias: String, in n: String) -> Bool {
        rx("(^|\\W)" + NSRegularExpression.escapedPattern(for: alias) + "(\\W|$)").matches(n)
    }

    /// Busca la receta por nombre o alias (prefiere el alias más largo). "Necesito arroz" es compra, no receta.
    static func matchRecipe(_ query: String) -> Recipe? {
        let n = BravIaText.normalize(query)
        guard !n.isEmpty else { return nil }
        let hits = recipes.flatMap { r in r.aliases.map { (r, $0) } }.filter { aliasMatches($0.1, in: n) }
        guard let best = hits.max(by: { $0.1.count < $1.1.count }) else { return nil }
        if ingredientLikeAliases.contains(best.1) && !cookingIntent.matches(n) { return nil }
        return best.0
    }

    // MARK: 6. Presupuesto escrito en el mensaje

    private static let budgetRx = rx(#"(?:presupuesto|con|por|de|hasta|maximo|menos de|no mas de)\s*(?:de\s*)?(?:rd\s*\$?|\$)?\s*(\d{1,3}(?:[.,]\d{3})+|\d{3,6})\s*(?:pesos|rd|\$)?"#)

    /// "algo para 4 personas con 1,500 pesos" → 1500. Solo cuenta si se habla de dinero.
    static func budgetFromQuery(_ query: String) -> Int? {
        let n = BravIaText.normalize(query.replacingOccurrences(of: "$", with: " $ "))
        let mentionsMoney = query.contains("$") || ["peso", "presupuesto", "rd ", "dinero", "gastar"].contains { n.contains($0) }
        guard mentionsMoney else { return nil }
        let lowered = query.lowercased().replacingOccurrences(of: "á", with: "a").replacingOccurrences(of: "ó", with: "o")
        guard let g = budgetRx.firstGroup(lowered),
              let v = Int(g.replacingOccurrences(of: ".", with: "").replacingOccurrences(of: ",", with: "")),
              (100...200_000).contains(v) else { return nil }
        return v
    }

    // MARK: 7. Respuesta local (sin conexión o sin clave)

    static func localReply(query: String, storeProducts: [Product], lookup: (String) -> Product?,
                           storePrefix: String, budget: Int?, hasImage: Bool) -> GroundedReply {
        let n = BravIaText.normalize(query)
        if storePrefix == "BRV", let recipe = matchRecipe(query) {
            return ground(RawAiReply(answer: "Aquí tienes la receta verificada de **\(recipe.name)**.", products: [],
                                     recipeId: recipe.id, question: nil, suggestion: nil),
                          lookup: lookup, storePrefix: storePrefix, matchedRecipe: recipe, budget: budget,
                          people: peopleFromQuery(query))
        }
        let fact: String? = {
            if hasImage { return "Ahora mismo no puedo analizar imágenes porque Brav IA está sin conexión. Inténtalo de nuevo en un momento; tu foto no se ha enviado." }
            if ["horario", "abren", "cierran", "abierto", "domingo"].contains { n.contains($0) } { return StoreFacts.hours }
            if ["bravisima", "puntos", "cashback"].contains { n.contains($0) } { return StoreFacts.bravisima }
            if ["ncf", "b01", "b02", "comprobante", "factura", "rnc"].contains { n.contains($0) } { return StoreFacts.ncf }
            if ["danad", "vencid", "reembolso", "devolucion", "reclamo", "queja", "defectuos"].contains { n.contains($0) } { return StoreFacts.support }
            return nil
        }()
        if let fact { return GroundedReply(text: fact, items: [], recipe: nil, suggestion: nil, total: 0, budgetExceededBy: nil) }

        let keywords = BravIaText.keywords(n)
        let matches = keywords.isEmpty ? [] : selectCandidates(query: query, storeProducts: storeProducts, recipe: nil, limit: 6)
            .filter { p in
                let h = BravIaText.normalize(p.name + " " + p.category)
                return keywords.contains { h.contains(BravIaText.stem($0)) }
            }
        let similar = storePrefix == "BRV" ? similarRecipes(query) : []
        let q = String(query.trimmingCharacters(in: .whitespaces).prefix(60))
        var text = ""
        if cookingIntent.matches(n) && storePrefix == "BRV" {
            text += "Esa receta no está en mi recetario verificado, así que no te doy medidas ni pasos inventados. "
        }
        text += matches.isEmpty
            ? "No encontré productos que coincidan con «\(q)». Prueba con otro nombre o revisa el catálogo."
            : "Esto encontré en el catálogo para «\(q)»."
        if !similar.isEmpty {
            text += "\n\nRecetas verificadas que te pueden servir: " + similar.map { "**\($0.name)**" }.joined(separator: ", ") + "."
        }
        let items = matches.map { GroundedItem(sku: $0.sku, qty: 1) }
        let total = totalOf(items, lookup: lookup)
        return GroundedReply(text: text, items: items, recipe: nil, suggestion: nil, total: total,
                             budgetExceededBy: budget.flatMap { b in total > b ? total - b : nil })
    }

    /// Respuesta de un modelo que no devolvió JSON: se usa el texto y los SKUs que mencione.
    static func fromPlainText(_ raw: String) -> RawAiReply {
        let skus = rx(#"\b(?:BRV|ARC)-\d{3}\b"#).allMatches(raw).map { GroundedItem(sku: $0.uppercased(), qty: 1) }
        let text = raw.components(separatedBy: "\n")
            .filter { !$0.trimmingCharacters(in: .whitespaces).uppercased().hasPrefix("SKUS") }
            .joined(separator: "\n")
        return RawAiReply(answer: text, products: skus, recipeId: nil, question: nil, suggestion: nil)
    }
}

// MARK: - Ayudas de expresiones regulares

struct Rx {
    let re: NSRegularExpression
    func matches(_ s: String) -> Bool { re.firstMatch(in: s, range: NSRange(s.startIndex..., in: s)) != nil }
    func replace(_ s: String, with t: String) -> String {
        re.stringByReplacingMatches(in: s, range: NSRange(s.startIndex..., in: s), withTemplate: NSRegularExpression.escapedTemplate(for: t))
    }
    func firstGroup(_ s: String) -> String? {
        guard let m = re.firstMatch(in: s, range: NSRange(s.startIndex..., in: s)), m.numberOfRanges > 1,
              let r = Range(m.range(at: 1), in: s) else { return nil }
        return String(s[r])
    }
    func allMatches(_ s: String) -> [String] {
        re.matches(in: s, range: NSRange(s.startIndex..., in: s)).compactMap { Range($0.range, in: s).map { String(s[$0]) } }
    }
}

private var rxCache: [String: Rx] = [:]
private let rxLock = NSLock()

/// Expresión regular sin distinguir mayúsculas, cacheada.
func rx(_ pattern: String) -> Rx {
    rxLock.lock(); defer { rxLock.unlock() }
    if let r = rxCache[pattern] { return r }
    let r = Rx(re: try! NSRegularExpression(pattern: pattern, options: [.caseInsensitive, .anchorsMatchLines]))
    rxCache[pattern] = r
    return r
}

extension String {
    var isBlank: Bool { trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }
}
