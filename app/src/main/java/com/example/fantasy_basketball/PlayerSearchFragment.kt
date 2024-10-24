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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject

class PlayerSearchFragment : Fragment() {

    private lateinit var playerAdapter: PlayerAdapter
    private lateinit var playerRecyclerView: RecyclerView
    private lateinit var playerSearchView: SearchView
    private lateinit var firestore: FirebaseFirestore

    // Create a mutable list to hold player data from Firestore
    private var playerList = mutableListOf<Player>()
    private var filteredList = mutableListOf<Player>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the main layout that contains the RecyclerView
        val view = inflater.inflate(R.layout.fragment_player_search, container, false)

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance()

        // Find the RecyclerView and SearchView from the layout
        playerRecyclerView = view.findViewById(R.id.playerRecyclerView)
        playerSearchView = view.findViewById(R.id.playerSearchView)

        // Use LinearLayoutManager for vertical RecyclerView
        playerRecyclerView.layoutManager = LinearLayoutManager(context)

        // Initialize the adapter with the empty player list
        playerAdapter = PlayerAdapter(filteredList)
        playerRecyclerView.adapter = playerAdapter

        // Set up SearchView to handle user input
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

        // Fetch player data from Firestore
        fetchPlayersFromFirestore()

        return view
    }

    // Fetch player data from Firestore
    private fun fetchPlayersFromFirestore() {
        firestore.collection("players")
            .get()
            .addOnSuccessListener { result ->
                playerList.clear() // Clear the list before adding new data
                for (document in result) {
                    // Convert Firestore document to Player object
                    val player = document.toObject<Player>()
                    Log.d("Player Data", player.toString()) // Log the player data
                    playerList.add(player)
                }
                filteredList = playerList.toMutableList()
                playerAdapter.updateList(filteredList) // Update adapter with new data
            }
            .addOnFailureListener { exception ->
                // Handle any errors that may occur
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
}

