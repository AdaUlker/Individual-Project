package com.example.individualproject

import android.os.Bundle
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

class CommentActivity : AppCompatActivity() {

    private lateinit var commentListView: ListView
    private lateinit var commentEditText: EditText
    private lateinit var sendCommentButton: Button
    private lateinit var backButton: ImageButton
    private lateinit var adapter: BaseAdapter

    private var answerId: Int = -1
    private var userId: Int = -1
    private val comments = mutableListOf<JSONObject>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comment)

        commentListView = findViewById(R.id.commentListView)
        commentEditText = findViewById(R.id.commentEditText)
        sendCommentButton = findViewById(R.id.sendCommentButton)
        backButton = findViewById(R.id.backButton)

        answerId = intent.getIntExtra("answer_id", -1)
        userId = intent.getIntExtra("user_id", -1)

        adapter = object : BaseAdapter() {
            override fun getCount(): Int = comments.size
            override fun getItem(position: Int): Any = comments[position]
            override fun getItemId(position: Int): Long = position.toLong()

            override fun getView(position: Int, convertView: android.view.View?, parent: android.view.ViewGroup): android.view.View {
                val inflater = layoutInflater
                val view = convertView ?: inflater.inflate(R.layout.comment_item, parent, false)

                val comment = comments[position]
                val body = comment.optString("body", "")
                val username = comment.optString("username", "Unknown")
                val createdAt = comment.optString("created_at", "")

                val bodyText = view.findViewById<TextView>(R.id.commentBodyTextView)
                val authorText = view.findViewById<TextView>(R.id.commentAuthorTextView)
                val timeText = view.findViewById<TextView>(R.id.commentTimeTextView)

                bodyText.text = body
                authorText.text = "By: $username"
                timeText.text = createdAt

                view.setOnLongClickListener {
                    val commentUserId = comment.optInt("author_id", -1)
                    if (commentUserId == userId) {
                        deleteComment(comment.getInt("id"))
                    } else {
                        Toast.makeText(this@CommentActivity, "You can only delete your own comments", Toast.LENGTH_SHORT).show()
                    }
                    true
                }

                return view
            }
        }


        commentListView.adapter = adapter

        backButton.setOnClickListener {
            finish()
        }

        sendCommentButton.setOnClickListener {
            val body = commentEditText.text.toString().trim()
            if (body.isNotEmpty()) {
                addComment(body)
            }
        }

        loadComments()
    }

    private fun loadComments() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL("http://10.0.2.2:5000/comments/$answerId")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                val response = connection.inputStream.bufferedReader().readText()
                val jsonArray = JSONArray(response)

                comments.clear()
                for (i in 0 until jsonArray.length()) {
                    comments.add(jsonArray.getJSONObject(i))
                }

                withContext(Dispatchers.Main) {
                    adapter.notifyDataSetChanged()

                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CommentActivity, "Error loading comments", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun addComment(body: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL("http://10.0.2.2:5000/add_comment")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                val json = JSONObject().apply {
                    put("answer_id", answerId)
                    put("author_id", userId)
                    put("body", body)
                }

                OutputStreamWriter(connection.outputStream).use {
                    it.write(json.toString())
                }

                val responseCode = connection.responseCode
                if (responseCode == 201) {
                    withContext(Dispatchers.Main) {
                        commentEditText.text.clear()
                        loadComments()
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CommentActivity, "Error adding comment", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun deleteComment(commentId: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL("http://10.0.2.2:5000/delete_comment")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                val json = JSONObject().apply {
                    put("comment_id", commentId)
                    put("user_id", userId)
                }

                OutputStreamWriter(connection.outputStream).use {
                    it.write(json.toString())
                }

                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@CommentActivity, "Comment deleted", Toast.LENGTH_SHORT).show()
                        loadComments()
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CommentActivity, "Error deleting comment", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


}