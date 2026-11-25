package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.model.Student
import com.example.model.UserRole
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.SchoolViewModel

@Composable
fun SiswaScreen(
    viewModel: SchoolViewModel
) {
    val students by viewModel.activeStudents.collectAsState()
    val classes by viewModel.activeClasses.collectAsState()
    val tenant by viewModel.currentTenant.collectAsState()
    val currentRole by viewModel.currentRole.collectAsState()
    val brandColors = LocalBrandColors.current
    val canManage = currentRole == UserRole.SUPERADMIN || currentRole == UserRole.GURU

    var searchQuery by remember { mutableStateOf("") }
    var selectedClassFilter by remember { mutableStateOf("Semua") }
    var selectedStatusFilter by remember { mutableStateOf("Semua") }

    var showAddEditDialog by remember { mutableStateOf(false) }
    var studentToEdit by remember { mutableStateOf<Student?>(null) }
    var studentToDelete by remember { mutableStateOf<Student?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }

    val filteredStudents = students.filter { s ->
        val matchesQuery = s.name.contains(searchQuery, ignoreCase = true) ||
            s.nisn.contains(searchQuery) ||
            s.nis.contains(searchQuery) ||
            s.className.contains(searchQuery, ignoreCase = true)
        val matchesClass = selectedClassFilter == "Semua" || s.className.equals(selectedClassFilter, ignoreCase = true)
        val matchesStatus = selectedStatusFilter == "Semua" || s.status.equals(selectedStatusFilter, ignoreCase = true)
        matchesQuery && matchesClass && matchesStatus
    }

    Scaffold(
        floatingActionButton = {
            if (canManage) {
                FloatingActionButton(
                    onClick = {
                        studentToEdit = null
                        showAddEditDialog = true
                    },
                    containerColor = brandColors.primary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Tambah Siswa")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    SneatSearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        placeholder = "Cari nama, NISN, atau NIS..."
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { showExportDialog = true },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = "Export Siswa", tint = brandColors.primary)
                }
            }

            // Filter Kelas
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedClassFilter == "Semua",
                        onClick = { selectedClassFilter = "Semua" },
                        label = { Text("Semua Kelas", fontSize = 11.sp) }
                    )
                }
                items(classes) { c ->
                    FilterChip(
                        selected = selectedClassFilter == c.name,
                        onClick = { selectedClassFilter = c.name },
                        label = { Text(c.name, fontSize = 11.sp) }
                    )
                }
            }

            // Filter Status Siswa
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("Semua", "Aktif", "Pindah", "Lulus", "Dikeluarkan")) { st ->
                    FilterChip(
                        selected = selectedStatusFilter == st,
                        onClick = { selectedStatusFilter = st },
                        label = { Text(st, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = brandColors.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Siswa Terdaftar: ${filteredStudents.size}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = tenant.name,
                    fontSize = 12.sp,
                    color = brandColors.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            if (filteredStudents.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Tidak ada data siswa", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredStudents, key = { it.id }) { student ->
                        SneatCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(if (student.gender == "L") SneatInfoLight else SneatDangerLight),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (student.gender == "L") Icons.Default.Face else Icons.Default.Face3,
                                            contentDescription = null,
                                            tint = if (student.gender == "L") SneatInfo else SneatDanger,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = student.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "NISN: ${student.nisn} • NIS: ${student.nis.ifBlank { "-" }}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    SneatBadge(text = student.className, textColor = brandColors.primary, backgroundColor = brandColors.containerLight)
                                    SneatBadge(
                                        text = student.status,
                                        textColor = if (student.status == "Aktif") SneatSuccess else SneatWarning,
                                        backgroundColor = if (student.status == "Aktif") SneatSuccessLight else SneatWarningLight
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Tgl Lahir: ${student.birthDate.ifBlank { "-" }} • Ortu: ${student.parentName.ifBlank { "-" }} (${student.parentPhone})", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(student.address, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (student.promotionHistory.isNotBlank()) {
                                        Text("Riwayat Naik Kelas: ${student.promotionHistory}", fontSize = 10.sp, color = brandColors.primary)
                                    }
                                }
                                if (canManage) {
                                    Row {
                                        IconButton(onClick = {
                                            studentToEdit = student
                                            showAddEditDialog = true
                                        }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = brandColors.primary)
                                        }
                                        IconButton(onClick = { studentToDelete = student }) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus", tint = SneatDanger)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddEditDialog) {
        FullStudentFormDialog(
            tenantId = tenant.idSekolah,
            availableClasses = classes.map { it.name },
            student = studentToEdit,
            primaryColor = brandColors.primary,
            onDismiss = { showAddEditDialog = false },
            onSave = { saved ->
                viewModel.saveStudent(saved) {
                    showAddEditDialog = false
                }
            }
        )
    }

    studentToDelete?.let { s ->
        SneatConfirmDialog(
            title = "Hapus Data Siswa",
            message = "Apakah Anda yakin ingin menghapus data siswa ${s.name} (NISN: ${s.nisn})?",
            onConfirm = {
                viewModel.deleteStudent(s.id)
                studentToDelete = null
            },
            onDismiss = { studentToDelete = null }
        )
    }

    if (showExportDialog) {
        SneatExportDialog(
            title = "Daftar Siswa",
            schoolName = tenant.name,
            onDismiss = { showExportDialog = false },
            onConfirmExport = {}
        )
    }
}
