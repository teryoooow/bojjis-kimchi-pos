package com.bojjis.kimchi.pos

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bojjis.kimchi.pos.data.CartLine
import com.bojjis.kimchi.pos.data.MenuCatalog
import com.bojjis.kimchi.pos.data.MenuProduct
import com.bojjis.kimchi.pos.data.PaymentMethod
import com.bojjis.kimchi.pos.data.Sale
import com.bojjis.kimchi.pos.data.SaleRepository
import com.bojjis.kimchi.pos.databinding.ActivityMainBinding
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repo: SaleRepository
    private val cart = mutableListOf<CartLine>()
    private var adapter: CartAdapter? = null

    private var selectedProduct: MenuProduct = MenuProduct.FRIED_RICE
    private val selectedToppings = linkedSetOf<String>()
    private var paymentMethod: PaymentMethod = PaymentMethod.CASH

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        repo = SaleRepository(this)

        adapter = CartAdapter(cart) { position ->
            cart.removeAt(position)
            refreshCart()
        }
        binding.cartList.layoutManager = LinearLayoutManager(this)
        binding.cartList.adapter = adapter

        setupProductChips()
        setupToppingChips()
        setupPaymentToggle()
        setupButtons()
        refreshCart()
    }

    private fun setupProductChips() {
        MenuProduct.entries.forEach { product ->
            val chip = Chip(this).apply {
                text = "${product.label}  ₱${product.basePrice}"
                isCheckable = true
                isChecked = product == selectedProduct
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            chip.setOnClickListener {
                selectedProduct = product
                selectedToppings.clear()
                syncToppingChips()
                updateToppingHint()
            }
            binding.productChipGroup.addView(chip)
        }
        binding.productChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) {
                val first = binding.productChipGroup.getChildAt(0) as Chip
                first.isChecked = true
            }
        }
        // Ensure initial check
        (binding.productChipGroup.getChildAt(1) as Chip).isChecked = true
        selectedProduct = MenuProduct.FRIED_RICE
        updateToppingHint()
    }

    private fun setupToppingChips() {
        MenuCatalog.friedRiceToppings.forEach { name ->
            val chip = Chip(this).apply {
                text = name
                isCheckable = true
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            chip.setOnClickListener {
                if (chip.isChecked) selectedToppings.add(name)
                else selectedToppings.remove(name)
                updateToppingHint()
            }
            binding.toppingChipGroup.addView(chip)
        }
        updateToppingHint()
    }

    private fun syncToppingChips() {
        for (i in 0 until binding.toppingChipGroup.childCount) {
            val chip = binding.toppingChipGroup.getChildAt(i) as Chip
            chip.isChecked = selectedToppings.contains(chip.text.toString())
        }
    }

    private fun updateToppingHint() {
        binding.toppingHint.text = when (selectedProduct) {
            MenuProduct.KIMCHI -> getString(R.string.toppings_hint_kimchi)
            MenuProduct.FRIED_RICE -> getString(R.string.toppings_hint_rice)
        }
    }

    private fun setupPaymentToggle() {
        binding.paymentGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            paymentMethod = when {
                checkedIds.contains(R.id.chipCash) -> PaymentMethod.CASH
                checkedIds.contains(R.id.chipGcash) -> PaymentMethod.GCASH
                else -> PaymentMethod.CASH
            }
            binding.cashSection.visibility =
                if (paymentMethod == PaymentMethod.CASH) View.VISIBLE else View.GONE
        }
        binding.chipCash.isChecked = true
    }

    private fun setupButtons() {
        binding.btnAddToCart.setOnClickListener { addToCart() }
        binding.btnCheckout.setOnClickListener { checkout() }
        binding.btnHistory.setOnClickListener {
            startActivity(Intent(this, SalesHistoryActivity::class.java))
        }
        binding.btnExpenses.setOnClickListener {
            startActivity(Intent(this, ExpensesActivity::class.java))
        }
    }

    private fun addToCart() {
        if (selectedProduct == MenuProduct.FRIED_RICE && selectedToppings.isEmpty()) {
            Toast.makeText(this, R.string.pick_topping, Toast.LENGTH_SHORT).show()
            return
        }
        val qty = binding.qtyEdit.text?.toString()?.toIntOrNull()?.coerceIn(1, 99) ?: 1
        cart.add(CartLine(selectedProduct, selectedToppings.toList(), qty))
        selectedToppings.clear()
        syncToppingChips()
        binding.qtyEdit.setText("1")
        refreshCart()
        Toast.makeText(this, R.string.added, Toast.LENGTH_SHORT).show()
    }

    private fun refreshCart() {
        adapter?.notifyDataSetChanged()
        val total = cart.sumOf { it.lineTotal }
        binding.totalText.text = getString(R.string.total_fmt, total)
        binding.btnCheckout.isEnabled = cart.isNotEmpty()
        binding.emptyCart.visibility = if (cart.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun checkout() {
        if (cart.isEmpty()) return
        val total = cart.sumOf { it.lineTotal }

        if (paymentMethod == PaymentMethod.CASH) {
            val input = EditText(this).apply {
                hint = getString(R.string.cash_hint)
                inputType = android.text.InputType.TYPE_CLASS_NUMBER
            }
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.cash_tendered)
                .setView(input)
                .setPositiveButton(R.string.confirm) { _, _ ->
                    val tendered = input.text.toString().toIntOrNull()
                    if (tendered == null || tendered < total) {
                        Toast.makeText(this, R.string.insufficient_cash, Toast.LENGTH_SHORT).show()
                    } else {
                        completeSale(total, tendered, tendered - total)
                    }
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        } else {
            completeSale(total, null, null)
        }
    }

    private fun completeSale(total: Int, tendered: Int?, change: Int?) {
        val sale = Sale(
            id = System.currentTimeMillis(),
            timestamp = System.currentTimeMillis(),
            paymentMethod = paymentMethod,
            lines = cart.toList(),
            total = total,
            cashTendered = tendered,
            changeDue = change
        )
        repo.saveSale(sale)
        cart.clear()
        refreshCart()
        binding.editCash.setText("")
        val msg = if (change != null) getString(R.string.sale_done_change, change)
        else getString(R.string.sale_done)
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.sale_complete)
            .setMessage(msg)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    class CartAdapter(
        private val items: List<CartLine>,
        private val onRemove: (Int) -> Unit
    ) : RecyclerView.Adapter<CartAdapter.VH>() {

        class VH(val view: View) : RecyclerView.ViewHolder(view)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_cart_line, parent, false)
            return VH(v)
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val line = items[position]
            val root = holder.view
            root.findViewById<android.widget.TextView>(R.id.lineTitle).text =
                line.summary()
            root.findViewById<android.widget.TextView>(R.id.lineMeta).text =
                root.context.getString(
                    R.string.line_meta,
                    line.qty,
                    line.unitPrice,
                    line.lineTotal
                )
            root.findViewById<android.widget.ImageButton>(R.id.btnRemove).setOnClickListener {
                onRemove(holder.bindingAdapterPosition)
            }
        }
    }
}
