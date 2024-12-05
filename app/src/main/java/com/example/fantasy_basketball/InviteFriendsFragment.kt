package com.example.fantasy_basketball

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels

class InviteFriendsFragment : Fragment() {

    private lateinit var inviteCodeTextView: TextView
   // private lateinit var leagueNameTextView: TextView
    private lateinit var shareInviteButton: Button
    private var inviteCode: String = ""
    private var leagueName: String = ""
    private val sharedViewModel: SharedDataViewModel by activityViewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Retrieve the inviteCode and leagueName passed from the previous fragment
        arguments?.let {
            inviteCode = it.getString("inviteCode", "")
            leagueName = it.getString("leagueName", "")
        }
        leagueName = sharedViewModel.leagueName.toString()





    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_invite_friends, container, false)

        // Initialize Views
        inviteCodeTextView = view.findViewById(R.id.inviteCodeTextView)
        //leagueNameTextView = view.findViewById(R.id.leagueNameTextView)
        shareInviteButton = view.findViewById(R.id.shareInviteButton)

        // Validate and Set the league name and invite code
        if (leagueName.isNotEmpty() && inviteCode.isNotEmpty()) {
          //  leagueNameTextView.text = leagueName
            inviteCodeTextView.text = inviteCode
        } else {
            Toast.makeText(requireContext(), "League details are missing", Toast.LENGTH_SHORT).show()
            // Optionally navigate back or handle missing data here
        }

        // Handle Share Button Click
        shareInviteButton.setOnClickListener {
            shareInviteCode()
        }



        return view
    }

    // Function to share the invite code via Android's share menu
    private fun shareInviteCode() {
        if (inviteCode.isNotEmpty()) {
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "Join my league using this invite code: $inviteCode")
                type = "text/plain"
            }
            startActivity(Intent.createChooser(shareIntent, "Share invite code via"))
        } else {
            Toast.makeText(requireContext(), "No invite code available", Toast.LENGTH_SHORT).show()
        }
    }
}
