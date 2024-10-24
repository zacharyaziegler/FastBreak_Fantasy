package com.example.fantasy_basketball

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class PlayerInfoFragment : Fragment() {

   // private lateinit var playerName: TextView
   // private lateinit var playerPtsProj: TextView
   // private lateinit var playerRebProj: TextView
   // private lateinit var playerAstProj: TextView
    //private lateinit var playerFantasyPointsProj: TextView
   // private lateinit var backButton: Button


    private val playerProjections = mutableListOf<PlayerDisplay>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_player_info, container, false)


        val playerName = view.findViewById<TextView>(R.id.playerName)
        val playerTeam = view.findViewById<TextView>(R.id.playerTeam)
        val playerPosition = view.findViewById<TextView>(R.id.playerPosition)
        val playerPoints = view.findViewById<TextView>(R.id.playerPts)
        val playerRebounds = view.findViewById<TextView>(R.id.playerReb)
        val playerAssists = view.findViewById<TextView>(R.id.playerAst)
        val playerSteals = view.findViewById<TextView>(R.id.playerStl)
        val playerBlocks = view.findViewById<TextView>(R.id.playerBlk)
        val playerTurnovers = view.findViewById<TextView>(R.id.playerTov)
        val playerImage = view.findViewById<ImageView>(R.id.playerImage)
        val backButton = view.findViewById<Button>(R.id.backButton)
        val playerStatus = view.findViewById<TextView>(R.id.injStatus)
        val playerDesc = view.findViewById<TextView>(R.id.injDesc)


        val playerPointsProj = view.findViewById<TextView>(R.id.playerPtsProj)
        val playerReboundsProj = view.findViewById<TextView>(R.id.playerRebProj)
        val playerAssistsProj = view.findViewById<TextView>(R.id.playerAstProj)
        val playerStealsProj = view.findViewById<TextView>(R.id.playerStlProj)
        val playerBlocksProj = view.findViewById<TextView>(R.id.playerBlkProj)
        val playerTurnoversProj = view.findViewById<TextView>(R.id.playerTovProj)
        val playerFantasyPointsProj = view.findViewById<TextView>(R.id.playerFantasyPointsProj)


        val playerID = arguments?.getString("playerID")
        val longName = arguments?.getString("longName")
        val team = arguments?.getString("team")
        val position = arguments?.getString("position")
        val points = arguments?.getString("points")
        val rebounds = arguments?.getString("rebounds")
        val assists = arguments?.getString("assists")
        val steals = arguments?.getString("steals")
        val blocks = arguments?.getString("blocks")
        val turnovers = arguments?.getString("turnovers")
        val headshotUrl = arguments?.getString("headshotUrl")
        val injStatus = arguments?.getString("injuryStatus")
        val injDesc = arguments?.getString("injuryDescription")
        val pointsProj = arguments?.getString("pointsProj")
        val reboundsProj = arguments?.getString("reboundsProj")
        val assistsProj = arguments?.getString("assistsProj")
        val stealsProj = arguments?.getString("stealsProj")
        val blocksProj = arguments?.getString("blocksProj")
        val turnoversProj = arguments?.getString("turnoversProj")
        val fantasyPointsProj = arguments?.getString("fantasyPointsProj")


        playerName.text = longName
        playerTeam.text = "Team: ${team ?: "N/A"}"
        playerPosition.text = "Position: ${position ?: "N/A"}"
        playerPoints.text = points ?: "N/A"
        playerRebounds.text = rebounds ?: "N/A"
        playerAssists.text = assists ?: "N/A"
        playerSteals.text = steals ?: "N/A"
        playerBlocks.text = blocks ?: "N/A"
        playerTurnovers.text = turnovers ?: "N/A"
        playerStatus.text = injStatus ?: "N/A"
        playerDesc.text = injDesc ?: "N/A"
        playerPointsProj.text = pointsProj ?:"N/A"
        playerReboundsProj.text = reboundsProj ?:"N/A"
        playerAssistsProj.text = assistsProj ?:"N/A"
        playerStealsProj.text = stealsProj ?:"N/A"
        playerBlocksProj.text = blocksProj ?:"N/A"
        playerTurnoversProj.text = turnoversProj ?:"N/A"
        playerFantasyPointsProj.text = fantasyPointsProj ?:"N/A"





        if (!headshotUrl.isNullOrEmpty()) {
            Glide.with(this).load(headshotUrl).into(playerImage)
        } else {
            playerImage.setImageResource(R.drawable.player)
        }


        backButton.setOnClickListener {
            findNavController().popBackStack()
        }

/*
        playerID?.let { id ->
            fetchPlayerProjectionsFromAPI { projections ->

                val playerProjection = projections?.optJSONObject(id)


                playerProjection?.let { projection ->
                    playerPointsProj.text = projection.optString("pts", "N/A")
                    playerReboundsProj.text = projection.optString("reb", "N/A")
                    playerAssistsProj.text = projection.optString("ast", "N/A")
                    playerFantasyPointsProj.text = projection.optString("fantasyPoints", "N/A")


                    playerStealsProj.text = projection.optString("stl", "N/A")
                    playerBlocksProj.text = projection.optString("blk", "N/A")
                    playerTurnoversProj.text = projection.optString("TOV", "N/A")
                } ?: run {

                    playerPointsProj.text = "N/A"
                    playerReboundsProj.text = "N/A"
                    playerAssistsProj.text = "N/A"
                    playerFantasyPointsProj.text = "N/A"
                    playerStealsProj.text = "N/A"
                    playerBlocksProj.text = "N/A"
                    playerTurnoversProj.text = "N/A"
                }
            }
        }

 */

        return view
    }


    private fun fetchPlayerProjectionsFromAPI(onProjectionsFetched: (JSONObject?) -> Unit) {
        val client = OkHttpClient()
        val url = "https://tank01-fantasy-stats.p.rapidapi.com/getNBAProjections?numOfDays=7&pts=1&reb=1.25&ast=1.5&stl=3&blk=3&TOV=-1"

        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader("x-rapidapi-key", "a0ac93dc3amsh37d315dd4ab6990p119d93jsn2d0bf27cf642")
            .addHeader("x-rapidapi-host", "tank01-fantasy-stats.p.rapidapi.com")
            .build()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string() ?: return@use
                        val jsonObject = JSONObject(responseBody)
                        val playerProjections = jsonObject.getJSONObject("body").getJSONObject("playerProjections")

                        withContext(Dispatchers.Main) {

                            onProjectionsFetched(playerProjections)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

