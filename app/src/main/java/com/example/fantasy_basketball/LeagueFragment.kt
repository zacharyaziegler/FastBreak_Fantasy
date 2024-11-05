package com.example.fantasy_basketball

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.fantasy_basketball.R

class LeagueFragment : Fragment() {

    private lateinit var leagueId: String
    private lateinit var teamId: String
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    // TextView for team name and league name
    private lateinit var leagueNameTextView: TextView
    private lateinit var teamNameTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Retrieve the leagueId passed from HomeFragment
        arguments?.let {
            leagueId = it.getString("leagueId", "")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_league, container, false)

        // Initialize TextViews
        leagueNameTextView = view.findViewById(R.id.leagueName)
        teamNameTextView = view.findViewById(R.id.teamName)

        // Initialize the teamInfoButton and set its click listener
        val teamInfoButton: ImageButton = view.findViewById(R.id.teamInfoButton)
        teamInfoButton.setOnClickListener {
            Log.d("LeagueFragment", "Pressed")
            navigateToTeamInfoFragment()
        }

        // Load the league and team data
        loadLeagueAndTeamData()

        // Initialize the toolbar and setup hamburger menu
        val toolbar: Toolbar = view.findViewById(R.id.leagueToolbar)
        toolbar.setNavigationIcon(R.drawable.ic_hamburger_menu)
        toolbar.setNavigationOnClickListener {
            showPopupMenu(it)
        }

        return view
    }

    private fun showPopupMenu(view: View) {
        // Create a PopupMenu and inflate the menu layout
        val popupMenu = PopupMenu(requireContext(), view)
        popupMenu.menuInflater.inflate(R.menu.league_menu, popupMenu.menu)

        // Handle menu item clicks
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_league_info -> {
                    // Navigate to League Info fragment (add this fragment if not already done)
//                    findNavController().navigate(R.id.action_leagueFragment_to_leagueInfoFragment)
                    true
                }
                R.id.action_invite_friends -> {
                    // Navigate to Invite Friends fragment (implement this feature)
//                    findNavController().navigate(R.id.action_leagueFragment_to_inviteFriendsFragment)
                    true
                }
                R.id.action_league_chat -> {
                    // Navigate to League Chat fragment and pass the leagueId
                    val bundle = Bundle().apply {
                        putString("leagueId", leagueId)
                    }
                    findNavController().navigate(R.id.action_leagueFragment_to_leagueChatFragment, bundle)
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun loadLeagueAndTeamData() {
        val currentUserId = auth.currentUser?.uid ?: return

        // Fetch league details
        firestore.collection("Leagues").document(leagueId).get().addOnSuccessListener { leagueDoc ->
            if (leagueDoc.exists()) {
                val leagueName = leagueDoc.getString("leagueName") ?: "Unknown League"
                leagueNameTextView.text = leagueName

                // Fetch the user's team in this league
                firestore.collection("Leagues").document(leagueId).collection("Teams")
                    .whereEqualTo("ownerID", currentUserId).limit(1).get()
                    .addOnSuccessListener { teamsSnapshot ->
                        if (!teamsSnapshot.isEmpty) {
                            val teamDoc = teamsSnapshot.documents[0]
                            val teamName = teamDoc.getString("teamName") ?: "Unknown Team"
                            teamId = teamDoc.id  // Save the teamId for navigation
                            teamNameTextView.text = teamName
                        } else {
                            teamNameTextView.text = "No Team Found"
                        }
                    }
            }
        }
    }

    private fun navigateToTeamInfoFragment() {
        val bundle = Bundle().apply {
            putString("leagueId", leagueId)
            putString("teamId", teamId)
        }

        // Navigate to TeamInfoFragment and pass leagueId and teamId
        findNavController().navigate(R.id.action_leagueFragment_to_teamInfoFragment, bundle)
    }
}
