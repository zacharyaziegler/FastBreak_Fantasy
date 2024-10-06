package com.example.fantasy_basketball

//import PlayerDataManager
import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import com.example.fantasy_basketball.R
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        // Create an instance of PlayerDataManager
        //val playerDataManager = PlayerDataManager()

        // Launch a coroutine to fetch and store the players
//        CoroutineScope(Dispatchers.IO).launch {
//            println("Entered Coroutine")
//            playerDataManager.fetchAndStorePlayers()
//        }

        // Initialize FirebaseAuth
        auth = FirebaseAuth.getInstance()

        // Get the NavHostFragment and NavController
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        navController.navigate(R.id.playerStatsFragment)

        /*
        // Check if the user is already signed in and navigate accordingly
        if (auth.currentUser != null) {
            // If the user is signed in, navigate to the home fragment
            navController.navigate(R.id.homeFragment)
        } else {
            // If not signed in, stay on the login fragment
            navController.navigate(R.id.loginFragment)
        }

         */





    }
}
