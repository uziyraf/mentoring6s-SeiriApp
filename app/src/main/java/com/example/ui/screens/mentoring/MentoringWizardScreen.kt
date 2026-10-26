package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SeiriRepository
import com.example.data.service.ExportService
import com.example.model.*
import com.example.ui.screens.mentoring.AddNewFieldObjectDialog
import com.example.ui.screens.mentoring.SessionSetupAndDraftPicker
import com.example.ui.screens.mentoring.Stage1ObjectIdentification
import com.example.ui.screens.mentoring.Stage2NeedsAssessment
import com.example.ui.screens.mentoring.Stage3SeiriDecisions
import com.example.ui.screens.mentoring.Stage4ActionPlans
import com.example.ui.screens.mentoring.Stage5ReviewAndFinalSubmit
import com.example.ui.theme.*


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MentoringWizardScreen(
    repository: SeiriRepository,
    onNavigateBack: () -> Unit,
    onNavigateToTransfers: (String) -> Unit
) {
    val context = LocalContext.current
    val currentUser by repository.currentUser.collectAsState()
    val allObjects by repository.objects.collectAsState()
    val areas by repository.areas.collectAsState()
    val zones by repository.zones.collectAsState()
    val sessions by repository.sessions.collectAsState()

    // Multi-session active session
    var activeSession by remember { mutableStateOf<MentoringSession?>(null) }
    var currentStage by remember { mutableStateOf(1) } // 1: Identifikasi, 2: Kebutuhan, 3: Keputusan, 4: PIC Tindakan, 5: Review Final

    // Selection state for creating / configuring a session
    val initialAreaId = currentUser.assignedAreaId ?: areas.first().id
    val initialArea = areas.find { it.id == initialAreaId }
    var selectedGroup by remember { mutableStateOf<String?>(initialArea?.group ?: "PLANT 1") }
    var selectedAreaId by remember { mutableStateOf(initialAreaId) }
    var selectedZoneId by remember {
        mutableStateOf(zones.firstOrNull { it.areaId == initialAreaId }?.id ?: "")
    }
    var selectedObjectId by remember { mutableStateOf<String?>(null) }
    var mentorNameInput by remember { mutableStateOf(currentUser.name) }
    var sessionDateInput by remember { mutableStateOf("2026-10-19") }
    var sessionNotesInput by remember { mutableStateOf("Mentoring rutin Seiri 6S") }

    // Dialog state for adding missing object at Stage 1
    var showAddNewObjectDialog by remember { mutableStateOf(false) }

    // Dialog state for viewing JSON / Google Sheets Schema
    var showSchemaDialog by remember { mutableStateOf(false) }

    // Toast feedback message
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    // Auto-detect draft session if any for current mentor
    val activeDraftSessions = sessions.filter {
        it.mentorId == currentUser.id && it.status == SessionStatus.DRAFT
    }

    LaunchedEffect(currentUser) {
        mentorNameInput = currentUser.name
        if (currentUser.assignedAreaId != null) {
            val assigned = currentUser.assignedAreaId ?: areas.first().id
            selectedAreaId = assigned
            val area = areas.find { it.id == assigned }
            if (area != null) selectedGroup = area.group
            selectedZoneId = zones.firstOrNull { it.areaId == selectedAreaId }?.id ?: ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Stage Progress Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // --- HEADER LOKASI & JUDUL ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Mentoring 6S: Seiri (Pemilahan)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        
                        val area = areas.find { it.id == selectedAreaId }
                        val zone = zones.find { it.id == selectedZoneId }
                        val locationText = "${area?.group ?: "PLANT 1"} â€¢ ${area?.areaName ?: "Area"} â€¢ ${zone?.zoneName ?: "Pilih Zona"}"
                        
                        Text(
                            text = locationText,
                            fontSize = 10.sp,
                            color = SecondarySlate,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // --- 5-STAGE STEPPER INDICATOR ---
                val stageTitles = listOf("Objek", "Kebutuhan", "Keputusan", "PIC Tindakan", "Review")
                
                // 1. Baris Garis Indikator Progress
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    stageTitles.forEachIndexed { idx, _ ->
                        val stepNum = idx + 1
                        val isDone = stepNum < currentStage
                        val isCurrent = stepNum == currentStage
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    when {
                                        isCurrent -> PrimaryBlue
                                        isDone -> StatusGreen
                                        else -> OutlineLight.copy(alpha = 0.4f)
                                    }
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 2. Teks Tahapan Aktif (Ringkas & Anti-Bertumpuk)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tahap $currentStage dari 5: ${stageTitles.getOrNull(currentStage - 1) ?: ""}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryBlue
                    )
                    Text(
                        text = "${(currentStage * 100) / 5}%",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = SecondarySlate
                    )
                }
            }
        }

        // CONTENT BY ACTIVE SESSION / STAGE
        Box(modifier = Modifier.weight(1f)) {
            if (activeSession == null) {
                // SESSION SETUP & MULTI-SESSION DRAFT PICKER
                SessionSetupAndDraftPicker(
                    currentUser = currentUser,
                    areas = areas,
                    zones = zones,
                    allObjects = allObjects,
                    selectedGroup = selectedGroup,
                    selectedAreaId = selectedAreaId,
                    selectedZoneId = selectedZoneId,
                    selectedObjectId = selectedObjectId,
                    mentorNameInput = mentorNameInput,
                    sessionDateInput = sessionDateInput,
                    sessionNotesInput = sessionNotesInput,
                    activeDrafts = activeDraftSessions,
                    onGroupSelected = { newGroup ->
                        selectedGroup = newGroup
                        if (newGroup != null) {
                            val matchingAreas = areas.filter { it.group == newGroup }
                            if (matchingAreas.none { it.id == selectedAreaId }) {
                                val firstA = matchingAreas.firstOrNull()
                                if (firstA != null) {
                                    selectedAreaId = firstA.id
                                    selectedZoneId = zones.firstOrNull { it.areaId == firstA.id }?.id ?: ""
                                    selectedObjectId = null
                                }
                            }
                        }
                    },
                    onAreaSelected = { newAreaId ->
                        selectedAreaId = newAreaId
                        val area = areas.find { it.id == newAreaId }
                        if (area != null) selectedGroup = area.group
                        selectedZoneId = zones.firstOrNull { z -> z.areaId == newAreaId }?.id ?: ""
                        selectedObjectId = null
                    },
                    onZoneSelected = { newZoneId ->
                        selectedZoneId = newZoneId
                        val zone = zones.find { it.id == newZoneId }
                        if (zone != null) {
                            selectedAreaId = zone.areaId
                            val area = areas.find { it.id == zone.areaId }
                            if (area != null) selectedGroup = area.group
                        }
                        selectedObjectId = null
                    },
                    onObjectSelected = { newObjId ->
                        selectedObjectId = newObjId
                        if (newObjId != null) {
                            val obj = allObjects.find { it.id == newObjId }
                            if (obj != null) {
                                selectedZoneId = obj.zoneId
                                selectedAreaId = obj.areaId
                                val area = areas.find { it.id == obj.areaId }
                                if (area != null) selectedGroup = area.group
                            }
                        }
                    },
                    onMentorNameChange = { mentorNameInput = it },
                    onDateChange = { sessionDateInput = it },
                    onNotesChange = { sessionNotesInput = it },
                    onResumeDraft = { draft ->
                        activeSession = draft
                        currentStage = draft.currentStage
                        selectedAreaId = draft.areaId
                        selectedZoneId = draft.zoneId
                        val area = areas.find { it.id == draft.areaId }
                        if (area != null) selectedGroup = area.group
                        feedbackMessage = "Melanjutkan draft sesi di Tahap $currentStage."
                    },
                    onStartNew = {
                        val area = areas.find { it.id == selectedAreaId }
                        val newSession = repository.createOrResumeSession(
                            mentorId = currentUser.id,
                            mentorName = mentorNameInput,
                            groupName = area?.group ?: selectedGroup ?: "PLANT 1",
                            areaId = selectedAreaId,
                            zoneId = selectedZoneId,
                            sessionDate = sessionDateInput,
                            notes = sessionNotesInput
                        )
                        activeSession = newSession
                        currentStage = 1
                    }
                )
            } else {
                val session = activeSession!!
                when (currentStage) {
                    1 -> {
                        // TAHAP 1  IDENTIFIKASI OBJEK
                        Stage1ObjectIdentification(
                            session = session,
                            repository = repository,
                            onAddNewObject = { showAddNewObjectDialog = true },
                            onNext = {
                                val updated = session.copy(currentStage = 2)
                                repository.saveSessionDraft(updated)
                                activeSession = updated
                                currentStage = 2
                            },
                            onSaveDraftAndExit = {
                                repository.saveSessionDraft(session.copy(currentStage = 1))
                                feedbackMessage = "Draft disimpan. Anda dapat melanjutkan kapan saja."
                                onNavigateBack()
                            }
                        )
                    }

                    2 -> {
                        // TAHAP 2  PERIKSA KEBUTUHAN (DIISI PER OBJEK)
                        Stage2NeedsAssessment(
                            session = session,
                            onUpdateInspection = { updatedList ->
                                val updated = session.copy(inspections = updatedList, currentStage = 2)
                                repository.saveSessionDraft(updated)
                                activeSession = updated
                            },
                            onNext = {
                                val updated = session.copy(currentStage = 3)
                                repository.saveSessionDraft(updated)
                                activeSession = updated
                                currentStage = 3
                            },
                            onSaveDraftAndExit = {
                                val updated = session.copy(currentStage = 2)
                                repository.saveSessionDraft(updated)
                                feedbackMessage = "Progress Tahap 2 berhasil disimpan sebagai Draft!"
                                onNavigateBack()
                            },
                            onPrev = { currentStage = 1 }
                        )
                    }

                    3 -> {
                        // TAHAP 3  KEPUTUSAN SEIRI
                        Stage3SeiriDecisions(
                            session = session,
                            onUpdateInspection = { updatedList ->
                                val updated = session.copy(inspections = updatedList, currentStage = 3)
                                repository.saveSessionDraft(updated)
                                activeSession = updated
                            },
                            onNext = {
                                val updated = session.copy(currentStage = 4)
                                repository.saveSessionDraft(updated)
                                activeSession = updated
                                currentStage = 4
                            },
                            onSaveDraftAndExit = {
                                val updated = session.copy(currentStage = 3)
                                repository.saveSessionDraft(updated)
                                feedbackMessage = "Progress Tahap 3 berhasil disimpan sebagai Draft."
                                onNavigateBack()
                            },
                            onPrev = { currentStage = 2 }
                        )
                    }

                    4 -> {
                        // TAHAP 4  PIC DAN TINDAKAN (KHUSUS RED TAG / DIPINDAHKAN)
                        Stage4ActionPlans(
                            session = session,
                            onUpdateInspection = { updatedList ->
                                val updated = session.copy(inspections = updatedList, currentStage = 4)
                                repository.saveSessionDraft(updated)
                                activeSession = updated
                            },
                            onNext = {
                                val updated = session.copy(currentStage = 5)
                                repository.saveSessionDraft(updated)
                                activeSession = updated
                                currentStage = 5
                            },
                            onSaveDraftAndExit = {
                                val updated = session.copy(currentStage = 4)
                                repository.saveSessionDraft(updated)
                                feedbackMessage = "Progress Tahap 4 berhasil disimpan sebagai Draft."
                                onNavigateBack()
                            },
                            onPrev = { currentStage = 3 }
                        )
                    }

                    5 -> {
                        // TAHAP 5 REVIEW & SIMPAN FINAL
                        Stage5ReviewAndFinalSubmit(
                            session = session,
                            repository = repository,
                            onShowSchema = { showSchemaDialog = true },
                            onSubmitFinal = {
                                repository.submitFinalSession(session)
                                feedbackMessage = "Sukses! Sesi mentoring Seiri resmi diselesaikan & dikunci."
                                onNavigateBack()
                            },
                            onSaveDraftAndExit = {
                                val updated = session.copy(currentStage = 5)
                                repository.saveSessionDraft(updated)
                                feedbackMessage = "Draft tersimpan."
                                onNavigateBack()
                            },
                            onBackToStage = { stage -> currentStage = stage }
                        )
                    }
                }
            }
        }
    }

    // DIALOG: ADD NEW OBJECT IN FIELD (+ Objek Baru)
    if (showAddNewObjectDialog && activeSession != null) {
        val session = activeSession!!
        AddNewFieldObjectDialog(
            session = session,
            repository = repository,
            currentUser = currentUser,
            onDismiss = { showAddNewObjectDialog = false },
            onAdded = { newObj ->
                // Add new inspection item
                val newInspection = SessionObjectInspection(
                    objectId = newObj.id,
                    objectCode = newObj.objectCode,
                    objectName = newObj.name,
                    function = newObj.function,
                    usageFrequency = "Setiap Hari/Shift",
                    lastUsed = "Hari ini",
                    initialAssessment = "Masih Diperlukan",
                    seiriDecision = "Diperlukan (Hijau)",
                    decisionReason = "Objek baru ditemukan di lapangan",
                    pic = newObj.pic
                )
                val updatedList = session.inspections + listOf(newInspection)
                val updatedSession = session.copy(inspections = updatedList)
                repository.saveSessionDraft(updatedSession)
                activeSession = updatedSession
                showAddNewObjectDialog = false
                feedbackMessage = "Objek '${newObj.name}' berhasil ditambahkan ke sesi ini."
            }
        )
    }

    // DIALOG: DATA SCHEMA / JSON / GOOGLE SHEETS
    if (showSchemaDialog && activeSession != null) {
        val session = activeSession!!
        val jsonText = ExportService.generateSessionJson(session, areas, zones)
        val sheetsCsv = ExportService.generateGoogleSheetsCsv(session, areas, zones)
        var tabIndex by remember { mutableStateOf(0) }

        AlertDialog(
            onDismissRequest = { showSchemaDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DataObject, contentDescription = null, tint = PrimaryBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Struktur Data Sesi & Objek", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    TabRow(selectedTabIndex = tabIndex) {
                        Tab(
                            selected = tabIndex == 0,
                            onClick = { tabIndex = 0 },
                            text = { Text("Format JSON", fontSize = 12.sp) }
                        )
                        Tab(
                            selected = tabIndex == 1,
                            onClick = { tabIndex = 1 },
                            text = { Text("Skema Google Sheets", fontSize = 12.sp) }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        color = IndustrialNavy,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                    ) {
                        LazyColumn(modifier = Modifier.padding(10.dp)) {
                            item {
                                Text(
                                    text = if (tabIndex == 0) jsonText else sheetsCsv,
                                    color = Color(0xFF67E8F9),
                                    fontSize = 11.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val textToCopy = if (tabIndex == 0) jsonText else sheetsCsv
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Seiri 6S Data", textToCopy)
                        clipboard.setPrimaryClip(clip)
                        feedbackMessage = "Data skema berhasil disalin ke clipboard!"
                        showSchemaDialog = false
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Salin Data")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSchemaDialog = false }) {
                    Text("Tutup")
                }
            }
        )
    }

    // Feedback Toast / SnackBar
    feedbackMessage?.let { msg ->
        LaunchedEffect(msg) {
            kotlinx.coroutines.delay(3000)
            feedbackMessage = null
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 70.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                color = IndustrialNavy,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = msg,
                    color = Color.White,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    }
}




