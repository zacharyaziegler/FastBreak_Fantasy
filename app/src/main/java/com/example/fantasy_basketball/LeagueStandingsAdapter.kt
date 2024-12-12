package com.example.fantasy_basketball

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class LeagueStandingsAdapter(
    private val teams: List<Team>
) : RecyclerView.Adapter<LeagueStandingsAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val teamRank: TextView = itemView.findViewById(R.id.teamRank)
        val teamIcon: ImageView = itemView.findViewById(R.id.teamIcon)
        val teamName: TextView = itemView.findViewById(R.id.teamName)
        val teamRecord: TextView = itemView.findViewById(R.id.teamRecord)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.league_standing_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val team = teams[position]
        holder.teamRank.text = (position + 1).toString()
        holder.teamName.text = team.name
        holder.teamRecord.text = "${team.wins}-${team.losses}"

        when (position) {
            0 -> holder.teamRank.setTextColor(
                ContextCompat.getColor(
                    holder.itemView.context,
                    R.color.gold
                )
            )

            1 -> holder.teamRank.setTextColor(
                ContextCompat.getColor(
                    holder.itemView.context,
                    R.color.silver
                )
            )

            2 -> holder.teamRank.setTextColor(
                ContextCompat.getColor(
                    holder.itemView.context,
                    R.color.bronze
                )
            )

            else -> holder.teamRank.setTextColor(
                ContextCompat.getColor(
                    holder.itemView.context,
                    android.R.color.black
                )
            )
        }

        // Load team icon
        Glide.with(holder.itemView.context)
            .load(team.iconUrl)
            .placeholder(R.drawable.ic_default_profile)
            .into(holder.teamIcon)
    }

    override fun getItemCount(): Int = teams.size
}
