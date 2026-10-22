package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SeiriRepository
import com.example.data.service.ExportService
import com.example.data.service.ImportCsvService
import com.example.model.*
import com.example.ui.components.*
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
                        // TAHAP 1 â€” IDENTIFIKASI OBJEK
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
                        // TAHAP 2 â€” PERIKSA KEBUTUHAN (DIISI PER OBJEK)
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
                        // TAHAP 3 â€” KEPUTUSAN SEIRI
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
                        // TAHAP 4 â€” PIC DAN TINDAKAN (KHUSUS RED TAG / DIPINDAHKAN)
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
                        // TAHAP 5 â€” REVIEW & SIMPAN FINAL
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

// -------------------------------------------------------------------------------------
// HEADER / MULTI-SESSION SETUP & DRAFT RESUME
// -------------------------------------------------------------------------------------
@Composable
private fun SessionSetupAndDraftPicker(
    currentUser: User,
    areas: List<Area>,
    zones: List<Zone>,
    allObjects: List<SeiriObject>,
    selectedGroup: String?,
    selectedAreaId: String,
    selectedZoneId: String,
    selectedObjectId: String?,
    mentorNameInput: String,
    sessionDateInput: String,
    sessionNotesInput: String,
    activeDrafts: List<MentoringSession>,
    onGroupSelected: (String?) -> Unit,
    onAreaSelected: (String) -> Unit,
    onZoneSelected: (String) -> Unit,
    onObjectSelected: (String?) -> Unit,
    onMentorNameChange: (String) -> Unit,
    onDateChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onResumeDraft: (MentoringSession) -> Unit,
    onStartNew: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    // Real-time suggestions for searching Area Name or Object Name
    val searchAreaMatches = remember(searchQuery, areas) {
        if (searchQuery.isBlank()) emptyList()
        else areas.filter {
            it.areaName.contains(searchQuery, ignoreCase = true) ||
            it.areaCode.contains(searchQuery, ignoreCase = true) ||
            it.group.contains(searchQuery, ignoreCase = true)
        }.take(3)
    }

    val searchObjectMatches = remember(searchQuery, allObjects) {
        if (searchQuery.isBlank()) emptyList()
        else allObjects.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.objectCode.contains(searchQuery, ignoreCase = true) ||
            it.function.contains(searchQuery, ignoreCase = true)
        }.take(4)
    }

    val objectsInSelectedZone = remember(allObjects, selectedZoneId) {
        allObjects.filter { it.zoneId == selectedZoneId && it.isActive }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Multi-session Active Drafts Banner
        if (activeDrafts.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = StatusAmberContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.HistoryEdu, contentDescription = null, tint = StatusAmber, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Draf Sesi Mentoring Tersedia (${activeDrafts.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF78350F)
                            )
                        }
                        Text(
                            text = "Anda memiliki sesi yang disimpan di Tahap 2 atau tahap lain. Klik untuk melanjutkan hari ini:",
                            fontSize = 12.sp,
                            color = Color(0xFF92400E)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        activeDrafts.forEach { draft ->
                            Surface(
                                color = Color.White,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${draft.groupName} â€¢ ${draft.sessionDate}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = IndustrialNavy
                                        )
                                        Text(
                                            text = "Tersimpan di Tahap ${draft.currentStage} â€¢ ${draft.inspections.size} Objek",
                                            fontSize = 11.sp,
                                            color = PrimaryBlueDark
                                        )
                                    }
                                    Button(
                                        onClick = { onResumeDraft(draft) },
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Lanjutkan", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 1. Search Bar: Cari Nama Area atau Nama/Kode Objek
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Pencarian Cepat Area / Objek",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = IndustrialNavy
                    )
                    Text(
                        text = "Ketik nama area atau objek untuk langsung memilih hirarki terkait",
                        fontSize = 11.sp,
                        color = SecondarySlate
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    AreaOrObjectSearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        placeholder = "Cari nama area atau objek (cth: Preform, Hopper)...",
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Autocomplete suggestion matches
                    if (searchQuery.isNotBlank() && (searchAreaMatches.isNotEmpty() || searchObjectMatches.isNotEmpty())) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = SurfaceVariantLight,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "Hasil Pencarian Langsung (Klik untuk pilih):",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryBlueDark
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                // Area Matches
                                searchAreaMatches.forEach { a ->
                                    Surface(
                                        color = Color.White,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp)
                                            .clickable {
                                                onGroupSelected(a.group)
                                                onAreaSelected(a.id)
                                                val firstZone = zones.firstOrNull { it.areaId == a.id }
                                                if (firstZone != null) onZoneSelected(firstZone.id)
                                                searchQuery = ""
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Domain, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column {
                                                Text(text = "[AREA] ${a.areaName}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = IndustrialNavy)
                                                Text(text = "${a.group} â€¢ Kode: ${a.areaCode}", fontSize = 10.sp, color = SecondarySlate)
                                            }
                                        }
                                    }
                                }

                                // Object Matches
                                searchObjectMatches.forEach { obj ->
                                    val objArea = areas.find { it.id == obj.areaId }
                                    val objZone = zones.find { it.id == obj.zoneId }
                                    Surface(
                                        color = Color.White,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp)
                                            .clickable {
                                                if (objArea != null) onGroupSelected(objArea.group)
                                                onAreaSelected(obj.areaId)
                                                onZoneSelected(obj.zoneId)
                                                onObjectSelected(obj.id)
                                                searchQuery = ""
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Inventory2, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column {
                                                Text(text = "[OBJEK] ${obj.name} (${obj.objectCode})", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = IndustrialNavy)
                                                Text(text = "${objArea?.group} > ${objArea?.areaName} > ${objZone?.zoneName}", fontSize = 10.sp, color = SecondarySlate)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Dropdown Menu dari Hirarki Paling Atas (GROUP -> AREA -> ZONA -> OBJEK)
        item {
            HierarchyCascadingFilterCard(
                allAreas = areas,
                allZones = zones,
                allObjects = allObjects,
                selectedGroup = selectedGroup,
                selectedAreaId = selectedAreaId,
                selectedZoneId = selectedZoneId,
                selectedObjectId = selectedObjectId,
                onGroupSelected = { newGroup ->
                    onGroupSelected(newGroup)
                },
                onAreaSelected = { newAreaId ->
                    if (newAreaId != null) {
                        onAreaSelected(newAreaId)
                    }
                },
                onZoneSelected = { newZoneId ->
                    if (newZoneId != null) {
                        onZoneSelected(newZoneId)
                    }
                },
                onObjectSelected = { newObjId ->
                    onObjectSelected(newObjId)
                },
                onResetFilter = {
                    val defaultArea = currentUser.assignedAreaId?.let { id -> areas.find { it.id == id } } ?: areas.first()
                    onGroupSelected(defaultArea.group)
                    onAreaSelected(defaultArea.id)
                    val firstZ = zones.firstOrNull { it.areaId == defaultArea.id }
                    if (firstZ != null) onZoneSelected(firstZ.id)
                    onObjectSelected(null)
                    searchQuery = ""
                },
                title = "Pilih Lokasi & Objek (Hirarki 4-Tingkat)",
                showObjectFilter = true
            )
        }

        // 3. New Session Details Form (Mentor, Tanggal, Catatan)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Data Sesi & Mentor 6S",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Target Location Info Pill
                    val currentArea = areas.find { it.id == selectedAreaId }
                    val currentZone = zones.find { it.id == selectedZoneId }
                    Surface(
                        color = PrimaryBlueContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Target: ${currentArea?.group ?: "PLANT 1"} > ${currentArea?.areaName ?: "Area"} > ${currentZone?.zoneName ?: "Pilih Zona"}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = PrimaryBlueDark
                                )
                                Text(
                                    text = "${objectsInSelectedZone.size} Objek terdata siap diperiksa",
                                    fontSize = 11.sp,
                                    color = SecondarySlate
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = mentorNameInput,
                        onValueChange = onMentorNameChange,
                        label = { Text("Nama Mentor 6S *") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = SecondarySlate, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = sessionDateInput,
                        onValueChange = onDateChange,
                        label = { Text("Tanggal Pelaksanaan (YYYY-MM-DD) *") },
                        leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = SecondarySlate, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = sessionNotesInput,
                        onValueChange = onNotesChange,
                        label = { Text("Audiens mentoring") },
                        leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null, tint = SecondarySlate, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        item {
            Button(
                onClick = onStartNew,
                enabled = selectedZoneId.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("start_mentoring_flow_button")
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Mulai Tahap 1 â€” Identifikasi Objek", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// TAHAP 1 â€” IDENTIFIKASI OBJEK
// -------------------------------------------------------------------------------------
@Composable
private fun Stage1ObjectIdentification(
    session: MentoringSession,
    repository: SeiriRepository,
    onAddNewObject: () -> Unit,
    onNext: () -> Unit,
    onSaveDraftAndExit: () -> Unit
) {
    val inspections = session.inspections
    var searchQuery by remember { mutableStateOf("") }
    var filterMode by remember { mutableStateOf("ALL") } // ALL, UNCHECKED, CHECKED

    // Search filter across area name or object name/code/function
    val filteredInspections = inspections.filter { item ->
        val areaName = repository.getAreaName(session.areaId)
        val zoneName = repository.getZoneName(session.zoneId)
        val matchesSearch = searchQuery.isBlank() ||
                item.objectName.contains(searchQuery, ignoreCase = true) ||
                item.objectCode.contains(searchQuery, ignoreCase = true) ||
                item.function.contains(searchQuery, ignoreCase = true) ||
                item.pic.contains(searchQuery, ignoreCase = true) ||
                areaName.contains(searchQuery, ignoreCase = true) ||
                zoneName.contains(searchQuery, ignoreCase = true)

        val matchesFilter = when (filterMode) {
            "UNCHECKED" -> !item.isStage2Checked
            "CHECKED" -> item.isStage2Checked
            else -> true
        }

        matchesSearch && matchesFilter
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Sticky Header
        
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            )
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                // Header tahap dan tombol tambah
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Tahap 1 â€” Identifikasi Objek",
                            fontSize = 14.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = "${inspections.size} objek terdata",
                            fontSize = 11.sp,
                            color = SecondarySlate,
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Tombol tambah lebih kecil dan proporsional
                    FilledTonalButton(
                        onClick = onAddNewObject,
                        modifier = Modifier.height(34.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(
                            horizontal = 10.dp,
                            vertical = 0.dp
                        ),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = PrimaryBlue,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Tambah objek",
                            modifier = Modifier.size(15.dp)
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        Text(
                            text = "Tambah",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Kolom pencarian
                AreaOrObjectSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = "Cari area atau objek...",
                    modifier = Modifier.fillMaxWidth()
                )

                // Filter status
                val filters = listOf(
                    Triple(
                        "ALL",
                        "Semua",
                        inspections.size
                    ),
                    Triple(
                        "UNCHECKED",
                        "Belum",
                        inspections.count { !it.isStage2Checked }
                    ),
                    Triple(
                        "CHECKED",
                        "Sudah",
                        inspections.count { it.isStage2Checked }
                    )
                )

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(
                        items = filters,
                        key = { it.first }
                    ) { (key, label, count) ->

                        val selected = filterMode == key

                        Surface(
                            onClick = { filterMode = key },
                            shape = RoundedCornerShape(7.dp),
                            color = if (selected) {
                                PrimaryBlue
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(
                                    alpha = 0.65f
                                )
                            },
                            border = if (selected) null else BorderStroke(
                                0.5.dp,
                                MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = if (selected) {
                                        FontWeight.SemiBold
                                    } else {
                                        FontWeight.Medium
                                    },
                                    color = if (selected) {
                                        Color.White
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    maxLines = 1
                                )

                                // Jumlah objek dalam badge kecil
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = if (selected) {
                                                Color.White.copy(alpha = 0.2f)
                                            } else {
                                                MaterialTheme.colorScheme.outlineVariant.copy(
                                                    alpha = 0.5f
                                                )
                                            },
                                            shape = CircleShape
                                        )
                                        .padding(
                                            horizontal = 5.dp,
                                            vertical = 2.dp
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = count.toString(),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) {
                                            Color.White
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }



        // List of Objects
        if (filteredInspections.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.SearchOff, contentDescription = null, tint = SecondarySlateLight, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Tidak Ada Objek yang Cocok", fontWeight = FontWeight.Bold, color = SecondarySlate)
                    Text("Coba ubah kata kunci pencarian atau reset filter di atas.", fontSize = 12.sp, color = SecondarySlateLight)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(filteredInspections) { idx, item ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = PrimaryBlueContainer,
                                shape = CircleShape,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${idx + 1}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = PrimaryBlueDark
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = SurfaceVariantLight,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = item.objectCode,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = IndustrialNavy,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    if (item.isStage2Checked) {
                                        Surface(color = StatusGreenContainer, shape = RoundedCornerShape(4.dp)) {
                                            Text("Sudah Diperiksa", color = StatusGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = item.objectName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (item.function.isNotBlank()) {
                                    Text(text = "Fungsi: ${item.function}", fontSize = 11.sp, color = SecondarySlate)
                                }
                            }

                            Icon(Icons.Default.CheckCircleOutline, contentDescription = null, tint = if (item.isStage2Checked) StatusGreen else SecondarySlateLight)
                        }
                    }
                }
            }
        }

        // Bottom Action Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onSaveDraftAndExit,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Simpan Draft & Keluar", fontSize = 12.sp)
                }

                Button(
                    onClick = onNext,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1.5f)
                ) {
                    Text("Lanjut: Tahap 2 (Kebutuhan)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// TAHAP 2 â€” PERIKSA KEBUTUHAN (DIISI PER OBJEK)
// -------------------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Stage2NeedsAssessment(
    session: MentoringSession,
    onUpdateInspection: (List<SessionObjectInspection>) -> Unit,
    onNext: () -> Unit,
    onSaveDraftAndExit: () -> Unit,
    onPrev: () -> Unit
) {
    val inspections = session.inspections
    var selectedIndex by remember { mutableStateOf(0) }
    val currentItem = inspections.getOrNull(selectedIndex)

    Column(modifier = Modifier.fillMaxSize()) {
        // Sticky Header: Tabs of objects
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tahap 2 â€” Periksa Kebutuhan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Objek ${selectedIndex + 1} dari ${inspections.size}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryBlue
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Object quick tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    inspections.forEachIndexed { idx, item ->
                        FilterChip(
                            selected = selectedIndex == idx,
                            onClick = { selectedIndex = idx },
                            label = { Text(item.objectName.take(16)) },
                            leadingIcon = {
                                if (item.isStage2Checked) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp), tint = StatusGreen)
                                }
                            }
                        )
                    }
                }
            }
        }

        // Form for current object
        if (currentItem != null) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Object identification banner
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = PrimaryBlueContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Surface(color = Color.White, shape = RoundedCornerShape(4.dp)) {
                                Text(
                                    text = currentItem.objectCode,
                                    color = PrimaryBlueDark,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = currentItem.objectName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlueDark
                            )
                        }
                    }
                }

                // 1. Input Fungsi Objek
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "1. Fungsi Objek *", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(text = "Jelaskan peran objek ini dalam alur kerja produksi", fontSize = 11.sp, color = SecondarySlate)
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = currentItem.function,
                                onValueChange = { newFunc ->
                                    val updated = inspections.toMutableList()
                                    updated[selectedIndex] = currentItem.copy(function = newFunc, isStage2Checked = true)
                                    onUpdateInspection(updated)
                                },
                                placeholder = { Text("Contoh: Menampung dan mengalirkan resin PET ke barrel") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // 2. Dropdown Frekuensi Penggunaan
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "2. Frekuensi Penggunaan *", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(text = "Seberapa sering objek ini digunakan oleh operator", fontSize = 11.sp, color = SecondarySlate)
                            Spacer(modifier = Modifier.height(8.dp))

                            val frequencies = listOf("Setiap Hari/Shift", "Mingguan/Bulanan", "Jarang/Tidak Pernah")
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                frequencies.forEach { freq ->
                                    FilterChip(
                                        selected = currentItem.usageFrequency == freq,
                                        onClick = {
                                            val updated = inspections.toMutableList()
                                            updated[selectedIndex] = currentItem.copy(usageFrequency = freq, isStage2Checked = true)
                                            onUpdateInspection(updated)
                                        },
                                        label = { Text(freq) }
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Input Kapan Terakhir Digunakan
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "3. Kapan Terakhir Digunakan *", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = currentItem.lastUsed,
                                onValueChange = { newLast ->
                                    val updated = inspections.toMutableList()
                                    updated[selectedIndex] = currentItem.copy(lastUsed = newLast, isStage2Checked = true)
                                    onUpdateInspection(updated)
                                },
                                placeholder = { Text("Contoh: Hari ini, 3 hari lalu, atau YYYY-MM-DD") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // 4. Radio Button Penilaian Awal
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "4. Penilaian Awal Kebutuhan *", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(8.dp))

                            val assessmentOptions = listOf("Masih Diperlukan", "Ragu-ragu / Tidak Diperlukan")
                            assessmentOptions.forEach { opt ->
                                Surface(
                                    color = if (currentItem.initialAssessment == opt) PrimaryBlueContainer else SurfaceVariantLight,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            val updated = inspections.toMutableList()
                                            val decision = if (opt == "Masih Diperlukan") "Diperlukan (Hijau)" else "Tidak Diperlukan / Red Tag (Merah)"
                                            updated[selectedIndex] = currentItem.copy(
                                                initialAssessment = opt,
                                                seiriDecision = decision,
                                                isStage2Checked = true
                                            )
                                            onUpdateInspection(updated)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = currentItem.initialAssessment == opt,
                                            onClick = {
                                                val updated = inspections.toMutableList()
                                                val decision = if (opt == "Masih Diperlukan") "Diperlukan (Hijau)" else "Tidak Diperlukan / Red Tag (Merah)"
                                                updated[selectedIndex] = currentItem.copy(
                                                    initialAssessment = opt,
                                                    seiriDecision = decision,
                                                    isStage2Checked = true
                                                )
                                                onUpdateInspection(updated)
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = opt, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. Upload / Ambil Foto Objek Saat Ini
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "5. Foto Fisik Objek Saat Ini (Kamera/Upload)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(8.dp))

                            Surface(
                                color = SurfaceVariantLight,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                                    .clickable {
                                        // Toggle mock photo capture
                                        val updated = inspections.toMutableList()
                                        val newPhoto = if (currentItem.photoUri == null) "foto_${currentItem.objectCode}.jpg" else null
                                        updated[selectedIndex] = currentItem.copy(photoUri = newPhoto, isStage2Checked = true)
                                        onUpdateInspection(updated)
                                    }
                            ) {
                                if (currentItem.photoUri != null) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Image, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(36.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(text = "Foto Objek Tersimpan", fontWeight = FontWeight.Bold, color = StatusGreen)
                                            Text(text = "${currentItem.photoUri} â€¢ Klik untuk hapus/ganti", fontSize = 11.sp, color = SecondarySlate)
                                        }
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(28.dp))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = "Ketuk untuk Ambil Foto / Upload", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PrimaryBlueDark)
                                        Text(text = "Dokumentasi visual kondisi lapangan", fontSize = 10.sp, color = SecondarySlate)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Action Bar: Draft & Exit + Navigation
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onSaveDraftAndExit,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Simpan Draft & Keluar", fontSize = 11.sp)
                    }

                    if (selectedIndex < inspections.size - 1) {
                        Button(
                            onClick = { selectedIndex++ },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Objek Berikutnya", fontSize = 12.sp)
                        }
                    } else {
                        Button(
                            onClick = onNext,
                            colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Lanjut Tahap 3", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// TAHAP 3 â€” KEPUTUSAN SEIRI
// -------------------------------------------------------------------------------------
@Composable
private fun Stage3SeiriDecisions(
    session: MentoringSession,
    onUpdateInspection: (List<SessionObjectInspection>) -> Unit,
    onNext: () -> Unit,
    onSaveDraftAndExit: () -> Unit,
    onPrev: () -> Unit
) {
    val inspections = session.inspections
    val decisionOptions = listOf(
        Pair("Diperlukan (Hijau)", StatusGreen),
        Pair("Dipindahkan", PrimaryBlue),
        Pair("Tidak Diperlukan / Red Tag (Merah)", StatusRed),
        Pair("Perlu Konfirmasi (Kuning)", StatusAmber)
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Tahap 3 â€” Keputusan Seiri",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Tentukan status akhir setiap objek dan berikan justifikasi keputusan.",
                    fontSize = 11.sp,
                    color = SecondarySlate
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            itemsIndexed(inspections) { idx, item ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${idx + 1}. ${item.objectName}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(color = SurfaceVariantLight, shape = RoundedCornerShape(4.dp)) {
                                Text(
                                    text = item.objectCode,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = "Penilaian Awal: ${item.initialAssessment} â€¢ Frekuensi: ${item.usageFrequency}",
                            fontSize = 11.sp,
                            color = SecondarySlate
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = "Pilih Keputusan Seiri *", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)

                        // 4 decision chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            decisionOptions.forEach { (dec, color) ->
                                FilterChip(
                                    selected = item.seiriDecision == dec,
                                    onClick = {
                                        val updated = inspections.toMutableList()
                                        updated[idx] = item.copy(seiriDecision = dec, isStage3Checked = true)
                                        onUpdateInspection(updated)
                                    },
                                    label = { Text(dec, fontSize = 11.sp) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = item.decisionReason,
                            onValueChange = { newReason ->
                                val updated = inspections.toMutableList()
                                updated[idx] = item.copy(decisionReason = newReason, isStage3Checked = true)
                                onUpdateInspection(updated)
                            },
                            label = { Text("Alasan Justifikasi Keputusan *") },
                            placeholder = { Text("Contoh: Mesin masih beroperasi aktif / barang tidak lagi terpakai") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Bottom Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onSaveDraftAndExit,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Simpan Draft & Keluar", fontSize = 12.sp)
                }

                Button(
                    onClick = onNext,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1.5f)
                ) {
                    Text("Lanjut: Tahap 4 (PIC Tindakan)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// TAHAP 4 â€” PIC DAN TINDAKAN (KHUSUS RED TAG / DIPINDAHKAN)
// -------------------------------------------------------------------------------------
@Composable
private fun Stage4ActionPlans(
    session: MentoringSession,
    onUpdateInspection: (List<SessionObjectInspection>) -> Unit,
    onNext: () -> Unit,
    onSaveDraftAndExit: () -> Unit,
    onPrev: () -> Unit
) {
    val inspections = session.inspections
    // Filter objects that need action (Red Tag / Dipindahkan)
    val actionItems = inspections.filter {
        it.seiriDecision.contains("Red Tag") ||
        it.seiriDecision.contains("Tidak Diperlukan") ||
        it.seiriDecision.contains("Dipindahkan")
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Tahap 4 â€” PIC dan Tindakan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Khusus objek bertanda Red Tag / Dipindahkan, tentukan tindakan, PIC, dan batas waktu.",
                    fontSize = 11.sp,
                    color = SecondarySlate
                )
            }
        }

        if (actionItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = StatusGreenContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(54.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Tidak Ada Objek Red Tag / Dipindahkan",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF14532D)
                        )
                        Text(
                            text = "Semua objek di zona ini dinilai Diperlukan. Anda dapat langsung melanjutkan ke Tahap 5.",
                            fontSize = 12.sp,
                            color = Color(0xFF166534)
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(actionItems) { item ->
                    val globalIdx = inspections.indexOfFirst { it.objectId == item.objectId }
                    val isRedTag = item.seiriDecision.contains("Red Tag")

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.objectName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    color = if (isRedTag) StatusRedContainer else StatusBlueContainer,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = item.seiriDecision,
                                        color = if (isRedTag) StatusRed else PrimaryBlue,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = item.actionDescription,
                                onValueChange = { newDesc ->
                                    if (globalIdx != -1) {
                                        val updated = inspections.toMutableList()
                                        updated[globalIdx] = item.copy(actionDescription = newDesc, isStage4Checked = true)
                                        onUpdateInspection(updated)
                                    }
                                },
                                label = { Text("Deskripsi Rencana Tindakan *") },
                                placeholder = { Text("Contoh: Pindahkan ke tempat penampungan limbah/Gudang B") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = item.pic,
                                    onValueChange = { newPic ->
                                        if (globalIdx != -1) {
                                            val updated = inspections.toMutableList()
                                            updated[globalIdx] = item.copy(pic = newPic, isStage4Checked = true)
                                            onUpdateInspection(updated)
                                        }
                                    },
                                    label = { Text("PIC Penanggung Jawab *") },
                                    modifier = Modifier.weight(1f)
                                )

                                OutlinedTextField(
                                    value = item.dueDate,
                                    onValueChange = { newDue ->
                                        if (globalIdx != -1) {
                                            val updated = inspections.toMutableList()
                                            updated[globalIdx] = item.copy(dueDate = newDue, isStage4Checked = true)
                                            onUpdateInspection(updated)
                                        }
                                    },
                                    label = { Text("Tenggat Waktu *") },
                                    placeholder = { Text("YYYY-MM-DD") },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Upload Foto Bukti Selesai (Opsional)
                            Surface(
                                color = SurfaceVariantLight,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (globalIdx != -1) {
                                            val updated = inspections.toMutableList()
                                            val newPhoto = if (item.completionPhotoUri == null) "bukti_tindakan_${item.objectCode}.jpg" else null
                                            updated[globalIdx] = item.copy(completionPhotoUri = newPhoto, isStage4Checked = true)
                                            onUpdateInspection(updated)
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (item.completionPhotoUri != null) Icons.Default.CheckCircle else Icons.Default.AddAPhoto,
                                        contentDescription = null,
                                        tint = if (item.completionPhotoUri != null) StatusGreen else PrimaryBlue,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (item.completionPhotoUri != null) "Foto Bukti Selesai Terunggah" else "Upload Foto Bukti Selesai (Opsional)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (item.completionPhotoUri != null) StatusGreen else IndustrialNavy
                                        )
                                        Text(
                                            text = if (item.completionPhotoUri != null) "${item.completionPhotoUri} (Klik untuk ganti/hapus)" else "Ambil foto atau lampirkan bukti visual perbaikan",
                                            fontSize = 10.sp,
                                            color = SecondarySlate
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onSaveDraftAndExit,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Simpan Draft & Keluar", fontSize = 12.sp)
                }

                Button(
                    onClick = onNext,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1.5f)
                ) {
                    Text("Lanjut: Tahap 5 (Review Final)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// TAHAP 5 â€” REVIEW & SIMPAN FINAL
// -------------------------------------------------------------------------------------
@Composable
private fun Stage5ReviewAndFinalSubmit(
    session: MentoringSession,
    repository: SeiriRepository,
    onShowSchema: () -> Unit,
    onSubmitFinal: () -> Unit,
    onSaveDraftAndExit: () -> Unit,
    onBackToStage: (Int) -> Unit
) {
    val inspections = session.inspections
    val countDiperlukan = inspections.count { it.seiriDecision.contains("Diperlukan") }
    val countRedTag = inspections.count { it.seiriDecision.contains("Red Tag") || it.seiriDecision.contains("Tidak Diperlukan") }
    val countPindah = inspections.count { it.seiriDecision.contains("Dipindahkan") }
    val countKonfirmasi = inspections.count { it.seiriDecision.contains("Konfirmasi") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Tahap 5 â€” Review & Simpan Final",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Periksa ringkasan hasil mentoring Seiri sebelum menyelesaikan dan mengunci sesi.",
                fontSize = 12.sp,
                color = SecondarySlate
            )
        }

        // Summary Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Ringkasan Keputusan Seiri", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "$countDiperlukan", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = StatusGreen)
                            Text(text = "Diperlukan", fontSize = 11.sp, color = SecondarySlate)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "$countRedTag", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = StatusRed)
                            Text(text = "Red Tag", fontSize = 11.sp, color = SecondarySlate)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "$countPindah", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = PrimaryBlue)
                            Text(text = "Dipindahkan", fontSize = 11.sp, color = SecondarySlate)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "$countKonfirmasi", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = StatusAmber)
                            Text(text = "Konfirmasi", fontSize = 11.sp, color = SecondarySlate)
                        }
                    }
                }
            }
        }

        // Action items / Red Tag list
        val redTagItems = inspections.filter { it.seiriDecision.contains("Red Tag") || it.seiriDecision.contains("Dipindahkan") }
        if (redTagItems.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Daftar Tugas / Action Items Red Tag", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))

                        redTagItems.forEach { itm ->
                            Surface(
                                color = SurfaceVariantLight,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(text = "${itm.objectName} (${itm.objectCode})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text(text = "Tindakan: ${itm.actionDescription.ifEmpty { "Perlu disposisi / mutasi" }}", fontSize = 11.sp)
                                    Text(text = "PIC: ${itm.pic.ifEmpty { "Operator Area" }} â€¢ Tenggat: ${itm.dueDate.ifEmpty { "Segera" }}", fontSize = 10.sp, color = SecondarySlate)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Schema / JSON button
        item {
            FilledTonalButton(
                onClick = onShowSchema,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Lihat Struktur Data JSON & Skema Google Sheets", fontSize = 12.sp)
            }
        }

        // Final Buttons
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onSubmitFinal,
                    colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("submit_final_session_button")
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Submit Final (Kunci Sesi)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                OutlinedButton(
                    onClick = onSaveDraftAndExit,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Simpan Sebagai Draft & Keluar")
                }

                TextButton(
                    onClick = { onBackToStage(2) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Kembali Periksa Data")
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// MODAL: ADD NEW OBJECT IN FIELD
// -------------------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddNewFieldObjectDialog(
    session: MentoringSession,
    repository: SeiriRepository,
    currentUser: User,
    onDismiss: () -> Unit,
    onAdded: (SeiriObject) -> Unit
) {
    var code by remember { mutableStateOf("OBJ-${repository.getAreaCode(session.areaId)}-${(100..999).random()}") }
    var name by remember { mutableStateOf("") }
    var function by remember { mutableStateOf("") }
    var pic by remember { mutableStateOf(currentUser.name) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("+ Tambah Objek Temuan Baru") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (errorMessage != null) {
                    Text(errorMessage ?: "", color = StatusRed, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Text("Zona: ${repository.getZoneName(session.zoneId)}", fontSize = 12.sp, color = SecondarySlate)
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Kode Objek *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Objek Temuan *") },
                    placeholder = { Text("Contoh: HOPPER B, CONVEYOR BELT CADANGAN") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = function,
                    onValueChange = { function = it },
                    label = { Text("Fungsi Objek") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = pic,
                    onValueChange = { pic = it },
                    label = { Text("PIC Penanggung Jawab *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (code.isBlank() || name.isBlank()) {
                        errorMessage = "Kode dan nama objek wajib diisi."
                        return@Button
                    }
                    val result = repository.addObject(
                        code = code,
                        name = name,
                        areaId = session.areaId,
                        zoneId = session.zoneId,
                        description = "Objek temuan baru di sesi mentoring",
                        function = function,
                        category = "Alat Kerja",
                        condition = ObjectCondition.BAIK,
                        pic = pic,
                        usageFrequency = UsageFrequency.DAILY
                    )
                    result.onSuccess { onAdded(it) }
                        .onFailure { errorMessage = it.message }
                }
            ) {
                Text("Tambahkan ke Sesi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
