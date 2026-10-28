package com.example.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SeiriRepository
import com.example.ui.theme.*

@Composable
fun GuestAreaBrowseScreen() {
    val repository = remember { SeiriRepository.instance }
    val areas by repository.areas.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Area Kerja",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = IndustrialNavy
        )
        Text(
            text = "Tampilan tamu hanya boleh melihat daftar area. Sumber penanggung jawab area akan disiapkan nanti.",
            style = MaterialTheme.typography.bodyMedium,
            color = SecondarySlate,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (areas.isEmpty()) {
            Text(text = "Belum ada data area.", color = SecondarySlate)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(areas, key = { it.id }) { area ->
                    Card(
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Apartment,
                                    contentDescription = null,
                                    tint = StatusGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "${area.areaCode} - ${area.areaName}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Group: ${area.group}",
                                        fontSize = 12.sp,
                                        color = SecondarySlate
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            if (area.description.isNotBlank()) {
                                Text(
                                    text = area.description,
                                    fontSize = 12.sp,
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
