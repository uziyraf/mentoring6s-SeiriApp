package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.AuthSessionCache
import com.example.model.User
import com.example.model.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object AuthRepository {
    private val auth: FirebaseAuth? by lazy {
        try { FirebaseAuth.getInstance() } catch (e: Exception) { null }
    }
    private val firestore: FirebaseFirestore? by lazy {
        try { FirebaseFirestore.getInstance() } catch (e: Exception) { null }
    }

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isInitializing = MutableStateFlow(true)
    val isInitializing: StateFlow<Boolean> = _isInitializing.asStateFlow()

    private var isInitialized = false

    fun initialize(context: Context) {
        if (isInitialized) return
        AuthSessionCache.initialize(context)
        
        auth?.addAuthStateListener { firebaseAuth ->
            if (firebaseAuth.currentUser == null && !AuthSessionCache.isGuest()) {
                _currentUser.value = null
                AuthSessionCache.clearProfile()
            }
        }
        checkCurrentUser()
        isInitialized = true
    }

    private fun checkCurrentUser() {
        try {
            if (AuthSessionCache.isGuest()) {
                val guestUser = AuthSessionCache.loadProfile() ?: User(
                    id = "guest_local", 
                    name = "Tamu (Guest)", 
                    email = "guest@seiri.local", 
                    role = UserRole.GUEST, 
                    department = "Visitor"
                )
                _currentUser.value = guestUser
                _isInitializing.value = false
                return
            }

            val cachedUser = AuthSessionCache.loadProfile()
            if (cachedUser != null) {
                _currentUser.value = cachedUser
                _isInitializing.value = false
            }

            val firebaseUser = auth?.currentUser
            if (firebaseUser != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    val freshUser = fetchProfile(firebaseUser.uid)
                    if (freshUser != null) {
                        _currentUser.value = freshUser
                        AuthSessionCache.saveProfile(freshUser)
                    }
                    if (cachedUser == null) _isInitializing.value = false
                }
            } else if (cachedUser == null) {
                _currentUser.value = null
                _isInitializing.value = false
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error checkCurrentUser: ${e.message}")
            _isInitializing.value = false
        }
    }

    private suspend fun fetchProfile(uid: String): User? {
        return try {
            val db = firestore ?: return null
            val doc = db.collection("users").document(uid).get().await()
            if (doc.exists()) {
                val name = doc.getString("name") ?: "Unknown User"
                val email = doc.getString("email") ?: ""
                val roleStr = doc.getString("role") ?: "MENTOR"
                val assignedAreaId = doc.getString("assignedAreaId")
                val department = doc.getString("department") ?: "Produksi"

                // 🔴 Perbaikan Mapping Role (USER_CI -> MENTOR)
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
            } else null
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error fetchProfile: ${e.message}")
            null
        }
    }

    private suspend fun fetchOrCreateProfile(uid: String, firebaseUser: FirebaseUser): User {
        val existing = fetchProfile(uid)
        if (existing != null) return existing
        
        val newUser = User(
            id = uid,
            name = firebaseUser.displayName ?: "User Baru",
            email = firebaseUser.email ?: "",
            role = UserRole.MENTOR, // Default Role
            department = "Produksi"
        )
        firestore?.collection("users")?.document(uid)?.set(newUser)?.await()
        return newUser
    }

    suspend fun login(email: String, password: String): Result<User> {
        return withContext(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val authInstance = auth ?: throw Exception("Firebase Auth belum siap")
                val result = authInstance.signInWithEmailAndPassword(email, password).await()
                val firebaseUser = result.user ?: throw Exception("User tidak ditemukan")
                val user = fetchOrCreateProfile(firebaseUser.uid, firebaseUser)
                
                AuthSessionCache.saveProfile(user)
                AuthSessionCache.setGuest(false)
                
                _isLoading.value = false
                _currentUser.value = user
                Result.success(user)
            } catch (e: Exception) {
                _isLoading.value = false
                Result.failure(Exception(mapLoginError(e)))
            }
        }
    }

    // 🔴 Perbaikan Urutan Parameter Register (name, email, password)
    suspend fun register(name: String, email: String, password: String): Result<User> {
        return withContext(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val authInstance = auth ?: throw Exception("Firebase Auth belum siap")
                val result = authInstance.createUserWithEmailAndPassword(email, password).await()
                val firebaseUser = result.user ?: throw Exception("Registrasi gagal")
                
                val user = User(
                    id = firebaseUser.uid,
                    name = name,
                    email = email,
                    role = UserRole.MENTOR, // Default Role
                    department = "Produksi"
                )
                firestore?.collection("users")?.document(user.id)?.set(user)?.await()
                
                AuthSessionCache.saveProfile(user)
                AuthSessionCache.setGuest(false)
                
                _isLoading.value = false
                _currentUser.value = user
                Result.success(user)
            } catch (e: Exception) {
                _isLoading.value = false
                Result.failure(Exception(e.localizedMessage ?: "Registrasi gagal"))
            }
        }
    }

    // 🔴 Mengembalikan fungsi signInWithGoogle yang sempat terhapus
    suspend fun signInWithGoogle(idToken: String): Result<User> {
        return withContext(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val authInstance = auth ?: throw Exception("Firebase Auth belum siap")
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val result = authInstance.signInWithCredential(credential).await()
                val firebaseUser = result.user ?: throw Exception("User Google tidak ditemukan")
                
                val user = fetchOrCreateProfile(firebaseUser.uid, firebaseUser)
                
                AuthSessionCache.saveProfile(user)
                AuthSessionCache.setGuest(false)
                
                _isLoading.value = false
                _currentUser.value = user
                Result.success(user)
            } catch (e: Exception) {
                _isLoading.value = false
                Result.failure(Exception(mapLoginError(e)))
            }
        }
    }

    fun loginAsGuest() {
        val guestUser = User(
            id = "guest_local",
            name = "Tamu (Guest)",
            email = "guest@seiri.local",
            role = UserRole.GUEST,
            assignedAreaId = null,
            department = "Visitor"
        )
        _currentUser.value = guestUser
        AuthSessionCache.saveProfile(guestUser)
        AuthSessionCache.setGuest(true)
    }

    fun logout() {
        auth?.signOut()
        _currentUser.value = null
        AuthSessionCache.clearProfile()
        AuthSessionCache.setGuest(false)
    }

    private fun mapLoginError(e: Exception): String {
        return e.localizedMessage ?: "Terjadi kesalahan sistem"
    }
    
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
    return withContext(Dispatchers.IO) {
        try {
            val authInstance = auth ?: throw Exception("Firebase Auth belum siap")
            authInstance.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(mapLoginError(e)))
        }
    }
}
}