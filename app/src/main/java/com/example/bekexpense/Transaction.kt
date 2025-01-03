package com.example.bekexpense

data class Transaction(
    var id: String = "",
    val amount: Double = 0.0,
    val category: String = "",
    val type: String = "",
    val timestamp: Long = 0L,
    var formattedDate: String = ""
)


