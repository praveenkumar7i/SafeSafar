package com.example.safesafar.utils

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class Contact(val name: String, val phone: String)

object ContactManager {
    private const val PREFS_NAME = "safesafar_contacts"
    private const val KEY_CONTACTS = "contacts_list"

    fun getContacts(context: Context): List<Contact> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_CONTACTS, "[]")
        val contacts = mutableListOf<Contact>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                contacts.add(Contact(obj.getString("name"), obj.getString("phone")))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return contacts
    }

    fun saveContact(context: Context, contact: Contact) {
        val contacts = getContacts(context).toMutableList()
        if (contacts.size >= 5) {
            return // Max 5 contacts
        }
        contacts.add(contact)
        saveAll(context, contacts)
    }

    fun deleteContact(context: Context, index: Int) {
        val contacts = getContacts(context).toMutableList()
        if(index in 0 until contacts.size) {
            contacts.removeAt(index)
            saveAll(context, contacts)
        }
    }

    private fun saveAll(context: Context, contacts: List<Contact>) {
        val jsonArray = JSONArray()
        contacts.forEach {
            val obj = JSONObject()
            obj.put("name", it.name)
            obj.put("phone", it.phone)
            jsonArray.put(obj)
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_CONTACTS, jsonArray.toString()).apply()
    }
}
