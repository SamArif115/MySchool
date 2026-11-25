package com.example.data

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

sealed interface GoogleSignInResult {
    data class Success(val user: FirebaseUser) : GoogleSignInResult
    data object Cancelled : GoogleSignInResult
    data class Error(val message: String) : GoogleSignInResult
}

class GoogleAuthHelper(
    private val context: Context,
    private val auth: FirebaseAuth
) {
    private val credentialManager = CredentialManager.create(context)

    private fun webClientId(): String = context.getString(R.string.default_web_client_id)

    /**
     * Explicit interactive Google Sign-In button flow using GetSignInWithGoogleOption.
     */
    suspend fun signInInteractive(activityContext: Context): GoogleSignInResult {
        return try {
            val googleOption = GetSignInWithGoogleOption.Builder(webClientId())
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleOption)
                .build()
            val response = credentialManager.getCredential(activityContext, request)
            handleCredential(response.credential)
        } catch (e: GetCredentialCancellationException) {
            GoogleSignInResult.Cancelled
        } catch (e: NoCredentialException) {
            GoogleSignInResult.Error("Tidak ditemukan akun Google di perangkat ini. Silakan tambahkan akun Google di Pengaturan.")
        } catch (e: GetCredentialException) {
            Log.e("GoogleAuthHelper", "Credential error", e)
            GoogleSignInResult.Error(e.localizedMessage ?: "Gagal membuka Google Sign-In.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("GoogleAuthHelper", "Unexpected sign-in error", e)
            GoogleSignInResult.Error(e.localizedMessage ?: "Gagal autentikasi Google.")
        }
    }

    /**
     * Silent auto sign-in for returning users via GetGoogleIdOption.
     */
    suspend fun attemptSilentSignIn(activityContext: Context): GoogleSignInResult {
        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(webClientId())
                .setAutoSelectEnabled(true)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            val response = credentialManager.getCredential(activityContext, request)
            handleCredential(response.credential)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            GoogleSignInResult.Cancelled
        }
    }

    private suspend fun handleCredential(credential: androidx.credentials.Credential): GoogleSignInResult {
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleCred = GoogleIdTokenCredential.createFrom(credential.data)
            val firebaseCred = GoogleAuthProvider.getCredential(googleCred.idToken, null)
            val authResult = auth.signInWithCredential(firebaseCred).await()
            val user = authResult.user
            return if (user != null) {
                GoogleSignInResult.Success(user)
            } else {
                GoogleSignInResult.Error("Firebase user kosong setelah login Google.")
            }
        }
        return GoogleSignInResult.Error("Tipe kredensial tidak didukung.")
    }

    suspend fun signOut() {
        withContext(NonCancellable) {
            auth.signOut()
            runCatching {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            }
        }
    }
}
