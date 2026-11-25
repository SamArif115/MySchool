package com.example.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write")
}

fun handleFirestoreError(
    error: Throwable,
    operationType: OperationType,
    path: String?,
    auth: FirebaseAuth? = null
): String {
    val currentUser = auth?.currentUser
    val payload = JSONObject().apply {
        put("error", error.message ?: error.toString())
        put("operationType", operationType.value)
        put("path", path ?: "")
        put("authInfo", JSONObject().apply {
            put("userId", currentUser?.uid ?: "")
            put("email", currentUser?.email ?: "")
            put("emailVerified", currentUser?.isEmailVerified ?: false)
            put("providerId", currentUser?.providerId ?: "")
        })
    }
    Log.e("FirestoreError", "Firestore Error: $payload", error)
    return when {
        error.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true ->
            "Akses ditolak oleh Firestore Security Rules (${operationType.value}: ${path ?: "dokumen"}). Periksa hak akses role Anda."
        error.message?.contains("UNAVAILABLE", ignoreCase = true) == true ->
            "Koneksi Firebase sedang terganggu. Data akan disinkronkan ulang saat online."
        else -> error.localizedMessage ?: "Gagal sinkronisasi ke Firebase Firestore."
    }
}
