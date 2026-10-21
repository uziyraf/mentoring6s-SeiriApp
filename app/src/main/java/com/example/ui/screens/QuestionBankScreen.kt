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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SeiriRepository
import com.example.model.SeiriQuestion
import com.example.ui.theme.*

@Composable
fun QuestionBankScreen(
    repository: SeiriRepository
) {
    val questions by repository.questions.collectAsState()
    var selectedStage by remember { mutableStateOf<Int?>(null) }
    var selectedLevel by remember { mutableStateOf<String?>(null) }

    val filteredQuestions = questions.filter { q ->
        val matchesStage = selectedStage == null || q.stage == selectedStage
        val matchesLevel = selectedLevel == null || q.targetLevel == selectedLevel
        matchesStage && matchesLevel
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
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Bank 30 Pertanyaan Panduan Seiri 6S",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Kuesioner standar berdasarkan poster Seiri yang dibagi menjadi 5 tahapan pemilahan, level Area, Zona, dan Objek.",
                    fontSize = 12.sp,
                    color = SecondarySlate
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Stage Filters
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedStage == null,
                        onClick = { selectedStage = null },
                        label = { Text("Semua Tahap (30)") }
                    )
                    for (stageNum in 1..5) {
                        val count = questions.count { it.stage == stageNum }
                        FilterChip(
                            selected = selectedStage == stageNum,
                            onClick = { selectedStage = if (selectedStage == stageNum) null else stageNum },
                            label = { Text("Tahap $stageNum ($count)") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Level Filters
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = selectedLevel == null,
                        onClick = { selectedLevel = null },
                        label = { Text("Semua Level") }
                    )
                    listOf("AREA", "ZONA", "OBJEK").forEach { lvl ->
                        FilterChip(
                            selected = selectedLevel == lvl,
                            onClick = { selectedLevel = if (selectedLevel == lvl) null else lvl },
                            label = { Text("Level $lvl") }
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredQuestions, key = { it.id }) { q ->
                Card(
                    shape = RoundedCornerShape(12.dp),
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
                                color = PrimaryBlueContainer,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = q.questionCode,
                                    color = PrimaryBlueDark,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Surface(
                                    color = when (q.targetLevel) {
                                        "AREA" -> StatusPurpleContainer
                                        "ZONA" -> StatusBlueContainer
                                        else -> StatusGreenContainer
                                    },
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "LEVEL ${q.targetLevel}",
                                        color = when (q.targetLevel) {
                                            "AREA" -> StatusPurple
                                            "ZONA" -> StatusBlue
                                            else -> StatusGreen
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                if (q.isRequired) {
                                    Surface(
                                        color = StatusRedContainer,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "WAJIB",
                                            color = StatusRed,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = q.stageName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SecondarySlate
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = q.questionText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
