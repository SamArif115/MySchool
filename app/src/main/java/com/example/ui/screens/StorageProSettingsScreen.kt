package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AdNetwork
import com.example.model.UserRole
import com.example.ui.components.SneatBadge
import com.example.ui.components.SneatCard
import com.example.ui.components.SneatRouteGuardRestricted
import com.example.ui.theme.*
import com.example.viewmodel.SchoolViewModel

@Composable
fun StorageProSettingsScreen(
    viewModel: SchoolViewModel
) {
    val currentRole by viewModel.currentRole.collectAsState()
    val proSubscription by viewModel.proSubscription.collectAsState()
    val brandColors = LocalBrandColors.current

    if (currentRole != UserRole.SUPERADMIN) {
        SneatRouteGuardRestricted(
            currentRole = currentRole,
            requiredRole = "Admin Utama Sekolah",
            onSwitchRoleClick = {}
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SneatCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Status Paket Sekolah", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        text = proSubscription.planName,
                        fontSize = 12.sp,
                        color = brandColors.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                SneatBadge(
                    text = if (proSubscription.isPro) "PRO AKTIF" else "FREE",
                    textColor = if (proSubscription.isPro) SneatSuccess else SneatWarning,
                    backgroundColor = if (proSubscription.isPro) SneatSuccessLight else SneatWarningLight
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Kapasitas Storage Terpakai", fontSize = 12.sp)
                Text(
                    "${proSubscription.storageUsedMb} MB / ${proSubscription.storageMaxMb} MB",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { (proSubscription.storageUsedMb.toFloat() / proSubscription.storageMaxMb).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = if (proSubscription.isPro) SneatSuccess else brandColors.primary,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Surface(color = SneatInfoLight, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = SneatInfo, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Kebijakan Anti-Pembengkakan Biaya: Setiap file upload (Logo, SK Guru, RPP, Materi, Foto KBM) dibatasi maksimal 5 MB.",
                        fontSize = 11.sp,
                        color = Color(0xFF036982)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { viewModel.togglePro() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (proSubscription.isPro) SneatSecondary else brandColors.primary
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().testTag("toggle_pro_settings_button")
            ) {
                Icon(Icons.Default.WorkspacePremium, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (proSubscription.isPro) "Turunkan ke Paket Free (500 MB + Iklan)" else "Aktifkan Paket PRO (50 GB Bebas Iklan)")
            }
        }

        SneatCard {
            Text("Konfigurasi Jaringan Iklan (Mode Free)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(
                text = "Saat sekolah berada di mode Free, pilih penyedia iklan utama yang ditampilkan pada banner aplikasi.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            AdNetwork.entries.forEach { net ->
                val isSelected = proSubscription.activeAdNetwork == net
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) brandColors.containerLight else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { viewModel.selectAdNetwork(net) }
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = isSelected, onClick = { viewModel.selectAdNetwork(net) })
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(net.displayName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Placement Tag: ${net.tag}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
