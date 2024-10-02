package com.example.fantasy_basketball

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class CreateLeagueFragment : Fragment() {

    private lateinit var leagueNameEditText: EditText
    private lateinit var leagueSizeRadioGroup: RadioGroup
    private lateinit var finalizeLeagueBtn: Button
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    // TODO: FIX MATCHUP GENERATION OF 20 MAN LEAGUE
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_create_league, container, false)

        // Initialize Firebase Firestore and Auth
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Initialize views
        leagueNameEditText = view.findViewById(R.id.leagueNameEditText)
        leagueSizeRadioGroup = view.findViewById(R.id.leagueSizeRadioGroup)
        finalizeLeagueBtn = view.findViewById(R.id.finalizeLeagueBtn)

        // Handle "Create League" button click
        finalizeLeagueBtn.setOnClickListener {
            val leagueName = leagueNameEditText.text.toString().trim()

            // Get selected league size
            val selectedSizeId = leagueSizeRadioGroup.checkedRadioButtonId
            val selectedSizeText = view.findViewById<RadioButton>(selectedSizeId)?.text.toString()
            val leagueSize = selectedSizeText.toInt()

            // Validate inputs
            if (leagueName.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a league name", Toast.LENGTH_SHORT).show()
            } else if (selectedSizeId == -1) {
                Toast.makeText(requireContext(), "Please select a league size", Toast.LENGTH_SHORT).show()
            } else {
                createLeague(leagueName, leagueSize)
            }
        }

        return view
    }

    // Create League function
    private fun createLeague(leagueName: String, leagueSize: Int) {
        val commissionerID = auth.currentUser?.uid ?: return

        // League Document Data
        val leagueData = hashMapOf(
            "leagueName" to leagueName,
            "commissionerID" to commissionerID,
            "members" to arrayListOf(commissionerID),  // Commissioner starts as the only member
            "settings" to hashMapOf(
                "scoringType" to "Head to Head Points",
                "leagueSize" to leagueSize
            ),
            "draftStatus" to "not_started"
        )

        // Create League document in Firestore
        val leagueRef = firestore.collection("Leagues").document()
        leagueRef.set(leagueData)
            .addOnSuccessListener {
                // Create Teams and Matchups Subcollections
                createTeamsSubcollection(leagueRef.id, leagueSize)
                createMatchupsSubcollection(leagueRef.id, leagueSize)
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to create league: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Create Teams Subcollection
    private fun createTeamsSubcollection(leagueID: String, leagueSize: Int) {
        val teamsCollection = firestore.collection("Leagues").document(leagueID).collection("Teams")

        for (i in 1..leagueSize) {
            val teamID = UUID.randomUUID().toString()
            val teamData = hashMapOf(
                "teamName" to "Team $i",
                "ownerID" to "",  // Owner will be assigned later
                "roster" to arrayListOf<String>(),
                "points" to 0,
                "leagueID" to leagueID,
                "wins" to 0,
                "losses" to 0
            )
            teamsCollection.document(teamID).set(teamData)
        }
    }

    // Create Matchups Subcollection with 19 weeks of scheduling
    private fun createMatchupsSubcollection(leagueID: String, leagueSize: Int) {
        val matchupsCollection = firestore.collection("Leagues").document(leagueID).collection("Matchups")

        // Generate round-robin matchups for unique weeks (ensures everyone plays everyone once)
        val teamIDs = (1..leagueSize).toList()
        val roundRobinSchedule = generateRoundRobinSchedule(teamIDs)

        // Generate 19 weeks of matchups
        val totalWeeks = 19
        val numUniqueWeeks = roundRobinSchedule.size

        // Extra weeks to fill until week 19 (we will reshuffle existing matchups to fill remaining weeks)
        val extraMatchups = mutableListOf<List<Pair<Int, Int>>>()
        roundRobinSchedule.forEach { extraMatchups.add(it) }

        for (week in 1..totalWeeks) {
            val matchups = if (week <= numUniqueWeeks) {
                // Use unique matchups for the first N weeks
                roundRobinSchedule[week - 1]
            } else {
                // Reuse matchups for remaining weeks using the modulus operator
                extraMatchups[(week - numUniqueWeeks - 1) % extraMatchups.size]
            }

            createMatchupsForWeek(matchupsCollection, week, matchups)
        }
    }


    // Helper to create matchups for a given week
    private fun createMatchupsForWeek(
        matchupsCollection: CollectionReference,
        week: Int,
        matchups: List<Pair<Int, Int>>
    ) {
        val weekStr = String.format("week%02d", week)

        for (matchup in matchups) {
            val matchupID = "${weekStr}_team${matchup.first}_team${matchup.second}"
            val matchupData = hashMapOf(
                "week" to weekStr,
                "team1ID" to "team${matchup.first}",
                "team2ID" to "team${matchup.second}",
                "team1Score" to 0,
                "team2Score" to 0,
                "result" to "pending"
            )
            matchupsCollection.document(matchupID).set(matchupData)
        }
    }

    // Generate Round-Robin Schedule (each team plays every other team once)
    private fun generateRoundRobinSchedule(teams: List<Int>): List<List<Pair<Int, Int>>> {
        val schedule = mutableListOf<List<Pair<Int, Int>>>()
        val numTeams = teams.size
        val numRounds = numTeams - 1  // Each team plays every other team once
        val teamList = teams.toMutableList()

        // Add a dummy team if odd number of teams
        if (numTeams % 2 != 0) {
            teamList.add(0)  // Dummy team with ID 0 to handle odd number of teams
        }

        for (round in 0 until numRounds) {
            val weekMatchups = mutableListOf<Pair<Int, Int>>()
            val teamsUsed = mutableSetOf<Int>()  // Track teams used to avoid double scheduling

            for (i in 0 until teamList.size / 2) {
                val team1 = teamList[i]
                val team2 = teamList[teamList.size - 1 - i]

                // Only schedule valid matchups (ignore dummy team 0 if present)
                if (team1 != 0 && team2 != 0 && !teamsUsed.contains(team1) && !teamsUsed.contains(team2)) {
                    weekMatchups.add(Pair(team1, team2))
                    teamsUsed.add(team1)
                    teamsUsed.add(team2)
                }
            }

            schedule.add(weekMatchups)

            // Rotate teams for next round while keeping the first team fixed
            teamList.add(1, teamList.removeAt(teamList.size - 1))
        }

        return schedule
    }
}
