package com.example.ui.screens.mentoring

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*

// -------------------------------------------------------------------------------------
// HEADER / MULTI-SESSION SETUP & DRAFT RESUME
// -------------------------------------------------------------------------------------
@Composable
internal fun SessionSetupAndDraftPicker(
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
