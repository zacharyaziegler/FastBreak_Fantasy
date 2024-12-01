package com.example.fantasy_basketball

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class CreateLeagueFragment : Fragment() {

    private lateinit var leagueNameEditText: EditText
    private lateinit var leagueSizeRadioGroup: RadioGroup
    private lateinit var finalizeLeagueBtn: Button
    private lateinit var progressBar: ProgressBar
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
        progressBar = view.findViewById(R.id.progressBar)

        // Handle "Create League" button click
        finalizeLeagueBtn.setOnClickListener {
            val leagueName = leagueNameEditText.text.toString().trim()

            // Get selected league size
            val selectedSizeId = leagueSizeRadioGroup.checkedRadioButtonId
            val selectedSizeText = view.findViewById<RadioButton>(selectedSizeId)?.text.toString()

            // Validate inputs
            if (leagueName.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a league name", Toast.LENGTH_SHORT).show()
            } else if (selectedSizeId == -1) {
                Toast.makeText(requireContext(), "Please select a league size", Toast.LENGTH_SHORT).show()
            } else {
                // Show progress bar and start creation
                val leagueSize = selectedSizeText.toInt()
                progressBar.visibility = View.VISIBLE
                progressBar.progress = 10 // Initial progress
                createLeague(leagueName, leagueSize)
            }
        }

        return view
    }

    // Create League function
    private fun createLeague(leagueName: String, leagueSize: Int) {
        val commissionerID = auth.currentUser?.uid ?: return

        // League Document Data
        val inviteCode = generateInviteCode()  // Create 6-letter invite code
        val leagueData = hashMapOf(
            "leagueName" to leagueName,
            "commissionerID" to commissionerID,
            "members" to arrayListOf(commissionerID),  // Commissioner starts as the only member
            "inviteCode" to inviteCode,  // Store invite code in the league document
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
                progressBar.progress = 30 // Update progress
                // Create Teams and Matchups Subcollections
                createTeamsSubcollection(leagueRef.id, leagueSize, commissionerID)
                createMessagesSubcollection(leagueRef.id,commissionerID)

                // Navigate to InviteFriendsFragment after creation
                val bundle = Bundle().apply {
                    putString("leagueId", leagueRef.id)
                    putString("inviteCode", inviteCode)
                    putString("leagueName", leagueName)
                }

                Toast.makeText(requireContext(), "League creation complete!", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_createLeagueFragment_to_inviteFriendsFragment, bundle)
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Failed to create league: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    // Create Messages Subcollection in Firestore
    private fun createMessagesSubcollection(leagueID: String, commissionerID: String) {
        val messagesCollection = firestore.collection("Leagues").document(leagueID).collection("messages")

        // List of initial messages
        val initialMessages = listOf(
            mapOf(
                "senderId" to commissionerID,
                "messageText" to "Welcome to the league chat!",
                "timestamp" to FieldValue.serverTimestamp(),
            ),
            mapOf(
                "senderId" to "other_user_id",
                "messageText" to "Let’s have a great season!",
                "timestamp" to FieldValue.serverTimestamp(),
            )
        )

        // Add each message to the messages subcollection
        for (messageData in initialMessages) {
            messagesCollection.add(messageData)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Initial message added to league chat", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Failed to add initial message: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
        Toast.makeText(requireContext(), "League and messages created!", Toast.LENGTH_SHORT).show()
    }



    // Create Teams Subcollection
    private fun createTeamsSubcollection(leagueID: String, leagueSize: Int, commissionerID: String) {
        val teamsCollection = firestore.collection("Leagues").document(leagueID).collection("Teams")
        val teamIDList = mutableListOf<String>()
        var teamsCreated = 0

        for (i in 1..leagueSize) {
            val teamID = UUID.randomUUID().toString()
            val teamData = hashMapOf(
                "teamName" to "Team $i",
                "ownerID" to if (i == 1) commissionerID else "",  // Assign commissioner to Team 1
                "roster" to arrayListOf<String>(),
                "Bench" to arrayListOf<String>(),
                "Starting" to arrayListOf<String>(),
                "points" to 0,
                "leagueID" to leagueID,
                "wins" to 0,
                "losses" to 0,
                "profilePictureUrl" to ""
            )

            teamsCollection.document(teamID).set(teamData)
                .addOnSuccessListener {
                    teamIDList.add(teamID) // Add teamID to the draft order list
                    teamsCreated++
                    progressBar.progress = (30 + (teamsCreated.toFloat() / leagueSize * 30)).toInt()

                    // Once all teams are created, finalize draft order and matchups
                    if (teamsCreated == leagueSize) {
                        assignCommissionerToTeam(leagueID, commissionerID, teamIDList[0])
                        finalizeDraftOrder(leagueID, teamIDList) // Add draft order to the league
                        createMatchupsSubcollection(leagueID, teamIDList)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to create team: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // Create Matchups Subcollection in Firestore with 19 weeks of scheduling
    private fun createMatchupsSubcollection(leagueID: String, teamIDList: List<String>) {
        val matchupsCollection = firestore.collection("Leagues").document(leagueID).collection("Matchups")

        val roundRobinSchedule = generateRoundRobinSchedule(teamIDList)
        val totalWeeks = 19
        val numUniqueWeeks = roundRobinSchedule.size

        // Create matchups for all weeks
        for (week in 1..totalWeeks) {
            val matchups = if (week <= numUniqueWeeks) {
                roundRobinSchedule[week - 1]
            } else {
                roundRobinSchedule[(week - numUniqueWeeks - 1) % numUniqueWeeks]
            }

            createMatchupsForWeek(matchupsCollection, week, matchups)
        }

        progressBar.progress = 100  // Set progress to 100% once matchups are created
        Toast.makeText(requireContext(), "League and matchups created!", Toast.LENGTH_SHORT).show()
    }

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
                "team1ID" to matchup.first,
                "team2ID" to matchup.second,
                "team1Score" to 0,
                "team2Score" to 0,
                "result" to "pending"
            )
            matchupsCollection.document(matchupID).set(matchupData)
        }
    }

    // Generate round-robin schedule using team IDs
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

    // Generate a 6-letter invite code
    private fun generateInviteCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        return (1..6)
            .map { chars.random() }
            .joinToString("")
    }

    // Assign commissioner to team and update user data
    private fun assignCommissionerToTeam(leagueID: String, commissionerID: String, teamID: String) {
        val userRef = firestore.collection("users").document(commissionerID)

        userRef.get().addOnSuccessListener { userDocument ->
            if (userDocument.exists()) {
                userRef.update(
                    "leagues", com.google.firebase.firestore.FieldValue.arrayUnion(leagueID),
                    "teams", com.google.firebase.firestore.FieldValue.arrayUnion(teamID)
                )
            }
        }
    }

    // Finalize the draft order by adding it to the league document
    private fun finalizeDraftOrder(leagueID: String, teamIDList: List<String>) {
        val leagueRef = firestore.collection("Leagues").document(leagueID)

        val draftOrder = teamIDList // Make it teamIDList.shuffled() for random, else it is in order
        leagueRef.update("draftOrder", draftOrder)
            .addOnSuccessListener {
                Log.d("CreateLeagueFragment", "Draft order set successfully: $draftOrder")
            }
            .addOnFailureListener { e ->
                Log.e("CreateLeagueFragment", "Failed to set draft order: ${e.message}", e)
            }
    }
}