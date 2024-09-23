package com.example.fantasy_basketball

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
/**
 * [RecyclerView.Adapter] that can display a [players].
 *
 */
// Change to List<dataClass>
class PlayerAdapter(private var players: List<String>) :
    RecyclerView.Adapter<PlayerAdapter.PlayerViewHolder>() {

    // ViewHolder represents each player item
    class PlayerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val playerName: TextView = itemView.findViewById(R.id.playerName)
        val playerStats: TextView = itemView.findViewById(R.id.playerStats)
    }

    // Inflate the fragment_player_search_list.xml for each player row
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_player_search_list, parent, false)
        return PlayerViewHolder(itemView)
    }

    // Bind the player's name and stats to the TextViews
    override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
        val player = players[position]
        holder.playerName.text = player
        holder.playerStats.text = player
    }

    // Return the total number of players in the list
    override fun getItemCount(): Int = players.size

    // Update the player list in adapter with the filtered list
    fun updateList(newPlayers: List<String>) {
        players = newPlayers // Update list of player in the list
        notifyDataSetChanged() // Notify the RecyclerView to show the new list

    }
}


