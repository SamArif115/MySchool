package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SchoolProfile
import com.example.model.UserRole
import com.example.ui.components.SchoolLogoAvatar
import com.example.ui.components.SneatBadge
import com.example.ui.components.SneatCard
import com.example.ui.theme.*
import com.example.viewmodel.SchoolViewModel

@Composable
fun ProfilSekolahScreen(
    viewModel: SchoolViewModel
) {
    val profile by viewModel.currentSchoolProfile.collectAsState()
    val currentRole by viewModel.currentRole.collectAsState()
    val brandColors = LocalBrandColors.current
    val canEdit = currentRole == UserRole.SUPERADMIN

    var showEditDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // School Identity Header Card
        SneatCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SchoolLogoAvatar(
                    logoBase64 = profile.logoBase64,
                    schoolName = profile.name,
                    size = 64.dp,
                    shape = RoundedCornerShape(16.dp),
                    containerColor = brandColors.containerLight
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SneatBadge(
                            text = "NPSN: ${profile.npsn}",
                            textColor = brandColors.primary,
                            backgroundColor = brandColors.containerLight
                        )
                        SneatBadge(
                            text = "Akreditasi ${profile.akreditasi}",
                            textColor = SneatSuccess,
                            backgroundColor = SneatSuccessLight
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "TA ${profile.academicYear} • Semester ${profile.semester}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(16.dp))

            // Contact & Key Info
            ProfileDetailRow(Icons.Default.Person, "Kepala Sekolah", profile.kepalaSekolah, brandColors.primary)
            ProfileDetailRow(Icons.Default.LocationOn, "Alamat Lengkap", profile.address, brandColors.primary)
            ProfileDetailRow(Icons.Default.Email, "Email Resmi", profile.email, brandColors.primary)
            ProfileDetailRow(Icons.Default.Phone, "Telepon Kantor", profile.phone, brandColors.primary)
            ProfileDetailRow(Icons.Default.Language, "Website", profile.website, brandColors.primary)

            if (canEdit) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { showEditDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = brandColors.primary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit Profil & Struktur Lembaga")
                }
            }
        }

        // Visi & Misi Card
        SneatCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = SneatWarning)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Visi Sekolah",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = profile.vision,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Flag, contentDescription = null, tint = SneatSuccess)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Misi Sekolah",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = profile.mission,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Struktur Organisasi Card
        SneatCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccountTree, contentDescription = null, tint = brandColors.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Struktur Organisasi Lembaga",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = profile.organizationStructure,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
        }
    }

    if (showEditDialog) {
        EditSchoolProfileDialog(
            profile = profile,
            primaryColor = brandColors.primary,
            onDismiss = { showEditDialog = false },
            onSave = { updated ->
                viewModel.updateSchoolProfile(updated) {
                    showEditDialog = false
                }
            }
        )
    }
}

@Composable
private fun ProfileDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    tint: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .size(20.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun EditSchoolProfileDialog(
    profile: SchoolProfile,
    primaryColor: androidx.compose.ui.graphics.Color,
    onDismiss: () -> Unit,
    onSave: (SchoolProfile) -> Unit
) {
    var name by remember { mutableStateOf(profile.name) }
    var npsn by remember { mutableStateOf(profile.npsn) }
    var akreditasi by remember { mutableStateOf(profile.akreditasi) }
    var kepalaSekolah by remember { mutableStateOf(profile.kepalaSekolah) }
    var address by remember { mutableStateOf(profile.address) }
    var email by remember { mutableStateOf(profile.email) }
    var phone by remember { mutableStateOf(profile.phone) }
    var website by remember { mutableStateOf(profile.website) }
    var vision by remember { mutableStateOf(profile.vision) }
    var mission by remember { mutableStateOf(profile.mission) }
    var orgStructure by remember { mutableStateOf(profile.organizationStructure) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profil & Struktur Lembaga", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama Sekolah") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = npsn, onValueChange = { npsn = it }, label = { Text("NPSN (8 Digit)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = akreditasi, onValueChange = { akreditasi = it }, label = { Text("Akreditasi (A/B/C)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = kepalaSekolah, onValueChange = { kepalaSekolah = it }, label = { Text("Kepala Sekolah") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Alamat") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email Sekolah") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Telepon") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = website, onValueChange = { website = it }, label = { Text("Website") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = vision, onValueChange = { vision = it }, label = { Text("Visi") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = mission, onValueChange = { mission = it }, label = { Text("Misi") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = orgStructure, onValueChange = { orgStructure = it }, label = { Text("Struktur Organisasi") }, minLines = 3, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        profile.copy(
                            name = name,
                            npsn = npsn,
                            akreditasi = akreditasi,
                            kepalaSekolah = kepalaSekolah,
                            address = address,
                            email = email,
                            phone = phone,
                            website = website,
                            vision = vision,
                            mission = mission,
                            organizationStructure = orgStructure
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Simpan Perubahan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
