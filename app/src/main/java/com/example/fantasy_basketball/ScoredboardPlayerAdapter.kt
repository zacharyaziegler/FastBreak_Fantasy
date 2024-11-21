package com.example.fantasy_basketball

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ScoreboardPlayerAdapter(
    private val teamAPlayers: List<Player>,
    private val teamBPlayers: List<Player>
) : RecyclerView.Adapter<ScoreboardPlayerAdapter.PlayerMatchupViewHolder>() {

    class PlayerMatchupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val teamAPlayerName: TextView = itemView.findViewById(R.id.teamAPlayerName)
        val teamAPlayerPoints: TextView = itemView.findViewById(R.id.teamAPlayerPoints)
        val playerPosition: TextView = itemView.findViewById(R.id.playerPosition)
        val teamBPlayerName: TextView = itemView.findViewById(R.id.teamBPlayerName)
        val teamBPlayerPoints: TextView = itemView.findViewById(R.id.teamBPlayerPoints)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerMatchupViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.scoreboard_player_item, parent, false)
        return PlayerMatchupViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlayerMatchupViewHolder, position: Int) {
        val teamAPlayer = teamAPlayers.getOrNull(position)
        val teamBPlayer = teamBPlayers.getOrNull(position)

        // Bind Team A player data
        holder.teamAPlayerName.text = teamAPlayer?.longName ?: "N/A"
        holder.teamAPlayerPoints.text = teamAPlayer?.stats?.pts ?: "0.0"

        // Bind Player Position (Shared between both teams)
        holder.playerPosition.text = teamAPlayer?.pos ?: teamBPlayer?.pos ?: "N/A"

        // Bind Team B player data
        holder.teamBPlayerName.text = teamBPlayer?.longName ?: "N/A"
        holder.teamBPlayerPoints.text = teamBPlayer?.stats?.pts ?: "0.0"
    }

    override fun getItemCount(): Int {
        return maxOf(teamAPlayers.size, teamBPlayers.size)
    }
}
