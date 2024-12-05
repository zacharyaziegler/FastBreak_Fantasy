package com.example.fantasy_basketball

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore

class TeamInfoFragment : Fragment() {

    private lateinit var leagueId: String
    private lateinit var teamId: String
    private lateinit var firestore: FirebaseFirestore

    private lateinit var teamImageView: ImageView
    private lateinit var teamNameTextView: TextView
    private lateinit var editTeamButton: Button

    private var currentTeamName: String = ""
    private var currentProfilePictureUrl: String = ""
    private val sharedViewModel: SharedDataViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firestore = FirebaseFirestore.getInstance()

        // Retrieve leagueId and teamId passed from the previous fragment
        arguments?.let {
            leagueId = it.getString("leagueId", "")
            teamId = it.getString("teamId", "")
        }

        leagueId = sharedViewModel.leagueID.toString()
        teamId = sharedViewModel.teamID.toString()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_team_info, container, false)

        // Initialize views
        teamImageView = view.findViewById(R.id.teamImageView)
        teamNameTextView = view.findViewById(R.id.teamNameTextView)
        editTeamButton = view.findViewById(R.id.editTeamButton)

        // Load team data
        loadTeamData()

        // Handle button click to navigate to EditTeamInfoFragment
        editTeamButton.setOnClickListener {
            val bundle = Bundle().apply {
                putString("leagueId", leagueId)
                putString("teamId", teamId)
                putString("teamName", currentTeamName)  // Pass current team name
                putString("profilePictureUrl", currentProfilePictureUrl)  // Pass current profile picture URL
            }
            findNavController().navigate(R.id.action_teamInfoFragment_to_editTeamInfoFragment, bundle)
        }

        return view
    }

    private fun loadTeamData() {
        firestore.collection("Leagues").document(leagueId).collection("Teams")
            .document(teamId).get()
            .addOnSuccessListener { teamDoc ->
                if (teamDoc.exists()) {
                    currentTeamName = teamDoc.getString("teamName") ?: "Unknown Team"
                    currentProfilePictureUrl = teamDoc.getString("profilePictureUrl") ?: ""

                    teamNameTextView.text = currentTeamName

                    // Load profile picture using Glide (or any other image loading library)
                    if (currentProfilePictureUrl.isNotEmpty()) {
                        Glide.with(this).load(currentProfilePictureUrl).override(500, 500).into(teamImageView)
                    } else {
                        teamImageView.setImageResource(R.drawable.team_placeholder_image)  // Placeholder image if no URL
                    }
                }
            }
    }
}
