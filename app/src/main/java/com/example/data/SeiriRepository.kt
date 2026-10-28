package com.example.data

import android.util.Log
import com.example.data.repository.AreaRepository
import com.example.data.repository.ObjectRepository
import com.example.data.service.ImportCsvService
import com.example.data.utils.DateUtils
import com.example.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class SeiriRepository private constructor() {

    val groups get() = AreaRepository.groups

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError.asStateFlow()

    private val _currentUser = MutableStateFlow<User>(
        User(
            id = "guest_local",
            name = "Tamu (Guest)",
            email = "guest@seiri.local",
            role = UserRole.GUEST,
            assignedAreaId = null,
            department = "Visitor"
        )
    )
    val currentUser: StateFlow<User> = _currentUser.asStateFlow()

    fun setCurrentUser(user: User) {
        _currentUser.value = user
    }

    val areas get() = AreaRepository.areas
    val zones get() = AreaRepository.zones

    val objects get() = ObjectRepository.objects

    private val _sessions = MutableStateFlow<List<MentoringSession>>(SampleData.initialSessions)
    val sessions: StateFlow<List<MentoringSession>> = _sessions.asStateFlow()

    private val _validations = MutableStateFlow<List<SeiriValidation>>(SampleData.initialValidations)
    val validations: StateFlow<List<SeiriValidation>> = _validations.asStateFlow()

    private val _transferRequests = MutableStateFlow<List<ObjectTransferRequest>>(SampleData.initialTransferRequests)
    val transferRequests: StateFlow<List<ObjectTransferRequest>> = _transferRequests.asStateFlow()

    val changeLogs get() = ObjectRepository.changeLogs

    private val _actions = MutableStateFlow<List<SeiriAction>>(SampleData.initialActions)
    val actions: StateFlow<List<SeiriAction>> = _actions.asStateFlow()

    private val _questions = MutableStateFlow<List<SeiriQuestion>>(SampleData.questionBank)
    val questions: StateFlow<List<SeiriQuestion>> = _questions.asStateFlow()

    init {
        initFirestoreSync()
    }

    fun initFirestoreSync() {
        _isSyncing.value = true
        AreaRepository.initSync()
        ObjectRepository.initSync()
        MentoringReportRepository.initSync()
    }

    // Demo Role Switcher
    fun switchDemoUser(userId: String) {
        val user = SampleData.allUsers.find { it.id == userId } ?: return
        _currentUser.value = user
    }

    // Permission checks (delegated to ObjectRepository)
    fun canManageObject(targetObject: SeiriObject, user: User = _currentUser.value): Boolean =
        ObjectRepository.canManageObject(targetObject, user)

    fun canManageArea(areaId: String, user: User = _currentUser.value): Boolean =
        ObjectRepository.canManageArea(areaId, user)

    // Object CRUD bridges (logic lives in ObjectRepository)
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
    ): Result<SeiriObject> = ObjectRepository.addObject(
        _currentUser.value, code, name, areaId, zoneId, description,
        function, category, condition, pic, usageFrequency, sourceObjectId
    )

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
    ): Result<SeiriObject> = ObjectRepository.updateObject(
        _currentUser.value, id, name, zoneId, description,
        function, category, condition, pic, usageFrequency
    )

    fun deactivateObject(id: String, reason: String): Result<Unit> =
        ObjectRepository.deactivateObject(_currentUser.value, id, reason)

    fun reactivateObject(id: String): Result<Unit> =
        ObjectRepository.reactivateObject(_currentUser.value, id)

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
        val zoneObjs = ObjectRepository.objects.value.filter { it.areaId == areaId && it.zoneId == zoneId && it.isActive }
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
            val targetObj = ObjectRepository.objects.value.find { it.id == insp.objectId }
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
                ObjectRepository.objects.value = ObjectRepository.objects.value.map { if (it.id == targetObj.id) updatedObj else it }

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

        ObjectRepository.logChange(
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
        val targetObj = ObjectRepository.objects.value.find { it.id == objectId }
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
        ObjectRepository.objects.value = ObjectRepository.objects.value.map { if (it.id == objectId) updatedObj else it }

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

        ObjectRepository.logChange(
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
        val targetObj = ObjectRepository.objects.value.find { it.id == objectId }
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

        ObjectRepository.logChange(
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
            val targetObj = ObjectRepository.objects.value.find { it.id == request.objectId }
            if (targetObj != null) {
                // Find a default zone in the target area
                val targetZone = AreaRepository.zones.value.find { it.areaId == request.toAreaId }
                val updatedObj = targetObj.copy(
                    areaId = request.toAreaId,
                    zoneId = targetZone?.id ?: targetObj.zoneId,
                    updatedBy = user.name,
                    updatedAt = DateUtils.currentDate()
                )
                ObjectRepository.objects.value = ObjectRepository.objects.value.map { if (it.id == targetObj.id) updatedObj else it }

                ObjectRepository.logChange(
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
            ObjectRepository.logChange(
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
            ObjectRepository.objects.value = result.newObjects + ObjectRepository.objects.value
            ObjectRepository.logChange(
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

    companion object {
        val instance: SeiriRepository by lazy { SeiriRepository() }
    }
}

