package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SeiriRepository
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun ReportsScreen(
    repository: SeiriRepository
) {
    val context = LocalContext.current
    val areas by repository.areas.collectAsState()
    val objects by repository.objects.collectAsState()
    val transferRequests by repository.transferRequests.collectAsState()
    val validations by repository.validations.collectAsState()
    val actions by repository.actions.collectAsState()

    var selectedAreaId by remember { mutableStateOf<String?>(null) }
    var showExportSuccessDialog by remember { mutableStateOf(false) }
    var exportedCsvText by remember { mutableStateOf("") }

    val activeObjects = objects.filter { it.isActive && (selectedAreaId == null || it.areaId == selectedAreaId) }
    val totalCount = activeObjects.size
    val validatedCount = activeObjects.count { it.validationStatus == ValidationStatus.TERVALIDASI }
    val needConfirmCount = activeObjects.count { it.validationStatus == ValidationStatus.PERLU_KONFIRMASI }
    val unvalidatedCount = activeObjects.count { it.validationStatus == ValidationStatus.BELUM_DIVALIDASI }

    // Objects marked unnecessary
    val unnecessaryObjects = validations.filter { it.seiriDecision == SeiriDecision.TIDAK_DIPERLUKAN }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header & Export Button
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
                        Column {
                            Text(
                                text = "Laporan Analisis Seiri 6S",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Rekapitulasi pemilahan aset di seluruh lini pabrik",
                                fontSize = 12.sp,
                                color = SecondarySlate
                            )
                        }

                        Button(
                            onClick = {
                                val csv = repository.generateObjectsCsv(selectedAreaId)
                                exportedCsvText = csv
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Seiri 6S Export", csv)
                                clipboard.setPrimaryClip(clip)
                                showExportSuccessDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("export_csv_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ekspor CSV", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Area Filter
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedAreaId == null,
                            onClick = { selectedAreaId = null },
                            label = { Text("Semua Area") }
                        )
                        areas.forEach { a ->
                            FilterChip(
                                selected = selectedAreaId == a.id,
                                onClick = { selectedAreaId = a.id },
                                label = { Text(a.areaCode) }
                            )
                        }
                    }
                }
            }
        }

        // Summary Metric Cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "Objek Terdaftar",
                        value = totalCount.toString(),
                        icon = Icons.Default.Category,
                        color = PrimaryBlue,
                        bgColor = PrimaryBlueContainer,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Tervalidasi",
                        value = "$validatedCount (${if (totalCount > 0) (validatedCount * 100 / totalCount) else 0}%)",
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
                        title = "Belum Diinspeksi",
                        value = unvalidatedCount.toString(),
                        icon = Icons.Default.Pending,
                        color = StatusRed,
                        bgColor = StatusRedContainer,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Breakdown per Area Table
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Progres Pemilahan per Area Kerja",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    areas.forEach { area ->
                        val areaObjs = objects.filter { it.areaId == area.id && it.isActive }
                        val valObjs = areaObjs.count { it.validationStatus == ValidationStatus.TERVALIDASI }
                        val total = areaObjs.size
                        val progress = if (total > 0) valObjs.toFloat() / total.toFloat() else 0f

                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${area.areaName} (${area.areaCode})",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "$valObjs / $total (${(progress * 100).toInt()}%)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (progress >= 0.75f) StatusGreen else StatusAmber
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

        // Follow-Up Action Items
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
                            text = "Tindak Lanjut & Red Tag (${actions.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            color = StatusAmberContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "${actions.count { it.status == "PENDING" }} Tertunda",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatusAmber,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    actions.forEach { act ->
                        Surface(
                            color = SurfaceVariantLight,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${act.objectCode}: ${act.objectName}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Surface(
                                        color = if (act.status == "PENDING") StatusAmberContainer else StatusGreenContainer,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = act.status,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (act.status == "PENDING") StatusAmber else StatusGreen,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(text = act.actionDescription, fontSize = 11.sp, color = IndustrialNavy)
                                Text(
                                    text = "PIC: ${act.assignedTo} • Target: ${act.dueDate} • Area: ${repository.getAreaName(act.areaId)}",
                                    fontSize = 10.sp,
                                    color = SecondarySlate
                                )
                            }
                        }
                    }
                }
            }
        }

        // Transfer Requests Summary
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Status Permintaan Perpindahan Aset",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val pendingCount = transferRequests.count { it.status == TransferStatus.MENUNGGU_PERSETUJUAN }
                    val approvedCount = transferRequests.count { it.status == TransferStatus.DISETUJUI }
                    val rejectedCount = transferRequests.count { it.status == TransferStatus.DITOLAK }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "$pendingCount", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = StatusAmber)
                            Text(text = "Menunggu", fontSize = 11.sp, color = SecondarySlate)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "$approvedCount", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = StatusGreen)
                            Text(text = "Disetujui", fontSize = 11.sp, color = SecondarySlate)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "$rejectedCount", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = StatusRed)
                            Text(text = "Ditolak", fontSize = 11.sp, color = SecondarySlate)
                        }
                    }
                }
            }
        }
    }

    if (showExportSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showExportSuccessDialog = false },
            icon = { Icon(Icons.Default.DownloadDone, contentDescription = null, tint = StatusGreen) },
            title = { Text("Ekspor CSV Berhasil!") },
            text = {
                Column {
                    Text("Data laporan objek berhasil di-generate dan disalin ke clipboard.")
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = SurfaceVariantLight,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = exportedCsvText.lines().take(5).joinToString("\n") + "\n...",
                            fontSize = 10.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showExportSuccessDialog = false }) {
                    Text("Selesai")
                }
            }
        )
    }
}
