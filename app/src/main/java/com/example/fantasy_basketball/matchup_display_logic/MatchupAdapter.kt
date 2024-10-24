package com.example.fantasy_basketball.matchup_display_logic

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.fantasy_basketball.R

class MatchupAdapter(
    private val matchups: List<MatchupData>,
    private val onLeagueClick: (String, String) -> Unit // Callback for league click
) : RecyclerView.Adapter<MatchupAdapter.MatchupViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MatchupViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.fragment_matchup, parent, false)
        return MatchupViewHolder(view)
    }

    override fun onBindViewHolder(holder: MatchupViewHolder, position: Int) {
        val matchup = matchups[position]
        holder.bind(matchup)

        // Handle View League button click
        holder.viewLeagueButton.setOnClickListener {
            // Pass the leagueId and leagueName to the callback
            onLeagueClick(matchup.leagueId, matchup.leagueName)
        }
    }

    override fun getItemCount(): Int = matchups.size

    class MatchupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val viewLeagueButton: Button = itemView.findViewById(R.id.viewLeagueButton)
        private val userTeamImageView: ImageView = itemView.findViewById(R.id.userTeamImage)
        private val opponentTeamImageView: ImageView = itemView.findViewById(R.id.opponentTeamImage)

        fun bind(matchup: MatchupData) {
            // Bind the matchup data to your UI elements
            itemView.findViewById<TextView>(R.id.userTeamName).text = matchup.userTeamName
            itemView.findViewById<TextView>(R.id.opponentTeamName).text = matchup.opponentTeamName
            itemView.findViewById<TextView>(R.id.leagueName).text = matchup.leagueName

            // Load images using Glide
            if (matchup.userTeamImageUrl.isNotEmpty()) {
                Glide.with(itemView.context).load(matchup.userTeamImageUrl).into(userTeamImageView)
            } else {
                userTeamImageView.setImageResource(R.drawable.team_placeholder_image) // Set a placeholder
            }

            if (matchup.opponentTeamImageUrl.isNotEmpty()) {
                Glide.with(itemView.context).load(matchup.opponentTeamImageUrl).into(opponentTeamImageView)
            } else {
                opponentTeamImageView.setImageResource(R.drawable.team_placeholder_image) // Set a placeholder
            }
        }
    }
}
