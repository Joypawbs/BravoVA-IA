package com.example.model

/**
 * Formas de pago de Bravo en línea: tarjeta de crédito o débito, Apple Pay (iPhone) y
 * Google Pay (Android). Se quitaron transferencia, tPago, efectivo y datáfono porque
 * Bravo no los ofrece en su compra en línea.
 */
enum class RdPaymentOption(val label: String, val detail: String, val needsBiometric: Boolean) {
    WALLET("Pago rápido", "Confirmas tu identidad", true),
    CARD("Tarjeta de crédito o débito", "Visa o Mastercard guardada", true)
}

/** Detalle según la plataforma (Face ID existe solo en iPhone). */
val RdPaymentOption.displayDetail: String
    get() = if (this == RdPaymentOption.WALLET) {
        if (com.example.platform.PlatformInfo.isIos) "Confirmas con Face ID" else "Confirmas con tu huella"
    } else detail

/** Nombre que ve el usuario: Apple Pay en iPhone, Google Pay en Android. */
val RdPaymentOption.displayLabel: String
    get() = if (this == RdPaymentOption.WALLET) {
        if (com.example.platform.PlatformInfo.isIos) "Apple Pay" else "Google Pay"
    } else label

val RD_BANKS = listOf("Banreservas", "Popular", "BHD", "Scotiabank")

/** Franjas de entrega. La primera es "lo antes posible". */
val RD_DELIVERY_SLOTS = listOf(
    "Lo antes posible (≈90 min)",
    "Hoy 2:00–4:00 pm",
    "Hoy 4:00–6:00 pm",
    "Hoy 6:00–8:00 pm",
    "Mañana 8:00–10:00 am"
)

enum class DiasporaCurrency(val code: String, val symbol: String, val rdPerUnit: Double) {
    // Tasas de referencia fijas para la demo (no son cotizaciones reales).
    USD("USD", "US$", 63.0),
    EUR("EUR", "€", 68.0)
}

/** Todo lo que el cliente eligió en el checkout además del carrito. */
data class CheckoutExtras(
    val payment: RdPaymentOption = RdPaymentOption.CARD,
    val transferBank: String? = null,
    val transferProofAttached: Boolean = false,
    val deliverySlot: String = RD_DELIVERY_SLOTS.first(),
    val addressReference: String = "",
    val tipRd: Int = 0,
    val diasporaRecipient: String? = null,
    val diasporaPhone: String? = null,
    val diasporaCurrency: DiasporaCurrency? = null,
    val ncfType: String = "B02",
    val rnc: String = "",
    val substitution: SubstitutionPref = SubstitutionPref.SIMILAR
) {
    val paymentSummary: String
        get() = payment.displayLabel
}

/** Combos de temporada: se arman buscando estas palabras en el catálogo. */
data class SeasonalCombo(val id: String, val title: String, val subtitle: String, val keywords: List<String>)

val SEASONAL_COMBOS = listOf(
    SeasonalCombo("sancocho", "Todo para el sancocho", "Carnes, víveres y sazón", listOf("pollo", "cerdo", "yuca", "plátano", "auyama", "mazorca", "sazón", "cilantr")),
    SeasonalCombo("habichuelas_dulce", "Habichuelas con dulce", "Para Semana Santa", listOf("habichuela", "leche evaporada", "leche de coco", "batata", "azúcar", "canela", "galleta")),
    SeasonalCombo("nochebuena", "La cena de Nochebuena", "Pernil, telera y ensalada rusa", listOf("cerdo", "pernil", "pan", "papa", "zanahoria", "mayonesa", "uva", "manzana")),
    SeasonalCombo("desayuno", "Mangú completo", "Los tres golpes", listOf("plátano", "salami", "queso", "huevo", "cebolla"))
)


/** Qué hacer si un producto del carrito se agota mientras se prepara el pedido. */
enum class SubstitutionPref(val label: String, val detail: String) {
    SIMILAR("Cambiar por similar", "Mismo tipo, igual o menor precio"),
    CALL("Llamarme", "El preparador te consulta antes"),
    NONE("No cambiar", "Se quita y se descuenta del total")
}
