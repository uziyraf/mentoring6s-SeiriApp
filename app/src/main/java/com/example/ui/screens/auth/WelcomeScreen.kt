package com.example.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.AuthRepository
import com.example.ui.screens.auth.RegisterScreen
import com.example.ui.theme.*

@Composable
fun WelcomeScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onLoginAsGuest: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // --- HEADER / HERO SECTION ---
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(PrimaryBlue.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Engineering,
                contentDescription = "Logo",
                tint = PrimaryBlue,
                modifier = Modifier.size(60.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Seiri Mentoring 6S",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = IndustrialNavy,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = "Sistem Pemilahan & Manajemen Area Kerja",
            style = MaterialTheme.typography.bodyMedium,
            color = SecondarySlate,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 40.dp)
        )

        // --- PILIHAN ROLE (KARTU) ---
        Text(
            text = "Pilih Akses Masuk",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = IndustrialNavy,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            textAlign = TextAlign.Start
        )

        // 1. Kartu Masuk
        RoleCard(
            title = "Masuk ke Akun",
            subtitle = "Login dengan email & password atau Google",
            icon = Icons.Default.Person,
            iconTint = PrimaryBlue,
            iconBg = PrimaryBlueContainer,
            onClick = onNavigateToLogin
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Kartu Daftar
        RoleCard(
            title = "Daftar Akun Baru",
            subtitle = "Dibuat sebagai Mentor 6S; area ditentukan admin setelahnya",
            icon = Icons.Default.Person,
            iconTint = StatusGreen,
            iconBg = StatusGreenContainer,
            onClick = onNavigateToRegister
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Divider "ATAU"
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = OutlineLight)
            Text(
                text = " Atau ",
                color = SecondarySlate,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = OutlineLight)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 3. Tombol Guest (Tanpa Login)
        OutlinedButton(
            onClick = {
                // Memanggil fungsi guest dari repository, lalu langsung masuk
                AuthRepository.loginAsGuest()
                onLoginAsGuest() 
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = IndustrialNavy
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, OutlineLight)
        ) {
            Icon(
                imageVector = Icons.Default.PersonOutline,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Masuk Tanpa Login (Tamu)",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Text(
            text = "© 2026 Seiri 6S System",
            fontSize = 11.sp,
            color = SecondarySlateLight,
            modifier = Modifier.padding(top = 32.dp)
        )
    }
}

@Composable
private fun RoleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = IndustrialNavy
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondarySlate,
                    lineHeight = 16.sp
                )
            }
        }
    }
}