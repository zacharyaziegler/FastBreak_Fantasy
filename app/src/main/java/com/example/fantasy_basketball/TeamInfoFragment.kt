package com.example.fantasy_basketball

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firestore = FirebaseFirestore.getInstance()

        // Retrieve leagueId and teamId passed from the previous fragment
        arguments?.let {
            leagueId = it.getString("leagueId", "")
            teamId = it.getString("teamId", "")
        }
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
                    val teamName = teamDoc.getString("teamName") ?: "Unknown Team"
                    val profilePictureUrl = teamDoc.getString("profilePictureUrl") ?: ""

                    teamNameTextView.text = teamName

                    // Load profile picture using Glide (or any other image loading library)
                    if (profilePictureUrl.isNotEmpty()) {
                        Glide.with(this).load(profilePictureUrl).override(500, 500).into(teamImageView)
                    } else {
                        teamImageView.setImageResource(R.drawable.team_placeholder_image)  // Placeholder image if no URL
                    }
                }
            }
    }
}
