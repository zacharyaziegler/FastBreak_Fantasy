package com.example.fantasy_basketball

import RosterAdapter
import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


class RosterFragment : Fragment() {
    private lateinit var startingLineupAdapter: RosterAdapter
    private lateinit var benchAdapter: RosterAdapter

    private val startingLineupSlots = listOf("PG", "SG", "SF", "PF", "C", "G", "F", "UTIL", "UTIL", "UTIL")
    private val benchLineupSlots = listOf("PG", "SG", "SF", "PF", "C", "UTIL", "UTIL", "UTIL", "UTIL", "UTIL")

    private val startingLineup = mutableListOf<Player?>(null, null, null, null, null, null, null, null, null, null)
    private val bench = mutableListOf<Player?>(null, null, null, null, null, null, null, null, null, null)

    private val roster = listOf(
        Player("1", "LeBron James", "23", "SF", "LAL", "1610612747", "https://example.com/headshot.jpg", null, null, null),
        Player("2", "Giannis Antetokounmpo", "34", "PF", "MIL", "1610612749", "https://example.com/headshot.jpg", null, null, null)
        // Add more players as needed
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_roster, container, false)

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

        return view
    }

    private fun showPlayerSelectionDialog(position: Int, slot: String, isStarting: Boolean) {
        // Filter eligible players based on the slot requirements
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
        val adapter = PlayerSelectionAdapter(eligiblePlayers) { selectedPlayer ->
            // Remove player from any existing slot
            removePlayerFromCurrentSlot(selectedPlayer.playerID)

            // Add selected player to the new slot
            if (isStarting) {
                startingLineup[position] = selectedPlayer
                startingLineupAdapter.notifyItemChanged(position)
            } else {
                bench[position] = selectedPlayer
                benchAdapter.notifyItemChanged(position)
            }
        }

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        AlertDialog.Builder(requireContext())
            .setTitle("Select Player for $slot")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun removePlayerFromCurrentSlot(playerID: String) {
        // Check and remove from starting lineup
        startingLineup.forEachIndexed { index, player ->
            if (player?.playerID == playerID) {
                startingLineup[index] = null
                startingLineupAdapter.notifyItemChanged(index)
            }
        }

        // Check and remove from bench
        bench.forEachIndexed { index, player ->
            if (player?.playerID == playerID) {
                bench[index] = null
                benchAdapter.notifyItemChanged(index)
            }
        }
    }

}


