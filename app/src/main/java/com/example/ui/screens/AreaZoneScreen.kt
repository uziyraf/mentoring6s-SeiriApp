package com.example.ui.screens

import androidx.compose.foundation.background
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
import com.example.data.SampleData
import com.example.data.SeiriRepository
import com.example.ui.theme.*

@Composable
fun AreaZoneScreen(
    repository: SeiriRepository
) {
    val areas by repository.areas.collectAsState()
    val zones by repository.zones.collectAsState()
    val objects by repository.objects.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Area Kerja & Zona Pabrik",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Struktur hirarki area kerja dan zona untuk pemilahan aset Seiri 6S.",
                    fontSize = 12.sp,
                    color = SecondarySlate
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(areas, key = { it.id }) { area ->
                val areaZones = zones.filter { it.areaId == area.id }
                val areaObjects = objects.filter { it.areaId == area.id && it.isActive }
                val assignedMentor = SampleData.mentors.find { it.assignedAreaId == area.id }

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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = StatusPurpleContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = area.group,
                                        color = StatusPurple,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = PrimaryBlueContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = area.areaCode,
                                        color = PrimaryBlueDark,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${areaObjects.size} Objek Aktif",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryBlue
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = area.areaName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(text = area.description, fontSize = 12.sp, color = SecondarySlate)

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Mentor Pembina: ${assignedMentor?.name ?: "Belum Ditugaskan"}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = IndustrialNavy
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "DAFTAR ZONA TERDAFTAR (${areaZones.size})",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = SecondarySlate
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        areaZones.forEach { z ->
                            val zoneObjCount = areaObjects.count { it.zoneId == z.id }
                            Surface(
                                color = SurfaceVariantLight,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${z.zoneCode} • ${z.zoneName}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = IndustrialNavy
                                    )
                                    Text(
                                        text = "$zoneObjCount Objek",
                                        fontSize = 11.sp,
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
}
