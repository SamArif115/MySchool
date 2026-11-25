package com.example.data

import com.example.model.*

object MockDataProvider {

    val tenantSchools = listOf(
        TenantSchool(
            idSekolah = "SCH-001",
            name = "SMA Negeri 1 Maju Bersama",
            npsn = "20108745",
            level = "SMA",
            city = "Jakarta Selatan"
        ),
        TenantSchool(
            idSekolah = "SCH-002",
            name = "SMK Merdeka Nusantara",
            npsn = "20109923",
            level = "SMK",
            city = "Kota Bandung"
        ),
        TenantSchool(
            idSekolah = "SCH-003",
            name = "SMP Bintang Prestasi",
            npsn = "20104512",
            level = "SMP",
            city = "Kota Surabaya"
        )
    )

    val schoolProfiles = mutableMapOf(
        "SCH-001" to SchoolProfile(
            idSekolah = "SCH-001",
            name = "SMA Negeri 1 Maju Bersama",
            npsn = "20108745",
            akreditasi = "A (Unggul)",
            level = "SMA",
            address = "Jl. Pendidikan No. 45, Kebayoran Baru, Jakarta Selatan",
            email = "info@sman1majubersama.sch.id",
            phone = "(021) 7892341",
            website = "https://sman1majubersama.sch.id",
            kepalaSekolah = "Drs. H. Bambang Suryanto, M.Pd.",
            vision = "Mewujudkan insan berkarakter Pancasila, berdaya saing global, dan berwawasan teknologi.",
            mission = "1. Menyelenggarakan pembelajaran berdiferensiasi berkualitas tinggi.\n2. Mengembangkan literasi digital dan sains modern.\n3. Membina budi pekerti luhur dan jiwa kewirausahaan."
        ),
        "SCH-002" to SchoolProfile(
            idSekolah = "SCH-002",
            name = "SMK Merdeka Nusantara",
            npsn = "20109923",
            akreditasi = "A",
            level = "SMK",
            address = "Jl. Industri Kreatif Kav. 8, Buahbatu, Bandung",
            email = "kontak@smkmerdekanusantara.sch.id",
            phone = "(022) 8732109",
            website = "https://smkmerdekanusantara.sch.id",
            kepalaSekolah = "Ir. Siti Rahmawati, M.T.",
            vision = "Menjadi SMK Pusat Keunggulan yang mencetak lulusan siap kerja dan adaptif industri 4.0.",
            mission = "1. Menerapkan kurikulum berbasis Teaching Factory dan industri mitra.\n2. Sertifikasi kompetensi BNSP untuk semua lulusan.\n3. Inkubasi startup siswa."
        ),
        "SCH-003" to SchoolProfile(
            idSekolah = "SCH-003",
            name = "SMP Bintang Prestasi",
            npsn = "20104512",
            akreditasi = "A",
            level = "SMP",
            address = "Jl. Darmo Permai Indah II No. 12, Surabaya",
            email = "admin@smpbintangprestasi.sch.id",
            phone = "(031) 5678901",
            website = "https://smpbintangprestasi.sch.id",
            kepalaSekolah = "Ahmad Zaelani, S.Pd., M.Ed.",
            vision = "Unggul dalam prestasi akademik, santun dalam budi pekerti, dan peduli lingkungan.",
            mission = "1. Memaksimalkan potensi bakat minat siswa.\n2. Pembelajaran aktif dan ramah anak.\n3. Program adiwiyata mandiri."
        )
    )

    // Whitelist akun Firestore untuk Guru & Kepsek yang didaftarkan Admin Utama
    fun initialUserAccounts(): List<UserAccount> = listOf(
        UserAccount(
            id = "USR-01",
            idSekolah = "SCH-001",
            email = "admin@sman1majubersama.sch.id",
            name = "Bambang Suryanto, M.Pd.",
            nip = "197501011999031001",
            role = UserRole.SUPERADMIN,
            jabatan = "Admin Utama Sistem Sekolah"
        ),
        UserAccount(
            id = "USR-02",
            idSekolah = "SCH-001",
            email = "kepsek@sman1majubersama.sch.id",
            name = "Dra. Hj. Nuraini, M.Pd.",
            nip = "197805122005011003",
            role = UserRole.KEPALA_SEKOLAH,
            jabatan = "Kepala Sekolah"
        ),
        UserAccount(
            id = "USR-03",
            idSekolah = "SCH-001",
            email = "eko.prasetyo@school.id",
            name = "Drs. Eko Prasetyo, M.Si.",
            nip = "197805122005011003",
            role = UserRole.GURU,
            jabatan = "Guru Matematika Peminatan & Wali Kelas"
        ),
        UserAccount(
            id = "USR-04",
            idSekolah = "SCH-001",
            email = "dewi.anggraini@school.id",
            name = "Dewi Anggraini, S.Pd.",
            nip = "198411032010012015",
            role = UserRole.GURU,
            jabatan = "Guru Bahasa Inggris"
        ),
        UserAccount(
            id = "USR-05",
            idSekolah = "SCH-001",
            email = "rizky.f@school.id",
            name = "Rizky Firmansyah, S.Pd.",
            nip = "199103242019031008",
            role = UserRole.GURU,
            jabatan = "Guru Fisika Terapan"
        )
    )

    fun initialTeachers(): List<Teacher> = listOf(
        Teacher(
            id = "T-01", idSekolah = "SCH-001", nip = "197805122005011003",
            name = "Drs. Eko Prasetyo, M.Si.", email = "eko.prasetyo@school.id", phone = "08128930129",
            subject = "Matematika Peminatan", employmentStatus = "PNS",
            jabatan = "Guru Madya & Wali Kelas XII MIPA 1", skNumber = "SK-823/2005",
            skDate = "2005-01-12", tmtDate = "2005-02-01", tenureYears = 21, rankGroup = "IV/a",
            weeklyHours = 24, classesTaught = listOf("XII MIPA 1", "XII MIPA 2"),
            address = "Jl. Kebayoran Baru No. 12", skDocumentName = "SK_PNS_Eko_2005.pdf",
            skDocumentSizeBytes = 1_450_000L, isActiveAccount = true,
            additionalDuty = "Wali Kelas XII MIPA 1 & Koordinator Olimpiade",
            isNonSatmikal = false, satmikalOrigin = "", nonSatmikalVerified = true,
            mutationStatus = "NONE", mutationTargetSchool = "", transferConfirmed = false
        ),
        Teacher(
            id = "T-02", idSekolah = "SCH-001", nip = "198411032010012015",
            name = "Dewi Anggraini, S.Pd.", email = "dewi.anggraini@school.id", phone = "08139045882",
            subject = "Bahasa Inggris Lanjut", employmentStatus = "PNS",
            jabatan = "Guru Muda Bahasa Inggris", skNumber = "SK-114/2010",
            skDate = "2010-01-03", tmtDate = "2010-02-01", tenureYears = 16, rankGroup = "III/d",
            weeklyHours = 24, classesTaught = listOf("XI IPS 2", "XII MIPA 1"),
            address = "Jl. Kemang Selatan No. 8", skDocumentName = "SK_PNS_Dewi_2010.pdf",
            skDocumentSizeBytes = 1_220_000L, isActiveAccount = true,
            additionalDuty = "Pembina English Club & OSIS",
            isNonSatmikal = false, satmikalOrigin = "", nonSatmikalVerified = true,
            mutationStatus = "DIAJUKAN", mutationTargetSchool = "SMA Negeri 8 Jakarta", transferConfirmed = false
        ),
        Teacher(
            id = "T-03", idSekolah = "SCH-001", nip = "199103242019031008",
            name = "Rizky Firmansyah, S.Pd.", email = "rizky.f@school.id", phone = "08215678129",
            subject = "Fisika Terapan", employmentStatus = "P3K",
            jabatan = "Guru P3K Fisika", skNumber = "SK-P3K-098/2021",
            skDate = "2021-03-10", tmtDate = "2021-04-01", tenureYears = 5, rankGroup = "",
            weeklyHours = 22, classesTaught = listOf("XII MIPA 1"),
            address = "Jl. Cilandak KKO No. 19", skDocumentName = "SK_P3K_Rizky.pdf",
            skDocumentSizeBytes = 980_000L, isActiveAccount = true,
            additionalDuty = "Kepala Laboratorium Fisika",
            isNonSatmikal = false, satmikalOrigin = "", nonSatmikalVerified = true,
            mutationStatus = "NONE", mutationTargetSchool = "", transferConfirmed = false
        ),
        Teacher(
            id = "T-04", idSekolah = "SCH-001", nip = "199507182022022001",
            name = "Anisa Nur Fitria, S.Si.", email = "anisa.fitria@school.id", phone = "08571239845",
            subject = "Biologi Molekuler", employmentStatus = "Honorer",
            jabatan = "Guru Tidak Tetap (GTT)", skNumber = "SK-GTT-012/2022",
            skDate = "2022-07-01", tmtDate = "2022-07-15", tenureYears = 4, rankGroup = "",
            weeklyHours = 18, classesTaught = listOf("XII MIPA 2"),
            address = "Jl. Pasar Minggu No. 4", skDocumentName = "SK_Honor_Anisa.pdf",
            skDocumentSizeBytes = 750_000L, isActiveAccount = true,
            additionalDuty = "Pembina KIR / Sains",
            isNonSatmikal = true, satmikalOrigin = "SMA Negeri 6 Jakarta", nonSatmikalVerified = false,
            mutationStatus = "NONE", mutationTargetSchool = "", transferConfirmed = false
        ),
        Teacher(
            id = "T-05", idSekolah = "SCH-001", nip = "198009152008011004",
            name = "Budi Santoso, S.Kom.", email = "budi.santoso@school.id", phone = "08129988776",
            subject = "Informatika & Coding", employmentStatus = "Kontrak",
            jabatan = "Guru Kontrak Yayasan/Komite", skNumber = "SK-KTR-045/2023",
            skDate = "2023-01-05", tmtDate = "2023-01-10", tenureYears = 3, rankGroup = "",
            weeklyHours = 20, classesTaught = listOf("X Merdeka 1"),
            address = "Jl. Fatmawati No. 31", skDocumentName = "SK_Kontrak_Budi.pdf",
            skDocumentSizeBytes = 890_000L, isActiveAccount = true,
            additionalDuty = "Operator Dapodik & IT Sekolah",
            isNonSatmikal = true, satmikalOrigin = "SMK Telkom Jakarta", nonSatmikalVerified = true,
            mutationStatus = "NONE", mutationTargetSchool = "", transferConfirmed = true
        ),
        Teacher("T-06", "SCH-002", "198502102011011002", "Hendra Wijaya, S.T.", "hendra.w@smkmerdeka.id", "08131122334", "Administrasi Server Jaringan", "PNS", rankGroup = "III/d", weeklyHours = 26, classesTaught = listOf("XII TKJ 1")),
        Teacher("T-07", "SCH-002", "199308152020122005", "Nurul Hidayati, S.Pd.", "nurul.h@smkmerdeka.id", "08223344556", "Bahasa Indonesia", "P3K", weeklyHours = 24, classesTaught = listOf("XII TKJ 1")),
        Teacher("T-08", "SCH-003", "198812042014022003", "Farida Kusumastuti, S.Pd.", "farida.k@smpbintang.id", "08174455667", "IPA Terpadu", "PNS", rankGroup = "III/c", weeklyHours = 24, classesTaught = listOf("IX Unggulan"))
    )

    fun initialStudents(): List<Student> = listOf(
        Student("S-01", "SCH-001", "0078129031", "Ahmad Fauzan Syahputra", "XII MIPA 1", "L", "Islam", "08129900112", "Jl. Tebet Barat No. 14", "Aktif", "", "240101", "2008-05-14", "H. Syahputra", "08129900112", "X -> XI MIPA 1 -> XII MIPA 1"),
        Student("S-02", "SCH-001", "0078129032", "Bella Amanda Putri", "XII MIPA 1", "P", "Islam", "08138877665", "Jl. Pancoran Timur III", "Aktif", "", "240102", "2008-08-21", "Siti Rahma", "08138877665", "X -> XI MIPA 1 -> XII MIPA 1"),
        Student("S-03", "SCH-001", "0078129033", "Christian Kevin Wijaya", "XII MIPA 1", "L", "Kristen", "08569911223", "Jl. Radio Dalam No. 8", "Aktif", "", "240103", "2008-02-11", "Antonius Wijaya", "08569911223", "X -> XI MIPA 1 -> XII MIPA 1"),
        Student("S-04", "SCH-001", "0078129034", "Dinda Rahmadani", "XII MIPA 1", "P", "Islam", "08213344556", "Jl. Pejaten Barat No. 22", "Pindah", "", "240104", "2008-10-03", "Bapak Rahmad", "08213344556", "X -> XI MIPA 1 -> Pindah Domisili"),
        Student("S-05", "SCH-001", "0089123451", "Fajar Hidayatullah", "XI IPS 2", "L", "Islam", "08123344556", "Jl. Ragunan No. 5", "Aktif", "", "250201", "2009-04-19", "Ibu Hidayah", "08123344556", "X -> XI IPS 2"),
        Student("S-06", "SCH-001", "0089123452", "Gisella Natasya", "XI IPS 2", "P", "Katolik", "08135566778", "Jl. Cilandak Tengah No. 3", "Aktif", "", "250202", "2009-07-08", "Hendrik Natasya", "08135566778", "X -> XI IPS 2"),
        Student("S-07", "SCH-001", "0091234561", "Hafiz Ridwan", "X Merdeka 1", "L", "Islam", "08178899001", "Jl. Fatmawati No. 89", "Aktif", "", "260301", "2010-01-25", "Ridwan Kamil", "08178899001", "Siswa Baru TA 2026/2027"),
        Student("S-08", "SCH-002", "0076543210", "Irfan Maulana", "XII TKJ 1", "L", "Islam", "08121122334", "Jl. Gatot Subroto No. 10", "Aktif", "", "240801", "2008-03-17", "Maulana Yusuf", "08121122334", "X TKJ -> XI TKJ -> XII TKJ 1"),
        Student("S-09", "SCH-002", "0076543211", "Jessica Tanuwijaya", "XII TKJ 1", "P", "Buddha", "08132233445", "Jl. Asia Afrika No. 44", "Lulus", "", "240802", "2007-12-09", "Tanuwijaya", "08132233445", "Lulus TA 2025/2026"),
        Student("S-10", "SCH-003", "0098765432", "Kresna Bayu Aji", "IX Unggulan", "L", "Hindu", "08153344556", "Jl. Gubeng Kertajaya No. 15", "Aktif", "", "250901", "2011-06-30", "Bayu Aji", "08153344556", "VII -> VIII -> IX Unggulan")
    )

    fun initialHolidays(): List<SchoolHoliday> = listOf(
        SchoolHoliday("HOL-01", "SCH-001", "2026-12-25", "Hari Raya Natal", "Libur Nasional"),
        SchoolHoliday("HOL-02", "SCH-001", "2027-01-01", "Tahun Baru Masehi 2027", "Libur Nasional"),
        SchoolHoliday("HOL-03", "SCH-001", "2026-12-21", "Libur Akhir Semester Ganjil", "Kalender Akademik")
    )

    fun initialAnnouncements(): List<SchoolAnnouncement> = listOf(
        SchoolAnnouncement(
            id = "ANN-01",
            idSekolah = "SCH-001",
            title = "Batas Akhir Unggah RPP Semester Ganjil",
            content = "Seluruh Guru pengajar dimohon mengunggah dokumen RPP (maks. 5 MB/file) melalui Dashboard Guru paling lambat akhir pekan ini.",
            targetRole = "Guru",
            authorName = "Admin Utama",
            publishedDate = "2026-11-18"
        ),
        SchoolAnnouncement(
            id = "ANN-02",
            idSekolah = "SCH-001",
            title = "Pembaruan PIN Kelas & Persiapan SAS",
            content = "Admin telah memperbarui PIN Kelas masing-masing rombel. Wali kelas harap membagikan PIN kelas hanya kepada siswa aktif di kelasnya.",
            targetRole = "Semua",
            authorName = "Admin Utama",
            publishedDate = "2026-11-17"
        )
    )

    fun initialLoginLogs(): List<LoginActivityLog> = listOf(
        LoginActivityLog(
            id = "LOG-01",
            idSekolah = "SCH-001",
            uid = "system-init",
            email = "admin@sman1majubersama.sch.id",
            displayName = "Admin Utama",
            role = "SUPERADMIN",
            timestamp = "2026-11-18 07:30",
            deviceInfo = "Android Mobile • Google Sign-In",
            status = "Sukses"
        ),
        LoginActivityLog(
            id = "LOG-02",
            idSekolah = "SCH-001",
            uid = "guru-init",
            email = "eko.prasetyo@school.id",
            displayName = "Drs. Eko Prasetyo, M.Si.",
            role = "GURU",
            timestamp = "2026-11-18 07:45",
            deviceInfo = "Android Tablet • Google Sign-In",
            status = "Sukses"
        )
    )

    fun initialClasses(): List<SchoolClass> = listOf(
        SchoolClass("C-01", "SCH-001", "XII MIPA 1", 12, "Drs. Eko Prasetyo, M.Si.", "R-301 Gedung A", 34, 36, "1234", "2026-11-10"),
        SchoolClass("C-02", "SCH-001", "XII MIPA 2", 12, "Anisa Nur Fitria, S.Si.", "R-302 Gedung A", 35, 36, "5678", "2026-11-10"),
        SchoolClass("C-03", "SCH-001", "XI IPS 2", 11, "Dewi Anggraini, S.Pd.", "R-204 Gedung B", 32, 36, "4321", "2026-11-10"),
        SchoolClass("C-04", "SCH-001", "X Merdeka 1", 10, "Budi Santoso, S.Kom.", "R-101 Gedung C", 36, 36, "9900", "2026-11-10"),
        SchoolClass("C-05", "SCH-002", "XII TKJ 1", 12, "Hendra Wijaya, S.T.", "Lab Jaringan 1", 30, 32, "7788", "2026-11-10"),
        SchoolClass("C-06", "SCH-003", "IX Unggulan", 9, "Farida Kusumastuti, S.Pd.", "R-12 SMP", 28, 30, "3344", "2026-11-10")
    )

    fun initialSubjects(): List<Subject> = listOf(
        Subject("SUB-01", "SCH-001", "MAT-PEM", "Matematika Peminatan", "Peminatan", 75, 4),
        Subject("SUB-02", "SCH-001", "ENG-01", "Bahasa Inggris Lanjut", "Umum", 75, 3),
        Subject("SUB-03", "SCH-001", "FIS-01", "Fisika Terapan", "Peminatan", 78, 4),
        Subject("SUB-04", "SCH-001", "BIO-01", "Biologi Molekuler", "Peminatan", 75, 3),
        Subject("SUB-05", "SCH-001", "INF-01", "Informatika & Coding", "Umum", 80, 2),
        Subject("SUB-06", "SCH-001", "IND-01", "Bahasa Indonesia", "Umum", 75, 3),
        Subject("SUB-07", "SCH-002", "TKJ-01", "Administrasi Server Jaringan", "Kejuruan", 78, 8),
        Subject("SUB-08", "SCH-003", "IPA-01", "IPA Terpadu", "Umum", 75, 4)
    )

    fun initialAttendance(): List<AttendanceRecord> = listOf(
        AttendanceRecord("ATT-01", "SCH-001", "2026-11-11", "XII MIPA 1", "S-01", "Ahmad Fauzan Syahputra", AttendanceStatus.HADIR),
        AttendanceRecord("ATT-02", "SCH-001", "2026-11-11", "XII MIPA 1", "S-02", "Bella Amanda Putri", AttendanceStatus.HADIR),
        AttendanceRecord("ATT-03", "SCH-001", "2026-11-11", "XII MIPA 1", "S-03", "Christian Kevin Wijaya", AttendanceStatus.SAKIT, "Surat dokter terlampir"),
        AttendanceRecord("ATT-04", "SCH-001", "2026-11-11", "XII MIPA 1", "S-04", "Dinda Rahmadani", AttendanceStatus.HADIR),
        AttendanceRecord("ATT-05", "SCH-001", "2026-11-11", "XI IPS 2", "S-05", "Fajar Hidayatullah", AttendanceStatus.IZIN, "Acara keluarga"),
        AttendanceRecord("ATT-06", "SCH-001", "2026-11-11", "XI IPS 2", "S-06", "Gisella Natasya", AttendanceStatus.HADIR),
        AttendanceRecord("ATT-07", "SCH-001", "2026-11-11", "X Merdeka 1", "S-07", "Hafiz Ridwan", AttendanceStatus.HADIR)
    )

    fun initialGrades(): List<GradeRecord> = listOf(
        GradeRecord("G-01", "SCH-001", "S-01", "Ahmad Fauzan Syahputra", "XII MIPA 1", "Matematika Peminatan", 88.0, 92.0, 90.0),
        GradeRecord("G-02", "SCH-001", "S-02", "Bella Amanda Putri", "XII MIPA 1", "Matematika Peminatan", 85.0, 80.0, 86.0),
        GradeRecord("G-03", "SCH-001", "S-03", "Christian Kevin Wijaya", "XII MIPA 1", "Matematika Peminatan", 92.0, 95.0, 94.0),
        GradeRecord("G-04", "SCH-001", "S-04", "Dinda Rahmadani", "XII MIPA 1", "Matematika Peminatan", 78.0, 72.0, 76.0),
        GradeRecord("G-05", "SCH-001", "S-01", "Ahmad Fauzan Syahputra", "XII MIPA 1", "Fisika Terapan", 90.0, 85.0, 88.0),
        GradeRecord("G-06", "SCH-001", "S-02", "Bella Amanda Putri", "XII MIPA 1", "Fisika Terapan", 80.0, 84.0, 82.0),
        GradeRecord("G-07", "SCH-001", "S-05", "Fajar Hidayatullah", "XI IPS 2", "Bahasa Inggris Lanjut", 85.0, 88.0, 84.0)
    )

    fun initialLessonPlans(): List<LessonPlan> = listOf(
        LessonPlan(
            id = "RPP-01",
            idSekolah = "SCH-001",
            title = "Kalkulus Diferensial & Integral Terapan",
            curriculum = "Kurikulum Merdeka",
            subject = "Matematika Peminatan",
            className = "XII MIPA 1",
            meetingCount = 6,
            duration = "2 x 45 Menit",
            learningObjectives = "Peserta didik mampu memodelkan masalah laju perubahan sesaat dan optimasi fungsi polinomial dalam konteks sains dan ekonomi.",
            fileName = "Modul_Ajar_Kalkulus_Merdeka_XII.pdf",
            teacherId = "T-01",
            teacherName = "Drs. Eko Prasetyo, M.Si.",
            teacherEmail = "eko.prasetyo@school.id",
            reviewNotes = "RPP sudah sangat baik dan sesuai panduan Kurikulum Merdeka Fase F. Lanjutkan KBM.",
            fileSizeBytes = 2_850_000L,
            status = RppStatus.APPROVED
        ),
        LessonPlan(
            id = "RPP-02",
            idSekolah = "SCH-001",
            title = "Termodinamika & Efisiensi Mesin Carnot",
            curriculum = "Kurikulum Merdeka",
            subject = "Fisika Terapan",
            className = "XII MIPA 1",
            meetingCount = 4,
            duration = "3 x 45 Menit",
            learningObjectives = "Menganalisis hukum ke-0, 1, dan 2 termodinamika serta menghitung efisiensi mesin pendingin dan kalor.",
            fileName = "RPP_Termodinamika_Fisika_FaseF.pdf",
            teacherId = "T-03",
            teacherName = "Rizky Firmansyah, S.Pd.",
            teacherEmail = "rizky.f@school.id",
            reviewNotes = "Mohon lengkapi rubrik penilaian proyek praktikum virtual di bagian lampiran.",
            fileSizeBytes = 3_120_000L,
            status = RppStatus.REJECTED
        ),
        LessonPlan(
            id = "RPP-03",
            idSekolah = "SCH-001",
            title = "Analytical Exposition Text & Critical Debate",
            curriculum = "Kurikulum Merdeka",
            subject = "Bahasa Inggris Lanjut",
            className = "XI IPS 2",
            meetingCount = 3,
            duration = "2 x 45 Menit",
            learningObjectives = "Constructing coherent thesis statements, arguments, and reiterations on contemporary environmental topics.",
            fileName = "LessonPlan_AnalyticalExposition_B_Inggris.docx",
            teacherId = "T-02",
            teacherName = "Dewi Anggraini, S.Pd.",
            teacherEmail = "dewi.anggraini@school.id",
            reviewNotes = "",
            fileSizeBytes = 1_450_000L,
            status = RppStatus.SUBMITTED
        ),
        LessonPlan(
            id = "RPP-04",
            idSekolah = "SCH-002",
            title = "Konfigurasi Firewall Router MikroTik & Cisco",
            curriculum = "Kurikulum Merdeka",
            subject = "Administrasi Server Jaringan",
            className = "XII TKJ 1",
            meetingCount = 8,
            duration = "4 x 45 Menit",
            learningObjectives = "Mampu mengamankan port, membuat filter rule NAT, dan menginspeksi traffic anomali.",
            fileName = "Modul_Praktek_Firewall_TKJ.pdf",
            teacherId = "T-06",
            teacherName = "Hendra Wijaya, S.T.",
            teacherEmail = "hendra.w@smkmerdeka.id",
            reviewNotes = "Disetujui. Sesuai standar industri Teaching Factory.",
            fileSizeBytes = 4_200_000L,
            status = RppStatus.APPROVED
        )
    )

    fun initialTeachingJournals(): List<TeachingJournal> = listOf(
        TeachingJournal(
            id = "JRN-01",
            idSekolah = "SCH-001",
            date = "2026-11-11",
            teacherName = "Drs. Eko Prasetyo, M.Si.",
            subject = "Matematika Peminatan",
            className = "XII MIPA 1",
            periodTime = "07:30 - 09:00 (Jam 1-2)",
            topic = "Turunan Fungsi Trigonometri Kompleks",
            activities = "Penjelasan konsep aturan rantai, pemodelan gelombang sinusoidal, dan latihan terbimbing 5 soal analisis tingkat lanjut.",
            obstacles = "3 siswa masih bingung membedakan turunan sec(x) dan tan(x).",
            reflection = "Perlu diadakan sesi drilling rumus dasar selama 10 menit di awal pertemuan berikutnya.",
            documentationFileName = "Dokumentasi_KBM_Matematika_FaseF.jpg",
            isCompleted = true
        ),
        TeachingJournal(
            id = "JRN-02",
            idSekolah = "SCH-001",
            date = "2026-11-11",
            teacherName = "Dewi Anggraini, S.Pd.",
            subject = "Bahasa Inggris Lanjut",
            className = "XI IPS 2",
            periodTime = "09:15 - 10:45 (Jam 3-4)",
            topic = "Group Peer-Review of Essay Drafts",
            activities = "Siswa bertukar draf tulisan argumentatif, memberikan masukan menggunakan rubrik CEFR B2, dan merevisi.",
            obstacles = "Waktu review terasa kurang karena diskusi antarkelompok sangat antusias.",
            reflection = "Sesi revisi akhir dialihkan ke penugasan portofolio mandiri di LMS My School.",
            documentationFileName = "Dokumentasi_Peer_Review_Essay.jpg",
            isCompleted = true
        ),
        TeachingJournal(
            id = "JRN-03",
            idSekolah = "SCH-001",
            date = "2026-11-10",
            teacherName = "Rizky Firmansyah, S.Pd.",
            subject = "Fisika Terapan",
            className = "XII MIPA 1",
            periodTime = "11:00 - 12:30 (Jam 5-6)",
            topic = "Praktikum Virtual Phet Mesin Kalor",
            activities = "Eksplorasi siklus Carnot di lab komputer, pencatatan data suhu reservoir tinggi dan rendah.",
            obstacles = "Koneksi internet lab sempat terputus selama 7 menit.",
            reflection = "Alternatif aplikasi offline Phet simulator berjalan lancar sebagai solusi cepat.",
            documentationFileName = "Dokumentasi_Praktikum_Lab_Virtual.jpg",
            isCompleted = true
        )
    )

    fun initialAssignments(): List<StudentAssignment> = listOf(
        StudentAssignment(
            id = "TSK-01",
            idSekolah = "SCH-001",
            className = "XII MIPA 1",
            subject = "Matematika Peminatan",
            title = "Latihan Uji Kompetensi Bab 3: Turunan",
            description = "Kerjakan 10 soal uraian terlampir pada file modul untuk menguji pemahaman aturan rantai dan optimasi fungsi.",
            deadline = "15 Nov 2026, 23:59 WIB",
            type = "Uji Kompetensi",
            fileName = "Paket_Soal_UjiKompetensi_Bab3.pdf"
        ),
        StudentAssignment(
            id = "TSK-02",
            idSekolah = "SCH-001",
            className = "XII MIPA 1",
            subject = "Fisika Terapan",
            title = "Tugas Mandiri: Laporan Praktikum Virtual Carnot",
            description = "Susun laporan singkat format PDF mengenai perhitungan efisiensi termal berdasarkan data praktikum simulator Phet.",
            deadline = "14 Nov 2026, 17:00 WIB",
            type = "Tugas",
            fileName = "Format_Laporan_Praktikum.docx"
        ),
        StudentAssignment(
            id = "TSK-03",
            idSekolah = "SCH-001",
            className = "XII MIPA 1",
            subject = "Matematika Peminatan",
            title = "Bahan Bacaan & Materi: Teorema Fundamental Kalkulus",
            description = "Slide presentasi dan modul suplemen pengayaan persiapan ujian semester.",
            deadline = "Materi Terbuka",
            type = "Materi",
            fileName = "Slide_Kalkulus_Integral.pdf"
        )
    )

    fun initialNotifications(): List<AppNotification> = listOf(
        AppNotification(
            id = "NOTIF-01",
            idSekolah = "SCH-001",
            recipientEmailOrRole = "eko.prasetyo@school.id",
            title = "RPP Disetujui Kepala Sekolah",
            message = "RPP 'Kalkulus Diferensial & Integral Terapan' telah di-Approve oleh Admin/Kepsek. Catatan: 'RPP sudah sangat baik dan sesuai panduan Kurikulum Merdeka Fase F.'",
            timestamp = "11 Nov, 14:20",
            isRead = false,
            type = "RPP_APPROVED"
        ),
        AppNotification(
            id = "NOTIF-02",
            idSekolah = "SCH-001",
            recipientEmailOrRole = "rizky.f@school.id",
            title = "RPP Memerlukan Revisi",
            message = "RPP 'Termodinamika & Efisiensi Mesin Carnot' ditolak sementara (Reject/Revisi). Catatan: 'Mohon lengkapi rubrik penilaian proyek praktikum virtual di bagian lampiran.'",
            timestamp = "11 Nov, 11:45",
            isRead = false,
            type = "RPP_REJECTED"
        ),
        AppNotification(
            id = "NOTIF-03",
            idSekolah = "SCH-001",
            recipientEmailOrRole = "GURU",
            title = "Pengingat Keamanan PIN Kelas",
            message = "Admin Sekolah telah memperbarui PIN untuk kelas XII MIPA 1 dan XI IPS 2 demi menjaga keamanan akses siswa.",
            timestamp = "10 Nov, 08:00",
            isRead = true,
            type = "PIN_ALERT"
        )
    )

    fun initialPtkAttendance(): List<PtkAttendanceRecord> = listOf(
        PtkAttendanceRecord("PTK-ATT-01", "SCH-001", "T-01", "Drs. Eko Prasetyo, M.Si.", "2026-11-18", PtkAttendanceStatus.HADIR, "Hadir mengajar jam ke-1"),
        PtkAttendanceRecord("PTK-ATT-02", "SCH-001", "T-02", "Dewi Anggraini, S.Pd.", "2026-11-18", PtkAttendanceStatus.HADIR, "Hadir lengkap"),
        PtkAttendanceRecord("PTK-ATT-03", "SCH-001", "T-03", "Rizky Firmansyah, S.Pd.", "2026-11-18", PtkAttendanceStatus.DINAS_LUAR, "MGMP Fisika Tingkat Provinsi"),
        PtkAttendanceRecord("PTK-ATT-04", "SCH-001", "T-04", "Anisa Nur Fitria, S.Si.", "2026-11-18", PtkAttendanceStatus.HADIR, "Non-Satmikal hadir sesuai jadwal")
    )

    fun initialTeachingSchedules(): List<TeachingSchedule> = listOf(
        TeachingSchedule("SCHD-01", "SCH-001", "Senin", "07:30", "09:00", "XII MIPA 1", "Matematika Peminatan", "T-01", "Drs. Eko Prasetyo, M.Si.", "R-301"),
        TeachingSchedule("SCHD-02", "SCH-001", "Senin", "09:15", "10:45", "XI IPS 2", "Bahasa Inggris Lanjut", "T-02", "Dewi Anggraini, S.Pd.", "R-204"),
        TeachingSchedule("SCHD-03", "SCH-001", "Selasa", "07:30", "09:00", "XII MIPA 1", "Fisika Terapan", "T-03", "Rizky Firmansyah, S.Pd.", "Lab Fisika"),
        TeachingSchedule("SCHD-04", "SCH-001", "Rabu", "10:45", "12:15", "XII MIPA 2", "Biologi Molekuler", "T-04", "Anisa Nur Fitria, S.Si.", "Lab Biologi"),
        TeachingSchedule("SCHD-05", "SCH-001", "Kamis", "07:30", "09:00", "X Merdeka 1", "Informatika & Coding", "T-05", "Budi Santoso, S.Kom.", "Lab Komputer")
    )

    fun initialForumMessages(): List<ForumMessage> = listOf(
        ForumMessage(
            id = "FRM-01",
            idSekolah = "SCH-001",
            senderUid = "admin-init",
            senderName = "Admin Utama Sekolah",
            senderRole = "SUPERADMIN",
            messageType = ForumMessageType.TEXT,
            textContent = "Selamat datang di Forum Diskusi Lembaga (Chat & Voice-Note Maks 30 dtk). Gunakan forum ini untuk koordinasi guru & admin secara hemat kuota.",
            sentDate = "2026-11-18",
            sentTime = "07:30",
            isArchived = false,
            createdAtMillis = 1700000001000L
        ),
        ForumMessage(
            id = "FRM-02",
            idSekolah = "SCH-001",
            senderUid = "guru-init",
            senderName = "Drs. Eko Prasetyo, M.Si.",
            senderRole = "GURU",
            messageType = ForumMessageType.TEXT,
            textContent = "Siap Pak Admin, jadwal kelas XII MIPA 1 minggu ini sudah sinkron dan RPP Bab Kalkulus siap dilaksanakan.",
            sentDate = "2026-11-18",
            sentTime = "07:45",
            isArchived = false,
            createdAtMillis = 1700000002000L
        )
    )
}
