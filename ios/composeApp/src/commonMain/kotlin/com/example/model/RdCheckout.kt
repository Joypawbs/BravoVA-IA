package com.example.model

/** Formas de pago comunes en República Dominicana (simuladas en esta demo). */
enum class RdPaymentOption(val label: String, val detail: String, val needsBiometric: Boolean) {
    CARD("Tarjeta guardada", "Cobro inmediato con huella", true),
    CASH_ON_DELIVERY("Efectivo al recibir", "Pagas al repartidor", false),
    POS_ON_DELIVERY("Tarjeta al recibir", "El repartidor lleva datáfono", false),
    TRANSFER("Transferencia bancaria", "Adjunta el comprobante", false),
    TPAGO("tPago", "Apruebas el pago desde tu celular", false),
    AZUL("Azul", "Pago en línea con tu tarjeta", true)
}

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
    val rnc: String = ""
) {
    val paymentSummary: String
        get() = when (payment) {
            RdPaymentOption.TRANSFER -> "Transferencia ${transferBank.orEmpty()}".trim()
            else -> payment.label
        }
}

/** Combos de temporada: se arman buscando estas palabras en el catálogo. */
data class SeasonalCombo(val id: String, val title: String, val subtitle: String, val keywords: List<String>)

val SEASONAL_COMBOS = listOf(
    SeasonalCombo("sancocho", "Todo para el sancocho", "Carnes, víveres y sazón", listOf("pollo", "cerdo", "yuca", "plátano", "auyama", "mazorca", "sazón", "cilantr")),
    SeasonalCombo("habichuelas_dulce", "Habichuelas con dulce", "Para Semana Santa", listOf("habichuela", "leche evaporada", "leche de coco", "batata", "azúcar", "canela", "galleta")),
    SeasonalCombo("nochebuena", "La cena de Nochebuena", "Pernil, telera y ensalada rusa", listOf("cerdo", "pernil", "pan", "papa", "zanahoria", "mayonesa", "uva", "manzana")),
    SeasonalCombo("desayuno", "Mangú completo", "Los tres golpes", listOf("plátano", "salami", "queso", "huevo", "cebolla"))
)
