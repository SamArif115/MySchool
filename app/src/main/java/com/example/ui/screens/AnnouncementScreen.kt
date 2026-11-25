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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SchoolAnnouncement
import com.example.model.UserRole
import com.example.ui.components.SneatBadge
import com.example.ui.components.SneatCard
import com.example.ui.components.SneatConfirmDialog
import com.example.ui.theme.*
import com.example.viewmodel.SchoolViewModel

@Composable
fun AnnouncementScreen(
    viewModel: SchoolViewModel
) {
    val announcements by viewModel.activeAnnouncements.collectAsState()
    val tenant by viewModel.currentTenant.collectAsState()
    val currentRole by viewModel.currentRole.collectAsState()
    val brandColors = LocalBrandColors.current
    val canManage = currentRole == UserRole.SUPERADMIN

    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<SchoolAnnouncement?>(null) }
    var deleting by remember { mutableStateOf<SchoolAnnouncement?>(null) }

    Scaffold(
        floatingActionButton = {
            if (canManage) {
                FloatingActionButton(
                    onClick = {
                        editing = null
                        showDialog = true
                    },
                    containerColor = brandColors.primary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_announcement_fab")
                ) {
                    Icon(Icons.Default.Campaign, contentDescription = "Buat Pengumuman")
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
            Text(
                text = "Pengumuman Resmi Sekolah (${announcements.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(announcements, key = { it.id }) { a ->
                    SneatCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(a.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(
                                    text = "${a.publishedDate} • Oleh ${a.authorName}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            SneatBadge(
                                text = "Target: ${a.targetRole}",
                                textColor = brandColors.primary,
                                backgroundColor = brandColors.containerLight
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(a.content, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                        if (canManage) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                IconButton(onClick = {
                                    editing = a
                                    showDialog = true
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = brandColors.primary)
                                }
                                IconButton(onClick = { deleting = a }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus", tint = SneatDanger)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        var title by remember { mutableStateOf(editing?.title ?: "") }
        var content by remember { mutableStateOf(editing?.content ?: "") }
        var targetRole by remember { mutableStateOf(editing?.targetRole ?: "Semua") }
        var publishedDate by remember { mutableStateOf(editing?.publishedDate ?: "2026-11-18") }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (editing == null) "Buat Pengumuman Sekolah" else "Edit Pengumuman", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Judul Pengumuman *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("Isi Pengumuman *") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = publishedDate, onValueChange = { publishedDate = it }, label = { Text("Tanggal") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Text("Target Penerima", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Semua", "Guru", "Siswa").forEach { role ->
                            FilterChip(selected = targetRole == role, onClick = { targetRole = role }, label = { Text(role, fontSize = 11.sp) })
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = editing?.id ?: "ANN-${System.currentTimeMillis() % 10000}"
                        viewModel.saveAnnouncement(
                            SchoolAnnouncement(
                                id = id,
                                idSekolah = tenant.idSekolah,
                                title = title,
                                content = content,
                                targetRole = targetRole,
                                publishedDate = publishedDate
                            )
                        ) { showDialog = false }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = brandColors.primary),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("Publikasikan") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Batal") } }
        )
    }

    deleting?.let { a ->
        SneatConfirmDialog(
            title = "Hapus Pengumuman",
            message = "Hapus pengumuman '${a.title}'?",
            onConfirm = {
                viewModel.deleteAnnouncement(a.id)
                deleting = null
            },
            onDismiss = { deleting = null }
        )
    }
}
