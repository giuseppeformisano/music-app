package com.example.data

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.example.model.User
import com.example.model.UserStats
import kotlinx.coroutines.tasks.await

object AuthRepository {

    private const val WEB_CLIENT_ID = "232939142049-kdmk25nh1i4cdqob1doemmuu7nhj6po7.apps.googleusercontent.com"
    private const val TAG = "AuthRepository"

    val currentFirebaseUser: FirebaseUser?
        get() = FirebaseAuth.getInstance().currentUser

    suspend fun signInWithGoogle(context: Context): Result<User> {
        return try {
            val credentialManager = CredentialManager.create(context)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(WEB_CLIENT_ID)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                val authResult = FirebaseAuth.getInstance().signInWithCredential(firebaseCredential).await()
                val firebaseUser = authResult.user ?: return Result.failure(Exception("Login fallito"))
                Result.success(firebaseUser.toAppUser())
            } else {
                Result.failure(Exception("Tipo di credenziale non supportato"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In error: ${e.message}")
            Result.failure(e)
        }
    }

    fun signOut() {
        FirebaseAuth.getInstance().signOut()
    }

    private fun FirebaseUser.toAppUser(): User {
        val username = email
            ?.substringBefore("@")
            ?.replace(".", "_")
            ?.replace("-", "_")
            ?: uid.take(8)
        return User(
            id = uid,
            name = displayName ?: "Utente",
            username = username,
            email = email ?: "",
            avatarUrl = photoUrl?.toString() ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400",
            isCurrentUser = true,
            isLiveNow = false,
            currentTrack = null,
            sharedTracks = emptyList(),
            stats = UserStats(sharedCount = 0, topArtist = "", totalMinutesOrGenres = "")
        )
    }
}
