package com.example.fantasy_basketball
//
//import androidx.recyclerview.widget.RecyclerView
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.ImageView
//import android.widget.TextView
///**
// * [RecyclerView.Adapter] that can display a [players].
// *
// */
//// Change to List<dataClass>
//
//class PlayerAdapter(private var playerList: MutableList<Player>) :
//    RecyclerView.Adapter<PlayerAdapter.PlayerViewHolder>() {
//
//    // ViewHolder class to represent each item in the RecyclerView
//    class PlayerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
//        val playerName: TextView = view.findViewById(R.id.playerName)
//        val playerPosition: TextView = view.findViewById(R.id.playerPosition)
//        val playerTeam: TextView = view.findViewById(R.id.playerTeam)
//        val playerStatus: TextView = view.findViewById(R.id.playerStatus)
//        val playerAvg: TextView = view.findViewById(R.id.playerAvg)
//        val playerScore: TextView = view.findViewById(R.id.playerPoints)
//        val playerMin: TextView = view.findViewById(R.id.playerMinutes)
//        val playerImage : ImageView = view.findViewById(R.id.playerPicture)
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
//        val view = LayoutInflater.from(parent.context)
//            .inflate(R.layout.fragment_player_search_list, parent, false)
//        return PlayerViewHolder(view)
//    }
//
//    override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
//        val player = playerList[position]
//        holder.playerName.text = player.name
//        holder.playerPosition.text = player.pos
//        holder.playerTeam.text = player.team
//        holder.playerStatus.text = player.status.toString()
//        holder.playerScore.text = player.score.toString()
//        holder.playerAvg.text = player.avg.toString()
//        holder.playerMin.text = player.min.toString()
//        holder.playerImage.setImageResource(player.imageUrl)
//    }
//
//    override fun getItemCount(): Int {
//        return playerList.size
//    }
//
//    // Update the list when filtering is applied
//    fun updateList(newPlayerList: MutableList<Player>) {
//        playerList = newPlayerList
//        notifyDataSetChanged()
//    }
//}



