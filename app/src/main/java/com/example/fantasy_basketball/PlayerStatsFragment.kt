package com.example.fantasy_basketball

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException


class PlayerStatsFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var playerAdapter: PlayerAdapter

    private val playerList = mutableListOf<Player>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = FirebaseFirestore.getInstance()
    }

    private var isLoading = false  // Prevent multiple API calls at once
    private var lastVisibleDocument: DocumentSnapshot? = null  // Track the last document fetched
    private val pageSize = 20  // Number of players to load per request

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_player_list, container, false)

        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        playerAdapter = PlayerAdapter(playerList)
        recyclerView.adapter = playerAdapter

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                if (!isLoading && (visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                    && firstVisibleItemPosition >= 0
                    && totalItemCount >= pageSize) {

                    fetchPlayerIDsWithPagination()
                }
            }
        })

        fetchPlayerIDsWithPagination()

        return view
    }

    private fun fetchPlayerIDsWithPagination() {
        if (isLoading) return  // Avoid making duplicate calls

        isLoading = true

        val query = if (lastVisibleDocument == null) {
            // Initial load
            db.collection("players").limit(pageSize.toLong())
        } else {
            // Load next set of data
            db.collection("players").startAfter(lastVisibleDocument!!).limit(pageSize.toLong())
        }

        query.get().addOnSuccessListener { result ->
            if (result.isEmpty) {
                Log.d("Firestore", "No more players to load")
                isLoading = false
                return@addOnSuccessListener
            }

            lastVisibleDocument = result.documents[result.size() - 1]  // Save the last document

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


    class PlayerAdapter(private val playerList: List<Player>) :
        RecyclerView.Adapter<PlayerAdapter.PlayerViewHolder>() {

        class PlayerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val playerName: TextView = itemView.findViewById(R.id.playerName)
            val playerPoints: TextView = itemView.findViewById(R.id.playerPoints)
            val playerRebounds: TextView = itemView.findViewById(R.id.playerRebounds)
            val playerAssists: TextView = itemView.findViewById(R.id.playerAssists)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
            val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.player_item, parent, false)
            return PlayerViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
            val player = playerList[position]
            holder.playerName.text = player.longName
            holder.playerPoints.text = "Points: ${player.points}"
            holder.playerRebounds.text = "Rebounds: ${player.rebounds}"
            holder.playerAssists.text = "Assists: ${player.assists}"
        }

        override fun getItemCount() = playerList.size
    }

    fun fetchPlayerIDsAndCallAPI() {
        CoroutineScope(Dispatchers.Main).launch {
            db.collection("players").get()
                .addOnSuccessListener { result ->
                    for (document in result) {
                        val playerID = document.id
                        CoroutineScope(Dispatchers.IO).launch {
                            getPlayerDataFromAPI(playerID)
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    exception.printStackTrace()
                }
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
            withContext(Dispatchers.IO) {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")

                    val responseBody = response.body?.string()
                    responseBody?.let {
                        Log.d("API Response", it)

                        val jsonObject = JSONObject(it)


                        if (jsonObject.has("body")) {
                            val bodyObject = jsonObject.getJSONObject("body")

                            val id = bodyObject.getString("playerID")
                            val longName = bodyObject.getString("longName")
                            val statsObject = bodyObject.getJSONObject("stats")

                            val points = statsObject.getString("pts")
                            val rebounds = statsObject.getString("reb")
                            val assists = statsObject.getString("ast")

                            val player = Player(id, longName, points, rebounds, assists)

                            withContext(Dispatchers.Main) {
                                playerList.add(player)
                                Log.d("Player List", "Player list size: ${playerList.size}")
                                playerAdapter.notifyItemInserted(playerList.size - 1)
                            }

                        } else {
                            Log.e("API Error", "No 'player' object in response for playerID: $playerID")
                        }


                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("API Error", "Failed to fetch player data for playerID: $playerID")
        }
    }





}
