package com.example.data.repository

import android.util.Log
import com.example.model.User
import com.example.model.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object AuthRepository {
    private val auth: FirebaseAuth? by lazy {
        try { FirebaseAuth.getInstance() } catch (e: Exception) { null }
    }
    private val firestore: FirebaseFirestore? by lazy {
        try { FirebaseFirestore.getInstance() } catch (e: Exception) { null }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Null = belum login. Guest juga disimpan di sini, tetapi tanpa sesi Firebase.
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isInitializing = MutableStateFlow(true)
    val isInitializing: StateFlow<Boolean> = _isInitializing.asStateFlow()

    init {
        checkCurrentUser()
        auth?.addAuthStateListener { firebaseAuth ->
            scope.launch {
                val firebaseUser = firebaseAuth.currentUser
                if (firebaseUser == null) {
                    _currentUser.value = null
                } else {
                    val profile = fetchOrCreateProfile(firebaseUser.uid, firebaseUser)
                    _currentUser.value = profile
                }
            }
        }
    }

    private fun checkCurrentUser() {
        scope.launch {
            val firebaseUser = auth?.currentUser
            _currentUser.value = if (firebaseUser == null) null else fetchOrCreateProfile(firebaseUser.uid, firebaseUser)
            _isInitializing.value = false
        }
    }

    /** Login email & password. */
    suspend fun login(email: String, password: String): Result<User> {
        return withContext(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val authInstance = auth ?: throw Exception("Firebase Auth belum siap")
                val result = authInstance.signInWithEmailAndPassword(email, password).await()
                val firebaseUser = result.user ?: throw Exception("User tidak ditemukan")
                val user = fetchOrCreateProfile(firebaseUser.uid, firebaseUser)
                _isLoading.value = false
                _currentUser.value = user
                Result.success(user)
            } catch (e: Exception) {
                _isLoading.value = false
                Result.failure(mapLoginError(e))
            }
        }
    }

    /** Registrasi akun baru. Hanya role MENTOR yang dapat dibuat sendiri. */
    suspend fun register(name: String, email: String, password: String): Result<User> {
        return withContext(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val authInstance = auth ?: throw Exception("Firebase Auth belum siap")
                val result = authInstance.createUserWithEmailAndPassword(email, password).await()
                val firebaseUser = result.user ?: throw Exception("User tidak ditemukan")

                try {
                    firebaseUser.updateProfile(
                        UserProfileChangeRequest.Builder()
                            .setDisplayName(name.trim())
                            .build()
                    ).await()
                } catch (e: Exception) {
                    Log.w("AuthRepository", "Gagal update profil nama: ${e.message}")
                }

                val user = createProfileIfMissing(firebaseUser, name.trim(), email.trim(), UserRole.MENTOR, null, "")
                _isLoading.value = false
                _currentUser.value = user
                Result.success(user)
            } catch (e: Exception) {
                _isLoading.value = false
                Result.failure(mapRegisterError(e))
            }
        }
    }

    /** Login dengan token ID Google dari Credential Manager. */
    suspend fun signInWithGoogle(idToken: String): Result<User> {
        return withContext(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val authInstance = auth ?: throw Exception("Firebase Auth belum siap")
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val result = authInstance.signInWithCredential(credential).await()
                val firebaseUser = result.user ?: throw Exception("User tidak ditemukan")
                val user = fetchOrCreateProfile(firebaseUser.uid, firebaseUser)
                _isLoading.value = false
                _currentUser.value = user
                Result.success(user)
            } catch (e: Exception) {
                _isLoading.value = false
                Result.failure(mapLoginError(e))
            }
        }
    }

    fun loginAsGuest() {
        _currentUser.value = User(
            id = "guest_local",
            name = "Tamu (Guest)",
            email = "guest@seiri.local",
            role = UserRole.GUEST,
            assignedAreaId = null,
            department = "Visitor"
        )
    }

    fun logout() {
        auth?.signOut()
        _currentUser.value = null
    }

    private suspend fun fetchOrCreateProfile(
        uid: String,
        firebaseUser: FirebaseUser
    ): User {
        val existing = fetchProfile(uid)
        if (existing != null) return existing

        val displayName = firebaseUser.displayName?.trim().takeIf { it?.isNotEmpty() == true }
        val emailPart = firebaseUser.email
        val name = if (displayName.isNullOrBlank()) {
            if (emailPart == null || emailPart.isBlank()) "Pengguna Baru" else emailPart!!.substringBefore("@")
        } else {
            displayName
        }
        return createProfileIfMissing(
            firebaseUser,
            name,
            firebaseUser.email ?: "",
            UserRole.MENTOR,
            null,
            ""
        )
    }

    private suspend fun createProfileIfMissing(
        firebaseUser: FirebaseUser,
        name: String,
        email: String,
        role: UserRole,
        assignedAreaId: String?,
        department: String
    ): User {
        val db = firestore
        val data = mapOf(
            "name" to name,
            "email" to email,
            "role" to role.name,
            "assignedAreaId" to assignedAreaId,
            "department" to department
        )
        if (db != null) {
            try {
                db.collection("users").document(firebaseUser.uid).set(data).await()
            } catch (e: Exception) {
                Log.e("AuthRepository", "Gagal simpan profil ke Firestore: ${e.message}")
            }
        }
        return User(
            id = firebaseUser.uid,
            name = name,
            email = email,
            role = role,
            assignedAreaId = assignedAreaId,
            department = department
        )
    }

    private suspend fun fetchProfile(uid: String): User? {
        return try {
            val db = firestore ?: return null
            val doc = db.collection("users").document(uid).get().await()
            if (!doc.exists()) return null

            val name = doc.getString("name") ?: "Unknown User"
            val email = doc.getString("email") ?: ""
            val roleStr = doc.getString("role") ?: "MENTOR"
            val assignedAreaId = doc.getString("assignedAreaId")
            val department = doc.getString("department") ?: ""

            val role = when (roleStr.uppercase()) {
                "ADMIN" -> UserRole.ADMIN
                "MENTOR", "USER_CI" -> UserRole.MENTOR
                "GUEST" -> UserRole.GUEST
                else -> UserRole.MENTOR
            }

            User(
                id = uid,
                name = name,
                email = email,
                role = role,
                assignedAreaId = assignedAreaId,
                department = department
            )
        } catch (e: Exception) {
            Log.e("AuthRepository", "Gagal ambil profil: ${e.message}")
            null
        }
    }

    private fun mapLoginError(e: Exception): Exception {
        val message = e.message ?: "Login gagal"
        return Exception(when {
            message.contains("INVALID_PASSWORD", ignoreCase = true) ||
                message.contains("WRONG_PASSWORD", ignoreCase = true) -> "Email atau password salah."
            message.contains("USER_NOT_FOUND", ignoreCase = true) ||
                message.contains("EMAIL_NOT_FOUND", ignoreCase = true) -> "Email tidak terdaftar."
            message.contains("INVALID_EMAIL", ignoreCase = true) -> "Format email tidak valid."
            message.contains("TOO_MANY_REQUESTS", ignoreCase = true) -> "Terlalu banyak percobaan. Coba lagi nanti."
            message.contains("NETWORK_REQUEST_FAILED", ignoreCase = true) -> "Tidak ada koneksi internet."
            else -> message
        })
    }

    private fun mapRegisterError(e: Exception): Exception {
        val message = e.message ?: "Registrasi gagal"
        return Exception(when {
            message.contains("EMAIL_EXISTS", ignoreCase = true) ||
                message.contains("ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL", ignoreCase = true) -> "Email sudah digunakan."
            message.contains("WEAK_PASSWORD", ignoreCase = true) -> "Password terlalu lemah. Minimal 6 karakter."
            message.contains("INVALID_EMAIL", ignoreCase = true) -> "Format email tidak valid."
            message.contains("NETWORK_REQUEST_FAILED", ignoreCase = true) -> "Tidak ada koneksi internet."
            else -> message
        })
    }
}
