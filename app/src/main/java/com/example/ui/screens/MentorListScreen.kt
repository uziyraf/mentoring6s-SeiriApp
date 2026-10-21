package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SampleData
import com.example.data.SeiriRepository
import com.example.model.UserRole
import com.example.ui.theme.*

@Composable
fun MentorListScreen(
    repository: SeiriRepository,
    onSwitchUser: (String) -> Unit
) {
    val currentUser by repository.currentUser.collectAsState()
    val objects by repository.objects.collectAsState()
    val sessions by repository.sessions.collectAsState()

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
                    text = "Data 6 Mentor 6S & Penugasan Area",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Daftar mentor resmi yang ditugaskan membina dan memvalidasi implementasi Seiri.",
                    fontSize = 12.sp,
                    color = SecondarySlate
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(SampleData.mentors, key = { it.id }) { mentor ->
                val assignedAreaName = repository.getAreaName(mentor.assignedAreaId ?: "")
                val mentorObjects = objects.filter { it.areaId == mentor.assignedAreaId && it.isActive }
                val mentorSessions = sessions.filter { it.mentorId == mentor.id }
                val isCurrentlyActive = currentUser.id == mentor.id

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCurrentlyActive) PrimaryBlueContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = mentor.name.take(2).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = mentor.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (isCurrentlyActive) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = StatusGreen,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "LOGIN SAAT INI",
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "${mentor.department} • @${mentor.username}",
                                    fontSize = 11.sp,
                                    color = SecondarySlate
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = OutlineLight.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Area Penugasan:", fontSize = 10.sp, color = SecondarySlate)
                                Text(assignedAreaName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = IndustrialNavy)
                            }
                            Column {
                                Text("Aset Dibina:", fontSize = 10.sp, color = SecondarySlate)
                                Text("${mentorObjects.size} Objek", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Column {
                                Text("Sesi Mentoring:", fontSize = 10.sp, color = SecondarySlate)
                                Text("${mentorSessions.size} Sesi", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        if (!isCurrentlyActive) {
                            OutlinedButton(
                                onClick = { onSwitchUser(mentor.id) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.SwitchAccount, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Simulasikan Login sebagai ${mentor.name.split(" ").first()}", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
