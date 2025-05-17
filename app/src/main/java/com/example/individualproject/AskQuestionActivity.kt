package com.example.individualproject

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class AskQuestionActivity : AppCompatActivity() {

    private lateinit var titleEditText: EditText
    private lateinit var bodyEditText: EditText
    private lateinit var postQuestionButton: Button
    private lateinit var backButton: ImageButton
    private var authorId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ask_question)

        titleEditText = findViewById(R.id.titleEditText)
        bodyEditText = findViewById(R.id.bodyEditText)
        postQuestionButton = findViewById(R.id.postQuestionButton)
        backButton = findViewById(R.id.backButton)

        val categorySpinner = findViewById<Spinner>(R.id.categorySpinner)
        val categories = listOf("General", "Math", "Science", "Programming", "Other")
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = categoryAdapter

        authorId = intent.getIntExtra("user_id", -1)

        backButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("user_id", authorId)
            startActivity(intent)
            finish()
        }

        postQuestionButton.setOnClickListener {
            val title = titleEditText.text.toString().trim()
            val body = bodyEditText.text.toString().trim()

            if (title.isNotEmpty() && body.isNotEmpty()) {
                postQuestion(title, body)
            } else {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun postQuestion(title: String, body: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("http://10.0.2.2:5000/ask_question")
                val connection = url.openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    connectTimeout = 5000
                    readTimeout = 5000
                    doOutput = true
                }

                val selectedCategory = findViewById<Spinner>(R.id.categorySpinner).selectedItem.toString()
                val json = JSONObject().apply {
                    put("title", title)
                    put("body", body)
                    put("author_id", authorId)
                    put("category", selectedCategory)
                    put("notify", true)
                }


                OutputStreamWriter(connection.outputStream).use { it.write(json.toString()) }

                val responseCode = connection.responseCode
                val responseMessage = if (responseCode == HttpURLConnection.HTTP_CREATED) {
                    "Question posted successfully"
                } else {
                    val errorReader = BufferedReader(InputStreamReader(connection.errorStream))
                    val errorResponse = errorReader.readText()
                    Log.e("AskQuestion", "Error response: $errorResponse")
                    "Failed to post: $responseCode"
                }

                runOnUiThread {
                    Toast.makeText(this@AskQuestionActivity, responseMessage, Toast.LENGTH_SHORT).show()
                    if (responseCode == HttpURLConnection.HTTP_CREATED) finish()
                }

                connection.disconnect()

            } catch (e: Exception) {
                Log.e("AskQuestion", "Exception", e)
                runOnUiThread {
                    Toast.makeText(this@AskQuestionActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}