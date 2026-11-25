package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.Grading
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CachedSchoolBranding
import com.example.model.UserRole
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.viewmodel.SchoolViewModel
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

enum class Screen(val title: String, val icon: ImageVector) {
    SPLASH("Splash", Icons.Default.FlashOn),
    DASHBOARD("Dashboard", Icons.Default.Dashboard),
    APPEARANCE("Tampilan & Logo", Icons.Default.Palette),
    PTK_MANAGEMENT("Monitoring & Manajemen PTK", Icons.Default.SupervisorAccount),
    PTK_GURU_KEPENDIDIKAN("Guru & Kependidikan", Icons.Default.WorkHistory),
    HOLIDAYS_REF("Referensi Hari Libur", Icons.Default.EventAvailable),
    SCHEDULE_CALENDAR("Kalender & Jadwal Mengajar", Icons.Default.CalendarMonth),
    PROFIL("Profil Lembaga", Icons.Default.AccountBalance),
    MASTER_DATA("Master Data", Icons.Default.Dataset),
    USERS("Kelola Pengguna", Icons.Default.ManageAccounts),
    GURU("Data Guru", Icons.Default.Badge),
    SISWA("Data Siswa", Icons.Default.People),
    KELAS("Rombel & PIN", Icons.Default.MeetingRoom),
    MAPEL("Mata Pelajaran", Icons.AutoMirrored.Filled.MenuBook),
    ABSENSI("Presensi Kelas", Icons.AutoMirrored.Filled.FactCheck),
    NILAI("Nilai Siswa", Icons.AutoMirrored.Filled.Grading),
    RPP("Kelola RPP", Icons.Default.AutoStories),
    JURNAL("Dokumentasi Mengajar", Icons.Default.EditCalendar),
    STORAGE_PRO("Storage / Iklan / Pro", Icons.Default.CloudQueue),
    ANNOUNCEMENTS("Pengumuman Sekolah", Icons.Default.Campaign),
    EXPORT("Export Laporan", Icons.Default.PictureAsPdf),
    LOGIN_LOGS("Log Aktivitas Login", Icons.Default.History),
    FORUM_DISKUSI("Forum Diskusi Lembaga", Icons.Default.Forum),
    DOCS("Arsitektur & Panduan", Icons.Default.IntegrationInstructions),
    MORE("Menu Lainnya", Icons.Default.Apps)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(
    viewModel: SchoolViewModel,
    firebaseUser: FirebaseUser? = null,
    isSigningIn: Boolean = false,
    authError: String? = null,
    cachedBranding: CachedSchoolBranding = CachedSchoolBranding(),
    onGoogleSignInClick: () -> Unit = {},
    onSignOutClick: () -> Unit = {},
    onForceSyncClick: () -> Unit = {}
) {
    var showSplash by remember { mutableStateOf(true) }
    val isOnboardingRequired by viewModel.isOnboardingRequired.collectAsState()
    val authenticatedSession by viewModel.authenticatedSession.collectAsState()
    val isFirebaseSynced by viewModel.isFirebaseSynced.collectAsState()
    val schoolProfile by viewModel.currentSchoolProfile.collectAsState()

    if (showSplash) {
        val splashName = if (authenticatedSession != null) schoolProfile.name else cachedBranding.schoolName
        val splashLogo = if (authenticatedSession != null) schoolProfile.logoBase64 else cachedBranding.logoBase64
        val splashTheme = if (authenticatedSession != null) schoolProfile.themePreset else cachedBranding.themePreset
        SplashScreen(
            schoolName = splashName,
            logoBase64 = splashLogo,
            themePreset = splashTheme,
            onNavigateToApp = { showSplash = false }
        )
        return
    }

    if (firebaseUser == null || isOnboardingRequired || authenticatedSession == null) {
        SignInScreen(
            viewModel = viewModel,
            isSignedInToFirebase = firebaseUser != null,
            firebaseEmail = firebaseUser?.email ?: "",
            firebaseDisplayName = firebaseUser?.displayName ?: "",
            isSigningIn = isSigningIn,
            authError = authError,
            cachedBranding = cachedBranding,
            onGoogleSignInClick = onGoogleSignInClick,
            onSignOutClick = onSignOutClick,
            onBackToSplashClick = { showSplash = true }
        )
        return
    }

    // Tahap 1 Role Guard: Hanya Dashboard Admin (SUPERADMIN) & Dashboard Guru (GURU)
    val session = authenticatedSession!!
    if (session.role == UserRole.KEPALA_SEKOLAH || session.role == UserRole.SISWA) {
        StageOneRoleReservedScreen(
            role = session.role,
            displayName = session.displayName,
            schoolName = schoolProfile.name,
            logoBase64 = schoolProfile.logoBase64,
            onSignOutClick = onSignOutClick
        )
        return
    }

    key(firebaseUser.uid, session.idSekolah, session.role) {
        AuthenticatedMainScaffold(
            viewModel = viewModel,
            isFirebaseSynced = isFirebaseSynced,
            onSignOutClick = onSignOutClick,
            onForceSyncClick = onForceSyncClick
        )
    }
}

@Composable
private fun StageOneRoleReservedScreen(
    role: UserRole,
    displayName: String,
    schoolName: String,
    logoBase64: String,
    onSignOutClick: () -> Unit
) {
    val brandColors = LocalBrandColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        SneatCard {
            Column(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SchoolLogoAvatar(
                    logoBase64 = logoBase64,
                    schoolName = schoolName,
                    size = 64.dp
                )
                SneatBadge(
                    text = "TAHAP 1: FOKUS ADMIN & GURU",
                    textColor = brandColors.primary,
                    backgroundColor = brandColors.containerLight
                )
                Text(
                    text = "Halo, $displayName (${role.label})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Sesuai ruang lingkup Tahap 1 aplikasi $schoolName, dashboard khusus ${role.label} disiapkan untuk Tahap 2. Silakan masuk dengan akun Admin atau Guru.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = onSignOutClick,
                    colors = ButtonDefaults.buttonColors(containerColor = SneatDanger),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("stage1_signout_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Keluar & Ganti Akun", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedMainScaffold(
    viewModel: SchoolViewModel,
    isFirebaseSynced: Boolean,
    onSignOutClick: () -> Unit,
    onForceSyncClick: () -> Unit
) {
    val backStack = remember { mutableStateListOf(Screen.DASHBOARD) }
    val currentScreen = backStack.lastOrNull() ?: Screen.DASHBOARD
    val previousScreen = if (backStack.size > 1) backStack[backStack.size - 2] else null

    fun navigateTo(target: Screen) {
        if (target == Screen.DASHBOARD) {
            backStack.clear()
            backStack.add(Screen.DASHBOARD)
        } else if (backStack.lastOrNull() != target) {
            backStack.add(target)
        }
    }

    fun navigateBack() {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    val currentRole by viewModel.currentRole.collectAsState()
    val tenant by viewModel.currentTenant.collectAsState()
    val schoolProfile by viewModel.currentSchoolProfile.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val proSubscription by viewModel.proSubscription.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val brandColors = LocalBrandColors.current

    var showProDialog by remember { mutableStateOf(false) }
    var showMoreSheet by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var showAdminDialog by remember { mutableStateOf(false) }
    val unreadNotifications by viewModel.unreadNotificationCount.collectAsState()

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val isExpandedScreen = configuration.screenWidthDp >= 600
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(toastMessage) {
        toastMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    LaunchedEffect(currentRole) {
        backStack.clear()
        backStack.add(Screen.DASHBOARD)
    }

    BackHandler(enabled = drawerState.isOpen || backStack.size > 1) {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else {
            navigateBack()
        }
    }

    // Strict Route Guard Tahap 1: Admin vs Guru
    val allowedScreens = remember(currentRole) {
        when (currentRole) {
            UserRole.SUPERADMIN -> listOf(
                Screen.DASHBOARD,
                Screen.APPEARANCE,
                Screen.PTK_MANAGEMENT,
                Screen.PTK_GURU_KEPENDIDIKAN,
                Screen.HOLIDAYS_REF,
                Screen.SCHEDULE_CALENDAR,
                Screen.PROFIL,
                Screen.MASTER_DATA,
                Screen.USERS,
                Screen.RPP,
                Screen.STORAGE_PRO,
                Screen.ANNOUNCEMENTS,
                Screen.EXPORT,
                Screen.LOGIN_LOGS,
                Screen.FORUM_DISKUSI,
                Screen.GURU,
                Screen.SISWA,
                Screen.KELAS,
                Screen.MAPEL,
                Screen.ABSENSI,
                Screen.NILAI,
                Screen.JURNAL,
                Screen.DOCS
            )
            UserRole.GURU -> listOf(
                Screen.DASHBOARD,
                Screen.SCHEDULE_CALENDAR,
                Screen.RPP,
                Screen.ABSENSI,
                Screen.NILAI,
                Screen.JURNAL,
                Screen.ANNOUNCEMENTS,
                Screen.FORUM_DISKUSI
            )
            else -> listOf(Screen.DASHBOARD)
        }
    }

    // Sidebar Menu (15 Admin items + Panduan | 8 Guru items)
    val drawerScreens = remember(currentRole) {
        if (currentRole == UserRole.SUPERADMIN) {
            listOf(
                Screen.DASHBOARD to "1. Dashboard Admin",
                Screen.APPEARANCE to "2. Pengaturan Tampilan & Logo",
                Screen.PTK_MANAGEMENT to "3. Monitoring & Manajemen PTK",
                Screen.PTK_GURU_KEPENDIDIKAN to "4. Guru & Kependidikan (Mutasi/Satmikal)",
                Screen.HOLIDAYS_REF to "5. Referensi Hari Libur Akademik",
                Screen.SCHEDULE_CALENDAR to "6. Kelola Kalender & Jadwal Mengajar",
                Screen.PROFIL to "7. Kelola Lembaga / Profil Sekolah",
                Screen.MASTER_DATA to "8. Master Data (TA, Kelas, Mapel)",
                Screen.USERS to "9. Kelola Pengguna (Guru & Siswa)",
                Screen.RPP to "10. Kelola Semua RPP",
                Screen.STORAGE_PRO to "11. Storage / Iklan / Pro",
                Screen.ANNOUNCEMENTS to "12. Pengumuman Sekolah",
                Screen.EXPORT to "13. Export Laporan",
                Screen.LOGIN_LOGS to "14. Log Aktivitas Login",
                Screen.FORUM_DISKUSI to "15. Forum Diskusi Lembaga",
                Screen.DOCS to "Arsitektur & Capacitor APK"
            )
        } else {
            listOf(
                Screen.DASHBOARD to "Dashboard Guru (Beranda & Profil)",
                Screen.SCHEDULE_CALENDAR to "Jadwal Mengajar & Kalender",
                Screen.RPP to "Kelola RPP Saya",
                Screen.ABSENSI to "Absensi Kelas yang Diajar",
                Screen.NILAI to "Nilai Siswa",
                Screen.JURNAL to "Dokumentasi Kegiatan Mengajar",
                Screen.ANNOUNCEMENTS to "Pengumuman Sekolah",
                Screen.FORUM_DISKUSI to "Forum Diskusi Lembaga"
            )
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.widthIn(max = 300.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(brandColors.headerBrush)
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SchoolLogoAvatar(
                            logoBase64 = schoolProfile.logoBase64,
                            schoolName = schoolProfile.name,
                            size = 44.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = schoolProfile.name,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1
                            )
                            Text(
                                text = currentRole.label,
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    drawerScreens.forEach { (sc, label) ->
                        NavigationDrawerItem(
                            selected = currentScreen == sc,
                            onClick = {
                                navigateTo(sc)
                                scope.launch { drawerState.close() }
                            },
                            icon = { Icon(sc.icon, contentDescription = null) },
                            label = { Text(label, fontSize = 13.sp, maxLines = 1) },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = brandColors.containerLight,
                                selectedIconColor = brandColors.primary,
                                selectedTextColor = brandColors.primary
                            )
                        )
                    }
                }
                HorizontalDivider()
                NavigationDrawerItem(
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onSignOutClick()
                    },
                    icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = SneatDanger) },
                    label = { Text("Keluar Akun", color = SneatDanger, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { scope.launch { drawerState.open() } },
                                modifier = Modifier.minimumInteractiveComponentSize().testTag("open_sidebar_button")
                            ) {
                                Icon(Icons.Default.Menu, contentDescription = "Buka Sidebar Menu", tint = brandColors.primary)
                            }
                            if (backStack.size > 1) {
                                IconButton(
                                    onClick = { navigateBack() },
                                    modifier = Modifier.minimumInteractiveComponentSize().testTag("top_bar_back_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Kembali ke halaman sebelumnya",
                                        tint = brandColors.primary
                                    )
                                }
                            }
                        }
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (backStack.size <= 1) {
                                SchoolLogoAvatar(
                                    logoBase64 = schoolProfile.logoBase64,
                                    schoolName = schoolProfile.name,
                                    size = 32.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = when {
                                            currentScreen == Screen.DASHBOARD && currentRole == UserRole.SUPERADMIN -> "Dashboard Admin Sekolah"
                                            currentScreen == Screen.DASHBOARD && currentRole == UserRole.GURU -> "Dashboard Guru"
                                            else -> currentScreen.title
                                        },
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                }
                                Text(
                                    text = "${schoolProfile.name} • ${currentRole.label}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = onForceSyncClick,
                            modifier = Modifier.testTag("firebase_sync_button")
                        ) {
                            Icon(
                                imageVector = if (isFirebaseSynced) Icons.Default.CloudDone else Icons.Default.CloudSync,
                                contentDescription = "Sinkronisasi Firebase",
                                tint = if (isFirebaseSynced) SneatSuccess else SneatWarning
                            )
                        }

                        IconButton(onClick = { showNotificationsDialog = true }) {
                            BadgedBox(
                                badge = {
                                    if (unreadNotifications > 0) {
                                        Badge(containerColor = SneatDanger, contentColor = Color.White) {
                                            Text("$unreadNotifications", fontSize = 10.sp)
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Pemberitahuan",
                                    tint = if (unreadNotifications > 0) brandColors.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (currentRole == UserRole.SUPERADMIN) {
                            IconButton(onClick = { showAdminDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = "Kelola Akun & PIN",
                                    tint = brandColors.primary
                                )
                            }
                        }

                        IconButton(onClick = { viewModel.toggleDarkMode() }) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Ganti Tema",
                                tint = brandColors.primary
                            )
                        }

                        IconButton(
                            onClick = onSignOutClick,
                            modifier = Modifier.testTag("sign_out_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Keluar Akun",
                                tint = SneatDanger
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            },
            bottomBar = {
                if (!isExpandedScreen) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp
                    ) {
                        val bottomItems = when (currentRole) {
                            UserRole.SUPERADMIN -> listOf(Screen.DASHBOARD, Screen.USERS, Screen.RPP, Screen.APPEARANCE, Screen.MORE)
                            else -> listOf(Screen.DASHBOARD, Screen.RPP, Screen.ABSENSI, Screen.NILAI, Screen.JURNAL)
                        }
                        bottomItems.forEach { screen ->
                            val isSelected = if (screen == Screen.MORE) {
                                currentScreen !in listOf(Screen.DASHBOARD, Screen.USERS, Screen.RPP, Screen.APPEARANCE)
                            } else currentScreen == screen

                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    if (screen == Screen.MORE) showMoreSheet = true else navigateTo(screen)
                                },
                                icon = { Icon(screen.icon, contentDescription = screen.title) },
                                label = { Text(screen.title, fontSize = 10.sp, maxLines = 1) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = brandColors.primary,
                                    selectedTextColor = brandColors.primary,
                                    indicatorColor = brandColors.containerLight
                                )
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                if ((currentRole == UserRole.SUPERADMIN || currentRole == UserRole.GURU) &&
                    currentScreen != Screen.FORUM_DISKUSI
                ) {
                    FloatingActionButton(
                        onClick = { navigateTo(Screen.FORUM_DISKUSI) },
                        containerColor = brandColors.primary,
                        contentColor = Color.White,
                        modifier = Modifier.testTag("floating_forum_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forum,
                            contentDescription = "Buka Forum Diskusi Lembaga"
                        )
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isExpandedScreen) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.width(104.dp)
                    ) {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            drawerScreens.forEach { (screen, _) ->
                                NavigationRailItem(
                                    selected = currentScreen == screen,
                                    onClick = { navigateTo(screen) },
                                    icon = { Icon(screen.icon, contentDescription = screen.title) },
                                    label = { Text(screen.title, fontSize = 9.sp, maxLines = 1) },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = brandColors.primary,
                                        selectedTextColor = brandColors.primary,
                                        indicatorColor = brandColors.containerLight
                                    )
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    val isRestricted = currentScreen !in allowedScreens && currentScreen != Screen.MORE

                    if (isRestricted) {
                        SneatRouteGuardRestricted(
                            currentRole = currentRole,
                            requiredRole = "Admin Utama Sekolah",
                            onSwitchRoleClick = { navigateTo(Screen.DASHBOARD) }
                        )
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            if (previousScreen != null && currentScreen != Screen.DOCS) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { navigateBack() }
                                            .minimumInteractiveComponentSize()
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                            .testTag("breadcrumb_back_button"),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = "Kembali ke ${previousScreen.title}",
                                                tint = brandColors.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Kembali ke ${previousScreen.title}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = brandColors.primary
                                            )
                                        }
                                        if (backStack.size > 2) {
                                            TextButton(
                                                onClick = { navigateTo(Screen.DASHBOARD) },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                            ) {
                                                Text("Beranda", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                when (currentScreen) {
                                    Screen.SPLASH -> {}
                                    Screen.DASHBOARD -> {
                                        when (currentRole) {
                                            UserRole.SUPERADMIN -> DashboardScreen(
                                                viewModel = viewModel,
                                                onNavigateToMenu = { code -> navigateTo(codeToScreen(code)) }
                                            )
                                            UserRole.GURU -> GuruDashboardScreen(
                                                viewModel = viewModel,
                                                onNavigateToMenu = { code -> navigateTo(codeToScreen(code)) }
                                            )
                                            else -> StageOneRoleReservedScreen(
                                                role = currentRole,
                                                displayName = "",
                                                schoolName = schoolProfile.name,
                                                logoBase64 = schoolProfile.logoBase64,
                                                onSignOutClick = onSignOutClick
                                            )
                                        }
                                    }
                                    Screen.APPEARANCE -> AppearanceSettingsScreen(viewModel = viewModel, onNavigateBack = { navigateBack() })
                                    Screen.PTK_MANAGEMENT -> PtkManagementScreen(viewModel = viewModel, initialTab = 0)
                                    Screen.PTK_GURU_KEPENDIDIKAN -> PtkManagementScreen(viewModel = viewModel, initialTab = 3)
                                    Screen.HOLIDAYS_REF -> ScheduleCalendarScreen(viewModel = viewModel, initialTab = 1)
                                    Screen.SCHEDULE_CALENDAR -> ScheduleCalendarScreen(viewModel = viewModel, initialTab = 0)
                                    Screen.PROFIL -> ProfilSekolahScreen(viewModel = viewModel)
                                    Screen.MASTER_DATA -> MasterDataScreen(viewModel = viewModel)
                                    Screen.USERS -> UserManagementScreen(viewModel = viewModel)
                                    Screen.GURU -> GuruScreen(viewModel = viewModel)
                                    Screen.SISWA -> SiswaScreen(viewModel = viewModel)
                                    Screen.KELAS -> KelasScreen(viewModel = viewModel)
                                    Screen.MAPEL -> MapelScreen(viewModel = viewModel)
                                    Screen.ABSENSI -> AbsensiScreen(viewModel = viewModel)
                                    Screen.NILAI -> NilaiScreen(viewModel = viewModel)
                                    Screen.RPP -> RppScreen(viewModel = viewModel)
                                    Screen.JURNAL -> JurnalMengajarScreen(viewModel = viewModel)
                                    Screen.STORAGE_PRO -> StorageProSettingsScreen(viewModel = viewModel)
                                    Screen.ANNOUNCEMENTS -> AnnouncementScreen(viewModel = viewModel)
                                    Screen.EXPORT -> ExportLaporanScreen(viewModel = viewModel)
                                    Screen.LOGIN_LOGS -> LoginActivityLogScreen(viewModel = viewModel)
                                    Screen.FORUM_DISKUSI -> ForumDiskusiScreen(viewModel = viewModel)
                                    Screen.DOCS -> DocumentationScreen(onBackClick = { navigateBack() })
                                    Screen.MORE -> {}
                                }
                            }
                        }
                    }

                    errorMessage?.let { err ->
                        Snackbar(
                            modifier = Modifier.align(Alignment.TopCenter).padding(16.dp),
                            action = {
                                TextButton(onClick = { viewModel.clearError() }) {
                                    Text("OK", color = Color.White)
                                }
                            },
                            containerColor = SneatDanger
                        ) {
                            Text(err, color = Color.White)
                        }
                    }

                    if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 6.dp
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator(color = brandColors.primary)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("Menyinkronkan ke Firebase...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showMoreSheet && currentRole == UserRole.SUPERADMIN) {
        ModalBottomSheet(
            onDismissRequest = { showMoreSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Semua Menu Admin Sekolah",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                drawerScreens.forEach { (sc, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                navigateTo(sc)
                                showMoreSheet = false
                            }
                            .padding(vertical = 10.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(sc.icon, contentDescription = null, tint = brandColors.primary, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showProDialog) {
        SneatProUpgradeDialog(
            proSubscription = proSubscription,
            onDismiss = { showProDialog = false },
            onTogglePro = { viewModel.togglePro() }
        )
    }

    if (showNotificationsDialog) {
        NotificationDialog(
            viewModel = viewModel,
            onDismiss = { showNotificationsDialog = false }
        )
    }

    if (showAdminDialog && currentRole == UserRole.SUPERADMIN) {
        AdminUserManagementDialog(
            viewModel = viewModel,
            onDismiss = { showAdminDialog = false }
        )
    }
}

private fun codeToScreen(menuCode: String): Screen = when (menuCode) {
    "APPEARANCE" -> Screen.APPEARANCE
    "PTK_MANAGEMENT" -> Screen.PTK_MANAGEMENT
    "PTK_GURU_KEPENDIDIKAN" -> Screen.PTK_GURU_KEPENDIDIKAN
    "HOLIDAYS_REF" -> Screen.HOLIDAYS_REF
    "SCHEDULE_CALENDAR" -> Screen.SCHEDULE_CALENDAR
    "PROFIL" -> Screen.PROFIL
    "MASTER_DATA" -> Screen.MASTER_DATA
    "USERS" -> Screen.USERS
    "GURU" -> Screen.GURU
    "SISWA" -> Screen.SISWA
    "KELAS" -> Screen.KELAS
    "MAPEL" -> Screen.MAPEL
    "ABSENSI" -> Screen.ABSENSI
    "NILAI" -> Screen.NILAI
    "RPP" -> Screen.RPP
    "JURNAL" -> Screen.JURNAL
    "STORAGE_PRO" -> Screen.STORAGE_PRO
    "ANNOUNCEMENTS" -> Screen.ANNOUNCEMENTS
    "EXPORT" -> Screen.EXPORT
    "LOGIN_LOGS" -> Screen.LOGIN_LOGS
    "FORUM_DISKUSI" -> Screen.FORUM_DISKUSI
    "DOCS" -> Screen.DOCS
    else -> Screen.DASHBOARD
}
