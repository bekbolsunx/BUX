package com.example.bekexpense

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class BalanceActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_balance)


        setSystemUIFlags()

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // UI References
        val balanceField = findViewById<EditText>(R.id.etBalance)
        val submitButton = findViewById<Button>(R.id.btnSubmitBalance)

        submitButton.setOnClickListener {
            val balance = balanceField.text.toString().trim()

            if (balance.isNotEmpty()) {
                val balanceValue = balance.toDoubleOrNull()
                if (balanceValue != null && balanceValue >= 0) {
                    val userId = auth.currentUser?.uid
                    userId?.let {
                        db.collection("Users").document(it)
                            .update("balance", balanceValue)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Balance saved successfully!", Toast.LENGTH_SHORT).show()
                                // Navigate to Home Page
                                val homeIntent = Intent(this, HomeActivity::class.java)
                                startActivity(homeIntent)
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Error saving balance: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    Toast.makeText(this, "Enter a valid balance!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please enter your balance!", Toast.LENGTH_SHORT).show()
            }
        }
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
}
