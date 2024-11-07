import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.fantasy_basketball.Player
import com.example.fantasy_basketball.PlayerAdapter.PlayerViewHolder
import com.example.fantasy_basketball.R

class RosterAdapter(
    private val players: List<Player?>,
    private val slotPositions: List<String>,
    private val onSlotClick: (position: Int, slot: String) -> Unit
) : RecyclerView.Adapter<RosterAdapter.PlayerViewHolder>() {

    class PlayerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val playerName: TextView = view.findViewById(R.id.playerName)
        val playerPosition: TextView = view.findViewById(R.id.playerPosition)
        val playerTeam: TextView = view.findViewById(R.id.playerTeam)
        val playerStatus: TextView = view.findViewById(R.id.playerStatus)
        val playerImage: ImageView = view.findViewById(R.id.playerPicture)
        val playerPoints: TextView = view.findViewById(R.id.playerPoints)
        val playerAvg: TextView = view.findViewById(R.id.playerAvg)
        val playerMin: TextView = view.findViewById(R.id.playerMinutes)
        val positionLabel: TextView = view.findViewById(R.id.btnAddPlayer)
        val selectPlayerButton: Button = view.findViewById(R.id.btnAddPlayer)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.roster_player_item, parent, false)
        return PlayerViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
        val player = players[position]
        val slot = slotPositions[position]

        // Set position label (e.g., PG, SG, etc.)
        holder.positionLabel.text = slotPositions[position]

        if (player != null) {
            // Set player's basic info
            holder.playerName.text = player.longName
            holder.playerPosition.text = player.pos
            holder.playerTeam.text = player.team

            // Display status as DTD, O, or H
            holder.playerStatus.text = when (player.injury?.designation) {
                "Day-To-Day" -> "DTD"
                "Out" -> "O"
                null, "Healthy" -> "H"
                else -> player.injury?.designation ?: "N/A"
            }

            // Load player's headshot using Glide
            Glide.with(holder.itemView.context)
                .load(player.nbaComHeadshot)
                .placeholder(R.drawable.player)  // This will be used only if the image fails to load
                .into(holder.playerImage)

            // Set player's projections (if available)
            player.projection?.let { projection ->
                holder.playerPoints.text = if (projection.fantasyPoints.isNotEmpty()) projection.fantasyPoints else "--"
                holder.playerAvg.text = if (projection.pts.isNotEmpty()) projection.pts else "--"
                holder.playerMin.text = if (projection.mins.isNotEmpty()) projection.mins else "--"
            } ?: run {
                holder.playerPoints.text = "--"
                holder.playerAvg.text = "--"
                holder.playerMin.text = "--"
            }

        } else {
            // If the player slot is empty, set all fields to default values
            holder.playerName.text = ""
            holder.playerPosition.text = ""
            holder.playerTeam.text = ""
            holder.playerStatus.text = ""
            holder.playerPoints.text = ""
            holder.playerAvg.text = ""
            holder.playerMin.text = ""

            // Don't load any image when there is no player
            holder.playerImage.setImageDrawable(null)
        }

        holder.selectPlayerButton.setOnClickListener {
            onSlotClick(position, slot)
        }
    }



    override fun getItemCount(): Int {
        return players.size
    }
}
