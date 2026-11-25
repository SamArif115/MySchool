package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.Grading
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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
fun KepsekDashboardScreen(
    viewModel: SchoolViewModel,
    onNavigateToRppApproval: () -> Unit
) {
    val tenant by viewModel.currentTenant.collectAsState()
    val schoolProfile by viewModel.currentSchoolProfile.collectAsState()
    val teachers by viewModel.activeTeachers.collectAsState()
    val students by viewModel.activeStudents.collectAsState()
    val subjects by viewModel.activeSubjects.collectAsState()
    val lessonPlans by viewModel.activeLessonPlans.collectAsState()
    val journals by viewModel.activeTeachingJournals.collectAsState()
    val attendance by viewModel.activeAttendance.collectAsState()
    val grades by viewModel.activeGrades.collectAsState()
    val proSubscription by viewModel.proSubscription.collectAsState()

    var selectedTeacherName by remember { mutableStateOf("Semua Guru") }
    var selectedSubject by remember { mutableStateOf("Semua Mapel") }
    var selectedPeriod by remember { mutableStateOf("Semester Ganjil 2026") }
    var showExportDialog by remember { mutableStateOf(false) }
    var rppToReview by remember { mutableStateOf<LessonPlan?>(null) }
    var reviewAction by remember { mutableStateOf("APPROVE") }

    val periodOptions = listOf("Semester Ganjil 2026", "2026-11 (November)", "2026-10 (Oktober)", "Semua Periode")

    val filteredRpps = remember(lessonPlans, selectedTeacherName, selectedSubject) {
        lessonPlans.filter { rpp ->
            (selectedTeacherName == "Semua Guru" || rpp.teacherName == selectedTeacherName) &&
                (selectedSubject == "Semua Mapel" || rpp.subject == selectedSubject)
        }
    }

    val filteredJournals = remember(journals, selectedTeacherName, selectedSubject, selectedPeriod) {
        journals.filter { j ->
            val matchTeacher = selectedTeacherName == "Semua Guru" || j.teacherName == selectedTeacherName
            val matchSubject = selectedSubject == "Semua Mapel" || j.subject == selectedSubject
            val matchPeriod = when {
                selectedPeriod.startsWith("2026-11") -> j.date.startsWith("2026-11")
                selectedPeriod.startsWith("2026-10") -> j.date.startsWith("2026-10")
                else -> true
            }
            matchTeacher && matchSubject && matchPeriod
        }
    }

    val filteredGrades = remember(grades, selectedSubject) {
        grades.filter { g -> selectedSubject == "Semua Mapel" || g.subjectName == selectedSubject }
    }

    val totalRpp = filteredRpps.size
    val approvedCount = filteredRpps.count { it.status == RppStatus.APPROVED }
    val pendingCount = filteredRpps.count { it.status == RppStatus.SUBMITTED || it.status == RppStatus.DRAFT }
    val rejectedCount = filteredRpps.count { it.status == RppStatus.REJECTED }

    val approvedPct = if (totalRpp > 0) (approvedCount * 100) / totalRpp else 0
    val pendingPct = if (totalRpp > 0) (pendingCount * 100) / totalRpp else 0
    val rejectedPct = if (totalRpp > 0) (rejectedCount * 100) / totalRpp else 0

    val attendanceRate = if (attendance.isNotEmpty()) {
        (attendance.count { it.status == AttendanceStatus.HADIR } * 100) / attendance.size
    } else 95

    val avgStudentScore = if (filteredGrades.isNotEmpty()) {
        filteredGrades.map { it.finalScore }.average()
    } else 84.5

    val approvalQueue = remember(lessonPlans) {
        lessonPlans.filter { it.status == RppStatus.SUBMITTED || it.status == RppStatus.DRAFT }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header Kepala Sekolah (Read-Only Master Data + RPP Approval)
        item {
            SneatCard(
                backgroundColor = SneatPrimary,
                borderColor = Color.Transparent
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SneatBadge(
                                text = "DASHBOARD KEPSEK",
                                textColor = SneatPrimary,
                                backgroundColor = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            SneatBadge(
                                text = "READ-ONLY + APPROVAL RPP",
                                textColor = Color.White,
                                backgroundColor = Color.White.copy(alpha = 0.22f)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = schoolProfile.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "Pimpinan: ${schoolProfile.kepalaSekolah} • ${tenant.idSekolah}",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                    FilledTonalButton(
                        onClick = { showExportDialog = true },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color.White.copy(alpha = 0.25f),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Laporan", fontSize = 11.sp)
                    }
                }
            }
        }

        // 2. Ringkasan Eksekutif Sekolah
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Ringkasan Sekolah & Antrian Persetujuan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SneatStatCard(
                        title = "Total Guru & Siswa",
                        value = "${teachers.size} / ${students.size}",
                        icon = Icons.Default.Groups,
                        iconColor = SneatPrimary,
                        iconBgColor = SneatPrimaryLight,
                        subtext = "Tenaga Pendidik & Siswa",
                        modifier = Modifier.weight(1f)
                    )
                    SneatStatCard(
                        title = "RPP Menunggu Review",
                        value = "${approvalQueue.size}",
                        icon = Icons.Default.PendingActions,
                        iconColor = SneatWarning,
                        iconBgColor = SneatWarningLight,
                        subtext = "Siap di-Approve / Revisi",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 3. Antrian Persetujuan RPP (Approve / Reject + Catatan; Tidak Edit Isi RPP)
        item {
            SneatCard(borderColor = SneatPrimary.copy(alpha = 0.25f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Persetujuan RPP Guru", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            "Kepsek dapat Approve / Reject + catatan (tidak mengubah isi dokumen RPP)",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(
                        onClick = onNavigateToRppApproval,
                        modifier = Modifier.testTag("kepsek_open_all_rpp_button")
                    ) {
                        Text("Semua RPP (${lessonPlans.size})", fontSize = 11.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                if (approvalQueue.isEmpty()) {
                    Text(
                        text = "Semua RPP guru sudah ditinjau.",
                        fontSize = 12.sp,
                        color = SneatSuccess,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    approvalQueue.forEach { rpp ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(rpp.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("${rpp.teacherName} • ${rpp.subject} (${rpp.className})", fontSize = 11.sp, color = SneatPrimary)
                                    }
                                    SneatBadge(
                                        text = rpp.status.label,
                                        textColor = SneatInfo,
                                        backgroundColor = SneatInfoLight
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            rppToReview = rpp
                                            reviewAction = "APPROVE"
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SneatSuccess),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.weight(1f).testTag("kepsek_approve_${rpp.id}")
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Approve", fontSize = 11.sp)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            rppToReview = rpp
                                            reviewAction = "REJECT"
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.weight(1f).testTag("kepsek_reject_${rpp.id}")
                                    ) {
                                        Icon(Icons.Default.Cancel, contentDescription = null, tint = SneatDanger, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Revisi / Reject", fontSize = 11.sp, color = SneatDanger)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. MONITORING KINERJA GURU PER MATA PELAJARAN (Filter Guru, Mapel, Periode + Grafik)
        item {
            SneatCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Analytics, contentDescription = null, tint = SneatPrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Monitoring Kinerja Guru per Mata Pelajaran",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Filter guru, mapel & periode bulan/semester",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Filter Guru
                Text("Filter Guru:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val teacherNames = listOf("Semua Guru") + teachers.map { it.name }.distinct()
                    items(teacherNames) { name ->
                        FilterChip(
                            selected = selectedTeacherName == name,
                            onClick = { selectedTeacherName = name },
                            label = { Text(name, fontSize = 10.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Filter Mata Pelajaran
                Text("Filter Mata Pelajaran:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val subjectNames = listOf("Semua Mapel") + subjects.map { it.name }.distinct()
                    items(subjectNames) { subj ->
                        FilterChip(
                            selected = selectedSubject == subj,
                            onClick = { selectedSubject = subj },
                            label = { Text(subj, fontSize = 10.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Filter Periode Bulan / Semester
                Text("Filter Periode Bulan / Semester:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(periodOptions) { period ->
                        FilterChip(
                            selected = selectedPeriod == period,
                            onClick = { selectedPeriod = period },
                            label = { Text(period, fontSize = 10.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(12.dp))

                // KPI Cards Hasil Monitoring
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MonitoringMiniMetric(
                        label = "Dok. Mengajar",
                        value = "${filteredJournals.size}",
                        sub = "Jurnal Tunjangan",
                        color = SneatPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    MonitoringMiniMetric(
                        label = "Rekap Absensi",
                        value = "$attendanceRate%",
                        sub = "${attendance.size} Catatan",
                        color = SneatSuccess,
                        modifier = Modifier.weight(1f)
                    )
                    MonitoringMiniMetric(
                        label = "Rata-rata Nilai",
                        value = String.format(Locale.US, "%.1f", avgStudentScore),
                        sub = "${filteredGrades.size} Penilaian",
                        color = SneatInfo,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Grafik Ringkasan Persentase RPP & Kinerja Guru (Canvas Chart)
                Text(
                    text = "Grafik Ringkasan Persentase RPP & Kinerja",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                KepsekPerformanceBarChart(
                    approvedPct = approvedPct,
                    pendingPct = pendingPct,
                    rejectedPct = rejectedPct,
                    attendancePct = attendanceRate,
                    avgScorePct = avgStudentScore.toInt().coerceIn(0, 100)
                )

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SneatBadge("Approved: $approvedPct% ($approvedCount)", SneatSuccess, SneatSuccessLight)
                    SneatBadge("Pending: $pendingPct% ($pendingCount)", SneatInfo, SneatInfoLight)
                    SneatBadge("Revisi: $rejectedPct% ($rejectedCount)", SneatDanger, SneatDangerLight)
                }
            }
        }

        // 5. Tabel Ringkas Per Guru Per Mapel
        item {
            SneatCard {
                Text("Rincian Dokumentasi & RPP per Guru", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                teachers
                    .filter { selectedTeacherName == "Semua Guru" || it.name == selectedTeacherName }
                    .forEach { teacher ->
                        val tRpps = filteredRpps.filter { it.teacherName == teacher.name || it.teacherId == teacher.id }
                        val tJournals = filteredJournals.filter { it.teacherName == teacher.name || it.teacherId == teacher.id }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(teacher.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(teacher.subject, fontSize = 11.sp, color = SneatPrimary)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("RPP: ${tRpps.size} • Dok. KBM: ${tJournals.size}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = "Disetujui: ${tRpps.count { it.status == RppStatus.APPROVED }}",
                                    fontSize = 10.sp,
                                    color = SneatSuccess
                                )
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    }
            }
        }

        item {
            SneatAdBanner(
                proSubscription = proSubscription,
                onUpgradeProClick = {}
            )
        }
    }

    rppToReview?.let { rpp ->
        var notes by remember(rpp, reviewAction) {
            mutableStateOf(
                if (reviewAction == "APPROVE") "RPP disetujui. Modul ajar lengkap dan siap dilaksanakan."
                else "Mohon lengkapi rubrik asesmen sebelum disetujui."
            )
        }
        AlertDialog(
            onDismissRequest = { rppToReview = null },
            title = {
                Text(
                    text = if (reviewAction == "APPROVE") "Setujui RPP (Approve)" else "Minta Revisi RPP (Reject)",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${rpp.title} • ${rpp.teacherName}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Catatan Kepala Sekolah") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (reviewAction == "APPROVE") {
                            viewModel.approveRppByKepsek(rpp.id, notes)
                        } else {
                            viewModel.rejectRppByKepsek(rpp.id, notes)
                        }
                        rppToReview = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (reviewAction == "APPROVE") SneatSuccess else SneatDanger
                    )
                ) {
                    Text(if (reviewAction == "APPROVE") "Kirim Persetujuan" else "Kirim Catatan Revisi")
                }
            },
            dismissButton = {
                TextButton(onClick = { rppToReview = null }) { Text("Batal") }
            }
        )
    }

    if (showExportDialog) {
        SneatExportDialog(
            title = "Laporan Monitoring Kinerja Guru",
            schoolName = schoolProfile.name,
            onDismiss = { showExportDialog = false },
            onConfirmExport = {}
        )
    }
}

@Composable
private fun MonitoringMiniMetric(
    label: String,
    value: String,
    sub: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.1f),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Text(sub, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun KepsekPerformanceBarChart(
    approvedPct: Int,
    pendingPct: Int,
    rejectedPct: Int,
    attendancePct: Int,
    avgScorePct: Int
) {
    val bars = listOf(
        Triple("RPP OK", approvedPct, SneatSuccess),
        Triple("Pending", pendingPct, SneatInfo),
        Triple("Revisi", rejectedPct, SneatDanger),
        Triple("Absensi", attendancePct, SneatPrimary),
        Triple("Nilai", avgScorePct, SneatWarning)
    )
    Column {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            val count = bars.size
            val slotWidth = size.width / count
            val barWidth = slotWidth * 0.52f
            bars.forEachIndexed { idx, (_, pct, color) ->
                val ratio = (pct.coerceIn(0, 100)) / 100f
                val barHeight = (size.height * ratio).coerceAtLeast(6f)
                val x = idx * slotWidth + (slotWidth - barWidth) / 2f
                val y = size.height - barHeight
                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(12f, 12f)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            bars.forEach { (label, pct, color) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$pct%", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
                    Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
