package com.example.fantasy_basketball

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class LeagueStandingsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: LeagueStandingsAdapter
    private lateinit var firestore: FirebaseFirestore
    private lateinit var noDataTextView: TextView // TextView for no data message
    private val sharedViewModel: SharedDataViewModel by activityViewModels()
    private var leagueId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_league_standings, container, false)

        // Initialize RecyclerView
        recyclerView = view.findViewById(R.id.leagueStandingsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)

        // Initialize "No Data" TextView
        noDataTextView = view.findViewById(R.id.noDataTextView)
        noDataTextView.visibility = View.GONE // Initially hide it

        // Firestore initialization
        firestore = FirebaseFirestore.getInstance()

        // Fetch League ID from arguments
        leagueId = arguments?.getString("leagueId")
        leagueId = sharedViewModel.leagueID
        if (leagueId != null) {
            leagueId?.let { fetchLeagueStandings(it)}
        } else {
            Log.e("LeagueStandingsFragment", "League ID is missing!")
            noDataTextView.text = "League ID is missing!"
            noDataTextView.visibility = View.VISIBLE
        }

        return view
    }

    private fun fetchLeagueStandings(leagueId: String) {
        firestore.collection("Leagues").document(leagueId).collection("Teams")
            .orderBy("wins", Query.Direction.DESCENDING) // Sort by wins descending
            .orderBy("losses", Query.Direction.ASCENDING) // Sort by losses ascending to prioritize fewer losses
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val teams = querySnapshot.documents.map { document ->
                        Team(
                            name = document.getString("teamName") ?: "Unknown",
                            wins = document.getLong("wins")?.toInt() ?: 0,
                            losses = document.getLong("losses")?.toInt() ?: 0,
                            points = document.getDouble("points") ?: 0.0,
                            iconUrl = document.getString("profilePictureUrl") ?: ""
                        )
                    }

                    // Log the team details for debugging
                    for (team in teams) {
                        Log.d("LeagueStandingsFragment", "Team: ${team.name}, Wins: ${team.wins}, Losses: ${team.losses}, Points: ${team.points}")
                    }

                    // Initialize and set the adapter
                    adapter = LeagueStandingsAdapter(teams)
                    recyclerView.adapter = adapter
                    recyclerView.visibility = View.VISIBLE
                    noDataTextView.visibility = View.GONE
                } else {
                    Log.d("LeagueStandingsFragment", "No teams found in the league.")
                    recyclerView.visibility = View.GONE
                    noDataTextView.text = "No standings available for this league."
                    noDataTextView.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener { e ->
                Log.e("LeagueStandingsFragment", "Error fetching standings", e)
                recyclerView.visibility = View.GONE
                noDataTextView.text = "Error fetching standings. Please try again later."
                noDataTextView.visibility = View.VISIBLE
            }
    }
}
