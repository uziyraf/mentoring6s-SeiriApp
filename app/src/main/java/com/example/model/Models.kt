package com.example.model

enum class UserRole(val label: String) {
    ADMIN("Administrator"),
    MENTOR("Mentor 6S"), // Selama ini dikenal sebagai User CI
    GUEST("Tamu")
}

data class User(
    val id: String,
    val name: String,
    val email: String = "", 
    val role: UserRole,
    val assignedAreaId: String? = null,
    val department: String = ""
)

// Hirarki: GROUP -> AREA -> ZONE -> OBJECT
data class Area(
    val id: String,
    val group: String = "PLANT 1", // Hirarki GROUP (Contoh: PLANT 1, PLANT 2)
    val areaCode: String,
    val areaName: String,          // Contoh: PREFORM Plant 1
    val description: String = "",
    val isActive: Boolean = true
)

data class Zone(
    val id: String,
    val areaId: String,
    val zoneCode: String,
    val zoneName: String,          // Contoh: PREFORM - AREA 1
    val isActive: Boolean = true
)

data class MentorAreaAssignment(
    val id: String,
    val mentorId: String,
    val areaId: String,
    val assignedBy: String,
    val validFrom: String,
    val validUntil: String? = null
)

enum class ObjectCondition(val label: String) {
    BAIK("Kondisi Baik"),
    RUSAK_RINGAN("Rusak Ringan"),
    RUSAK_BERAT("Rusak Berat"),
    USANG("Usang / Kadaluarsa")
}

enum class UsageFrequency(val label: String) {
    DAILY("Setiap Hari"),
    WEEKLY("Mingguan"),
    MONTHLY("Bulanan"),
    RARELY("Jarang Sekali"),
    NEVER("Tidak Pernah Digunakan"),
    UNKNOWN("Tidak Diketahui")
}

enum class ValidationStatus(val label: String) {
    BELUM_DIVALIDASI("Belum Divalidasi"),
    TERVALIDASI("Tervalidasi"),
    PERLU_KONFIRMASI("Perlu Konfirmasi")
}

enum class SeiriDecision(val label: String, val description: String) {
    TETAP_DI_AREA("Diperlukan (Hijau)", "Objek masih dibutuhkan dan berada di area yang tepat"),
    PINDAHKAN_LOKASI("Dipindahkan", "Objek berguna namun harus dipindahkan ke area/zona lain"),
    PERLU_KONFIRMASI("Perlu Konfirmasi (Kuning)", "Perlu klarifikasi supervisor atau PIC mengenai kebutuhan objek"),
    TIDAK_DIPERLUKAN("Tidak Diperlukan / Red Tag (Merah)", "Barang tidak dipakai di area ini, usulkan red tag / disposisi"),
    PERLU_TINDAK_LANJUT("Perlu Tindak Lanjut", "Perlu perbaikan, kalibrasi, atau pembersihan khusus")
}

data class SeiriObject(
    val id: String,
    val sourceObjectId: String? = null,
    val objectCode: String,
    val areaId: String,
    val zoneId: String,
    val name: String,              // Contoh: HOPPER A
    val description: String = "",
    val function: String = "",
    val category: String = "Alat Kerja",
    val condition: ObjectCondition = ObjectCondition.BAIK,
    val pic: String = "",
    val usageFrequency: UsageFrequency = UsageFrequency.DAILY,
    val lastUsedDate: String = "2026-10-15",
    val validationStatus: ValidationStatus = ValidationStatus.BELUM_DIVALIDASI,
    val isActive: Boolean = true,
    val version: Int = 1,
    val createdBy: String = "System",
    val updatedBy: String = "System",
    val createdAt: String = "2026-10-01",
    val updatedAt: String = "2026-10-18",
    val photoUrl: String? = null
)

enum class SessionStatus(val label: String) {
    DRAFT("Draft / Berjalan"),
    COMPLETED("Selesai")
}

// Data per objek yang diinspeksi dalam sesi mentoring (multi-session)
data class SessionObjectInspection(
    val objectId: String,
    val objectCode: String,
    val objectName: String,
    var function: String = "",
    var usageFrequency: String = "Setiap Hari/Shift", // [Setiap Hari/Shift, Mingguan/Bulanan, Jarang/Tidak Pernah]
    var lastUsed: String = "Hari ini",
    var initialAssessment: String = "Masih Diperlukan", // [Masih Diperlukan, Ragu-ragu / Tidak Diperlukan]
    var photoUri: String? = null,
    var seiriDecision: String = "Diperlukan (Hijau)", // [Diperlukan (Hijau), Dipindahkan, Tidak Diperlukan / Red Tag (Merah), Perlu Konfirmasi (Kuning)]
    var decisionReason: String = "",
    // Tahap 4 fields (Khusus Red Tag / Dipindahkan)
    var actionDescription: String = "",
    var pic: String = "",
    var dueDate: String = "",
    var completionPhotoUri: String? = null,
    var isStage2Checked: Boolean = false,
    var isStage3Checked: Boolean = false,
    var isStage4Checked: Boolean = false
)

data class MentoringSession(
    val id: String,
    val mentorId: String,
    val mentorName: String = "",
    val groupName: String = "PLANT 1",
    val areaId: String,
    val zoneId: String,
    val sessionDate: String,
    val currentStage: Int = 1, // Tahap 1 sampai 5
    val status: SessionStatus = SessionStatus.DRAFT,
    val notes: String = "",
    val startedAt: String = "",
    val completedAt: String? = null,
    val inspections: List<SessionObjectInspection> = emptyList()
)

data class SeiriValidation(
    val id: String,
    val sessionId: String,
    val objectId: String,
    val validationStatus: ValidationStatus,
    val condition: ObjectCondition = ObjectCondition.BAIK,
    val isNecessary: Boolean = true,
    val usageFrequency: UsageFrequency,
    val lastUsedAt: String? = null,
    val seiriDecision: SeiriDecision,
    val decisionReason: String,
    val responsiblePerson: String,
    val conditionNotes: String = "",
    val followUpAction: String = "",
    val objectNameSnapshot: String = "",
    val areaNameSnapshot: String = "",
    val createdBy: String,
    val createdAt: String
)

enum class TransferStatus(val label: String) {
    MENUNGGU_PERSETUJUAN("Menunggu Persetujuan"),
    DISETUJUI("Disetujui"),
    DITOLAK("Ditolak"),
    DIBATALKAN("Dibatalkan")
}

data class ObjectTransferRequest(
    val id: String,
    val objectId: String,
    val objectCode: String,
    val objectName: String,
    val fromAreaId: String,
    val toAreaId: String,
    val requestedBy: String,
    val requesterName: String,
    val requestReason: String,
    val supportingNotes: String = "",
    val status: TransferStatus = TransferStatus.MENUNGGU_PERSETUJUAN,
    val reviewedBy: String? = null,
    val reviewerName: String? = null,
    val reviewNotes: String? = null,
    val requestedAt: String,
    val reviewedAt: String? = null
)

data class ObjectChangeLog(
    val id: String,
    val objectId: String,
    val objectCode: String,
    val objectName: String,
    val changedBy: String,
    val changeType: String, // CREATE, UPDATE, TRANSFER, DEACTIVATE, REACTIVATE, VALIDATION
    val oldValues: String,
    val newValues: String,
    val relatedTransferRequestId: String? = null,
    val changedAt: String
)

data class SeiriQuestion(
    val id: String,
    val questionCode: String,
    val stage: Int, // 1 to 5
    val stageName: String,
    val questionText: String,
    val targetLevel: String = "OBJEK", // AREA, ZONA, OBJEK
    val isRequired: Boolean = false,
    val answerType: String = "YES_NO", // YES_NO, CHOICE, TEXT
    val isActive: Boolean = true
)

data class SeiriAction(
    val id: String,
    val validationId: String? = null,
    val objectId: String,
    val objectCode: String,
    val objectName: String,
    val areaId: String,
    val actionDescription: String,
    val assignedTo: String,
    val dueDate: String,
    val status: String = "PENDING", // PENDING, IN_PROGRESS, COMPLETED
    val completedAt: String? = null
)
