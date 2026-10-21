package com.example.ui.screens

import androidx.compose.foundation.background
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
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferRequestsScreen(
    repository: SeiriRepository,
    preselectedObjectId: String? = null
) {
    val currentUser by repository.currentUser.collectAsState()
    val allRequests by repository.transferRequests.collectAsState()
    val allObjects by repository.objects.collectAsState()
    val areas by repository.areas.collectAsState()

    var selectedStatusFilter by remember { mutableStateOf<TransferStatus?>(null) }
    var showNewRequestDialog by remember { mutableStateOf(preselectedObjectId != null) }
    var reviewTargetRequest by remember { mutableStateOf<ObjectTransferRequest?>(null) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    val filteredRequests = allRequests.filter { req ->
        val matchesStatus = selectedStatusFilter == null || req.status == selectedStatusFilter
        val matchesUser = if (currentUser.role == UserRole.ADMIN) true else {
            req.fromAreaId == currentUser.assignedAreaId || req.toAreaId == currentUser.assignedAreaId || req.requestedBy == currentUser.id
        }
        matchesStatus && matchesUser
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewRequestDialog = true },
                containerColor = PrimaryBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("submit_transfer_fab")
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = "Ajukan Pindah")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ajukan Pindah Aset", fontWeight = FontWeight.Bold)
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
            // Header Info Banner
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Permintaan Perpindahan Kepemilikan Aset",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (currentUser.role == UserRole.ADMIN) {
                            "Administrator berwenang meninjau, menyetujui, atau menolak mutasi aset antar-area."
                        } else {
                            "Mentor dapat mengajukan permohonan pemindahan aset ke area lain. Persetujuan dilakukan oleh Admin."
                        },
                        fontSize = 12.sp,
                        color = SecondarySlate
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Status Filters
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedStatusFilter == null,
                            onClick = { selectedStatusFilter = null },
                            label = { Text("Semua Status (${allRequests.size})") }
                        )
                        TransferStatus.values().forEach { st ->
                            val count = allRequests.count { it.status == st }
                            FilterChip(
                                selected = selectedStatusFilter == st,
                                onClick = { selectedStatusFilter = if (selectedStatusFilter == st) null else st },
                                label = { Text("${st.label} ($count)") }
                            )
                        }
                    }
                }
            }

            // List of Requests
            if (filteredRequests.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = SecondarySlateLight, modifier = Modifier.size(56.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Tidak Ada Permintaan Perpindahan", fontWeight = FontWeight.Bold, color = SecondarySlate)
                        Text("Klik 'Ajukan Pindah Aset' untuk mengirim permohonan mutasi.", fontSize = 12.sp, color = SecondarySlateLight)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredRequests, key = { it.id }) { req ->
                        RequestCardItem(
                            request = req,
                            fromAreaName = repository.getAreaName(req.fromAreaId),
                            toAreaName = repository.getAreaName(req.toAreaId),
                            isAdmin = currentUser.role == UserRole.ADMIN,
                            onReview = { reviewTargetRequest = req }
                        )
                    }
                }
            }
        }
    }

    // NEW TRANSFER REQUEST DIALOG
    if (showNewRequestDialog) {
        NewTransferRequestModal(
            repository = repository,
            currentUser = currentUser,
            preselectedObjectId = preselectedObjectId,
            onDismiss = { showNewRequestDialog = false },
            onSubmitted = {
                showNewRequestDialog = false
                feedbackMessage = "Permintaan perpindahan berhasil dikirim ke Administrator."
            }
        )
    }

    // ADMIN REVIEW DIALOG
    reviewTargetRequest?.let { req ->
        AdminReviewTransferModal(
            request = req,
            fromAreaName = repository.getAreaName(req.fromAreaId),
            toAreaName = repository.getAreaName(req.toAreaId),
            onDismiss = { reviewTargetRequest = null },
            onDecided = { approve, notes ->
                repository.reviewTransferRequest(req.id, approve, notes).onSuccess {
                    feedbackMessage = if (approve) "Permintaan disetujui. Kepemilikan aset resmi dimutasi." else "Permintaan perpindahan ditolak."
                    reviewTargetRequest = null
                }.onFailure {
                    feedbackMessage = it.message
                }
            }
        )
    }

    // Feedback Toast
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
private fun RequestCardItem(
    request: ObjectTransferRequest,
    fromAreaName: String,
    toAreaName: String,
    isAdmin: Boolean,
    onReview: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = SurfaceVariantLight,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = request.objectCode,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = IndustrialNavy,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                TransferStatusBadge(request.status)
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(text = request.objectName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(8.dp))

            // Area Transition
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceVariantLight, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(fromAreaName, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = SecondarySlate)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(toAreaName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryBlueDark)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Alasan: ${request.requestReason}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)

            if (request.supportingNotes.isNotBlank()) {
                Text(text = "Catatan: ${request.supportingNotes}", fontSize = 11.sp, color = SecondarySlate)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Diajukan: ${request.requesterName} • ${request.requestedAt.take(10)}",
                    fontSize = 10.sp,
                    color = SecondarySlateLight
                )

                if (isAdmin && request.status == TransferStatus.MENUNGGU_PERSETUJUAN) {
                    Button(
                        onClick = onReview,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Text("Tinjau & Putuskan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Review Note banner if already decided
            if (request.reviewedBy != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = if (request.status == TransferStatus.DISETUJUI) StatusGreenContainer else StatusRedContainer,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Keputusan Admin (${request.reviewerName}): ${request.reviewNotes ?: "-"}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (request.status == TransferStatus.DISETUJUI) StatusGreen else StatusRed,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewTransferRequestModal(
    repository: SeiriRepository,
    currentUser: User,
    preselectedObjectId: String?,
    onDismiss: () -> Unit,
    onSubmitted: () -> Unit
) {
    val allObjects by repository.objects.collectAsState()
    val areas by repository.areas.collectAsState()

    // Candidates for transfer
    val candidates = allObjects.filter { obj ->
        if (currentUser.role == UserRole.ADMIN) true
        else obj.areaId == currentUser.assignedAreaId
    }

    var selectedObjectId by remember {
        mutableStateOf(preselectedObjectId ?: candidates.firstOrNull()?.id ?: "")
    }
    val selectedObject = allObjects.find { it.id == selectedObjectId }
    val currentAreaId = selectedObject?.areaId ?: currentUser.assignedAreaId ?: areas.first().id

    // Available target areas
    val targetAreas = areas.filter { it.id != currentAreaId }
    var selectedTargetAreaId by remember { mutableStateOf(targetAreas.firstOrNull()?.id ?: "") }

    var reason by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Formulir Pengajuan Pindah Kepemilikan Aset") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (errorMessage != null) {
                    Text(errorMessage ?: "", color = StatusRed, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Text("1. Pilih Objek yang Akan Dipindahkan *", fontSize = 11.sp, color = SecondarySlate)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    candidates.forEach { obj ->
                        FilterChip(
                            selected = selectedObjectId == obj.id,
                            onClick = {
                                selectedObjectId = obj.id
                                val newTargetList = areas.filter { it.id != obj.areaId }
                                if (selectedTargetAreaId == obj.areaId) {
                                    selectedTargetAreaId = newTargetList.firstOrNull()?.id ?: ""
                                }
                            },
                            label = { Text("${obj.objectCode} - ${obj.name.take(15)}") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                selectedObject?.let {
                    Surface(
                        color = SurfaceVariantLight,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Area Pemilik Saat Ini (Terkunci):", fontSize = 10.sp, color = SecondarySlate)
                            Text(repository.getAreaName(it.areaId), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = IndustrialNavy)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("2. Area Tujuan Pemindahan *", fontSize = 11.sp, color = SecondarySlate)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    targetAreas.forEach { a ->
                        FilterChip(
                            selected = selectedTargetAreaId == a.id,
                            onClick = { selectedTargetAreaId = a.id },
                            label = { Text(a.areaName) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Alasan Perpindahan Aset *") },
                    placeholder = { Text("Contoh: Dibutuhkan untuk lini perakitan baru") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan Pendukung / PIC Penerima") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedObjectId.isBlank() || selectedTargetAreaId.isBlank() || reason.isBlank()) {
                        errorMessage = "Objek, area tujuan, dan alasan wajib diisi."
                        return@Button
                    }
                    val result = repository.submitTransferRequest(
                        objectId = selectedObjectId,
                        toAreaId = selectedTargetAreaId,
                        reason = reason,
                        supportingNotes = notes
                    )
                    result.onSuccess { onSubmitted() }
                        .onFailure { errorMessage = it.message }
                }
            ) {
                Text("Kirim Permohonan")
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
private fun AdminReviewTransferModal(
    request: ObjectTransferRequest,
    fromAreaName: String,
    toAreaName: String,
    onDismiss: () -> Unit,
    onDecided: (Boolean, String) -> Unit
) {
    var reviewNote by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tinjau Permohonan Mutasi Aset") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (errorMessage != null) {
                    Text(errorMessage ?: "", color = StatusRed, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Text(
                    text = "${request.objectCode}: ${request.objectName}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Text("Mutasi dari: $fromAreaName", fontSize = 12.sp, color = SecondarySlate)
                Text("Menuju: $toAreaName", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryBlueDark)
                Text("Diajukan Oleh: ${request.requesterName}", fontSize = 12.sp, color = SecondarySlate)
                Text("Alasan: ${request.requestReason}", fontSize = 12.sp, fontWeight = FontWeight.Medium)

                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = reviewNote,
                    onValueChange = { reviewNote = it },
                    label = { Text("Catatan Keputusan Administrator *") },
                    placeholder = { Text("Tuliskan justifikasi persetujuan atau penolakan...") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Row {
                Button(
                    onClick = {
                        if (reviewNote.isBlank()) {
                            errorMessage = "Catatan keputusan wajib diisi."
                            return@Button
                        }
                        onDecided(false, reviewNote)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed)
                ) {
                    Text("Tolak")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (reviewNote.isBlank()) {
                            errorMessage = "Catatan keputusan wajib diisi."
                            return@Button
                        }
                        onDecided(true, reviewNote)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusGreen)
                ) {
                    Text("Setujui Mutasi")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
