package com.example.data.repository

import android.util.Log
import com.example.data.SampleData
import com.example.model.Area
import com.example.model.Zone
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AreaRepository {

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e("AreaRepository", "FirebaseFirestore init error: ${e.message}")
            null
        }
    }

    private var groupsListener: ListenerRegistration? = null
    private var areasListener: ListenerRegistration? = null
    private var zonesListener: ListenerRegistration? = null

    private val _groups = MutableStateFlow<List<String>>(listOf("PLANT 1", "PLANT 2"))
    val groups: StateFlow<List<String>> = _groups.asStateFlow()

    private val _areas = MutableStateFlow<List<Area>>(SampleData.initialAreas)
    val areas: StateFlow<List<Area>> = _areas.asStateFlow()

    private val _zones = MutableStateFlow<List<Zone>>(SampleData.initialZones)
    val zones: StateFlow<List<Zone>> = _zones.asStateFlow()

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError.asStateFlow()

    fun reportError(message: String) {
        _syncError.value = message
    }

    fun initSync() {
        val db = firestore ?: run {
            Log.w("AreaRepository", "Firestore belum siap, menggunakan data lokal.")
            return
        }

        val groupMap = mutableMapOf<String, String>()

        // 1. master_groups
        groupsListener?.remove()
        groupsListener = db.collection("master_groups").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w("AreaRepository", "Error listen master_groups: ${error.message}")
                return@addSnapshotListener
            }
            if (snapshot != null && !snapshot.isEmpty) {
                val list = snapshot.documents.mapNotNull { doc ->
                    val gId = doc.getString("group_id") ?: doc.id
                    val gName = doc.getString("group_name")
                        ?: doc.getString("groupName")
                        ?: doc.getString("name")
                        ?: gId
                    groupMap[gId] = gName
                    gName
                }.distinct()
                if (list.isNotEmpty()) {
                    _groups.value = list
                }
            }
        }

        // 2. master_areas
        areasListener?.remove()
        areasListener = db.collection("master_areas").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("AreaRepository", "Error listen master_areas: ${error.message}")
                reportError("Error areas: ${error.localizedMessage}")
                return@addSnapshotListener
            }
            if (snapshot != null && !snapshot.isEmpty) {
                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        val id = doc.getString("area_id") ?: doc.getString("id") ?: doc.id
                        val gId = doc.getString("group_id") ?: ""
                        val group = doc.getString("group_name")
                            ?: groupMap[gId]
                            ?: doc.getString("group")
                            ?: gId.ifEmpty { "PLANT 1" }
                        val code = doc.getString("area_code")
                            ?: doc.getString("areaCode")
                            ?: doc.getString("code")
                            ?: id
                        val name = doc.getString("area_name")
                            ?: doc.getString("areaName")
                            ?: doc.getString("name")
                            ?: code
                        val desc = doc.getString("description")
                            ?: doc.getString("desc")
                            ?: ""
                        val isActive = doc.getBoolean("isActive")
                            ?: doc.getBoolean("is_active")
                            ?: true
                        Area(
                            id = id,
                            group = group,
                            areaCode = code,
                            areaName = name,
                            description = desc,
                            isActive = isActive
                        )
                    } catch (e: Exception) {
                        Log.e("AreaRepository", "Error parsing area doc ${doc.id}: ${e.message}")
                        null
                    }
                }
                if (list.isNotEmpty()) {
                    _areas.value = list
                    Log.d("AreaRepository", "Tersinkron ${list.size} area dari master_areas")
                }
            }
        }

        // 3. master_zones
        zonesListener?.remove()
        zonesListener = db.collection("master_zones").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("AreaRepository", "Error listen master_zones: ${error.message}")
                return@addSnapshotListener
            }
            if (snapshot != null && !snapshot.isEmpty) {
                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        val id = doc.getString("zone_id") ?: doc.getString("id") ?: doc.id
                        val areaId = doc.getString("area_id")
                            ?: doc.getString("areaId")
                            ?: ""
                        val code = doc.getString("zone_code")
                            ?: doc.getString("zoneCode")
                            ?: doc.getString("code")
                            ?: id
                        val name = doc.getString("zone_name")
                            ?: doc.getString("zoneName")
                            ?: doc.getString("name")
                            ?: code
                        val isActive = doc.getBoolean("isActive")
                            ?: doc.getBoolean("is_active")
                            ?: true
                        Zone(
                            id = id,
                            areaId = areaId,
                            zoneCode = code,
                            zoneName = name,
                            isActive = isActive
                        )
                    } catch (e: Exception) {
                        Log.e("AreaRepository", "Error parsing zone doc ${doc.id}: ${e.message}")
                        null
                    }
                }
                if (list.isNotEmpty()) {
                    _zones.value = list
                    Log.d("AreaRepository", "Tersinkron ${list.size} zona dari master_zones")
                }
            }
        }
    }

    fun getAreaName(areaId: String): String = _areas.value.find { it.id == areaId }?.areaName ?: "Unknown Area"
    fun getAreaCode(areaId: String): String = _areas.value.find { it.id == areaId }?.areaCode ?: "??"
    fun getZoneName(zoneId: String): String = _zones.value.find { it.id == zoneId }?.zoneName ?: "Unknown Zone"
}
