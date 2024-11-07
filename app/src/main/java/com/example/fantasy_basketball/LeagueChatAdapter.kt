package com.example.fantasy_basketball

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class LeagueChatAdapter(private val messages: List<Message>, private val leagueId: String) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val firestore = FirebaseFirestore.getInstance()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    companion object {
        private const val VIEW_TYPE_OTHER_USER = 1
        private const val VIEW_TYPE_CURRENT_USER = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].senderId == currentUserId) VIEW_TYPE_CURRENT_USER else VIEW_TYPE_OTHER_USER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layout = if (viewType == VIEW_TYPE_CURRENT_USER) {
            R.layout.item_message_currsender
        } else {
            R.layout.item_message_othersender
        }
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        if (holder is MessageViewHolder) {
            // Format and set timestamp
            message.timestamp?.let {
                val sdf = SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault())
                holder.timestampTextView.text = sdf.format(it.toDate())
            } ?: run {
                holder.timestampTextView.text = ""
            }

            // Set message text
            holder.messageTextView.text = message.messageText

            // Fetch team name and profile picture URL based on sender's ID (ownerID)
            firestore.collection("Leagues")
                .document(leagueId)
                .collection("Teams")
                .whereEqualTo("ownerID", message.senderId)
                .limit(1)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        val teamDoc = documents.documents[0]

                        // Set team name as the username
                        holder.usernameTextView.text = teamDoc.getString("teamName") ?: "Unknown Team"

                        // Load the profile picture with placeholder
                        val profilePictureUrl = teamDoc.getString("profilePictureUrl")
                        Glide.with(holder.profileImageView.context)
                            .load(profilePictureUrl) // URL from Firestore
                            .placeholder(R.drawable.profile_picture_circle_shape) // Placeholder
                            .circleCrop()
                            .into(holder.profileImageView)
                    } else {
                        // Set default text and image if no team document is found
                        holder.usernameTextView.text = "Unknown Team"
                        holder.profileImageView.setImageResource(R.drawable.ic_default_profile)
                    }
                }
                .addOnFailureListener {
                    // Error case: Set default text and image
                    holder.usernameTextView.text = "Error loading team"
                    holder.profileImageView.setImageResource(R.drawable.ic_default_profile)
                }
        }
    }

    override fun getItemCount(): Int = messages.size

    inner class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val profileImageView: ImageView = view.findViewById(R.id.profileImageView)
        val usernameTextView: TextView = view.findViewById(R.id.usernameTextView)
        val timestampTextView: TextView = view.findViewById(R.id.timestampTextView)
        val messageTextView: TextView = view.findViewById(R.id.messageTextView)
    }
}
