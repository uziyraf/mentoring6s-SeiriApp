package com.example.data

import android.util.Log
import com.example.data.mapper.FirestoreMapper
import com.example.data.utils.DateUtils
import com.example.model.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class SeiriRepository private constructor() {

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e("SeiriRepository", "FirebaseFirestore init error: ${e.message}")
            null
        }
    }

    private var areasListener: ListenerRegistration? = null
    private var zonesListener: ListenerRegistration? = null
    private var objectsListener: ListenerRegistration? = null
    private var groupsListener: ListenerRegistration? = null

    private val _groups = MutableStateFlow<List<String>>(listOf("PLANT 1", "PLANT 2"))
    val groups: StateFlow<List<String>> = _groups.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError.asStateFlow()

    private val _currentUser = MutableStateFlow<User>(SampleData.adminUser)
    val currentUser: StateFlow<User> = _currentUser.asStateFlow()

    private val _areas = MutableStateFlow<List<Area>>(SampleData.initialAreas)
    val areas: StateFlow<List<Area>> = _areas.asStateFlow()

    private val _zones = MutableStateFlow<List<Zone>>(SampleData.initialZones)
    val zones: StateFlow<List<Zone>> = _zones.asStateFlow()

    private val _objects = MutableStateFlow<List<SeiriObject>>(SampleData.initialObjects)
    val objects: StateFlow<List<SeiriObject>> = _objects.asStateFlow()

    private val _sessions = MutableStateFlow<List<MentoringSession>>(SampleData.initialSessions)
    val sessions: StateFlow<List<MentoringSession>> = _sessions.asStateFlow()

    private val _validations = MutableStateFlow<List<SeiriValidation>>(SampleData.initialValidations)
    val validations: StateFlow<List<SeiriValidation>> = _validations.asStateFlow()

    private val _transferRequests = MutableStateFlow<List<ObjectTransferRequest>>(SampleData.initialTransferRequests)
    val transferRequests: StateFlow<List<ObjectTransferRequest>> = _transferRequests.asStateFlow()

    private val _changeLogs = MutableStateFlow<List<ObjectChangeLog>>(SampleData.initialChangeLogs)
    val changeLogs: StateFlow<List<ObjectChangeLog>> = _changeLogs.asStateFlow()

    private val _actions = MutableStateFlow<List<SeiriAction>>(SampleData.initialActions)
    val actions: StateFlow<List<SeiriAction>> = _actions.asStateFlow()

    private val _questions = MutableStateFlow<List<SeiriQuestion>>(SampleData.questionBank)
    val questions: StateFlow<List<SeiriQuestion>> = _questions.asStateFlow()

    init {
        initFirestoreSync()
    }

    fun initFirestoreSync() {
        val db = firestore ?: run {
            Log.w("SeiriRepository", "Firestore belum siap, menggunakan data lokal.")
            return
        }

        _isSyncing.value = true

        val groupMap = mutableMapOf<String, String>()

        // 1. master_groups
        groupsListener?.remove()
        groupsListener = db.collection("master_groups").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w("SeiriRepository", "Error listen master_groups: ${error.message}")
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
                Log.e("SeiriRepository", "Error listen master_areas: ${error.message}")
                _syncError.value = "Error areas: ${error.localizedMessage}"
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
                        Log.e("SeiriRepository", "Error parsing area doc ${doc.id}: ${e.message}")
                        null
                    }
                }
                if (list.isNotEmpty()) {
                    _areas.value = list
                    Log.d("SeiriRepository", "Tersinkron ${list.size} area dari master_areas")
                }
            }
        }

        // 3. master_zones
        zonesListener?.remove()
        zonesListener = db.collection("master_zones").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("SeiriRepository", "Error listen master_zones: ${error.message}")
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
                        Log.e("SeiriRepository", "Error parsing zone doc ${doc.id}: ${e.message}")
                        null
                    }
                }
                if (list.isNotEmpty()) {
                    _zones.value = list
                    Log.d("SeiriRepository", "Tersinkron ${list.size} zona dari master_zones")
                }
            }
        }

        // 4. master_objects
        objectsListener?.remove()
        objectsListener = db.collection("master_objects").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("SeiriRepository", "Error listen master_objects: ${error.message}")
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
                            updatedBy = FirestoreMapper.safeString(doc, "updatedBy", "updated_by") ?: "System",
                            createdAt = createdAt,
                            updatedAt = updatedAt
                        )
                    } catch (e: Exception) {
                        Log.e("SeiriRepository", "Error parsing object doc ${doc.id}: ${e.message}", e)
                        null
                    }
                }
                if (list.isNotEmpty()) {
                    _objects.value = list
                    Log.d("SeiriRepository", "Tersinkron ${list.size} objek dari master_objects")
                }
            }
        }
    }

    // Demo Role Switcher
    fun switchDemoUser(userId: String) {
        val user = SampleData.allUsers.find { it.id == userId } ?: return
        _currentUser.value = user
    }

    // Permission checks
    fun canManageObject(targetObject: SeiriObject, user: User = _currentUser.value): Boolean {
        if (user.role == UserRole.ADMIN) return true
        return user.role == UserRole.MENTOR && user.assignedAreaId == targetObject.areaId
    }

    fun canManageArea(areaId: String, user: User = _currentUser.value): Boolean {
        if (user.role == UserRole.ADMIN) return true
        return user.role == UserRole.MENTOR && user.assignedAreaId == areaId
    }

    // Object CRUD with audit logging
    fun addObject(
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
        val user = _currentUser.value
        if (!canManageArea(areaId, user)) {
            return Result.failure(IllegalAccessException("Mentor hanya boleh menambahkan objek ke area binaan sendiri."))
        }

        // Duplicate code detection
        val existing = _objects.value.find { it.objectCode.equals(code.trim(), ignoreCase = true) }
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

        _objects.value = listOf(newObj) + _objects.value

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
            Log.e("SeiriRepository", "Gagal menyimpan objek ke Firestore: ${e.message}")
        }

        logChange(
            objectId = newObj.id,
            objectCode = newObj.objectCode,
            objectName = newObj.name,
            changedBy = "${user.name} (${user.role.label})",
            changeType = "CREATE",
            oldValues = "-",
            newValues = "Objek baru ditambahkan: ${newObj.name} di ${getAreaName(areaId)}"
        )

        return Result.success(newObj)
    }

    fun updateObject(
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
        val user = _currentUser.value
        val current = _objects.value.find { it.id == id }
            ?: return Result.failure(NoSuchElementException("Objek tidak ditemukan."))

        if (!canManageObject(current, user)) {
            return Result.failure(IllegalAccessException("Anda tidak memiliki izin mengedit objek dari area lain."))
        }

        val oldSummary = "Nama: ${current.name}, Zona: ${getZoneName(current.zoneId)}, PIC: ${current.pic}, Frekuensi: ${current.usageFrequency.label}"
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
        val newSummary = "Nama: ${updated.name}, Zona: ${getZoneName(updated.zoneId)}, PIC: ${updated.pic}, Frekuensi: ${updated.usageFrequency.label}"

        _objects.value = _objects.value.map { if (it.id == id) updated else it }

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
            Log.e("SeiriRepository", "Gagal update objek di Firestore: ${e.message}")
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

    fun deactivateObject(id: String, reason: String): Result<Unit> {
        val user = _currentUser.value
        val current = _objects.value.find { it.id == id }
            ?: return Result.failure(NoSuchElementException("Objek tidak ditemukan."))

        if (!canManageObject(current, user)) {
            return Result.failure(IllegalAccessException("Anda tidak memiliki izin menonaktifkan objek ini."))
        }

        val updated = current.copy(
            isActive = false,
            updatedBy = user.name,
            updatedAt = DateUtils.currentDate()
        )
        _objects.value = _objects.value.map { if (it.id == id) updated else it }
        try {
            firestore?.collection("master_objects")?.document(current.id)?.update(
                mapOf(
                    "isActive" to false,
                    "updatedBy" to user.name,
                    "updatedAt" to DateUtils.currentDate()
                )
            )
        } catch (e: Exception) {
            Log.e("SeiriRepository", "Gagal deactivate objek di Firestore: ${e.message}")
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

    fun reactivateObject(id: String): Result<Unit> {
        val user = _currentUser.value
        val current = _objects.value.find { it.id == id }
            ?: return Result.failure(NoSuchElementException("Objek tidak ditemukan."))

        if (!canManageObject(current, user)) {
            return Result.failure(IllegalAccessException("Anda tidak memiliki izin mengaktifkan objek ini."))
        }

        val updated = current.copy(
            isActive = true,
            updatedBy = user.name,
            updatedAt = DateUtils.currentDate()
        )
        _objects.value = _objects.value.map { if (it.id == id) updated else it }
        try {
            firestore?.collection("master_objects")?.document(current.id)?.update(
                mapOf(
                    "isActive" to true,
                    "updatedBy" to user.name,
                    "updatedAt" to DateUtils.currentDate()
                )
            )
        } catch (e: Exception) {
            Log.e("SeiriRepository", "Gagal reactivate objek di Firestore: ${e.message}")
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

    // Mentoring Sessions (Multi-session / Draft support)
    fun createOrResumeSession(
        mentorId: String,
        mentorName: String,
        groupName: String,
        areaId: String,
        zoneId: String,
        sessionDate: String,
        notes: String
    ): MentoringSession {
        // Find existing draft session for same mentor and zone if available
        val existingDraft = _sessions.value.find {
            it.mentorId == mentorId && it.zoneId == zoneId && it.status == SessionStatus.DRAFT
        }
        if (existingDraft != null) {
            return existingDraft
        }

        // Initialize inspections from active objects in zone
        val zoneObjs = _objects.value.filter { it.areaId == areaId && it.zoneId == zoneId && it.isActive }
        val inspections = zoneObjs.map { obj ->
            SessionObjectInspection(
                objectId = obj.id,
                objectCode = obj.objectCode,
                objectName = obj.name,
                function = obj.function,
                usageFrequency = "Setiap Hari/Shift",
                lastUsed = "Hari ini",
                initialAssessment = "Masih Diperlukan",
                seiriDecision = "Diperlukan (Hijau)",
                decisionReason = "Objek masih aktif digunakan di lini produksi",
                pic = obj.pic
            )
        }

        val session = MentoringSession(
            id = "ses_${UUID.randomUUID().toString().take(8)}",
            mentorId = mentorId,
            mentorName = mentorName,
            groupName = groupName,
            areaId = areaId,
            zoneId = zoneId,
            sessionDate = sessionDate.ifEmpty { DateUtils.currentDate() },
            currentStage = 1,
            status = SessionStatus.DRAFT,
            notes = notes,
            startedAt = DateUtils.currentTimestamp(),
            inspections = inspections
        )
        _sessions.value = listOf(session) + _sessions.value
        return session
    }

    fun saveSessionDraft(session: MentoringSession): Result<Unit> {
        _sessions.value = _sessions.value.map {
            if (it.id == session.id) session else it
        }
        return Result.success(Unit)
    }

    fun createSession(mentorId: String, areaId: String, zoneId: String, notes: String): MentoringSession {
        val user = _currentUser.value
        val area = _areas.value.find { it.id == areaId }
        return createOrResumeSession(
            mentorId = mentorId,
            mentorName = user.name,
            groupName = area?.group ?: "PLANT 1",
            areaId = areaId,
            zoneId = zoneId,
            sessionDate = DateUtils.currentDate(),
            notes = notes
        )
    }

    fun completeSession(sessionId: String): Result<Unit> {
        val session = _sessions.value.find { it.id == sessionId }
            ?: return Result.failure(NoSuchElementException("Sesi tidak ditemukan."))

        val updated = session.copy(
            status = SessionStatus.COMPLETED,
            completedAt = DateUtils.currentTimestamp()
        )
        _sessions.value = _sessions.value.map { if (it.id == sessionId) updated else it }
        return Result.success(Unit)
    }

    fun submitFinalSession(session: MentoringSession): Result<Unit> {
        val user = _currentUser.value
        val completedSession = session.copy(
            status = SessionStatus.COMPLETED,
            completedAt = DateUtils.currentTimestamp(),
            currentStage = 5
        )
        _sessions.value = _sessions.value.map {
            if (it.id == session.id) completedSession else it
        }

        // Sync inspections to objects and validations
        session.inspections.forEach { insp ->
            val targetObj = _objects.value.find { it.id == insp.objectId }
            if (targetObj != null) {
                val valStatus = when {
                    insp.seiriDecision.contains("Konfirmasi", ignoreCase = true) -> ValidationStatus.PERLU_KONFIRMASI
                    else -> ValidationStatus.TERVALIDASI
                }

                val updatedObj = targetObj.copy(
                    function = insp.function.ifEmpty { targetObj.function },
                    lastUsedDate = insp.lastUsed.ifEmpty { targetObj.lastUsedDate },
                    pic = insp.pic.ifEmpty { targetObj.pic },
                    validationStatus = valStatus,
                    updatedBy = user.name,
                    updatedAt = DateUtils.currentDate()
                )
                _objects.value = _objects.value.map { if (it.id == targetObj.id) updatedObj else it }

                // Save validation record
                val valRecord = SeiriValidation(
                    id = "val_${UUID.randomUUID().toString().take(8)}",
                    sessionId = session.id,
                    objectId = targetObj.id,
                    validationStatus = valStatus,
                    condition = targetObj.condition,
                    isNecessary = insp.initialAssessment == "Masih Diperlukan",
                    usageFrequency = when (insp.usageFrequency) {
                        "Setiap Hari/Shift" -> UsageFrequency.DAILY
                        "Mingguan/Bulanan" -> UsageFrequency.WEEKLY
                        else -> UsageFrequency.RARELY
                    },
                    lastUsedAt = insp.lastUsed,
                    seiriDecision = when {
                        insp.seiriDecision.contains("Dipindahkan") -> SeiriDecision.PINDAHKAN_LOKASI
                        insp.seiriDecision.contains("Red Tag") || insp.seiriDecision.contains("Tidak Diperlukan") -> SeiriDecision.TIDAK_DIPERLUKAN
                        insp.seiriDecision.contains("Konfirmasi") -> SeiriDecision.PERLU_KONFIRMASI
                        else -> SeiriDecision.TETAP_DI_AREA
                    },
                    decisionReason = insp.decisionReason.ifEmpty { "Keputusan Seiri dari sesi mentoring" },
                    responsiblePerson = insp.pic.ifEmpty { targetObj.pic },
                    followUpAction = insp.actionDescription,
                    objectNameSnapshot = targetObj.name,
                    areaNameSnapshot = getAreaName(targetObj.areaId),
                    createdBy = user.name,
                    createdAt = DateUtils.currentTimestamp()
                )
                _validations.value = listOf(valRecord) + _validations.value

                // If Red Tag / Dipindahkan action exists, record SeiriAction
                if (insp.actionDescription.isNotBlank()) {
                    val act = SeiriAction(
                        id = "act_${UUID.randomUUID().toString().take(8)}",
                        validationId = valRecord.id,
                        objectId = targetObj.id,
                        objectCode = targetObj.objectCode,
                        objectName = targetObj.name,
                        areaId = targetObj.areaId,
                        actionDescription = insp.actionDescription,
                        assignedTo = insp.pic.ifEmpty { user.name },
                        dueDate = insp.dueDate.ifEmpty { DateUtils.currentDate() },
                        status = "PENDING"
                    )
                    _actions.value = listOf(act) + _actions.value
                }
            }
        }

        logChange(
            objectId = "SES_${session.id}",
            objectCode = "SESI-6S",
            objectName = "Sesi Mentoring ${getZoneName(session.zoneId)}",
            changedBy = "${user.name} (${user.role.label})",
            changeType = "SESSION_COMPLETE",
            oldValues = "Status: DRAFT",
            newValues = "Status: COMPLETED (${session.inspections.size} objek diperiksa)"
        )

        return Result.success(Unit)
    }

    fun generateSessionJson(session: MentoringSession): String {
        val area = _areas.value.find { it.id == session.areaId }
        val zone = _zones.value.find { it.id == session.zoneId }
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"session_id\": \"${session.id}\",\n")
        sb.append("  \"group\": \"${session.groupName}\",\n")
        sb.append("  \"area_code\": \"${area?.areaCode ?: ""}\",\n")
        sb.append("  \"area_name\": \"${area?.areaName ?: ""}\",\n")
        sb.append("  \"zone_code\": \"${zone?.zoneCode ?: ""}\",\n")
        sb.append("  \"zone_name\": \"${zone?.zoneName ?: ""}\",\n")
        sb.append("  \"mentor_id\": \"${session.mentorId}\",\n")
        sb.append("  \"mentor_name\": \"${session.mentorName}\",\n")
        sb.append("  \"session_date\": \"${session.sessionDate}\",\n")
        sb.append("  \"status\": \"${session.status.name}\",\n")
        sb.append("  \"current_stage\": ${session.currentStage},\n")
        sb.append("  \"total_objects\": ${session.inspections.size},\n")
        sb.append("  \"objects\": [\n")
        session.inspections.forEachIndexed { idx, item ->
            sb.append("    {\n")
            sb.append("      \"object_id\": \"${item.objectId}\",\n")
            sb.append("      \"object_code\": \"${item.objectCode}\",\n")
            sb.append("      \"object_name\": \"${item.objectName}\",\n")
            sb.append("      \"function\": \"${item.function.replace("\"", "\\\"")}\",\n")
            sb.append("      \"usage_frequency\": \"${item.usageFrequency}\",\n")
            sb.append("      \"last_used\": \"${item.lastUsed}\",\n")
            sb.append("      \"initial_assessment\": \"${item.initialAssessment}\",\n")
            sb.append("      \"seiri_decision\": \"${item.seiriDecision}\",\n")
            sb.append("      \"decision_reason\": \"${item.decisionReason.replace("\"", "\\\"")}\",\n")
            sb.append("      \"action_description\": \"${item.actionDescription.replace("\"", "\\\"")}\",\n")
            sb.append("      \"pic\": \"${item.pic}\",\n")
            sb.append("      \"due_date\": \"${item.dueDate}\"\n")
            sb.append("    }${if (idx < session.inspections.size - 1) "," else ""}\n")
        }
        sb.append("  ]\n")
        sb.append("}")
        return sb.toString()
    }

    fun generateGoogleSheetsCsv(session: MentoringSession): String {
        val area = _areas.value.find { it.id == session.areaId }
        val zone = _zones.value.find { it.id == session.zoneId }
        val sb = StringBuilder()
        sb.append("Timestamp,Session ID,Group,Area,Zone,Mentor,Object Code,Object Name,Fungsi,Frekuensi Penggunaan,Terakhir Digunakan,Penilaian Awal,Keputusan Seiri,Alasan Keputusan,Rencana Tindakan,PIC,Tenggat Waktu,Status Sesi\n")
        session.inspections.forEach { item ->
            sb.append("\"${session.sessionDate}\",\"${session.id}\",\"${session.groupName}\",\"${area?.areaName}\",\"${zone?.zoneName}\",\"${session.mentorName}\",\"${item.objectCode}\",\"${item.objectName}\",\"${item.function}\",\"${item.usageFrequency}\",\"${item.lastUsed}\",\"${item.initialAssessment}\",\"${item.seiriDecision}\",\"${item.decisionReason}\",\"${item.actionDescription}\",\"${item.pic}\",\"${item.dueDate}\",\"${session.status.name}\"\n")
        }
        return sb.toString()
    }

    fun saveValidation(
        sessionId: String,
        objectId: String,
        isNecessary: Boolean,
        condition: ObjectCondition,
        usageFrequency: UsageFrequency,
        lastUsedAt: String,
        responsiblePerson: String,
        seiriDecision: SeiriDecision,
        decisionReason: String,
        conditionNotes: String,
        followUpAction: String
    ): Result<SeiriValidation> {
        val user = _currentUser.value
        val targetObj = _objects.value.find { it.id == objectId }
            ?: return Result.failure(NoSuchElementException("Objek tidak ditemukan."))

        val validationStatus = when (seiriDecision) {
            SeiriDecision.PERLU_KONFIRMASI -> ValidationStatus.PERLU_KONFIRMASI
            else -> ValidationStatus.TERVALIDASI
        }

        val validation = SeiriValidation(
            id = "val_${UUID.randomUUID().toString().take(8)}",
            sessionId = sessionId,
            objectId = objectId,
            validationStatus = validationStatus,
            condition = condition,
            isNecessary = isNecessary,
            usageFrequency = usageFrequency,
            lastUsedAt = lastUsedAt.ifEmpty { DateUtils.currentDate() },
            seiriDecision = seiriDecision,
            decisionReason = decisionReason,
            responsiblePerson = responsiblePerson.ifEmpty { targetObj.pic },
            conditionNotes = conditionNotes,
            followUpAction = followUpAction,
            objectNameSnapshot = targetObj.name,
            areaNameSnapshot = getAreaName(targetObj.areaId),
            createdBy = user.name,
            createdAt = DateUtils.currentTimestamp()
        )

        _validations.value = listOf(validation) + _validations.value

        // Update target object state
        val updatedObj = targetObj.copy(
            validationStatus = validationStatus,
            condition = condition,
            usageFrequency = usageFrequency,
            lastUsedDate = lastUsedAt.ifEmpty { targetObj.lastUsedDate },
            pic = responsiblePerson.ifEmpty { targetObj.pic },
            updatedBy = user.name,
            updatedAt = DateUtils.currentDate()
        )
        _objects.value = _objects.value.map { if (it.id == objectId) updatedObj else it }

        // If follow-up action is created, register action item
        if (followUpAction.isNotBlank()) {
            val action = SeiriAction(
                id = "act_${UUID.randomUUID().toString().take(8)}",
                validationId = validation.id,
                objectId = targetObj.id,
                objectCode = targetObj.objectCode,
                objectName = targetObj.name,
                areaId = targetObj.areaId,
                actionDescription = followUpAction,
                assignedTo = responsiblePerson.ifEmpty { user.name },
                dueDate = DateUtils.currentDate(),
                status = "PENDING"
            )
            _actions.value = listOf(action) + _actions.value
        }

        logChange(
            objectId = targetObj.id,
            objectCode = targetObj.objectCode,
            objectName = targetObj.name,
            changedBy = "${user.name} (${user.role.label})",
            changeType = "VALIDATION",
            oldValues = "Status Validasi: ${targetObj.validationStatus.label}",
            newValues = "Keputusan Seiri: ${seiriDecision.label} | Alasan: $decisionReason"
        )

        return Result.success(validation)
    }

    // Ownership Transfer Requests
    fun submitTransferRequest(
        objectId: String,
        toAreaId: String,
        reason: String,
        supportingNotes: String
    ): Result<ObjectTransferRequest> {
        val user = _currentUser.value
        val targetObj = _objects.value.find { it.id == objectId }
            ?: return Result.failure(NoSuchElementException("Objek tidak ditemukan."))

        if (targetObj.areaId == toAreaId) {
            return Result.failure(IllegalArgumentException("Area tujuan harus berbeda dari area pemilik saat ini."))
        }

        // Check if there is already a pending transfer request for this object
        val hasPending = _transferRequests.value.any {
            it.objectId == objectId && it.status == TransferStatus.MENUNGGU_PERSETUJUAN
        }
        if (hasPending) {
            return Result.failure(IllegalStateException("Objek ini masih memiliki permohonan perpindahan yang menunggu persetujuan."))
        }

        val request = ObjectTransferRequest(
            id = "req_tr_${UUID.randomUUID().toString().take(8)}",
            objectId = objectId,
            objectCode = targetObj.objectCode,
            objectName = targetObj.name,
            fromAreaId = targetObj.areaId,
            toAreaId = toAreaId,
            requestedBy = user.id,
            requesterName = user.name,
            requestReason = reason.trim(),
            supportingNotes = supportingNotes.trim(),
            status = TransferStatus.MENUNGGU_PERSETUJUAN,
            requestedAt = DateUtils.currentTimestamp()
        )

        _transferRequests.value = listOf(request) + _transferRequests.value

        logChange(
            objectId = targetObj.id,
            objectCode = targetObj.objectCode,
            objectName = targetObj.name,
            changedBy = "${user.name} (${user.role.label})",
            changeType = "TRANSFER_REQ",
            oldValues = "Area Pemilik: ${getAreaName(targetObj.areaId)}",
            newValues = "Permintaan transfer ke: ${getAreaName(toAreaId)} | Alasan: $reason",
            relatedTransferRequestId = request.id
        )

        return Result.success(request)
    }

    fun reviewTransferRequest(
        requestId: String,
        approve: Boolean,
        reviewNotes: String
    ): Result<Unit> {
        val user = _currentUser.value
        if (user.role != UserRole.ADMIN) {
            return Result.failure(IllegalAccessException("Hanya Administrator yang berwenang menyetujui atau menolak transfer kepemilikan aset."))
        }

        val request = _transferRequests.value.find { it.id == requestId }
            ?: return Result.failure(NoSuchElementException("Permintaan perpindahan tidak ditemukan."))

        if (request.status != TransferStatus.MENUNGGU_PERSETUJUAN) {
            return Result.failure(IllegalStateException("Permintaan ini sudah diproses sebelumnya."))
        }

        val newStatus = if (approve) TransferStatus.DISETUJUI else TransferStatus.DITOLAK
        val updatedRequest = request.copy(
            status = newStatus,
            reviewedBy = user.id,
            reviewerName = user.name,
            reviewNotes = reviewNotes.trim(),
            reviewedAt = DateUtils.currentTimestamp()
        )

        _transferRequests.value = _transferRequests.value.map {
            if (it.id == requestId) updatedRequest else it
        }

        if (approve) {
            // Apply transfer to object
            val targetObj = _objects.value.find { it.id == request.objectId }
            if (targetObj != null) {
                // Find a default zone in the target area
                val targetZone = _zones.value.find { it.areaId == request.toAreaId }
                val updatedObj = targetObj.copy(
                    areaId = request.toAreaId,
                    zoneId = targetZone?.id ?: targetObj.zoneId,
                    updatedBy = user.name,
                    updatedAt = DateUtils.currentDate()
                )
                _objects.value = _objects.value.map { if (it.id == targetObj.id) updatedObj else it }

                logChange(
                    objectId = targetObj.id,
                    objectCode = targetObj.objectCode,
                    objectName = targetObj.name,
                    changedBy = "${user.name} (Admin)",
                    changeType = "TRANSFER",
                    oldValues = "Area Pemilik: ${getAreaName(request.fromAreaId)}",
                    newValues = "Area Pemilik Baru: ${getAreaName(request.toAreaId)} (Disetujui)",
                    relatedTransferRequestId = requestId
                )
            }
        } else {
            logChange(
                objectId = request.objectId,
                objectCode = request.objectCode,
                objectName = request.objectName,
                changedBy = "${user.name} (Admin)",
                changeType = "TRANSFER_REJECTED",
                oldValues = "Pengajuan transfer ke ${getAreaName(request.toAreaId)}",
                newValues = "Ditolak oleh Admin: $reviewNotes",
                relatedTransferRequestId = requestId
            )
        }

        return Result.success(Unit)
    }

    // Spreadsheet / CSV Import Engine
    data class ImportPreviewRow(
        val rowIndex: Int,
        val rawCode: String,
        val rawName: String,
        val rawAreaCode: String,
        val rawZoneCode: String,
        val rawCategory: String,
        val rawFunction: String,
        val rawPic: String,
        val isValid: Boolean,
        val matchedArea: Area?,
        val matchedZone: Zone?,
        val errors: List<String>,
        val isDuplicate: Boolean
    )

    data class ImportResult(
        val totalProcessed: Int,
        val importedCount: Int,
        val skippedCount: Int,
        val duplicateCount: Int,
        val errorCount: Int,
        val logs: List<String>
    )

    fun parseAndPreviewCsv(csvContent: String): List<ImportPreviewRow> {
        val lines = csvContent.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()

        val rows = mutableListOf<ImportPreviewRow>()
        // Determine if line 0 is header
        val startIndex = if (lines[0].contains("Kode", ignoreCase = true) || lines[0].contains("Code", ignoreCase = true) || lines[0].contains("Nama", ignoreCase = true)) 1 else 0

        for (i in startIndex until lines.size) {
            val line = lines[i]
            val cols = line.split(",").map { it.trim().removeSurrounding("\"") }
            val rawCode = cols.getOrElse(0) { "" }
            val rawName = cols.getOrElse(1) { "" }
            val rawArea = cols.getOrElse(2) { "" }
            val rawZone = cols.getOrElse(3) { "" }
            val rawCat = cols.getOrElse(4) { "Alat Kerja" }
            val rawFunc = cols.getOrElse(5) { "" }
            val rawPic = cols.getOrElse(6) { "Operator" }

            val errors = mutableListOf<String>()
            if (rawCode.isBlank()) errors.add("Kode objek kosong")
            if (rawName.isBlank()) errors.add("Nama objek kosong")

            // Match area
            val matchedArea = _areas.value.find {
                it.areaCode.equals(rawArea, ignoreCase = true) ||
                it.areaCode.startsWith(rawArea, ignoreCase = true) ||
                rawArea.startsWith(it.areaCode, ignoreCase = true) ||
                it.areaName.contains(rawArea, ignoreCase = true) ||
                it.id.equals(rawArea, ignoreCase = true)
            }
            if (matchedArea == null) {
                errors.add("Area '$rawArea' tidak dikenali")
            }

            // Match zone
            val matchedZone = _zones.value.find {
                (matchedArea == null || it.areaId == matchedArea.id) &&
                (it.zoneCode.equals(rawZone, ignoreCase = true) ||
                 it.zoneCode.startsWith(rawZone, ignoreCase = true) ||
                 rawZone.startsWith(it.zoneCode, ignoreCase = true) ||
                 it.zoneName.contains(rawZone, ignoreCase = true))
            } ?: _zones.value.find { matchedArea != null && it.areaId == matchedArea.id }

            // Check duplicate
            val isDuplicate = _objects.value.any { it.objectCode.equals(rawCode, ignoreCase = true) }
            if (isDuplicate) {
                errors.add("Kode '$rawCode' sudah ada di sistem")
            }

            rows.add(
                ImportPreviewRow(
                    rowIndex = i + 1,
                    rawCode = rawCode,
                    rawName = rawName,
                    rawAreaCode = rawArea,
                    rawZoneCode = rawZone,
                    rawCategory = rawCat,
                    rawFunction = rawFunc,
                    rawPic = rawPic,
                    isValid = errors.isEmpty(),
                    matchedArea = matchedArea,
                    matchedZone = matchedZone,
                    errors = errors,
                    isDuplicate = isDuplicate
                )
            )
        }
        return rows
    }

    fun executeImport(previewRows: List<ImportPreviewRow>): ImportResult {
        val user = _currentUser.value
        val validRows = previewRows.filter { it.isValid }
        var imported = 0
        var skipped = 0
        var duplicates = 0
        var errors = 0
        val logs = mutableListOf<String>()

        val newObjects = mutableListOf<SeiriObject>()

        for (row in previewRows) {
            if (row.isDuplicate) {
                duplicates++
                skipped++
                logs.add("Baris ${row.rowIndex}: Dilewati karena duplikat kode '${row.rawCode}'.")
                continue
            }
            if (!row.isValid) {
                errors++
                skipped++
                logs.add("Baris ${row.rowIndex}: Gagal diimpor - ${row.errors.joinToString(", ")}.")
                continue
            }

            val area = row.matchedArea ?: _areas.value.first()
            val zone = row.matchedZone ?: _zones.value.first { it.areaId == area.id }

            val obj = SeiriObject(
                id = "obj_imp_${UUID.randomUUID().toString().take(8)}",
                sourceObjectId = "IMP-${row.rawCode}",
                objectCode = row.rawCode.uppercase(),
                areaId = area.id,
                zoneId = zone.id,
                name = row.rawName,
                description = "Diimpor dari berkas spreadsheet",
                function = row.rawFunction.ifEmpty { "Peralatan operasional standar" },
                category = row.rawCategory.ifEmpty { "Alat Kerja" },
                condition = ObjectCondition.BAIK,
                pic = row.rawPic.ifEmpty { "PIC Area" },
                usageFrequency = UsageFrequency.DAILY,
                lastUsedDate = DateUtils.currentDate(),
                validationStatus = ValidationStatus.BELUM_DIVALIDASI,
                createdBy = user.name,
                updatedBy = user.name,
                createdAt = DateUtils.currentDate(),
                updatedAt = DateUtils.currentDate()
            )
            newObjects.add(obj)
            imported++
        }

        if (newObjects.isNotEmpty()) {
            _objects.value = newObjects + _objects.value
            logChange(
                objectId = "BATCH_IMPORT",
                objectCode = "BATCH",
                objectName = "Impor Spreadsheet $imported Objek",
                changedBy = "${user.name} (Admin)",
                changeType = "IMPORT_BATCH",
                oldValues = "-",
                newValues = "Berhasil mengimpor $imported objek dari spreadsheet."
            )
        }

        return ImportResult(
            totalProcessed = previewRows.size,
            importedCount = imported,
            skippedCount = skipped,
            duplicateCount = duplicates,
            errorCount = errors,
            logs = logs
        )
    }

    // Reports & CSV export generator
    fun generateObjectsCsv(areaIdFilter: String? = null): String {
        val filtered = if (areaIdFilter != null) {
            _objects.value.filter { it.areaId == areaIdFilter }
        } else {
            _objects.value
        }

        val sb = StringBuilder()
        sb.append("Kode Objek,Nama Objek,Area,Zona,Kategori,Kondisi,PIC,Frekuensi,Status Validasi,Status Aset,Terakhir Digunakan\n")
        for (o in filtered) {
            sb.append("\"${o.objectCode}\",")
            sb.append("\"${o.name}\",")
            sb.append("\"${getAreaName(o.areaId)}\",")
            sb.append("\"${getZoneName(o.zoneId)}\",")
            sb.append("\"${o.category}\",")
            sb.append("\"${o.condition.label}\",")
            sb.append("\"${o.pic}\",")
            sb.append("\"${o.usageFrequency.label}\",")
            sb.append("\"${o.validationStatus.label}\",")
            sb.append("\"${if (o.isActive) "Aktif" else "Nonaktif"}\",")
            sb.append("\"${o.lastUsedDate}\"\n")
        }
        return sb.toString()
    }

    // Helpers
    fun getAreaName(areaId: String): String = _areas.value.find { it.id == areaId }?.areaName ?: "Unknown Area"
    fun getAreaCode(areaId: String): String = _areas.value.find { it.id == areaId }?.areaCode ?: "??"
    fun getZoneName(zoneId: String): String = _zones.value.find { it.id == zoneId }?.zoneName ?: "Unknown Zone"

    private fun logChange(
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

    companion object {
        val instance: SeiriRepository by lazy { SeiriRepository() }
    }
}

