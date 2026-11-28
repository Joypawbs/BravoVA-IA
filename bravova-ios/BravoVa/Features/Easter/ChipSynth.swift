import Foundation
import AVFoundation

/// Sintetizador de 8 bits de los huevos de pascua (el mismo de la 3.1, en Swift).
/// Las melodías son originales y se generan con matemáticas: no se usa audio de nadie.
enum Wave { case square, pulse, triangle, saw, noise }

struct ChipNote {
    var note: String
    var ms: Int
    var wave: Wave = .square
    var volume: Double = 1
    /// Semitonos que sube (o baja) mientras suena
    var slide: Double = 0
}

enum ChipSynth {
    static let sampleRate = 22_050
    private static let master = 0.28
    private static var player: AVAudioPlayer?

    private static let index: [String: Int] = [
        "C": 0, "C#": 1, "DB": 1, "D": 2, "D#": 3, "EB": 3, "E": 4, "F": 5,
        "F#": 6, "GB": 6, "G": 7, "G#": 8, "AB": 8, "A": 9, "A#": 10, "BB": 10, "B": 11
    ]

    static func frequency(_ note: String) -> Double? {
        let n = note.trimmingCharacters(in: .whitespaces).uppercased()
        guard !n.isEmpty, n != "-" else { return nil }
        let name = String(n.prefix { !$0.isNumber && $0 != "-" })
        let octave = Int(n.dropFirst(name.count)) ?? 4
        guard let idx = index[name] else { return nil }
        let midi = (octave + 1) * 12 + idx
        return 440 * pow(2, Double(midi - 69) / 12)
    }

    static func seq(_ notes: String, _ ms: Int, _ wave: Wave = .square, _ volume: Double = 1) -> [ChipNote] {
        notes.split(whereSeparator: \.isWhitespace).map { ChipNote(note: String($0), ms: ms, wave: wave, volume: volume) }
    }

    static func render(_ notes: [ChipNote]) -> [Int16] {
        let total = notes.reduce(0) { $0 + $1.ms } * sampleRate / 1000
        var out = [Int16](repeating: 0, count: total + sampleRate / 20)
        var pos = 0
        var rng = SeededRandom(seed: 7)
        var noise = 0.0
        for n in notes {
            let count = n.ms * sampleRate / 1000
            guard let base = frequency(n.note) else { pos += count; continue }
            var phase = 0.0
            for i in 0..<count {
                let progress = Double(i) / Double(count)
                let freq = base * pow(2, n.slide * progress / 12)
                phase += freq / Double(sampleRate)
                let p = phase - floor(phase)
                let raw: Double
                switch n.wave {
                case .square: raw = p < 0.5 ? 1 : -1
                case .pulse: raw = p < 0.25 ? 1 : -1
                case .triangle: raw = p < 0.5 ? 4 * p - 1 : 3 - 4 * p
                case .saw: raw = 2 * p - 1
                case .noise:
                    let step = max(1, Int(Double(sampleRate) / max(freq, 60)))
                    if i % step == 0 { noise = rng.nextDouble() * 2 - 1 }
                    raw = noise
                }
                let attack = min(1, Double(i) / (Double(sampleRate) * 0.005))
                let release = exp(-3.2 * progress)
                let v = raw * attack * release * n.volume * master
                if pos < out.count { out[pos] = Int16(max(-32767, min(32767, v * 32767))) }
                pos += 1
            }
        }
        return out
    }

    /// Respeta el interruptor de silencio (categoría "ambient").
    static func play(_ notes: [ChipNote]) {
        let samples = render(notes)
        try? AVAudioSession.sharedInstance().setCategory(.ambient, options: .mixWithOthers)
        try? AVAudioSession.sharedInstance().setActive(true)
        player = try? AVAudioPlayer(data: wav(samples))
        player?.play()
    }

    private static func wav(_ samples: [Int16]) -> Data {
        var d = Data()
        func u32(_ v: UInt32) { var x = v.littleEndian; d.append(Data(bytes: &x, count: 4)) }
        func u16(_ v: UInt16) { var x = v.littleEndian; d.append(Data(bytes: &x, count: 2)) }
        let bytes = UInt32(samples.count * 2)
        d.append(contentsOf: Array("RIFF".utf8)); u32(36 + bytes)
        d.append(contentsOf: Array("WAVEfmt ".utf8)); u32(16); u16(1); u16(1)
        u32(UInt32(sampleRate)); u32(UInt32(sampleRate * 2)); u16(2); u16(16)
        d.append(contentsOf: Array("data".utf8)); u32(bytes)
        samples.withUnsafeBufferPointer { d.append(Data(buffer: $0)) }
        return d
    }
}

/// Aleatorio con semilla fija (el ruido suena igual cada vez).
struct SeededRandom {
    private var state: UInt64
    init(seed: UInt64) { state = seed &* 0x9E3779B97F4A7C15 | 1 }
    mutating func next() -> UInt64 {
        state ^= state << 13; state ^= state >> 7; state ^= state << 17
        return state
    }
    mutating func nextDouble() -> Double { Double(next() % 1_000_000) / 1_000_000 }
}
