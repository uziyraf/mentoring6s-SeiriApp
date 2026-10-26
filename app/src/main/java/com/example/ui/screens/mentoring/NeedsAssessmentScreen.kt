package com.example.ui.screens.mentoring

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*

// -------------------------------------------------------------------------------------
// TAHAP 2 PERIKSA KEBUTUHAN (DIISI PER OBJEK)
// -------------------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun Stage2NeedsAssessment(
    session: MentoringSession,
    onUpdateInspection: (List<SessionObjectInspection>) -> Unit,
    onNext: () -> Unit,
    onSaveDraftAndExit: () -> Unit,
    onPrev: () -> Unit
) {
    val inspections = session.inspections
    var selectedIndex by remember { mutableStateOf(0) }
    val currentItem = inspections.getOrNull(selectedIndex)

    Column(modifier = Modifier.fillMaxSize()) {
        // Sticky Header: Tabs of objects
        Surface(C
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tahap 2 â€” Periksa Kebutuhan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Objek ${selectedIndex + 1} dari ${inspections.size}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryBlue
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Object quick tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    inspections.forEachIndexed { idx, item ->
                        FilterChip(
                            selected = selectedIndex == idx,
                            onClick = { selectedIndex = idx },
                            label = { Text(item.objectName.take(16)) },
                            leadingIcon = {
                                if (item.isStage2Checked) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp), tint = StatusGreen)
                                }
                            }
                        )
                    }
                }
            }
        }

        // Form for current object
        if (currentItem != null) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Object identification banner
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = PrimaryBlueContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Surface(color = Color.White, shape = RoundedCornerShape(4.dp)) {
                                Text(
                                    text = currentItem.objectCode,
                                    color = PrimaryBlueDark,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = currentItem.objectName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlueDark
                            )
                        }
                    }
                }

                // 1. Input Fungsi Objek
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "1. Fungsi Objek *", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(text = "Jelaskan peran objek ini dalam alur kerja produksi", fontSize = 11.sp, color = SecondarySlate)
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = currentItem.function,
                                onValueChange = { newFunc ->
                                    val updated = inspections.toMutableList()
                                    updated[selectedIndex] = currentItem.copy(function = newFunc, isStage2Checked = true)
                                    onUpdateInspection(updated)
                                },
                                placeholder = { Text("Contoh: Menampung dan mengalirkan resin PET ke barrel") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // 2. Dropdown Frekuensi Penggunaan
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "2. Frekuensi Penggunaan *", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(text = "Seberapa sering objek ini digunakan oleh operator", fontSize = 11.sp, color = SecondarySlate)
                            Spacer(modifier = Modifier.height(8.dp))

                            val frequencies = listOf("Setiap Hari/Shift", "Mingguan", "Bulanan", "Tidak Pernah")
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                frequencies.forEach { freq ->
                                    FilterChip(
                                        selected = currentItem.usageFrequency == freq,
                                        onClick = {
                                            val updated = inspections.toMutableList()
                                            updated[selectedIndex] = currentItem.copy(usageFrequency = freq, isStage2Checked = true)
                                            onUpdateInspection(updated)
                                        },
                                        label = { Text(freq) }
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Input Kapan Terakhir Digunakan
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "3. Kapan Terakhir Digunakan *", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = currentItem.lastUsed,
                                onValueChange = { newLast ->
                                    val updated = inspections.toMutableList()
                                    updated[selectedIndex] = currentItem.copy(lastUsed = newLast, isStage2Checked = true)
                                    onUpdateInspection(updated)
                                },
                                placeholder = { Text("Contoh: Hari ini, 3 hari lalu, atau YYYY-MM-DD") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // 4. Radio Button Penilaian Awal
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "4. Penilaian Awal Kebutuhan *", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(8.dp))

                            val assessmentOptions = listOf("Masih Diperlukan", "Tidak Diperlukan")
                            assessmentOptions.forEach { opt ->
                                Surface(
                                    color = if (currentItem.initialAssessment == opt) PrimaryBlueContainer else SurfaceVariantLight,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            val updated = inspections.toMutableList()
                                            val decision = if (opt == "Masih Diperlukan") "Diperlukan (Hijau)" else "Tidak Diperlukan / Red Tag (Merah)"
                                            updated[selectedIndex] = currentItem.copy(
                                                initialAssessment = opt,
                                                seiriDecision = decision,
                                                isStage2Checked = true
                                            )
                                            onUpdateInspection(updated)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = currentItem.initialAssessment == opt,
                                            onClick = {
                                                val updated = inspections.toMutableList()
                                                val decision = if (opt == "Masih Diperlukan") "Diperlukan (Hijau)" else "Tidak Diperlukan / Red Tag (Merah)"
                                                updated[selectedIndex] = currentItem.copy(
                                                    initialAssessment = opt,
                                                    seiriDecision = decision,
                                                    isStage2Checked = true
                                                )
                                                onUpdateInspection(updated)
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = opt, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. Upload / Ambil Foto Objek Saat Ini
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "5. Foto Fisik Objek Saat Ini (Kamera/Upload)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(8.dp))

                            Surface(
                                color = SurfaceVariantLight,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                                    .clickable {
                                        // Toggle mock photo capture
                                        val updated = inspections.toMutableList()
                                        val newPhoto = if (currentItem.photoUri == null) "foto_${currentItem.objectCode}.jpg" else null
                                        updated[selectedIndex] = currentItem.copy(photoUri = newPhoto, isStage2Checked = true)
                                        onUpdateInspection(updated)
                                    }
                            ) {
                                if (currentItem.photoUri != null) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Image, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(36.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(text = "Foto Objek Tersimpan", fontWeight = FontWeight.Bold, color = StatusGreen)
                                            Text(text = "${currentItem.photoUri} â€¢ Klik untuk hapus/ganti", fontSize = 11.sp, color = SecondarySlate)
                                        }
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(28.dp))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = "Ketuk untuk Ambil Foto / Upload", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PrimaryBlueDark)
                                        Text(text = "Dokumentasi visual kondisi lapangan", fontSize = 10.sp, color = SecondarySlate)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Action Bar: Draft & Exit + Navigation
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onSaveDraftAndExit,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Simpan Draft & Keluar", fontSize = 11.sp)
                    }

                    if (selectedIndex < inspections.size - 1) {
                        Button(
                            onClick = { selectedIndex++ },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Objek Berikutnya", fontSize = 12.sp)
                        }
                    } else {
                        Button(
                            onClick = onNext,
                            colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Lanjut Tahap 3", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

