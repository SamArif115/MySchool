package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.UserRole
import com.example.ui.components.SneatBadge
import com.example.ui.components.SneatCard
import com.example.ui.components.SneatExportDialog
import com.example.ui.components.SneatRouteGuardRestricted
import com.example.ui.theme.*
import com.example.viewmodel.SchoolViewModel

private data class ExportReportOption(
    val title: String,
    val subtitle: String,
    val badge: String,
    val icon: ImageVector
)

@Composable
fun ExportLaporanScreen(
    viewModel: SchoolViewModel
) {
    val currentRole by viewModel.currentRole.collectAsState()
    val profile by viewModel.currentSchoolProfile.collectAsState()
    val teachers by viewModel.activeTeachers.collectAsState()
    val students by viewModel.activeStudents.collectAsState()
    val attendance by viewModel.activeAttendance.collectAsState()
    val grades by viewModel.activeGrades.collectAsState()
    val journals by viewModel.activeTeachingJournals.collectAsState()
    val brandColors = LocalBrandColors.current

    if (currentRole != UserRole.SUPERADMIN) {
        SneatRouteGuardRestricted(
            currentRole = currentRole,
            requiredRole = "Admin Utama Sekolah",
            onSwitchRoleClick = {}
        )
        return
    }

    var selectedReportTitle by remember { mutableStateOf<String?>(null) }

    val pnsP3kCount = teachers.count {
        it.employmentStatus.equals("PNS", true) ||
            it.employmentStatus.equals("P3K", true) ||
            it.employmentStatus.equals("PPPK", true)
    }

    val reports = listOf(
        ExportReportOption(
            title = "Laporan Tunjangan Guru (PNS / P3K)",
            subtitle = "Rekap NIP, Golongan Ruang, SK, TMT & Beban Jam Mengajar ($pnsP3kCount guru memenuhi syarat)",
            badge = "$pnsP3kCount PNS/P3K",
            icon = Icons.Default.Badge
        ),
        ExportReportOption(
            title = "Data Induk Siswa & Riwayat Naik Kelas",
            subtitle = "NISN, NIS, Status Aktif/Pindah/Lulus, Nama & No. HP Orang Tua (${students.size} siswa)",
            badge = "${students.size} Siswa",
            icon = Icons.Default.People
        ),
        ExportReportOption(
            title = "Rekap Absensi Harian Seluruh Kelas",
            subtitle = "Data Hadir, Sakit, Izin & Alpha periode ${profile.academicYear} (${attendance.size} catatan)",
            badge = "${attendance.size} Catatan",
            icon = Icons.AutoMirrored.Filled.FactCheck
        ),
        ExportReportOption(
            title = "Rekap Rapor & Nilai Akhir Siswa",
            subtitle = "Nilai Tugas (30%), UTS (30%), UAS (40%) & predikat kelulusan (${grades.size} nilai)",
            badge = "${grades.size} Nilai",
            icon = Icons.AutoMirrored.Filled.Grading
        ),
        ExportReportOption(
            title = "Dokumentasi Jurnal KBM Guru",
            subtitle = "Bukti pelaksanaan kegiatan belajar mengajar untuk supervisi & tunjangan (${journals.size} jurnal)",
            badge = "${journals.size} Jurnal",
            icon = Icons.Default.EditCalendar
        )
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Pusat Export Laporan PDF & Excel",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${profile.name} • Tahun Ajaran ${profile.academicYear} (${profile.semester})",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(reports) { report ->
            SneatCard(modifier = Modifier.clickable { selectedReportTitle = report.title }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(report.icon, contentDescription = null, tint = brandColors.primary, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(report.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(report.subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(6.dp))
                        SneatBadge(text = report.badge, textColor = brandColors.primary, backgroundColor = brandColors.containerLight)
                    }
                    FilledTonalButton(
                        onClick = { selectedReportTitle = report.title },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export", fontSize = 11.sp)
                    }
                }
            }
        }
    }

    selectedReportTitle?.let { reportTitle ->
        SneatExportDialog(
            title = reportTitle,
            schoolName = profile.name,
            onDismiss = { selectedReportTitle = null },
            onConfirmExport = {}
        )
    }
}
