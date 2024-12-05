package com.example.fantasy_basketball

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class LeagueSettingsFragment : Fragment() {

    private lateinit var draftDateEditText: EditText
    private lateinit var draftTimeEditText: EditText
    private lateinit var setDraftButton: Button
   // private lateinit var leagueNameTextView: TextView

    private var selectedYear = 0
    private var selectedMonth = 0
    private var selectedDay = 0
    private var selectedHour = 0
    private var selectedMinute = 0

    private lateinit var firestore: FirebaseFirestore
    private lateinit var leagueId: String
    private val sharedViewModel: SharedDataViewModel by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_league_settings, container, false)

        // Initialize views
        draftDateEditText = view.findViewById(R.id.draftDateEditText)
        draftTimeEditText = view.findViewById(R.id.draftTimeEditText)
        setDraftButton = view.findViewById(R.id.setDraftButton)
       // leagueNameTextView = view.findViewById(R.id.leagueName)

        // Retrieve and display the league name and ID
        var leagueName = arguments?.getString("leagueName") ?: "Unknown League"
        leagueId = arguments?.getString("leagueId") ?: ""
        leagueId = sharedViewModel.leagueID.toString()
        leagueName = sharedViewModel.leagueName.toString()

     //   leagueNameTextView.text = leagueName


        firestore = FirebaseFirestore.getInstance()



        // Load current draft date and time if available
        loadDraftDateTime()

        // Set click listeners for date and time selection
        draftDateEditText.setOnClickListener { openDatePicker() }
        draftTimeEditText.setOnClickListener { openTimePicker() }

        setDraftButton.setOnClickListener {
            validateAndSetDraft()
        }

        return view
    }

    private fun loadDraftDateTime() {
        firestore.collection("Leagues").document(leagueId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val draftDateTime = document.getTimestamp("draftDateTime")?.toDate()
                    if (draftDateTime != null) {
                        // Format and display the date and time
                        val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
                        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                        draftDateEditText.setText(dateFormat.format(draftDateTime))
                        draftTimeEditText.setText(timeFormat.format(draftDateTime))

                        // Set the selected fields for comparison in validation
                        val calendar = Calendar.getInstance().apply { time = draftDateTime }
                        selectedYear = calendar.get(Calendar.YEAR)
                        selectedMonth = calendar.get(Calendar.MONTH)
                        selectedDay = calendar.get(Calendar.DAY_OF_MONTH)
                        selectedHour = calendar.get(Calendar.HOUR_OF_DAY)
                        selectedMinute = calendar.get(Calendar.MINUTE)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("LeagueSettingsFragment", "Failed to load draft date/time: ${e.message}")
            }
    }

    private fun openDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
            // Update selected date
            this.selectedYear = selectedYear
            this.selectedMonth = selectedMonth
            this.selectedDay = selectedDay

            // Update EditText with selected date
            draftDateEditText.setText("${selectedMonth + 1}/$selectedDay/$selectedYear")
        }, year, month, day)

        // Set minimum date to current date
        datePickerDialog.datePicker.minDate = calendar.timeInMillis
        datePickerDialog.show()
    }

    private fun openTimePicker() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(requireContext(), { _, selectedHour, selectedMinute ->
            // Update selected time
            this.selectedHour = selectedHour
            this.selectedMinute = selectedMinute

            // Format the time to display in 12-hour format with AM/PM
            val formattedTime = if (selectedHour >= 12) {
                String.format("%02d:%02d PM", if (selectedHour == 12) selectedHour else selectedHour - 12, selectedMinute)
            } else {
                String.format("%02d:%02d AM", if (selectedHour == 0) 12 else selectedHour, selectedMinute)
            }
            draftTimeEditText.setText(formattedTime)
        }, hour, minute, false) // Pass 'false' here to use 12-hour format

        timePickerDialog.show()
    }

    private fun validateAndSetDraft() {
        if (draftDateEditText.text.isEmpty() || draftTimeEditText.text.isEmpty()) {
            Toast.makeText(requireContext(), "Please select both date and time", Toast.LENGTH_SHORT).show()
            return
        }

        // Get the current date and time
        val currentCalendar = Calendar.getInstance()
        val selectedCalendar = Calendar.getInstance().apply {
            set(selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute)
        }

        // Check if selected date/time is in the past
        if (selectedCalendar.timeInMillis <= currentCalendar.timeInMillis) {
            Toast.makeText(requireContext(), "Please select a future date and time", Toast.LENGTH_SHORT).show()
            return
        }

        // Confirmation dialog
        val formattedDate = draftDateEditText.text.toString()
        val formattedTime = draftTimeEditText.text.toString()
        AlertDialog.Builder(requireContext())
            .setTitle("Confirm Draft Date/Time")
            .setMessage("Are you sure you want to set the draft for $formattedDate at $formattedTime?")
            .setPositiveButton("Yes") { _, _ ->
                saveDraftDateTime(selectedCalendar)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun saveDraftDateTime(selectedCalendar: Calendar) {
        val draftDateTime = selectedCalendar.time

        // Calculate pickExpirationTime (60 seconds after draftDateTime)
        val pickExpirationTime = Calendar.getInstance().apply {
            time = draftDateTime
            add(Calendar.SECOND, 10) //TODO: Change to 60 for prod
        }.time

        // Update Firestore with draftDateTime, draftStatus, currentPickIndex, and pickExpirationTime
        firestore.collection("Leagues").document(leagueId)
            .update(
                mapOf(
                    "draftStatus" to "date_set",
                    "draftDateTime" to draftDateTime,
                    "currentPickIndex" to 0, // Initialize the current pick index
                    "pickExpirationTime" to pickExpirationTime // Initialize the pick expiration time
                )
            )
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Draft date/time set successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("LeagueSettingsFragment", "Failed to set draft date/time: ${e.message}")
                Toast.makeText(requireContext(), "Failed to set draft date/time: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

}
