package com.example.individualproject

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject

class InstructorUserAdapter(
    private val context: Context,
    private val userList: List<JSONObject>,
    private val currentUserId: Int,
    private val isCurrentUserAdmin: Boolean,
    private val isCurrentUserInstructor: Boolean,
    private val onPromoteClick: (userId: Int, isNowInstructor: Boolean) -> Unit,
    private val onDeleteClick: (userId: Int) -> Unit
) : RecyclerView.Adapter<InstructorUserAdapter.UserViewHolder>() {


    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.userNameText)
        val emailText: TextView = view.findViewById(R.id.userEmailText)
        val instructorBadge: TextView = view.findViewById(R.id.instructorBadge)
        val promoteButton: Button = view.findViewById(R.id.promoteButton)
        val deleteButton: ImageButton = view.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.user_list_item, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]
        val userId = user.getInt("id")
        val username = user.getString("username")
        val email = user.getString("email")
        val isInstructor = user.optInt("is_instructor", 0) == 1
        val isAdmin = user.optInt("is_admin", 0) == 1

        // İsim ve e-posta göster
        holder.nameText.text = username
        holder.emailText.text = email

        // Rozet: Admin veya Instructor
        holder.instructorBadge.text = when {
            isAdmin -> "Admin"
            isInstructor -> "Instructor"
            else -> ""
        }
        holder.instructorBadge.visibility = if (isAdmin || isInstructor) View.VISIBLE else View.GONE

        // Promote/Demote butonu yalnızca admin görür (kendi hariç ve admin olmayanlar için)
        holder.promoteButton.visibility = if (
            isCurrentUserAdmin && !isAdmin && userId != currentUserId
        ) View.VISIBLE else View.GONE
        holder.promoteButton.text = if (isInstructor) "Demote" else "Promote"
        holder.promoteButton.setOnClickListener {
            onPromoteClick(userId, !isInstructor)
        }

        // Silme butonu: Admin herkes için, eğitmen sadece normal kullanıcılar için görebilir
        holder.deleteButton.visibility = if (
            userId != currentUserId && !isAdmin && (
                    isCurrentUserAdmin || (isCurrentUserInstructor && !isInstructor)
                    )
        ) View.VISIBLE else View.GONE

        holder.deleteButton.setOnClickListener {
            onDeleteClick(userId)
        }
    }

    override fun getItemCount(): Int = userList.size
}
