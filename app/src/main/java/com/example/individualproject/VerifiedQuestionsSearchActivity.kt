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

class VerifiedQuestionsSearchActivity : AppCompatActivity() {

    private lateinit var searchQueryEditText: EditText
    private lateinit var categorySpinner: Spinner
    private lateinit var searchResultsListView: ListView
    private lateinit var backButton: ImageButton

    private val questions = mutableListOf<JSONObject>()
    private lateinit var adapter: ArrayAdapter<String>
    private var userId: Int = -1
    private var selectedCategory: String = "All"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verified_questions_search)

        userId = intent.getIntExtra("user_id", -1)

        searchQueryEditText = findViewById(R.id.searchQueryEditText)
        categorySpinner = findViewById(R.id.categorySpinner)
        searchResultsListView = findViewById(R.id.searchResultsListView)
        backButton = findViewById(R.id.backButton)

        val categories = listOf("All", "General", "Math", "Science", "Programming", "Other")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = spinnerAdapter

        categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedCategory = categories[position]
                loadVerifiedQuestions(searchQueryEditText.text.toString())
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        searchResultsListView.adapter = adapter

        searchQueryEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                loadVerifiedQuestions(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        searchResultsListView.setOnItemClickListener { _, _, position, _ ->
            val selected = questions[position]
            val intent = Intent(this, QuestionDetailActivity::class.java)
            intent.putExtra("question", selected.toString())
            intent.putExtra("user_id", userId)
            startActivity(intent)
        }

        backButton.setOnClickListener {
            finish()
        }

        loadVerifiedQuestions("")
    }

    private fun loadVerifiedQuestions(query: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL("http://10.0.2.2:5000/get_questions")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                val response = connection.inputStream.bufferedReader().readText()
                val jsonArray = JSONArray(response)

                val filtered = mutableListOf<String>()
                questions.clear()

                for (i in 0 until jsonArray.length()) {
                    val q = jsonArray.getJSONObject(i)
                    val title = q.getString("title")
                    val category = q.optString("category", "")
                    val isVerified = q.optInt("verified", 0) == 1

                    val matchesQuery = title.contains(query, ignoreCase = true)
                    val matchesCategory = selectedCategory == "All" || selectedCategory.equals(category, ignoreCase = true)

                    if (isVerified && matchesQuery && matchesCategory) {
                        questions.add(q)
                        val createdAt = q.optString("created_at", "")
                        val display = "✔ $title\n⏱ ${formatDate(createdAt)} | 🏷 $category"
                        filtered.add(display)
                    }
                }

                withContext(Dispatchers.Main) {
                    adapter.clear()
                    adapter.addAll(filtered)
                    adapter.notifyDataSetChanged()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@VerifiedQuestionsSearchActivity, "Error loading questions", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun formatDate(raw: String): String {
        return raw.replace("T", " ").substringBefore(".")
    }
}
