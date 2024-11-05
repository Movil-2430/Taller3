package com.icm2430.firebase

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.icm2430.firebase.adapters.MessageAdapter
import com.icm2430.firebase.databinding.ActivityChatBinding
import com.icm2430.firebase.model.DatabasePaths
import com.icm2430.firebase.model.Message
import java.util.Date

class ChatActivity : AppCompatActivity() {

    private val TAG = ChatActivity::class.java.name

    // Varriables for Firebase Auth
    private lateinit var mAuth: FirebaseAuth
    lateinit var currentUser: FirebaseUser

    // Vaiables for Firebase DB
    lateinit var database: FirebaseDatabase
    lateinit var myRef: DatabaseReference
    lateinit var valueEventListener: ValueEventListener

    // Local Data
    lateinit var adapter: MessageAdapter
    lateinit var sendButton: LottieAnimationView
    lateinit var messageEdit: EditText
    var messages = ArrayList<Message>()
    lateinit var messageList: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Auth

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance()
        currentUser = mAuth.currentUser!!

        // Initialize Firebase database
        database = FirebaseDatabase.getInstance()
        myRef = database.reference

        // Initialize Adapter
        adapter = MessageAdapter(this@ChatActivity, messages)
        messageList = binding.chatMessages
        messageList.adapter = adapter

        sendButton = binding.sendButton
        messageEdit = binding.messageEdit
    }


    override fun onStart() {
        super.onStart()
        currentUser = mAuth.currentUser!!
        if (currentUser == null) {
            logout()
        }
        loadMessages()
    }


    override fun onPause() {
        super.onPause()
        if (valueEventListener != null) {
            myRef.removeEventListener(valueEventListener)
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menuLogOut -> {
                logout()
                true
            } else -> super.onOptionsItemSelected(item)
        }
    }


    fun loadMessages() {
        myRef = database.getReference(DatabasePaths.CHAT)
        myRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                messages.clear()
                for (snapshot in dataSnapshot.children) {
                    val tmpMsg = snapshot.getValue(Message::class.java)
                    messages.add(tmpMsg!!)
                }
                adapter.notifyDataSetChanged()
                messageList.post { messageList.setSelection(messages.size - 1) }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w(TAG, "Error in query", databaseError.toException())
            }
        })
    }


    fun sendMessage(view: View?) {
        val message = messageEdit.text.toString()
        if (message.isNotEmpty()) {
            messageEdit.text.clear()
            val key = myRef.push().key
            val msgToSend = Message(key, message, currentUser.email, Date())
            myRef = database.getReference(DatabasePaths.CHAT + key)
            myRef.setValue(msgToSend)
            sendButton.playAnimation()
        } else {
            Toast.makeText(this@ChatActivity, "Message cannot be empty.", Toast.LENGTH_SHORT).show()
        }
    }


    private fun logout() {
        mAuth.signOut()
        val intent = Intent(this@ChatActivity, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
    }
}