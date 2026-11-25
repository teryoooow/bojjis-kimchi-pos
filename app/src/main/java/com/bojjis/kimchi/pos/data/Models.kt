package com.bojjis.kimchi.pos.data

enum class PaymentMethod(val label: String) {
    CASH("Cash"),
    GCASH("GCash")
}

enum class MenuProduct(val id: String, val label: String, val basePrice: Int) {
    KIMCHI("kimchi", "Kimchi", 200),
    FRIED_RICE("fried_rice", "Fried Rice Kimchi", 100);

    companion object {
        const val EXTRA_TOPPING_PRICE = 25
    }
}

data class CartLine(
    val product: MenuProduct,
    val toppings: List<String> = emptyList(),
    val qty: Int = 1
) {
    /** First topping on fried rice is included; extras are ₱25 each. Kimchi toppings are all ₱25. */
    val unitPrice: Int
        get() {
            val extras = when (product) {
                MenuProduct.KIMCHI -> toppings.size
                MenuProduct.FRIED_RICE -> (toppings.size - 1).coerceAtLeast(0)
            }
            return product.basePrice + extras * MenuProduct.EXTRA_TOPPING_PRICE
        }

    val lineTotal: Int get() = unitPrice * qty

    fun summary(): String {
        val top = if (toppings.isEmpty()) "" else " + " + toppings.joinToString(", ")
        return product.label + top
    }
}

data class Sale(
    val id: Long,
    val timestamp: Long,
    val paymentMethod: PaymentMethod,
    val lines: List<CartLine>,
    val total: Int,
    val cashTendered: Int? = null,
    val changeDue: Int? = null
)

object MenuCatalog {
    val friedRiceToppings = listOf("Egg", "Odeng", "Tteokbokki", "Korean Sausage")
}
