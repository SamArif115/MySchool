package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.model.SchoolHoliday
import com.example.model.UserRole
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.SchoolViewModel

@Composable
fun MasterDataScreen(
    viewModel: SchoolViewModel
) {
    val profile by viewModel.currentSchoolProfile.collectAsState()
    val holidays by viewModel.activeHolidays.collectAsState()
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
    val tabs = listOf("Tahun & Semester", "Hari Libur", "Kelas & PIN", "Mata Pelajaran")

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 12.dp,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = brandColors.primary
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (selectedTab) {
                0 -> AcademicYearTab(
                    initialYear = profile.academicYear,
                    initialSemester = profile.semester,
                    primaryColor = brandColors.primary,
                    onSave = { year, sem ->
                        viewModel.updateMasterAcademicConfig(year, sem) {}
                    }
                )
                1 -> HolidaysTab(
                    tenantId = profile.idSekolah,
                    holidays = holidays,
                    primaryColor = brandColors.primary,
                    onSave = { h, done -> viewModel.saveHoliday(h, done) },
                    onDelete = { viewModel.deleteHoliday(it) }
                )
                2 -> KelasScreen(viewModel = viewModel)
                3 -> MapelScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun AcademicYearTab(
    initialYear: String,
    initialSemester: String,
    primaryColor: Color,
    onSave: (String, String) -> Unit
) {
    var academicYear by remember(initialYear) { mutableStateOf(initialYear) }
    var semester by remember(initialSemester) { mutableStateOf(initialSemester) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SneatCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = primaryColor)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Konfigurasi Tahun Ajaran & Semester Aktif", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Berlaku untuk seluruh perhitungan RPP, Jadwal, Absensi, Rapor, dan Kenaikan Kelas.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = academicYear,
                onValueChange = { academicYear = it },
                label = { Text("Tahun Ajaran (cth: 2026/2027)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text("Pilih Semester Aktif", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("Ganjil", "Genap").forEach { sem ->
                    FilterChip(
                        selected = semester == sem,
                        onClick = { semester = sem },
                        label = { Text("Semester $sem", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = primaryColor,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = { onSave(academicYear, semester) },
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().testTag("save_academic_year_button")
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Simpan Tahun Ajaran & Semester")
            }
        }
    }
}

@Composable
private fun HolidaysTab(
    tenantId: String,
    holidays: List<SchoolHoliday>,
    primaryColor: Color,
    onSave: (SchoolHoliday, () -> Unit) -> Unit,
    onDelete: (String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<SchoolHoliday?>(null) }
    var deleting by remember { mutableStateOf<SchoolHoliday?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editing = null
                    showDialog = true
                },
                containerColor = primaryColor,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Hari Libur")
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
            Text(
                text = "Kalender Hari Libur Sekolah (${holidays.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(holidays, key = { it.id }) { h ->
                    SneatCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(h.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Tanggal: ${h.date}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                                SneatBadge(
                                    text = h.category,
                                    textColor = SneatWarning,
                                    backgroundColor = SneatWarningLight
                                )
                            }
                            Row {
                                IconButton(onClick = {
                                    editing = h
                                    showDialog = true
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = primaryColor)
                                }
                                IconButton(onClick = { deleting = h }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus", tint = SneatDanger)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        var date by remember { mutableStateOf(editing?.date ?: "2026-12-25") }
        var name by remember { mutableStateOf(editing?.name ?: "") }
        var category by remember { mutableStateOf(editing?.category ?: "Libur Nasional") }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (editing == null) "Tambah Hari Libur" else "Edit Hari Libur", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Keterangan Hari Libur *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Tanggal (YYYY-MM-DD) *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Kategori Libur", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Libur Nasional", "Cuti Bersama", "Kalender Akademik").forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat, fontSize = 10.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = editing?.id ?: "HOL-${System.currentTimeMillis() % 10000}"
                        onSave(SchoolHoliday(id = id, idSekolah = tenantId, date = date, name = name, category = category)) {
                            showDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Batal") } }
        )
    }

    deleting?.let { h ->
        SneatConfirmDialog(
            title = "Hapus Hari Libur",
            message = "Hapus '${h.name}' (${h.date}) dari kalender sekolah?",
            onConfirm = {
                onDelete(h.id)
                deleting = null
            },
            onDismiss = { deleting = null }
        )
    }
}
