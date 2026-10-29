package com.example.ui.components

import android.util.Log
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
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
fun ObjectPhoto(
    photoUrl: String?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val url = photoUrl?.trim()?.takeIf { it.isNotEmpty() }
    val context = LocalContext.current

    if (url == null) {
        // Placeholder jika URL kosong (null)
        Box(
            modifier = modifier.background(SurfaceVariantLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = null,
                tint = SecondarySlate,
                modifier = Modifier.size(32.dp)
            )
        }
    } else {
        // Menggunakan SubcomposeAsyncImage untuk menghindari bug loading pada rememberAsyncImagePainter
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(url)
                // User-Agent membantu mem-bypass beberapa pemblokiran Google Drive
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .crossfade(true)
                .build(),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier,
            loading = {
                // Tampilan saat gambar sedang diunduh
                Box(
                    Modifier.fillMaxSize().background(SurfaceVariantLight),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = PrimaryBlue
                    )
                }
            },
            error = { state ->
                // Tampilan jika gambar gagal dimuat (akan muncul di Logcat)
                Log.e("ObjectPhoto", "Gagal memuat gambar: $url", state.result.throwable)
                Box(
                    Modifier.fillMaxSize().background(SurfaceVariantLight),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.BrokenImage,
                            contentDescription = "Error",
                            tint = StatusRed,
                            modifier = Modifier.size(24.dp)
                        )
                        Text("Gagal", fontSize = 10.sp, color = StatusRed)
                    }
                }
            },
            success = {
                // Menggambar gambar yang berhasil diunduh
                SubcomposeAsyncImageContent()
            }
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