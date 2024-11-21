package com.example.fantasy_basketball

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class DraftPlayerAdapter(
    private var players: List<Player>,
    private val onDraftClick: (Player) -> Unit,
    private var isUserOnTheClock: Boolean
) : RecyclerView.Adapter<DraftPlayerAdapter.PlayerViewHolder>() {

    inner class PlayerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val playerName = itemView.findViewById<TextView>(R.id.playerName)
        private val playerFantasyPoints = itemView.findViewById<TextView>(R.id.playerFantasyPoints)
        private val draftButton = itemView.findViewById<Button>(R.id.draftButton)

        fun bind(player: Player) {
            playerName.text = player.longName
            playerFantasyPoints.text = player.projection?.fantasyPoints

            draftButton.isEnabled = isUserOnTheClock

            // Change the appearance of the button when it is disabled
            if (isUserOnTheClock) {
                draftButton.alpha = 1.0f // Fully opaque
            } else {
                draftButton.alpha = 0.5f // Semi-transparent to indicate it is disabled
                           }

            draftButton.setOnClickListener {
                if (isUserOnTheClock) {
                    onDraftClick(player)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_draft_player, parent, false)
        return PlayerViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
        if (players.isEmpty()) {
            // Handle empty list gracefully (optional)
            Log.e("DraftPlayerAdapter", "No players available to bind")
            return
        }
        holder.bind(players[position])
    }

    override fun getItemCount(): Int = players.size

    fun updateList(newPlayers: List<Player>, isUserOnTheClock: Boolean ) {
        players = newPlayers
        this.isUserOnTheClock = isUserOnTheClock
        notifyDataSetChanged()
    }
}