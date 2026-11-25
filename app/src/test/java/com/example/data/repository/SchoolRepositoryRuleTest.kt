package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.SchoolFirestoreRepository
import com.example.model.*
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SchoolRepositoryRuleTest : FirestoreEmulatorTestBase() {

    private val timeoutMs = 10_000L

    @Test
    fun registerNewSchool_adminDiscoversSessionAndReadsProtectedClasses() = runBlocking {
        val adminUid = signInTestUser("admin_rule_${System.currentTimeMillis()}@school.id")
        val repo = SchoolFirestoreRepository(firestore, auth)
        val schoolId = "SCH-RULE-${System.currentTimeMillis() % 100000}"

        val regResult = withTimeout(timeoutMs) {
            repo.registerNewSchool(
                tenant = TenantSchool(schoolId, "SMA Uji Coba", "20109911", "SMA", "Jakarta"),
                profile = SchoolProfile(
                    idSekolah = schoolId,
                    name = "SMA Uji Coba",
                    npsn = "20109911",
                    akreditasi = "A",
                    level = "SMA",
                    address = "Jl. Uji No. 1",
                    email = "info@smauji.sch.id",
                    phone = "021555",
                    website = "https://smauji.sch.id",
                    kepalaSekolah = "Kepsek Uji",
                    vision = "Visi Uji",
                    mission = "Misi Uji"
                ),
                seedStarterData = true
            )
        }
        assertTrue("Register school failed: ${regResult.exceptionOrNull()}", regResult.isSuccess)
        val session = regResult.getOrThrow()
        assertEquals(UserRole.SUPERADMIN, session.role)
        assertEquals(adminUid, session.uid)

        // Discover session works cleanly
        val discovered = withTimeout(timeoutMs) { repo.discoverSession().getOrThrow() }
        assertNotNull(discovered)
        assertEquals(schoolId, discovered?.idSekolah)
        assertEquals(UserRole.SUPERADMIN, discovered?.role)

        // Realtime classes listener sees seeded classes with PINs
        val classes = withTimeout(timeoutMs) {
            repo.observeClasses(schoolId, UserRole.SUPERADMIN).first { it.isNotEmpty() }
        }
        assertTrue(classes.any { it.id == "C-01" && it.pinKelas == "1234" })
    }

    @Test
    fun staffWhitelistAndKepsekRppReview_enforcesRoleBoundaries() = runBlocking {
        val ts = System.currentTimeMillis()
        val adminEmail = "admin_$ts@school.id"
        val kepsekEmail = "kepsek_$ts@school.id"
        val guruEmail = "guru_$ts@school.id"
        val schoolId = "SCH-RBAC-${ts % 100000}"

        signInTestUser(adminEmail)
        val adminRepo = SchoolFirestoreRepository(firestore, auth)
        adminRepo.registerNewSchool(
            tenant = TenantSchool(schoolId, "SMA RBAC Test", "20109922", "SMA", "Bandung"),
            profile = SchoolProfile(
                schoolId, "SMA RBAC Test", "20109922", "A", "SMA", "Jl. RBAC",
                adminEmail, "022", "https://rbac.id", "Kepsek", "Visi", "Misi"
            ),
            seedStarterData = true
        ).getOrThrow()

        // Admin whitelists Kepsek & Guru
        adminRepo.saveStaffWhitelist(
            UserAccount("WL-KEP-$ts", schoolId, kepsekEmail, "Kepsek RBAC", "1975", UserRole.KEPALA_SEKOLAH, "Kepala Sekolah")
        ).getOrThrow()
        adminRepo.saveStaffWhitelist(
            UserAccount(
                id = "WL-GURU-$ts", idSekolah = schoolId, email = guruEmail,
                name = "Guru RBAC", nip = "1988", role = UserRole.GURU,
                jabatan = "Guru Matematika", idGuru = "T-RBAC",
                idKelas = listOf("C-01"), idMapel = listOf("SUB-01")
            )
        ).getOrThrow()

        // Sign in as Guru -> auto-discovers GURU role & creates RPP
        auth.signOut()
        val guruUid = signInTestUser(guruEmail)
        val guruRepo = SchoolFirestoreRepository(firestore, auth)
        val guruSession = withTimeout(timeoutMs) { guruRepo.discoverSession().getOrThrow() }
        assertEquals(UserRole.GURU, guruSession?.role)
        assertEquals("T-RBAC", guruSession?.idGuru)

        val validRpp = LessonPlan(
            id = "RPP-RBAC-1",
            idSekolah = schoolId,
            title = "Matriks & Vektor",
            curriculum = "Kurikulum Merdeka",
            subject = "Matematika Peminatan",
            className = "XII MIPA 1",
            meetingCount = 4,
            duration = "2 x 45 Menit",
            learningObjectives = "Memahami matriks",
            fileName = "matriks.pdf",
            teacherId = "T-RBAC",
            teacherName = "Guru RBAC",
            teacherEmail = guruEmail,
            userId = guruUid,
            fileSizeBytes = 2_000_000L,
            status = RppStatus.SUBMITTED
        )
        assertTrue(withTimeout(timeoutMs) { guruRepo.saveLessonPlan(validRpp).isSuccess })

        // Sign in as Kepsek -> auto-discovers KEPALA_SEKOLAH, approves RPP
        auth.signOut()
        signInTestUser(kepsekEmail)
        val kepsekRepo = SchoolFirestoreRepository(firestore, auth)
        val kepsekSession = withTimeout(timeoutMs) { kepsekRepo.discoverSession().getOrThrow() }
        assertEquals(UserRole.KEPALA_SEKOLAH, kepsekSession?.role)

        val approveRes = withTimeout(timeoutMs) {
            kepsekRepo.reviewLessonPlanByKepsek(schoolId, "RPP-RBAC-1", RppStatus.APPROVED, "Lengkap dan valid.")
        }
        assertTrue("Kepsek approve failed: ${approveRes.exceptionOrNull()}", approveRes.isSuccess)

        // Kepsek CANNOT create master subject
        val subjectRes = withTimeout(timeoutMs) {
            kepsekRepo.saveSubject(Subject("SUB-HACK", schoolId, "HACK", "Hack", "Umum", 75, 2))
        }
        assertTrue(subjectRes.isFailure)
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            subjectRes.exceptionOrNull()?.unwrapFirestoreException()?.code
        )
    }

    @Test
    fun studentPinJoin_succeedsWithValidPinAndFailsWithWrongPin() = runBlocking {
        val ts = System.currentTimeMillis()
        val adminEmail = "admin_stu_$ts@school.id"
        val studentEmail = "student_$ts@gmail.com"
        val schoolId = "SCH-STU-${ts % 100000}"

        signInTestUser(adminEmail)
        val adminRepo = SchoolFirestoreRepository(firestore, auth)
        adminRepo.registerNewSchool(
            tenant = TenantSchool(schoolId, "SMA Siswa Test", "20109933", "SMA", "Surabaya"),
            profile = SchoolProfile(
                schoolId, "SMA Siswa Test", "20109933", "A", "SMA", "Jl. Siswa",
                adminEmail, "031", "https://stu.id", "Kepsek", "Visi", "Misi"
            ),
            seedStarterData = true
        ).getOrThrow()

        auth.signOut()
        signInTestUser(studentEmail)
        val studentRepo = SchoolFirestoreRepository(firestore, auth)

        // Wrong PIN fails
        val wrongPin = withTimeout(timeoutMs) {
            studentRepo.verifyStudentPinAndJoin(schoolId, "C-01", "XII MIPA 1", "Budi Siswa", "9999")
        }
        assertTrue(wrongPin.isFailure)

        // Valid PIN "1234" succeeds
        val rightPin = withTimeout(timeoutMs) {
            studentRepo.verifyStudentPinAndJoin(schoolId, "C-01", "XII MIPA 1", "Budi Siswa", "1234")
        }
        assertTrue("Valid student PIN join failed: ${rightPin.exceptionOrNull()}", rightPin.isSuccess)
        assertEquals(UserRole.SISWA, rightPin.getOrThrow().role)
    }

    @Test
    fun stageOneAppearanceHolidaysAnnouncementsAndTeachers_enforcesAdminOnlyWrites() = runBlocking {
        val ts = System.currentTimeMillis()
        val adminEmail = "admin_s1_$ts@school.id"
        val guruEmail = "guru_s1_$ts@school.id"
        val schoolId = "SCH-S1-${ts % 100000}"

        signInTestUser(adminEmail)
        val adminRepo = SchoolFirestoreRepository(firestore, auth)
        val baseProfile = SchoolProfile(
            idSekolah = schoolId,
            name = "SMA Stage 1",
            npsn = "20109944",
            akreditasi = "A",
            level = "SMA",
            address = "Jl. Merdeka",
            email = adminEmail,
            phone = "021444",
            website = "https://s1.sch.id",
            kepalaSekolah = "Kepsek S1",
            vision = "Visi S1",
            mission = "Misi S1"
        )
        val tenant = TenantSchool(schoolId, "SMA Stage 1", "20109944", "SMA", "Jakarta")
        adminRepo.registerNewSchool(tenant, baseProfile, seedStarterData = true).getOrThrow()

        // Admin updates appearance preset & logo metadata
        val updatedProfile = baseProfile.copy(
            name = "SMA Stage 1 Baru",
            themePreset = ThemePreset.INDIGO_SNEAT,
            logoFileName = "logo_s1.png",
            logoSizeBytes = 128_000L
        )
        val profileUpdateRes = withTimeout(timeoutMs) {
            adminRepo.updateSchoolProfileAndTenant(tenant.copy(name = updatedProfile.name), updatedProfile)
        }
        assertTrue("Appearance update failed: ${profileUpdateRes.exceptionOrNull()}", profileUpdateRes.isSuccess)

        // Admin saves PNS teacher + Holiday + Announcement
        val teacherRes = withTimeout(timeoutMs) {
            adminRepo.saveTeacher(
                Teacher(
                    id = "T-S1",
                    idSekolah = schoolId,
                    nip = "198701012015011001",
                    name = "Guru Stage Satu",
                    email = guruEmail,
                    phone = "08123456789",
                    subject = "Biologi",
                    employmentStatus = "PNS",
                    rankGroup = "III/c",
                    skDocumentName = "sk.pdf",
                    skDocumentSizeBytes = 250_000L,
                    isActiveAccount = true
                )
            )
        }
        assertTrue("Save teacher failed: ${teacherRes.exceptionOrNull()}", teacherRes.isSuccess)

        assertTrue(
            withTimeout(timeoutMs) {
                adminRepo.saveHoliday(SchoolHoliday("HOL-S1", schoolId, "2026-12-25", "Libur Natal", "Libur Nasional")).isSuccess
            }
        )
        assertTrue(
            withTimeout(timeoutMs) {
                adminRepo.saveAnnouncement(
                    SchoolAnnouncement("ANN-S1", schoolId, "Info RPP", "Harap unggah RPP.", "Guru", "Admin", "2026-11-18")
                ).isSuccess
            }
        )

        // Guru signs in, discovers role via auto-synced whitelist, can read holidays & announcements, CANNOT mutate
        auth.signOut()
        signInTestUser(guruEmail)
        val guruRepo = SchoolFirestoreRepository(firestore, auth)
        val guruSession = withTimeout(timeoutMs) { guruRepo.discoverSession().getOrThrow() }
        assertEquals(UserRole.GURU, guruSession?.role)

        val holidays = withTimeout(timeoutMs) { guruRepo.observeHolidays(schoolId).first { it.isNotEmpty() } }
        assertTrue(holidays.any { it.id == "HOL-S1" })

        val guruHolidayWrite = withTimeout(timeoutMs) {
            guruRepo.saveHoliday(SchoolHoliday("HOL-DENY", schoolId, "2026-12-30", "Libur Ilegal", "Libur Nasional"))
        }
        assertTrue(guruHolidayWrite.isFailure)
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            guruHolidayWrite.exceptionOrNull()?.unwrapFirestoreException()?.code
        )
    }

    @Test
    fun ptkSchedulesAndForumDiskusi_enforcesAdminAndGuruAccessWhileBlockingSiswa() = runBlocking {
        val ts = System.currentTimeMillis()
        val adminEmail = "admin_frm_$ts@school.id"
        val guruEmail = "guru_frm_$ts@school.id"
        val studentEmail = "siswa_frm_$ts@gmail.com"
        val schoolId = "SCH-FRM-${ts % 100000}"

        val adminUid = signInTestUser(adminEmail)
        val adminRepo = SchoolFirestoreRepository(firestore, auth)
        adminRepo.registerNewSchool(
            tenant = TenantSchool(schoolId, "SMA Forum Test", "20109955", "SMA", "Semarang"),
            profile = SchoolProfile(
                schoolId, "SMA Forum Test", "20109955", "A", "SMA", "Jl. Forum",
                adminEmail, "024", "https://forum.id", "Kepsek", "Visi", "Misi"
            ),
            seedStarterData = true
        ).getOrThrow()

        adminRepo.saveStaffWhitelist(
            UserAccount(
                id = "WL-G-FRM-$ts",
                idSekolah = schoolId,
                email = guruEmail,
                name = "Guru Forum",
                nip = "1990",
                role = UserRole.GURU,
                jabatan = "Guru IPA",
                idGuru = "T-FRM",
                idKelas = listOf("XII MIPA 1"),
                idMapel = listOf("Fisika")
            )
        ).getOrThrow()

        assertTrue(
            withTimeout(timeoutMs) {
                adminRepo.savePtkAttendance(
                    PtkAttendanceRecord("PTK-R1", schoolId, "T-FRM", "Guru Forum", "2026-11-18", PtkAttendanceStatus.HADIR, "Tepat waktu", adminUid)
                ).isSuccess
            }
        )
        assertTrue(
            withTimeout(timeoutMs) {
                adminRepo.saveTeachingSchedule(
                    TeachingSchedule("SCHD-R1", schoolId, "Senin", "07:30", "09:00", "XII MIPA 1", "Fisika", "T-FRM", "Guru Forum", "Lab IPA")
                ).isSuccess
            }
        )

        // Guru can send Forum Text & Voice-Note, but CANNOT delete message
        auth.signOut()
        val guruUid = signInTestUser(guruEmail)
        val guruRepo = SchoolFirestoreRepository(firestore, auth)
        assertEquals(UserRole.GURU, withTimeout(timeoutMs) { guruRepo.discoverSession().getOrThrow() }?.role)

        val guruMsg = ForumMessage(
            id = "FRM-R1",
            idSekolah = schoolId,
            senderUid = guruUid,
            senderName = "Guru Forum",
            senderRole = "GURU",
            messageType = ForumMessageType.VOICE_NOTE,
            textContent = "Voice-Note (15 dtk)",
            voiceBase64 = "QUFBQQ==",
            voiceDurationSec = 15,
            sentDate = "2026-11-18",
            sentTime = "09:10",
            isArchived = false
        )
        val sendRes = withTimeout(timeoutMs) { guruRepo.sendForumMessage(guruMsg) }
        assertTrue("Guru sendForumMessage failed: ${sendRes.exceptionOrNull()}", sendRes.isSuccess)

        val msgs = withTimeout(timeoutMs) { guruRepo.observeForumMessages(schoolId, 25L).first { it.any { m -> m.id == "FRM-R1" } } }
        assertTrue(msgs.any { it.id == "FRM-R1" && it.voiceDurationSec == 15 })

        val guruDeleteDenied = withTimeout(timeoutMs) { guruRepo.deleteForumMessage(schoolId, "FRM-R1") }
        assertTrue(guruDeleteDenied.isFailure)
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            guruDeleteDenied.exceptionOrNull()?.unwrapFirestoreException()?.code
        )

        // Siswa CANNOT write to forum_messages
        auth.signOut()
        val stuUid = signInTestUser(studentEmail)
        val stuRepo = SchoolFirestoreRepository(firestore, auth)
        stuRepo.verifyStudentPinAndJoin(schoolId, "C-01", "XII MIPA 1", "Siswa Test", "1234").getOrThrow()

        val siswaWriteDenied = withTimeout(timeoutMs) {
            stuRepo.sendForumMessage(
                ForumMessage(
                    id = "FRM-SISWA-HACK",
                    idSekolah = schoolId,
                    senderUid = stuUid,
                    senderName = "Siswa Test",
                    senderRole = "SISWA",
                    messageType = ForumMessageType.TEXT,
                    textContent = "Halo",
                    sentDate = "2026-11-18",
                    sentTime = "09:15"
                )
            )
        }
        assertTrue(siswaWriteDenied.isFailure)
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            siswaWriteDenied.exceptionOrNull()?.unwrapFirestoreException()?.code
        )

        // Admin can delete forum message
        auth.signOut()
        signInTestUser(adminEmail)
        val adminRepo2 = SchoolFirestoreRepository(firestore, auth)
        assertTrue(withTimeout(timeoutMs) { adminRepo2.deleteForumMessage(schoolId, "FRM-R1").isSuccess })
    }
}
