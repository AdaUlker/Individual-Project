package com.example.individualproject

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class QuestionDetailActivity : AppCompatActivity() {

    private lateinit var questionTitle: TextView
    private lateinit var questionBody: TextView
    private lateinit var questionUsernameTextView: TextView
    private lateinit var answerButton: Button
    private lateinit var answersListView: ListView
    private lateinit var backButton: ImageButton
    private lateinit var deleteQuestionButton: Button
    private lateinit var verifyQuestionButton: Button
    private lateinit var verifiedBadge: TextView
    private lateinit var questionTimeTextView: TextView

    private lateinit var adapter: AnswerAdapter
    private val answersJsonList = mutableListOf<JSONObject>()

    private lateinit var questionData: JSONObject
    private var userId: Int = -1
    private var isInstructor = false
    private var isAdmin = false
    private var questionId: Int = -1
    private var focusAnswerId: Int = -1
    private var focusCommentId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_question_detail)

        questionTitle = findViewById(R.id.questionTitle)
        questionBody = findViewById(R.id.questionBody)
        questionUsernameTextView = findViewById(R.id.questionUsernameTextView)
        answerButton = findViewById(R.id.answerButton)
        answersListView = findViewById(R.id.answersListView)
        backButton = findViewById(R.id.backButton)
        deleteQuestionButton = findViewById(R.id.deleteQuestionButton)
        verifyQuestionButton = findViewById(R.id.verifyQuestionButton)
        verifiedBadge = findViewById(R.id.verifiedBadge)
        questionTimeTextView = findViewById(R.id.questionTimeTextView)

        userId = intent.getIntExtra("user_id", -1)
        val questionJson = intent.getStringExtra("question")
        questionData = JSONObject(questionJson ?: "{}")
        questionId = questionData.optInt("id")

        backButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("user_id", userId)
            startActivity(intent)
            finish()
        }

        focusAnswerId = intent.getIntExtra("focus_answer_id", -1)

        focusCommentId = intent.getIntExtra("focus_comment_id", -1)

        answerButton.setOnClickListener {
            val intent = Intent(this, AnswerSubmissionActivity::class.java)
            intent.putExtra("question_id", questionId)
            intent.putExtra("user_id", userId)
            startActivity(intent)
        }

        verifyQuestionButton.setOnClickListener {
            val currentVerified = questionData.optBoolean("verified", false)
            toggleQuestionVerification(questionId, !currentVerified)
        }

        deleteQuestionButton.setOnClickListener {
            AlertDialog.Builder(this).apply {
                setTitle("Delete Question")
                setMessage("Are you sure you want to delete this question and all related data?")
                setPositiveButton("Yes") { _, _ -> deleteQuestion(questionId) }
                setNegativeButton("Cancel", null)
                show()
            }
        }

        checkUserRoleAndRefresh()
    }

    override fun onResume() {
        super.onResume()
        checkUserRoleAndRefresh()
    }

    private fun checkUserRoleAndRefresh() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val userUrl = URL("http://10.0.2.2:5000/get_user_info/$userId")
                val userConn = userUrl.openConnection() as HttpURLConnection
                userConn.requestMethod = "GET"
                val userResponse = userConn.inputStream.bufferedReader().readText()
                val userJson = JSONObject(userResponse)
                isInstructor = userJson.optInt("is_instructor", 0) == 1
                isAdmin = userJson.optInt("is_admin", 0) == 1
                userConn.disconnect()

                val questionUrl = URL("http://10.0.2.2:5000/get_question_by_id/$questionId")
                val questionConn = questionUrl.openConnection() as HttpURLConnection
                questionConn.requestMethod = "GET"
                val questionResponse = questionConn.inputStream.bufferedReader().readText()
                questionData = JSONObject(questionResponse)
                questionConn.disconnect()

                val isVerified = questionData.optInt("verified", 0) == 1
                questionData.put("verified", isVerified)

                withContext(Dispatchers.Main) {
                    questionTitle.text = questionData.optString("title")
                    questionBody.text = questionData.optString("body")
                    questionUsernameTextView.text = "Asked by: ${questionData.optString("username", "user")}"
                    val createdAt = questionData.optString("created_at", "")
                    questionTimeTextView.text = "Asked on: ${formatDate(createdAt)}"


                    verifiedBadge.visibility = if (isVerified) View.VISIBLE else View.GONE
                    verifyQuestionButton.text = if (isVerified) "Unverify" else "Verify"
                    verifyQuestionButton.visibility = if (isInstructor || isAdmin) View.VISIBLE else View.GONE

                    deleteQuestionButton.visibility = if (questionData.optInt("author_id") == userId) View.VISIBLE else View.GONE

                    adapter = AnswerAdapter(
                        context = this@QuestionDetailActivity,
                        answers = answersJsonList,
                        userId = userId,
                        isInstructor = isInstructor,
                        isAdmin = isAdmin
                    )
                    answersListView.adapter = adapter

                    loadAnswers(questionId)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@QuestionDetailActivity, "Error loading data", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun toggleQuestionVerification(questionId: Int, verify: Boolean) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL("http://10.0.2.2:5000/verify_question")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                val json = JSONObject().apply {
                    put("question_id", questionId)
                    put("verified", verify)
                    put("verifier_id", userId)
                }

                OutputStreamWriter(connection.outputStream).use {
                    it.write(json.toString())
                }

                if (connection.responseCode == 200) {
                    questionData.put("verified", verify)
                    withContext(Dispatchers.Main) {
                        verifiedBadge.visibility = if (verify) View.VISIBLE else View.GONE
                        verifyQuestionButton.text = if (verify) "Unverify" else "Verify"
                    }
                }
                connection.disconnect()

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@QuestionDetailActivity, "Verification failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadAnswers(questionId: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL("http://10.0.2.2:5000/get_answers/$questionId")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                val response = connection.inputStream.bufferedReader().readText()
                val jsonArray = JSONArray(response)

                answersJsonList.clear()
                for (i in 0 until jsonArray.length()) {
                    answersJsonList.add(jsonArray.getJSONObject(i))
                }

                withContext(Dispatchers.Main) {
                    adapter.notifyDataSetChanged()

                    if (focusAnswerId != -1) {
                        val position = answersJsonList.indexOfFirst { it.optInt("id") == focusAnswerId }
                        if (position != -1) {
                            answersListView.post {
                                answersListView.setSelection(position)
                            }
                        }
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@QuestionDetailActivity, "Error loading answers", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun deleteQuestion(questionId: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL("http://10.0.2.2:5000/delete_question")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                val json = JSONObject().apply {
                    put("question_id", questionId)
                    put("user_id", userId)
                }

                OutputStreamWriter(connection.outputStream).use {
                    it.write(json.toString())
                }

                if (connection.responseCode == 200) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@QuestionDetailActivity, "Question deleted", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@QuestionDetailActivity, MainActivity::class.java)
                        intent.putExtra("user_id", userId)
                        startActivity(intent)
                        finish()
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@QuestionDetailActivity, "Failed to delete", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun formatDate(raw: String): String {
        return raw.replace("T", " ").substringBefore(".")
    }

}