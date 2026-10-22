package com.example.data.mapper

import com.example.data.utils.DateUtils
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

object FirestoreMapper {

    fun safeString(doc: DocumentSnapshot, vararg fields: String): String? {
        for (f in fields) {
            if (doc.contains(f)) {
                val v = doc.get(f) ?: continue
                if (v is Timestamp) {
                    return DateUtils.dateFormat.format(v.toDate())
                }
                val s = v.toString().trim()
                if (s.isNotEmpty()) return s
            }
        }
        return null
    }

    fun safeBoolean(doc: DocumentSnapshot, vararg fields: String, default: Boolean = true): Boolean {
        for (f in fields) {
            if (doc.contains(f)) {
                val v = doc.get(f) ?: continue
                when (v) {
                    is Boolean -> return v
                    is String -> return v.equals("true", ignoreCase = true) || v == "1"
                    is Number -> return v.toInt() == 1
                }
            }
        }
        return default
    }

    fun safeInt(doc: DocumentSnapshot, vararg fields: String, default: Int = 1): Int {
        for (f in fields) {
            if (doc.contains(f)) {
                val v = doc.get(f) ?: continue
                when (v) {
                    is Number -> return v.toInt()
                    is String -> return v.toIntOrNull() ?: default
                }
            }
        }
        return default
    }
}
