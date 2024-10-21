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
import androidx.navigation.fragment.findNavController
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
                createTeamsSubcollection(leagueRef.id, leagueSize, commissionerID)

                // After the league is successfully created and teams are assigned,
                // navigate back to the home screen
                findNavController().navigate(R.id.action_createLeagueFragment_to_homeFragment)
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to create league: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Create Teams Subcollection
    private fun createTeamsSubcollection(leagueID: String, leagueSize: Int, commissionerID: String) {
        val teamsCollection = firestore.collection("Leagues").document(leagueID).collection("Teams")
        val teamIDList = mutableListOf<String>()
        var teamsCreated = 0  // Track the number of teams created

        for (i in 1..leagueSize) {
            val teamID = UUID.randomUUID().toString()
            val teamData = hashMapOf(
                "teamName" to "Team $i",
                "ownerID" to if (i == 1) commissionerID else "",  // Assign commissioner to Team 1
                "roster" to arrayListOf<String>(),
                "points" to 0,
                "leagueID" to leagueID,
                "wins" to 0,
                "losses" to 0,
                "profilePictureUrl" to ""
            )

            teamsCollection.document(teamID).set(teamData)
                .addOnSuccessListener {
                    teamIDList.add(teamID)  // Add teamID to the list
                    teamsCreated++

                    // Once all teams are created, assign commissioner and generate matchups
                    if (teamsCreated == leagueSize) {
                        // After all teams are created, assign the commissioner to Team 1
                        assignCommissionerToTeam(leagueID, commissionerID, teamIDList[0])

                        // Generate matchups after commissioner is assigned
                        createMatchupsSubcollection(leagueID, teamIDList)
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Failed to create team: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }


    // Function to assign the commissioner to Team 1 and update the User's leagues and teams
    private fun assignCommissionerToTeam(leagueID: String, commissionerID: String, teamID: String) {
        val userRef = firestore.collection("users").document(commissionerID)

        // Check if the user document exists
        userRef.get().addOnSuccessListener { userDocument ->
            if (userDocument.exists()) {
                val userLeagues = userDocument.get("leagues") as? MutableList<String> ?: mutableListOf()
                val userTeams = userDocument.get("teams") as? MutableList<String> ?: mutableListOf()

                // Add the leagueID and teamID to the User's fields using arrayUnion
                userRef.update(
                    "leagues", com.google.firebase.firestore.FieldValue.arrayUnion(leagueID),
                    "teams", com.google.firebase.firestore.FieldValue.arrayUnion(teamID)
                ).addOnSuccessListener {
                    Toast.makeText(requireContext(), "Commissioner assigned to Team 1", Toast.LENGTH_SHORT).show()
                }.addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Failed to update commissioner data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "User document does not exist", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            Toast.makeText(requireContext(), "Error fetching user data: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }



    // Create Matchups Subcollection in Firestore with 19 weeks of scheduling
    private fun createMatchupsSubcollection(leagueID: String, teamIDList: List<String>) {
        val matchupsCollection = firestore.collection("Leagues").document(leagueID).collection("Matchups")

        val roundRobinSchedule = generateRoundRobinSchedule(teamIDList)

        val totalWeeks = 19
        val numUniqueWeeks = roundRobinSchedule.size

        // Extra weeks to fill until week 19 (reshuffle existing matchups)
        val extraMatchups = mutableListOf<List<Pair<String, String>>>()
        roundRobinSchedule.forEach { extraMatchups.add(it) }

        for (week in 1..totalWeeks) {
            val matchups = if (week <= numUniqueWeeks) {
                roundRobinSchedule[week - 1]
            } else {
                extraMatchups[(week - numUniqueWeeks - 1) % extraMatchups.size]
            }

            createMatchupsForWeek(matchupsCollection, week, matchups)
        }
    }

    // Helper to create matchups for a given week
    private fun createMatchupsForWeek(
        matchupsCollection: CollectionReference,
        week: Int,
        matchups: List<Pair<String, String>>
    ) {
        val weekStr = String.format("week%02d", week)

        for (matchup in matchups) {
            val matchupID = "${weekStr}_${matchup.first}_${matchup.second}"
            val matchupData = hashMapOf(
                "week" to weekStr,
                "team1ID" to matchup.first,  // Use actual teamID
                "team2ID" to matchup.second,  // Use actual teamID
                "team1Score" to 0,
                "team2Score" to 0,
                "result" to "pending"
            )
            matchupsCollection.document(matchupID).set(matchupData)
        }
    }

    // Generate round robin schedule using team IDs instead of numeric values
    private fun generateRoundRobinSchedule(teamIDs: List<String>): List<List<Pair<String, String>>> {
        val schedule = mutableListOf<List<Pair<String, String>>>()
        val numTeams = teamIDs.size
        val numRounds = numTeams - 1
        val teamList = teamIDs.toMutableList()

        if (numTeams % 2 != 0) {
            teamList.add("BYE")  // Add a dummy team for odd number of teams
        }

        for (round in 0 until numRounds) {
            val weekMatchups = mutableListOf<Pair<String, String>>()
            val teamsUsed = mutableSetOf<String>()

            for (i in 0 until teamList.size / 2) {
                val team1 = teamList[i]
                val team2 = teamList[teamList.size - 1 - i]

                if (team1 != "BYE" && team2 != "BYE" && !teamsUsed.contains(team1) && !teamsUsed.contains(team2)) {
                    weekMatchups.add(Pair(team1, team2))
                    teamsUsed.add(team1)
                    teamsUsed.add(team2)
                }
            }

            schedule.add(weekMatchups)
            teamList.add(1, teamList.removeAt(teamList.size - 1))  // Rotate teams for the next round
        }

        return schedule
    }
}
