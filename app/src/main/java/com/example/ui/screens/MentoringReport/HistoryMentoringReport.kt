package com.example.ui.screens.MentoringReport

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.MentoringReport
import com.example.model.MentoringReportRepository
import com.example.ui.theme.*

/**
 * Histori laporan mentoring: list + aksi hapus.
 * Logic & UI histori sengaja ditaruh di sini, dipanggil dari MentoringReportScreen.
 */
@Composable
fun HistoryMentoringReport(
    onCreate: () -> Unit,
    onEdit: (MentoringReport) -> Unit
) {
    val reports by MentoringReportRepository.reports.collectAsState()
    var pendingDelete by remember { mutableStateOf<MentoringReport?>(null) }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Histori Laporan Mentoring",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${reports.size} laporan",
                    style = MaterialTheme.typography.labelSmall,
                    color = SecondarySlate
                )
            }
            FilledTonalButton(onClick = onCreate) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Baru", style = MaterialTheme.typography.labelLarge)
            }
        }

        Spacer(Modifier.height(8.dp))

        if (reports.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Assignment,
                        contentDescription = null,
                        tint = SecondarySlateLight,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Belum ada laporan.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondarySlate
                    )
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(reports, key = { it.id }) { report ->
                    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(report.mentorName, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${report.plant} > ${report.areaName} > ${report.zoneName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = PrimaryBlueDark
                            )
                            Text(
                                "PIC: ${report.pic}",
                                style = MaterialTheme.typography.bodySmall,
                                color = SecondarySlate
                            )
                            Text(
                                "Audiens: ${report.audience}",
                                style = MaterialTheme.typography.bodySmall,
                                color = SecondarySlate
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(report.activityDescription, style = MaterialTheme.typography.bodyMedium)
                            Text("Hasil: ${report.result}", style = MaterialTheme.typography.bodyMedium)
                            Text("Kendala: ${report.obstacles}", style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                report.createdAt,
                                style = MaterialTheme.typography.labelSmall,
                                color = SecondarySlateLight
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(onClick = { onEdit(report) }) {
                                    Text("Edit", style = MaterialTheme.typography.labelMedium)
                                }
                                TextButton(onClick = { pendingDelete = report }) {
                                    Text(
                                        "Hapus",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = StatusRed
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Hapus Laporan?", style = MaterialTheme.typography.titleMedium) },
            text = {
                Text(
                    "Laporan ${target.mentorName} (${target.createdAt}) akan dihapus permanen.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    MentoringReportRepository.deleteReport(target.id)
                    pendingDelete = null
                }) {
                    Text("Hapus", style = MaterialTheme.typography.labelLarge, color = StatusRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Batal", style = MaterialTheme.typography.labelLarge)
                }
            }
        )
    }
}
