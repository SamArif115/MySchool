package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PtkAttendanceRecord
import com.example.model.PtkAttendanceStatus
import com.example.model.Teacher
import com.example.model.UserRole
import com.example.ui.components.SneatBadge
import com.example.ui.components.SneatCard
import com.example.ui.components.SneatConfirmDialog
import com.example.ui.components.SneatRouteGuardRestricted
import com.example.ui.components.SneatSearchBar
import com.example.ui.theme.*
import com.example.viewmodel.SchoolViewModel

@Composable
fun PtkManagementScreen(
    viewModel: SchoolViewModel,
    initialTab: Int = 0
) {
    val currentRole by viewModel.currentRole.collectAsState()
    if (currentRole != UserRole.SUPERADMIN) {
        SneatRouteGuardRestricted(
            currentRole = currentRole,
            requiredRole = "Admin Utama Sekolah",
            onSwitchRoleClick = { viewModel.switchRole(UserRole.SUPERADMIN) }
        )
        return
    }

    val tenant by viewModel.currentTenant.collectAsState()
    val teachers by viewModel.activeTeachers.collectAsState()
    val ptkAttendance by viewModel.activePtkAttendance.collectAsState()
    val brandColors = LocalBrandColors.current

    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 3)) }
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("Semua") }
    var activeFilter by remember { mutableStateOf("Semua") }
    var nonSatmikalFilter by remember { mutableStateOf("Semua") }

    var dutyTeacherTarget by remember { mutableStateOf<Teacher?>(null) }
    var mutasiTeacherTarget by remember { mutableStateOf<Teacher?>(null) }
    var nonSatmikalTeacherTarget by remember { mutableStateOf<Teacher?>(null) }
    var showAddAttendanceDialog by remember { mutableStateOf(false) }
    var attendanceToDelete by remember { mutableStateOf<PtkAttendanceRecord?>(null) }

    val filteredTeachers = remember(teachers, searchQuery, statusFilter, activeFilter) {
        teachers.filter { t ->
            val matchQuery = searchQuery.isBlank() ||
                t.name.contains(searchQuery, ignoreCase = true) ||
                t.nip.contains(searchQuery, ignoreCase = true) ||
                t.subject.contains(searchQuery, ignoreCase = true) ||
                t.additionalDuty.contains(searchQuery, ignoreCase = true)
            val matchStatus = statusFilter == "Semua" ||
                t.employmentStatus.equals(statusFilter, ignoreCase = true) ||
                (statusFilter == "P3K" && t.employmentStatus.equals("PPPK", ignoreCase = true))
            val matchActive = when (activeFilter) {
                "Aktif" -> t.isActiveAccount
                "Non-Aktif" -> !t.isActiveAccount
                else -> true
            }
            matchQuery && matchStatus && matchActive
        }
    }

    val tabTitles = listOf("Monitoring PTK", "Tugas Tambahan", "Presensi PTK", "Mutasi & Non-Satmikal")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = brandColors.primary,
            edgePadding = 12.dp
        ) {
            tabTitles.forEachIndexed { idx, label ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    modifier = Modifier.testTag("ptk_tab_$idx"),
                    text = {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == idx) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SneatCard(borderColor = brandColors.primary.copy(alpha = 0.25f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Manajemen & Monitoring PTK",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${teachers.size} PTK • ${teachers.count { it.isActiveAccount }} Aktif • ${teachers.count { it.isNonSatmikal }} Non-Satmikal • ${teachers.count { it.mutationStatus == "DIAJUKAN" }} Mutasi",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (selectedTab == 2) {
                            Button(
                                onClick = { showAddAttendanceDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = brandColors.primary),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("add_ptk_attendance_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Catat Presensi PTK", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Presensi", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            if (selectedTab != 2) {
                item {
                    SneatSearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        placeholder = "Cari nama PTK, NIP, mapel, atau tugas tambahan...",
                        modifier = Modifier.testTag("ptk_search_input")
                    )
                }
            }

            when (selectedTab) {
                0 -> {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(listOf("Semua", "PNS", "P3K", "Honorer", "Kontrak")) { st ->
                                    FilterChip(
                                        selected = statusFilter == st,
                                        onClick = { statusFilter = st },
                                        label = { Text(st, fontSize = 11.sp) }
                                    )
                                }
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(listOf("Semua", "Aktif", "Non-Aktif")) { act ->
                                    FilterChip(
                                        selected = activeFilter == act,
                                        onClick = { activeFilter = act },
                                        label = { Text("Akun: $act", fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                    }
                    items(filteredTeachers, key = { it.id }) { t ->
                        val attCount = ptkAttendance.count { it.teacherId == t.id }
                        SneatCard(modifier = Modifier.testTag("ptk_card_${t.id}")) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(t.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("${t.jabatan} • NIP: ${t.nip.ifBlank { "-" }}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                SneatBadge(
                                    text = if (t.isActiveAccount) "Aktif" else "Non-Aktif",
                                    textColor = if (t.isActiveAccount) SneatSuccess else SneatDanger,
                                    backgroundColor = if (t.isActiveAccount) SneatSuccessLight else SneatDangerLight
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                item {
                                    SneatBadge(
                                        text = "${t.employmentStatus}${if (t.rankGroup.isNotBlank()) " (${t.rankGroup})" else ""}",
                                        textColor = brandColors.primary,
                                        backgroundColor = brandColors.containerLight
                                    )
                                }
                                if (t.additionalDuty.isNotBlank()) {
                                    item { SneatBadge(t.additionalDuty, SneatInfo, SneatInfoLight) }
                                }
                                if (t.isNonSatmikal) {
                                    item {
                                        SneatBadge(
                                            text = if (t.nonSatmikalVerified) "Non-Satmikal Terverifikasi" else "Non-Satmikal",
                                            textColor = if (t.nonSatmikalVerified) SneatSuccess else SneatWarning,
                                            backgroundColor = if (t.nonSatmikalVerified) SneatSuccessLight else SneatWarningLight
                                        )
                                    }
                                }
                                if (t.mutationStatus != "NONE") {
                                    item { SneatBadge("Mutasi: ${t.mutationStatus}", SneatWarning, SneatWarningLight) }
                                }
                                if (t.transferConfirmed) {
                                    item { SneatBadge("Alih Guru Terkonfirmasi", SneatSuccess, SneatSuccessLight) }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "SK: ${t.skNumber.ifBlank { "-" }} • TMT: ${t.tmtDate.ifBlank { "-" }} • Presensi tercatat: $attCount",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.toggleTeacherAccountActive(t.id, !t.isActiveAccount) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .minimumInteractiveComponentSize()
                                        .testTag("toggle_ptk_active_${t.id}")
                                ) {
                                    Text(if (t.isActiveAccount) "Nonaktifkan Akun" else "Aktifkan Akun", fontSize = 11.sp)
                                }
                                FilledTonalButton(
                                    onClick = { dutyTeacherTarget = t },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .minimumInteractiveComponentSize()
                                ) {
                                    Text("Tugas Tambahan", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                1 -> {
                    items(filteredTeachers, key = { "duty-${it.id}" }) { t ->
                        SneatCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(t.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Mapel: ${t.subject} • ${t.weeklyHours} JP/minggu", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Tugas Tambahan: ${t.additionalDuty.ifBlank { "Belum ditetapkan" }}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (t.additionalDuty.isNotBlank()) brandColors.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                FilledTonalButton(
                                    onClick = { dutyTeacherTarget = t },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("edit_duty_${t.id}")
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Atur Tugas Tambahan", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Atur", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                2 -> {
                    if (ptkAttendance.isEmpty()) {
                        item {
                            SneatCard {
                                Text("Belum ada catatan presensi PTK. Tekan tombol '+ Presensi' untuk mencatat.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        items(ptkAttendance, key = { it.id }) { att ->
                            val (badgeColor, badgeBg) = when (att.status) {
                                PtkAttendanceStatus.HADIR -> SneatSuccess to SneatSuccessLight
                                PtkAttendanceStatus.DINAS_LUAR -> SneatInfo to SneatInfoLight
                                PtkAttendanceStatus.IZIN, PtkAttendanceStatus.CUTI -> SneatWarning to SneatWarningLight
                                PtkAttendanceStatus.SAKIT -> SneatDanger to SneatDangerLight
                            }
                            SneatCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(att.teacherName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("${att.date}${if (att.notes.isNotBlank()) " • ${att.notes}" else ""}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        SneatBadge(att.status.label, badgeColor, badgeBg)
                                        IconButton(
                                            onClick = { attendanceToDelete = att },
                                            modifier = Modifier.minimumInteractiveComponentSize()
                                        ) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus Presensi PTK", tint = SneatDanger)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> {
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(listOf("Semua", "Non-Satmikal", "Pengajuan Mutasi", "Alih Guru")) { f ->
                                FilterChip(
                                    selected = nonSatmikalFilter == f,
                                    onClick = { nonSatmikalFilter = f },
                                    label = { Text(f, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                    val mutasiList = filteredTeachers.filter { t ->
                        when (nonSatmikalFilter) {
                            "Non-Satmikal" -> t.isNonSatmikal
                            "Pengajuan Mutasi" -> t.mutationStatus != "NONE"
                            "Alih Guru" -> t.transferConfirmed
                            else -> true
                        }
                    }
                    items(mutasiList, key = { "mut-${it.id}" }) { t ->
                        SneatCard {
                            Text(t.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("NIP: ${t.nip.ifBlank { "-" }} • Status: ${t.employmentStatus}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Non-Satmikal: ${if (t.isNonSatmikal) "Ya (${t.satmikalOrigin.ifBlank { "Asal Belum Diisi" }})" else "Tidak (Satmikal Utama)"} • Verifikasi: ${if (t.nonSatmikalVerified) "Terverifikasi" else "Belum"}",
                                fontSize = 11.sp
                            )
                            Text(
                                text = "Mutasi: ${t.mutationStatus}${if (t.mutationTargetSchool.isNotBlank()) " -> ${t.mutationTargetSchool}" else ""} • Alih Guru: ${if (t.transferConfirmed) "Terkonfirmasi" else "Belum"}",
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { nonSatmikalTeacherTarget = t },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .minimumInteractiveComponentSize()
                                        .testTag("btn_non_satmikal_${t.id}")
                                ) {
                                    Text("Non-Satmikal", fontSize = 10.sp)
                                }
                                OutlinedButton(
                                    onClick = { mutasiTeacherTarget = t },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .minimumInteractiveComponentSize()
                                        .testTag("btn_mutasi_${t.id}")
                                ) {
                                    Text("Mutasi", fontSize = 10.sp)
                                }
                                Button(
                                    onClick = { viewModel.confirmGuruTransfer(t.id, !t.transferConfirmed) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (t.transferConfirmed) SneatSuccess else brandColors.primary
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .minimumInteractiveComponentSize()
                                        .testTag("btn_alih_guru_${t.id}")
                                ) {
                                    Text(if (t.transferConfirmed) "Alih OK" else "Konfirmasi Alih", fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    dutyTeacherTarget?.let { target ->
        var duty by remember(target.id) { mutableStateOf(target.additionalDuty) }
        AlertDialog(
            onDismissRequest = { dutyTeacherTarget = null },
            title = { Text("Tugas Tambahan PTK", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(target.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("Wali Kelas", "Kepala Lab", "Pembina OSIS", "Operator Dapodik", "Waka Kurikulum")) { preset ->
                            FilterChip(
                                selected = duty == preset,
                                onClick = { duty = preset },
                                label = { Text(preset, fontSize = 10.sp) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = duty,
                        onValueChange = { duty = it },
                        label = { Text("Tugas Tambahan") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updatePtkExtraDuty(target.id, duty) { dutyTeacherTarget = null }
                }) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { dutyTeacherTarget = null }) { Text("Batal") }
            }
        )
    }

    nonSatmikalTeacherTarget?.let { target ->
        var isNonSatmikal by remember(target.id) { mutableStateOf(target.isNonSatmikal) }
        var verified by remember(target.id) { mutableStateOf(target.nonSatmikalVerified) }
        var origin by remember(target.id) { mutableStateOf(target.satmikalOrigin) }
        AlertDialog(
            onDismissRequest = { nonSatmikalTeacherTarget = null },
            title = { Text("Verifikasi Non-Satmikal Lembaga", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(target.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Status Guru Non-Satmikal", fontSize = 12.sp)
                        Switch(checked = isNonSatmikal, onCheckedChange = { isNonSatmikal = it })
                    }
                    if (isNonSatmikal) {
                        OutlinedTextField(
                            value = origin,
                            onValueChange = { origin = it },
                            label = { Text("Sekolah Induk / Satmikal Asal") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Verifikasi Admin Lembaga", fontSize = 12.sp)
                            Switch(checked = verified, onCheckedChange = { verified = it })
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.verifyNonSatmikal(target.id, isNonSatmikal, verified, origin) {
                        nonSatmikalTeacherTarget = null
                    }
                }) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { nonSatmikalTeacherTarget = null }) { Text("Batal") }
            }
        )
    }

    mutasiTeacherTarget?.let { target ->
        var mutStatus by remember(target.id) { mutableStateOf(target.mutationStatus) }
        var mutTarget by remember(target.id) { mutableStateOf(target.mutationTargetSchool) }
        AlertDialog(
            onDismissRequest = { mutasiTeacherTarget = null },
            title = { Text("Pengajuan Mutasi PTK", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(target.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("NONE", "DIAJUKAN", "DISETUJUI", "DITOLAK")) { st ->
                            FilterChip(selected = mutStatus == st, onClick = { mutStatus = st }, label = { Text(st, fontSize = 10.sp) })
                        }
                    }
                    OutlinedTextField(
                        value = mutTarget,
                        onValueChange = { mutTarget = it },
                        label = { Text("Sekolah Tujuan Mutasi") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateTeacherMutation(target.id, mutStatus, mutTarget) {
                        mutasiTeacherTarget = null
                    }
                }) { Text("Proses Mutasi") }
            },
            dismissButton = {
                TextButton(onClick = { mutasiTeacherTarget = null }) { Text("Batal") }
            }
        )
    }

    if (showAddAttendanceDialog) {
        var selectedTeacher by remember { mutableStateOf(teachers.firstOrNull()) }
        var date by remember { mutableStateOf("2026-11-18") }
        var status by remember { mutableStateOf(PtkAttendanceStatus.HADIR) }
        var notes by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddAttendanceDialog = false },
            title = { Text("Catat Presensi PTK", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Pilih PTK:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(teachers) { t ->
                            FilterChip(
                                selected = selectedTeacher?.id == t.id,
                                onClick = { selectedTeacher = t },
                                label = { Text(t.name.substringBefore(","), fontSize = 10.sp) }
                            )
                        }
                    }
                    OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Tanggal (YYYY-MM-DD)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(PtkAttendanceStatus.values().toList()) { st ->
                            FilterChip(selected = status == st, onClick = { status = st }, label = { Text(st.label, fontSize = 10.sp) })
                        }
                    }
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Keterangan") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    val t = selectedTeacher ?: return@Button
                    viewModel.savePtkAttendance(
                        PtkAttendanceRecord(
                            id = "PTKATT-${System.currentTimeMillis() % 100000}",
                            idSekolah = tenant.idSekolah,
                            teacherId = t.id,
                            teacherName = t.name,
                            date = date.trim(),
                            status = status,
                            notes = notes.trim()
                        )
                    ) { showAddAttendanceDialog = false }
                }) { Text("Simpan Presensi") }
            },
            dismissButton = {
                TextButton(onClick = { showAddAttendanceDialog = false }) { Text("Batal") }
            }
        )
    }

    attendanceToDelete?.let { item ->
        SneatConfirmDialog(
            title = "Hapus Presensi PTK",
            message = "Hapus catatan presensi ${item.teacherName} (${item.date})?",
            onConfirm = {
                viewModel.deletePtkAttendance(item.id)
                attendanceToDelete = null
            },
            onDismiss = { attendanceToDelete = null }
        )
    }
}
