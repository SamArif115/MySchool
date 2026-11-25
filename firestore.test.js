const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const DATABASE_ID = "ai-studio-android-myschool-09225d12-9da2-4182-8ef1-798a0d122558";

const ADMIN_UID = "admin_uid_01";
const KEPSEK_UID = "kepsek_uid_02";
const GURU_UID = "guru_uid_03";
const GURU_OTHER_UID = "guru_other_uid_04";
const SISWA_UID = "siswa_uid_05";
const OUTSIDER_UID = "outsider_uid_06";

const SCHOOL_ID = "SCH-001";
const OTHER_SCHOOL_ID = "SCH-002";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function getDb(context) {
  return context.firestore(DATABASE_ID);
}

async function seedBaseSchool(withRulesDisabled) {
  await withRulesDisabled(async (ctx) => {
    const db = getDb(ctx);
    const now = new Date();
    await db.doc(`schools/${SCHOOL_ID}`).set({
      idSekolah: SCHOOL_ID,
      name: "SMA Negeri 1 Maju Bersama",
      npsn: "20108745",
      akreditasi: "A (Unggul)",
      level: "SMA",
      city: "Jakarta Selatan",
      address: "Jl. Pendidikan No. 45",
      email: "info@sman1.sch.id",
      phone: "0217892341",
      website: "https://sman1.sch.id",
      kepalaSekolah: "Dra. Hj. Nuraini, M.Pd.",
      vision: "Unggul dan Berkarakter",
      mission: "Pembelajaran Berkualitas",
      logoBase64: "",
      logoFileName: "",
      logoSizeBytes: 0,
      themePreset: "BLUE_TEAL",
      organizationStructure: "Struktur Org",
      academicYear: "2026/2027",
      semester: "Ganjil",
      ownerUid: ADMIN_UID,
      isPro: false,
      storageUsedMb: 180,
      storageMaxMb: 500,
      activeAdNetwork: "ADMOB",
      createdAt: now,
      updatedAt: now,
    });

    await db.doc(`schools/${SCHOOL_ID}/members/${ADMIN_UID}`).set({
      uid: ADMIN_UID, idSekolah: SCHOOL_ID, email: "admin@sman1.sch.id",
      name: "Admin Utama", role: "SUPERADMIN", idGuru: "",
      idKelas: [], idMapel: [], whitelistId: "WL-ADMIN", studentId: "", className: "",
      createdAt: now, updatedAt: now,
    });

    await db.doc(`schools/${SCHOOL_ID}/members/${KEPSEK_UID}`).set({
      uid: KEPSEK_UID, idSekolah: SCHOOL_ID, email: "kepsek@sman1.sch.id",
      name: "Dra. Hj. Nuraini, M.Pd.", role: "KEPALA_SEKOLAH", idGuru: "",
      idKelas: [], idMapel: [], whitelistId: "WL-KEPSEK", studentId: "", className: "",
      createdAt: now, updatedAt: now,
    });

    await db.doc(`schools/${SCHOOL_ID}/members/${GURU_UID}`).set({
      uid: GURU_UID, idSekolah: SCHOOL_ID, email: "eko.prasetyo@school.id",
      name: "Drs. Eko Prasetyo, M.Si.", role: "GURU", idGuru: "T-01",
      idKelas: ["C-01"], idMapel: ["SUB-01"], whitelistId: "WL-GURU1", studentId: "", className: "",
      createdAt: now, updatedAt: now,
    });

    await db.doc(`schools/${SCHOOL_ID}/members/${GURU_OTHER_UID}`).set({
      uid: GURU_OTHER_UID, idSekolah: SCHOOL_ID, email: "dewi@school.id",
      name: "Dewi Anggraini, S.Pd.", role: "GURU", idGuru: "T-02",
      idKelas: ["C-03"], idMapel: ["SUB-02"], whitelistId: "WL-GURU2", studentId: "", className: "",
      createdAt: now, updatedAt: now,
    });

    await db.doc(`schools/${SCHOOL_ID}/members/${SISWA_UID}`).set({
      uid: SISWA_UID, idSekolah: SCHOOL_ID, email: "ahmad@gmail.com",
      name: "Ahmad Fauzan", role: "SISWA", idGuru: "",
      idKelas: ["C-01"], idMapel: [], whitelistId: "C-01_1234", studentId: "S-01", className: "XII MIPA 1",
      createdAt: now, updatedAt: now,
    });

    await db.doc(`schools/${SCHOOL_ID}/staff_whitelist/WL-GURU-NEW`).set({
      id: "WL-GURU-NEW", idSekolah: SCHOOL_ID, email: "newguru@school.id",
      name: "Guru Baru", nip: "199001012020011001", role: "GURU",
      jabatan: "Guru Biologi", idGuru: "T-04", idKelas: ["C-01"], idMapel: ["SUB-04"],
      isVerified: true, createdAt: now, updatedAt: now,
    });

    await db.doc(`schools/${SCHOOL_ID}/classes/C-01`).set({
      id: "C-01", idSekolah: SCHOOL_ID, name: "XII MIPA 1", gradeLevel: 12,
      homeroomTeacher: "Drs. Eko Prasetyo", room: "R-301", studentCount: 34, capacity: 36,
      pinKelas: "1234", pinUpdatedAt: "2026-11-12", createdAt: now, updatedAt: now,
    });

    await db.doc(`schools/${SCHOOL_ID}/pin_verifications/C-01_1234`).set({
      classId: "C-01", className: "XII MIPA 1", idSekolah: SCHOOL_ID,
      createdAt: now, updatedAt: now,
    });

    await db.doc(`schools/${SCHOOL_ID}/lesson_plans/RPP-01`).set({
      id: "RPP-01", idSekolah: SCHOOL_ID, title: "Kalkulus Dasar",
      curriculum: "Kurikulum Merdeka", subject: "Matematika Peminatan",
      className: "XII MIPA 1", meetingCount: 4, duration: "2 x 45 Menit",
      learningObjectives: "Memahami turunan fungsi", fileName: "kalkulus.pdf",
      teacherId: "T-01", teacherName: "Drs. Eko Prasetyo", teacherEmail: "eko.prasetyo@school.id",
      userId: GURU_UID, reviewNotes: "", fileSizeBytes: 2048000,
      storageUrl: "gs://myschool/rpp/RPP-01.pdf", status: "SUBMITTED",
      createdAt: now, updatedAt: now,
    });

    await db.doc(`schools/${SCHOOL_ID}/grades/G-01`).set({
      id: "G-01", idSekolah: SCHOOL_ID, studentId: "S-01", studentName: "Ahmad Fauzan",
      className: "XII MIPA 1", subjectName: "Matematika Peminatan",
      tugasScore: 88, utsScore: 90, uasScore: 92, teacherId: "T-01",
      studentUserId: SISWA_UID, createdAt: now, updatedAt: now,
    });

    await db.doc(`schools/${SCHOOL_ID}/grades/G-OTHER`).set({
      id: "G-OTHER", idSekolah: SCHOOL_ID, studentId: "S-02", studentName: "Bella Amanda",
      className: "XII MIPA 1", subjectName: "Matematika Peminatan",
      tugasScore: 80, utsScore: 82, uasScore: 85, teacherId: "T-01",
      studentUserId: "other_student_uid", createdAt: now, updatedAt: now,
    });
  });
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) await testEnv.cleanup();
});

beforeEach(async () => {
  if (testEnv) await testEnv.clearFirestore();
});

test("Unauthenticated user cannot read or write any school data", async () => {
  await seedBaseSchool(testEnv.withSecurityRulesDisabled.bind(testEnv));
  const unauthDb = getDb(testEnv.unauthenticatedContext());
  await assertFails(unauthDb.doc(`schools/${SCHOOL_ID}`).get());
  await assertFails(unauthDb.collection(`schools/${SCHOOL_ID}/lesson_plans`).get());
  await assertFails(unauthDb.collection(`schools/${SCHOOL_ID}/classes`).get());
});

test("Student cannot read protected classes (pinKelas) or other students' grades", async () => {
  await seedBaseSchool(testEnv.withSecurityRulesDisabled.bind(testEnv));
  const siswaDb = getDb(testEnv.authenticatedContext(SISWA_UID, { email: "ahmad@gmail.com", email_verified: true }));

  // Student CANNOT read classes containing pinKelas
  await assertFails(siswaDb.collection(`schools/${SCHOOL_ID}/classes`).get());
  // Student CANNOT list pin_verifications
  await assertFails(siswaDb.collection(`schools/${SCHOOL_ID}/pin_verifications`).get());
  // Student CANNOT list all grades without personal studentUserId filter
  await assertFails(siswaDb.collection(`schools/${SCHOOL_ID}/grades`).get());
  await assertFails(siswaDb.doc(`schools/${SCHOOL_ID}/grades/G-OTHER`).get());

  // Student CAN read own personal grade using matching query filter
  await assertSucceeds(
    siswaDb.collection(`schools/${SCHOOL_ID}/grades`).where("studentUserId", "==", SISWA_UID).get()
  );
  // Student CAN get exact pin verification doc when PIN is known
  await assertSucceeds(siswaDb.doc(`schools/${SCHOOL_ID}/pin_verifications/C-01_1234`).get());
});

test("Kepsek can approve/reject RPP review fields ONLY, and CANNOT edit RPP content or master data", async () => {
  await seedBaseSchool(testEnv.withSecurityRulesDisabled.bind(testEnv));
  const kepsekDb = getDb(testEnv.authenticatedContext(KEPSEK_UID, { email: "kepsek@sman1.sch.id", email_verified: true }));
  const now = new Date();

  // Kepsek CAN approve RPP updating only status, reviewNotes, updatedAt
  await assertSucceeds(
    kepsekDb.doc(`schools/${SCHOOL_ID}/lesson_plans/RPP-01`).update({
      status: "APPROVED",
      reviewNotes: "Disetujui sesuai Kurikulum Merdeka.",
      updatedAt: now,
    })
  );

  // Kepsek CANNOT modify RPP title/subject/learningObjectives
  await assertFails(
    kepsekDb.doc(`schools/${SCHOOL_ID}/lesson_plans/RPP-01`).update({
      title: "Judul Diubah Kepsek",
      updatedAt: now,
    })
  );

  // Kepsek CANNOT delete RPP or create staff whitelist
  await assertFails(kepsekDb.doc(`schools/${SCHOOL_ID}/lesson_plans/RPP-01`).delete());
  await assertFails(
    kepsekDb.doc(`schools/${SCHOOL_ID}/staff_whitelist/WL-HACK`).set({
      id: "WL-HACK", idSekolah: SCHOOL_ID, email: "hack@gmail.com",
      name: "Hacker", nip: "1", role: "SUPERADMIN", jabatan: "Admin",
      idGuru: "", idKelas: [], idMapel: [], isVerified: true,
      createdAt: now, updatedAt: now,
    })
  );
});

test("Guru can create own RPP <= 5MB, CANNOT upload > 5MB, CANNOT self-approve, and CANNOT edit another teacher's RPP", async () => {
  await seedBaseSchool(testEnv.withSecurityRulesDisabled.bind(testEnv));
  const guruDb = getDb(testEnv.authenticatedContext(GURU_UID, { email: "eko.prasetyo@school.id", email_verified: true }));
  const otherGuruDb = getDb(testEnv.authenticatedContext(GURU_OTHER_UID, { email: "dewi@school.id", email_verified: true }));
  const now = new Date();

  // Valid 3MB RPP creation by Guru
  await assertSucceeds(
    guruDb.doc(`schools/${SCHOOL_ID}/lesson_plans/RPP-NEW`).set({
      id: "RPP-NEW", idSekolah: SCHOOL_ID, title: "Integral Lanjut",
      curriculum: "Kurikulum Merdeka", subject: "Matematika Peminatan",
      className: "XII MIPA 1", meetingCount: 4, duration: "2 x 45 Menit",
      learningObjectives: "Menguasai integral tentu", fileName: "integral.pdf",
      teacherId: "T-01", teacherName: "Drs. Eko Prasetyo", teacherEmail: "eko.prasetyo@school.id",
      userId: GURU_UID, reviewNotes: "", fileSizeBytes: 3145728,
      storageUrl: "gs://myschool/rpp/RPP-NEW.pdf", status: "DRAFT",
      createdAt: now, updatedAt: now,
    })
  );

  // Oversized 6MB RPP rejected by Firestore rules
  await assertFails(
    guruDb.doc(`schools/${SCHOOL_ID}/lesson_plans/RPP-BIG`).set({
      id: "RPP-BIG", idSekolah: SCHOOL_ID, title: "File Terlalu Besar",
      curriculum: "Kurikulum Merdeka", subject: "Matematika Peminatan",
      className: "XII MIPA 1", meetingCount: 4, duration: "2 x 45 Menit",
      learningObjectives: "Test", fileName: "big.pdf",
      teacherId: "T-01", teacherName: "Drs. Eko Prasetyo", teacherEmail: "eko.prasetyo@school.id",
      userId: GURU_UID, reviewNotes: "", fileSizeBytes: 6291456,
      storageUrl: "gs://myschool/rpp/RPP-BIG.pdf", status: "DRAFT",
      createdAt: now, updatedAt: now,
    })
  );

  // Guru CANNOT self-approve own RPP
  await assertFails(
    guruDb.doc(`schools/${SCHOOL_ID}/lesson_plans/RPP-01`).update({
      status: "APPROVED",
      updatedAt: now,
    })
  );

  // Another Guru CANNOT edit Eko's RPP
  await assertFails(
    otherGuruDb.doc(`schools/${SCHOOL_ID}/lesson_plans/RPP-01`).update({
      title: "Dibajak Guru Lain",
      updatedAt: now,
    })
  );
});

test("Verified staff can claim own whitelist membership ONLY; outsiders cannot escalate roles", async () => {
  await seedBaseSchool(testEnv.withSecurityRulesDisabled.bind(testEnv));
  const now = new Date();

  const newGuruDb = getDb(testEnv.authenticatedContext("new_guru_uid", { email: "newguru@school.id", email_verified: true }));
  // Can query own whitelist by email
  await assertSucceeds(
    newGuruDb.collection(`schools/${SCHOOL_ID}/staff_whitelist`).where("email", "==", "newguru@school.id").get()
  );
  // Can claim matching GURU member doc
  await assertSucceeds(
    newGuruDb.doc(`schools/${SCHOOL_ID}/members/new_guru_uid`).set({
      uid: "new_guru_uid", idSekolah: SCHOOL_ID, email: "newguru@school.id",
      name: "Guru Baru", role: "GURU", idGuru: "T-04",
      idKelas: ["C-01"], idMapel: ["SUB-04"], whitelistId: "WL-GURU-NEW",
      studentId: "", className: "", createdAt: now, updatedAt: now,
    })
  );

  // Outsider CANNOT claim SUPERADMIN or dump staff_whitelist
  const outsiderDb = getDb(testEnv.authenticatedContext(OUTSIDER_UID, { email: "evil@gmail.com", email_verified: true }));
  await assertFails(outsiderDb.collection(`schools/${SCHOOL_ID}/staff_whitelist`).get());
  await assertFails(
    outsiderDb.doc(`schools/${SCHOOL_ID}/members/${OUTSIDER_UID}`).set({
      uid: OUTSIDER_UID, idSekolah: SCHOOL_ID, email: "evil@gmail.com",
      name: "Evil", role: "SUPERADMIN", idGuru: "",
      idKelas: [], idMapel: [], whitelistId: "WL-GURU-NEW",
      studentId: "", className: "", createdAt: now, updatedAt: now,
    })
  );
});

test("Stage 1: Admin ONLY can update school appearance & <=5MB logo; Guru is denied", async () => {
  await seedBaseSchool(testEnv.withSecurityRulesDisabled.bind(testEnv));
  const adminDb = getDb(testEnv.authenticatedContext(ADMIN_UID, { email: "admin@sman1.sch.id", email_verified: true }));
  const guruDb = getDb(testEnv.authenticatedContext(GURU_UID, { email: "eko.prasetyo@school.id", email_verified: true }));
  const now = new Date();

  // Admin CAN update school appearance & logo <= 5MB
  await assertSucceeds(
    adminDb.doc(`schools/${SCHOOL_ID}`).update({
      name: "SMA Negeri 1 Unggul",
      logoFileName: "logo_sman1.png",
      logoSizeBytes: 4194304,
      themePreset: "INDIGO_SNEAT",
      academicYear: "2026/2027",
      semester: "Genap",
      updatedAt: now,
    })
  );

  // Admin CANNOT upload logo > 5MB
  await assertFails(
    adminDb.doc(`schools/${SCHOOL_ID}`).update({
      logoSizeBytes: 6291456,
      updatedAt: now,
    })
  );

  // Guru CANNOT update school appearance or name
  await assertFails(
    guruDb.doc(`schools/${SCHOOL_ID}`).update({
      name: "Dibajak Guru",
      updatedAt: now,
    })
  );
});

test("Stage 1: Admin CRUD on teachers/holidays/announcements/login_logs vs Guru bounds", async () => {
  await seedBaseSchool(testEnv.withSecurityRulesDisabled.bind(testEnv));
  const adminDb = getDb(testEnv.authenticatedContext(ADMIN_UID, { email: "admin@sman1.sch.id", email_verified: true }));
  const guruDb = getDb(testEnv.authenticatedContext(GURU_UID, { email: "eko.prasetyo@school.id", email_verified: true }));
  const now = new Date();

  // Admin creates PNS teacher with rankGroup & SK <= 5MB
  await assertSucceeds(
    adminDb.doc(`schools/${SCHOOL_ID}/teachers/T-PNS-1`).set({
      id: "T-PNS-1", idSekolah: SCHOOL_ID, nip: "198501012010011002",
      name: "Budi Santoso, S.Pd.", email: "budi.santoso@school.id", phone: "081234567890",
      subject: "Fisika", employmentStatus: "PNS", jabatan: "Guru Fisika",
      skNumber: "SK-2026-01", skDate: "2026-01-10", tmtDate: "2010-01-01",
      tenureYears: 16, rankGroup: "IV/a", weeklyHours: 24, classesTaught: ["XII MIPA 1"],
      address: "Jl. Merdeka No. 10", photoBase64: "", skDocumentName: "sk_budi.pdf",
      skDocumentSizeBytes: 1024000, isActiveAccount: true, createdAt: now, updatedAt: now,
    })
  );

  // Admin creates holiday & announcement
  await assertSucceeds(
    adminDb.doc(`schools/${SCHOOL_ID}/holidays/HOL-01`).set({
      id: "HOL-01", idSekolah: SCHOOL_ID, date: "2026-08-17",
      name: "Hari Kemerdekaan RI", category: "Libur Nasional",
      createdAt: now, updatedAt: now,
    })
  );
  await assertSucceeds(
    adminDb.doc(`schools/${SCHOOL_ID}/announcements/ANN-01`).set({
      id: "ANN-01", idSekolah: SCHOOL_ID, title: "Rapat Awal Semester",
      content: "Seluruh guru hadir pukul 08.00 WIB.", targetRole: "Guru",
      authorName: "Admin Utama", publishedDate: "2026-11-18", userId: ADMIN_UID,
      createdAt: now, updatedAt: now,
    })
  );

  // Guru CAN read holidays & announcements, CANNOT write them
  await assertSucceeds(guruDb.collection(`schools/${SCHOOL_ID}/holidays`).get());
  await assertSucceeds(guruDb.collection(`schools/${SCHOOL_ID}/announcements`).get());
  await assertFails(
    guruDb.doc(`schools/${SCHOOL_ID}/holidays/HOL-HACK`).set({
      id: "HOL-HACK", idSekolah: SCHOOL_ID, date: "2026-12-01",
      name: "Libur Palsu", category: "Libur Nasional",
      createdAt: now, updatedAt: now,
    })
  );
  await assertFails(
    guruDb.doc(`schools/${SCHOOL_ID}/announcements/ANN-HACK`).set({
      id: "ANN-HACK", idSekolah: SCHOOL_ID, title: "Pengumuman Palsu",
      content: "Test", targetRole: "Semua",
      authorName: "Guru", publishedDate: "2026-11-18", userId: GURU_UID,
      createdAt: now, updatedAt: now,
    })
  );

  // Guru CAN create own login log, CANNOT read or delete login_logs; Admin CAN read & delete
  await assertSucceeds(
    guruDb.doc(`schools/${SCHOOL_ID}/login_logs/LOG-GURU-1`).set({
      id: "LOG-GURU-1", idSekolah: SCHOOL_ID, uid: GURU_UID,
      email: "eko.prasetyo@school.id", displayName: "Drs. Eko Prasetyo",
      role: "GURU", timestamp: "2026-11-18 07:30:00",
      deviceInfo: "Android App", status: "Sukses",
      createdAt: now, updatedAt: now,
    })
  );
  await assertFails(guruDb.collection(`schools/${SCHOOL_ID}/login_logs`).get());
  await assertFails(guruDb.doc(`schools/${SCHOOL_ID}/login_logs/LOG-GURU-1`).delete());
  await assertSucceeds(adminDb.collection(`schools/${SCHOOL_ID}/login_logs`).get());
  await assertSucceeds(adminDb.doc(`schools/${SCHOOL_ID}/login_logs/LOG-GURU-1`).delete());
});

test("Forum Diskusi Lembaga: Admin & Guru ONLY (Chat + Voice-Note <=30s), blocks Siswa/Kepsek/files, Admin quota archive/delete", async () => {
  await seedBaseSchool(testEnv.withSecurityRulesDisabled.bind(testEnv));
  const adminDb = getDb(testEnv.authenticatedContext(ADMIN_UID, { email: "admin@sman1.sch.id", email_verified: true }));
  const guruDb = getDb(testEnv.authenticatedContext(GURU_UID, { email: "eko.prasetyo@school.id", email_verified: true }));
  const siswaDb = getDb(testEnv.authenticatedContext(SISWA_UID, { email: "ahmad@gmail.com", email_verified: true }));
  const kepsekDb = getDb(testEnv.authenticatedContext(KEPSEK_UID, { email: "kepsek@sman1.sch.id", email_verified: true }));
  const now = new Date();

  // Admin sends TEXT message -> succeeds
  await assertSucceeds(
    adminDb.doc(`schools/${SCHOOL_ID}/forum_messages/MSG-1`).set({
      id: "MSG-1", idSekolah: SCHOOL_ID, senderUid: ADMIN_UID,
      senderName: "Admin Utama", senderRole: "SUPERADMIN", messageType: "TEXT",
      textContent: "Rapat kurikulum hari Jumat pukul 09.00 WIB.", voiceBase64: "",
      voiceDurationSec: 0, sentDate: "2026-11-18", sentTime: "08:00",
      isArchived: false, createdAt: now, updatedAt: now,
    })
  );

  // Guru sends VOICE_NOTE <= 30s -> succeeds
  await assertSucceeds(
    guruDb.doc(`schools/${SCHOOL_ID}/forum_messages/MSG-2`).set({
      id: "MSG-2", idSekolah: SCHOOL_ID, senderUid: GURU_UID,
      senderName: "Drs. Eko Prasetyo", senderRole: "GURU", messageType: "VOICE_NOTE",
      textContent: "Voice-Note (18 dtk)", voiceBase64: "GkXfo59ChoEBQveBAULygQRC84EIQoKEd2VibQ==",
      voiceDurationSec: 18, sentDate: "2026-11-18", sentTime: "08:05",
      isArchived: false, createdAt: now, updatedAt: now,
    })
  );

  // Guru CAN list forum messages (limit 25 lazy load)
  await assertSucceeds(
    guruDb.collection(`schools/${SCHOOL_ID}/forum_messages`).orderBy("createdAt").limit(25).get()
  );

  // Guru CANNOT send voice note > 30s (quota protection)
  await assertFails(
    guruDb.doc(`schools/${SCHOOL_ID}/forum_messages/MSG-LONG`).set({
      id: "MSG-LONG", idSekolah: SCHOOL_ID, senderUid: GURU_UID,
      senderName: "Drs. Eko Prasetyo", senderRole: "GURU", messageType: "VOICE_NOTE",
      textContent: "Voice-Note (45 dtk)", voiceBase64: "GkXfo59ChoEBQveBAULygQ==",
      voiceDurationSec: 45, sentDate: "2026-11-18", sentTime: "08:10",
      isArchived: false, createdAt: now, updatedAt: now,
    })
  );

  // Reject any file/image/document attachment fields
  await assertFails(
    guruDb.doc(`schools/${SCHOOL_ID}/forum_messages/MSG-FILE`).set({
      id: "MSG-FILE", idSekolah: SCHOOL_ID, senderUid: GURU_UID,
      senderName: "Drs. Eko Prasetyo", senderRole: "GURU", messageType: "TEXT",
      textContent: "Cek lampiran", voiceBase64: "", voiceDurationSec: 0,
      sentDate: "2026-11-18", sentTime: "08:12", isArchived: false,
      fileUrl: "https://evil.test/big.pdf", createdAt: now, updatedAt: now,
    })
  );

  // Siswa & Kepsek are strictly BLOCKED from forum_messages
  await assertFails(siswaDb.collection(`schools/${SCHOOL_ID}/forum_messages`).get());
  await assertFails(kepsekDb.collection(`schools/${SCHOOL_ID}/forum_messages`).get());
  await assertFails(
    siswaDb.doc(`schools/${SCHOOL_ID}/forum_messages/MSG-SISWA`).set({
      id: "MSG-SISWA", idSekolah: SCHOOL_ID, senderUid: SISWA_UID,
      senderName: "Ahmad", senderRole: "GURU", messageType: "TEXT",
      textContent: "Halo", voiceBase64: "", voiceDurationSec: 0,
      sentDate: "2026-11-18", sentTime: "08:15", isArchived: false,
      createdAt: now, updatedAt: now,
    })
  );

  // Guru CANNOT archive or delete; Admin CAN archive & delete for quota control
  await assertFails(
    guruDb.doc(`schools/${SCHOOL_ID}/forum_messages/MSG-1`).update({ isArchived: true, updatedAt: now })
  );
  await assertFails(guruDb.doc(`schools/${SCHOOL_ID}/forum_messages/MSG-2`).delete());
  await assertSucceeds(
    adminDb.doc(`schools/${SCHOOL_ID}/forum_messages/MSG-1`).update({ isArchived: true, updatedAt: now })
  );
  await assertSucceeds(adminDb.doc(`schools/${SCHOOL_ID}/forum_messages/MSG-2`).delete());
});

test("PTK management, PTK Attendance & Teaching Schedules: Admin CRUD vs Staff read & Siswa deny", async () => {
  await seedBaseSchool(testEnv.withSecurityRulesDisabled.bind(testEnv));
  const adminDb = getDb(testEnv.authenticatedContext(ADMIN_UID, { email: "admin@sman1.sch.id", email_verified: true }));
  const guruDb = getDb(testEnv.authenticatedContext(GURU_UID, { email: "eko.prasetyo@school.id", email_verified: true }));
  const siswaDb = getDb(testEnv.authenticatedContext(SISWA_UID, { email: "ahmad@gmail.com", email_verified: true }));
  const now = new Date();

  // Admin creates extended PTK teacher with non-satmikal & mutasi fields
  await assertSucceeds(
    adminDb.doc(`schools/${SCHOOL_ID}/teachers/T-PTK-1`).set({
      id: "T-PTK-1", idSekolah: SCHOOL_ID, nip: "198601012012011001",
      name: "Sri Wahyuni, M.Pd.", email: "sri@school.id", phone: "08123400001",
      subject: "Kimia", employmentStatus: "PNS", jabatan: "Guru Kimia",
      skNumber: "SK-PTK-01", skDate: "2026-01-01", tmtDate: "2012-01-01",
      tenureYears: 14, rankGroup: "III/d", weeklyHours: 24, classesTaught: ["XII MIPA 1"],
      address: "Jl. Cendekia No. 5", photoBase64: "", skDocumentName: "sk_sri.pdf",
      skDocumentSizeBytes: 500000, isActiveAccount: true,
      additionalDuty: "Kepala Lab IPA", isNonSatmikal: true,
      satmikalOrigin: "SMA Negeri 3 Jakarta", nonSatmikalVerified: true,
      mutationStatus: "DIAJUKAN", mutationTargetSchool: "SMA Negeri 1 Maju Bersama",
      transferConfirmed: true, createdAt: now, updatedAt: now,
    })
  );

  // Admin creates PTK attendance & teaching schedule
  await assertSucceeds(
    adminDb.doc(`schools/${SCHOOL_ID}/ptk_attendance/PTK-ATT-1`).set({
      id: "PTK-ATT-1", idSekolah: SCHOOL_ID, teacherId: "T-PTK-1",
      teacherName: "Sri Wahyuni, M.Pd.", date: "2026-11-18",
      status: "HADIR", notes: "Hadir tepat waktu", recordedByUid: ADMIN_UID,
      createdAt: now, updatedAt: now,
    })
  );
  await assertSucceeds(
    adminDb.doc(`schools/${SCHOOL_ID}/teaching_schedules/SCHD-1`).set({
      id: "SCHD-1", idSekolah: SCHOOL_ID, dayOfWeek: "Senin",
      startTime: "07:30", endTime: "09:00", className: "XII MIPA 1",
      subjectName: "Matematika Peminatan", teacherId: "T-01",
      teacherName: "Drs. Eko Prasetyo, M.Si.", room: "R-301",
      createdAt: now, updatedAt: now,
    })
  );

  // Guru CAN read PTK attendance & teaching schedules, CANNOT mutate
  await assertSucceeds(guruDb.collection(`schools/${SCHOOL_ID}/ptk_attendance`).get());
  await assertSucceeds(guruDb.collection(`schools/${SCHOOL_ID}/teaching_schedules`).get());
  await assertFails(
    guruDb.doc(`schools/${SCHOOL_ID}/teaching_schedules/SCHD-HACK`).set({
      id: "SCHD-HACK", idSekolah: SCHOOL_ID, dayOfWeek: "Senin",
      startTime: "07:30", endTime: "09:00", className: "XII MIPA 1",
      subjectName: "Matematika", teacherId: "T-01",
      teacherName: "Eko", room: "R-301", createdAt: now, updatedAt: now,
    })
  );

  // Siswa CANNOT read PTK attendance or schedules directly
  await assertFails(siswaDb.collection(`schools/${SCHOOL_ID}/ptk_attendance`).get());
  await assertFails(siswaDb.collection(`schools/${SCHOOL_ID}/teaching_schedules`).get());
});


