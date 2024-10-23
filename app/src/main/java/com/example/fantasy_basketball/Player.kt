package com.example.fantasy_basketball

import android.annotation.SuppressLint
import android.os.Parcel
import android.os.Parcelable
import kotlinx.parcelize.Parcelize


data class Player(
    val playerID: String,
    val longName: String,
    val points: String,
    val rebounds: String,
    val assists: String
) {
    // Enum class defined within Player to represent player status
//    enum class Status {
//        H,
//        Q,
//        IR
//    }
}

data class PlayerProjection(
    val playerID: String,
    val longName: String,
    val points: String,
    val rebounds: String,
    val assists: String,
    val steals: String,
    val blocks: String,
    val fantasyPoints: String
)


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

