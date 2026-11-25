package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ThemePreset
import com.example.model.UserRole
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.SchoolViewModel
import java.util.Locale

@Composable
fun AppearanceSettingsScreen(
    viewModel: SchoolViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val profile by viewModel.currentSchoolProfile.collectAsState()
    val currentRole by viewModel.currentRole.collectAsState()
    val brandColors = LocalBrandColors.current

    if (currentRole != UserRole.SUPERADMIN) {
        SneatRouteGuardRestricted(
            currentRole = currentRole,
            requiredRole = "Admin Utama Sekolah",
            onSwitchRoleClick = { onNavigateBack() }
        )
        return
    }

    var schoolName by remember(profile.idSekolah, profile.name) { mutableStateOf(profile.name) }
    var npsn by remember(profile.idSekolah, profile.npsn) { mutableStateOf(profile.npsn) }
    var phone by remember(profile.idSekolah, profile.phone) { mutableStateOf(profile.phone) }
    var address by remember(profile.idSekolah, profile.address) { mutableStateOf(profile.address) }
    var vision by remember(profile.idSekolah, profile.vision) { mutableStateOf(profile.vision) }
    var mission by remember(profile.idSekolah, profile.mission) { mutableStateOf(profile.mission) }
    var orgStructure by remember(profile.idSekolah, profile.organizationStructure) {
        mutableStateOf(profile.organizationStructure)
    }
    var selectedTheme by remember(profile.idSekolah, profile.themePreset) {
        mutableStateOf(profile.themePreset)
    }
    var logoBase64 by remember(profile.idSekolah, profile.logoBase64) {
        mutableStateOf(profile.logoBase64)
    }
    var logoFileName by remember(profile.idSekolah, profile.logoFileName) {
        mutableStateOf(profile.logoFileName)
    }
    var logoSizeBytes by remember(profile.idSekolah, profile.logoSizeBytes) {
        mutableStateOf(profile.logoSizeBytes)
    }
    var uploadError by remember { mutableStateOf<String?>(null) }

    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            context.processPickedPngJpgImage(uri)
                .onSuccess { picked ->
                    logoBase64 = picked.base64
                    logoFileName = picked.fileName
                    logoSizeBytes = picked.originalSizeBytes
                    uploadError = null
                }
                .onFailure { err ->
                    uploadError = err.localizedMessage ?: "Gagal memproses logo (Maks 5 MB PNG/JPG)."
                }
        }
    }

    val previewPrimary = Color(selectedTheme.primaryHex)
    val previewSecondary = Color(selectedTheme.secondaryHex)
    val previewAccent = Color(selectedTheme.accentHex)
    val previewGradient = Brush.linearGradient(listOf(previewPrimary, previewSecondary))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(brandColors.headerBrush)
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SchoolLogoAvatar(
                    logoBase64 = logoBase64,
                    schoolName = schoolName,
                    size = 48.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    SneatBadge(
                        text = "KHUSUS ADMIN SEKOLAH",
                        textColor = brandColors.primary,
                        backgroundColor = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Pengaturan Tampilan & Identitas Sekolah",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "Logo, tema gradasi, dan nama lembaga otomatis tampil di Splash-Screen & seluruh dashboard.",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }

        // 1. Live Preview Card before saving
        SneatCard(borderColor = previewPrimary.copy(alpha = 0.4f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Live Preview Tampilan Sebelum Simpan",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                SneatBadge(
                    text = selectedTheme.label,
                    textColor = previewPrimary,
                    backgroundColor = Color(selectedTheme.lightBgHex)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Mini Splash Preview
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(110.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.verticalGradient(listOf(previewPrimary, previewSecondary, previewAccent)))
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        SchoolLogoAvatar(
                            logoBase64 = logoBase64,
                            schoolName = schoolName,
                            size = 42.dp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = schoolName.ifBlank { "Nama Sekolah" },
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                        Text(
                            text = "Splash-Screen Preview",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 9.sp
                        )
                    }
                }

                // Mini Header Dashboard Preview
                Column(
                    modifier = Modifier
                        .weight(1.3f)
                        .height(110.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(previewGradient)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SchoolLogoAvatar(
                            logoBase64 = logoBase64,
                            schoolName = schoolName,
                            size = 24.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = schoolName.ifBlank { "Nama Sekolah" },
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                            Text(
                                text = "NPSN: ${npsn.ifBlank { "-" }}",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 9.sp
                            )
                        }
                    }
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Header Dashboard Semua Role", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "Kontras warna hangat siap baca • Multi-Tenant (${profile.idSekolah})",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 2. Upload Logo Sekolah (PNG/JPG <= 5 MB)
        SneatCard {
            Text("Upload / Ganti Logo Sekolah", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(
                text = "Format didukung: PNG atau JPG. Ukuran maksimal 5 MB. Disimpan terenkripsi ke Firestore/Storage.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SchoolLogoAvatar(
                    logoBase64 = logoBase64,
                    schoolName = schoolName,
                    size = 64.dp,
                    shape = RoundedCornerShape(14.dp),
                    containerColor = Color(selectedTheme.lightBgHex)
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (logoFileName.isNotBlank()) logoFileName else "Logo Default My School",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    if (logoSizeBytes > 0L) {
                        val sizeKb = logoSizeBytes / 1024
                        Text(
                            text = "Ukuran file: ${if (sizeKb > 1024) String.format(Locale.US, "%.2f MB", logoSizeBytes.toDouble() / (1024 * 1024)) else "$sizeKb KB"} (Valid <= 5 MB)",
                            fontSize = 11.sp,
                            color = SneatSuccess
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                logoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = previewPrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("upload_school_logo_button")
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pilih Logo", fontSize = 12.sp)
                        }
                        if (logoBase64.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    logoBase64 = ""
                                    logoFileName = ""
                                    logoSizeBytes = 0L
                                    uploadError = null
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Reset", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            uploadError?.let { err ->
                Spacer(modifier = Modifier.height(8.dp))
                Surface(color = SneatDangerLight, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = err,
                        color = SneatDanger,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }

        // 3. Pilih Warna Tema Utama Aplikasi (Default Gradasi Biru-Teal)
        SneatCard {
            Text("Warna Tema Utama Aplikasi", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(
                text = "Pilih kombinasi warna gradasi yang tampil pada header dashboard & splash screen.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(ThemePreset.entries) { preset ->
                    val isSelected = selectedTheme == preset
                    val pColor = Color(preset.primaryHex)
                    val sColor = Color(preset.secondaryHex)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            width = if (isSelected) 2.5.dp else 1.dp,
                            color = if (isSelected) pColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        ),
                        color = if (isSelected) Color(preset.lightBgHex) else MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .width(140.dp)
                            .clickable { selectedTheme = preset }
                            .testTag("theme_preset_${preset.name}")
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Brush.linearGradient(listOf(pColor, sColor))),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .padding(end = 6.dp)
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(Color.White),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = pColor, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = preset.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // 4. Form Identitas Lembaga (Nama, NPSN, Telepon, Alamat, Visi, Misi, Struktur)
        SneatCard {
            Text("Identitas & Profil Lembaga Global", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = schoolName,
                    onValueChange = { schoolName = it },
                    label = { Text("Nama Lembaga / Sekolah (Tampil di Semua Dashboard) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = npsn,
                        onValueChange = { npsn = it },
                        label = { Text("NPSN") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("No. Telepon Sekolah") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Alamat Lengkap Sekolah") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = vision,
                    onValueChange = { vision = it },
                    label = { Text("Visi Sekolah") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = mission,
                    onValueChange = { mission = it },
                    label = { Text("Misi Sekolah") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = orgStructure,
                    onValueChange = { orgStructure = it },
                    label = { Text("Struktur Organisasi Sekolah") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Button(
            onClick = {
                viewModel.updateAppearanceSettings(
                    schoolName = schoolName,
                    npsn = npsn,
                    address = address,
                    phone = phone,
                    vision = vision,
                    mission = mission,
                    organizationStructure = orgStructure,
                    logoBase64 = logoBase64,
                    logoFileName = logoFileName,
                    logoSizeBytes = logoSizeBytes,
                    themePreset = selectedTheme,
                    onSuccess = {}
                )
            },
            colors = ButtonDefaults.buttonColors(containerColor = previewPrimary),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("save_appearance_button")
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Simpan Tampilan & Identitas ke Firebase", fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}
