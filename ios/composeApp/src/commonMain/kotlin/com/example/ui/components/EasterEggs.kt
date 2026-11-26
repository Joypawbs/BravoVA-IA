package com.example.ui.components

import androidx.compose.ui.graphics.Color
import com.example.platform.ChipNote
import com.example.platform.ChipSynth.seq
import com.example.platform.Wave
import com.example.ui.components.ParticleMotion.BURST
import com.example.ui.components.ParticleMotion.FALL
import com.example.ui.components.ParticleMotion.JITTER
import com.example.ui.components.ParticleMotion.ORBIT
import com.example.ui.components.ParticleMotion.RISE
import com.example.ui.components.ParticleMotion.SPIN_FALL
import com.example.ui.components.ParticleShape.CLUB
import com.example.ui.components.ParticleShape.DIAMOND
import com.example.ui.components.ParticleShape.DROP
import com.example.ui.components.ParticleShape.FEATHER
import com.example.ui.components.ParticleShape.FLAME
import com.example.ui.components.ParticleShape.HEART
import com.example.ui.components.ParticleShape.PELLET
import com.example.ui.components.ParticleShape.SNOW
import com.example.ui.components.ParticleShape.SPADE
import com.example.ui.components.ParticleShape.SPARK
import com.example.ui.components.ParticleShape.SPEAR
import com.example.ui.components.ParticleShape.SQUARE
import com.example.ui.components.ParticleShape.STAR

/**
 * Huevos de pascua para fans de Toby Fox (Undertale / Deltarune).
 *
 * Se activan escribiendo la palabra en cualquier buscador. Cada uno tiene:
 * - una frase original con tono de súper (no se copian diálogos de los juegos);
 * - un efecto con figuras simples (nunca se dibuja un personaje);
 * - un sonido de 8 bits con melodía original, hecho por [com.example.platform.ChipSynth].
 * La música y los sonidos de los juegos tienen derechos de autor, por eso nada aquí los usa.
 */
data class EasterEgg(
    val keywords: List<String>,
    val line: String,
    val sound: List<ChipNote>,
    val effect: EasterEffect? = null,
    val particles: ParticleStyle? = null
)

private fun c(hex: Long) = Color(hex)

/* Sonidos cortos que se reusan */
private val CHIME = seq("E5 G#5 B5 E6", 90, Wave.TRIANGLE)                // campanita suave
private val HIT = listOf(ChipNote("C3", 110, Wave.NOISE, slide = -12f), ChipNote("G4", 90, Wave.SQUARE, 0.6f, slide = 7f))

val EASTER_EGGS: List<EasterEgg> = listOf(
    // ---------- Los de antes (ahora con sonido) ----------
    EasterEgg(listOf("determinacion", "determination"), "* Ver tantas ofertas te llena de DETERMINACIÓN.",
        seq("C5 E5 G5 C6", 80, Wave.PULSE)),
    EasterEgg(listOf("sans"), "* jeje. ¿sabías que el kétchup combina con todo?",
        listOf(ChipNote("C4", 70, Wave.TRIANGLE), ChipNote("-", 40), ChipNote("G3", 140, Wave.TRIANGLE, slide = -2f))),
    EasterEgg(listOf("papyrus", "espagueti", "spaghetti"), "* ¡NYEH HEH HEH! El gran Papyrus aprueba este espagueti.",
        seq("G4 G4 A4 G4 C5", 70)),
    EasterEgg(listOf("temmie", "tem"), "* hOI!! bienbenidos a... ¡BRAVO!",
        seq("C6 E6 C6 G6", 50, Wave.PULSE)),
    EasterEgg(listOf("perro molesto", "annoying dog", "toby fox", "toby"), "* (Un perro molesto se llevó tu búsqueda. Guau.)",
        listOf(ChipNote("A4", 90, Wave.SQUARE, slide = 5f), ChipNote("-", 60), ChipNote("A4", 90, Wave.SQUARE, slide = 5f))),
    EasterEgg(listOf("gaster"), "* ... ... ... (Este producto no existe.)",
        seq("C3 - - F#3", 180, Wave.TRIANGLE, 0.7f), EasterEffect.TV_STATIC),
    EasterEgg(listOf("deltarune", "kris"), "* (Algo te dice que revises el pasillo de lácteos.)",
        seq("D5 A4 F5 E5", 110, Wave.TRIANGLE)),
    EasterEgg(listOf("undertale"), "* Nadie tiene que salir herido... ni siquiera tu presupuesto.",
        seq("E5 D5 C5 G5", 120, Wave.TRIANGLE)),
    EasterEgg(listOf("spamton"), "* ¡¡ES TU OPORTUNIDAD DE SER UN [[GRAN AHORRADOR]]!!",
        seq("C5 C6 B4 C6 G5 C6", 55, Wave.SAW), EasterEffect.SPAMTON),
    EasterEgg(listOf("muffet"), "* Las arañas te ofrecen un dulce... por un módico precio. Jijiji.",
        seq("A4 C5 E5 A5 E5 C5", 75, Wave.PULSE), EasterEffect.MUFFET),
    EasterEgg(listOf("ralsei"), "* ¡Que tengas una compra llena de esponjosidad!",
        seq("G5 B5 D6 G6", 110, Wave.TRIANGLE), EasterEffect.RALSEI),
    EasterEgg(listOf("asgore"), "* ¿Una taza de té? Combina muy bien con el pan de agua.",
        seq("F4 A4 C5 F5", 150, Wave.TRIANGLE), EasterEffect.ASGORE),

    // ---------- Nuevos en BravoVa 2.0 (23) ----------
    EasterEgg(listOf("toriel"), "* Hay pastel de canela y caramelo en Panadería. Huele a hogar.",
        seq("C5 E5 G5 E5 C6", 130, Wave.TRIANGLE), EasterEffect.PARTICLES,
        ParticleStyle(listOf(HEART), listOf(c(0xFFFF8FB1), c(0xFFFFFFFF)), FALL, count = 18, size = 11f, periodMs = 6500)),
    EasterEgg(listOf("undyne"), "* ¡NGAAAH! ¡Ofertas con la fuerza de mil lanzas!",
        HIT + seq("C5 E5 G5", 60), EasterEffect.PARTICLES,
        ParticleStyle(listOf(SPEAR), listOf(c(0xFF38BDF8), c(0xFF1D4ED8)), RISE, count = 16, size = 14f, periodMs = 1800)),
    EasterEgg(listOf("alphys"), "* L-los fideos instantáneos están en el pasillo 4... n-no es que yo los compre.",
        seq("E6 - E6 C6", 45, Wave.PULSE, 0.7f), EasterEffect.PARTICLES,
        ParticleStyle(listOf(SPARK), listOf(c(0xFFFDE047), c(0xFFF472B6)), BURST, count = 26, size = 10f, periodMs = 2400)),
    EasterEgg(listOf("mettaton"), "* ¡OH, SÍ! Los ratings de esta oferta están por las nubes.",
        seq("C5 E5 G5 C6 - G5 C6", 85, Wave.SQUARE), EasterEffect.PARTICLES,
        ParticleStyle(listOf(STAR, SPARK), listOf(c(0xFFFFD233), c(0xFFFF4FD8)), BURST, count = 30, size = 13f, periodMs = 2200)),
    EasterEgg(listOf("napstablook", "blooky"), "* oh... no quería interrumpir tu compra... ya me voy...",
        seq("E4 D4 C4 B3", 220, Wave.TRIANGLE, 0.7f), EasterEffect.PARTICLES,
        ParticleStyle(listOf(DROP), listOf(c(0xFF93C5FD)), FALL, count = 14, size = 9f, tint = c(0x1F1E3A8A), periodMs = 7000)),
    EasterEgg(listOf("flowey"), "* Psst. En este súper es AHORRA o te quedas sin ahorrar. Jijiji.",
        seq("C6 B5 A5 G5 F5 E5", 55, Wave.PULSE), EasterEffect.PARTICLES,
        ParticleStyle(listOf(PELLET), listOf(c(0xFFFFFFFF)), ORBIT, count = 16, size = 14f, periodMs = 3000)),
    EasterEgg(listOf("froggit", "ribbit"), "* Croac, croac. (Traducción: hay ofertas en Frutas y vegetales.)",
        listOf(ChipNote("C3", 140, Wave.NOISE, 0.8f, slide = 5f), ChipNote("-", 60), ChipNote("C3", 140, Wave.NOISE, 0.8f, slide = 5f)),
        EasterEffect.PARTICLES,
        ParticleStyle(listOf(PELLET), listOf(c(0xFF4ADE80), c(0xFF16A34A)), JITTER, count = 20, size = 16f)),
    EasterEgg(listOf("grillby"), "* (Un señor de fuego te señala la sección de Carnes. Muy amable.)",
        listOf(ChipNote("C5", 500, Wave.NOISE, 0.5f, slide = -6f)), EasterEffect.PARTICLES,
        ParticleStyle(listOf(FLAME), listOf(c(0xFFFF7A1A), c(0xFFFFB020)), RISE, count = 22, size = 12f, periodMs = 3200)),
    EasterEgg(listOf("burgerpants"), "* Hola, bienvenido a... ¿esto es ser adulto? ¿Comparar precios por libra?",
        seq("G4 F#4 F4 E4", 200, Wave.SAW, 0.7f), EasterEffect.PARTICLES,
        ParticleStyle(listOf(SQUARE), listOf(c(0xFFD97706), c(0xFF92400E)), SPIN_FALL, count = 14, size = 14f)),
    EasterEgg(listOf("guardar", "save", "punto de guardado"), "* (Tu carrito quedó a salvo. Eso te llena de determinación.)",
        CHIME, EasterEffect.PARTICLES,
        ParticleStyle(listOf(STAR), listOf(c(0xFFFFE600)), BURST, count = 18, size = 14f, periodMs = 2600)),
    EasterEgg(listOf("snowdin"), "* Hace frío afuera, pero el chocolate caliente está en Bebidas.",
        seq("E6 B5 E6 G#6", 140, Wave.TRIANGLE, 0.8f), EasterEffect.PARTICLES,
        ParticleStyle(listOf(SNOW), listOf(c(0xFFFFFFFF), c(0xFFBFDBFE)), FALL, count = 34, size = 9f, periodMs = 7000)),
    EasterEgg(listOf("hotland"), "* Hace calor... buen momento para pasar por los helados.",
        seq("A3 C4 E4 A4", 120, Wave.SAW, 0.7f), EasterEffect.PARTICLES,
        ParticleStyle(listOf(FLAME), listOf(c(0xFFEF4444), c(0xFFF97316)), RISE, count = 18, size = 11f, tint = c(0x1AEF4444), periodMs = 3500)),
    EasterEgg(listOf("waterfall"), "* (Una flor repite: \"oferta... oferta... oferta...\")",
        seq("B4 - B4 - B4", 160, Wave.TRIANGLE, 0.6f), EasterEffect.PARTICLES,
        ParticleStyle(listOf(SPARK), listOf(c(0xFF67E8F9), c(0xFFA5F3FC)), FALL, count = 20, size = 7f, tint = c(0x1A0E7490), periodMs = 9000)),
    EasterEgg(listOf("alma", "soul"), "* Tu ALMA brilla de rojo. Es la determinación... o el ají picante.",
        listOf(ChipNote("C2", 90, Wave.TRIANGLE), ChipNote("-", 70), ChipNote("C2", 120, Wave.TRIANGLE)), EasterEffect.PARTICLES,
        ParticleStyle(listOf(HEART), listOf(c(0xFFFF0033)), BURST, count = 12, size = 16f, periodMs = 3000)),
    EasterEgg(listOf("lancer"), "* ¡HO HO HO! ¡El príncipe de las picas aprueba este carrito!",
        seq("C5 - C5 - C5 G5", 70, Wave.SQUARE), EasterEffect.PARTICLES,
        ParticleStyle(listOf(SPADE), listOf(c(0xFF1E3A8A), c(0xFF3B82F6)), SPIN_FALL, count = 20, size = 12f)),
    EasterEgg(listOf("susie"), "* Susie se comió la muestra gratis. Y la mesa. Pide otra en la caja.",
        listOf(ChipNote("C6", 160, Wave.NOISE, 0.7f, slide = -24f)) + HIT, EasterEffect.PARTICLES,
        ParticleStyle(listOf(SPEAR), listOf(c(0xFFD946EF), c(0xFF7C3AED)), JITTER, count = 12, size = 16f)),
    EasterEgg(listOf("noelle"), "* ¡Felices fiestas! El ponche navideño ya llegó a Bebidas.",
        seq("G5 E5 G5 C6 B5", 120, Wave.TRIANGLE), EasterEffect.PARTICLES,
        ParticleStyle(listOf(SNOW), listOf(c(0xFFE0F2FE), c(0xFFFCA5A5)), SPIN_FALL, count = 28, size = 10f, periodMs = 6500)),
    EasterEgg(listOf("berdly"), "* Como el MÁS inteligente del grupo, te recomiendo la Marca Exclusiva Bravo.",
        seq("A5 C6 A5 E6", 60, Wave.PULSE), EasterEffect.PARTICLES,
        ParticleStyle(listOf(FEATHER), listOf(c(0xFF60A5FA), c(0xFF2563EB)), SPIN_FALL, count = 16, size = 10f, periodMs = 6000)),
    EasterEgg(listOf("jevil"), "* ¡PUEDO HACER CUALQUIER COSA! ...menos pagar de más. Jiji.",
        seq("C5 F#5 C6 F#5 C5 F#5 C6 G6", 55, Wave.PULSE), EasterEffect.PARTICLES,
        ParticleStyle(listOf(SPADE, CLUB, DIAMOND, HEART), listOf(c(0xFF7C3AED), c(0xFFFACC15), c(0xFF22D3EE)), JITTER, count = 28, size = 12f)),
    EasterEgg(listOf("seam"), "* Un gato viejo vende cosas raras... aquí solo hay comida para gatos, en El Arca.",
        seq("D4 F4 A4 C#5", 170, Wave.TRIANGLE, 0.8f), EasterEffect.PARTICLES,
        ParticleStyle(listOf(STAR), listOf(c(0xFFC4B5FD), c(0xFF8B5CF6)), FALL, count = 16, size = 10f, periodMs = 8000)),
    EasterEgg(listOf("queen"), "* Las ofertas de hoy están 100% verificadas por mi algoritmo. Je.",
        seq("C5 G5 E5 C6 G5 E6", 60, Wave.SAW, 0.7f), EasterEffect.PARTICLES,
        ParticleStyle(listOf(SQUARE), listOf(c(0xFF22D3EE), c(0xFF0EA5E9), c(0xFF38BDF8)), JITTER, count = 34, size = 9f)),
    EasterEgg(listOf("tenna", "mr tenna"), "* ¡NO CAMBIES DE CANAL! Esta oferta es EN VIVO.",
        listOf(ChipNote("C6", 400, Wave.NOISE, 0.6f)) + seq("C5 E5 G5 C6", 70), EasterEffect.TV_STATIC),
    EasterEgg(listOf("starwalker"), "* Este * súper * es * el * original *",
        CHIME + seq("B5 E6", 120, Wave.TRIANGLE), EasterEffect.PARTICLES,
        ParticleStyle(listOf(STAR), listOf(c(0xFFFFE600), c(0xFFFFF3A8)), SPIN_FALL, count = 10, size = 18f, periodMs = 8000)),
)

/** Busca un huevo por la palabra escrita (sin tildes ni mayúsculas). */
fun findEasterEgg(query: String): EasterEgg? {
    val q = normalizeForSearch(query.trim())
    if (q.isEmpty()) return null
    return EASTER_EGGS.firstOrNull { egg -> egg.keywords.any { it == q } }
}

/** Dispara el huevo: efecto visual (si no se pidió reducir movimiento) y sonido. */
fun EasterEgg.play() {
    // Diagnóstico (visible en logcat / consola): confirma que el huevo se activó y si el efecto se omitió
    println("[huevo] ${keywords.first()} efecto=$effect reducirMovimiento=${EasterEffects.reduceMotion}")
    effect?.let { EasterEffects.start(it, particles) }
    com.example.platform.ChipSynth.play(sound)
}
