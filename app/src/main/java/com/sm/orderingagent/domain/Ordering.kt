package com.sm.orderingagent.domain

import java.util.UUID

enum class Allergen(val label: String) {
    PEANUT("花生"), MILK("乳制品"), EGG("鸡蛋"), WHEAT("小麦"), SOY("大豆"), FISH("鱼类");
}

data class Choice(val id: String, val label: String, val extraCents: Int = 0)
data class OptionGroup(val id: String, val label: String, val choices: List<Choice>, val defaultId: String)
data class MenuItem(
    val id: String,
    val name: String,
    val category: String,
    val priceCents: Int,
    val image: String,
    val description: String,
    val ingredients: String,
    val portion: String,
    val contains: Set<Allergen> = emptySet(),
    val mayContain: Set<Allergen> = emptySet(),
    val allergenInfoComplete: Boolean = true,
    val stock: Int = 20,
    val options: List<OptionGroup> = emptyList(),
) {
    val available: Boolean get() = stock > 0
    val defaults: Map<String, String> get() = options.associate { it.id to it.defaultId }
    fun unitPrice(selection: Map<String, String>): Int {
        if (selection.keys != options.map { it.id }.toSet()) throw OrderingException("请选择完整规格", "INVALID_OPTIONS")
        return priceCents + options.sumOf { group ->
            group.choices.firstOrNull { it.id == selection[group.id] }?.extraCents
                ?: throw OrderingException("${group.label}选项无效", "INVALID_OPTIONS")
        }
    }
    fun optionText(selection: Map<String, String>): String = options.joinToString(" / ") { group ->
        group.choices.firstOrNull { it.id == selection[group.id] }?.label ?: "未选择"
    }
}

data class CartItem(val id: String, val itemId: String, val quantity: Int, val options: Map<String, String>)
data class OrderLine(val name: String, val quantity: Int, val options: String, val unitPriceCents: Int)
data class Order(
    val id: String,
    val number: String,
    val createdAt: Long,
    val lines: List<OrderLine>,
    val totalCents: Int,
    val serviceMode: String,
    val partySize: Int,
    val allergens: Set<Allergen>,
    val paymentMethod: String,
    val summaryId: String? = null,
)
data class CommandReceipt(val requestId: String, val fingerprint: String, val resultJson: String)
data class OrderingState(
    val cart: List<CartItem> = emptyList(),
    val allergens: Set<Allergen> = emptySet(),
    val serviceMode: String = "堂食",
    val partySize: Int = 2,
    val version: Long = 0,
    val orders: List<Order> = emptyList(),
    val receipts: List<CommandReceipt> = emptyList(),
    val currentOrderId: String = UUID.randomUUID().toString(),
)
data class CheckoutQuote(val id: String, val version: Long, val totalCents: Int, val createdAt: Long)

class OrderingException(message: String, val code: String = "INVALID_ARGUMENT") : IllegalArgumentException(message)

class OrderingEngine(val menu: List<MenuItem>, initial: OrderingState = OrderingState()) {
    var state: OrderingState = initial
        private set
    private val byId = menu.associateBy { it.id }
    private val quotes = mutableMapOf<String, CheckoutQuote>()
    private val submitted = mutableMapOf<String, Order>()

    fun <Result> transaction(persist: (OrderingState) -> Unit, action: OrderingEngine.() -> Result): Result {
        val before = state
        val previousQuotes = quotes.toMap()
        val previousSubmitted = submitted.toMap()
        try {
            val result = action()
            persist(state)
            return result
        } catch (failure: Exception) {
            state = before
            quotes.clear()
            quotes.putAll(previousQuotes)
            submitted.clear()
            submitted.putAll(previousSubmitted)
            throw failure
        }
    }

    fun recordReceipt(receipt: CommandReceipt) {
        require(state.receipts.none { it.requestId == receipt.requestId })
        state = state.copy(receipts = state.receipts + receipt)
    }

    fun requireOrder(orderId: String) {
        if (orderId != state.currentOrderId) throw OrderingException("Order changed; read the current cart", "ORDER_ID_CONFLICT")
    }

    fun item(id: String): MenuItem = byId[id] ?: throw OrderingException("商品不存在", "ITEM_NOT_FOUND")
    fun unitPrice(line: CartItem): Int = item(line.itemId).unitPrice(line.options)
    fun totalCents(): Int = state.cart.sumOf { unitPrice(it) * it.quantity }
    fun quantity(itemId: String): Int = state.cart.filter { it.itemId == itemId }.sumOf { it.quantity }

    fun risks(product: MenuItem): List<String> = buildList {
        (state.allergens intersect product.contains).forEach { add("含${it.label}") }
        (state.allergens intersect product.mayContain).forEach { add("可能接触${it.label}") }
        if (state.allergens.isNotEmpty() && !product.allergenInfoComplete) add("过敏原资料不完整")
    }

    fun blockers(): List<String> = buildList {
        if (state.cart.isEmpty()) add("请先选择餐品")
        state.cart.forEach { line ->
            val product = item(line.itemId)
            risks(product).forEach { add("${product.name}：$it") }
            if (quantity(product.id) > remainingStock(product.id)) add("${product.name}：库存不足")
        }
    }.distinct()

    fun remainingStock(itemId: String): Int {
        val ordered = state.orders.sumOf { order ->
            order.lines.filter { it.name == item(itemId).name }.sumOf { it.quantity }
        }
        return (item(itemId).stock - ordered).coerceAtLeast(0)
    }

    fun add(itemId: String, quantity: Int, options: Map<String, String> = item(itemId).defaults) {
        val product = item(itemId)
        checkQuantity(quantity)
        product.unitPrice(options)
        checkRisks(product)
        if (this.quantity(itemId) + quantity > remainingStock(itemId)) throw OrderingException("${product.name}库存不足", "OUT_OF_STOCK")
        val existing = state.cart.firstOrNull { it.itemId == itemId && it.options == options }
        val cart = if (existing == null) {
            state.cart + CartItem(UUID.randomUUID().toString(), itemId, quantity, options.toMap())
        } else state.cart.map { if (it.id == existing.id) it.copy(quantity = it.quantity + quantity) else it }
        change(state.copy(cart = cart))
    }

    fun update(cartItemId: String, quantity: Int, options: Map<String, String>) {
        val line = state.cart.firstOrNull { it.id == cartItemId } ?: throw OrderingException("购物车条目不存在", "CART_ITEM_NOT_FOUND")
        val product = item(line.itemId)
        checkQuantity(quantity)
        product.unitPrice(options)
        checkRisks(product)
        if (this.quantity(line.itemId) - line.quantity + quantity > remainingStock(line.itemId)) throw OrderingException("库存不足", "OUT_OF_STOCK")
        val duplicate = state.cart.firstOrNull { it.id != line.id && it.itemId == line.itemId && it.options == options }
        val updated = if (duplicate == null) {
            state.cart.map { if (it.id == line.id) it.copy(quantity = quantity, options = options.toMap()) else it }
        } else {
            state.cart.filterNot { it.id == line.id }.map {
                if (it.id == duplicate.id) it.copy(quantity = it.quantity + quantity) else it
            }
        }
        change(state.copy(cart = updated))
    }

    fun remove(cartItemId: String) {
        if (state.cart.none { it.id == cartItemId }) throw OrderingException("购物车条目不存在", "CART_ITEM_NOT_FOUND")
        change(state.copy(cart = state.cart.filterNot { it.id == cartItemId }))
    }

    fun clear() = change(state.copy(cart = emptyList()))
    fun startNewOrder() {
        val defaults = OrderingState()
        change(state.copy(cart = defaults.cart, allergens = defaults.allergens,
            serviceMode = defaults.serviceMode, partySize = defaults.partySize,
            currentOrderId = defaults.currentOrderId))
    }
    fun setAllergens(allergens: Set<Allergen>) = change(state.copy(allergens = allergens.toSet()))
    fun setDining(serviceMode: String, partySize: Int) {
        require(serviceMode in listOf("堂食", "打包"))
        require(partySize in 1..12)
        change(state.copy(serviceMode = serviceMode, partySize = partySize))
    }

    fun quote(now: Long = System.currentTimeMillis()): CheckoutQuote {
        validateCheckout()
        return CheckoutQuote(UUID.randomUUID().toString(), state.version, totalCents(), now).also { quotes[it.id] = it }
    }

    fun checkout(quoteId: String, now: Long = System.currentTimeMillis()): CheckoutQuote {
        val quote = quotes[quoteId] ?: throw OrderingException("核对单已失效，请重新核对订单", "SUMMARY_NOT_FOUND")
        if (quote.version != state.version) throw OrderingException("订单已变更，请重新核对", "CART_VERSION_CONFLICT")
        if (now - quote.createdAt !in 0..300_000) throw OrderingException("核对单已过期，请重新核对", "SUMMARY_EXPIRED")
        validateCheckout()
        return quote
    }

    private fun validateCheckout() {
        if (state.cart.isEmpty()) throw OrderingException("请先选择餐品", "EMPTY_CART")
        state.cart.forEach { line ->
            checkRisks(item(line.itemId))
            if (quantity(line.itemId) > remainingStock(line.itemId)) throw OrderingException("库存不足", "OUT_OF_STOCK")
        }
    }

    fun submit(quoteId: String, paymentMethod: String, now: Long = System.currentTimeMillis()): Order {
        state.orders.firstOrNull { it.summaryId == quoteId }?.let { return it }
        submitted[quoteId]?.let { return it }
        val quote = checkout(quoteId, now)
        require(paymentMethod in listOf("扫码支付", "现金"))
        val order = Order(
            state.currentOrderId, "A%03d".format(state.orders.size + 1), now,
            state.cart.map { OrderLine(item(it.itemId).name, it.quantity, item(it.itemId).optionText(it.options), unitPrice(it)) },
            quote.totalCents, state.serviceMode, state.partySize, state.allergens.toSet(), paymentMethod, quoteId,
        )
        submitted[quoteId] = order
        state = state.copy(orders = listOf(order) + state.orders)
        startNewOrder()
        return order
    }

    private fun change(next: OrderingState) {
        state = next.copy(version = state.version + 1)
        quotes.clear()
    }
    private fun checkQuantity(quantity: Int) {
        if (quantity !in 1..99) throw OrderingException("数量应为1至99")
    }
    private fun checkRisks(product: MenuItem) {
        val risks = risks(product)
        if (risks.isNotEmpty()) throw OrderingException("${product.name}：${risks.joinToString("、")}，请更换餐品或咨询工作人员", "ALLERGY_CONFLICT")
    }
}

fun money(cents: Int): String = "¥%d.%02d".format(cents / 100, cents % 100)
