package com.example.fantasy_basketball

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView

/**
 * A fragment representing a list of Items.
 */
class PlayerSearchFragment : Fragment() {

//    private lateinit var playerAdapter: PlayerAdapter
    private lateinit var playerRecyclerView: RecyclerView
    private lateinit var playerSearchView: SearchView

    // Define the Player data
//    private val playerList = listOf(
//        Player("Player 1", "PG", "NY", Player.Status.H, 20, 12.5, 30, 5,R.drawable.player),
//        Player("Player 2", "SG", "Utah", Player.Status.Q, 25, 15.2, 32, 6,R.drawable.player),
//        Player("Player 3", "SF", "Por", Player.Status.IR, 15, 10.0, 28, 7,R.drawable.player),
//        Player("Player 4", "PF", "Ati", Player.Status.H, 30, 18.5, 40, 10,R.drawable.player)
//    )


    // Create a mutable list for filtering the players
//    private var filteredList = playerList.toMutableList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the main layout that contains the RecyclerView
//        val view = inflater.inflate(R.layout.fragment_player_search, container, false)

        // Find the RecyclerView and SearchView from the layout
//        playerRecyclerView = view.findViewById(R.id.playerRecyclerView)
//        playerSearchView = view.findViewById(R.id.playerSearchView)

        // Use LinearLayoutManager for vertical RecyclerView
//        playerRecyclerView.layoutManager = LinearLayoutManager(context)

        // Initialize the adapter with the original player list
//        playerAdapter = PlayerAdapter(filteredList)

        // Set the adapter to the RecyclerView
//        playerRecyclerView.adapter = playerAdapter

        // Set up SearchView to handle user input
//        playerSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            // Function runs when the user presses enter or submits
//            override fun onQueryTextSubmit(query: String?): Boolean {
//                filterPlayers(query)
//                return true
//            }
//
//            // Function runs when the user updates the search bar (adding or deleting text)
//            override fun onQueryTextChange(newText: String?): Boolean {
//                filterPlayers(newText)
//                return true
//            }
//        })
//
        return view
    }}

//    // Function to filter players based on user search input
//    private fun filterPlayers(input: String?) {
//        // If user input is not empty, filter the list by player name, otherwise show the original list
//        filteredList = if (!input.isNullOrEmpty()) {
//            playerList.filter { player ->
//                player.name.contains(input, ignoreCase = true)
//            }.toMutableList()
//        } else {
//            playerList.toMutableList() // Show the original list if input is empty
//        }

        // Update the adapter with the filtered list
//        playerAdapter.updateList(filteredList)
//    }
//}
