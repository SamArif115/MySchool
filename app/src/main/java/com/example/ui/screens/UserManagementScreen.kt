package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.SchoolViewModel

@Composable
fun UserManagementScreen(
    viewModel: SchoolViewModel
) {
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

    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("Input Guru Baru", "Input Siswa Baru & PIN", "Whitelist Akun RBAC")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = brandColors.primary
        ) {
            tabTitles.forEachIndexed { idx, title ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = {
                        Text(
                            text = title,
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == idx) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1
                        )
                    }
                )
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (selectedTab) {
                0 -> AdminTeacherManagementTab(viewModel = viewModel)
                1 -> AdminStudentManagementTab(viewModel = viewModel)
                2 -> AdminWhitelistTab(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminTeacherManagementTab(viewModel: SchoolViewModel) {
    val teachers by viewModel.activeTeachers.collectAsState()
    val tenant by viewModel.currentTenant.collectAsState()
    val classes by viewModel.activeClasses.collectAsState()
    val brandColors = LocalBrandColors.current

    var query by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("Semua") }
    var filterExpanded by remember { mutableStateOf(false) }
    val statusOptions = listOf("Semua", "PNS", "P3K", "Honorer", "Kontrak")

    var showTeacherDialog by remember { mutableStateOf(false) }
    var editingTeacher by remember { mutableStateOf<Teacher?>(null) }
    var deletingTeacher by remember { mutableStateOf<Teacher?>(null) }
    var showAllowanceExport by remember { mutableStateOf(false) }

    val filtered = teachers.filter { t ->
        val matchQ = t.name.contains(query, ignoreCase = true) ||
            t.nip.contains(query, ignoreCase = true) ||
            t.subject.contains(query, ignoreCase = true)
        val matchS = if (statusFilter == "Semua") true
        else if (statusFilter == "P3K") t.employmentStatus.equals("P3K", ignoreCase = true) || t.employmentStatus.equals("PPPK", ignoreCase = true)
        else t.employmentStatus.equals(statusFilter, ignoreCase = true)
        matchQ && matchS
    }
    val allowanceCandidates = teachers.count {
        it.employmentStatus.equals("PNS", ignoreCase = true) ||
            it.employmentStatus.equals("P3K", ignoreCase = true) ||
            it.employmentStatus.equals("PPPK", ignoreCase = true)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingTeacher = null
                    showTeacherDialog = true
                },
                containerColor = brandColors.primary,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_teacher_fab")
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Input Guru Baru")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SneatSearchBar(
                query = query,
                onQueryChange = { query = it },
                placeholder = "Cari nama guru, NIP, atau mapel..."
            )

            // Dropdown Status Kepegawaian + Laporan Tunjangan PNS/P3K
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExposedDropdownMenuBox(
                    expanded = filterExpanded,
                    onExpandedChange = { filterExpanded = !filterExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = "Status: $statusFilter",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = filterExpanded) },
                        singleLine = true,
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = filterExpanded,
                        onDismissRequest = { filterExpanded = false }
                    ) {
                        statusOptions.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt) },
                                onClick = {
                                    statusFilter = opt
                                    filterExpanded = false
                                }
                            )
                        }
                    }
                }

                FilledTonalButton(
                    onClick = { showAllowanceExport = true },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(52.dp)
                ) {
                    Icon(Icons.Default.Summarize, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tunjangan ($allowanceCandidates)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Menampilkan ${filtered.size} Guru • PNS/P3K Layak Tunjangan: $allowanceCandidates",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(filtered, key = { it.id }) { t ->
                    SneatCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                val bmp = rememberBase64ImageBitmap(t.photoBase64)
                                if (bmp != null) {
                                    androidx.compose.foundation.Image(
                                        bitmap = bmp,
                                        contentDescription = "Foto ${t.name}",
                                        modifier = Modifier.size(44.dp).clip(CircleShape),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.size(44.dp).clip(CircleShape).background(brandColors.containerLight),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = t.name.split(" ").take(2).mapNotNull { it.firstOrNull() }.joinToString(""),
                                            fontWeight = FontWeight.Bold,
                                            color = brandColors.primary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(t.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("${t.jabatan} • NIP: ${t.nip}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${t.subject} (${t.weeklyHours} JP/minggu)", fontSize = 11.sp, color = brandColors.primary, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                SneatBadge(
                                    text = if (t.employmentStatus.equals("PNS", true) && t.rankGroup.isNotBlank()) "PNS (${t.rankGroup})" else t.employmentStatus,
                                    textColor = if (t.employmentStatus.equals("PNS", true)) SneatSuccess else if (t.employmentStatus.contains("P3K", true) || t.employmentStatus.contains("PPPK", true)) SneatInfo else SneatWarning,
                                    backgroundColor = if (t.employmentStatus.equals("PNS", true)) SneatSuccessLight else if (t.employmentStatus.contains("P3K", true) || t.employmentStatus.contains("PPPK", true)) SneatInfoLight else SneatWarningLight
                                )
                                SneatBadge(
                                    text = if (t.isActiveAccount) "Aktif" else "Non-Aktif",
                                    textColor = if (t.isActiveAccount) SneatSuccess else SneatDanger,
                                    backgroundColor = if (t.isActiveAccount) SneatSuccessLight else SneatDangerLight
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "SK: ${t.skNumber.ifBlank { "-" }} (${t.skDate.ifBlank { "-" }}) • TMT: ${t.tmtDate.ifBlank { "-" }} • Masa Kerja: ${t.tenureYears} thn",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Kelas: ${t.classesTaught.joinToString(", ").ifBlank { "-" }} • Email: ${t.email}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (t.skDocumentName.isNotBlank()) {
                            Text(
                                text = "Bukti SK: ${t.skDocumentName}",
                                fontSize = 11.sp,
                                color = SneatSuccess,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            IconButton(onClick = {
                                editingTeacher = t
                                showTeacherDialog = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Guru", tint = brandColors.primary)
                            }
                            IconButton(onClick = { deletingTeacher = t }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus Guru", tint = SneatDanger)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTeacherDialog) {
        FullTeacherFormDialog(
            tenantId = tenant.idSekolah,
            availableClasses = classes.map { it.name },
            teacher = editingTeacher,
            primaryColor = brandColors.primary,
            onDismiss = { showTeacherDialog = false },
            onSave = { saved ->
                viewModel.saveTeacher(saved) { showTeacherDialog = false }
            }
        )
    }

    deletingTeacher?.let { t ->
        SneatConfirmDialog(
            title = "Hapus Guru",
            message = "Hapus data kepegawaian & whitelist untuk ${t.name}?",
            onConfirm = {
                viewModel.deleteTeacher(t.id)
                deletingTeacher = null
            },
            onDismiss = { deletingTeacher = null }
        )
    }

    if (showAllowanceExport) {
        SneatExportDialog(
            title = "Laporan Tunjangan Guru PNS & P3K",
            schoolName = tenant.name,
            onDismiss = { showAllowanceExport = false },
            onConfirmExport = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullTeacherFormDialog(
    tenantId: String,
    availableClasses: List<String>,
    teacher: Teacher?,
    primaryColor: Color,
    onDismiss: () -> Unit,
    onSave: (Teacher) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(teacher?.name ?: "") }
    var nip by remember { mutableStateOf(teacher?.nip ?: "") }
    var jabatan by remember { mutableStateOf(teacher?.jabatan ?: "Guru Mata Pelajaran") }
    var employmentStatus by remember { mutableStateOf(teacher?.employmentStatus ?: "PNS") }
    var statusExpanded by remember { mutableStateOf(false) }
    var rankGroup by remember { mutableStateOf(teacher?.rankGroup ?: "III/c") }
    var skNumber by remember { mutableStateOf(teacher?.skNumber ?: "") }
    var skDate by remember { mutableStateOf(teacher?.skDate ?: "2024-07-01") }
    var tmtDate by remember { mutableStateOf(teacher?.tmtDate ?: "2018-01-01") }
    var tenureYears by remember { mutableStateOf(teacher?.tenureYears?.toString() ?: "5") }
    var weeklyHours by remember { mutableStateOf(teacher?.weeklyHours?.toString() ?: "24") }
    var subject by remember { mutableStateOf(teacher?.subject ?: "") }
    var classesTaught by remember {
        mutableStateOf(
            teacher?.classesTaught?.joinToString(", ") ?: availableClasses.firstOrNull().orEmpty()
        )
    }
    var email by remember { mutableStateOf(teacher?.email ?: "") }
    var phone by remember { mutableStateOf(teacher?.phone ?: "") }
    var address by remember { mutableStateOf(teacher?.address ?: "") }
    var photoBase64 by remember { mutableStateOf(teacher?.photoBase64 ?: "") }
    var skDocumentName by remember { mutableStateOf(teacher?.skDocumentName ?: "") }
    var skDocumentSize by remember { mutableStateOf(teacher?.skDocumentSizeBytes ?: 0L) }
    var isActive by remember { mutableStateOf(teacher?.isActiveAccount ?: true) }
    var mediaError by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            context.processPickedPngJpgImage(uri)
                .onSuccess { res ->
                    photoBase64 = res.base64
                    mediaError = null
                }
                .onFailure { mediaError = it.localizedMessage }
        }
    }

    val skPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            context.processPickedDocument(uri)
                .onSuccess { res ->
                    skDocumentName = res.fileName
                    skDocumentSize = res.sizeBytes
                    mediaError = null
                }
                .onFailure { mediaError = it.localizedMessage }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (teacher == null) "Input Daftar Guru Baru" else "Edit Data Kepegawaian Guru",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(
                        onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (photoBase64.isBlank()) "Foto Profil (<=5MB)" else "Foto Terpilih ✓", fontSize = 11.sp)
                    }
                    FilledTonalButton(
                        onClick = { skPicker.launch(arrayOf("application/pdf", "image/png", "image/jpeg")) },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (skDocumentName.isBlank()) "Upload SK (<=5MB)" else "SK: ${skDocumentName.take(12)}", fontSize = 11.sp)
                    }
                }
                mediaError?.let { Text(it, color = SneatDanger, fontSize = 11.sp, fontWeight = FontWeight.Bold) }

                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama Lengkap & Gelar *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = nip, onValueChange = { nip = it }, label = { Text("NIP / NUPTK") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = jabatan, onValueChange = { jabatan = it }, label = { Text("Jabatan *") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                // Dropdown Status Kepegawaian
                ExposedDropdownMenuBox(expanded = statusExpanded, onExpandedChange = { statusExpanded = !statusExpanded }) {
                    OutlinedTextField(
                        value = employmentStatus,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Status Kepegawaian *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = statusExpanded, onDismissRequest = { statusExpanded = false }) {
                        listOf("PNS", "P3K", "Honorer", "Kontrak").forEach { st ->
                            DropdownMenuItem(
                                text = { Text(st) },
                                onClick = {
                                    employmentStatus = st
                                    statusExpanded = false
                                }
                            )
                        }
                    }
                }

                if (employmentStatus.equals("PNS", ignoreCase = true)) {
                    OutlinedTextField(
                        value = rankGroup,
                        onValueChange = { rankGroup = it },
                        label = { Text("Golongan / Ruang (Khusus PNS, cth: III/c, IV/a)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(value = skNumber, onValueChange = { skNumber = it }, label = { Text("Nomor SK") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = skDate, onValueChange = { skDate = it }, label = { Text("Tanggal SK") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = tmtDate, onValueChange = { tmtDate = it }, label = { Text("TMT") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = tenureYears, onValueChange = { tenureYears = it }, label = { Text("Masa Kerja (Thn)") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = weeklyHours, onValueChange = { weeklyHours = it }, label = { Text("Jam/Minggu") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Mata Pelajaran Utama *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = classesTaught, onValueChange = { classesTaught = it }, label = { Text("Kelas yang Diajar (pisahkan koma)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email Google Guru (Untuk Whitelist Login) *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Nomor HP / WhatsApp") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Alamat Tempat Tinggal") }, modifier = Modifier.fillMaxWidth())

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Status Akun Aktif", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Switch(checked = isActive, onCheckedChange = { isActive = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val id = teacher?.id ?: "T-${System.currentTimeMillis() % 10000}"
                    onSave(
                        Teacher(
                            id = id,
                            idSekolah = tenantId,
                            nip = nip.ifBlank { "-" },
                            name = name,
                            email = email,
                            phone = phone,
                            subject = subject,
                            employmentStatus = employmentStatus,
                            jabatan = jabatan,
                            skNumber = skNumber,
                            skDate = skDate,
                            tmtDate = tmtDate,
                            tenureYears = tenureYears.toIntOrNull() ?: 0,
                            rankGroup = if (employmentStatus == "PNS") rankGroup else "",
                            weeklyHours = weeklyHours.toIntOrNull() ?: 24,
                            classesTaught = classesTaught.split(",").map { it.trim() }.filter { it.isNotBlank() },
                            address = address,
                            photoBase64 = photoBase64,
                            skDocumentName = skDocumentName,
                            skDocumentSizeBytes = skDocumentSize,
                            isActiveAccount = isActive
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                shape = RoundedCornerShape(8.dp)
            ) { Text("Simpan Data Guru") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@Composable
private fun AdminStudentManagementTab(viewModel: SchoolViewModel) {
    val students by viewModel.activeStudents.collectAsState()
    val classes by viewModel.activeClasses.collectAsState()
    val tenant by viewModel.currentTenant.collectAsState()
    val brandColors = LocalBrandColors.current

    var query by remember { mutableStateOf("") }
    var classFilter by remember { mutableStateOf("Semua") }
    var statusFilter by remember { mutableStateOf("Semua") }

    var showStudentDialog by remember { mutableStateOf(false) }
    var editingStudent by remember { mutableStateOf<Student?>(null) }
    var deletingStudent by remember { mutableStateOf<Student?>(null) }
    var showPinModal by remember { mutableStateOf(false) }

    val filtered = students.filter { s ->
        val matchQ = s.name.contains(query, true) || s.nisn.contains(query) || s.nis.contains(query)
        val matchC = classFilter == "Semua" || s.className.equals(classFilter, ignoreCase = true)
        val matchS = statusFilter == "Semua" || s.status.equals(statusFilter, ignoreCase = true)
        matchQ && matchC && matchS
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingStudent = null
                    showStudentDialog = true
                },
                containerColor = brandColors.primary,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_student_fab")
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Input Siswa Baru")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    SneatSearchBar(
                        query = query,
                        onQueryChange = { query = it },
                        placeholder = "Cari nama, NISN, atau NIS..."
                    )
                }
                FilledTonalButton(
                    onClick = { showPinModal = true },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(52.dp).testTag("manage_class_pin_button")
                ) {
                    Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PIN Kelas", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Filter Kelas
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    FilterChip(selected = classFilter == "Semua", onClick = { classFilter = "Semua" }, label = { Text("Semua Kelas", fontSize = 11.sp) })
                }
                items(classes) { c ->
                    FilterChip(selected = classFilter == c.name, onClick = { classFilter = c.name }, label = { Text(c.name, fontSize = 11.sp) })
                }
            }

            // Filter Status Siswa
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(listOf("Semua", "Aktif", "Pindah", "Lulus", "Dikeluarkan")) { st ->
                    FilterChip(
                        selected = statusFilter == st,
                        onClick = { statusFilter = st },
                        label = { Text(st, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = brandColors.primary, selectedLabelColor = Color.White)
                    )
                }
            }

            Text(
                text = "Menampilkan ${filtered.size} dari ${students.size} Siswa",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(filtered, key = { it.id }) { s ->
                    SneatCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(s.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("NISN: ${s.nisn} • NIS: ${s.nis.ifBlank { "-" }} • Tgl Lahir: ${s.birthDate.ifBlank { "-" }}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Ortu: ${s.parentName.ifBlank { "-" }} (${s.parentPhone})", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (s.promotionHistory.isNotBlank()) {
                                    Text("Riwayat Naik Kelas: ${s.promotionHistory}", fontSize = 10.sp, color = brandColors.primary)
                                }
                            }
                            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                SneatBadge(text = s.className, textColor = brandColors.primary, backgroundColor = brandColors.containerLight)
                                SneatBadge(
                                    text = s.status,
                                    textColor = if (s.status == "Aktif") SneatSuccess else if (s.status == "Lulus") SneatInfo else SneatDanger,
                                    backgroundColor = if (s.status == "Aktif") SneatSuccessLight else if (s.status == "Lulus") SneatInfoLight else SneatDangerLight
                                )
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            IconButton(onClick = {
                                editingStudent = s
                                showStudentDialog = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Siswa", tint = brandColors.primary)
                            }
                            IconButton(onClick = { deletingStudent = s }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus Siswa", tint = SneatDanger)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showStudentDialog) {
        FullStudentFormDialog(
            tenantId = tenant.idSekolah,
            availableClasses = classes.map { it.name },
            student = editingStudent,
            primaryColor = brandColors.primary,
            onDismiss = { showStudentDialog = false },
            onSave = { saved ->
                viewModel.saveStudent(saved) { showStudentDialog = false }
            }
        )
    }

    deletingStudent?.let { s ->
        SneatConfirmDialog(
            title = "Hapus Siswa",
            message = "Hapus data siswa ${s.name} (NISN: ${s.nisn})?",
            onConfirm = {
                viewModel.deleteStudent(s.id)
                deletingStudent = null
            },
            onDismiss = { deletingStudent = null }
        )
    }

    if (showPinModal) {
        AdminUserManagementDialog(viewModel = viewModel, onDismiss = { showPinModal = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullStudentFormDialog(
    tenantId: String,
    availableClasses: List<String>,
    student: Student?,
    primaryColor: Color,
    onDismiss: () -> Unit,
    onSave: (Student) -> Unit
) {
    var nisn by remember { mutableStateOf(student?.nisn ?: "") }
    var nis by remember { mutableStateOf(student?.nis ?: "") }
    var name by remember { mutableStateOf(student?.name ?: "") }
    var className by remember { mutableStateOf(student?.className ?: availableClasses.firstOrNull() ?: "XII MIPA 1") }
    var birthDate by remember { mutableStateOf(student?.birthDate ?: "2009-05-14") }
    var gender by remember { mutableStateOf(student?.gender ?: "L") }
    var address by remember { mutableStateOf(student?.address ?: "") }
    var parentName by remember { mutableStateOf(student?.parentName ?: "") }
    var parentPhone by remember { mutableStateOf(student?.parentPhone ?: student?.phone ?: "") }
    var status by remember { mutableStateOf(student?.status ?: "Aktif") }
    var statusExpanded by remember { mutableStateOf(false) }
    var promotionHistory by remember { mutableStateOf(student?.promotionHistory ?: "Naik ke $className") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (student == null) "Pendaftaran Siswa Baru" else "Edit Data Siswa", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = nisn, onValueChange = { nisn = it }, label = { Text("NISN (Min 8 Digit) *") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = nis, onValueChange = { nis = it }, label = { Text("NIS") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama Lengkap Siswa *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = className, onValueChange = { className = it }, label = { Text("Kelas (cth: XII MIPA 1) *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = birthDate, onValueChange = { birthDate = it }, label = { Text("Tanggal Lahir (YYYY-MM-DD)") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = gender == "L", onClick = { gender = "L" }, label = { Text("Laki-laki") })
                    FilterChip(selected = gender == "P", onClick = { gender = "P" }, label = { Text("Perempuan") })
                }

                ExposedDropdownMenuBox(expanded = statusExpanded, onExpandedChange = { statusExpanded = !statusExpanded }) {
                    OutlinedTextField(
                        value = status,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Status Siswa") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = statusExpanded, onDismissRequest = { statusExpanded = false }) {
                        listOf("Aktif", "Pindah", "Lulus", "Dikeluarkan").forEach { st ->
                            DropdownMenuItem(text = { Text(st) }, onClick = {
                                status = st
                                statusExpanded = false
                            })
                        }
                    }
                }

                OutlinedTextField(value = parentName, onValueChange = { parentName = it }, label = { Text("Nama Orang Tua / Wali") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = parentPhone, onValueChange = { parentPhone = it }, label = { Text("Nomor HP Orang Tua") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Alamat Lengkap") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = promotionHistory, onValueChange = { promotionHistory = it }, label = { Text("Riwayat Naik Kelas") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val id = student?.id ?: "S-${System.currentTimeMillis() % 10000}"
                    onSave(
                        Student(
                            id = id,
                            idSekolah = tenantId,
                            nisn = nisn,
                            nis = nis,
                            name = name,
                            className = className,
                            gender = gender,
                            religion = student?.religion ?: "Islam",
                            phone = parentPhone,
                            address = address,
                            status = status,
                            userId = student?.userId ?: "",
                            birthDate = birthDate,
                            parentName = parentName,
                            parentPhone = parentPhone,
                            promotionHistory = promotionHistory
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                shape = RoundedCornerShape(8.dp)
            ) { Text("Simpan Siswa") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@Composable
private fun AdminWhitelistTab(viewModel: SchoolViewModel) {
    val userAccounts by viewModel.userAccounts.collectAsState()
    val brandColors = LocalBrandColors.current
    var showManageModal by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = { showManageModal = true },
            colors = ButtonDefaults.buttonColors(containerColor = brandColors.primary),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Kelola Whitelist Akun & PIN Kelas", fontWeight = FontWeight.Bold)
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(userAccounts, key = { it.id }) { acc ->
                SneatCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(acc.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(acc.email, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${acc.jabatan} • NIP: ${acc.nip}", fontSize = 11.sp, color = brandColors.primary)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SneatBadge(
                                text = acc.role.label,
                                textColor = brandColors.primary,
                                backgroundColor = brandColors.containerLight
                            )
                            if (acc.role != UserRole.SUPERADMIN) {
                                IconButton(onClick = { viewModel.deleteStaffAccount(acc.id) }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus", tint = SneatDanger)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showManageModal) {
        AdminUserManagementDialog(viewModel = viewModel, onDismiss = { showManageModal = false })
    }
}
