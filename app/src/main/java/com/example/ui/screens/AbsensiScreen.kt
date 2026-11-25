package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.example.model.AttendanceRecord
import com.example.model.AttendanceStatus
import com.example.model.UserRole
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.SchoolViewModel

@Composable
fun AbsensiScreen(
    viewModel: SchoolViewModel
) {
    val allAttendance by viewModel.activeAttendance.collectAsState()
    val guruAttendance by viewModel.guruFilteredAttendance.collectAsState()
    val classes by viewModel.activeClasses.collectAsState()
    val tenant by viewModel.currentTenant.collectAsState()
    val currentRole by viewModel.currentRole.collectAsState()
    val currentUserAccount by viewModel.currentUserAccount.collectAsState()
    val session by viewModel.authenticatedSession.collectAsState()
    val brandColors = LocalBrandColors.current

    val attendanceList = if (currentRole == UserRole.GURU) guruAttendance else allAttendance
    val studentSession by viewModel.currentStudentSession.collectAsState()
    val isStudent = currentRole == UserRole.SISWA
    val studentName = studentSession?.name ?: "Ahmad Fauzan Syahputra"
    val canEdit = currentRole == UserRole.SUPERADMIN || currentRole == UserRole.GURU

    val guruAssignedClasses = remember(currentUserAccount, session) {
        currentUserAccount?.idKelas?.ifEmpty { session?.idKelas ?: emptyList() }
            ?: (session?.idKelas ?: emptyList())
    }
    val visibleClasses = remember(classes, currentRole, guruAssignedClasses) {
        if (currentRole == UserRole.GURU && guruAssignedClasses.isNotEmpty()) {
            classes.filter { c -> guruAssignedClasses.any { it.equals(c.name, ignoreCase = true) } }
                .ifEmpty { classes }
        } else {
            classes
        }
    }

    var selectedClass by remember(visibleClasses) {
        mutableStateOf(visibleClasses.firstOrNull()?.name ?: guruAssignedClasses.firstOrNull() ?: "XII MIPA 1")
    }
    LaunchedEffect(visibleClasses) {
        if (visibleClasses.isNotEmpty() && visibleClasses.none { it.name == selectedClass }) {
            selectedClass = visibleClasses.first().name
        }
    }
    var showExportDialog by remember { mutableStateOf(false) }

    val classAttendance = attendanceList.filter { record ->
        val matchesClass = if (isStudent) true else record.className.equals(selectedClass, ignoreCase = true)
        val matchesStudent = if (isStudent) {
            record.studentName.contains(studentName, ignoreCase = true) ||
                studentName.contains(record.studentName, ignoreCase = true)
        } else true
        matchesClass && matchesStudent
    }

    val hadirCount = classAttendance.count { it.status == AttendanceStatus.HADIR }
    val sakitCount = classAttendance.count { it.status == AttendanceStatus.SAKIT }
    val izinCount = classAttendance.count { it.status == AttendanceStatus.IZIN }
    val alphaCount = classAttendance.count { it.status == AttendanceStatus.ALPHA }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isStudent) "Presensi Pribadi Siswa" else "Presensi & Absensi Siswa",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { showExportDialog = true }) {
                Icon(Icons.Default.FileDownload, contentDescription = "Export Absensi", tint = brandColors.primary)
            }
        }

        if (!isStudent) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(visibleClasses) { c ->
                    val isSelected = c.name == selectedClass
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedClass = c.name },
                        label = { Text(c.name, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = brandColors.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AttendanceStatMini("Hadir", hadirCount, SneatSuccess, SneatSuccessLight, Modifier.weight(1f))
            AttendanceStatMini("Sakit", sakitCount, SneatWarning, SneatWarningLight, Modifier.weight(1f))
            AttendanceStatMini("Izin", izinCount, SneatInfo, SneatInfoLight, Modifier.weight(1f))
            AttendanceStatMini("Alpha", alphaCount, SneatDanger, SneatDangerLight, Modifier.weight(1f))
        }

        if (canEdit) {
            FilledTonalButton(
                onClick = { viewModel.markAllPresent(selectedClass) },
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = SneatSuccessLight, contentColor = SneatSuccess),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tandai Semua Siswa Hadir", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        if (classAttendance.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("Belum ada data absensi untuk kelas $selectedClass hari ini.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(classAttendance, key = { it.id }) { item ->
                    AttendanceItemCard(
                        item = item,
                        canEdit = canEdit,
                        onStatusChange = { newStatus ->
                            viewModel.toggleAttendanceStatus(item.id, newStatus)
                        }
                    )
                }
            }
        }
    }

    if (showExportDialog) {
        SneatExportDialog(
            title = "Absensi Kelas $selectedClass",
            schoolName = tenant.name,
            onDismiss = { showExportDialog = false },
            onConfirmExport = {}
        )
    }
}

@Composable
private fun AttendanceStatMini(
    label: String,
    count: Int,
    color: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 11.sp, color = color, fontWeight = FontWeight.Medium)
            Text("$count", fontSize = 18.sp, color = color, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun AttendanceItemCard(
    item: AttendanceRecord,
    canEdit: Boolean,
    onStatusChange: (AttendanceStatus) -> Unit
) {
    SneatCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.studentName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (item.notes.isNotBlank()) {
                    Text(
                        text = "Catatan: ${item.notes}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                AttendanceStatus.entries.forEach { st ->
                    val isSelected = item.status == st
                    val (textColor, bgColor) = when (st) {
                        AttendanceStatus.HADIR -> SneatSuccess to SneatSuccessLight
                        AttendanceStatus.SAKIT -> SneatWarning to SneatWarningLight
                        AttendanceStatus.IZIN -> SneatInfo to SneatInfoLight
                        AttendanceStatus.ALPHA -> SneatDanger to SneatDangerLight
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSelected) textColor else bgColor.copy(alpha = 0.5f),
                        modifier = Modifier.clickable(enabled = canEdit) { onStatusChange(st) }
                    ) {
                        Text(
                            text = st.shortCode,
                            color = if (isSelected) Color.White else textColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}
