package com.example.bekexpense

import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Suppress("DEPRECATION")
class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)


        setSystemUIFlags()

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val ivProfilePicture = findViewById<ImageView>(R.id.ivProfilePicture)
        val tvUserName = findViewById<TextView>(R.id.tvUserName)
        val tvUserBalance = findViewById<TextView>(R.id.tvUserBalance)
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        // Bottom Navigation
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_profile -> true
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
        bottomNavigationView.selectedItemId = R.id.nav_profile

        // Fetch and display user details
        fetchUserData(tvUserName, tvUserBalance)

        // Load profile picture
        loadProfilePicture(ivProfilePicture)

        // Logout button functionality
        btnLogout.setOnClickListener {
            showLogoutConfirmationDialog()
        }
    }

    private fun fetchUserData(tvUserName: TextView, tvUserBalance: TextView) {
        val userId = auth.currentUser?.uid
        userId?.let {
            db.collection("Users").document(it).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val userName = document.getString("name") ?: "Unknown User"
                        val initialBalance = document.getDouble("balance") ?: 0.0

                        tvUserName.text = userName
                        calculateTotalBalance(userId, initialBalance, tvUserBalance)
                    }
                }
                .addOnFailureListener {
                    tvUserName.text = "Error loading name"
                    tvUserBalance.text = "Error loading balance"
                }
        }
    }

    private fun calculateTotalBalance(userId: String, initialBalance: Double, tvUserBalance: TextView) {
        db.collection("Users").document(userId)
            .collection("Transactions")
            .get()
            .addOnSuccessListener { querySnapshot ->
                var totalExpenses = 0.0
                var totalIncome = 0.0

                for (transaction in querySnapshot.documents) {
                    val type = transaction.getString("type")
                    val amount = transaction.getDouble("amount") ?: 0.0

                    if (type == "Expense") {
                        totalExpenses += amount
                    } else if (type == "Income") {
                        totalIncome += amount
                    }
                }

                val totalBalance = initialBalance + totalIncome - totalExpenses
                tvUserBalance.text = "Balance: $%.2f".format(totalBalance)
            }
            .addOnFailureListener {
                tvUserBalance.text = "Error calculating balance"
            }
    }

    private fun loadProfilePicture(ivProfilePicture: ImageView) {
        val user = auth.currentUser
        val profilePictureUrl = user?.photoUrl // Google profile picture
        if (profilePictureUrl != null) {
            Glide.with(this).load(profilePictureUrl).into(ivProfilePicture) // Use Glide to load the image
        } else {
            ivProfilePicture.setImageResource(R.drawable.ic_placeholder_profile) // Default placeholder
        }
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Log Out")
            .setMessage("Do you really want to log out?")
            .setPositiveButton("Yes") { _, _ ->
                // Sign out from Firebase
                auth.signOut()

                // Sign out from Google
                val googleSignInClient = GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN)
                googleSignInClient.signOut().addOnCompleteListener {
                    // Redirect to login page
                    val intent = Intent(this, login::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
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
