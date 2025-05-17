package com.example.individualproject

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class InstructorManagementActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: InstructorUserAdapter
    private lateinit var searchEditText: EditText
    private lateinit var backButton: ImageButton

    private var userId: Int = -1
    private var isAdmin: Boolean = false

    private val userList = mutableListOf<JSONObject>()
    private val filteredList = mutableListOf<JSONObject>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_instructor_management)

        recyclerView = findViewById(R.id.userRecyclerView)
        searchEditText = findViewById(R.id.searchUserEditText)
        backButton = findViewById(R.id.backButton)

        userId = intent.getIntExtra("user_id", -1)

        recyclerView.layoutManager = LinearLayoutManager(this)

        backButton.setOnClickListener {
            finish()
        }

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().lowercase()
                filteredList.clear()
                filteredList.addAll(userList.filter {
                    it.getString("username").lowercase().contains(query)
                })
                adapter.notifyDataSetChanged()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        loadUsers()
    }

    private fun loadUsers() {
        lifecycleScope.launch(Dispatchers.IO) {
            var isInstructor = false // <- Ekledik
            try {
                val url = URL("http://10.0.2.2:5000/get_all_users?promoter_id=$userId")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                val response = connection.inputStream.bufferedReader().readText()
                val jsonArray = JSONArray(response)

                userList.clear()
                filteredList.clear()
                for (i in 0 until jsonArray.length()) {
                    val user = jsonArray.getJSONObject(i)
                    userList.add(user)
                    filteredList.add(user)

                    if (user.getInt("id") == userId) {
                        isAdmin = user.optInt("is_admin", 0) == 1
                        isInstructor = user.optInt("is_instructor", 0) == 1
                    }
                }

                withContext(Dispatchers.Main) {
                    adapter = InstructorUserAdapter(
                        context = this@InstructorManagementActivity,
                        userList = filteredList,
                        currentUserId = userId,
                        isCurrentUserAdmin = isAdmin,
                        isCurrentUserInstructor = isInstructor,
                        onPromoteClick = { id, isNowInstructor -> handlePromoteClick(id, isNowInstructor) },
                        onDeleteClick = { id -> handleDeleteUser(id) }
                    )
                    recyclerView.adapter = adapter
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@InstructorManagementActivity, "Error loading users", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun handlePromoteClick(userIdToToggle: Int, isNowInstructor: Boolean) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL("http://10.0.2.2:5000/toggle_instructor")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                val body = JSONObject().apply {
                    put("promoter_id", userId)
                    put("target_id", userIdToToggle)
                    put("is_instructor", isNowInstructor)
                }

                connection.outputStream.use {
                    it.write(body.toString().toByteArray())
                }

                if (connection.responseCode == 200) {
                    loadUsers() // liste güncelleniyor
                }

                connection.disconnect()
            } catch (_: Exception) {}
        }
    }


    private fun handleDeleteUser(userIdToDelete: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL("http://10.0.2.2:5000/delete_user_completely")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                val body = JSONObject().apply {
                    put("requester_id", userId)
                    put("target_user_id", userIdToDelete)
                }

                connection.outputStream.use {
                    it.write(body.toString().toByteArray())
                }

                if (connection.responseCode == 200) {
                    loadUsers()
                }

                connection.disconnect()
            } catch (_: Exception) {}
        }
    }
}



