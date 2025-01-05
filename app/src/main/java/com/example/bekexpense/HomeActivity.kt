package com.example.bekexpense


import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import androidx.core.view.WindowInsetsControllerCompat
import android.view.Window
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import com.github.mikephil.charting.formatter.PercentFormatter
import com.google.android.material.tabs.TabLayout
import java.util.*

@Suppress("DEPRECATION")
class HomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var pieChart: PieChart

    private var selectedType: String = "Expense" // Default to Expense
    private var selectedTimestamp: Long = System.currentTimeMillis() // Default to current time


    @SuppressLint("WrongViewCast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)


        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setSystemUIFlags()

        // Initialize PieChart
        pieChart = findViewById(R.id.pieChart)

        // Set up the PieChart
        setupPieChart()

        calculateTotalBalance()

        val startOfDay = getStartOfDay(System.currentTimeMillis())
        val endOfDay = getEndOfDay(System.currentTimeMillis())
        // Load data into the PieChart
        loadChartData("Expense", startOfDay, endOfDay)

        setupTransactionTypeTab() // Initialize transaction type tabs (Expense/Income)
        setupTimeFilterTab() // Initialize time filter tabs (Day, Week, etc.)


        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    // Stay on Home
                    true
                }
                R.id.nav_profile -> {
                    // Navigate to ProfileActivity
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                R.id.nav_settings -> {
                    // Navigate to SettingsActivity
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }

        // Default Selection
        bottomNavigationView.selectedItemId = R.id.nav_home

        // Other Button Listeners
        findViewById<ImageView>(R.id.btnAllTransactions).setOnClickListener {
            startActivity(Intent(this, AllTransactionsActivity::class.java))
        }

        findViewById<ImageView>(R.id.btnAddTransaction).setOnClickListener {
            showAddTransactionDialog()
        }
        findViewById<TabLayout>(R.id.tabLayoutTransactionType).addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                selectedType = when (tab?.text) {
                    "Expenses" -> "Expense"
                    "Income" -> "Income"
                    else -> "Expense" // Default to Expense
                }

                // Trigger data loading for the currently selected period
                val startOfDay = getStartOfDay(System.currentTimeMillis())
                val endOfDay = getEndOfDay(System.currentTimeMillis())
                loadChartData(selectedType, startOfDay, endOfDay)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        findViewById<TabLayout>(R.id.tabLayoutFilter).addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                // Determine the selected transaction type from the "Transaction Type" TabLayout
                val selectedTypeTab = findViewById<TabLayout>(R.id.tabLayoutTransactionType).selectedTabPosition
                val selectedType = when (selectedTypeTab) {
                    0 -> "Expense" // First tab is for Expenses
                    1 -> "Income" // Second tab is for Income
                    else -> "Expense" // Default to Expense
                }

                when (tab?.text) {
                    "Day" -> {
                        val startOfDay = getStartOfDay(System.currentTimeMillis())
                        val endOfDay = getEndOfDay(System.currentTimeMillis())
                        loadChartData(selectedType, startOfDay, endOfDay)
                    }
                    "Week" -> {
                        val calendar = Calendar.getInstance()
                        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                        val startOfWeek = calendar.timeInMillis
                        calendar.add(Calendar.WEEK_OF_YEAR, 1)
                        calendar.add(Calendar.MILLISECOND, -1)
                        val endOfWeek = calendar.timeInMillis
                        loadChartData(selectedType, startOfWeek, endOfWeek)
                    }
                    "Month" -> {
                        val calendar = Calendar.getInstance()
                        calendar.set(Calendar.DAY_OF_MONTH, 1)
                        val startOfMonth = calendar.timeInMillis
                        calendar.add(Calendar.MONTH, 1)
                        calendar.add(Calendar.MILLISECOND, -1)
                        val endOfMonth = calendar.timeInMillis
                        loadChartData(selectedType, startOfMonth, endOfMonth)
                    }
                    "Year" -> {
                        val calendar = Calendar.getInstance()
                        calendar.set(Calendar.DAY_OF_YEAR, 1)
                        val startOfYear = calendar.timeInMillis
                        calendar.add(Calendar.YEAR, 1)
                        calendar.add(Calendar.MILLISECOND, -1)
                        val endOfYear = calendar.timeInMillis
                        loadChartData(selectedType, startOfYear, endOfYear)
                    }
                    "Period" -> showDatePickerDialog() // Use custom date range
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })


        // All transaction btn
        val allTransactionsButton = findViewById<ImageView>(R.id.btnAllTransactions)
        allTransactionsButton.setOnClickListener {
            val intent = Intent(this, AllTransactionsActivity::class.java)
            startActivity(intent)
        }

        // Reference Add Transaction Button
        val addTransactionButton = findViewById<ImageView>(R.id.btnAddTransaction)
        addTransactionButton.setOnClickListener {
            showAddTransactionDialog()
        }
    }

    private fun calculateTotalBalance() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            // Fetch the user's current balance from Firestore
            db.collection("Users").document(userId).get()
                .addOnSuccessListener { document ->
                    val startingBalance = document.getDouble("balance") ?: 0.0

                    db.collection("Users").document(userId)
                        .collection("Transactions")
                        .get()
                        .addOnSuccessListener { querySnapshot ->
                            var totalIncome = 0.0
                            var totalExpense = 0.0

                            // Calculate total income and expenses from transactions
                            for (transaction in querySnapshot.documents) {
                                val type = transaction.getString("type") ?: "Expense"
                                val amount = transaction.getDouble("amount") ?: 0.0

                                if (type == "Income") {
                                    totalIncome += amount
                                } else {
                                    totalExpense += amount
                                }
                            }

                            // Calculate final balance
                            val totalBalance = startingBalance + totalIncome - totalExpense
                            updateTotalBalanceUI(totalBalance)
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error loading transactions: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error loading user data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun updateTotalBalanceUI(balance: Double) {
        val totalBalanceTextView = findViewById<TextView>(R.id.tvTotalBalance)
        totalBalanceTextView.text = "TOTAL: $${String.format("%.2f", balance)}"
    }



    private fun setupTransactionTypeTab() {
        findViewById<TabLayout>(R.id.tabLayoutTransactionType).addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                selectedType = when (tab?.text) {
                    "Expenses" -> "Expense"
                    "Income" -> "Income"
                    else -> "Expense" // Default to Expense
                }
                loadChartData(selectedType)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

    }

    private fun setupTimeFilterTab() {
        findViewById<TabLayout>(R.id.tabLayoutFilter).addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val now = System.currentTimeMillis()
                when (tab?.text) {
                    "Day" -> loadChartData(selectedType, getStartOfDay(now), getEndOfDay(now))
                    "Week" -> loadChartData(selectedType, getStartOfWeek(), getEndOfDay(now))
                    "Month" -> loadChartData(selectedType, getStartOfMonth(), getEndOfDay(now))
                    "Year" -> loadChartData(selectedType, getStartOfYear(), getEndOfDay(now))
                    "Period" -> showDatePickerDialog()
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupPieChart() {
        pieChart.isDrawHoleEnabled = true // Enable the hole in the center
        pieChart.setHoleColor(Color.WHITE) // Set hole color
        pieChart.setEntryLabelColor(Color.BLACK) // Set entry labels color
        pieChart.setEntryLabelTextSize(12f) // Set text size for labels
        pieChart.description.isEnabled = false // Disable chart description
        pieChart.legend.isEnabled = false // Disable the chart legend
    }
    private fun loadChartData(type: String, startDate: Long? = null, endDate: Long? = null) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            var query = db.collection("Users").document(userId)
                .collection("Transactions")
                .whereEqualTo("type", type) // Filter by type (Expense or Income)

            // Apply date range filter if provided
            if (startDate != null && endDate != null) {
                query = query.whereGreaterThanOrEqualTo("timestamp", startDate)
                    .whereLessThanOrEqualTo("timestamp", endDate)
            }

            query.get()
                .addOnSuccessListener { querySnapshot ->
                    val categoryTotals = mutableMapOf<String, Double>()
                    var totalAmount = 0.0 // Initialize the total amount

                    for (document in querySnapshot.documents) {
                        val category = document.getString("category") ?: "Other"
                        val amount = document.getDouble("amount") ?: 0.0
                        categoryTotals[category] = categoryTotals.getOrDefault(category, 0.0) + amount
                        totalAmount += amount // Sum up the total amount
                    }

                    // Update PieChart and total amount in TextView
                    updatePieChart(categoryTotals)
                    findViewById<TextView>(R.id.tvTotalAmount).text = "$${"%.2f".format(totalAmount)}"
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to load data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "User is not logged in!", Toast.LENGTH_SHORT).show()
        }
    }




    private fun updatePieChart(categoryTotals: Map<String, Double>) {
        val entries = mutableListOf<PieEntry>()
        val colors = mutableListOf<Int>()

        categoryTotals.forEach { (category, total) ->
            entries.add(PieEntry(total.toFloat(), category))
            colors.add(generateRandomColor())
        }

        val dataSet = PieDataSet(entries, "Categories")
        dataSet.colors = colors
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = Color.BLACK

        val data = PieData(dataSet)
        data.setValueFormatter(PercentFormatter(pieChart))

        pieChart.data = data
        pieChart.setUsePercentValues(true)
        pieChart.invalidate() // Refresh the chart
    }


    private fun generateRandomColor(): Int {
        val random = Random()
        return Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256))
    }




    private fun getStartOfDay(timestamp: Long): Long = Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun getEndOfDay(timestamp: Long): Long = Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }.timeInMillis

    private fun getStartOfWeek(): Long = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun getStartOfMonth(): Long = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun getStartOfYear(): Long = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_YEAR, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun showDatePickerDialog() {
        val dateRangePicker = com.google.android.material.datepicker.MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Select Date Range")
            .build()

        dateRangePicker.show(supportFragmentManager, "DATE_PICKER")
        dateRangePicker.addOnPositiveButtonClickListener { selection ->
            val startDate = selection?.first ?: getStartOfDay(System.currentTimeMillis())
            val endDate = selection?.second ?: getEndOfDay(System.currentTimeMillis())
            loadChartData(selectedType, startDate, endDate) // Pass selectedType here
        }
    }




    private fun showAddTransactionDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.add_transaction_dialog)

        val radioGroup = dialog.findViewById<RadioGroup>(R.id.radioGroupTransactionType)
        val amountField = dialog.findViewById<EditText>(R.id.etTransactionAmount)
        val categoryField = dialog.findViewById<EditText>(R.id.etTransactionCategory)
        val dateButton = dialog.findViewById<Button>(R.id.btnSelectDate)
        val saveButton = dialog.findViewById<Button>(R.id.btnSaveTransaction)

        // Set default date on the date button
        dateButton.text = formatTimestamp(selectedTimestamp)

        // Date picker dialog
        dateButton.setOnClickListener {
            openDatePicker { timestamp ->
                selectedTimestamp = timestamp
                dateButton.text = formatTimestamp(selectedTimestamp) // Update date button text
            }
        }

        saveButton.setOnClickListener {
            val selectedTypeId = radioGroup.checkedRadioButtonId
            val transactionType = when (selectedTypeId) {
                R.id.radioExpense -> "Expense"
                R.id.radioIncome -> "Income"
                else -> null
            }

            val amount = amountField.text.toString().toDoubleOrNull()
            val category = categoryField.text.toString().trim()

            if (transactionType != null && amount != null && category.isNotEmpty()) {
                val userId = auth.currentUser?.uid
                if (userId != null) {
                    val transaction = hashMapOf(
                        "type" to transactionType,
                        "amount" to amount,
                        "category" to category,
                        "timestamp" to selectedTimestamp
                    )
                    db.collection("Users").document(userId)
                        .collection("Transactions")
                        .add(transaction)
                        .addOnSuccessListener {
                            Toast.makeText(this@HomeActivity, "Transaction added!", Toast.LENGTH_SHORT).show()
                            calculateTotalBalance() // Update the total balance dynamically
                            // Reload the chart data to reflect the new transaction
                            val startOfDay = getStartOfDay(System.currentTimeMillis())
                            val endOfDay = getEndOfDay(System.currentTimeMillis())
                            loadChartData(selectedType, startOfDay, endOfDay)
                            dialog.dismiss() // Close the dialog
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this@HomeActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            } else {
                Toast.makeText(this, "Please fill all fields correctly!", Toast.LENGTH_SHORT).show()
            }
        }


        dialog.show()
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

    private fun openDatePicker(onDateSelected: (Long) -> Unit) {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = selectedTimestamp

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            calendar.set(selectedYear, selectedMonth, selectedDay, 0, 0, 0)
            onDateSelected(calendar.timeInMillis)
        }, year, month, day).show()
    }

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

//    private fun saveTransactionToFirestore(type: String, amount: Double, category: String, timestamp: Long) {
//        val userId = auth.currentUser?.uid
//        println(userId)
//        if (userId != null) {
//            val transaction = hashMapOf(
//                "type" to type,
//                "amount" to amount,
//                "category" to category,
//                "timestamp" to timestamp
//            )
//            db.collection("Users").document(userId)
//                .collection("Transactions")
//                .add(transaction)
//                .addOnSuccessListener {
//                    Toast.makeText(this, "Transaction added!", Toast.LENGTH_SHORT).show()
//                    // TODO: Update UI or refresh data dynamically
//                }
//                .addOnFailureListener { e ->
//                    println(e.message)
//                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
//                }
//        }
//    }
}

