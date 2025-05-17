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

class VerifiedAnswersSearchActivity : AppCompatActivity() {

    private lateinit var searchQueryEditText: EditText
    private lateinit var categorySpinner: Spinner
    private lateinit var searchResultsListView: ListView
    private lateinit var backButton: ImageButton

    private val results = mutableListOf<JSONObject>()
    private lateinit var adapter: ArrayAdapter<String>
    private var userId: Int = -1
    private var selectedCategory: String = "All"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verified_answers_search)

        userId = intent.getIntExtra("user_id", -1)

        searchQueryEditText = findViewById(R.id.searchQueryEditText)
        categorySpinner = findViewById(R.id.categorySpinner)
        searchResultsListView = findViewById(R.id.searchResultsListView)
        backButton = findViewById(R.id.backButton)

        val categories = listOf("All", "General", "Math", "Science", "Programming", "Other")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = spinnerAdapter

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        searchResultsListView.adapter = adapter

        categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedCategory = categories[position]
                loadResults(searchQueryEditText.text.toString())
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        searchQueryEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                loadResults(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        backButton.setOnClickListener {
            finish()
        }

        searchResultsListView.setOnItemClickListener { _, _, position, _ ->
            val selected = results[position]
            val questionId = selected.optInt("question_id", -1)
            val questionTitle = selected.optString("question_title", "")
            val questionVerified = selected.optInt("question_verified", 0)
            val questionCategory = selected.optString("category", "")
            val questionBody = selected.optString("question_body", "No body available")

            val questionJson = JSONObject().apply {
                put("id", questionId)
                put("title", questionTitle)
                put("body", questionBody)
                put("verified", questionVerified)
                put("category", questionCategory)
            }

            val intent = Intent(this, QuestionDetailActivity::class.java)
            intent.putExtra("question", questionJson.toString())
            intent.putExtra("user_id", userId)
            startActivity(intent)
        }

        loadResults("")
    }

    private fun loadResults(query: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL("http://10.0.2.2:5000/search_verified_answers")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                val response = connection.inputStream.bufferedReader().readText()
                val jsonArray = JSONArray(response)

                results.clear()
                val filtered = mutableListOf<String>()

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val answer = obj.optString("answer_body", "")
                    val question = obj.optString("question_title", "")
                    val category = obj.optString("category", "")
                    val questionVerified = obj.optInt("question_verified", 0) == 1

                    val fullText = buildString {
                        append("Q: ${if (questionVerified) "✔ " else ""}$question\n")
                        append("A: $answer\n")
                        append("🏷 $category")
                    }

                    val matchesQuery = fullText.contains(query, ignoreCase = true)
                    val matchesCategory = selectedCategory == "All" || selectedCategory.equals(category, ignoreCase = true)

                    if (matchesQuery && matchesCategory) {
                        results.add(obj)
                        filtered.add(fullText)
                    }
                }

                withContext(Dispatchers.Main) {
                    adapter.clear()
                    adapter.addAll(filtered)
                    adapter.notifyDataSetChanged()
                }

                connection.disconnect()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@VerifiedAnswersSearchActivity, "Error loading answers", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
