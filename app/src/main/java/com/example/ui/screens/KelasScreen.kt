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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SchoolClass
import com.example.model.UserRole
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.SchoolViewModel

@Composable
fun KelasScreen(
    viewModel: SchoolViewModel
) {
    val classes by viewModel.activeClasses.collectAsState()
    val tenant by viewModel.currentTenant.collectAsState()
    val currentRole by viewModel.currentRole.collectAsState()
    val brandColors = LocalBrandColors.current
    val canManage = currentRole == UserRole.SUPERADMIN

    var showAddEditDialog by remember { mutableStateOf(false) }
    var classToEdit by remember { mutableStateOf<SchoolClass?>(null) }
    var classToDelete by remember { mutableStateOf<SchoolClass?>(null) }

    Scaffold(
        floatingActionButton = {
            if (canManage) {
                FloatingActionButton(
                    onClick = {
                        classToEdit = null
                        showAddEditDialog = true
                    },
                    containerColor = brandColors.primary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah Kelas")
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
                        text = "Rombongan Belajar, Kelas & PIN",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Total: ${classes.size} Rombel di ${tenant.name}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(classes, key = { it.id }) { c ->
                    SneatCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = c.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Wali Kelas: ${c.homeroomTeacher}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Ruang: ${c.room} • Tingkat ${c.gradeLevel}",
                                    fontSize = 11.sp,
                                    color = brandColors.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                SneatBadge(
                                    text = "${c.studentCount} / ${c.capacity} Siswa",
                                    textColor = if (c.studentCount >= c.capacity) SneatWarning else SneatSuccess,
                                    backgroundColor = if (c.studentCount >= c.capacity) SneatWarningLight else SneatSuccessLight
                                )
                                if (canManage) {
                                    SneatBadge(
                                        text = "PIN: ${c.pinKelas}",
                                        textColor = brandColors.primary,
                                        backgroundColor = brandColors.containerLight
                                    )
                                    Row(modifier = Modifier.padding(top = 2.dp)) {
                                        IconButton(onClick = {
                                            classToEdit = c
                                            showAddEditDialog = true
                                        }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = brandColors.primary, modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(onClick = {
                                            classToDelete = c
                                        }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus", tint = SneatDanger, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { (c.studentCount.toFloat() / c.capacity.coerceAtLeast(1)).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = brandColors.primary,
                            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    }
                }
            }
        }
    }

    if (showAddEditDialog) {
        AddEditClassDialog(
            tenantId = tenant.idSekolah,
            schoolClass = classToEdit,
            primaryColor = brandColors.primary,
            onDismiss = { showAddEditDialog = false },
            onSave = { saved ->
                viewModel.saveClass(saved) {
                    showAddEditDialog = false
                }
            }
        )
    }

    classToDelete?.let { c ->
        SneatConfirmDialog(
            title = "Hapus Rombel Kelas",
            message = "Apakah Anda yakin ingin menghapus kelas ${c.name}?",
            onConfirm = {
                viewModel.deleteClass(c.id)
                classToDelete = null
            },
            onDismiss = { classToDelete = null }
        )
    }
}

@Composable
private fun AddEditClassDialog(
    tenantId: String,
    schoolClass: SchoolClass?,
    primaryColor: Color,
    onDismiss: () -> Unit,
    onSave: (SchoolClass) -> Unit
) {
    var name by remember { mutableStateOf(schoolClass?.name ?: "") }
    var gradeLevel by remember { mutableStateOf(schoolClass?.gradeLevel?.toString() ?: "10") }
    var homeroom by remember { mutableStateOf(schoolClass?.homeroomTeacher ?: "") }
    var room by remember { mutableStateOf(schoolClass?.room ?: "R-101") }
    var capacity by remember { mutableStateOf(schoolClass?.capacity?.toString() ?: "36") }
    var studentCount by remember { mutableStateOf(schoolClass?.studentCount?.toString() ?: "30") }
    var pinKelas by remember { mutableStateOf(schoolClass?.pinKelas ?: "1234") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (schoolClass == null) "Tambah Kelas & PIN" else "Edit Kelas & PIN", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Kelas (misal: XII MIPA 1)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = gradeLevel,
                    onValueChange = { gradeLevel = it },
                    label = { Text("Tingkat (10 / 11 / 12)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = homeroom,
                    onValueChange = { homeroom = it },
                    label = { Text("Nama Wali Kelas") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = room,
                    onValueChange = { room = it },
                    label = { Text("Ruangan Belajar") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = capacity,
                        onValueChange = { capacity = it },
                        label = { Text("Kapasitas") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = pinKelas,
                        onValueChange = { pinKelas = it },
                        label = { Text("PIN Kelas (Min 4)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val id = schoolClass?.id ?: "C-${System.currentTimeMillis() % 10000}"
                    onSave(
                        SchoolClass(
                            id = id,
                            idSekolah = tenantId,
                            name = name,
                            gradeLevel = gradeLevel.toIntOrNull() ?: 10,
                            homeroomTeacher = homeroom,
                            room = room,
                            studentCount = studentCount.toIntOrNull() ?: 30,
                            capacity = capacity.toIntOrNull() ?: 36,
                            pinKelas = pinKelas.ifBlank { "1234" }
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
