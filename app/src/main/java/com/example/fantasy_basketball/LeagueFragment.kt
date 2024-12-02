package com.example.fantasy_basketball

import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.fantasy_basketball.R
import java.util.Date
import java.util.concurrent.TimeUnit

class LeagueFragment : Fragment() {

    private lateinit var leagueId: String
    private lateinit var teamId: String
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var leagueNameTextView: TextView
    private lateinit var teamNameTextView: TextView
    private lateinit var draftMessageTextView: TextView
    private lateinit var draftCountdownTextView: TextView
    private lateinit var enterDraftRoomButton: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var teamInfoButton: ImageButton

    private var inviteCode: String = ""
    private var countdownTimer: CountDownTimer? = null

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

        // Initialize views
        leagueNameTextView = view.findViewById(R.id.leagueName)
        teamNameTextView = view.findViewById(R.id.teamName)
        draftMessageTextView = view.findViewById(R.id.draftMessageTextView)
        draftCountdownTextView = view.findViewById(R.id.draftCountdownTextView)
        enterDraftRoomButton = view.findViewById(R.id.enterDraftRoomButton)
        recyclerView = view.findViewById(R.id.recyclerViewRoster)
        teamInfoButton = view.findViewById(R.id.teamInfoButton)

        // Initialize RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Placeholder data for testing purposes
        val placeholderRoster = List(13) { Player("Player ${it + 1}", "Position ${it + 1}", "Team ${it + 1}") }
        recyclerView.adapter = TeamRosterAdapter(placeholderRoster)

        // Set up the toolbar
        val toolbar: Toolbar = view.findViewById(R.id.leagueToolbar)
        toolbar.setNavigationIcon(R.drawable.ic_hamburger_menu)
        toolbar.setNavigationOnClickListener {
            showPopupMenu(it)
        }

        teamInfoButton.setOnClickListener {
            navigateToTeamInfoFragment()
        }

        // Navigate to Draft Room when the button is clicked
        enterDraftRoomButton.setOnClickListener {
            // Ensure teamId is initialized before navigating
            if (this::teamId.isInitialized) {
                val bundle = Bundle().apply {
                    putString("leagueId", leagueId)
                    putString("teamId", teamId)
                }
                findNavController().navigate(R.id.action_leagueFragment_to_draftRoomFragment, bundle)
            } else {
                Log.e("LeagueFragment", "Team ID not initialized. Cannot navigate to DraftRoomFragment.")
            }
        }
        // Load league and team data
        loadLeagueAndTeamData()

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Cancel countdown timer if it's running
        countdownTimer?.cancel()
    }

    private fun showPopupMenu(view: View) {
        // Create a PopupMenu and inflate the menu layout
        val popupMenu = PopupMenu(requireContext(), view)
        popupMenu.menuInflater.inflate(R.menu.league_menu, popupMenu.menu)

        val currentUserId = auth.currentUser?.uid ?: return

        // Check if the user is the commissioner
        firestore.collection("Leagues").document(leagueId).get().addOnSuccessListener { leagueDoc ->
            if (leagueDoc.exists()) {
                val commissionerId = leagueDoc.getString("commissionerID")
                val leagueName = leagueDoc.getString("leagueName")
                    ?: "Unknown League"  // Get the league name here

                // Only make "League Settings" visible if the current user is the commissioner
                if (commissionerId == currentUserId) {
                    popupMenu.menu.findItem(R.id.action_league_settings).isVisible = true
                }

                // Handle menu item clicks
                popupMenu.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.action_league_info -> {
                            // Navigate to League Info fragment (you can implement this)
                            true
                        }

                        R.id.action_invite_friends -> {
                            // Navigate to Invite Friends fragment
                            val bundle = Bundle().apply {
                                putString("leagueId", leagueId)
                                putString("leagueName", leagueName) // Pass league name
                                putString("inviteCode", inviteCode)
                            }
                            findNavController().navigate(
                                R.id.action_leagueFragment_to_inviteFriendsFragment,
                                bundle
                            )
                            true
                        }

                        R.id.action_league_settings -> {
                            // Navigate to League Settings fragment and pass the league name
                            val bundle = Bundle().apply {
                                putString("leagueId", leagueId)
                                putString("leagueName", leagueName) // Pass league name
                            }
                            findNavController().navigate(
                                R.id.action_leagueFragment_to_leagueSettingsFragment,
                                bundle
                            )
                            true
                        }

                        R.id.action_league_chat -> {
                            // Navigate to League Chat fragment and pass the leagueId
                            val bundle = Bundle().apply {
                                putString("leagueId", leagueId)
                            }
                            findNavController().navigate(
                                R.id.action_leagueFragment_to_leagueChatFragment,
                                bundle
                            )
                            true
                        }

                        R.id.action_scoreboard

                        -> {
                            // Navigate to League Chat fragment and pass the leagueId
                            val bundle = Bundle().apply {
                                putString("leagueId", leagueId)
                            }
                            findNavController().navigate(
                                R.id.action_leagueFragment_to_scoreboardFragment,
                                bundle
                            )
                            true
                        }

                        else -> false
                    }
                }
            }

            popupMenu.show()

        }
    }



    private fun loadLeagueAndTeamData() {
        val currentUserId = auth.currentUser?.uid ?: return

        // Load the current team ID and team name
        firestore.collection("Leagues").document(leagueId).collection("Teams")
            .whereEqualTo("ownerID", currentUserId).limit(1).get()
            .addOnSuccessListener { teamsSnapshot ->
                if (!teamsSnapshot.isEmpty) {
                    val teamDoc = teamsSnapshot.documents[0]
                    val teamName = teamDoc.getString("teamName") ?: "Unknown Team"
                    teamId = teamDoc.id // Initialize teamId
                    teamNameTextView.text = teamName

                    // Once teamId is available, proceed to load league details
                    loadLeagueDetails()
                } else {
                    teamNameTextView.text = "No Team Found"
                    Log.e("LeagueFragment", "No team found for the current user in league $leagueId")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("LeagueFragment", "Error loading team data: ${exception.message}")
                Toast.makeText(requireContext(), "Failed to load team data. Please try again.", Toast.LENGTH_SHORT).show()
            }
    }

    // Function to load league details and optionally the team roster
    private fun loadLeagueDetails() {
        firestore.collection("Leagues").document(leagueId).get()
            .addOnSuccessListener { leagueDoc ->
                if (leagueDoc.exists()) {
                    val leagueName = leagueDoc.getString("leagueName") ?: "Unknown League"
                    val draftStatus = leagueDoc.getString("draftStatus") ?: "pending"
                    leagueNameTextView.text = leagueName

                    // Check if the draft is completed
                    if (draftStatus == "completed") {
                        recyclerView.visibility = View.VISIBLE
                        draftMessageTextView.visibility = View.GONE

                        // Ensure teamId is initialized before loading the roster
                        if (::teamId.isInitialized && teamId.isNotEmpty()) {
                            loadTeamRoster() // Load the roster
                        } else {
                            Log.e("LeagueFragment", "teamId is not initialized before loading roster.")
                            Toast.makeText(requireContext(), "Team ID not available. Unable to load roster.", Toast.LENGTH_SHORT).show()
                        }
                        return@addOnSuccessListener
                    } else {
                        recyclerView.visibility = View.GONE
                        draftMessageTextView.visibility = View.VISIBLE
                        draftMessageTextView.text = "The draft is not complete yet."
                    }

                    // Check if draftDateTime is set for countdown
                    val draftTimestamp = leagueDoc.getTimestamp("draftDateTime")
                    if (draftTimestamp == null) {
                        draftMessageTextView.visibility = View.VISIBLE
                        enterDraftRoomButton.visibility = View.VISIBLE
                        enterDraftRoomButton.isEnabled = false
                    } else {
                        draftMessageTextView.visibility = View.INVISIBLE
                        enterDraftRoomButton.visibility = View.VISIBLE

                        val draftDateTime = draftTimestamp.toDate()
                        startCountdown(draftDateTime)
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("LeagueFragment", "Error loading league data: ${exception.message}")
                Toast.makeText(requireContext(), "Failed to load league data. Please try again.", Toast.LENGTH_SHORT).show()
            }
    }


    private fun loadTeamRoster() {
        // TODO: Implement logic to load actual players on the team from Firestore when available
        Log.d("LeagueFragment", "Loading team roster data...")
        // Create a bundle to pass the data

        val bundle = Bundle().apply {
            putString("leagueID", leagueId)
            putString("teamID", teamId)
        }

// Create an instance of RosterFragment and pass the data
        findNavController().navigate(
            R.id.action_leagueFragment_to_rosterFragment,
            bundle
        )
    }




    private fun startCountdown(draftDateTime: Date) {
        val currentTime = System.currentTimeMillis()
        val draftTimeInMillis = draftDateTime.time
        val timeUntilDraft = draftTimeInMillis - currentTime

        // Show the countdown TextView
        draftCountdownTextView.visibility = View.VISIBLE

        countdownTimer = object : CountDownTimer(timeUntilDraft, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val days = TimeUnit.MILLISECONDS.toDays(millisUntilFinished)
                val hours = TimeUnit.MILLISECONDS.toHours(millisUntilFinished) % 24
                val minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) % 60
                val seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60

                val countdownText = if (days > 0) {
                    if (days == 1L) {
                        String.format("1 Day %02d:%02d:%02d", hours, minutes, seconds)
                    } else {
                        String.format("%d Days %02d:%02d:%02d", days, hours, minutes, seconds)
                    }
                } else {
                    String.format("%02d:%02d:%02d", hours, minutes, seconds)
                }

                draftCountdownTextView.text = "Draft in: $countdownText"

                if (millisUntilFinished <= TimeUnit.MINUTES.toMillis(30)) {
                    enterDraftRoomButton.isEnabled = true
                }
            }

            override fun onFinish() {
                draftCountdownTextView.text = "Draft starting now!"
                enterDraftRoomButton.isEnabled = true
            }
        }.start()
    }
    private fun navigateToTeamInfoFragment() {
        val bundle = Bundle().apply {
            putString("leagueId", leagueId)
            putString("teamId", teamId)
        }

        // Navigate to TeamInfoFragment and pass leagueId and teamId
        findNavController().navigate(R.id.action_leagueFragment_to_teamInfoFragment, bundle)
    }

    override fun onResume() {
        super.onResume()
        // Make sure global navigation bar is visible when in LeagueFragment
        (activity as? MainActivity)?.showBottomNavigation()
    }
}