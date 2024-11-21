package com.example.fantasy_basketball

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.fragment.app.FragmentTransaction
import androidx.navigation.fragment.findNavController

class DraftRoomFragment : Fragment() {
    private lateinit var leagueId: String
    private lateinit var teamId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_draft_room, container, false)
        val exitButton = view.findViewById<TextView>(R.id.exitDraftRoom)
        val bottomNav = view.findViewById<BottomNavigationView>(R.id.draftRoomBottomNav)
        // Retrieve leagueId and teamId from arguments
        arguments?.let {
            leagueId = it.getString("leagueId") ?: ""
            teamId = it.getString("teamId") ?: ""
        }

        // Set default fragment to load on entry
        loadFragment(DraftPlayersFragment().apply {
            arguments = Bundle().apply {
                putString("leagueId", leagueId)
                putString("teamId", teamId)
            }
        })

        // Set up the "Exit" button to navigate back to LeagueFragment
        exitButton.setOnClickListener {
            val bundle = Bundle().apply {
                putString("leagueId", leagueId)
            }
            findNavController().navigate(R.id.action_draftRoomFragment_to_leagueFragment, bundle)
        }

        // Set up listener for bottom navigation to switch fragments
        bottomNav.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_players -> {
                    loadFragment(DraftPlayersFragment().apply {
                        arguments = Bundle().apply {
                            putString("leagueId", leagueId)
                            putString("teamId", teamId)
                        }
                    })
                    true
                }
                R.id.nav_your_team -> {
                    loadFragment(YourTeamFragment().apply {
                        arguments = Bundle().apply {
                            putString("leagueId", leagueId)
                            putString("teamId", teamId)
                        }
                    })
                    true
                }
                R.id.nav_chat -> {
                    loadFragment(DraftChatFragment().apply {
                        arguments = Bundle().apply {
                            putString("leagueId", leagueId)
                            putString("teamId", teamId)
                        }
                    })
                    true
                }
                else -> false
            }
        }

        return view
    }

    private fun loadFragment(fragment: Fragment) {
        // Use childFragmentManager for transactions inside this parent fragment
        childFragmentManager.beginTransaction()
            .replace(R.id.draftRoomContent, fragment)
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .commit()
    }

    override fun onResume() {
        super.onResume()
        // Hide global bottom navigation in the draft room
        (activity as? MainActivity)?.hideBottomNavigation()
    }

    override fun onPause() {
        super.onPause()
        // Show it again when leaving draft room (optional)
        (activity as? MainActivity)?.showBottomNavigation()
    }
}
