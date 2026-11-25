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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SchoolHoliday
import com.example.model.TeachingSchedule
import com.example.model.UserRole
import com.example.ui.components.SneatBadge
import com.example.ui.components.SneatCard
import com.example.ui.components.SneatConfirmDialog
import com.example.ui.components.SneatRouteGuardRestricted
import com.example.ui.theme.*
import com.example.viewmodel.SchoolViewModel

@Composable
fun ScheduleCalendarScreen(
    viewModel: SchoolViewModel,
    initialTab: Int = 0
) {
    val currentRole by viewModel.currentRole.collectAsState()
    if (currentRole != UserRole.SUPERADMIN && currentRole != UserRole.GURU) {
        SneatRouteGuardRestricted(
            currentRole = currentRole,
            requiredRole = "Admin Sekolah / Guru Pengajar",
            onSwitchRoleClick = { viewModel.switchRole(UserRole.SUPERADMIN) }
        )
        return
    }

    val isAdmin = currentRole == UserRole.SUPERADMIN
    val tenant by viewModel.currentTenant.collectAsState()
    val profile by viewModel.currentSchoolProfile.collectAsState()
    val adminSchedules by viewModel.activeTeachingSchedules.collectAsState()
    val guruSchedules by viewModel.guruFilteredSchedules.collectAsState()
    val holidays by viewModel.activeHolidays.collectAsState()
    val classes by viewModel.activeClasses.collectAsState()
    val subjects by viewModel.activeSubjects.collectAsState()
    val teachers by viewModel.activeTeachers.collectAsState()
    val brandColors = LocalBrandColors.current

    val displayedSchedules = if (isAdmin) adminSchedules else guruSchedules
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 1)) }
    var selectedDay by remember { mutableStateOf("Semua") }

    var showScheduleDialog by remember { mutableStateOf(false) }
    var scheduleToEdit by remember { mutableStateOf<TeachingSchedule?>(null) }
    var scheduleToDelete by remember { mutableStateOf<TeachingSchedule?>(null) }

    var showHolidayDialog by remember { mutableStateOf(false) }
    var holidayToEdit by remember { mutableStateOf<SchoolHoliday?>(null) }
    var holidayToDelete by remember { mutableStateOf<SchoolHoliday?>(null) }

    val daysFilter = listOf("Semua", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu")
    val filteredSchedules = remember(displayedSchedules, selectedDay) {
        if (selectedDay == "Semua") displayedSchedules
        else displayedSchedules.filter { it.dayOfWeek.equals(selectedDay, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = brandColors.primary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                modifier = Modifier.testTag("schedule_tab_jadwal"),
                text = { Text("Jadwal Mengajar", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                modifier = Modifier.testTag("schedule_tab_libur"),
                text = { Text("Kalender & Hari Libur", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
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
                                text = if (selectedTab == 0) "Jadwal Mengajar Per Kelas-Mapel-Guru" else "Kalender Akademik & Hari Libur",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "TA ${profile.academicYear} (${profile.semester}) • ${displayedSchedules.size} Sesi • ${holidays.size} Hari Libur",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isAdmin) {
                            Button(
                                onClick = {
                                    if (selectedTab == 0) {
                                        scheduleToEdit = null
                                        showScheduleDialog = true
                                    } else {
                                        holidayToEdit = null
                                        showHolidayDialog = true
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = brandColors.primary),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag(if (selectedTab == 0) "add_schedule_button" else "add_holiday_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (selectedTab == 0) "Jadwal" else "Libur", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            if (selectedTab == 0) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(daysFilter) { day ->
                            FilterChip(
                                selected = selectedDay == day,
                                onClick = { selectedDay = day },
                                label = { Text(day, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                if (filteredSchedules.isEmpty()) {
                    item {
                        SneatCard {
                            Text(
                                text = "Belum ada jadwal mengajar untuk filter '$selectedDay'.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(filteredSchedules, key = { it.id }) { sch ->
                        SneatCard(modifier = Modifier.testTag("schedule_card_${sch.id}")) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        SneatBadge(sch.dayOfWeek, brandColors.primary, brandColors.containerLight)
                                        SneatBadge("${sch.startTime} - ${sch.endTime}", SneatSuccess, SneatSuccessLight)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("${sch.subjectName} • ${sch.className}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        text = "Guru: ${sch.teacherName}${if (sch.room.isNotBlank()) " • Ruang: ${sch.room}" else ""}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isAdmin) {
                                    Row {
                                        IconButton(
                                            onClick = {
                                                scheduleToEdit = sch
                                                showScheduleDialog = true
                                            },
                                            modifier = Modifier.minimumInteractiveComponentSize()
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit Jadwal", tint = brandColors.primary)
                                        }
                                        IconButton(
                                            onClick = { scheduleToDelete = sch },
                                            modifier = Modifier.minimumInteractiveComponentSize()
                                        ) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus Jadwal", tint = SneatDanger)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                if (holidays.isEmpty()) {
                    item {
                        SneatCard {
                            Text("Belum ada referensi hari libur akademik.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    items(holidays, key = { it.id }) { hol ->
                        SneatCard(modifier = Modifier.testTag("holiday_card_${hol.id}")) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        SneatBadge(hol.date, SneatDanger, SneatDangerLight)
                                        SneatBadge(hol.category, brandColors.primary, brandColors.containerLight)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(hol.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                if (isAdmin) {
                                    Row {
                                        IconButton(
                                            onClick = {
                                                holidayToEdit = hol
                                                showHolidayDialog = true
                                            },
                                            modifier = Modifier.minimumInteractiveComponentSize()
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit Hari Libur", tint = brandColors.primary)
                                        }
                                        IconButton(
                                            onClick = { holidayToDelete = hol },
                                            modifier = Modifier.minimumInteractiveComponentSize()
                                        ) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus Hari Libur", tint = SneatDanger)
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

    if (showScheduleDialog && isAdmin) {
        val initial = scheduleToEdit
        var day by remember(initial) { mutableStateOf(initial?.dayOfWeek ?: "Senin") }
        var startTime by remember(initial) { mutableStateOf(initial?.startTime ?: "07:00") }
        var endTime by remember(initial) { mutableStateOf(initial?.endTime ?: "08:30") }
        var className by remember(initial) { mutableStateOf(initial?.className ?: classes.firstOrNull()?.name ?: "XII MIPA 1") }
        var subjectName by remember(initial) { mutableStateOf(initial?.subjectName ?: subjects.firstOrNull()?.name ?: "Matematika Peminatan") }
        var selectedTeacher by remember(initial) {
            mutableStateOf(
                teachers.firstOrNull { it.id == initial?.teacherId } ?: teachers.firstOrNull()
            )
        }
        var room by remember(initial) { mutableStateOf(initial?.room ?: "R. Kelas") }

        AlertDialog(
            onDismissRequest = { showScheduleDialog = false },
            title = { Text(if (initial == null) "Tambah Jadwal Mengajar" else "Edit Jadwal Mengajar", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu")) { d ->
                            FilterChip(selected = day == d, onClick = { day = d }, label = { Text(d, fontSize = 10.sp) })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = startTime, onValueChange = { startTime = it }, label = { Text("Mulai") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = endTime, onValueChange = { endTime = it }, label = { Text("Selesai") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(classes) { c ->
                            FilterChip(selected = className == c.name, onClick = { className = c.name }, label = { Text(c.name, fontSize = 10.sp) })
                        }
                    }
                    OutlinedTextField(value = className, onValueChange = { className = it }, label = { Text("Kelas *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(subjects) { s ->
                            FilterChip(selected = subjectName == s.name, onClick = { subjectName = s.name }, label = { Text(s.name, fontSize = 10.sp) })
                        }
                    }
                    OutlinedTextField(value = subjectName, onValueChange = { subjectName = it }, label = { Text("Mata Pelajaran *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(teachers) { t ->
                            FilterChip(selected = selectedTeacher?.id == t.id, onClick = { selectedTeacher = t }, label = { Text(t.name.substringBefore(","), fontSize = 10.sp) })
                        }
                    }
                    OutlinedTextField(value = room, onValueChange = { room = it }, label = { Text("Ruang Kelas / Lab") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    val t = selectedTeacher
                    viewModel.saveTeachingSchedule(
                        TeachingSchedule(
                            id = initial?.id ?: "SCHD-${System.currentTimeMillis() % 100000}",
                            idSekolah = tenant.idSekolah,
                            dayOfWeek = day,
                            startTime = startTime.trim(),
                            endTime = endTime.trim(),
                            className = className.trim(),
                            subjectName = subjectName.trim(),
                            teacherId = t?.id ?: (initial?.teacherId ?: "T-01"),
                            teacherName = t?.name ?: (initial?.teacherName ?: "Guru Pengajar"),
                            room = room.trim()
                        )
                    ) { showScheduleDialog = false }
                }) { Text("Simpan Jadwal") }
            },
            dismissButton = { TextButton(onClick = { showScheduleDialog = false }) { Text("Batal") } }
        )
    }

    if (showHolidayDialog && isAdmin) {
        val initial = holidayToEdit
        var date by remember(initial) { mutableStateOf(initial?.date ?: "2026-12-25") }
        var name by remember(initial) { mutableStateOf(initial?.name ?: "") }
        var category by remember(initial) { mutableStateOf(initial?.category ?: "Libur Nasional") }

        AlertDialog(
            onDismissRequest = { showHolidayDialog = false },
            title = { Text(if (initial == null) "Tambah Hari Libur Akademik" else "Edit Hari Libur", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Tanggal (YYYY-MM-DD) *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama Hari Libur / Agenda *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("Libur Nasional", "Cuti Bersama", "Kalender Akademik", "Kegiatan Sekolah")) { cat ->
                            FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat, fontSize = 10.sp) })
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.saveHoliday(
                        SchoolHoliday(
                            id = initial?.id ?: "HOL-${System.currentTimeMillis() % 100000}",
                            idSekolah = tenant.idSekolah,
                            date = date.trim(),
                            name = name.trim(),
                            category = category
                        )
                    ) { showHolidayDialog = false }
                }) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { showHolidayDialog = false }) { Text("Batal") } }
        )
    }

    scheduleToDelete?.let { sch ->
        SneatConfirmDialog(
            title = "Hapus Jadwal Mengajar",
            message = "Hapus jadwal ${sch.subjectName} (${sch.className}, ${sch.dayOfWeek})?",
            onConfirm = {
                viewModel.deleteTeachingSchedule(sch.id)
                scheduleToDelete = null
            },
            onDismiss = { scheduleToDelete = null }
        )
    }

    holidayToDelete?.let { hol ->
        SneatConfirmDialog(
            title = "Hapus Hari Libur",
            message = "Hapus '${hol.name}' (${hol.date}) dari kalender akademik?",
            onConfirm = {
                viewModel.deleteHoliday(hol.id)
                holidayToDelete = null
            },
            onDismiss = { holidayToDelete = null }
        )
    }
}
