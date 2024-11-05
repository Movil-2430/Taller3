package com.icm2430.firebase


import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.github.javafaker.Faker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.icm2430.firebase.adapters.PeopleAdapter
import com.icm2430.firebase.databinding.ActivityPeopleBinding
import com.icm2430.firebase.model.Address
import com.icm2430.firebase.model.DatabasePaths
import com.icm2430.firebase.model.People
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class PeopleActivity : AppCompatActivity() {

    private val TAG = PeopleActivity::class.java.name
    private lateinit var mAuth: FirebaseAuth

    // Auth user
    lateinit var currentUser: FirebaseUser

    // Variables for Firebase DB
    // lateinit var database: FirebaseDatabase
    val database = Firebase.database
    lateinit var myRef: DatabaseReference
    lateinit var valueEventListener: ValueEventListener

    //Local Data
    lateinit var adapter: PeopleAdapter
    var peopleLocal = ArrayList<People>()
    lateinit var listView: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityPeopleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance()
        currentUser = mAuth.currentUser!!

        // Initialize Firebase database
        // database = FirebaseDatabase.getInstance()
        myRef = Firebase.database.reference
        // val storage = Firebase.storage

        // Initialize Adapter
        adapter = PeopleAdapter(this@PeopleActivity, peopleLocal)
        listView = binding.peopleList
        listView.adapter = adapter

        binding.randomPeopleBtn.setOnClickListener {
            generatePeople()
        }
    }

    override fun onStart() {
        super.onStart()
        currentUser = mAuth.currentUser!!
        if (currentUser == null) {
            logout()
        }
        loadQueryPeople()
    }


    fun loadQueryPeople() {
        myRef = database.getReference(DatabasePaths.PEOPLE)
        myRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(datasnapshot: DataSnapshot) {
                peopleLocal.clear()
                for (snapshot in datasnapshot.children) {
                    val ppl = snapshot.getValue(People::class.java)
                    peopleLocal.add(ppl!!)
                }
                adapter.notifyDataSetChanged()
                Log.i(TAG, "Data changed from realtime DB")
                listView.post { listView.setSelection(peopleLocal.size - 1) }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w(TAG, "Error en la consulta", databaseError.toException())
            }
        })
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
                mAuth.signOut()
                val intent = Intent(this@PeopleActivity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                true
            } else -> super.onOptionsItemSelected(item)
        }
    }


    // Generate random people
    fun generatePeople() {
        val faker = Faker()
        val pl = People()
        pl.firstName = faker.funnyName().name()
        pl.lastName = faker.name().lastName()
        pl.age = faker.date().birthday().year
        pl.height = faker.number().randomDouble(1, 1, 2)
        pl.weight = faker.number().randomDouble(1, 50, 150)
        pl.currentAddress = Address()
        pl.currentAddress!!.city = faker.address().fullAddress()
        pl.currentAddress!!.address = faker.address().fullAddress()
        val key = myRef.push().key
        myRef = database.getReference(DatabasePaths.PEOPLE + key)
        Log.i(TAG, "Saving new people: $pl")
        myRef.setValue(pl)
    }


    private fun logout() {
        mAuth.signOut()
        val intent = Intent(this@PeopleActivity, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
    }
}