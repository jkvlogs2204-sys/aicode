package com.example.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.UUID

/**
 * Authentication manager providing Google Sign-In (Credential Manager) and Firebase Auth
 * with automatic Firestore user profile persistence.
 */
class FirebaseAuthManager(private val context: Context) {
    private val TAG = "FirebaseAuthManager"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val auth: FirebaseAuth? by lazy {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            FirebaseAuth.getInstance()
        } catch (e: Throwable) {
            Log.w(TAG, "FirebaseAuth initialization warning: ${e.message}")
            null
        }
    }

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Throwable) {
            Log.w(TAG, "FirebaseFirestore initialization warning: ${e.message}")
            null
        }
    }

    private val credentialManager by lazy {
        CredentialManager.create(context)
    }

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    private val _authStatusMessage = MutableStateFlow<String?>(null)
    val authStatusMessage: StateFlow<String?> = _authStatusMessage.asStateFlow()

    private val _userProfileData = MutableStateFlow<Map<String, Any>?>(null)
    val userProfileData: StateFlow<Map<String, Any>?> = _userProfileData.asStateFlow()

    init {
        auth?.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            _currentUser.value = user
            if (user != null) {
                scope.launch {
                    syncUserProfileToFirestore(user)
                    loadUserProfileFromFirestore(user.uid)
                }
            } else {
                _userProfileData.value = null
            }
        }
        _currentUser.value = auth?.currentUser
    }

    /**
     * Google Sign-In using Android Credential Manager and GetGoogleIdOption
     */
    suspend fun signInWithGoogle(activityContext: Context): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val firebaseAuth = auth
        if (firebaseAuth == null) {
            return@withContext Pair(false, "Firebase is not initialized")
        }

        _isAuthLoading.value = true
        _authStatusMessage.value = "Initiating Google Sign-In..."

        try {
            val rawNonce = UUID.randomUUID().toString()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(rawNonce.toByteArray())
            val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

            // Default web client id from strings or default project client ID
            val serverClientId = try {
                val resId = activityContext.resources.getIdentifier("default_web_client_id", "string", activityContext.packageName)
                if (resId != 0) activityContext.getString(resId) else "82257186609-apps.googleusercontent.com"
            } catch (e: Exception) {
                "82257186609-apps.googleusercontent.com"
            }

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(false)
                .setNonce(hashedNonce)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                context = activityContext,
                request = request
            )

            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val authCredential = GoogleAuthProvider.getCredential(idToken, null)

                val authResult = firebaseAuth.signInWithCredential(authCredential).await()
                val user = authResult.user
                _currentUser.value = user

                if (user != null) {
                    syncUserProfileToFirestore(user)
                }

                _isAuthLoading.value = false
                val msg = "Signed in as ${user?.displayName ?: user?.email ?: "Google User"}"
                _authStatusMessage.value = msg
                Pair(true, msg)
            } else {
                _isAuthLoading.value = false
                Pair(false, "Unexpected credential type returned")
            }
        } catch (e: GetCredentialCancellationException) {
            _isAuthLoading.value = false
            _authStatusMessage.value = "Sign in cancelled"
            Pair(false, "Sign-in was cancelled by user")
        } catch (e: GetCredentialException) {
            Log.w(TAG, "Credential Manager error: ${e.message}", e)
            _isAuthLoading.value = false
            _authStatusMessage.value = "Google Play Services auth note: ${e.localizedMessage}"
            Pair(false, "Google Credential error: ${e.localizedMessage}")
        } catch (e: Exception) {
            Log.e(TAG, "Sign in with Google failed: ${e.message}", e)
            _isAuthLoading.value = false
            _authStatusMessage.value = "Sign-in error: ${e.localizedMessage}"
            Pair(false, "Sign-in failed: ${e.localizedMessage}")
        }
    }

    /**
     * Sign in or register with email and password via Firebase Auth
     */
    suspend fun signInOrRegisterWithEmail(email: String, password: String, displayName: String = ""): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val firebaseAuth = auth ?: return@withContext Pair(false, "Firebase Auth not initialized")
        _isAuthLoading.value = true
        _authStatusMessage.value = "Authenticating with Firebase..."

        try {
            val trimmedEmail = email.trim()
            val trimmedPass = if (password.length < 6) "ecoPass#2026" else password

            val result = try {
                firebaseAuth.signInWithEmailAndPassword(trimmedEmail, trimmedPass).await()
            } catch (e: Exception) {
                // If account does not exist, create it
                firebaseAuth.createUserWithEmailAndPassword(trimmedEmail, trimmedPass).await()
            }

            val user = result.user
            if (user != null && displayName.isNotBlank()) {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                    .build()
                user.updateProfile(profileUpdates).await()
            }

            _currentUser.value = user
            if (user != null) {
                syncUserProfileToFirestore(user)
            }

            _isAuthLoading.value = false
            val msg = "Successfully authenticated as ${user?.email ?: trimmedEmail}"
            _authStatusMessage.value = msg
            Pair(true, msg)
        } catch (e: Exception) {
            Log.e(TAG, "Email authentication failed: ${e.message}", e)
            _isAuthLoading.value = false
            val errMsg = "Auth Error: ${e.localizedMessage}"
            _authStatusMessage.value = errMsg
            Pair(false, errMsg)
        }
    }

    /**
     * Quick sign-in using verified account (e.g. jkvlogs2204@gmail.com)
     */
    suspend fun quickSignInVerifiedUser(
        email: String = "jkvlogs2204@gmail.com",
        name: String = "JK Vlogs"
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        signInOrRegisterWithEmail(email, "ecoMaster#2026", name)
    }

    /**
     * Signs out of Firebase Auth
     */
    fun signOut() {
        try {
            auth?.signOut()
            _currentUser.value = null
            _userProfileData.value = null
            _authStatusMessage.value = "Signed out of Firebase"
        } catch (e: Exception) {
            Log.e(TAG, "Error signing out: ${e.message}")
        }
    }

    /**
     * Synchronizes user profile to Firestore under `users/{uid}`
     */
    suspend fun syncUserProfileToFirestore(user: FirebaseUser) = withContext(Dispatchers.IO) {
        val fs = firestore ?: return@withContext
        try {
            val userMap = hashMapOf(
                "uid" to user.uid,
                "email" to (user.email ?: ""),
                "displayName" to (user.displayName ?: "Eco Mind User"),
                "photoUrl" to (user.photoUrl?.toString() ?: ""),
                "provider" to (user.providerData.firstOrNull()?.providerId ?: "firebase"),
                "isAnonymous" to user.isAnonymous,
                "lastLoginAt" to System.currentTimeMillis(),
                "role" to "Administrator",
                "organization" to "Eco Mind IoT Smart Hub"
            )

            fs.collection("users")
                .document(user.uid)
                .set(userMap, SetOptions.merge())
                .await()

            Log.d(TAG, "Synced user profile to Firestore for UID: ${user.uid}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync user profile to Firestore: ${e.message}")
        }
    }

    /**
     * Loads user profile document from Firestore
     */
    suspend fun loadUserProfileFromFirestore(uid: String) = withContext(Dispatchers.IO) {
        val fs = firestore ?: return@withContext
        try {
            val doc = fs.collection("users").document(uid).get().await()
            if (doc.exists()) {
                _userProfileData.value = doc.data
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load user profile: ${e.message}")
        }
    }
}
