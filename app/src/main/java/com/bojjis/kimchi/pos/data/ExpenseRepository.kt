package com.bojjis.kimchi.pos.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

class ExpenseRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("bojjis_expenses", Context.MODE_PRIVATE)

    fun loadExpenses(): List<Expense> {
        val raw = prefs.getString(KEY_EXPENSES, "[]") ?: "[]"
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i -> fromJson(arr.getJSONObject(i)) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveExpense(expense: Expense) {
        val list = loadExpenses() + expense
        writeAll(list)
    }

    fun deleteExpense(id: Long) {
        writeAll(loadExpenses().filterNot { it.id == id })
    }

    fun clearExpenses() {
        prefs.edit().remove(KEY_EXPENSES).apply()
    }

    private fun writeAll(list: List<Expense>) {
        val arr = JSONArray()
        list.forEach { arr.put(toJson(it)) }
        prefs.edit().putString(KEY_EXPENSES, arr.toString()).apply()
    }

    private fun toJson(e: Expense): JSONObject = JSONObject().apply {
        put("id", e.id)
        put("timestamp", e.timestamp)
        put("amount", e.amount)
        put("category", e.category.name)
        put("note", e.note)
        put("payment", e.paymentMethod.name)
    }

    private fun fromJson(o: JSONObject): Expense = Expense(
        id = o.getLong("id"),
        timestamp = o.getLong("timestamp"),
        amount = o.getInt("amount"),
        category = ExpenseCategory.valueOf(o.getString("category")),
        note = o.optString("note", ""),
        paymentMethod = PaymentMethod.valueOf(o.getString("payment"))
    )

    companion object {
        private const val KEY_EXPENSES = "expenses_json"
    }
}
