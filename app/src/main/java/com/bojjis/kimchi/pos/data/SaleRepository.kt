package com.bojjis.kimchi.pos.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

class SaleRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("bojjis_sales", Context.MODE_PRIVATE)

    fun loadSales(): List<Sale> {
        val raw = prefs.getString(KEY_SALES, "[]") ?: "[]"
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i -> fromJson(arr.getJSONObject(i)) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveSale(sale: Sale) {
        val list = loadSales() + sale
        val arr = JSONArray()
        list.forEach { arr.put(toJson(it)) }
        prefs.edit().putString(KEY_SALES, arr.toString()).apply()
    }

    fun deleteSale(id: Long) {
        val list = loadSales().filterNot { it.id == id }
        val arr = JSONArray()
        list.forEach { arr.put(toJson(it)) }
        prefs.edit().putString(KEY_SALES, arr.toString()).apply()
    }

    fun clearSales() {
        prefs.edit().remove(KEY_SALES).apply()
    }

    private fun toJson(sale: Sale): JSONObject = JSONObject().apply {
        put("id", sale.id)
        put("timestamp", sale.timestamp)
        put("payment", sale.paymentMethod.name)
        put("total", sale.total)
        put("cashTendered", sale.cashTendered ?: JSONObject.NULL)
        put("changeDue", sale.changeDue ?: JSONObject.NULL)
        put("lines", JSONArray().apply {
            sale.lines.forEach { line ->
                put(JSONObject().apply {
                    put("product", line.product.name)
                    put("toppings", JSONArray(line.toppings))
                    put("qty", line.qty)
                    put("unitPrice", line.unitPrice)
                })
            }
        })
    }

    private fun fromJson(o: JSONObject): Sale {
        val linesArr = o.optJSONArray("lines") ?: JSONArray()
        val lines = (0 until linesArr.length()).map { i ->
            val lo = linesArr.getJSONObject(i)
            val topArr = lo.optJSONArray("toppings") ?: JSONArray()
            CartLine(
                product = MenuProduct.valueOf(lo.getString("product")),
                toppings = (0 until topArr.length()).map { topArr.getString(it) },
                qty = lo.getInt("qty")
            )
        }
        val cash = if (o.isNull("cashTendered")) null else o.getInt("cashTendered")
        val change = if (o.isNull("changeDue")) null else o.getInt("changeDue")
        return Sale(
            id = o.getLong("id"),
            timestamp = o.getLong("timestamp"),
            paymentMethod = PaymentMethod.valueOf(o.getString("payment")),
            lines = lines,
            total = o.getInt("total"),
            cashTendered = cash,
            changeDue = change
        )
    }

    companion object {
        private const val KEY_SALES = "sales_json"
    }
}
