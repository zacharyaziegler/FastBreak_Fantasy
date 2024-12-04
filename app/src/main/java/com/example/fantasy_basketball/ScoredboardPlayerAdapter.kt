package com.example.fantasy_basketball

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ScoreboardPlayerAdapter(
    private val teamAPlayers: List<Player>,
    private val teamBPlayers: List<Player>,
    private val onPlayerClick: (Player) -> Unit
) : RecyclerView.Adapter<ScoreboardPlayerAdapter.PlayerMatchupViewHolder>() {

    class PlayerMatchupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val teamAPlayerImage: ImageView = itemView.findViewById(R.id.teamAPlayerImage)
        val teamAPlayerName: TextView = itemView.findViewById(R.id.teamAPlayerName)
        val teamAPlayerTeam: TextView = itemView.findViewById(R.id.teamAPlayerTeam)
        val teamAPlayerPoints: TextView = itemView.findViewById(R.id.teamAPlayerPoints)
        val playerPosition: TextView = itemView.findViewById(R.id.playerPosition)
        val teamBPlayerImage: ImageView = itemView.findViewById(R.id.teamBPlayerImage)
        val teamBPlayerName: TextView = itemView.findViewById(R.id.teamBPlayerName)
        val teamBPlayerTeam: TextView = itemView.findViewById(R.id.teamBPlayerTeam)
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

        holder.teamAPlayerName.text = teamAPlayer?.longName ?: "N/A"
        holder.teamAPlayerPoints.text = teamAPlayer?.stats?.pts ?: "0.0"
        holder.teamAPlayerTeam.text = teamAPlayer?.team ?: "N/A"
        Glide.with(holder.itemView.context)
            .load(teamAPlayer?.nbaComHeadshot ?: "")
            .placeholder(R.drawable.ic_default_profile)
            .into(holder.teamAPlayerImage)

        holder.playerPosition.text = teamAPlayer?.pos ?: teamBPlayer?.pos ?: "N/A"

        holder.teamBPlayerName.text = teamBPlayer?.longName ?: "N/A"
        holder.teamBPlayerPoints.text = teamBPlayer?.stats?.pts ?: "0.0"
        holder.teamBPlayerTeam.text = teamBPlayer?.team ?: "N/A"
        Glide.with(holder.itemView.context)
            .load(teamBPlayer?.nbaComHeadshot ?: "")
            .placeholder(R.drawable.ic_default_profile)
            .into(holder.teamBPlayerImage)

        holder.itemView.findViewById<View>(R.id.teamAPlayerImage).setOnClickListener {
            teamAPlayer?.let { onPlayerClick(it) }
        }

        holder.itemView.findViewById<View>(R.id.teamBPlayerImage).setOnClickListener {
            teamBPlayer?.let { onPlayerClick(it) }
        }
    }

    override fun getItemCount(): Int {
        return maxOf(teamAPlayers.size, teamBPlayers.size)
    }
}


class MatchupsAdapter(
    private val matchups: List<FullMatchup>,
    private val onMatchupClick: (FullMatchup) -> Unit
) : RecyclerView.Adapter<MatchupsAdapter.MatchupViewHolder>() {

    class MatchupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val team1Logo: ImageView = itemView.findViewById(R.id.team1Logo)
        val team1Name: TextView = itemView.findViewById(R.id.team1Name)
        val team2Logo: ImageView = itemView.findViewById(R.id.team2Logo)
        val team2Name: TextView = itemView.findViewById(R.id.team2Name)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MatchupViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.matchup_item, parent, false)
        return MatchupViewHolder(view)
    }

    override fun onBindViewHolder(holder: MatchupViewHolder, position: Int) {
        val matchup = matchups[position]

        val team1Details = matchup.team1Details
        if (team1Details != null) {
            holder.team1Name.text = team1Details.name
            Glide.with(holder.itemView.context)
                .load(team1Details.logo)
                .placeholder(R.drawable.ic_default_profile)
                .into(holder.team1Logo)
        } else {
            holder.team1Name.text = "Unknown Team"
            holder.team1Logo.setImageResource(R.drawable.ic_default_profile)
        }

        val team2Details = matchup.team2Details
        if (team2Details != null) {
            holder.team2Name.text = team2Details.name
            Glide.with(holder.itemView.context)
                .load(team2Details.logo)
                .placeholder(R.drawable.ic_default_profile)
                .into(holder.team2Logo)
        } else {
            holder.team2Name.text = "Unknown Team"
            holder.team2Logo.setImageResource(R.drawable.ic_default_profile)
        }

        holder.itemView.setOnClickListener { onMatchupClick(matchup) }
    }



    override fun getItemCount(): Int = matchups.size
}

