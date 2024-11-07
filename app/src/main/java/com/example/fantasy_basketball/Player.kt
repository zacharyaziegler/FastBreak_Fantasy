package com.example.fantasy_basketball


import com.google.gson.annotations.SerializedName
import android.annotation.SuppressLint
import android.os.Parcel
import android.os.Parcelable
import kotlinx.parcelize.Parcelize


@Parcelize
data class Player(
    val playerID: String = "",                    // Unique identifier for the player
    val longName: String = "",                    // Full name of the player
    val jerseyNum: String = "",                   // Player's jersey number
    val pos: String = "",                         // Position of the player
    val team: String = "",                        // Team abbreviation
    val teamID: String = "",                      // Team ID
    val nbaComHeadshot: String = "",              // URL for the NBA.com headshot
    @SerializedName("Injury")                     // Ensure this matches the Firestore field exactly
    val injury: Injury? = null,                   // Nullable injury details
    @SerializedName("TotalStats")                 // Ensure this matches the Firestore field exactly
    val stats: PlayerStats? = null,               // Nullable player statistics
    @SerializedName("Projections")                // Ensure this matches the Firestore field exactly
    val projection: PlayerProjection? = null      // Nullable player projection
): Parcelable


// Update to match the stats provided in the response
@Parcelize
data class PlayerStats(
    val blk: String? = null,                      // Blocks per game
    val fga: String? = null,                      // Field Goals Attempted
    val DefReb: String? = null,                   // Defensive Rebounds
    val ast: String? = null,                      // Assists
    val ftp: String? = null,                      // Free Throw Percentage
    val tptfgp: String? = null,                   // Three Point Field Goal Percentage
    val tptfgm: String? = null,                   // Three Point Field Goals Made
    val stl: String? = null,                      // Steals
    val fgm: String? = null,                      // Field Goals Made
    val pts: String? = null,                      // Points per game
    val reb: String? = null,                      // Rebounds per game
    val fgp: String? = null,                      // Field Goal Percentage
    val fta: String? = null,                      // Free Throws Attempted
    val mins: String? = null,                     // Minutes played
    val trueShootingAttempts: String? = null,     // True Shooting Attempts
    val gamesPlayed: String? = null,              // Number of games played
    val TOV: String? = null,                      // Turnovers
    val tptfga: String? = null,                   // Three Point Field Goals Attempted
    val OffReb: String? = null,                   // Offensive Rebounds
    val ftm: String? = null                        // Free Throws Made
): Parcelable
@Parcelize
data class PlayerProjection(
    val blk: String = "",
    val mins: String = "",
    val ast: String = "",
    val pos: String = "",
    val teamID: String = "",
    val stl: String = "",
    val TOV: String = "",
    val team: String = "",
    val pts: String = "",
    val reb: String = "",
    val longName: String = "",      // Add longName to the projection
    val playerID: String = "",      // Add playerID to the projection
    val fantasyPoints: String = ""   // Add fantasyPoints to the projection
): Parcelable

// Update to match the injury details provided in the response
@Parcelize
data class Injury(
    val injReturnDate: String? = null,           // Return date from injury
    val description: String? = null,              // Injury description
    val injDate: String? = null,                  // Injury date
    val designation: String? = null                // Injury designation (e.g., Day-To-Day)
): Parcelable


data class PlayerDisplay(
    val playerID: String = "",
    val longName: String = "",
    val headshotUrl: String? = null,
    val team: String? = null,
    val position: String? = null,
    val points: String? = null,
    val rebounds: String? = null,
    val assists: String? = null,
    val steals: String? = null,
    val blocks: String? = null,
    val turnovers: String? = null,
    val injStatus: String? = null,
    val injDesc: String? = null,
    val fantasyPointsProj: String? = null,
    val pointsProj: String? = null,
    val reboundsProj: String? = null,
    val assistsProj: String? = null,
    val stealsProj: String? = null,
    val blocksProj: String? = null,
    val turnoversProj: String? = null,

)

data class PlayerFirestore(
    val playerID: String ,
    val longName: String ,
    val headshotUrl: String? = null ,
    val team: String? = null,
    val position: String? = null,
    val overallADP: String? = null,
    val posADP: String? = null,
    val fantasyPointsProj: String? = null,
    val pointsProj: String? = null,
    val reboundsProj: String? = null,
    val assistsProj: String? = null,
    val stealsProj: String? = null,
    val blocksProj: String? = null
)

data class PlayerGameLog(
    val date: String,
    val opponent: String,
    val pts: String,
    val reb: String,
    val ast: String,
    val stl: String,
    val blk: String,
    val tov: String,
    val fantasyPoints: String
)



