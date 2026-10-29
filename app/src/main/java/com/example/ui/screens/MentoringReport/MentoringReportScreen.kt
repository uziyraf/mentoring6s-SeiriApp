package com.example.ui.screens.MentoringReport

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SampleData
import com.example.data.SeiriRepository
import com.example.model.AreaMentoringGroup
import com.example.model.MentoringReport
import com.example.model.MentoringReportRepository
import com.example.ui.theme.*

private sealed interface ReportPage {
    data object History : ReportPage
    data class Timeline(val group: AreaMentoringGroup) : ReportPage
    data class Form(val editing: MentoringReport?, val preselectedGroup: AreaMentoringGroup?) : ReportPage
}

@Composable
fun MentoringReportScreen(
    repository: SeiriRepository,
    sessionId: String,
    mentorName: String,
    mentorId: String = ""
) {
    var page by remember { mutableStateOf<ReportPage>(ReportPage.History) }
    var showDeleteConfirm by remember { mutableStateOf<MentoringReport?>(null) }

    BackHandler(enabled = page !is ReportPage.History) {
        if (page is ReportPage.Form && (page as ReportPage.Form).preselectedGroup != null) {
            page = ReportPage.Timeline((page as ReportPage.Form).preselectedGroup!!)
        } else {
            page = ReportPage.History
        }
    }

    when (val current = page) {
        ReportPage.History -> HistoryMentoringReport(
            onCreateNewForArea = { 
                page = ReportPage.Form(null, null) 
            },
            onOpenTimeline = { group -> 
                page = ReportPage.Timeline(group) 
            }
        )

        is ReportPage.Timeline -> AreaTimelineScreen(
            group = current.group,
            onBack = { page = ReportPage.History },
            onAddNewNote = { page = ReportPage.Form(null, current.group) }, 
            onEditNote = { page = ReportPage.Form(it, current.group) },
            onDeleteNote = { showDeleteConfirm = it }
        )

        is ReportPage.Form -> ReportFormPage(
            initial = current.editing,
            preselectedGroup = current.preselectedGroup, 
            repository = repository,
            sessionId = sessionId,
            mentorName = mentorName,
            mentorId = mentorId,
            onBack = { 
                if (current.preselectedGroup != null) page = ReportPage.Timeline(current.preselectedGroup)
                else page = ReportPage.History
            }
        )
    }

    // Dialog Konfirmasi Hapus
    showDeleteConfirm?.let { target ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Hapus Catatan?") },
            text = { Text("Catatan kunjungan pada ${target.createdAt} akan dihapus permanen.") },
            confirmButton = {
                TextButton(onClick = {
                    MentoringReportRepository.deleteReport(target.id)
                    showDeleteConfirm = null
                }) { Text("Hapus", color = StatusRed) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) { Text("Batal") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportFormPage(
    initial: MentoringReport?,
    preselectedGroup: AreaMentoringGroup?,
    repository: SeiriRepository,
    sessionId: String,
    mentorName: String,
    mentorId: String,
    onBack: () -> Unit
) {
    val areas by repository.areas.collectAsState()
    val zones by repository.zones.collectAsState()

    var plant by remember { mutableStateOf(initial?.plant ?: "") }
    var areaId by remember { mutableStateOf(initial?.areaId ?: "") }
    var pic by remember { mutableStateOf(initial?.pic ?: "") }
    var zoneId by remember { mutableStateOf(initial?.zoneId ?: "") }
    var audience by remember { mutableStateOf(initial?.audience ?: "") }
    var activity by remember { mutableStateOf(initial?.activityDescription ?: "") }
    var result by remember { mutableStateOf(initial?.result ?: "") }
    var obstacles by remember { mutableStateOf(initial?.obstacles ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    val plants = remember(areas) { areas.map { it.group }.distinct().sorted() }
    val areaOptions = remember(areas, plant) { areas.filter { it.group == plant } }
    val zoneOptions = remember(zones, areaId) { zones.filter { it.areaId == areaId } }
    val picOptions = remember { SampleData.allUsers.map { it.name } }

    // Otomatis isi state jika berasal dari Timeline
    LaunchedEffect(preselectedGroup) {
        if (preselectedGroup != null) {
            plant = preselectedGroup.reports.firstOrNull()?.plant ?: ""
            areaId = preselectedGroup.areaId
            zoneId = preselectedGroup.zoneId
            // pic dibiarkan kosong atau mengambil dari report sebelumnya jika edit
        }
    }

    Column(Modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                }
                Column {
                    Text(
                        if (initial == null) "Laporan Baru" else "Edit Laporan",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "Mentor: $mentorName",
                        style = MaterialTheme.typography.labelSmall,
                        color = SecondarySlate
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            error?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = StatusRed)
            }

            if (preselectedGroup == null) {
                // Tampilkan Dropdown Plant, Area, Zona seperti biasa
                PickerField("Plant *", plant, plants) { selectedPlant ->
                    plant = selectedPlant
                    areaId = ""
                    zoneId = ""
                }
                PickerField("Area *", areas.find { it.id == areaId }?.areaName ?: "", areaOptions.map { it.areaName }) { name ->
                    areaId = areaOptions.find { it.areaName == name }?.id ?: ""
                    zoneId = ""
                }
                PickerField("Zona *", zones.find { it.id == zoneId }?.zoneName ?: "", zoneOptions.map { it.zoneName }) { name ->
                    zoneId = zoneOptions.find { it.zoneName == name }?.id ?: ""
                }
            } else {
                // Jika diakses dari Timeline, tampilkan info lokasi yang terkunci (Read-only)
                Surface(color = PrimaryBlueContainer, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Lokasi Terkunci (Dari Riwayat Area)", fontSize = 10.sp, color = PrimaryBlueDark)
                        Text("${preselectedGroup.areaName} - ${preselectedGroup.zoneName}", fontWeight = FontWeight.Bold, color = PrimaryBlueDark)
                    }
                }
            }

            // Picker untuk PIC
            PickerField("PIC *", pic, picOptions) { selectedPic ->
                pic = selectedPic
            }

            HorizontalDivider(Modifier.padding(vertical = 4.dp))

            Text(
                "Isi Laporan",
                style = MaterialTheme.typography.titleSmall,
                color = PrimaryBlueDark
            )
            OutlinedTextField(audience, { audience = it }, label = { Text("Audiens *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(activity, { activity = it }, label = { Text("Uraian Kegiatan *") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(result, { result = it }, label = { Text("Hasil") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(obstacles, { obstacles = it }, label = { Text("Kendala") }, minLines = 2, modifier = Modifier.fillMaxWidth())
        }

        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Batal") }
                Button(
                    onClick = {
                        if (plant.isBlank() || areaId.isBlank() || pic.isBlank() || zoneId.isBlank()) {
                            error = "Plant, area, PIC, dan zona wajib dipilih."
                            return@Button
                        }
                        if (audience.isBlank() || activity.isBlank()) {
                            error = "Audiens dan uraian kegiatan wajib diisi."
                            return@Button
                        }
                        val now = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                            .format(java.util.Date())
                        val areaName = areas.find { it.id == areaId }?.areaName ?: ""
                        val zoneName = zones.find { it.id == zoneId }?.zoneName ?: ""
                        
                        if (initial == null) {
                            MentoringReportRepository.createReport(
                                MentoringReport(
                                    id = java.util.UUID.randomUUID().toString(),
                                    sessionId = sessionId,
                                    mentorName = mentorName,
                                    plant = plant,
                                    areaId = areaId,
                                    areaName = areaName,
                                    pic = pic,
                                    zoneId = zoneId,
                                    zoneName = zoneName,
                                    audience = audience,
                                    activityDescription = activity,
                                    result = result,
                                    obstacles = obstacles,
                                    createdAt = now,
                                    mentorId = mentorId
                                )
                            )
                        } else {
                            MentoringReportRepository.updateReport(
                                initial.copy(
                                    plant = plant,
                                    areaId = areaId,
                                    areaName = areaName,
                                    pic = pic,
                                    zoneId = zoneId,
                                    zoneName = zoneName,
                                    audience = audience,
                                    activityDescription = activity,
                                    result = result,
                                    obstacles = obstacles
                                )
                            )
                        }
                        onBack()
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Simpan") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickerField(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (options.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("-", style = MaterialTheme.typography.bodySmall) },
                    onClick = { expanded = false }
                )
            }
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, style = MaterialTheme.typography.bodyMedium) },
                    onClick = { onSelect(option); expanded = false }
                )
            }
        }
    }
}