package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.example.data.SeiriRepository
import com.example.ui.components.ObjectPhoto
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObjectMasterScreen(
    repository: SeiriRepository,
    onNavigateToTransfers: (String?) -> Unit
) {
    val currentUser by repository.currentUser.collectAsState()
    val allObjects by repository.objects.collectAsState()
    val areas by repository.areas.collectAsState()
    val zones by repository.zones.collectAsState()
    val validations by repository.validations.collectAsState()
    val changeLogs by repository.changeLogs.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedGroup by remember { mutableStateOf<String?>(null) }
    var selectedAreaId by remember { mutableStateOf<String?>(if (currentUser.role == UserRole.MENTOR) currentUser.assignedAreaId else null) }
    var selectedZoneId by remember { mutableStateOf<String?>(null) }
    var selectedObjectId by remember { mutableStateOf<String?>(null) }
    var selectedValidationStatus by remember { mutableStateOf<ValidationStatus?>(null) }
    var showOnlyActive by remember { mutableStateOf(true) }

    // Selected object for Detail, Edit, Deactivate, or Transfer
    var selectedObjectForDetail by remember { mutableStateOf<SeiriObject?>(null) }
    var selectedObjectForEdit by remember { mutableStateOf<SeiriObject?>(null) }
    var selectedObjectForDeactivate by remember { mutableStateOf<SeiriObject?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    // Feedback Snackbar / Alert
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    // Filter objects by search (Area Name or Object Name/Code/PIC/Function) & 4-tier hierarchy
    val filteredObjects = allObjects.filter { obj ->
        val area = areas.find { it.id == obj.areaId }
        val zone = zones.find { it.id == obj.zoneId }
        val areaName = area?.areaName ?: ""
        val areaCode = area?.areaCode ?: ""
        val groupName = area?.group ?: ""
        val zoneName = zone?.zoneName ?: ""

        // Can search by Area Name OR Object Name, Object Code, Function, PIC
        val matchesSearch = searchQuery.isBlank() ||
                obj.name.contains(searchQuery, ignoreCase = true) ||
                obj.objectCode.contains(searchQuery, ignoreCase = true) ||
                obj.function.contains(searchQuery, ignoreCase = true) ||
                obj.pic.contains(searchQuery, ignoreCase = true) ||
                areaName.contains(searchQuery, ignoreCase = true) ||
                areaCode.contains(searchQuery, ignoreCase = true) ||
                zoneName.contains(searchQuery, ignoreCase = true) ||
                groupName.contains(searchQuery, ignoreCase = true)

        val matchesGroup = selectedGroup == null || groupName.equals(selectedGroup, ignoreCase = true)
        val matchesArea = selectedAreaId == null || obj.areaId == selectedAreaId
        val matchesZone = selectedZoneId == null || obj.zoneId == selectedZoneId
        val matchesObject = selectedObjectId == null || obj.id == selectedObjectId
        val matchesValidation = selectedValidationStatus == null || obj.validationStatus == selectedValidationStatus
        val matchesActive = !showOnlyActive || obj.isActive

        matchesSearch && matchesGroup && matchesArea && matchesZone && matchesObject && matchesValidation && matchesActive
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = PrimaryBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("add_object_fab")
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah Objek")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tambah Objek", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 1. Search Bar (Cari Nama Area atau Nama/Kode Objek)
            AreaOrObjectSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = "Cari nama area atau objek (cth: Preform, Hopper, Mold)...",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            // 2. Dropdown Menu dari Hirarki Paling Atas (GROUP -> AREA -> ZONA -> OBJEK)
            HierarchyCascadingFilterCard(
                allAreas = areas,
                allZones = zones,
                allObjects = allObjects,
                selectedGroup = selectedGroup,
                selectedAreaId = selectedAreaId,
                selectedZoneId = selectedZoneId,
                selectedObjectId = selectedObjectId,
                onGroupSelected = { newGroup ->
                    selectedGroup = newGroup
                    if (newGroup != null && selectedAreaId != null) {
                        val currentArea = areas.find { it.id == selectedAreaId }
                        if (currentArea?.group != newGroup) {
                            selectedAreaId = null
                            selectedZoneId = null
                            selectedObjectId = null
                        }
                    }
                },
                onAreaSelected = { newAreaId ->
                    selectedAreaId = newAreaId
                    if (newAreaId != null) {
                        val area = areas.find { it.id == newAreaId }
                        if (area != null) selectedGroup = area.group
                        if (selectedZoneId != null) {
                            val zone = zones.find { it.id == selectedZoneId }
                            if (zone?.areaId != newAreaId) {
                                selectedZoneId = null
                                selectedObjectId = null
                            }
                        }
                    } else {
                        selectedZoneId = null
                        selectedObjectId = null
                    }
                },
                onZoneSelected = { newZoneId ->
                    selectedZoneId = newZoneId
                    if (newZoneId != null) {
                        val zone = zones.find { it.id == newZoneId }
                        if (zone != null) {
                            selectedAreaId = zone.areaId
                            val area = areas.find { it.id == zone.areaId }
                            if (area != null) selectedGroup = area.group
                        }
                        if (selectedObjectId != null) {
                            val obj = allObjects.find { it.id == selectedObjectId }
                            if (obj?.zoneId != newZoneId) {
                                selectedObjectId = null
                            }
                        }
                    } else {
                        selectedObjectId = null
                    }
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
                onResetFilter = {
                    searchQuery = ""
                    selectedGroup = null
                    selectedAreaId = if (currentUser.role == UserRole.MENTOR) currentUser.assignedAreaId else null
                    selectedZoneId = null
                    selectedObjectId = null
                    selectedValidationStatus = null
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                title = "Dropdown Menu Hirarki (Group, Area, Zona, Objek)",
                showObjectFilter = true
            )

            // Secondary Quick Filter Chips (Validation Status & Nonaktif)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = selectedValidationStatus == null,
                    onClick = { selectedValidationStatus = null },
                    label = { Text("Semua Status") }
                )

                FilterChip(
                    selected = selectedValidationStatus == ValidationStatus.BELUM_DIVALIDASI,
                    onClick = {
                        selectedValidationStatus = if (selectedValidationStatus == ValidationStatus.BELUM_DIVALIDASI) null else ValidationStatus.BELUM_DIVALIDASI
                    },
                    label = { Text("Belum Validasi") }
                )

                FilterChip(
                    selected = selectedValidationStatus == ValidationStatus.TERVALIDASI,
                    onClick = {
                        selectedValidationStatus = if (selectedValidationStatus == ValidationStatus.TERVALIDASI) null else ValidationStatus.TERVALIDASI
                    },
                    label = { Text("Tervalidasi") }
                )

                FilterChip(
                    selected = selectedValidationStatus == ValidationStatus.PERLU_KONFIRMASI,
                    onClick = {
                        selectedValidationStatus = if (selectedValidationStatus == ValidationStatus.PERLU_KONFIRMASI) null else ValidationStatus.PERLU_KONFIRMASI
                    },
                    label = { Text("Perlu Konfirmasi") }
                )

                // Active toggle
                FilterChip(
                    selected = !showOnlyActive,
                    onClick = { showOnlyActive = !showOnlyActive },
                    label = { Text("Termasuk Nonaktif") }
                )
            }

            // Results count banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Menampilkan ${filteredObjects.size} Objek",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SecondarySlate
                )
                if (currentUser.role == UserRole.MENTOR) {
                    Text(
                        text = "Area: ${repository.getAreaName(currentUser.assignedAreaId ?: "")}",
                        fontSize = 11.sp,
                        color = PrimaryBlue,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Objects List
            if (filteredObjects.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = SecondarySlateLight,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Tidak Ada Objek Ditemukan",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SecondarySlate
                        )
                        Text(
                            text = "Coba ubah kata kunci pencarian atau reset filter di atas.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondarySlateLight
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredObjects, key = { it.id }) { obj ->
                        ObjectCardItem(
                            obj = obj,
                            areaName = repository.getAreaName(obj.areaId),
                            zoneName = repository.getZoneName(obj.zoneId),
                            canManage = repository.canManageObject(obj, currentUser),
                            onViewDetail = { selectedObjectForDetail = obj },
                            onEdit = { selectedObjectForEdit = obj },
                            onDeactivate = { selectedObjectForDeactivate = obj },
                            onReactivate = {
                                repository.reactivateObject(obj.id).onSuccess {
                                    feedbackMessage = "Objek ${obj.name} berhasil diaktifkan kembali."
                                }
                            },
                            onRequestTransfer = {
                                onNavigateToTransfers(obj.id)
                            }
                        )
                    }
                }
            }
        }
    }

    // DETAIL DIALOG
    selectedObjectForDetail?.let { obj ->
        ObjectDetailModal(
            obj = obj,
            repository = repository,
            onDismiss = { selectedObjectForDetail = null },
            onEdit = {
                selectedObjectForDetail = null
                selectedObjectForEdit = obj
            },
            onRequestTransfer = {
                selectedObjectForDetail = null
                onNavigateToTransfers(obj.id)
            }
        )
    }

    // EDIT DIALOG
    selectedObjectForEdit?.let { obj ->
        ObjectEditModal(
            obj = obj,
            repository = repository,
            onDismiss = { selectedObjectForEdit = null },
            onSaved = { updated ->
                selectedObjectForEdit = null
                feedbackMessage = "Data objek ${updated.name} berhasil diperbarui."
            }
        )
    }

    // ADD OBJECT DIALOG
    if (showAddDialog) {
        ObjectAddModal(
            repository = repository,
            currentUser = currentUser,
            onDismiss = { showAddDialog = false },
            onAdded = { newObj ->
                showAddDialog = false
                feedbackMessage = "Objek baru '${newObj.name}' (${newObj.objectCode}) berhasil didaftarkan."
            }
        )
    }

    // DEACTIVATE CONFIRMATION DIALOG
    selectedObjectForDeactivate?.let { obj ->
        var deactReason by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { selectedObjectForDeactivate = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = StatusRed) },
            title = { Text("Nonaktifkan Objek (Soft Delete)") },
            text = {
                Column {
                    Text("Objek '${obj.name}' (${obj.objectCode}) akan ditandai nonaktif/afkir tanpa menghapus riwayat audit atau validasi terdahulu.")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = deactReason,
                        onValueChange = { deactReason = it },
                        label = { Text("Alasan Penonaktifan / Red Tag") },
                        placeholder = { Text("Contoh: Rusak permanen, afkir, atau dibuang") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.deactivateObject(obj.id, deactReason)
                        selectedObjectForDeactivate = null
                        feedbackMessage = "Objek ${obj.name} dinonaktifkan."
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed)
                ) {
                    Text("Nonaktifkan")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedObjectForDeactivate = null }) {
                    Text("Batal")
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

@Composable
private fun ObjectCardItem(
    obj: SeiriObject,
    areaName: String,
    zoneName: String,
    canManage: Boolean,
    onViewDetail: () -> Unit,
    onEdit: () -> Unit,
    onDeactivate: () -> Unit,
    onReactivate: () -> Unit,
    onRequestTransfer: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onViewDetail)
            .testTag("object_card_${obj.objectCode}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            if (!obj.photoUrl.isNullOrBlank()) {
                ObjectPhoto(
                    photoUrl = obj.photoUrl,
                    modifier = Modifier.fillMaxWidth().height(120.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Header Row: Code + Status Badge + Active Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = SurfaceVariantLight,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = obj.objectCode,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = IndustrialNavy,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    if (!obj.isActive) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = StatusRedContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "NONAKTIF",
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = StatusRed,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                ValidationStatusBadge(obj.validationStatus)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Object Name
            Text(
                text = obj.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (obj.isActive) MaterialTheme.colorScheme.onSurface else SecondarySlateLight
            )

            // Function / Description
            if (obj.function.isNotBlank()) {
                Text(
                    text = obj.function,
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondarySlate,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Metadata Row: Area & Zone, Condition, Usage
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ConditionBadge(obj.condition)
                Surface(
                    color = SurfaceVariantLight,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "$areaName • $zoneName",
                        fontSize = 11.sp,
                        color = SecondarySlate,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PIC: ${obj.pic} • Frek: ${obj.usageFrequency.label}",
                    fontSize = 11.sp,
                    color = SecondarySlateLight
                )

                Row {
                    if (canManage && obj.isActive) {
                        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = onRequestTransfer, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = "Pindah Aset", tint = StatusAmber, modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = onDeactivate, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Nonaktifkan", tint = StatusRed, modifier = Modifier.size(18.dp))
                        }
                    } else if (canManage && !obj.isActive) {
                        TextButton(onClick = onReactivate) {
                            Text("Aktifkan Lagi", fontSize = 11.sp, color = StatusGreen)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ObjectDetailModal(
    obj: SeiriObject,
    repository: SeiriRepository,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onRequestTransfer: () -> Unit
) {
    val validations by repository.validations.collectAsState()
    val changeLogs by repository.changeLogs.collectAsState()
    val objValidations = validations.filter { it.objectId == obj.id }
    val objLogs = changeLogs.filter { it.objectId == obj.id }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = PrimaryBlueContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = obj.objectCode,
                            color = PrimaryBlueDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    ValidationStatusBadge(obj.validationStatus)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = obj.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (!obj.photoUrl.isNullOrBlank()) {
                    ObjectPhoto(
                        photoUrl = obj.photoUrl,
                        modifier = Modifier.fillMaxWidth().height(200.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text(
                    text = "DATA MASTER ASET",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue
                )
                Spacer(modifier = Modifier.height(4.dp))
                DetailRow("Area Pemilik", repository.getAreaName(obj.areaId))
                DetailRow("Zona Lokasi", repository.getZoneName(obj.zoneId))
                DetailRow("Kategori", obj.category)
                DetailRow("Fungsi", obj.function.ifEmpty { "-" })
                DetailRow("Kondisi Fisik", obj.condition.label)
                DetailRow("Penanggung Jawab (PIC)", obj.pic)
                DetailRow("Frekuensi Penggunaan", obj.usageFrequency.label)
                DetailRow("Terakhir Digunakan", obj.lastUsedDate)
                DetailRow("ID Sumber Spreadsheet", obj.sourceObjectId ?: "Dibuat Manual")
                DetailRow("Versi Data Master", "v${obj.version}")

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "RIWAYAT VALIDASI SEIRI (${objValidations.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = StatusGreen
                )
                if (objValidations.isEmpty()) {
                    Text("Belum ada riwayat validasi Seiri tercatat.", fontSize = 12.sp, color = SecondarySlate)
                } else {
                    objValidations.forEach { valItem ->
                        Surface(
                            color = SurfaceVariantLight,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    DecisionBadge(valItem.seiriDecision)
                                    Text(valItem.createdAt, fontSize = 10.sp, color = SecondarySlate)
                                }
                                Text("Alasan: ${valItem.decisionReason}", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                if (valItem.followUpAction.isNotBlank()) {
                                    Text("Tindak Lanjut: ${valItem.followUpAction}", fontSize = 11.sp, color = StatusPurple)
                                }
                                Text("Validator: ${valItem.createdBy}", fontSize = 10.sp, color = SecondarySlateLight)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "LOG PERUBAHAN & AUDIT (${objLogs.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SecondarySlate
                )
                objLogs.take(3).forEach { log ->
                    Text(
                        text = "• ${log.changedAt}: ${log.changeType} oleh ${log.changedBy}",
                        fontSize = 11.sp,
                        color = SecondarySlate
                    )
                }
            }
        },
        confirmButton = {
            Row {
                Button(
                    onClick = onRequestTransfer,
                    colors = ButtonDefaults.buttonColors(containerColor = StatusAmber)
                ) {
                    Text("Ajukan Pindah")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onEdit) {
                    Text("Edit Data")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ObjectEditModal(
    obj: SeiriObject,
    repository: SeiriRepository,
    onDismiss: () -> Unit,
    onSaved: (SeiriObject) -> Unit
) {
    val zones by repository.zones.collectAsState()
    val areaZones = zones.filter { it.areaId == obj.areaId }

    var name by remember { mutableStateOf(obj.name) }
    var zoneId by remember { mutableStateOf(obj.zoneId) }
    var description by remember { mutableStateOf(obj.description) }
    var function by remember { mutableStateOf(obj.function) }
    var category by remember { mutableStateOf(obj.category) }
    var condition by remember { mutableStateOf(obj.condition) }
    var pic by remember { mutableStateOf(obj.pic) }
    var usageFrequency by remember { mutableStateOf(obj.usageFrequency) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Koreksi Data Master: ${obj.objectCode}") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (errorMessage != null) {
                    Text(errorMessage ?: "", color = StatusRed, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Objek *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Zone Selector
                Text("Zona Lokasi di ${repository.getAreaName(obj.areaId)}", fontSize = 11.sp, color = SecondarySlate)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    areaZones.forEach { z ->
                        FilterChip(
                            selected = zoneId == z.id,
                            onClick = { zoneId = z.id },
                            label = { Text(z.zoneName, fontSize = 11.sp) }
                        )
                    }
                }
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
                    label = { Text("PIC / Penanggung Jawab") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text("Frekuensi Penggunaan", fontSize = 11.sp, color = SecondarySlate)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    UsageFrequency.values().forEach { freq ->
                        FilterChip(
                            selected = usageFrequency == freq,
                            onClick = { usageFrequency = freq },
                            label = { Text(freq.label, fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMessage = "Nama objek tidak boleh kosong."
                        return@Button
                    }
                    val result = repository.updateObject(
                        id = obj.id,
                        name = name,
                        zoneId = zoneId,
                        description = description,
                        function = function,
                        category = category,
                        condition = condition,
                        pic = pic,
                        usageFrequency = usageFrequency
                    )
                    result.onSuccess { onSaved(it) }
                        .onFailure { errorMessage = it.message }
                }
            ) {
                Text("Simpan Perubahan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ObjectAddModal(
    repository: SeiriRepository,
    currentUser: User,
    onDismiss: () -> Unit,
    onAdded: (SeiriObject) -> Unit
) {
    val areas by repository.areas.collectAsState()
    val zones by repository.zones.collectAsState()

    var code by remember { mutableStateOf("OBJ-${currentUser.assignedAreaId?.takeLast(2)?.uppercase() ?: "NEW"}-") }
    var name by remember { mutableStateOf("") }
    var selectedAreaId by remember { mutableStateOf(currentUser.assignedAreaId ?: areas.first().id) }
    var selectedZoneId by remember {
        mutableStateOf(zones.firstOrNull { it.areaId == selectedAreaId }?.id ?: "")
    }
    var function by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Alat Kerja") }
    var condition by remember { mutableStateOf(ObjectCondition.BAIK) }
    var pic by remember { mutableStateOf(currentUser.name) }
    var usageFrequency by remember { mutableStateOf(UsageFrequency.DAILY) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Objek Baru ke Master") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (errorMessage != null) {
                    Text(errorMessage ?: "", color = StatusRed, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                }

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Kode Objek *") },
                    placeholder = { Text("Contoh: OBJ-MC-010") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Objek *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (currentUser.role == UserRole.ADMIN) {
                    Text("Pilih Area", fontSize = 11.sp, color = SecondarySlate)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        areas.forEach { a ->
                            FilterChip(
                                selected = selectedAreaId == a.id,
                                onClick = {
                                    selectedAreaId = a.id
                                    selectedZoneId = zones.firstOrNull { it.areaId == a.id }?.id ?: ""
                                },
                                label = { Text(a.areaCode) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text("Zona Lokasi", fontSize = 11.sp, color = SecondarySlate)
                val currentZones = zones.filter { it.areaId == selectedAreaId }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    currentZones.forEach { z ->
                        FilterChip(
                            selected = selectedZoneId == z.id,
                            onClick = { selectedZoneId = z.id },
                            label = { Text(z.zoneName, fontSize = 11.sp) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = function,
                    onValueChange = { function = it },
                    label = { Text("Fungsi / Kegunaan") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = pic,
                    onValueChange = { pic = it },
                    label = { Text("PIC Penanggung Jawab") },
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
                        areaId = selectedAreaId,
                        zoneId = selectedZoneId,
                        description = "Ditambahkan secara manual",
                        function = function,
                        category = category,
                        condition = condition,
                        pic = pic,
                        usageFrequency = usageFrequency
                    )
                    result.onSuccess { onAdded(it) }
                        .onFailure { errorMessage = it.message }
                }
            ) {
                Text("Daftarkan Objek")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 11.sp, color = SecondarySlate)
        Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = IndustrialNavy)
    }
}
