package com.example.individualproject

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

class VerifiedAnswersActivity : AppCompatActivity() {

    private lateinit var verifiedAnswersListView: ListView
    private lateinit var adapter: ArrayAdapter<String>
    private var userId: Int = -1
    private val answerList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verified_answers)

        verifiedAnswersListView = findViewById(R.id.verifiedAnswersListView)
        userId = intent.getIntExtra("user_id", -1)

        val backButton = findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener {
            finish()
        }

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, answerList)
        verifiedAnswersListView.adapter = adapter

        loadVerifiedAnswers()
    }


    private fun loadVerifiedAnswers() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL("http://10.0.2.2:5000/verified_answers/$userId")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                val response = connection.inputStream.bufferedReader().readText()
                val jsonArray = JSONArray(response)

                answerList.clear()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val questionTitle = obj.optString("title", "Untitled")
                    val answerBody = obj.optString("body", "")
                    answerList.add("Q: $questionTitle\nA: $answerBody")
                }

                withContext(Dispatchers.Main) {
                    adapter.notifyDataSetChanged()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@VerifiedAnswersActivity, "Error loading verified answers", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
