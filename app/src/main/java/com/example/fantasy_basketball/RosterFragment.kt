package com.example.fantasy_basketball

import RosterAdapter
import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class RosterFragment : Fragment() {

    private lateinit var leagueId: String
    private lateinit var teamId: String
    private lateinit var startingLineupAdapter: RosterAdapter
    private lateinit var benchAdapter: RosterAdapter

    private val startingLineupSlots = listOf("PG", "SG", "SF", "PF", "C", "G", "F", "UTIL", "UTIL", "UTIL")
    private val benchLineupSlots = listOf("PG", "SG", "SF", "PF", "C", "UTIL", "UTIL", "UTIL", "UTIL", "UTIL")
    val roster = mutableListOf<Player>()
    private val startingLineup = mutableListOf<Player?>(null, null, null, null, null, null, null, null, null, null)
    private val bench = mutableListOf<Player?>(null, null, null, null, null, null, null, null, null, null)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_roster, container, false)


        // Retrieve the leagueId passed from HomeFragment
      /*  arguments?.let {
            leagueId = it.getString("leagueId", "")
            teamId = it.getString("teamId", "")
        }*/

        startingLineupAdapter = RosterAdapter(startingLineup, startingLineupSlots) { position, slot ->
            showPlayerSelectionDialog(position, slot, true)
        }
        benchAdapter = RosterAdapter(bench, benchLineupSlots) { position, slot ->
            showPlayerSelectionDialog(position, slot, false)
        }

        view.findViewById<RecyclerView>(R.id.recycler_view_starting_lineup).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = startingLineupAdapter
        }

        view.findViewById<RecyclerView>(R.id.recycler_view_bench).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = benchAdapter
        }

        // Fetch players for roster upon fragment creation
        lifecycleScope.launch {
            val fetchedRoster = fetchRosterWithPositionRequirements(FirebaseFirestore.getInstance())
            //fetchTeamRosterDetails(FirebaseFirestore.getInstance(),leagueId,teamId)
            updateRosterUI(roster)
        }

        return view
    }

    private suspend fun fetchRosterWithPositionRequirements(firestore: FirebaseFirestore): List<Player> {

        try {
            // Fetch players to meet each position requirement
            val primaryPositions = listOf("PG", "SG", "SF", "PF", "C", "G", "F")
            for (position in primaryPositions) {
                val positionPlayers = firestore.collection("players")
                    .whereEqualTo("pos", position)
                    .limit(2) // Fetch two players per position to ensure diversity
                    .get()
                    .await()
                    .documents // Get the documents instead of using `toObjects` directly
                    .mapNotNull { document ->
                        val playerID = document.id
                        val longName = document.getString("longName") ?: ""
                        val jerseyNum = document.getString("jerseyNum") ?: ""
                        val pos = document.getString("pos") ?: ""
                        val team = document.getString("team") ?: ""
                        val teamID = document.getString("teamID") ?: ""
                        val nbaComHeadshot = document.getString("nbaComHeadshot") ?: ""

                        // Fetching injury details as a map
                        val injuryMap = document.get("Injury") as? Map<String, Any> ?: emptyMap()
                        val designation = injuryMap["status"] as? String
                        val injury = Injury(
                            injReturnDate = injuryMap["injReturnDate"] as? String,
                            description = injuryMap["description"] as? String,
                            injDate = injuryMap["injDate"] as? String,
                            designation = if (designation.isNullOrEmpty()) "Healthy" else designation
                        )

                        // Fetching projections as a map
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

                        // Fetching stats as a map
                        val statsMap = document.get("TotalStats") as? Map<String, Any> ?: emptyMap()
                        val stats = PlayerStats(
                            blk = statsMap["blk"] as? String ?: null,
                            fga = statsMap["fga"] as? String ?: null,
                            DefReb = statsMap["DefReb"] as? String ?: null,
                            ast = statsMap["ast"] as? String ?: null,
                            ftp = statsMap["ftp"] as? String ?: null,
                            tptfgp = statsMap["tptfgp"] as? String ?: null,
                            tptfgm = statsMap["tptfgm"] as? String ?: null,
                            stl = statsMap["stl"] as? String ?: null,
                            fgm = statsMap["fgm"] as? String ?: null,
                            pts = statsMap["pts"] as? String ?: null,
                            reb = statsMap["reb"] as? String ?: null,
                            fgp = statsMap["fgp"] as? String ?: null,
                            fta = statsMap["fta"] as? String ?: null,
                            mins = statsMap["mins"] as? String ?: null,
                            trueShootingAttempts = statsMap["trueShootingAttempts"] as? String ?: null,
                            gamesPlayed = statsMap["gamesPlayed"] as? String ?: null,
                            TOV = statsMap["TOV"] as? String ?: null,
                            tptfga = statsMap["tptfga"] as? String ?: null,
                            OffReb = statsMap["OffReb"] as? String ?: null,
                            ftm = statsMap["ftm"] as? String
                        )

                        // Create the Player object
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
                    }

                // Add position players to the roster
                roster.addAll(positionPlayers)
            }

            // Ensure at least 13 players in the roster
            if (roster.size < 13) {
                val additionalPlayers = firestore.collection("players")
                    .limit((13 - roster.size).toLong())
                    .get()
                    .await()
                    .documents
                    .mapNotNull { document ->
                        val playerID = document.id
                        val longName = document.getString("longName") ?: ""
                        val jerseyNum = document.getString("jerseyNum") ?: ""
                        val pos = document.getString("pos") ?: ""
                        val team = document.getString("team") ?: ""
                        val teamID = document.getString("teamID") ?: ""
                        val nbaComHeadshot = document.getString("nbaComHeadshot") ?: ""

                        // Fetching injury details as a map
                        val injuryMap = document.get("Injury") as? Map<String, Any> ?: emptyMap()
                        val designation = injuryMap["status"] as? String
                        val injury = Injury(
                            injReturnDate = injuryMap["injReturnDate"] as? String,
                            description = injuryMap["description"] as? String,
                            injDate = injuryMap["injDate"] as? String,
                            designation = if (designation.isNullOrEmpty()) "Healthy" else designation
                        )

                        // Fetching projections as a map
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

                        // Fetching stats as a map
                        val statsMap = document.get("TotalStats") as? Map<String, Any> ?: emptyMap()
                        val stats = PlayerStats(
                            blk = statsMap["blk"] as? String ?: null,
                            fga = statsMap["fga"] as? String ?: null,
                            DefReb = statsMap["DefReb"] as? String ?: null,
                            ast = statsMap["ast"] as? String ?: null,
                            ftp = statsMap["ftp"] as? String ?: null,
                            tptfgp = statsMap["tptfgp"] as? String ?: null,
                            tptfgm = statsMap["tptfgm"] as? String ?: null,
                            stl = statsMap["stl"] as? String ?: null,
                            fgm = statsMap["fgm"] as? String ?: null,
                            pts = statsMap["pts"] as? String ?: null,
                            reb = statsMap["reb"] as? String ?: null,
                            fgp = statsMap["fgp"] as? String ?: null,
                            fta = statsMap["fta"] as? String ?: null,
                            mins = statsMap["mins"] as? String ?: null,
                            trueShootingAttempts = statsMap["trueShootingAttempts"] as? String ?: null,
                            gamesPlayed = statsMap["gamesPlayed"] as? String ?: null,
                            TOV = statsMap["TOV"] as? String ?: null,
                            tptfga = statsMap["tptfga"] as? String ?: null,
                            OffReb = statsMap["OffReb"] as? String ?: null,
                            ftm = statsMap["ftm"] as? String
                        )

                        // Create the Player object
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
                    }
                roster.addAll(additionalPlayers)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return roster.take(13) // Ensure we only return a roster of 13 players
    }


    private suspend fun fetchPlayerByIdAndAddToRoster(
        firestore: FirebaseFirestore,
        playerID: String,
        roster: MutableList<Player>
    ): List<Player> {
        try {
            // Fetch the player document by ID
            val document = firestore.collection("players")
                .document(playerID)
                .get()
                .await()

            // Check if the document exists
            if (document.exists()) {
                val longName = document.getString("longName") ?: ""
                val jerseyNum = document.getString("jerseyNum") ?: ""
                val pos = document.getString("pos") ?: ""
                val team = document.getString("team") ?: ""
                val teamID = document.getString("teamID") ?: ""
                val nbaComHeadshot = document.getString("nbaComHeadshot") ?: ""

                // Fetching injury details as a map
                val injuryMap = document.get("Injury") as? Map<String, Any> ?: emptyMap()
                val designation = injuryMap["status"] as? String
                val injury = Injury(
                    injReturnDate = injuryMap["injReturnDate"] as? String,
                    description = injuryMap["description"] as? String,
                    injDate = injuryMap["injDate"] as? String,
                    designation = if (designation.isNullOrEmpty()) "Healthy" else designation
                )

                // Fetching projections as a map
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

                // Fetching stats as a map
                val statsMap = document.get("TotalStats") as? Map<String, Any> ?: emptyMap()
                val stats = PlayerStats(
                    blk = statsMap["blk"] as? String ?: null,
                    fga = statsMap["fga"] as? String ?: null,
                    DefReb = statsMap["DefReb"] as? String ?: null,
                    ast = statsMap["ast"] as? String ?: null,
                    ftp = statsMap["ftp"] as? String ?: null,
                    tptfgp = statsMap["tptfgp"] as? String ?: null,
                    tptfgm = statsMap["tptfgm"] as? String ?: null,
                    stl = statsMap["stl"] as? String ?: null,
                    fgm = statsMap["fgm"] as? String ?: null,
                    pts = statsMap["pts"] as? String ?: null,
                    reb = statsMap["reb"] as? String ?: null,
                    fgp = statsMap["fgp"] as? String ?: null,
                    fta = statsMap["fta"] as? String ?: null,
                    mins = statsMap["mins"] as? String ?: null,
                    trueShootingAttempts = statsMap["trueShootingAttempts"] as? String ?: null,
                    gamesPlayed = statsMap["gamesPlayed"] as? String ?: null,
                    TOV = statsMap["TOV"] as? String ?: null,
                    tptfga = statsMap["tptfga"] as? String ?: null,
                    OffReb = statsMap["OffReb"] as? String ?: null,
                    ftm = statsMap["ftm"] as? String
                )

                // Create the Player object
                val player = Player(
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

                // Add the player to the roster
                roster.add(player)
            } else {
                println("Player with ID $playerID does not exist.")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return roster;
    }

    private suspend fun fetchTeamRosterDetails(
        firestore: FirebaseFirestore,
        leagueId: String,
        teamId: String
    ) {
        try {
            // Fetch the team document
            val teamDocument = firestore.collection("Leagues")
                .document(leagueId)
                .collection("Teams")
                .document(teamId)
                .get()
                .await()

            // Check if the team document exists
            if (teamDocument.exists()) {
                // Retrieve the roster array
                val rosterArray = teamDocument.get("roster") as? List<String> ?: emptyList()

                // Iterate through each player ID in the roster and fetch player details
                for (playerID in rosterArray) {
                    //fetchPlayerByIdAndAddToRoster(firestore, playerID)
                }
            } else {
                println("Team document not found for League ID: $leagueId, Team ID: $teamId")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }



    private fun updateRosterUI(fetchedRoster: List<Player>) {
        // Clear previous data
        startingLineup.fill(null)
        bench.fill(null)

        // Update starting lineup and bench
        for (i in fetchedRoster.indices) {
            if (i < startingLineup.size) {
                startingLineup[i] = fetchedRoster[i]
            } else {
                bench[i - startingLineup.size] = fetchedRoster[i]
            }
        }

        // Notify adapters about the changes
        startingLineupAdapter.notifyDataSetChanged()
        benchAdapter.notifyDataSetChanged()
    }
    private fun showPlayerSelectionDialog(position: Int, slot: String, isStarting: Boolean) {
        // Filter eligible players based on slot requirements
        val eligiblePlayers = roster.filter {
            when (slot) {
                "UTIL" -> true
                "F" -> it.pos == "SF" || it.pos == "PF"
                "G" -> it.pos == "PG" || it.pos == "SG"
                else -> it.pos == slot
            }
        }

        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_select_player, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recycler_view_dialog)

        // Create the dialog first before setting up the adapter
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Select Player for $slot")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .create()

        // Initialize the adapter
        val adapter = PlayerSelectionAdapter(eligiblePlayers) { selectedPlayer ->

            // Step 1: Check if the selected player is already in the starting lineup
            val currentStartingPosition = startingLineup.indexOfFirst { it != null && it.playerID == selectedPlayer.playerID }

            if (currentStartingPosition != -1) {
                // The selected player is in the starting lineup, remove them from their current slot
                startingLineup[currentStartingPosition] = null
                // Clear associated information (picture, team) by notifying the adapter to refresh
                startingLineupAdapter.notifyItemChanged(currentStartingPosition)
            } else {
                // Step 2: If the selected player is on the bench, remove them from the bench
                val currentBenchPosition = bench.indexOfFirst { it?.playerID == selectedPlayer.playerID }
                if (currentBenchPosition != -1) {
                    // Remove player from bench
                    bench[currentBenchPosition] = null
                    // Clear associated information (picture, team) by notifying the adapter to refresh
                    benchAdapter.notifyItemChanged(currentBenchPosition)
                }
            }

            // Step 3: Place the selected player into the desired position
            if (isStarting) {
                // If the player is moved to the starting lineup
                if (startingLineup[position] != null) {
                    // Move the player currently in the selected position to the bench
                    movePlayerToBench(startingLineup[position]!!)
                }

                // Now place the selected player into the starting lineup
                startingLineup[position] = selectedPlayer
                startingLineupAdapter.notifyItemChanged(position) // Refresh the starting lineup slot
            } else {
                // If the player is moved to the bench
                if (bench[position] != null) {
                    // Move the player currently in the selected position on the bench to the starting lineup
                    movePlayerToStartingLineup(bench[position]!!)
                }

                // Place the selected player into the bench
                bench[position] = selectedPlayer
                benchAdapter.notifyItemChanged(position) // Refresh the bench slot
            }

            // Dismiss the dialog after selecting a player
            dialog.dismiss()
        }

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        // Show the dialog
        dialog.show()
    }





    private fun movePlayerToBench(player: Player) {
        // Move player to bench if space is available
        val benchIndex = bench.indexOfFirst { it == null }
        if (benchIndex != -1) {
            bench[benchIndex] = player
            benchAdapter.notifyItemChanged(benchIndex)
        }
    }

    private fun movePlayerToStartingLineup(player: Player) {
        // Move player to starting lineup if space is available
        val startingIndex = startingLineup.indexOfFirst { it == null }
        if (startingIndex != -1) {
            startingLineup[startingIndex] = player
            startingLineupAdapter.notifyItemChanged(startingIndex)
        }
    }




}
