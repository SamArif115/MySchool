package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
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
import com.example.model.LessonPlan
import com.example.model.RppStatus
import com.example.model.UserAccount
import com.example.model.UserRole
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.SchoolViewModel
import java.util.Locale

@Composable
fun RppScreen(
    viewModel: SchoolViewModel
) {
    val allRppList by viewModel.activeLessonPlans.collectAsState()
    val guruRppList by viewModel.guruFilteredLessonPlans.collectAsState()
    val tenant by viewModel.currentTenant.collectAsState()
    val currentRole by viewModel.currentRole.collectAsState()
    val currentUser by viewModel.currentUserAccount.collectAsState()
    val proSubscription by viewModel.proSubscription.collectAsState()
    val brandColors = LocalBrandColors.current
    val rppList = if (currentRole == UserRole.GURU) guruRppList else allRppList

    val canCreate = currentRole == UserRole.SUPERADMIN || currentRole == UserRole.GURU
    val canApprove = currentRole == UserRole.SUPERADMIN || currentRole == UserRole.KEPALA_SEKOLAH

    var showAddEditDialog by remember { mutableStateOf(false) }
    var rppToEdit by remember { mutableStateOf<LessonPlan?>(null) }
    var rppToDelete by remember { mutableStateOf<LessonPlan?>(null) }

    var rppToReview by remember { mutableStateOf<LessonPlan?>(null) }
    var reviewAction by remember { mutableStateOf<String?>(null) }

    Scaffold(
        floatingActionButton = {
            if (canCreate) {
                FloatingActionButton(
                    onClick = {
                        rppToEdit = null
                        showAddEditDialog = true
                    },
                    containerColor = brandColors.primary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Buat RPP Baru")
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
                        text = if (currentRole == UserRole.SUPERADMIN) "Kelola Semua RPP Sekolah" else "RPP & Modul Ajar Saya",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Kurikulum Merdeka & K13 • Maksimal 5 MB per file",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            SneatCard(
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                borderColor = brandColors.primary.copy(alpha = 0.25f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = brandColors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Batas Upload Storage: Maks 5 MB per file",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Total Terpakai: ${proSubscription.storageUsedMb} MB / ${proSubscription.storageMaxMb} MB",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    SneatBadge(
                        text = if (proSubscription.isPro) "50 GB PRO" else "500 MB FREE",
                        textColor = if (proSubscription.isPro) SneatSuccess else SneatWarning,
                        backgroundColor = if (proSubscription.isPro) SneatSuccessLight else SneatWarningLight
                    )
                }
            }

            if (rppList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Belum ada dokumen RPP / Modul Ajar.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(rppList, key = { it.id }) { item ->
                        RppCard(
                            rpp = item,
                            currentRole = currentRole,
                            canApprove = canApprove,
                            canCreate = canCreate,
                            primaryColor = brandColors.primary,
                            onSubmitForReview = { viewModel.submitRppForReview(item.id) },
                            onReviewClick = { action ->
                                rppToReview = item
                                reviewAction = action
                            },
                            onEdit = {
                                rppToEdit = item
                                showAddEditDialog = true
                            },
                            onDelete = { rppToDelete = item }
                        )
                    }
                }
            }
        }
    }

    if (showAddEditDialog) {
        AddEditRppDialog(
            tenantId = tenant.idSekolah,
            currentUser = currentUser,
            rpp = rppToEdit,
            primaryColor = brandColors.primary,
            onDismiss = { showAddEditDialog = false },
            onSave = { saved ->
                viewModel.saveLessonPlan(saved) {
                    showAddEditDialog = false
                }
            }
        )
    }

    rppToDelete?.let { r ->
        SneatConfirmDialog(
            title = "Hapus Dokumen RPP",
            message = "Apakah Anda yakin ingin menghapus RPP '${r.title}'?",
            onConfirm = {
                viewModel.deleteLessonPlan(r.id)
                rppToDelete = null
            },
            onDismiss = { rppToDelete = null }
        )
    }

    rppToReview?.let { rpp ->
        val action = reviewAction ?: "APPROVE"
        var reviewNotes by remember {
            mutableStateOf(
                if (action == "APPROVE") "RPP sudah sangat baik dan sesuai panduan Kurikulum Merdeka Fase F."
                else "Mohon lengkapi rubrik penilaian dan evaluasi belajar di lampiran."
            )
        }

        AlertDialog(
            onDismissRequest = {
                rppToReview = null
                reviewAction = null
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (action == "APPROVE") Icons.Default.CheckCircle else Icons.Default.Cancel,
                        contentDescription = null,
                        tint = if (action == "APPROVE") SneatSuccess else SneatDanger,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (action == "APPROVE") "Setujui RPP (Approve)" else "Tolak RPP (Minta Revisi)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "RPP: ${rpp.title}\nGuru Pengajar: ${rpp.teacherName}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Notifikasi persetujuan/penolakan akan dikirim secara real-time ke Dashboard Guru:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = reviewNotes,
                        onValueChange = { reviewNotes = it },
                        label = { Text("Catatan Admin / Kepala Sekolah") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (action == "APPROVE") {
                            viewModel.approveRppByKepsek(rpp.id, reviewNotes)
                        } else {
                            viewModel.rejectRppByKepsek(rpp.id, reviewNotes)
                        }
                        rppToReview = null
                        reviewAction = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (action == "APPROVE") SneatSuccess else SneatDanger
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (action == "APPROVE") "Kirim Persetujuan" else "Kirim Catatan Revisi")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    rppToReview = null
                    reviewAction = null
                }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun RppCard(
    rpp: LessonPlan,
    currentRole: UserRole,
    canApprove: Boolean,
    canCreate: Boolean,
    primaryColor: Color,
    onSubmitForReview: () -> Unit,
    onReviewClick: (String) -> Unit,
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
                    text = rpp.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${rpp.teacherName} • ${rpp.subject} (${rpp.className})",
                    fontSize = 12.sp,
                    color = primaryColor,
                    fontWeight = FontWeight.SemiBold
                )
            }

            SneatBadge(
                text = rpp.status.label,
                textColor = when (rpp.status) {
                    RppStatus.APPROVED -> SneatSuccess
                    RppStatus.REJECTED -> SneatDanger
                    RppStatus.SUBMITTED -> SneatInfo
                    RppStatus.DRAFT -> SneatWarning
                },
                backgroundColor = when (rpp.status) {
                    RppStatus.APPROVED -> SneatSuccessLight
                    RppStatus.REJECTED -> SneatDangerLight
                    RppStatus.SUBMITTED -> SneatInfoLight
                    RppStatus.DRAFT -> SneatWarningLight
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = rpp.learningObjectives,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2
        )

        if (rpp.reviewNotes.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = if (rpp.status == RppStatus.APPROVED) SneatSuccessLight.copy(alpha = 0.6f) else SneatDangerLight.copy(alpha = 0.6f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = if (rpp.status == RppStatus.APPROVED) Icons.Default.CheckCircle else Icons.Default.Info,
                        contentDescription = null,
                        tint = if (rpp.status == RppStatus.APPROVED) SneatSuccess else SneatDanger,
                        modifier = Modifier.size(16.dp).padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (rpp.status == RppStatus.APPROVED) "Catatan Persetujuan:" else "Catatan Revisi:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (rpp.status == RppStatus.APPROVED) SneatSuccess else SneatDanger
                        )
                        Text(
                            text = rpp.reviewNotes,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.AttachFile, contentDescription = null, tint = primaryColor, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${rpp.fileName} (${rpp.fileSizeFormatted})",
                    fontSize = 11.sp,
                    color = primaryColor,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (currentRole == UserRole.GURU && (rpp.status == RppStatus.DRAFT || rpp.status == RppStatus.REJECTED)) {
                    FilledTonalButton(
                        onClick = onSubmitForReview,
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ajukan", fontSize = 10.sp)
                    }
                }

                if (canApprove && (rpp.status == RppStatus.SUBMITTED || rpp.status == RppStatus.DRAFT)) {
                    IconButton(onClick = { onReviewClick("APPROVE") }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Approve RPP", tint = SneatSuccess, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { onReviewClick("REJECT") }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Cancel, contentDescription = "Reject / Revisi", tint = SneatDanger, modifier = Modifier.size(20.dp))
                    }
                }

                if (canCreate && (currentRole == UserRole.SUPERADMIN || rpp.status != RppStatus.APPROVED)) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit RPP", tint = primaryColor, modifier = Modifier.size(16.dp))
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
private fun AddEditRppDialog(
    tenantId: String,
    currentUser: UserAccount?,
    rpp: LessonPlan?,
    primaryColor: Color,
    onDismiss: () -> Unit,
    onSave: (LessonPlan) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(rpp?.title ?: "") }
    var curriculum by remember { mutableStateOf(rpp?.curriculum ?: "Kurikulum Merdeka") }
    var subject by remember { mutableStateOf(rpp?.subject ?: currentUser?.idMapel?.firstOrNull() ?: "") }
    var className by remember { mutableStateOf(rpp?.className ?: currentUser?.idKelas?.firstOrNull() ?: "XII MIPA 1") }
    var objectives by remember { mutableStateOf(rpp?.learningObjectives ?: "") }
    var duration by remember { mutableStateOf(rpp?.duration ?: "2 x 45 Menit") }
    var fileName by remember { mutableStateOf(rpp?.fileName ?: "") }
    var fileSizeMb by remember { mutableStateOf(rpp?.let { String.format(Locale.US, "%.2f", it.fileSizeBytes.toDouble() / (1024 * 1024)) } ?: "2.40") }
    var fileSizeError by remember { mutableStateOf<String?>(null) }

    val docPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            context.processPickedDocument(uri)
                .onSuccess { res ->
                    fileName = res.fileName
                    fileSizeMb = String.format(Locale.US, "%.2f", (res.sizeBytes.toDouble() / (1024 * 1024)).coerceAtLeast(0.05))
                    fileSizeError = null
                }
                .onFailure { fileSizeError = it.localizedMessage }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (rpp == null) "Buat & Upload RPP Baru" else "Edit Dokumen RPP", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Judul Modul / Materi Pokok *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("Pilihan Kurikulum", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Kurikulum Merdeka", "Kurikulum 2013").forEach { c ->
                        FilterChip(selected = curriculum == c, onClick = { curriculum = c }, label = { Text(c, fontSize = 11.sp) })
                    }
                }
                OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Mata Pelajaran") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = className, onValueChange = { className = it }, label = { Text("Kelas Target") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = duration, onValueChange = { duration = it }, label = { Text("Alokasi Waktu") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                FilledTonalButton(
                    onClick = { docPicker.launch(arrayOf("application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")) },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (fileName.isBlank()) "Pilih File PDF / DOCX (Maks 5 MB)" else "File: $fileName", fontSize = 12.sp)
                }

                OutlinedTextField(
                    value = fileSizeMb,
                    onValueChange = {
                        fileSizeMb = it
                        val size = it.trim().replace(',', '.').toDoubleOrNull() ?: 0.0
                        fileSizeError = if (size > 5.0) "Ukuran file (${it} MB) melebihi batas maksimal 5 MB!" else null
                    },
                    label = { Text("Ukuran Dokumen (Maks 5.0 MB)") },
                    isError = fileSizeError != null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                fileSizeError?.let { err ->
                    Text(text = err, color = SneatDanger, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedTextField(value = objectives, onValueChange = { objectives = it }, label = { Text("Tujuan Pembelajaran (CP/KD)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val sizeMb = fileSizeMb.trim().replace(',', '.').toDoubleOrNull() ?: 2.4
                    if (sizeMb > 5.0) {
                        fileSizeError = "Ukuran file (${fileSizeMb} MB) melebihi batas maksimal 5 MB!"
                        return@Button
                    }
                    val id = rpp?.id ?: "RPP-${System.currentTimeMillis() % 10000}"
                    val bytes = (sizeMb * 1024 * 1024).toLong()
                    onSave(
                        LessonPlan(
                            id = id,
                            idSekolah = tenantId,
                            title = title,
                            curriculum = curriculum,
                            subject = subject,
                            className = className,
                            meetingCount = 4,
                            duration = duration,
                            learningObjectives = objectives,
                            fileName = fileName.ifBlank { "Modul_${title.take(15).replace(" ", "_")}.pdf" },
                            teacherId = rpp?.teacherId ?: currentUser?.idGuru?.ifBlank { currentUser.id } ?: "T-01",
                            teacherName = rpp?.teacherName ?: currentUser?.name ?: "Guru Pengajar",
                            teacherEmail = rpp?.teacherEmail ?: currentUser?.email ?: "",
                            reviewNotes = rpp?.reviewNotes ?: "",
                            fileSizeBytes = bytes,
                            storageUrl = "gs://myschool-app.appspot.com/rpp/$id.pdf",
                            status = rpp?.status ?: RppStatus.DRAFT
                        )
                    )
                },
                enabled = fileSizeError == null,
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Simpan ke Storage")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
