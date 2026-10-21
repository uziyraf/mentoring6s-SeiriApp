package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SampleData
import com.example.model.*
import com.example.ui.theme.*

@Composable
fun ValidationStatusBadge(status: ValidationStatus, modifier: Modifier = Modifier) {
    val (bgColor, textColor, icon) = when (status) {
        ValidationStatus.TERVALIDASI -> Triple(StatusGreenContainer, StatusGreen, Icons.Default.CheckCircle)
        ValidationStatus.PERLU_KONFIRMASI -> Triple(StatusAmberContainer, StatusAmber, Icons.Default.HelpOutline)
        ValidationStatus.BELUM_DIVALIDASI -> Triple(SurfaceVariantLight, SecondarySlate, Icons.Default.Pending)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = status.label,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun DecisionBadge(decision: SeiriDecision, modifier: Modifier = Modifier) {
    val (bgColor, textColor) = when (decision) {
        SeiriDecision.TETAP_DI_AREA -> Pair(StatusGreenContainer, StatusGreen)
        SeiriDecision.PINDAHKAN_LOKASI -> Pair(StatusBlueContainer, StatusBlue)
        SeiriDecision.PERLU_KONFIRMASI -> Pair(StatusAmberContainer, StatusAmber)
        SeiriDecision.TIDAK_DIPERLUKAN -> Pair(StatusRedContainer, StatusRed)
        SeiriDecision.PERLU_TINDAK_LANJUT -> Pair(StatusPurpleContainer, StatusPurple)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Text(
            text = decision.label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun TransferStatusBadge(status: TransferStatus, modifier: Modifier = Modifier) {
    val (bgColor, textColor, icon) = when (status) {
        TransferStatus.MENUNGGU_PERSETUJUAN -> Triple(StatusAmberContainer, StatusAmber, Icons.Default.Schedule)
        TransferStatus.DISETUJUI -> Triple(StatusGreenContainer, StatusGreen, Icons.Default.CheckCircle)
        TransferStatus.DITOLAK -> Triple(StatusRedContainer, StatusRed, Icons.Default.Cancel)
        TransferStatus.DIBATALKAN -> Triple(SurfaceVariantLight, SecondarySlate, Icons.Default.Block)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = status.label,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun ConditionBadge(condition: ObjectCondition, modifier: Modifier = Modifier) {
    val (bgColor, textColor) = when (condition) {
        ObjectCondition.BAIK -> Pair(StatusGreenContainer, StatusGreen)
        ObjectCondition.RUSAK_RINGAN -> Pair(StatusAmberContainer, StatusAmber)
        ObjectCondition.RUSAK_BERAT -> Pair(StatusRedContainer, StatusRed)
        ObjectCondition.USANG -> Pair(SurfaceVariantLight, SecondarySlate)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Text(
            text = condition.label,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color = PrimaryBlue,
    bgColor: Color = PrimaryBlueContainer,
    subText: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondarySlate,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subText != null) {
                    Text(
                        text = subText,
                        fontSize = 10.sp,
                        color = SecondarySlateLight
                    )
                }
            }
        }
    }
}

@Composable
fun RoleSwitcherDialog(
    currentUser: User,
    onSelectUser: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.SwitchAccount,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Ganti Peran Demonstrasi",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Pilih akun Admin atau Mentor untuk simulasi hak akses",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondarySlate
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "AKUN ADMINISTRATOR",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                UserRowItem(
                    user = SampleData.adminUser,
                    isSelected = currentUser.id == SampleData.adminUser.id,
                    onClick = {
                        onSelectUser(SampleData.adminUser.id)
                        onDismiss()
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "TIM 6 MENTOR 6S (PER AREA)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SecondarySlate,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                SampleData.mentors.forEach { mentor ->
                    UserRowItem(
                        user = mentor,
                        isSelected = currentUser.id == mentor.id,
                        onClick = {
                            onSelectUser(mentor.id)
                            onDismiss()
                        }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_role_dialog_button")
            ) {
                Text("Tutup")
            }
        }
    )
}

@Composable
private fun UserRowItem(
    user: User,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) PrimaryBlueContainer else SurfaceVariantLight,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("select_user_${user.id}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (user.role == UserRole.ADMIN) PrimaryBlue else SecondarySlate),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.name.take(2).uppercase(),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) PrimaryBlueDark else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${user.role.label} • ${user.department}",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = SecondarySlate
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Aktif",
                    tint = PrimaryBlue,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
