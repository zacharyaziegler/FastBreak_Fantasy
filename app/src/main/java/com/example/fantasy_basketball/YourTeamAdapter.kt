package com.example.fantasy_basketball

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class YourTeamAdapter(private val playerList: List<Player>) :
    RecyclerView.Adapter<YourTeamAdapter.PlayerViewHolder>() {

    inner class PlayerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val playerImage: ImageView = view.findViewById(R.id.playerImage)
        val playerName: TextView = view.findViewById(R.id.playerName)
        val playerPosition: TextView = view.findViewById(R.id.playerPosition)
        val playerTeam: TextView = view.findViewById(R.id.playerTeam)
        val playerInjuryStatus: TextView = view.findViewById(R.id.playerInjuryStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_your_team_player, parent, false)
        return PlayerViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
        val player = playerList[position]
        holder.playerName.text = player.longName
        holder.playerPosition.text = player.pos
        holder.playerTeam.text = player.team
        holder.playerInjuryStatus.text = player.injury?.status ?: "Healthy"

        // Load the player's image (if you have a URL, use a library like Glide or Picasso)
        // Example with Glide:
        // Glide.with(holder.itemView.context).load(player.imageUrl).into(holder.playerImage)
    }

    override fun getItemCount(): Int = playerList.size
}
