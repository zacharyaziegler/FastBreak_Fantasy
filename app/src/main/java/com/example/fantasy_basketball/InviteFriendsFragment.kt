package com.example.fantasy_basketball

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment

class InviteFriendsFragment : Fragment() {

    private lateinit var inviteCodeTextView: TextView
    private lateinit var leagueNameTextView: TextView
    private lateinit var shareInviteButton: Button
    private lateinit var inviteCode: String
    private lateinit var leagueName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Retrieve the inviteCode and leagueName passed from the previous fragment
        arguments?.let {
            inviteCode = it.getString("inviteCode", "")
            leagueName = it.getString("leagueName", "")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_invite_friends, container, false)

        // Initialize Views
        inviteCodeTextView = view.findViewById(R.id.inviteCodeTextView)
        leagueNameTextView = view.findViewById(R.id.leagueNameTextView)
        shareInviteButton = view.findViewById(R.id.shareInviteButton)

        // Set the league name and invite code
        leagueNameTextView.text = leagueName
        inviteCodeTextView.text = inviteCode

        // Handle Share Button Click
        shareInviteButton.setOnClickListener {
            shareInviteCode()
        }

        // Handle toolbar setup
        val toolbar: Toolbar = view.findViewById(R.id.inviteToolbar)
        toolbar.setNavigationOnClickListener {
            activity?.onBackPressed()
        }

        return view
    }

    // Function to share the invite code via Android's share menu
    private fun shareInviteCode() {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "Join my league using this invite code: $inviteCode")
            type = "text/plain"
        }
        startActivity(Intent.createChooser(shareIntent, "Share invite code via"))
    }
}
