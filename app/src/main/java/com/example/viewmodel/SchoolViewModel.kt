package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MockDataProvider
import com.example.data.PublicClassEntry
import com.example.data.SchoolDirectoryEntry
import com.example.data.SchoolFirestoreRepository
import com.example.model.*
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SchoolViewModel : ViewModel() {

    private var repository: SchoolFirestoreRepository? = null
    private var schoolListenersJob: Job? = null
    private var forumListenerJob: Job? = null
    private var directoryListenerJob: Job? = null
    private var publicClassesJob: Job? = null

    private val _isFirebaseSynced = MutableStateFlow(false)
    val isFirebaseSynced: StateFlow<Boolean> = _isFirebaseSynced.asStateFlow()

    private val _authenticatedSession = MutableStateFlow<AuthenticatedSession?>(null)
    val authenticatedSession: StateFlow<AuthenticatedSession?> = _authenticatedSession.asStateFlow()

    private val _isOnboardingRequired = MutableStateFlow(false)
    val isOnboardingRequired: StateFlow<Boolean> = _isOnboardingRequired.asStateFlow()

    private val _schoolDirectory = MutableStateFlow<List<SchoolDirectoryEntry>>(emptyList())
    val schoolDirectory: StateFlow<List<SchoolDirectoryEntry>> = _schoolDirectory.asStateFlow()

    private val _publicClassesForOnboarding = MutableStateFlow<List<PublicClassEntry>>(emptyList())
    val publicClassesForOnboarding: StateFlow<List<PublicClassEntry>> = _publicClassesForOnboarding.asStateFlow()

    private val _tenants = MutableStateFlow(MockDataProvider.tenantSchools)
    val tenants: StateFlow<List<TenantSchool>> = _tenants.asStateFlow()

    private val _currentTenant = MutableStateFlow(MockDataProvider.tenantSchools.first())
    val currentTenant: StateFlow<TenantSchool> = _currentTenant.asStateFlow()

    private val _currentRole = MutableStateFlow(UserRole.SUPERADMIN)
    val currentRole: StateFlow<UserRole> = _currentRole.asStateFlow()

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _proSubscription = MutableStateFlow(ProSubscription(isPro = false))
    val proSubscription: StateFlow<ProSubscription> = _proSubscription.asStateFlow()

    private val _schoolProfiles = MutableStateFlow(MockDataProvider.schoolProfiles)
    val currentSchoolProfile: StateFlow<SchoolProfile> = combine(_currentTenant, _schoolProfiles) { tenant, profiles ->
        profiles[tenant.idSekolah] ?: MockDataProvider.schoolProfiles["SCH-001"]!!
    }.stateIn(viewModelScope, SharingStarted.Eagerly, MockDataProvider.schoolProfiles["SCH-001"]!!)

    private val _userAccounts = MutableStateFlow(MockDataProvider.initialUserAccounts())
    val userAccounts: StateFlow<List<UserAccount>> = combine(_currentTenant, _userAccounts) { tenant, accounts ->
        accounts.filter { it.idSekolah == tenant.idSekolah }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _currentUserAccount = MutableStateFlow<UserAccount?>(MockDataProvider.initialUserAccounts().first())
    val currentUserAccount: StateFlow<UserAccount?> = _currentUserAccount.asStateFlow()

    private val _currentStudentSession = MutableStateFlow<Student?>(null)
    val currentStudentSession: StateFlow<Student?> = _currentStudentSession.asStateFlow()

    private val _teachers = MutableStateFlow(MockDataProvider.initialTeachers())
    val activeTeachers: StateFlow<List<Teacher>> = combine(_currentTenant, _teachers) { tenant, list ->
        list.filter { it.idSekolah == tenant.idSekolah }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _students = MutableStateFlow(MockDataProvider.initialStudents())
    val activeStudents: StateFlow<List<Student>> = combine(_currentTenant, _students) { tenant, list ->
        list.filter { it.idSekolah == tenant.idSekolah }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _classes = MutableStateFlow(MockDataProvider.initialClasses())
    val activeClasses: StateFlow<List<SchoolClass>> = combine(_currentTenant, _classes) { tenant, list ->
        list.filter { it.idSekolah == tenant.idSekolah }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _subjects = MutableStateFlow(MockDataProvider.initialSubjects())
    val activeSubjects: StateFlow<List<Subject>> = combine(_currentTenant, _subjects) { tenant, list ->
        list.filter { it.idSekolah == tenant.idSekolah }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _attendance = MutableStateFlow(MockDataProvider.initialAttendance())
    val activeAttendance: StateFlow<List<AttendanceRecord>> = combine(_currentTenant, _attendance) { tenant, list ->
        list.filter { it.idSekolah == tenant.idSekolah }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _grades = MutableStateFlow(MockDataProvider.initialGrades())
    val activeGrades: StateFlow<List<GradeRecord>> = combine(_currentTenant, _grades) { tenant, list ->
        list.filter { it.idSekolah == tenant.idSekolah }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _lessonPlans = MutableStateFlow(MockDataProvider.initialLessonPlans())
    val activeLessonPlans: StateFlow<List<LessonPlan>> = combine(_currentTenant, _lessonPlans) { tenant, list ->
        list.filter { it.idSekolah == tenant.idSekolah }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _teachingJournals = MutableStateFlow(MockDataProvider.initialTeachingJournals())
    val activeTeachingJournals: StateFlow<List<TeachingJournal>> = combine(_currentTenant, _teachingJournals) { tenant, list ->
        list.filter { it.idSekolah == tenant.idSekolah }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _assignments = MutableStateFlow(MockDataProvider.initialAssignments())
    val activeAssignments: StateFlow<List<StudentAssignment>> = combine(_currentTenant, _assignments) { tenant, list ->
        list.filter { it.idSekolah == tenant.idSekolah }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _holidays = MutableStateFlow(MockDataProvider.initialHolidays())
    val activeHolidays: StateFlow<List<SchoolHoliday>> = combine(_currentTenant, _holidays) { tenant, list ->
        list.filter { it.idSekolah == tenant.idSekolah }.sortedBy { it.date }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _announcements = MutableStateFlow(MockDataProvider.initialAnnouncements())
    val activeAnnouncements: StateFlow<List<SchoolAnnouncement>> = combine(_currentTenant, _announcements) { tenant, list ->
        list.filter { it.idSekolah == tenant.idSekolah }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _loginLogs = MutableStateFlow(MockDataProvider.initialLoginLogs())
    val activeLoginLogs: StateFlow<List<LoginActivityLog>> = combine(_currentTenant, _loginLogs) { tenant, list ->
        list.filter { it.idSekolah == tenant.idSekolah }.sortedByDescending { it.timestamp }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _notifications = MutableStateFlow(MockDataProvider.initialNotifications())
    val allNotifications: List<AppNotification> get() = _notifications.value
    val notifications: StateFlow<List<AppNotification>> = combine(_currentTenant, _notifications) { tenant, list ->
        list.filter { it.idSekolah == tenant.idSekolah }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val unreadNotificationCount: StateFlow<Int> = combine(_currentTenant, _notifications) { tenant, list ->
        list.count { !it.isRead && it.idSekolah == tenant.idSekolah }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    private val _ptkAttendance = MutableStateFlow(MockDataProvider.initialPtkAttendance())
    val activePtkAttendance: StateFlow<List<PtkAttendanceRecord>> = combine(_currentTenant, _ptkAttendance) { tenant, list ->
        list.filter { it.idSekolah == tenant.idSekolah }.sortedByDescending { it.date }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _teachingSchedules = MutableStateFlow(MockDataProvider.initialTeachingSchedules())
    val activeTeachingSchedules: StateFlow<List<TeachingSchedule>> = combine(_currentTenant, _teachingSchedules) { tenant, list ->
        val dayOrder = mapOf("Senin" to 1, "Selasa" to 2, "Rabu" to 3, "Kamis" to 4, "Jumat" to 5, "Sabtu" to 6)
        list.filter { it.idSekolah == tenant.idSekolah }
            .sortedWith(compareBy({ dayOrder[it.dayOfWeek] ?: 99 }, { it.startTime }))
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _forumMessageLimit = MutableStateFlow(25L)
    val forumMessageLimit: StateFlow<Long> = _forumMessageLimit.asStateFlow()

    private val _forumMessages = MutableStateFlow(MockDataProvider.initialForumMessages())
    val forumMessages: StateFlow<List<ForumMessage>> = combine(_currentTenant, _forumMessages) { tenant, list ->
        list.filter { it.idSekolah == tenant.idSekolah }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Guru-scoped auto-filtered streams (id_guru, id_kelas, id_mapel)
    val guruFilteredLessonPlans: StateFlow<List<LessonPlan>> = combine(
        activeLessonPlans, _currentUserAccount, _authenticatedSession
    ) { list, acc, session ->
        val email = (acc?.email ?: session?.email ?: "").lowercase()
        val idGuru = acc?.idGuru?.ifBlank { session?.idGuru ?: "" } ?: (session?.idGuru ?: "")
        val classes = acc?.idKelas?.ifEmpty { session?.idKelas ?: emptyList() } ?: (session?.idKelas ?: emptyList())
        val subjects = acc?.idMapel?.ifEmpty { session?.idMapel ?: emptyList() } ?: (session?.idMapel ?: emptyList())
        if (idGuru.isBlank() && email.isBlank() && classes.isEmpty() && subjects.isEmpty()) {
            list
        } else {
            list.filter { rpp ->
                val matchTeacher = (idGuru.isNotBlank() && rpp.teacherId == idGuru) ||
                    (email.isNotBlank() && rpp.teacherEmail.equals(email, ignoreCase = true))
                val matchClass = classes.isEmpty() || classes.any { it.equals(rpp.className, ignoreCase = true) }
                val matchSubject = subjects.isEmpty() || subjects.any { it.equals(rpp.subject, ignoreCase = true) }
                matchTeacher || (classes.isNotEmpty() && matchClass && matchSubject)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val guruFilteredJournals: StateFlow<List<TeachingJournal>> = combine(
        activeTeachingJournals, _currentUserAccount, _authenticatedSession
    ) { list, acc, session ->
        val idGuru = acc?.idGuru?.ifBlank { session?.idGuru ?: "" } ?: (session?.idGuru ?: "")
        val name = acc?.name ?: session?.displayName ?: ""
        val classes = acc?.idKelas?.ifEmpty { session?.idKelas ?: emptyList() } ?: (session?.idKelas ?: emptyList())
        if (idGuru.isBlank() && name.isBlank() && classes.isEmpty()) {
            list
        } else {
            list.filter { j ->
                (idGuru.isNotBlank() && j.teacherId == idGuru) ||
                    (name.isNotBlank() && j.teacherName.equals(name, ignoreCase = true)) ||
                    (classes.isNotEmpty() && classes.any { it.equals(j.className, ignoreCase = true) })
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val guruFilteredAttendance: StateFlow<List<AttendanceRecord>> = combine(
        activeAttendance, _currentUserAccount, _authenticatedSession
    ) { list, acc, session ->
        val classes = acc?.idKelas?.ifEmpty { session?.idKelas ?: emptyList() } ?: (session?.idKelas ?: emptyList())
        if (classes.isEmpty()) list else list.filter { a -> classes.any { it.equals(a.className, ignoreCase = true) } }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val guruFilteredGrades: StateFlow<List<GradeRecord>> = combine(
        activeGrades, _currentUserAccount, _authenticatedSession
    ) { list, acc, session ->
        val classes = acc?.idKelas?.ifEmpty { session?.idKelas ?: emptyList() } ?: (session?.idKelas ?: emptyList())
        val subjects = acc?.idMapel?.ifEmpty { session?.idMapel ?: emptyList() } ?: (session?.idMapel ?: emptyList())
        if (classes.isEmpty() && subjects.isEmpty()) {
            list
        } else {
            list.filter { g ->
                (classes.isEmpty() || classes.any { it.equals(g.className, ignoreCase = true) }) &&
                    (subjects.isEmpty() || subjects.any { it.equals(g.subjectName, ignoreCase = true) })
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val guruFilteredAssignments: StateFlow<List<StudentAssignment>> = combine(
        activeAssignments, _currentUserAccount, _authenticatedSession
    ) { list, acc, session ->
        val classes = acc?.idKelas?.ifEmpty { session?.idKelas ?: emptyList() } ?: (session?.idKelas ?: emptyList())
        val subjects = acc?.idMapel?.ifEmpty { session?.idMapel ?: emptyList() } ?: (session?.idMapel ?: emptyList())
        if (classes.isEmpty() && subjects.isEmpty()) {
            list
        } else {
            list.filter { a ->
                (classes.isEmpty() || classes.any { it.equals(a.className, ignoreCase = true) }) &&
                    (subjects.isEmpty() || subjects.any { it.equals(a.subject, ignoreCase = true) })
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val guruFilteredSchedules: StateFlow<List<TeachingSchedule>> = combine(
        activeTeachingSchedules, _currentUserAccount, _authenticatedSession
    ) { list, acc, session ->
        val idGuru = acc?.idGuru?.ifBlank { session?.idGuru ?: "" } ?: (session?.idGuru ?: "")
        val name = acc?.name ?: session?.displayName ?: ""
        val classes = acc?.idKelas?.ifEmpty { session?.idKelas ?: emptyList() } ?: (session?.idKelas ?: emptyList())
        if (idGuru.isBlank() && name.isBlank() && classes.isEmpty()) {
            list
        } else {
            list.filter { s ->
                (idGuru.isNotBlank() && s.teacherId == idGuru) ||
                    (name.isNotBlank() && s.teacherName.equals(name, ignoreCase = true)) ||
                    (classes.isNotEmpty() && classes.any { it.equals(s.className, ignoreCase = true) })
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // =========================================================================
    // FIREBASE ATTACHMENT & AUTH STATE SYNC
    // =========================================================================

    fun attachFirebaseRepository(repo: SchoolFirestoreRepository) {
        if (repository == null) {
            repository = repo
        }
    }

    fun onFirebaseAuthStateChanged(user: FirebaseUser?) {
        val repo = repository ?: return
        schoolListenersJob?.cancel()
        forumListenerJob?.cancel()
        directoryListenerJob?.cancel()
        publicClassesJob?.cancel()

        if (user == null) {
            _isFirebaseSynced.value = false
            _authenticatedSession.value = null
            _isOnboardingRequired.value = false
            return
        }

        // Start non-PII school directory observer for onboarding
        directoryListenerJob = viewModelScope.launch {
            repo.observeSchoolDirectory().collect { dir ->
                _schoolDirectory.value = dir
            }
        }

        viewModelScope.launch {
            _isLoading.value = true
            val result = repo.discoverSession()
            _isLoading.value = false
            result.onSuccess { session ->
                if (session != null) {
                    applyAuthenticatedSession(session, repo)
                } else {
                    _authenticatedSession.value = null
                    _isOnboardingRequired.value = true
                    _isFirebaseSynced.value = false
                }
            }.onFailure { err ->
                _errorMessage.value = err.localizedMessage ?: "Gagal memuat sesi sekolah dari Firestore."
                _isOnboardingRequired.value = true
            }
        }
    }

    fun loadPublicClassesForSchool(schoolId: String) {
        val repo = repository ?: return
        publicClassesJob?.cancel()
        publicClassesJob = viewModelScope.launch {
            repo.observeClassesPublic(schoolId).collect { list ->
                _publicClassesForOnboarding.value = list
            }
        }
    }

    fun registerNewSchoolInFirebase(
        tenant: TenantSchool,
        profile: SchoolProfile,
        seedStarterData: Boolean
    ) {
        val repo = repository ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val res = repo.registerNewSchool(tenant, profile, seedStarterData)
            _isLoading.value = false
            res.onSuccess { session ->
                _toastMessage.value = "Sekolah '${tenant.name}' (${tenant.idSekolah}) berhasil disinkronkan ke Firebase!"
                applyAuthenticatedSession(session, repo)
            }.onFailure { err ->
                _errorMessage.value = err.localizedMessage ?: "Gagal mendaftarkan lembaga ke Firestore."
            }
        }
    }

    fun verifyStudentPinInFirebase(
        schoolId: String,
        classId: String,
        className: String,
        studentName: String,
        pin: String
    ) {
        val repo = repository ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val res = repo.verifyStudentPinAndJoin(schoolId, classId, className, studentName, pin)
            _isLoading.value = false
            res.onSuccess { session ->
                _toastMessage.value = "PIN Kelas valid! Selamat datang, ${session.displayName}"
                applyAuthenticatedSession(session, repo)
            }.onFailure { err ->
                _errorMessage.value = err.localizedMessage ?: "PIN Kelas tidak valid."
            }
        }
    }

    fun forceRefreshSync(user: FirebaseUser?) {
        onFirebaseAuthStateChanged(user)
        _toastMessage.value = "Menyinkronkan ulang data dengan Cloud Firestore..."
    }

    private fun applyAuthenticatedSession(session: AuthenticatedSession, repo: SchoolFirestoreRepository) {
        _authenticatedSession.value = session
        _isOnboardingRequired.value = false
        _currentRole.value = session.role
        _isFirebaseSynced.value = true

        if (session.role == UserRole.SISWA) {
            _currentStudentSession.value = Student(
                id = session.studentId.ifBlank { "S-SESSION" },
                idSekolah = session.idSekolah,
                nisn = "0078129099",
                name = session.displayName,
                className = session.className,
                gender = "L",
                religion = "Islam",
                phone = "-",
                address = "Terdaftar via PIN Kelas",
                userId = session.uid
            )
            _currentUserAccount.value = null
        } else {
            _currentStudentSession.value = null
            _currentUserAccount.value = UserAccount(
                id = session.uid,
                idSekolah = session.idSekolah,
                email = session.email,
                name = session.displayName,
                nip = "-",
                role = session.role,
                jabatan = session.role.label,
                idGuru = session.idGuru,
                idKelas = session.idKelas,
                idMapel = session.idMapel
            )
        }

        startSchoolListeners(repo, session)
    }

    private fun startSchoolListeners(repo: SchoolFirestoreRepository, session: AuthenticatedSession) {
        schoolListenersJob?.cancel()
        val schoolId = session.idSekolah
        val role = session.role

        schoolListenersJob = viewModelScope.launch {
            // Owned schools list (for Admin multi-tenant switcher)
            launch {
                repo.observeOwnedSchools(session.uid).collect { owned ->
                    if (owned.isNotEmpty()) {
                        _tenants.value = owned
                        val matched = owned.firstOrNull { it.idSekolah == schoolId } ?: owned.first()
                        _currentTenant.value = matched
                    }
                }
            }
            // School profile + Pro/Ad config
            launch {
                repo.observeSchoolProfileAndConfig(schoolId).collect { pair ->
                    if (pair != null) {
                        val (profile, sub) = pair
                        val profiles = _schoolProfiles.value.toMutableMap()
                        profiles[schoolId] = profile
                        _schoolProfiles.value = profiles
                        _proSubscription.value = sub

                        val tenantItem = TenantSchool(
                            idSekolah = profile.idSekolah,
                            name = profile.name,
                            npsn = profile.npsn,
                            level = profile.level,
                            city = _currentTenant.value.city.ifBlank { "Indonesia" }
                        )
                        _currentTenant.value = tenantItem
                        val updatedTenants = _tenants.value.toMutableList()
                        val idx = updatedTenants.indexOfFirst { it.idSekolah == schoolId }
                        if (idx >= 0) updatedTenants[idx] = tenantItem else updatedTenants.add(tenantItem)
                        _tenants.value = updatedTenants
                    }
                }
            }
            // Staff whitelist (Admin sees all; Staff sees own verified email doc)
            if (role != UserRole.SISWA) {
                launch {
                    repo.observeStaffWhitelist(schoolId, role, session.email).collect { accounts ->
                        if (accounts.isNotEmpty() || role == UserRole.SUPERADMIN) {
                            _userAccounts.value = accounts
                            accounts.firstOrNull { it.email.equals(session.email, ignoreCase = true) }?.let {
                                _currentUserAccount.value = it
                            }
                        }
                    }
                }
                launch {
                    repo.observeTeachers(schoolId).collect { _teachers.value = it }
                }
                launch {
                    repo.observeLessonPlans(schoolId).collect { _lessonPlans.value = it }
                }
                launch {
                    repo.observeTeachingJournals(schoolId).collect { _teachingJournals.value = it }
                }
                launch {
                    repo.observeNotifications(schoolId, role, session.email).collect { _notifications.value = it }
                }
                launch {
                    repo.observePtkAttendance(schoolId).collect { _ptkAttendance.value = it }
                }
                launch {
                    repo.observeTeachingSchedules(schoolId).collect { _teachingSchedules.value = it }
                }
            } else {
                _lessonPlans.value = emptyList()
                _teachingJournals.value = emptyList()
                _teachers.value = emptyList()
                _notifications.value = emptyList()
                _ptkAttendance.value = emptyList()
                _teachingSchedules.value = emptyList()
            }

            if (role == UserRole.SUPERADMIN || role == UserRole.GURU) {
                subscribeForumListener(repo, schoolId, _forumMessageLimit.value)
            } else {
                forumListenerJob?.cancel()
                _forumMessages.value = emptyList()
            }
            // Shared / role-filtered collections
            launch {
                repo.observeClasses(schoolId, role).collect { _classes.value = it }
            }
            launch {
                repo.observeStudents(schoolId, role, session.uid).collect { _students.value = it }
            }
            launch {
                repo.observeSubjects(schoolId).collect { _subjects.value = it }
            }
            launch {
                repo.observeAttendance(schoolId, role, session.uid).collect { _attendance.value = it }
            }
            launch {
                repo.observeGrades(schoolId, role, session.uid).collect { _grades.value = it }
            }
            launch {
                repo.observeAssignments(schoolId).collect { _assignments.value = it }
            }
            launch {
                repo.observeHolidays(schoolId).collect { _holidays.value = it }
            }
            launch {
                repo.observeAnnouncements(schoolId).collect { _announcements.value = it }
            }
            if (role == UserRole.SUPERADMIN) {
                launch {
                    repo.observeLoginLogs(schoolId).collect { _loginLogs.value = it }
                }
            } else {
                _loginLogs.value = emptyList()
            }
        }
    }

    // =========================================================================
    // TENANT & ROLE MANAGEMENT
    // =========================================================================

    fun switchTenant(idSekolah: String) {
        val found = _tenants.value.firstOrNull { it.idSekolah == idSekolah } ?: return
        _currentTenant.value = found
        _toastMessage.value = "Berganti ke sekolah: ${found.name}"
        val repo = repository
        val session = _authenticatedSession.value
        if (repo != null && session != null && session.role == UserRole.SUPERADMIN) {
            val updatedSession = session.copy(idSekolah = idSekolah)
            _authenticatedSession.value = updatedSession
            startSchoolListeners(repo, updatedSession)
        }
    }

    fun saveTenant(
        tenant: TenantSchool,
        akreditasi: String = "A (Unggul)",
        kepalaSekolah: String = "Kepala Sekolah",
        address: String = "Jl. Pendidikan No. 1",
        onSuccess: () -> Unit
    ) {
        if (tenant.name.isBlank()) {
            _errorMessage.value = "Nama lembaga tidak boleh kosong"
            return
        }
        val currentTenants = _tenants.value.toMutableList()
        val profiles = _schoolProfiles.value.toMutableMap()
        val existingIndex = currentTenants.indexOfFirst { it.idSekolah == tenant.idSekolah }

        val targetProfile = if (existingIndex >= 0) {
            currentTenants[existingIndex] = tenant
            val existingProfile = profiles[tenant.idSekolah]
            val updated = (existingProfile ?: SchoolProfile(
                idSekolah = tenant.idSekolah,
                name = tenant.name,
                npsn = tenant.npsn,
                akreditasi = akreditasi,
                level = tenant.level,
                address = address,
                email = "info@${tenant.idSekolah.lowercase()}.sch.id",
                phone = "(021) 1234567",
                website = "https://${tenant.idSekolah.lowercase()}.sch.id",
                kepalaSekolah = kepalaSekolah,
                vision = "Mewujudkan lembaga pendidikan ${tenant.name} yang unggul, berakhlak mulia, dan berprestasi.",
                mission = "1. Memberikan layanan pendidikan terbaik dan bermutu tinggi.\n2. Memfasilitasi pengembangan talenta, sains, dan teknologi."
            )).copy(
                name = tenant.name,
                npsn = tenant.npsn,
                level = tenant.level,
                akreditasi = akreditasi,
                kepalaSekolah = if (kepalaSekolah.isNotBlank()) kepalaSekolah else existingProfile?.kepalaSekolah ?: "Kepala Sekolah",
                address = if (address.isNotBlank()) address else existingProfile?.address ?: "Jl. Pendidikan No. 1"
            )
            profiles[tenant.idSekolah] = updated
            _toastMessage.value = "Lembaga '${tenant.name}' berhasil diperbarui"
            updated
        } else {
            currentTenants.add(tenant)
            val created = SchoolProfile(
                idSekolah = tenant.idSekolah,
                name = tenant.name,
                npsn = tenant.npsn,
                akreditasi = akreditasi,
                level = tenant.level,
                address = address,
                email = "info@${tenant.idSekolah.lowercase().replace("-", "")}.sch.id",
                phone = "(021) 1234567",
                website = "https://${tenant.idSekolah.lowercase().replace("-", "")}.sch.id",
                kepalaSekolah = kepalaSekolah,
                vision = "Mewujudkan lembaga pendidikan ${tenant.name} yang unggul, berakhlak mulia, dan berprestasi.",
                mission = "1. Memberikan layanan pendidikan terbaik dan bermutu tinggi.\n2. Memfasilitasi pengembangan talenta, sains, dan teknologi."
            )
            profiles[tenant.idSekolah] = created
            _toastMessage.value = "Lembaga baru '${tenant.name}' berhasil dibuat!"
            created
        }

        _tenants.value = currentTenants
        _schoolProfiles.value = profiles
        _currentTenant.value = tenant

        val repo = repository
        if (repo != null && _authenticatedSession.value != null) {
            viewModelScope.launch {
                _isLoading.value = true
                if (existingIndex >= 0) {
                    repo.updateSchoolProfileAndTenant(tenant, targetProfile)
                        .onFailure { _errorMessage.value = it.localizedMessage }
                } else {
                    repo.registerNewSchool(tenant, targetProfile, seedStarterData = true)
                        .onSuccess { session -> applyAuthenticatedSession(session, repo) }
                        .onFailure { _errorMessage.value = it.localizedMessage }
                }
                _isLoading.value = false
                onSuccess()
            }
        } else {
            viewModelScope.launch {
                _isLoading.value = true
                delay(50)
                _isLoading.value = false
                onSuccess()
            }
        }
    }

    fun deleteTenant(idSekolah: String) {
        if (_tenants.value.size <= 1) {
            _errorMessage.value = "Tidak dapat menghapus lembaga terakhir"
            return
        }
        val currentTenants = _tenants.value.toMutableList()
        val item = currentTenants.firstOrNull { it.idSekolah == idSekolah } ?: return
        currentTenants.removeAll { it.idSekolah == idSekolah }
        _tenants.value = currentTenants

        val profiles = _schoolProfiles.value.toMutableMap()
        profiles.remove(idSekolah)
        _schoolProfiles.value = profiles

        if (_currentTenant.value.idSekolah == idSekolah) {
            _currentTenant.value = currentTenants.first()
        }
        _toastMessage.value = "Lembaga '${item.name}' telah dihapus"
    }

    fun switchRole(newRole: UserRole) {
        // Strict Role Guard when Firebase authenticated session is non-Admin
        val session = _authenticatedSession.value
        if (session != null && session.role != UserRole.SUPERADMIN && newRole != session.role) {
            _errorMessage.value = "Akses ditolak: Anda login sebagai ${session.role.label} dan tidak dapat membuka dashboard ${newRole.label}."
            return
        }
        _currentRole.value = newRole
        _toastMessage.value = "Role diganti: ${newRole.label}"
    }

    fun loginWithGoogle(
        email: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val registered = _userAccounts.value.firstOrNull {
            it.email.equals(email.trim(), ignoreCase = true) && it.idSekolah == _currentTenant.value.idSekolah
        }

        if (registered != null) {
            _currentUserAccount.value = registered
            _currentRole.value = registered.role
            _currentStudentSession.value = null
            _toastMessage.value = "Login berhasil! Selamat datang, ${registered.name} (${registered.jabatan})"
            onSuccess()
        } else {
            val error = "Email '$email' belum didaftarkan oleh Admin Sekolah. Hubungi Admin Utama untuk didaftarkan ke Firestore terlebih dahulu."
            _errorMessage.value = error
            onError(error)
        }
    }

    fun loginStudentWithPin(
        studentName: String,
        className: String,
        pin: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val matchedClass = _classes.value.firstOrNull {
            it.name.equals(className.trim(), ignoreCase = true) && it.idSekolah == _currentTenant.value.idSekolah
        }

        if (matchedClass == null) {
            val error = "Kelas '$className' tidak ditemukan di ${_currentTenant.value.name}."
            _errorMessage.value = error
            onError(error)
            return
        }

        val repo = repository
        if (repo != null && _authenticatedSession.value != null) {
            viewModelScope.launch {
                _isLoading.value = true
                repo.verifyStudentPinAndJoin(_currentTenant.value.idSekolah, matchedClass.id, matchedClass.name, studentName, pin)
                    .onSuccess { session ->
                        applyAuthenticatedSession(session, repo)
                        _isLoading.value = false
                        onSuccess()
                    }
                    .onFailure { err ->
                        _isLoading.value = false
                        val msg = err.localizedMessage ?: "PIN Kelas salah."
                        _errorMessage.value = msg
                        onError(msg)
                    }
            }
            return
        }

        if (matchedClass.pinKelas != pin.trim()) {
            val error = "PIN Kelas salah untuk kelas $className! Hubungi Wali Kelas atau Admin Sekolah."
            _errorMessage.value = error
            onError(error)
            return
        }

        val studentRecord = _students.value.firstOrNull {
            it.name.contains(studentName.trim(), ignoreCase = true) && it.className == className
        } ?: Student(
            id = "S-SESSION",
            idSekolah = _currentTenant.value.idSekolah,
            nisn = "0078129099",
            name = studentName,
            className = className,
            gender = "L",
            religion = "Islam",
            phone = "08129900112",
            address = "Siswa Terdaftar"
        )

        _currentStudentSession.value = studentRecord
        _currentRole.value = UserRole.SISWA
        _currentUserAccount.value = null
        _toastMessage.value = "Login Siswa Berhasil: ${studentRecord.name} ($className)"
        onSuccess()
    }

    fun registerStaffAccount(account: UserAccount, onSuccess: () -> Unit) {
        if (account.name.isBlank() || account.email.isBlank()) {
            _errorMessage.value = "Nama dan email wajib diisi"
            return
        }
        val normalized = account.copy(email = account.email.trim().lowercase())
        viewModelScope.launch {
            _isLoading.value = true
            val repo = repository
            if (repo != null && _authenticatedSession.value != null) {
                repo.saveStaffWhitelist(normalized)
                    .onSuccess { _toastMessage.value = "Akun ${normalized.name} disinkronkan ke Firestore Whitelist" }
                    .onFailure { _errorMessage.value = it.localizedMessage }
            } else {
                val list = _userAccounts.value.toMutableList()
                val existingIndex = list.indexOfFirst { it.id == normalized.id || it.email.equals(normalized.email, ignoreCase = true) }
                if (existingIndex >= 0) {
                    list[existingIndex] = normalized
                    _toastMessage.value = "Akun ${normalized.name} berhasil diperbarui"
                } else {
                    list.add(0, normalized)
                    _toastMessage.value = "Akun baru ${normalized.name} (${normalized.role.label}) didaftarkan"
                }
                _userAccounts.value = list
            }
            _isLoading.value = false
            onSuccess()
        }
    }

    fun deleteStaffAccount(userId: String) {
        val schoolId = _currentTenant.value.idSekolah
        val list = _userAccounts.value.toMutableList()
        val item = list.firstOrNull { it.id == userId } ?: return
        list.removeAll { it.id == userId }
        _userAccounts.value = list
        repository?.let { repo ->
            if (_authenticatedSession.value != null) {
                viewModelScope.launch { repo.deleteStaffWhitelist(schoolId, userId) }
            }
        }
        _toastMessage.value = "Akun ${item.name} dihapus dari Firestore"
    }

    fun updateClassPin(classId: String, newPin: String, onSuccess: () -> Unit) {
        if (newPin.length < 4) {
            _errorMessage.value = "PIN kelas minimal 4 karakter/angka"
            return
        }
        val list = _classes.value.toMutableList()
        val index = list.indexOfFirst { it.id == classId }
        if (index >= 0) {
            val c = list[index]
            val updated = c.copy(pinKelas = newPin, pinUpdatedAt = "2026-11-12")
            list[index] = updated
            _classes.value = list

            val notif = AppNotification(
                id = "NOTIF-${System.currentTimeMillis() % 100000}",
                idSekolah = _currentTenant.value.idSekolah,
                recipientEmailOrRole = "GURU",
                title = "PIN Kelas Diperbarui",
                message = "PIN untuk kelas ${c.name} telah diganti menjadi '$newPin'. Mohon infokan kepada siswa kelas bersangkutan.",
                timestamp = "Hari Ini",
                isRead = false,
                type = "PIN_ALERT"
            )
            _notifications.value = listOf(notif) + _notifications.value

            repository?.let { repo ->
                if (_authenticatedSession.value != null) {
                    viewModelScope.launch {
                        repo.saveClassWithPin(updated)
                        repo.createNotification(notif)
                    }
                }
            }
            _toastMessage.value = "PIN Kelas ${c.name} berhasil diperbarui ke '$newPin'"
            onSuccess()
        }
    }

    // =========================================================================
    // RPP REVIEW & APPROVAL WORKFLOW DENGAN NOTIFIKASI REAL-TIME
    // =========================================================================

    fun submitRppForReview(rppId: String) {
        val list = _lessonPlans.value.toMutableList()
        val index = list.indexOfFirst { it.id == rppId }
        if (index >= 0) {
            val old = list[index]
            val updated = old.copy(status = RppStatus.SUBMITTED)
            list[index] = updated
            _lessonPlans.value = list

            val notif = AppNotification(
                id = "NOTIF-${System.currentTimeMillis() % 100000}",
                idSekolah = _currentTenant.value.idSekolah,
                recipientEmailOrRole = "KEPALA_SEKOLAH",
                title = "Pengajuan RPP Baru",
                message = "Guru ${old.teacherName} mengajukan RPP '${old.title}' (${old.subject}) untuk ditinjau.",
                timestamp = "Baru Saja",
                isRead = false,
                type = "RPP_SUBMITTED"
            )
            _notifications.value = listOf(notif) + _notifications.value

            repository?.let { repo ->
                if (_authenticatedSession.value != null) {
                    viewModelScope.launch {
                        repo.saveLessonPlan(updated)
                        repo.createNotification(notif)
                    }
                }
            }
            _toastMessage.value = "RPP '${old.title}' dikirim ke Kepala Sekolah untuk persetujuan"
        }
    }

    fun approveRppByKepsek(rppId: String, notes: String) {
        val list = _lessonPlans.value.toMutableList()
        val index = list.indexOfFirst { it.id == rppId }
        if (index >= 0) {
            val old = list[index]
            list[index] = old.copy(status = RppStatus.APPROVED, reviewNotes = notes)
            _lessonPlans.value = list

            val notif = AppNotification(
                id = "NOTIF-${System.currentTimeMillis() % 100000}",
                idSekolah = _currentTenant.value.idSekolah,
                recipientEmailOrRole = old.teacherEmail.ifBlank { "GURU" },
                title = "RPP Disetujui Kepala Sekolah",
                message = "RPP '${old.title}' telah DISETUJUI oleh Kepala Sekolah. Catatan: '$notes'",
                timestamp = "Baru Saja",
                isRead = false,
                type = "RPP_APPROVED"
            )
            _notifications.value = listOf(notif) + _notifications.value

            repository?.let { repo ->
                if (_authenticatedSession.value != null) {
                    viewModelScope.launch {
                        repo.reviewLessonPlanByKepsek(old.idSekolah, rppId, RppStatus.APPROVED, notes)
                            .onFailure { _errorMessage.value = it.localizedMessage }
                        repo.createNotification(notif)
                    }
                }
            }
            _toastMessage.value = "RPP '${old.title}' telah di-Approve!"
        }
    }

    fun rejectRppByKepsek(rppId: String, notes: String) {
        val list = _lessonPlans.value.toMutableList()
        val index = list.indexOfFirst { it.id == rppId }
        if (index >= 0) {
            val old = list[index]
            list[index] = old.copy(status = RppStatus.REJECTED, reviewNotes = notes)
            _lessonPlans.value = list

            val notif = AppNotification(
                id = "NOTIF-${System.currentTimeMillis() % 100000}",
                idSekolah = _currentTenant.value.idSekolah,
                recipientEmailOrRole = old.teacherEmail.ifBlank { "GURU" },
                title = "RPP Perlu Revisi (Ditolak)",
                message = "RPP '${old.title}' memerlukan perbaikan. Catatan Kepsek: '$notes'",
                timestamp = "Baru Saja",
                isRead = false,
                type = "RPP_REJECTED"
            )
            _notifications.value = listOf(notif) + _notifications.value

            repository?.let { repo ->
                if (_authenticatedSession.value != null) {
                    viewModelScope.launch {
                        repo.reviewLessonPlanByKepsek(old.idSekolah, rppId, RppStatus.REJECTED, notes)
                            .onFailure { _errorMessage.value = it.localizedMessage }
                        repo.createNotification(notif)
                    }
                }
            }
            _toastMessage.value = "RPP '${old.title}' berstatus Perlu Revisi"
        }
    }

    fun getLessonPlan(rppId: String): LessonPlan? = _lessonPlans.value.firstOrNull { it.id == rppId }

    fun markAllNotificationsRead() {
        val unreadIds = _notifications.value.filter { !it.isRead && it.idSekolah == _currentTenant.value.idSekolah }.map { it.id }
        _notifications.value = _notifications.value.map { it.copy(isRead = true) }
        repository?.let { repo ->
            if (_authenticatedSession.value != null) {
                viewModelScope.launch {
                    unreadIds.forEach { repo.markNotificationRead(_currentTenant.value.idSekolah, it) }
                }
            }
        }
    }

    fun markNotificationRead(id: String) {
        _notifications.value = _notifications.value.map { if (it.id == id) it.copy(isRead = true) else it }
        repository?.let { repo ->
            if (_authenticatedSession.value != null) {
                viewModelScope.launch { repo.markNotificationRead(_currentTenant.value.idSekolah, id) }
            }
        }
    }

    fun validateUploadFileSize(fileSizeBytes: Long): Boolean {
        val maxAllowed = 5 * 1024 * 1024L
        if (fileSizeBytes > maxAllowed) {
            _errorMessage.value = "Gagal upload: Ukuran file (${String.format(java.util.Locale.US, "%.2f", fileSizeBytes.toDouble() / (1024 * 1024))} MB) melebihi batas maksimal 5 MB!"
            return false
        }
        return true
    }

    fun markNotificationAsRead(id: String) = markNotificationRead(id)

    fun clearAllNotifications() {
        markAllNotificationsRead()
        _toastMessage.value = "Semua notifikasi ditandai telah dibaca"
    }

    // =========================================================================
    // CRUD MUTATIONS (LOCAL + FIRESTORE SYNC)
    // =========================================================================

    fun saveAssignment(assignment: StudentAssignment, onSuccess: () -> Unit) {
        if (!validateUploadFileSize(assignment.fileSizeBytes)) return
        val session = _authenticatedSession.value
        val enriched = assignment.copy(
            userId = assignment.userId.ifBlank { session?.uid ?: "" },
            teacherId = assignment.teacherId.ifBlank { session?.idGuru?.ifBlank { "T-01" } ?: "T-01" }
        )
        val list = _assignments.value.toMutableList()
        val index = list.indexOfFirst { it.id == enriched.id }
        if (index >= 0) {
            list[index] = enriched
            _toastMessage.value = "Materi/Tugas diperbarui"
        } else {
            list.add(0, enriched)
            _toastMessage.value = "Materi/Tugas baru ditambahkan untuk kelas ${enriched.className}"
        }
        _assignments.value = list
        repository?.let { repo ->
            if (session != null) {
                viewModelScope.launch {
                    repo.saveAssignment(enriched).onFailure { _errorMessage.value = it.localizedMessage }
                }
            }
        }
        onSuccess()
    }

    fun deleteAssignment(id: String) {
        val list = _assignments.value.toMutableList()
        list.removeAll { it.id == id }
        _assignments.value = list
        repository?.let { repo ->
            if (_authenticatedSession.value != null) {
                viewModelScope.launch { repo.deleteAssignment(_currentTenant.value.idSekolah, id) }
            }
        }
        _toastMessage.value = "Tugas/Materi berhasil dihapus"
    }

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun togglePro() {
        val nextPro = !_proSubscription.value.isPro
        val updated = _proSubscription.value.copy(
            isPro = nextPro,
            storageMaxMb = if (nextPro) 51200 else 500
        )
        _proSubscription.value = updated
        repository?.let { repo ->
            if (_authenticatedSession.value != null) {
                viewModelScope.launch { repo.updateProAndAdConfig(_currentTenant.value.idSekolah, updated) }
            }
        }
        _toastMessage.value = if (nextPro) "Akun PRO Aktif! Iklan dinonaktifkan & Storage 50GB" else "Kembali ke Akun Free"
    }

    fun selectAdNetwork(adNetwork: AdNetwork) {
        val updated = _proSubscription.value.copy(activeAdNetwork = adNetwork)
        _proSubscription.value = updated
        repository?.let { repo ->
            if (_authenticatedSession.value != null) {
                viewModelScope.launch { repo.updateProAndAdConfig(_currentTenant.value.idSekolah, updated) }
            }
        }
        _toastMessage.value = "Jaringan iklan diperbarui ke ${adNetwork.displayName}"
    }

    fun clearToast() { _toastMessage.value = null }
    fun clearError() { _errorMessage.value = null }
    fun setSearchQuery(query: String) { _searchQuery.value = query }

    fun updateAppearanceSettings(
        schoolName: String,
        npsn: String,
        address: String,
        phone: String,
        vision: String,
        mission: String,
        organizationStructure: String,
        logoBase64: String,
        logoFileName: String,
        logoSizeBytes: Long,
        themePreset: ThemePreset,
        onSuccess: () -> Unit
    ) {
        if (_currentRole.value != UserRole.SUPERADMIN) {
            _errorMessage.value = "Hanya Admin Sekolah yang diizinkan mengubah tampilan & identitas lembaga."
            return
        }
        if (schoolName.isBlank()) {
            _errorMessage.value = "Nama lembaga/sekolah tidak boleh kosong"
            return
        }
        if (!validateUploadFileSize(logoSizeBytes)) return

        val existing = currentSchoolProfile.value
        val updatedProfile = existing.copy(
            name = schoolName.trim(),
            npsn = npsn.trim().ifBlank { existing.npsn },
            address = address.trim().ifBlank { existing.address },
            phone = phone.trim().ifBlank { existing.phone },
            vision = vision.trim().ifBlank { existing.vision },
            mission = mission.trim().ifBlank { existing.mission },
            organizationStructure = organizationStructure.trim().ifBlank { existing.organizationStructure },
            logoBase64 = logoBase64,
            logoFileName = logoFileName,
            logoSizeBytes = logoSizeBytes,
            themePreset = themePreset
        )
        val updatedTenant = _currentTenant.value.copy(
            name = updatedProfile.name,
            npsn = updatedProfile.npsn
        )
        val profiles = _schoolProfiles.value.toMutableMap()
        profiles[updatedProfile.idSekolah] = updatedProfile
        _schoolProfiles.value = profiles
        _currentTenant.value = updatedTenant
        _tenants.value = _tenants.value.map {
            if (it.idSekolah == updatedTenant.idSekolah) updatedTenant else it
        }

        val repo = repository
        if (repo != null && _authenticatedSession.value != null) {
            viewModelScope.launch {
                _isLoading.value = true
                repo.updateSchoolProfileAndTenant(updatedTenant, updatedProfile)
                    .onSuccess {
                        _toastMessage.value = "Tampilan, Logo & Identitas '${updatedProfile.name}' diterapkan ke seluruh dashboard!"
                        onSuccess()
                    }
                    .onFailure { _errorMessage.value = it.localizedMessage ?: "Gagal menyimpan tampilan ke Firestore" }
                _isLoading.value = false
            }
        } else {
            _toastMessage.value = "Tampilan, Logo & Identitas '${updatedProfile.name}' disimpan!"
            onSuccess()
        }
    }

    fun updateMasterAcademicConfig(academicYear: String, semester: String, onSuccess: () -> Unit) {
        if (academicYear.isBlank()) {
            _errorMessage.value = "Tahun ajaran tidak boleh kosong"
            return
        }
        val existing = currentSchoolProfile.value
        val updated = existing.copy(
            academicYear = academicYear.trim(),
            semester = semester.trim().ifBlank { "Ganjil" }
        )
        val profiles = _schoolProfiles.value.toMutableMap()
        profiles[updated.idSekolah] = updated
        _schoolProfiles.value = profiles

        repository?.let { repo ->
            if (_authenticatedSession.value != null) {
                viewModelScope.launch {
                    repo.updateMasterAcademicConfig(updated.idSekolah, updated.academicYear, updated.semester)
                        .onFailure { _errorMessage.value = it.localizedMessage }
                }
            }
        }
        _toastMessage.value = "Tahun Ajaran ${updated.academicYear} (Semester ${updated.semester}) disimpan"
        onSuccess()
    }

    fun saveHoliday(holiday: SchoolHoliday, onSuccess: () -> Unit) {
        if (holiday.name.isBlank() || holiday.date.isBlank()) {
            _errorMessage.value = "Tanggal dan nama hari libur wajib diisi"
            return
        }
        val list = _holidays.value.toMutableList()
        val idx = list.indexOfFirst { it.id == holiday.id }
        if (idx >= 0) list[idx] = holiday else list.add(0, holiday)
        _holidays.value = list
        repository?.let { repo ->
            if (_authenticatedSession.value != null) {
                viewModelScope.launch { repo.saveHoliday(holiday) }
            }
        }
        _toastMessage.value = "Hari libur '${holiday.name}' disimpan"
        onSuccess()
    }

    fun deleteHoliday(id: String) {
        _holidays.value = _holidays.value.filterNot { it.id == id }
        repository?.let { repo ->
            if (_authenticatedSession.value != null) {
                viewModelScope.launch { repo.deleteHoliday(_currentTenant.value.idSekolah, id) }
            }
        }
        _toastMessage.value = "Hari libur dihapus"
    }

    fun saveAnnouncement(announcement: SchoolAnnouncement, onSuccess: () -> Unit) {
        if (announcement.title.isBlank() || announcement.content.isBlank()) {
            _errorMessage.value = "Judul dan isi pengumuman wajib diisi"
            return
        }
        val session = _authenticatedSession.value
        val enriched = announcement.copy(
            userId = announcement.userId.ifBlank { session?.uid ?: "" },
            authorName = announcement.authorName.ifBlank { _currentUserAccount.value?.name ?: session?.displayName ?: "Admin Sekolah" }
        )
        val list = _announcements.value.toMutableList()
        val idx = list.indexOfFirst { it.id == enriched.id }
        if (idx >= 0) list[idx] = enriched else list.add(0, enriched)
        _announcements.value = list
        repository?.let { repo ->
            if (session != null) {
                viewModelScope.launch { repo.saveAnnouncement(enriched) }
            }
        }
        _toastMessage.value = "Pengumuman '${enriched.title}' dipublikasikan"
        onSuccess()
    }

    fun deleteAnnouncement(id: String) {
        _announcements.value = _announcements.value.filterNot { it.id == id }
        repository?.let { repo ->
            if (_authenticatedSession.value != null) {
                viewModelScope.launch { repo.deleteAnnouncement(_currentTenant.value.idSekolah, id) }
            }
        }
        _toastMessage.value = "Pengumuman dihapus"
    }

    fun deleteLoginLog(id: String) {
        _loginLogs.value = _loginLogs.value.filterNot { it.id == id }
        repository?.let { repo ->
            if (_authenticatedSession.value != null) {
                viewModelScope.launch { repo.deleteLoginLog(_currentTenant.value.idSekolah, id) }
            }
        }
        _toastMessage.value = "Log aktivitas login dihapus"
    }

    fun updateSchoolProfile(profile: SchoolProfile, onSuccess: () -> Unit) {
        if (profile.name.isBlank()) {
            _errorMessage.value = "Nama sekolah tidak boleh kosong"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            val currentMap = _schoolProfiles.value.toMutableMap()
            currentMap[profile.idSekolah] = profile
            _schoolProfiles.value = currentMap

            val updatedTenant = _currentTenant.value.copy(
                name = profile.name,
                npsn = profile.npsn,
                level = profile.level
            )
            _currentTenant.value = updatedTenant
            _tenants.value = _tenants.value.map {
                if (it.idSekolah == updatedTenant.idSekolah) updatedTenant else it
            }

            val repo = repository
            if (repo != null && _authenticatedSession.value != null) {
                repo.updateSchoolProfileAndTenant(updatedTenant, profile)
                    .onFailure { _errorMessage.value = it.localizedMessage }
            }
            _isLoading.value = false
            _toastMessage.value = "Profil sekolah berhasil diperbarui"
            onSuccess()
        }
    }

    fun saveTeacher(teacher: Teacher, onSuccess: () -> Unit) {
        if (teacher.name.isBlank()) {
            _errorMessage.value = "Nama guru tidak boleh kosong"
            return
        }
        if (!teacher.email.contains("@")) {
            _errorMessage.value = "Format email guru tidak valid"
            return
        }
        if (!validateUploadFileSize(teacher.skDocumentSizeBytes)) return
        val normalizedEmail = teacher.email.trim().lowercase()
        val cleanTeacher = teacher.copy(
            email = normalizedEmail,
            rankGroup = if (teacher.employmentStatus.equals("PNS", ignoreCase = true)) teacher.rankGroup else ""
        )
        val list = _teachers.value.toMutableList()
        val existingIndex = list.indexOfFirst { it.id == cleanTeacher.id }
        if (existingIndex >= 0) {
            list[existingIndex] = cleanTeacher
            _toastMessage.value = "Data guru ${cleanTeacher.name} diperbarui"
        } else {
            list.add(0, cleanTeacher)
            _toastMessage.value = "Guru baru ${cleanTeacher.name} ditambahkan"
        }
        _teachers.value = list

        // Mirror staff_whitelist locally for instant role/filter feedback
        val wlId = "WL-${cleanTeacher.id}"
        val accounts = _userAccounts.value.toMutableList()
        if (cleanTeacher.isActiveAccount) {
            val wlAccount = UserAccount(
                id = wlId,
                idSekolah = cleanTeacher.idSekolah,
                email = normalizedEmail,
                name = cleanTeacher.name,
                nip = cleanTeacher.nip,
                role = UserRole.GURU,
                jabatan = cleanTeacher.jabatan.ifBlank { "Guru Mata Pelajaran" },
                isVerified = true,
                idGuru = cleanTeacher.id,
                idKelas = cleanTeacher.classesTaught,
                idMapel = listOf(cleanTeacher.subject).filter { it.isNotBlank() }
            )
            val wlIdx = accounts.indexOfFirst { it.id == wlId || it.email.equals(normalizedEmail, ignoreCase = true) }
            if (wlIdx >= 0) accounts[wlIdx] = wlAccount else accounts.add(wlAccount)
        } else {
            accounts.removeAll { it.id == wlId }
        }
        _userAccounts.value = accounts

        repository?.let { repo ->
            if (_authenticatedSession.value != null) {
                viewModelScope.launch { repo.saveTeacher(cleanTeacher) }
            }
        }
        onSuccess()
    }

    fun deleteTeacher(id: String) {
        val current = _teachers.value.toMutableList()
        val item = current.firstOrNull { it.id == id } ?: return
        current.removeAll { it.id == id }
        _teachers.value = current
        repository?.let { repo ->
            if (_authenticatedSession.value != null) {
                viewModelScope.launch { repo.deleteTeacher(_currentTenant.value.idSekolah, id) }
            }
        }
        _toastMessage.value = "Guru ${item.name} berhasil dihapus"
    }

    fun saveStudent(student: Student, onSuccess: () -> Unit) {
        if (student.name.isBlank()) {
            _errorMessage.value = "Nama siswa tidak boleh kosong"
            return
        }
        if (student.nisn.length < 8) {
            _errorMessage.value = "NISN minimal 8 digit angka"
            return
        }
        val list = _students.value.toMutableList()
        val existingIndex = list.indexOfFirst { it.id == student.id }
        if (existingIndex >= 0) {
            list[existingIndex] = student
            _toastMessage.value = "Data siswa ${student.name} diperbarui"
        } else {
            list.add(0, student)
            _toastMessage.value = "Siswa baru ${student.name} ditambahkan"
        }
        _students.value = list
        repository?.let { repo ->
            if (_authenticatedSession.value != null) {
                viewModelScope.launch { repo.saveStudent(student) }
            }
        }
        onSuccess()
    }

    fun deleteStudent(id: String) {
        val current = _students.value.toMutableList()
        val item = current.firstOrNull { it.id == id } ?: return
        current.removeAll { it.id == id }
        _students.value = current
        repository?.let { repo ->
            if (_authenticatedSession.value != null) {
                viewModelScope.launch { repo.deleteStudent(_currentTenant.value.idSekolah, id) }
            }
        }
        _toastMessage.value = "Siswa ${item.name} berhasil dihapus"
    }

    fun saveClass(schoolClass: SchoolClass, onSuccess: () -> Unit) {
        if (schoolClass.name.isBlank()) {
            _errorMessage.value = "Nama kelas tidak boleh kosong"
            return
        }
        val list = _classes.value.toMutableList()
        val existingIndex = list.indexOfFirst { it.id == schoolClass.id }
        if (existingIndex >= 0) {
            list[existingIndex] = schoolClass
            _toastMessage.value = "Kelas ${schoolClass.name} diperbarui"
        } else {
            list.add(schoolClass)
            _toastMessage.value = "Kelas baru ${schoolClass.name} ditambahkan"
        }
        _classes.value = list
        repository?.let { repo ->
            if (_authenticatedSession.value != null) {
                viewModelScope.launch { repo.saveClassWithPin(schoolClass) }
            }
        }
        onSuccess()
    }

    fun deleteClass(id: String) {
        val current = _classes.value.toMutableList()
        val existing = current.firstOrNull { it.id == id }
        current.removeAll { it.id == id }
        _classes.value = current
        repository?.let { repo ->
            if (_authenticatedSession.value != null) {
                viewModelScope.launch { repo.deleteClass(_currentTenant.value.idSekolah, id, existing?.pinKelas ?: "") }
            }
        }
        _toastMessage.value = "Kelas berhasil dihapus"
    }

    fun saveSubject(subject: Subject, onSuccess: () -> Unit) {
        if (subject.name.isBlank() || subject.code.isBlank()) {
            _errorMessage.value = "Kode dan nama mata pelajaran harus diisi"
            return
        }
        val list = _subjects.value.toMutableList()
        val existingIndex = list.indexOfFirst { it.id == subject.id }
        if (existingIndex >= 0) {
            list[existingIndex] = subject
            _toastMessage.value = "Mapel ${subject.name} diperbarui"
        } else {
            list.add(subject)
            _toastMessage.value = "Mapel baru ${subject.name} ditambahkan"
        }
        _subjects.value = list
        repository?.let { repo ->
            if (_authenticatedSession.value != null) {
                viewModelScope.launch { repo.saveSubject(subject) }
            }
        }
        onSuccess()
    }

    fun deleteSubject(id: String) {
        val current = _subjects.value.toMutableList()
        current.removeAll { it.id == id }
        _subjects.value = current
        repository?.let { repo ->
            if (_authenticatedSession.value != null) {
                viewModelScope.launch { repo.deleteSubject(_currentTenant.value.idSekolah, id) }
            }
        }
        _toastMessage.value = "Mata pelajaran berhasil dihapus"
    }

    fun toggleAttendanceStatus(recordId: String, newStatus: AttendanceStatus) {
        val list = _attendance.value.toMutableList()
        val index = list.indexOfFirst { it.id == recordId }
        if (index >= 0) {
            val old = list[index]
            val updated = old.copy(status = newStatus)
            list[index] = updated
            _attendance.value = list
            repository?.let { repo ->
                if (_authenticatedSession.value != null) {
                    viewModelScope.launch { repo.saveAttendance(updated) }
                }
            }
            _toastMessage.value = "Status ${old.studentName} diubah ke ${newStatus.label}"
        }
    }

    fun markAllPresent(className: String) {
        val list = _attendance.value.toMutableList()
        val updatedItems = mutableListOf<AttendanceRecord>()
        list.indices.forEach { i ->
            if (list[i].className == className && list[i].idSekolah == _currentTenant.value.idSekolah) {
                val updated = list[i].copy(status = AttendanceStatus.HADIR)
                list[i] = updated
                updatedItems.add(updated)
            }
        }
        _attendance.value = list
        repository?.let { repo ->
            if (_authenticatedSession.value != null) {
                viewModelScope.launch { updatedItems.forEach { repo.saveAttendance(it) } }
            }
        }
        _toastMessage.value = "${updatedItems.size} siswa di kelas $className ditandai Hadir"
    }

    fun saveGrade(grade: GradeRecord, onSuccess: () -> Unit) {
        if (grade.tugasScore !in 0.0..100.0 || grade.utsScore !in 0.0..100.0 || grade.uasScore !in 0.0..100.0) {
            _errorMessage.value = "Rentang nilai harus antara 0 sampai 100"
            return
        }
        val list = _grades.value.toMutableList()
        val index = list.indexOfFirst { it.id == grade.id }
        if (index >= 0) {
            list[index] = grade
            _toastMessage.value = "Nilai ${grade.studentName} berhasil diperbarui"
        } else {
            list.add(0, grade)
            _toastMessage.value = "Nilai siswa berhasil diinput"
        }
        _grades.value = list
        repository?.let { repo ->
            if (_authenticatedSession.value != null) {
                viewModelScope.launch { repo.saveGrade(grade) }
            }
        }
        onSuccess()
    }

    fun deleteGrade(id: String) {
        val current = _grades.value.toMutableList()
        current.removeAll { it.id == id }
        _grades.value = current
        repository?.let { repo ->
            if (_authenticatedSession.value != null) {
                viewModelScope.launch { repo.deleteGrade(_currentTenant.value.idSekolah, id) }
            }
        }
        _toastMessage.value = "Rekap nilai berhasil dihapus"
    }

    fun saveLessonPlan(rpp: LessonPlan, onSuccess: () -> Unit) {
        if (rpp.title.isBlank() || rpp.subject.isBlank()) {
            _errorMessage.value = "Judul dan mata pelajaran RPP harus diisi"
            return
        }
        if (!validateUploadFileSize(rpp.fileSizeBytes)) {
            return
        }
        val session = _authenticatedSession.value
        val enriched = rpp.copy(
            userId = rpp.userId.ifBlank { session?.uid ?: "" },
            teacherId = rpp.teacherId.ifBlank { session?.idGuru?.ifBlank { "T-01" } ?: "T-01" },
            teacherName = rpp.teacherName.ifBlank { _currentUserAccount.value?.name ?: session?.displayName ?: "Guru" },
            teacherEmail = rpp.teacherEmail.ifBlank { _currentUserAccount.value?.email ?: session?.email ?: "guru@school.id" }
        )
        val list = _lessonPlans.value.toMutableList()
        val index = list.indexOfFirst { it.id == enriched.id }
        if (index >= 0) {
            list[index] = enriched
            _toastMessage.value = "RPP ${enriched.title} diperbarui"
        } else {
            list.add(0, enriched)
            _toastMessage.value = "RPP baru berhasil disimpan ke Cloud"
        }
        _lessonPlans.value = list
        repository?.let { repo ->
            if (session != null) {
                viewModelScope.launch {
                    repo.saveLessonPlan(enriched).onFailure { _errorMessage.value = it.localizedMessage }
                }
            }
        }
        onSuccess()
    }

    fun updateRppStatus(id: String, newStatus: RppStatus) {
        val list = _lessonPlans.value.toMutableList()
        val index = list.indexOfFirst { it.id == id }
        if (index >= 0) {
            val old = list[index]
            val updated = old.copy(status = newStatus)
            list[index] = updated
            _lessonPlans.value = list
            repository?.let { repo ->
                if (_authenticatedSession.value != null) {
                    viewModelScope.launch { repo.saveLessonPlan(updated) }
                }
            }
            _toastMessage.value = "Status RPP diubah menjadi ${newStatus.label}"
        }
    }

    fun deleteLessonPlan(id: String) {
        val current = _lessonPlans.value.toMutableList()
        current.removeAll { it.id == id }
        _lessonPlans.value = current
        repository?.let { repo ->
            if (_authenticatedSession.value != null) {
                viewModelScope.launch { repo.deleteLessonPlan(_currentTenant.value.idSekolah, id) }
            }
        }
        _toastMessage.value = "Dokumen RPP berhasil dihapus"
    }

    fun saveTeachingJournal(journal: TeachingJournal, onSuccess: () -> Unit) {
        if (journal.topic.isBlank() || journal.activities.isBlank()) {
            _errorMessage.value = "Topik materi dan kegiatan mengajar wajib diisi"
            return
        }
        val session = _authenticatedSession.value
        val enriched = journal.copy(
            userId = journal.userId.ifBlank { session?.uid ?: "" },
            teacherId = journal.teacherId.ifBlank { session?.idGuru?.ifBlank { "T-01" } ?: "T-01" },
            teacherName = journal.teacherName.ifBlank { _currentUserAccount.value?.name ?: session?.displayName ?: "Guru" }
        )
        val list = _teachingJournals.value.toMutableList()
        val index = list.indexOfFirst { it.id == enriched.id }
        if (index >= 0) {
            list[index] = enriched
            _toastMessage.value = "Jurnal mengajar diperbarui"
        } else {
            list.add(0, enriched)
            _toastMessage.value = "Jurnal mengajar baru dicatat"
        }
        _teachingJournals.value = list
        repository?.let { repo ->
            if (session != null) {
                viewModelScope.launch {
                    repo.saveTeachingJournal(enriched).onFailure { _errorMessage.value = it.localizedMessage }
                }
            }
        }
        onSuccess()
    }

    fun deleteTeachingJournal(id: String) {
        val current = _teachingJournals.value.toMutableList()
        current.removeAll { it.id == id }
        _teachingJournals.value = current
        repository?.let { repo ->
            if (_authenticatedSession.value != null) {
                viewModelScope.launch { repo.deleteTeachingJournal(_currentTenant.value.idSekolah, id) }
            }
        }
        _toastMessage.value = "Catatan jurnal mengajar dihapus"
    }

    // =========================================================================
    // PTK MANAGEMENT (TUGAS TAMBAHAN, NON SATMIKAL, MUTASI, ALIH GURU, PRESENSI PTK)
    // =========================================================================

    fun updatePtkExtraDuty(teacherId: String, duty: String, onSuccess: () -> Unit = {}) {
        if (_currentRole.value != UserRole.SUPERADMIN) {
            _errorMessage.value = "Hanya Admin Sekolah yang dapat mengatur tugas tambahan PTK."
            return
        }
        val teacher = _teachers.value.firstOrNull { it.id == teacherId } ?: return
        val updated = teacher.copy(additionalDuty = duty.trim())
        saveTeacher(updated) {
            _toastMessage.value = "Tugas tambahan ${updated.name} disimpan"
            onSuccess()
        }
    }

    fun verifyNonSatmikal(teacherId: String, isNonSatmikal: Boolean, verified: Boolean, satmikalOrigin: String, onSuccess: () -> Unit = {}) {
        if (_currentRole.value != UserRole.SUPERADMIN) {
            _errorMessage.value = "Hanya Admin Sekolah yang dapat memverifikasi Non Satmikal."
            return
        }
        val teacher = _teachers.value.firstOrNull { it.id == teacherId } ?: return
        val updated = teacher.copy(
            isNonSatmikal = isNonSatmikal,
            nonSatmikalVerified = if (isNonSatmikal) verified else false,
            satmikalOrigin = if (isNonSatmikal) satmikalOrigin.trim() else ""
        )
        saveTeacher(updated) {
            _toastMessage.value = if (verified) "Non-Satmikal ${updated.name} terverifikasi" else "Status Non-Satmikal ${updated.name} diperbarui"
            onSuccess()
        }
    }

    fun confirmGuruTransfer(teacherId: String, confirmed: Boolean) {
        if (_currentRole.value != UserRole.SUPERADMIN) {
            _errorMessage.value = "Hanya Admin Sekolah yang dapat mengonfirmasi Alih Guru."
            return
        }
        val teacher = _teachers.value.firstOrNull { it.id == teacherId } ?: return
        val updated = teacher.copy(transferConfirmed = confirmed)
        saveTeacher(updated) {
            _toastMessage.value = if (confirmed) "Konfirmasi Alih Guru ${updated.name} disetujui" else "Konfirmasi Alih Guru dibatalkan"
        }
    }

    fun updateTeacherMutation(teacherId: String, mutationStatus: String, targetSchool: String, onSuccess: () -> Unit = {}) {
        if (_currentRole.value != UserRole.SUPERADMIN) {
            _errorMessage.value = "Hanya Admin Sekolah yang dapat memproses pengajuan mutasi."
            return
        }
        val teacher = _teachers.value.firstOrNull { it.id == teacherId } ?: return
        val updated = teacher.copy(
            mutationStatus = mutationStatus,
            mutationTargetSchool = targetSchool.trim(),
            transferConfirmed = if (mutationStatus == "DISETUJUI") true else teacher.transferConfirmed
        )
        saveTeacher(updated) {
            _toastMessage.value = "Status mutasi ${updated.name}: $mutationStatus"
            onSuccess()
        }
    }

    fun toggleTeacherAccountActive(teacherId: String, active: Boolean) {
        if (_currentRole.value != UserRole.SUPERADMIN) {
            _errorMessage.value = "Hanya Admin Sekolah yang dapat mengubah status aktif akun PTK."
            return
        }
        val teacher = _teachers.value.firstOrNull { it.id == teacherId } ?: return
        val updated = teacher.copy(isActiveAccount = active)
        saveTeacher(updated) {
            _toastMessage.value = "Akun PTK ${updated.name}: ${if (active) "Aktif" else "Non-Aktif"}"
        }
    }

    fun savePtkAttendance(record: PtkAttendanceRecord, onSuccess: () -> Unit = {}) {
        if (_currentRole.value != UserRole.SUPERADMIN) {
            _errorMessage.value = "Hanya Admin Sekolah yang dapat mencatat presensi PTK."
            return
        }
        if (record.teacherName.isBlank() || record.date.isBlank()) {
            _errorMessage.value = "Nama PTK dan tanggal presensi wajib diisi."
            return
        }
        val session = _authenticatedSession.value
        val enriched = record.copy(recordedByUid = record.recordedByUid.ifBlank { session?.uid ?: "admin" })
        val list = _ptkAttendance.value.toMutableList()
        val idx = list.indexOfFirst { it.id == enriched.id }
        if (idx >= 0) list[idx] = enriched else list.add(0, enriched)
        _ptkAttendance.value = list
        repository?.let { repo ->
            if (session != null) {
                viewModelScope.launch {
                    repo.savePtkAttendance(enriched).onFailure { _errorMessage.value = it.localizedMessage }
                }
            }
        }
        _toastMessage.value = "Presensi PTK ${enriched.teacherName} (${enriched.status.label}) disimpan"
        onSuccess()
    }

    fun deletePtkAttendance(id: String) {
        if (_currentRole.value != UserRole.SUPERADMIN) return
        _ptkAttendance.value = _ptkAttendance.value.filterNot { it.id == id }
        repository?.let { repo ->
            if (_authenticatedSession.value != null) {
                viewModelScope.launch { repo.deletePtkAttendance(_currentTenant.value.idSekolah, id) }
            }
        }
        _toastMessage.value = "Catatan presensi PTK dihapus"
    }

    // =========================================================================
    // KALENDER AKADEMIK + JADWAL MENGAJAR
    // =========================================================================

    fun saveTeachingSchedule(schedule: TeachingSchedule, onSuccess: () -> Unit = {}) {
        if (_currentRole.value != UserRole.SUPERADMIN) {
            _errorMessage.value = "Hanya Admin Sekolah yang dapat mengelola jadwal mengajar."
            return
        }
        if (schedule.className.isBlank() || schedule.subjectName.isBlank() || schedule.teacherName.isBlank()) {
            _errorMessage.value = "Kelas, Mata Pelajaran, dan Guru pengajar wajib diisi."
            return
        }
        val list = _teachingSchedules.value.toMutableList()
        val idx = list.indexOfFirst { it.id == schedule.id }
        if (idx >= 0) {
            list[idx] = schedule
            _toastMessage.value = "Jadwal ${schedule.dayOfWeek} (${schedule.className}) diperbarui"
        } else {
            list.add(schedule)
            _toastMessage.value = "Jadwal mengajar baru ditambahkan"
        }
        _teachingSchedules.value = list
        repository?.let { repo ->
            if (_authenticatedSession.value != null) {
                viewModelScope.launch {
                    repo.saveTeachingSchedule(schedule).onFailure { _errorMessage.value = it.localizedMessage }
                }
            }
        }
        onSuccess()
    }

    fun deleteTeachingSchedule(id: String) {
        if (_currentRole.value != UserRole.SUPERADMIN) return
        _teachingSchedules.value = _teachingSchedules.value.filterNot { it.id == id }
        repository?.let { repo ->
            if (_authenticatedSession.value != null) {
                viewModelScope.launch { repo.deleteTeachingSchedule(_currentTenant.value.idSekolah, id) }
            }
        }
        _toastMessage.value = "Jadwal mengajar dihapus"
    }

    // =========================================================================
    // FORUM DISKUSI LEMBAGA (CHAT + VOICE-OVER <= 30 DETIK ONLY)
    // =========================================================================

    private fun subscribeForumListener(repo: SchoolFirestoreRepository, schoolId: String, limit: Long) {
        forumListenerJob?.cancel()
        forumListenerJob = viewModelScope.launch {
            repo.observeForumMessages(schoolId, limit).collect { msgs ->
                _forumMessages.value = msgs
            }
        }
    }

    fun loadOlderForumMessages() {
        val nextLimit = (_forumMessageLimit.value + 25L).coerceAtMost(200L)
        _forumMessageLimit.value = nextLimit
        val repo = repository
        val session = _authenticatedSession.value
        if (repo != null && session != null && (session.role == UserRole.SUPERADMIN || session.role == UserRole.GURU)) {
            subscribeForumListener(repo, session.idSekolah, nextLimit)
        }
        _toastMessage.value = "Memuat hingga $nextLimit pesan forum (Lazy Load Kuota)"
    }

    fun sendForumTextMessage(text: String, onSuccess: () -> Unit = {}) {
        val role = _currentRole.value
        if (role != UserRole.SUPERADMIN && role != UserRole.GURU) {
            _errorMessage.value = "Akses ditolak: Forum Diskusi hanya untuk Admin dan Guru."
            return
        }
        val cleanText = text.trim()
        if (cleanText.isBlank()) {
            _errorMessage.value = "Pesan teks tidak boleh kosong."
            return
        }
        if (cleanText.length > 1000) {
            _errorMessage.value = "Pesan maksimal 1000 karakter untuk menjaga kuota Firebase."
            return
        }
        val session = _authenticatedSession.value
        val senderName = _currentUserAccount.value?.name ?: session?.displayName ?: role.label
        val nowDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        val nowTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.US).format(java.util.Date())
        val msgId = "FRM-${System.currentTimeMillis()}"
        val msg = ForumMessage(
            id = msgId,
            idSekolah = _currentTenant.value.idSekolah,
            senderUid = session?.uid ?: if (role == UserRole.SUPERADMIN) "admin-local" else "guru-local",
            senderName = senderName,
            senderRole = role.name,
            messageType = ForumMessageType.TEXT,
            textContent = cleanText,
            voiceBase64 = "",
            voiceDurationSec = 0,
            sentDate = nowDate,
            sentTime = nowTime,
            isArchived = false,
            createdAtMillis = System.currentTimeMillis()
        )
        _forumMessages.value = _forumMessages.value + msg

        val notif = AppNotification(
            id = "NOTIF-${System.currentTimeMillis() % 100000}",
            idSekolah = _currentTenant.value.idSekolah,
            recipientEmailOrRole = "GURU",
            title = "Forum Diskusi: $senderName",
            message = cleanText.take(120),
            timestamp = "$nowTime WIB",
            isRead = false,
            type = "FORUM_CHAT",
            senderUid = session?.uid ?: "local"
        )
        _notifications.value = listOf(notif) + _notifications.value

        repository?.let { repo ->
            if (session != null) {
                viewModelScope.launch {
                    repo.sendForumMessage(msg).onFailure { _errorMessage.value = it.localizedMessage }
                    repo.createNotification(notif)
                }
            }
        }
        onSuccess()
    }

    fun sendForumVoiceMessage(voiceBase64: String, durationSec: Int, onSuccess: () -> Unit = {}) {
        val role = _currentRole.value
        if (role != UserRole.SUPERADMIN && role != UserRole.GURU) {
            _errorMessage.value = "Akses ditolak: Forum Diskusi hanya untuk Admin dan Guru."
            return
        }
        if (durationSec <= 0 || durationSec > 30) {
            _errorMessage.value = "Durasi Voice-Note dibatasi maksimal 30 detik untuk optimasi kuota Firebase."
            return
        }
        if (voiceBase64.isBlank() || voiceBase64.length > 140_000) {
            _errorMessage.value = "Audio Voice-Note tidak valid atau melebihi batas kuota ringan Firebase."
            return
        }
        val session = _authenticatedSession.value
        val senderName = _currentUserAccount.value?.name ?: session?.displayName ?: role.label
        val nowDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        val nowTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.US).format(java.util.Date())
        val msgId = "FRM-VN-${System.currentTimeMillis()}"
        val msg = ForumMessage(
            id = msgId,
            idSekolah = _currentTenant.value.idSekolah,
            senderUid = session?.uid ?: if (role == UserRole.SUPERADMIN) "admin-local" else "guru-local",
            senderName = senderName,
            senderRole = role.name,
            messageType = ForumMessageType.VOICE_NOTE,
            textContent = "Voice-Note ($durationSec dtk)",
            voiceBase64 = voiceBase64,
            voiceDurationSec = durationSec,
            sentDate = nowDate,
            sentTime = nowTime,
            isArchived = false,
            createdAtMillis = System.currentTimeMillis()
        )
        _forumMessages.value = _forumMessages.value + msg

        val notif = AppNotification(
            id = "NOTIF-${System.currentTimeMillis() % 100000}",
            idSekolah = _currentTenant.value.idSekolah,
            recipientEmailOrRole = "GURU",
            title = "Voice-Note Forum: $senderName",
            message = "Mengirim Voice-Note ($durationSec detik) di Forum Diskusi Lembaga.",
            timestamp = "$nowTime WIB",
            isRead = false,
            type = "FORUM_VOICE",
            senderUid = session?.uid ?: "local"
        )
        _notifications.value = listOf(notif) + _notifications.value

        repository?.let { repo ->
            if (session != null) {
                viewModelScope.launch {
                    repo.sendForumMessage(msg).onFailure { _errorMessage.value = it.localizedMessage }
                    repo.createNotification(notif)
                }
            }
        }
        _toastMessage.value = "Voice-Note ($durationSec dtk) terkirim!"
        onSuccess()
    }

    fun archiveForumMessage(msgId: String, archived: Boolean) {
        if (_currentRole.value != UserRole.SUPERADMIN) {
            _errorMessage.value = "Hanya Admin Sekolah yang dapat mengarsipkan pesan forum."
            return
        }
        _forumMessages.value = _forumMessages.value.map { if (it.id == msgId) it.copy(isArchived = archived) else it }
        repository?.let { repo ->
            if (_authenticatedSession.value != null) {
                viewModelScope.launch { repo.archiveForumMessage(_currentTenant.value.idSekolah, msgId, archived) }
            }
        }
        _toastMessage.value = if (archived) "Pesan forum diarsipkan" else "Arsip pesan dipulihkan"
    }

    fun deleteForumMessage(msgId: String) {
        if (_currentRole.value != UserRole.SUPERADMIN) {
            _errorMessage.value = "Hanya Admin Sekolah yang dapat menghapus pesan forum untuk menghemat kuota."
            return
        }
        _forumMessages.value = _forumMessages.value.filterNot { it.id == msgId }
        repository?.let { repo ->
            if (_authenticatedSession.value != null) {
                viewModelScope.launch { repo.deleteForumMessage(_currentTenant.value.idSekolah, msgId) }
            }
        }
        _toastMessage.value = "Pesan forum dihapus (Kuota Firestore dihemat)"
    }
}
