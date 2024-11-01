package com.example.moviltaller3.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.moviltaller3.R
import com.example.moviltaller3.model.UserPOJO

class ListUsersAdapter (
    private val context: Context,
    private val userList: MutableList<UserPOJO>,
    private val onViewPositionClick: (UserPOJO) -> Unit
    ) : RecyclerView.Adapter<ListUsersAdapter.UserViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_available_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]
        holder.userNameTextView.text = "${user.name} ${user.surname}"
        Glide.with(context).load(user.photoUrl).into(holder.userImageView)
        holder.viewPositionButton.setOnClickListener {
            onViewPositionClick(user)
        }
    }

    override fun getItemCount(): Int {
        return userList.size
    }

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userImageView: ImageView = itemView.findViewById(R.id.userImageView)
        val userNameTextView: TextView = itemView.findViewById(R.id.userNameTextView)
        val viewPositionButton: Button = itemView.findViewById(R.id.viewPositionButton)
    }

    fun agregarUsuario(user: UserPOJO){
        userList.add(user)
        notifyItemInserted(userList.size - 1)
    }

    fun eliminarUsuario(user: UserPOJO){
        val index = userList.indexOf(user)
        userList.removeAt(index)
        notifyItemRemoved(index)
    }
}