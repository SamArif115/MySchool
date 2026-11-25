package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.*
import com.example.data.BrandingPreferences
import com.example.data.CachedSchoolBranding
import com.example.data.GoogleAuthHelper
import com.example.data.GoogleSignInResult
import com.example.data.SchoolFirestoreRepository
import com.example.ui.MainApp
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.SchoolViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val schoolViewModel: SchoolViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val databaseId = getString(R.string.firestore_database_id)
        val firestore = FirebaseFirestore.getInstance(databaseId)
        val auth = Firebase.auth
        val repository = SchoolFirestoreRepository(firestore, auth)
        val googleAuthHelper = GoogleAuthHelper(applicationContext, auth)
        val brandingPrefs = BrandingPreferences(applicationContext)

        schoolViewModel.attachFirebaseRepository(repository)

        setContent {
            val isDarkMode by schoolViewModel.isDarkMode.collectAsState()
            val schoolProfile by schoolViewModel.currentSchoolProfile.collectAsState()
            val authenticatedSession by schoolViewModel.authenticatedSession.collectAsState()
            var cachedBranding by remember { mutableStateOf(brandingPrefs.loadBranding()) }

            val coroutineScope = rememberCoroutineScope()
            var isSigningIn by remember { mutableStateOf(false) }
            var authError by remember { mutableStateOf<String?>(null) }

            val currentUser by remember(auth) {
                callbackFlow<FirebaseUser?> {
                    val listener = FirebaseAuth.AuthStateListener { fa ->
                        trySend(fa.currentUser)
                    }
                    auth.addAuthStateListener(listener)
                    awaitClose { auth.removeAuthStateListener(listener) }
                }
            }.collectAsState(initial = auth.currentUser)

            LaunchedEffect(currentUser) {
                schoolViewModel.onFirebaseAuthStateChanged(currentUser)
            }

            // Persist live branding once authenticated & synced so Splash-Screen displays Admin logo & school name
            LaunchedEffect(
                authenticatedSession?.idSekolah,
                schoolProfile.name,
                schoolProfile.logoBase64,
                schoolProfile.themePreset
            ) {
                if (authenticatedSession != null && schoolProfile.name.isNotBlank()) {
                    brandingPrefs.saveBranding(
                        schoolName = schoolProfile.name,
                        logoBase64 = schoolProfile.logoBase64,
                        themePreset = schoolProfile.themePreset
                    )
                    cachedBranding = CachedSchoolBranding(
                        schoolName = schoolProfile.name,
                        logoBase64 = schoolProfile.logoBase64,
                        themePreset = schoolProfile.themePreset
                    )
                }
            }

            // Attempt silent Google Sign-In once when starting signed-out
            LaunchedEffect(Unit) {
                if (auth.currentUser == null) {
                    googleAuthHelper.attemptSilentSignIn(this@MainActivity)
                }
            }

            val activeTheme = if (authenticatedSession != null) schoolProfile.themePreset else cachedBranding.themePreset

            MyApplicationTheme(darkTheme = isDarkMode, themePreset = activeTheme) {
                MainApp(
                    viewModel = schoolViewModel,
                    cachedBranding = cachedBranding,
                    firebaseUser = currentUser,
                    isSigningIn = isSigningIn,
                    authError = authError,
                    onGoogleSignInClick = {
                        coroutineScope.launch {
                            isSigningIn = true
                            authError = null
                            when (val res = googleAuthHelper.signInInteractive(this@MainActivity)) {
                                is GoogleSignInResult.Success -> {
                                    authError = null
                                }
                                is GoogleSignInResult.Cancelled -> Unit
                                is GoogleSignInResult.Error -> {
                                    authError = res.message
                                }
                            }
                            isSigningIn = false
                        }
                    },
                    onSignOutClick = {
                        coroutineScope.launch {
                            googleAuthHelper.signOut()
                            authError = null
                        }
                    },
                    onForceSyncClick = {
                        schoolViewModel.forceRefreshSync(currentUser)
                    }
                )
            }
        }
    }
}
