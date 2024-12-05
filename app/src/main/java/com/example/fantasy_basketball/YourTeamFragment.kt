package com.example.fantasy_basketball

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class YourTeamFragment : Fragment() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var leagueId: String
    private lateinit var teamId: String
    private lateinit var playerAdapter: YourTeamAdapter
    private val rosterPlayers = mutableListOf<Player>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_your_team, container, false)

        // Initialize Firestore and arguments
        firestore = FirebaseFirestore.getInstance()
        arguments?.let {
            leagueId = it.getString("leagueId") ?: ""
            teamId = it.getString("teamId") ?: ""
        }



        // Set up RecyclerView
        val recyclerView: RecyclerView = view.findViewById(R.id.recyclerViewYourTeam)
        recyclerView.layoutManager = LinearLayoutManager(context)
        playerAdapter = YourTeamAdapter(rosterPlayers)
        recyclerView.adapter = playerAdapter

        // Load roster data
        loadRoster()

        return view
    }

    private fun loadRoster() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Fetch roster (player IDs) from the team's document
                val teamSnapshot = firestore.collection("Leagues")
                    .document(leagueId)
                    .collection("Teams")
                    .document(teamId)
                    .get()
                    .await()
                val roster = teamSnapshot["roster"] as? List<String> ?: emptyList()

                // Fetch player details for each ID
                rosterPlayers.clear()
                for (playerId in roster) {
                    val playerSnapshot = firestore.collection("players")
                        .document(playerId)
                        .get()
                        .await()
                    val player = playerSnapshot.toObject(Player::class.java)
                    if (player != null) {
                        rosterPlayers.add(player)
                    }
                }

                // Update RecyclerView on the main thread
                withContext(Dispatchers.Main) {
                    playerAdapter.notifyDataSetChanged()
                }
            } catch (e: Exception) {
                Log.e("YourTeamFragment", "Error loading roster", e)
            }
        }
    }
}
