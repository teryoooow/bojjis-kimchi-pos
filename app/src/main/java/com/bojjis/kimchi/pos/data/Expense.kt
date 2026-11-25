package com.bojjis.kimchi.pos.data

enum class ExpenseCategory(val label: String) {
    INGREDIENTS("Ingredients"),
    PACKAGING("Packaging"),
    TRANSPORT("Transport"),
    UTILITIES("Utilities"),
    OTHER("Other")
}

data class Expense(
    val id: Long,
    val timestamp: Long,
    val amount: Int,
    val category: ExpenseCategory,
    val note: String,
    val paymentMethod: PaymentMethod
)
