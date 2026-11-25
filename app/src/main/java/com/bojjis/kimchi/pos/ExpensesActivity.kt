package com.bojjis.kimchi.pos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bojjis.kimchi.pos.data.Expense
import com.bojjis.kimchi.pos.data.ExpenseCategory
import com.bojjis.kimchi.pos.data.ExpenseRepository
import com.bojjis.kimchi.pos.data.PaymentMethod
import com.bojjis.kimchi.pos.databinding.ActivityExpensesBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExpensesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExpensesBinding
    private lateinit var repo: ExpenseRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExpensesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        repo = ExpenseRepository(this)

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.btnAddExpense.setOnClickListener { showAddDialog() }
        binding.btnClearExpenses.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.clear_expenses)
                .setMessage(R.string.clear_expenses_confirm)
                .setPositiveButton(R.string.clear) { _, _ ->
                    repo.clearExpenses()
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
        val expenses = repo.loadExpenses().sortedByDescending { it.timestamp }
        val total = expenses.sumOf { it.amount }
        val cashOut = expenses.filter { it.paymentMethod == PaymentMethod.CASH }.sumOf { it.amount }
        val gcashOut = expenses.filter { it.paymentMethod == PaymentMethod.GCASH }.sumOf { it.amount }

        binding.statExpenseTotal.text = getString(R.string.total_fmt, total)
        binding.statExpenseCash.text = getString(R.string.total_fmt, cashOut)
        binding.statExpenseGcash.text = getString(R.string.total_fmt, gcashOut)
        binding.statExpenseCount.text = expenses.size.toString()

        binding.emptyExpenses.visibility = if (expenses.isEmpty()) View.VISIBLE else View.GONE
        binding.expenseList.visibility = if (expenses.isEmpty()) View.GONE else View.VISIBLE
        binding.expenseList.layoutManager = LinearLayoutManager(this)
        binding.expenseList.adapter = ExpenseAdapter(expenses) { expense ->
            confirmDelete(expense)
        }
        binding.btnClearExpenses.visibility = if (expenses.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun confirmDelete(expense: Expense) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_expense)
            .setMessage(
                getString(
                    R.string.delete_expense_confirm,
                    expense.amount,
                    expense.category.label,
                    expense.note.ifEmpty { "-" }
                )
            )
            .setPositiveButton(R.string.delete) { _, _ ->
                repo.deleteExpense(expense.id)
                Toast.makeText(this, R.string.expense_deleted, Toast.LENGTH_SHORT).show()
                render()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showAddDialog() {
        val form = layoutInflater.inflate(R.layout.dialog_add_expense, null)
        val amountEdit = form.findViewById<TextInputEditText>(R.id.editAmount)
        val noteEdit = form.findViewById<TextInputEditText>(R.id.editNote)
        val categorySpinner = form.findViewById<Spinner>(R.id.spinnerCategory)
        val paymentSpinner = form.findViewById<Spinner>(R.id.spinnerPayment)

        categorySpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            ExpenseCategory.entries.map { it.label }
        )
        paymentSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            PaymentMethod.entries.map { it.label }
        )

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.add_expense)
            .setView(form)
            .setPositiveButton(R.string.save) { _, _ ->
                val amount = amountEdit.text?.toString()?.toIntOrNull()
                if (amount == null || amount <= 0) {
                    Toast.makeText(this, R.string.invalid_amount, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val category = ExpenseCategory.entries[categorySpinner.selectedItemPosition]
                val payment = PaymentMethod.entries[paymentSpinner.selectedItemPosition]
                val note = noteEdit.text?.toString()?.trim().orEmpty()
                repo.saveExpense(
                    Expense(
                        id = System.currentTimeMillis(),
                        timestamp = System.currentTimeMillis(),
                        amount = amount,
                        category = category,
                        note = note,
                        paymentMethod = payment
                    )
                )
                Toast.makeText(this, R.string.expense_saved, Toast.LENGTH_SHORT).show()
                render()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    class ExpenseAdapter(
        private val items: List<Expense>,
        private val onDelete: (Expense) -> Unit
    ) : RecyclerView.Adapter<ExpenseAdapter.VH>() {

        private val fmt = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault())

        class VH(val view: View) : RecyclerView.ViewHolder(view)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_expense, parent, false)
            return VH(v)
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val e = items[position]
            val root = holder.view
            root.findViewById<TextView>(R.id.expenseHeader).text =
                "${e.category.label} · ${e.paymentMethod.label}"
            root.findViewById<TextView>(R.id.expenseDate).text = fmt.format(Date(e.timestamp))
            root.findViewById<TextView>(R.id.expenseNote).apply {
                text = e.note
                visibility = if (e.note.isEmpty()) View.GONE else View.VISIBLE
            }
            root.findViewById<TextView>(R.id.expenseAmount).text =
                root.context.getString(R.string.expense_dash_fmt, e.amount)
            root.findViewById<ImageButton>(R.id.btnDeleteExpense).setOnClickListener {
                onDelete(e)
            }
        }
    }
}
