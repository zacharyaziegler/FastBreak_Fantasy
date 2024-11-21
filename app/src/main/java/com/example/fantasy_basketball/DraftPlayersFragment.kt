package com.example.fantasy_basketball

import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.*

class DraftPlayersFragment : Fragment() {

    private lateinit var countdownTimerView: TextView
    private lateinit var roundPickInfoView: TextView
    private lateinit var currentTeamNameView: TextView
    private lateinit var onTheClockLabel: TextView

    private lateinit var playerAPIService: PlayerAPIService
    private lateinit var playerAdapter: DraftPlayerAdapter
    private lateinit var firestore: FirebaseFirestore
    private lateinit var leagueId: String
    private lateinit var teamId: String
    private var isDataLoaded = false
    private var isUserOnTheClock: Boolean = false

    private var cachedPlayers = mutableListOf<Player>()
    private var countdownTimer: CountDownTimer? = null
    private var draftListenerRegistration: ListenerRegistration? = null

    // Configurable pick duration (in seconds)
    private val PICK_DURATION_SECONDS = 5 // Change to 60 for production

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_draft_players, container, false)

        firestore = FirebaseFirestore.getInstance()
        playerAPIService = RetrofitInstance.api

        arguments?.let {
            leagueId = it.getString("leagueId") ?: ""
            teamId = it.getString("teamId") ?: ""
        }

        Log.d("DraftPlayersFragment", "League ID: $leagueId, Team ID: $teamId")

        countdownTimerView = view.findViewById(R.id.countdownTimer)
        roundPickInfoView = view.findViewById(R.id.roundPickInfo)
        currentTeamNameView = view.findViewById(R.id.currentTeamName)
        onTheClockLabel = view.findViewById(R.id.onTheClockLabel)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewPlayers)
        recyclerView.layoutManager = LinearLayoutManager(context)
        playerAdapter = DraftPlayerAdapter(cachedPlayers, { selectedPlayer ->
            draftPlayer(selectedPlayer)
        }, false)
        recyclerView.adapter = playerAdapter

        setupDraftInfo()

        if (!isDataLoaded) {
            loadPlayersFromAPI()
        } else {
            playerAdapter.updateList(cachedPlayers, isUserOnTheClock)
        }

        setupDraftedPlayerListener()
        setupDraftListener()

        return view
    }

    private fun setupDraftInfo() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val leagueSnapshot =
                    firestore.collection("Leagues").document(leagueId).get().await()
                val draftStatus = leagueSnapshot.getString("draftStatus") ?: "not_started"
                val draftOrder = leagueSnapshot.get("draftOrder") as? List<String> ?: emptyList()
                val draftDateTime = leagueSnapshot.getTimestamp("draftDateTime")?.toDate()

                if (draftStatus == "date_set" && draftDateTime != null) {
                    val firstTeamId = draftOrder.firstOrNull() ?: "Unknown Team"
                    val firstTeamSnapshot = firestore.collection("Leagues")
                        .document(leagueId)
                        .collection("Teams")
                        .document(firstTeamId)
                        .get()
                        .await()
                    val firstTeamName = firstTeamSnapshot.getString("teamName") ?: "Team 1"

                    withContext(Dispatchers.Main) {
                        currentTeamNameView.text = firstTeamName
                        roundPickInfoView.text = "Time Until Start"
                        onTheClockLabel.text = "First Pick:"
                        startCountdownToDraft(draftDateTime.time)
                    }
                }
            } catch (e: Exception) {
                Log.e("DraftPlayersFragment", "Error setting up draft info", e)
            }
        }
    }

    private fun startCountdownToDraft(draftTimeInMillis: Long) {
        val currentTime = System.currentTimeMillis()
        val timeUntilDraft = draftTimeInMillis - currentTime

        countdownTimer?.cancel()
        countdownTimer = object : CountDownTimer(timeUntilDraft, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000 % 60
                val minutes = millisUntilFinished / 1000 / 60
                countdownTimerView.text = String.format("%02d:%02d", minutes, seconds)
            }

            override fun onFinish() {
                countdownTimerView.text = "00:00"
                updateDraftStatusToInProgress()
            }
        }.start()
    }

    private fun updateDraftStatusToInProgress() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                firestore.collection("Leagues")
                    .document(leagueId)
                    .update("draftStatus", "in_progress")
                    .await()

                withContext(Dispatchers.Main) {
                    roundPickInfoView.text = "Draft Started"
                    onTheClockLabel.text = "On The Clock:"
                    if (draftListenerRegistration == null) setupDraftListener() // Only initialize once
                }
            } catch (e: Exception) {
                Log.e("DraftPlayersFragment", "Error updating draft status to in_progress", e)
            }
        }
    }


    private fun setupDraftListener() {
        if (draftListenerRegistration != null) return // Prevent adding duplicate listeners

        draftListenerRegistration = firestore.collection("Leagues")
            .document(leagueId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("DraftPlayersFragment", "Error listening to draft state", error)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val draftStatus = snapshot.getString("draftStatus") ?: "not_started"
                    val draftOrder = snapshot.get("draftOrder") as? List<String> ?: emptyList()
                    val currentPickIndex = snapshot.getLong("currentPickIndex")?.toInt() ?: 0
                    val pickExpirationTime = snapshot.getTimestamp("pickExpirationTime")?.toDate()?.time ?: 0L

                    if (draftStatus == "in_progress") {
                        val totalTeams = draftOrder.size
                        val roundNumber = (currentPickIndex / totalTeams) + 1
                        val isReverseRound = roundNumber % 2 == 0
                        val normalizedIndex = currentPickIndex % totalTeams

                        val currentTeamId = if (isReverseRound) {
                            draftOrder[totalTeams - 1 - normalizedIndex]
                        } else {
                            draftOrder[normalizedIndex]
                        }

                        updateDraftUI(
                            currentTeamId,
                            currentPickIndex,
                            totalTeams,
                            pickExpirationTime
                        )
                    }
                }
            }
    }



    private fun updateDraftUI(
        currentTeamId: String,
        currentPickIndex: Int,
        totalTeams: Int,
        pickExpirationTime: Long
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Fetch the current team details
                val teamSnapshot = firestore.collection("Leagues")
                    .document(leagueId)
                    .collection("Teams")
                    .document(currentTeamId)
                    .get()
                    .await()
                val teamName = teamSnapshot.getString("teamName") ?: "Unknown Team"

                // Calculate the round number
                val roundNumber = (currentPickIndex / totalTeams) + 1

                // Determine the pick number within the round
                val pickNumberInRound = (currentPickIndex % totalTeams) + 1

                // Check if the user's team is on the clock
                val isUserOnTheClock = currentTeamId == teamId
                this@DraftPlayersFragment.isUserOnTheClock = isUserOnTheClock

                withContext(Dispatchers.Main) {
                    // Update the UI elements
                    currentTeamNameView.text = teamName
                    roundPickInfoView.text = "Round $roundNumber, Pick $pickNumberInRound"
                    onTheClockLabel.text =
                        if (isUserOnTheClock) "Your Turn" else "On The Clock"

                    // Notify the adapter about the draft button status
                    playerAdapter.updateList(cachedPlayers, isUserOnTheClock)

                    // Start the countdown for the current pick
                    startCountdown(pickExpirationTime - System.currentTimeMillis())
                }
            } catch (e: Exception) {
                Log.e("DraftPlayersFragment", "Error updating draft UI", e)
            }
        }
    }



    private fun startCountdown(timeInMillis: Long) {
        countdownTimer?.cancel()
        countdownTimer = object : CountDownTimer(timeInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000 % 60
                val minutes = millisUntilFinished / 1000 / 60
                countdownTimerView.text = String.format("%02d:%02d", minutes, seconds)
            }

            override fun onFinish() {
                countdownTimerView.text = "00:00"
                handlePickExpiration() // Handle the logic for when the timer expires
            }
        }.start()
    }

    private fun handlePickExpiration() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Fetch the latest state of the draft
                val leagueSnapshot =
                    firestore.collection("Leagues").document(leagueId).get().await()
                val draftOrder = leagueSnapshot.get("draftOrder") as? List<String> ?: emptyList()
                val currentPickIndex = leagueSnapshot.getLong("currentPickIndex")?.toInt() ?: 0
                val totalTeams = draftOrder.size

                if (totalTeams == 0 || draftOrder.isEmpty()) {
                    Log.e("DraftPlayersFragment", "Draft order is empty.")
                    return@launch
                }

                // Calculate the current team on the clock
                val roundNumber = (currentPickIndex / totalTeams) + 1
                val isReverseRound = roundNumber % 2 == 0
                val normalizedIndex = currentPickIndex % totalTeams
                val currentTeamId = if (isReverseRound) {
                    draftOrder[totalTeams - 1 - normalizedIndex]
                } else {
                    draftOrder[normalizedIndex]
                }

                // Auto-draft the player
                val autoDraftSuccessful = autoDraftPlayer(currentPickIndex, currentTeamId)

                if (autoDraftSuccessful) {
                    // Move to the next pick only if auto-draft succeeds
                    val nextPickIndex = currentPickIndex + 1
                    val nextPickExpirationTime = Calendar.getInstance().apply {
                        add(Calendar.SECOND, PICK_DURATION_SECONDS)
                    }.time

                    firestore.collection("Leagues").document(leagueId).update(
                        mapOf(
                            "currentPickIndex" to nextPickIndex,
                            "pickExpirationTime" to nextPickExpirationTime
                        )
                    ).await()

                    Log.d(
                        "DraftPlayersFragment",
                        "Moved to next pick: $nextPickIndex, Team: $currentTeamId (Auto-draft complete)"
                    )
                } else {
                    Log.e("DraftPlayersFragment", "Auto-draft failed for team: $currentTeamId")
                }
            } catch (e: Exception) {
                Log.e("DraftPlayersFragment", "Error handling pick expiration", e)
            }
        }
    }


    private suspend fun autoDraftPlayer(currentPickIndex: Int, currentTeamId: String): Boolean {
        val teamRef = firestore.collection("Leagues")
            .document(leagueId)
            .collection("Teams")
            .document(currentTeamId)

        return try {
            // Fetch the drafted player IDs outside the transaction
            val draftedPlayerIds = firestore.collection("Leagues")
                .document(leagueId)
                .collection("draftedPlayers")
                .get()
                .await()
                .documents
                .map { it.id }

            // Await the result of the Firestore transaction
            val result = firestore.runTransaction { transaction ->
                // Ensure the pick is still valid and hasn't been handled by another listener
                val leagueDoc = transaction.get(firestore.collection("Leagues").document(leagueId))
                val currentPickInFirestore = leagueDoc.getLong("currentPickIndex")?.toInt() ?: currentPickIndex
                if (currentPickInFirestore != currentPickIndex) {
                    Log.w("DraftPlayersFragment", "Pick already handled by another listener.")
                    return@runTransaction false
                }

                // Get the first available player that hasn't been drafted
                val availablePlayer = cachedPlayers.firstOrNull { it.playerID !in draftedPlayerIds }
                    ?: throw Exception("No available players for auto-draft")

                // Add the player to the draftedPlayers collection
                transaction.set(
                    firestore.collection("Leagues")
                        .document(leagueId)
                        .collection("draftedPlayers")
                        .document(availablePlayer.playerID),
                    mapOf("teamID" to currentTeamId)
                )

                // Add the player to the team's roster
                transaction.update(teamRef, "roster", FieldValue.arrayUnion(availablePlayer.playerID))

                // Remove the player from cachedPlayers locally
                cachedPlayers.remove(availablePlayer)

                Log.d("DraftPlayersFragment", "Auto-drafted player: ${availablePlayer.playerID} for team: $currentTeamId")
                true
            }.await() // Wait for the transaction to complete

            result // Return the result of the transaction
        } catch (e: Exception) {
            Log.e("DraftPlayersFragment", "Error in autoDraftPlayer", e)
            false // Return false if any error occurs
        }
    }







    private fun loadPlayersFromAPI() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val draftedPlayerIds = firestore.collection("Leagues")
                    .document(leagueId)
                    .collection("draftedPlayers")
                    .get()
                    .await()
                    .documents
                    .map { it.id }

                val adpResponse = playerAPIService.getNBAADP()
                val adpList = adpResponse.body.adpList

                cachedPlayers.clear()

                for (adpEntry in adpList) {
                    val playerId = adpEntry.playerID
                    if (draftedPlayerIds.contains(playerId)) continue

                    val playerSnapshot =
                        firestore.collection("players").document(playerId).get().await()
                    val player = playerSnapshot.toObject(Player::class.java)
                    if (player != null) {
                        player.playerID = playerSnapshot.id
                        cachedPlayers.add(player)

                        withContext(Dispatchers.Main) {
                            playerAdapter.updateList(cachedPlayers, isUserOnTheClock)
                        }
                    }
                }

                isDataLoaded = true
            } catch (e: Exception) {
                Log.e("DraftPlayersFragment", "Error loading players from API and Firestore", e)
            }
        }
    }

    private fun setupDraftedPlayerListener() {
        firestore.collection("Leagues")
            .document(leagueId)
            .collection("draftedPlayers")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("DraftPlayersFragment", "Error listening for drafted players", error)
                    return@addSnapshotListener
                }

                val draftedPlayerIds = snapshot?.documents?.map { it.id } ?: emptyList()
                cachedPlayers.removeAll { player -> draftedPlayerIds.contains(player.playerID) }
                playerAdapter.updateList(cachedPlayers, isUserOnTheClock)

                // Check if the draft is completed
                checkDraftCompletion(draftedPlayerIds.size)
            }
    }

    private fun draftPlayer(player: Player) {
        val draftedPlayerRef = firestore.collection("Leagues")
            .document(leagueId)
            .collection("draftedPlayers")
            .document(player.playerID)

        val teamRef = firestore.collection("Leagues")
            .document(leagueId)
            .collection("Teams")
            .document(teamId)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                firestore.runTransaction { transaction ->
                    // 1. Perform all reads first
                    val snapshot = transaction.get(draftedPlayerRef)
                    if (snapshot.exists()) {
                        throw Exception("Player already drafted")
                    }

                    // Fetch the current league document
                    val leagueDoc = transaction.get(firestore.collection("Leagues").document(leagueId))
                    val currentPickIndex = leagueDoc.getLong("currentPickIndex")?.toInt() ?: 0
                    val totalTeams = (leagueDoc.get("draftOrder") as? List<*>)?.size ?: 0

                    // Calculate the next pick index
                    val nextPickIndex = currentPickIndex + 1

                    // Calculate new pick expiration time based on the configurable duration
                    val nextPickExpirationTime = Calendar.getInstance().apply {
                        add(Calendar.SECOND, PICK_DURATION_SECONDS)
                    }.time

                    // 2. Perform all writes after reads
                    // Add the player to the draftedPlayers collection
                    transaction.set(draftedPlayerRef, mapOf("teamID" to teamId))

                    // Add the player to the team's roster
                    transaction.update(teamRef, "roster", FieldValue.arrayUnion(player.playerID))

                    // Update the league document with the new pick index and expiration time
                    transaction.update(
                        firestore.collection("Leagues").document(leagueId),
                        mapOf(
                            "currentPickIndex" to nextPickIndex,
                            "pickExpirationTime" to nextPickExpirationTime
                        )
                    )
                }.await()

                // Update the UI on the main thread
                withContext(Dispatchers.Main) {
                    // Remove the drafted player from cachedPlayers
                    cachedPlayers.remove(player)

                    // Update the player adapter
                    playerAdapter.updateList(cachedPlayers, isUserOnTheClock)
                    Log.d("DraftPlayersFragment", "Player drafted successfully: ${player.playerID}")
                }
            } catch (e: Exception) {
                Log.e("DraftPlayersFragment", "Error drafting player: ${player.playerID}", e)
            }
        }
    }




    private fun checkDraftCompletion(draftedPlayerCount: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Fetch the total number of teams in the league
                val leagueSnapshot = firestore.collection("Leagues").document(leagueId).get().await()
                val draftOrder = leagueSnapshot.get("draftOrder") as? List<*>
                val totalTeams = draftOrder?.size ?: 0

                // Check if all teams have drafted 13 players
                if (draftedPlayerCount == totalTeams * 13) {
                    // Update the draft status to "completed" in Firestore
                    firestore.collection("Leagues")
                        .document(leagueId)
                        .update("draftStatus", "completed")
                        .await()

                    withContext(Dispatchers.Main) {
                        onDraftComplete()
                    }
                }
            } catch (e: Exception) {
                Log.e("DraftPlayersFragment", "Error checking draft completion", e)
            }
        }
    }

    private fun onDraftComplete() {
        // Notify the user that the draft is complete

        // Disable further interactions
        playerAdapter.updateList(emptyList(), false) // Clear players and disable buttons
        countdownTimer?.cancel() // Stop the countdown timer

        // Show a popup notification
        requireActivity().runOnUiThread {
            val alertDialog = android.app.AlertDialog.Builder(requireContext())
                .setTitle("Draft Complete")
                .setMessage("The draft is complete!")
                .setCancelable(false) // Prevent dialog from being dismissed without interaction
                .setPositiveButton("OK") { _, _ ->
                    // Navigate back to the league fragment
                    requireActivity().supportFragmentManager.popBackStack()
                }
                .create()
            alertDialog.show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        draftListenerRegistration?.remove() // Remove the Firestore listener
        draftListenerRegistration = null // Clear the reference
    }

    override fun onPause() {
        super.onPause()
        // Detach listeners when the fragment is not active
        draftListenerRegistration?.remove()
    }

    override fun onResume() {
        super.onResume()
        // Reattach listeners when returning to the fragment
        setupDraftListener()
        playerAdapter.updateList(cachedPlayers, isUserOnTheClock)
    }
}

