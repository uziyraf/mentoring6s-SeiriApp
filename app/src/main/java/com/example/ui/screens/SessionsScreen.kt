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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SampleData
import com.example.data.SeiriRepository
import com.example.model.MentoringSession
import com.example.model.SessionStatus
import com.example.ui.theme.*

@Composable
fun SessionsScreen(
    repository: SeiriRepository,
    onStartNewSession: () -> Unit
) {
    val currentUser by repository.currentUser.collectAsState()
    val allSessions by repository.sessions.collectAsState()
    val validations by repository.validations.collectAsState()

    val filteredSessions = if (currentUser.role == com.example.model.UserRole.ADMIN) {
        allSessions
    } else {
        allSessions.filter { it.mentorId == currentUser.id }
    }

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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (currentUser.role == com.example.model.UserRole.ADMIN) "Seluruh Sesi Mentoring 6S" else "Sesi Mentoring Saya",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${filteredSessions.size} Sesi Tercatat",
                        fontSize = 12.sp,
                        color = SecondarySlate
                    )
                }

                Button(
                    onClick = onStartNewSession,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Sesi Baru", fontSize = 12.sp)
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredSessions, key = { it.id }) { ses ->
                val mentor = SampleData.allUsers.find { it.id == ses.mentorId }
                val sessionValCount = validations.count { it.sessionId == ses.id }

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = if (ses.status == SessionStatus.COMPLETED) StatusGreenContainer else StatusAmberContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = ses.status.label.uppercase(),
                                    color = if (ses.status == SessionStatus.COMPLETED) StatusGreen else StatusAmber,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(text = ses.sessionDate, fontSize = 11.sp, color = SecondarySlate)
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${repository.getAreaName(ses.areaId)} • ${repository.getZoneName(ses.zoneId)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Mentor: ${mentor?.name ?: "Mentor"} (${mentor?.department ?: "-"})",
                            fontSize = 12.sp,
                            color = SecondarySlate
                        )

                        if (ses.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Catatan: ${ses.notes}",
                                fontSize = 12.sp,
                                color = IndustrialNavy
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = PrimaryBlueContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "$sessionValCount Objek Divalidasi",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryBlueDark,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }

                            if (ses.completedAt != null) {
                                Text(
                                    text = "Selesai: ${ses.completedAt}",
                                    fontSize = 10.sp,
                                    color = SecondarySlateLight
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
