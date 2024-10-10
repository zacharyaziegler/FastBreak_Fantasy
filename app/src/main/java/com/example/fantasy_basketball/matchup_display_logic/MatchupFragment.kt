package com.example.fantasy_basketball.matchup_display_logic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.fantasy_basketball.R

class MatchupFragment : Fragment() {

    private var teamName: String? = null
    private var opponentName: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_matchup, container, false)

        val userTeamNameTextView: TextView = view.findViewById(R.id.userTeamName)
        val opponentTeamNameTextView: TextView = view.findViewById(R.id.opponentTeamName)
        val userTeamImageView: ImageView = view.findViewById(R.id.userTeamImage)
        val opponentTeamImageView: ImageView = view.findViewById(R.id.opponentTeamImage)

        // Set the team names and profile images
        userTeamNameTextView.text = teamName
        opponentTeamNameTextView.text = opponentName
        // Optionally, set images or fetch based on team data

        return view
    }

    companion object {
        // Create new instance with arguments
        @JvmStatic
        fun newInstance(teamName: String, opponentName: String): MatchupFragment {
            val fragment = MatchupFragment()
            val args = Bundle()
            args.putString("teamName", teamName)
            args.putString("opponentName", opponentName)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            teamName = it.getString("teamName")
            opponentName = it.getString("opponentName")
        }
    }
}
