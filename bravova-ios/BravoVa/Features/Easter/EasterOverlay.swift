import SwiftUI

/// Capa de los huevos de pascua: efecto, frase y huellitas. No bloquea los toques
/// (salvo las huellitas, que se cierran con un toque, como en la 3.1).
struct EasterOverlay: View {
    @State private var easter = Easter.shared

    var body: some View {
        ZStack {
            if let effect = easter.effect {
                EffectCanvas(effect: effect, style: easter.style)
                    .id(easter.serial)
                    .transition(.opacity)
                    .allowsHitTesting(false)
            }
            if let line = easter.line {
                VStack {
                    Spacer()
                    Text(line)
                        .font(.system(.callout, design: .monospaced))
                        .foregroundStyle(.white)
                        .padding(16)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(.black, in: .rect(cornerRadius: 4))
                        .overlay(RoundedRectangle(cornerRadius: 4).stroke(.white, lineWidth: 3))
                        .padding(.horizontal, 20)
                        .padding(.bottom, 110)
                }
                .transition(.opacity)
                .allowsHitTesting(false)
            }
            if easter.pawVisible { PawWalk().transition(.opacity) }
        }
        .ignoresSafeArea()
        .animation(.easeInOut(duration: 0.35), value: easter.serial)
    }
}

/// Huellitas que cruzan la pantalla y el aviso del perro molesto.
private struct PawWalk: View {
    @State private var start = Date()

    var body: some View {
        GeometryReader { geo in
            TimelineView(.animation) { ctx in
                let walk = ctx.date.timeIntervalSince(start).truncatingRemainder(dividingBy: 5.2) / 5.2
                ZStack {
                    Color.black.opacity(0.12)
                    ForEach(0..<9, id: \.self) { i in
                        let t = Double(i) / 8
                        let phase = min(max((walk - t * 0.7) * 3, 0), 1)
                        let alpha = phase <= 0 ? 0 : min(max(1 - max(phase - 0.15, 0), 0), 1)
                        Image(systemName: "pawprint.fill")
                            .font(.system(size: 26))
                            .foregroundStyle(argb(0xFF3A2E2A).opacity(0.75))
                            .rotationEffect(.degrees(35))
                            .opacity(alpha)
                            .position(x: geo.size.width * (0.08 + 0.78 * t) + (i % 2 == 0 ? -9 : 9),
                                      y: geo.size.height * (0.82 - 0.6 * t))
                    }
                    VStack {
                        Spacer()
                        Text("* (Un perro molesto se echó a dormir sobre el logo.)\n* Toca para despertarlo.")
                            .font(.system(.callout, design: .monospaced))
                            .foregroundStyle(.white)
                            .padding(16)
                            .background(.black, in: .rect(cornerRadius: 4))
                            .padding(.horizontal, 20)
                            .padding(.bottom, 120)
                    }
                }
            }
        }
        .contentShape(.rect)
        .onTapGesture { Easter.shared.dismissPaw() }
        .accessibilityAddTraits(.isButton)
        .accessibilityLabel("Un perro molesto se echó a dormir sobre el logo. Toca para despertarlo.")
    }
}

/// Dibujo de cada efecto con figuras simples, como en la 3.1.
private struct EffectCanvas: View {
    let effect: EasterEffect
    let style: ParticleStyle?
    @State private var start = Date()
    @State private var seeds: [Seed] = (0..<40).map { _ in Seed() }

    struct Seed {
        var x = Double.random(in: 0...1), y = Double.random(in: 0...1)
        var speed = Double.random(in: 0.6...1.4), phase = Double.random(in: 0...1)
        var shape = Int.random(in: 0..<100), color = Int.random(in: 0..<100)
        var scale = Double.random(in: 0.7...1.4)
    }

    var body: some View {
        TimelineView(.animation) { ctx in
            let elapsed = ctx.date.timeIntervalSince(start)
            Canvas { g, size in
                switch effect {
                case .spamton: spamton(g, size, elapsed)
                case .muffet: spiders(g, size, elapsed)
                case .ralsei: fluffyStars(g, size, elapsed)
                case .asgore: flowers(g, size, elapsed)
                case .tvStatic: tvStatic(g, size, elapsed)
                case .particles: if let style { particles(g, size, elapsed, style) }
                }
            }
        }
    }

    private func clock(_ e: Double, _ period: Double) -> Double { e.truncatingRemainder(dividingBy: period) / period }

    private func spamton(_ g: GraphicsContext, _ size: CGSize, _ e: Double) {
        let words = ["[[OFERTA]]", "[[GRAN AHORRO]]", "¡¡1997!!", "[[GRATIS*]]", "[[HOT DEAL]]", "¡¡KROMER!!"]
        let phase = Int(clock(e, 0.9) * 6)
        for i in 0..<9 {
            if (i + phase) % 3 == 0 { continue }
            let s = seeds[i]
            let color: Color = (i + phase) % 2 == 0 ? argb(0xFFFFE600) : argb(0xFFFF4FD8)
            let text = g.resolve(Text(words[(s.shape + phase) % words.count])
                .font(.system(size: 22, weight: .black, design: .monospaced)).foregroundColor(color))
            let ts = text.measure(in: size)
            let pos = CGPoint(x: s.x * max(size.width - ts.width, 1), y: 80 + s.y * (size.height - 260))
            var c = g
            c.translateBy(x: pos.x, y: pos.y)
            c.rotate(by: .degrees(i % 2 == 0 ? -8 : 6))
            c.fill(Path(CGRect(x: -10, y: -6, width: ts.width + 20, height: ts.height + 12)), with: .color(.black.opacity(0.78)))
            c.draw(text, at: .zero, anchor: .topLeading)
        }
    }

    private func spiders(_ g: GraphicsContext, _ size: CGSize, _ e: Double) {
        let t = clock(e, 2.6)
        for i in 0..<6 {
            let s = seeds[i]
            let bob = sin((t + s.phase) * 2 * .pi)
            let cx = (0.08 + Double(i) * 0.17 + s.x * 0.04) * size.width
            let cy = size.height * (0.12 + s.y * 0.25 + 0.03 * bob)
            var line = Path(); line.move(to: CGPoint(x: cx, y: 0)); line.addLine(to: CGPoint(x: cx, y: cy))
            g.stroke(line, with: .color(argb(0xCCB8A6E0)), lineWidth: 2)
            let r = 13.0, body = argb(0xFF5B2A86)
            for side in [-1.0, 1.0] {
                for k in 0..<4 {
                    let a = (-50 + Double(k) * 30 + bob * 6) * .pi / 180
                    let knee = CGPoint(x: cx + side * r * 1.5 * cos(a), y: cy + r * 1.2 * sin(a) - r * 0.4)
                    let foot = CGPoint(x: knee.x + side * r * 0.8, y: knee.y + r * 1.1)
                    var leg = Path(); leg.move(to: CGPoint(x: cx, y: cy)); leg.addLine(to: knee); leg.addLine(to: foot)
                    g.stroke(leg, with: .color(body), lineWidth: 3)
                }
            }
            g.fill(circle(CGPoint(x: cx, y: cy), r), with: .color(body))
            g.fill(circle(CGPoint(x: cx, y: cy - r * 0.9), r * 0.62), with: .color(argb(0xFF7E44B3)))
            g.fill(circle(CGPoint(x: cx - r * 0.25, y: cy - r), r * 0.18), with: .color(.white))
            g.fill(circle(CGPoint(x: cx + r * 0.25, y: cy - r), r * 0.18), with: .color(.white))
        }
    }

    private func fluffyStars(_ g: GraphicsContext, _ size: CGSize, _ e: Double) {
        let t = clock(e, 4.2)
        g.fill(Path(CGRect(origin: .zero, size: size)), with: .color(argb(0xFF3DDC97).opacity(0.10)))
        for i in 0..<22 {
            let s = seeds[i]
            let y = (s.y + t * s.speed).truncatingRemainder(dividingBy: 1) * size.height
            let c = CGPoint(x: s.x * size.width + sin((t * 2 + s.x) * 2 * .pi) * 12, y: y)
            g.fill(star(c, 9 * s.speed), with: .color(argb(0xE6FFF3A8)))
        }
    }

    private func flowers(_ g: GraphicsContext, _ size: CGSize, _ e: Double) {
        let t = clock(e, 5.2)
        for i in 0..<16 {
            let s = seeds[i], k = s.speed * 0.85
            let y = (s.y + t * k).truncatingRemainder(dividingBy: 1) * size.height
            let c = CGPoint(x: s.x * size.width + sin((t + s.x) * 2 * .pi) * 20, y: y)
            var ctx = g
            ctx.translateBy(x: c.x, y: c.y)
            ctx.rotate(by: .degrees(t * 360 * k))
            for p in 0..<5 {
                let a = Double(p) * 72 * .pi / 180
                ctx.fill(circle(CGPoint(x: 8 * k * cos(a), y: 8 * k * sin(a)), 7 * k), with: .color(argb(0xFFFFD233)))
            }
            ctx.fill(circle(.zero, 5 * k), with: .color(argb(0xFFF59E0B)))
            ctx.stroke(circle(.zero, 5 * k), with: .color(argb(0xFF8A5A00)), lineWidth: 1.2)
        }
    }

    private func tvStatic(_ g: GraphicsContext, _ size: CGSize, _ e: Double) {
        var rng = SeededRandom(seed: UInt64(e * 25))
        g.fill(Path(CGRect(origin: .zero, size: size)), with: .color(.black.opacity(0.18)))
        for _ in 0..<700 {
            let v = rng.nextDouble()
            let r = CGRect(x: rng.nextDouble() * size.width, y: rng.nextDouble() * size.height, width: 5, height: 3)
            g.fill(Path(r), with: .color(Color(white: v).opacity(0.32)))
        }
        let band = clock(e, 0.4) * size.height
        g.fill(Path(CGRect(x: 0, y: band, width: size.width, height: 22)), with: .color(.white.opacity(0.10)))
    }

    private func particles(_ g: GraphicsContext, _ size: CGSize, _ e: Double, _ st: ParticleStyle) {
        let t = clock(e, st.period)
        if let tint = st.tint { g.fill(Path(CGRect(origin: .zero, size: size)), with: .color(tint)) }
        let center = CGPoint(x: size.width / 2, y: size.height / 2)
        for i in 0..<min(st.count, seeds.count) {
            let s = seeds[i]
            let r = st.size * s.scale
            let p = (s.y + t * s.speed).truncatingRemainder(dividingBy: 1)
            var alpha = 0.92, angle = 0.0
            let pos: CGPoint
            switch st.motion {
            case .fall: pos = CGPoint(x: s.x * size.width + sin((t * 2 + s.phase) * 2 * .pi) * 14, y: p * size.height)
            case .rise: pos = CGPoint(x: s.x * size.width + sin((t * 3 + s.phase) * 2 * .pi) * 10, y: (1 - p) * size.height)
            case .spinFall:
                angle = t * 360 * s.speed + s.phase * 360
                pos = CGPoint(x: s.x * size.width + sin((t + s.phase) * 2 * .pi) * 26, y: p * size.height)
            case .burst:
                let a = s.x * 2 * .pi, d = p * max(size.width, size.height) * 0.6
                alpha = max(0, 1 - p)
                pos = CGPoint(x: center.x + cos(a) * d, y: center.y + sin(a) * d)
            case .jitter:
                let j = sin((t * 40 + s.phase * 10) * .pi) * 7
                angle = j * 3
                pos = CGPoint(x: s.x * size.width + j, y: s.y * size.height - j)
            case .orbit:
                let a = (s.x + t * s.speed) * 2 * .pi, radius = min(size.width, size.height) * (0.22 + s.y * 0.2)
                pos = CGPoint(x: center.x + cos(a) * radius, y: center.y + sin(a) * radius)
            }
            let color = st.colors[s.color % st.colors.count].opacity(alpha)
            var ctx = g
            ctx.translateBy(x: pos.x, y: pos.y)
            ctx.rotate(by: .degrees(angle))
            draw(st.shapes[s.shape % st.shapes.count], in: ctx, r: r, color: color)
        }
    }

    private func draw(_ shape: ParticleShape, in g: GraphicsContext, r: Double, color: Color) {
        let o = CGPoint.zero
        switch shape {
        case .heart: g.fill(heart(o, r), with: .color(color))
        case .spade:
            var f = g; f.rotate(by: .degrees(180)); f.fill(heart(o, r), with: .color(color))
            g.fill(tri(o, r, 0.45, 1.2), with: .color(color))
        case .club:
            g.fill(circle(CGPoint(x: 0, y: -r * 0.5), r * 0.5), with: .color(color))
            g.fill(circle(CGPoint(x: -r * 0.5, y: r * 0.15), r * 0.5), with: .color(color))
            g.fill(circle(CGPoint(x: r * 0.5, y: r * 0.15), r * 0.5), with: .color(color))
            g.fill(tri(o, r, 0.4, 1.1), with: .color(color))
        case .diamond:
            var p = Path(); p.move(to: CGPoint(x: 0, y: -r)); p.addLine(to: CGPoint(x: r * 0.7, y: 0))
            p.addLine(to: CGPoint(x: 0, y: r)); p.addLine(to: CGPoint(x: -r * 0.7, y: 0)); p.closeSubpath()
            g.fill(p, with: .color(color))
        case .snow:
            for k in 0..<3 {
                let a = Double(k) * 60 * .pi / 180
                var p = Path(); p.move(to: CGPoint(x: -r * cos(a), y: -r * sin(a))); p.addLine(to: CGPoint(x: r * cos(a), y: r * sin(a)))
                g.stroke(p, with: .color(color), lineWidth: r * 0.22)
            }
        case .flame:
            g.fill(drop(o, r * 1.2), with: .color(color))
            g.fill(drop(CGPoint(x: 0, y: r * 0.35), r * 0.6), with: .color(argb(0xFFFFE08A)))
        case .spark:
            var p = Path()
            let pts: [(Double, Double)] = [(0, -1), (0.22, -0.22), (1, 0), (0.22, 0.22), (0, 1), (-0.22, 0.22), (-1, 0), (-0.22, -0.22)]
            for (i, q) in pts.enumerated() {
                let pt = CGPoint(x: q.0 * r, y: q.1 * r)
                if i == 0 { p.move(to: pt) } else { p.addLine(to: pt) }
            }
            p.closeSubpath()
            g.fill(p, with: .color(color))
        case .drop: g.fill(drop(o, r), with: .color(color))
        case .feather:
            g.fill(Path(ellipseIn: CGRect(x: -r * 0.35, y: -r, width: r * 0.7, height: r * 2)), with: .color(color))
            var p = Path(); p.move(to: CGPoint(x: 0, y: -r)); p.addLine(to: CGPoint(x: 0, y: r * 1.3))
            g.stroke(p, with: .color(color.opacity(0.6)), lineWidth: r * 0.12)
        case .pellet: g.fill(circle(o, r * 0.45), with: .color(color))
        case .square: g.fill(Path(CGRect(x: -r / 2, y: -r / 2, width: r, height: r)), with: .color(color))
        case .spear:
            var p = Path(); p.move(to: CGPoint(x: 0, y: r * 1.6)); p.addLine(to: CGPoint(x: 0, y: -r * 0.6))
            g.stroke(p, with: .color(color), lineWidth: r * 0.22)
            var h = Path(); h.move(to: CGPoint(x: 0, y: -r * 1.4)); h.addLine(to: CGPoint(x: -r * 0.4, y: -r * 0.5))
            h.addLine(to: CGPoint(x: r * 0.4, y: -r * 0.5)); h.closeSubpath()
            g.fill(h, with: .color(color))
        case .star: g.fill(star(o, r), with: .color(color))
        }
    }

    private func circle(_ c: CGPoint, _ r: Double) -> Path { Path(ellipseIn: CGRect(x: c.x - r, y: c.y - r, width: r * 2, height: r * 2)) }

    private func tri(_ c: CGPoint, _ r: Double, _ w: Double, _ h: Double) -> Path {
        var p = Path(); p.move(to: c); p.addLine(to: CGPoint(x: c.x - r * w, y: c.y + r * h))
        p.addLine(to: CGPoint(x: c.x + r * w, y: c.y + r * h)); p.closeSubpath(); return p
    }

    private func heart(_ c: CGPoint, _ r: Double) -> Path {
        var p = Path()
        p.move(to: CGPoint(x: c.x, y: c.y + r * 0.9))
        p.addCurve(to: CGPoint(x: c.x, y: c.y - r * 0.4), control1: CGPoint(x: c.x - r * 1.4, y: c.y - r * 0.1), control2: CGPoint(x: c.x - r * 0.6, y: c.y - r * 1.2))
        p.addCurve(to: CGPoint(x: c.x, y: c.y + r * 0.9), control1: CGPoint(x: c.x + r * 0.6, y: c.y - r * 1.2), control2: CGPoint(x: c.x + r * 1.4, y: c.y - r * 0.1))
        p.closeSubpath()
        return p
    }

    private func drop(_ c: CGPoint, _ r: Double) -> Path {
        var p = Path()
        p.move(to: CGPoint(x: c.x, y: c.y - r * 1.3))
        p.addCurve(to: CGPoint(x: c.x, y: c.y + r * 0.9), control1: CGPoint(x: c.x + r * 0.9, y: c.y), control2: CGPoint(x: c.x + r * 0.7, y: c.y + r * 0.9))
        p.addCurve(to: CGPoint(x: c.x, y: c.y - r * 1.3), control1: CGPoint(x: c.x - r * 0.7, y: c.y + r * 0.9), control2: CGPoint(x: c.x - r * 0.9, y: c.y))
        p.closeSubpath()
        return p
    }

    private func star(_ c: CGPoint, _ r: Double) -> Path {
        var p = Path()
        for i in 0..<10 {
            let rr = i % 2 == 0 ? r : r * 0.45
            let a = (-90 + Double(i) * 36) * .pi / 180
            let pt = CGPoint(x: c.x + rr * cos(a), y: c.y + rr * sin(a))
            if i == 0 { p.move(to: pt) } else { p.addLine(to: pt) }
        }
        p.closeSubpath()
        return p
    }
}
