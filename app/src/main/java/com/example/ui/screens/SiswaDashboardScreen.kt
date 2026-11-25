package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.SchoolViewModel
import java.util.Locale

@Composable
fun SiswaDashboardScreen(
    viewModel: SchoolViewModel,
    onNavigateToMenu: (String) -> Unit
) {
    val tenant by viewModel.currentTenant.collectAsState()
    val studentSession by viewModel.currentStudentSession.collectAsState()
    val session by viewModel.authenticatedSession.collectAsState()
    val assignments by viewModel.activeAssignments.collectAsState()
    val attendance by viewModel.activeAttendance.collectAsState()
    val grades by viewModel.activeGrades.collectAsState()
    val proSubscription by viewModel.proSubscription.collectAsState()

    val studentName = studentSession?.name ?: session?.displayName ?: "Siswa Terdaftar"
    val studentClass = studentSession?.className?.ifBlank { session?.className ?: "XII MIPA 1" } ?: "XII MIPA 1"
    val studentUid = session?.uid ?: ""

    val personalAttendance = remember(attendance, studentName, studentUid) {
        attendance.filter {
            (studentUid.isNotBlank() && it.studentUserId == studentUid) ||
                it.studentName.equals(studentName, ignoreCase = true)
        }
    }
    val personalGrades = remember(grades, studentName, studentUid) {
        grades.filter {
            (studentUid.isNotBlank() && it.studentUserId == studentUid) ||
                it.studentName.equals(studentName, ignoreCase = true)
        }
    }
    val classAssignments = remember(assignments, studentClass) {
        assignments.filter { it.className.equals(studentClass, ignoreCase = true) || it.className.isBlank() }
    }

    var selectedType by remember { mutableStateOf("Semua") }
    val types = listOf("Semua", "Tugas", "Materi", "Uji Kompetensi")
    val filteredAssignments = remember(classAssignments, selectedType) {
        if (selectedType == "Semua") classAssignments else classAssignments.filter { it.type == selectedType }
    }

    val hadirCount = personalAttendance.count { it.status == AttendanceStatus.HADIR }
    val attendancePct = if (personalAttendance.isNotEmpty()) (hadirCount * 100) / personalAttendance.size else 100
    val avgScore = if (personalGrades.isNotEmpty()) String.format(Locale.US, "%.1f", personalGrades.map { it.finalScore }.average()) else "-"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header Portal Siswa Pribadi
        item {
            SneatCard(
                backgroundColor = SneatInfo,
                borderColor = Color.Transparent
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        SneatBadge(
                            text = "PORTAL PRIBADI SISWA",
                            textColor = SneatInfo,
                            backgroundColor = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = studentName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "Kelas: $studentClass • ${tenant.name}",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        // 2. Ringkasan Pribadi (Absensi + Nilai Pribadi)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SneatStatCard(
                    title = "Absensi Pribadi",
                    value = "$attendancePct%",
                    icon = Icons.AutoMirrored.Filled.FactCheck,
                    iconColor = SneatSuccess,
                    iconBgColor = SneatSuccessLight,
                    subtext = "${personalAttendance.size} Catatan Anda",
                    modifier = Modifier.weight(1f)
                )
                SneatStatCard(
                    title = "Rata-rata Nilai",
                    value = avgScore,
                    icon = Icons.AutoMirrored.Filled.Grading,
                    iconColor = SneatPrimary,
                    iconBgColor = SneatPrimaryLight,
                    subtext = "${personalGrades.size} Mata Pelajaran",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 3. Tugas, Materi & Latihan Uji Kompetensi
        item {
            SneatCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null, tint = SneatPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tugas, Materi & Uji Kompetensi", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(types) { t ->
                        FilterChip(
                            selected = selectedType == t,
                            onClick = { selectedType = t },
                            label = { Text(t, fontSize = 11.sp) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                if (filteredAssignments.isEmpty()) {
                    Text("Belum ada $selectedType untuk kelas $studentClass.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    filteredAssignments.forEach { item ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(item.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                    SneatBadge(item.type, SneatPrimary, SneatPrimaryLight)
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("${item.subject} • Tenggat: ${item.deadline}", fontSize = 11.sp, color = SneatPrimary)
                                if (item.description.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(item.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Rincian Nilai Pribadi & Absensi Pribadi
        item {
            SneatCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Nilai Rapor Pribadi Saya", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    TextButton(onClick = { onNavigateToMenu("NILAI") }) {
                        Text("Detail", fontSize = 11.sp)
                    }
                }
                if (personalGrades.isEmpty()) {
                    Text("Belum ada nilai rapor pribadi yang tercatat.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    personalGrades.forEach { g ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(g.subjectName, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                Text("Tugas: ${g.tugasScore.toInt()} • UTS: ${g.utsScore.toInt()} • UAS: ${g.uasScore.toInt()}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            SneatBadge(
                                text = "${String.format(Locale.US, "%.1f", g.finalScore)} (${g.letterGrade})",
                                textColor = if (g.isPassed) SneatSuccess else SneatDanger,
                                backgroundColor = if (g.isPassed) SneatSuccessLight else SneatDangerLight
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    }
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
}
