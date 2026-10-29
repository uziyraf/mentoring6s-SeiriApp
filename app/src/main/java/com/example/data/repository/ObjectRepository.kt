package com.example.data.repository

import android.util.Log
import com.example.data.SampleData
import com.example.data.mapper.FirestoreMapper
import com.example.data.utils.DateUtils
import com.example.model.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

object ObjectRepository {

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e("ObjectRepository", "FirebaseFirestore init error: ${e.message}")
            null
        }
    }

    private var objectsListener: ListenerRegistration? = null

    val objects = MutableStateFlow<List<SeiriObject>>(SampleData.initialObjects)

    private val _changeLogs = MutableStateFlow<List<ObjectChangeLog>>(SampleData.initialChangeLogs)
    val changeLogs: StateFlow<List<ObjectChangeLog>> = _changeLogs.asStateFlow()

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError.asStateFlow()

    fun initSync() {
        val db = firestore ?: run {
            Log.w("ObjectRepository", "Firestore belum siap, menggunakan data lokal.")
            return
        }

        objectsListener?.remove()
        objectsListener = db.collection("master_objects").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("ObjectRepository", "Error listen master_objects: ${error.message}")
                _syncError.value = "Error objects: ${error.localizedMessage}"
                return@addSnapshotListener
            }
            if (snapshot != null && !snapshot.isEmpty) {
                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        val id = FirestoreMapper.safeString(doc, "object_id", "id") ?: doc.id
                        val code = FirestoreMapper.safeString(doc, "object_code", "objectCode", "object_id", "code") ?: id
                        val name = FirestoreMapper.safeString(doc, "object_name", "name", "objectName") ?: code
                        val areaId = FirestoreMapper.safeString(doc, "area_id", "areaId") ?: ""
                        val zoneId = FirestoreMapper.safeString(doc, "zone_id", "zoneId") ?: ""
                        val desc = FirestoreMapper.safeString(doc, "description", "desc") ?: ""
                        val func = FirestoreMapper.safeString(doc, "function", "fungsi") ?: ""
                        val cat = FirestoreMapper.safeString(doc, "category", "kategori") ?: "Alat Kerja"
                        val pic = FirestoreMapper.safeString(doc, "pic") ?: ""

                        val condStr = FirestoreMapper.safeString(doc, "condition", "kondisi") ?: "BAIK"
                        val cond = ObjectCondition.entries.find {
                            it.name.equals(condStr, true) || it.label.equals(condStr, true)
                        } ?: ObjectCondition.BAIK

                        val freqStr = FirestoreMapper.safeString(doc, "usageFrequency", "usage_frequency", "frekuensi") ?: "DAILY"
                        val freq = UsageFrequency.entries.find {
                            it.name.equals(freqStr, true) || it.label.equals(freqStr, true)
                        } ?: UsageFrequency.DAILY

                        val statusStr = FirestoreMapper.safeString(doc, "validationStatus", "validation_status", "status") ?: "BELUM_DIVALIDASI"
                        val status = ValidationStatus.entries.find {
                            it.name.equals(statusStr, true) || it.label.equals(statusStr, true)
                        } ?: ValidationStatus.BELUM_DIVALIDASI

                        val isActive = FirestoreMapper.safeBoolean(doc, "isActive", "is_active", default = true)
                        val lastUsed = FirestoreMapper.safeString(doc, "lastUsedDate", "last_used_date") ?: DateUtils.currentDate()
                        val createdAt = FirestoreMapper.safeString(doc, "created_at", "createdAt") ?: DateUtils.currentDate()
                        val updatedAt = FirestoreMapper.safeString(doc, "updated_at", "updatedAt") ?: DateUtils.currentDate()
                        val photoUrl = FirestoreMapper.safeString(
                            doc,
                            "photoUrl",
                            "photo_url",
                            "imageUrl",
                            "image_url",
                            "fotoUrl",
                            "foto_url",
                            "photo",
                            "image"
                        )

                        SeiriObject(
                            id = id,
                            sourceObjectId = FirestoreMapper.safeString(doc, "sourceObjectId", "source_object_id"),
                            objectCode = code,
                            areaId = areaId,
                            zoneId = zoneId,
                            name = name,
                            description = desc,
                            function = func,
                            category = cat,
                            condition = cond,
                            pic = pic,
                            usageFrequency = freq,
                            lastUsedDate = lastUsed,
                            validationStatus = status,
                            isActive = isActive,
                            version = FirestoreMapper.safeInt(doc, "version", default = 1),
                            createdBy = FirestoreMapper.safeString(doc, "createdBy", "created_by") ?: "System",
                            updatedBy = FirestoreMapper.safeString(doc, "updated_by", "updated_by") ?: "System",
                            createdAt = createdAt,
                            updatedAt = updatedAt,
                            photoUrl = photoUrl
                        )
                    } catch (e: Exception) {
                        Log.e("ObjectRepository", "Error parsing object doc ${doc.id}: ${e.message}", e)
                        null
                    }
                }
                if (list.isNotEmpty()) {
                    objects.value = list
                    Log.d("ObjectRepository", "Tersinkron ${list.size} objek dari master_objects")
                }
            }
        }
    }

    // Permission checks
    fun canManageObject(targetObject: SeiriObject, user: User): Boolean {
        if (user.role == UserRole.ADMIN) return true
        return user.role == UserRole.MENTOR && user.assignedAreaId == targetObject.areaId
    }

    fun canManageArea(areaId: String, user: User): Boolean {
        if (user.role == UserRole.ADMIN) return true
        return user.role == UserRole.MENTOR && user.assignedAreaId == areaId
    }

    // Object CRUD with audit logging
    fun addObject(
        user: User,
        code: String,
        name: String,
        areaId: String,
        zoneId: String,
        description: String,
        function: String,
        category: String,
        condition: ObjectCondition,
        pic: String,
        usageFrequency: UsageFrequency,
        sourceObjectId: String? = null
    ): Result<SeiriObject> {
        if (!canManageArea(areaId, user)) {
            return Result.failure(IllegalAccessException("Mentor hanya boleh menambahkan objek ke area binaan sendiri."))
        }

        // Duplicate code detection
        val existing = objects.value.find { it.objectCode.equals(code.trim(), ignoreCase = true) }
        if (existing != null) {
            return Result.failure(IllegalArgumentException("Kode objek '$code' sudah digunakan oleh '${existing.name}'."))
        }

        val newObj = SeiriObject(
            id = "obj_${UUID.randomUUID().toString().take(8)}",
            sourceObjectId = sourceObjectId,
            objectCode = code.trim().uppercase(),
            areaId = areaId,
            zoneId = zoneId,
            name = name.trim(),
            description = description.trim(),
            function = function.trim(),
            category = category.trim().ifEmpty { "Alat Kerja" },
            condition = condition,
            pic = pic.trim().ifEmpty { user.name },
            usageFrequency = usageFrequency,
            lastUsedDate = DateUtils.currentDate(),
            validationStatus = ValidationStatus.BELUM_DIVALIDASI,
            isActive = true,
            createdBy = user.name,
            updatedBy = user.name,
            createdAt = DateUtils.currentDate(),
            updatedAt = DateUtils.currentDate()
        )

        objects.value = listOf(newObj) + objects.value

        try {
            firestore?.collection("master_objects")?.document(newObj.id)?.set(
                mapOf(
                    "id" to newObj.id,
                    "objectCode" to newObj.objectCode,
                    "name" to newObj.name,
                    "areaId" to newObj.areaId,
                    "zoneId" to newObj.zoneId,
                    "description" to newObj.description,
                    "function" to newObj.function,
                    "category" to newObj.category,
                    "condition" to newObj.condition.name,
                    "pic" to newObj.pic,
                    "usageFrequency" to newObj.usageFrequency.name,
                    "lastUsedDate" to newObj.lastUsedDate,
                    "validationStatus" to newObj.validationStatus.name,
                    "isActive" to newObj.isActive,
                    "createdBy" to newObj.createdBy,
                    "updatedBy" to newObj.updatedBy,
                    "createdAt" to newObj.createdAt,
                    "updatedAt" to newObj.updatedAt
                )
            )
        } catch (e: Exception) {
            Log.e("ObjectRepository", "Gagal menyimpan objek ke Firestore: ${e.message}")
        }

        logChange(
            objectId = newObj.id,
            objectCode = newObj.objectCode,
            objectName = newObj.name,
            changedBy = "${user.name} (${user.role.label})",
            changeType = "CREATE",
            oldValues = "-",
            newValues = "Objek baru ditambahkan: ${newObj.name} di ${AreaRepository.getAreaName(areaId)}"
        )

        return Result.success(newObj)
    }

    fun updateObject(
        user: User,
        id: String,
        name: String,
        zoneId: String,
        description: String,
        function: String,
        category: String,
        condition: ObjectCondition,
        pic: String,
        usageFrequency: UsageFrequency
    ): Result<SeiriObject> {
        val current = objects.value.find { it.id == id }
            ?: return Result.failure(NoSuchElementException("Objek tidak ditemukan."))

        if (!canManageObject(current, user)) {
            return Result.failure(IllegalAccessException("Anda tidak memiliki izin mengedit objek dari area lain."))
        }

        val oldSummary = "Nama: ${current.name}, Zona: ${AreaRepository.getZoneName(current.zoneId)}, PIC: ${current.pic}, Frekuensi: ${current.usageFrequency.label}"
        val updated = current.copy(
            name = name.trim(),
            zoneId = zoneId,
            description = description.trim(),
            function = function.trim(),
            category = category.trim(),
            condition = condition,
            pic = pic.trim(),
            usageFrequency = usageFrequency,
            version = current.version + 1,
            updatedBy = user.name,
            updatedAt = DateUtils.currentDate()
        )
        val newSummary = "Nama: ${updated.name}, Zona: ${AreaRepository.getZoneName(updated.zoneId)}, PIC: ${updated.pic}, Frekuensi: ${updated.usageFrequency.label}"

        objects.value = objects.value.map { if (it.id == id) updated else it }

        try {
            firestore?.collection("master_objects")?.document(updated.id)?.update(
                mapOf(
                    "name" to updated.name,
                    "zoneId" to updated.zoneId,
                    "description" to updated.description,
                    "function" to updated.function,
                    "category" to updated.category,
                    "condition" to updated.condition.name,
                    "pic" to updated.pic,
                    "usageFrequency" to updated.usageFrequency.name,
                    "updatedBy" to updated.updatedBy,
                    "updatedAt" to updated.updatedAt,
                    "version" to updated.version
                )
            )
        } catch (e: Exception) {
            Log.e("ObjectRepository", "Gagal update objek di Firestore: ${e.message}")
        }

        logChange(
            objectId = updated.id,
            objectCode = updated.objectCode,
            objectName = updated.name,
            changedBy = "${user.name} (${user.role.label})",
            changeType = "UPDATE",
            oldValues = oldSummary,
            newValues = newSummary
        )

        return Result.success(updated)
    }

    fun deactivateObject(user: User, id: String, reason: String): Result<Unit> {
        val current = objects.value.find { it.id == id }
            ?: return Result.failure(NoSuchElementException("Objek tidak ditemukan."))

        if (!canManageObject(current, user)) {
            return Result.failure(IllegalAccessException("Anda tidak memiliki izin menonaktifkan objek ini."))
        }

        val updated = current.copy(
            isActive = false,
            updatedBy = user.name,
            updatedAt = DateUtils.currentDate()
        )
        objects.value = objects.value.map { if (it.id == id) updated else it }
        try {
            firestore?.collection("master_objects")?.document(current.id)?.update(
                mapOf(
                    "isActive" to false,
                    "updatedBy" to user.name,
                    "updatedAt" to DateUtils.currentDate()
                )
            )
        } catch (e: Exception) {
            Log.e("ObjectRepository", "Gagal deactivate objek di Firestore: ${e.message}")
        }

        logChange(
            objectId = current.id,
            objectCode = current.objectCode,
            objectName = current.name,
            changedBy = "${user.name} (${user.role.label})",
            changeType = "DEACTIVATE",
            oldValues = "Status: Aktif",
            newValues = "Status: Nonaktif (Alasan: ${reason.ifEmpty { "Red Tag / Afkir" }})"
        )

        return Result.success(Unit)
    }

    fun reactivateObject(user: User, id: String): Result<Unit> {
        val current = objects.value.find { it.id == id }
            ?: return Result.failure(NoSuchElementException("Objek tidak ditemukan."))

        if (!canManageObject(current, user)) {
            return Result.failure(IllegalAccessException("Anda tidak memiliki izin mengaktifkan objek ini."))
        }

        val updated = current.copy(
            isActive = true,
            updatedBy = user.name,
            updatedAt = DateUtils.currentDate()
        )
        objects.value = objects.value.map { if (it.id == id) updated else it }
        try {
            firestore?.collection("master_objects")?.document(current.id)?.update(
                mapOf(
                    "isActive" to true,
                    "updatedBy" to user.name,
                    "updatedAt" to DateUtils.currentDate()
                )
            )
        } catch (e: Exception) {
            Log.e("ObjectRepository", "Gagal reactivate objek di Firestore: ${e.message}")
        }

        logChange(
            objectId = current.id,
            objectCode = current.objectCode,
            objectName = current.name,
            changedBy = "${user.name} (${user.role.label})",
            changeType = "REACTIVATE",
            oldValues = "Status: Nonaktif",
            newValues = "Status: Aktif kembali"
        )

        return Result.success(Unit)
    }

    fun logChange(
        objectId: String,
        objectCode: String,
        objectName: String,
        changedBy: String,
        changeType: String,
        oldValues: String,
        newValues: String,
        relatedTransferRequestId: String? = null
    ) {
        val log = ObjectChangeLog(
            id = "log_${UUID.randomUUID().toString().take(8)}",
            objectId = objectId,
            objectCode = objectCode,
            objectName = objectName,
            changedBy = changedBy,
            changeType = changeType,
            oldValues = oldValues,
            newValues = newValues,
            relatedTransferRequestId = relatedTransferRequestId,
            changedAt = DateUtils.currentTimestamp()
        )
        _changeLogs.value = listOf(log) + _changeLogs.value
    }
}
