package com.example.fantasy_basketball

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth

class SettingsFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Initialize Google Sign-In Client
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireContext(), gso)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        // Set up the "Change Password" button
        val changePasswordButton: Button = view.findViewById(R.id.changePasswordButton)
        changePasswordButton.setOnClickListener {
            checkUserAndRedirect()
        }

        // Set up the "Logout" button
        val logoutButton: Button = view.findViewById(R.id.logoutButton)
        logoutButton.setOnClickListener {
            logoutUser()
        }

        return view
    }

    // Function to check if the user is signed in with Google and redirect accordingly
    private fun checkUserAndRedirect() {
        val currentUser = auth.currentUser

        if (currentUser != null) {
            // Check if the user has signed in with Google
            val providers = currentUser.providerData
            var isGoogleUser = false

            for (provider in providers) {
                if (provider.providerId == "google.com") {
                    isGoogleUser = true
                    break
                }
            }

            if (isGoogleUser) {
                // Show a Toast message for Google sign-in users
                Toast.makeText(requireContext(), "Google sign in users must change their password through Google", Toast.LENGTH_LONG).show()
            } else {
                // Navigate to ChangePasswordFragment if it's not a Google sign-in user
                findNavController().navigate(R.id.action_settingsFragment_to_changePasswordFragment)
            }
        }
    }

    // Function to log out the user
    private fun logoutUser() {
        try {
            Log.d("SettingsFragment", "Logout button clicked")

            // Sign out of Firebase (works for both Google and regular Firebase Auth)
            auth.signOut()

            // Check if the user is logged in with Google
            val googleSignInAccount = GoogleSignIn.getLastSignedInAccount(requireContext())
            if (googleSignInAccount != null) {
                // If the user is signed in with Google, revoke access
                googleSignInClient.revokeAccess().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("SettingsFragment", "Google Sign-Out successful")
                        // After successful sign-out, navigate to the LoginFragment
                        findNavController().navigate(R.id.action_settingsFragment_to_loginFragment)
                    } else {
                        Log.e("SettingsFragment", "Google Sign-Out failed: ${task.exception?.message}")
                    }
                }
            } else {
                // If the user is not signed in with Google, directly navigate to the LoginFragment
                findNavController().navigate(R.id.action_settingsFragment_to_loginFragment)
            }

        } catch (e: Exception) {
            Log.e("SettingsFragment", "Error during logout: ${e.message}")
        }
    }
}
