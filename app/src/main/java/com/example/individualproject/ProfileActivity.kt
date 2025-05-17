package com.example.individualproject

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class ProfileActivity : AppCompatActivity() {

    private lateinit var profilePicture: ImageView
    private lateinit var nameTextView: TextView
    private lateinit var emailTextView: TextView
    private lateinit var editProfileButton: Button
    private lateinit var logoutButton: Button
    private lateinit var notificationSwitch: Switch
    private lateinit var backButton: ImageButton
    private lateinit var statsTextView: TextView
    private lateinit var roleTextView: TextView
    private lateinit var verifiedAnswersButton: Button

    private var currentName: String = ""
    private var currentEmail: String = ""
    private var currentPassword: String = ""
    private var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        profilePicture = findViewById(R.id.profilePicture)
        nameTextView = findViewById(R.id.nameTextView)
        emailTextView = findViewById(R.id.emailTextView)
        editProfileButton = findViewById(R.id.editProfileButton)
        logoutButton = findViewById(R.id.logoutButton)
        backButton = findViewById(R.id.backButton)
        statsTextView = findViewById(R.id.userStats)
        roleTextView = findViewById(R.id.roleTextView)
        verifiedAnswersButton = findViewById(R.id.verifiedAnswersButton)


        userId = intent.getIntExtra("user_id", -1)

        // Verileri yükle
        loadUserInfo()
        loadUserStats()

        editProfileButton.setOnClickListener {
            val intent = Intent(this, EditProfileActivity::class.java)
            intent.putExtra("user_id", userId)
            intent.putExtra("name", currentName)
            intent.putExtra("email", currentEmail)
            intent.putExtra("password", currentPassword)
            startActivityForResult(intent, 100)
        }

        logoutButton.setOnClickListener {
            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        backButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("user_id", userId)
            startActivity(intent)
            finish()
        }
    }

    private fun loadUserInfo() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL("http://10.0.2.2:5000/get_user_info/$userId")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                val response = connection.inputStream.bufferedReader().readText()
                val user = JSONObject(response)

                currentName = user.getString("username")
                currentEmail = user.getString("email")

                withContext(Dispatchers.Main) {
                    nameTextView.text = currentName
                    emailTextView.text = currentEmail

                    val isInstructor = user.optInt("is_instructor", 0) == 1
                    val isAdmin = user.optInt("is_admin", 0) == 1

                    if (isInstructor) {
                        roleTextView.text = "Role: Instructor"
                        roleTextView.visibility = View.VISIBLE
                    } else if (isAdmin) {
                        roleTextView.text = "Role: Admin"
                        roleTextView.visibility = View.VISIBLE
                    } else {
                        roleTextView.visibility = View.GONE
                    }
                }


                connection.disconnect()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ProfileActivity, "Error loading profile", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadUserInfo()
        loadUserStats()
    }

    private fun loadUserStats() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL("http://10.0.2.2:5000/get_user_stats/$userId")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                val response = connection.inputStream.bufferedReader().readText()
                val json = JSONObject(response)

                val stats = """
                    Questions Asked: ${json.getInt("questions")}
                    Answers Given: ${json.getInt("answers")}
                    Votes Received: ${json.getInt("votes")}
                    Verified Answers: ${json.getInt("verified")}
                    Comments Made: ${json.getInt("comments")}
                """.trimIndent()

                withContext(Dispatchers.Main) {
                    statsTextView.text = stats

                    if (json.getInt("verified") > 0) {
                        verifiedAnswersButton.visibility = View.VISIBLE
                        verifiedAnswersButton.setOnClickListener {
                            val intent = Intent(this@ProfileActivity, VerifiedAnswersActivity::class.java)
                            intent.putExtra("user_id", userId)
                            startActivity(intent)
                        }
                    } else {
                        verifiedAnswersButton.visibility = View.GONE
                    }
                }


            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ProfileActivity, "Error loading stats", Toast.LENGTH_SHORT).show()
                }
            }
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == Activity.RESULT_OK) {
            val updatedName = data?.getStringExtra("updatedName")
            val updatedEmail = data?.getStringExtra("updatedEmail")
            val updatedPassword = data?.getStringExtra("updatedPassword")

            if (updatedName != null) {
                currentName = updatedName
                nameTextView.text = currentName
            }

            if (updatedEmail != null) {
                currentEmail = updatedEmail
                emailTextView.text = currentEmail
            }

            if (updatedPassword != null) {
                currentPassword = updatedPassword
            }
        }
    }
}
