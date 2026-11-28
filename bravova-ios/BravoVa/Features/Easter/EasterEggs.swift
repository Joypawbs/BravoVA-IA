import SwiftUI
import Observation

/// Huevos de pascua para fans de Toby Fox (Undertale / Deltarune), iguales a la 3.1.
/// Se activan escribiendo la palabra en un buscador o en Brav IA. Frases originales con tono de
/// súper, figuras simples (nunca un personaje) y melodías de 8 bits originales.
enum EasterEffect: Equatable {
    case spamton, muffet, ralsei, asgore, particles, tvStatic
    var duration: Double {
        switch self {
        case .spamton: 6; case .muffet, .ralsei: 6.5; case .asgore: 7; case .particles: 6; case .tvStatic: 3.5
        }
    }
}

enum ParticleShape { case heart, spade, club, diamond, snow, flame, spark, drop, feather, pellet, square, spear, star }
enum ParticleMotion { case fall, rise, spinFall, burst, jitter, orbit }

struct ParticleStyle: Equatable {
    var shapes: [ParticleShape]
    var colors: [Color]
    var motion: ParticleMotion = .fall
    var count = 22
    var size: Double = 12
    var tint: Color? = nil
    var period: Double = 4.5
}

struct EasterEgg {
    let keywords: [String]
    let line: String
    let sound: [ChipNote]
    var effect: EasterEffect? = nil
    var particles: ParticleStyle? = nil
}

/// Color ARGB como en Kotlin (0xAARRGGBB)
func argb(_ v: UInt32) -> Color {
    Color(.sRGB, red: Double((v >> 16) & 0xFF) / 255, green: Double((v >> 8) & 0xFF) / 255,
          blue: Double(v & 0xFF) / 255, opacity: Double((v >> 24) & 0xFF) / 255)
}

private let S = ChipSynth.seq
private let chime = ChipSynth.seq("E5 G#5 B5 E6", 90, .triangle)
private let hit = [ChipNote(note: "C3", ms: 110, wave: .noise, slide: -12), ChipNote(note: "G4", ms: 90, wave: .square, volume: 0.6, slide: 7)]

let easterEggs: [EasterEgg] = [
    EasterEgg(keywords: ["determinacion", "determination"], line: "* Ver tantas ofertas te llena de DETERMINACIÓN.", sound: S("C5 E5 G5 C6", 80, .pulse, 1)),
    EasterEgg(keywords: ["sans"], line: "* jeje. ¿sabías que el kétchup combina con todo?",
              sound: [ChipNote(note: "C4", ms: 70, wave: .triangle), ChipNote(note: "-", ms: 40), ChipNote(note: "G3", ms: 140, wave: .triangle, slide: -2)]),
    EasterEgg(keywords: ["papyrus", "espagueti", "spaghetti"], line: "* ¡NYEH HEH HEH! El gran Papyrus aprueba este espagueti.", sound: S("G4 G4 A4 G4 C5", 70, .square, 1)),
    EasterEgg(keywords: ["temmie", "tem"], line: "* hOI!! bienbenidos a... ¡BRAVO!", sound: S("C6 E6 C6 G6", 50, .pulse, 1)),
    EasterEgg(keywords: ["perro molesto", "annoying dog", "toby fox", "toby"], line: "* (Un perro molesto se llevó tu búsqueda. Guau.)",
              sound: [ChipNote(note: "A4", ms: 90, slide: 5), ChipNote(note: "-", ms: 60), ChipNote(note: "A4", ms: 90, slide: 5)]),
    EasterEgg(keywords: ["gaster"], line: "* ... ... ... (Este producto no existe.)", sound: S("C3 - - F#3", 180, .triangle, 0.7), effect: .tvStatic),
    EasterEgg(keywords: ["deltarune", "kris"], line: "* (Algo te dice que revises el pasillo de lácteos.)", sound: S("D5 A4 F5 E5", 110, .triangle, 1)),
    EasterEgg(keywords: ["undertale"], line: "* Nadie tiene que salir herido... ni siquiera tu presupuesto.", sound: S("E5 D5 C5 G5", 120, .triangle, 1)),
    EasterEgg(keywords: ["spamton"], line: "* ¡¡ES TU OPORTUNIDAD DE SER UN [[GRAN AHORRADOR]]!!", sound: S("C5 C6 B4 C6 G5 C6", 55, .saw, 1), effect: .spamton),
    EasterEgg(keywords: ["muffet"], line: "* Las arañas te ofrecen un dulce... por un módico precio. Jijiji.", sound: S("A4 C5 E5 A5 E5 C5", 75, .pulse, 1), effect: .muffet),
    EasterEgg(keywords: ["ralsei"], line: "* ¡Que tengas una compra llena de esponjosidad!", sound: S("G5 B5 D6 G6", 110, .triangle, 1), effect: .ralsei),
    EasterEgg(keywords: ["asgore"], line: "* ¿Una taza de té? Combina muy bien con el pan de agua.", sound: S("F4 A4 C5 F5", 150, .triangle, 1), effect: .asgore),

    EasterEgg(keywords: ["toriel"], line: "* Hay pastel de canela y caramelo en Panadería. Huele a hogar.", sound: S("C5 E5 G5 E5 C6", 130, .triangle, 1), effect: .particles,
              particles: ParticleStyle(shapes: [.heart], colors: [argb(0xFFFF8FB1), argb(0xFFFFFFFF)], motion: .fall, count: 18, size: 11, period: 6.5)),
    EasterEgg(keywords: ["undyne"], line: "* ¡NGAAAH! ¡Ofertas con la fuerza de mil lanzas!", sound: hit + S("C5 E5 G5", 60, .square, 1), effect: .particles,
              particles: ParticleStyle(shapes: [.spear], colors: [argb(0xFF38BDF8), argb(0xFF1D4ED8)], motion: .rise, count: 16, size: 14, period: 1.8)),
    EasterEgg(keywords: ["alphys"], line: "* L-los fideos instantáneos están en el pasillo 4... n-no es que yo los compre.", sound: S("E6 - E6 C6", 45, .pulse, 0.7), effect: .particles,
              particles: ParticleStyle(shapes: [.spark], colors: [argb(0xFFFDE047), argb(0xFFF472B6)], motion: .burst, count: 26, size: 10, period: 2.4)),
    EasterEgg(keywords: ["mettaton"], line: "* ¡OH, SÍ! Los ratings de esta oferta están por las nubes.", sound: S("C5 E5 G5 C6 - G5 C6", 85, .square, 1), effect: .particles,
              particles: ParticleStyle(shapes: [.star, .spark], colors: [argb(0xFFFFD233), argb(0xFFFF4FD8)], motion: .burst, count: 30, size: 13, period: 2.2)),
    EasterEgg(keywords: ["napstablook", "blooky"], line: "* oh... no quería interrumpir tu compra... ya me voy...", sound: S("E4 D4 C4 B3", 220, .triangle, 0.7), effect: .particles,
              particles: ParticleStyle(shapes: [.drop], colors: [argb(0xFF93C5FD)], motion: .fall, count: 14, size: 9, tint: argb(0x1F1E3A8A), period: 7)),
    EasterEgg(keywords: ["flowey"], line: "* Psst. En este súper es AHORRA o te quedas sin ahorrar. Jijiji.", sound: S("C6 B5 A5 G5 F5 E5", 55, .pulse, 1), effect: .particles,
              particles: ParticleStyle(shapes: [.pellet], colors: [argb(0xFFFFFFFF)], motion: .orbit, count: 16, size: 14, period: 3)),
    EasterEgg(keywords: ["froggit", "ribbit"], line: "* Croac, croac. (Traducción: hay ofertas en Frutas y vegetales.)",
              sound: [ChipNote(note: "C3", ms: 140, wave: .noise, volume: 0.8, slide: 5), ChipNote(note: "-", ms: 60), ChipNote(note: "C3", ms: 140, wave: .noise, volume: 0.8, slide: 5)],
              effect: .particles, particles: ParticleStyle(shapes: [.pellet], colors: [argb(0xFF4ADE80), argb(0xFF16A34A)], motion: .jitter, count: 20, size: 16)),
    EasterEgg(keywords: ["grillby"], line: "* (Un señor de fuego te señala la sección de Carnes. Muy amable.)",
              sound: [ChipNote(note: "C5", ms: 500, wave: .noise, volume: 0.5, slide: -6)], effect: .particles,
              particles: ParticleStyle(shapes: [.flame], colors: [argb(0xFFFF7A1A), argb(0xFFFFB020)], motion: .rise, count: 22, size: 12, period: 3.2)),
    EasterEgg(keywords: ["burgerpants"], line: "* Hola, bienvenido a... ¿esto es ser adulto? ¿Comparar precios por libra?", sound: S("G4 F#4 F4 E4", 200, .saw, 0.7), effect: .particles,
              particles: ParticleStyle(shapes: [.square], colors: [argb(0xFFD97706), argb(0xFF92400E)], motion: .spinFall, count: 14, size: 14)),
    EasterEgg(keywords: ["guardar", "save", "punto de guardado"], line: "* (Tu carrito quedó a salvo. Eso te llena de determinación.)", sound: chime, effect: .particles,
              particles: ParticleStyle(shapes: [.star], colors: [argb(0xFFFFE600)], motion: .burst, count: 18, size: 14, period: 2.6)),
    EasterEgg(keywords: ["snowdin"], line: "* Hace frío afuera, pero el chocolate caliente está en Bebidas.", sound: S("E6 B5 E6 G#6", 140, .triangle, 0.8), effect: .particles,
              particles: ParticleStyle(shapes: [.snow], colors: [argb(0xFFFFFFFF), argb(0xFFBFDBFE)], motion: .fall, count: 34, size: 9, period: 7)),
    EasterEgg(keywords: ["hotland"], line: "* Hace calor... buen momento para pasar por los helados.", sound: S("A3 C4 E4 A4", 120, .saw, 0.7), effect: .particles,
              particles: ParticleStyle(shapes: [.flame], colors: [argb(0xFFEF4444), argb(0xFFF97316)], motion: .rise, count: 18, size: 11, tint: argb(0x1AEF4444), period: 3.5)),
    EasterEgg(keywords: ["waterfall"], line: "* (Una flor repite: \"oferta... oferta... oferta...\")", sound: S("B4 - B4 - B4", 160, .triangle, 0.6), effect: .particles,
              particles: ParticleStyle(shapes: [.spark], colors: [argb(0xFF67E8F9), argb(0xFFA5F3FC)], motion: .fall, count: 20, size: 7, tint: argb(0x1A0E7490), period: 9)),
    EasterEgg(keywords: ["alma", "soul"], line: "* Tu ALMA brilla de rojo. Es la determinación... o el ají picante.",
              sound: [ChipNote(note: "C2", ms: 90, wave: .triangle), ChipNote(note: "-", ms: 70), ChipNote(note: "C2", ms: 120, wave: .triangle)], effect: .particles,
              particles: ParticleStyle(shapes: [.heart], colors: [argb(0xFFFF0033)], motion: .burst, count: 12, size: 16, period: 3)),
    EasterEgg(keywords: ["lancer"], line: "* ¡HO HO HO! ¡El príncipe de las picas aprueba este carrito!", sound: S("C5 - C5 - C5 G5", 70, .square, 1), effect: .particles,
              particles: ParticleStyle(shapes: [.spade], colors: [argb(0xFF1E3A8A), argb(0xFF3B82F6)], motion: .spinFall, count: 20, size: 12)),
    EasterEgg(keywords: ["susie"], line: "* Susie se comió la muestra gratis. Y la mesa. Pide otra en la caja.",
              sound: [ChipNote(note: "C6", ms: 160, wave: .noise, volume: 0.7, slide: -24)] + hit, effect: .particles,
              particles: ParticleStyle(shapes: [.spear], colors: [argb(0xFFD946EF), argb(0xFF7C3AED)], motion: .jitter, count: 12, size: 16)),
    EasterEgg(keywords: ["noelle"], line: "* ¡Felices fiestas! El ponche navideño ya llegó a Bebidas.", sound: S("G5 E5 G5 C6 B5", 120, .triangle, 1), effect: .particles,
              particles: ParticleStyle(shapes: [.snow], colors: [argb(0xFFE0F2FE), argb(0xFFFCA5A5)], motion: .spinFall, count: 28, size: 10, period: 6.5)),
    EasterEgg(keywords: ["berdly"], line: "* Como el MÁS inteligente del grupo, te recomiendo la Marca Exclusiva Bravo.", sound: S("A5 C6 A5 E6", 60, .pulse, 1), effect: .particles,
              particles: ParticleStyle(shapes: [.feather], colors: [argb(0xFF60A5FA), argb(0xFF2563EB)], motion: .spinFall, count: 16, size: 10, period: 6)),
    EasterEgg(keywords: ["jevil"], line: "* ¡PUEDO HACER CUALQUIER COSA! ...menos pagar de más. Jiji.", sound: S("C5 F#5 C6 F#5 C5 F#5 C6 G6", 55, .pulse, 1), effect: .particles,
              particles: ParticleStyle(shapes: [.spade, .club, .diamond, .heart], colors: [argb(0xFF7C3AED), argb(0xFFFACC15), argb(0xFF22D3EE)], motion: .jitter, count: 28, size: 12)),
    EasterEgg(keywords: ["seam"], line: "* Un gato viejo vende cosas raras... aquí solo hay comida para gatos, en El Arca.", sound: S("D4 F4 A4 C#5", 170, .triangle, 0.8), effect: .particles,
              particles: ParticleStyle(shapes: [.star], colors: [argb(0xFFC4B5FD), argb(0xFF8B5CF6)], motion: .fall, count: 16, size: 10, period: 8)),
    EasterEgg(keywords: ["queen"], line: "* Las ofertas de hoy están 100% verificadas por mi algoritmo. Je.", sound: S("C5 G5 E5 C6 G5 E6", 60, .saw, 0.7), effect: .particles,
              particles: ParticleStyle(shapes: [.square], colors: [argb(0xFF22D3EE), argb(0xFF0EA5E9), argb(0xFF38BDF8)], motion: .jitter, count: 34, size: 9)),
    EasterEgg(keywords: ["tenna", "mr tenna"], line: "* ¡NO CAMBIES DE CANAL! Esta oferta es EN VIVO.",
              sound: [ChipNote(note: "C6", ms: 400, wave: .noise, volume: 0.6)] + S("C5 E5 G5 C6", 70, .square, 1), effect: .tvStatic),
    EasterEgg(keywords: ["starwalker"], line: "* Este * súper * es * el * original *", sound: chime + S("B5 E6", 120, .triangle, 1), effect: .particles,
              particles: ParticleStyle(shapes: [.star], colors: [argb(0xFFFFE600), argb(0xFFFFF3A8)], motion: .spinFall, count: 10, size: 18, period: 8)),
]

/// Estado global de los huevos: el que está sonando y el de las huellitas.
@MainActor
@Observable
final class Easter {
    static let shared = Easter()
    private(set) var line: String?
    private(set) var effect: EasterEffect?
    private(set) var style: ParticleStyle?
    private(set) var serial = 0
    private(set) var pawVisible = false
    private var lastLine = ""
    private var taps = 0
    private var lastTap = Date.distantPast

    /// Revisa lo escrito; si es un huevo lo dispara y devuelve true.
    @discardableResult
    func check(_ query: String) -> Bool {
        let q = query.trimmingCharacters(in: .whitespaces).folded
        guard !q.isEmpty, let egg = easterEggs.first(where: { $0.keywords.contains(q) }) else {
            lastLine = ""
            return false
        }
        if egg.line == lastLine { return true }
        lastLine = egg.line
        play(egg)
        return true
    }

    private func play(_ egg: EasterEgg) {
        serial += 1
        let mine = serial
        line = egg.line
        let reduce = UIAccessibility.isReduceMotionEnabled
        effect = reduce ? nil : egg.effect
        style = egg.particles
        ChipSynth.play(egg.sound)
        let wait = max(egg.effect?.duration ?? 4, 4)
        Task { @MainActor in
            try? await Task.sleep(for: .seconds(wait))
            guard self.serial == mine else { return }
            withAnimation(.easeOut(duration: 0.6)) { self.effect = nil; self.line = nil }
        }
    }

    /// Logo tocado: 5 toques seguidos (menos de 3 s entre cada uno) muestran las huellitas.
    func logoTapped() {
        let now = Date()
        taps = now.timeIntervalSince(lastTap) <= 3 ? taps + 1 : 1
        lastTap = now
        if taps >= 5 {
            taps = 0
            withAnimation(.easeIn(duration: 0.3)) { pawVisible = true }
            ChipSynth.play([ChipNote(note: "A4", ms: 90, slide: 5), ChipNote(note: "-", ms: 60), ChipNote(note: "A4", ms: 90, slide: 5)])
        }
    }

    func dismissPaw() { withAnimation(.easeOut(duration: 0.3)) { pawVisible = false } }
}
