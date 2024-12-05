package com.example.fantasy_basketball

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class LeagueChatFragment : Fragment() {

    private lateinit var recyclerViewMessages: RecyclerView
    private lateinit var messageInput: EditText
    private lateinit var sendButton: ImageView
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var adapter: LeagueChatAdapter
    private val messages = mutableListOf<Message>()
    private var messageListener: ListenerRegistration? = null
    private var leagueId: String? = null

    private val sharedViewModel: SharedDataViewModel by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_league_chat, container, false)

        // Initialize Firebase and views
        recyclerViewMessages = view.findViewById(R.id.recycler_view_messages)
        messageInput = view.findViewById(R.id.editText_message)
        sendButton = view.findViewById(R.id.imageView_send)
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Retrieve leagueId from arguments and handle if missing
        leagueId = arguments?.getString("leagueId")
        leagueId = sharedViewModel.leagueID
        if (leagueId == null) {
            Log.e("LeagueChatFragment", "League ID is missing")
            Toast.makeText(requireContext(), "League ID is missing", Toast.LENGTH_SHORT).show()
            return view
        } else {
            Log.d("LeagueChatFragment", "Received leagueId: $leagueId")
        }

        // Initialize RecyclerView and Adapter
        setupRecyclerView()

        // Load messages from Firestore
        loadMessages()

        // Set up the send button to add messages
        sendButton.setOnClickListener {
            val messageText = messageInput.text.toString().trim()
            if (messageText.isNotEmpty()) {
                sendMessage(messageText)
                messageInput.text.clear() // Clear input after sending
            }
        }

        return view
    }

    private fun setupRecyclerView() {
        adapter = LeagueChatAdapter(messages, leagueId!!)
        recyclerViewMessages.layoutManager = LinearLayoutManager(requireContext())
        recyclerViewMessages.adapter = adapter
    }

    private fun loadMessages() {
        leagueId?.let { id ->
            messageListener = firestore.collection("Leagues")
                .document(id)
                .collection("messages")
                .orderBy("timestamp")
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.e("LeagueChatFragment", "Error loading messages: ${error.message}")
                        return@addSnapshotListener
                    }
                    snapshots?.let {
                        messages.clear()
                        for (document in snapshots.documents) {
                            val message = document.toObject(Message::class.java)
                            message?.let { messages.add(it) }
                        }
                        adapter.notifyDataSetChanged()
                        recyclerViewMessages.scrollToPosition(messages.size - 1)
                    }
                }
        }
    }

    private fun sendMessage(messageText: String) {
        val senderId = auth.currentUser?.uid ?: ""
        if (senderId.isEmpty()) {
            Toast.makeText(requireContext(), "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        val message = mapOf(
            "senderId" to senderId,
            "messageText" to messageText,
            "timestamp" to FieldValue.serverTimestamp()
        )

        leagueId?.let { id ->
            val messagesCollection = firestore.collection("Leagues")
                .document(id)
                .collection("messages")

            messagesCollection.add(message)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Message sent!", Toast.LENGTH_SHORT).show()
                    Log.d("LeagueChatFragment", "Message sent successfully")
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Failed to send message: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("LeagueChatFragment", "Error sending message: ${e.message}")
                }
        } ?: run {
            Toast.makeText(requireContext(), "League ID is missing", Toast.LENGTH_SHORT).show()
            Log.e("LeagueChatFragment", "League ID is null")
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        messageListener?.remove()
    }
}
