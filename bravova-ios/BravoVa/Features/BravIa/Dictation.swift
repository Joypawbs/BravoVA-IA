import Foundation
import Speech
import AVFoundation
import Observation

/// Dictado por voz para Brav IA (es-DO). Pide permiso de micrófono y de reconocimiento la
/// primera vez; si el iPhone lo permite, reconoce en el propio teléfono.
@MainActor
@Observable
final class Dictation {
    private(set) var isRecording = false
    private(set) var text = ""
    private(set) var error: String?

    private let recognizer = SFSpeechRecognizer(locale: Locale(identifier: "es-DO")) ?? SFSpeechRecognizer(locale: Locale(identifier: "es-ES"))
    private let engine = AVAudioEngine()
    private var request: SFSpeechAudioBufferRecognitionRequest?
    private var task: SFSpeechRecognitionTask?

    func toggle() async {
        if isRecording { stop() } else { await start() }
    }

    func start() async {
        error = nil
        let speech = await withCheckedContinuation { c in
            SFSpeechRecognizer.requestAuthorization { c.resume(returning: $0) }
        }
        guard speech == .authorized else { error = "Activa el reconocimiento de voz en Ajustes para dictar."; return }
        guard await AVAudioApplication.requestRecordPermission() else {
            error = "Activa el micrófono en Ajustes para dictar."; return
        }
        guard let recognizer, recognizer.isAvailable else { error = "El dictado no está disponible ahora."; return }

        do {
            let session = AVAudioSession.sharedInstance()
            try session.setCategory(.record, mode: .measurement, options: .duckOthers)
            try session.setActive(true, options: .notifyOthersOnDeactivation)

            let req = SFSpeechAudioBufferRecognitionRequest()
            req.shouldReportPartialResults = true
            if recognizer.supportsOnDeviceRecognition { req.requiresOnDeviceRecognition = true }
            request = req

            Self.installTap(on: engine.inputNode, feeding: req)
            engine.prepare()
            try engine.start()
            text = ""
            isRecording = true

            task = Self.recognize(recognizer, req) { [weak self] t, done in
                guard let self else { return }
                if let t { self.text = t }
                if done { self.stop() }
            }
        } catch {
            self.error = "No se pudo iniciar el dictado."
            stop()
        }
    }

    /// Fuera del actor principal: el audio llega en otro hilo.
    nonisolated private static func installTap(on input: AVAudioInputNode, feeding req: SFSpeechAudioBufferRecognitionRequest) {
        input.removeTap(onBus: 0)
        input.installTap(onBus: 0, bufferSize: 1024, format: input.outputFormat(forBus: 0)) { buffer, _ in
            req.append(buffer)
        }
    }

    nonisolated private static func recognize(_ recognizer: SFSpeechRecognizer, _ req: SFSpeechAudioBufferRecognitionRequest,
                                              update: @escaping @MainActor (String?, Bool) -> Void) -> SFSpeechRecognitionTask {
        recognizer.recognitionTask(with: req) { result, err in
            let t = result?.bestTranscription.formattedString
            let done = err != nil || (result?.isFinal ?? false)
            Task { @MainActor in update(t, done) }
        }
    }

    func stop() {
        guard isRecording || task != nil else { return }
        engine.stop()
        engine.inputNode.removeTap(onBus: 0)
        request?.endAudio()
        task?.cancel()
        request = nil
        task = nil
        isRecording = false
        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
    }
}
