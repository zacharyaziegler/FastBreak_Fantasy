package com.example.fantasy_basketball

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.fantasy_basketball.PlayerStatsFragment.PlayerAdapter
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

class PlayerListFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var playerAdapter: PlayerAdapter
    private lateinit var playerNameInput: EditText
    private lateinit var searchButton: Button
    private lateinit var resetButton: Button

    private val playerList = mutableListOf<PlayerDisplay>()
    private var isLoading = false
    private var lastVisibleDocument: DocumentSnapshot? = null
    private var isSearching = false
    private val pageSize = 10


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_players_list, container, false)

        db = FirebaseFirestore.getInstance()

        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        playerAdapter = PlayerAdapter(playerList) { player ->
            val bundle = Bundle().apply {
                putString("playerID", player.playerID)
                putString("longName", player.longName)
                putString("team", player.team)
                putString("position", player.position)
                putString("points", player.points)
                putString("rebounds", player.rebounds)
                putString("assists", player.assists)
                putString("steals", player.steals)
                putString("blocks", player.blocks)
                putString("turnovers", player.turnovers)
                putString("headshotUrl", player.headshotUrl)
                putString("injuryStatus", player.injStatus)
                putString("injuryDescription", player.injDesc)
                putString("fantasyPointsProj", player.fantasyPointsProj)
                putString("pointsProj", player.pointsProj)
                putString("reboundsProj", player.reboundsProj)
                putString("assistsProj", player.assistsProj)
                putString("stealsProj", player.stealsProj)
                putString("blocksProj", player.blocksProj)
                putString("turnoversProj", player.turnoversProj)
            }
            findNavController().navigate(R.id.playerInfoFragment, bundle)
        }
        recyclerView.adapter = playerAdapter

        searchButton = view.findViewById(R.id.searchButton)
        resetButton = view.findViewById(R.id.resetButton)
        playerNameInput = view.findViewById(R.id.playerNameInput)

        searchButton.setOnClickListener {
            val playerName = playerNameInput.text.toString().trim()
            if (playerName.isNotEmpty()) {
                isSearching = true
                CoroutineScope(Dispatchers.Main).launch {
                    fetchPlayerStatsByName(playerName)
                }
            } else {
                Toast.makeText(requireContext(), "Please enter a player name", Toast.LENGTH_SHORT).show()
            }
        }

        resetButton.setOnClickListener {
            resetPlayerList()
        }

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                if (!isLoading && !isSearching && layoutManager.findLastVisibleItemPosition() == playerList.size - 1) {
                    CoroutineScope(Dispatchers.Main).launch {
                        fetchPlayerIDsWithPagination()
                    }
                }
            }
        })

        CoroutineScope(Dispatchers.Main).launch {
            fetchPlayerIDsWithPagination()
        }
        return view
    }

    private suspend fun fetchPlayerStatsByName(playerName: String) {
        val client = OkHttpClient()

        val url = "https://tank01-fantasy-stats.p.rapidapi.com/getNBAPlayerInfo?playerName=$playerName&statsToGet=averages"

        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader("x-rapidapi-key", "a0ac93dc3amsh37d315dd4ab6990p119d93jsn2d0bf27cf642")
            .addHeader("x-rapidapi-host", "tank01-fantasy-stats.p.rapidapi.com")
            .build()

        try {
            val response = withContext(Dispatchers.IO) {
                client.newCall(request).execute()
            }
            if (!response.isSuccessful) throw IOException("Unexpected code $response")

            val responseBody = response.body?.string()
            responseBody?.let {
                val jsonObject = JSONObject(it)

                if (jsonObject.has("body")) {
                    val bodyArray = jsonObject.getJSONArray("body")

                    withContext(Dispatchers.Main) {
                        playerList.clear()
                    }

                    for (i in 0 until bodyArray.length()) {
                        val playerObject = bodyArray.getJSONObject(i)

                        val id = playerObject.getString("playerID")
                        val longName = playerObject.getString("longName")
                        val headshotUrl = playerObject.getString("nbaComHeadshot")
                        val team = playerObject.getString("team")
                        val position = playerObject.getString("pos")
                        val statsObject = playerObject.optJSONObject("stats") ?: JSONObject()

                        val points = statsObject.optString("pts", null)
                        val rebounds = statsObject.optString("reb", null)
                        val assists = statsObject.optString("ast", null)
                        val steals = statsObject.optString("stl", null)
                        val blocks = statsObject.optString("blk", null)
                        val turnovers = statsObject.optString("TOV", null)

                        // Fetch Firestore injury and projections data
                        fetchPlayerInjuryAndProjections(id) { injStatus, injDesc,  fantasyPointsProj,
                                                              pointsProj, reboundsProj, assistsProj,
                                                              stealsProj, blocksProj, turnoversProj->
                            CoroutineScope(Dispatchers.Main).launch {
                                val player = PlayerDisplay(
                                    id, longName, headshotUrl, team, position,
                                    points, rebounds, assists, steals, blocks, turnovers,
                                    injStatus, injDesc, fantasyPointsProj, pointsProj,
                                    reboundsProj, assistsProj, stealsProj, blocksProj, turnoversProj,
                                )

                                playerList.add(player)
                                playerAdapter.notifyItemInserted(playerList.size - 1)
                            }
                        }
                    }

                    withContext(Dispatchers.Main) {
                        playerAdapter.notifyDataSetChanged()
                    }
                } else {
                    Log.e("API Error", "No 'body' object found in response for name: $playerName")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("API Error", "Failed to fetch player data for playerName: $playerName")
        }
    }

    private fun resetPlayerList() {
        playerNameInput.text.clear()
        playerList.clear()
        isSearching = false
        lastVisibleDocument = null
        CoroutineScope(Dispatchers.Main).launch {
            fetchPlayerIDsWithPagination()
        }
        playerAdapter.notifyDataSetChanged()
    }

    private suspend fun fetchPlayerIDsWithPagination() {
        if (isLoading || isSearching) return

        isLoading = true

        val query = if (lastVisibleDocument == null) {
            db.collection("players").limit(pageSize.toLong())
        } else {
            db.collection("players").startAfter(lastVisibleDocument!!).limit(pageSize.toLong())
        }

        query.get().addOnSuccessListener { result ->
            if (result.isEmpty) {
                Log.d("Firestore", "No more players to load")
                isLoading = false
                return@addOnSuccessListener
            }

            lastVisibleDocument = result.documents[result.size() - 1]

            for (document in result) {
                val playerID = document.id
                CoroutineScope(Dispatchers.IO).launch {
                    getPlayerDataFromAPI(playerID)
                }
            }

            isLoading = false
        }.addOnFailureListener { exception ->
            exception.printStackTrace()
            isLoading = false
        }
    }

    private suspend fun getPlayerDataFromAPI(playerID: String) {
        val client = OkHttpClient()

        val url = "https://tank01-fantasy-stats.p.rapidapi.com/getNBAPlayerInfo?playerID=$playerID&statsToGet=averages"

        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader("x-rapidapi-key", "a0ac93dc3amsh37d315dd4ab6990p119d93jsn2d0bf27cf642")
            .addHeader("x-rapidapi-host", "tank01-fantasy-stats.p.rapidapi.com")
            .build()

        try {
            val response = withContext(Dispatchers.IO) {
                client.newCall(request).execute()
            }
            if (!response.isSuccessful) throw IOException("Unexpected code $response")

            val responseBody = response.body?.string()
            responseBody?.let {
                val jsonObject = JSONObject(it)

                if (jsonObject.has("body")) {
                    val bodyObject = jsonObject.getJSONObject("body")

                    val id = bodyObject.getString("playerID")
                    val longName = bodyObject.getString("longName")
                    val headshotUrl = bodyObject.getString("nbaComHeadshot")
                    val team = bodyObject.getString("team")
                    val position = bodyObject.getString("pos")
                    val statsObject = bodyObject.getJSONObject("stats")

                    val points = statsObject.getString("pts")
                    val rebounds = statsObject.getString("reb")
                    val assists = statsObject.getString("ast")
                    val steals = statsObject.optString("stl", null)
                    val blocks = statsObject.optString("blk", null)
                    val turnovers = statsObject.optString("TOV", null)

                    fetchPlayerInjuryAndProjections(id) { injStatus, injDescription, fantasyPointsProj, pointsProj, reboundsProj, assistsProj, stealsProj, blocksProj, turnoversProj ->
                        CoroutineScope(Dispatchers.Main).launch {
                            val player = PlayerDisplay(
                                id, longName, headshotUrl, team, position,
                                points, rebounds, assists, steals, blocks, turnovers,
                                injStatus, injDescription, fantasyPointsProj,
                                pointsProj, reboundsProj, assistsProj, stealsProj, blocksProj, turnoversProj
                            )

                            playerList.add(player)
                            playerAdapter.notifyItemInserted(playerList.size - 1)
                        }
                    }

                } else {
                    Log.e("API Error", "No 'body' object in response for playerID: $playerID")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("API Error", "Failed to fetch player data for playerID: $playerID")
        }
    }

    private fun fetchPlayerInjuryAndProjections(
        playerID: String,
        callback: (String, String, String, String, String, String, String, String, String) -> Unit
    ) {
        db.collection("players").document(playerID).get()
            .addOnSuccessListener { firestoreDocument ->
                val injuryData = firestoreDocument.get("Injury") as? Map<String, Any>
                val injStatus = injuryData?.get("status") as? String ?: "No injury"
                val injDescription = injuryData?.get("description") as? String ?: ""

                val projectionsData = firestoreDocument.get("Projections") as? Map<String, Any>
                val fantasyPointsProj = projectionsData?.get("fantasyPoints") as? String ?: "N/A"
                val pointsProj = projectionsData?.get("pts") as? String ?: "N/A"
                val reboundsProj = projectionsData?.get("reb") as? String ?: "N/A"
                val assistsProj = projectionsData?.get("ast") as? String ?: "N/A"
                val stealsProj = projectionsData?.get("stl") as? String ?: "N/A"
                val blocksProj = projectionsData?.get("blk") as? String ?: "N/A"
                val turnoversProj = projectionsData?.get("TOV") as? String ?: "N/A"

                callback(injStatus, injDescription, fantasyPointsProj, pointsProj, reboundsProj,
                    assistsProj, stealsProj, blocksProj, turnoversProj)
            }
            .addOnFailureListener { firestoreException ->
                Log.e("Firestore", "Error fetching Firestore data for player: $playerID", firestoreException)
                callback("", "", "N/A", "N/A", "N/A", "N/A", "N/A", "N/A", "N/A")
            }
    }

    class PlayerAdapter(
        private val playerList: List<PlayerDisplay>,
        private val clickListener: (PlayerDisplay) -> Unit
    ) : RecyclerView.Adapter<PlayerAdapter.PlayerViewHolder>() {

        class PlayerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val playerImage: ImageView = itemView.findViewById(R.id.playerImage)
            val playerName: TextView = itemView.findViewById(R.id.playerName)
            val playerPositionTeam: TextView = itemView.findViewById(R.id.playerPositionTeam)
            val playerPoints: TextView = itemView.findViewById(R.id.playerPoints)
            val playerReb: TextView = itemView.findViewById(R.id.playerReb)
            val playerAst: TextView = itemView.findViewById(R.id.playerAst)
            val playerStatus: TextView = itemView.findViewById(R.id.playerStatus)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
            val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.player_list_item, parent, false)
            return PlayerViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
            val player = playerList[position]
            holder.playerName.text = player.longName
            holder.playerPositionTeam.text = "${player.position} - ${player.team ?: "Unknown"}"
            holder.playerPoints.text = player.points ?: "N/A"
            holder.playerReb.text = player.rebounds ?: "N/A"
            holder.playerAst.text = player.assists ?: "N/A"
            holder.playerStatus.text = player.injStatus ?: "Healthy"


            if (!player.headshotUrl.isNullOrEmpty()) {
                Glide.with(holder.playerImage.context)
                    .load(player.headshotUrl)
                    .into(holder.playerImage)
            } else {
                holder.playerImage.setImageResource(R.drawable.player)
            }


            holder.itemView.setOnClickListener {
                clickListener(player)
                Log.d("PlayerListFragment", "Player Projections: pointsProj=${player.pointsProj}, reboundsProj=${player.reboundsProj}")

            }
        }

        override fun getItemCount() = playerList.size
    }
}
