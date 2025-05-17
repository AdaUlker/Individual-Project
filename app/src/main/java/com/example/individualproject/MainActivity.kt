package com.example.individualproject

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var questionListView: ListView
    private lateinit var askQuestionButton: Button
    private lateinit var profileButton: Button
    private lateinit var notificationButton: ImageButton
    private lateinit var manageInstructorsButton: Button

    private lateinit var adapter: ArrayAdapter<String>
    private var questionList = mutableListOf<JSONObject>()
    private var userId: Int = -1

    private lateinit var categoryFilterSpinner: Spinner
    private var selectedCategory: String = "All"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        userId = intent.getIntExtra("user_id", -1)

        questionListView = findViewById(R.id.questionListView)
        askQuestionButton = findViewById(R.id.askQuestionButton)
        profileButton = findViewById(R.id.profileButton)
        notificationButton = findViewById(R.id.notificationButton)
        manageInstructorsButton = findViewById(R.id.manageInstructorsButton)
        categoryFilterSpinner = findViewById(R.id.categoryFilterSpinner)
        val searchButton = findViewById<Button>(R.id.searchButton) // Yeni Search butonu

        // Spinner için kategori listesi
        val categories = listOf("All", "General", "Math", "Science", "Programming", "Other")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categoryFilterSpinner.adapter = spinnerAdapter

        categoryFilterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedCategory = categories[position]
                loadQuestions("")
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        questionListView.adapter = adapter

        askQuestionButton.setOnClickListener {
            val intent = Intent(this, AskQuestionActivity::class.java)
            intent.putExtra("user_id", userId)
            startActivity(intent)
        }

        profileButton.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            intent.putExtra("user_id", userId)
            startActivity(intent)
        }

        notificationButton.setOnClickListener {
            val intent = Intent(this, NotificationActivity::class.java)
            intent.putExtra("user_id", userId)
            startActivity(intent)
        }

        manageInstructorsButton.setOnClickListener {
            val intent = Intent(this, InstructorManagementActivity::class.java)
            intent.putExtra("user_id", userId)
            startActivity(intent)
        }

        searchButton.setOnClickListener {
            val intent = Intent(this, SearchActivity::class.java)
            intent.putExtra("user_id", userId)
            startActivity(intent)
        }

        questionListView.setOnItemClickListener { _, _, position, _ ->
            val selectedQuestion = questionList[position]
            val intent = Intent(this, QuestionDetailActivity::class.java)
            intent.putExtra("question", selectedQuestion.toString())
            intent.putExtra("user_id", userId)
            startActivity(intent)
        }

        checkIfAdmin()
        loadQuestions("")
    }

    private fun checkIfAdmin() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL("http://10.0.2.2:5000/get_user_info/$userId")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                val response = connection.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                val isAdmin = json.optInt("is_admin", 0) == 1

                withContext(Dispatchers.Main) {
                    manageInstructorsButton.visibility = View.VISIBLE
                }


            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Failed to check admin", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadQuestions("")
    }

    private fun loadQuestions(query: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL("http://10.0.2.2:5000/get_questions")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                val response = connection.inputStream.bufferedReader().readText()
                val jsonArray = JSONArray(response)

                questionList.clear()
                val filteredTitles = mutableListOf<String>()

                for (i in 0 until jsonArray.length()) {
                    val question = jsonArray.getJSONObject(i)
                    val isVerified = question.optInt("verified", 0) == 1
                    val rawTitle = question.getString("title")
                    val category = question.optString("category", "")
                    val finalTitle = if (isVerified) "✔ $rawTitle" else rawTitle

                    val matchesQuery = finalTitle.contains(query, ignoreCase = true)
                    val matchesCategory = (selectedCategory == "All" || selectedCategory.equals(category, ignoreCase = true))

                    if (matchesQuery && matchesCategory) {
                        questionList.add(question)
                        val createdAt = question.optString("created_at", "")
                        val displayTitle = "$finalTitle\n⏱ ${formatDate(createdAt)} | 🏷 $category"
                        filteredTitles.add(displayTitle)
                    }
                }

                withContext(Dispatchers.Main) {
                    adapter.clear()
                    adapter.addAll(filteredTitles)
                    adapter.notifyDataSetChanged()
                }

                connection.disconnect()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(applicationContext, "Error loading questions", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun formatDate(raw: String): String {
        return raw.replace("T", " ").substringBefore(".")
    }

}


