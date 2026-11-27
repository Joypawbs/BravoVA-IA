package com.example.data

import com.example.model.Product

data class ProductRichDescription(
    val summary: String,
    val highlights: List<String>,
    val usageOrStorageTip: String
)

object ProductDescriptionProvider {

    fun describe(product: Product): ProductRichDescription {
        val nameLower = product.name.lowercase()
        val catLower = product.category.lowercase()
        val unitLabel = product.unit.ifBlank { "Unidad" }
        val isArca = product.sku.startsWith("ARC-")
        val brandLabel = when {
            isArca && product.isBravo -> "Selección El Arca"
            product.isBravo -> "Marca Exclusiva Bravo"
            product.badge.isNotBlank() -> product.badge
            else -> if (isArca) "El Arca Petshop" else "Marca Seleccionada"
        }

        val (summary, specHighlight, tip) = when {
            isArca -> buildArcaCopy(product, nameLower, catLower, unitLabel, brandLabel)
            else -> buildBravoCopy(product, nameLower, catLower, unitLabel, brandLabel)
        }

        val originHighlight = if (isArca) "Garantía Veterinaria El Arca" else "Calidad Certificada Bravo"
        return ProductRichDescription(
            summary = summary,
            highlights = listOf(brandLabel, "Presentación: $unitLabel", specHighlight, originHighlight),
            usageOrStorageTip = tip
        )
    }

    private fun buildBravoCopy(
        p: Product,
        n: String,
        cat: String,
        unit: String,
        brand: String
    ): Triple<String, String, String> {
        return when {
            // Pescadería y mariscos
            cat.contains("pescad") || n.contains("salmón") || n.contains("salmon") ||
                n.contains("dorado") || n.contains("chillo") || n.contains("mero") ||
                n.contains("camar") || n.contains("langost") || n.contains("pulpo") ||
                n.contains("calamar") || n.contains("almeja") || n.contains("mejill") ||
                n.contains("bacalao") || n.contains("tilapia") -> {
                val prep = when {
                    n.contains("ahumad") -> "ahumado lento en madera noble con textura sedosa y laminado fino"
                    n.contains("camar") || n.contains("langost") -> "limpieza rigurosa, sabor marino dulce y textura firme al punto"
                    n.contains("bacalao") -> "curado tradicional de textura firme, ideal tras desalado progresivo"
                    n.contains("filete") -> "corte limpio sin espinas de cocción rápida en sartén, horno o airfryer"
                    else -> "pieza fresca de pesquería seleccionada bajo estricto control de hielo escarchado"
                }
                Triple(
                    "${p.name} ($unit) de $brand: $prep. Empacado en sucursal Bravo para preservar su aroma oceánico y frescura natural.",
                    "Cadena fría 0°C–2°C",
                    "Conservación y uso: mantén entre 0°C y 2°C y consume dentro de las 48 horas; salpimienta con limón y ajo justo antes de sellar."
                )
            }

            // Embutidos y charcutería
            cat.contains("embutid") || p.artType == "salami" || n.contains("salami") ||
                n.contains("longaniza") || n.contains("chorizo") || n.contains("jamón") ||
                n.contains("jamon") || n.contains("tocineta") || n.contains("salchicha") ||
                n.contains("mortadela") || n.contains("pepperoni") -> {
                val style = when {
                    n.contains("longaniza") -> "receta criolla con naranja agria, orégano y ajo fresco molido"
                    n.contains("salami") -> "curado equilibrado de corte firme, clásico para fritura crujiente o guisos dominicanos"
                    n.contains("jamón") || n.contains("jamon") || n.contains("pavo") -> "rebanado uniforme bajo en exceso de humedad, perfecto para sándwiches y picaderas"
                    n.contains("tocineta") -> "ahumado uniforme con proporción balanceada de magro y veta dorada"
                    else -> "embutido sellado al vacío con especias seleccionadas y control térmico continuo"
                }
                Triple(
                    "${p.name} en presentación de $unit ($brand): $style. Textura consistente y frescura verificada en cámara frigorífica.",
                    "Sellado higiénico 0°C–4°C",
                    "Conservación y uso: refrigera entre 0°C y 4°C; una vez abierto, envuelve herméticamente y consume en un máximo de 5 días."
                )
            }

            // Carnes res, cerdo y aves
            cat.contains("carne") || n.contains("angus") || n.contains("churrasco") ||
                n.contains("picaña") || n.contains("picana") || n.contains("ribeye") ||
                n.contains("filete de res") || n.contains("chuleta") || n.contains("cerdo") ||
                n.contains("pollo") || n.contains("muslo") || n.contains("pechuga") ||
                n.contains("costilla") || n.contains("molida") || n.contains("mondongo") ||
                n.contains("patitas") -> {
                val cutNote = when {
                    n.contains("angus") || n.contains("ribeye") || n.contains("churrasco") || n.contains("picaña") ->
                        "corte con marmoleo natural y maduración controlada para máxima jugosidad a la parrilla"
                    n.contains("chuleta") || n.contains("costilla") || n.contains("cerdo") ->
                        "porcionado uniforme de carne tierna ideal para brasa, horno o caldero criollo"
                    n.contains("pollo") || n.contains("pechuga") || n.contains("muslo") || n.contains("alita") ->
                        "ave fresca procesada el mismo día sin hormonas añadidas, lista para sazonar"
                    n.contains("molida") ->
                        "molienda fresca en frío con balance preciso entre carne magra y sabor"
                    else ->
                        "limpieza esmerada en carnicería certificada Bravo para cocción lenta o tradicional"
                }
                Triple(
                    "${p.name} ($unit) seleccionado por maestros carniceros ($brand): $cutNote.",
                    "Corte fresco del día",
                    "Conservación y uso: mantén refrigerado a 0°C–4°C (o congela a -18°C); atempera 15 minutos antes de llevar al fuego."
                )
            }

            // Lácteos, quesos y huevos
            cat.contains("lácteo") || cat.contains("lacteo") || n.contains("queso") ||
                n.contains("leche") || n.contains("yogur") || n.contains("mantequilla") ||
                n.contains("huevo") || n.contains("crema") -> {
                val dairyNote = when {
                    n.contains("queso") -> "pasta uniforme, aroma lácteo auténtico y excelente fundido o corte en lonjas"
                    n.contains("yogur") -> "fermentación suave con cultivos activos y textura cremosa sin grumos"
                    n.contains("huevo") -> "huevos frescos de granja clasificados por tamaño y cáscara limpia resistente"
                    n.contains("mantequilla") -> "batido tradicional de crema pura con punto exacto para untar o repostería"
                    else -> "lácteo pasteurizado de alta rotación diaria con aporte natural de calcio y proteína"
                }
                Triple(
                    "${p.name} ($unit) de $brand: $dairyNote.",
                    "Rotación diaria refrigerada",
                    "Conservación y uso: refrigera entre 2°C y 5°C (leches UHT en lugar fresco hasta abrir); mantén cerrado tras cada porción."
                )
            }

            // Congelados y helados
            cat.contains("congelado") || cat.contains("helado") -> {
                val frozenNote = when {
                    n.contains("helado") || n.contains("paleta") || n.contains("sorbete") ->
                        "textura cremosa sin cristales de hielo, elaborado con base láctea o fruta real"
                    else ->
                        "ultracongelación rápida (IQF) que bloquea sabor, crujencia y nutrientes listos en minutos"
                }
                Triple(
                    "${p.name} ($unit) de $brand: $frozenNote.",
                    "Cadena -18°C continua",
                    "Conservación y uso: guarda directamente a -18°C; cocina sin descongelar en horno, sartén o freidora de aire según empaque."
                )
            }

            // Panadería y galletas
            cat.contains("pan") || cat.contains("galleta") || n.contains("casabe") ||
                n.contains("bizcocho") || n.contains("croissant") || n.contains("tostada") -> {
                val bakeryNote = when {
                    n.contains("casabe") -> "tostado crujiente de yuca artesanal dominicana, naturalmente libre de gluten"
                    n.contains("galleta") -> "horneado dorado de textura crocante en empaque resellable contra humedad"
                    else -> "miga tierna y corteza equilibrada horneada con harinas seleccionadas"
                }
                Triple(
                    "${p.name} ($unit) de $brand: $bakeryNote, ideal para desayunos, meriendas y acompañamientos.",
                    "Horneado fresco Bravo",
                    "Conservación y uso: almacena en lugar fresco y seco dentro de su bolsa cerrada; tuesta ligeramente para reavivar su aroma."
                )
            }

            // Frutas y vegetales
            cat.contains("fruta") || cat.contains("vegetal") || n.contains("plátano") ||
                n.contains("platano") || n.contains("aguacate") || n.contains("mango") ||
                n.contains("yuca") || n.contains("papa") || n.contains("tomate") ||
                n.contains("cebolla") || n.contains("ajo") || n.contains("limón") -> {
                Triple(
                    "${p.name} ($unit) de cosecha seleccionada ($brand): clasificación manual pieza por pieza en su punto óptimo de firmeza, color y sabor tropical.",
                    "Cosecha fresca local",
                    "Conservación y uso: lava con agua potable justo antes de consumir; mantén raíces/tubérculos en sitio ventilado y hojas verdes refrigeradas."
                )
            }

            // Aceites y vinagres
            cat.contains("aceite") || cat.contains("vinagre") -> {
                val oilNote = if (n.contains("vinagre")) {
                    "acidez balanceada y fermentación limpia ideal para aderezos, escabeches y lavado culinario"
                } else {
                    "extracción cuidada y alto punto de humo para salteados, frituras ligeras y aderezos en frío"
                }
                Triple(
                    "${p.name} ($unit) de $brand: $oilNote.",
                    "Pureza y rendimiento",
                    "Conservación y uso: mantén bien tapado lejos de la luz directa y fuentes de calor para preservar su perfil aromático."
                )
            }

            // Agua y refrescos
            cat.contains("agua") || cat.contains("refresco") || n.contains("jugo") ||
                n.contains("malta") || n.contains("energ") -> {
                Triple(
                    "${p.name} ($unit) de $brand: bebida refrescante lista para servir bien fría, con sellado inviolable y burbujeo o pureza mineral verificada.",
                    "Hidratación al instante",
                    "Conservación y uso: refrigera entre 4°C y 8°C antes de servir para disfrutar su punto óptimo de frescor."
                )
            }

            // Bodega (vinos, cervezas, rones)
            cat.contains("bodega") || n.contains("vino") || n.contains("cerveza") ||
                n.contains("ron") || n.contains("cava") || n.contains("sangr") ||
                n.contains("whisky") -> {
                Triple(
                    "${p.name} ($unit) de Bodega Bravo ($brand): guarda controlada con equilibrio aromático para maridajes, celebraciones y sobremesa. Venta exclusiva +18.",
                    "Cava climatizada +18",
                    "Servicio sugerido: sirve blancos, espumosos y cervezas entre 4°C y 8°C; tintos reserva entre 14°C y 17°C."
                )
            }

            // Alimentación general / despensa dominicana
            cat.contains("alimentación") || cat.contains("alimentacion") || cat.contains("nuevo") ||
                n.contains("arroz") || n.contains("habichuela") || n.contains("pasta") ||
                n.contains("café") || n.contains("cafe") || n.contains("sazón") ||
                n.contains("sazon") || n.contains("miel") || n.contains("coco") ||
                n.contains("harina") || n.contains("azúcar") || n.contains("azucar") -> {
                val pantryNote = when {
                    n.contains("arroz") -> "grano selecto entero de cocción suelta y rendimiento constante en caldero"
                    n.contains("habichuela") || n.contains("gandul") -> "legumbre calibrada de piel tierna y ablandado uniforme para guisos tradicionales"
                    n.contains("café") || n.contains("cafe") -> "tueste aromático de altura con notas tostadas intensas y molienda pareja"
                    n.contains("miel") -> "miel pura de abeja con aroma floral natural y viscosidad densa sin azúcares añadidos"
                    n.contains("coco") -> "pulpa de coco tropical de consistencia rica para habichuelas con dulce, arroces o postres"
                    else -> "ingrediente esencial de despensa con trazabilidad de lote y sellado contra humedad"
                }
                Triple(
                    "${p.name} ($unit) de $brand: $pantryNote.",
                    "Despensa certificada",
                    "Conservación y uso: guarda en alacena fresca y seca; tras abrir envases secos o especias, cierra herméticamente."
                )
            }

            // Snacks y dulces
            cat.contains("snack") || cat.contains("dulce") || n.contains("chocolate") ||
                n.contains("maní") || n.contains("mani") || n.contains("papas") ||
                n.contains("platanito") || n.contains("chicharr") || n.contains("almendra") -> {
                Triple(
                    "${p.name} ($unit) de $brand: bocadillo crocante o confite balanceado, sellado en atmósfera protectora para conservar su textura crujiente desde el primer bocado.",
                    "Empaque crujiente sellado",
                    "Conservación y uso: mantén en lugar seco a temperatura ambiente (evita sol directo en chocolates y frutos secos)."
                )
            }

            // Cuidado personal
            cat.contains("cuidado personal") || n.contains("pasta dental") || n.contains("enjuague") ||
                n.contains("cepillo") || n.contains("champ") || n.contains("shampoo") ||
                n.contains("desodorante") || n.contains("jabón") || n.contains("jabon") ||
                n.contains("gotero") || n.contains("protector") || n.contains("crema") -> {
                Triple(
                    "${p.name} ($unit) de $brand: fórmula dermatológicamente probada para higiene, protección y rutina diaria personal con textura de rápida absorción o limpieza suave.",
                    "Cuidado diario probado",
                    "Modo de uso: aplica la cantidad recomendada sobre piel, cabello o zona bucal limpia; evita contacto directo con los ojos."
                )
            }

            // Bebés
            cat.contains("bebé") || cat.contains("bebe") -> {
                Triple(
                    "${p.name} ($unit) de $brand: formulado especialmente con ingredientes suaves e hipoalergénicos pensando en el confort y la piel delicada de tu bebé.",
                    "Suavidad hipoalergénica",
                    "Uso y cuidado: revisa siempre talla o etapa recomendada y conserva en espacio limpio y seco fuera del alcance infantil."
                )
            }

            // Hogar y limpieza
            else -> {
                Triple(
                    "${p.name} ($unit) de $brand: desempeño concentrado para limpieza profunda, desinfección y orden del hogar con máximo rendimiento por aplicación.",
                    "Alto rendimiento hogar",
                    "Uso seguro: sigue la dosificación sugerida en etiqueta y mantén envases cerrados en área ventilada fuera del alcance de niños."
                )
            }
        }
    }

    private fun buildArcaCopy(
        p: Product,
        n: String,
        cat: String,
        unit: String,
        brand: String
    ): Triple<String, String, String> {
        return when {
            cat.contains("seco") || p.artType == "pet_kibble" -> {
                val petTarget = when {
                    n.contains("gato") || n.contains("kitten") -> "felinos con taurina esencial y control urinario"
                    n.contains("cachorro") || n.contains("puppy") -> "cachorros con DHA, calcio y proteína de alta digestibilidad"
                    else -> "perros con prebióticos, omega 3 y 6 y croqueta de palatabilidad superior"
                }
                Triple(
                    "${p.name} ($unit) — $brand en El Arca Petshop: nutrición completa para $petTarget, formulada para energía diaria y pelaje brillante.",
                    "Nutrición balanceada",
                    "Guía veterinaria: divide la ración diaria en 2 tomas según peso y mantén agua fresca disponible; haz transición gradual en 7 días."
                )
            }
            cat.contains("húmedo") || cat.contains("humedo") || cat.contains("snack") ||
                p.artType == "pet_wet" || p.artType == "pet_snack" -> {
                Triple(
                    "${p.name} ($unit) — $brand: receta jugosa o premio funcional de alta aceptación para hidratación extra, adiestramiento positivo y refuerzo digestivo.",
                    "Alta palatabilidad",
                    "Guía veterinaria: ofrece como complemento o premio (máx. 10% de calorías diarias) y refrigera la porción sobrante tras abrir."
                )
            }
            cat.contains("farmacia") || cat.contains("salud") || p.artType == "pet_health" -> {
                Triple(
                    "${p.name} ($unit) — $brand en El Arca VetCare: soporte clínico y preventivo formulado bajo estándares veterinarios para protección antiparasitaria, dermatológica o articular.",
                    "Respaldo VetCare",
                    "Guía veterinaria: respeta dosificación exacta por peso y especie indicada en caja o consulta a nuestro veterinario en Brav IA."
                )
            }
            cat.contains("ave") || cat.contains("pez") || cat.contains("exótic") || cat.contains("exotic") -> {
                Triple(
                    "${p.name} ($unit) — $brand: producto especializado para hábitat, nutrición y bienestar de aves ornamentales, peces tropicales, tortugas y pequeños mamíferos.",
                    "Especialidad exóticos",
                    "Guía veterinaria: verifica dosis por volumen de agua o peso de tu mascota exótica y mantén el hábitat limpio y ventilado."
                )
            }
            cat.contains("arena") || cat.contains("hogar") -> {
                Triple(
                    "${p.name} ($unit) — $brand: control superior de olores, aglomeración rápida o absorción higiénica libre de polvo excesivo para mantener fresco el hogar.",
                    "Control total de olor",
                    "Guía práctica: retira residuos diariamente y renueva la bandeja o paño según frecuencia de uso de tus mascotas."
                )
            }
            cat.contains("juguete") || p.artType == "pet_toy" -> {
                Triple(
                    "${p.name} ($unit) — $brand: diseño ergonómico y materiales seguros libres de tóxicos para estimular el instinto natural, reducir estrés y cuidar la dentadura.",
                    "Material Pet-Safe",
                    "Guía de uso: supervisa el juego activo y revisa periódicamente el estado del juguete acorde al nivel de mordida."
                )
            }
            else -> {
                Triple(
                    "${p.name} ($unit) — $brand en El Arca Petshop: accesorio resistente y ergonómico pensado para paseos seguros, descanso confortable o aseo sin estrés.",
                    "Ergonomía y duración",
                    "Guía práctica: ajusta correas o accesorios dejando espacio cómodo de dos dedos y limpia regularmente con agua tibia."
                )
            }
        }
    }
}
