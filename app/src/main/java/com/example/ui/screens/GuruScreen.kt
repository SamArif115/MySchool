package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Teacher
import com.example.model.UserRole
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.SchoolViewModel

@Composable
fun GuruScreen(
    viewModel: SchoolViewModel
) {
    val teachers by viewModel.activeTeachers.collectAsState()
    val classes by viewModel.activeClasses.collectAsState()
    val tenant by viewModel.currentTenant.collectAsState()
    val currentRole by viewModel.currentRole.collectAsState()
    val brandColors = LocalBrandColors.current
    val canManage = currentRole == UserRole.SUPERADMIN

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Semua") }
    val filterOptions = listOf("Semua", "PNS", "P3K", "Honorer", "Kontrak")

    var showAddEditDialog by remember { mutableStateOf(false) }
    var teacherToEdit by remember { mutableStateOf<Teacher?>(null) }
    var teacherToDelete by remember { mutableStateOf<Teacher?>(null) }
    var showAllowanceExport by remember { mutableStateOf(false) }

    val filteredTeachers = teachers.filter { t ->
        val matchesQuery = t.name.contains(searchQuery, ignoreCase = true) ||
            t.nip.contains(searchQuery) ||
            t.subject.contains(searchQuery, ignoreCase = true)
        val matchesFilter = when (selectedFilter) {
            "Semua" -> true
            "P3K" -> t.employmentStatus.equals("P3K", true) || t.employmentStatus.equals("PPPK", true)
            else -> t.employmentStatus.equals(selectedFilter, ignoreCase = true)
        }
        matchesQuery && matchesFilter
    }

    Scaffold(
        floatingActionButton = {
            if (canManage) {
                FloatingActionButton(
                    onClick = {
                        teacherToEdit = null
                        showAddEditDialog = true
                    },
                    containerColor = brandColors.primary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah Guru")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            SneatSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = "Cari nama, NIP, atau mata pelajaran..."
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(filterOptions) { opt ->
                    val isSelected = opt == selectedFilter
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = opt },
                        label = { Text(opt, fontSize = 12.sp) },
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
                    text = "Tenaga Pendidik: ${filteredTeachers.size} (${selectedFilter})",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                if (canManage) {
                    TextButton(onClick = { showAllowanceExport = true }) {
                        Icon(Icons.Default.Summarize, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Laporan Tunjangan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (filteredTeachers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Tidak ada guru ditemukan", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredTeachers, key = { it.id }) { teacher ->
                        SneatCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                    val bmp = rememberBase64ImageBitmap(teacher.photoBase64)
                                    if (bmp != null) {
                                        Image(
                                            bitmap = bmp,
                                            contentDescription = "Foto ${teacher.name}",
                                            modifier = Modifier.size(44.dp).clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier.size(44.dp).clip(CircleShape).background(brandColors.containerLight),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = teacher.name.split(" ").take(2).mapNotNull { it.firstOrNull() }.joinToString(""),
                                                fontWeight = FontWeight.Bold,
                                                color = brandColors.primary,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = teacher.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${teacher.jabatan} • NIP: ${teacher.nip}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    SneatBadge(
                                        text = if (teacher.employmentStatus == "PNS" && teacher.rankGroup.isNotBlank()) "PNS (${teacher.rankGroup})" else teacher.employmentStatus,
                                        textColor = when (teacher.employmentStatus) {
                                            "PNS" -> SneatSuccess
                                            "P3K", "PPPK" -> SneatInfo
                                            else -> SneatWarning
                                        },
                                        backgroundColor = when (teacher.employmentStatus) {
                                            "PNS" -> SneatSuccessLight
                                            "P3K", "PPPK" -> SneatInfoLight
                                            else -> SneatWarningLight
                                        }
                                    )
                                    SneatBadge(
                                        text = if (teacher.isActiveAccount) "Aktif" else "Non-Aktif",
                                        textColor = if (teacher.isActiveAccount) SneatSuccess else SneatDanger,
                                        backgroundColor = if (teacher.isActiveAccount) SneatSuccessLight else SneatDangerLight
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
                                    Text("${teacher.subject} • ${teacher.weeklyHours} JP/Minggu", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text("SK: ${teacher.skNumber.ifBlank { "-" }} • TMT: ${teacher.tmtDate.ifBlank { "-" }}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Kelas: ${teacher.classesTaught.joinToString(", ").ifBlank { "-" }} • ${teacher.email}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (canManage) {
                                    Row {
                                        IconButton(onClick = {
                                            teacherToEdit = teacher
                                            showAddEditDialog = true
                                        }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = brandColors.primary)
                                        }
                                        IconButton(onClick = { teacherToDelete = teacher }) {
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
        FullTeacherFormDialog(
            tenantId = tenant.idSekolah,
            availableClasses = classes.map { it.name },
            teacher = teacherToEdit,
            primaryColor = brandColors.primary,
            onDismiss = { showAddEditDialog = false },
            onSave = { saved ->
                viewModel.saveTeacher(saved) {
                    showAddEditDialog = false
                }
            }
        )
    }

    teacherToDelete?.let { teacher ->
        SneatConfirmDialog(
            title = "Hapus Data Guru",
            message = "Apakah Anda yakin ingin menghapus data ${teacher.name} (NIP: ${teacher.nip})?",
            onConfirm = {
                viewModel.deleteTeacher(teacher.id)
                teacherToDelete = null
            },
            onDismiss = { teacherToDelete = null }
        )
    }

    if (showAllowanceExport) {
        SneatExportDialog(
            title = "Laporan Tunjangan Guru (PNS / P3K)",
            schoolName = tenant.name,
            onDismiss = { showAllowanceExport = false },
            onConfirmExport = {}
        )
    }
}
