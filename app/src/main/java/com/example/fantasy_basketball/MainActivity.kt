package com.example.fantasy_basketball


import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import android.util.Log
import android.widget.ImageView
import android.widget.PopupMenu
import androidx.appcompat.widget.Toolbar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
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
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        //scheduleWeeklyPlayerProjectionsWorker()

        //scheduleLeagueProcessing()
        //triggerOneTimeLeagueProcessing()

        //processLeagueMatchups("g11QJdRoaR7WhJIuya3A", "week01")


        // Use the helper class to check permissions and schedule WorkManager
        //WorkManagerHelper.checkAndRequestNotificationPermission(this)
        //WorkManagerHelper.scheduleWorkManager(this)
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
    }

    fun updateToolbarTitle() {
        when (activeFragment) {
            "LeagueFragment" -> supportActionBar?.title = sharedViewModel.leagueName
            "HomeFragment" -> supportActionBar?.title = getString(R.string.app_name)

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




/*
    fun triggerOneTimeLeagueProcessing() {
        val oneTimeWorkRequest = OneTimeWorkRequestBuilder<ProcessLeaguesWorker>().build()

        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            "OneTimeProcessLeaguesWork", // Unique name to prevent duplicate runs
            ExistingWorkPolicy.REPLACE, // Replace any existing work with the same name
            oneTimeWorkRequest
        )

        Log.d("MainActivity", "One-time ProcessLeaguesWorker triggered.")
    }

 */










    fun showBottomNavigation() {
        findViewById<BottomNavigationView>(R.id.bottom_navigation)?.visibility = View.VISIBLE
    }

    fun hideBottomNavigation() {
        findViewById<BottomNavigationView>(R.id.bottom_navigation)?.visibility = View.GONE
    }







}


