package com.example.individualproject

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class AnswerSubmissionActivity : AppCompatActivity() {

    private lateinit var answerEditText: EditText
    private lateinit var submitAnswerButton: Button
    private lateinit var backButton: ImageButton
    private var userId: Int = -1  // Dinamik userId
    private var questionId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_answer_submission)

        answerEditText = findViewById(R.id.answerEditText)
        submitAnswerButton = findViewById(R.id.submitAnswerButton)
        backButton = findViewById(R.id.backButton)

        questionId = intent.getIntExtra("question_id", -1)
        userId = intent.getIntExtra("user_id", -1)

        backButton.setOnClickListener {
            finish()
        }

        submitAnswerButton.setOnClickListener {
            val answerText = answerEditText.text.toString().trim()

            if (answerText.isNotEmpty()) {
                postAnswer(questionId, userId, answerText)
            } else {
                Toast.makeText(this, "Answer cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun postAnswer(questionId: Int, userId: Int, answer: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("http://10.0.2.2:5000/post_answer")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                val json = JSONObject().apply {
                    put("question_id", questionId)
                    put("body", answer)
                    put("author_id", userId)
                }

                val writer = OutputStreamWriter(connection.outputStream)
                writer.write(json.toString())
                writer.flush()

                val responseCode = connection.responseCode
                runOnUiThread {
                    if (responseCode in 200..299) {
                        Toast.makeText(this@AnswerSubmissionActivity, "Answer submitted", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@AnswerSubmissionActivity, "Submission failed", Toast.LENGTH_SHORT).show()
                    }
                }

                connection.disconnect()
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@AnswerSubmissionActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}