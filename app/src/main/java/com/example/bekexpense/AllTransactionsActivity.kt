package com.example.bekexpense

import android.app.DatePickerDialog
import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

@Suppress("DEPRECATION")
class AllTransactionsActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var transactionAdapter: TransactionAdapter

    private lateinit var tabLayoutTransactionType: TabLayout
    private lateinit var tabLayoutFilter: TabLayout
    private lateinit var spinnerSort: Spinner

    private var selectedType: String = "Expense" // Default transaction type
    private var startDate: Long? = null
    private var endDate: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_transactions)


        setSystemUIFlags()


        // Initialize Firebase
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Initialize views
        tabLayoutTransactionType = findViewById(R.id.tabLayoutTransactionType)
        tabLayoutFilter = findViewById(R.id.tabLayoutFilter)
        spinnerSort = findViewById(R.id.spinnerSort)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewTransactions)
        recyclerView.layoutManager = LinearLayoutManager(this)
        transactionAdapter = TransactionAdapter(mutableListOf()) { transaction ->
            handleTransactionClick(transaction)
        }
        recyclerView.adapter = transactionAdapter

        setupTabLayouts()
        setupSortSpinner()

        // Default: Load Expense transactions for the current day
        val currentDayStart = getStartOfDay(System.currentTimeMillis())
        val currentDayEnd = getEndOfDay(System.currentTimeMillis())
        loadTransactions(type = selectedType, startDate = currentDayStart, endDate = currentDayEnd)
    }

    private fun setSystemUIFlags() {
        val backgroundColor = Color.parseColor("#000000")
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                )
        window.statusBarColor = backgroundColor
        window.navigationBarColor = backgroundColor
    }

    private fun getStartOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    private fun getEndOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return calendar.timeInMillis
    }

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    private fun setupTabLayouts() {
        // Setup transaction type tabs (Expenses, Income)
        tabLayoutTransactionType.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                selectedType = when (tab?.text) {
                    "Expenses" -> "Expense"
                    "Income" -> "Income"
                    else -> "Expense"
                }
                applyCurrentFilters()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // Setup filter tabs (Day, Week, Month, Year, Period)
        tabLayoutFilter.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.text) {
                    "Day" -> filterTransactionsByDay()
                    "Week" -> filterTransactionsByWeek()
                    "Month" -> filterTransactionsByMonth()
                    "Year" -> filterTransactionsByYear()
                    "Period" -> showDatePickerDialog()
                    else -> applyCurrentFilters()
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupSortSpinner() {
        spinnerSort.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (position) {
                    0 -> transactionAdapter.sortByDate()
                    1 -> transactionAdapter.sortByAmount()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadTransactions(type: String? = null, startDate: Long? = null, endDate: Long? = null) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            var query: Query = db.collection("Users")
                .document(userId)
                .collection("Transactions")

            // Apply filters for type (Expense/Income)
            if (type != null) {
                query = query.whereEqualTo("type", type)
            }

            // Apply date range filters
            if (startDate != null && endDate != null) {
                query = query.whereGreaterThanOrEqualTo("timestamp", startDate)
                    .whereLessThanOrEqualTo("timestamp", endDate)
            }

            // Order by timestamp (mandatory for Firestore composite queries)
            query = query.orderBy("timestamp", Query.Direction.ASCENDING)

            query.get()
                .addOnSuccessListener { querySnapshot ->
                    // Map query results to a list of Transaction objects
                    val transactions = querySnapshot.documents.mapNotNull { document ->
                        document.toObject(Transaction::class.java)?.apply {
                            id = document.id // Store Firestore document ID
                            formattedDate = formatTimestamp(timestamp) // Format the timestamp
                        }
                    }
                    // Update the RecyclerView adapter
                    transactionAdapter.updateData(transactions)
                    transactionAdapter.sortByDate() // Update and show recent transactions
                }
                .addOnFailureListener { e ->
                    // Show a toast in case of failure
                    Toast.makeText(this, "Failed to load transactions: ${e.message}", Toast.LENGTH_LONG).show()
                }
        } else {
            Toast.makeText(this, "User is not logged in!", Toast.LENGTH_SHORT).show()
        }
    }


    private fun applyCurrentFilters() {
        loadTransactions(type = selectedType, startDate = startDate, endDate = endDate)
    }

    private fun filterTransactionsByDay() {
        startDate = getStartOfDay(System.currentTimeMillis())
        endDate = getEndOfDay(System.currentTimeMillis())
        applyCurrentFilters()
    }

    private fun filterTransactionsByWeek() {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        startDate = getStartOfDay(calendar.timeInMillis)

        calendar.add(Calendar.WEEK_OF_YEAR, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        endDate = calendar.timeInMillis

        applyCurrentFilters()
    }

    private fun filterTransactionsByMonth() {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        startDate = getStartOfDay(calendar.timeInMillis)

        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        endDate = calendar.timeInMillis

        applyCurrentFilters()
    }

    private fun filterTransactionsByYear() {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_YEAR, 1)
        startDate = getStartOfDay(calendar.timeInMillis)

        calendar.add(Calendar.YEAR, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        endDate = calendar.timeInMillis

        applyCurrentFilters()
    }

    private fun showDatePickerDialog() {
        // Create a MaterialDatePicker for a date range selection
        val dateRangePicker = com.google.android.material.datepicker.MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Select Date Range")
            .build()

        // Show the picker
        dateRangePicker.show(supportFragmentManager, "DATE_PICKER")

        // Handle the result when the user selects a range
        dateRangePicker.addOnPositiveButtonClickListener { selection ->
            val startCalendar = Calendar.getInstance()
            val endCalendar = Calendar.getInstance()

            val startMillis = selection?.first
            val endMillis = selection?.second

            if (startMillis != null && endMillis != null) {
                startCalendar.timeInMillis = startMillis
                endCalendar.timeInMillis = endMillis

                // Update startDate and endDate, and apply filters
                startDate = getStartOfDay(startMillis)
                endDate = getEndOfDay(endMillis)
                applyCurrentFilters()
            } else {
                Toast.makeText(this, "Invalid date range selected!", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun handleTransactionClick(transaction: Transaction) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.edit_transaction_dialog)

        val amountField = dialog.findViewById<EditText>(R.id.etTransactionAmount)
        val categoryField = dialog.findViewById<EditText>(R.id.etTransactionCategory)
        val radioGroup = dialog.findViewById<RadioGroup>(R.id.radioGroupTransactionType)
        val dateButton = dialog.findViewById<Button>(R.id.btnSelectDate)
        val saveButton = dialog.findViewById<Button>(R.id.btnSaveTransaction)
        val deleteButton = dialog.findViewById<Button>(R.id.btnDeleteTransaction)

        // Populate fields with current transaction data
        amountField.setText(transaction.amount.toString())
        categoryField.setText(transaction.category)
        when (transaction.type) {
            "Expense" -> radioGroup.check(R.id.radioExpense)
            "Income" -> radioGroup.check(R.id.radioIncome)
        }

        var selectedDate = transaction.timestamp
        dateButton.text = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(selectedDate))

        // Select a new date
        dateButton.setOnClickListener {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = selectedDate
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth, 0, 0, 0)
                    selectedDate = calendar.timeInMillis
                    dateButton.text = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(selectedDate))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        saveButton.setOnClickListener {
            val updatedAmount = amountField.text.toString().toDoubleOrNull()
            val updatedCategory = categoryField.text.toString().trim()
            val updatedType = when (radioGroup.checkedRadioButtonId) {
                R.id.radioExpense -> "Expense"
                R.id.radioIncome -> "Income"
                else -> transaction.type
            }

            if (updatedAmount != null && updatedCategory.isNotEmpty()) {
                val userId = auth.currentUser?.uid
                if (userId != null) {
                    val transactionRef = db.collection("Users").document(userId)
                        .collection("Transactions").document(transaction.id)

                    transactionRef.update(
                        mapOf(
                            "amount" to updatedAmount,
                            "category" to updatedCategory,
                            "type" to updatedType,
                            "timestamp" to selectedDate
                        )
                    ).addOnSuccessListener {
                        Toast.makeText(this, "Transaction updated!", Toast.LENGTH_SHORT).show()
                        applyCurrentFilters()
                        dialog.dismiss()
                    }.addOnFailureListener { e ->
                        Toast.makeText(this, "Error updating transaction: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Please fill all fields correctly!", Toast.LENGTH_SHORT).show()
            }
        }

        deleteButton.setOnClickListener {
            val userId = auth.currentUser?.uid
            if (userId != null) {
                val transactionRef = db.collection("Users").document(userId)
                    .collection("Transactions").document(transaction.id)

                transactionRef.delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Transaction deleted!", Toast.LENGTH_SHORT).show()
                        applyCurrentFilters()
                        dialog.dismiss()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error deleting transaction: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        dialog.show()
    }


}
