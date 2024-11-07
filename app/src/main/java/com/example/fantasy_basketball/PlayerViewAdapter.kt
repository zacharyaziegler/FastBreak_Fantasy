package com.example.fantasy_basketball

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide // Use Glide for loading images

class PlayerAdapter(
    private var playerList: MutableList<Player>,
    private val onPlayerClick: (Player) -> Unit // Callback for click event
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // Constants for view types
    private val TYPE_HEADER = 0
    private val TYPE_ITEM = 1

    // ViewHolder class for each player item
    class PlayerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val playerName: TextView = view.findViewById(R.id.playerName)
        val playerPosition: TextView = view.findViewById(R.id.playerPosition)
        val playerTeam: TextView = view.findViewById(R.id.playerTeam)
        val playerStatus: TextView = view.findViewById(R.id.playerStatus)
        val playerImage: ImageView = view.findViewById(R.id.playerPicture)
        val playerPoints: TextView = view.findViewById(R.id.playerPoints)
        val playerRebounds: TextView = view.findViewById(R.id.playerRebounds)
        val playerAssists: TextView = view.findViewById(R.id.playerAssists)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_player_search_list, parent, false)
        return PlayerViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is PlayerViewHolder) {
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
                .placeholder(R.drawable.player)
                .into(holder.playerImage)

            // Set player's stats (if available)
            player.stats?.let { stats ->
                holder.playerPoints.text =  stats.pts ?: "--"
                holder.playerRebounds.text = stats.reb ?: "--"
                holder.playerAssists.text = stats.ast ?: "--"
            } ?: run {
                holder.playerPoints.text = "--"
                holder.playerRebounds.text = "--"
                holder.playerAssists.text = "--"
            }

            // Set click listener to open PlayerProfileFragment
            holder.itemView.setOnClickListener {
                onPlayerClick(player) // Trigger the callback with the clicked player
            }
        }
    }

    override fun getItemCount(): Int {
        return playerList.size
    }

    fun updateList(newPlayerList: MutableList<Player>) {
        playerList = newPlayerList
        notifyDataSetChanged()
    }
}
