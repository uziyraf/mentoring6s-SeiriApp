package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SeiriRepository
import com.example.data.service.ImportCsvService
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpreadsheetImportScreen(
    repository: SeiriRepository,
    onNavigateBack: () -> Unit
) {
    val currentUser by repository.currentUser.collectAsState()
    val areas by repository.areas.collectAsState()
    val zones by repository.zones.collectAsState()
    val objects by repository.objects.collectAsState()

    // 4 Steps: 1. Input CSV, 2. Column Mapping & Preview, 3. Confirmation Summary, 4. Final Report
    var currentStep by remember { mutableStateOf(1) }

    val defaultSampleCsv = """
Kode,Nama,Area,Zona,Kategori,Fungsi,PIC
OBJ-MC-050,"Ragum Mesin Milling 6 Inchi",MC,MC-Z1,Fixture,"Penjepit benda kerja milling",Andi Pratama
OBJ-AS-030,"Heat Gun Solder Uap 600W",AS,AS-Z2,Power Tool,"Pemanas shrink tubing kabel",Budi Santoso
OBJ-QC-020,"Mikrometer Kedalaman 0-25mm",QC,QC-Z2,Alat Ukur,"Pengukur kedalaman lubang bor",Citra Dewi
OBJ-WH-015,"Trolley Drum Oli Hidrolik",WH,WH-Z2,Handling,"Pengangkut drum oli 200L",Doni Wijaya
OBJ-MC-001,"Kunci Pas Chuck CNC 24mm (Duplikat)",MC,MC-Z1,Perkakas,"Kunci pas collet chuck",Andi Pratama
OBJ-ERR-001,"Barang Tanpa Area Valid",XX,XX-Z1,Lainnya,"Barang uji coba error",Operator
""".trimIndent()

    var csvText by remember { mutableStateOf(defaultSampleCsv) }
    var previewRows by remember { mutableStateOf<List<ImportCsvService.ImportPreviewRow>>(emptyList()) }
    var importResult by remember { mutableStateOf<ImportCsvService.ImportResult?>(null) }
    var activeTab by remember { mutableStateOf(0) } // 0: Semua, 1: Valid, 2: Duplikat, 3: Error

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Wizard Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Impor Spreadsheet Data Master Objek",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        color = PrimaryBlueContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Tahap $currentStep dari 4",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlueDark,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (step in 1..4) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .background(
                                    if (step <= currentStep) PrimaryBlue else OutlineLight,
                                    RoundedCornerShape(2.dp)
                                )
                        )
                    }
                }
            }
        }

        // CONTENT BY STEP
        when (currentStep) {
            1 -> {
                // STEP 1: CSV INPUT / UPLOAD
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "1. Masukkan Berkas CSV atau Salin Data Spreadsheet",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Format kolom yang didukung: Kode, Nama, Area, Zona, Kategori, Fungsi, PIC.",
                                    fontSize = 12.sp,
                                    color = SecondarySlate
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Surface(
                                    color = SurfaceVariantLight,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Info, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Fitur impor native CSV siap digunakan. Parser Excel (.xlsx) akan dikonversi ke format CSV saat sinkronisasi backend.",
                                            fontSize = 11.sp,
                                            color = SecondarySlate
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = csvText,
                                    onValueChange = { csvText = it },
                                    label = { Text("Teks CSV Data Master Objek") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                    maxLines = 10
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    TextButton(onClick = { csvText = defaultSampleCsv }) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Muat Contoh CSV Pabrik", fontSize = 12.sp)
                                    }
                                    TextButton(onClick = { csvText = "" }) {
                                        Text("Bersihkan", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Button(
                            onClick = {
                                previewRows = ImportCsvService.parseAndPreviewCsv(csvText, areas, zones, objects)
                                currentStep = 2
                            },
                            enabled = csvText.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("import_step1_next_button")
                        ) {
                            Text("Pratinjau & Validasi Data (Langkah 2)", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = null)
                        }
                    }
                }
            }

            2 -> {
                // STEP 2: PREVIEW & MAPPING
                val validRows = previewRows.filter { it.isValid }
                val duplicateRows = previewRows.filter { it.isDuplicate }
                val invalidRows = previewRows.filter { !it.isValid && !it.isDuplicate }

                val displayedRows = when (activeTab) {
                    1 -> validRows
                    2 -> duplicateRows
                    3 -> invalidRows
                    else -> previewRows
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    // Filter Tabs
                    TabRow(selectedTabIndex = activeTab) {
                        Tab(
                            selected = activeTab == 0,
                            onClick = { activeTab = 0 },
                            text = { Text("Semua (${previewRows.size})", fontSize = 11.sp) }
                        )
                        Tab(
                            selected = activeTab == 1,
                            onClick = { activeTab = 1 },
                            text = { Text("Valid (${validRows.size})", fontSize = 11.sp, color = StatusGreen) }
                        )
                        Tab(
                            selected = activeTab == 2,
                            onClick = { activeTab = 2 },
                            text = { Text("Duplikat (${duplicateRows.size})", fontSize = 11.sp, color = StatusAmber) }
                        )
                        Tab(
                            selected = activeTab == 3,
                            onClick = { activeTab = 3 },
                            text = { Text("Error (${invalidRows.size})", fontSize = 11.sp, color = StatusRed) }
                        )
                    }

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(displayedRows) { row ->
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (row.isDuplicate) StatusAmberContainer.copy(alpha = 0.5f)
                                    else if (!row.isValid) StatusRedContainer.copy(alpha = 0.5f)
                                    else MaterialTheme.colorScheme.surface
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Baris #${row.rowIndex}: ${row.rawCode}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                        if (row.isDuplicate) {
                                            Surface(color = StatusAmber, shape = RoundedCornerShape(4.dp)) {
                                                Text("DUPLIKAT", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                            }
                                        } else if (row.isValid) {
                                            Surface(color = StatusGreen, shape = RoundedCornerShape(4.dp)) {
                                                Text("VALID", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                            }
                                        } else {
                                            Surface(color = StatusRed, shape = RoundedCornerShape(4.dp)) {
                                                Text("ERROR", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = row.rawName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text(
                                        text = "Area: ${row.matchedArea?.areaName ?: "Tidak Cocok ('${row.rawAreaCode}')"} â€¢ Zona: ${row.matchedZone?.zoneName ?: "Zona Default"} â€¢ PIC: ${row.rawPic}",
                                        fontSize = 11.sp,
                                        color = SecondarySlate
                                    )

                                    if (row.errors.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Masalah: ${row.errors.joinToString("; ")}",
                                            fontSize = 11.sp,
                                            color = StatusRed,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Navigation Buttons
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
                                onClick = { currentStep = 1 },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Kembali")
                            }

                            Button(
                                onClick = { currentStep = 3 },
                                enabled = validRows.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                modifier = Modifier.weight(2f)
                            ) {
                                Text("Lanjut ke Konfirmasi (${validRows.size} Objek)")
                            }
                        }
                    }
                }
            }

            3 -> {
                // STEP 3: CONFIRMATION SUMMARY
                val validRows = previewRows.filter { it.isValid }
                val duplicateRows = previewRows.filter { it.isDuplicate }
                val invalidRows = previewRows.filter { !it.isValid && !it.isDuplicate }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "3. Ringkasan & Konfirmasi Impor",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Total Baris Diproses:", fontSize = 12.sp, color = SecondarySlate)
                                    Text("${previewRows.size} baris", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Objek Valid (Akan Diimpor):", fontSize = 12.sp, color = StatusGreen)
                                    Text("${validRows.size} objek", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StatusGreen)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Duplikat (Dilewati):", fontSize = 12.sp, color = StatusAmber)
                                    Text("${duplicateRows.size} objek", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StatusAmber)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Data Error (Dilewati):", fontSize = 12.sp, color = StatusRed)
                                    Text("${invalidRows.size} objek", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StatusRed)
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                Surface(
                                    color = PrimaryBlueContainer,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Setelah impor berhasil, database aplikasi akan menjadi sumber kebenaran (source of truth). Kode sumber spreadsheet akan disimpan di source_object_id.",
                                        fontSize = 11.sp,
                                        color = PrimaryBlueDark,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = { currentStep = 2 },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Kembali")
                            }

                            Button(
                                onClick = {
                                    val result = repository.executeImport(previewRows)
                                    importResult = result
                                    currentStep = 4
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                                modifier = Modifier.weight(2f)
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Terapkan Impor Sekarang", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            4 -> {
                // STEP 4: FINAL REPORT
                val res = importResult
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = StatusGreenContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = StatusGreen,
                                    modifier = Modifier.size(54.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Impor Spreadsheet Selesai!",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF14532D)
                                )
                                Text(
                                    text = "Data berhasil dimasukkan ke Master Objek dan tercatat di Audit Log.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF166534)
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(text = "${res?.importedCount ?: 0}", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = StatusGreen)
                                        Text(text = "Berhasil", fontSize = 11.sp, color = Color(0xFF166534))
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(text = "${res?.duplicateCount ?: 0}", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = StatusAmber)
                                        Text(text = "Duplikat", fontSize = 11.sp, color = Color(0xFF92400E))
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(text = "${res?.errorCount ?: 0}", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = StatusRed)
                                        Text(text = "Error", fontSize = 11.sp, color = Color(0xFF991B1B))
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Rincian Log Impor",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = SecondarySlate
                        )
                    }

                    items(res?.logs ?: emptyList()) { logLine ->
                        Surface(
                            color = SurfaceVariantLight,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = logLine,
                                fontSize = 11.sp,
                                color = IndustrialNavy,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    item {
                        Button(
                            onClick = onNavigateBack,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text("Buka Master Objek", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
