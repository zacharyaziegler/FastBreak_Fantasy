package com.example.fantasy_basketball

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class LeagueChatAdapter(private val messages: List<Message>, private val leagueId: String) : RecyclerView.Adapter<LeagueChatAdapter.MessageViewHolder>() {

    private val firestore = FirebaseFirestore.getInstance()

    class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val profileImageView: ImageView = view.findViewById(R.id.profileImageView)
        val usernameTextView: TextView = view.findViewById(R.id.usernameTextView)
        val timestampTextView: TextView = view.findViewById(R.id.timestampTextView)
        val messageTextView: TextView = view.findViewById(R.id.messageTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]

        // Set sender's ID (or use a display name if available)
        holder.usernameTextView.text = message.senderId

        // Set message text
        holder.messageTextView.text = message.messageText

        // Format and set timestamp
        message.timestamp?.let {
            val sdf = SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault())
            holder.timestampTextView.text = sdf.format(it.toDate())
        } ?: run {
            holder.timestampTextView.text = "" // Display nothing if timestamp is null
        }

        // Fetch the profile picture URL from Firestore based on sender's ID (ownerID)
        firestore.collection("Leagues")
            .document(leagueId)
            .collection("Teams")
            .whereEqualTo("ownerID", message.senderId)
            .limit(1)  // Limiting to 1 as we only need the sender's team document
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val teamDoc = documents.documents[0]
                    val profilePictureUrl = teamDoc.getString("profilePictureUrl")
                    if (!profilePictureUrl.isNullOrEmpty()) {
                        // Load the profile picture with Glide
                        Glide.with(holder.profileImageView.context)
                            .load(profilePictureUrl)
                            .circleCrop()
                            .into(holder.profileImageView)
                    } else {
                        // Set a placeholder image if URL is empty
                        holder.profileImageView.setImageResource(R.drawable.ic_default_profile)
                    }
                } else {
                    // Set a placeholder if no matching team document is found
                    holder.profileImageView.setImageResource(R.drawable.ic_default_profile)
                }
            }
            .addOnFailureListener {
                // Set a placeholder image if there's an error
                holder.profileImageView.setImageResource(R.drawable.ic_default_profile)
            }
    }

    override fun getItemCount(): Int = messages.size
}
