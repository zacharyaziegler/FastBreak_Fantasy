package com.example.fantasy_basketball

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
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
import kotlin.coroutines.cancellation.CancellationException


class PlayerStatsFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var playerAdapter: PlayerAdapter

    private lateinit var playerNameInput: EditText
    private lateinit var searchButton: Button
    private lateinit var resetButton: Button


    private var isSearching = false


    private val playerList = mutableListOf<Player>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = FirebaseFirestore.getInstance()
    }

    private var isLoading = false
    private var lastVisibleDocument: DocumentSnapshot? = null
    private val pageSize = 10

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_player_list, container, false)

        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        playerAdapter = PlayerAdapter(playerList)
        recyclerView.adapter = playerAdapter

        searchButton = view.findViewById(R.id.searchButton)
        resetButton = view.findViewById(R.id.resetButton)
        playerNameInput = view.findViewById(R.id.playerNameInput)


        searchButton.setOnClickListener {
            val playerName = playerNameInput.text.toString().trim()
            if (playerName.isNotEmpty()) {
                isSearching = true
                fetchPlayerStatsByName(playerName)
            } else {
                Toast.makeText(requireContext(), "Please enter a player name", Toast.LENGTH_SHORT).show()
            }
        }

        resetButton.setOnClickListener {
            resetPlayerList()
        }


        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                //Log.d("ScrollListener", "visibleItemCount: $visibleItemCount, firstVisibleItemPosition: $firstVisibleItemPosition, totalItemCount: $totalItemCount")

                if (!isLoading && layoutManager.findLastVisibleItemPosition() == totalItemCount - 1) {
                    Log.d("ScrollListener", "End reached, loading more players")
                    fetchPlayerIDsWithPagination()
                }
            }
        })

        fetchPlayerIDsWithPagination()

        return view
    }

    private fun fetchPlayerIDsWithPagination() {
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

            Log.d("Firestore", "Last visible document: ${lastVisibleDocument?.id}")

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

    private fun fetchPlayerStatsByName(playerName: String) {
        val client = OkHttpClient()

        val url = "https://tank01-fantasy-stats.p.rapidapi.com/getNBAPlayerInfo?playerName=$playerName&statsToGet=averages"

        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader("x-rapidapi-key", "a0ac93dc3amsh37d315dd4ab6990p119d93jsn2d0bf27cf642")
            .addHeader("x-rapidapi-host", "tank01-fantasy-stats.p.rapidapi.com")
            .build()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                client.newCall(request).execute().use { response ->
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
                                val statsObject = playerObject.optJSONObject("stats") ?: JSONObject()

                                val points = statsObject.optString("pts", null)
                                val rebounds = statsObject.optString("reb", null)
                                val assists = statsObject.optString("ast", null)


                                if (points != null && rebounds != null && assists != null) {
                                    val player = Player(id, longName, points, rebounds, assists)
                                    playerList.add(player)
                                }
                            }

                            withContext(Dispatchers.Main) {
                                playerAdapter.notifyDataSetChanged()
                            }

                        } else {
                            Log.e("API Error", "No 'body' object found in response for name: $playerName")
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("API Error", "Failed to fetch player data for playerName: $playerName")
            }
        }
    }


    private fun resetPlayerList() {
        playerNameInput.text.clear()
        playerList.clear()
        isSearching = false
        lastVisibleDocument = null
        fetchPlayerIDsWithPagination()
        playerAdapter.notifyDataSetChanged()
    }





}

