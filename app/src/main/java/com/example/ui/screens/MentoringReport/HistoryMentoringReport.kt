package com.example.ui.screens.MentoringReport

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AreaMentoringGroup
import com.example.model.MentoringReportRepository
import com.example.ui.theme.*

@Composable
fun HistoryMentoringReport(
    onCreateNewForArea: () -> Unit,
    onOpenTimeline: (AreaMentoringGroup) -> Unit
) {
    val reports by MentoringReportRepository.reports.collectAsState()

    // Mengelompokkan laporan berdasarkan Area + Zona
    val groupedAreas = remember(reports) {
        reports.groupBy { "${it.areaId}|${it.zoneId}" }
            .map { (_, list) ->
                val first = list.first()
                AreaMentoringGroup(
                    areaId = first.areaId,
                    areaName = first.areaName,
                    zoneId = first.zoneId,
                    zoneName = first.zoneName,
                    reports = list.sortedByDescending { it.createdAt }
                )
            }.sortedByDescending { it.lastVisited }
    }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Riwayat Area Mentoring", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("${groupedAreas.size} Area Terdata", style = MaterialTheme.typography.labelSmall, color = SecondarySlate)
            }
            FilledTonalButton(onClick = onCreateNewForArea) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Kunjungan Baru")
            }
        }

        Spacer(Modifier.height(12.dp))

        if (groupedAreas.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Belum ada area yang pernah dikunjungi.", color = SecondarySlate)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(groupedAreas, key = { it.areaId + it.zoneId }) { group ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().clickable { onOpenTimeline(group) }
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(group.areaName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Surface(color = PrimaryBlueContainer, shape = RoundedCornerShape(6.dp)) {
                                    Text("${group.totalVisits} Kunjungan", fontSize = 10.sp, color = PrimaryBlueDark, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                            Text(group.zoneName, style = MaterialTheme.typography.bodySmall, color = PrimaryBlue)
                            Spacer(Modifier.height(8.dp))
                            Text("Kunjungan Terakhir: ${group.lastVisited}", fontSize = 11.sp, color = SecondarySlate)
                        }
                    }
                }
            }
        }
    }
}