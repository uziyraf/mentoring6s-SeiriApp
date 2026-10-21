package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SeiriRepository
import com.example.model.ObjectChangeLog
import com.example.ui.theme.*

@Composable
fun AuditLogScreen(
    repository: SeiriRepository
) {
    val changeLogs by repository.changeLogs.collectAsState()
    var filterType by remember { mutableStateOf<String?>(null) }

    val filteredLogs = if (filterType == null) changeLogs else changeLogs.filter { it.changeType == filterType }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Audit Log & Riwayat Perubahan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Jejak audit lengkap mencatat setiap penambahan, koreksi master data, mutasi kepemilikan, dan validasi Seiri.",
                    fontSize = 12.sp,
                    color = SecondarySlate
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = filterType == null,
                        onClick = { filterType = null },
                        label = { Text("Semua (${changeLogs.size})") }
                    )
                    FilterChip(
                        selected = filterType == "VALIDATION",
                        onClick = { filterType = if (filterType == "VALIDATION") null else "VALIDATION" },
                        label = { Text("Validasi") }
                    )
                    FilterChip(
                        selected = filterType == "TRANSFER",
                        onClick = { filterType = if (filterType == "TRANSFER") null else "TRANSFER" },
                        label = { Text("Mutasi Aset") }
                    )
                    FilterChip(
                        selected = filterType == "CREATE",
                        onClick = { filterType = if (filterType == "CREATE") null else "CREATE" },
                        label = { Text("Tambah Baru") }
                    )
                }
            }
        }

        // List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredLogs, key = { it.id }) { log ->
                val (badgeColor, badgeText) = when (log.changeType) {
                    "CREATE" -> Pair(StatusGreen, "TAMBAH")
                    "UPDATE" -> Pair(PrimaryBlue, "KOREKSI")
                    "TRANSFER" -> Pair(StatusPurple, "MUTASI")
                    "TRANSFER_REQ" -> Pair(StatusAmber, "PENGAJUAN")
                    "TRANSFER_REJECTED" -> Pair(StatusRed, "DITOLAK")
                    "DEACTIVATE" -> Pair(StatusRed, "NONAKTIF")
                    "REACTIVATE" -> Pair(StatusGreen, "REAKTIVASI")
                    "VALIDATION" -> Pair(StatusGreen, "VALIDASI")
                    "IMPORT", "IMPORT_BATCH" -> Pair(PrimaryBlue, "IMPOR")
                    else -> Pair(SecondarySlate, log.changeType)
                }

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(badgeColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (log.changeType) {
                                    "TRANSFER", "TRANSFER_REQ" -> Icons.Default.SwapHoriz
                                    "VALIDATION" -> Icons.Default.CheckCircle
                                    "DEACTIVATE" -> Icons.Default.DeleteOutline
                                    else -> Icons.Default.History
                                },
                                contentDescription = null,
                                tint = badgeColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = badgeColor.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = badgeText,
                                            color = badgeColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = log.objectCode,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = IndustrialNavy
                                    )
                                }

                                Text(
                                    text = log.changedAt,
                                    fontSize = 10.sp,
                                    color = SecondarySlate
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = log.objectName,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )

                            Spacer(modifier = Modifier.height(4.dp))
                            if (log.oldValues != "-") {
                                Text(
                                    text = "Sebelum: ${log.oldValues}",
                                    fontSize = 11.sp,
                                    color = SecondarySlate
                                )
                            }
                            Text(
                                text = "Sesudah: ${log.newValues}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = IndustrialNavy
                            )

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Oleh: ${log.changedBy}",
                                fontSize = 10.sp,
                                color = SecondarySlateLight
                            )
                        }
                    }
                }
            }
        }
    }
}
