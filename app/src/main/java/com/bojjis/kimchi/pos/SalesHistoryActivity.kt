package com.bojjis.kimchi.pos

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import com.bojjis.kimchi.pos.data.ExpenseRepository
import com.bojjis.kimchi.pos.data.MenuProduct
import com.bojjis.kimchi.pos.data.Sale
import com.bojjis.kimchi.pos.data.SaleRepository
import com.bojjis.kimchi.pos.databinding.ActivitySalesHistoryBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class SalesHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySalesHistoryBinding
    private lateinit var repo: SaleRepository
    private var adapter: SaleAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySalesHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        repo = SaleRepository(this)

        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.btnClear.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.clear_history)
                .setMessage(R.string.clear_history_confirm)
                .setPositiveButton(R.string.clear) { _, _ ->
                    repo.clearSales()
                    render()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        finish()
    }

    private fun render() {
        val sales = repo.loadSales().sortedByDescending { it.timestamp }
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val today = sales.filter { it.timestamp >= todayStart }
        val todayTotal = today.sumOf { it.total }
        val todayCash = today.filter { it.paymentMethod.name == "CASH" }.sumOf { it.total }
        val todayGcash = today.filter { it.paymentMethod.name == "GCASH" }.sumOf { it.total }
        val allTime = sales.sumOf { it.total }

        val kimchiRevenue = sales.sumOf { sale ->
            sale.lines.filter { it.product == MenuProduct.KIMCHI }.sumOf { it.lineTotal }
        }
        val kimchiQty = sales.sumOf { sale ->
            sale.lines.filter { it.product == MenuProduct.KIMCHI }.sumOf { it.qty }
        }
        val riceRevenue = sales.sumOf { sale ->
            sale.lines.filter { it.product == MenuProduct.FRIED_RICE }.sumOf { it.lineTotal }
        }
        val riceQty = sales.sumOf { sale ->
            sale.lines.filter { it.product == MenuProduct.FRIED_RICE }.sumOf { it.qty }
        }

        binding.statTodayTotal.text = getString(R.string.total_fmt, todayTotal)
        binding.statTodayCash.text = getString(R.string.total_fmt, todayCash)
        binding.statTodayGcash.text = getString(R.string.total_fmt, todayGcash)
        binding.statAllTime.text = getString(R.string.total_fmt, allTime)
        binding.statCount.text = sales.size.toString()

        binding.statKimchiSales.text = getString(R.string.total_fmt, kimchiRevenue)
        binding.statKimchiQty.text = getString(R.string.units_fmt, kimchiQty)
        binding.statRiceSales.text = getString(R.string.total_fmt, riceRevenue)
        binding.statRiceQty.text = getString(R.string.units_fmt, riceQty)

        val allTimeCashSales = sales.filter { it.paymentMethod.name == "CASH" }
        val allTimeGcashSales = sales.filter { it.paymentMethod.name == "GCASH" }
        binding.statAllTimeCash.text = getString(R.string.total_fmt, allTimeCashSales.sumOf { it.total })
        binding.statAllTimeCashCount.text = getString(R.string.txns_fmt, allTimeCashSales.size)
        binding.statAllTimeGcash.text = getString(R.string.total_fmt, allTimeGcashSales.sumOf { it.total })
        binding.statAllTimeGcashCount.text = getString(R.string.txns_fmt, allTimeGcashSales.size)

        val totalExpenses = ExpenseRepository(this).loadExpenses().sumOf { it.amount }
        val net = allTime - totalExpenses
        binding.statProfitSales.text = getString(R.string.total_fmt, allTime)
        binding.statProfitExpenses.text = getString(R.string.total_fmt, totalExpenses)
        binding.statNetProfit.text = getString(R.string.total_fmt, net)
        binding.statNetProfit.setTextColor(
            getColor(if (net >= 0) R.color.green else R.color.red)
        )

        binding.emptyState.visibility = if (sales.isEmpty()) View.VISIBLE else View.GONE
        binding.salesList.visibility = if (sales.isEmpty()) View.GONE else View.VISIBLE
        binding.salesList.layoutManager = LinearLayoutManager(this)
        adapter = SaleAdapter(
            items = sales,
            onDelete = { sale -> confirmDelete(sale) }
        )
        binding.salesList.adapter = adapter
    }

    private fun confirmDelete(sale: Sale) {
        val detail = sale.lines.joinToString("\n") { "- ${it.summary()} ×${it.qty}" }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_txn)
            .setMessage(getString(R.string.delete_txn_confirm, sale.total, detail))
            .setPositiveButton(R.string.delete) { _, _ ->
                repo.deleteSale(sale.id)
                Toast.makeText(this, R.string.txn_deleted, Toast.LENGTH_SHORT).show()
                render()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    class SaleAdapter(
        private val items: List<Sale>,
        private val onDelete: (Sale) -> Unit
    ) : RecyclerView.Adapter<SaleAdapter.VH>() {

        private val fmt = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault())

        class VH(val view: View) : RecyclerView.ViewHolder(view)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_sale, parent, false)
            return VH(v)
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val sale = items[position]
            val root = holder.view
            root.findViewById<TextView>(R.id.saleHeader).text =
                "${sale.paymentMethod.label} · ${fmt.format(Date(sale.timestamp))}"
            root.findViewById<TextView>(R.id.saleItems).text =
                sale.lines.joinToString("\n") { "- ${it.summary()} ×${it.qty}" }
            root.findViewById<TextView>(R.id.saleTotal).text =
                root.context.getString(R.string.total_fmt, sale.total)
            val extra = buildString {
                sale.cashTendered?.let {
                    append(root.context.getString(R.string.tendered_fmt, it))
                }
                sale.changeDue?.let {
                    if (isNotEmpty()) append("  ·  ")
                    append(root.context.getString(R.string.change_fmt, it))
                }
            }
            root.findViewById<TextView>(R.id.saleExtra).apply {
                text = extra
                visibility = if (extra.isEmpty()) View.GONE else View.VISIBLE
            }
            root.findViewById<ImageButton>(R.id.btnDeleteSale).setOnClickListener {
                onDelete(sale)
            }
        }
    }
}
