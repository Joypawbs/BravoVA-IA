import SwiftUI

/// Paleta base de la marca (la misma de la app anterior; no se cambia).
enum Brand {
    static let bravoRed = Color(hex: 0xE1251B)
    static let bravoRedDark = Color(hex: 0xB71C14)
    static let arcaTeal = Color(hex: 0x0F766E)
    static let positive = Color(hex: 0x16A34A)
}

extension Color {
    init(hex: UInt32, opacity: Double = 1) {
        self.init(.sRGB,
                  red: Double((hex >> 16) & 0xFF) / 255,
                  green: Double((hex >> 8) & 0xFF) / 255,
                  blue: Double(hex & 0xFF) / 255,
                  opacity: opacity)
    }
}
