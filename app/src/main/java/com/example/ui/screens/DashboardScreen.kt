package com.example.ui.screens

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.SchoolViewModel

@Composable
fun DashboardScreen(
    viewModel: SchoolViewModel,
    onNavigateToMenu: (String) -> Unit
) {
    val tenant by viewModel.currentTenant.collectAsState()
    val schoolProfile by viewModel.currentSchoolProfile.collectAsState()
    val proSubscription by viewModel.proSubscription.collectAsState()
    val brandColors = LocalBrandColors.current

    val teachers by viewModel.activeTeachers.collectAsState()
    val students by viewModel.activeStudents.collectAsState()
    val classes by viewModel.activeClasses.collectAsState()
    val attendance by viewModel.activeAttendance.collectAsState()
    val grades by viewModel.activeGrades.collectAsState()
    val lessonPlans by viewModel.activeLessonPlans.collectAsState()
    val journals by viewModel.activeTeachingJournals.collectAsState()
    val announcements by viewModel.activeAnnouncements.collectAsState()
    val loginLogs by viewModel.activeLoginLogs.collectAsState()
    val ptkAttendance by viewModel.activePtkAttendance.collectAsState()
    val schedules by viewModel.activeTeachingSchedules.collectAsState()
    val holidays by viewModel.activeHolidays.collectAsState()
    val forumMessages by viewModel.forumMessages.collectAsState()

    val presentCount = attendance.count { it.status == AttendanceStatus.HADIR }
    val attendancePct = if (attendance.isNotEmpty()) (presentCount * 100) / attendance.size else 95
    val pendingRppCount = lessonPlans.count { it.status == RppStatus.SUBMITTED || it.status == RppStatus.DRAFT }
    val pnsP3kCount = teachers.count {
        it.employmentStatus.equals("PNS", true) ||
            it.employmentStatus.equals("P3K", true) ||
            it.employmentStatus.equals("PPPK", true)
    }
    val activeStudentsCount = students.count { it.status.equals("Aktif", true) }

    var selectedModuleCategory by remember { mutableStateOf("Semua") }
    val moduleCategories = listOf("Semua", "Identitas & Sistem", "PTK & Jadwal", "Pengguna & Master", "Akademik & Laporan")

    var showTenantDialog by remember { mutableStateOf(false) }
    var showAddEditTenantDialog by remember { mutableStateOf(false) }
    var tenantToEdit by remember { mutableStateOf<TenantSchool?>(null) }
    var tenantToDelete by remember { mutableStateOf<TenantSchool?>(null) }
    var showAdminDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showProDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header Branded: Dashboard Admin Sekolah + Logo & Warna Tema
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(brandColors.headerBrush)
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            SchoolLogoAvatar(
                                logoBase64 = schoolProfile.logoBase64,
                                schoolName = schoolProfile.name,
                                size = 50.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    SneatBadge(
                                        text = "Dashboard Admin Sekolah",
                                        textColor = brandColors.primary,
                                        backgroundColor = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    SneatBadge(
                                        text = tenant.level,
                                        textColor = Color.White,
                                        backgroundColor = Color.White.copy(alpha = 0.25f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = schoolProfile.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "NPSN: ${schoolProfile.npsn} • TA ${schoolProfile.academicYear} (${schoolProfile.semester})",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }

                        FilledTonalButton(
                            onClick = { showTenantDialog = true },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color.White.copy(alpha = 0.24f),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Lembaga", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.16f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "ID: ${tenant.idSekolah} • ${announcements.size} Pengumuman • ${loginLogs.size} Log Login",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Tampilan",
                                color = brandColors.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.White)
                                    .clickable { onNavigateToMenu("APPEARANCE") }
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                            Text(
                                text = "PIN Kelas",
                                color = Color(0xFF7A5200),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.White)
                                    .clickable { showAdminDialog = true }
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }

        // 2. Ringkasan Statistik Keseluruhan Sekolah
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Ringkasan Statistik Sekolah",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SneatStatCard(
                        title = "Siswa Aktif",
                        value = "$activeStudentsCount/${students.size}",
                        icon = Icons.Default.People,
                        iconColor = brandColors.primary,
                        iconBgColor = brandColors.containerLight,
                        subtext = "${classes.size} Rombel & PIN Kelas",
                        modifier = Modifier.weight(1f)
                    )
                    SneatStatCard(
                        title = "Guru & Pendidik",
                        value = "${teachers.size}",
                        icon = Icons.Default.Badge,
                        iconColor = SneatSuccess,
                        iconBgColor = SneatSuccessLight,
                        subtext = "$pnsP3kCount PNS/P3K Tunjangan",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SneatStatCard(
                        title = "Kehadiran Hari Ini",
                        value = "$attendancePct%",
                        icon = Icons.AutoMirrored.Filled.FactCheck,
                        iconColor = SneatInfo,
                        iconBgColor = SneatInfoLight,
                        subtext = "$presentCount Hadir Realtime",
                        modifier = Modifier.weight(1f)
                    )
                    SneatStatCard(
                        title = "Semua RPP",
                        value = "${lessonPlans.size}",
                        icon = Icons.Default.AutoStories,
                        iconColor = SneatWarning,
                        iconBgColor = SneatWarningLight,
                        subtext = "$pendingRppCount Menunggu Review",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 3. Menu Dashboard Admin (10 Modul Lengkap Tahap 1)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Menu Manajemen Admin Sekolah",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Akses lengkap konfigurasi lembaga, master data, pengguna & laporan",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    TextButton(onClick = { showExportDialog = true }) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export", fontSize = 12.sp)
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(moduleCategories) { cat ->
                        val isSelected = cat == selectedModuleCategory
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedModuleCategory = cat },
                            label = { Text(cat, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = brandColors.primary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                val allModules = listOf(
                    AdminModuleItem(
                        id = "APPEARANCE",
                        title = "Tampilan & Logo",
                        subtitle = "Logo <=5MB & Tema",
                        category = "Identitas & Sistem",
                        icon = Icons.Default.Palette,
                        color = brandColors.primary,
                        bgColor = brandColors.containerLight
                    ),
                    AdminModuleItem(
                        id = "PTK_MANAGEMENT",
                        title = "Monitoring PTK",
                        subtitle = "${teachers.size} PTK • ${ptkAttendance.size} Presensi",
                        category = "PTK & Jadwal",
                        icon = Icons.Default.SupervisorAccount,
                        color = brandColors.primary,
                        bgColor = brandColors.containerLight
                    ),
                    AdminModuleItem(
                        id = "PTK_GURU_KEPENDIDIKAN",
                        title = "Mutasi & Satmikal",
                        subtitle = "${teachers.count { it.isNonSatmikal }} Non-Satmikal",
                        category = "PTK & Jadwal",
                        icon = Icons.Default.WorkHistory,
                        color = SneatWarning,
                        bgColor = SneatWarningLight
                    ),
                    AdminModuleItem(
                        id = "HOLIDAYS_REF",
                        title = "Hari Libur Akademik",
                        subtitle = "${holidays.size} Agenda Libur",
                        category = "PTK & Jadwal",
                        icon = Icons.Default.EventAvailable,
                        color = SneatDanger,
                        bgColor = SneatDangerLight
                    ),
                    AdminModuleItem(
                        id = "SCHEDULE_CALENDAR",
                        title = "Jadwal Mengajar",
                        subtitle = "${schedules.size} Sesi Terjadwal",
                        category = "PTK & Jadwal",
                        icon = Icons.Default.CalendarMonth,
                        color = SneatSuccess,
                        bgColor = SneatSuccessLight
                    ),
                    AdminModuleItem(
                        id = "FORUM_DISKUSI",
                        title = "Forum Diskusi",
                        subtitle = "${forumMessages.size} Chat + VN 30s",
                        category = "Identitas & Sistem",
                        icon = Icons.Default.Forum,
                        color = SneatInfo,
                        bgColor = SneatInfoLight
                    ),
                    AdminModuleItem(
                        id = "PROFIL",
                        title = "Profil Lembaga",
                        subtitle = "NPSN & Struktur Org.",
                        category = "Identitas & Sistem",
                        icon = Icons.Default.AccountBalance,
                        color = SneatSuccess,
                        bgColor = SneatSuccessLight
                    ),
                    AdminModuleItem(
                        id = "MASTER_DATA",
                        title = "Master Data",
                        subtitle = "${classes.size} Kls • Libur • TA",
                        category = "Pengguna & Master",
                        icon = Icons.Default.Dataset,
                        color = SneatInfo,
                        bgColor = SneatInfoLight
                    ),
                    AdminModuleItem(
                        id = "USERS",
                        title = "Kelola Pengguna",
                        subtitle = "Input Guru & Siswa",
                        category = "Pengguna & Master",
                        icon = Icons.Default.ManageAccounts,
                        color = brandColors.secondary,
                        bgColor = brandColors.containerLight
                    ),
                    AdminModuleItem(
                        id = "RPP",
                        title = "Kelola Semua RPP",
                        subtitle = "$pendingRppCount Perlu Review",
                        category = "Akademik & Laporan",
                        icon = Icons.Default.AutoStories,
                        color = SneatWarning,
                        bgColor = SneatWarningLight
                    ),
                    AdminModuleItem(
                        id = "STORAGE_PRO",
                        title = "Storage / Iklan / Pro",
                        subtitle = if (proSubscription.isPro) "50 GB PRO" else "500 MB Free",
                        category = "Identitas & Sistem",
                        icon = Icons.Default.CloudQueue,
                        color = SneatInfo,
                        bgColor = SneatInfoLight
                    ),
                    AdminModuleItem(
                        id = "ANNOUNCEMENTS",
                        title = "Pengumuman Sekolah",
                        subtitle = "${announcements.size} Terbit",
                        category = "Identitas & Sistem",
                        icon = Icons.Default.Campaign,
                        color = SneatWarning,
                        bgColor = SneatWarningLight
                    ),
                    AdminModuleItem(
                        id = "EXPORT",
                        title = "Export Laporan",
                        subtitle = "Tunjangan, Rapor, PDF",
                        category = "Akademik & Laporan",
                        icon = Icons.Default.PictureAsPdf,
                        color = SneatDanger,
                        bgColor = SneatDangerLight
                    ),
                    AdminModuleItem(
                        id = "LOGIN_LOGS",
                        title = "Log Aktivitas Login",
                        subtitle = "${loginLogs.size} Audit Record",
                        category = "Identitas & Sistem",
                        icon = Icons.Default.History,
                        color = SneatPrimary,
                        bgColor = SneatPrimaryLight
                    ),
                    AdminModuleItem(
                        id = "GURU",
                        title = "Data Guru",
                        subtitle = "$pnsP3kCount PNS/P3K",
                        category = "Pengguna & Master",
                        icon = Icons.Default.Badge,
                        color = SneatSuccess,
                        bgColor = SneatSuccessLight
                    ),
                    AdminModuleItem(
                        id = "SISWA",
                        title = "Data Siswa",
                        subtitle = "$activeStudentsCount Aktif",
                        category = "Pengguna & Master",
                        icon = Icons.Default.Groups,
                        color = brandColors.primary,
                        bgColor = brandColors.containerLight
                    ),
                    AdminModuleItem(
                        id = "ABSENSI",
                        title = "Presensi & Nilai",
                        subtitle = "${grades.size} Nilai • ${journals.size} Jnl",
                        category = "Akademik & Laporan",
                        icon = Icons.AutoMirrored.Filled.Grading,
                        color = SneatSuccess,
                        bgColor = SneatSuccessLight
                    )
                )

                val displayed = if (selectedModuleCategory == "Semua") allModules
                else allModules.filter { it.category == selectedModuleCategory }

                displayed.chunked(3).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowItems.forEach { mod ->
                            AdminModuleCard(
                                title = mod.title,
                                subtitle = mod.subtitle,
                                icon = mod.icon,
                                color = mod.color,
                                bgColor = mod.bgColor,
                                testTag = "admin_card_${mod.id}",
                                modifier = Modifier.weight(1f)
                            ) {
                                onNavigateToMenu(mod.id)
                            }
                        }
                        if (rowItems.size < 3) {
                            repeat(3 - rowItems.size) { Spacer(modifier = Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }

        // 4. Pengumuman Terbaru
        if (announcements.isNotEmpty()) {
            item {
                SneatCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Pengumuman Sekolah Terbaru", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        TextButton(onClick = { onNavigateToMenu("ANNOUNCEMENTS") }) {
                            Text("Kelola", fontSize = 12.sp)
                        }
                    }
                    announcements.take(2).forEach { a ->
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(a.title, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                SneatBadge(text = a.targetRole, textColor = brandColors.primary, backgroundColor = brandColors.containerLight)
                            }
                            Text(a.content, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    }
                }
            }
        }

        // 5. Ad Banner
        item {
            SneatAdBanner(
                proSubscription = proSubscription,
                onUpgradeProClick = { showProDialog = true }
            )
        }

        // 6. Panduan Firebase & Capacitor APK
        item {
            SneatCard(borderColor = brandColors.primary.copy(alpha = 0.3f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(brandColors.containerLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.IntegrationInstructions, contentDescription = null, tint = brandColors.primary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Arsitektur & Konfigurasi", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                            Text("Firebase Rules • Catatan Capacitor APK • Play Policy", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    FilledTonalButton(
                        onClick = { onNavigateToMenu("DOCS") },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Buka Panduan", fontSize = 12.sp)
                    }
                }
            }
        }
    }

    // Modal Kelola Lembaga Multi-Tenant
    if (showTenantDialog) {
        val allTenants by viewModel.tenants.collectAsState()
        AlertDialog(
            onDismissRequest = { showTenantDialog = false },
            title = { Text("Pilih & Kelola Lembaga", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            tenantToEdit = null
                            showAddEditTenantDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = brandColors.primary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AddBusiness, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tambah Lembaga Baru", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    allTenants.forEach { t ->
                        val isSelected = t.idSekolah == tenant.idSekolah
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) brandColors.containerLight else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = isSelected, onClick = {
                                    viewModel.switchTenant(t.idSekolah)
                                    showTenantDialog = false
                                })
                                Column(
                                    modifier = Modifier.weight(1f).clickable {
                                        viewModel.switchTenant(t.idSekolah)
                                        showTenantDialog = false
                                    }
                                ) {
                                    Text(t.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("${t.idSekolah} • ${t.level} • NPSN: ${t.npsn}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = {
                                    tenantToEdit = t
                                    showAddEditTenantDialog = true
                                }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Lembaga", tint = brandColors.primary, modifier = Modifier.size(16.dp))
                                }
                                if (allTenants.size > 1) {
                                    IconButton(onClick = { tenantToDelete = t }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus Lembaga", tint = SneatDanger, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showTenantDialog = false }) { Text("Tutup") } }
        )
    }

    if (showAddEditTenantDialog) {
        val allTenants by viewModel.tenants.collectAsState()
        AddEditTenantDialog(
            tenant = tenantToEdit,
            allTenants = allTenants,
            primaryColor = brandColors.primary,
            onDismiss = { showAddEditTenantDialog = false },
            onSave = { updatedTenant, akreditasi, kepalaSekolah, address ->
                viewModel.saveTenant(updatedTenant, akreditasi, kepalaSekolah, address) {
                    showAddEditTenantDialog = false
                    showTenantDialog = false
                }
            }
        )
    }

    tenantToDelete?.let { t ->
        SneatConfirmDialog(
            title = "Hapus Lembaga / Sekolah",
            message = "Apakah Anda yakin ingin menghapus '${t.name}' (${t.idSekolah})?",
            onConfirm = {
                viewModel.deleteTenant(t.idSekolah)
                tenantToDelete = null
            },
            onDismiss = { tenantToDelete = null }
        )
    }

    if (showExportDialog) {
        SneatExportDialog(
            title = "Data Manajemen Terpadu",
            schoolName = schoolProfile.name,
            onDismiss = { showExportDialog = false },
            onConfirmExport = {}
        )
    }

    if (showProDialog) {
        SneatProUpgradeDialog(
            proSubscription = proSubscription,
            onDismiss = { showProDialog = false },
            onTogglePro = { viewModel.togglePro() }
        )
    }

    if (showAdminDialog) {
        AdminUserManagementDialog(
            viewModel = viewModel,
            onDismiss = { showAdminDialog = false }
        )
    }
}

@Composable
private fun AddEditTenantDialog(
    tenant: TenantSchool?,
    allTenants: List<TenantSchool>,
    primaryColor: Color,
    onDismiss: () -> Unit,
    onSave: (TenantSchool, String, String, String) -> Unit
) {
    val isEdit = tenant != null
    val defaultId = remember(allTenants) { tenant?.idSekolah ?: "SCH-00${allTenants.size + 1}" }
    var idSekolah by remember { mutableStateOf(tenant?.idSekolah ?: defaultId) }
    var name by remember { mutableStateOf(tenant?.name ?: "") }
    var npsn by remember { mutableStateOf(tenant?.npsn ?: "") }
    var level by remember { mutableStateOf(tenant?.level ?: "SMA") }
    var city by remember { mutableStateOf(tenant?.city ?: "Jakarta Selatan") }
    var akreditasi by remember { mutableStateOf("A (Unggul)") }
    var kepalaSekolah by remember { mutableStateOf("Kepala Sekolah") }
    var address by remember { mutableStateOf("Jl. Pendidikan No. 1") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "Edit Lembaga / Sekolah" else "Tambah Lembaga Baru", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama Lembaga / Sekolah *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = idSekolah, onValueChange = { idSekolah = it }, label = { Text("ID Lembaga") }, singleLine = true, enabled = !isEdit, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = npsn, onValueChange = { npsn = it }, label = { Text("NPSN") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("SMA", "SMK", "SMP", "SD", "Ponpes")) { lvl ->
                        FilterChip(selected = level == lvl, onClick = { level = lvl }, label = { Text(lvl, fontSize = 11.sp) })
                    }
                }
                OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("Kota / Kabupaten") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = akreditasi, onValueChange = { akreditasi = it }, label = { Text("Status Akreditasi") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = kepalaSekolah, onValueChange = { kepalaSekolah = it }, label = { Text("Kepala Lembaga") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Alamat") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        TenantSchool(
                            idSekolah = idSekolah.trim(),
                            name = name.trim(),
                            npsn = npsn.trim().ifBlank { "20100000" },
                            level = level,
                            city = city.trim().ifBlank { "Indonesia" }
                        ),
                        akreditasi,
                        kepalaSekolah,
                        address
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                shape = RoundedCornerShape(8.dp)
            ) { Text(if (isEdit) "Simpan Perubahan" else "Tambah Lembaga") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

private data class AdminModuleItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: String,
    val icon: ImageVector,
    val color: Color,
    val bgColor: Color
)

@Composable
private fun AdminModuleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    bgColor: Color,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .testTag(testTag)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = color, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, fontWeight = FontWeight.Medium)
        }
    }
}
