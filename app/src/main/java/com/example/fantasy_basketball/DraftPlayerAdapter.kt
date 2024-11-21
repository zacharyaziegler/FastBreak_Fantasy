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
import com.bumptech.glide.load.engine.DiskCacheStrategy

class DraftPlayerAdapter(
    private var players: List<Player>,
    private val onDraftClick: (Player) -> Unit,
    private var isUserOnTheClock: Boolean
) : RecyclerView.Adapter<DraftPlayerAdapter.PlayerViewHolder>() {

    inner class PlayerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val playerName = itemView.findViewById<TextView>(R.id.playerName)
        private val playerFantasyPoints = itemView.findViewById<TextView>(R.id.playerFantasyPoints)
        private val draftButton = itemView.findViewById<Button>(R.id.draftButton)
        private val playerTeam = itemView.findViewById<TextView>(R.id.playerTeam)
        private val playerInjury = itemView.findViewById<TextView>(R.id.playerInjuryStatus)
        private val playerImage = itemView.findViewById<ImageView>(R.id.playerImage)

        fun bind(player: Player) {
            playerName.text = player.longName
            playerFantasyPoints.text = player.projection?.fantasyPoints
            playerTeam.text = player.team
            playerInjury.text = player.injury?.status

            // Load player image using Glide
            Glide.with(itemView.context)
                .load(player.nbaComHeadshot) // Assuming player.imageUrl contains the URL for the player's image
                .placeholder(R.drawable.ic_player_placeholder) // Placeholder image while loading
                .error(R.drawable.ic_player_placeholder) // Fallback image if the URL is invalid
                .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache the image for better performance
                .into(playerImage)

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
            // Handle empty list
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