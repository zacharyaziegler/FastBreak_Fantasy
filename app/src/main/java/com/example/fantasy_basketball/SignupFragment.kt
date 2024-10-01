package com.example.fantasy_basketball

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

class SignupFragment : Fragment() {
    // Firebase auth and Firestore instances
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // EditTexts
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText

    // TextViews for password requirements
    private lateinit var requirementLength: TextView
    private lateinit var requirementUppercase: TextView
    private lateinit var requirementLowercase: TextView
    private lateinit var requirementSpecialChar: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_signup, container, false)

        // Find views
        emailEditText = view.findViewById(R.id.signupEmailEditText)
        passwordEditText = view.findViewById(R.id.signupPasswordEditText)
        confirmPasswordEditText = view.findViewById(R.id.signupConfirmPasswordEditText)

        // Find password requirement TextViews
        requirementLength = view.findViewById(R.id.requirementLength)
        requirementUppercase = view.findViewById(R.id.requirementUppercase)
        requirementLowercase = view.findViewById(R.id.requirementLowercase)
        requirementSpecialChar = view.findViewById(R.id.requirementSpecialChar)

        // Handle Sign Up button click
        view.findViewById<Button>(R.id.signupButton).setOnClickListener {
            signUpUser()
        }

        // Handle "Back to Login" button click
        view.findViewById<Button>(R.id.goToLoginButton).setOnClickListener {
            findNavController().navigate(R.id.action_signupFragment_to_loginFragment)
        }

        // Add text change listener to the password field to check requirements
        passwordEditText.addTextChangedListener(passwordWatcher)

        return view
    }

    // TextWatcher to monitor password field changes
    private val passwordWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            val password = s.toString()
            checkPasswordRequirements(password)
        }

        override fun afterTextChanged(s: Editable?) {}
    }

    // Function to check password requirements
    private fun checkPasswordRequirements(password: String) {
        // At least 7 characters
        if (password.length >= 7) {
            updateRequirement(requirementLength, true)
        } else {
            updateRequirement(requirementLength, false)
        }

        // At least 1 uppercase letter
        if (password.any { it.isUpperCase() }) {
            updateRequirement(requirementUppercase, true)
        } else {
            updateRequirement(requirementUppercase, false)
        }

        // At least 1 lowercase letter
        if (password.any { it.isLowerCase() }) {
            updateRequirement(requirementLowercase, true)
        } else {
            updateRequirement(requirementLowercase, false)
        }

        // At least 1 special character
        if (password.any { "!@#$%^&*()-_+=?".contains(it) }) {
            updateRequirement(requirementSpecialChar, true)
        } else {
            updateRequirement(requirementSpecialChar, false)
        }
    }

    // Helper function to update the requirement TextView
    private fun updateRequirement(textView: TextView, isValid: Boolean) {
        val currentText = textView.text.toString()

        // Extract the text part (without the emoji)
        val requirementText = currentText.substring(2) // Skip the first 2 characters (emoji and space)

        // Update the emoji part only
        if (isValid) {
            textView.text = "✔ $requirementText"
            textView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark))
        } else {
            textView.text = "❌ $requirementText"
            textView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
        }
    }

    // Function to handle user sign-up with Firebase and Firestore
    private fun signUpUser() {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()
        val confirmPassword = confirmPasswordEditText.text.toString().trim()

        // Validate email
        if (email.isEmpty()) {
            emailEditText.error = "Email is required"
            emailEditText.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.error = "Please enter a valid email"
            emailEditText.requestFocus()
            return
        }

        // Validate password
        if (password.isEmpty()) {
            passwordEditText.error = "Password is required"
            passwordEditText.requestFocus()
            return
        }

        // Validate password length
        if (password.length < 7) {
            passwordEditText.error = "Password must be at least 7 characters"
            passwordEditText.requestFocus()
            return
        }

        // Validate if password contains at least 1 special character
        if (!password.any { "!@#$%^&*()-_+=?".contains(it) }) {
            passwordEditText.error = "Password must contain at least 1 special character"
            passwordEditText.requestFocus()
            return
        }

        // Validate if password contains at least 1 uppercase letter
        if (!password.any { it.isUpperCase() }) {
            passwordEditText.error = "Password must contain at least 1 uppercase letter"
            passwordEditText.requestFocus()
            return
        }

        // Validate if password contains at least 1 lowercase letter
        if (!password.any { it.isLowerCase() }) {
            passwordEditText.error = "Password must contain at least 1 lowercase letter"
            passwordEditText.requestFocus()
            return
        }

        // Validate if password and confirm password match
        if (confirmPassword != password) {
            confirmPasswordEditText.error = "Passwords do not match"
            confirmPasswordEditText.requestFocus()
            return
        }

        // Create the user using FirebaseAuth
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    // Send email verification
                    val user = auth.currentUser
                    user?.sendEmailVerification()?.addOnCompleteListener { verificationTask ->
                        if (verificationTask.isSuccessful) {
                            // Add user to Firestore
                            if (user != null) {
                                addUserToFirestore(user)
                            }
                            // Show success message and prompt user to verify email
                            Toast.makeText(requireContext(), "Account created. Please verify your email.", Toast.LENGTH_SHORT).show()
                            findNavController().navigate(R.id.action_signupFragment_to_loginFragment)
                        } else {
                            // If email sending fails
                            Toast.makeText(requireContext(), "Failed to send verification email: ${verificationTask.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    // If sign-up fails, display a message to the user
                    Toast.makeText(requireContext(), "Sign-up failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // Add the newly signed-up user to Firestore
    private fun addUserToFirestore(user: FirebaseUser) {
        val userRef = db.collection("users").document(user.uid)

        userRef.get().addOnSuccessListener { document ->
            if (!document.exists()) {
                // Add user to Firestore
                val newUser = hashMapOf(
                    "email" to user.email,
                    "leagues" to emptyList<String>(),  // Initialize as empty list
                    "teams" to emptyList<String>()     // Initialize as empty list
                )
                userRef.set(newUser)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "User added to Firestore", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Error adding user to Firestore: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(requireContext(), "User already exists in Firestore", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            Toast.makeText(requireContext(), "Error checking user in Firestore: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}