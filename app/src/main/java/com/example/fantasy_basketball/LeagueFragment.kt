package com.example.fantasy_basketball

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LeagueFragment : Fragment() {

    private lateinit var leagueId: String
    private lateinit var teamId: String
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    // TextView for team name and league name
    private lateinit var leagueNameTextView: TextView
    private lateinit var teamNameTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Retrieve the leagueId passed from HomeFragment
        arguments?.let {
            leagueId = it.getString("leagueId", "")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_league, container, false)

        // Initialize TextViews
        leagueNameTextView = view.findViewById(R.id.leagueName)
        teamNameTextView = view.findViewById(R.id.teamName)

        // Initialize the teamInfoButton and set its click listener
        val teamInfoButton: ImageButton = view.findViewById(R.id.teamInfoButton)
        teamInfoButton.setOnClickListener {
            navigateToTeamInfoFragment()
        }

        // Load the league and team data
        loadLeagueAndTeamData()

        return view
    }

    private fun loadLeagueAndTeamData() {
        val currentUserId = auth.currentUser?.uid ?: return

        // Fetch league details
        firestore.collection("Leagues").document(leagueId).get().addOnSuccessListener { leagueDoc ->
            if (leagueDoc.exists()) {
                val leagueName = leagueDoc.getString("leagueName") ?: "Unknown League"
                leagueNameTextView.text = leagueName

                // Fetch the user's team in this league
                firestore.collection("Leagues").document(leagueId).collection("Teams")
                    .whereEqualTo("ownerID", currentUserId).limit(1).get()
                    .addOnSuccessListener { teamsSnapshot ->
                        if (!teamsSnapshot.isEmpty) {
                            val teamDoc = teamsSnapshot.documents[0]
                            val teamName = teamDoc.getString("teamName") ?: "Unknown Team"
                            teamId = teamDoc.id  // Save the teamId for navigation
                            teamNameTextView.text = teamName
                        } else {
                            teamNameTextView.text = "No Team Found"
                        }
                    }
            }
        }
    }

    private fun navigateToTeamInfoFragment() {
        val bundle = Bundle().apply {
            putString("leagueId", leagueId)
            putString("teamId", teamId)
        }

        // Navigate to TeamInfoFragment and pass leagueId and teamId
        findNavController().navigate(R.id.action_leagueFragment_to_teamInfoFragment, bundle)
    }
}
