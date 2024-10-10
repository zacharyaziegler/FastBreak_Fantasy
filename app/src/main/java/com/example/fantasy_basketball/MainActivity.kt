package com.example.fantasy_basketball

//import PlayerDataManager
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

import android.util.Log
import androidx.navigation.findNavController

import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
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


        val bottomNavigation: BottomNavigationView = findViewById(R.id.bottom_navigation)


        navController.navigate(R.id.playerProjectionsFragment)



        /*

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
                    navController.navigate(R.id.signupFragment)
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


}
         */

