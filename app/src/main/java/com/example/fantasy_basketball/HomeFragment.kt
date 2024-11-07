package com.example.fantasy_basketball

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.example.fantasy_basketball.matchup_display_logic.MatchupAdapter
import com.example.fantasy_basketball.matchup_display_logic.MatchupData
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var viewPager: ViewPager2
    private lateinit var matchupAdapter: MatchupAdapter
    private lateinit var firestore: FirebaseFirestore

    // Create a list to store matchups
    private var matchupsList: MutableList<MatchupData> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireContext(), gso)

        // Initialize ViewPager2 and adapter
        viewPager = view.findViewById(R.id.viewPager)
        matchupAdapter = MatchupAdapter(matchupsList) { leagueId, leagueName ->
            // Use a Bundle to pass the leagueId and leagueName to the LeagueFragment
            val bundle = Bundle().apply {
                putString("leagueId", leagueId)
                putString("leagueName", leagueName)
            }
            findNavController().navigate(R.id.action_homeFragment_to_leagueFragment, bundle)
        }
        viewPager.adapter = matchupAdapter

        // Set "My Teams" title at the top
        view.findViewById<TextView>(R.id.myTeamsTitle).text = "My Teams"

        // Fetch and display matchups
        fetchUserMatchups()

        // Create a League button functionality
        view.findViewById<Button>(R.id.createLeagueBtn).setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_createLeagueFragment)
        }

        // Join a League button functionality
        view.findViewById<Button>(R.id.joinLeagueBtn).setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_joinLeagueFragment)
        }
    }

    private fun fetchUserMatchups() {
        val userId = auth.currentUser?.uid ?: return
        val userRef = firestore.collection("users").document(userId)
        val currentWeek = "week01" // TODO: Add logic to track which week

        // Fetch user details (leagues and teams)
        userRef.get().addOnSuccessListener { userDocument ->
            if (userDocument.exists()) {
                val leagues = userDocument.get("leagues") as? List<String> ?: emptyList()
                val teams = userDocument.get("teams") as? List<String> ?: emptyList()

                // Loop through leagues and fetch matchups for each league
                for ((index, leagueId) in leagues.withIndex()) {
                    val teamId = teams.getOrNull(index) ?: continue
                    fetchMatchupForTeam(leagueId, teamId, currentWeek)
                }
            } else {
                Toast.makeText(requireContext(), "User not found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchMatchupForTeam(leagueId: String, teamId: String, currentWeek: String) {
        val matchupsCollection = firestore.collection("Leagues").document(leagueId).collection("Matchups")

        // Query for matchups where the user's team is either team1ID or team2ID and for the current week
        matchupsCollection
            .whereEqualTo("result", "pending")
            .whereEqualTo("week", currentWeek)  // Filter by the current week
            .whereIn("team1ID", listOf(teamId))
            .get()
            .addOnSuccessListener { team1Snapshot ->
                val team1Matchups = team1Snapshot.documents

                matchupsCollection
                    .whereEqualTo("result", "pending")
                    .whereEqualTo("week", currentWeek)  // Filter by the current week
                    .whereIn("team2ID", listOf(teamId))
                    .get()
                    .addOnSuccessListener { team2Snapshot ->
                        val team2Matchups = team2Snapshot.documents

                        // Combine matchups for both team1 and team2
                        val combinedMatchups = team1Matchups + team2Matchups

                        if (combinedMatchups.isNotEmpty()) {
                            val matchup = combinedMatchups[0]
                            val team1ID = matchup.getString("team1ID")
                            val team2ID = matchup.getString("team2ID")

                            // Determine which team is the opponent based on current user's teamID
                            val opponentTeamId = if (teamId == team1ID) team2ID else team1ID

                            if (opponentTeamId != null) {
                                fetchOpponentDetailsAndAddMatchup(leagueId, teamId, opponentTeamId)
                            }
                        } else {
                            Toast.makeText(requireContext(), "No matchups found for the current week.", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
    }


    private fun fetchOpponentDetailsAndAddMatchup(
        leagueId: String,
        userTeamId: String,
        opponentTeamId: String?
    ) {
        Log.d("HomeFragment","FetchOppenentDetailsAndAddMatchup entered")
        if (opponentTeamId == null) return

        // Fetch opponent team details (including image URL)
        firestore.collection("Leagues").document(leagueId).collection("Teams")
            .document(opponentTeamId)
            .get().addOnSuccessListener { opponentTeamDoc ->
                if (opponentTeamDoc.exists()) {
                    val opponentTeamName = opponentTeamDoc.getString("teamName") ?: "Unknown"
                    val opponentTeamImageUrl = opponentTeamDoc.getString("profilePictureUrl") ?: ""

                    // Fetch league name and user's team details
                    firestore.collection("Leagues").document(leagueId).get()
                        .addOnSuccessListener { leagueDoc ->
                            if (leagueDoc.exists()) {
                                val leagueName =
                                    leagueDoc.getString("leagueName") ?: "Unknown League"

                                // Fetch user's team name and image
                                firestore.collection("Leagues").document(leagueId)
                                    .collection("Teams")
                                    .document(userTeamId).get()
                                    .addOnSuccessListener { userTeamDoc ->
                                        if (userTeamDoc.exists()) {
                                            val userTeamName =
                                                userTeamDoc.getString("teamName") ?: "My Team"
                                            val userTeamImageUrl =
                                                userTeamDoc.getString("profilePictureUrl") ?: ""

                                            // Add the matchup to the list with image URLs and update the adapter
                                            val matchupData = MatchupData(
                                                userTeamName,
                                                opponentTeamName,
                                                leagueName,
                                                leagueId,
                                                userTeamImageUrl,
                                                opponentTeamImageUrl
                                            )
                                            matchupsList.add(matchupData)
                                            matchupAdapter.notifyDataSetChanged()
                                        }
                                    }
                            }
                        }
                }
            }
    }
}