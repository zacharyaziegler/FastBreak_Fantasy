package com.example.fantasy_basketball

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.firestore.v1.StructuredQuery.Projection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class PlayerInfoFragment : Fragment() {

    private lateinit var player: Player

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_player_info, container, false)

        // Initialize views
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

        // Initialize projection views
        val playerPointsProj = view.findViewById<TextView>(R.id.playerPtsProj)
        val playerReboundsProj = view.findViewById<TextView>(R.id.playerRebProj)
        val playerAssistsProj = view.findViewById<TextView>(R.id.playerAstProj)
        val playerStealsProj = view.findViewById<TextView>(R.id.playerStlProj)
        val playerBlocksProj = view.findViewById<TextView>(R.id.playerBlkProj)
        val playerTurnoversProj = view.findViewById<TextView>(R.id.playerTovProj)
        val playerFantasyPointsProj = view.findViewById<TextView>(R.id.playerFantasyPointsProj)

        // Retrieve the Player object from arguments
        player = arguments?.getParcelable("selectedPlayer") ?: return view

        // Populate views with player data
        populatePlayerInfo(view, player, playerName, playerTeam, playerPosition, playerPoints,
            playerRebounds, playerAssists, playerSteals, playerBlocks, playerTurnovers,
            playerStatus, playerDesc, playerImage)

        // Display projections from Player object
        displayProjections(player.projection, playerPointsProj, playerReboundsProj,
            playerAssistsProj, playerStealsProj, playerBlocksProj,
            playerTurnoversProj, playerFantasyPointsProj)

        // Fetch and display projections
        /*fetchPlayerProjections(player.playerID) { projections ->
            displayProjections(projections, playerPointsProj, playerReboundsProj,
                playerAssistsProj, playerStealsProj, playerBlocksProj,
                playerTurnoversProj, playerFantasyPointsProj)
        }*/


        // Back button listener
        backButton.setOnClickListener {
            findNavController().popBackStack()
        }

        return view
    }

    private fun populatePlayerInfo(
        view: View,
        player: Player,
        playerName: TextView,
        playerTeam: TextView,
        playerPosition: TextView,
        playerPoints: TextView,
        playerRebounds: TextView,
        playerAssists: TextView,
        playerSteals: TextView,
        playerBlocks: TextView,
        playerTurnovers: TextView,
        playerStatus: TextView,
        playerDesc: TextView,
        playerImage: ImageView
    ) {
        playerName.text = player.longName
        playerTeam.text = "Team: ${player.team ?: "N/A"}"
        playerPosition.text = "Position: ${player.pos ?: "N/A"}"
        playerPoints.text = player.stats?.pts ?: "N/A"
        playerRebounds.text = player.stats?.reb ?: "N/A"
        playerAssists.text = player.stats?.ast ?: "N/A"
        playerSteals.text = player.stats?.stl ?: "N/A"
        playerBlocks.text = player.stats?.blk ?: "N/A"
        playerTurnovers.text = player.stats?.TOV ?: "N/A"
        playerStatus.text = player.injury?.designation ?: "N/A"
        playerDesc.text = player.injury?.description ?: "N/A"


        if (!player.nbaComHeadshot.isNullOrEmpty()) {
            Glide.with(this).load(player.nbaComHeadshot).into(playerImage)
        } else {
            playerImage.setImageResource(R.drawable.player)
        }

    }

    private fun displayProjections(
        projection: PlayerProjection?,
        playerPointsProj: TextView,
        playerReboundsProj: TextView,
        playerAssistsProj: TextView,
        playerStealsProj: TextView,
        playerBlocksProj: TextView,
        playerTurnoversProj: TextView,
        playerFantasyPointsProj: TextView
    ) {
        if (projection != null) {
            playerPointsProj.text = projection.pts
            playerReboundsProj.text = projection.reb
            playerAssistsProj.text = projection.ast
            playerStealsProj.text = projection.stl
            playerBlocksProj.text = projection.blk
            playerTurnoversProj.text = projection.TOV
            playerFantasyPointsProj.text = projection.fantasyPoints
        } else {
            // Display N/A if the projection data is not available
            playerPointsProj.text = "N/A"
            playerReboundsProj.text = "N/A"
            playerAssistsProj.text = "N/A"
            playerStealsProj.text = "N/A"
            playerBlocksProj.text = "N/A"
            playerTurnoversProj.text = "N/A"
            playerFantasyPointsProj.text = "N/A"
        }
    }

    private fun fetchPlayerProjections(playerID: String, onProjectionsFetched: (JSONObject?) -> Unit) {
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
                            onProjectionsFetched(playerProjections.optJSONObject(playerID))
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun displayProjections(
        projections: JSONObject?,
        playerPointsProj: TextView,
        playerReboundsProj: TextView,
        playerAssistsProj: TextView,
        playerStealsProj: TextView,
        playerBlocksProj: TextView,
        playerTurnoversProj: TextView,
        playerFantasyPointsProj: TextView
    ) {
        playerPointsProj.text = projections?.optString("pts")?.takeIf { it.isNotEmpty() } ?: "--"
        playerReboundsProj.text = projections?.optString("reb")?.takeIf { it.isNotEmpty() } ?: "--"
        playerAssistsProj.text = projections?.optString("ast")?.takeIf { it.isNotEmpty() } ?: "--"
        playerStealsProj.text = projections?.optString("stl")?.takeIf { it.isNotEmpty() } ?: "--"
        playerBlocksProj.text = projections?.optString("blk")?.takeIf { it.isNotEmpty() } ?: "--"
        playerTurnoversProj.text = projections?.optString("TOV")?.takeIf { it.isNotEmpty() } ?: "--"
        playerFantasyPointsProj.text = projections?.optString("fantasyPoints")?.takeIf { it.isNotEmpty() } ?: "--"
    }

}
