package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.ForumMessage
import com.example.model.ForumMessageType
import com.example.model.UserRole
import com.example.ui.components.ForumVoiceHelper
import com.example.ui.components.SneatBadge
import com.example.ui.components.SneatCard
import com.example.ui.components.SneatConfirmDialog
import com.example.ui.components.SneatRouteGuardRestricted
import com.example.ui.theme.*
import com.example.viewmodel.SchoolViewModel
import kotlinx.coroutines.delay

@Composable
fun ForumDiskusiScreen(viewModel: SchoolViewModel) {
    val currentRole by viewModel.currentRole.collectAsState()
    if (currentRole != UserRole.SUPERADMIN && currentRole != UserRole.GURU) {
        SneatRouteGuardRestricted(
            currentRole = currentRole,
            requiredRole = "Admin Sekolah & Guru Pengajar",
            onSwitchRoleClick = { viewModel.switchRole(UserRole.GURU) }
        )
        return
    }

    val context = LocalContext.current
    val messages by viewModel.forumMessages.collectAsState()
    val forumLimit by viewModel.forumMessageLimit.collectAsState()
    val brandColors = LocalBrandColors.current
    val isAdmin = currentRole == UserRole.SUPERADMIN

    val voiceHelper = remember(context) { ForumVoiceHelper(context) }
    DisposableEffect(voiceHelper) {
        onDispose { voiceHelper.releaseAll() }
    }

    var textInput by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableIntStateOf(0) }
    var playingMessageId by remember { mutableStateOf<String?>(null) }
    var audioError by remember { mutableStateOf<String?>(null) }
    var permissionDeniedRationale by remember { mutableStateOf(false) }
    var messageToDelete by remember { mutableStateOf<ForumMessage?>(null) }

    fun beginRecording() {
        permissionDeniedRationale = false
        audioError = null
        voiceHelper.startRecording()
            .onSuccess {
                isRecording = true
                recordingSeconds = 0
            }
            .onFailure { err ->
                isRecording = false
                audioError = err.localizedMessage ?: "Gagal memulai perekaman suara."
            }
    }

    fun finishAndSendVoiceNote() {
        isRecording = false
        voiceHelper.stopRecording()
            .onSuccess { (base64, durSec) ->
                viewModel.sendForumVoiceMessage(base64, durSec)
            }
            .onFailure { err ->
                audioError = err.localizedMessage ?: "Voice-Note tidak dapat disimpan."
            }
    }

    val recordAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            beginRecording()
        } else {
            permissionDeniedRationale = true
        }
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (isRecording && recordingSeconds < 30) {
                delay(1000L)
                if (isRecording) {
                    recordingSeconds += 1
                    if (recordingSeconds >= 30) {
                        finishAndSendVoiceNote()
                        break
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header & Quota Policy Banner
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Forum Diskusi Lembaga",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Khusus Admin & Guru • Teks & Voice-Note Maks 30 dtk",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    SneatBadge(
                        text = "${messages.size}/$forumLimit",
                        textColor = brandColors.primary,
                        backgroundColor = brandColors.containerLight
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Optimasi Kuota Firebase: Upload gambar/dokumen dinonaktifkan di chat (gunakan menu RPP/PTK).",
                    fontSize = 10.sp,
                    color = SneatInfo,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        if (permissionDeniedRationale) {
            Surface(
                color = SneatWarningLight,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Izin mikrofon (RECORD_AUDIO) ditolak. Aktifkan izin mikrofon untuk merekam Voice-Note.",
                        fontSize = 11.sp,
                        color = Color(0xFF7A5200),
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { permissionDeniedRationale = false }) {
                        Text("Tutup", fontSize = 11.sp)
                    }
                }
            }
        }

        audioError?.let { err ->
            Surface(
                color = SneatDangerLight,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(err, fontSize = 11.sp, color = SneatDanger, modifier = Modifier.weight(1f))
                    TextButton(onClick = { audioError = null }) { Text("OK", fontSize = 11.sp) }
                }
            }
        }

        // Message List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    OutlinedButton(
                        onClick = { viewModel.loadOlderForumMessages() },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("forum_load_older_button")
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Muat Pesan Lama (+25)", fontSize = 12.sp)
                    }
                }
            }

            if (messages.isEmpty()) {
                item {
                    SneatCard {
                        Text(
                            text = "Belum ada percakapan di Forum Diskusi Lembaga. Kirim pesan teks atau rekam Voice-Note singkat.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(messages, key = { it.id }) { msg ->
                    val isSenderAdmin = msg.senderRole.equals("SUPERADMIN", ignoreCase = true)
                    val isPlaying = playingMessageId == msg.id
                    SneatCard(
                        borderColor = if (msg.isArchived) MaterialTheme.colorScheme.outline.copy(alpha = 0.25f) else brandColors.primary.copy(alpha = 0.2f),
                        modifier = Modifier.testTag("forum_msg_${msg.id}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(msg.senderName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                SneatBadge(
                                    text = if (isSenderAdmin) "Admin" else "Guru",
                                    textColor = if (isSenderAdmin) brandColors.primary else SneatSuccess,
                                    backgroundColor = if (isSenderAdmin) brandColors.containerLight else SneatSuccessLight
                                )
                                if (msg.isArchived) {
                                    SneatBadge("Diarsipkan", SneatWarning, SneatWarningLight)
                                }
                            }
                            Text(
                                text = "${msg.sentDate} • ${msg.sentTime}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        if (msg.messageType == ForumMessageType.VOICE_NOTE) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(brandColors.containerLight.copy(alpha = 0.55f))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    IconButton(
                                        onClick = {
                                            audioError = null
                                            voiceHelper.playOrStopVoiceNote(
                                                messageId = msg.id,
                                                base64Audio = msg.voiceBase64,
                                                onStateChanged = { id -> playingMessageId = id },
                                                onError = { err -> audioError = err }
                                            )
                                        },
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("forum_play_vn_${msg.id}")
                                    ) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Default.StopCircle else Icons.Default.PlayCircle,
                                            contentDescription = if (isPlaying) "Hentikan Voice-Note" else "Putar Voice-Note",
                                            tint = brandColors.primary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = if (isPlaying) "Memutar Voice-Note..." else "Voice-Note Lembaga",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Durasi: ${msg.voiceDurationSec} dtk (Maks 30 dtk)",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = brandColors.primary)
                            }
                        } else {
                            Text(
                                text = msg.textContent,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 18.sp
                            )
                        }

                        if (isAdmin) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = { viewModel.archiveForumMessage(msg.id, !msg.isArchived) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.testTag("forum_archive_${msg.id}")
                                ) {
                                    Icon(
                                        imageVector = if (msg.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (msg.isArchived) "Pulihkan" else "Arsip", fontSize = 11.sp)
                                }
                                TextButton(
                                    onClick = { messageToDelete = msg },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.testTag("forum_delete_${msg.id}")
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = SneatDanger, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Hapus", fontSize = 11.sp, color = SneatDanger)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Composer Bar (Text + Voice-Note ONLY, no file/image picker)
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                if (isRecording) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SneatDangerLight)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Mic, contentDescription = null, tint = SneatDanger)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Merekam Voice-Note: $recordingSeconds / 30 dtk",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = SneatDanger
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TextButton(onClick = {
                                isRecording = false
                                voiceHelper.cancelRecording()
                            }) {
                                Text("Batal", fontSize = 11.sp, color = SneatDanger)
                            }
                            Button(
                                onClick = { finishAndSendVoiceNote() },
                                colors = ButtonDefaults.buttonColors(containerColor = SneatDanger),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Kirim VN", fontSize = 11.sp)
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalIconButton(
                            onClick = {
                                val granted = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                if (granted) {
                                    beginRecording()
                                } else {
                                    recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("forum_record_voice_button")
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = "Rekam Voice-Note (Maks 30 Detik)", tint = brandColors.primary)
                        }

                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { if (it.length <= 1000) textInput = it },
                            placeholder = { Text("Tulis pesan diskusi...", fontSize = 13.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("forum_text_input")
                        )

                        FilledIconButton(
                            onClick = {
                                viewModel.sendForumTextMessage(textInput) {
                                    textInput = ""
                                }
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = brandColors.primary),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("forum_send_text_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Kirim Pesan Forum", tint = Color.White)
                        }
                    }
                }
            }
        }
    }

    messageToDelete?.let { msg ->
        SneatConfirmDialog(
            title = "Hapus Pesan Forum",
            message = "Hapus permanen pesan dari ${msg.senderName} untuk menghemat kuota Firestore?",
            onConfirm = {
                viewModel.deleteForumMessage(msg.id)
                messageToDelete = null
            },
            onDismiss = { messageToDelete = null }
        )
    }
}
