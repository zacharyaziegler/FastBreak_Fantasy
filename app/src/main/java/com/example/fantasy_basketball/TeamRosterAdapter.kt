package com.example.fantasy_basketball

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TeamRosterAdapter(private val roster: List<Player>) : RecyclerView.Adapter<TeamRosterAdapter.PlayerViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_player, parent, false)
        return PlayerViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
        val player = roster[position]
        holder.bind(player)
    }

    override fun getItemCount(): Int = roster.size

    inner class PlayerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val playerName: TextView = itemView.findViewById(R.id.playerName)
        private val playerPosition: TextView = itemView.findViewById(R.id.playerPosition)
        private val playerTeam: TextView = itemView.findViewById(R.id.playerTeam)

        fun bind(player: Player) {
            playerName.text = player.longName
            playerPosition.text = player.pos
            playerTeam.text = player.team
        }
    }
}
