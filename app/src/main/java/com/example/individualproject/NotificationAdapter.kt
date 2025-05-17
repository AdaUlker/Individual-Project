package com.example.individualproject

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class NotificationAdapter(
    private val context: Context,
    private val notifications: MutableList<JSONObject>,
    private val userId: Int,
    private val onRefresh: () -> Unit
) : BaseAdapter() {

    override fun getCount(): Int = notifications.size

    override fun getItem(position: Int): Any = notifications[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.notification_item, parent, false)
        val messageTextView = view.findViewById<TextView>(R.id.messageTextView)
        val deleteButton = view.findViewById<ImageButton>(R.id.deleteButton)

        val notification = notifications[position]
        val message = notification.getString("message")
        val notificationId = notification.getInt("id")
        val questionId = notification.optInt("question_id", -1)
        val answerId = notification.optInt("answer_id", -1)
        val commentId = notification.optInt("comment_id", -1)

        messageTextView.text = message

        view.setOnClickListener {
            incrementSeen(notificationId)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Eğer yorum bildirimi ise -> Yorum sayfasına git
                    if (commentId != -1 && answerId != -1) {
                        val intent = Intent(context, CommentActivity::class.java).apply {
                            putExtra("answer_id", answerId)
                            putExtra("user_id", userId)
                            putExtra("focus_comment_id", commentId)
                        }
                        CoroutineScope(Dispatchers.Main).launch {
                            context.startActivity(intent)
                        }
                    }

                    // Eğer doğrudan question_id varsa
                    else if (questionId != -1) {
                        val intent = Intent(context, QuestionDetailActivity::class.java).apply {
                            putExtra("question_id", questionId)
                            putExtra("user_id", userId)
                        }
                        CoroutineScope(Dispatchers.Main).launch {
                            context.startActivity(intent)
                        }
                    }

                    // Eğer answer_id varsa -> ilgili soruya git
                    else if (answerId != -1) {
                        val url = URL("http://10.0.2.2:5000/get_answer_by_id/$answerId")
                        val connection = url.openConnection() as HttpURLConnection
                        connection.requestMethod = "GET"
                        val response = connection.inputStream.bufferedReader().readText()
                        connection.disconnect()

                        val answerJson = JSONObject(response)
                        val relatedQuestionId = answerJson.optInt("question_id", -1)

                        if (relatedQuestionId != -1) {
                            val intent = Intent(context, QuestionDetailActivity::class.java).apply {
                                putExtra("question_id", relatedQuestionId)
                                putExtra("user_id", userId)
                                putExtra("focus_answer_id", answerId)
                            }
                            CoroutineScope(Dispatchers.Main).launch {
                                context.startActivity(intent)
                            }
                        } else {
                            showToast("")
                        }
                    }

                    // Hiçbir bilgi yoksa
                    else {
                        showToast("")
                    }

                } catch (e: Exception) {
                    showToast("")
                }
            }
        }

        deleteButton.setOnClickListener {
            deleteNotification(notificationId)
        }

        return view
    }

    private fun deleteNotification(notificationId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("http://10.0.2.2:5000/delete_notification")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                val json = JSONObject().apply {
                    put("notification_id", notificationId)
                }

                OutputStreamWriter(connection.outputStream).use {
                    it.write(json.toString())
                }

                if (connection.responseCode == 200) {
                    (context as NotificationActivity).runOnUiThread {
                        Toast.makeText(context, "Notification deleted", Toast.LENGTH_SHORT).show()
                        onRefresh()
                    }
                }

                connection.disconnect()
            } catch (e: Exception) {
                showToast("Error deleting notification")
            }
        }
    }

    private fun incrementSeen(notificationId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("http://10.0.2.2:5000/increment_seen")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                val json = JSONObject().apply {
                    put("notification_id", notificationId)
                }

                OutputStreamWriter(connection.outputStream).use {
                    it.write(json.toString())
                }

                connection.responseCode
                connection.disconnect()
            } catch (_: Exception) {}
        }
    }

    private fun showToast(message: String) {
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}