import Foundation
import UIKit
import FirebaseAI
import ComposeApp

/// Implements the Kotlin `IosAiBridge` with the official Firebase AI Logic SDK
/// (Gemini Developer API backend, same as `Firebase.ai` on Android).
final class FirebaseAiBridge: NSObject, IosAiBridge {
    private let ai = FirebaseAI.firebaseAI(backend: .googleAI())

    func generate(
        model: String,
        prompt: String,
        imageJpeg: Data?,
        onChunk: @escaping (String) -> Void,
        onComplete: @escaping (String?) -> Void
    ) {
        let generativeModel = ai.generativeModel(modelName: model)
        Task {
            do {
                let stream: AsyncThrowingStream<GenerateContentResponse, Error>
                if let data = imageJpeg, let image = UIImage(data: data) {
                    stream = try generativeModel.generateContentStream(image, prompt)
                } else {
                    stream = try generativeModel.generateContentStream(prompt)
                }
                for try await chunk in stream {
                    if let text = chunk.text, !text.isEmpty {
                        await MainActor.run { onChunk(text) }
                    }
                }
                await MainActor.run { onComplete(nil) }
            } catch {
                await MainActor.run { onComplete(error.localizedDescription) }
            }
        }
    }
}
