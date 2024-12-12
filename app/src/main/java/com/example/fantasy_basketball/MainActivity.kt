package com.example.fantasy_basketball


import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ContentValues.TAG
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Tasks
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.initialize
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var activeFragment: String? = null
    private val sharedViewModel: SharedDataViewModel by viewModels()
    private lateinit var navController : NavController
    private lateinit var bottomNavigation: BottomNavigationView
    private var isTradeListenerActive = false //
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        //scheduleWeeklyPlayerProjectionsWorker()

        //scheduleLeagueProcessing()
//        triggerOneTimeLeagueProcessing()

        //processLeagueMatchups("g11QJdRoaR7WhJIuya3A", "week01")


        // Use the helper class to check permissions and schedule WorkManager
        //WorkManagerHelper.checkAndRequestNotificationPermission(this)
        //WorkManagerHelper.scheduleWorkManager(this)

        // Initialize Firebase
                FirebaseApp.initializeApp(this)

        // Set up Firebase App Check with Play Integrity
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance()
        )
        firestore = FirebaseFirestore.getInstance()

        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { isGranted: Boolean ->
            if (isGranted) {
                FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        Log.w("Firebaselogs", "Fetching FCM registration token failed", task.exception)
                        return@OnCompleteListener
                    }

                    // Get new FCM registration token
                    val token = task.result

                })
            } else {
            }
        }

        fun askNotificationPermission() {
            // This is only necessary for API level >= 33 (TIRAMISU)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this,android.Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
                ) {

                } else if (shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS)) {

                } else {
                    requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }


////         Create an instance of PlayerDataManager
//        val playerDataManager = PlayerDataManager()
//
////         Launch a coroutine to fetch and store the players
//        CoroutineScope(Dispatchers.IO).launch {
//           println("Entered Coroutine")
//
//            playerDataManager.fetchAndStorePlayersFromTeam()
//            playerDataManager.fetchAndStorePlayerProjections()
//      }

        // Initialize FirebaseAuth

        /*

        FirebaseApp.initializeApp(this)

        val appCheck = FirebaseAppCheck.getInstance()
        appCheck.installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance()
        )

         */




        auth = FirebaseAuth.getInstance()

        // Get the NavHostFragment and NavController
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController



        bottomNavigation= findViewById(R.id.bottom_navigation)


       // navController.navigate(R.id.scoreboardFragment)

/*
        lifecycleScope.launch {
            playerDataManager.fetchAndStoreADP()
        }

 */

        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)

        // Check if the user is already signed in and navigate accordingly
        if (auth.currentUser != null) {
            // If the user is signed in, navigate to the home fragment
            bottomNavigation.visibility = View.VISIBLE

            navController.navigate(R.id.homeFragment)

            updateBottomNavigationVisibility()
        } else {
            // If not signed in, stay on the login fragment
            bottomNavigation.visibility = View.GONE

            navController.navigate(R.id.loginFragment)

        }






            //

           // navController.navigate(R.id.loginFragment)


        // Set the item selection listener for the BottomNavigationView
        bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.ic_Roster -> {
                    // Navigate to HomeFragment when home icon is selected
                    navController.navigate(R.id.rosterFragment)
                    true
                }
                R.id.ic_Match -> {
                    // Change this to navigate to the settings fragment when create
                    navController.navigate(R.id.scoreboardFragment)
                    true
                }
                R.id.ic_Search -> {
                    navController.navigate(R.id.playerSearchFragment)
                    true
                }
                R.id.ic_Chat -> {
                    navController.navigate(R.id.leagueChatFragment)
                    true
                }
                R.id.ic_standing -> {
                    navController.navigate(R.id.leagueStandingsFragment)
                    true
                }

                else -> false
            }
        }


        val toolbar: Toolbar = findViewById(R.id.league_toolbar1)
        setSupportActionBar(toolbar)
        //supportActionBar?.title = "League Name"
        // Monitor fragment changes
        supportFragmentManager.addOnBackStackChangedListener {
            updateToolbarNavigation()
        }

        val hamburgerIcon = toolbar.findViewById<ImageView>(R.id.hamburgerIcon)
        hamburgerIcon.setOnClickListener {
            showPopupMenu(it)
        }

        // Add an authentication state listener to handle sign-in/sign-out events
        auth.addAuthStateListener { firebaseAuth ->
            if (firebaseAuth.currentUser != null) {
                // User is signed in
                updateBottomNavigationVisibility()
            } else {
                // User is not signed in
                bottomNavigation.visibility = View.GONE
            }
        }



    }

    private fun updateToolbarNavigation() {
        val toolbar = findViewById<Toolbar>(R.id.league_toolbar1)

        when (activeFragment) {
            "HomeFragment" -> {
                toolbar.navigationIcon = null // Hide back button
            }
            else -> {
                toolbar.setNavigationIcon(R.drawable.baseline_arrow_back_ios_24) // Show back button
                toolbar.setNavigationOnClickListener {
                    //onBackPressed() // Handle back navigation
                    navController.navigate(R.id.homeFragment)
                }
            }
        }
    }



    fun setActiveFragment(fragmentTag: String) {
        activeFragment = fragmentTag
        invalidateOptionsMenu() // Forces menu to refresh
        updateToolbarNavigation()
        updateBottomNavigationVisibility()
        updateToolbarTitle()
        updateTradeListenerVisibility()
    }

    fun updateToolbarTitle() {
        when (activeFragment) {
            "LeagueFragment" -> supportActionBar?.title = sharedViewModel.leagueName
            "HomeFragment" -> supportActionBar?.title = getString(R.string.app_name)

        }
    }
    fun updateTradeListenerVisibility() {
        // Check if the current active fragment is one of the relevant ones

        if (activeFragment == "LeagueFragment" || activeFragment == "RosterFragment" ||
            activeFragment == "MatchupFragment" || activeFragment == "PlayerSearchFragment") {
            val leagueID = sharedViewModel.leagueID
            val teamID = sharedViewModel.teamID
            // Start the trade listener when relevant fragment is active
            if (leagueID != null && teamID != null ) {

                listenForTradeOffers(leagueID, teamID)

            }
        }

    }



    fun updateBottomNavigationVisibility() {
        if (activeFragment == "HomeFragment" || activeFragment == "RulesFragment" || activeFragment == "SettingsFragment"||activeFragment =="LeagueFragment") {
            bottomNavigation.visibility = View.GONE // Hide bottom navigation for HomeFragment, RulesFragment, and SettingsFragment

        } else {
            bottomNavigation.visibility = View.VISIBLE // Show bottom navigation for other fragments
        }
    }

    private fun showPopupMenu(view: View) {
        val popupMenu = PopupMenu(this, view)
        popupMenu.menuInflater.inflate(R.menu.league_menu, popupMenu.menu)

        // Default: Hide all menu items
        popupMenu.menu.findItem(R.id.action_team_info)?.isVisible = false
        popupMenu.menu.findItem(R.id.action_invite_friends)?.isVisible = false
        popupMenu.menu.findItem(R.id.action_league_chat)?.isVisible = false
        popupMenu.menu.findItem(R.id.action_scoreboard)?.isVisible = false
        popupMenu.menu.findItem(R.id.action_league_settings)?.isVisible = false
        popupMenu.menu.findItem(R.id.action_home_Setting)?.isVisible = false
        popupMenu.menu.findItem(R.id.action_rules)?.isVisible = false

        // Adjust menu visibility based on the current fragment
        when (activeFragment) {
            "HomeFragment" -> {
                popupMenu.menu.findItem(R.id.action_home_Setting)?.isVisible = true
                popupMenu.menu.findItem(R.id.action_rules)?.isVisible = true
            }
            "LeagueFragment", "RosterFragment" -> {
                val currentUserId = auth.currentUser?.uid ?: return

                // Check if the user is the commissioner
                sharedViewModel.leagueID?.let { leagueID ->
                    firestore.collection("Leagues").document(leagueID).get()
                        .addOnSuccessListener { leagueDoc ->
                            if (leagueDoc.exists()) {
                                val commissionerId = leagueDoc.getString("commissionerID")

                                // Make "League Settings" visible only if the current user is the commissioner
                                if (commissionerId == currentUserId) {
                                    popupMenu.menu.findItem(R.id.action_league_settings)?.isVisible = true
                                }
                            }
                        }
                }

                // These menu items are always visible in LeagueFragment and RosterFragment
                popupMenu.menu.findItem(R.id.action_team_info)?.isVisible = true
                popupMenu.menu.findItem(R.id.action_invite_friends)?.isVisible = true
                //popupMenu.menu.findItem(R.id.action_rules)?.isVisible = true
              //  popupMenu.menu.findItem(R.id.action_league_chat)?.isVisible = true
               // popupMenu.menu.findItem(R.id.action_scoreboard)?.isVisible = true
            }
        }


        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_home_Setting -> {
                    // Handle Home Settings click
                    // Change this to navigate to the settings fragment when create
                    navController.navigate(R.id.settingsFragment)
                    true
                }
                R.id.action_rules -> {
                    navController.navigate(R.id.rulesFragment)
                    true
                }
                R.id.action_team_info -> {
                    // Handle League Info click
                    navController.navigate(R.id.teamInfoFragment)
                    true
                }
                R.id.action_invite_friends -> {
                    // Handle Invite Friends click
                    navController.navigate(R.id.inviteFriendsFragment)
                    true
                }
                R.id.action_league_chat -> {
                    // Handle League Chat click
                    true
                }
                R.id.action_scoreboard -> {
                    // Handle League Scoreboard click
                    true
                }
                R.id.action_league_settings -> {
                    // Handle League Settings click
                    navController.navigate(R.id.leagueSettingsFragment)
                    true
                }
                else -> false
            }
        }

        popupMenu.show()
    }



    /*

        private fun scheduleWeeklyPlayerProjectionsWorker() {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)  // Requires the device to be connected to the internet
                .build()

            // Create a periodic work request with constraints
            val playerProjectionsWorkRequest = PeriodicWorkRequestBuilder<PlayerProjectionsWorker>(
                7, TimeUnit.DAYS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "PlayerProjectionsWork",
                ExistingPeriodicWorkPolicy.KEEP,
                playerProjectionsWorkRequest
            )
        }

     */


/*
    fun scheduleLeagueProcessing() {
        Log.d("MainActivity", "Scheduling league processing triggered")

        val workRequest = PeriodicWorkRequestBuilder<ProcessLeaguesWorker>(7, TimeUnit.DAYS)
            .setInitialDelay(7, TimeUnit.DAYS)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "ProcessLeaguesWork",
            androidx.work.ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }

 */





    fun triggerOneTimeLeagueProcessing() {
        val oneTimeWorkRequest = OneTimeWorkRequestBuilder<ProcessLeaguesWorker>().build()

        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            "OneTimeProcessLeaguesWork", // Unique name to prevent duplicate runs
            ExistingWorkPolicy.REPLACE, // Replace any existing work with the same name
            oneTimeWorkRequest
        )

        Log.d("MainActivity", "One-time ProcessLeaguesWorker triggered.")
    }



    // Listen for trade offers
    private fun listenForTradeOffers(leagueID: String, teamID: String) {
        val db = FirebaseFirestore.getInstance()

        db.collection("Leagues")
            .document(leagueID)
            .collection("Trades")
            .whereEqualTo("currentTeamID", teamID) // Filter by the user's team
            .whereEqualTo("status", "Pending") // Only listen for pending trade offers
            .addSnapshotListener { tradesSnapshot, error ->
                if (error != null) {
                    Log.e("TradeListener", "Error fetching trades: ", error)
                    return@addSnapshotListener
                }

                tradesSnapshot?.documents?.forEach { tradeDoc ->
                    val offeredPlayers = tradeDoc.get("offeredPlayers") as? List<String> ?: emptyList()
                    val playerToTradeFor = tradeDoc.getString("playerToTradeFor")

                    if (!playerToTradeFor.isNullOrBlank()) {
                        fetchPlayerDetailsAndShowDialog(tradeDoc, playerToTradeFor, offeredPlayers)
                    }
                }
            }
    }

    private fun fetchPlayerDetailsAndShowDialog(
        tradeDoc: DocumentSnapshot,
        playerToTradeFor: String,
        offeredPlayers: List<String>
    ) {
        val db = FirebaseFirestore.getInstance()

        // Reference to the player the user wants to trade for
        val playerToTradeForRef = db.collection("players").document(playerToTradeFor)
        // Fetch offered players' details
        val offeredPlayersFetches = offeredPlayers.map { playerID ->
            db.collection("players").document(playerID).get()
        }

        // Fetch the player to trade for details
        playerToTradeForRef.get().addOnSuccessListener { playerDoc ->
            val playerToTradeForName = playerDoc.getString("longName") ?: "Unknown Player"

            // Wait for all offered players to be fetched
            Tasks.whenAllSuccess<DocumentSnapshot>(offeredPlayersFetches).addOnSuccessListener { results ->
                val offeredPlayerNames = results.map { it.getString("longName") ?: "Unknown Player" }

                // Map the results to Player objects
                val offeredPlayers = results.mapNotNull { createPlayerFromDocument(it) }

                // Pass tradeDoc, playerToTradeFor, and offeredPlayers to the dialog
                showTradeDialog(createPlayerFromDocument(playerDoc), offeredPlayers, tradeDoc)
            }.addOnFailureListener {
                Log.e("TradeListener", "Failed to fetch offered players.")
            }
        }.addOnFailureListener {
            Log.e("TradeListener", "Failed to fetch player to trade for.")
        }
    }


    private fun showTradeDialog(playerToTradeFor: Player?, offeredPlayers: List<Player>, tradeDoc: DocumentSnapshot) {
        // Inflate dialog view
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_player_list, null)

        // Create the dialog
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Accept") { _, _ ->
                playerToTradeFor?.let {
                    acceptTrade(tradeDoc)

                }
            }
            .setNegativeButton("Reject") { _, _ ->
                playerToTradeFor?.let {
                    rejectTrade(tradeDoc)

                }
            }
            .create()

        // Set up RecyclerView for "Player to Trade For"
        val playerToTradeForRecyclerView = dialogView.findViewById<RecyclerView>(R.id.playerToTradeForRecyclerView)
        playerToTradeForRecyclerView.layoutManager = LinearLayoutManager(this)
        val playerToTradeForAdapter = PlayerAdapter(mutableListOf()) {}
        playerToTradeForRecyclerView.adapter = playerToTradeForAdapter

        // Set up RecyclerView for "Offered Players"
        val offeredPlayersRecyclerView = dialogView.findViewById<RecyclerView>(R.id.offeredPlayersRecyclerView)
        offeredPlayersRecyclerView.layoutManager = LinearLayoutManager(this)
        val offeredPlayersAdapter = PlayerAdapter(mutableListOf()) {}
        offeredPlayersRecyclerView.adapter = offeredPlayersAdapter

        // Populate the adapters
        playerToTradeFor?.let {
            playerToTradeForAdapter.updateList(mutableListOf(it))
        }
        offeredPlayersAdapter.updateList(offeredPlayers.toMutableList())

        // Show the dialog
        dialog.show()
    }



    private fun acceptTrade(tradeDoc: DocumentSnapshot) {
        val tradeID = tradeDoc.id
        val leagueID = sharedViewModel.leagueID ?: return
        val db = FirebaseFirestore.getInstance()


        // Extract trade details
        val playerToTradeFor = tradeDoc.getString("playerToTradeFor") ?: return
        val offeredPlayers = tradeDoc.get("offeredPlayers") as? List<String> ?: emptyList()
        val currentTeamID = tradeDoc.getString("currentTeamID") ?: return
        val offeringTeamID = tradeDoc.getString("offeredBy") ?: return

        val currentTeamRef = db.collection("Leagues")
            .document(leagueID)
            .collection("Teams")
            .document(currentTeamID)

        val offeringTeamRef = db.collection("Leagues")
            .document(leagueID)
            .collection("Teams")
            .document(offeringTeamID)

        // Fetch rosters for both teams
        offeringTeamRef.get().addOnSuccessListener { offeringTeamDoc ->
            val offeringRoster = offeringTeamDoc.get("roster") as? List<String> ?: listOf()

            // Validate that all offered players are in the offering team's roster
            val invalidPlayers = offeredPlayers.filter { !offeringRoster.contains(it) }
            if (invalidPlayers.isNotEmpty()) {
                // Reject the trade if validation fails
                rejectTrade(tradeDoc)
                Toast.makeText(this, "Invalid trade. Offered players not found in offering team.", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }

            currentTeamRef.get().addOnSuccessListener { currentTeamDoc ->
                val currentRoster = currentTeamDoc.get("roster") as? List<String> ?: listOf()

                // Perform the trade operations
                if (currentRoster.contains(playerToTradeFor)) {
                    dropPlayer(playerToTradeFor, currentTeamID) // Drop the player from the current team
                } else {
                    Toast.makeText(this, "Player to trade for not found in current team roster.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                offeredPlayers.forEach { playerID ->
                    dropPlayer(playerID, offeringTeamID) // Drop offered players from the offering team
                }

                addPlayer(playerToTradeFor, offeringTeamID) // Add the player to trade for to the offering team

                offeredPlayers.forEach { playerID ->
                    addPlayer(playerID, currentTeamID) // Add offered players to the current team
                }

                // Update trade status to "Accepted"
                db.collection("Leagues")
                    .document(leagueID)
                    .collection("Trades")
                    .document(tradeID)
                    .update("status", "Accepted")
                    .addOnSuccessListener {
                        Toast.makeText(this, "Trade accepted!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to update trade status.", Toast.LENGTH_SHORT).show()
                    }
            }.addOnFailureListener {
                Toast.makeText(this, "Failed to fetch current team roster.", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to fetch offering team roster.", Toast.LENGTH_SHORT).show()
        }
    }


    // Reject the trade and update its status
    private fun rejectTrade(tradeDoc: DocumentSnapshot) {
        val tradeID = tradeDoc.id
        val leagueID = sharedViewModel.leagueID ?: return

        FirebaseFirestore.getInstance()
            .collection("Leagues")
            .document(leagueID)
            .collection("Trades")
            .document(tradeID)
            .update("status", "Rejected")
            .addOnSuccessListener {
                Toast.makeText(this, "Trade rejected.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to reject trade.", Toast.LENGTH_SHORT).show()
            }
    }


    // Create Player object from Firestore document
    private fun createPlayerFromDocument(doc: DocumentSnapshot): Player {
        return Player(
            playerID = doc.id,
            longName = doc.getString("longName") ?: "Unknown",
            pos = doc.getString("position") ?: "UTIL",
            projection = doc.get("Projections")?.let { projections ->
                val projectionsMap = projections as Map<*, *>
                PlayerProjection(
                    fantasyPoints = projectionsMap["fantasyPoints"]?.toString() ?: "0.0",
                    pts = projectionsMap["pts"]?.toString() ?: "0.0",
                    reb = projectionsMap["reb"]?.toString() ?: "0.0",
                    ast = projectionsMap["ast"]?.toString() ?: "0.0",
                    stl = projectionsMap["stl"]?.toString() ?: "0.0",
                    blk = projectionsMap["blk"]?.toString() ?: "0.0",
                    TOV = projectionsMap["TOV"]?.toString() ?: "0.0"
                )
            },
            injury = doc.get("Injury")?.let { injury ->
                val injuryMap = injury as Map<*, *>
                Injury(
                    status = injuryMap["status"]?.toString(),
                    description = injuryMap["description"]?.toString()
                )
            },
            stats = doc.get("TotalStats")?.let { stats ->
                val statsMap = stats as Map<*, *>
                PlayerStats(
                    pts = statsMap["pts"]?.toString() ?: "0.0",
                    reb = statsMap["reb"]?.toString() ?: "0.0",
                    ast = statsMap["ast"]?.toString() ?: "0.0",
                    stl = statsMap["stl"]?.toString() ?: "0.0",
                    blk = statsMap["blk"]?.toString() ?: "0.0",
                    TOV = statsMap["TOV"]?.toString() ?: "0.0"
                )
            },
            team = doc.getString("team") ?: "",
            nbaComHeadshot = doc.getString("nbaComHeadshot") ?: ""
        )
    }


    public fun addPlayer(playerID: String,teamID: String ) {
        val leagueID = sharedViewModel.leagueID


        if (leagueID != null && teamID != null) {
            val db = FirebaseFirestore.getInstance()

            val teamRef = db.collection("Leagues")
                .document(leagueID)
                .collection("Teams")
                .document(teamID)

            val draftedPlayersRef = db.collection("Leagues")
                .document(leagueID)
                .collection("draftedPlayers")

            // Fetch the current roster size
            teamRef.get()
                .addOnSuccessListener { document ->
                    if (document != null && document.contains("roster")) {
                        val roster = document.get("roster") as? List<*>
                        if (roster != null && roster.size >= 13) {
                            // Show a toast if the roster is full
                            Toast.makeText(this, "Roster full: 13-player limit reached", Toast.LENGTH_SHORT).show()
                        } else {
                            // Add player to roster and Bench
                            teamRef.update(
                                mapOf(
                                    "roster" to FieldValue.arrayUnion(playerID),
                                    "Bench" to FieldValue.arrayUnion(playerID)
                                )
                            ).addOnSuccessListener {
                                // Add the player to the draftedPlayers collection
                                val draftedPlayerData = mapOf(
                                    "teamID" to teamID // Add any other necessary fields
                                )

                                draftedPlayersRef.document(playerID)
                                    .set(draftedPlayerData)
                                    .addOnSuccessListener {
                                        Toast.makeText(this, "Player added to Roster", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(this, "Failed to add player to draftedPlayers", Toast.LENGTH_SHORT).show()
                                    }
                            }.addOnFailureListener {
                                Toast.makeText(this, "Failed to add player", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(this, "Roster data is unavailable.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to fetch roster data.", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "LeagueID or TeamID is null", Toast.LENGTH_SHORT).show()
        }
    }

    // Action to drop a player from the team
    public fun dropPlayer(playerID: String,teamID: String ) {
        val leagueID = sharedViewModel.leagueID


        if (leagueID != null && teamID != null) {
            val db = FirebaseFirestore.getInstance()

            // Reference to the team document
            val teamRef = db.collection("Leagues")
                .document(leagueID)
                .collection("Teams")
                .document(teamID)

            // Fetch the current team data
            teamRef.get().addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val startingPlayers = documentSnapshot.get("Starting") as? List<String> ?: emptyList()

                    // Check if the player is in the Starting lineup
                    if (startingPlayers.contains(playerID)) {
                        // Replace the player slot with an empty string
                        val updatedStarting = startingPlayers.map { if (it == playerID) "" else it }
                        teamRef.update("Starting", updatedStarting)
                            .addOnSuccessListener {
                                // Proceed with removing the player from other arrays
                                removePlayerFromTeam(teamRef, playerID)
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Failed to update Starting lineup", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        // If not in Starting, remove the player from other arrays directly
                        removePlayerFromTeam(teamRef, playerID)
                    }
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Failed to fetch team data", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "LeagueID or TeamID is null", Toast.LENGTH_SHORT).show()
        }
    }

    // Helper function to remove player from all relevant arrays
    private fun removePlayerFromTeam(teamRef: DocumentReference, playerID: String) {
        teamRef.update(
            mapOf(
                "roster" to FieldValue.arrayRemove(playerID),
                "Bench" to FieldValue.arrayRemove(playerID)
            )
        ).addOnSuccessListener {
            Toast.makeText(this, "Player dropped from team", Toast.LENGTH_SHORT).show()

            // Now remove from draftedPlayers in the Leagues collection
            removeFromDraftedPlayers(playerID)
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to drop player from team", Toast.LENGTH_SHORT).show()
        }
    }

    private fun removeFromDraftedPlayers(playerID: String) {
        val leagueID = sharedViewModel.leagueID ?: return
        val db = FirebaseFirestore.getInstance()

        // Reference to the draftedPlayers document using playerID
        val draftedPlayerRef = db.collection("Leagues")
            .document(leagueID)
            .collection("draftedPlayers")
            .document(playerID)  // Directly reference the player document by ID

        // Delete the player document
        draftedPlayerRef.delete()
            .addOnSuccessListener {
                //   Toast.makeText(context, "Player removed from drafted players", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to remove player from drafted players", Toast.LENGTH_SHORT).show()
            }
    }





    fun showBottomNavigation() {
        findViewById<BottomNavigationView>(R.id.bottom_navigation)?.visibility = View.VISIBLE
    }

    fun hideBottomNavigation() {
        findViewById<BottomNavigationView>(R.id.bottom_navigation)?.visibility = View.GONE
    }







}


