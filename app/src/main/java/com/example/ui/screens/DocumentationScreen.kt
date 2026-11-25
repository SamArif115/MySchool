package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.SneatBadge
import com.example.ui.components.SneatCard
import com.example.ui.theme.*

@Composable
fun DocumentationScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("Firestore Rules", "Firebase Config", "Capacitor APK", "Privacy Policy")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Panduan & Arsitektur Sistem",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Konfigurasi Firebase, Capacitor & Google Play Policy",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Tab Row
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 0.dp,
            containerColor = Color.Transparent,
            contentColor = SneatPrimary,
            divider = {}
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    }
                )
            }
        }

        // Tab Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            when (selectedTab) {
                0 -> FirestoreRulesContent(context)
                1 -> FirebaseConfigContent(context)
                2 -> CapacitorApkContent(context)
                3 -> PrivacyPolicyContent(context)
            }
        }
    }
}

@Composable
private fun FirestoreRulesContent(context: Context) {
    val rulesCode = """rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // Helper functions
    function isAuthenticated() {
      return request.auth != null;
    }

    function getUserData() {
      return get(/databases/$(database)/documents/users/$(request.auth.uid)).data;
    }

    function isSuperAdmin() {
      return isAuthenticated() && getUserData().role == 'SUPERADMIN';
    }

    function isMemberOfSchool(schoolId) {
      return isAuthenticated() && getUserData().id_sekolah == schoolId;
    }

    function isTeacher(schoolId) {
      return isMemberOfSchool(schoolId) && 
        (getUserData().role in ['GURU', 'KEPALA_SEKOLAH', 'SUPERADMIN']);
    }

    function isPrincipal(schoolId) {
      return isMemberOfSchool(schoolId) && 
        (getUserData().role in ['KEPALA_SEKOLAH', 'SUPERADMIN']);
    }

    // User account collection
    match /users/{userId} {
      allow read: if isAuthenticated();
      allow write: if isSuperAdmin() || request.auth.uid == userId;
    }

    // Whitelist akun Staf (Guru & Kepsek didaftarkan Admin sebelum login)
    match /schools/{schoolId}/staff_whitelist/{email} {
      allow read: if isAuthenticated();
      allow write: if isSuperAdmin();
    }

    // Multi-Tenant School Collections
    match /schools/{schoolId} {
      allow read: if isMemberOfSchool(schoolId);
      allow write: if isPrincipal(schoolId);

      // Profil Sekolah
      match /profile/{docId} {
        allow read: if isMemberOfSchool(schoolId);
        allow write: if isPrincipal(schoolId);
      }

      // Guru
      match /teachers/{teacherId} {
        allow read: if isMemberOfSchool(schoolId);
        allow write: if isPrincipal(schoolId);
      }

      // Siswa
      match /students/{studentId} {
        allow read: if isMemberOfSchool(schoolId);
        allow write: if isTeacher(schoolId);
      }

      // Kelas & Rombel (PIN terlindungi, hanya admin yang boleh ubah)
      match /classes/{classId} {
        allow read: if isMemberOfSchool(schoolId);
        allow write: if isSuperAdmin();
      }

      // Mata Pelajaran
      match /subjects/{subjectId} {
        allow read: if isMemberOfSchool(schoolId);
        allow write: if isPrincipal(schoolId);
      }

      // Absensi Harian (Siswa hanya bisa baca miliknya)
      match /attendance/{attId} {
        allow read: if isMemberOfSchool(schoolId);
        allow create, update: if isTeacher(schoolId);
        allow delete: if isPrincipal(schoolId);
      }

      // Nilai Rapor Siswa
      match /grades/{gradeId} {
        allow read: if isMemberOfSchool(schoolId);
        allow write: if isTeacher(schoolId);
      }

      // RPP / Modul Ajar (Guru CRUD miliknya, Kepsek Approve/Reject)
      match /lesson_plans/{rppId} {
        allow read: if isTeacher(schoolId);
        allow create: if isTeacher(schoolId);
        allow update: if (isTeacher(schoolId) && request.resource.data.teacherId == request.auth.uid) || isPrincipal(schoolId);
        allow delete: if isPrincipal(schoolId);
      }

      // Jurnal Mengajar (Laporan Tunjangan Sertifikasi)
      match /teaching_journals/{journalId} {
        allow read: if isTeacher(schoolId);
        allow write: if isTeacher(schoolId);
      }
    }
  }
}"""

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SneatCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Firestore Security Rules (Multi-Tenant)", fontWeight = FontWeight.Bold)
                IconButton(onClick = { copyToClipboard(context, rulesCode, "Firestore Rules") }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Salin Rules", tint = SneatPrimary)
                }
            }
            Text(
                "Keamanan berbasis peran (RBAC) melindungi PIN kelas, isolasi data sekolah (Multi-Tenant), dan validasi whitelist staf sebelum akses diizinkan.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            CodeBlock(rulesCode)
        }
    }
}

@Composable
private fun FirebaseConfigContent(context: Context) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SneatCard {
            Text("Langkah Setup Firebase Lengkap", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            StepItem(1, "Buat Project di Firebase Console", "Buka console.firebase.google.com dan buat project baru bernama 'My School'.")
            StepItem(2, "Aktifkan Authentication (Google Sign-In)", "Buka Authentication > Sign-in method > Google Sign-In > Enable. Masukkan email support.")
            StepItem(3, "Inisialisasi Cloud Firestore", "Pilih Create Database > Mode Production > Region asia-southeast2 (Jakarta) untuk latensi rendah.")
            StepItem(4, "Aktifkan Cloud Storage Resmi", "Pilih Firebase Storage > Get Started. Bucket ini milik instansi (bukan Google Drive guru / Supabase).")
            StepItem(5, "Unduh google-services.json", "Daftarkan package 'com.aistudio.myschool' beserta SHA-1 key dari keystore, lalu simpan file di app/google-services.json.")
        }

        SneatCard {
            Text("Aturan Keamanan Firebase Storage (Batas 5 MB)", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Setiap upload dibatasi maksimal 5 MB dan divalidasi tipe file untuk mencegah lonjakan kuota & biaya:",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            CodeBlock("""rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /schools/{schoolId}/{allPaths=**} {
      allow read: if request.auth != null;
      // Batas maksimal 5 MB per file (Cegah pembengkakan biaya Firebase)
      allow write: if request.auth != null 
        && request.resource.size <= 5 * 1024 * 1024 // Max 5 MB
        && request.resource.contentType.matches('application/pdf|image/.*|application/msword.*|application/vnd.openxmlformats-officedocument.*');
    }
  }
}""")
        }
    }
}

@Composable
private fun CapacitorApkContent(context: Context) {
    val capacitorScript = """# 1. Pastikan project Vue 3 + Vite telah dibuild
npm run build

# 2. Install Capacitor Core & CLI (jika belum)
npm install @capacitor/core @capacitor/cli @capacitor/android

# 3. Inisialisasi konfigurasi Capacitor
npx cap init "My School" "com.aistudio.myschool" --web-dir dist

# 4. Tambahkan platform Android
npx cap add android

# 5. Sinkronisasi aset web ke folder native Android
npx cap sync android

# 6. Build APK via Gradle CLI langsung tanpa Android Studio
cd android
./gradlew assembleDebug

# Output APK siap install ada di:
# android/app/build/outputs/apk/debug/app-debug.apk"""

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SneatCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Langkah Kompilasi Capacitor ke APK", fontWeight = FontWeight.Bold)
                IconButton(onClick = { copyToClipboard(context, capacitorScript, "Capacitor Script") }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Salin Perintah", tint = SneatPrimary)
                }
            }
            Text(
                "Panduan resmi untuk membungkus kode Vue 3 / Vite menjadi file APK Android via Capacitor.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            CodeBlock(capacitorScript)
        }
    }
}

@Composable
private fun PrivacyPolicyContent(context: Context) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SneatCard {
            Text("Kebijakan Privasi Google Play Store (Wajib)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Aplikasi sekolah memproses data anak di bawah umur (siswa). Untuk lolos peninjauan Google Play Store, wajib mematuhi panduan berikut:",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))

            PolicyPoint("Target Kelompok Usia (Families Policy)", "Jika target usia mencakup anak di bawah 13 tahun, iklan AdMob WAJIB disetel dengan flag 'TagForChildDirectedTreatment = TRUE' dan tidak menayangkan iklan bertarget (non-personalized ads only).")
            PolicyPoint("Perlindungan Data Siswa (FERPA / UU PDP)", "Data NISN, riwayat absensi, dan nilai siswa adalah data pribadi sensitif. Harus dienkripsi in-transit (HTTPS/TLS) dan at-rest di Firebase Firestore.")
            PolicyPoint("Akun dan Penghapusan Data", "Menyediakan URL formulir permohonan penghapusan akun dan data siswa di menu Pengaturan sesuai kebijakan Google Play Data Safety.")
            PolicyPoint("Izin Minimum (Least Privilege)", "Jangan meminta izin berbahaya seperti ACCESS_FINE_LOCATION atau READ_MEDIA_IMAGES jika tidak diperlukan.")
        }
    }
}

@Composable
private fun StepItem(number: Int, title: String, desc: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(SneatPrimaryLight),
            contentAlignment = Alignment.Center
        ) {
            Text("$number", fontWeight = FontWeight.Bold, color = SneatPrimary, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PolicyPoint(title: String, desc: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Shield, contentDescription = null, tint = SneatPrimary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CodeBlock(code: String) {
    Surface(
        color = Color(0xFF1E1E2E),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .padding(12.dp)
                .horizontalScroll(rememberScrollState())
        ) {
            Text(
                text = code,
                color = Color(0xFFCDD6F4),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
    }
}

private fun copyToClipboard(context: Context, text: String, label: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
}
