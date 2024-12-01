package com.example.fantasy_basketball

import RosterAdapter
import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class RosterFragment : Fragment() {

    private lateinit var leagueId: String
    private lateinit var teamId: String
    private lateinit var startingLineupAdapter: RosterAdapter
    private lateinit var benchAdapter: RosterAdapter

    private val startingLineupSlots = listOf("PG", "SG", "SF", "PF", "C", "G", "F", "UTIL", "UTIL", "UTIL")
    private val benchLineupSlots = mutableListOf("IR")
    var roster = mutableListOf<Player?>()
    private val startingLineup = mutableListOf<Player?>(null, null, null, null, null, null, null, null, null, null)
    private val bench = mutableListOf<Player?>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_roster, container, false)

        // Retrieve the leagueId and teamId passed from the previous fragment
        arguments?.let {
            leagueId = it.getString("leagueID", "")
            teamId = it.getString("teamID", "")
        }

// Create the RosterAdapter for starting lineup
        startingLineupAdapter = RosterAdapter(
            startingLineup,
            startingLineupSlots,
            { position, slot ->
                showPlayerSelectionDialog(position, slot, true)  // Handling slot selection
            },
            { player ->
                openPlayerProfile(player)  // Handle CardView click here
            }
        )

// Create the RosterAdapter for bench lineup
        benchAdapter = RosterAdapter(
            bench,
            benchLineupSlots,
            { position, slot ->
                showPlayerSelectionDialog(position, slot, false)  // Handling slot selection
            },
            { player ->
                openPlayerProfile(player)  // Handle CardView click here
            }
        )



        view.findViewById<RecyclerView>(R.id.recycler_view_starting_lineup).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = startingLineupAdapter
        }

        view.findViewById<RecyclerView>(R.id.recycler_view_bench).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = benchAdapter
        }

        // Fetch players for roster upon fragment creation
        lifecycleScope.launch{
            fetchTeamRosterDetails(FirebaseFirestore.getInstance(), leagueId, teamId)  //gets the players from the roster
            val (startingPlayers, benchPlayers) = fetchBenchAndStartingPlayers(FirebaseFirestore.getInstance(), leagueId, teamId, roster)
            updateRosterUI(startingPlayers, benchPlayers)

        }

        return view
    }

    private suspend fun fetchPlayerById(
        firestore: FirebaseFirestore,
        playerID: String
    ): Player? {
        if (playerID.isEmpty()) {
            // Return null if the playerID is empty
            return null
        }

        return try {
            val document = firestore.collection("players")
                .document(playerID)
                .get()
                .await()

            if (document.exists()) {
                val longName = document.getString("longName") ?: "Unknown Player"
                val jerseyNum = document.getString("jerseyNum") ?: "00"
                val pos = document.getString("pos") ?: "N/A"
                val team = document.getString("team") ?: "Free Agent"
                val teamID = document.getString("teamID") ?: "N/A"
                val nbaComHeadshot = document.getString("nbaComHeadshot") ?: ""

                // Parse injury details
                val injuryMap = document.get("Injury") as? Map<String, Any> ?: emptyMap()
                val designation = injuryMap["status"] as? String
                val injury = Injury(
                    injReturnDate = injuryMap["injReturnDate"] as? String,
                    description = injuryMap["description"] as? String,
                    injDate = injuryMap["injDate"] as? String,
                    designation = if (designation.isNullOrEmpty()) "Healthy" else designation
                )

                // Parse projections
                val projectionsMap = document.get("Projections") as? Map<String, Any> ?: emptyMap()
                val projection = PlayerProjection(
                    blk = projectionsMap["blk"] as? String ?: "",
                    mins = projectionsMap["mins"] as? String ?: "",
                    ast = projectionsMap["ast"] as? String ?: "",
                    pos = projectionsMap["pos"] as? String ?: "",
                    teamID = projectionsMap["teamID"] as? String ?: "",
                    stl = projectionsMap["stl"] as? String ?: "",
                    TOV = projectionsMap["TOV"] as? String ?: "",
                    team = projectionsMap["team"] as? String ?: "",
                    pts = projectionsMap["pts"] as? String ?: "",
                    reb = projectionsMap["reb"] as? String ?: "",
                    longName = projectionsMap["longName"] as? String ?: "",
                    playerID = projectionsMap["playerID"] as? String ?: "",
                    fantasyPoints = projectionsMap["fantasyPoints"] as? String ?: ""
                )

                // Parse stats
                val statsMap = document.get("TotalStats") as? Map<String, Any> ?: emptyMap()
                val stats = PlayerStats(
                    blk = statsMap["blk"] as? String,
                    fga = statsMap["fga"] as? String,
                    DefReb = statsMap["DefReb"] as? String,
                    ast = statsMap["ast"] as? String,
                    ftp = statsMap["ftp"] as? String,
                    tptfgp = statsMap["tptfgp"] as? String,
                    tptfgm = statsMap["tptfgm"] as? String,
                    stl = statsMap["stl"] as? String,
                    fgm = statsMap["fgm"] as? String,
                    pts = statsMap["pts"] as? String,
                    reb = statsMap["reb"] as? String,
                    fgp = statsMap["fgp"] as? String,
                    fta = statsMap["fta"] as? String,
                    mins = statsMap["mins"] as? String,
                    trueShootingAttempts = statsMap["trueShootingAttempts"] as? String,
                    gamesPlayed = statsMap["gamesPlayed"] as? String,
                    TOV = statsMap["TOV"] as? String,
                    tptfga = statsMap["tptfga"] as? String,
                    OffReb = statsMap["OffReb"] as? String,
                    ftm = statsMap["ftm"] as? String
                )

                // Return the Player object
                Player(
                    playerID = playerID,
                    longName = longName,
                    jerseyNum = jerseyNum,
                    pos = pos,
                    team = team,
                    teamID = teamID,
                    nbaComHeadshot = nbaComHeadshot,
                    injury = injury,
                    stats = stats,
                    projection = projection
                )
            } else {
                null // Player not found
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun calculateAndDisplayTotalPoints() {
        // Calculate total points from the starting lineup
        val startingTotalPoints = startingLineup.sumOf { player ->
            player?.projection?.pts?.toDoubleOrNull() ?: 0.0
        }

        // Total points for the roster
        val totalPoints = startingTotalPoints

        // Update the UI with the total points
        view?.findViewById<TextView>(R.id.text_total_points)?.text =
            totalPoints.toString()

    }


    private suspend fun fetchTeamRosterDetails(
        firestore: FirebaseFirestore,
        leagueId: String,
        teamId: String
    ) {
        try {
            val teamDocument = firestore.collection("Leagues")
                .document(leagueId)
                .collection("Teams")
                .document(teamId)
                .get()
                .await()

            if (teamDocument.exists()) {
                val rosterArray = teamDocument.get("roster") as? List<String> ?: emptyList()
                for (playerID in rosterArray) {
                    // Fetch player by ID, set to null if not found
                    val player = fetchPlayerById(firestore, playerID)
                    if (player != null) {
                        roster.add(player)
                    } else {
                        roster.add(null)  // Add null if the player is not found
                    }
                }
            } else {
                println("Team document not found for League ID: $leagueId, Team ID: $teamId")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }




    private suspend fun fetchBenchAndStartingPlayers(
        firestore: FirebaseFirestore,
        leagueId: String,
        teamId: String,
        roster: List<Player?>
    ): Pair<List<Player?>, List<Player>> {
        return try {
            val teamDocument = firestore.collection("Leagues")
                .document(leagueId)
                .collection("Teams")
                .document(teamId)
                .get()
                .await()

            if (teamDocument.exists()) {
                val startingPlayerIds = teamDocument.get("Starting") as? List<String> ?: emptyList()
                val benchPlayerIds = teamDocument.get("Bench") as? List<String> ?: emptyList()

                if (startingPlayerIds.isEmpty() && roster.isNotEmpty()) {
                    // Populate the starting lineup and bench if Starting is empty
                    val newStartingPlayers = mutableListOf<Player?>()
                    val newBenchPlayers = mutableListOf<Player?>()

                    // Allocate players to starting lineup based on position
                    allocateStartingLineupSlots(roster, newStartingPlayers)

                    // The remainin gplayers after filling starting lineup will go to the bench
                    val remainingPlayers = roster.filterNot { newStartingPlayers.contains(it) }
                    newBenchPlayers.addAll(remainingPlayers)

                    // Ensure there is at least one player for each starting slot
                    fillStartingLineupIfNecessary(newStartingPlayers)

                    // Update Firestore with new Starting and Bench lists
                    updatePlayerPositionInFirestore("Starting", newStartingPlayers)
                    updatePlayerPositionInFirestore("Bench", newBenchPlayers)
                    updateBenchLineupSlots(newBenchPlayers.size)

                    return Pair(newStartingPlayers.filterNotNull(), newBenchPlayers.filterNotNull())
                } else {
                    // Use the existing starting and bench data
                    // Use the existing starting and bench data
                    val startingPlayers = startingPlayerIds.map { id ->
                        if (id.isEmpty()) {
                            null // Explicitly add null for empty player IDs
                        } else {
                            roster.find { it?.playerID == id } // Add the matching player or null if not found
                        }
                    }

                    val benchPlayers = benchPlayerIds.mapNotNull { id ->
                        roster.find { it?.playerID == id }
                    }

                    updateBenchLineupSlots(benchPlayers.size)

                    return Pair(startingPlayers, benchPlayers)
                }
            } else {
                println("Team document not found for League ID: $leagueId, Team ID: $teamId")
                Pair(emptyList(), emptyList())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(emptyList(), emptyList())
        }
    }

    private fun allocateStartingLineupSlots(roster: List<Player?>, startingLineup: MutableList<Player?>) {
        // Fill positions with players that match their respective positions
        val positions = listOf("PG", "SG", "SF", "PF", "C", "G", "F", "UTIL","UTIL","UTIL")
        val assignedPlayers = mutableSetOf<Player>() // Track already assigned players

        // Allocate players to starting positions based on available roster
        for (slot in positions) {
            val availablePlayer = roster.firstOrNull { player ->
                player !in assignedPlayers && when (slot) {
                    "PG" -> player?.pos == "PG"
                    "SG" -> player?.pos == "SG"
                    "SF" -> player?.pos == "SF"
                    "PF" -> player?.pos == "PF"
                    "C" -> player?.pos == "C"
                    "G" -> player?.pos == "PG" || player?.pos == "SG"
                    "F" -> player?.pos == "SF" || player?.pos == "PF"
                    "UTIL" -> true  // Any player can be assigned to UTIL
                    "UTIL" -> true
                    "UTIL" -> true
                    else -> false
                }
            }

            if (availablePlayer != null) {
                startingLineup.add(availablePlayer)  // Add player to the starting lineup
                assignedPlayers.add(availablePlayer)  // Mark player as assigned
            } else {
                startingLineup.add(null)  // If no player found, keep the slot empty
            }
        }
    }


    private fun fillStartingLineupIfNecessary(startingLineup: MutableList<Player?>) {
        // Ensure that all starting lineup positions are filled
        val missingSlots = startingLineup.filter { it == null }

        if (missingSlots.isNotEmpty()) {
            // Filter out the players already assigned to starting lineup
            val availablePlayers = roster.filterNot { startingLineup.contains(it) }

            // Allocate remaining available players to missing starting lineup slots
            for (i in startingLineup.indices) {
                if (startingLineup[i] == null && availablePlayers.isNotEmpty()) {
                    startingLineup[i] = availablePlayers.first()
                    // Remove the player from the available players list
                    availablePlayers.drop(1)
                }
            }
        }
    }



    private fun updateBenchLineupSlots(benchSize: Int) {
        benchLineupSlots.clear()
        repeat(benchSize) {
            benchLineupSlots.add("BE") // Add a "BE" slot for each bench player
        }
        benchLineupSlots.add("IR") // Add "IR" as the last slot
        benchAdapter.notifyDataSetChanged() // Notify the adapter to refresh the UI
    }





    private fun updateRosterUI(startingPlayers: List<Player?>, benchPlayers: List<Player>) {
        // Update Starting Lineup
        startingLineup.fill(null) // Clear all starting slots
        var currentSlotIndex = 0

        for ((index, player) in startingPlayers.withIndex()) {
            if (index < startingLineup.size) {
                startingLineup[index] = player // Assign player (null or non-null) to the same slot
            }
        }
        calculateAndDisplayTotalPoints()
        // Update Bench
        bench.clear()
        val irPlayer = benchPlayers.find { it.injury?.designation == "OUT" }
        benchPlayers.filter { it.injury?.designation != "OUT" }.forEach { bench.add(it) }

        // IR Slot Handling
        benchLineupSlots.clear()
        benchLineupSlots.addAll(List(bench.size) { "BE" }) // Add BE slots for bench players
        bench.add(irPlayer) // Add the IR player if available, else null
        benchLineupSlots.add("IR")

        // Notify Adapters
        startingLineupAdapter.notifyDataSetChanged()
        benchAdapter.notifyDataSetChanged()
    }



    private fun showPlayerSelectionDialog(position: Int, slot: String, isStarting: Boolean) {
        val eligiblePlayers = roster.filter {
            when (slot) {
                "UTIL" -> true
                "F" -> it?.pos == "SF" || it?.pos == "PF"
                "G" -> it?.pos == "PG" || it?.pos == "SG"
                "IR" -> it?.injury?.designation == "OUT"
                "BE" -> true
                else -> it?.pos == slot
            }
        }

        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_select_player, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recycler_view_dialog)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Select Player for $slot")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .create()

        val adapter = PlayerSelectionAdapter(eligiblePlayers) { selectedPlayer ->

            // Ensure player is in roster before proceeding
            if (!roster.contains(selectedPlayer)) {

                return@PlayerSelectionAdapter
            }

            val currentStartingPosition = startingLineup.indexOfFirst { it?.playerID == selectedPlayer?.playerID }

            if (currentStartingPosition != -1) {
                startingLineup[currentStartingPosition] = null
                startingLineupAdapter.notifyItemChanged(currentStartingPosition)
            } else {
                val currentBenchPosition = bench.indexOfFirst { it?.playerID == selectedPlayer?.playerID }
                if (currentBenchPosition != -1) {
                  //  bench[currentBenchPosition] = null
                    bench.removeAt(currentBenchPosition)
                    updateBenchLineupSlots(bench.size-1)
                    benchAdapter.notifyItemChanged(currentBenchPosition)
                }
            }

            if (isStarting) {
                if (startingLineup[position] != null) {
                    movePlayerToBench(startingLineup[position]!!)
                }
                startingLineup[position] = selectedPlayer
                startingLineupAdapter.notifyItemChanged(position)
                updatePlayerPositionInFirestore("Starting", startingLineup)
            } else {
                if (bench[position] != null) {
                    movePlayerToStartingLineup(bench[position]!!)
                }
                bench[position] = selectedPlayer
                benchAdapter.notifyItemChanged(position)
                updatePlayerPositionInFirestore("Bench", bench)
            }
            calculateAndDisplayTotalPoints()
            dialog.dismiss()

        }

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
        dialog.show()
    }

    private fun movePlayerToBench(player: Player) {
        // Find the first null slot in the bench excluding the IR slot
        val benchIndex = bench.subList(0, bench.size - 1).indexOfFirst { it == null }
        if (benchIndex != -1) {
            bench[benchIndex] = player
            benchAdapter.notifyItemChanged(benchIndex)
        } else {
            updateBenchLineupSlots(bench.size)
            // Add a new slot to the bench if no slots are available
            bench.add(bench.size - 1, player) // Insert before the IR slot
            benchAdapter.notifyItemInserted(bench.size - 2) // Notify change for newly added slot
        }

        // Find the position of the player in the starting lineup and set that slot to null
        val startingIndex = startingLineup.indexOfFirst { it?.playerID == player.playerID }
        if (startingIndex != -1) {
            startingLineup[startingIndex] = null // Set the starting slot to null
            startingLineupAdapter.notifyItemChanged(startingIndex) // Notify that the item changed
        }

        // Update Firestore with the updated bench and starting lineup
        updatePlayerPositionInFirestore("Bench", bench)
        updatePlayerPositionInFirestore("Starting", startingLineup) // Make sure the starting lineup is updated with null slot
    }



    private fun movePlayerToStartingLineup(player: Player) {
        val startingIndex = startingLineup.indexOfFirst { it == null }
        if (startingIndex != -1) {
            startingLineup[startingIndex] = player
            startingLineupAdapter.notifyItemChanged(startingIndex)
        }


        // If a player was removed from the starting lineup, set that slot to null
        val previousStartingPosition = startingLineup.indexOfFirst { it?.playerID == player.playerID }
        if (previousStartingPosition != -1 && previousStartingPosition != startingIndex) {
            startingLineup[previousStartingPosition] = null // Set the slot to null
            startingLineupAdapter.notifyItemChanged(previousStartingPosition) // Notify that the item changed
        }

        // Update Firestore with the updated starting lineup
        updatePlayerPositionInFirestore("Starting", startingLineup)
    }


    private fun updatePlayerPositionInFirestore(positionType: String, playerList: List<Player?>) {
        // Replace null elements with an empty string "" in the playerIds list
        val playerIds = playerList.map { it?.playerID ?: "" }

        val teamDocumentRef = FirebaseFirestore.getInstance()
            .collection("Leagues")
            .document(leagueId)
            .collection("Teams")
            .document(teamId)

        // Create a map of data to update based on the position type
        val updateData = when (positionType) {
            "Starting" -> mapOf(
                "Starting" to playerIds, // Set the entire Starting array
                "Bench" to FieldValue.arrayRemove(*playerIds.toTypedArray()) // Remove from Bench
            )
            "Bench" -> mapOf(
                "Bench" to playerIds, // Set the entire Bench array
                "Starting" to FieldValue.arrayRemove(*playerIds.toTypedArray()) // Remove from Starting
            )
            else -> return // Invalid positionType, exit
        }

        // Update the document with the new player positions
        teamDocumentRef.update(updateData)
            .addOnSuccessListener {
                println("$positionType updated successfully in Firestore.")
            }
            .addOnFailureListener { e ->
                println("Error updating $positionType in Firestore: ${e.message}")
            }
    }

    private fun openPlayerProfile(player: Player?) {
        val playerProfileFragment = PlayerInfoFragment()

        val bundle = Bundle()
        bundle.putParcelable("selectedPlayer", player) // Assuming Player class implements Parcelable
        playerProfileFragment.arguments = bundle
        findNavController().navigate(R.id.playerInfoFragment, bundle)
        // Use fragment transaction to navigate
        /*requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, playerProfileFragment)
            .addToBackStack(null)
            .commit()*/
    }


}