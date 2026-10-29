package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.model.User
import com.example.model.UserRole

object AuthSessionCache {
    
    private var prefs: SharedPreferences? = null

    fun initialize(context: Context) {
        prefs = context.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE)
    }

    fun saveProfile(user: User) {
        prefs?.edit()?.apply {
            putString("id", user.id)
            putString("name", user.name)
            putString("email", user.email)
            putString("role", user.role.name)
            putString("assignedAreaId", user.assignedAreaId)
            putString("department", user.department)
            apply()
        }
    }

    fun loadProfile(): User? {
        val p = prefs ?: return null
        val id = p.getString("id", "") ?: ""
        
        // Jika ID kosong, berarti belum pernah login / sudah logout
        if (id.isEmpty()) return null

        val name = p.getString("name", "") ?: ""
        val email = p.getString("email", "") ?: ""
        val roleStr = p.getString("role", UserRole.GUEST.name) ?: UserRole.GUEST.name
        val assignedAreaId = p.getString("assignedAreaId", null)
        val department = p.getString("department", "") ?: ""

        // Konversi string role kembali menjadi Enum
        val role = try {
            UserRole.valueOf(roleStr)
        } catch (e: Exception) {
            UserRole.GUEST
        }

        return User(
            id = id,
            name = name,
            email = email,
            role = role,
            assignedAreaId = assignedAreaId,
            department = department
        )
    }

    fun setGuest(isGuest: Boolean) {
        prefs?.edit()?.putBoolean("is_guest", isGuest)?.apply()
    }

    fun isGuest(): Boolean {
        return prefs?.getBoolean("is_guest", false) ?: false
    }

    fun clearProfile() {
        prefs?.edit()?.clear()?.apply()
    }
}