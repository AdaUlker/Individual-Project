package com.example.individualproject

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class AnswerAdapter(
    private val context: Context,
    private val answers: MutableList<JSONObject>,
    private val userId: Int,
    private val isInstructor: Boolean,
    private val isAdmin: Boolean
) : BaseAdapter() {

    override fun getCount(): Int = answers.size
    override fun getItem(position: Int): Any = answers[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val answer = answers[position]
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.answer_item, parent, false)

        val answerTextView = view.findViewById<TextView>(R.id.answerTextView)
        val voteCountTextView = view.findViewById<TextView>(R.id.voteCountTextView)
        val upvoteButton = view.findViewById<Button>(R.id.upvoteButton)
        val downvoteButton = view.findViewById<Button>(R.id.downvoteButton)
        val containerLayout = view.findViewById<LinearLayout>(R.id.answerContainerLayout)
        val answerUsernameTextView = view.findViewById<TextView>(R.id.answerUsernameTextView)
        val answerTimeTextView = view.findViewById<TextView>(R.id.answerTimeTextView)
        val createdAt = answer.optString("created_at", "")
        answerTimeTextView.text = "Answered on: ${formatDate(createdAt)}"


        answerTextView.text = answer.getString("body")
        answerUsernameTextView.text = "By: ${answer.optString("username", "user")}"

        val upvotes = answer.optInt("upvotes", 0)
        val downvotes = answer.optInt("downvotes", 0)
        voteCountTextView.text = (upvotes - downvotes).toString()

        upvoteButton.setOnClickListener {
            vote(answer.getInt("id"), "up", voteCountTextView, upvotes, downvotes)
        }

        downvoteButton.setOnClickListener {
            vote(answer.getInt("id"), "down", voteCountTextView, upvotes, downvotes)
        }

        if (containerLayout.findViewWithTag<Button>("commentButton_$position") == null) {
            val commentButton = Button(context).apply {
                text = "Comments"
                tag = "commentButton_$position"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 12 }
                setOnClickListener {
                    val intent = Intent(context, CommentActivity::class.java)
                    intent.putExtra("answer_id", answer.getInt("id"))
                    intent.putExtra("user_id", userId)
                    context.startActivity(intent)
                }
            }
            containerLayout.addView(commentButton)
        }

        if (answer.optInt("author_id") == userId) {
            if (containerLayout.findViewWithTag<Button>("deleteButton_$position") == null) {
                val deleteButton = Button(context).apply {
                    text = "Delete"
                    tag = "deleteButton_$position"
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { topMargin = 8 }
                    setOnClickListener {
                        AlertDialog.Builder(context).apply {
                            setTitle("Delete Answer")
                            setMessage("Are you sure you want to delete this answer?")
                            setPositiveButton("Yes") { _, _ -> deleteAnswer(answer.getInt("id")) }
                            setNegativeButton("Cancel", null)
                            show()
                        }
                    }
                }
                containerLayout.addView(deleteButton)
            }
        }

        // ✅ Instructor Verified Badge
        val verifiedTag = "verifiedText_$position"
        containerLayout.findViewWithTag<TextView>(verifiedTag)?.let {
            containerLayout.removeView(it)
        }
        if (answer.optInt("verified", 0) == 1) {
            val verifiedTextView = TextView(context).apply {
                text = "Instructor Verified"
                setTextColor(0xFF4CAF50.toInt())
                tag = verifiedTag
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 8 }
            }
            containerLayout.addView(verifiedTextView)
        }

        // ✅ Verify/Unverify Button
        val toggleTag = "verifyButton_$position"
        containerLayout.findViewWithTag<Button>(toggleTag)?.let {
            containerLayout.removeView(it)
        }
        if (isInstructor || isAdmin) {
            val isVerified = answer.optInt("verified", 0) == 1
            val verifyButton = Button(context).apply {
                text = if (isVerified) "Unverify" else "Verify"
                tag = toggleTag
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 8 }
                setOnClickListener {
                    toggleVerification(answer.getInt("id"), !isVerified)
                }
            }
            containerLayout.addView(verifyButton)
        }

        return view
    }

    private fun vote(answerId: Int, voteType: String, voteCountView: TextView, currentUp: Int, currentDown: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("http://10.0.2.2:5000/vote_answer")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                val json = JSONObject().apply {
                    put("answer_id", answerId)
                    put("user_id", userId)
                    put("vote_type", voteType)
                }

                OutputStreamWriter(connection.outputStream).use {
                    it.write(json.toString())
                }

                connection.inputStream.close()
                connection.disconnect()

                val newCount = if (voteType == "up") currentUp + 1 - currentDown else currentUp - (currentDown + 1)
                CoroutineScope(Dispatchers.Main).launch {
                    voteCountView.text = newCount.toString()
                }

            } catch (e: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, "Error voting: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun deleteAnswer(answerId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("http://10.0.2.2:5000/delete_answer")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                val json = JSONObject().apply {
                    put("answer_id", answerId)
                    put("user_id", userId)
                }

                OutputStreamWriter(connection.outputStream).use {
                    it.write(json.toString())
                }

                val responseCode = connection.responseCode
                connection.disconnect()

                if (responseCode in 200..299) {
                    CoroutineScope(Dispatchers.Main).launch {
                        Toast.makeText(context, "Answer deleted", Toast.LENGTH_SHORT).show()

                        if (context is QuestionDetailActivity) {
                            val intent = Intent(context, QuestionDetailActivity::class.java).apply {
                                putExtra("question_id", context.intent.getIntExtra("question_id", -1))
                                putExtra("user_id", userId)
                                // ESKİ: question put edilmemiş
                                putExtra("question", context.intent.getStringExtra("question"))  // ✅ bu satırı ekle
                            }
                            context.startActivity(intent)
                            context.finish()
                        }

                    }
                } else {
                    CoroutineScope(Dispatchers.Main).launch {
                        Toast.makeText(context, "Delete failed", Toast.LENGTH_SHORT).show()
                    }
                }

            } catch (e: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun toggleVerification(answerId: Int, verified: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("http://10.0.2.2:5000/verify_answer")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                val body = JSONObject().apply {
                    put("answer_id", answerId)
                    put("user_id", userId)
                    put("verified", verified)
                }

                OutputStreamWriter(connection.outputStream).use {
                    it.write(body.toString())
                }

                if (connection.responseCode == 200) {
                    val index = answers.indexOfFirst { it.getInt("id") == answerId }
                    if (index != -1) answers[index].put("verified", if (verified) 1 else 0)
                    (context as? QuestionDetailActivity)?.runOnUiThread {
                        notifyDataSetChanged()
                    }
                }

                connection.disconnect()
            } catch (e: Exception) {
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, "Failed to verify", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    private fun formatDate(raw: String): String {
        return raw.replace("T", " ").substringBefore(".")
    }
}