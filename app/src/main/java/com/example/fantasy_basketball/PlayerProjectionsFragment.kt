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
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

class PlayerProjectionsFragment : Fragment() {

    private lateinit var playerAdapter: PlayerAdapter
    private val playerList = mutableListOf<PlayerProjection>()
    private val fullPlayerList = mutableListOf<PlayerProjection>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_player_projection, container, false)

        val recyclerView: RecyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        playerAdapter = PlayerAdapter(playerList)
        recyclerView.adapter = playerAdapter

        val playerNameInput: EditText = view.findViewById(R.id.playerNameInput)
        val searchButton: Button = view.findViewById(R.id.searchButton)
        val resetButton: Button = view.findViewById(R.id.resetButton)

        searchButton.setOnClickListener {
            val query = playerNameInput.text.toString().trim()
            if (query.isNotEmpty()) {
                filterPlayers(query)
            }
        }

        resetButton.setOnClickListener {
            resetPlayerList()
        }


        viewLifecycleOwner.lifecycleScope.launch {
            fetchProjectionsFromAPI()
        }

        return view
    }

    class PlayerAdapter(private val playerList: List<PlayerProjection>) : RecyclerView.Adapter<PlayerAdapter.PlayerViewHolder>() {

        class PlayerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val playerName: TextView = itemView.findViewById(R.id.playerName)
            val playerPoints: TextView = itemView.findViewById(R.id.playerPoints)
            val playerRebounds: TextView = itemView.findViewById(R.id.playerRebounds)
            val playerAssists: TextView = itemView.findViewById(R.id.playerAssists)
            val playerSteals: TextView = itemView.findViewById(R.id.playerSteals)
            val playerBlocks: TextView = itemView.findViewById(R.id.playerBlocks)
            val playerProjection: TextView = itemView.findViewById(R.id.playerProjection)
        }



        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
            val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.player_projection, parent, false)
            return PlayerViewHolder(itemView)

        }

        override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
            val player = playerList[position]
            holder.playerName.text = player.longName
            holder.playerPoints.text = "Points: ${player.pts}"
            holder.playerRebounds.text = "Rebounds: ${player.reb}"
            holder.playerAssists.text = "Assists: ${player.ast}"
            holder.playerSteals.text = "Steals: ${player.stl}"
            holder.playerBlocks.text = "Blocks: ${player.blk}"
            holder.playerProjection.text = "Fantasy Points: ${player.fantasyPoints}"
        }

        override fun getItemCount() = playerList.size
    }

    private suspend fun fetchProjectionsFromAPI() {
        val client = OkHttpClient()

        val url =
            "https://tank01-fantasy-stats.p.rapidapi.com/getNBAProjections?numOfDays=7&pts=1&reb=1.25&TOV=-1&stl=3&blk=3&ast=1.5&mins=0"

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
                            if (bodyObject.has("playerProjections")) {
                                val playerProjections =
                                    bodyObject.getJSONObject("playerProjections")

                                val keys = playerProjections.keys()
                                while (keys.hasNext()) {
                                    val playerID = keys.next()
                                    val playerObject = playerProjections.getJSONObject(playerID)

                                    val playerProjection = PlayerProjection(
                                        playerID = playerID,
                                        longName = playerObject.getString("longName"),
                                        pts = playerObject.getString("pts"),
                                        reb = playerObject.getString("reb"),
                                        ast = playerObject.getString("ast"),
                                        stl = playerObject.getString("stl"),
                                        blk = playerObject.getString("blk"),
                                        mins = "0",
                                        TOV = "",
                                        team = "0",
                                        pos = "2",
                                        teamID = "1",
                                        fantasyPoints = playerObject.getString("fantasyPoints")
                                    )

                                    withContext(Dispatchers.Main) {
                                        playerList.add(playerProjection)
                                        fullPlayerList.add(playerProjection)
                                        playerAdapter.notifyItemInserted(playerList.size - 1)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("API Error", "Failed to fetch player data")
            }
        }

    private fun filterPlayers(query: String) {
        val filteredList = fullPlayerList.filter { player ->
            player.longName.contains(query, ignoreCase = true)
        }
        playerList.clear()
        playerList.addAll(filteredList)
        playerAdapter.notifyDataSetChanged()
    }



    private fun resetPlayerList() {
        playerList.clear()
        playerList.addAll(fullPlayerList)
        playerAdapter.notifyDataSetChanged()
    }

}

