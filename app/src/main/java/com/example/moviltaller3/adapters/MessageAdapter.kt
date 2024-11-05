package com.icm2430.firebase.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.icm2430.firebase.R
import com.icm2430.firebase.model.Message
import java.text.SimpleDateFormat

class MessageAdapter(context: Context?, messages: ArrayList<Message>) :
    ArrayAdapter<Message?>(context!!, 0, messages!! as List<Message?>) {
    private val mAuth: FirebaseAuth
    private val currentUser: FirebaseUser?

    init {
        mAuth = FirebaseAuth.getInstance()
        currentUser = mAuth.currentUser
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        // Grab the message to render
        var convertView = convertView
        val message: Message? = getItem(position)
        // Check if an existing view is being reused, otherwise inflat the view
        if (convertView == null) {
            convertView =
                LayoutInflater.from(context).inflate(R.layout.message_adapter, parent, false)
        }
        // Get all the fields from the adapter
        val layout = convertView!!.findViewById<ConstraintLayout>(R.id.messageContainer)
        val user = convertView.findViewById<TextView>(R.id.messageUser)
        val messageContent = convertView.findViewById<TextView>(R.id.messageContent)
        val date = convertView.findViewById<TextView>(R.id.messageDate)
        //Format and Set the values in the view
        user.text = message?.let { String.format("%s says:", it.userId) }
        messageContent.text = message?.text
        val simpleDateFormat = SimpleDateFormat(MESSAGE_DATE_FORMAT)
        date.text = message?.timestamp?.let { simpleDateFormat.format(it) }
        if (mAuth.currentUser!!.email == message?.userId) {
            layout.setBackgroundResource(R.drawable.mymessage_layout)
        } else {
            layout.setBackgroundResource(R.drawable.message_layout)
        }
        return convertView
    }

    companion object {
        private const val MESSAGE_DATE_FORMAT = "dd MMMM yyyy h:ma"
    }
}