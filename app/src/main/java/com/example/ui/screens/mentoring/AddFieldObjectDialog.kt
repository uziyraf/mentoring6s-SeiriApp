package com.example.ui.screens.mentoring

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SeiriRepository
import com.example.model.*
import com.example.ui.theme.*


// -------------------------------------------------------------------------------------
// MODAL: ADD NEW OBJECT IN FIELD
// -------------------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddNewFieldObjectDialog(
    session: MentoringSession,
    repository: SeiriRepository,
    currentUser: User,
    onDismiss: () -> Unit,
    onAdded: (SeiriObject) -> Unit
) {
    var code by remember { mutableStateOf("OBJ-${repository.getAreaCode(session.areaId)}-${(100..999).random()}") }
    var name by remember { mutableStateOf("") }
    var function by remember { mutableStateOf("") }
    var pic by remember { mutableStateOf(currentUser.name) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("+ Tambah Objek Temuan Baru") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (errorMessage != null) {
                    Text(errorMessage ?: "", color = StatusRed, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Text("Zona: ${repository.getZoneName(session.zoneId)}", fontSize = 12.sp, color = SecondarySlate)
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Kode Objek *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Objek Temuan *") },
                    placeholder = { Text("Contoh: HOPPER B, CONVEYOR BELT CADANGAN") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = function,
                    onValueChange = { function = it },
                    label = { Text("Fungsi Objek") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = pic,
                    onValueChange = { pic = it },
                    label = { Text("PIC Penanggung Jawab *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (code.isBlank() || name.isBlank()) {
                        errorMessage = "Kode dan nama objek wajib diisi."
                        return@Button
                    }
                    val result = repository.addObject(
                        code = code,
                        name = name,
                        areaId = session.areaId,
                        zoneId = session.zoneId,
                        description = "Objek temuan baru di sesi mentoring",
                        function = function,
                        category = "Alat Kerja",
                        condition = ObjectCondition.BAIK,
                        pic = pic,
                        usageFrequency = UsageFrequency.DAILY
                    )
                    result.onSuccess { onAdded(it) }
                        .onFailure { errorMessage = it.message }
                }
            ) {
                Text("Tambahkan ke Sesi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}