package com.example.ui.screens.mentoring

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SeiriRepository
import com.example.model.*
import com.example.ui.theme.*


// -------------------------------------------------------------------------------------
// TAHAP 5 REVIEW & SIMPAN FINAL
// -------------------------------------------------------------------------------------
@Composable
internal fun Stage5ReviewAndFinalSubmit(
    session: MentoringSession,
    repository: SeiriRepository,
    onShowSchema: () -> Unit,
    onSubmitFinal: () -> Unit,
    onSaveDraftAndExit: () -> Unit,
    onBackToStage: (Int) -> Unit
) {
    val inspections = session.inspections
    val countDiperlukan = inspections.count { it.seiriDecision.contains("Diperlukan") }
    val countRedTag = inspections.count { it.seiriDecision.contains("Red Tag") || it.seiriDecision.contains("Tidak Diperlukan") }
    val countPindah = inspections.count { it.seiriDecision.contains("Dipindahkan") }
    val countKonfirmasi = inspections.count { it.seiriDecision.contains("Konfirmasi") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Tahap 5 â€” Review & Simpan Final",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Periksa ringkasan hasil mentoring Seiri sebelum menyelesaikan dan mengunci sesi.",
                fontSize = 12.sp,
                color = SecondarySlate
            )
        }

        // Summary Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Ringkasan Keputusan Seiri", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "$countDiperlukan", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = StatusGreen)
                            Text(text = "Diperlukan", fontSize = 11.sp, color = SecondarySlate)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "$countRedTag", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = StatusRed)
                            Text(text = "Red Tag", fontSize = 11.sp, color = SecondarySlate)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "$countPindah", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = PrimaryBlue)
                            Text(text = "Dipindahkan", fontSize = 11.sp, color = SecondarySlate)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "$countKonfirmasi", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = StatusAmber)
                            Text(text = "Konfirmasi", fontSize = 11.sp, color = SecondarySlate)
                        }
                    }
                }
            }
        }

        // Action items / Red Tag list
        val redTagItems = inspections.filter { it.seiriDecision.contains("Red Tag") || it.seiriDecision.contains("Dipindahkan") }
        if (redTagItems.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Daftar Tugas / Action Items Red Tag", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))

                        redTagItems.forEach { itm ->
                            Surface(
                                color = SurfaceVariantLight,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(text = "${itm.objectName} (${itm.objectCode})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text(text = "Tindakan: ${itm.actionDescription.ifEmpty { "Perlu disposisi / mutasi" }}", fontSize = 11.sp)
                                    Text(text = "PIC: ${itm.pic.ifEmpty { "Operator Area" }} â€¢ Tenggat: ${itm.dueDate.ifEmpty { "Segera" }}", fontSize = 10.sp, color = SecondarySlate)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Schema / JSON button
        item {
            FilledTonalButton(
                onClick = onShowSchema,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Lihat Struktur Data JSON & Skema Google Sheets", fontSize = 12.sp)
            }
        }

        // Final Buttons
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onSubmitFinal,
                    colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("submit_final_session_button")
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Submit Final (Kunci Sesi)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                OutlinedButton(
                    onClick = onSaveDraftAndExit,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Simpan Sebagai Draft & Keluar")
                }

                TextButton(
                    onClick = { onBackToStage(2) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Kembali Periksa Data")
                }
            }
        }
    }
}