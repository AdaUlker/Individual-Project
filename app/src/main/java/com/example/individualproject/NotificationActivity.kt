package com.example.individualproject

import android.os.Bundle
import android.widget.ImageButton
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class NotificationActivity : AppCompatActivity() {

    private lateinit var notificationListView: ListView
    private lateinit var backButton: ImageButton
    private var userId: Int = -1
    private val notificationList = mutableListOf<JSONObject>()
    private lateinit var adapter: NotificationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)

        notificationListView = findViewById(R.id.notificationListView)
        backButton = findViewById(R.id.backButton)

        userId = intent.getIntExtra("user_id", -1)

        adapter = NotificationAdapter(this, notificationList, userId) {
            fetchNotifications() // Yenileme fonksiyonu
        }

        notificationListView.adapter = adapter

        backButton.setOnClickListener {
            finish()
        }

        fetchNotifications()
    }

    override fun onResume() {
        super.onResume()
        fetchNotifications()
    }

    private fun fetchNotifications() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL("http://10.0.2.2:5000/notifications/$userId")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                val response = connection.inputStream.bufferedReader().readText()
                val jsonArray = JSONArray(response)

                notificationList.clear()
                for (i in 0 until jsonArray.length()) {
                    notificationList.add(jsonArray.getJSONObject(i))
                }

                withContext(Dispatchers.Main) {
                    adapter.notifyDataSetChanged()
                }

                connection.disconnect()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@NotificationActivity, "Error loading notifications", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}