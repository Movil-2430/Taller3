package com.icm2430.firebase.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.icm2430.firebase.R
import com.icm2430.firebase.model.People


class PeopleAdapter(context: Context?, people: ArrayList<People>) :
    ArrayAdapter<People?>(context!!, 0, people as List<People?>) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        // Grab the person to render
        var convertView = convertView
        val person: People? = getItem(position)
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView =
                LayoutInflater.from(context).inflate(R.layout.people_adapter, parent, false)
        }
        // Get all the fields from the adapter
        val firstName = convertView!!.findViewById<TextView>(R.id.peopleFirstName)
        val lastName = convertView.findViewById<TextView>(R.id.peopleLastName)
        val address = convertView.findViewById<TextView>(R.id.peopleAddress)
        val city = convertView.findViewById<TextView>(R.id.peopleCity)
        val age = convertView.findViewById<TextView>(R.id.peopleAge)
        val height = convertView.findViewById<TextView>(R.id.peopleHeight)
        val weight = convertView.findViewById<TextView>(R.id.peopleWeight)
        // Format and set the values in the view
        firstName.text = person!!.firstName
        lastName.text = person.lastName
        address.text = person.currentAddress?.address
        city.text = person.currentAddress?.city
        age.text = String.format("%s yrs.", person.age)
        height.text = String.format("%S m.", person.height)
        weight.text = String.format("%S kgs.", person.weight)
        return convertView
    }
}