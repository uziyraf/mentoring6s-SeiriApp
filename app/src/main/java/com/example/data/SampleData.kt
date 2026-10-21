package com.example.data

import com.example.model.*

object SampleData {
    val adminUser = User(
        id = "usr_admin",
        name = "Hartono Kusuma",
        username = "admin.hartono",
        role = UserRole.ADMIN,
        department = "Komite 6S & Plant Management"
    )

    val mentors = listOf(
        User(
            id = "usr_m1",
            name = "Andi Pratama",
            username = "mentor.andi",
            role = UserRole.MENTOR,
            assignedAreaId = "area_prf",
            department = "Divisi Produksi Preform"
        ),
        User(
            id = "usr_m2",
            name = "Budi Santoso",
            username = "mentor.budi",
            role = UserRole.MENTOR,
            assignedAreaId = "area_blw",
            department = "Divisi Blowing & Mold"
        ),
        User(
            id = "usr_m3",
            name = "Citra Dewi",
            username = "mentor.citra",
            role = UserRole.MENTOR,
            assignedAreaId = "area_qc",
            department = "Quality Control"
        ),
        User(
            id = "usr_m4",
            name = "Doni Wijaya",
            username = "mentor.doni",
            role = UserRole.MENTOR,
            assignedAreaId = "area_wh",
            department = "Warehouse & Logistik"
        ),
        User(
            id = "usr_m5",
            name = "Eko Prasetyo",
            username = "mentor.eko",
            role = UserRole.MENTOR,
            assignedAreaId = "area_pck",
            department = "Divisi Packing & Palletizing"
        ),
        User(
            id = "usr_m6",
            name = "Fani Rahma",
            username = "mentor.fani",
            role = UserRole.MENTOR,
            assignedAreaId = "area_mc",
            department = "Machining & Tool Crib"
        )
    )

    val allUsers = listOf(adminUser) + mentors

    // Hirarki: GROUP (PLANT 1, PLANT 2) -> AREA -> ZONE -> OBJECT
    val initialAreas = listOf(
        // PLANT 1
        Area("area_prf", "PLANT 1", "PRF-1", "PREFORM Plant 1", "Area produksi injection preform botol"),
        Area("area_blw", "PLANT 1", "BLW-1", "BLOWING Plant 1", "Area mesin blowing dan kompresor bertekanan tinggi"),
        Area("area_pck", "PLANT 1", "PCK-1", "PACKING Plant 1", "Area pengemasan otomatis dan palletizing"),
        // PLANT 2
        Area("area_mc", "PLANT 2", "MC-2", "MACHINING Plant 2", "Area permesinan CNC, milling, dan bubut"),
        Area("area_qc", "PLANT 2", "QC-2", "QUALITY CONTROL Plant 2", "Area inspeksi mutu, CMM, dan kalibrasi"),
        Area("area_wh", "PLANT 2", "WH-2", "WAREHOUSE Plant 2", "Area penyimpanan bahan baku, transit, dan FG")
    )

    val initialZones = listOf(
        // PREFORM Plant 1
        Zone("zn_prf_1", "area_prf", "PRF-Z1", "PREFORM - AREA 1"),
        Zone("zn_prf_2", "area_prf", "PRF-Z2", "PREFORM - AREA 2"),
        // BLOWING Plant 1
        Zone("zn_blw_1", "area_blw", "BLW-Z1", "BLOWING - LINE 1"),
        Zone("zn_blw_2", "area_blw", "BLW-Z2", "BLOWING - MOLD STORAGE"),
        // PACKING Plant 1
        Zone("zn_pck_1", "area_pck", "PCK-Z1", "PACKING - AUTO CARTON"),
        Zone("zn_pck_2", "area_pck", "PCK-Z2", "PACKING - PALLETIZING"),
        // MACHINING Plant 2
        Zone("zn_mc_1", "area_mc", "MC-Z1", "Zona CNC Milling Vertikal"),
        Zone("zn_mc_2", "area_mc", "MC-Z2", "Zona Mesin Bubut Manual"),
        // QC & WH Plant 2
        Zone("zn_qc_1", "area_qc", "QC-Z1", "Zona Mesin Pengukur CMM"),
        Zone("zn_wh_1", "area_wh", "WH-Z1", "Zona Rak Raw Material Steel Bar")
    )

    val initialObjects = listOf(
        // PREFORM - AREA 1
        SeiriObject(
            id = "obj_prf_1",
            sourceObjectId = "SPR-PRF-101",
            objectCode = "OBJ-PRF-001",
            areaId = "area_prf",
            zoneId = "zn_prf_1",
            name = "HOPPER A",
            description = "Hopper penampung biji plastik resin PET mesin injeksi #1",
            function = "Menampung dan mengalirkan resin PET ke barrel",
            category = "Komponen Mesin",
            condition = ObjectCondition.BAIK,
            pic = "Andi Pratama",
            usageFrequency = UsageFrequency.DAILY,
            lastUsedDate = "2026-10-19",
            validationStatus = ValidationStatus.TERVALIDASI
        ),
        SeiriObject(
            id = "obj_prf_2",
            sourceObjectId = "SPR-PRF-102",
            objectCode = "OBJ-PRF-002",
            areaId = "area_prf",
            zoneId = "zn_prf_1",
            name = "INJECTION MOLD 1",
            description = "Cetakan injeksi preform botol 600ml cavity 48",
            function = "Pencetak preform PET",
            category = "Dies Cetakan",
            condition = ObjectCondition.BAIK,
            pic = "Andi Pratama",
            usageFrequency = UsageFrequency.DAILY,
            lastUsedDate = "2026-10-19",
            validationStatus = ValidationStatus.BELUM_DIVALIDASI
        ),
        SeiriObject(
            id = "obj_prf_3",
            sourceObjectId = "SPR-PRF-103",
            objectCode = "OBJ-PRF-003",
            areaId = "area_prf",
            zoneId = "zn_prf_1",
            name = "ROBOT ARM TAKEOUT",
            description = "Lengan robot pengambil preform dari cetakan",
            function = "Pelepas benda kerja otomatis",
            category = "Otomasi",
            condition = ObjectCondition.BAIK,
            pic = "Andi Pratama",
            usageFrequency = UsageFrequency.DAILY,
            lastUsedDate = "2026-10-18",
            validationStatus = ValidationStatus.BELUM_DIVALIDASI
        ),
        SeiriObject(
            id = "obj_prf_4",
            sourceObjectId = "SPR-PRF-104",
            objectCode = "OBJ-PRF-004",
            areaId = "area_prf",
            zoneId = "zn_prf_1",
            name = "DRUM PELUMAS MOLD BEKAS",
            description = "Wadah drum kosong yang tertinggal di samping conveyor",
            function = "Wadah pelumas",
            category = "Limbah / Kemasan",
            condition = ObjectCondition.USANG,
            pic = "Sutrisno",
            usageFrequency = UsageFrequency.NEVER,
            lastUsedDate = "2026-07-12",
            validationStatus = ValidationStatus.BELUM_DIVALIDASI
        ),
        // PREFORM - AREA 2
        SeiriObject(
            id = "obj_prf_5",
            sourceObjectId = "SPR-PRF-201",
            objectCode = "OBJ-PRF-005",
            areaId = "area_prf",
            zoneId = "zn_prf_2",
            name = "DRYER SILICA PET 500KG",
            description = "Mesin pengering dehumidifier resin PET suhu 160°C",
            function = "Menghilangkan kelembaban biji plastik",
            category = "Mesin Pengering",
            condition = ObjectCondition.BAIK,
            pic = "Andi Pratama",
            usageFrequency = UsageFrequency.DAILY,
            lastUsedDate = "2026-10-19",
            validationStatus = ValidationStatus.TERVALIDASI
        ),

        // BLOWING Plant 1
        SeiriObject(
            id = "obj_blw_1",
            sourceObjectId = "SPR-BLW-101",
            objectCode = "OBJ-BLW-001",
            areaId = "area_blw",
            zoneId = "zn_blw_1",
            name = "BLOW MOLD CAVITY 8",
            description = "Cetakan tiup botol 1500ml",
            function = "Membentuk botol dari preform panas",
            category = "Mold",
            condition = ObjectCondition.BAIK,
            pic = "Budi Santoso",
            usageFrequency = UsageFrequency.DAILY,
            lastUsedDate = "2026-10-19",
            validationStatus = ValidationStatus.TERVALIDASI
        ),
        SeiriObject(
            id = "obj_blw_2",
            sourceObjectId = "SPR-BLW-102",
            objectCode = "OBJ-BLW-002",
            areaId = "area_blw",
            zoneId = "zn_blw_1",
            name = "KOMPRESOR HIGH PRESSURE 40 BAR",
            description = "Kompresor udara peniup botol PET",
            function = "Suplai udara tekanan tinggi 40 bar",
            category = "Kompresor",
            condition = ObjectCondition.BAIK,
            pic = "Budi Santoso",
            usageFrequency = UsageFrequency.DAILY,
            lastUsedDate = "2026-10-19",
            validationStatus = ValidationStatus.TERVALIDASI
        ),

        // PACKING Plant 1
        SeiriObject(
            id = "obj_pck_1",
            sourceObjectId = "SPR-PCK-101",
            objectCode = "OBJ-PCK-001",
            areaId = "area_pck",
            zoneId = "zn_pck_1",
            name = "TAPING MACHINE AUTO",
            description = "Mesin perekat isolasi kardus",
            function = "Penyegelan karton box otomatis",
            category = "Mesin Packing",
            condition = ObjectCondition.BAIK,
            pic = "Eko Prasetyo",
            usageFrequency = UsageFrequency.DAILY,
            lastUsedDate = "2026-10-19",
            validationStatus = ValidationStatus.TERVALIDASI
        ),

        // MACHINING Plant 2
        SeiriObject(
            id = "obj_mc_1",
            sourceObjectId = "SPR-MC-101",
            objectCode = "OBJ-MC-001",
            areaId = "area_mc",
            zoneId = "zn_mc_1",
            name = "Kunci Pas Chuck CNC 24mm",
            description = "Kunci pas khusus membuka collet chuck mesin CNC Mazak",
            function = "Penggantian alat potong collet ER32",
            category = "Perkakas Tangan",
            condition = ObjectCondition.BAIK,
            pic = "Fani Rahma",
            usageFrequency = UsageFrequency.DAILY,
            lastUsedDate = "2026-10-18",
            validationStatus = ValidationStatus.TERVALIDASI
        ),
        SeiriObject(
            id = "obj_mc_2",
            sourceObjectId = "SPR-MC-102",
            objectCode = "OBJ-MC-002",
            areaId = "area_mc",
            zoneId = "zn_mc_1",
            name = "Jig Fixture Clamp Model Lama (Tipe X1)",
            description = "Clamp bekas produk batch tahun 2024 yang sudah discontinue",
            function = "Penjepit benda kerja tipe X1",
            category = "Jig & Fixture",
            condition = ObjectCondition.USANG,
            pic = "Fani Rahma",
            usageFrequency = UsageFrequency.NEVER,
            lastUsedDate = "2025-04-10",
            validationStatus = ValidationStatus.PERLU_KONFIRMASI
        )
    )

    // Initial multi-session data (termasuk draf yang tersimpan di Tahap 2)
    val initialSessions = listOf(
        MentoringSession(
            id = "ses_draft_01",
            mentorId = "usr_m1",
            mentorName = "Andi Pratama",
            groupName = "PLANT 1",
            areaId = "area_prf",
            zoneId = "zn_prf_1",
            sessionDate = "2026-10-19",
            currentStage = 2, // Tersimpan di Tahap 2 (Multi-session draft)
            status = SessionStatus.DRAFT,
            notes = "Mentoring rutin Seiri di PREFORM - AREA 1. Melanjutkan pemeriksaan kebutuhan objek.",
            startedAt = "2026-10-19 08:30",
            inspections = listOf(
                SessionObjectInspection(
                    objectId = "obj_prf_1",
                    objectCode = "OBJ-PRF-001",
                    objectName = "HOPPER A",
                    function = "Menampung dan mengalirkan resin PET ke barrel injeksi",
                    usageFrequency = "Setiap Hari/Shift",
                    lastUsed = "Hari ini",
                    initialAssessment = "Masih Diperlukan",
                    seiriDecision = "Diperlukan (Hijau)",
                    decisionReason = "Alat produksi vital",
                    isStage2Checked = true
                ),
                SessionObjectInspection(
                    objectId = "obj_prf_2",
                    objectCode = "OBJ-PRF-002",
                    objectName = "INJECTION MOLD 1",
                    function = "Mencetak preform 600ml",
                    usageFrequency = "Setiap Hari/Shift",
                    lastUsed = "Hari ini",
                    initialAssessment = "Masih Diperlukan",
                    seiriDecision = "Diperlukan (Hijau)",
                    decisionReason = "Cetakan utama",
                    isStage2Checked = true
                ),
                SessionObjectInspection(
                    objectId = "obj_prf_3",
                    objectCode = "OBJ-PRF-003",
                    objectName = "ROBOT ARM TAKEOUT",
                    function = "Pelepas benda kerja otomatis",
                    usageFrequency = "Setiap Hari/Shift",
                    lastUsed = "Kemarin",
                    initialAssessment = "Masih Diperlukan",
                    seiriDecision = "Diperlukan (Hijau)",
                    decisionReason = "Bagian dari siklus otomatis",
                    isStage2Checked = false
                ),
                SessionObjectInspection(
                    objectId = "obj_prf_4",
                    objectCode = "OBJ-PRF-004",
                    objectName = "DRUM PELUMAS MOLD BEKAS",
                    function = "Wadah pelumas bekas",
                    usageFrequency = "Jarang/Tidak Pernah",
                    lastUsed = "3 bulan lalu",
                    initialAssessment = "Ragu-ragu / Tidak Diperlukan",
                    seiriDecision = "Tidak Diperlukan / Red Tag (Merah)",
                    decisionReason = "Sudah kosong dan menghalangi akses conveyor",
                    actionDescription = "Pindahkan ke TPS limbah B3 dan bersihkan ceceran oli",
                    pic = "Sutrisno",
                    dueDate = "2026-10-22",
                    isStage2Checked = true
                )
            )
        ),
        MentoringSession(
            id = "ses_comp_01",
            mentorId = "usr_m2",
            mentorName = "Budi Santoso",
            groupName = "PLANT 1",
            areaId = "area_blw",
            zoneId = "zn_blw_1",
            sessionDate = "2026-10-18",
            currentStage = 5,
            status = SessionStatus.COMPLETED,
            notes = "Validasi Seiri BLOWING LINE 1 selesai 100%.",
            startedAt = "2026-10-18 09:00",
            completedAt = "2026-10-18 10:15",
            inspections = listOf(
                SessionObjectInspection(
                    objectId = "obj_blw_1",
                    objectCode = "OBJ-BLW-001",
                    objectName = "BLOW MOLD CAVITY 8",
                    function = "Membentuk botol dari preform",
                    usageFrequency = "Setiap Hari/Shift",
                    lastUsed = "Hari ini",
                    initialAssessment = "Masih Diperlukan",
                    seiriDecision = "Diperlukan (Hijau)",
                    decisionReason = "Alat aktif produksi",
                    isStage2Checked = true,
                    isStage3Checked = true,
                    isStage4Checked = true
                )
            )
        )
    )

    val initialValidations = listOf(
        SeiriValidation(
            id = "val_01",
            sessionId = "ses_comp_01",
            objectId = "obj_prf_1",
            validationStatus = ValidationStatus.TERVALIDASI,
            condition = ObjectCondition.BAIK,
            isNecessary = true,
            usageFrequency = UsageFrequency.DAILY,
            lastUsedAt = "2026-10-18",
            seiriDecision = SeiriDecision.TETAP_DI_AREA,
            decisionReason = "Komponen utama jalur injeksi",
            responsiblePerson = "Andi Pratama",
            objectNameSnapshot = "HOPPER A",
            areaNameSnapshot = "PREFORM Plant 1",
            createdBy = "Andi Pratama",
            createdAt = "2026-10-18 09:30"
        )
    )

    val initialTransferRequests = listOf(
        ObjectTransferRequest(
            id = "req_tr_01",
            objectId = "obj_prf_4",
            objectCode = "OBJ-PRF-004",
            objectName = "DRUM PELUMAS MOLD BEKAS",
            fromAreaId = "area_prf",
            toAreaId = "area_wh",
            requestedBy = "usr_m1",
            requesterName = "Andi Pratama",
            requestReason = "Drum kosong tidak terpakai di area preform, dipindahkan ke gudang limbah",
            supportingNotes = "Disetujui supervisor shift 1",
            status = TransferStatus.MENUNGGU_PERSETUJUAN,
            requestedAt = "2026-10-19 10:00"
        )
    )

    val initialChangeLogs = listOf(
        ObjectChangeLog(
            id = "log_01",
            objectId = "obj_prf_1",
            objectCode = "OBJ-PRF-001",
            objectName = "HOPPER A",
            changedBy = "Hartono Kusuma (Admin)",
            changeType = "IMPORT",
            oldValues = "-",
            newValues = "Master aset diinisialisasi untuk PLANT 1",
            changedAt = "2026-10-15 08:00"
        )
    )

    val initialActions = listOf(
        SeiriAction(
            id = "act_01",
            objectId = "obj_prf_4",
            objectCode = "OBJ-PRF-004",
            objectName = "DRUM PELUMAS MOLD BEKAS",
            areaId = "area_prf",
            actionDescription = "Pindahkan ke TPS limbah B3 dan bersihkan ceceran oli",
            assignedTo = "Sutrisno",
            dueDate = "2026-10-22",
            status = "PENDING"
        )
    )

    val questionBank = listOf(
        SeiriQuestion("q01", "SEIRI-T1-01", 1, "Tahap 1: Identifikasi Objek", "Apakah objek memiliki label identitas dan terdaftar di master?", "OBJEK", false),
        SeiriQuestion("q02", "SEIRI-T1-02", 1, "Tahap 1: Identifikasi Objek", "Apakah objek berada di zona yang tepat?", "OBJEK", true),
        SeiriQuestion("q03", "SEIRI-T2-01", 2, "Tahap 2: Periksa Kebutuhan", "Berapa frekuensi penggunaan objek ini dalam shift kerja?", "OBJEK", true),
        SeiriQuestion("q04", "SEIRI-T2-02", 2, "Tahap 2: Periksa Kebutuhan", "Apakah ketiadaan objek ini akan menghambat alur produksi?", "OBJEK", false),
        SeiriQuestion("q05", "SEIRI-T3-01", 3, "Tahap 3: Keputusan Seiri", "Apakah objek harus tetap, dipindahkan, atau diberi Red Tag?", "OBJEK", true),
        SeiriQuestion("q06", "SEIRI-T4-01", 4, "Tahap 4: PIC & Tindakan", "Apakah ada PIC dan tenggat waktu penyelesaian tindakan korektif?", "OBJEK", true),
        SeiriQuestion("q07", "SEIRI-T5-01", 5, "Tahap 5: Standarisasi", "Apakah seluruh tim di area sepakat menjaga standar Seiri?", "AREA", true)
    )
}
