package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CachedSchoolBranding
import com.example.model.SchoolProfile
import com.example.model.TenantSchool
import com.example.ui.components.SchoolLogoAvatar
import com.example.ui.components.SneatBadge
import com.example.ui.components.SneatCard
import com.example.ui.theme.*
import com.example.viewmodel.SchoolViewModel

@Composable
fun SignInScreen(
    viewModel: SchoolViewModel,
    cachedBranding: CachedSchoolBranding = CachedSchoolBranding(),
    isSignedInToFirebase: Boolean,
    firebaseEmail: String,
    firebaseDisplayName: String,
    isSigningIn: Boolean,
    authError: String?,
    onGoogleSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onBackToSplashClick: () -> Unit = {}
) {
    val schoolDirectory by viewModel.schoolDirectory.collectAsState()
    val publicClasses by viewModel.publicClassesForOnboarding.collectAsState()
    val brandColors = LocalBrandColors.current

    var selectedMode by remember { mutableStateOf(0) } // 0 = Daftar Lembaga (Admin), 1 = Siswa (PIN Kelas)
    var schoolName by remember { mutableStateOf("") }
    var npsn by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("Jakarta Selatan") }
    var level by remember { mutableStateOf("SMA") }
    var seedDemoData by remember { mutableStateOf(true) }

    var selectedSchoolForStudent by remember(schoolDirectory) { mutableStateOf(schoolDirectory.firstOrNull()) }
    var selectedClassForStudent by remember(publicClasses) { mutableStateOf(publicClasses.firstOrNull()) }
    var studentNameInput by remember { mutableStateOf(firebaseDisplayName.ifBlank { "" }) }
    var pinInput by remember { mutableStateOf("") }

    LaunchedEffect(selectedSchoolForStudent?.idSekolah) {
        selectedSchoolForStudent?.idSekolah?.let { viewModel.loadPublicClassesForSchool(it) }
    }

    BackHandler {
        if (isSignedInToFirebase) {
            onSignOutClick()
        } else {
            onBackToSplashClick()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        if (isSignedInToFirebase) onSignOutClick() else onBackToSplashClick()
                    },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("sign_in_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali ke halaman sebelumnya",
                        tint = brandColors.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isSignedInToFirebase) "Ganti Akun / Kembali" else "Kembali ke Splash",
                        color = brandColors.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            SchoolLogoAvatar(
                logoBase64 = cachedBranding.logoBase64,
                schoolName = cachedBranding.schoolName,
                size = 84.dp,
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.surface
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = cachedBranding.schoolName.ifBlank { "My School" },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = brandColors.primary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Sistem Manajemen Sekolah Full Firebase • Tahap 1 (Dashboard Admin & Guru)",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            if (!isSignedInToFirebase) {
                SneatCard(
                    modifier = Modifier.widthIn(max = 520.dp),
                    borderColor = brandColors.primary.copy(alpha = 0.25f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(brandColors.headerBrush)
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Masuk ke Portal Sekolah",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Gunakan akun Google (Admin Sekolah / Guru) yang terdaftar di Firestore. Role Anda dibaca otomatis dan diarahkan ke Dashboard Admin atau Dashboard Guru.",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    authError?.let { err ->
                        Surface(
                            color = SneatDangerLight,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = err,
                                color = SneatDanger,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Button(
                        onClick = onGoogleSignInClick,
                        enabled = !isSigningIn,
                        colors = ButtonDefaults.buttonColors(containerColor = brandColors.primary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("google_sign_in_button")
                    ) {
                        if (isSigningIn) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Menghubungkan ke Google...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Masuk dengan Google (Admin & Guru)", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        color = brandColors.containerLight.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "Alur Verifikasi RBAC Otomatis:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = brandColors.primary
                            )
                            Text("• Admin Utama: Mengelola profil, tampilan/logo, master data, guru, siswa & RPP.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("• Guru Pengajar: Daftarkan email di Menu Kelola Guru Admin untuk akses otomatis Dashboard Guru.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("• Lembaga baru? Setelah login Google, Anda dapat langsung mendaftarkan lembaga sekolah baru.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            } else {
                // STEP 2: User sudah terautentikasi Google, tapi belum terdaftar di Whitelist/Lembaga mana pun -> Onboarding
                SneatCard(
                    modifier = Modifier.widthIn(max = 560.dp),
                    borderColor = brandColors.primary.copy(alpha = 0.25f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            SneatBadge("AKUN GOOGLE TERVERIFIKASI", SneatSuccess, SneatSuccessLight)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(firebaseEmail, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        TextButton(onClick = onSignOutClick) {
                            Text("Ganti Akun", fontSize = 12.sp, color = SneatDanger)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Email Anda belum terhubung ke lembaga sekolah. Pilih salah satu langkah berikut:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    TabRow(
                        selectedTabIndex = selectedMode,
                        containerColor = Color.Transparent,
                        contentColor = SneatPrimary
                    ) {
                        Tab(
                            selected = selectedMode == 0,
                            onClick = { selectedMode = 0 },
                            text = { Text("Daftarkan Lembaga (Admin)", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedMode == 1,
                            onClick = { selectedMode = 1 },
                            text = { Text("Masuk Siswa (PIN Kelas)", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    authError?.let { err ->
                        Surface(
                            color = SneatDangerLight,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(err, color = SneatDanger, fontSize = 11.sp, modifier = Modifier.padding(10.dp))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (selectedMode == 0) {
                        OutlinedTextField(
                            value = schoolName,
                            onValueChange = { schoolName = it },
                            label = { Text("Nama Lembaga / Sekolah *") },
                            placeholder = { Text("Contoh: SMA Negeri 1 Maju Bersama") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = npsn,
                            onValueChange = { npsn = it },
                            label = { Text("NPSN / Kode Lembaga") },
                            placeholder = { Text("20108745") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(listOf("SMA", "SMK", "SMP", "SD", "Ponpes")) { lvl ->
                                FilterChip(selected = level == lvl, onClick = { level = lvl }, label = { Text(lvl, fontSize = 11.sp) })
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = city,
                            onValueChange = { city = it },
                            label = { Text("Kota / Kabupaten") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = seedDemoData, onCheckedChange = { seedDemoData = it })
                            Text("Isi data awal (kelas, mapel, RPP & contoh siswa) ke Firestore", fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                if (schoolName.isNotBlank()) {
                                    val newId = "SCH-${System.currentTimeMillis() % 100000}"
                                    val safeName = schoolName.trim()
                                    viewModel.registerNewSchoolInFirebase(
                                        tenant = TenantSchool(
                                            idSekolah = newId,
                                            name = safeName,
                                            npsn = npsn.ifBlank { "20108745" },
                                            level = level,
                                            city = city.ifBlank { "Indonesia" }
                                        ),
                                        profile = SchoolProfile(
                                            idSekolah = newId,
                                            name = safeName,
                                            npsn = npsn.ifBlank { "20108745" },
                                            akreditasi = "A (Unggul)",
                                            level = level,
                                            address = "Jl. Pendidikan No. 1, $city",
                                            email = firebaseEmail,
                                            phone = "(021) 1234567",
                                            website = "https://myschool.sch.id",
                                            kepalaSekolah = "Kepala Sekolah $safeName",
                                            vision = "Mewujudkan lembaga pendidikan $safeName yang unggul dan berkarakter.",
                                            mission = "1. Pembelajaran bermutu.\n2. Digitalisasi tata kelola sekolah."
                                        ),
                                        seedStarterData = seedDemoData
                                    )
                                }
                            },
                            enabled = schoolName.isNotBlank() && !isSigningIn,
                            colors = ButtonDefaults.buttonColors(containerColor = SneatSuccess),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_school_firebase_button")
                        ) {
                            Icon(Icons.Default.AddBusiness, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Buat Sekolah & Sinkronkan ke Firebase", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // Student Class PIN onboarding
                        if (schoolDirectory.isEmpty()) {
                            Text(
                                text = "Belum ada lembaga sekolah terdaftar di Firestore. Minta Admin Utama mendaftarkan lembaga terlebih dahulu.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text("Pilih Lembaga / Sekolah:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(schoolDirectory) { sch ->
                                    FilterChip(
                                        selected = selectedSchoolForStudent?.idSekolah == sch.idSekolah,
                                        onClick = { selectedSchoolForStudent = sch },
                                        label = { Text("${sch.name} (${sch.idSekolah})", fontSize = 10.sp) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Pilih Kelas:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(publicClasses) { cls ->
                                    FilterChip(
                                        selected = selectedClassForStudent?.id == cls.id,
                                        onClick = { selectedClassForStudent = cls },
                                        label = { Text(cls.name, fontSize = 11.sp) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = studentNameInput,
                                onValueChange = { studentNameInput = it },
                                label = { Text("Nama Lengkap Siswa *") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = pinInput,
                                onValueChange = { pinInput = it },
                                label = { Text("PIN Kelas dari Admin *") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    val sch = selectedSchoolForStudent
                                    val cls = selectedClassForStudent
                                    if (sch != null && cls != null && studentNameInput.isNotBlank() && pinInput.isNotBlank()) {
                                        viewModel.verifyStudentPinInFirebase(
                                            schoolId = sch.idSekolah,
                                            classId = cls.id,
                                            className = cls.name,
                                            studentName = studentNameInput,
                                            pin = pinInput
                                        )
                                    }
                                },
                                enabled = selectedSchoolForStudent != null && selectedClassForStudent != null && studentNameInput.isNotBlank() && pinInput.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = SneatInfo),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Verifikasi PIN & Masuk Dashboard Siswa", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
