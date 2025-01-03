package com.example.bekexpense

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Suppress("DEPRECATION")
class SplashActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setSystemUIFlags()

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val currentUser = auth.currentUser

        if (currentUser == null) {
            // No user is logged in, navigate to Login Page
            navigateToLogin()
        } else {
            // Check Firestore for user data
            val userId = currentUser.uid
            db.collection("Users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val balance = document.getDouble("balance")
                        if (balance == null || balance == 0.0) {
                            // User exists but balance is missing
                            navigateToBalance()
                        } else {
                            // User data is complete, navigate to Home Page
                            navigateToHome()
                        }
                    } else {
                        // User doesn't exist in Firestore
                        navigateToLogin()
                    }
                }
                .addOnFailureListener {
                    // Handle database errors
                    navigateToLogin()
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


    private fun navigateToLogin() {
        val loginIntent = Intent(this, login::class.java)
        startActivity(loginIntent)
        finish()
    }

    private fun navigateToBalance() {
        val balanceIntent = Intent(this, BalanceActivity::class.java)
        startActivity(balanceIntent)
        finish()
    }

    private fun navigateToHome() {
        val homeIntent = Intent(this, HomeActivity::class.java)
        startActivity(homeIntent)
        finish()
    }
}
