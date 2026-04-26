package com.example.safesafar.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ItemTouchHelper
import com.example.safesafar.R
import com.example.safesafar.utils.Contact
import com.example.safesafar.utils.ContactManager

class ContactsActivity : AppCompatActivity() {

    private lateinit var rvContacts: RecyclerView
    private lateinit var etName: EditText
    private lateinit var etPhone: EditText
    private lateinit var btnAdd: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contacts)
        supportActionBar?.title = "Emergency Contacts"

        rvContacts = findViewById(R.id.rvContacts)
        etName = findViewById(R.id.etName)
        etPhone = findViewById(R.id.etPhone)
        btnAdd = findViewById(R.id.btnAdd)

        rvContacts.layoutManager = LinearLayoutManager(this)
        loadContacts()

        btnAdd.setOnClickListener {
            val name = etName.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val contacts = ContactManager.getContacts(this)
            
            if (name.isNotEmpty() && phone.isNotEmpty()) {
                if (!phone.matches(Regex("^[0-9]{10}$"))) {
                    Toast.makeText(this, "Enter a valid 10-digit phone number", Toast.LENGTH_SHORT).show()
                } else if (contacts.size >= 5) {
                    Toast.makeText(this, "Max 5 contacts allowed", Toast.LENGTH_SHORT).show()
                } else if (contacts.any { it.phone == phone }) {
                    Toast.makeText(this, "Contact already exists", Toast.LENGTH_SHORT).show()
                } else {
                    ContactManager.saveContact(this, Contact(name, phone))
                    etName.text.clear()
                    etPhone.text.clear()
                    loadContacts()
                }
            } else {
                Toast.makeText(this, "Enter both name and phone", Toast.LENGTH_SHORT).show()
            }
        }
        
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                ContactManager.deleteContact(this@ContactsActivity, viewHolder.adapterPosition)
                loadContacts()
            }
        }
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(rvContacts)
    }

    private fun loadContacts() {
        val contacts = ContactManager.getContacts(this)
        rvContacts.adapter = ContactsAdapter(contacts) { index ->
            ContactManager.deleteContact(this, index)
            loadContacts()
        }
    }
}
