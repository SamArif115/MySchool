package com.example.ui.components

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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TenantSchool
import com.example.model.UserRole
import com.example.ui.theme.*
import com.example.viewmodel.SchoolViewModel

@Composable
fun AuthDialog(
    viewModel: SchoolViewModel,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("Guru / Kepsek", "Siswa (PIN)", "Daftar Lembaga")

    val tenant by viewModel.currentTenant.collectAsState()
    val classes by viewModel.activeClasses.collectAsState()
    val userAccounts by viewModel.userAccounts.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.VpnKey,
                    contentDescription = null,
                    tint = SneatPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Autentikasi & Login Aman",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Tab Selection
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = SneatPrimary,
                    divider = {}
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontSize = 11.sp, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                when (selectedTab) {
                    0 -> StaffLoginContent(
                        viewModel = viewModel,
                        currentTenant = tenant,
                        userAccounts = userAccounts,
                        onLoginSuccess = onDismiss
                    )
                    1 -> StudentPinLoginContent(
                        viewModel = viewModel,
                        currentTenant = tenant,
                        classes = classes,
                        onLoginSuccess = onDismiss
                    )
                    2 -> RegisterInstitutionContent(
                        viewModel = viewModel,
                        onSuccess = onDismiss
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    )
}

@Composable
private fun StaffLoginContent(
    viewModel: SchoolViewModel,
    currentTenant: TenantSchool,
    userAccounts: List<com.example.model.UserAccount>,
    onLoginSuccess: () -> Unit
) {
    var emailInput by remember { mutableStateOf("eko.prasetyo@school.id") }
    var loginError by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            color = SneatPrimaryLight,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = SneatPrimary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Otentikasi Google Sign-In terintegrasi Firestore Whitelist. NIP, Jabatan & Role dibaca otomatis.",
                    fontSize = 11.sp,
                    color = SneatPrimary,
                    lineHeight = 15.sp
                )
            }
        }

        OutlinedTextField(
            value = emailInput,
            onValueChange = {
                emailInput = it
                loginError = null
            },
            label = { Text("Email Akun Google (Gmail)") },
            placeholder = { Text("nama@sekolah.sch.id") },
            singleLine = true,
            isError = loginError != null,
            modifier = Modifier.fillMaxWidth()
        )

        loginError?.let { err ->
            Text(text = err, color = SneatDanger, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        // Quick Preset Email Whitelist Buttons for Testing
        Text("Pilih Akun Terdaftar (Firestore):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        userAccounts.take(4).forEach { acc ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        emailInput = acc.email
                        loginError = null
                    }
                    .padding(vertical = 4.dp, horizontal = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(acc.name, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("${acc.email} • ${acc.jabatan}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    SneatBadge(
                        text = acc.role.label,
                        textColor = SneatPrimary,
                        backgroundColor = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Button(
            onClick = {
                viewModel.loginWithGoogle(
                    email = emailInput,
                    onSuccess = onLoginSuccess,
                    onError = { loginError = it }
                )
            },
            colors = ButtonDefaults.buttonColors(containerColor = SneatPrimary),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Masuk via Google Sign-In", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StudentPinLoginContent(
    viewModel: SchoolViewModel,
    currentTenant: TenantSchool,
    classes: List<com.example.model.SchoolClass>,
    onLoginSuccess: () -> Unit
) {
    var studentName by remember { mutableStateOf("Ahmad Fauzan Syahputra") }
    var selectedClassName by remember { mutableStateOf(classes.firstOrNull()?.name ?: "XII MIPA 1") }
    var pinInput by remember { mutableStateOf("1234") }
    var pinError by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            color = SneatInfoLight,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Pin, contentDescription = null, tint = SneatInfo, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Siswa masuk TANPA akun Google. Cukup Nama + Kelas + PIN Kelas yang diset oleh Admin.",
                    fontSize = 11.sp,
                    color = Color(0xFF036982),
                    lineHeight = 15.sp
                )
            }
        }

        OutlinedTextField(
            value = studentName,
            onValueChange = { studentName = it },
            label = { Text("Nama Lengkap Siswa") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Text("Pilih Kelas Siswa:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(classes) { c ->
                FilterChip(
                    selected = selectedClassName == c.name,
                    onClick = {
                        selectedClassName = c.name
                        pinInput = c.pinKelas // pre-fill for demo ease
                        pinError = null
                    },
                    label = { Text(c.name, fontSize = 11.sp) }
                )
            }
        }

        OutlinedTextField(
            value = pinInput,
            onValueChange = {
                pinInput = it
                pinError = null
            },
            label = { Text("PIN Kelas (4-6 Digit)") },
            placeholder = { Text("Masukkan PIN kelas Anda") },
            singleLine = true,
            isError = pinError != null,
            modifier = Modifier.fillMaxWidth()
        )

        pinError?.let { err ->
            Text(text = err, color = SneatDanger, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        // Catatan pengingat PIN
        Text(
            text = "Catatan: Semua siswa satu kelas menggunakan PIN yang sama. Admin dapat mengganti PIN secara berkala demi keamanan.",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Button(
            onClick = {
                viewModel.loginStudentWithPin(
                    studentName = studentName,
                    className = selectedClassName,
                    pin = pinInput,
                    onSuccess = onLoginSuccess,
                    onError = { pinError = it }
                )
            },
            colors = ButtonDefaults.buttonColors(containerColor = SneatInfo),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Masuk Sebagai Siswa", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RegisterInstitutionContent(
    viewModel: SchoolViewModel,
    onSuccess: () -> Unit
) {
    var schoolName by remember { mutableStateOf("") }
    var adminEmail by remember { mutableStateOf("admin.sekolah@gmail.com") }
    var npsn by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("Jakarta") }
    var level by remember { mutableStateOf("SMA") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            color = SneatSuccessLight,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AddBusiness, contentDescription = null, tint = SneatSuccess, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Admin Utama pertama kali mendaftar menggunakan 1 akun Gmail. Sistem membuat id_sekolah otomatis.",
                    fontSize = 11.sp,
                    color = Color(0xFF1E5B09),
                    lineHeight = 15.sp
                )
            }
        }

        OutlinedTextField(
            value = schoolName,
            onValueChange = { schoolName = it },
            label = { Text("Nama Lembaga / Sekolah Baru *") },
            placeholder = { Text("Contoh: SMA Budi Luhur") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = adminEmail,
            onValueChange = { adminEmail = it },
            label = { Text("Akun Gmail Admin Utama *") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = npsn,
            onValueChange = { npsn = it },
            label = { Text("NPSN / Nomor Izin Lembaga") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = city,
            onValueChange = { city = it },
            label = { Text("Kota / Kabupaten") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                if (schoolName.isNotBlank()) {
                    val id = "SCH-00${System.currentTimeMillis() % 1000}"
                    viewModel.saveTenant(
                        tenant = TenantSchool(
                            idSekolah = id,
                            name = schoolName,
                            npsn = if (npsn.isNotBlank()) npsn else "20108899",
                            level = level,
                            city = city
                        ),
                        akreditasi = "A (Unggul)",
                        kepalaSekolah = "Admin Utama",
                        address = "Jl. Pendidikan No. 1"
                    ) {
                        // Tambahkan akun admin ke whitelist
                        viewModel.registerStaffAccount(
                            com.example.model.UserAccount(
                                id = "USR-$id",
                                idSekolah = id,
                                email = adminEmail,
                                name = "Admin Utama $schoolName",
                                nip = "198001012000011001",
                                role = UserRole.SUPERADMIN,
                                jabatan = "Admin Utama Sistem"
                            )
                        ) {}
                        onSuccess()
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = SneatSuccess),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.AppRegistration, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Daftarkan Lembaga & Buat ID Sekolah", fontWeight = FontWeight.Bold)
        }
    }
}
