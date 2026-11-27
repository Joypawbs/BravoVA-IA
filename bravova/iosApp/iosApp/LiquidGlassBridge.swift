import SwiftUI
import UIKit
import ComposeApp

// Vidrio líquido nativo de Apple (iOS 26, SwiftUI .glassEffect) para la barra inferior
// y las barras superiores de BravoVa. Compose dibuja el resto de la pantalla; estas vistas
// van encima (interop "overlay") y el vidrio refracta lo que Compose dibuja detrás.
// En iOS anteriores se usa el material translúcido del sistema.

// MARK: - Modificadores de vidrio

extension View {
    @ViewBuilder
    func bravoGlass<S: Shape>(_ shape: S, tint: Color? = nil, interactive: Bool = true) -> some View {
        #if compiler(>=6.2)
        if #available(iOS 26.0, *) {
            let base: Glass = tint.map { Glass.regular.tint($0) } ?? .regular
            self.glassEffect(interactive ? base.interactive() : base, in: shape)
        } else {
            self.background(.ultraThinMaterial, in: shape)
                .overlay(shape.stroke(Color.white.opacity(0.18), lineWidth: 0.8))
        }
        #else
        self.background(.ultraThinMaterial, in: shape)
            .overlay(shape.stroke(Color.white.opacity(0.18), lineWidth: 0.8))
        #endif
    }
}

enum BravoFeedbackKind { case selection, increase, success }

extension View {
    /// Vibración háptica del sistema (iOS 17+); en iOS 16 no hace nada.
    @ViewBuilder
    func bravoFeedback<T: Equatable>(_ kind: BravoFeedbackKind, trigger: T) -> some View {
        if #available(iOS 17.0, *) {
            switch kind {
            case .selection: self.sensoryFeedback(.selection, trigger: trigger)
            case .increase: self.sensoryFeedback(.increase, trigger: trigger)
            case .success: self.sensoryFeedback(.success, trigger: trigger)
            }
        } else {
            self
        }
    }
}

struct BravoGlassGroup<Content: View>: View {
    let spacing: CGFloat
    @ViewBuilder let content: () -> Content
    var body: some View {
        #if compiler(>=6.2)
        if #available(iOS 26.0, *) {
            GlassEffectContainer(spacing: spacing) { content() }
        } else {
            content()
        }
        #else
        content()
        #endif
    }
}

extension View {
    /// Botón de vidrio del sistema (iOS 26+): .glass para acciones secundarias y
    /// .glassProminent (vidrio teñido) para la acción principal. Antes de iOS 26, estilo con borde.
    @ViewBuilder
    func bravoGlassButton(prominent: Bool) -> some View {
        #if compiler(>=6.2)
        if #available(iOS 26.0, *) {
            if prominent { self.buttonStyle(.glassProminent) } else { self.buttonStyle(.glass) }
        } else {
            if prominent { self.buttonStyle(.borderedProminent) } else { self.buttonStyle(.bordered) }
        }
        #else
        if prominent { self.buttonStyle(.borderedProminent) } else { self.buttonStyle(.bordered) }
        #endif
    }

    /// Identidad de vidrio para que las piezas se fundan y se separen (morphing) al aparecer o
    /// desaparecer dentro de un mismo grupo (GlassEffectContainer).
    @ViewBuilder
    func bravoGlassID(_ id: String, in ns: Namespace.ID) -> some View {
        #if compiler(>=6.2)
        if #available(iOS 26.0, *) {
            self.glassEffectID(id, in: ns)
        } else {
            self
        }
        #else
        self
        #endif
    }
}

private extension Color {
    init(argb: Int32) {
        let v = UInt32(bitPattern: argb)
        self.init(.sRGB,
                  red: Double((v >> 16) & 0xFF) / 255,
                  green: Double((v >> 8) & 0xFF) / 255,
                  blue: Double(v & 0xFF) / 255,
                  opacity: Double((v >> 24) & 0xFF) / 255)
    }
}

// MARK: - Contenedor de la vista SwiftUI
// (SwiftUI dibuja los botones dentro de la vista del hosting, así que los toques deben llegarle a ella)

final class PassthroughView: UIView {}

final class GlassHostController<V: View>: UIViewController {
    let hosting: UIHostingController<V>
    init(root: V) {
        hosting = UIHostingController(rootView: root)
        super.init(nibName: nil, bundle: nil)
    }
    required init?(coder: NSCoder) { fatalError() }

    override func loadView() {
        let container = PassthroughView()
        container.backgroundColor = .clear
        view = container
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        addChild(hosting)
        hosting.view.backgroundColor = .clear
        hosting.view.isOpaque = false
        if #available(iOS 16.4, *) { hosting.safeAreaRegions = [] }
        hosting.view.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(hosting.view)
        NSLayoutConstraint.activate([
            hosting.view.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            hosting.view.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            hosting.view.topAnchor.constraint(equalTo: view.topAnchor),
            hosting.view.bottomAnchor.constraint(equalTo: view.bottomAnchor),
        ])
        hosting.didMove(toParent: self)
    }
}

// MARK: - Aparecer y desaparecer con fundido
// Compose no puede desvanecer vistas nativas: Kotlin dice "visible o no" y SwiftUI hace el
// fundido (opacidad + leve escala y desenfoque, como el vidrio de iOS al aparecer).

final class GlassPresenceModel: ObservableObject {
    @Published var visible = false
}

struct GlassPresenceContainer<Content: View>: View {
    @ObservedObject var p: GlassPresenceModel
    let anchor: UnitPoint
    let travel: CGFloat
    @ViewBuilder let content: () -> Content
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        content()
            .opacity(p.visible ? 1 : 0)
            .scaleEffect(p.visible || reduceMotion ? 1 : 0.94, anchor: anchor)
            .blur(radius: p.visible || reduceMotion ? 0 : 5)
            .offset(y: p.visible || reduceMotion ? 0 : travel)
            .allowsHitTesting(p.visible)
    }
}

// MARK: - Barra superior

final class TopBarModel: ObservableObject {
    struct Action: Identifiable, Equatable { let id: Int; let symbol: String; let label: String; let text: String; let badge: String; let highlighted: Bool; let tint: Color? }
    @Published var title = ""
    @Published var subtitle = ""
    @Published var subtitleColor = Color.accentColor
    @Published var hasBack = false
    @Published var backLabel = "Volver"
    @Published var actions: [Action] = []
    @Published var accent = Color.red
    @Published var dark = true
    var onBack: () -> Void = {}
    var onAction: (Int) -> Void = { _ in }
}

struct GlassTopBarView: View {
    @ObservedObject var m: TopBarModel
    @Namespace private var ns
    var body: some View {
        BravoGlassGroup(spacing: 10) {
            HStack(spacing: 8) {
                if m.hasBack {
                    Button(action: m.onBack) {
                        Image(systemName: "chevron.backward")
                            .font(.system(size: 18, weight: .semibold))
                            .foregroundStyle(.primary)
                            .frame(width: 48, height: 48)
                            .contentShape(Circle())
                    }
                    .buttonStyle(.plain)
                    .bravoGlass(Circle())
                    .bravoGlassID("back", in: ns)
                    .accessibilityLabel(m.backLabel)
                }
                if m.title.isEmpty {
                    // Con título grande en la pantalla, la píldora del título se oculta hasta hacer scroll
                    Spacer(minLength: 0).frame(maxWidth: .infinity, maxHeight: 48)
                } else {
                    VStack(spacing: 1) {
                        Text(m.title)
                            .font(.system(size: 15, weight: .bold, design: .rounded))
                            .foregroundStyle(.primary)
                            .lineLimit(1)
                        if !m.subtitle.isEmpty {
                            Text(m.subtitle)
                                .font(.system(size: 10.5, weight: .semibold, design: .rounded))
                                .foregroundStyle(m.subtitleColor)
                                .lineLimit(1)
                        }
                    }
                    .padding(.horizontal, 14)
                    .frame(maxWidth: .infinity)
                    .frame(height: 48)
                    .bravoGlass(Capsule(), interactive: false)
                    // Al hacer scroll, la píldora del título nace del grupo de vidrio (se funde con sus vecinas)
                    .bravoGlassID("title", in: ns)
                    .accessibilityElement(children: .combine)
                    .transition(.opacity.combined(with: .scale(scale: 0.92)))
                }

                if !m.actions.isEmpty {
                    HStack(spacing: 2) {
                        ForEach(m.actions) { a in
                            Button { m.onAction(a.id) } label: {
                                HStack(spacing: 4) {
                                    if !a.symbol.isEmpty { Image(systemName: a.symbol).font(.system(size: 17, weight: .semibold)) }
                                    if !a.text.isEmpty { Text(a.text).font(.system(size: 12, weight: .bold, design: .rounded)).lineLimit(1) }
                                }
                                .foregroundStyle(a.tint ?? (a.highlighted ? m.accent : Color.primary))
                                .padding(.horizontal, a.text.isEmpty ? 0 : 8)
                                .frame(minWidth: 40, minHeight: 40)
                                .background { if a.highlighted { Capsule().fill(m.accent.opacity(0.20)) } }
                                .overlay(alignment: .topTrailing) {
                                    if !a.badge.isEmpty {
                                        Text(a.badge)
                                            .contentTransition(.numericText())
                                            .font(.system(size: 9, weight: .heavy))
                                            .foregroundStyle(.white)
                                            .frame(minWidth: 16, minHeight: 16)
                                            .background(Circle().fill(m.accent))
                                            .offset(x: 3, y: -1)
                                    }
                                }
                                .contentShape(Capsule())
                            }
                            .buttonStyle(.plain)
                            .accessibilityLabel(a.label)
                        }
                    }
                    .padding(.horizontal, m.actions.count == 1 && m.actions[0].text.isEmpty ? 4 : 4)
                    .frame(minWidth: 48, minHeight: 48)
                    .bravoGlass(Capsule())
                    .bravoGlassID("actions", in: ns)
                }
            }
            .padding(.horizontal, 12)
            .animation(.spring(duration: 0.35, bounce: 0.2), value: m.title.isEmpty)
        }
        .tint(m.accent)
        .environment(\.colorScheme, m.dark ? .dark : .light)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

// MARK: - Barra inferior (estilo iOS 26/27: se encoge al hacer scroll, búsqueda aparte)

final class DockModel: ObservableObject {
    @Published var symbols: [String] = []
    @Published var labels: [String] = []
    @Published var selected = 0
    @Published var minimized = false
    @Published var accent = Color.red
    @Published var dark = true
    var onSelect: (Int) -> Void = { _ in }
    var onSearch: () -> Void = {}
}

struct GlassDockView: View {
    @ObservedObject var m: DockModel
    @Namespace private var ns
    // Arrastre con el dedo sobre las pestañas (como la barra de pestañas de iOS 26):
    // una lente de vidrio sigue el dedo y al soltar se abre la pestaña que quedó debajo.
    @State private var dragX: CGFloat? = nil
    @State private var hovered: Int? = nil

    var body: some View {
        BravoGlassGroup(spacing: 12) {
            HStack(spacing: 10) {
                tabs
                    .padding(.horizontal, m.minimized ? 0 : 6)
                    .frame(width: m.minimized ? 58 : nil)
                    .frame(maxWidth: m.minimized ? 58 : .infinity, maxHeight: .infinity)
                    .bravoGlass(Capsule())
                    .bravoGlassID("tabs", in: ns)

                if m.minimized { Spacer(minLength: 0) }

                Button(action: m.onSearch) {
                    Image(systemName: "magnifyingglass")
                        .font(.system(size: 19, weight: .semibold))
                        .foregroundStyle(Color.primary)
                        .frame(width: 58, height: 58)
                        .contentShape(Circle())
                }
                .buttonStyle(.plain)
                .bravoGlass(Circle())
                .bravoGlassID("search", in: ns)
                .accessibilityLabel("Buscar")
            }
        }
        .animation(.spring(response: 0.42, dampingFraction: 0.78), value: m.minimized)
        .animation(.spring(response: 0.38, dampingFraction: 0.72), value: m.selected)
        .padding(.horizontal, 16)
        .padding(.vertical, 6)
        .tint(m.accent)
        .environment(\.colorScheme, m.dark ? .dark : .light)
        .bravoFeedback(.selection, trigger: m.selected)
        .bravoFeedback(.selection, trigger: hovered)
    }

    private var tabs: some View {
        HStack(spacing: 0) {
            ForEach(Array(m.symbols.enumerated()), id: \.offset) { i, symbol in
                if !m.minimized || i == m.selected {
                    tab(i, symbol)
                }
            }
        }
        .overlay {
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    if let x = dragX, !m.minimized, !m.symbols.isEmpty {
                        let w = geo.size.width / CGFloat(m.symbols.count)
                        Capsule()
                            .fill(m.accent.opacity(0.10))
                            .bravoGlass(Capsule(), tint: m.accent.opacity(0.18))
                            .frame(width: w * 1.08, height: geo.size.height * 0.92)
                            .scaleEffect(1.12)
                            .offset(x: min(max(x - w * 0.54, -4), geo.size.width - w * 1.04))
                            .frame(maxHeight: .infinity)
                            .allowsHitTesting(false)
                            .transition(.scale(scale: 0.7).combined(with: .opacity))
                    }
                    Color.clear
                        .contentShape(Rectangle())
                        .gesture(
                            DragGesture(minimumDistance: 0)
                                .onChanged { v in
                                    guard !m.minimized else { return }
                                    let idx = index(at: v.location.x, width: geo.size.width)
                                    // La lente aparece solo al arrastrar, no en un toque corto
                                    if abs(v.translation.width) > 6 || dragX != nil {
                                        withAnimation(.interactiveSpring(response: 0.28, dampingFraction: 0.8)) { dragX = v.location.x }
                                    }
                                    if hovered != idx { hovered = idx }
                                }
                                .onEnded { v in
                                    let idx = m.minimized ? m.selected : index(at: v.location.x, width: geo.size.width)
                                    withAnimation(.spring(response: 0.35, dampingFraction: 0.75)) {
                                        dragX = nil
                                        hovered = nil
                                    }
                                    m.onSelect(idx)
                                }
                        )
                }
            }
        }
    }

    private func index(at x: CGFloat, width: CGFloat) -> Int {
        guard !m.symbols.isEmpty, width > 0 else { return m.selected }
        let i = Int((x / width) * CGFloat(m.symbols.count))
        return min(max(i, 0), m.symbols.count - 1)
    }

    private func tab(_ i: Int, _ symbol: String) -> some View {
        let isActive = hovered.map { $0 == i } ?? (i == m.selected)
        return VStack(spacing: 2) {
            Image(systemName: isActive && UIImage(systemName: symbol + ".fill") != nil ? symbol + ".fill" : symbol)
                .symbolRenderingMode(.hierarchical)
                .font(.system(size: 19, weight: isActive ? .semibold : .regular))
                .scaleEffect(dragX != nil && hovered == i ? 1.15 : 1)
                .symbolEffect(.bounce.down, value: m.selected == i)
            if !m.minimized, m.labels.indices.contains(i) {
                Text(m.labels[i]).font(.system(size: 10, weight: .semibold)).lineLimit(1)
            }
        }
        .foregroundStyle(isActive ? m.accent : Color.primary)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background {
            if i == m.selected && !m.minimized && dragX == nil {
                Capsule()
                    .fill(m.accent.opacity(0.14))
                    .padding(.vertical, 4)
                    .padding(.horizontal, 2)
                    .matchedGeometryEffect(id: "seleccion", in: ns)
            }
        }
        .contentShape(Rectangle())
        .accessibilityElement(children: .combine)
        .accessibilityLabel(m.labels.indices.contains(i) ? m.labels[i] : "")
        .accessibilityAddTraits(i == m.selected ? [.isSelected, .isButton] : .isButton)
        .accessibilityAction { m.onSelect(i) }
        .animation(.spring(response: 0.3, dampingFraction: 0.7), value: hovered)
    }
}

// MARK: - Barra del carrito (vidrio teñido con el rojo de Bravo)

final class CartBarModel: ObservableObject {
    @Published var count = 0
    @Published var total = ""
    @Published var accent = Color.red
    @Published var dark = false
    var onTap: () -> Void = {}
}

struct GlassCartBarView: View {
    @ObservedObject var m: CartBarModel
    var body: some View {
        Button(action: m.onTap) {
            HStack(spacing: 14) {
                Image(systemName: "cart.fill")
                    .font(.system(size: 18, weight: .semibold))
                    .symbolEffect(.bounce, value: m.count)
                    .overlay(alignment: .topTrailing) {
                        Text("\(m.count)")
                            .font(.system(size: 10, weight: .heavy))
                            .foregroundStyle(m.accent)
                            .padding(.horizontal, 4)
                            .frame(minWidth: 17, minHeight: 17)
                            .background(Capsule().fill(.white))
                            .offset(x: 10, y: -8)
                            .contentTransition(.numericText())
                    }
                Text(m.total)
                    .font(.system(size: 17, weight: .bold, design: .rounded))
                    .contentTransition(.numericText())
                Spacer()
                Text("Ver carrito")
                    .font(.system(size: 13, weight: .semibold))
                    .padding(.horizontal, 12).padding(.vertical, 6)
                    .background(Capsule().fill(.white.opacity(0.22)))
            }
            .foregroundStyle(.white)
            .padding(.horizontal, 18)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .contentShape(Capsule())
        }
        .buttonStyle(.plain)
        .bravoGlass(Capsule(), tint: m.accent)
        .animation(.snappy, value: m.count)
        .bravoFeedback(.increase, trigger: m.count)
        .padding(.horizontal, 16)
        .padding(.vertical, 4)
        .environment(\.colorScheme, m.dark ? .dark : .light)
        .accessibilityLabel("Ver carrito, \(m.count) productos, \(m.total)")
    }
}

// MARK: - Aviso flotante

final class ToastModel: ObservableObject {
    @Published var text = ""
    @Published var symbol = "checkmark.circle.fill"
    @Published var accent = Color.red
    @Published var dark = false
}

struct GlassToastView: View {
    @ObservedObject var m: ToastModel
    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: m.symbol).foregroundStyle(m.accent).font(.system(size: 17, weight: .semibold))
                .symbolEffect(.bounce, value: m.text)
            Text(m.text).font(.system(size: 14, weight: .semibold)).lineLimit(2)
        }
        .padding(.horizontal, 18).padding(.vertical, 12)
        .bravoGlass(Capsule(), tint: m.accent.opacity(0.10), interactive: false)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .environment(\.colorScheme, m.dark ? .dark : .light)
        .accessibilityElement(children: .combine)
    }
}

// MARK: - Vista rápida de producto (hoja nativa; en iOS 26+ es de vidrio)

struct QuickViewSheet: View {
    let title: String; let subtitle: String; let price: String; let imageUrl: String
    @State var isFavorite: Bool
    let accent: Color
    let onAdd: () -> Void; let onFavorite: () -> Void; let onShare: () -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var added = false

    var body: some View {
        VStack(spacing: 16) {
            AsyncImage(url: URL(string: imageUrl)) { phase in
                if let image = phase.image { image.resizable().scaledToFit() }
                else { RoundedRectangle(cornerRadius: 20).fill(accent.opacity(0.10)) }
            }
            .frame(height: 220)
            .padding(.top, 24)
            VStack(spacing: 4) {
                Text(title).font(.title3.weight(.bold)).multilineTextAlignment(.center)
                Text(subtitle).font(.subheadline).foregroundStyle(.secondary)
                Text(price).font(.title2.weight(.bold)).foregroundStyle(accent).padding(.top, 4)
            }
            HStack(spacing: 12) {
                Button {
                    isFavorite.toggle(); onFavorite()
                } label: {
                    Image(systemName: isFavorite ? "heart.fill" : "heart").font(.title3).frame(width: 52, height: 52)
                }
                .buttonStyle(.plain).foregroundStyle(accent).bravoGlass(Circle())
                .accessibilityLabel(isFavorite ? "Quitar de favoritos" : "Agregar a favoritos")
                Button {
                    onAdd(); added = true
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.6) { dismiss() }
                } label: {
                    Label(added ? "Agregado" : "Agregar al carrito", systemImage: added ? "checkmark" : "cart.badge.plus")
                        .font(.headline).foregroundStyle(.white)
                        .frame(maxWidth: .infinity, minHeight: 52)
                }
                .buttonStyle(.plain).bravoGlass(Capsule(), tint: accent)
                Button(action: onShare) {
                    Image(systemName: "square.and.arrow.up").font(.title3).frame(width: 52, height: 52)
                }
                .buttonStyle(.plain).foregroundStyle(.primary).bravoGlass(Circle())
                .accessibilityLabel("Compartir")
            }
            .bravoFeedback(.success, trigger: added)
            Spacer(minLength: 0)
        }
        .padding(.horizontal, 20)
        .presentationDetents([.medium, .large])
        .presentationDragIndicator(.visible)
    }
}

private func topViewController() -> UIViewController? {
    let scene = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }.first
    var top = scene?.windows.first(where: { $0.isKeyWindow })?.rootViewController
    while let presented = top?.presentedViewController { top = presented }
    return top
}

// MARK: - Buscador de vidrio (campo de texto nativo con .glassEffect)

final class SearchFieldModel: ObservableObject {
    @Published var text = ""
    @Published var placeholder = "Buscar"
    @Published var accent = Color.red
    @Published var dark = false
    @Published var trailingSymbol = ""
    @Published var focusRequest = 0
    /// Al ocultar el buscador (cambio de pantalla, hoja abierta) se cierra el teclado
    @Published var blurRequest = 0
    var onChange: (String) -> Void = { _ in }
    var onSubmit: () -> Void = {}
    var onFocus: (Bool) -> Void = { _ in }
    var onTrailing: () -> Void = {}
}

struct GlassSearchFieldView: View {
    @ObservedObject var m: SearchFieldModel
    @FocusState private var focused: Bool
    @Namespace private var ns
    var body: some View {
        // Al enfocar, un botón circular de vidrio "se desprende" del buscador (morphing) para cerrar el teclado,
        // como la búsqueda del sistema en iOS 26.
        BravoGlassGroup(spacing: 8) {
        HStack(spacing: 8) {
        HStack(spacing: 10) {
            Image(systemName: "magnifyingglass")
                .font(.system(size: 16, weight: .semibold))
                .foregroundStyle(m.accent)
            TextField(m.placeholder, text: Binding(
                get: { m.text },
                set: { new in m.text = new; m.onChange(new) }
            ))
            .focused($focused)
            .submitLabel(.search)
            .onSubmit { m.onSubmit() }
            .textInputAutocapitalization(.never)
            .autocorrectionDisabled()
            .font(.system(size: 15, weight: .medium))
            if !m.text.isEmpty {
                Button {
                    m.text = ""
                    m.onChange("")
                } label: {
                    Image(systemName: "xmark.circle.fill").foregroundStyle(.secondary)
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Borrar búsqueda")
            }
            if !m.trailingSymbol.isEmpty {
                Button(action: m.onTrailing) {
                    Image(systemName: m.trailingSymbol)
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundStyle(m.accent)
                        .frame(width: 32, height: 32)
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Escanear código")
            }
        }
        .padding(.leading, 16).padding(.trailing, 10)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .contentShape(Capsule())
        .onTapGesture { focused = true }
        .bravoGlass(Capsule())
        .bravoGlassID("field", in: ns)

        if focused {
            Button { focused = false } label: {
                Image(systemName: "xmark")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(Color.primary)
                    .frame(width: 44, height: 44)
                    .contentShape(Circle())
            }
            .buttonStyle(.plain)
            .bravoGlass(Circle())
            .bravoGlassID("dismiss", in: ns)
            .accessibilityLabel("Cerrar teclado")
        }
        }
        }
        .animation(.spring(response: 0.38, dampingFraction: 0.78), value: focused)
        .padding(.horizontal, 1).padding(.vertical, 2)
        .environment(\.colorScheme, m.dark ? .dark : .light)
        .onChange(of: focused) { _, now in m.onFocus(now) }
        .onChange(of: m.focusRequest) { _, _ in focused = true }
        .onChange(of: m.blurRequest) { _, _ in focused = false }
    }
}

// MARK: - Panel de presupuesto de Brav IA (deslizador de vidrio)
// El deslizador es el del sistema: en iOS 26+ el control se vuelve una lente de Liquid Glass
// mientras se arrastra (la guía de Apple pide justo eso para controles dentro del contenido).
// Los botones usan los estilos de vidrio del sistema y la acción principal ("Armar") va teñida.

final class BudgetPanelModel: ObservableObject {
    @Published var value: Double = 1200
    @Published var minV: Double = 400
    @Published var maxV: Double = 3500
    @Published var step: Double = 50
    @Published var chips: [String] = []
    @Published var accent = Color.red
    @Published var dark = true
    @Published var busy = false
    /// Mientras el dedo está en el deslizador no se acepta el valor que llega de Kotlin
    var editing = false
    var onChange: (Int) -> Void = { _ in }
    /// 0 = Mis listas, 1 = Armar, 2 = Ajustar, 3 = Foto / NCF, 10 + i = sugerencia i
    var onAction: (Int) -> Void = { _ in }
}

private let pesosFormatter: NumberFormatter = {
    let f = NumberFormatter()
    f.numberStyle = .decimal
    f.locale = Locale(identifier: "en_US")
    f.maximumFractionDigits = 0
    return f
}()

struct GlassBudgetPanelView: View {
    @ObservedObject var m: BudgetPanelModel
    @Namespace private var ns
    @State private var editing = false
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    private var binding: Binding<Double> {
        Binding(get: { m.value }, set: { v in
            let stepped = (v / m.step).rounded() * m.step
            if stepped != m.value {
                m.value = stepped
                m.onChange(Int(stepped))
            }
        })
    }

    var body: some View {
        VStack(spacing: 10) {
            HStack(alignment: .center, spacing: 8) {
                Text("RD$" + (pesosFormatter.string(from: NSNumber(value: m.value)) ?? "\(Int(m.value))"))
                    .font(.system(size: 22, weight: .heavy, design: .rounded))
                    .monospacedDigit()
                    .foregroundStyle(m.accent)
                    .contentTransition(.numericText(value: m.value))
                    .scaleEffect(editing && !reduceMotion ? 1.08 : 1, anchor: .leading)
                    .animation(.snappy(duration: 0.22), value: m.value)
                    .animation(.spring(response: 0.3, dampingFraction: 0.7), value: editing)
                    .lineLimit(1)
                    .minimumScaleFactor(0.7)
                    .accessibilityLabel("Presupuesto: \(Int(m.value)) pesos")
                Spacer(minLength: 4)
                BravoGlassGroup(spacing: 6) {
                    HStack(spacing: 6) {
                        pill("Listas", id: 0, prominent: false)
                        pill("Armar", id: 1, prominent: true)
                        pill("Ajustar", id: 2, prominent: false)
                    }
                }
                .disabled(m.busy)
            }
            .padding(.horizontal, 16)

            Slider(
                value: binding,
                in: m.minV...m.maxV,
                step: m.step,
                label: { Text("Presupuesto meta") },
                minimumValueLabel: {
                    Image(systemName: "banknote").font(.system(size: 12, weight: .semibold)).foregroundStyle(Color.secondary)
                },
                maximumValueLabel: {
                    Image(systemName: "banknote.fill").font(.system(size: 15, weight: .semibold)).foregroundStyle(m.accent)
                },
                onEditingChanged: { e in
                    editing = e
                    m.editing = e
                }
            )
            .tint(m.accent)
            .bravoFeedback(.selection, trigger: Int(m.value))
            .accessibilityLabel("Presupuesto meta")
            .accessibilityValue("\(Int(m.value)) pesos")
            .padding(.horizontal, 16)

            ScrollView(.horizontal, showsIndicators: false) {
                BravoGlassGroup(spacing: 8) {
                    HStack(spacing: 8) {
                        chip(symbol: "camera.fill", text: "Foto / NCF", id: 3, tinted: true)
                        ForEach(Array(m.chips.enumerated()), id: \.offset) { i, label in
                            chip(symbol: nil, text: label, id: 10 + i, tinted: false)
                        }
                    }
                    .padding(.vertical, 3)
                }
            }
            // Los chips se desplazan hasta el borde de la pantalla, pero empiezan y terminan
            // alineados a 16 pt: ningún texto queda cortado contra un borde fijo
            .contentMargins(.horizontal, 16, for: .scrollContent)
            .disabled(m.busy)
        }
        .tint(m.accent)
        .environment(\.colorScheme, m.dark ? .dark : .light)
        .opacity(m.busy ? 0.75 : 1)
        .animation(.easeInOut(duration: 0.25), value: m.busy)
    }

    private func pill(_ title: String, id: Int, prominent: Bool) -> some View {
        Button { m.onAction(id) } label: {
            Text(title)
                .font(.system(size: 13, weight: .bold, design: .rounded))
                .lineLimit(1)
                .padding(.horizontal, 2)
        }
        .controlSize(.small)
        .bravoGlassButton(prominent: prominent)
        .bravoGlassID("pill\(id)", in: ns)
    }

    private func chip(symbol: String?, text: String, id: Int, tinted: Bool) -> some View {
        Button { m.onAction(id) } label: {
            HStack(spacing: 5) {
                if let symbol { Image(systemName: symbol).font(.system(size: 12, weight: .semibold)) }
                Text(text).font(.system(size: 13, weight: .semibold, design: .rounded)).lineLimit(1)
            }
            .foregroundStyle(tinted ? m.accent : Color.primary)
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .contentShape(Capsule())
        }
        .buttonStyle(.plain)
        .bravoGlass(Capsule(), tint: tinted ? m.accent.opacity(0.22) : nil)
        .bravoGlassID("chip\(id)", in: ns)
    }
}

// MARK: - Cápsula de carga (pantalla de inicio)

final class ProgressCapsuleModel: ObservableObject {
    @Published var progress: Double = 0
    @Published var text = ""
    @Published var accent = Color.red
    @Published var dark = true
}

struct GlassProgressCapsuleView: View {
    @ObservedObject var m: ProgressCapsuleModel
    var body: some View {
        VStack(spacing: 10) {
            ProgressView(value: m.progress)
                .progressViewStyle(.linear)
                .tint(m.accent)
                // El padding de la cápsula mantiene la barra lejos del borde redondeado
                .animation(.linear(duration: 0.2), value: m.progress)
            Text(m.text)
                .font(.system(size: 13, weight: .semibold, design: .rounded))
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .lineLimit(1)
                .minimumScaleFactor(0.85)
        }
        .padding(.horizontal, 26)
        .padding(.vertical, 16)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .bravoGlass(Capsule(), interactive: false)
        .environment(\.colorScheme, m.dark ? .dark : .light)
        .accessibilityElement(children: .combine)
        .accessibilityLabel(m.text)
        .accessibilityValue("\(Int(m.progress * 100)) por ciento")
    }
}

// MARK: - Puente con Kotlin

final class GlassHandle: NSObject, IosGlassHandle {
    enum Kind { case topBar, dock, cartBar, toast, search, budget, progress }
    private let topModel: TopBarModel
    private let dockModel: DockModel
    private let cartModel: CartBarModel
    private let toastModel: ToastModel
    private let searchModel: SearchFieldModel
    private let budgetModel: BudgetPanelModel
    private let progressModel: ProgressCapsuleModel
    private let presence: GlassPresenceModel
    private var receivedFirstVisibility = false
    private let controller: UIViewController

    init(kind: Kind) {
        let top = TopBarModel(), dock = DockModel(), cart = CartBarModel()
        let toast = ToastModel(), search = SearchFieldModel(), p = GlassPresenceModel()
        topModel = top; dockModel = dock; cartModel = cart
        toastModel = toast; searchModel = search; presence = p
        let budget = BudgetPanelModel()
        budgetModel = budget
        let progressM = ProgressCapsuleModel()
        progressModel = progressM
        switch kind {
        case .topBar:
            controller = GlassHostController(root: GlassPresenceContainer(p: p, anchor: .top, travel: -6) { GlassTopBarView(m: top) })
        case .dock:
            controller = GlassHostController(root: GlassPresenceContainer(p: p, anchor: .bottom, travel: 14) { GlassDockView(m: dock) })
        case .cartBar:
            controller = GlassHostController(root: GlassPresenceContainer(p: p, anchor: .bottom, travel: 12) { GlassCartBarView(m: cart) })
        case .toast:
            controller = GlassHostController(root: GlassPresenceContainer(p: p, anchor: .top, travel: -14) { GlassToastView(m: toast) })
        case .search:
            controller = GlassHostController(root: GlassPresenceContainer(p: p, anchor: .center, travel: 0) { GlassSearchFieldView(m: search) })
        case .budget:
            controller = GlassHostController(root: GlassPresenceContainer(p: p, anchor: .center, travel: 0) { GlassBudgetPanelView(m: budget) })
        case .progress:
            controller = GlassHostController(root: GlassPresenceContainer(p: p, anchor: .center, travel: 8) { GlassProgressCapsuleView(m: progressM) })
        }
        super.init()
    }

    func setVisible(visible: Bool) {
        // Oculta: los toques pasan a Compose (lo que hay debajo)
        controller.view.isUserInteractionEnabled = visible
        if !receivedFirstVisibility {
            // Primera vez: se dibuja invisible y en el siguiente ciclo aparece con fundido
            receivedFirstVisibility = true
            guard visible else { return }
            DispatchQueue.main.async { [presence] in
                withAnimation(.smooth(duration: 0.30)) { presence.visible = true }
            }
            return
        }
        guard presence.visible != visible else { return }
        if !visible { searchModel.blurRequest += 1 }
        withAnimation(visible ? .smooth(duration: 0.30) : .easeOut(duration: 0.16)) {
            presence.visible = visible
        }
    }

    func viewController() -> UIViewController { controller }

    func updateTopBar(title: String, subtitle: String, subtitleArgb: Int32, hasBack: Bool, backLabel: String,
                      symbols: [String], labels: [String], texts: [String], badges: [String],
                      highlighted: [KotlinBoolean], tints: [KotlinInt], accentArgb: Int32, dark: Bool,
                      onBack: @escaping () -> Void, onAction: @escaping (KotlinInt) -> Void) {
        let m = topModel
        m.onBack = onBack
        m.onAction = { i in onAction(KotlinInt(int: Int32(i))) }
        if m.title != title { m.title = title }
        if m.subtitle != subtitle { m.subtitle = subtitle }
        let subColor = Color(argb: subtitleArgb)
        if m.subtitleColor != subColor { m.subtitleColor = subColor }
        if m.hasBack != hasBack { m.hasBack = hasBack }
        if m.backLabel != backLabel { m.backLabel = backLabel }
        let accent = Color(argb: accentArgb)
        if m.accent != accent { m.accent = accent }
        if m.dark != dark { m.dark = dark }
        // Solo se publica si algo cambió: cada publicación redibuja la barra
        let newActions: [TopBarModel.Action] = symbols.indices.map { i in
            let t = tints.indices.contains(i) ? tints[i].int32Value : 0
            return TopBarModel.Action(
                id: i, symbol: symbols[i],
                label: labels.indices.contains(i) ? labels[i] : "",
                text: texts.indices.contains(i) ? texts[i] : "",
                badge: badges.indices.contains(i) ? badges[i] : "",
                highlighted: highlighted.indices.contains(i) ? highlighted[i].boolValue : false,
                tint: t == 0 ? nil : Color(argb: t))
        }
        if m.actions != newActions { m.actions = newActions }
    }

    func updateDock(symbols: [String], labels: [String], selected: Int32, accentArgb: Int32, dark: Bool,
                    minimized: Bool, onSelect: @escaping (KotlinInt) -> Void, onSearch: @escaping () -> Void) {
        let m = dockModel
        m.onSelect = { i in onSelect(KotlinInt(int: Int32(i))) }
        m.onSearch = onSearch
        if m.symbols != symbols { m.symbols = symbols }
        if m.labels != labels { m.labels = labels }
        if m.selected != Int(selected) { m.selected = Int(selected) }
        if m.minimized != minimized { m.minimized = minimized }
        let accentColor = Color(argb: accentArgb)
        if m.accent != accentColor { m.accent = accentColor }
        if m.dark != dark { m.dark = dark }
    }

    func updateCartBar(count: Int32, total: String, accentArgb: Int32, dark: Bool, onTap: @escaping () -> Void) {
        let m = cartModel
        m.onTap = onTap
        if m.count != Int(count) { m.count = Int(count) }
        if m.total != total { m.total = total }
        let accentColor = Color(argb: accentArgb)
        if m.accent != accentColor { m.accent = accentColor }
        if m.dark != dark { m.dark = dark }
    }

    func updateSearchField(text: String, placeholder: String, accentArgb: Int32, dark: Bool, trailingSymbol: String,
                           onChange: @escaping (String) -> Void, onSubmit: @escaping () -> Void,
                           onFocus: @escaping (KotlinBoolean) -> Void, onTrailing: @escaping () -> Void) {
        let m = searchModel
        m.onChange = onChange
        m.onSubmit = onSubmit
        m.onFocus = { f in onFocus(KotlinBoolean(bool: f)) }
        m.onTrailing = onTrailing
        // Solo se reemplaza el texto si Kotlin lo cambió (p. ej. al elegir una sugerencia)
        if m.text != text { m.text = text }
        if m.placeholder != placeholder { m.placeholder = placeholder }
        if m.trailingSymbol != trailingSymbol { m.trailingSymbol = trailingSymbol }
        let accentColor = Color(argb: accentArgb)
        if m.accent != accentColor { m.accent = accentColor }
        if m.dark != dark { m.dark = dark }
    }

    func focusSearchField() { searchModel.focusRequest += 1 }

    func updateProgressCapsule(progress: Double, text: String, accentArgb: Int32, dark: Bool) {
        let m = progressModel
        if m.progress != progress { m.progress = progress }
        if m.text != text { m.text = text }
        let accentColor = Color(argb: accentArgb)
        if m.accent != accentColor { m.accent = accentColor }
        if m.dark != dark { m.dark = dark }
    }

    func updateBudgetPanel(value: Int32, min: Int32, max: Int32, step: Int32, chips: [String],
                           accentArgb: Int32, dark: Bool, busy: Bool,
                           onChange: @escaping (KotlinInt) -> Void, onAction: @escaping (KotlinInt) -> Void) {
        let m = budgetModel
        m.onChange = { v in onChange(KotlinInt(int: Int32(v))) }
        m.onAction = { i in onAction(KotlinInt(int: Int32(i))) }
        if !m.editing, m.value != Double(value) { m.value = Double(value) }
        if m.minV != Double(min) { m.minV = Double(min) }
        if m.maxV != Double(max) { m.maxV = Double(max) }
        if m.step != Double(step) { m.step = Double(step) }
        if m.chips != chips { m.chips = chips }
        let accentColor = Color(argb: accentArgb)
        if m.accent != accentColor { m.accent = accentColor }
        if m.dark != dark { m.dark = dark }
        if m.busy != busy { m.busy = busy }
    }

    func updateToast(text: String, symbol: String, accentArgb: Int32, dark: Bool) {
        let m = toastModel
        if m.text != text { m.text = text }
        if m.symbol != symbol { m.symbol = symbol }
        let accentColor = Color(argb: accentArgb)
        if m.accent != accentColor { m.accent = accentColor }
        if m.dark != dark { m.dark = dark }
    }
}

final class LiquidGlassBridge: NSObject, IosGlassBridge {
    func isLiquidGlassAvailable() -> Bool {
        #if compiler(>=6.2)
        if #available(iOS 26.0, *) { return true }
        #endif
        return false
    }
    func makeTopBar() -> IosGlassHandle { GlassHandle(kind: .topBar) }
    func makeDock() -> IosGlassHandle { GlassHandle(kind: .dock) }
    func makeCartBar() -> IosGlassHandle { GlassHandle(kind: .cartBar) }
    func makeToast() -> IosGlassHandle { GlassHandle(kind: .toast) }
    func makeSearchField() -> IosGlassHandle { GlassHandle(kind: .search) }
    func makeBudgetPanel() -> IosGlassHandle { GlassHandle(kind: .budget) }
    func makeProgressCapsule() -> IosGlassHandle { GlassHandle(kind: .progress) }

    func presentQuickView(title: String, subtitle: String, price: String, imageUrl: String, isFavorite: Bool,
                          accentArgb: Int32, onAdd: @escaping () -> Void, onFavorite: @escaping () -> Void,
                          onShare: @escaping () -> Void) -> Bool {
        guard let top = topViewController() else { return false }
        let sheet = UIHostingController(rootView: QuickViewSheet(
            title: title, subtitle: subtitle, price: price, imageUrl: imageUrl, isFavorite: isFavorite,
            accent: Color(argb: accentArgb), onAdd: onAdd, onFavorite: onFavorite, onShare: onShare))
        if let s = sheet.sheetPresentationController {
            s.detents = [.medium(), .large()]
            s.prefersGrabberVisible = true
        }
        top.present(sheet, animated: true)
        return true
    }

    func startDeliveryActivity(orderId: String, branch: String, stage: String, etaMinutes: Int32, progress: Double) {
        if #available(iOS 16.2, *) { DeliveryActivityController.shared.start(orderId: orderId, branch: branch, stage: stage, eta: Int(etaMinutes), progress: progress) }
    }
    func updateDeliveryActivity(stage: String, etaMinutes: Int32, progress: Double) {
        if #available(iOS 16.2, *) { DeliveryActivityController.shared.update(stage: stage, eta: Int(etaMinutes), progress: progress) }
    }
    func endDeliveryActivity() {
        if #available(iOS 16.2, *) { DeliveryActivityController.shared.end() }
    }
}
