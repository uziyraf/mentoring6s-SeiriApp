package com.example.ui.screens.mentoring

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SeiriRepository
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*

// -------------------------------------------------------------------------------------
// TAHAP 1 â€” IDENTIFIKASI OBJEK
// -------------------------------------------------------------------------------------


@Composable
internal fun Stage1ObjectIdentification(
    session: MentoringSession,
    repository: SeiriRepository,
    onAddNewObject: () -> Unit,
    onNext: () -> Unit,
    onSaveDraftAndExit: () -> Unit
) {
    val inspections = session.inspections
    var searchQuery by remember { mutableStateOf("") }
    var filterMode by remember { mutableStateOf("ALL") } // ALL, UNCHECKED, CHECKED

    // Search filter across area name or object name/code/function
    val filteredInspections = inspections.filter { item ->
        val areaName = repository.getAreaName(session.areaId)
        val zoneName = repository.getZoneName(session.zoneId)
        val matchesSearch = searchQuery.isBlank() ||
                item.objectName.contains(searchQuery, ignoreCase = true) ||
                item.objectCode.contains(searchQuery, ignoreCase = true) ||
                item.function.contains(searchQuery, ignoreCase = true) ||
                item.pic.contains(searchQuery, ignoreCase = true) ||
                areaName.contains(searchQuery, ignoreCase = true) ||
                zoneName.contains(searchQuery, ignoreCase = true)

        val matchesFilter = when (filterMode) {
            "UNCHECKED" -> !item.isStage2Checked
            "CHECKED" -> item.isStage2Checked
            else -> true
        }

        matchesSearch && matchesFilter
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Sticky Header
        
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            )
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                // Header tahap dan tombol tambah
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Tahap 1 Identifikasi Objek",
                            fontSize = 14.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = "${inspections.size} objek terdata",
                            fontSize = 11.sp,
                            color = SecondarySlate,
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Tombol tambah lebih kecil dan proporsional
                    FilledTonalButton(
                        onClick = onAddNewObject,
                        modifier = Modifier.height(34.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(
                            horizontal = 10.dp,
                            vertical = 0.dp
                        ),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = PrimaryBlue,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Tambah objek",
                            modifier = Modifier.size(15.dp)
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        Text(
                            text = "Tambah",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Kolom pencarian
                AreaOrObjectSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = "Cari area atau objek...",
                    modifier = Modifier.fillMaxWidth()
                )

                // Filter status
                val filters = listOf(
                    Triple(
                        "ALL",
                        "Semua",
                        inspections.size
                    ),
                    Triple(
                        "UNCHECKED",
                        "Belum",
                        inspections.count { !it.isStage2Checked }
                    ),
                    Triple(
                        "CHECKED",
                        "Sudah",
                        inspections.count { it.isStage2Checked }
                    )
                )

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(
                        items = filters,
                        key = { it.first }
                    ) { (key, label, count) ->

                        val selected = filterMode == key

                        Surface(
                            onClick = { filterMode = key },
                            shape = RoundedCornerShape(7.dp),
                            color = if (selected) {
                                PrimaryBlue
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(
                                    alpha = 0.65f
                                )
                            },
                            border = if (selected) null else BorderStroke(
                                0.5.dp,
                                MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = if (selected) {
                                        FontWeight.SemiBold
                                    } else {
                                        FontWeight.Medium
                                    },
                                    color = if (selected) {
                                        Color.White
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    maxLines = 1
                                )

                                // Jumlah objek dalam badge kecil
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = if (selected) {
                                                Color.White.copy(alpha = 0.2f)
                                            } else {
                                                MaterialTheme.colorScheme.outlineVariant.copy(
                                                    alpha = 0.5f
                                                )
                                            },
                                            shape = CircleShape
                                        )
                                        .padding(
                                            horizontal = 5.dp,
                                            vertical = 2.dp
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = count.toString(),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) {
                                            Color.White
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }



        // List of Objects
        if (filteredInspections.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.SearchOff, contentDescription = null, tint = SecondarySlateLight, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Tidak Ada Objek yang Cocok", fontWeight = FontWeight.Bold, color = SecondarySlate)
                    Text("Coba ubah kata kunci pencarian atau reset filter di atas.", fontSize = 12.sp, color = SecondarySlateLight)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(filteredInspections) { idx, item ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = PrimaryBlueContainer,
                                shape = CircleShape,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${idx + 1}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = PrimaryBlueDark
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = SurfaceVariantLight,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = item.objectCode,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = IndustrialNavy,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    if (item.isStage2Checked) {
                                        Surface(color = StatusGreenContainer, shape = RoundedCornerShape(4.dp)) {
                                            Text("Sudah Diperiksa", color = StatusGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = item.objectName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (item.function.isNotBlank()) {
                                    Text(text = "Fungsi: ${item.function}", fontSize = 11.sp, color = SecondarySlate)
                                }
                            }

                            Icon(Icons.Default.CheckCircleOutline, contentDescription = null, tint = if (item.isStage2Checked) StatusGreen else SecondarySlateLight)
                        }
                    }
                }
            }
        }

        // Bottom Action Bar
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
                    Text("Lanjut: Tahap 2 (Kebutuhan)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}