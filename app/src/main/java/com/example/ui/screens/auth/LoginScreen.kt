package com.example.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.AuthRepository
import com.example.data.repository.GoogleSignInHelper
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val googleWebClientId = context.getString(com.example.R.string.google_web_client_id)
    val isLoading by AuthRepository.isLoading.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // State untuk Dialog Lupa Password
    var showForgotPasswordDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo / Icon
        Icon(
            imageVector = Icons.Default.Engineering,
            contentDescription = "Logo",
            tint = PrimaryBlue,
            modifier = Modifier.size(80.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Seiri Mentoring 6S",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = IndustrialNavy
        )
        
        Text(
            text = "Silakan login untuk melanjutkan",
            style = MaterialTheme.typography.bodyMedium,
            color = SecondarySlate,
            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
        )

        // Card untuk Form
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                
                // Error Message
                if (errorMessage != null) {
                    Surface(
                        color = StatusRedContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = StatusRed,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(10.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Input Email
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; errorMessage = null },
                    label = { Text("Email") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = SecondarySlate) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Input Password
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; errorMessage = null },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = SecondarySlate) },
                    trailingIcon = {
                        val icon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(icon, contentDescription = "Tampilkan Password", tint = SecondarySlate)
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )

                // 🔴 TAMBAHAN: Teks Lupa Password
                Text(
                    text = "Lupa Password?",
                    color = PrimaryBlue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 6.dp, end = 4.dp)
                        .clickable { showForgotPasswordDialog = true }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Tombol Login
                Button(
                    onClick = {
                        if (email.isBlank() || password.isBlank()) {
                            errorMessage = "Email dan Password tidak boleh kosong."
                            return@Button
                        }
                        scope.launch {
                            val result = AuthRepository.login(email, password)
                            if (result.isSuccess) {
                                onLoginSuccess()
                            } else {
                                errorMessage = result.exceptionOrNull()?.message ?: "Login gagal."
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Masuk", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Divider "ATAU"
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = OutlineLight)
            Text(
                text = " ATAU ",
                color = SecondarySlate,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = OutlineLight)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Tombol Google
        Button(
            onClick = {
                scope.launch {
                    val tokenResult = GoogleSignInHelper.getIdToken(context, googleWebClientId)
                    if (tokenResult.isFailure) {
                        errorMessage = tokenResult.exceptionOrNull()?.message ?: "Login dengan Google gagal."
                        return@launch
                    }
                    val idToken = tokenResult.getOrThrow()
                    val result = AuthRepository.signInWithGoogle(idToken)
                    if (result.isSuccess) {
                        onLoginSuccess()
                    } else {
                        errorMessage = result.exceptionOrNull()?.message ?: "Login dengan Google gagal."
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryBlueDark),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = PrimaryBlueDark,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = SecondarySlate,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Masuk dengan Google", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Tombol Guest
        OutlinedButton(
            onClick = {
                AuthRepository.loginAsGuest()
                onLoginSuccess()
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryBlueDark)
        ) {
            Text("Masuk Tanpa Login (Guest)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
        
        Spacer(modifier = Modifier.weight(1f))

        OutlinedButton(
            onClick = onNavigateToRegister,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryBlueDark)
        ) {
            Text("Belum punya akun?", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Butuh bantuan? Hubungi Admin IT",
            fontSize = 11.sp,
            color = SecondarySlate,
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }

    // 🔴 TAMBAHAN: Dialog Lupa Password
    if (showForgotPasswordDialog) {
        var resetEmail by remember { mutableStateOf(email) } // Otomatis isi email jika sudah diketik di form
        var isSending by remember { mutableStateOf(false) }
        var resetMessage by remember { mutableStateOf<String?>(null) }
        var isSuccess by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isSending) showForgotPasswordDialog = false },
            title = { Text("Reset Password", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Masukkan email yang terdaftar. Kami akan mengirimkan tautan untuk mengatur ulang password Anda.", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it; resetMessage = null },
                        label = { Text("Email") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSending && !isSuccess
                    )
                    
                    if (resetMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = resetMessage!!,
                            color = if (isSuccess) StatusGreen else StatusRed,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (resetEmail.isBlank()) {
                            resetMessage = "Email tidak boleh kosong."
                            return@Button
                        }
                        scope.launch {
                            isSending = true
                            val result = AuthRepository.sendPasswordResetEmail(resetEmail)
                            isSending = false
                            if (result.isSuccess) {
                                isSuccess = true
                                resetMessage = "Email reset password telah dikirim. Silakan cek inbox/spam Anda."
                            } else {
                                resetMessage = result.exceptionOrNull()?.message ?: "Gagal mengirim email."
                            }
                        }
                    },
                    enabled = !isSending && !isSuccess
                ) {
                    if (isSending) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text(if (isSuccess) "Tutup" else "Kirim")
                    }
                }
            },
            dismissButton = {
                if (!isSuccess) {
                    TextButton(onClick = { showForgotPasswordDialog = false }, enabled = !isSending) {
                        Text("Batal")
                    }
                }
            }
        )
    }
}