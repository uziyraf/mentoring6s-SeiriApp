package com.example.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class MentoringReport(
    val id: String,
    val sessionId: String,
    val mentorName: String,
    // Hirarki lokasi
    val plant: String = "",
    val areaId: String = "",
    val areaName: String = "",
    val pic: String = "",
    val zoneId: String = "",
    val zoneName: String = "",
    // Isi laporan
    val audience: String,
    val activityDescription: String,
    val result: String,
    val obstacles: String,
    val createdAt: String,
    val mentorId: String = ""
)

object MentoringReportRepository {
    private val _reports = MutableStateFlow<List<MentoringReport>>(emptyList())
    val reports: StateFlow<List<MentoringReport>> = _reports.asStateFlow()

    fun createReport(report: MentoringReport) {
        _reports.update { it + report }
    }

    fun updateReport(report: MentoringReport) {
        _reports.update { list -> list.map { if (it.id == report.id) report else it } }
    }

    fun deleteReport(id: String) {
        _reports.update { list -> list.filterNot { it.id == id } }
    }

    fun getReportsByMentor(mentorId: String): List<MentoringReport> =
        _reports.value.filter { it.mentorId == mentorId }
}
