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
    var body: some View {
        BravoGlassGroup(spacing: 12) {
            HStack(spacing: 10) {
                HStack(spacing: 0) {
                    ForEach(Array(m.symbols.enumerated()), id: \.offset) { i, symbol in
                        if !m.minimized || i == m.selected {
                            tab(i, symbol)
                        }
                    }
                }
                .padding(.horizontal, m.minimized ? 0 : 6)
                .frame(width: m.minimized ? 58 : nil)
                .frame(maxWidth: m.minimized ? 58 : .infinity, maxHeight: .infinity)
                .bravoGlass(Capsule())

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
    }

    private func tab(_ i: Int, _ symbol: String) -> some View {
        Button { m.onSelect(i) } label: {
            VStack(spacing: 2) {
                Image(systemName: i == m.selected && UIImage(systemName: symbol + ".fill") != nil ? symbol + ".fill" : symbol)
                    .symbolRenderingMode(.hierarchical)
                    .font(.system(size: 19, weight: i == m.selected ? .semibold : .regular))
                if !m.minimized, m.labels.indices.contains(i) {
                    Text(m.labels[i]).font(.system(size: 10, weight: .semibold)).lineLimit(1)
                }
            }
            .foregroundStyle(i == m.selected ? m.accent : Color.primary)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background {
                if i == m.selected && !m.minimized {
                    Capsule()
                        .fill(m.accent.opacity(0.14))
                        .padding(.vertical, 4)
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
            Text(m.text).font(.system(size: 14, weight: .semibold)).lineLimit(2)
        }
        .padding(.horizontal, 18).padding(.vertical, 12)
        .bravoGlass(Capsule(), interactive: false)
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

// MARK: - Puente con Kotlin

final class GlassHandle: NSObject, IosGlassHandle {
    enum Kind { case topBar, dock, cartBar, toast }
    private let topModel = TopBarModel()
    private let dockModel = DockModel()
    private let cartModel = CartBarModel()
    private let toastModel = ToastModel()
    private let controller: UIViewController

    init(kind: Kind) {
        switch kind {
        case .topBar: controller = GlassHostController(root: GlassTopBarView(m: topModel))
        case .dock: controller = GlassHostController(root: GlassDockView(m: dockModel))
        case .cartBar: controller = GlassHostController(root: GlassCartBarView(m: cartModel))
        case .toast: controller = GlassHostController(root: GlassToastView(m: toastModel))
        }
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
                    minimized: Bool, onSelect: @escaping (KotlinInt) -> Void, onSearch: @escaping () -> Void) {
        let m = dockModel
        m.onSelect = { i in onSelect(KotlinInt(int: Int32(i))) }
        m.onSearch = onSearch
        if m.symbols != symbols { m.symbols = symbols }
        m.labels = labels
        if m.selected != Int(selected) { m.selected = Int(selected) }
        if m.minimized != minimized { m.minimized = minimized }
        m.accent = Color(argb: accentArgb)
        if m.dark != dark { m.dark = dark }
    }

    func updateCartBar(count: Int32, total: String, accentArgb: Int32, dark: Bool, onTap: @escaping () -> Void) {
        let m = cartModel
        m.onTap = onTap
        if m.count != Int(count) { m.count = Int(count) }
        if m.total != total { m.total = total }
        m.accent = Color(argb: accentArgb)
        if m.dark != dark { m.dark = dark }
    }

    func updateToast(text: String, symbol: String, accentArgb: Int32, dark: Bool) {
        let m = toastModel
        if m.text != text { m.text = text }
        if m.symbol != symbol { m.symbol = symbol }
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
    func makeTopBar() -> IosGlassHandle { GlassHandle(kind: .topBar) }
    func makeDock() -> IosGlassHandle { GlassHandle(kind: .dock) }
    func makeCartBar() -> IosGlassHandle { GlassHandle(kind: .cartBar) }
    func makeToast() -> IosGlassHandle { GlassHandle(kind: .toast) }

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
