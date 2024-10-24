package com.example.fantasy_basketball.matchup_display_logic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.fantasy_basketball.R

class MatchupFragment : Fragment() {

    private var teamName: String? = null
    private var opponentName: String? = null
    private var userTeamImageUrl: String? = null
    private var opponentTeamImageUrl: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_matchup, container, false)

        val userTeamNameTextView: TextView = view.findViewById(R.id.userTeamName)
        val opponentTeamNameTextView: TextView = view.findViewById(R.id.opponentTeamName)
        val userTeamImageView: ImageView = view.findViewById(R.id.userTeamImage)
        val opponentTeamImageView: ImageView = view.findViewById(R.id.opponentTeamImage)

        // Set the team names
        userTeamNameTextView.text = teamName
        opponentTeamNameTextView.text = opponentName

        // Use Glide to load the team images
        if (!userTeamImageUrl.isNullOrEmpty()) {
            Glide.with(this).load(userTeamImageUrl).into(userTeamImageView)
        } else {
            userTeamImageView.setImageResource(R.drawable.team_placeholder_image)  // Placeholder image
        }

        if (!opponentTeamImageUrl.isNullOrEmpty()) {
            Glide.with(this).load(opponentTeamImageUrl).into(opponentTeamImageView)
        } else {
            opponentTeamImageView.setImageResource(R.drawable.team_placeholder_image)  // Placeholder image
        }

        return view
    }

    companion object {
        // Create new instance with arguments
        @JvmStatic
        fun newInstance(teamName: String, opponentName: String, userTeamImageUrl: String, opponentTeamImageUrl: String): MatchupFragment {
            val fragment = MatchupFragment()
            val args = Bundle()
            args.putString("teamName", teamName)
            args.putString("opponentName", opponentName)
            args.putString("userTeamImageUrl", userTeamImageUrl)
            args.putString("opponentTeamImageUrl", opponentTeamImageUrl)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            teamName = it.getString("teamName")
            opponentName = it.getString("opponentName")
            userTeamImageUrl = it.getString("userTeamImageUrl")
            opponentTeamImageUrl = it.getString("opponentTeamImageUrl")
        }
    }
}
