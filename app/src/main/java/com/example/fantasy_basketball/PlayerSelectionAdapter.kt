package com.example.fantasy_basketball

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PlayerSelectionAdapter(
    private val players: List<Player?>,
    private val onPlayerSelected: (Player?) -> Unit
) : RecyclerView.Adapter<PlayerSelectionAdapter.PlayerViewHolder>() {

    inner class PlayerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val playerName: TextView = view.findViewById(R.id.playerName)

        fun bind(player: Player?) {
            playerName.text = player?.longName
            itemView.setOnClickListener { onPlayerSelected(player) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.dialog_player_item, parent, false)
        return PlayerViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
        holder.bind(players[position])
    }

    override fun getItemCount() = players.size
}
