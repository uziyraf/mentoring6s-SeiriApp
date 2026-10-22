package com.example.data

import android.util.Log
import com.example.data.mapper.FirestoreMapper
import com.example.data.repository.AreaRepository
import com.example.data.service.ImportCsvService
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

    private var objectsListener: ListenerRegistration? = null

    val groups get() = AreaRepository.groups

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError.asStateFlow()

    private val _currentUser = MutableStateFlow<User>(SampleData.adminUser)
    val currentUser: StateFlow<User> = _currentUser.asStateFlow()

    val areas get() = AreaRepository.areas
    val zones get() = AreaRepository.zones

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

        AreaRepository.initSync()

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
            newValues = "Objek baru ditambahkan: ${newObj.name} di ${AreaRepository.getAreaName(areaId)}"
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
        val area = AreaRepository.areas.value.find { it.id == areaId }
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
                    areaNameSnapshot = AreaRepository.getAreaName(targetObj.areaId),
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
            objectName = "Sesi Mentoring ${AreaRepository.getZoneName(session.zoneId)}",
            changedBy = "${user.name} (${user.role.label})",
            changeType = "SESSION_COMPLETE",
            oldValues = "Status: DRAFT",
            newValues = "Status: COMPLETED (${session.inspections.size} objek diperiksa)"
        )

        return Result.success(Unit)
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
            areaNameSnapshot = AreaRepository.getAreaName(targetObj.areaId),
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
            oldValues = "Area Pemilik: ${AreaRepository.getAreaName(targetObj.areaId)}",
            newValues = "Permintaan transfer ke: ${AreaRepository.getAreaName(toAreaId)} | Alasan: $reason",
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
                val targetZone = AreaRepository.zones.value.find { it.areaId == request.toAreaId }
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
                    oldValues = "Area Pemilik: ${AreaRepository.getAreaName(request.fromAreaId)}",
                    newValues = "Area Pemilik Baru: ${AreaRepository.getAreaName(request.toAreaId)} (Disetujui)",
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
                oldValues = "Pengajuan transfer ke ${AreaRepository.getAreaName(request.toAreaId)}",
                newValues = "Ditolak oleh Admin: $reviewNotes",
                relatedTransferRequestId = requestId
            )
        }

        return Result.success(Unit)
    }

    // Spreadsheet import: parsing lives in ImportCsvService, state mutation stays here
    fun executeImport(previewRows: List<ImportCsvService.ImportPreviewRow>): ImportCsvService.ImportResult {
        val user = _currentUser.value
        val result = ImportCsvService.executeImport(previewRows, user, AreaRepository.areas.value, AreaRepository.zones.value)
        if (result.newObjects.isNotEmpty()) {
            _objects.value = result.newObjects + _objects.value
            logChange(
                objectId = "BATCH_IMPORT",
                objectCode = "BATCH",
                objectName = "Impor Spreadsheet ${result.importedCount} Objek",
                changedBy = "${user.name} (Admin)",
                changeType = "IMPORT_BATCH",
                oldValues = "-",
                newValues = "Berhasil mengimpor ${result.importedCount} objek dari spreadsheet."
            )
        }
        return result
    }

    // Helpers (delegated to AreaRepository)
    fun getAreaName(areaId: String): String = AreaRepository.getAreaName(areaId)
    fun getAreaCode(areaId: String): String = AreaRepository.getAreaCode(areaId)
    fun getZoneName(zoneId: String): String = AreaRepository.getZoneName(zoneId)

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

