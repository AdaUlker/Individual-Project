package com.example.individualproject

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class EditProfileActivity : AppCompatActivity() {

    private lateinit var profilePicture: ImageView
    private lateinit var nameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var backButton: ImageButton

    private var userId: Int = -1
    private val PICK_IMAGE_REQUEST = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        profilePicture = findViewById(R.id.editProfilePicture)
        nameEditText = findViewById(R.id.editNameEditText)
        emailEditText = findViewById(R.id.editEmailEditText)
        passwordEditText = findViewById(R.id.editPasswordEditText)
        saveButton = findViewById(R.id.saveEditProfileButton)
        backButton = findViewById(R.id.backButton)

        // Intent'ten gelen veriler
        userId = intent.getIntExtra("user_id", -1)
        nameEditText.setText(intent.getStringExtra("name") ?: "")
        emailEditText.setText(intent.getStringExtra("email") ?: "")
        passwordEditText.setText(intent.getStringExtra("password") ?: "")

        profilePicture.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        backButton.setOnClickListener {
            finish() // Değişiklikleri kaydetmeden geri dön
        }

        saveButton.setOnClickListener {
            val updatedName = nameEditText.text.toString()
            val updatedEmail = emailEditText.text.toString()
            val updatedPassword = passwordEditText.text.toString()

            updateUser(userId, updatedName, updatedEmail, updatedPassword)
        }
    }

    private fun updateUser(userId: Int, name: String, email: String, password: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL("http://10.0.2.2:5000/update_user")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                val json = JSONObject().apply {
                    put("id", userId)
                    put("username", name)
                    put("email", email)
                    put("password", password)
                }

                OutputStreamWriter(connection.outputStream).use {
                    it.write(json.toString())
                }

                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@EditProfileActivity, "Profile updated", Toast.LENGTH_SHORT).show()
                        val resultIntent = Intent().apply {
                            putExtra("updatedName", name)
                            putExtra("updatedEmail", email)
                            putExtra("updatedPassword", password)
                        }
                        setResult(Activity.RESULT_OK, resultIntent)
                        finish()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@EditProfileActivity, "Update failed", Toast.LENGTH_SHORT).show()
                    }
                }

                connection.disconnect()

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EditProfileActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            val selectedImageUri: Uri? = data?.data
            if (selectedImageUri != null) {
                profilePicture.setImageURI(selectedImageUri)
            }
        }
    }
}
