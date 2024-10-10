package com.example.fantasy_basketball.matchup_display_logic

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.fantasy_basketball.R

class MatchupAdapter(private val matchups: List<MatchupData>) : RecyclerView.Adapter<MatchupAdapter.MatchupViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MatchupViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.fragment_matchup, parent, false)
        return MatchupViewHolder(view)
    }

    override fun onBindViewHolder(holder: MatchupViewHolder, position: Int) {
        val matchup = matchups[position]
        holder.bind(matchup)
    }

    override fun getItemCount(): Int {
        return matchups.size
    }

    class MatchupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val userTeamName: TextView = itemView.findViewById(R.id.userTeamName)
        private val opponentTeamName: TextView = itemView.findViewById(R.id.opponentTeamName)
        private val leagueName: TextView = itemView.findViewById(R.id.leagueName)

        fun bind(matchup: MatchupData) {
            userTeamName.text = matchup.userTeamName
            opponentTeamName.text = matchup.opponentTeamName
            leagueName.text = matchup.leagueName  // Ensure the league name is bound here
        }
    }
}

