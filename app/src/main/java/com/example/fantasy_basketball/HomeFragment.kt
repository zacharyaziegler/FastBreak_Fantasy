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

        // Initialize Google Sign-In Client
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireContext(), gso)

        // Initialize ViewPager2 and adapter
        viewPager = view.findViewById(R.id.viewPager)
        matchupAdapter = MatchupAdapter(matchupsList)
        viewPager.adapter = matchupAdapter

        // Set "My Teams" title at the top
        view.findViewById<TextView>(R.id.myTeamsTitle).text = "My Teams"

        // Fetch and display matchups
        fetchUserMatchups()

        // Logout button functionality
        view.findViewById<Button>(R.id.logoutButton).setOnClickListener {
            try {
                Log.d("HomeFragment", "Logout button clicked")

                // Sign out of Firebase (this works for both Google and regular Firebase Auth)
                auth.signOut()

                // Check if the user is logged in with Google
                val googleSignInAccount = GoogleSignIn.getLastSignedInAccount(requireContext())
                if (googleSignInAccount != null) {
                    // If the user is signed in with Google, revoke access
                    googleSignInClient.revokeAccess().addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d("HomeFragment", "Google Sign-Out successful")
                            // After successful sign-out, navigate to the LoginFragment
                            findNavController().navigate(R.id.action_homeFragment_to_loginFragment)
                        } else {
                            Log.e("HomeFragment", "Google Sign-Out failed: ${task.exception?.message}")
                        }
                    }
                } else {
                    // If the user is not signed in with Google, directly navigate to the LoginFragment
                    findNavController().navigate(R.id.action_homeFragment_to_loginFragment)
                }

            } catch (e: Exception) {
                Log.e("HomeFragment", "Error during logout: ${e.message}")
            }
        }

        // Create a League button functionality
        view.findViewById<Button>(R.id.createLeagueBtn).setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_createLeagueFragment)
        }
    }

    private fun fetchUserMatchups() {
        val userId = auth.currentUser?.uid ?: return
        val userRef = firestore.collection("users").document(userId)

        // Fetch user details (leagues and teams)
        userRef.get().addOnSuccessListener { userDocument ->
            if (userDocument.exists()) {
                val leagues = userDocument.get("leagues") as? List<String> ?: emptyList()
                val teams = userDocument.get("teams") as? List<String> ?: emptyList()

                // Loop through leagues and fetch matchups for each league
                for ((index, leagueId) in leagues.withIndex()) {
                    val teamId = teams.getOrNull(index) ?: continue
                    fetchMatchupForTeam(leagueId, teamId)
                }
            } else {
                Toast.makeText(requireContext(), "User not found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchMatchupForTeam(leagueId: String, teamId: String) {
        val matchupsCollection = firestore.collection("Leagues").document(leagueId).collection("Matchups")

        // Fetch next pending matchup involving user's team (team1 or team2)
        matchupsCollection.whereEqualTo("team1ID", teamId).whereEqualTo("result", "pending").limit(1).get()
            .addOnSuccessListener { matchupsSnapshot ->
                if (!matchupsSnapshot.isEmpty) {
                    val matchup = matchupsSnapshot.documents[0]
                    val opponentTeamId = matchup.getString("team2ID") ?: return@addOnSuccessListener
                    fetchOpponentDetailsAndAddMatchup(leagueId, teamId, opponentTeamId)
                } else {
                    // If no matchups for team1, check if the team is team2
                    matchupsCollection.whereEqualTo("team2ID", teamId).whereEqualTo("result", "pending").limit(1).get()
                        .addOnSuccessListener { matchupsSnapshot2 ->
                            if (!matchupsSnapshot2.isEmpty) {
                                val matchup = matchupsSnapshot2.documents[0]
                                val opponentTeamId = matchup.getString("team1ID") ?: return@addOnSuccessListener
                                fetchOpponentDetailsAndAddMatchup(leagueId, teamId, opponentTeamId)
                            }
                        }
                }
            }
    }

    private fun fetchOpponentDetailsAndAddMatchup(leagueId: String, userTeamId: String, opponentTeamId: String) {
        // Fetch opponent team details
        firestore.collection("Leagues").document(leagueId).collection("Teams").document(opponentTeamId)
            .get().addOnSuccessListener { opponentTeamDoc ->
                if (opponentTeamDoc.exists()) {
                    val opponentTeamName = opponentTeamDoc.getString("teamName") ?: "Unknown"

                    // Fetch league name and display matchup
                    firestore.collection("Leagues").document(leagueId).get()
                        .addOnSuccessListener { leagueDoc ->
                            if (leagueDoc.exists()) {
                                val leagueName = leagueDoc.getString("leagueName") ?: "Unknown League"
                                addMatchupToViewPager(userTeamId, opponentTeamName, leagueName, leagueId)
                            }
                        }
                }
            }
    }

    private fun addMatchupToViewPager(userTeamId: String, opponentTeamName: String, leagueName: String, leagueId: String) {
        // Fetch user's team name
        firestore.collection("Leagues").document(leagueId).collection("Teams").document(userTeamId)
            .get().addOnSuccessListener { userTeamDoc ->
                if (userTeamDoc.exists()) {
                    val userTeamName = userTeamDoc.getString("teamName") ?: "My Team"

                    // Add the matchup to the list and update adapter
                    val matchupData = MatchupData(userTeamName, opponentTeamName, leagueName)
                    matchupsList.add(matchupData)
                    matchupAdapter.notifyDataSetChanged()

                    // Set the league name in the lower section
                    view?.findViewById<TextView>(R.id.leagueName)?.text = leagueName
                }
            }
    }
}
