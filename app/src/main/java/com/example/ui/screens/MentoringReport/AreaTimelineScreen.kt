package com.example.ui.screens.MentoringReport

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AreaMentoringGroup
import com.example.model.MentoringReport
import com.example.ui.theme.*

@Composable
fun AreaTimelineScreen(
    group: AreaMentoringGroup,
    onBack: () -> Unit,
    onAddNewNote: () -> Unit,
    onEditNote: (MentoringReport) -> Unit,
    onDeleteNote: (MentoringReport) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        // Header
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                }
                Column {
                    Text(group.areaName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Zona: ${group.zoneName}", style = MaterialTheme.typography.labelSmall, color = SecondarySlate)
                }
            }
        }

        // List Timeline
        LazyColumn(
            modifier = Modifier.weight(1f).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(group.reports) { report ->
                Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(report.createdAt, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                            Text(report.mentorName, fontSize = 12.sp, color = SecondarySlate)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("Audiens: ${report.audience}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        
                        HorizontalDivider(Modifier.padding(vertical = 8.dp), color = OutlineLight)
                        
                        Text("Aktivitas: ${report.activityDescription}", fontSize = 13.sp)
                        Text("Hasil: ${report.result}", fontSize = 13.sp)
                        Text("Kendala: ${report.obstacles}", fontSize = 13.sp)
                        
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { onEditNote(report) }) { Text("Edit", fontSize = 12.sp) }
                            TextButton(onClick = { onDeleteNote(report) }) { Text("Hapus", fontSize = 12.sp, color = StatusRed) }
                        }
                    }
                }
            }
        }

        // Tombol Tambah
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 4.dp) {
            Button(
                onClick = onAddNewNote,
                modifier = Modifier.fillMaxWidth().padding(16.dp).height(48.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Tambah Catatan Kunjungan", fontWeight = FontWeight.Bold)
            }
        }
    }
}