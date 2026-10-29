package com.example.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

data class MentoringReport(
    val id: String = "",
    val sessionId: String = "",
    val mentorId: String = "",
    val mentorName: String = "",
    val plant: String = "",
    val areaId: String = "",
    val areaName: String = "",
    val pic: String = "",
    val zoneId: String = "",
    val zoneName: String = "",
    val audience: String = "",
    val activityDescription: String = "",
    val result: String = "",
    val obstacles: String = "",
    val createdAt: String = ""
)

data class AreaMentoringGroup(
    val areaId: String,
    val areaName: String,
    val zoneId: String,
    val zoneName: String,
    val reports: List<MentoringReport>
) {
    val lastVisited: String get() = reports.maxByOrNull { it.createdAt }?.createdAt ?: "-"
    val totalVisits: Int get() = reports.size
}


object MentoringReportRepository {
    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e("ReportRepo", "Firebase init error: ${e.message}")
            null
        }
    }

    private var reportsListener: ListenerRegistration? = null

    private val _reports = MutableStateFlow<List<MentoringReport>>(emptyList())
    val reports: StateFlow<List<MentoringReport>> = _reports.asStateFlow()

    // 1. Fungsi untuk mendengarkan data real-time dari Firebase
    fun initSync() {
        val db = firestore ?: return
        reportsListener?.remove()
        reportsListener = db.collection("mentoring_reports")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ReportRepo", "Listen error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(MentoringReport::class.java)?.copy(id = doc.id)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    // Urutkan dari yang terbaru
                    _reports.value = list.sortedByDescending { it.createdAt }
                }
            }
    }

    // 2. Fungsi Create
    fun createReport(report: MentoringReport) {
        _reports.update { listOf(report) + it } // Update local state dulu (Instant UI)

        firestore?.collection("mentoring_reports")?.document(report.id)?.set(report)
            ?.addOnFailureListener { e ->
                Log.e("ReportRepo", "Gagal simpan ke Firestore: ${e.message}")
            }
    }

    // 3. Fungsi Update
    fun updateReport(report: MentoringReport) {
        _reports.update { list -> list.map { if (it.id == report.id) report else it } }

        firestore?.collection("mentoring_reports")?.document(report.id)?.set(report)
            ?.addOnFailureListener { e ->
                Log.e("ReportRepo", "Gagal update Firestore: ${e.message}")
            }
    }

    // 4. Fungsi Delete
    fun deleteReport(id: String) {
        _reports.update { list -> list.filterNot { it.id == id } }

        firestore?.collection("mentoring_reports")?.document(id)?.delete()
            ?.addOnFailureListener { e ->
                Log.e("ReportRepo", "Gagal hapus dari Firestore: ${e.message}")
            }
    }

    fun getReportsByMentor(mentorId: String): List<MentoringReport> =
        _reports.value.filter { it.mentorId == mentorId }
}