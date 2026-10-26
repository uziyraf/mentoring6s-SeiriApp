package com.example.ui.screens.mentoring

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*

// -------------------------------------------------------------------------------------
// TAHAP 4 â€” PIC DAN TINDAKAN (KHUSUS RED TAG / DIPINDAHKAN)
// -------------------------------------------------------------------------------------
@Composable
internal fun Stage4ActionPlans(
    session: MentoringSession,
    onUpdateInspection: (List<SessionObjectInspection>) -> Unit,
    onNext: () -> Unit,
    onSaveDraftAndExit: () -> Unit,
    onPrev: () -> Unit
) {
    val inspections = session.inspections
    // Filter objects that need action (Red Tag / Dipindahkan)
    val actionItems = inspections.filter {
        it.seiriDecision.contains("Red Tag") ||
        it.seiriDecision.contains("Tidak Diperlukan") ||
        it.seiriDecision.contains("Dipindahkan")
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Tahap 4 â€” PIC dan Tindakan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Khusus objek bertanda Red Tag / Dipindahkan, tentukan tindakan, PIC, dan batas waktu.",
                    fontSize = 11.sp,
                    color = SecondarySlate
                )
            }
        }

        if (actionItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = StatusGreenContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(54.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Tidak Ada Objek Red Tag / Dipindahkan",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF14532D)
                        )
                        Text(
                            text = "Semua objek di zona ini dinilai Diperlukan. Anda dapat langsung melanjutkan ke Tahap 5.",
                            fontSize = 12.sp,
                            color = Color(0xFF166534)
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(actionItems) { item ->
                    val globalIdx = inspections.indexOfFirst { it.objectId == item.objectId }
                    val isRedTag = item.seiriDecision.contains("Red Tag")

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
                                    text = item.objectName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    color = if (isRedTag) StatusRedContainer else StatusBlueContainer,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = item.seiriDecision,
                                        color = if (isRedTag) StatusRed else PrimaryBlue,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = item.actionDescription,
                                onValueChange = { newDesc ->
                                    if (globalIdx != -1) {
                                        val updated = inspections.toMutableList()
                                        updated[globalIdx] = item.copy(actionDescription = newDesc, isStage4Checked = true)
                                        onUpdateInspection(updated)
                                    }
                                },
                                label = { Text("Deskripsi Rencana Tindakan *") },
                                placeholder = { Text("Contoh: Pindahkan ke tempat penampungan limbah/Gudang B") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = item.pic,
                                    onValueChange = { newPic ->
                                        if (globalIdx != -1) {
                                            val updated = inspections.toMutableList()
                                            updated[globalIdx] = item.copy(pic = newPic, isStage4Checked = true)
                                            onUpdateInspection(updated)
                                        }
                                    },
                                    label = { Text("PIC Penanggung Jawab *") },
                                    modifier = Modifier.weight(1f)
                                )

                                OutlinedTextField(
                                    value = item.dueDate,
                                    onValueChange = { newDue ->
                                        if (globalIdx != -1) {
                                            val updated = inspections.toMutableList()
                                            updated[globalIdx] = item.copy(dueDate = newDue, isStage4Checked = true)
                                            onUpdateInspection(updated)
                                        }
                                    },
                                    label = { Text("Tenggat Waktu *") },
                                    placeholder = { Text("YYYY-MM-DD") },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Upload Foto Bukti Selesai (Opsional)
                            Surface(
                                color = SurfaceVariantLight,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (globalIdx != -1) {
                                            val updated = inspections.toMutableList()
                                            val newPhoto = if (item.completionPhotoUri == null) "bukti_tindakan_${item.objectCode}.jpg" else null
                                            updated[globalIdx] = item.copy(completionPhotoUri = newPhoto, isStage4Checked = true)
                                            onUpdateInspection(updated)
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (item.completionPhotoUri != null) Icons.Default.CheckCircle else Icons.Default.AddAPhoto,
                                        contentDescription = null,
                                        tint = if (item.completionPhotoUri != null) StatusGreen else PrimaryBlue,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (item.completionPhotoUri != null) "Foto Bukti Selesai Terunggah" else "Upload Foto Bukti Selesai (Opsional)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (item.completionPhotoUri != null) StatusGreen else IndustrialNavy
                                        )
                                        Text(
                                            text = if (item.completionPhotoUri != null) "${item.completionPhotoUri} (Klik untuk ganti/hapus)" else "Ambil foto atau lampirkan bukti visual perbaikan",
                                            fontSize = 10.sp,
                                            color = SecondarySlate
                                        )
                                    }
                                }
                            }
                        }
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
                    Text("Lanjut: Tahap 5 (Review Final)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}