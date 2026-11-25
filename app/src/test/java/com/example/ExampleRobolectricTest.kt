package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.Teacher
import com.example.model.UserRole
import com.example.viewmodel.SchoolViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("My School", appName)
    }

    @Test
    fun `test multi tenant and role switching`() {
        val viewModel = SchoolViewModel()
        assertEquals("SCH-001", viewModel.currentTenant.value.idSekolah)
        assertEquals(UserRole.SUPERADMIN, viewModel.currentRole.value)

        viewModel.switchTenant("SCH-002")
        assertEquals("SCH-002", viewModel.currentTenant.value.idSekolah)

        viewModel.switchRole(UserRole.GURU)
        assertEquals(UserRole.GURU, viewModel.currentRole.value)
    }

    @Test
    fun `test pro toggle functionality`() {
        val viewModel = SchoolViewModel()
        val initialPro = viewModel.proSubscription.value.isPro
        assertEquals(false, initialPro)

        viewModel.togglePro()
        assertEquals(true, viewModel.proSubscription.value.isPro)
        assertEquals(51200, viewModel.proSubscription.value.storageMaxMb)
    }

    @Test
    fun `test teacher crud operations`() {
        val viewModel = SchoolViewModel()
        val newTeacher = Teacher(
            id = "T-TEST",
            idSekolah = "SCH-001",
            nip = "199001012020011001",
            name = "Guru Uji Coba, S.Pd.",
            email = "uji.coba@school.id",
            phone = "08123456789",
            subject = "Kimia",
            employmentStatus = "PNS"
        )

        var successCalled = false
        viewModel.saveTeacher(newTeacher) {
            successCalled = true
        }
        assertNotNull(newTeacher)
    }

    @Test
    fun `test add and edit tenant operations`() {
        val viewModel = SchoolViewModel()
        val initialCount = viewModel.tenants.value.size

        // Tambah lembaga kustom baru
        val newTenant = com.example.model.TenantSchool(
            idSekolah = "SCH-KUSTOM",
            name = "SMA IT Bina Insani Mandiri",
            npsn = "20109988",
            level = "SMA",
            city = "Yogyakarta"
        )

        var addCalled = false
        viewModel.saveTenant(newTenant, "A (Unggul)", "Drs. H. Mulyono, M.Pd.", "Jl. Kaliurang Km 5") {
            addCalled = true
        }
        assertEquals(initialCount + 1, viewModel.tenants.value.size)
        assertEquals("SCH-KUSTOM", viewModel.currentTenant.value.idSekolah)
        assertEquals("SMA IT Bina Insani Mandiri", viewModel.currentTenant.value.name)

        // Edit nama lembaga yang ada
        val editedTenant = newTenant.copy(name = "SMA Unggulan Bintang Prestasi Global")
        viewModel.saveTenant(editedTenant, "A (Unggul)", "Drs. H. Mulyono, M.Pd.", "Jl. Kaliurang Km 5") {}
        assertEquals("SMA Unggulan Bintang Prestasi Global", viewModel.currentTenant.value.name)
    }

    @Test
    fun `test google sign in with firestore whitelist`() {
        val viewModel = SchoolViewModel()

        var loginSuccess = false
        var errorMsg: String? = null

        // 1. Akun terdaftar di Firestore whitelist (Guru Eko Prasetyo)
        viewModel.loginWithGoogle(
            email = "eko.prasetyo@school.id",
            onSuccess = { loginSuccess = true },
            onError = { errorMsg = it }
        )
        assertTrue(loginSuccess)
        assertEquals(UserRole.GURU, viewModel.currentRole.value)

        // 2. Akun tidak terdaftar di whitelist (Ditolak)
        var unauthFailed = false
        viewModel.loginWithGoogle(
            email = "stranger@gmail.com",
            onSuccess = { },
            onError = { unauthFailed = true }
        )
        assertTrue(unauthFailed)
    }

    @Test
    fun `test student pin login validation`() {
        val viewModel = SchoolViewModel()

        var loginSuccess = false
        var loginError: String? = null

        // Login dengan PIN benar ("1234")
        viewModel.loginStudentWithPin(
            studentName = "Ahmad Fauzan Syahputra",
            className = "XII MIPA 1",
            pin = "1234",
            onSuccess = { loginSuccess = true },
            onError = { loginError = it }
        )
        assertTrue(loginSuccess)
        assertEquals(UserRole.SISWA, viewModel.currentRole.value)
        assertEquals("Ahmad Fauzan Syahputra", viewModel.currentStudentSession.value?.name)

        // Login dengan PIN salah
        var wrongPinFailed = false
        viewModel.loginStudentWithPin(
            studentName = "Ahmad Fauzan Syahputra",
            className = "XII MIPA 1",
            pin = "9999",
            onSuccess = { },
            onError = { wrongPinFailed = true }
        )
        assertTrue(wrongPinFailed)
    }

    @Test
    fun `test rpp review approval and real time notification`() {
        val viewModel = SchoolViewModel()
        val initialNotifsCount = viewModel.allNotifications.size

        // Kepsek approves RPP-03 with review notes
        viewModel.approveRppByKepsek("RPP-03", "RPP disetujui sesuai Kurikulum Merdeka.")

        val rpp3 = viewModel.getLessonPlan("RPP-03")
        assertNotNull(rpp3)
        assertEquals(com.example.model.RppStatus.APPROVED, rpp3?.status)
        assertEquals("RPP disetujui sesuai Kurikulum Merdeka.", rpp3?.reviewNotes)

        // Notifikasi bertambah
        assertEquals(initialNotifsCount + 1, viewModel.allNotifications.size)
        val latestNotif = viewModel.allNotifications.first()
        assertEquals("RPP Disetujui Kepala Sekolah", latestNotif.title)
    }

    @Test
    fun `test 5mb file size validation limit`() {
        val viewModel = SchoolViewModel()

        // File 4 MB (Valid)
        val validSize = 4 * 1024 * 1024L
        assertTrue(viewModel.validateUploadFileSize(validSize))

        // File 6 MB (Melebihi batas maksimal 5 MB -> Ditolak)
        val invalidSize = 6 * 1024 * 1024L
        val isValid = viewModel.validateUploadFileSize(invalidSize)
        assertEquals(false, isValid)
    }

    @Test
    fun `test appearance settings update 5mb limit and role guard`() {
        val viewModel = SchoolViewModel()

        // 1. Logo > 5MB ditolak
        var updatedCalled = false
        viewModel.updateAppearanceSettings(
            schoolName = "SMA Teladan Bangsa",
            npsn = "20108745",
            address = "Jl. Pendidikan No. 45",
            phone = "0217892341",
            vision = "Unggul",
            mission = "Berkualitas",
            organizationStructure = "Kepsek: Budi",
            logoBase64 = "abc",
            logoFileName = "big_logo.png",
            logoSizeBytes = 6 * 1024 * 1024L,
            themePreset = com.example.model.ThemePreset.INDIGO_SNEAT
        ) {
            updatedCalled = true
        }
        assertEquals(false, updatedCalled)

        // 2. Role non-admin ditolak
        viewModel.switchRole(UserRole.GURU)
        viewModel.updateAppearanceSettings(
            schoolName = "SMA Dibajak Guru",
            npsn = "20108745",
            address = "Jl. Pendidikan No. 45",
            phone = "0217892341",
            vision = "Unggul",
            mission = "Berkualitas",
            organizationStructure = "Kepsek: Budi",
            logoBase64 = "",
            logoFileName = "",
            logoSizeBytes = 0L,
            themePreset = com.example.model.ThemePreset.INDIGO_SNEAT
        ) {
            updatedCalled = true
        }
        assertEquals(false, updatedCalled)

        // 3. Admin dengan ukuran valid berhasil menyimpan & sinkron nama + tema
        viewModel.switchRole(UserRole.SUPERADMIN)
        viewModel.updateAppearanceSettings(
            schoolName = "SMA Teladan Nusantara",
            npsn = "20108745",
            address = "Jl. Pendidikan No. 45",
            phone = "0217892341",
            vision = "Unggul dan Berkarakter",
            mission = "Pembelajaran Modern",
            organizationStructure = "Kepsek: Dra. Nuraini",
            logoBase64 = "validBase64",
            logoFileName = "logo.png",
            logoSizeBytes = 512_000L,
            themePreset = com.example.model.ThemePreset.INDIGO_SNEAT
        ) {
            updatedCalled = true
        }
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        assertTrue(updatedCalled)
        assertEquals("SMA Teladan Nusantara", viewModel.currentSchoolProfile.value.name)
        assertEquals("SMA Teladan Nusantara", viewModel.currentTenant.value.name)
        assertEquals(com.example.model.ThemePreset.INDIGO_SNEAT, viewModel.currentSchoolProfile.value.themePreset)
    }

    @Test
    fun `test master academic config holiday and announcement crud`() {
        val viewModel = SchoolViewModel()
        var saved = false
        viewModel.updateMasterAcademicConfig("2027/2028", "Genap") { saved = true }
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        assertTrue(saved)
        assertEquals("2027/2028", viewModel.currentSchoolProfile.value.academicYear)
        assertEquals("Genap", viewModel.currentSchoolProfile.value.semester)

        val holiday = com.example.model.SchoolHoliday(
            id = "HOL-UNIT-1",
            idSekolah = "SCH-001",
            date = "2026-12-25",
            name = "Hari Raya Natal",
            category = "Libur Nasional"
        )
        var holSaved = false
        viewModel.saveHoliday(holiday) { holSaved = true }
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        assertTrue(holSaved)
        assertTrue(viewModel.activeHolidays.value.any { it.id == "HOL-UNIT-1" })
        viewModel.deleteHoliday("HOL-UNIT-1")
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        assertEquals(false, viewModel.activeHolidays.value.any { it.id == "HOL-UNIT-1" })

        val ann = com.example.model.SchoolAnnouncement(
            id = "ANN-UNIT-1",
            idSekolah = "SCH-001",
            title = "Jadwal Penilaian Akhir",
            content = "Dimulai minggu depan.",
            targetRole = "Guru"
        )
        var annSaved = false
        viewModel.saveAnnouncement(ann) { annSaved = true }
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        assertTrue(annSaved)
        assertTrue(viewModel.activeAnnouncements.value.any { it.id == "ANN-UNIT-1" })
        viewModel.deleteAnnouncement("ANN-UNIT-1")
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        assertEquals(false, viewModel.activeAnnouncements.value.any { it.id == "ANN-UNIT-1" })
    }

    @Test
    fun `test teacher pns rank clearing and staff whitelist sync`() {
        val viewModel = SchoolViewModel()
        val p3kTeacher = Teacher(
            id = "T-P3K-UNIT",
            idSekolah = "SCH-001",
            nip = "199201012023212001",
            name = "Siti Rahma, S.Pd.",
            email = "SITI.RAHMA@SCHOOL.ID",
            phone = "08120001112",
            subject = "Bahasa Indonesia",
            employmentStatus = "P3K",
            rankGroup = "III/b", // Harus dibersihkan karena bukan PNS
            isActiveAccount = true
        )
        var saved = false
        viewModel.saveTeacher(p3kTeacher) { saved = true }
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        assertTrue(saved)

        val stored = viewModel.activeTeachers.value.first { it.id == "T-P3K-UNIT" }
        assertEquals("", stored.rankGroup)
        assertEquals("siti.rahma@school.id", stored.email)
        assertTrue(viewModel.userAccounts.value.any { it.id == "WL-T-P3K-UNIT" && it.email == "siti.rahma@school.id" })
    }

    @Test
    fun `test ptk extra duty non satmikal transfer mutasi and attendance`() {
        val viewModel = SchoolViewModel()
        org.robolectric.shadows.ShadowLooper.idleMainLooper()

        val targetId = viewModel.activeTeachers.value.first().id
        viewModel.updatePtkExtraDuty(targetId, "Kepala Lab Komputer")
        viewModel.verifyNonSatmikal(targetId, isNonSatmikal = true, verified = true, satmikalOrigin = "SMAN 2 Bandung")
        viewModel.confirmGuruTransfer(targetId, true)
        viewModel.updateTeacherMutation(targetId, "DIAJUKAN", "SMAN 5 Surabaya")
        org.robolectric.shadows.ShadowLooper.idleMainLooper()

        val updated = viewModel.activeTeachers.value.first { it.id == targetId }
        assertEquals("Kepala Lab Komputer", updated.additionalDuty)
        assertTrue(updated.isNonSatmikal)
        assertTrue(updated.nonSatmikalVerified)
        assertEquals("SMAN 2 Bandung", updated.satmikalOrigin)
        assertTrue(updated.transferConfirmed)
        assertEquals("DIAJUKAN", updated.mutationStatus)
        assertEquals("SMAN 5 Surabaya", updated.mutationTargetSchool)

        val att = com.example.model.PtkAttendanceRecord(
            id = "PTK-ATT-UNIT",
            idSekolah = "SCH-001",
            teacherId = targetId,
            teacherName = updated.name,
            date = "2026-11-18",
            status = com.example.model.PtkAttendanceStatus.DINAS_LUAR,
            notes = "Rapat MGMP"
        )
        var attSaved = false
        viewModel.savePtkAttendance(att) { attSaved = true }
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        assertTrue(attSaved)
        assertTrue(viewModel.activePtkAttendance.value.any { it.id == "PTK-ATT-UNIT" })

        viewModel.deletePtkAttendance("PTK-ATT-UNIT")
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        assertEquals(false, viewModel.activePtkAttendance.value.any { it.id == "PTK-ATT-UNIT" })
    }

    @Test
    fun `test teaching schedule crud and guru filtering`() {
        val viewModel = SchoolViewModel()
        org.robolectric.shadows.ShadowLooper.idleMainLooper()

        val schedule = com.example.model.TeachingSchedule(
            id = "SCHD-UNIT-1",
            idSekolah = "SCH-001",
            dayOfWeek = "Rabu",
            startTime = "09:00",
            endTime = "10:30",
            className = "XII MIPA 1",
            subjectName = "Matematika Peminatan",
            teacherId = "T-01",
            teacherName = "Drs. Eko Prasetyo, M.Si.",
            room = "Lab Math"
        )
        var saved = false
        viewModel.saveTeachingSchedule(schedule) { saved = true }
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        assertTrue(saved)
        assertTrue(viewModel.activeTeachingSchedules.value.any { it.id == "SCHD-UNIT-1" })

        viewModel.deleteTeachingSchedule("SCHD-UNIT-1")
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        assertEquals(false, viewModel.activeTeachingSchedules.value.any { it.id == "SCHD-UNIT-1" })
    }

    @Test
    fun `test forum diskusi text voice 30s limit role guard and admin quota controls`() {
        val viewModel = SchoolViewModel()
        org.robolectric.shadows.ShadowLooper.idleMainLooper()

        val initialSize = viewModel.forumMessages.value.size

        // 1. Admin sends text message -> succeeds + emits notification
        var sentText = false
        viewModel.sendForumTextMessage("Rapat kurikulum pukul 13.00 WIB.") { sentText = true }
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        assertTrue(sentText)
        assertEquals(initialSize + 1, viewModel.forumMessages.value.size)

        // 2. Voice-Note > 30 detik ditolak
        var longVoiceSent = false
        viewModel.sendForumVoiceMessage("AAAA", 45) { longVoiceSent = true }
        assertEquals(false, longVoiceSent)

        // 3. Guru sends valid <=30s Voice-Note -> succeeds
        viewModel.switchRole(UserRole.GURU)
        var validVoiceSent = false
        viewModel.sendForumVoiceMessage("QUFBQQ==", 18) { validVoiceSent = true }
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        assertTrue(validVoiceSent)

        // 4. Siswa diblokir dari Forum Diskusi
        viewModel.switchRole(UserRole.SISWA)
        var siswaSent = false
        viewModel.sendForumTextMessage("Siswa mencoba chat") { siswaSent = true }
        assertEquals(false, siswaSent)

        // 5. Lazy load +25 & Admin archive/delete
        val latestMsgId = viewModel.forumMessages.value.last().id
        viewModel.switchRole(UserRole.SUPERADMIN)
        val beforeLimit = viewModel.forumMessageLimit.value
        viewModel.loadOlderForumMessages()
        assertEquals(beforeLimit + 25L, viewModel.forumMessageLimit.value)

        viewModel.archiveForumMessage(latestMsgId, true)
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        assertTrue(viewModel.forumMessages.value.first { it.id == latestMsgId }.isArchived)

        viewModel.deleteForumMessage(latestMsgId)
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        assertEquals(false, viewModel.forumMessages.value.any { it.id == latestMsgId })
    }
}
