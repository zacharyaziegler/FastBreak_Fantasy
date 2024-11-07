package com.example.fantasy_basketball

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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firestore.v1.StructuredQuery.Projection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class PlayerInfoFragment : Fragment() {

    private lateinit var player: Player
    private lateinit var pastGamesRecyclerView: RecyclerView
    private lateinit var pastGamesAdapter: PastGamesAdapter

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://tank01-fantasy-stats.p.rapidapi.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api: PlayerAPIService = retrofit.create(PlayerAPIService::class.java)

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

        pastGamesRecyclerView = view.findViewById(R.id.past_games_recycler_view)

        // Retrieve the Player object from arguments
        player = arguments?.getParcelable("selectedPlayer") ?: return view

        Log.d("PlayerInfoFragment", "Retrieved playerID: ${player.playerID}")
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


        fetchPastGamesData(player.playerID)

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


    private fun fetchPastGamesData(playerID: String) {
        Log.d("PlayerInfoFragment", "Fetching past games for playerID: $playerID")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = api.getNBAGamesForPlayer(playerID, "2025", "true")
                Log.d("PlayerInfoFragment", "Response Status Code: ${response.statusCode}")
                Log.d("PlayerInfoFragment", "Response Body: ${response.body}")

                if (response.statusCode == 200) {
                    val pastGames = response.body.values.toList()
                    if (pastGames.isEmpty()) {
                        Log.d("PlayerInfoFragment", "No past games available for this player.")
                    }

                    withContext(Dispatchers.Main) {
                        setupRecyclerView(pastGames)
                    }
                } else {
                    Log.e("PlayerInfoFragment", "Failed to fetch past games data: Status Code = ${response.statusCode}")
                }
            } catch (e: Exception) {
                Log.e("PlayerInfoFragment", "Exception during API call", e)
            }
        }
    }





    private fun setupRecyclerView(games: List<PlayerGameStatsBody>) {
        pastGamesAdapter = PastGamesAdapter(games)
        pastGamesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
            adapter = pastGamesAdapter
        }
    }


    class PastGamesAdapter(private val games: List<PlayerGameStatsBody>) : RecyclerView.Adapter<PastGamesAdapter.PastGameViewHolder>() {

        inner class PastGameViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val dateTextView: TextView = view.findViewById(R.id.date_text)
            val opponentTextView: TextView = view.findViewById(R.id.opponent_text)
            val pointsTextView: TextView = view.findViewById(R.id.pts_text)
            val reboundsTextView: TextView = view.findViewById(R.id.reb_text)
            val assistsTextView: TextView = view.findViewById(R.id.ast_text)
            val stealsTextView: TextView = view.findViewById(R.id.stl_text)
            val blocksTextView: TextView = view.findViewById(R.id.blk_text)
            val turnoversTextView: TextView = view.findViewById(R.id.tov_text)
            val fantasyPointsTextView: TextView = view.findViewById(R.id.fpts_text)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PastGameViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.player_game_log_item, parent, false)
            return PastGameViewHolder(view)
        }

        override fun onBindViewHolder(holder: PastGameViewHolder, position: Int) {
            val game = games[position]
            val (opponent, date) = game.extractOpponentAndDate()

            holder.dateTextView.text = date
            holder.opponentTextView.text = opponent
            holder.pointsTextView.text = game.pts
            holder.reboundsTextView.text = game.reb
            holder.assistsTextView.text = game.ast
            holder.stealsTextView.text = game.stl
            holder.blocksTextView.text = game.blk
            holder.turnoversTextView.text = game.TOV
            holder.fantasyPointsTextView.text = game.fantasyPoints
        }

        override fun getItemCount() = games.size
    }
}

