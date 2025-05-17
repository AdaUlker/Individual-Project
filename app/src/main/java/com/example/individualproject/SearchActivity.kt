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

class SearchActivity : AppCompatActivity() {

    private lateinit var searchQueryEditText: EditText
    private lateinit var categorySpinner: Spinner
    private lateinit var searchResultsListView: ListView
    private lateinit var verifiedQuestionsButton: Button
    private lateinit var verifiedAnswersButton: Button
    private lateinit var backButton: ImageButton

    private lateinit var adapter: ArrayAdapter<String>
    private val questionList = mutableListOf<JSONObject>()
    private var userId: Int = -1
    private var selectedCategory = "All"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_activity)

        searchQueryEditText = findViewById(R.id.searchQueryEditText)
        categorySpinner = findViewById(R.id.categorySpinner)
        searchResultsListView = findViewById(R.id.searchResultsListView)
        verifiedQuestionsButton = findViewById(R.id.verifiedQuestionsButton)
        verifiedAnswersButton = findViewById(R.id.verifiedAnswersButton)
        backButton = findViewById(R.id.backButton)

        userId = intent.getIntExtra("user_id", -1)

        val categories = listOf("All", "General", "Math", "Science", "Programming", "Other")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = spinnerAdapter

        categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedCategory = categories[position]
                loadSearchResults()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        searchResultsListView.adapter = adapter

        searchQueryEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = loadSearchResults()
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        searchResultsListView.setOnItemClickListener { _, _, position, _ ->
            val selectedQuestion = questionList[position]
            val intent = Intent(this, QuestionDetailActivity::class.java)
            intent.putExtra("question", selectedQuestion.toString())
            intent.putExtra("user_id", userId)
            startActivity(intent)
        }

        verifiedQuestionsButton.setOnClickListener {
            val intent = Intent(this, VerifiedQuestionsSearchActivity::class.java)
            intent.putExtra("user_id", userId)
            startActivity(intent)
        }

        verifiedAnswersButton.setOnClickListener {
            val intent = Intent(this, VerifiedAnswersSearchActivity::class.java)
            intent.putExtra("user_id", userId)
            startActivity(intent)
        }

        backButton.setOnClickListener {
            finish()
        }

        loadSearchResults()
    }

    private fun loadSearchResults() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL("http://10.0.2.2:5000/get_questions")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                val response = connection.inputStream.bufferedReader().readText()
                val jsonArray = JSONArray(response)

                questionList.clear()
                val query = searchQueryEditText.text.toString().trim()
                val displayList = mutableListOf<String>()

                for (i in 0 until jsonArray.length()) {
                    val q = jsonArray.getJSONObject(i)
                    val title = q.getString("title")
                    val verified = q.optInt("verified", 0) == 1
                    val category = q.optString("category", "")
                    val createdAt = q.optString("created_at", "")

                    val matchesCategory = (selectedCategory == "All" || selectedCategory.equals(category, ignoreCase = true))
                    val matchesQuery = title.contains(query, ignoreCase = true)

                    if (matchesCategory && matchesQuery) {
                        questionList.add(q)
                        val displayTitle = (if (verified) "✔ " else "") + title + "\n⏱ ${createdAt.replace("T", " ").substringBefore(".")} | 🏷 $category"
                        displayList.add(displayTitle)
                    }
                }

                withContext(Dispatchers.Main) {
                    adapter.clear()
                    adapter.addAll(displayList)
                    adapter.notifyDataSetChanged()
                }

                connection.disconnect()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SearchActivity, "Error loading search results", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
