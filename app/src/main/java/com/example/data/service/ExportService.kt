package com.example.data.service

import com.example.model.Area
import com.example.model.MentoringSession
import com.example.model.SeiriObject
import com.example.model.Zone

object ExportService {

    fun generateSessionJson(session: MentoringSession, areas: List<Area>, zones: List<Zone>): String {
        val area = areas.find { it.id == session.areaId }
        val zone = zones.find { it.id == session.zoneId }
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

    fun generateGoogleSheetsCsv(session: MentoringSession, areas: List<Area>, zones: List<Zone>): String {
        val area = areas.find { it.id == session.areaId }
        val zone = zones.find { it.id == session.zoneId }
        val sb = StringBuilder()
        sb.append("Timestamp,Session ID,Group,Area,Zone,Mentor,Object Code,Object Name,Fungsi,Frekuensi Penggunaan,Terakhir Digunakan,Penilaian Awal,Keputusan Seiri,Alasan Keputusan,Rencana Tindakan,PIC,Tenggat Waktu,Status Sesi\n")
        session.inspections.forEach { item ->
            sb.append("\"${session.sessionDate}\",\"${session.id}\",\"${session.groupName}\",\"${area?.areaName}\",\"${zone?.zoneName}\",\"${session.mentorName}\",\"${item.objectCode}\",\"${item.objectName}\",\"${item.function}\",\"${item.usageFrequency}\",\"${item.lastUsed}\",\"${item.initialAssessment}\",\"${item.seiriDecision}\",\"${item.decisionReason}\",\"${item.actionDescription}\",\"${item.pic}\",\"${item.dueDate}\",\"${session.status.name}\"\n")
        }
        return sb.toString()
    }

    fun generateObjectsCsv(
        areaIdFilter: String?,
        areas: List<Area>,
        zones: List<Zone>,
        objects: List<SeiriObject>
    ): String {
        val filtered = if (areaIdFilter != null) {
            objects.filter { it.areaId == areaIdFilter }
        } else {
            objects
        }

        val sb = StringBuilder()
        sb.append("Kode Objek,Nama Objek,Area,Zona,Kategori,Kondisi,PIC,Frekuensi,Status Validasi,Status Aset,Terakhir Digunakan\n")
        for (o in filtered) {
            sb.append("\"${o.objectCode}\",")
            sb.append("\"${o.name}\",")
            sb.append("\"${areas.find { it.id == o.areaId }?.areaName ?: "Unknown Area"}\",")
            sb.append("\"${zones.find { it.id == o.zoneId }?.zoneName ?: "Unknown Zone"}\",")
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
}
