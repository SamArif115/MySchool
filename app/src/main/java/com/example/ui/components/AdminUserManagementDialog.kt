package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SchoolClass
import com.example.model.UserAccount
import com.example.model.UserRole
import com.example.ui.theme.*
import com.example.viewmodel.SchoolViewModel

@Composable
fun AdminUserManagementDialog(
    viewModel: SchoolViewModel,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("Whitelist Akun Guru/Kepsek", "Kelola PIN Kelas Siswa")

    val userAccounts by viewModel.userAccounts.collectAsState()
    val classes by viewModel.activeClasses.collectAsState()
    val tenant by viewModel.currentTenant.collectAsState()

    var showAddStaffDialog by remember { mutableStateOf(false) }
    var classToEditPin by remember { mutableStateOf<SchoolClass?>(null) }
    var userToDelete by remember { mutableStateOf<UserAccount?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AdminPanelSettings,
                    contentDescription = null,
                    tint = SneatPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Panel Admin: Akun & PIN Kelas",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
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

                if (selectedTab == 0) {
                    // TAB 1: Kelola Akun Guru & Kepsek (Whitelist Firestore)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = SneatPrimaryLight,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = SneatPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Sebelum Guru/Kepsek login dengan Google, Admin HARUS mendaftarkan email, NIP & Jabatan di sini.",
                                    fontSize = 11.sp,
                                    color = SneatPrimary,
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        Button(
                            onClick = { showAddStaffDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = SneatPrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Daftarkan Akun Guru / Kepsek Baru", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        userAccounts.forEach { acc ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(acc.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("${acc.email} • NIP: ${acc.nip}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(acc.jabatan, fontSize = 10.sp, color = SneatPrimary, fontWeight = FontWeight.Medium)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        SneatBadge(
                                            text = acc.role.label,
                                            textColor = if (acc.role == UserRole.KEPALA_SEKOLAH) SneatSuccess else SneatPrimary,
                                            backgroundColor = if (acc.role == UserRole.KEPALA_SEKOLAH) SneatSuccessLight else SneatPrimaryLight
                                        )
                                        if (acc.role != UserRole.SUPERADMIN) {
                                            IconButton(onClick = { userToDelete = acc }, modifier = Modifier.size(32.dp)) {
                                                Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus", tint = SneatDanger, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // TAB 2: Kelola PIN Kelas Siswa
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = SneatWarningLight,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = SneatWarning, modifier = Modifier.size(18.dp).padding(top = 2.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "PENTING UNTUK ADMIN: Semua siswa satu kelas menggunakan PIN yang sama. Demi keamanan, PIN kelas HARUS diganti secara berkala (misal tiap semester atau sebulan sekali).",
                                    fontSize = 11.sp,
                                    color = Color(0xFF7A5200),
                                    lineHeight = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        classes.forEach { c ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(c.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Wali Kelas: ${c.homeroomTeacher}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("Terakhir Ganti: ${c.pinUpdatedAt}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        SneatBadge(
                                            text = "PIN: ${c.pinKelas}",
                                            textColor = SneatPrimary,
                                            backgroundColor = SneatPrimaryLight
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        IconButton(onClick = { classToEditPin = c }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.Key, contentDescription = "Ganti PIN", tint = SneatPrimary, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    )

    // Modal Tambah Akun Baru
    if (showAddStaffDialog) {
        var email by remember { mutableStateOf("") }
        var name by remember { mutableStateOf("") }
        var nip by remember { mutableStateOf("") }
        var jabatan by remember { mutableStateOf("Guru Mata Pelajaran") }
        var idGuruInput by remember { mutableStateOf("T-${System.currentTimeMillis() % 100}") }
        var idKelasInput by remember { mutableStateOf("XII MIPA 1") }
        var idMapelInput by remember { mutableStateOf("Matematika Peminatan") }
        var role by remember { mutableStateOf(UserRole.GURU) }

        AlertDialog(
            onDismissRequest = { showAddStaffDialog = false },
            title = { Text("Daftarkan Akun ke Firestore", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Gmail Staf *") },
                        placeholder = { Text("guru@gmail.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nama Lengkap & Gelar *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = nip,
                        onValueChange = { nip = it },
                        label = { Text("NIP / Nomor Induk") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = jabatan,
                        onValueChange = { jabatan = it },
                        label = { Text("Jabatan / Tugas Mengajar") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (role == UserRole.GURU) {
                        OutlinedTextField(
                            value = idGuruInput,
                            onValueChange = { idGuruInput = it },
                            label = { Text("ID Guru (id_guru)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = idKelasInput,
                            onValueChange = { idKelasInput = it },
                            label = { Text("Kelas Diampu (id_kelas, pisahkan koma)") },
                            placeholder = { Text("XII MIPA 1, XI IPS 2") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = idMapelInput,
                            onValueChange = { idMapelInput = it },
                            label = { Text("Mapel Diampu (id_mapel, pisahkan koma)") },
                            placeholder = { Text("Matematika Peminatan") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Text("Hak Akses (Role):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = role == UserRole.GURU,
                            onClick = { role = UserRole.GURU },
                            label = { Text("Guru") }
                        )
                        FilterChip(
                            selected = role == UserRole.KEPALA_SEKOLAH,
                            onClick = { role = UserRole.KEPALA_SEKOLAH },
                            label = { Text("Kepala Sekolah") }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val kelasList = idKelasInput.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        val mapelList = idMapelInput.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        viewModel.registerStaffAccount(
                            UserAccount(
                                id = "USR-${System.currentTimeMillis() % 10000}",
                                idSekolah = tenant.idSekolah,
                                email = email.trim().lowercase(),
                                name = name.trim(),
                                nip = if (nip.isNotBlank()) nip.trim() else "-",
                                role = role,
                                jabatan = jabatan.trim(),
                                idGuru = if (role == UserRole.GURU) idGuruInput.trim() else "",
                                idKelas = if (role == UserRole.GURU) kelasList else emptyList(),
                                idMapel = if (role == UserRole.GURU) mapelList else emptyList()
                            )
                        ) {
                            showAddStaffDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SneatPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Daftarkan ke Firestore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddStaffDialog = false }) { Text("Batal") }
            }
        )
    }

    // Modal Ubah PIN Kelas
    classToEditPin?.let { c ->
        var newPin by remember { mutableStateOf(c.pinKelas) }
        AlertDialog(
            onDismissRequest = { classToEditPin = null },
            title = { Text("Ganti PIN Kelas ${c.name}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Masukkan PIN baru untuk seluruh siswa di kelas ${c.name}:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { newPin = it },
                        label = { Text("PIN Kelas (4-6 Digit)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Pengingat: Beritahukan PIN baru kepada siswa dan wali kelas setelah disimpan.",
                        fontSize = 11.sp,
                        color = SneatWarning,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateClassPin(c.id, newPin) {
                            classToEditPin = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SneatPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Simpan PIN Baru")
                }
            },
            dismissButton = {
                TextButton(onClick = { classToEditPin = null }) { Text("Batal") }
            }
        )
    }

    userToDelete?.let { user ->
        SneatConfirmDialog(
            title = "Hapus Akun Whitelist",
            message = "Apakah Anda yakin ingin menghapus akses akun ${user.name} (${user.email}) dari Firestore?",
            onConfirm = {
                viewModel.deleteStaffAccount(user.id)
                userToDelete = null
            },
            onDismiss = { userToDelete = null }
        )
    }
}
