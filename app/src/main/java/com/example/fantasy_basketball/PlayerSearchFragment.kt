package com.example.fantasy_basketball

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
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

    private lateinit var playerAdapter: PlayerAdapter
    private lateinit var playerRecyclerView: RecyclerView
    private lateinit var playerSearchView: SearchView
    //Testing with input
    private val playerList = listOf(
        "Player 1",
        "Player 2",
        "Player 3",
        "Player 4",
        "Player 5"
    )
    private var filteredList = playerList.toMutableList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the main layout that contains the RecyclerView
        // Take design of playerSearchFragment.xml and use on the screen
        val view = inflater.inflate(R.layout.fragment_player_search, container, false)

        // Find the RecyclerView and SearchView from the layout
        playerRecyclerView = view.findViewById(R.id.playerRecyclerView)
        playerSearchView = view .findViewById(R.id.playerSearchView)
        playerRecyclerView.layoutManager = LinearLayoutManager(context)

        //Initialize the adapter with the original player list
        playerAdapter = PlayerAdapter(filteredList)

        // Set the adapter to the RecyclerView
        playerRecyclerView.adapter = playerAdapter

        //Set up SearchView to handle user input
        playerSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            //function run when user press enter or submit
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterPlayers(query)
                return true
            }
            //function run when user update the search bar( adding or deleting)
            override fun onQueryTextChange(newText: String?): Boolean {
                filterPlayers(newText)
                return true
            }
        })

        return view
    }
    //Function to filter player based on the user search input
    private fun filterPlayers(input: String?) {
        // If the userInput is not empty, filter the list, otherwise show the original list
        filteredList = if (!input.isNullOrEmpty()) {
            playerList.filter { it.contains(input, ignoreCase = true) }.toMutableList()
        } else {
            playerList.toMutableList()
        }

        // Update the adapter with the filtered list
        playerAdapter.updateList(filteredList)
    }

}
