package com.example.fantasy_basketball

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class LoginFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var db: FirebaseFirestore

    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d("LoginFragment", "onViewCreated: Fragment created and Firebase initialized")

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Find email and password input fields
        emailEditText = view.findViewById(R.id.loginEmailEditText)
        passwordEditText = view.findViewById(R.id.loginPasswordEditText)

        // Configure Google Sign-In options
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireContext(), gso)
        // Revoke any lingering session when opening the login screen
        googleSignInClient.revokeAccess().addOnCompleteListener {
            googleSignInClient.signOut().addOnCompleteListener {
                Log.d("LoginFragment", "Google session terminated successfully.")
            }
        }

        // Handle Google Sign-In button click
        view.findViewById<ImageButton>(R.id.googleSignInButton).setOnClickListener {
            if (isGooglePlayServicesAvailable()) {
                signInWithGoogle()
            } else {
                Toast.makeText(requireContext(), "Google Play Services not available", Toast.LENGTH_SHORT).show()
            }
        }

        // Handle Login button click
        view.findViewById<Button>(R.id.loginButton).setOnClickListener {
            loginUser() // Call loginUser() when the login button is clicked
        }

        // Handle Sign-Up button click to navigate to SignupFragment
        view.findViewById<Button>(R.id.goToSignupButton).setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_signupFragment)
        }
    }

    private fun loginUser() {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        Log.d("LoginFragment", "Attempting login with email: $email")

        // Validate email and password
        if (email.isEmpty()) {
            Log.w("LoginFragment", "Email is empty")
            emailEditText.error = "Email is required"
            emailEditText.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Log.w("LoginFragment", "Invalid email format: $email")
            emailEditText.error = "Please enter a valid email"
            emailEditText.requestFocus()
            return
        }

        if (password.isEmpty()) {
            Log.w("LoginFragment", "Password is empty")
            passwordEditText.error = "Password is required"
            passwordEditText.requestFocus()
            return
        }

        // Sign in with FirebaseAuth
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    Log.d("LoginFragment", "Email/password login successful for user: ${auth.currentUser?.uid}")
                    val user = auth.currentUser
                    if (user?.isEmailVerified == true) {
                        Log.d("LoginFragment", "User email verified")
                        Toast.makeText(requireContext(), "Login successful!", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                    } else {
                        Log.w("LoginFragment", "User email not verified")
                        Toast.makeText(requireContext(), "You MUST verify your email in order to login.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e("LoginFragment", "Login failed: ${task.exception?.message}")
                    Toast.makeText(requireContext(), "Login failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // Launch Google Sign-In intent
    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    // Handle the Google Sign-In result
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                Log.d("LoginFragment", "Google sign-in success, attempting Firebase auth")
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Log.e("LoginFragment", "Google sign-in failed: ${e.message}")
                Toast.makeText(requireContext(), "Google sign-in failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Authenticate with Firebase using the Google ID token
    private fun firebaseAuthWithGoogle(idToken: String) {
        Log.d("LoginFragment", "firebaseAuthWithGoogle: Authenticating with Firebase using Google token")

        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    Log.d("LoginFragment", "Google sign-in successful, user: ${auth.currentUser?.uid}")
                    val user = auth.currentUser
                    if (user != null) {
                        checkAndAddUserToFirestore(user)
                    }
                    findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                } else {
                    Log.e("LoginFragment", "Google sign-in failed: ${task.exception?.message}")
                    Toast.makeText(requireContext(), "Google sign-in failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // Check if user exists in Firestore, add them if they don't
    private fun checkAndAddUserToFirestore(user: FirebaseUser) {
        val userRef = db.collection("users").document(user.uid)

        // Check if user exists in Firestore
        userRef.get().addOnSuccessListener { document ->
            if (!document.exists()) {
                Log.d("LoginFragment", "Adding user to Firestore")
                val newUser = hashMapOf(
                    "email" to user.email,
                    "leagues" to emptyList<String>(),  // Initialize as empty
                    "teams" to emptyList<String>()     // Initialize as empty
                )
                userRef.set(newUser)
                    .addOnSuccessListener {
                        Log.d("LoginFragment", "User added to Firestore")
                        Toast.makeText(requireContext(), "User added to Firestore", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Log.e("LoginFragment", "Error adding user to Firestore: ${e.message}")
                        Toast.makeText(requireContext(), "Error adding user to Firestore: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Log.d("LoginFragment", "User already exists in Firestore")
                Toast.makeText(requireContext(), "User already exists in Firestore", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            Log.e("LoginFragment", "Error checking user in Firestore: ${e.message}")
            Toast.makeText(requireContext(), "Error checking user in Firestore: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isGooglePlayServicesAvailable(): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(requireContext())
        if (resultCode != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                googleApiAvailability.getErrorDialog(requireActivity(), resultCode, 9000)?.show()
            } else {
                Log.e("LoginFragment", "Google Play Services not supported on this device")
                Toast.makeText(requireContext(), "This device is not supported.", Toast.LENGTH_LONG).show()
            }
            return false
        }
        return true
    }

    companion object {
        private const val RC_SIGN_IN = 9001
    }
}
