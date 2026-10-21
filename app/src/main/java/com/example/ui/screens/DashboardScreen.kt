package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SeiriRepository
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun DashboardScreen(
    repository: SeiriRepository,
    onNavigateToMaster: () -> Unit,
    onNavigateToImport: () -> Unit,
    onNavigateToTransfers: () -> Unit,
    onNavigateToWizard: () -> Unit,
    onNavigateToSessions: () -> Unit,
    onNavigateToReports: () -> Unit
) {
    val currentUser by repository.currentUser.collectAsState()
    val objects by repository.objects.collectAsState()
    val areas by repository.areas.collectAsState()
    val sessions by repository.sessions.collectAsState()
    val transferRequests by repository.transferRequests.collectAsState()
    val changeLogs by repository.changeLogs.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Header & Current Role Pill
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = IndustrialNavy),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = if (currentUser.role == UserRole.ADMIN) PrimaryBlue else StatusGreen,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = currentUser.role.label.uppercase(),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        if (currentUser.assignedAreaId != null) {
                            val areaName = repository.getAreaName(currentUser.assignedAreaId ?: "")
                            Surface(
                                color = IndustrialNavyLight,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = areaName,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Selamat Datang, ${currentUser.name}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (currentUser.role == UserRole.ADMIN) {
                            "Pusat Komando Seiri 6S • Pemantauan Pemilahan Aset Seluruh Pabrik"
                        } else {
                            "Mentoring Seiri (Sort) di ${repository.getAreaName(currentUser.assignedAreaId ?: "")}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondarySlateLight
                    )
                }
            }
        }

        if (currentUser.role == UserRole.ADMIN) {
            // ADMIN DASHBOARD
            item {
                Text(
                    text = "RINGKASAN METRIK GLOBAL",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = SecondarySlate
                )
            }

            // Stat Cards Grid
            item {
                val activeObjects = objects.count { it.isActive }
                val awaitingVal = objects.count { it.isActive && it.validationStatus == ValidationStatus.BELUM_DIVALIDASI }
                val pendingTransfers = transferRequests.count { it.status == TransferStatus.MENUNGGU_PERSETUJUAN }
                val completedSessions = sessions.count { it.status == SessionStatus.COMPLETED }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = "Total Objek Aktif",
                            value = activeObjects.toString(),
                            icon = Icons.Default.Category,
                            color = PrimaryBlue,
                            bgColor = PrimaryBlueContainer,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Menunggu Validasi",
                            value = awaitingVal.toString(),
                            icon = Icons.Default.PendingActions,
                            color = StatusAmber,
                            bgColor = StatusAmberContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = "Permintaan Pindah",
                            value = pendingTransfers.toString(),
                            icon = Icons.Default.SwapHoriz,
                            color = if (pendingTransfers > 0) StatusRed else StatusGreen,
                            bgColor = if (pendingTransfers > 0) StatusRedContainer else StatusGreenContainer,
                            subText = if (pendingTransfers > 0) "Perlu tinjauan" else "Semua diproses",
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Sesi Selesai",
                            value = completedSessions.toString(),
                            icon = Icons.Default.FactCheck,
                            color = StatusGreen,
                            bgColor = StatusGreenContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Quick Actions Admin
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Aksi Cepat Admin",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onNavigateToImport,
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("admin_action_import")
                            ) {
                                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Impor CSV", fontSize = 12.sp)
                            }

                            FilledTonalButton(
                                onClick = onNavigateToTransfers,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("admin_action_transfers")
                            ) {
                                Icon(Icons.Default.AssignmentTurnedIn, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Tinjau Pindah", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Progress by Area
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Progres Validasi per Area",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(onClick = onNavigateToReports) {
                                Text("Laporan Lengkap", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        areas.forEach { area ->
                            val areaObjects = objects.filter { it.areaId == area.id && it.isActive }
                            val validatedCount = areaObjects.count { it.validationStatus == ValidationStatus.TERVALIDASI }
                            val total = areaObjects.size
                            val progress = if (total > 0) validatedCount.toFloat() / total.toFloat() else 0f

                            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${area.areaName} (${area.areaCode})",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "$validatedCount / $total (${(progress * 100).toInt()}%)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (progress >= 0.75f) StatusGreen else StatusAmber,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = if (progress >= 0.75f) StatusGreen else PrimaryBlue,
                                    trackColor = SurfaceVariantLight
                                )
                            }
                        }
                    }
                }
            }

            // Pending Transfer Requests Alert
            val pendingList = transferRequests.filter { it.status == TransferStatus.MENUNGGU_PERSETUJUAN }
            if (pendingList.isNotEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = StatusAmberContainer),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onNavigateToTransfers)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = StatusAmber,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${pendingList.size} Permintaan Pindah Menunggu Persetujuan",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF78350F)
                                )
                                Text(
                                    text = "Mentor mengajukan perpindahan aset antar-area. Klik untuk meninjau.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF92400E)
                                )
                            }
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = StatusAmber
                            )
                        }
                    }
                }
            }

            // Recent Changes
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Aktivitas & Perubahan Terakhir",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        changeLogs.take(5).forEach { log ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryBlueContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.History,
                                        contentDescription = null,
                                        tint = PrimaryBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = log.objectName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = log.changedAt.takeLast(5),
                                            fontSize = 11.sp,
                                            color = SecondarySlate
                                        )
                                    }
                                    Text(
                                        text = "${log.changedBy} • ${log.newValues}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SecondarySlate,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            HorizontalDivider(color = OutlineLight.copy(alpha = 0.5f))
                        }
                    }
                }
            }

        } else {
            // MENTOR DASHBOARD
            val areaId = currentUser.assignedAreaId ?: "area_mc"
            val areaObjects = objects.filter { it.areaId == areaId && it.isActive }
            val validatedCount = areaObjects.count { it.validationStatus == ValidationStatus.TERVALIDASI }
            val needConfirmCount = areaObjects.count { it.validationStatus == ValidationStatus.PERLU_KONFIRMASI }
            val unvalidatedCount = areaObjects.count { it.validationStatus == ValidationStatus.BELUM_DIVALIDASI }

            item {
                Text(
                    text = "STATUS OBJEK AREA BINAAN",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = SecondarySlate
                )
            }

            // Mentor Stat Grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = "Objek di Area",
                            value = areaObjects.size.toString(),
                            icon = Icons.Default.Category,
                            color = PrimaryBlue,
                            bgColor = PrimaryBlueContainer,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Tervalidasi",
                            value = validatedCount.toString(),
                            icon = Icons.Default.CheckCircle,
                            color = StatusGreen,
                            bgColor = StatusGreenContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = "Perlu Konfirmasi",
                            value = needConfirmCount.toString(),
                            icon = Icons.Default.HelpOutline,
                            color = StatusAmber,
                            bgColor = StatusAmberContainer,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Belum Divalidasi",
                            value = unvalidatedCount.toString(),
                            icon = Icons.Default.Pending,
                            color = StatusRed,
                            bgColor = StatusRedContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Big Action Button: Start Mentoring Wizard
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PrimaryBlue),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Mulai Mentoring Seiri 6S",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Validasi objek langkah-demi-langkah dengan panduan mobile",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = onNavigateToWizard,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = PrimaryBlueDark
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("mentor_start_session_button")
                        ) {
                            Text(
                                text = "Buka Wizard Sesi Mentoring",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Quick Mentor Actions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateToMaster,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Daftar Objek", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onNavigateToTransfers,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pindah Aset", fontSize = 12.sp)
                    }
                }
            }

            // Objects Awaiting Inspection
            val awaitingObjects = areaObjects.filter { it.validationStatus != ValidationStatus.TERVALIDASI }
            if (awaitingObjects.isNotEmpty()) {
                item {
                    Text(
                        text = "OBJEK MENUNGGU VALIDASI (${awaitingObjects.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = SecondarySlate
                    )
                }

                items(awaitingObjects.take(4)) { obj ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onNavigateToWizard)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = SurfaceVariantLight,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = obj.objectCode,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = IndustrialNavy,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    ValidationStatusBadge(obj.validationStatus)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = obj.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Zona: ${repository.getZoneName(obj.zoneId)} • PIC: ${obj.pic}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SecondarySlate
                                )
                            }
                            Icon(
                                Icons.Default.ArrowForwardIos,
                                contentDescription = null,
                                tint = SecondarySlate,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
