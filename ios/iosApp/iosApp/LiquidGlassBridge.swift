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

// MARK: - Barra superior

final class TopBarModel: ObservableObject {
    struct Action: Identifiable { let id: Int; let symbol: String; let label: String; let text: String; let badge: String; let highlighted: Bool; let tint: Color? }
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
                    .accessibilityLabel(m.backLabel)
                }
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
                .accessibilityElement(children: .combine)

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
                }
            }
            .padding(.horizontal, 12)
        }
        .tint(m.accent)
        .environment(\.colorScheme, m.dark ? .dark : .light)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

// MARK: - Barra inferior

final class DockModel: ObservableObject {
    @Published var symbols: [String] = []
    @Published var labels: [String] = []
    @Published var selected = 0
    @Published var accent = Color.red
    @Published var dark = true
    var onSelect: (Int) -> Void = { _ in }
}

struct GlassDockView: View {
    @ObservedObject var m: DockModel
    @Namespace private var ns
    var body: some View {
        HStack(spacing: 0) {
            ForEach(Array(m.symbols.enumerated()), id: \.offset) { i, symbol in
                Button { m.onSelect(i) } label: {
                    Image(systemName: symbol)
                        .font(.system(size: 20, weight: i == m.selected ? .bold : .medium))
                        .foregroundStyle(i == m.selected ? m.accent : Color.primary)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                        .background {
                            if i == m.selected {
                                Capsule()
                                    .fill(m.accent.opacity(0.16))
                                    .padding(.vertical, 5)
                                    .padding(.horizontal, 2)
                                    .matchedGeometryEffect(id: "seleccion", in: ns)
                            }
                        }
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel(m.labels.indices.contains(i) ? m.labels[i] : "")
                .accessibilityAddTraits(i == m.selected ? .isSelected : [])
            }
        }
        .padding(.horizontal, 6)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .bravoGlass(Capsule())
        .animation(.spring(response: 0.38, dampingFraction: 0.72), value: m.selected)
        .padding(.horizontal, 16)
        .padding(.vertical, 6)
        .tint(m.accent)
        .environment(\.colorScheme, m.dark ? .dark : .light)
    }
}

// MARK: - Puente con Kotlin

final class GlassHandle: NSObject, IosGlassHandle {
    private let topModel = TopBarModel()
    private let dockModel = DockModel()
    private let controller: UIViewController

    init(dock: Bool) {
        controller = dock
            ? GlassHostController(root: GlassDockView(m: dockModel))
            : GlassHostController(root: GlassTopBarView(m: topModel))
        super.init()
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
        m.subtitleColor = Color(argb: subtitleArgb)
        if m.hasBack != hasBack { m.hasBack = hasBack }
        m.backLabel = backLabel
        m.accent = Color(argb: accentArgb)
        if m.dark != dark { m.dark = dark }
        m.actions = symbols.indices.map { i in
            let t = tints.indices.contains(i) ? tints[i].int32Value : 0
            return TopBarModel.Action(
                id: i, symbol: symbols[i],
                label: labels.indices.contains(i) ? labels[i] : "",
                text: texts.indices.contains(i) ? texts[i] : "",
                badge: badges.indices.contains(i) ? badges[i] : "",
                highlighted: highlighted.indices.contains(i) ? highlighted[i].boolValue : false,
                tint: t == 0 ? nil : Color(argb: t))
        }
    }

    func updateDock(symbols: [String], labels: [String], selected: Int32, accentArgb: Int32, dark: Bool,
                    onSelect: @escaping (KotlinInt) -> Void) {
        let m = dockModel
        m.onSelect = { i in onSelect(KotlinInt(int: Int32(i))) }
        if m.symbols != symbols { m.symbols = symbols }
        m.labels = labels
        if m.selected != Int(selected) { m.selected = Int(selected) }
        m.accent = Color(argb: accentArgb)
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
    func makeTopBar() -> IosGlassHandle { GlassHandle(dock: false) }
    func makeDock() -> IosGlassHandle { GlassHandle(dock: true) }
}
