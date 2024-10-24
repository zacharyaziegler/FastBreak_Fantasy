package com.example.fantasy_basketball

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth

class ChangePasswordFragment : Fragment() {

    private lateinit var auth: FirebaseAuth

    // EditTexts
    private lateinit var oldPasswordEditText: EditText
    private lateinit var newPasswordEditText: EditText
    private lateinit var confirmNewPasswordEditText: EditText

    // TextViews for password requirements
    private lateinit var requirementLength: TextView
    private lateinit var requirementUppercase: TextView
    private lateinit var requirementLowercase: TextView
    private lateinit var requirementSpecialChar: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_change_password, container, false)

        // Find views
        oldPasswordEditText = view.findViewById(R.id.oldPasswordEditText)
        newPasswordEditText = view.findViewById(R.id.newPasswordEditText)
        confirmNewPasswordEditText = view.findViewById(R.id.confirmNewPasswordEditText)

        // Find password requirement TextViews
        requirementLength = view.findViewById(R.id.requirementLength)
        requirementUppercase = view.findViewById(R.id.requirementUppercase)
        requirementLowercase = view.findViewById(R.id.requirementLowercase)
        requirementSpecialChar = view.findViewById(R.id.requirementSpecialChar)

        // Add text change listener to the new password field to check requirements
        newPasswordEditText.addTextChangedListener(passwordWatcher)

        // Set up the change password button
        val changePasswordButton: Button = view.findViewById(R.id.changePasswordButton)
        changePasswordButton.setOnClickListener {
            validateAndChangePassword()
        }

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
        updateRequirement(requirementLength, password.length >= 7)

        // At least 1 uppercase letter
        updateRequirement(requirementUppercase, password.any { it.isUpperCase() })

        // At least 1 lowercase letter
        updateRequirement(requirementLowercase, password.any { it.isLowerCase() })

        // At least 1 special character
        updateRequirement(requirementSpecialChar, password.any { "!@#$%^&*()-_+=?".contains(it) })
    }

    // Helper function to update the requirement TextView
    private fun updateRequirement(textView: TextView, isValid: Boolean) {
        val currentText = textView.text.toString()
        val requirementText = currentText.substring(2) // Skip the first 2 characters (emoji and space)

        if (isValid) {
            textView.text = "✔ $requirementText"
            textView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark))
        } else {
            textView.text = "❌ $requirementText"
            textView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
        }
    }

    // Validate the password and perform password change
    private fun validateAndChangePassword() {
        val oldPassword = oldPasswordEditText.text.toString().trim()
        val newPassword = newPasswordEditText.text.toString().trim()
        val confirmNewPassword = confirmNewPasswordEditText.text.toString().trim()

        // Validate old password (this could be used for re-authentication if needed)
        if (oldPassword.isEmpty()) {
            Toast.makeText(requireContext(), "Old password is required", Toast.LENGTH_SHORT).show()
            return
        }

        // Validate new password requirements
        if (newPassword.length < 7 || !newPassword.any { it.isUpperCase() } ||
            !newPassword.any { it.isLowerCase() } || !newPassword.any { "!@#$%^&*()-_+=?".contains(it) }
        ) {
            Toast.makeText(requireContext(), "New password does not meet the requirements", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if new password and confirm password match
        if (newPassword != confirmNewPassword) {
            Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        // Change password logic (assuming the user is authenticated)
        val currentUser = auth.currentUser
        currentUser?.updatePassword(newPassword)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(requireContext(), "Password changed successfully", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_changePasswordFragment_to_settingsFragment)

                } else {
                    Toast.makeText(requireContext(), "Failed to change password: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
