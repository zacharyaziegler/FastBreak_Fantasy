package com.example.fantasy_basketball

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide // Use Glide for loading images

class PlayerAdapter(private var playerList: MutableList<Player>) :
    RecyclerView.Adapter<PlayerAdapter.PlayerViewHolder>() {

    // ViewHolder class to represent each item in the RecyclerView
    class PlayerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val playerName: TextView = view.findViewById(R.id.playerName)
        val playerPosition: TextView = view.findViewById(R.id.playerPosition)
        val playerTeam: TextView = view.findViewById(R.id.playerTeam)
        val playerStatus: TextView = view.findViewById(R.id.playerStatus)
        val playerImage: ImageView = view.findViewById(R.id.playerPicture)
        val playerPoints: TextView = view.findViewById(R.id.playerPoints)  // Fantasy Points
        val playerAvg: TextView = view.findViewById(R.id.playerAvg)        // Projected Avg
        val playerMin: TextView = view.findViewById(R.id.playerMinutes)    // Projected Minutes
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_player_search_list, parent, false)
        return PlayerViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
        val player = playerList[position]

        // Set player's basic info
        holder.playerName.text = player.longName
        holder.playerPosition.text = player.pos
        holder.playerTeam.text = player.team

        // Display status as DTD, O, or H
        holder.playerStatus.text = when (player.injury?.designation) {
            "Day-To-Day" -> "DTD"
            "Out" -> "O"
            null, "Healthy" -> "H"
            else -> player.injury?.designation ?: "N/A"
        }

        // Load player's headshot using Glide
        Glide.with(holder.itemView.context)
            .load(player.nbaComHeadshot)
            .placeholder(R.drawable.player) // Optional placeholder
            .into(holder.playerImage)

        // Set player's projections (if available)
        player.projection?.let {
            holder.playerPoints.text = it.fantasyPoints // Projected Fantasy Points
            holder.playerAvg.text = it.pts              // Projected Points
            holder.playerMin.text = it.mins             // Projected Minutes
        } ?: run {
            // Handle case where projections are not available
            holder.playerPoints.text = "N/A"
            holder.playerAvg.text = "N/A"
            holder.playerMin.text = "N/A"
        }
    }

    override fun getItemCount(): Int {
        return playerList.size
    }

    // Update the list when filtering is applied
    fun updateList(newPlayerList: MutableList<Player>) {
        playerList = newPlayerList
        notifyDataSetChanged()
    }
}
