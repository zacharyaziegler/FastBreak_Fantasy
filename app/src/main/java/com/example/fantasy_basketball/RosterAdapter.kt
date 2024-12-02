import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.fantasy_basketball.Player
import com.example.fantasy_basketball.R

class RosterAdapter(
    private val players: List<Player?>,
    private val slotPositions: List<String>,
    private val onSlotClick: (position: Int, slot: String) -> Unit,
    private val onCardClick: (player: Player?) -> Unit
) : RecyclerView.Adapter<RosterAdapter.PlayerViewHolder>() {

    class PlayerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardView: CardView = view.findViewById(R.id.cardView)
        val playerName: TextView = view.findViewById(R.id.playerName)
        val playerPosition: TextView = view.findViewById(R.id.playerPosition)
        val playerTeam: TextView = view.findViewById(R.id.playerTeam)
        val playerStatus: TextView = view.findViewById(R.id.playerStatus)
        val playerImage: ImageView = view.findViewById(R.id.playerPicture)
        val playerPoints: TextView = view.findViewById(R.id.playerPoints)
        val selectPlayerButton: Button = view.findViewById(R.id.selectPlayerButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.roster_player_item, parent, false)
        return PlayerViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
        val player = players[position]
        val slot = slotPositions[position]

        // Set the slot label on the selectPlayerButton
        holder.selectPlayerButton.text = slot

        if (player != null) {
            // Set player's details
            holder.playerName.text = player.longName
            holder.playerPosition.text = player.pos
            holder.playerTeam.text = player.team
            holder.playerStatus.text = when (player.injury?.designation) {
                "Day-To-Day" -> "DTD"
                "Out" -> "O"
                else -> "H"
            }

            // Load player image
            Glide.with(holder.itemView.context)
                .load(player.nbaComHeadshot)
                .placeholder(R.drawable.player)
                .error(R.drawable.player)
                .into(holder.playerImage)

            // Set player fantasy points projection
            holder.playerPoints.text = player.projection?.fantasyPoints ?: "--"
        } else {
            // Clear player details if no player is available
            holder.playerName.text = ""
            holder.playerPosition.text = ""
            holder.playerTeam.text = ""
            holder.playerStatus.text = ""
            holder.playerPoints.text = ""
            holder.playerImage.setImageDrawable(null)

            holder.cardView.isEnabled = false
        }

        // Enable or disable the button based on the slot
        when (slot) {
            "BE" -> {
                holder.selectPlayerButton.isEnabled = false
                holder.selectPlayerButton.alpha = 0.5f
            }
            "IR" -> {
                holder.selectPlayerButton.isEnabled = true
                holder.selectPlayerButton.alpha = 1f
            }
            else -> {
                holder.selectPlayerButton.isEnabled = true
                holder.selectPlayerButton.alpha = 1f
            }
        }

        // Handle select button click (slot click)
        holder.selectPlayerButton.setOnClickListener {
            onSlotClick(position, slot)
        }

        // Handle card click (player profile)
        holder.cardView.setOnClickListener {
            onCardClick(player)
        }
    }

    override fun getItemCount(): Int {
        return players.size
    }
}
