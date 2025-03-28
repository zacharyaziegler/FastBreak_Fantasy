package com.example.fantasy_basketball

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore

class EditTeamInfoFragment : Fragment() {

    private lateinit var teamNameEditText: EditText
    private lateinit var profilePicUrlEditText: EditText
    private lateinit var saveTeamButton: Button
    private lateinit var editTeamImageView: ImageView
    private lateinit var firestore: FirebaseFirestore

    private lateinit var leagueId: String
    private lateinit var teamId: String

    private var currentTeamName: String = ""
    private var currentProfilePictureUrl: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firestore = FirebaseFirestore.getInstance()

        // Retrieve leagueId, teamId, teamName, and profilePictureUrl passed from TeamInfoFragment
        arguments?.let {
            leagueId = it.getString("leagueId", "")
            teamId = it.getString("teamId", "")
            currentTeamName = it.getString("teamName", "")  // Get the current team name
            currentProfilePictureUrl = it.getString("profilePictureUrl", "")  // Get the current profile pic URL
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_edit_team_info, container, false)

        // Initialize views
//        toolbar = view.findViewById(R.id.editTeamToolbar)
        teamNameEditText = view.findViewById(R.id.teamNameEditText)
        profilePicUrlEditText = view.findViewById(R.id.profilePicUrlEditText)
        saveTeamButton = view.findViewById(R.id.saveTeamButton)
        editTeamImageView = view.findViewById(R.id.teamImageView)

        // Set up the toolbar
//        setupToolbar()

        // Prepopulate the EditTexts with the current data
        teamNameEditText.setText(currentTeamName)
        profilePicUrlEditText.setText(currentProfilePictureUrl)

        // Load the current profile picture using Glide
        if (currentProfilePictureUrl.isNotEmpty()) {
            Glide.with(this)
                .load(currentProfilePictureUrl)
                .override(500, 500)
                .into(editTeamImageView)
        } else {
            editTeamImageView.setImageResource(R.drawable.team_placeholder_image) // Placeholder image if no URL is provided
        }

        // Set button click listener to save team info
        saveTeamButton.setOnClickListener {
            saveTeamInfo()
        }

        return view
    }

//    private fun setupToolbar() {
//        toolbar.setNavigationIcon(R.drawable.ic_arrow_back) // Use a back arrow icon
//        toolbar.setNavigationOnClickListener {
//            findNavController().popBackStack() // Navigate back to the previous fragment
//        }
//    }

    private fun saveTeamInfo() {
        val teamName = teamNameEditText.text.toString().trim()
        val profilePicUrl = profilePicUrlEditText.text.toString().trim()

        // Validation for team name
        if (TextUtils.isEmpty(teamName)) {
            teamNameEditText.error = "Team name cannot be empty"
            return
        }

        if (teamName.length > 20) {
            teamNameEditText.error = "Team name cannot exceed 20 characters"
            return
        }

        // Prepare the data to update in Firestore
        val updateData = mutableMapOf<String, Any>()
        updateData["teamName"] = teamName

        // Only add profilePicUrl if it's not empty
        if (profilePicUrl.isNotEmpty()) {
            updateData["profilePictureUrl"] = profilePicUrl
        }

        // Update the team document in Firestore
        firestore.collection("Leagues").document(leagueId).collection("Teams").document(teamId)
            .update(updateData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Team info updated successfully", Toast.LENGTH_SHORT).show()

                // Navigate back to TeamInfoFragment
                val bundle = Bundle().apply {
                    putString("leagueId", leagueId)
                    putString("teamId", teamId)
                }
                findNavController().navigate(R.id.action_editTeamInfoFragment_to_teamInfoFragment, bundle)
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to update team info: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
