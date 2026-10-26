package com.example.ui.screens.mentoring

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*

// -------------------------------------------------------------------------------------
// TAHAP 3 KEPUTUSAN SEIRI
// -------------------------------------------------------------------------------------
@Composable
internal fun Stage3SeiriDecisions(
    session: MentoringSession,
    onUpdateInspection: (List<SessionObjectInspection>) -> Unit,
    onNext: () -> Unit,
    onSaveDraftAndExit: () -> Unit,
    onPrev: () -> Unit
) {
    val inspections = session.inspections
    val decisionOptions = listOf(
        Pair("Diperlukan (Hijau)", StatusGreen),
        Pair("Dipindahkan", PrimaryBlue),
        Pair("Tidak Diperlukan / Red Tag (Merah)", StatusRed),
        Pair("Perlu Konfirmasi (Kuning)", StatusAmber)
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Tahap 3 Keputusan Seiri",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Tentukan status akhir setiap objek dan berikan justifikasi keputusan.",
                    fontSize = 11.sp,
                    color = SecondarySlate
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            itemsIndexed(inspections) { idx, item ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${idx + 1}. ${item.objectName}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(color = SurfaceVariantLight, shape = RoundedCornerShape(4.dp)) {
                                Text(
                                    text = item.objectCode,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = "Penilaian Awal: ${item.initialAssessment} â€¢ Frekuensi: ${item.usageFrequency}",
                            fontSize = 11.sp,
                            color = SecondarySlate
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = "Pilih Keputusan Seiri *", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)

                        // 4 decision chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            decisionOptions.forEach { (dec, color) ->
                                FilterChip(
                                    selected = item.seiriDecision == dec,
                                    onClick = {
                                        val updated = inspections.toMutableList()
                                        updated[idx] = item.copy(seiriDecision = dec, isStage3Checked = true)
                                        onUpdateInspection(updated)
                                    },
                                    label = { Text(dec, fontSize = 11.sp) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = item.decisionReason,
                            onValueChange = { newReason ->
                                val updated = inspections.toMutableList()
                                updated[idx] = item.copy(decisionReason = newReason, isStage3Checked = true)
                                onUpdateInspection(updated)
                            },
                            label = { Text("Alasan Justifikasi Keputusan *") },
                            placeholder = { Text("Contoh: Mesin masih beroperasi aktif / barang tidak lagi terpakai") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Bottom Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onSaveDraftAndExit,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Simpan Draft & Keluar", fontSize = 12.sp)
                }

                Button(
                    onClick = onNext,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1.5f)
                ) {
                    Text("Lanjut: Tahap 4 (PIC Tindakan)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}