package com.example.data.repository

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

object GoogleSignInHelper {
    private const val TAG = "GoogleSignInHelper"

    suspend fun getIdToken(context: Context, serverClientId: String): Result<String> {
        // Tolak hanya jika kosong atau masih placeholder
        if (serverClientId.isBlank() || serverClientId == "YOUR_GOOGLE_WEB_CLIENT_ID") {
            return Result.failure(Exception("Google Sign-In belum dikonfigurasi. Hubungi admin IT."))
        }

        val manager = CredentialManager.create(context)

        // Percobaan 1: pilih akun yang ada di perangkat
        val firstTry = requestToken(
            context = context,
            manager = manager,
            request = GetCredentialRequest.Builder()
                .addCredentialOption(
                    GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(serverClientId)
                        .setAutoSelectEnabled(false)
                        .build()
                )
                .build()
        )

        // Jika gagal karena NoCredentialException, coba alur tombol "Sign in with Google"
        val needFallback = firstTry.exceptionOrNull() is NoCredentialException
        if (!needFallback) return firstTry.toUserResult()

        Log.w(TAG, "GetGoogleIdOption gagal (NoCredential), mencoba GetSignInWithGoogleOption")

        val secondTry = requestToken(
            context = context,
            manager = manager,
            request = GetCredentialRequest.Builder()
                .addCredentialOption(
                    GetSignInWithGoogleOption.Builder(serverClientId).build()
                )
                .build()
        )
        return secondTry.toUserResult()
    }

    private suspend fun requestToken(
        context: Context,
        manager: CredentialManager,
        request: GetCredentialRequest
    ): Result<String> {
        return try {
            val response: GetCredentialResponse = manager.getCredential(context, request)
            val credential = response.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
                Result.success(token)
            } else {
                Result.failure(Exception("Format kredensial tidak sesuai."))
            }
        } catch (e: GetCredentialException) {
            // Log lengkap supaya penyebab asli terlihat di Logcat
            Log.e(TAG, "GetCredentialException type=${e.type} msg=${e.message}", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun Result<String>.toUserResult(): Result<String> {
        val error = exceptionOrNull() ?: return this
        val msg = error.message ?: ""
        val friendly = when {
            error is GetCredentialCancellationException ->
                "Login Google dibatalkan."
            error is NoCredentialException ->
                "Google menolak permintaan ini. Cek package name, SHA-1, dan Android OAuth client di Google Cloud."
            msg.contains("Developer console is not set up correctly", ignoreCase = true) ||
                msg.contains("[10]") ->
                "Konfigurasi OAuth belum cocok. Pastikan SHA-1 dan package name terdaftar."
            msg.contains("Format kredensial") || msg.contains("belum dikonfigurasi") -> msg
            else -> "Gagal login Google: ${error.localizedMessage ?: msg}"
        }
        return Result.failure(Exception(friendly))
    }
}