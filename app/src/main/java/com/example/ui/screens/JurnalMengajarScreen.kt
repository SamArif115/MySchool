package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TeachingJournal
import com.example.model.UserAccount
import com.example.model.UserRole
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.SchoolViewModel

@Composable
fun JurnalMengajarScreen(
    viewModel: SchoolViewModel
) {
    val allJournals by viewModel.activeTeachingJournals.collectAsState()
    val guruJournals by viewModel.guruFilteredJournals.collectAsState()
    val tenant by viewModel.currentTenant.collectAsState()
    val currentRole by viewModel.currentRole.collectAsState()
    val currentUser by viewModel.currentUserAccount.collectAsState()
    val brandColors = LocalBrandColors.current
    val journals = if (currentRole == UserRole.GURU) guruJournals else allJournals
    val canCreate = currentRole == UserRole.SUPERADMIN || currentRole == UserRole.GURU

    var showAddEditDialog by remember { mutableStateOf(false) }
    var journalToEdit by remember { mutableStateOf<TeachingJournal?>(null) }
    var journalToDelete by remember { mutableStateOf<TeachingJournal?>(null) }

    Scaffold(
        floatingActionButton = {
            if (canCreate) {
                FloatingActionButton(
                    onClick = {
                        journalToEdit = null
                        showAddEditDialog = true
                    },
                    containerColor = brandColors.primary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tulis Jurnal")
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
                        text = "Dokumentasi & Jurnal Mengajar",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Catatan KBM, Refleksi & Foto Bukti Mengajar (Maks 5 MB)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (journals.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Belum ada catatan jurnal mengajar.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(journals, key = { it.id }) { item ->
                        JournalCard(
                            journal = item,
                            canManage = canCreate,
                            primaryColor = brandColors.primary,
                            onEdit = {
                                journalToEdit = item
                                showAddEditDialog = true
                            },
                            onDelete = { journalToDelete = item }
                        )
                    }
                }
            }
        }
    }

    if (showAddEditDialog) {
        AddEditJournalDialog(
            tenantId = tenant.idSekolah,
            currentUser = currentUser,
            journal = journalToEdit,
            primaryColor = brandColors.primary,
            onDismiss = { showAddEditDialog = false },
            onSave = { saved ->
                viewModel.saveTeachingJournal(saved) {
                    showAddEditDialog = false
                }
            }
        )
    }

    journalToDelete?.let { j ->
        SneatConfirmDialog(
            title = "Hapus Jurnal Mengajar",
            message = "Apakah Anda yakin ingin menghapus catatan jurnal materi '${j.topic}'?",
            onConfirm = {
                viewModel.deleteTeachingJournal(j.id)
                journalToDelete = null
            },
            onDismiss = { journalToDelete = null }
        )
    }
}

@Composable
private fun JournalCard(
    journal: TeachingJournal,
    canManage: Boolean,
    primaryColor: Color,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    SneatCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = journal.topic,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${journal.teacherName} • ${journal.subject}",
                    fontSize = 12.sp,
                    color = primaryColor,
                    fontWeight = FontWeight.SemiBold
                )
            }

            SneatBadge(
                text = journal.periodTime,
                textColor = SneatInfo,
                backgroundColor = SneatInfoLight
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Kegiatan: ${journal.activities}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (journal.obstacles.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Kendala: ${journal.obstacles}",
                style = MaterialTheme.typography.bodySmall,
                color = SneatWarning,
                fontSize = 11.sp
            )
        }

        if (journal.reflection.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Refleksi: ${journal.reflection}",
                style = MaterialTheme.typography.bodySmall,
                color = SneatSuccess,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Tanggal: ${journal.date} • ${journal.className}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Dokumentasi: ${journal.documentationFileName}",
                    fontSize = 11.sp,
                    color = primaryColor,
                    fontWeight = FontWeight.Medium
                )
            }

            if (canManage) {
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = primaryColor, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus", tint = SneatDanger, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AddEditJournalDialog(
    tenantId: String,
    currentUser: UserAccount?,
    journal: TeachingJournal?,
    primaryColor: Color,
    onDismiss: () -> Unit,
    onSave: (TeachingJournal) -> Unit
) {
    val context = LocalContext.current
    var teacherName by remember { mutableStateOf(journal?.teacherName ?: currentUser?.name ?: "Guru Pengajar") }
    var subject by remember { mutableStateOf(journal?.subject ?: currentUser?.idMapel?.firstOrNull() ?: "Matematika Peminatan") }
    var className by remember { mutableStateOf(journal?.className ?: currentUser?.idKelas?.firstOrNull() ?: "XII MIPA 1") }
    var periodTime by remember { mutableStateOf(journal?.periodTime ?: "07:30 - 09:00 (Jam 1-2)") }
    var topic by remember { mutableStateOf(journal?.topic ?: "") }
    var activities by remember { mutableStateOf(journal?.activities ?: "") }
    var obstacles by remember { mutableStateOf(journal?.obstacles ?: "") }
    var reflection by remember { mutableStateOf(journal?.reflection ?: "") }
    var docName by remember { mutableStateOf(journal?.documentationFileName ?: "Foto_KBM_Kelas.jpg") }
    var docSizeBytes by remember { mutableStateOf(journal?.fileSizeBytes ?: 1_200_000L) }
    var uploadErr by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            context.processPickedPngJpgImage(uri)
                .onSuccess { res ->
                    docName = res.fileName
                    docSizeBytes = res.originalSizeBytes
                    uploadErr = null
                }
                .onFailure { uploadErr = it.localizedMessage }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (journal == null) "Dokumentasi & Jurnal Mengajar" else "Edit Jurnal", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = topic, onValueChange = { topic = it }, label = { Text("Topik / Materi Pembelajaran *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Mata Pelajaran") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = className, onValueChange = { className = it }, label = { Text("Kelas") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = periodTime, onValueChange = { periodTime = it }, label = { Text("Waktu / Jam Pertemuan") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = activities, onValueChange = { activities = it }, label = { Text("Deskripsi Kegiatan Belajar Mengajar *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = obstacles, onValueChange = { obstacles = it }, label = { Text("Kendala / Catatan Siswa") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = reflection, onValueChange = { reflection = it }, label = { Text("Refleksi Tindak Lanjut") }, modifier = Modifier.fillMaxWidth())

                FilledTonalButton(
                    onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Upload Foto KBM PNG/JPG (Maks 5 MB): $docName", fontSize = 11.sp)
                }
                uploadErr?.let { Text(it, color = SneatDanger, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val id = journal?.id ?: "JRN-${System.currentTimeMillis() % 10000}"
                    onSave(
                        TeachingJournal(
                            id = id,
                            idSekolah = tenantId,
                            date = "2026-11-18",
                            teacherId = journal?.teacherId ?: currentUser?.idGuru?.ifBlank { currentUser.id } ?: "T-01",
                            teacherName = teacherName,
                            subject = subject,
                            className = className,
                            periodTime = periodTime,
                            topic = topic,
                            activities = activities,
                            obstacles = obstacles,
                            reflection = reflection,
                            documentationFileName = docName,
                            fileSizeBytes = docSizeBytes,
                            isCompleted = true
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Simpan Jurnal")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
