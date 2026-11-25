package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.UserRole
import com.example.ui.components.SneatBadge
import com.example.ui.components.SneatCard
import com.example.ui.components.SneatRouteGuardRestricted
import com.example.ui.theme.*
import com.example.viewmodel.SchoolViewModel

@Composable
fun LoginActivityLogScreen(
    viewModel: SchoolViewModel
) {
    val logs by viewModel.activeLoginLogs.collectAsState()
    val currentRole by viewModel.currentRole.collectAsState()
    val brandColors = LocalBrandColors.current

    if (currentRole != UserRole.SUPERADMIN) {
        SneatRouteGuardRestricted(
            currentRole = currentRole,
            requiredRole = "Admin Utama Sekolah",
            onSwitchRoleClick = {}
        )
        return
    }

    var roleFilter by remember { mutableStateOf("Semua") }
    val filtered = logs.filter { log ->
        roleFilter == "Semua" || log.role.equals(roleFilter, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Log Aktivitas Login (${filtered.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Audit trail Google Sign-In real-time yang tersimpan di Cloud Firestore.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(listOf("Semua", "SUPERADMIN", "GURU", "KEPALA_SEKOLAH", "SISWA")) { r ->
                FilterChip(
                    selected = roleFilter == r,
                    onClick = { roleFilter = r },
                    label = { Text(r, fontSize = 11.sp) }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filtered, key = { it.id }) { log ->
                SneatCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(log.displayName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(log.email, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${log.timestamp} • ${log.deviceInfo}", fontSize = 10.sp, color = brandColors.primary)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SneatBadge(
                                text = "${log.role} • ${log.status}",
                                textColor = SneatSuccess,
                                backgroundColor = SneatSuccessLight
                            )
                            IconButton(onClick = { viewModel.deleteLoginLog(log.id) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus Log", tint = SneatDanger)
                            }
                        }
                    }
                }
            }
        }
    }
}
