package com.example.platform

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await

private const val AUTH_TAG = "BravoGoogleAuth"

private fun FirebaseUser.toAuthUser() = AuthUser(uid = uid, displayName = displayName, email = email)

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

actual object AuthPlatform {
    actual val isSignInAvailable: Boolean = true

    actual fun currentUser(): AuthUser? =
        runCatching { FirebaseAuth.getInstance().currentUser?.toAuthUser() }.getOrNull()

    actual fun userFlow(): Flow<AuthUser?> {
        val auth = runCatching { FirebaseAuth.getInstance() }.getOrNull() ?: return flowOf(null)
        return callbackFlow {
            val listener = FirebaseAuth.AuthStateListener { a -> trySend(a.currentUser?.toAuthUser()) }
            auth.addAuthStateListener(listener)
            awaitClose { auth.removeAuthStateListener(listener) }
        }
    }

    actual suspend fun attemptSilentSignIn(context: PlatformContext): Result<AuthUser?> =
        attemptSilentGoogleSignInImpl(context).map { it?.toAuthUser() }

    actual suspend fun launchSignInSheet(context: PlatformContext, onFallbackSession: () -> Unit): Result<AuthUser?> =
        launchGoogleSignInSheetImpl(context, onFallbackSession).map { it?.toAuthUser() }

    actual suspend fun signOut(context: PlatformContext) {
        try {
            CredentialManager.create(context.applicationContext)
                .clearCredentialState(ClearCredentialStateRequest())
        } catch (e: ClearCredentialException) {
            Log.w(AUTH_TAG, "Failed to clear CredentialManager state", e)
        }
        Firebase.auth.signOut()
    }
}

private suspend fun attemptSilentGoogleSignInImpl(context: Context): Result<FirebaseUser?> {
    val auth = Firebase.auth
    auth.currentUser?.let { return Result.success(it) }

    val activityContext: Context = context.findActivity() ?: context
    val credentialManager = CredentialManager.create(activityContext)
    val serverClientId = context.getString(R.string.default_web_client_id)

    val silentOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(serverClientId)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(silentOption)
        .build()

    return try {
        val result = credentialManager.getCredential(activityContext, request)
        val credential = result.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
            val googleCred = GoogleAuthProvider.getCredential(idToken, null)
            val user = auth.signInWithCredential(googleCred).await().user
            Result.success(user)
        } else {
            Result.success(null)
        }
    } catch (e: NoCredentialException) {
        Log.d(AUTH_TAG, "No authorized Google credentials found for silent sign-in")
        Result.success(null)
    } catch (e: GetCredentialCancellationException) {
        Log.d(AUTH_TAG, "Silent Google Sign-In cancelled or dismissed: ${e.message}")
        Result.success(null)
    } catch (e: GetCredentialException) {
        Log.d(AUTH_TAG, "Silent sign-in unavailable (${e.type}): ${e.message}")
        Result.success(null)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(AUTH_TAG, "Unexpected error during silent sign-in", e)
        Result.failure(e)
    }
}

private suspend fun launchGoogleSignInSheetImpl(context: Context, activateFallback: () -> Unit): Result<FirebaseUser?> {
    val activityContext: Context = context.findActivity() ?: context
    val credentialManager = CredentialManager.create(activityContext)
    val serverClientId = context.getString(R.string.default_web_client_id)

    val googleSignInOption = GetSignInWithGoogleOption.Builder(serverClientId).build()
    val primaryRequest = GetCredentialRequest.Builder()
        .addCredentialOption(googleSignInOption)
        .build()

    try {
        val result = credentialManager.getCredential(activityContext, primaryRequest)
        val credential = result.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
            val googleCred = GoogleAuthProvider.getCredential(idToken, null)
            val user = Firebase.auth.signInWithCredential(googleCred).await().user
            return Result.success(user)
        }
    } catch (e: GetCredentialCancellationException) {
        Log.d(AUTH_TAG, "Google Sign-In sheet dismissed by user: ${e.message}")
        return Result.success(null)
    } catch (e: CancellationException) {
        throw e
    } catch (primaryErr: Exception) {
        Log.d(AUTH_TAG, "Primary GetSignInWithGoogleOption fallback triggered: ${primaryErr.message}")
    }

    // Secondary attempt using GetGoogleIdOption (unfiltered accounts)
    val fallbackGoogleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(serverClientId)
        .setAutoSelectEnabled(false)
        .build()
    val secondaryRequest = GetCredentialRequest.Builder()
        .addCredentialOption(fallbackGoogleIdOption)
        .build()

    return try {
        val result = credentialManager.getCredential(activityContext, secondaryRequest)
        val credential = result.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
            val googleCred = GoogleAuthProvider.getCredential(idToken, null)
            val user = Firebase.auth.signInWithCredential(googleCred).await().user
            Result.success(user)
        } else {
            activateFallback()
            Result.success(null)
        }
    } catch (e: GetCredentialCancellationException) {
        Log.d(AUTH_TAG, "Secondary Google Sign-In sheet cancelled by user: ${e.message}")
        Result.success(null)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.d(AUTH_TAG, "Activating instant Google account session on device without Play account sheet: ${e.message}")
        activateFallback()
        Result.success(null)
    }
}

