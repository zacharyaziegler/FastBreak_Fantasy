package com.example.fantasy_basketball

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment

import androidx.fragment.app.activityViewModels

import androidx.navigation.Navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore

class ScoreboardFragment : Fragment() {


    private lateinit var firestore: FirebaseFirestore
    private lateinit var weekSpinner: Spinner
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ScoreboardPlayerAdapter
    private val sharedViewModel: SharedDataViewModel by activityViewModels()
    private var leagueId: String? = null
    private var selectedWeek: String = "week01" // Default week

    private lateinit var matchupsRecyclerView: RecyclerView
    private lateinit var playerRecyclerView: RecyclerView
    private lateinit var matchupsAdapter: MatchupsAdapter
    private lateinit var playerAdapter: ScoreboardPlayerAdapter

    private val matchupsList = mutableListOf<FullMatchup>() // Store all matchups for the week
    private val teamAPlayers = mutableListOf<Player>()
    private val teamBPlayers = mutableListOf<Player>()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_scoreboard, container, false)


        // Fetch the leagueId from arguments
        /*
        leagueId = arguments?.getString("leagueId") ?: run {
            Log.e("ScoreboardFragment", "leagueId argument is missing")
            return view
        }

         */

        leagueId = "g11QJdRoaR7WhJIuya3A"
        Log.d("ScoreboardFragment", "League ID: $leagueId")

        setupMatchupsRecyclerView(view)
        setupPlayerRecyclerView(view)

        fetchLeagueCurrentWeek()

        return view
    }

    private fun setupMatchupsRecyclerView(view: View) {
        matchupsRecyclerView = view.findViewById(R.id.matchupsRecyclerView)
        matchupsAdapter = MatchupsAdapter(matchupsList) { fullMatchup ->
            Log.d("ScoreboardFragment", "Matchup clicked: ${fullMatchup.matchup.matchupId}")
            displayPlayersForMatchup(fullMatchup.matchup)
        }
        matchupsRecyclerView.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        matchupsRecyclerView.adapter = matchupsAdapter

        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(matchupsRecyclerView)

        matchupsRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val snappedView = snapHelper.findSnapView(layoutManager)
                    val position = layoutManager.getPosition(snappedView ?: return)

                    if (position in matchupsList.indices) {
                        Log.d("ScoreboardFragment", "Scrolled to matchup at position: $position")
                        displayPlayersForMatchup(matchupsList[position].matchup)
                    }
                }
            }
        })
    }

    private fun setupPlayerRecyclerView(view: View) {
        playerRecyclerView = view.findViewById(R.id.playerMatchupRecyclerView)
        playerAdapter = ScoreboardPlayerAdapter(
            teamAPlayers,
            teamBPlayers
        ) { selectedPlayer ->
            Log.d("ScoreboardFragment", "Player clicked: ${selectedPlayer.longName}")
            openPlayerProfile(selectedPlayer)
        }
        playerRecyclerView.layoutManager = LinearLayoutManager(context)
        playerRecyclerView.adapter = playerAdapter
    }

    private fun fetchLeagueCurrentWeek() {
        val db = FirebaseFirestore.getInstance()
        db.collection("Leagues").document(leagueId!!).get()
            .addOnSuccessListener { document ->
                val currentWeek = document.getString("currentWeek")
                Log.d("fetchLeagueCurrentWeek", "Current week: $currentWeek")
                if (currentWeek != null) {
                    fetchMatchupsForWeek(currentWeek)
                } else {
                    Log.e("ScoreboardFragment", "No current week found for league.")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("fetchLeagueCurrentWeek", "Error fetching current week", exception)
            }
    }

    private fun fetchMatchupsForWeek(currentWeek: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("Leagues").document(leagueId!!).collection("Matchups")
            .whereEqualTo("week", currentWeek)
            .get()
            .addOnSuccessListener { documents ->
                matchupsList.clear()
                Log.d("fetchMatchupsForWeek", "Matchups found: ${documents.size()}")
                val matchups = documents.map { doc ->
                    Log.d("fetchMatchupsForWeek", "Matchup: ${doc.data}")
                    Matchup(
                        matchupId = doc.id,
                        team1ID = doc.getString("team1ID") ?: "",
                        team2ID = doc.getString("team2ID") ?: "",
                        week = doc.getString("week") ?: ""
                    )
                }
                fetchTeamDetailsForMatchups(matchups)
            }
            .addOnFailureListener { exception ->
                Log.e("fetchMatchupsForWeek", "Error fetching matchups", exception)
            }
    }

    private fun fetchTeamDetailsForMatchups(matchups: List<Matchup>) {
        val db = FirebaseFirestore.getInstance()
        val teamsCollection = db.collection("Leagues").document(leagueId!!).collection("Teams")
        val fullMatchups = mutableListOf<FullMatchup>()
        val teamIds = matchups.flatMap { listOf(it.team1ID, it.team2ID) }.distinct()

        teamsCollection.whereIn(FieldPath.documentId(), teamIds)
            .get()
            .addOnSuccessListener { teamDocuments ->
                val teamDetails = teamDocuments.associateBy({ it.id }, { doc ->
                    Log.d("fetchTeamDetails", "Team: ${doc.id} -> ${doc.data}")
                    TeamDetails(
                        name = doc.getString("teamName") ?: "Unknown",
                        logo = doc.getString("profilePictureUrl") ?: ""
                    )
                })

                fullMatchups.addAll(matchups.map { matchup ->
                    FullMatchup(
                        matchup = matchup,
                        team1Details = teamDetails[matchup.team1ID],
                        team2Details = teamDetails[matchup.team2ID]
                    )
                })

                Log.d("fetchTeamDetails", "Full matchups: $fullMatchups")
                matchupsList.clear()
                matchupsList.addAll(fullMatchups)
                matchupsAdapter.notifyDataSetChanged()

                if (fullMatchups.isNotEmpty()) {
                    displayPlayersForMatchup(fullMatchups[0].matchup)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("fetchTeamDetailsForMatchups", "Error fetching team details", exception)
            }
    }

    private fun displayPlayersForMatchup(matchup: Matchup) {
        Log.d("displayPlayersForMatchup", "Displaying players for matchup: $matchup")
        fetchTeamRosters(matchup.team1ID, matchup.team2ID)
    }

    private fun fetchTeamRosters(team1ID: String, team2ID: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("Leagues").document(leagueId!!).collection("Teams").get()
            .addOnSuccessListener { documents ->
                val rosters = documents.associate { doc ->
                    Log.d("fetchTeamRosters", "Team: ${doc.id} -> ${doc.data}")
                    doc.id to (doc.get("Starting") as? List<String> ?: emptyList())
                }

                val team1Roster = rosters[team1ID] ?: emptyList()
                val team2Roster = rosters[team2ID] ?: emptyList()

                Log.d("fetchTeamRosters", "Team 1 roster: $team1Roster")
                Log.d("fetchTeamRosters", "Team 2 roster: $team2Roster")

                fetchPlayersForLineup(team1Roster, isTeam1 = true)
                fetchPlayersForLineup(team2Roster, isTeam1 = false)
            }
            .addOnFailureListener { exception ->
                Log.e("fetchTeamRosters", "Error fetching team rosters", exception)
            }
    }

    private fun fetchPlayersForLineup(playerIds: List<String>, isTeam1: Boolean) {
        val positionOrder = listOf("PG", "SG", "SF", "PF", "C", "G", "F", "UTIL", "UTIL", "UTIL")

        if (playerIds.isEmpty()) {
            Log.d("fetchPlayersForLineup", "No player IDs provided for ${if (isTeam1) "Team A" else "Team B"}")
            if (isTeam1) teamAPlayers.clear() else teamBPlayers.clear()
            playerAdapter.notifyDataSetChanged()
            return
        }

        val db = FirebaseFirestore.getInstance()
        db.collection("players")
            .whereIn(FieldPath.documentId(), playerIds)
            .get()
            .addOnSuccessListener { documents ->
                val playersMap = documents.associateBy { it.id }

                val players = playerIds.mapIndexedNotNull { index, playerId ->
                    val doc = playersMap[playerId]
                    if (doc != null) {
                        Player(
                            playerID = doc.id,
                            longName = doc.getString("longName") ?: "Unknown",
                            pos = positionOrder.getOrNull(index) ?: "UTIL",
                            stats = PlayerStats(
                                pts = (doc.get("TotalStats") as? Map<*, *>)?.get("pts")?.toString() ?: "0.0"
                            ),
                            team = doc.getString("team") ?: "",
                            nbaComHeadshot = doc.getString("nbaComHeadshot") ?: ""
                        )
                    } else {
                        Log.w("fetchPlayersForLineup", "Player ID $playerId not found in Firestore")
                        null
                    }
                }

                if (isTeam1) {
                    Log.d("fetchPlayersForLineup", "Team A players fetched: $players")
                    teamAPlayers.clear()
                    teamAPlayers.addAll(players)
                } else {
                    Log.d("fetchPlayersForLineup", "Team B players fetched: $players")
                    teamBPlayers.clear()
                    teamBPlayers.addAll(players)
                }

                playerAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Log.e("fetchPlayersForLineup", "Error fetching players", exception)
            }
    }




    private fun openPlayerProfile(player: Player) {
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
