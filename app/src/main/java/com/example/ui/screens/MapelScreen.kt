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
import com.example.model.Subject
import com.example.model.UserRole
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.SchoolViewModel

@Composable
fun MapelScreen(
    viewModel: SchoolViewModel
) {
    val subjects by viewModel.activeSubjects.collectAsState()
    val tenant by viewModel.currentTenant.collectAsState()
    val currentRole by viewModel.currentRole.collectAsState()
    val canManage = currentRole == UserRole.SUPERADMIN || currentRole == UserRole.KEPALA_SEKOLAH

    var showAddEditDialog by remember { mutableStateOf(false) }
    var subjectToEdit by remember { mutableStateOf<Subject?>(null) }
    var subjectToDelete by remember { mutableStateOf<Subject?>(null) }

    Scaffold(
        floatingActionButton = {
            if (canManage) {
                FloatingActionButton(
                    onClick = {
                        subjectToEdit = null
                        showAddEditDialog = true
                    },
                    containerColor = SneatPrimary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah Mapel")
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
                        text = "Mata Pelajaran & Kurikulum",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Total: ${subjects.size} Mata Pelajaran Terdaftar",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(subjects, key = { it.id }) { sub ->
                    SneatCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = sub.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    SneatBadge(
                                        text = sub.code,
                                        textColor = SneatPrimary,
                                        backgroundColor = SneatPrimaryLight
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Kelompok: ${sub.category} • Beban: ${sub.hoursPerWeek} Jam/Minggu",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                SneatBadge(
                                    text = "KKM: ${sub.kkm}",
                                    textColor = SneatWarning,
                                    backgroundColor = SneatWarningLight
                                )
                                if (canManage) {
                                    Row(modifier = Modifier.padding(top = 4.dp)) {
                                        IconButton(onClick = {
                                            subjectToEdit = sub
                                            showAddEditDialog = true
                                        }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = SneatPrimary, modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(onClick = {
                                            subjectToDelete = sub
                                        }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus", tint = SneatDanger, modifier = Modifier.size(16.dp))
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
        AddEditSubjectDialog(
            tenantId = tenant.idSekolah,
            subject = subjectToEdit,
            onDismiss = { showAddEditDialog = false },
            onSave = { saved ->
                viewModel.saveSubject(saved) {
                    showAddEditDialog = false
                }
            }
        )
    }

    subjectToDelete?.let { sub ->
        SneatConfirmDialog(
            title = "Hapus Mata Pelajaran",
            message = "Apakah Anda yakin ingin menghapus mapel ${sub.name}?",
            onConfirm = {
                viewModel.deleteSubject(sub.id)
                subjectToDelete = null
            },
            onDismiss = { subjectToDelete = null }
        )
    }
}

@Composable
private fun AddEditSubjectDialog(
    tenantId: String,
    subject: Subject?,
    onDismiss: () -> Unit,
    onSave: (Subject) -> Unit
) {
    var code by remember { mutableStateOf(subject?.code ?: "") }
    var name by remember { mutableStateOf(subject?.name ?: "") }
    var category by remember { mutableStateOf(subject?.category ?: "Umum") }
    var kkm by remember { mutableStateOf(subject?.kkm?.toString() ?: "75") }
    var hours by remember { mutableStateOf(subject?.hoursPerWeek?.toString() ?: "3") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (subject == null) "Tambah Mata Pelajaran" else "Edit Mata Pelajaran", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Kode Mapel (misal: MAT-01)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Mata Pelajaran") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Kelompok / Kategori", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Umum", "Peminatan", "Kejuruan").forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }
                OutlinedTextField(
                    value = kkm,
                    onValueChange = { kkm = it },
                    label = { Text("Standar KKM (Kriteria Ketuntasan)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = hours,
                    onValueChange = { hours = it },
                    label = { Text("Alokasi Jam / Minggu") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val id = subject?.id ?: "SUB-${System.currentTimeMillis() % 10000}"
                    onSave(
                        Subject(
                            id = id,
                            idSekolah = tenantId,
                            code = code,
                            name = name,
                            category = category,
                            kkm = kkm.toIntOrNull() ?: 75,
                            hoursPerWeek = hours.toIntOrNull() ?: 3
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = SneatPrimary),
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
