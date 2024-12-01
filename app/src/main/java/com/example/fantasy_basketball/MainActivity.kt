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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
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

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        //scheduleWeeklyPlayerProjectionsWorker()

        //scheduleLeagueProcessing()
        //triggerOneTimeLeagueProcessing()

        //processLeagueMatchups("9bMCw7gJRZrS0b9tQTGo", "week01")


        // Use the helper class to check permissions and schedule WorkManager
        //WorkManagerHelper.checkAndRequestNotificationPermission(this)
        //WorkManagerHelper.scheduleWorkManager(this)


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
        val navController = navHostFragment.navController



        val bottomNavigation: BottomNavigationView = findViewById(R.id.bottom_navigation)


        //navController.navigate(R.id.playerProjectionsFragment)

/*
        lifecycleScope.launch {
            playerDataManager.fetchAndStoreADP()
        }

 */

        // Check if the user is already signed in and navigate accordingly
        if (auth.currentUser != null) {
            // If the user is signed in, navigate to the home fragment
            bottomNavigation.visibility = View.VISIBLE
            navController.navigate(R.id.homeFragment)

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
                R.id.ic_home -> {
                    // Navigate to HomeFragment when home icon is selected
                    navController.navigate(R.id.homeFragment)
                    true
                }
                R.id.ic_setting -> {
                    // Change this to navigate to the settings fragment when create
                    navController.navigate(R.id.settingsFragment)
                    true
                }
                R.id.ic_Search -> {
                    navController.navigate(R.id.playerSearchFragment)
                    true
                }
                R.id.ic_info -> {
                    navController.navigate(R.id.rulesFragment)
                    true
                }
                else -> false
            }
        }


        // Add an authentication state listener to handle sign-in/sign-out events
        auth.addAuthStateListener { firebaseAuth ->
            if (firebaseAuth.currentUser != null) {
                // User is signed in
                bottomNavigation.visibility = View.VISIBLE
            } else {
                // User is not signed in
                bottomNavigation.visibility = View.GONE
            }
        }

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


