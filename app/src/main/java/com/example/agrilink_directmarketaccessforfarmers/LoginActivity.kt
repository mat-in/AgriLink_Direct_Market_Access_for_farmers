package com.example.agrilink_directmarketaccessforfarmers

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : ComponentActivity() {
    private lateinit var sharedPrefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedPrefs = getSharedPreferences("AgriLinkPrefs", Context.MODE_PRIVATE)

        // Check if the user is already authenticated
        val storedUserType = sharedPrefs.getString("userType", null)
        if (FirebaseAuth.getInstance().currentUser != null && storedUserType != null) {
            navigateToDashboard(storedUserType)
        } else {
            val userType = intent.getStringExtra("userType") ?: "buyer" // Default to Buyer
            setContent {
                AuthenticationScreen(userType, sharedPrefs)
            }
        }
    }

    private fun navigateToDashboard(userType: String) {
        val intent = if (userType == "farmer") {
            Intent(this, FarmerDashboardActivity::class.java)
        } else {
            Intent(this, BuyerDashboardActivity::class.java)
        }
        startActivity(intent)
        finish()
    }
}

@Composable
fun AuthenticationScreen(userType: String, sharedPrefs: SharedPreferences) {
    val context = LocalContext.current

    LoginScreen(
        onLoginSuccess = {
            sharedPrefs.edit().putString("userType", userType).apply()
            navigateToDashboard(context, userType)
        }
    )
}

fun navigateToDashboard(context: Context, userType: String) {
    val intent = if (userType == "farmer") {
        Intent(context, FarmerDashboardActivity::class.java)
    } else {
        Intent(context, BuyerDashboardActivity::class.java)
    }
    context.startActivity(intent)
    (context as? Activity)?.finish()
}

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val credentials = Identity.getSignInClient(context).getSignInCredentialFromIntent(result.data)
            val googleIdToken = credentials.googleIdToken
            if (googleIdToken != null) {
                val firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                FirebaseAuth.getInstance().signInWithCredential(firebaseCredential)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val user = FirebaseAuth.getInstance().currentUser
                            user?.let {
                                val userMap = hashMapOf(
                                    "name" to it.displayName,
                                    "photoUrl" to it.photoUrl.toString()
                                )
                                FirebaseFirestore.getInstance().collection("users").document(it.uid).set(userMap)
                                    .addOnSuccessListener {
                                        onLoginSuccess()
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(context, "Failed to save user info: ${e.message}", Toast.LENGTH_SHORT).show()
                                        onLoginSuccess() //continue login even if saving user info fails.
                                    }
                            } ?: run{
                                Toast.makeText(context, "User data not found", Toast.LENGTH_SHORT).show()
                                onLoginSuccess()
                            }

                        } else {
                            Toast.makeText(context, "Google Sign-In Failed", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = {
                val signInRequest = BeginSignInRequest.builder()
                    .setGoogleIdTokenRequestOptions(
                        BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                            .setSupported(true)
                            .setServerClientId("419871606766-lhm6hgu43eqi0qavthevuand2g5qsi57.apps.googleusercontent.com") // Replace with your actual Web Client ID
                            .setFilterByAuthorizedAccounts(false)
                            .build()
                    )
                    .build()

                Identity.getSignInClient(context).beginSignIn(signInRequest)
                    .addOnSuccessListener { result ->
                        launcher.launch(IntentSenderRequest.Builder(result.pendingIntent.intentSender).build())
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Google Sign-In Error: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Sign in with Google")
        }
    }
}