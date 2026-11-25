package com.example.model

enum class UserRole(val label: String, val description: String) {
    SUPERADMIN("Admin Utama", "Akses Penuh Semua Modul, Kelola Akun & Master Data"),
    KEPALA_SEKOLAH("Kepala Sekolah", "Review & Approval RPP, Monitoring Kinerja Guru"),
    GURU("Guru Pengajar", "Kelola RPP & Upload, Jurnal KBM, Absensi & Nilai Kelas"),
    SISWA("Siswa", "Akses Tugas, Materi, Uji Kompetensi, Rapor Pribadi")
}

enum class ThemePreset(
    val label: String,
    val primaryHex: Long,
    val secondaryHex: Long,
    val accentHex: Long,
    val lightBgHex: Long
) {
    BLUE_TEAL(
        label = "Biru-Teal (Default)",
        primaryHex = 0xFF0284C7,
        secondaryHex = 0xFF0D9488,
        accentHex = 0xFF0369A1,
        lightBgHex = 0xFFE0F2FE
    ),
    INDIGO_SNEAT(
        label = "Indigo Sneat",
        primaryHex = 0xFF6366F1,
        secondaryHex = 0xFF4F46E5,
        accentHex = 0xFF312E81,
        lightBgHex = 0xFFEEF2FF
    ),
    EMERALD_FOREST(
        label = "Emerald Hijau",
        primaryHex = 0xFF059669,
        secondaryHex = 0xFF0D9488,
        accentHex = 0xFF065F46,
        lightBgHex = 0xFFECFDF5
    ),
    ROYAL_AMETHYST(
        label = "Ungu Elegan",
        primaryHex = 0xFF7C3AED,
        secondaryHex = 0xFF4F46E5,
        accentHex = 0xFF5B21B6,
        lightBgHex = 0xFFF3E8FF
    )
}

data class TenantSchool(
    val idSekolah: String,
    val name: String,
    val npsn: String,
    val level: String, // SMA / SMK / SMP / SD / Ponpes
    val city: String,
    val ownerUid: String = ""
)

data class SchoolProfile(
    val idSekolah: String,
    val name: String,
    val npsn: String,
    val akreditasi: String,
    val level: String,
    val address: String,
    val email: String,
    val phone: String,
    val website: String,
    val kepalaSekolah: String,
    val vision: String,
    val mission: String,
    val logoBase64: String = "",
    val logoFileName: String = "",
    val logoSizeBytes: Long = 0L,
    val themePreset: ThemePreset = ThemePreset.BLUE_TEAL,
    val organizationStructure: String = "Kepala Sekolah: Drs. H. Bambang Suryanto, M.Pd.\nWakil Kurikulum: Dra. Hj. Nuraini, M.Pd.\nWakil Kesiswaan: Drs. Eko Prasetyo, M.Si.\nKepala Tata Usaha: Siti Aminah, S.E.",
    val academicYear: String = "2026/2027",
    val semester: String = "Ganjil"
)

// Akun Pengguna Whitelist Firestore (Admin, Kepsek, Guru)
data class UserAccount(
    val id: String,
    val idSekolah: String,
    val email: String,
    val name: String,
    val nip: String,
    val role: UserRole,
    val jabatan: String,
    val isVerified: Boolean = true,
    val idGuru: String = "",
    val idKelas: List<String> = emptyList(),
    val idMapel: List<String> = emptyList()
)

data class Teacher(
    val id: String,
    val idSekolah: String,
    val nip: String,
    val name: String,
    val email: String,
    val phone: String,
    val subject: String,
    val employmentStatus: String, // PNS / P3K / Honorer / Kontrak
    val jabatan: String = "Guru Mata Pelajaran",
    val skNumber: String = "",
    val skDate: String = "",
    val tmtDate: String = "",
    val tenureYears: Int = 0,
    val rankGroup: String = "", // Golongan/Ruang khusus PNS (cth: III/c, IV/a)
    val weeklyHours: Int = 24,
    val classesTaught: List<String> = emptyList(),
    val address: String = "",
    val photoBase64: String = "",
    val skDocumentName: String = "",
    val skDocumentSizeBytes: Long = 0L,
    val isActiveAccount: Boolean = true,
    val additionalDuty: String = "",
    val isNonSatmikal: Boolean = false,
    val satmikalOrigin: String = "",
    val nonSatmikalVerified: Boolean = false,
    val mutationStatus: String = "NONE", // NONE / DIAJUKAN / DISETUJUI / DITOLAK
    val mutationTargetSchool: String = "",
    val transferConfirmed: Boolean = false
)

data class Student(
    val id: String,
    val idSekolah: String,
    val nisn: String,
    val name: String,
    val className: String,
    val gender: String, // "L" or "P"
    val religion: String,
    val phone: String,
    val address: String,
    val status: String = "Aktif", // Aktif / Pindah / Lulus / Dikeluarkan
    val userId: String = "",
    val nis: String = "",
    val birthDate: String = "",
    val parentName: String = "",
    val parentPhone: String = phone,
    val promotionHistory: String = ""
)

data class SchoolClass(
    val id: String,
    val idSekolah: String,
    val name: String,
    val gradeLevel: Int, // 10, 11, 12 or 7, 8, 9
    val homeroomTeacher: String,
    val room: String,
    val studentCount: Int,
    val capacity: Int = 36,
    val pinKelas: String = "1234", // PIN Kelas untuk login Siswa
    val pinUpdatedAt: String = "2026-11-12"
)

data class Subject(
    val id: String,
    val idSekolah: String,
    val code: String,
    val name: String,
    val category: String, // Umum, Peminatan, Kejuruan
    val kkm: Int,
    val hoursPerWeek: Int
)

data class SchoolHoliday(
    val id: String,
    val idSekolah: String,
    val date: String,
    val name: String,
    val category: String = "Libur Nasional" // Libur Nasional / Cuti Bersama / Kalender Akademik
)

data class SchoolAnnouncement(
    val id: String,
    val idSekolah: String,
    val title: String,
    val content: String,
    val targetRole: String = "Semua", // Semua / Guru / Siswa
    val authorName: String = "Admin Sekolah",
    val publishedDate: String = "2026-11-18",
    val userId: String = ""
)

data class LoginActivityLog(
    val id: String,
    val idSekolah: String,
    val uid: String,
    val email: String,
    val displayName: String,
    val role: String,
    val timestamp: String,
    val deviceInfo: String = "Android App",
    val status: String = "Sukses"
)

enum class AttendanceStatus(val label: String, val shortCode: String) {
    HADIR("Hadir", "H"),
    SAKIT("Sakit", "S"),
    IZIN("Izin", "I"),
    ALPHA("Alpha", "A")
}

data class AttendanceRecord(
    val id: String,
    val idSekolah: String,
    val date: String,
    val className: String,
    val studentId: String,
    val studentName: String,
    val status: AttendanceStatus,
    val notes: String = "",
    val teacherId: String = "",
    val subjectName: String = "",
    val studentUserId: String = ""
)

data class GradeRecord(
    val id: String,
    val idSekolah: String,
    val studentId: String,
    val studentName: String,
    val className: String,
    val subjectName: String,
    val tugasScore: Double,
    val utsScore: Double,
    val uasScore: Double,
    val teacherId: String = "",
    val studentUserId: String = ""
) {
    val finalScore: Double
        get() = (tugasScore * 0.3) + (utsScore * 0.3) + (uasScore * 0.4)

    val letterGrade: String
        get() = when {
            finalScore >= 85.0 -> "A"
            finalScore >= 75.0 -> "B"
            finalScore >= 65.0 -> "C"
            else -> "D"
        }

    val isPassed: Boolean
        get() = finalScore >= 75.0
}

enum class RppStatus(val label: String) {
    DRAFT("Draft"),
    SUBMITTED("Menunggu Approval"),
    APPROVED("Disetujui Admin/Kepsek"),
    REJECTED("Perlu Revisi")
}

data class LessonPlan(
    val id: String,
    val idSekolah: String,
    val title: String,
    val curriculum: String, // "Kurikulum Merdeka" or "Kurikulum 2013"
    val subject: String,
    val className: String,
    val meetingCount: Int,
    val duration: String,
    val learningObjectives: String,
    val fileName: String,
    val teacherId: String = "T-01",
    val teacherName: String = "Drs. Eko Prasetyo, M.Si.",
    val teacherEmail: String = "eko.prasetyo@school.id",
    val userId: String = "",
    val reviewNotes: String = "",
    val fileSizeBytes: Long = 2_450_000L, // Max 5MB (5,242,880 bytes)
    val storageUrl: String = "gs://myschool-app.appspot.com/rpp/modul.pdf",
    val status: RppStatus = RppStatus.DRAFT
) {
    val fileSizeFormatted: String
        get() = String.format(java.util.Locale.US, "%.2f MB", fileSizeBytes.toDouble() / (1024 * 1024))
}

data class TeachingJournal(
    val id: String,
    val idSekolah: String,
    val date: String,
    val teacherName: String,
    val subject: String,
    val className: String,
    val periodTime: String,
    val topic: String,
    val activities: String,
    val obstacles: String,
    val reflection: String,
    val documentationFileName: String = "Foto_KBM_Lab.jpg",
    val isCompleted: Boolean = true,
    val teacherId: String = "T-01",
    val userId: String = "",
    val fileSizeBytes: Long = 1_200_000L,
    val monthKey: String = date.take(7) // YYYY-MM
)

// Tugas & Materi Pembelajaran
data class StudentAssignment(
    val id: String,
    val idSekolah: String,
    val className: String,
    val subject: String,
    val title: String,
    val description: String,
    val deadline: String,
    val type: String = "Tugas", // Tugas / Materi / Latihan / Uji Kompetensi
    val fileName: String = "Panduan_Soal.pdf",
    val fileSizeBytes: Long = 1_000_000L,
    val teacherId: String = "T-01",
    val userId: String = ""
)

// Notifikasi Real-Time
data class AppNotification(
    val id: String,
    val idSekolah: String,
    val recipientEmailOrRole: String,
    val title: String,
    val message: String,
    val timestamp: String,
    val isRead: Boolean = false,
    val type: String = "RPP_STATUS",
    val senderUid: String = ""
)

enum class AdNetwork(val displayName: String, val tag: String) {
    ADMOB("Google AdMob", "ca-app-pub-3940256099942544/6300978111"),
    PANGLE("Pangle SDK", "pangle-slot-980124"),
    UNDRADS("UndrAds Network", "undr-slot-school-banner"),
    META("Meta Audience Network", "IMG_16_9_APP_INSTALL#YOUR_PLACEMENT_ID")
}

data class ProSubscription(
    val isPro: Boolean = false,
    val planName: String = if (isPro) "My School PRO Enterprise" else "My School Free Starter",
    val storageUsedMb: Int = 180,
    val storageMaxMb: Int = if (isPro) 51200 else 500, // 50GB vs 500MB
    val activeAdNetwork: AdNetwork = AdNetwork.ADMOB
)

data class AuthenticatedSession(
    val uid: String,
    val email: String,
    val displayName: String,
    val idSekolah: String,
    val role: UserRole,
    val idGuru: String = "",
    val idKelas: List<String> = emptyList(),
    val idMapel: List<String> = emptyList(),
    val studentId: String = "",
    val className: String = ""
)

enum class PtkAttendanceStatus(val label: String) {
    HADIR("Hadir"),
    SAKIT("Sakit"),
    IZIN("Izin"),
    CUTI("Cuti"),
    DINAS_LUAR("Dinas Luar")
}

data class PtkAttendanceRecord(
    val id: String,
    val idSekolah: String,
    val teacherId: String,
    val teacherName: String,
    val date: String,
    val status: PtkAttendanceStatus = PtkAttendanceStatus.HADIR,
    val notes: String = "",
    val recordedByUid: String = ""
)

data class TeachingSchedule(
    val id: String,
    val idSekolah: String,
    val dayOfWeek: String, // Senin..Sabtu
    val startTime: String,
    val endTime: String,
    val className: String,
    val subjectName: String,
    val teacherId: String,
    val teacherName: String,
    val room: String = ""
)

enum class ForumMessageType {
    TEXT,
    VOICE_NOTE
}

data class ForumMessage(
    val id: String,
    val idSekolah: String,
    val senderUid: String,
    val senderName: String,
    val senderRole: String, // SUPERADMIN / GURU
    val messageType: ForumMessageType = ForumMessageType.TEXT,
    val textContent: String = "",
    val voiceBase64: String = "",
    val voiceDurationSec: Int = 0, // Maks 30 detik untuk optimasi kuota Firebase
    val sentDate: String = "",
    val sentTime: String = "",
    val isArchived: Boolean = false,
    val createdAtMillis: Long = 0L
)

