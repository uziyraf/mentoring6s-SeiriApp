package com.example.data.service

import com.example.data.utils.DateUtils
import com.example.model.Area
import com.example.model.ObjectCondition
import com.example.model.SeiriObject
import com.example.model.UsageFrequency
import com.example.model.User
import com.example.model.ValidationStatus
import com.example.model.Zone
import java.util.UUID

object ImportCsvService {

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
        val logs: List<String>,
        val newObjects: List<SeiriObject> = emptyList()
    )

    fun parseAndPreviewCsv(
        csvContent: String,
        areas: List<Area>,
        zones: List<Zone>,
        objects: List<SeiriObject>
    ): List<ImportPreviewRow> {
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
            val matchedArea = areas.find {
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
            val matchedZone = zones.find {
                (matchedArea == null || it.areaId == matchedArea.id) &&
                (it.zoneCode.equals(rawZone, ignoreCase = true) ||
                 it.zoneCode.startsWith(rawZone, ignoreCase = true) ||
                 rawZone.startsWith(it.zoneCode, ignoreCase = true) ||
                 it.zoneName.contains(rawZone, ignoreCase = true))
            } ?: zones.find { matchedArea != null && it.areaId == matchedArea.id }

            // Check duplicate
            val isDuplicate = objects.any { it.objectCode.equals(rawCode, ignoreCase = true) }
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

    fun executeImport(
        previewRows: List<ImportPreviewRow>,
        currentUser: User,
        areas: List<Area>,
        zones: List<Zone>
    ): ImportResult {
        val user = currentUser
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

            val area = row.matchedArea ?: areas.first()
            val zone = row.matchedZone ?: zones.first { it.areaId == area.id }

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

        return ImportResult(
            totalProcessed = previewRows.size,
            importedCount = imported,
            skippedCount = skipped,
            duplicateCount = duplicates,
            errorCount = errors,
            logs = logs,
            newObjects = newObjects
        )
    }
}
