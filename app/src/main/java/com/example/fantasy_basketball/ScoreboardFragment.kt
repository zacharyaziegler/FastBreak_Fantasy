package com.example.fantasy_basketball

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore

class ScoreboardFragment : Fragment() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var weekSpinner: Spinner
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ScoreboardPlayerAdapter
    private val sharedViewModel: SharedDataViewModel by activityViewModels()
    private var leagueId: String? = null
    private var selectedWeek: String = "week01" // Default week

    // Dummy data for testing
    private val teamAPlayers = listOf(
        Player(
            playerID = "101",
            longName = "LeBron James",
            pos = "SF",
            stats = PlayerStats(pts = "27.8")
        ),
        Player(
            playerID = "102",
            longName = "Anthony Davis",
            pos = "PF",
            stats = PlayerStats(pts = "24.1")
        ),
        Player(
            playerID = "103",
            longName = "Russell Westbrook",
            pos = "PG",
            stats = PlayerStats(pts = "19.6")
        )
    )

    private val teamBPlayers = listOf(
        Player(
            playerID = "201",
            longName = "Stephen Curry",
            pos = "PG",
            stats = PlayerStats(pts = "30.2")
        ),
        Player(
            playerID = "202",
            longName = "Klay Thompson",
            pos = "SG",
            stats = PlayerStats(pts = "22.3")
        ),
        Player(
            playerID = "203",
            longName = "Draymond Green",
            pos = "PF",
            stats = PlayerStats(pts = "10.5")
        )
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_scoreboard, container, false)
        firestore = FirebaseFirestore.getInstance()

        // Retrieve leagueId from arguments
        leagueId = arguments?.getString("leagueId")
        leagueId = sharedViewModel.leagueID
        // Initialize Spinner
        weekSpinner = view.findViewById(R.id.weekSpinner)
        setupWeekSpinner()

        // Initialize RecyclerView
        recyclerView = view.findViewById(R.id.playerMatchupRecyclerView)
        adapter = ScoreboardPlayerAdapter(teamAPlayers, teamBPlayers)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        // Fetch matchup details for the default week
        if (leagueId != null) {
            fetchMatchupDetails(view)
        } else {
            Toast.makeText(requireContext(), "League ID is missing.", Toast.LENGTH_SHORT).show()
        }

        return view
    }

    private fun setupWeekSpinner() {
        // Fetch available weeks dynamically
        firestore.collection("Leagues")
            .document(leagueId!!)
            .collection("Matchups")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val weeks = querySnapshot.documents.mapNotNull { it.getString("week") }.distinct()
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, weeks)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                weekSpinner.adapter = adapter

                // Set default selection
                weekSpinner.setSelection(0)

                // Listen for selection changes
                weekSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        selectedWeek = weeks[position]
                        fetchMatchupDetails(requireView())
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {
                        // Do nothing
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error fetching weeks: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchMatchupDetails(view: View) {
        firestore.collection("Leagues")
            .document(leagueId!!)
            .collection("Matchups")
            .whereEqualTo("week", selectedWeek)
            .limit(1)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val matchup = querySnapshot.documents[0]
                    val team1Id = matchup.getString("team1ID") ?: return@addOnSuccessListener
                    val team2Id = matchup.getString("team2ID") ?: return@addOnSuccessListener

                    // Fetch team details for both teams
                    fetchTeamDetails(view, team1Id, isTeam1 = true)
                    fetchTeamDetails(view, team2Id, isTeam1 = false)
                } else {
                    Toast.makeText(requireContext(), "No matchup found for this week.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error fetching matchup: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchTeamDetails(view: View, teamId: String, isTeam1: Boolean) {
        firestore.collection("Leagues")
            .document(leagueId!!)
            .collection("Teams")
            .document(teamId)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val teamName = documentSnapshot.getString("teamName") ?: "Unknown"
                    val teamImageUrl = documentSnapshot.getString("profilePictureUrl") ?: ""

                    if (isTeam1) {
                        view.findViewById<TextView>(R.id.team1Name).text = teamName
                        Glide.with(this).load(teamImageUrl).into(view.findViewById(R.id.team1Image))
                    } else {
                        view.findViewById<TextView>(R.id.team2Name).text = teamName
                        Glide.with(this).load(teamImageUrl).into(view.findViewById(R.id.team2Image))
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error fetching team details: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
