import com.example.fantasy_basketball.Player
import com.example.fantasy_basketball.PlayerAPIService
import com.google.firebase.firestore.FirebaseFirestore
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
/*
class PlayerDataManager {

    private val db = FirebaseFirestore.getInstance()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://tank01-fantasy-stats.p.rapidapi.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api: PlayerAPIService = retrofit.create(PlayerAPIService::class.java)

    // Fetch player list from the API
    suspend fun fetchPlayerList(): List<Player> {
        println("Starting API request to fetch player list...")
        val response = api.getNBAPlayerList()  // Get the full response object
        println("Fetched ${response.body.size} players from API.")
        return response.body  // Extract and return the list of players
    }

    // Add players to Firestore in batches with logging
    fun addPlayersToFirestore(players: List<Player>) {
        val batchSize = 500
        val playerChunks = players.chunked(batchSize)  // Split the list into chunks of 500

        playerChunks.forEachIndexed { chunkIndex, playerChunk ->
            val batch = db.batch()  // Create a new Firestore batch
            playerChunk.forEach { player ->
                val playerRef = db.collection("players").document(player.playerID)
                val playerData = hashMapOf(
                    "team" to player.team,
                    "longName" to player.longName,
                    "teamID" to player.teamID
                )
                batch.set(playerRef, playerData)
            }

            batch.commit()
                .addOnSuccessListener {
                    println("Successfully committed chunk $chunkIndex to Firestore.")
                }
                .addOnFailureListener { e ->
                    println("Error committing chunk $chunkIndex to Firestore: $e")
                }
        }
    }

    // Fetch and store the player list in Firestore
    suspend fun fetchAndStorePlayers() {
        try {
            println("Initiating process to fetch and store players...")
            val players = fetchPlayerList()
            addPlayersToFirestore(players)
            println("Player data has been written to Firestore.")
        } catch (e: Exception) {
            println("Error fetching or storing player data: $e")
        }
    }
}



 */