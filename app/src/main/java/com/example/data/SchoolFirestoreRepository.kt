package com.example.data

import com.example.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SchoolDirectoryEntry(
    val idSekolah: String,
    val name: String,
    val level: String,
    val city: String,
    val npsn: String
)

data class PublicClassEntry(
    val id: String,
    val idSekolah: String,
    val name: String,
    val gradeLevel: Int,
    val homeroomTeacher: String,
    val room: String,
    val studentCount: Int,
    val capacity: Int
)

class SchoolFirestoreRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val schoolsCol = firestore.collection("schools")
    private val usersCol = firestore.collection("users")
    private val directoryCol = firestore.collection("school_directory")

    private fun currentUid(): String? = auth.currentUser?.uid
    private fun currentEmail(): String = (auth.currentUser?.email ?: "").trim().lowercase()

    // =========================================================================
    // AUTH / DISCOVERY / ONBOARDING
    // =========================================================================

    /**
     * Resolves an authenticated user's verified session in Firestore:
     * 1. Checks /users/{uid} for last activeSchoolId and verifies /schools/{schoolId}/members/{uid}
     * 2. Searches schools owned by uid
     * 3. Searches staff_whitelist across schools via collectionGroup for matching verified email
     *    and auto-creates /schools/{schoolId}/members/{uid} if missing.
     */
    suspend fun discoverSession(): Result<AuthenticatedSession?> {
        val user = auth.currentUser ?: return Result.success(null)
        val uid = user.uid
        val email = currentEmail()

        return runCatching {
            val userDoc = usersCol.document(uid).get().await()
            val preferredSchoolId = userDoc.getString("activeSchoolId").orEmpty()

            // Try preferred school membership first
            if (preferredSchoolId.isNotBlank()) {
                val memberDoc = schoolsCol.document(preferredSchoolId)
                    .collection("members").document(uid).get().await()
                if (memberDoc.exists()) {
                    val s = memberDoc.toAuthenticatedSession()
                    if (s != null) runCatching { recordLoginActivity(s) }
                    return@runCatching s
                }
            }

            // Try staff_whitelist collectionGroup lookup by email
            if (email.isNotBlank()) {
                val wlSnapshot = firestore.collectionGroup("staff_whitelist")
                    .whereEqualTo("email", email)
                    .get()
                    .await()
                val wlDoc = wlSnapshot.documents.firstOrNull()
                if (wlDoc != null) {
                    val schoolId = wlDoc.getString("idSekolah").orEmpty()
                    val roleStr = wlDoc.getString("role") ?: "GURU"
                    val idGuru = wlDoc.getString("idGuru").orEmpty()
                    val name = wlDoc.getString("name").orEmpty().ifBlank { user.displayName ?: email }
                    val idKelas = (wlDoc.get("idKelas") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                    val idMapel = (wlDoc.get("idMapel") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

                    if (schoolId.isNotBlank()) {
                        val memberRef = schoolsCol.document(schoolId).collection("members").document(uid)
                        val existingMember = memberRef.get().await()
                        if (!existingMember.exists()) {
                            val memberMap = hashMapOf(
                                "uid" to uid,
                                "idSekolah" to schoolId,
                                "email" to email,
                                "name" to name,
                                "role" to roleStr,
                                "idGuru" to idGuru,
                                "idKelas" to idKelas,
                                "idMapel" to idMapel,
                                "whitelistId" to wlDoc.id,
                                "studentId" to "",
                                "className" to "",
                                "createdAt" to FieldValue.serverTimestamp(),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                            memberRef.set(memberMap).await()
                        }
                        upsertUserProfile(uid, email, name, schoolId)
                        val refreshed = memberRef.get().await()
                        if (refreshed.exists()) {
                            val s = refreshed.toAuthenticatedSession()
                            if (s != null) runCatching { recordLoginActivity(s, "Android App • Whitelist Sign-In") }
                            return@runCatching s
                        }
                    }
                }
            }

            // Try schools owned by this user
            val ownedSchools = schoolsCol.whereEqualTo("ownerUid", uid).get().await()
            val ownedDoc = ownedSchools.documents.firstOrNull()
            if (ownedDoc != null) {
                val schoolId = ownedDoc.id
                val memberRef = schoolsCol.document(schoolId).collection("members").document(uid)
                val memberDoc = memberRef.get().await()
                if (memberDoc.exists()) {
                    upsertUserProfile(uid, email, user.displayName ?: "Admin Utama", schoolId)
                    val s = memberDoc.toAuthenticatedSession()
                    if (s != null) runCatching { recordLoginActivity(s) }
                    return@runCatching s
                }
                val displayName = user.displayName ?: "Admin Utama"
                val memberMap = hashMapOf(
                    "uid" to uid,
                    "idSekolah" to schoolId,
                    "email" to email,
                    "name" to displayName,
                    "role" to UserRole.SUPERADMIN.name,
                    "idGuru" to "",
                    "idKelas" to emptyList<String>(),
                    "idMapel" to emptyList<String>(),
                    "whitelistId" to "WL-OWNER",
                    "studentId" to "",
                    "className" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                memberRef.set(memberMap).await()
                upsertUserProfile(uid, email, displayName, schoolId)
                val s = memberRef.get().await().toAuthenticatedSession()
                if (s != null) runCatching { recordLoginActivity(s) }
                return@runCatching s
            }

            null
        }.onFailure { e ->
            handleFirestoreError(e, OperationType.GET, "users/$uid", auth)
        }
    }

    private suspend fun upsertUserProfile(uid: String, email: String, displayName: String, activeSchoolId: String) {
        val ref = usersCol.document(uid)
        val snap = ref.get().await()
        val safeName = displayName.ifBlank { "Pengguna My School" }
        if (snap.exists()) {
            ref.update(
                mapOf(
                    "email" to email,
                    "displayName" to safeName,
                    "activeSchoolId" to activeSchoolId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            ref.set(
                mapOf(
                    "uid" to uid,
                    "email" to email,
                    "displayName" to safeName,
                    "activeSchoolId" to activeSchoolId,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    /**
     * Registers a brand new school institution owned by the current authenticated user,
     * creates their SUPERADMIN whitelist & member record, and optionally seeds initial data.
     */
    suspend fun registerNewSchool(
        tenant: TenantSchool,
        profile: SchoolProfile,
        seedStarterData: Boolean = true
    ): Result<AuthenticatedSession> {
        val user = auth.currentUser
            ?: return Result.failure(IllegalStateException("Pengguna belum login Google."))
        val uid = user.uid
        val email = currentEmail().ifBlank { profile.email.lowercase() }
        val displayName = user.displayName?.takeIf { it.isNotBlank() } ?: "Admin Utama ${tenant.name}"
        val schoolId = tenant.idSekolah

        return runCatching {
            val batch = firestore.batch()
            val schoolRef = schoolsCol.document(schoolId)
            batch.set(
                schoolRef,
                mapOf(
                    "idSekolah" to schoolId,
                    "name" to tenant.name,
                    "npsn" to tenant.npsn.ifBlank { "20100000" },
                    "akreditasi" to profile.akreditasi.ifBlank { "A (Unggul)" },
                    "level" to tenant.level.ifBlank { "SMA" },
                    "city" to tenant.city.ifBlank { "Indonesia" },
                    "address" to profile.address.ifBlank { "Jl. Pendidikan No. 1" },
                    "email" to profile.email.ifBlank { email },
                    "phone" to profile.phone.ifBlank { "(021) 1234567" },
                    "website" to profile.website.ifBlank { "https://myschool.sch.id" },
                    "kepalaSekolah" to profile.kepalaSekolah.ifBlank { "Kepala Sekolah" },
                    "vision" to profile.vision.ifBlank { "Mewujudkan lembaga pendidikan unggul dan berkarakter." },
                    "mission" to profile.mission.ifBlank { "1. Pembelajaran bermutu.\n2. Literasi digital." },
                    "logoBase64" to profile.logoBase64,
                    "logoFileName" to profile.logoFileName,
                    "logoSizeBytes" to profile.logoSizeBytes.coerceIn(0L, 5_242_880L),
                    "themePreset" to profile.themePreset.name,
                    "organizationStructure" to profile.organizationStructure,
                    "academicYear" to profile.academicYear.ifBlank { "2026/2027" },
                    "semester" to profile.semester.ifBlank { "Ganjil" },
                    "ownerUid" to uid,
                    "isPro" to false,
                    "storageUsedMb" to 180,
                    "storageMaxMb" to 500,
                    "activeAdNetwork" to AdNetwork.ADMOB.name,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            batch.set(
                directoryCol.document(schoolId),
                mapOf(
                    "idSekolah" to schoolId,
                    "name" to tenant.name,
                    "level" to tenant.level.ifBlank { "SMA" },
                    "city" to tenant.city.ifBlank { "Indonesia" },
                    "npsn" to tenant.npsn.ifBlank { "20100000" },
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            val whitelistId = "WL-ADMIN-${uid.take(8)}"
            batch.set(
                schoolRef.collection("staff_whitelist").document(whitelistId),
                mapOf(
                    "id" to whitelistId,
                    "idSekolah" to schoolId,
                    "email" to email,
                    "name" to displayName,
                    "nip" to "198001012000011001",
                    "role" to UserRole.SUPERADMIN.name,
                    "jabatan" to "Admin Utama Sistem Sekolah",
                    "idGuru" to "",
                    "idKelas" to emptyList<String>(),
                    "idMapel" to emptyList<String>(),
                    "isVerified" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            batch.set(
                schoolRef.collection("members").document(uid),
                mapOf(
                    "uid" to uid,
                    "idSekolah" to schoolId,
                    "email" to email,
                    "name" to displayName,
                    "role" to UserRole.SUPERADMIN.name,
                    "idGuru" to "",
                    "idKelas" to emptyList<String>(),
                    "idMapel" to emptyList<String>(),
                    "whitelistId" to whitelistId,
                    "studentId" to "",
                    "className" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            batch.commit().await()

            if (seedStarterData) {
                seedInitialDataIntoBatches(schoolId, uid, tenant.name)
            }

            upsertUserProfile(uid, email, displayName, schoolId)

            val session = AuthenticatedSession(
                uid = uid,
                email = email,
                displayName = displayName,
                idSekolah = schoolId,
                role = UserRole.SUPERADMIN
            )
            runCatching { recordLoginActivity(session, "Android App • Registrasi Admin") }
            session
        }.onFailure { e ->
            handleFirestoreError(e, OperationType.CREATE, "schools/$schoolId", auth)
        }
    }

    private suspend fun seedInitialDataIntoBatches(
        schoolId: String,
        ownerUid: String,
        schoolName: String
    ) {
        val schoolRef = schoolsCol.document(schoolId)

        // Chunk 1: Classes, PINs, Teachers & Staff Whitelist
        val batch1 = firestore.batch()
        val starterClasses = listOf(
            SchoolClass("C-01", schoolId, "XII MIPA 1", 12, "Drs. Eko Prasetyo, M.Si.", "R-301", 34, 36, "1234", "2026-11-12"),
            SchoolClass("C-02", schoolId, "XI IPS 2", 11, "Dewi Anggraini, S.Pd.", "R-204", 32, 36, "4321", "2026-11-12")
        )
        starterClasses.forEach { c ->
            batch1.set(schoolRef.collection("classes").document(c.id), c.toCreateMap())
            batch1.set(schoolRef.collection("classes_public").document(c.id), c.toPublicCreateMap())
            batch1.set(
                schoolRef.collection("pin_verifications").document("${c.id}_${c.pinKelas}"),
                mapOf(
                    "classId" to c.id,
                    "className" to c.name,
                    "idSekolah" to schoolId,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }
        MockDataProvider.initialTeachers().filter { it.idSekolah == "SCH-001" }.forEach { t ->
            batch1.set(schoolRef.collection("teachers").document(t.id), t.copy(idSekolah = schoolId).toCreateMap())
        }
        MockDataProvider.initialUserAccounts()
            .filter { it.idSekolah == "SCH-001" && it.role != UserRole.SUPERADMIN }
            .forEach { acc ->
                batch1.set(schoolRef.collection("staff_whitelist").document(acc.id), acc.copy(idSekolah = schoolId).toCreateMap())
            }
        batch1.commit().await()

        // Chunk 2: Subjects, Students, Attendance, Grades
        val batch2 = firestore.batch()
        MockDataProvider.initialSubjects().filter { it.idSekolah == "SCH-001" }.forEach { sub ->
            batch2.set(schoolRef.collection("subjects").document(sub.id), sub.copy(idSekolah = schoolId).toCreateMap())
        }
        MockDataProvider.initialStudents().filter { it.idSekolah == "SCH-001" }.forEach { st ->
            batch2.set(schoolRef.collection("students").document(st.id), st.copy(idSekolah = schoolId).toCreateMap())
        }
        MockDataProvider.initialAttendance().filter { it.idSekolah == "SCH-001" }.forEach { att ->
            batch2.set(schoolRef.collection("attendance").document(att.id), att.copy(idSekolah = schoolId, teacherId = "T-01", subjectName = "Matematika Peminatan").toCreateMap())
        }
        MockDataProvider.initialGrades().filter { it.idSekolah == "SCH-001" }.forEach { gr ->
            batch2.set(schoolRef.collection("grades").document(gr.id), gr.copy(idSekolah = schoolId, teacherId = "T-01").toCreateMap())
        }
        batch2.commit().await()

        // Chunk 3: RPPs, Journals, Assignments, Notifications, Holidays, Announcements
        val batch3 = firestore.batch()
        MockDataProvider.initialLessonPlans().filter { it.idSekolah == "SCH-001" }.forEach { rpp ->
            batch3.set(schoolRef.collection("lesson_plans").document(rpp.id), rpp.copy(idSekolah = schoolId, userId = ownerUid).toCreateMap())
        }
        MockDataProvider.initialTeachingJournals().filter { it.idSekolah == "SCH-001" }.forEach { j ->
            batch3.set(schoolRef.collection("teaching_journals").document(j.id), j.copy(idSekolah = schoolId, userId = ownerUid).toCreateMap())
        }
        MockDataProvider.initialAssignments().filter { it.idSekolah == "SCH-001" }.forEach { a ->
            batch3.set(schoolRef.collection("assignments").document(a.id), a.copy(idSekolah = schoolId, userId = ownerUid).toCreateMap())
        }
        MockDataProvider.initialNotifications().filter { it.idSekolah == "SCH-001" }.forEach { n ->
            batch3.set(
                schoolRef.collection("notifications").document(n.id),
                n.copy(idSekolah = schoolId, senderUid = ownerUid, message = "${n.message} [$schoolName]").toCreateMap()
            )
        }
        MockDataProvider.initialHolidays().filter { it.idSekolah == "SCH-001" }.forEach { h ->
            batch3.set(schoolRef.collection("holidays").document(h.id), h.copy(idSekolah = schoolId).toCreateMap())
        }
        MockDataProvider.initialAnnouncements().filter { it.idSekolah == "SCH-001" }.forEach { ann ->
            batch3.set(schoolRef.collection("announcements").document(ann.id), ann.copy(idSekolah = schoolId, userId = ownerUid).toCreateMap())
        }
        batch3.commit().await()

        // Chunk 4: PTK Attendance, Teaching Schedules, Forum Diskusi Starter
        val batch4 = firestore.batch()
        MockDataProvider.initialPtkAttendance().filter { it.idSekolah == "SCH-001" }.forEach { ptk ->
            batch4.set(schoolRef.collection("ptk_attendance").document(ptk.id), ptk.copy(idSekolah = schoolId, recordedByUid = ownerUid).toCreateMap())
        }
        MockDataProvider.initialTeachingSchedules().filter { it.idSekolah == "SCH-001" }.forEach { sch ->
            batch4.set(schoolRef.collection("teaching_schedules").document(sch.id), sch.copy(idSekolah = schoolId).toCreateMap())
        }
        MockDataProvider.initialForumMessages().filter { it.idSekolah == "SCH-001" }.forEach { msg ->
            batch4.set(schoolRef.collection("forum_messages").document(msg.id), msg.copy(idSekolah = schoolId, senderUid = ownerUid, senderRole = "SUPERADMIN").toCreateMap())
        }
        batch4.commit().await()
    }

    /**
     * Verifies a Student Class PIN using Zero-Exposure pin_verifications lookup
     * and binds the student membership doc in Firestore.
     */
    suspend fun verifyStudentPinAndJoin(
        schoolId: String,
        classId: String,
        className: String,
        studentName: String,
        pin: String
    ): Result<AuthenticatedSession> {
        val user = auth.currentUser
            ?: return Result.failure(IllegalStateException("Silakan autentikasi Google terlebih dahulu."))
        val uid = user.uid
        val email = currentEmail()
        val cleanPin = pin.trim()
        val verificationId = "${classId}_${cleanPin}"

        return runCatching {
            val pinDoc = schoolsCol.document(schoolId)
                .collection("pin_verifications").document(verificationId)
                .get()
                .await()

            if (!pinDoc.exists()) {
                throw IllegalArgumentException("PIN Kelas '$cleanPin' salah untuk kelas $className. Hubungi Wali Kelas atau Admin.")
            }

            val memberRef = schoolsCol.document(schoolId).collection("members").document(uid)
            val studentId = "STU-${uid.take(8)}"
            val memberMap = hashMapOf(
                "uid" to uid,
                "idSekolah" to schoolId,
                "email" to email,
                "name" to studentName.trim(),
                "role" to UserRole.SISWA.name,
                "idGuru" to "",
                "idKelas" to listOf(classId),
                "idMapel" to emptyList<String>(),
                "whitelistId" to verificationId,
                "studentId" to studentId,
                "className" to className,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            memberRef.set(memberMap).await()
            upsertUserProfile(uid, email, studentName.trim(), schoolId)

            val session = AuthenticatedSession(
                uid = uid,
                email = email,
                displayName = studentName.trim(),
                idSekolah = schoolId,
                role = UserRole.SISWA,
                idKelas = listOf(classId),
                studentId = studentId,
                className = className
            )
            runCatching { recordLoginActivity(session, "Android App • PIN Kelas Siswa") }
            session
        }.onFailure { e ->
            handleFirestoreError(e, OperationType.GET, "schools/$schoolId/pin_verifications/$verificationId", auth)
        }
    }

    // =========================================================================
    // REAL-TIME SNAPSHOT OBSERVERS
    // =========================================================================

    fun observeSchoolDirectory(): Flow<List<SchoolDirectoryEntry>> = callbackFlow {
        val reg = directoryCol.addSnapshotListener { snap, err ->
            if (err != null) {
                handleFirestoreError(err, OperationType.LIST, "school_directory", auth)
                return@addSnapshotListener
            }
            val list = snap?.documents?.mapNotNull { doc ->
                SchoolDirectoryEntry(
                    idSekolah = doc.getString("idSekolah") ?: doc.id,
                    name = doc.getString("name") ?: return@mapNotNull null,
                    level = doc.getString("level") ?: "SMA",
                    city = doc.getString("city") ?: "",
                    npsn = doc.getString("npsn") ?: ""
                )
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    fun observeClassesPublic(schoolId: String): Flow<List<PublicClassEntry>> = callbackFlow {
        val reg = schoolsCol.document(schoolId).collection("classes_public")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    handleFirestoreError(err, OperationType.LIST, "schools/$schoolId/classes_public", auth)
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { doc ->
                    PublicClassEntry(
                        id = doc.getString("id") ?: doc.id,
                        idSekolah = doc.getString("idSekolah") ?: schoolId,
                        name = doc.getString("name") ?: return@mapNotNull null,
                        gradeLevel = (doc.getLong("gradeLevel") ?: 10L).toInt(),
                        homeroomTeacher = doc.getString("homeroomTeacher") ?: "",
                        room = doc.getString("room") ?: "",
                        studentCount = (doc.getLong("studentCount") ?: 0L).toInt(),
                        capacity = (doc.getLong("capacity") ?: 36L).toInt()
                    )
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    fun observeOwnedSchools(uid: String): Flow<List<TenantSchool>> = callbackFlow {
        val reg = schoolsCol.whereEqualTo("ownerUid", uid)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    handleFirestoreError(err, OperationType.LIST, "schools", auth)
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { doc ->
                    TenantSchool(
                        idSekolah = doc.getString("idSekolah") ?: doc.id,
                        name = doc.getString("name") ?: return@mapNotNull null,
                        npsn = doc.getString("npsn") ?: "",
                        level = doc.getString("level") ?: "SMA",
                        city = doc.getString("city") ?: "",
                        ownerUid = doc.getString("ownerUid") ?: ""
                    )
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    fun observeSchoolProfileAndConfig(schoolId: String): Flow<Pair<SchoolProfile, ProSubscription>?> = callbackFlow {
        val reg = schoolsCol.document(schoolId).addSnapshotListener { doc, err ->
            if (err != null) {
                handleFirestoreError(err, OperationType.GET, "schools/$schoolId", auth)
                return@addSnapshotListener
            }
            if (doc != null && doc.exists()) {
                val presetName = doc.getString("themePreset") ?: ThemePreset.BLUE_TEAL.name
                val themePreset = runCatching { ThemePreset.valueOf(presetName) }.getOrDefault(ThemePreset.BLUE_TEAL)
                val profile = SchoolProfile(
                    idSekolah = doc.getString("idSekolah") ?: schoolId,
                    name = doc.getString("name") ?: "",
                    npsn = doc.getString("npsn") ?: "",
                    akreditasi = doc.getString("akreditasi") ?: "A",
                    level = doc.getString("level") ?: "SMA",
                    address = doc.getString("address") ?: "",
                    email = doc.getString("email") ?: "",
                    phone = doc.getString("phone") ?: "",
                    website = doc.getString("website") ?: "",
                    kepalaSekolah = doc.getString("kepalaSekolah") ?: "",
                    vision = doc.getString("vision") ?: "",
                    mission = doc.getString("mission") ?: "",
                    logoBase64 = doc.getString("logoBase64") ?: "",
                    logoFileName = doc.getString("logoFileName") ?: "",
                    logoSizeBytes = doc.getLong("logoSizeBytes") ?: 0L,
                    themePreset = themePreset,
                    organizationStructure = doc.getString("organizationStructure") ?: "",
                    academicYear = doc.getString("academicYear") ?: "2026/2027",
                    semester = doc.getString("semester") ?: "Ganjil"
                )
                val isPro = doc.getBoolean("isPro") ?: false
                val used = (doc.getLong("storageUsedMb") ?: 180L).toInt()
                val max = (doc.getLong("storageMaxMb") ?: if (isPro) 51200L else 500L).toInt()
                val adNetName = doc.getString("activeAdNetwork") ?: AdNetwork.ADMOB.name
                val adNet = runCatching { AdNetwork.valueOf(adNetName) }.getOrDefault(AdNetwork.ADMOB)
                val sub = ProSubscription(
                    isPro = isPro,
                    storageUsedMb = used,
                    storageMaxMb = max,
                    activeAdNetwork = adNet
                )
                trySend(profile to sub)
            }
        }
        awaitClose { reg.remove() }
    }

    fun observeStaffWhitelist(schoolId: String, role: UserRole, email: String): Flow<List<UserAccount>> = callbackFlow {
        val col = schoolsCol.document(schoolId).collection("staff_whitelist")
        val query = if (role == UserRole.SUPERADMIN) {
            col
        } else {
            col.whereEqualTo("email", email.trim().lowercase())
        }
        val reg = query.addSnapshotListener { snap, err ->
            if (err != null) {
                handleFirestoreError(err, OperationType.LIST, "schools/$schoolId/staff_whitelist", auth)
                return@addSnapshotListener
            }
            val list = snap?.documents?.mapNotNull { doc ->
                val roleStr = doc.getString("role") ?: "GURU"
                UserAccount(
                    id = doc.getString("id") ?: doc.id,
                    idSekolah = doc.getString("idSekolah") ?: schoolId,
                    email = doc.getString("email") ?: return@mapNotNull null,
                    name = doc.getString("name") ?: "",
                    nip = doc.getString("nip") ?: "",
                    role = runCatching { UserRole.valueOf(roleStr) }.getOrDefault(UserRole.GURU),
                    jabatan = doc.getString("jabatan") ?: "",
                    isVerified = doc.getBoolean("isVerified") ?: true,
                    idGuru = doc.getString("idGuru") ?: "",
                    idKelas = (doc.get("idKelas") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                    idMapel = (doc.get("idMapel") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                )
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    fun observeClasses(schoolId: String, role: UserRole): Flow<List<SchoolClass>> = callbackFlow {
        var reg: ListenerRegistration? = null
        if (role == UserRole.SISWA) {
            // Students observe pin-free classes_public collection
            reg = schoolsCol.document(schoolId).collection("classes_public")
                .addSnapshotListener { snap, err ->
                    if (err != null) {
                        handleFirestoreError(err, OperationType.LIST, "schools/$schoolId/classes_public", auth)
                        return@addSnapshotListener
                    }
                    val list = snap?.documents?.mapNotNull { doc ->
                        SchoolClass(
                            id = doc.getString("id") ?: doc.id,
                            idSekolah = doc.getString("idSekolah") ?: schoolId,
                            name = doc.getString("name") ?: return@mapNotNull null,
                            gradeLevel = (doc.getLong("gradeLevel") ?: 10L).toInt(),
                            homeroomTeacher = doc.getString("homeroomTeacher") ?: "",
                            room = doc.getString("room") ?: "",
                            studentCount = (doc.getLong("studentCount") ?: 0L).toInt(),
                            capacity = (doc.getLong("capacity") ?: 36L).toInt(),
                            pinKelas = "****",
                            pinUpdatedAt = "-"
                        )
                    } ?: emptyList()
                    trySend(list)
                }
        } else {
            reg = schoolsCol.document(schoolId).collection("classes")
                .addSnapshotListener { snap, err ->
                    if (err != null) {
                        handleFirestoreError(err, OperationType.LIST, "schools/$schoolId/classes", auth)
                        return@addSnapshotListener
                    }
                    val list = snap?.documents?.mapNotNull { doc ->
                        SchoolClass(
                            id = doc.getString("id") ?: doc.id,
                            idSekolah = doc.getString("idSekolah") ?: schoolId,
                            name = doc.getString("name") ?: return@mapNotNull null,
                            gradeLevel = (doc.getLong("gradeLevel") ?: 10L).toInt(),
                            homeroomTeacher = doc.getString("homeroomTeacher") ?: "",
                            room = doc.getString("room") ?: "",
                            studentCount = (doc.getLong("studentCount") ?: 0L).toInt(),
                            capacity = (doc.getLong("capacity") ?: 36L).toInt(),
                            pinKelas = doc.getString("pinKelas") ?: "1234",
                            pinUpdatedAt = doc.getString("pinUpdatedAt") ?: ""
                        )
                    } ?: emptyList()
                    trySend(list)
                }
        }
        awaitClose { reg?.remove() }
    }

    fun observeTeachers(schoolId: String): Flow<List<Teacher>> = callbackFlow {
        val reg = schoolsCol.document(schoolId).collection("teachers")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    handleFirestoreError(err, OperationType.LIST, "schools/$schoolId/teachers", auth)
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { doc ->
                    Teacher(
                        id = doc.getString("id") ?: doc.id,
                        idSekolah = doc.getString("idSekolah") ?: schoolId,
                        nip = doc.getString("nip") ?: "",
                        name = doc.getString("name") ?: return@mapNotNull null,
                        email = doc.getString("email") ?: "",
                        phone = doc.getString("phone") ?: "",
                        subject = doc.getString("subject") ?: "",
                        employmentStatus = doc.getString("employmentStatus") ?: "PNS",
                        jabatan = doc.getString("jabatan") ?: "Guru Mata Pelajaran",
                        skNumber = doc.getString("skNumber") ?: "",
                        skDate = doc.getString("skDate") ?: "",
                        tmtDate = doc.getString("tmtDate") ?: "",
                        tenureYears = (doc.getLong("tenureYears") ?: 0L).toInt(),
                        rankGroup = doc.getString("rankGroup") ?: "",
                        weeklyHours = (doc.getLong("weeklyHours") ?: 24L).toInt(),
                        classesTaught = (doc.get("classesTaught") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                        address = doc.getString("address") ?: "",
                        photoBase64 = doc.getString("photoBase64") ?: "",
                        skDocumentName = doc.getString("skDocumentName") ?: "",
                        skDocumentSizeBytes = doc.getLong("skDocumentSizeBytes") ?: 0L,
                        isActiveAccount = doc.getBoolean("isActiveAccount") ?: true,
                        additionalDuty = doc.getString("additionalDuty") ?: "",
                        isNonSatmikal = doc.getBoolean("isNonSatmikal") ?: false,
                        satmikalOrigin = doc.getString("satmikalOrigin") ?: "",
                        nonSatmikalVerified = doc.getBoolean("nonSatmikalVerified") ?: false,
                        mutationStatus = doc.getString("mutationStatus") ?: "NONE",
                        mutationTargetSchool = doc.getString("mutationTargetSchool") ?: "",
                        transferConfirmed = doc.getBoolean("transferConfirmed") ?: false
                    )
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    fun observeStudents(schoolId: String, role: UserRole, uid: String): Flow<List<Student>> = callbackFlow {
        val col = schoolsCol.document(schoolId).collection("students")
        val query = if (role == UserRole.SISWA) col.whereEqualTo("userId", uid) else col
        val reg = query.addSnapshotListener { snap, err ->
            if (err != null) {
                handleFirestoreError(err, OperationType.LIST, "schools/$schoolId/students", auth)
                return@addSnapshotListener
            }
            val list = snap?.documents?.mapNotNull { doc ->
                val phone = doc.getString("phone") ?: ""
                Student(
                    id = doc.getString("id") ?: doc.id,
                    idSekolah = doc.getString("idSekolah") ?: schoolId,
                    nisn = doc.getString("nisn") ?: "",
                    name = doc.getString("name") ?: return@mapNotNull null,
                    className = doc.getString("className") ?: "",
                    gender = doc.getString("gender") ?: "L",
                    religion = doc.getString("religion") ?: "",
                    phone = phone,
                    address = doc.getString("address") ?: "",
                    status = doc.getString("status") ?: "Aktif",
                    userId = doc.getString("userId") ?: "",
                    nis = doc.getString("nis") ?: "",
                    birthDate = doc.getString("birthDate") ?: "",
                    parentName = doc.getString("parentName") ?: "",
                    parentPhone = doc.getString("parentPhone") ?: phone,
                    promotionHistory = doc.getString("promotionHistory") ?: ""
                )
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    fun observeSubjects(schoolId: String): Flow<List<Subject>> = callbackFlow {
        val reg = schoolsCol.document(schoolId).collection("subjects")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    handleFirestoreError(err, OperationType.LIST, "schools/$schoolId/subjects", auth)
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { doc ->
                    Subject(
                        id = doc.getString("id") ?: doc.id,
                        idSekolah = doc.getString("idSekolah") ?: schoolId,
                        code = doc.getString("code") ?: "",
                        name = doc.getString("name") ?: return@mapNotNull null,
                        category = doc.getString("category") ?: "Umum",
                        kkm = (doc.getLong("kkm") ?: 75L).toInt(),
                        hoursPerWeek = (doc.getLong("hoursPerWeek") ?: 2L).toInt()
                    )
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    fun observeAttendance(schoolId: String, role: UserRole, uid: String): Flow<List<AttendanceRecord>> = callbackFlow {
        val col = schoolsCol.document(schoolId).collection("attendance")
        val query = if (role == UserRole.SISWA) col.whereEqualTo("studentUserId", uid) else col
        val reg = query.addSnapshotListener { snap, err ->
            if (err != null) {
                handleFirestoreError(err, OperationType.LIST, "schools/$schoolId/attendance", auth)
                return@addSnapshotListener
            }
            val list = snap?.documents?.mapNotNull { doc ->
                val statusStr = doc.getString("status") ?: "HADIR"
                AttendanceRecord(
                    id = doc.getString("id") ?: doc.id,
                    idSekolah = doc.getString("idSekolah") ?: schoolId,
                    date = doc.getString("date") ?: "",
                    className = doc.getString("className") ?: "",
                    studentId = doc.getString("studentId") ?: "",
                    studentName = doc.getString("studentName") ?: return@mapNotNull null,
                    status = runCatching { AttendanceStatus.valueOf(statusStr) }.getOrDefault(AttendanceStatus.HADIR),
                    notes = doc.getString("notes") ?: "",
                    teacherId = doc.getString("teacherId") ?: "",
                    subjectName = doc.getString("subjectName") ?: "",
                    studentUserId = doc.getString("studentUserId") ?: ""
                )
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    fun observeGrades(schoolId: String, role: UserRole, uid: String): Flow<List<GradeRecord>> = callbackFlow {
        val col = schoolsCol.document(schoolId).collection("grades")
        val query = if (role == UserRole.SISWA) col.whereEqualTo("studentUserId", uid) else col
        val reg = query.addSnapshotListener { snap, err ->
            if (err != null) {
                handleFirestoreError(err, OperationType.LIST, "schools/$schoolId/grades", auth)
                return@addSnapshotListener
            }
            val list = snap?.documents?.mapNotNull { doc ->
                GradeRecord(
                    id = doc.getString("id") ?: doc.id,
                    idSekolah = doc.getString("idSekolah") ?: schoolId,
                    studentId = doc.getString("studentId") ?: "",
                    studentName = doc.getString("studentName") ?: return@mapNotNull null,
                    className = doc.getString("className") ?: "",
                    subjectName = doc.getString("subjectName") ?: "",
                    tugasScore = doc.getDouble("tugasScore") ?: 0.0,
                    utsScore = doc.getDouble("utsScore") ?: 0.0,
                    uasScore = doc.getDouble("uasScore") ?: 0.0,
                    teacherId = doc.getString("teacherId") ?: "",
                    studentUserId = doc.getString("studentUserId") ?: ""
                )
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    fun observeLessonPlans(schoolId: String): Flow<List<LessonPlan>> = callbackFlow {
        val reg = schoolsCol.document(schoolId).collection("lesson_plans")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    handleFirestoreError(err, OperationType.LIST, "schools/$schoolId/lesson_plans", auth)
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { doc ->
                    val statusStr = doc.getString("status") ?: "DRAFT"
                    LessonPlan(
                        id = doc.getString("id") ?: doc.id,
                        idSekolah = doc.getString("idSekolah") ?: schoolId,
                        title = doc.getString("title") ?: return@mapNotNull null,
                        curriculum = doc.getString("curriculum") ?: "Kurikulum Merdeka",
                        subject = doc.getString("subject") ?: "",
                        className = doc.getString("className") ?: "",
                        meetingCount = (doc.getLong("meetingCount") ?: 4L).toInt(),
                        duration = doc.getString("duration") ?: "2 x 45 Menit",
                        learningObjectives = doc.getString("learningObjectives") ?: "",
                        fileName = doc.getString("fileName") ?: "",
                        teacherId = doc.getString("teacherId") ?: "",
                        teacherName = doc.getString("teacherName") ?: "",
                        teacherEmail = doc.getString("teacherEmail") ?: "",
                        userId = doc.getString("userId") ?: "",
                        reviewNotes = doc.getString("reviewNotes") ?: "",
                        fileSizeBytes = doc.getLong("fileSizeBytes") ?: 0L,
                        storageUrl = doc.getString("storageUrl") ?: "",
                        status = runCatching { RppStatus.valueOf(statusStr) }.getOrDefault(RppStatus.DRAFT)
                    )
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    fun observeTeachingJournals(schoolId: String): Flow<List<TeachingJournal>> = callbackFlow {
        val reg = schoolsCol.document(schoolId).collection("teaching_journals")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    handleFirestoreError(err, OperationType.LIST, "schools/$schoolId/teaching_journals", auth)
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { doc ->
                    TeachingJournal(
                        id = doc.getString("id") ?: doc.id,
                        idSekolah = doc.getString("idSekolah") ?: schoolId,
                        date = doc.getString("date") ?: "",
                        teacherName = doc.getString("teacherName") ?: "",
                        subject = doc.getString("subject") ?: "",
                        className = doc.getString("className") ?: "",
                        periodTime = doc.getString("periodTime") ?: "",
                        topic = doc.getString("topic") ?: return@mapNotNull null,
                        activities = doc.getString("activities") ?: "",
                        obstacles = doc.getString("obstacles") ?: "",
                        reflection = doc.getString("reflection") ?: "",
                        documentationFileName = doc.getString("documentationFileName") ?: "",
                        isCompleted = doc.getBoolean("isCompleted") ?: true,
                        teacherId = doc.getString("teacherId") ?: "",
                        userId = doc.getString("userId") ?: "",
                        fileSizeBytes = doc.getLong("fileSizeBytes") ?: 0L,
                        monthKey = doc.getString("monthKey") ?: ""
                    )
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    fun observeAssignments(schoolId: String): Flow<List<StudentAssignment>> = callbackFlow {
        val reg = schoolsCol.document(schoolId).collection("assignments")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    handleFirestoreError(err, OperationType.LIST, "schools/$schoolId/assignments", auth)
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { doc ->
                    StudentAssignment(
                        id = doc.getString("id") ?: doc.id,
                        idSekolah = doc.getString("idSekolah") ?: schoolId,
                        className = doc.getString("className") ?: "",
                        subject = doc.getString("subject") ?: "",
                        title = doc.getString("title") ?: return@mapNotNull null,
                        description = doc.getString("description") ?: "",
                        deadline = doc.getString("deadline") ?: "",
                        type = doc.getString("type") ?: "Tugas",
                        fileName = doc.getString("fileName") ?: "",
                        fileSizeBytes = doc.getLong("fileSizeBytes") ?: 0L,
                        teacherId = doc.getString("teacherId") ?: "",
                        userId = doc.getString("userId") ?: ""
                    )
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    fun observeNotifications(schoolId: String, role: UserRole, email: String): Flow<List<AppNotification>> = callbackFlow {
        val col = schoolsCol.document(schoolId).collection("notifications")
        val query = when (role) {
            UserRole.SUPERADMIN, UserRole.KEPALA_SEKOLAH -> col
            UserRole.GURU -> col.whereIn("recipientEmailOrRole", listOf(email.trim().lowercase(), "GURU"))
            UserRole.SISWA -> {
                trySend(emptyList())
                awaitClose {}
                return@callbackFlow
            }
        }
        val reg = query.addSnapshotListener { snap, err ->
            if (err != null) {
                handleFirestoreError(err, OperationType.LIST, "schools/$schoolId/notifications", auth)
                return@addSnapshotListener
            }
            val list = snap?.documents?.mapNotNull { doc ->
                AppNotification(
                    id = doc.getString("id") ?: doc.id,
                    idSekolah = doc.getString("idSekolah") ?: schoolId,
                    recipientEmailOrRole = doc.getString("recipientEmailOrRole") ?: "",
                    title = doc.getString("title") ?: return@mapNotNull null,
                    message = doc.getString("message") ?: "",
                    timestamp = doc.getString("timestamp") ?: "",
                    isRead = doc.getBoolean("isRead") ?: false,
                    type = doc.getString("type") ?: "RPP_STATUS",
                    senderUid = doc.getString("senderUid") ?: ""
                )
            } ?: emptyList()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    fun observeHolidays(schoolId: String): Flow<List<SchoolHoliday>> = callbackFlow {
        val reg = schoolsCol.document(schoolId).collection("holidays")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    handleFirestoreError(err, OperationType.LIST, "schools/$schoolId/holidays", auth)
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { doc ->
                    SchoolHoliday(
                        id = doc.getString("id") ?: doc.id,
                        idSekolah = doc.getString("idSekolah") ?: schoolId,
                        date = doc.getString("date") ?: "",
                        name = doc.getString("name") ?: return@mapNotNull null,
                        category = doc.getString("category") ?: "Libur Nasional"
                    )
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    fun observeAnnouncements(schoolId: String): Flow<List<SchoolAnnouncement>> = callbackFlow {
        val reg = schoolsCol.document(schoolId).collection("announcements")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    handleFirestoreError(err, OperationType.LIST, "schools/$schoolId/announcements", auth)
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { doc ->
                    SchoolAnnouncement(
                        id = doc.getString("id") ?: doc.id,
                        idSekolah = doc.getString("idSekolah") ?: schoolId,
                        title = doc.getString("title") ?: return@mapNotNull null,
                        content = doc.getString("content") ?: "",
                        targetRole = doc.getString("targetRole") ?: "Semua",
                        authorName = doc.getString("authorName") ?: "Admin Sekolah",
                        publishedDate = doc.getString("publishedDate") ?: "",
                        userId = doc.getString("userId") ?: ""
                    )
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    fun observeLoginLogs(schoolId: String): Flow<List<LoginActivityLog>> = callbackFlow {
        val reg = schoolsCol.document(schoolId).collection("login_logs")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    handleFirestoreError(err, OperationType.LIST, "schools/$schoolId/login_logs", auth)
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { doc ->
                    LoginActivityLog(
                        id = doc.getString("id") ?: doc.id,
                        idSekolah = doc.getString("idSekolah") ?: schoolId,
                        uid = doc.getString("uid") ?: "",
                        email = doc.getString("email") ?: "",
                        displayName = doc.getString("displayName") ?: return@mapNotNull null,
                        role = doc.getString("role") ?: "GURU",
                        timestamp = doc.getString("timestamp") ?: "",
                        deviceInfo = doc.getString("deviceInfo") ?: "Android App",
                        status = doc.getString("status") ?: "Sukses"
                    )
                }?.sortedByDescending { it.timestamp } ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    fun observePtkAttendance(schoolId: String): Flow<List<PtkAttendanceRecord>> = callbackFlow {
        val reg = schoolsCol.document(schoolId).collection("ptk_attendance")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    handleFirestoreError(err, OperationType.LIST, "schools/$schoolId/ptk_attendance", auth)
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { doc ->
                    val st = doc.getString("status") ?: "HADIR"
                    PtkAttendanceRecord(
                        id = doc.getString("id") ?: doc.id,
                        idSekolah = doc.getString("idSekolah") ?: schoolId,
                        teacherId = doc.getString("teacherId") ?: "",
                        teacherName = doc.getString("teacherName") ?: return@mapNotNull null,
                        date = doc.getString("date") ?: "",
                        status = runCatching { PtkAttendanceStatus.valueOf(st) }.getOrDefault(PtkAttendanceStatus.HADIR),
                        notes = doc.getString("notes") ?: "",
                        recordedByUid = doc.getString("recordedByUid") ?: ""
                    )
                }?.sortedByDescending { it.date } ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    fun observeTeachingSchedules(schoolId: String): Flow<List<TeachingSchedule>> = callbackFlow {
        val reg = schoolsCol.document(schoolId).collection("teaching_schedules")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    handleFirestoreError(err, OperationType.LIST, "schools/$schoolId/teaching_schedules", auth)
                    return@addSnapshotListener
                }
                val dayOrder = mapOf("Senin" to 1, "Selasa" to 2, "Rabu" to 3, "Kamis" to 4, "Jumat" to 5, "Sabtu" to 6)
                val list = snap?.documents?.mapNotNull { doc ->
                    TeachingSchedule(
                        id = doc.getString("id") ?: doc.id,
                        idSekolah = doc.getString("idSekolah") ?: schoolId,
                        dayOfWeek = doc.getString("dayOfWeek") ?: "Senin",
                        startTime = doc.getString("startTime") ?: "07:30",
                        endTime = doc.getString("endTime") ?: "09:00",
                        className = doc.getString("className") ?: "",
                        subjectName = doc.getString("subjectName") ?: return@mapNotNull null,
                        teacherId = doc.getString("teacherId") ?: "",
                        teacherName = doc.getString("teacherName") ?: "",
                        room = doc.getString("room") ?: ""
                    )
                }?.sortedWith(compareBy({ dayOrder[it.dayOfWeek] ?: 99 }, { it.startTime })) ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    fun observeForumMessages(schoolId: String, limitCount: Long = 25L): Flow<List<ForumMessage>> = callbackFlow {
        val reg = schoolsCol.document(schoolId).collection("forum_messages")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limitCount.coerceIn(10L, 200L))
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    handleFirestoreError(err, OperationType.LIST, "schools/$schoolId/forum_messages", auth)
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { doc ->
                    val typeStr = doc.getString("messageType") ?: "TEXT"
                    val createdMs = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L
                    ForumMessage(
                        id = doc.getString("id") ?: doc.id,
                        idSekolah = doc.getString("idSekolah") ?: schoolId,
                        senderUid = doc.getString("senderUid") ?: "",
                        senderName = doc.getString("senderName") ?: return@mapNotNull null,
                        senderRole = doc.getString("senderRole") ?: "GURU",
                        messageType = runCatching { ForumMessageType.valueOf(typeStr) }.getOrDefault(ForumMessageType.TEXT),
                        textContent = doc.getString("textContent") ?: "",
                        voiceBase64 = doc.getString("voiceBase64") ?: "",
                        voiceDurationSec = (doc.getLong("voiceDurationSec") ?: 0L).toInt(),
                        sentDate = doc.getString("sentDate") ?: "",
                        sentTime = doc.getString("sentTime") ?: "",
                        isArchived = doc.getBoolean("isArchived") ?: false,
                        createdAtMillis = createdMs
                    )
                }?.reversed() ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    // =========================================================================
    // WRITE MUTATIONS (STRICT RULE COMPLIANCE)
    // =========================================================================

    suspend fun updateSchoolProfileAndTenant(tenant: TenantSchool, profile: SchoolProfile): Result<Unit> {
        val schoolId = tenant.idSekolah
        return runCatching {
            val batch = firestore.batch()
            batch.update(
                schoolsCol.document(schoolId),
                mapOf(
                    "name" to profile.name,
                    "npsn" to profile.npsn.ifBlank { tenant.npsn },
                    "akreditasi" to profile.akreditasi,
                    "level" to profile.level,
                    "city" to tenant.city,
                    "address" to profile.address,
                    "email" to profile.email,
                    "phone" to profile.phone,
                    "website" to profile.website,
                    "kepalaSekolah" to profile.kepalaSekolah,
                    "vision" to profile.vision,
                    "mission" to profile.mission,
                    "logoBase64" to profile.logoBase64,
                    "logoFileName" to profile.logoFileName,
                    "logoSizeBytes" to profile.logoSizeBytes.coerceIn(0L, 5_242_880L),
                    "themePreset" to profile.themePreset.name,
                    "organizationStructure" to profile.organizationStructure,
                    "academicYear" to profile.academicYear.ifBlank { "2026/2027" },
                    "semester" to profile.semester.ifBlank { "Ganjil" },
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                directoryCol.document(schoolId),
                mapOf(
                    "name" to profile.name,
                    "level" to profile.level,
                    "city" to tenant.city,
                    "npsn" to profile.npsn.ifBlank { tenant.npsn },
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Unit
        }.onFailure { handleFirestoreError(it, OperationType.UPDATE, "schools/$schoolId", auth) }
    }

    suspend fun updateMasterAcademicConfig(schoolId: String, academicYear: String, semester: String): Result<Unit> {
        return runCatching {
            schoolsCol.document(schoolId).update(
                mapOf(
                    "academicYear" to academicYear.ifBlank { "2026/2027" },
                    "semester" to semester.ifBlank { "Ganjil" },
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Unit
        }.onFailure { handleFirestoreError(it, OperationType.UPDATE, "schools/$schoolId", auth) }
    }

    suspend fun updateProAndAdConfig(schoolId: String, pro: ProSubscription): Result<Unit> {
        return runCatching {
            schoolsCol.document(schoolId).update(
                mapOf(
                    "isPro" to pro.isPro,
                    "storageUsedMb" to pro.storageUsedMb,
                    "storageMaxMb" to pro.storageMaxMb,
                    "activeAdNetwork" to pro.activeAdNetwork.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Unit
        }.onFailure { handleFirestoreError(it, OperationType.UPDATE, "schools/$schoolId", auth) }
    }

    suspend fun saveStaffWhitelist(account: UserAccount): Result<Unit> {
        val schoolId = account.idSekolah
        return runCatching {
            val ref = schoolsCol.document(schoolId).collection("staff_whitelist").document(account.id)
            val snap = ref.get().await()
            if (snap.exists()) {
                ref.update(account.toUpdateMap()).await()
            } else {
                ref.set(account.toCreateMap()).await()
            }
            Unit
        }.onFailure { handleFirestoreError(it, OperationType.WRITE, "schools/$schoolId/staff_whitelist/${account.id}", auth) }
    }

    suspend fun deleteStaffWhitelist(schoolId: String, accountId: String): Result<Unit> = runCatching {
        schoolsCol.document(schoolId).collection("staff_whitelist").document(accountId).delete().await()
        Unit
    }.onFailure { handleFirestoreError(it, OperationType.DELETE, "schools/$schoolId/staff_whitelist/$accountId", auth) }

    suspend fun saveClassWithPin(schoolClass: SchoolClass): Result<Unit> {
        val schoolId = schoolClass.idSekolah
        return runCatching {
            val batch = firestore.batch()
            val classRef = schoolsCol.document(schoolId).collection("classes").document(schoolClass.id)
            val pubRef = schoolsCol.document(schoolId).collection("classes_public").document(schoolClass.id)
            val oldSnap = classRef.get().await()
            val oldPin = oldSnap.getString("pinKelas")

            if (oldSnap.exists()) {
                batch.update(classRef, schoolClass.toUpdateMap())
                batch.update(pubRef, schoolClass.toPublicUpdateMap())
                if (oldPin != null && oldPin != schoolClass.pinKelas) {
                    batch.delete(schoolsCol.document(schoolId).collection("pin_verifications").document("${schoolClass.id}_${oldPin}"))
                }
            } else {
                batch.set(classRef, schoolClass.toCreateMap())
                batch.set(pubRef, schoolClass.toPublicCreateMap())
            }

            batch.set(
                schoolsCol.document(schoolId).collection("pin_verifications").document("${schoolClass.id}_${schoolClass.pinKelas}"),
                mapOf(
                    "classId" to schoolClass.id,
                    "className" to schoolClass.name,
                    "idSekolah" to schoolId,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Unit
        }.onFailure { handleFirestoreError(it, OperationType.WRITE, "schools/$schoolId/classes/${schoolClass.id}", auth) }
    }

    suspend fun deleteClass(schoolId: String, classId: String, currentPin: String): Result<Unit> = runCatching {
        val batch = firestore.batch()
        batch.delete(schoolsCol.document(schoolId).collection("classes").document(classId))
        batch.delete(schoolsCol.document(schoolId).collection("classes_public").document(classId))
        if (currentPin.isNotBlank()) {
            batch.delete(schoolsCol.document(schoolId).collection("pin_verifications").document("${classId}_${currentPin}"))
        }
        batch.commit().await()
        Unit
    }.onFailure { handleFirestoreError(it, OperationType.DELETE, "schools/$schoolId/classes/$classId", auth) }

    suspend fun saveTeacher(teacher: Teacher): Result<Unit> {
        val res = upsertDoc(
            schoolId = teacher.idSekolah,
            collection = "teachers",
            docId = teacher.id,
            createMap = teacher.toCreateMap(),
            updateMap = teacher.toUpdateMap()
        )
        if (res.isSuccess && teacher.email.isNotBlank()) {
            val wlId = "WL-${teacher.id}"
            if (teacher.isActiveAccount) {
                val account = UserAccount(
                    id = wlId,
                    idSekolah = teacher.idSekolah,
                    email = teacher.email.trim().lowercase(),
                    name = teacher.name.trim(),
                    nip = teacher.nip.trim(),
                    role = UserRole.GURU,
                    jabatan = teacher.jabatan.ifBlank { "Guru Mata Pelajaran" },
                    isVerified = true,
                    idGuru = teacher.id,
                    idKelas = teacher.classesTaught,
                    idMapel = listOf(teacher.subject).filter { it.isNotBlank() }
                )
                runCatching { saveStaffWhitelist(account) }
            } else {
                runCatching { deleteStaffWhitelist(teacher.idSekolah, wlId) }
            }
        }
        return res
    }

    suspend fun deleteTeacher(schoolId: String, id: String): Result<Unit> {
        runCatching { deleteStaffWhitelist(schoolId, "WL-$id") }
        return deleteDoc(schoolId, "teachers", id)
    }

    suspend fun saveStudent(student: Student): Result<Unit> = upsertDoc(
        schoolId = student.idSekolah,
        collection = "students",
        docId = student.id,
        createMap = student.toCreateMap(),
        updateMap = student.toUpdateMap()
    )

    suspend fun deleteStudent(schoolId: String, id: String): Result<Unit> = deleteDoc(schoolId, "students", id)

    suspend fun saveSubject(subject: Subject): Result<Unit> = upsertDoc(
        schoolId = subject.idSekolah,
        collection = "subjects",
        docId = subject.id,
        createMap = subject.toCreateMap(),
        updateMap = subject.toUpdateMap()
    )

    suspend fun deleteSubject(schoolId: String, id: String): Result<Unit> = deleteDoc(schoolId, "subjects", id)

    suspend fun saveHoliday(holiday: SchoolHoliday): Result<Unit> = upsertDoc(
        schoolId = holiday.idSekolah,
        collection = "holidays",
        docId = holiday.id,
        createMap = holiday.toCreateMap(),
        updateMap = holiday.toUpdateMap()
    )

    suspend fun deleteHoliday(schoolId: String, id: String): Result<Unit> = deleteDoc(schoolId, "holidays", id)

    suspend fun saveAnnouncement(announcement: SchoolAnnouncement): Result<Unit> {
        val uid = currentUid() ?: return Result.failure(IllegalStateException("Belum login"))
        val safeAnn = announcement.copy(userId = announcement.userId.ifBlank { uid })
        return upsertDoc(
            schoolId = safeAnn.idSekolah,
            collection = "announcements",
            docId = safeAnn.id,
            createMap = safeAnn.toCreateMap(),
            updateMap = safeAnn.toUpdateMap()
        )
    }

    suspend fun deleteAnnouncement(schoolId: String, id: String): Result<Unit> = deleteDoc(schoolId, "announcements", id)

    suspend fun recordLoginActivity(session: AuthenticatedSession, deviceInfo: String = "Android App • Google Sign-In"): Result<Unit> {
        val uid = currentUid() ?: return Result.failure(IllegalStateException("Belum login"))
        val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val logId = "LOG-${System.currentTimeMillis()}"
        val log = LoginActivityLog(
            id = logId,
            idSekolah = session.idSekolah,
            uid = uid,
            email = session.email,
            displayName = session.displayName.ifBlank { session.email.ifBlank { "Pengguna" } },
            role = session.role.name,
            timestamp = ts,
            deviceInfo = deviceInfo,
            status = "Sukses"
        )
        return runCatching {
            schoolsCol.document(session.idSekolah)
                .collection("login_logs")
                .document(logId)
                .set(log.toCreateMap())
                .await()
            Unit
        }
    }

    suspend fun deleteLoginLog(schoolId: String, id: String): Result<Unit> = deleteDoc(schoolId, "login_logs", id)

    suspend fun saveAttendance(record: AttendanceRecord): Result<Unit> = upsertDoc(
        schoolId = record.idSekolah,
        collection = "attendance",
        docId = record.id,
        createMap = record.toCreateMap(),
        updateMap = record.toUpdateMap()
    )

    suspend fun saveGrade(grade: GradeRecord): Result<Unit> = upsertDoc(
        schoolId = grade.idSekolah,
        collection = "grades",
        docId = grade.id,
        createMap = grade.toCreateMap(),
        updateMap = grade.toUpdateMap()
    )

    suspend fun deleteGrade(schoolId: String, id: String): Result<Unit> = deleteDoc(schoolId, "grades", id)

    suspend fun saveLessonPlan(rpp: LessonPlan): Result<Unit> {
        val uid = currentUid() ?: return Result.failure(IllegalStateException("Belum login"))
        val safeRpp = rpp.copy(
            userId = rpp.userId.ifBlank { uid },
            teacherId = rpp.teacherId.ifBlank { "T-01" },
            teacherName = rpp.teacherName.ifBlank { auth.currentUser?.displayName ?: "Guru" },
            teacherEmail = rpp.teacherEmail.ifBlank { currentEmail().ifBlank { "guru@school.id" } }
        )
        return upsertDoc(
            schoolId = safeRpp.idSekolah,
            collection = "lesson_plans",
            docId = safeRpp.id,
            createMap = safeRpp.toCreateMap(),
            updateMap = safeRpp.toUpdateMap()
        )
    }

    suspend fun reviewLessonPlanByKepsek(
        schoolId: String,
        rppId: String,
        newStatus: RppStatus,
        reviewNotes: String
    ): Result<Unit> = runCatching {
        schoolsCol.document(schoolId).collection("lesson_plans").document(rppId)
            .update(
                mapOf(
                    "status" to newStatus.name,
                    "reviewNotes" to reviewNotes,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        Unit
    }.onFailure { handleFirestoreError(it, OperationType.UPDATE, "schools/$schoolId/lesson_plans/$rppId", auth) }

    suspend fun deleteLessonPlan(schoolId: String, id: String): Result<Unit> = deleteDoc(schoolId, "lesson_plans", id)

    suspend fun saveTeachingJournal(journal: TeachingJournal): Result<Unit> {
        val uid = currentUid() ?: return Result.failure(IllegalStateException("Belum login"))
        val safeJournal = journal.copy(
            userId = journal.userId.ifBlank { uid },
            teacherId = journal.teacherId.ifBlank { "T-01" },
            teacherName = journal.teacherName.ifBlank { auth.currentUser?.displayName ?: "Guru" }
        )
        return upsertDoc(
            schoolId = safeJournal.idSekolah,
            collection = "teaching_journals",
            docId = safeJournal.id,
            createMap = safeJournal.toCreateMap(),
            updateMap = safeJournal.toUpdateMap()
        )
    }

    suspend fun deleteTeachingJournal(schoolId: String, id: String): Result<Unit> = deleteDoc(schoolId, "teaching_journals", id)

    suspend fun saveAssignment(assignment: StudentAssignment): Result<Unit> {
        val uid = currentUid() ?: return Result.failure(IllegalStateException("Belum login"))
        val safeAssignment = assignment.copy(
            userId = assignment.userId.ifBlank { uid },
            teacherId = assignment.teacherId.ifBlank { "T-01" }
        )
        return upsertDoc(
            schoolId = safeAssignment.idSekolah,
            collection = "assignments",
            docId = safeAssignment.id,
            createMap = safeAssignment.toCreateMap(),
            updateMap = safeAssignment.toUpdateMap()
        )
    }

    suspend fun deleteAssignment(schoolId: String, id: String): Result<Unit> = deleteDoc(schoolId, "assignments", id)

    suspend fun createNotification(notification: AppNotification): Result<Unit> {
        val uid = currentUid() ?: return Result.failure(IllegalStateException("Belum login"))
        val safeNotif = notification.copy(
            senderUid = notification.senderUid.ifBlank { uid },
            recipientEmailOrRole = notification.recipientEmailOrRole.trim().ifBlank { "GURU" }.lowercase().let {
                if (it == "guru" || it == "kepala_sekolah" || it == "superadmin") it.uppercase() else it
            }
        )
        return runCatching {
            schoolsCol.document(safeNotif.idSekolah)
                .collection("notifications")
                .document(safeNotif.id)
                .set(safeNotif.toCreateMap())
                .await()
            Unit
        }.onFailure { handleFirestoreError(it, OperationType.CREATE, "schools/${safeNotif.idSekolah}/notifications/${safeNotif.id}", auth) }
    }

    suspend fun markNotificationRead(schoolId: String, notifId: String): Result<Unit> = runCatching {
        schoolsCol.document(schoolId).collection("notifications").document(notifId)
            .update(
                mapOf(
                    "isRead" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        Unit
    }.onFailure { handleFirestoreError(it, OperationType.UPDATE, "schools/$schoolId/notifications/$notifId", auth) }

    suspend fun savePtkAttendance(record: PtkAttendanceRecord): Result<Unit> {
        val uid = currentUid() ?: return Result.failure(IllegalStateException("Belum login"))
        val safe = record.copy(recordedByUid = record.recordedByUid.ifBlank { uid })
        return upsertDoc(
            schoolId = safe.idSekolah,
            collection = "ptk_attendance",
            docId = safe.id,
            createMap = safe.toCreateMap(),
            updateMap = safe.toUpdateMap()
        )
    }

    suspend fun deletePtkAttendance(schoolId: String, id: String): Result<Unit> =
        deleteDoc(schoolId, "ptk_attendance", id)

    suspend fun saveTeachingSchedule(schedule: TeachingSchedule): Result<Unit> = upsertDoc(
        schoolId = schedule.idSekolah,
        collection = "teaching_schedules",
        docId = schedule.id,
        createMap = schedule.toCreateMap(),
        updateMap = schedule.toUpdateMap()
    )

    suspend fun deleteTeachingSchedule(schoolId: String, id: String): Result<Unit> =
        deleteDoc(schoolId, "teaching_schedules", id)

    suspend fun sendForumMessage(message: ForumMessage): Result<Unit> {
        val uid = currentUid() ?: return Result.failure(IllegalStateException("Belum login"))
        val safe = message.copy(
            senderUid = uid,
            voiceDurationSec = if (message.messageType == ForumMessageType.VOICE_NOTE) message.voiceDurationSec.coerceIn(1, 30) else 0,
            voiceBase64 = if (message.messageType == ForumMessageType.VOICE_NOTE) message.voiceBase64 else "",
            isArchived = false
        )
        return runCatching {
            schoolsCol.document(safe.idSekolah)
                .collection("forum_messages")
                .document(safe.id)
                .set(safe.toCreateMap())
                .await()
            Unit
        }.onFailure { handleFirestoreError(it, OperationType.CREATE, "schools/${safe.idSekolah}/forum_messages/${safe.id}", auth) }
    }

    suspend fun archiveForumMessage(schoolId: String, msgId: String, isArchived: Boolean): Result<Unit> = runCatching {
        schoolsCol.document(schoolId).collection("forum_messages").document(msgId)
            .update(
                mapOf(
                    "isArchived" to isArchived,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        Unit
    }.onFailure { handleFirestoreError(it, OperationType.UPDATE, "schools/$schoolId/forum_messages/$msgId", auth) }

    suspend fun deleteForumMessage(schoolId: String, msgId: String): Result<Unit> =
        deleteDoc(schoolId, "forum_messages", msgId)

    // =========================================================================
    // HELPERS & MAPPERS
    // =========================================================================

    private suspend fun upsertDoc(
        schoolId: String,
        collection: String,
        docId: String,
        createMap: Map<String, Any>,
        updateMap: Map<String, Any>
    ): Result<Unit> = runCatching {
        val ref = schoolsCol.document(schoolId).collection(collection).document(docId)
        val snap = ref.get().await()
        if (snap.exists()) {
            ref.update(updateMap).await()
        } else {
            ref.set(createMap).await()
        }
        Unit
    }.onFailure { handleFirestoreError(it, OperationType.WRITE, "schools/$schoolId/$collection/$docId", auth) }

    private suspend fun deleteDoc(schoolId: String, collection: String, docId: String): Result<Unit> = runCatching {
        schoolsCol.document(schoolId).collection(collection).document(docId).delete().await()
        Unit
    }.onFailure { handleFirestoreError(it, OperationType.DELETE, "schools/$schoolId/$collection/$docId", auth) }

    private fun DocumentSnapshot.toAuthenticatedSession(): AuthenticatedSession? {
        val roleStr = getString("role") ?: return null
        val role = runCatching { UserRole.valueOf(roleStr) }.getOrNull() ?: return null
        return AuthenticatedSession(
            uid = getString("uid") ?: id,
            email = getString("email") ?: "",
            displayName = getString("name") ?: "",
            idSekolah = getString("idSekolah") ?: return null,
            role = role,
            idGuru = getString("idGuru") ?: "",
            idKelas = (get("idKelas") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            idMapel = (get("idMapel") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            studentId = getString("studentId") ?: "",
            className = getString("className") ?: ""
        )
    }

    private fun UserAccount.toUpdateMap(): Map<String, Any> = mapOf(
        "id" to id,
        "idSekolah" to idSekolah,
        "email" to email.trim().lowercase(),
        "name" to name.trim().ifBlank { "Staf" },
        "nip" to nip.trim(),
        "role" to role.name,
        "jabatan" to jabatan.trim(),
        "idGuru" to idGuru.trim(),
        "idKelas" to idKelas,
        "idMapel" to idMapel,
        "isVerified" to isVerified,
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun UserAccount.toCreateMap(): Map<String, Any> = toUpdateMap() + mapOf(
        "createdAt" to FieldValue.serverTimestamp()
    )

    private fun SchoolClass.toUpdateMap(): Map<String, Any> = mapOf(
        "id" to id,
        "idSekolah" to idSekolah,
        "name" to name.trim().ifBlank { "Kelas Baru" },
        "gradeLevel" to gradeLevel.coerceIn(1, 13),
        "homeroomTeacher" to homeroomTeacher.trim(),
        "room" to room.trim(),
        "studentCount" to studentCount.coerceIn(0, 100),
        "capacity" to capacity.coerceIn(1, 100),
        "pinKelas" to pinKelas.trim().ifBlank { "1234" }.let { if (it.length < 4) it.padEnd(4, '0') else it.take(12) },
        "pinUpdatedAt" to pinUpdatedAt.ifBlank { todayString() },
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun SchoolClass.toCreateMap(): Map<String, Any> = toUpdateMap() + mapOf(
        "createdAt" to FieldValue.serverTimestamp()
    )

    private fun SchoolClass.toPublicUpdateMap(): Map<String, Any> = mapOf(
        "id" to id,
        "idSekolah" to idSekolah,
        "name" to name.trim().ifBlank { "Kelas Baru" },
        "gradeLevel" to gradeLevel.coerceIn(1, 13),
        "homeroomTeacher" to homeroomTeacher.trim(),
        "room" to room.trim(),
        "studentCount" to studentCount.coerceIn(0, 100),
        "capacity" to capacity.coerceIn(1, 100),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun SchoolClass.toPublicCreateMap(): Map<String, Any> = toPublicUpdateMap() + mapOf(
        "createdAt" to FieldValue.serverTimestamp()
    )

    private fun Teacher.toUpdateMap(): Map<String, Any> = mapOf(
        "id" to id,
        "idSekolah" to idSekolah,
        "nip" to nip.trim(),
        "name" to name.trim(),
        "email" to email.trim().lowercase(),
        "phone" to phone.trim(),
        "subject" to subject.trim().ifBlank { "Umum" },
        "employmentStatus" to employmentStatus.trim().ifBlank { "PNS" },
        "jabatan" to jabatan.trim().ifBlank { "Guru Mata Pelajaran" },
        "skNumber" to skNumber.trim(),
        "skDate" to skDate.trim(),
        "tmtDate" to tmtDate.trim(),
        "tenureYears" to tenureYears.coerceIn(0, 60),
        "rankGroup" to if (employmentStatus.equals("PNS", ignoreCase = true)) rankGroup.trim() else "",
        "weeklyHours" to weeklyHours.coerceIn(0, 60),
        "classesTaught" to classesTaught.take(20),
        "address" to address.trim(),
        "photoBase64" to photoBase64,
        "skDocumentName" to skDocumentName.trim(),
        "skDocumentSizeBytes" to skDocumentSizeBytes.coerceIn(0L, 5_242_880L),
        "isActiveAccount" to isActiveAccount,
        "additionalDuty" to additionalDuty.trim(),
        "isNonSatmikal" to isNonSatmikal,
        "satmikalOrigin" to satmikalOrigin.trim(),
        "nonSatmikalVerified" to nonSatmikalVerified,
        "mutationStatus" to if (mutationStatus in listOf("NONE", "DIAJUKAN", "DISETUJUI", "DITOLAK")) mutationStatus else "NONE",
        "mutationTargetSchool" to mutationTargetSchool.trim(),
        "transferConfirmed" to transferConfirmed,
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun Teacher.toCreateMap(): Map<String, Any> = toUpdateMap() + mapOf(
        "createdAt" to FieldValue.serverTimestamp()
    )

    private fun Student.toUpdateMap(): Map<String, Any> = mapOf(
        "id" to id,
        "idSekolah" to idSekolah,
        "nisn" to nisn.trim().ifBlank { "00000000" },
        "nis" to nis.trim(),
        "name" to name.trim(),
        "className" to className.trim().ifBlank { "XII MIPA 1" },
        "gender" to if (gender == "P") "P" else "L",
        "religion" to religion.trim(),
        "phone" to phone.trim(),
        "address" to address.trim(),
        "birthDate" to birthDate.trim(),
        "parentName" to parentName.trim(),
        "parentPhone" to parentPhone.ifBlank { phone }.trim(),
        "promotionHistory" to promotionHistory.trim(),
        "status" to if (status in listOf("Aktif", "Pindah", "Lulus", "Dikeluarkan")) status else "Aktif",
        "userId" to userId,
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun Student.toCreateMap(): Map<String, Any> = toUpdateMap() + mapOf(
        "createdAt" to FieldValue.serverTimestamp()
    )

    private fun Subject.toUpdateMap(): Map<String, Any> = mapOf(
        "id" to id,
        "idSekolah" to idSekolah,
        "code" to code.trim().ifBlank { "MAPEL" },
        "name" to name.trim().ifBlank { "Mata Pelajaran" },
        "category" to category.trim().ifBlank { "Umum" },
        "kkm" to kkm.coerceIn(0, 100),
        "hoursPerWeek" to hoursPerWeek.coerceIn(1, 40),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun Subject.toCreateMap(): Map<String, Any> = toUpdateMap() + mapOf(
        "createdAt" to FieldValue.serverTimestamp()
    )

    private fun AttendanceRecord.toUpdateMap(): Map<String, Any> = mapOf(
        "id" to id,
        "idSekolah" to idSekolah,
        "date" to date.ifBlank { todayString() },
        "className" to className.ifBlank { "XII MIPA 1" },
        "studentId" to studentId,
        "studentName" to studentName,
        "status" to status.name,
        "notes" to notes,
        "teacherId" to teacherId,
        "subjectName" to subjectName,
        "studentUserId" to studentUserId,
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun AttendanceRecord.toCreateMap(): Map<String, Any> = toUpdateMap() + mapOf(
        "createdAt" to FieldValue.serverTimestamp()
    )

    private fun GradeRecord.toUpdateMap(): Map<String, Any> = mapOf(
        "id" to id,
        "idSekolah" to idSekolah,
        "studentId" to studentId.ifBlank { "S-01" },
        "studentName" to studentName.trim().ifBlank { "Siswa" },
        "className" to className.trim().ifBlank { "XII MIPA 1" },
        "subjectName" to subjectName.trim().ifBlank { "Umum" },
        "tugasScore" to tugasScore.coerceIn(0.0, 100.0),
        "utsScore" to utsScore.coerceIn(0.0, 100.0),
        "uasScore" to uasScore.coerceIn(0.0, 100.0),
        "teacherId" to teacherId,
        "studentUserId" to studentUserId,
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun GradeRecord.toCreateMap(): Map<String, Any> = toUpdateMap() + mapOf(
        "createdAt" to FieldValue.serverTimestamp()
    )

    private fun LessonPlan.toUpdateMap(): Map<String, Any> = mapOf(
        "id" to id,
        "idSekolah" to idSekolah,
        "title" to title.trim(),
        "curriculum" to curriculum.ifBlank { "Kurikulum Merdeka" },
        "subject" to subject.trim().ifBlank { "Umum" },
        "className" to className.trim().ifBlank { "XII MIPA 1" },
        "meetingCount" to meetingCount.coerceIn(1, 60),
        "duration" to duration.ifBlank { "2 x 45 Menit" },
        "learningObjectives" to learningObjectives,
        "fileName" to fileName.ifBlank { "modul.pdf" },
        "teacherId" to teacherId.ifBlank { "T-01" },
        "teacherName" to teacherName.ifBlank { "Guru" },
        "teacherEmail" to teacherEmail.trim().lowercase().ifBlank { "guru@school.id" },
        "userId" to userId,
        "reviewNotes" to reviewNotes,
        "fileSizeBytes" to fileSizeBytes,
        "storageUrl" to storageUrl,
        "status" to status.name,
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun LessonPlan.toCreateMap(): Map<String, Any> = toUpdateMap() + mapOf(
        "createdAt" to FieldValue.serverTimestamp()
    )

    private fun TeachingJournal.toUpdateMap(): Map<String, Any> = mapOf(
        "id" to id,
        "idSekolah" to idSekolah,
        "date" to date.ifBlank { todayString() },
        "teacherId" to teacherId.ifBlank { "T-01" },
        "teacherName" to teacherName.ifBlank { "Guru" },
        "userId" to userId,
        "subject" to subject.trim().ifBlank { "Umum" },
        "className" to className.trim().ifBlank { "XII MIPA 1" },
        "periodTime" to periodTime.ifBlank { "07:30 - 09:00" },
        "topic" to topic.trim(),
        "activities" to activities.trim(),
        "obstacles" to obstacles,
        "reflection" to reflection,
        "documentationFileName" to documentationFileName,
        "fileSizeBytes" to fileSizeBytes,
        "isCompleted" to isCompleted,
        "monthKey" to monthKey.ifBlank { date.take(7) },
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun TeachingJournal.toCreateMap(): Map<String, Any> = toUpdateMap() + mapOf(
        "createdAt" to FieldValue.serverTimestamp()
    )

    private fun StudentAssignment.toUpdateMap(): Map<String, Any> = mapOf(
        "id" to id,
        "idSekolah" to idSekolah,
        "className" to className.trim().ifBlank { "XII MIPA 1" },
        "subject" to subject.trim().ifBlank { "Umum" },
        "title" to title.trim(),
        "description" to description,
        "deadline" to deadline.ifBlank { "Terbuka" },
        "type" to if (type in listOf("Tugas", "Materi", "Latihan", "Uji Kompetensi")) type else "Tugas",
        "fileName" to fileName,
        "fileSizeBytes" to fileSizeBytes,
        "teacherId" to teacherId.ifBlank { "T-01" },
        "userId" to userId,
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun StudentAssignment.toCreateMap(): Map<String, Any> = toUpdateMap() + mapOf(
        "createdAt" to FieldValue.serverTimestamp()
    )

    private fun SchoolHoliday.toUpdateMap(): Map<String, Any> = mapOf(
        "id" to id,
        "idSekolah" to idSekolah,
        "date" to date.ifBlank { todayString() },
        "name" to name.trim(),
        "category" to category.ifBlank { "Libur Nasional" },
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun SchoolHoliday.toCreateMap(): Map<String, Any> = toUpdateMap() + mapOf(
        "createdAt" to FieldValue.serverTimestamp()
    )

    private fun SchoolAnnouncement.toUpdateMap(): Map<String, Any> = mapOf(
        "id" to id,
        "idSekolah" to idSekolah,
        "title" to title.trim(),
        "content" to content.trim(),
        "targetRole" to if (targetRole in listOf("Semua", "Guru", "Siswa")) targetRole else "Semua",
        "authorName" to authorName.ifBlank { "Admin Sekolah" },
        "publishedDate" to publishedDate.ifBlank { todayString() },
        "userId" to userId,
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun SchoolAnnouncement.toCreateMap(): Map<String, Any> = toUpdateMap() + mapOf(
        "createdAt" to FieldValue.serverTimestamp()
    )

    private fun LoginActivityLog.toCreateMap(): Map<String, Any> = mapOf(
        "id" to id,
        "idSekolah" to idSekolah,
        "uid" to uid,
        "email" to email,
        "displayName" to displayName,
        "role" to role,
        "timestamp" to timestamp,
        "deviceInfo" to deviceInfo,
        "status" to status,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun AppNotification.toCreateMap(): Map<String, Any> = mapOf(
        "id" to id,
        "idSekolah" to idSekolah,
        "recipientEmailOrRole" to recipientEmailOrRole,
        "title" to title,
        "message" to message,
        "timestamp" to timestamp.ifBlank { "Baru Saja" },
        "isRead" to isRead,
        "type" to type,
        "senderUid" to senderUid,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun PtkAttendanceRecord.toUpdateMap(): Map<String, Any> = mapOf(
        "id" to id,
        "idSekolah" to idSekolah,
        "teacherId" to teacherId.ifBlank { "T-01" },
        "teacherName" to teacherName.trim().ifBlank { "PTK" },
        "date" to date.ifBlank { todayString() },
        "status" to status.name,
        "notes" to notes.trim(),
        "recordedByUid" to recordedByUid,
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun PtkAttendanceRecord.toCreateMap(): Map<String, Any> = toUpdateMap() + mapOf(
        "createdAt" to FieldValue.serverTimestamp()
    )

    private fun TeachingSchedule.toUpdateMap(): Map<String, Any> = mapOf(
        "id" to id,
        "idSekolah" to idSekolah,
        "dayOfWeek" to if (dayOfWeek in listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu")) dayOfWeek else "Senin",
        "startTime" to startTime.ifBlank { "07:30" },
        "endTime" to endTime.ifBlank { "09:00" },
        "className" to className.trim().ifBlank { "XII MIPA 1" },
        "subjectName" to subjectName.trim().ifBlank { "Matematika" },
        "teacherId" to teacherId.ifBlank { "T-01" },
        "teacherName" to teacherName.trim().ifBlank { "Guru" },
        "room" to room.trim(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun TeachingSchedule.toCreateMap(): Map<String, Any> = toUpdateMap() + mapOf(
        "createdAt" to FieldValue.serverTimestamp()
    )

    private fun ForumMessage.toCreateMap(): Map<String, Any> = mapOf(
        "id" to id,
        "idSekolah" to idSekolah,
        "senderUid" to senderUid,
        "senderName" to senderName.trim().ifBlank { "Pengguna" },
        "senderRole" to if (senderRole == "SUPERADMIN") "SUPERADMIN" else "GURU",
        "messageType" to messageType.name,
        "textContent" to textContent.trim().ifBlank { if (messageType == ForumMessageType.VOICE_NOTE) "Voice-Note ($voiceDurationSec dtk)" else "Pesan" },
        "voiceBase64" to voiceBase64,
        "voiceDurationSec" to voiceDurationSec.coerceIn(0, 30),
        "sentDate" to sentDate.ifBlank { todayString() },
        "sentTime" to sentTime.ifBlank { SimpleDateFormat("HH:mm", Locale.US).format(Date()) },
        "isArchived" to isArchived,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun todayString(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
}
