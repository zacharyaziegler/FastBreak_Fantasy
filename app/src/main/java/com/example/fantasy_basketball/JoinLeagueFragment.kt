package com.example.fantasy_basketball

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class JoinLeagueFragment : Fragment() {

    private lateinit var inviteCodeInput: EditText
    private lateinit var joinLeagueButton: Button
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firestore and Auth
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_join_league, container, false)

        // Initialize Views
        inviteCodeInput = view.findViewById(R.id.inviteCodeInput)
        joinLeagueButton = view.findViewById(R.id.joinLeagueButton)

        // Set up Join League Button Click Listener
        joinLeagueButton.setOnClickListener {
            val inviteCode = inviteCodeInput.text.toString().trim().uppercase()

            if (inviteCode.length == 6) {
                // Start the process to join the league
                checkInviteCode(inviteCode)
            } else {
                Toast.makeText(requireContext(), "Please enter a valid 6-letter invite code", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    private fun checkInviteCode(inviteCode: String) {
        val currentUserId = auth.currentUser?.uid ?: return

        // Check if a league with this invite code exists
        firestore.collection("Leagues")
            .whereEqualTo("inviteCode", inviteCode)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    Toast.makeText(requireContext(), "Invalid invite code", Toast.LENGTH_SHORT).show()
                } else {
                    val leagueDocument = querySnapshot.documents[0]
                    val leagueId = leagueDocument.id
                    val leagueSize = (leagueDocument.get("settings.leagueSize") as Long).toInt()
                    val members = leagueDocument.get("members") as? List<String> ?: emptyList()

                    // Check if the user is already in this league
                    if (members.contains(currentUserId)) {
                        Toast.makeText(requireContext(), "You are already in this league", Toast.LENGTH_SHORT).show()
                    } else {
                        checkLeagueCapacity(leagueId, leagueSize, currentUserId)
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to check invite code: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkLeagueCapacity(leagueId: String, leagueSize: Int, userId: String) {
        // Check if the league has room for new members
        firestore.collection("Leagues").document(leagueId).collection("Teams")
            .whereEqualTo("ownerID", "")
            .get()
            .addOnSuccessListener { teamSnapshot ->
                val vacantTeams = teamSnapshot.documents

                if (vacantTeams.size == 0) {
                    Toast.makeText(requireContext(), "This league is already full", Toast.LENGTH_SHORT).show()
                } else {
                    // There is room, assign the user to a vacant team
                    val vacantTeam = vacantTeams[0]
                    val teamId = vacantTeam.id

                    assignUserToTeam(leagueId, teamId, userId)
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to check league capacity: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun assignUserToTeam(leagueId: String, teamId: String, userId: String) {
        val userRef = firestore.collection("users").document(userId)
        val leagueRef = firestore.collection("Leagues").document(leagueId)

        // Update the team with the new owner
        firestore.collection("Leagues").document(leagueId).collection("Teams").document(teamId)
            .update("ownerID", userId)
            .addOnSuccessListener {
                // Update the user's leagues and teams array
                userRef.update(
                    "leagues", com.google.firebase.firestore.FieldValue.arrayUnion(leagueId),
                    "teams", com.google.firebase.firestore.FieldValue.arrayUnion(teamId)
                ).addOnSuccessListener {
                    // Update the league's members array
                    leagueRef.update("members", com.google.firebase.firestore.FieldValue.arrayUnion(userId))
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Successfully joined the league!", Toast.LENGTH_SHORT).show()
                            findNavController().navigate(R.id.action_joinLeagueFragment_to_homeFragment)
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(requireContext(), "Failed to update league members: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }.addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Failed to update user data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to assign team: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
