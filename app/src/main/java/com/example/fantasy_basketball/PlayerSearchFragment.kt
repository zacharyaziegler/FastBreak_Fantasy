package com.example.fantasy_basketball

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.launch


class PlayerSearchFragment : Fragment() {

    private lateinit var playerAdapter: PlayerAdapter
    private lateinit var playerRecyclerView: RecyclerView
    private lateinit var playerSearchView: SearchView
    private lateinit var firestore: FirebaseFirestore

    private var playerList = mutableListOf<Player>()
    private var filteredList = mutableListOf<Player>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_player_search, container, false)

        firestore = FirebaseFirestore.getInstance()

        playerRecyclerView = view.findViewById(R.id.playerRecyclerView)
        playerSearchView = view.findViewById(R.id.playerSearchView)

        playerRecyclerView.layoutManager = LinearLayoutManager(context)

        // Initialize the adapter with a click listener to navigate to PlayerProfileFragment
        playerAdapter = PlayerAdapter(filteredList) { selectedPlayer ->
            openPlayerProfile(selectedPlayer)
        }
        playerRecyclerView.adapter = playerAdapter

        playerSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterPlayers(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterPlayers(newText)
                return true
            }
        })

        viewLifecycleOwner.lifecycleScope.launch {
            fetchPlayersFromFirestore()
        }

        return view
    }
    private fun fetchPlayersFromFirestore() {
        firestore.collection("players")
            .get()
            .addOnSuccessListener { result ->
                playerList.clear() // Clear the list before adding new data
                for (document in result) {
                    if (document is QueryDocumentSnapshot) {
                        Log.d("Document Data", document.data.toString())

                        // Extract fields directly from the document
                        val playerID = document.getString("playerID") ?: ""
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
                            designation = if (designation.isNullOrEmpty()) "Healthy" else designation,

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

                        playerList.add(player)
                    }
                }
                filteredList = playerList.toMutableList()
                playerAdapter.updateList(filteredList) // Update adapter with new data
            }
            .addOnFailureListener { exception ->
                exception.printStackTrace()
            }
    }





    // Function to filter players based on user search input
    private fun filterPlayers(input: String?) {
        filteredList = if (!input.isNullOrEmpty()) {
            playerList.filter { player ->
                player.longName.contains(input, ignoreCase = true)
            }.toMutableList()
        } else {
            playerList.toMutableList() // Show the original list if input is empty
        }

        // Update the adapter with the filtered list
        playerAdapter.updateList(filteredList)
    }

    // Navigate to PlayerProfileFragment and pass the selected player data
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

