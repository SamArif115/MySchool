package com.example.ui.screens

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
import com.example.model.GradeRecord
import com.example.model.UserRole
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.SchoolViewModel
import java.util.Locale

@Composable
fun NilaiScreen(
    viewModel: SchoolViewModel
) {
    val allGrades by viewModel.activeGrades.collectAsState()
    val guruGrades by viewModel.guruFilteredGrades.collectAsState()
    val classes by viewModel.activeClasses.collectAsState()
    val tenant by viewModel.currentTenant.collectAsState()
    val currentRole by viewModel.currentRole.collectAsState()
    val currentUserAccount by viewModel.currentUserAccount.collectAsState()
    val session by viewModel.authenticatedSession.collectAsState()
    val brandColors = LocalBrandColors.current

    val grades = if (currentRole == UserRole.GURU) guruGrades else allGrades
    val studentSession by viewModel.currentStudentSession.collectAsState()
    val isStudent = currentRole == UserRole.SISWA
    val studentName = studentSession?.name ?: "Ahmad Fauzan Syahputra"
    val canManage = currentRole == UserRole.SUPERADMIN || currentRole == UserRole.GURU

    val guruAssignedClasses = remember(currentUserAccount, session) {
        currentUserAccount?.idKelas?.ifEmpty { session?.idKelas ?: emptyList() }
            ?: (session?.idKelas ?: emptyList())
    }
    val guruAssignedSubjects = remember(currentUserAccount, session) {
        currentUserAccount?.idMapel?.ifEmpty { session?.idMapel ?: emptyList() }
            ?: (session?.idMapel ?: emptyList())
    }

    val classOptions = remember(classes, currentRole, guruAssignedClasses) {
        val baseNames = if (currentRole == UserRole.GURU && guruAssignedClasses.isNotEmpty()) {
            guruAssignedClasses
        } else {
            classes.map { it.name }.ifEmpty { listOf("XII MIPA 1", "XI IPS 2", "X Merdeka 1") }
        }
        listOf("Semua") + baseNames.distinct()
    }
    var selectedClassFilter by remember(classOptions) { mutableStateOf("Semua") }

    var showAddEditDialog by remember { mutableStateOf(false) }
    var gradeToEdit by remember { mutableStateOf<GradeRecord?>(null) }
    var gradeToDelete by remember { mutableStateOf<GradeRecord?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }

    val filteredGrades = grades.filter { g ->
        val matchesStudent = if (isStudent) {
            g.studentName.contains(studentName, ignoreCase = true) ||
                studentName.contains(g.studentName, ignoreCase = true)
        } else true
        val matchesClass = selectedClassFilter == "Semua" || g.className.equals(selectedClassFilter, ignoreCase = true)
        matchesStudent && matchesClass
    }

    Scaffold(
        floatingActionButton = {
            if (canManage) {
                FloatingActionButton(
                    onClick = {
                        gradeToEdit = null
                        showAddEditDialog = true
                    },
                    containerColor = brandColors.primary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Input Nilai")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isStudent) "Rapor & Nilai Pribadi Siswa" else "Rapor & Rekap Nilai Siswa",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Komposisi: Tugas 30% • UTS 30% • UAS 40%",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = { showExportDialog = true }) {
                    Icon(Icons.Default.FileDownload, contentDescription = "Export Nilai", tint = brandColors.primary)
                }
            }

            if (!isStudent) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(classOptions) { opt ->
                        val isSelected = opt == selectedClassFilter
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedClassFilter = opt },
                            label = { Text(opt, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = brandColors.primary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            if (filteredGrades.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Belum ada rekap nilai untuk kelas yang dipilih.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredGrades, key = { it.id }) { item ->
                        GradeItemCard(
                            grade = item,
                            canManage = canManage,
                            primaryColor = brandColors.primary,
                            primaryLight = brandColors.containerLight,
                            onEdit = {
                                gradeToEdit = item
                                showAddEditDialog = true
                            },
                            onDelete = { gradeToDelete = item }
                        )
                    }
                }
            }
        }
    }

    if (showAddEditDialog) {
        AddEditGradeDialog(
            tenantId = tenant.idSekolah,
            defaultClass = guruAssignedClasses.firstOrNull() ?: "XII MIPA 1",
            defaultSubject = guruAssignedSubjects.firstOrNull() ?: "Matematika Peminatan",
            primaryColor = brandColors.primary,
            grade = gradeToEdit,
            onDismiss = { showAddEditDialog = false },
            onSave = { saved ->
                viewModel.saveGrade(saved) {
                    showAddEditDialog = false
                }
            }
        )
    }

    gradeToDelete?.let { g ->
        SneatConfirmDialog(
            title = "Hapus Nilai Siswa",
            message = "Hapus rekap nilai ${g.studentName} untuk mapel ${g.subjectName}?",
            onConfirm = {
                viewModel.deleteGrade(g.id)
                gradeToDelete = null
            },
            onDismiss = { gradeToDelete = null }
        )
    }

    if (showExportDialog) {
        SneatExportDialog(
            title = "Rekapitulasi Nilai & Rapor",
            schoolName = tenant.name,
            onDismiss = { showExportDialog = false },
            onConfirmExport = {}
        )
    }
}

@Composable
private fun GradeItemCard(
    grade: GradeRecord,
    canManage: Boolean,
    primaryColor: Color,
    primaryLight: Color,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    SneatCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = grade.studentName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${grade.className} • ${grade.subjectName}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SneatBadge(
                    text = "Nilai: ${String.format(Locale.US, "%.1f", grade.finalScore)} (${grade.letterGrade})",
                    textColor = if (grade.isPassed) SneatSuccess else SneatDanger,
                    backgroundColor = if (grade.isPassed) SneatSuccessLight else SneatDangerLight
                )
                SneatBadge(
                    text = if (grade.isPassed) "Tuntas" else "Remedial",
                    textColor = if (grade.isPassed) primaryColor else SneatWarning,
                    backgroundColor = if (grade.isPassed) primaryLight else SneatWarningLight
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column {
                    Text("Tugas", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${grade.tugasScore.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("UTS", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${grade.utsScore.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("UAS", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${grade.uasScore.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (canManage) {
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = primaryColor, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus", tint = SneatDanger, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AddEditGradeDialog(
    tenantId: String,
    defaultClass: String,
    defaultSubject: String,
    primaryColor: Color,
    grade: GradeRecord?,
    onDismiss: () -> Unit,
    onSave: (GradeRecord) -> Unit
) {
    var studentName by remember { mutableStateOf(grade?.studentName ?: "Ahmad Fauzan Syahputra") }
    var className by remember { mutableStateOf(grade?.className ?: defaultClass) }
    var subjectName by remember { mutableStateOf(grade?.subjectName ?: defaultSubject) }
    var tugas by remember { mutableStateOf(grade?.tugasScore?.toInt()?.toString() ?: "85") }
    var uts by remember { mutableStateOf(grade?.utsScore?.toInt()?.toString() ?: "80") }
    var uas by remember { mutableStateOf(grade?.uasScore?.toInt()?.toString() ?: "85") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (grade == null) "Input Nilai Siswa" else "Edit Nilai Siswa", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = studentName, onValueChange = { studentName = it }, label = { Text("Nama Siswa") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = className, onValueChange = { className = it }, label = { Text("Kelas") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = subjectName, onValueChange = { subjectName = it }, label = { Text("Mata Pelajaran") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = tugas, onValueChange = { tugas = it }, label = { Text("Tugas (30%)") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = uts, onValueChange = { uts = it }, label = { Text("UTS (30%)") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = uas, onValueChange = { uas = it }, label = { Text("UAS (40%)") }, singleLine = true, modifier = Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val id = grade?.id ?: "G-${System.currentTimeMillis() % 10000}"
                    onSave(
                        GradeRecord(
                            id = id,
                            idSekolah = tenantId,
                            studentId = grade?.studentId ?: "S-99",
                            studentName = studentName,
                            className = className,
                            subjectName = subjectName,
                            tugasScore = tugas.trim().replace(',', '.').toDoubleOrNull() ?: 80.0,
                            utsScore = uts.trim().replace(',', '.').toDoubleOrNull() ?: 80.0,
                            uasScore = uas.trim().replace(',', '.').toDoubleOrNull() ?: 80.0
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Simpan Nilai")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
