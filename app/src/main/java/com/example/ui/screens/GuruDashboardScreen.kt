package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.Grading
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.SchoolViewModel
import java.util.Locale

@Composable
fun GuruDashboardScreen(
    viewModel: SchoolViewModel,
    onNavigateToMenu: (String) -> Unit
) {
    val tenant by viewModel.currentTenant.collectAsState()
    val schoolProfile by viewModel.currentSchoolProfile.collectAsState()
    val currentUserAccount by viewModel.currentUserAccount.collectAsState()
    val session by viewModel.authenticatedSession.collectAsState()
    val rppList by viewModel.guruFilteredLessonPlans.collectAsState()
    val journals by viewModel.guruFilteredJournals.collectAsState()
    val attendance by viewModel.guruFilteredAttendance.collectAsState()
    val grades by viewModel.guruFilteredGrades.collectAsState()
    val assignments by viewModel.guruFilteredAssignments.collectAsState()
    val schedules by viewModel.guruFilteredSchedules.collectAsState()
    val forumMessages by viewModel.forumMessages.collectAsState()
    val announcements by viewModel.activeAnnouncements.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val proSubscription by viewModel.proSubscription.collectAsState()
    val brandColors = LocalBrandColors.current

    var showProfileDialog by remember { mutableStateOf(false) }
    var showMateriDialog by remember { mutableStateOf(false) }
    var showProDialog by remember { mutableStateOf(false) }

    val teacherName = currentUserAccount?.name ?: session?.displayName ?: "Guru Pengajar"
    val teacherEmail = (currentUserAccount?.email ?: session?.email ?: "").lowercase()
    val teacherJabatan = currentUserAccount?.jabatan ?: "Guru Mata Pelajaran"
    val teacherNip = currentUserAccount?.nip ?: "-"
    val teacherId = currentUserAccount?.idGuru?.ifBlank { session?.idGuru ?: "T-01" } ?: "T-01"
    val assignedClasses = currentUserAccount?.idKelas?.ifEmpty { session?.idKelas ?: emptyList() } ?: emptyList()
    val assignedSubjects = currentUserAccount?.idMapel?.ifEmpty { session?.idMapel ?: emptyList() } ?: emptyList()

    val rppNotifs = remember(notifications, teacherEmail) {
        notifications.filter {
            it.recipientEmailOrRole.equals(teacherEmail, ignoreCase = true) ||
                it.recipientEmailOrRole.equals("GURU", ignoreCase = true)
        }
    }

    val guruAnnouncements = remember(announcements) {
        announcements.filter {
            it.targetRole.equals("Semua", ignoreCase = true) ||
                it.targetRole.equals("Guru", ignoreCase = true)
        }
    }

    val pendingRppCount = rppList.count { it.status == RppStatus.SUBMITTED || it.status == RppStatus.DRAFT }
    val approvedRppCount = rppList.count { it.status == RppStatus.APPROVED }
    val rejectedRppCount = rppList.count { it.status == RppStatus.REJECTED }
    val todaySchedulesCount = if (schedules.isNotEmpty()) schedules.size else maxOf(assignedClasses.size, journals.size.coerceAtLeast(1))
    val unfilledAttendanceCount = if (attendance.isEmpty()) maxOf(1, assignedClasses.size)
    else attendance.count { it.notes.isBlank() && it.status != AttendanceStatus.HADIR }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header Branded "Dashboard Guru" dengan Logo Sekolah
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(brandColors.headerBrush)
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            SchoolLogoAvatar(
                                logoBase64 = schoolProfile.logoBase64,
                                schoolName = schoolProfile.name,
                                size = 48.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    SneatBadge(
                                        text = "Dashboard Guru",
                                        textColor = brandColors.primary,
                                        backgroundColor = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    SneatBadge(
                                        text = teacherId,
                                        textColor = Color.White,
                                        backgroundColor = Color.White.copy(alpha = 0.22f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = teacherName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "${schoolProfile.name} • $teacherJabatan",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }

                        FilledTonalButton(
                            onClick = { showProfileDialog = true },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color.White.copy(alpha = 0.22f),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("guru_profile_button")
                        ) {
                            Icon(Icons.Default.Person, contentDescription = "Profil Guru", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Profil", fontSize = 12.sp)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.16f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Filter Otomatis: Kelas [${assignedClasses.joinToString(", ").ifBlank { "Kelas Diampu" }}] • Mapel [${assignedSubjects.joinToString(", ").ifBlank { "Mapel Diampu" }}] • TA ${schoolProfile.academicYear}",
                            fontSize = 10.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // 2. Card Ringkasan Utama Guru (Status RPP, Jadwal Hari Ini, Absensi Belum Terisi)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Ringkasan Hari Ini",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SneatStatCard(
                        title = "Status RPP",
                        value = "$approvedRppCount/${rppList.size}",
                        icon = Icons.Default.AutoStories,
                        iconColor = brandColors.primary,
                        iconBgColor = brandColors.containerLight,
                        subtext = "$pendingRppCount Menunggu • $rejectedRppCount Revisi",
                        modifier = Modifier.weight(1f)
                    )
                    SneatStatCard(
                        title = "Jadwal Hari Ini",
                        value = "$todaySchedulesCount Sesi",
                        icon = Icons.Default.Schedule,
                        iconColor = SneatSuccess,
                        iconBgColor = SneatSuccessLight,
                        subtext = "${assignedSubjects.firstOrNull() ?: "Semua Mapel"}",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SneatStatCard(
                        title = "Absensi Belum Terisi",
                        value = "$unfilledAttendanceCount",
                        icon = Icons.AutoMirrored.Filled.FactCheck,
                        iconColor = if (unfilledAttendanceCount > 0) SneatWarning else SneatSuccess,
                        iconBgColor = if (unfilledAttendanceCount > 0) SneatWarningLight else SneatSuccessLight,
                        subtext = if (unfilledAttendanceCount > 0) "Segera lengkapi presensi kelas" else "Semua kelas lengkap",
                        modifier = Modifier.weight(1f)
                    )
                    SneatStatCard(
                        title = "Materi / Tugas / Latihan",
                        value = "${assignments.size}",
                        icon = Icons.AutoMirrored.Filled.Assignment,
                        iconColor = SneatInfo,
                        iconBgColor = SneatInfoLight,
                        subtext = "${grades.size} Nilai Diinput",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 3. Notifikasi Persetujuan RPP Real-Time
        if (rppNotifs.isNotEmpty()) {
            item {
                SneatCard(
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    borderColor = SneatInfo.copy(alpha = 0.35f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = brandColors.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Notifikasi Real-Time (RPP & Forum)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        SneatBadge(
                            text = "${rppNotifs.count { !it.isRead }} Baru",
                            textColor = SneatDanger,
                            backgroundColor = SneatDangerLight
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    rppNotifs.take(3).forEach { notif ->
                        val isApproved = notif.type == "RPP_APPROVED"
                        val isRejected = notif.type == "RPP_REJECTED"
                        val isForum = notif.type.startsWith("FORUM_")
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when {
                                isApproved -> SneatSuccessLight.copy(alpha = 0.6f)
                                isRejected -> SneatDangerLight.copy(alpha = 0.6f)
                                else -> SneatInfoLight.copy(alpha = 0.5f)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clickable {
                                    viewModel.markNotificationRead(notif.id)
                                    if (isForum) onNavigateToMenu("FORUM_DISKUSI")
                                }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(notif.title, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text(notif.timestamp, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(notif.message, fontSize = 11.sp, lineHeight = 15.sp)
                            }
                        }
                    }
                }
            }
        }

        // 4. Pengumuman Sekolah untuk Guru
        if (guruAnnouncements.isNotEmpty()) {
            item {
                SneatCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Pengumuman Sekolah Terbaru", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        TextButton(onClick = { onNavigateToMenu("ANNOUNCEMENTS") }) {
                            Text("Semua (${guruAnnouncements.size})", fontSize = 11.sp)
                        }
                    }
                    guruAnnouncements.take(2).forEach { ann ->
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(ann.title, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Text(ann.content, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    }
                }
            }
        }

        // 5. 6 Menu Khusus Guru Pengajar (Route-Guarded)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Menu Guru Pengajar",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                val guruMenus = listOf(
                    Triple("FORUM_DISKUSI", "Forum Diskusi Lembaga", "${forumMessages.size} pesan • Chat teks & Voice-Note maks 30 dtk"),
                    Triple("SCHEDULE_CALENDAR", "Jadwal Mengajar & Kalender", "${schedules.size} sesi terfilter kelas & mapel Anda"),
                    Triple("RPP", "Kelola RPP Miliknya Saja", "CRUD, Upload <=5MB & Pengajuan Persetujuan"),
                    Triple("MATERI", "Materi, Tugas & Latihan", "Upload bahan ajar, tugas & latihan soal (<= 5 MB)"),
                    Triple("ABSENSI", "Absensi Kelas yang Diajar", "Input kehadiran siswa kelas terfilter"),
                    Triple("NILAI", "Nilai Siswa", "Kelola nilai Tugas, UTS & UAS siswa"),
                    Triple("JURNAL", "Dokumentasi Kegiatan Mengajar", "Jurnal KBM & foto dokumentasi (<= 5 MB)"),
                    Triple("PROFIL_GURU", "Profil Guru", "Lihat detail NIP, jabatan, mapel & kelas diampu")
                )
                guruMenus.forEach { (code, title, subtitle) ->
                    val (icon, tint, bg) = when (code) {
                        "FORUM_DISKUSI" -> Triple(Icons.Default.Forum, brandColors.primary, brandColors.containerLight)
                        "SCHEDULE_CALENDAR" -> Triple(Icons.Default.CalendarMonth, SneatSuccess, SneatSuccessLight)
                        "RPP" -> Triple(Icons.Default.AutoStories, brandColors.primary, brandColors.containerLight)
                        "MATERI" -> Triple(Icons.AutoMirrored.Filled.Assignment, SneatInfo, SneatInfoLight)
                        "ABSENSI" -> Triple(Icons.AutoMirrored.Filled.FactCheck, SneatSuccess, SneatSuccessLight)
                        "NILAI" -> Triple(Icons.AutoMirrored.Filled.Grading, SneatWarning, SneatWarningLight)
                        "JURNAL" -> Triple(Icons.Default.EditCalendar, SneatDanger, SneatDangerLight)
                        else -> Triple(Icons.Default.Person, brandColors.secondary, brandColors.containerLight)
                    }
                    GuruActionMenuCard(
                        title = title,
                        subtitle = subtitle,
                        icon = icon,
                        tint = tint,
                        bg = bg,
                        testTag = "guru_menu_$code"
                    ) {
                        when (code) {
                            "MATERI" -> showMateriDialog = true
                            "PROFIL_GURU" -> showProfileDialog = true
                            else -> onNavigateToMenu(code)
                        }
                    }
                }
            }
        }

        // 6. Materi / Tugas / Latihan Terbaru
        item {
            SneatCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Materi, Tugas & Latihan Terbaru", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    TextButton(onClick = { showMateriDialog = true }) {
                        Text("+ Tambah", fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                if (assignments.isEmpty()) {
                    Text("Belum ada materi/tugas untuk kelas Anda.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    assignments.take(4).forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                Text("${item.subject} • ${item.className} • Batas: ${item.deadline}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            SneatBadge(text = item.type, textColor = brandColors.primary, backgroundColor = brandColors.containerLight)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    }
                }
            }
        }

        item {
            SneatAdBanner(
                proSubscription = proSubscription,
                onUpgradeProClick = { showProDialog = true }
            )
        }
    }

    if (showProfileDialog) {
        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = { Text("Profil Guru Pengajar", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Nama: $teacherName", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("Email: $teacherEmail", fontSize = 12.sp)
                    Text("NIP: $teacherNip", fontSize = 12.sp)
                    Text("Jabatan: $teacherJabatan", fontSize = 12.sp)
                    Text("ID Guru: $teacherId", fontSize = 12.sp)
                    Text("Kelas Diampu: ${assignedClasses.joinToString(", ").ifBlank { "Semua Kelas Terkait" }}", fontSize = 12.sp)
                    Text("Mapel Diampu: ${assignedSubjects.joinToString(", ").ifBlank { "Semua Mapel Terkait" }}", fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { showProfileDialog = false }) { Text("Tutup") }
            }
        )
    }

    if (showMateriDialog) {
        GuruMateriAssignmentDialog(
            viewModel = viewModel,
            tenantId = tenant.idSekolah,
            teacherId = teacherId,
            defaultSubject = assignedSubjects.firstOrNull() ?: "Matematika Peminatan",
            defaultClass = assignedClasses.firstOrNull() ?: "XII MIPA 1",
            primaryColor = brandColors.primary,
            assignments = assignments,
            onDismiss = { showMateriDialog = false }
        )
    }

    if (showProDialog) {
        SneatProUpgradeDialog(
            proSubscription = proSubscription,
            onDismiss = { showProDialog = false },
            onTogglePro = { viewModel.togglePro() }
        )
    }
}

@Composable
private fun GuruActionMenuCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    bg: Color,
    testTag: String,
    onClick: () -> Unit
) {
    SneatCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(bg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = tint, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun GuruMateriAssignmentDialog(
    viewModel: SchoolViewModel,
    tenantId: String,
    teacherId: String,
    defaultSubject: String,
    defaultClass: String,
    primaryColor: Color,
    assignments: List<StudentAssignment>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf(defaultSubject) }
    var className by remember { mutableStateOf(defaultClass) }
    var description by remember { mutableStateOf("") }
    var deadline by remember { mutableStateOf("20 Nov 2026") }
    var selectedType by remember { mutableStateOf("Materi") }
    var fileName by remember { mutableStateOf("") }
    var fileSizeMb by remember { mutableStateOf("1.50") }
    var uploadErr by remember { mutableStateOf<String?>(null) }

    val docPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            context.processPickedDocument(uri)
                .onSuccess { res ->
                    fileName = res.fileName
                    fileSizeMb = String.format(Locale.US, "%.2f", (res.sizeBytes.toDouble() / (1024 * 1024)).coerceAtLeast(0.05))
                    uploadErr = null
                }
                .onFailure { uploadErr = it.localizedMessage }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Kelola Materi, Tugas & Latihan", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("Materi", "Tugas", "Latihan", "Uji Kompetensi")) { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type, fontSize = 10.sp) }
                        )
                    }
                }
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Judul Materi / Tugas / Latihan *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Mata Pelajaran") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = className, onValueChange = { className = it }, label = { Text("Kelas Target") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = deadline, onValueChange = { deadline = it }, label = { Text("Batas Waktu / Keterangan") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                FilledTonalButton(
                    onClick = { docPicker.launch(arrayOf("application/pdf", "image/png", "image/jpeg", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")) },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (fileName.isBlank()) "Pilih File Lampiran (Maks 5 MB)" else "File: $fileName", fontSize = 11.sp)
                }
                uploadErr?.let { Text(it, color = SneatDanger, fontSize = 11.sp, fontWeight = FontWeight.Bold) }

                OutlinedTextField(value = fileSizeMb, onValueChange = { fileSizeMb = it }, label = { Text("Ukuran File Lampiran (Maks 5.0 MB)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Deskripsi / Petunjuk Pengerjaan") }, modifier = Modifier.fillMaxWidth())

                if (assignments.isNotEmpty()) {
                    HorizontalDivider()
                    Text("Daftar Terpublikasi (${assignments.size}):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    assignments.forEach { a ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("[${a.type}] ${a.title}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                Text("${a.subject} • ${a.className}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { viewModel.deleteAssignment(a.id) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus", tint = SneatDanger, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        uploadErr = "Judul materi / tugas wajib diisi."
                        return@Button
                    }
                    val parsedMb = fileSizeMb.trim().replace(',', '.').toDoubleOrNull() ?: 1.0
                    val bytes = (parsedMb * 1024 * 1024).toLong()
                    if (!viewModel.validateUploadFileSize(bytes)) {
                        uploadErr = "Ukuran file melebihi batas maksimal 5 MB."
                        return@Button
                    }
                    uploadErr = null
                    viewModel.saveAssignment(
                        StudentAssignment(
                            id = "TSK-${System.currentTimeMillis() % 100000}",
                            idSekolah = tenantId,
                            className = className,
                            subject = subject,
                            title = title,
                            description = description,
                            deadline = deadline,
                            type = selectedType,
                            fileName = fileName.ifBlank { "${selectedType}_${title.take(12).replace(" ", "_")}.pdf" },
                            fileSizeBytes = bytes,
                            teacherId = teacherId
                        )
                    ) {
                        title = ""
                        description = ""
                        fileName = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Publikasikan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    )
}
