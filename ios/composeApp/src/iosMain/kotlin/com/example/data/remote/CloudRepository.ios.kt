package com.example.data.remote

import com.example.platform.Log
import com.example.platform.PlatformContext

actual fun handleFirestoreError(exception: Throwable, operationType: OperationType, path: String?): String {
    val json = """{"error":"${exception.message?.replace("\"", "'")}","operationType":"${operationType.value}","path":"${path ?: ""}"}"""
    Log.e("FirestoreError", json)
    return json
}

/** Firestore for iOS is not wired yet: data stays on-device (Room + NSUserDefaults). */
actual fun createCloudRepository(context: PlatformContext): CloudRepository = LocalOnlyCloudRepository()
