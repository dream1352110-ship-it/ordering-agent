package com.sm.orderingagent.operations

import android.app.Application
import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.sm.orderingagent.MainActivity
import com.sm.orderingagent.OrderingSession
import com.sm.orderingagent.domain.Allergen
import com.sm.orderingagent.domain.CheckoutQuote
import com.sm.orderingagent.domain.CommandReceipt
import com.sm.orderingagent.domain.MenuItem
import com.sm.orderingagent.domain.Order
import com.sm.orderingagent.domain.OrderingEngine
import com.sm.orderingagent.domain.OrderingException
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.ExecutionException
import java.util.concurrent.FutureTask

data class OrderingOperationResult(
    val success: Boolean,
    val resultJson: String? = null,
    val errorCode: String? = null,
    val errorMessage: String? = null,
)

class OrderingOperations(
    private val application: Application,
    private val launchActivity: (Intent) -> Unit = { application.startActivity(it) },
    val gaapPosGateway: com.sm.orderingagent.gateway.GaapPosGateway = com.sm.orderingagent.gateway.RealMaxIotGaapPosGateway(),
) {
    private val main = Handler(Looper.getMainLooper())
    private val session: OrderingSession get() = OrderingSession.get(application)
    private val legacyCategoryIds = mapOf(
        "主食" to "main", "轻食" to "light", "小食" to "snacks",
        "烘焙" to "bakery", "咖啡" to "coffee", "茶饮" to "tea", "冷饮" to "cold-drinks",
    )

    fun prepareConfirmation(raw: String): String = try {
        ensureLiveMenu()
        onMain { confirmation(parse(raw)) }
    } catch (failure: OperationFailure) {
        throw OrderingException(failure.message ?: "Operation failed", failure.code)
    }

    fun execute(id: String, raw: String): OrderingOperationResult = try {
        ensureLiveMenu()
        val result = onMain { dispatch(id, parse(raw)) }
        OrderingOperationResult(success = true, resultJson = result.toString())
    } catch (failure: Exception) {
        OrderingOperationResult(
            success = false,
            errorCode = when (failure) {
                is OperationFailure -> failure.code
                is OrderingException -> failure.code
                is IllegalArgumentException -> "INVALID_ARGUMENT"
                else -> "INTERNAL_ERROR"
            },
            errorMessage = failure.message ?: "Operation failed",
        )
    }

    fun reportHandover(customerUtterance: String, failureReason: String): com.sm.orderingagent.gateway.HandoverResponse {
        val cart = session.state.cart
        val orderId = session.state.currentOrderId
        return gaapPosGateway.reportHandover(
            com.sm.orderingagent.gateway.HandoverRequest(
                sessionId = "SESS-$orderId",
                currentOrderId = orderId,
                cartItems = cart,
                customerUtterance = customerUtterance,
                failureReason = failureReason,
            ),
        )
    }

    private fun dispatch(id: String, args: JSONObject): JSONObject {
        if (id in writeOperationIds) completed(id, args)?.let { return it }
        return when (id) {
            "list-menu" -> {
                keys(args, setOf("categoryId"))
                session.requestShowMenu()
                val category = if (args.has("categoryId")) text(args, "categoryId") else null
                val categories = categoryCatalog()
                if (category != null && category !in categories) fail("INVALID_ARGUMENT", "Unknown categoryId")
                base()
                    .put(
                        "categories",
                        JSONArray(categories.map { (key, label) ->
                            JSONObject().put("categoryId", key).put("name", label)
                        }),
                    )
                    .put("allergens", allergenDefinitions())
                    .put(
                        "items",
                        JSONArray(
                            session.menu
                                .filter { category == null || it.category == categories[category] }
                                .map(::productJson),
                        ),
                    )
                    .put("ui", launchJson(launch()))
                    .put("displayNotice", "已在点餐应用屏幕内打开菜单全览，请告知用户在屏幕上浏览，不要在此处罗列商品。")
            }
            "get-menu-item" -> {
                keys(args, setOf("itemId"))
                base().put("item", productJson(menuDetail(session.product(text(args, "itemId")))))
            }
            "search-menu" -> searchMenu(args)
            "apply-cart-changes" -> {
                keys(args, writeKeys + "changes")
                val changes = array(args, "changes")
                if (changes.length() == 0) fail("INVALID_ARGUMENT", "changes must not be empty")
                write(id, args) {
                    for (index in 0 until changes.length()) {
                        val change = changes.get(index) as? JSONObject
                            ?: fail("INVALID_ARGUMENT", "changes must contain objects")
                        val operation = text(change, "operation")
                        val fields = JSONObject(change.toString()).apply { remove("operation") }
                        applyChange(operation, fields)
                    }
                    cartJson(this).put("appliedChanges", changes.length())
                }
            }
            "get-cart" -> {
                keys(args, emptySet())
                cartJson()
            }
            "start-new-order" -> {
                keys(args, writeKeys)
                var executed = false
                val result = write(id, args) {
                    startNewOrder()
                    executed = true
                    cartJson(this).put("resetApplied", true)
                }
                if (executed) session.clearTransientForNewOrder()
                result
            }
            "set-dining-settings", "add-cart-item", "update-cart-item", "remove-cart-item",
            "set-allergy-requirements" -> {
                val fields = JSONObject(args.toString()).apply { writeKeys.forEach { remove(it) } }
                write(id, args) {
                    applyChange(id, fields)
                    cartJson(this)
                }
            }
            "calculate-price" -> calculate(args)
            "get-store-order" -> storeOrder(args)
            "open-checkout", "review-order" -> {
                keys(args, setOf("expectedCartVersion"))
                if (args.has("expectedCartVersion")) {
                    checkVersion(version(args), session.state.version)
                }
                session.requestReviewOrder()
                val result = base()
                if (session.state.cart.isNotEmpty()) {
                    try {
                        val quote = session.quote()
                        session.requestCheckout(quote)
                        result.put("summary", quoteJson(quote))
                    } catch (_: Exception) {}
                }
                result.put("cart", cartJson())
                result.put("ui", launchJson(launch()))
            }
            "submit-demo-order" -> {
                keys(args, writeKeys + setOf("summaryId", "expectedCartVersion"))
                if (!args.has("summaryId")) {
                    session.requestReviewOrder()
                    val fallbackResult = base().put("status", "pending_review").put("cart", cartJson())
                    if (session.state.cart.isNotEmpty()) {
                        try {
                            val quote = session.quote()
                            session.requestCheckout(quote)
                            fallbackResult.put("summary", quoteJson(quote))
                        } catch (_: Exception) {}
                    }
                    fallbackResult.put("ui", launchJson(launch()))
                    return fallbackResult
                }
                val summaryId = text(args, "summaryId")
                val liveStore = !session.usesLocalDemoMenu &&
                    gaapPosGateway is com.sm.orderingagent.gateway.RealMaxIotGaapPosGateway
                val submitGateway = if (liveStore || gaapPosGateway !is com.sm.orderingagent.gateway.RealMaxIotGaapPosGateway) {
                    gaapPosGateway
                } else {
                    com.sm.orderingagent.gateway.MockGaapPosGateway()
                }
                var committedOrder: Order? = null
                val result = write(id, args) {
                    checkout(summaryId)
                    val order = submit(summaryId, "扫码支付")
                    committedOrder = order
                    val gaapResult = submitGateway.submitOrder(order, summaryId, state.serviceMode)
                    if (!gaapResult.isSuccess) {
                        fail(gaapResult.errorCode ?: "POS_SUBMIT_FAILED", gaapResult.errorMessage ?: "门店创单失败")
                    }
                    val storeOrder = if (liveStore) storeOrderSnapshot(gaapResult.gaapOrderId) else null
                    base(state.version, order.id).put("nextOrderId", state.currentOrderId)
                        .put("status", if (liveStore) gaapResult.status else "simulated").put("paymentProcessed", false)
                        .put("summaryId", summaryId).put("order", orderJson(order))
                        .put(
                            "gaap",
                            JSONObject()
                                .put("gaapOrderId", gaapResult.gaapOrderId)
                                .put("displayOrderNo", gaapResult.displayOrderNo)
                                .put("pickupWindow", gaapResult.pickupWindow)
                                .put("estimatedWaitSeconds", gaapResult.estimatedWaitSeconds)
                                .put("status", gaapResult.status),
                        )
                        .apply { if (storeOrder != null) put("storeOrder", storeOrder) }
                        .put("ui", JSONObject().put("screenState", "requested").put("foregroundGuaranteed", false))
                }
                committedOrder?.let { order ->
                    session.showRemoteOrder(order)
                    launch()
                }
                result
            }
            "request-staff-assistance" -> {
                keys(args, setOf("reason"))
                val reason = if (args.has("reason")) text(args, "reason") else "Customer requested a team member"
                session.requestStaffAssistance(reason)
                base().put("status", "requested").put("reason", reason)
                    .put("ui", launchJson(launch()))
                    .put("displayNotice", "已在点餐应用屏幕内打开菜单全览，请告知用户在屏幕上浏览，不要在此处罗列商品。")
            }
            "show-menu", "view-menu" -> {
                keys(args, emptySet())
                session.requestShowMenu()
                val launched = launch()
                base().put("status", "menu_displayed")
                    .put("action", "jump_to_menu_screen")
                    .put("message", "已在点餐应用内跳转到菜单全览页，请直接在应用屏幕上查看与选购。")
                    .put("ui", launchJson(launched))
            }
            else -> fail("FUNCTION_NOT_FOUND", "Unknown function")
        }
    }

    private fun OrderingEngine.applyChange(id: String, args: JSONObject) {
        when (id) {
            "add-cart-item" -> {
                keys(args, setOf("itemId", "quantity", "options"))
                add(text(args, "itemId"), quantity(args), options(args))
            }
            "update-cart-item", "adjust-cart-item" -> {
                val quantityKey = if (id == "adjust-cart-item") "delta" else "quantity"
                keys(args, setOf("cartItemId", quantityKey, "options"))
                if (!args.has(quantityKey) && !args.has("options")) fail("INVALID_ARGUMENT", "Provide $quantityKey or options")
                val lineId = text(args, "cartItemId")
                val line = state.cart.firstOrNull { it.id == lineId }
                    ?: fail("CART_ITEM_NOT_FOUND", "Cart item does not exist")
                val nextQuantity = if (id == "adjust-cart-item") {
                    val delta = number(args, "delta")
                    if (delta !in -99L..99L || delta == 0L) fail("INVALID_ARGUMENT", "delta must be -99..99, nonzero")
                    val next = line.quantity.toLong() + delta
                    if (next !in 0L..99L) fail("INVALID_ARGUMENT", "Resulting quantity must be 0..99")
                    next.toInt()
                } else if (args.has("quantity")) quantity(args) else line.quantity
                if (nextQuantity == 0) {
                    if (args.has("options")) fail("INVALID_ARGUMENT", "Cannot change options when removing a line")
                    remove(lineId)
                } else update(lineId, nextQuantity, if (args.has("options")) options(args) else line.options)
            }
            "remove-cart-item" -> {
                keys(args, setOf("cartItemId"))
                remove(text(args, "cartItemId"))
            }
            "set-dining-settings" -> {
                keys(args, setOf("serviceMode", "partySize"))
                if (!args.has("serviceMode") && !args.has("partySize")) fail("INVALID_ARGUMENT", "Provide serviceMode or partySize")
                val mode = if (args.has("serviceMode")) when (text(args, "serviceMode")) {
                    "dine-in" -> "堂食"
                    "takeout" -> "打包"
                    else -> fail("INVALID_ARGUMENT", "serviceMode must be dine-in or takeout")
                } else state.serviceMode
                val people = if (args.has("partySize")) number(args, "partySize").also {
                    if (it !in 1L..12L) fail("INVALID_ARGUMENT", "partySize must be 1..12")
                }.toInt() else state.partySize
                setDining(mode, people)
            }
            "set-allergy-requirements" -> {
                keys(args, setOf("allergenIds"))
                val ids = array(args, "allergenIds")
                setAllergens((0 until ids.length()).map { index ->
                    val value = ids.get(index)
                    if (value !is String) fail("INVALID_ARGUMENT", "allergenIds must contain strings")
                    Allergen.entries.firstOrNull { allergenId(it) == value }
                        ?: fail("INVALID_ARGUMENT", "Unknown allergenId")
                }.toSet())
            }
            else -> fail("INVALID_ARGUMENT", "Unsupported cart change operation")
        }
    }

    private fun searchMenu(args: JSONObject): JSONObject {
        keys(args, setOf("names", "itemIds"))
        fun values(key: String): List<String> {
            if (!args.has(key)) return emptyList()
            val values = array(args, key)
            return (0 until values.length()).map { index ->
                val value = values.get(index)
                if (value !is String || value.isBlank() || value.length > 128) fail("INVALID_ARGUMENT", "$key must contain nonblank strings of 1..128 characters")
                value
            }
        }
        val names = values("names")
        val ids = values("itemIds")
        if (names.isEmpty() && ids.isEmpty()) fail("INVALID_ARGUMENT", "Provide names or itemIds")
        val matches = session.menu.filter { product ->
            product.id in ids || names.any { product.name.contains(it, ignoreCase = true) }
        }
        return base().put("items", JSONArray(matches.map { product ->
            productJson(product).apply {
                listOf("description", "ingredients", "portion", "categoryName", "currency", "status").forEach { remove(it) }
            }
        })).put("unmatchedItemIds", JSONArray(ids.distinct().filter { id -> matches.none { it.id == id } }))
            .put("unmatchedNames", JSONArray(names.distinct().filter { name -> matches.none { it.name.contains(name, ignoreCase = true) } }))
    }

    private fun write(
        id: String,
        args: JSONObject,
        action: OrderingEngine.() -> JSONObject,
    ): JSONObject {
        val requestId = text(args, "requestId")
        val orderId = if (args.has("orderId") || id in setOf("start-new-order", "submit-demo-order")) {
            text(args, "orderId")
        } else null
        val fingerprint = fingerprint(id, args)
        completed(id, args)?.let { return it }
        return session.mutate {
            if (orderId != null) requireOrder(orderId)
            if (id == "submit-demo-order") checkVersion(version(args), state.version)
            val result = action().put("requestId", requestId).put("operation", id)
            recordReceipt(CommandReceipt(requestId, fingerprint, result.toString()))
            result
        }
    }

    private fun fingerprint(id: String, args: JSONObject): String {
        val arguments = JSONObject(args.toString()).apply { remove("requestId") }
        return canonical(JSONObject().put("function", id).put("arguments", arguments))
    }

    private fun completed(id: String, args: JSONObject): JSONObject? {
        val requestId = text(args, "requestId")
        val fingerprint = fingerprint(id, args)
        return session.inspect {
            state.receipts.firstOrNull { it.requestId == requestId }?.let { receipt ->
                if (receipt.fingerprint != fingerprint) fail("REQUEST_ID_REUSED", "requestId was used with different arguments")
                JSONObject(receipt.resultJson)
            }
        }
    }

    private fun confirmation(args: JSONObject): String {
        completed("submit-demo-order", args)?.let { result ->
            val order = result.getJSONObject("order")
            val summaryId = result.getString("summaryId")
            val replay = if (session.usesLocalDemoMenu) "该模拟订单已提交，重试只返回原结果，不会再次下单或扣款。" else "该门店订单已提交，重试只返回原结果，不会再次创单。"
            return "$replay 订单：${order.getString("number")}，核对单：$summaryId，合计 CNY ${amount(order.getInt("totalCents"))}。"
        }
        keys(args, writeKeys + setOf("summaryId", "expectedCartVersion"))
        text(args, "requestId")
        val orderId = text(args, "orderId")
        val summaryId = text(args, "summaryId")
        val expected = version(args)
        return session.inspect {
            requireOrder(orderId)
            checkVersion(expected, state.version)
            val quote = checkout(summaryId)
            buildString {
                if (session.usesLocalDemoMenu) {
                    append("确认模拟下单？不会真实扣款或通知厨房。\n核对单：")
                } else {
                    append("确认提交门店订单？本机不扣款，订单将发送到门店。\n核对单：")
                }
                append(quote.id)
                append("\n购物车版本：").append(state.version)
                state.cart.forEachIndexed { index, line ->
                    val product = item(line.itemId)
                    val unit = product.unitPrice(line.options)
                    append("\n").append(index + 1).append(". ").append(product.name)
                        .append(" [").append(line.id).append("] x").append(line.quantity)
                        .append("; options=").append(canonical(JSONObject(line.options)))
                        .append(" (").append(product.optionText(line.options)).append(")")
                        .append("; unit CNY ").append(amount(unit))
                        .append("; subtotal CNY ").append(amount(unit * line.quantity))
                }
                append("\n合计 CNY ").append(amount(quote.totalCents))
                append("\n过敏原要求：").append(state.allergens.sortedBy(::allergenId)
                    .joinToString { "${allergenId(it)} (${it.label})" }.ifEmpty { "未设置" })
                append("\n用餐方式：").append(state.serviceMode).append("；人数：").append(state.partySize)
                if (session.usesLocalDemoMenu) {
                    append("\n付款方式：扫码支付（模拟）。\n")
                } else {
                    append("\n付款方式：到店支付，本机不扣款。\n")
                }
                append(disclaimer)
            }
        }
    }

    private fun calculate(args: JSONObject): JSONObject {
        keys(args, setOf("items"))
        val items = array(args, "items")
        if (items.length() == 0) fail("INVALID_ARGUMENT", "items must not be empty")
        val specs = (0 until items.length()).map { index ->
            val item = items.get(index) as? JSONObject ?: fail("INVALID_ARGUMENT", "items must contain objects")
            keys(item, setOf("itemId", "quantity", "options"))
            PriceSpec(text(item, "itemId"), quantity(item), options(item))
        }
        val quantities = specs.groupBy { it.itemId }.mapValues { (_, lines) -> lines.sumOf { it.quantity } }
        var total = 0L
        var complete = true
        val lines = specs.mapIndexed { index, spec ->
            val issues = mutableListOf<String>()
            val product = session.menu.firstOrNull { it.id == spec.itemId }
            var unit: Int? = null
            if (product == null) {
                issues += "ITEM_NOT_FOUND"
            } else {
                try {
                    unit = product.unitPrice(spec.options)
                } catch (_: IllegalArgumentException) {
                    issues += "INVALID_OPTIONS"
                }
                issues += session.risks(product)
                if (quantities.getValue(spec.itemId) > session.remainingStock(spec.itemId)) issues += "OUT_OF_STOCK"
            }
            val valid = issues.isEmpty()
            val subtotal = unit?.toLong()?.times(spec.quantity)
            if (valid) total += subtotal ?: 0L else complete = false
            JSONObject().put("index", index).put("itemId", spec.itemId).put("quantity", spec.quantity)
                .put("options", JSONObject(spec.options)).put("valid", valid).put("issues", JSONArray(issues))
                .put("unitPriceCents", unit ?: JSONObject.NULL)
                .put("subtotalCents", if (valid) subtotal ?: JSONObject.NULL else JSONObject.NULL)
        }
        return base().put("complete", complete).put("lines", JSONArray(lines))
            .put("validLinesTotalCents", total).put("totalCents", if (complete) total else JSONObject.NULL)
            .put("allergenIds", allergensJson(session.state.allergens))
    }

    private fun cartJson(): JSONObject = session.inspect { cartJson(this) }

    private fun cartJson(engine: OrderingEngine): JSONObject = with(engine) {
        base(state.version, state.currentOrderId).put("lines", JSONArray(state.cart.map { line ->
            val product = item(line.itemId)
            val unit = product.unitPrice(line.options)
            JSONObject().put("cartItemId", line.id).put("itemId", line.itemId).put("name", product.name)
                .put("quantity", line.quantity).put("options", JSONObject(line.options))
                .put("optionText", product.optionText(line.options)).put("unitPriceCents", unit)
                .put("subtotalCents", unit * line.quantity).put("conflicts", JSONArray(risks(product)))
        })).put("totalCents", totalCents()).put("blockers", JSONArray(blockers()))
            .put("allergenIds", allergensJson(state.allergens)).put("serviceMode", state.serviceMode)
            .put("serviceModeId", if (state.serviceMode == "打包") "takeout" else "dine-in")
            .put("partySize", state.partySize)
    }

    private fun productJson(product: MenuItem): JSONObject {
        val remaining = session.remainingStock(product.id)
        val categoryId = product.categoryId.ifBlank {
            categoryCatalog().entries.firstOrNull { it.value == product.category }?.key.orEmpty()
        }
        return JSONObject().put("itemId", product.id).put("name", product.name)
            .put("categoryId", if (categoryId.isBlank()) JSONObject.NULL else categoryId)
            .put("categoryName", product.category).put("priceCents", product.priceCents)
            .put("currency", product.currency.ifBlank { storeCurrency() }).put("description", product.description).put("ingredients", product.ingredients)
            .put("portion", product.portion).put("stock", remaining).put("available", remaining > 0)
            .put("status", if (remaining > 0) "available" else "sold-out")
            .put("contains", allergensJson(product.contains)).put("mayContain", allergensJson(product.mayContain))
            .put("allergenInfoComplete", product.allergenInfoComplete).put("conflicts", JSONArray(session.risks(product)))
            .put("defaultOptions", JSONObject(product.defaults)).put("optionGroups", JSONArray(product.options.map { group ->
                JSONObject().put("id", group.id).put("name", group.label).put("defaultId", group.defaultId)
                    .put("required", group.kind == "option" || group.kind == "spec").put("kind", group.kind)
                    .put("choices", JSONArray(group.choices.map { choice ->
                        JSONObject().put("id", choice.id).put("name", choice.label).put("extraCents", choice.extraCents)
                    }))
            }))
    }

    private fun quoteJson(quote: CheckoutQuote): JSONObject = JSONObject()
        .put("orderId", session.state.currentOrderId)
        .put("summaryId", quote.id).put("cartVersion", quote.version).put("totalCents", quote.totalCents)
        .put("currency", storeCurrency()).put("createdAt", quote.createdAt).put("expiresAt", quote.createdAt + 300_000L)

    private fun orderJson(order: Order): JSONObject = JSONObject()
        .put("orderId", order.id).put("number", order.number).put("createdAt", order.createdAt)
        .put("summaryId", order.summaryId ?: JSONObject.NULL).put("totalCents", order.totalCents)
        .put("currency", order.lines.firstOrNull()?.currency?.ifBlank { storeCurrency() } ?: storeCurrency())
        .put("serviceMode", order.serviceMode).put("partySize", order.partySize)
        .put("allergenIds", allergensJson(order.allergens)).put("paymentMethod", order.paymentMethod)
        .put("isDemo", session.usesLocalDemoMenu).put("status", if (session.usesLocalDemoMenu) "simulated" else "submitted")
        .put("paymentProcessed", false)
        .put("lines", JSONArray(order.lines.map { line ->
            JSONObject().put("name", line.name).put("quantity", line.quantity).put("optionText", line.options)
                .put("unitPriceCents", line.unitPriceCents).put("skuId", line.skuId)
                .put("subtotalCents", line.unitPriceCents * line.quantity)
        }))

    private fun base(version: Long = session.state.version, orderId: String = session.state.currentOrderId): JSONObject = JSONObject()
        .put("orderId", orderId)
        .put("isDemo", session.usesLocalDemoMenu).put("currency", storeCurrency()).put("cartVersion", version).put("allergenDisclaimer", disclaimer)

    private fun allergenDefinitions(): JSONArray = JSONArray(Allergen.entries.map {
        JSONObject().put("id", allergenId(it)).put("name", it.label)
    })

    private fun allergensJson(allergens: Set<Allergen>): JSONArray = JSONArray(allergens.map(::allergenId).sorted())
    private fun allergenId(allergen: Allergen): String = allergen.name.lowercase(Locale.ROOT)
    private fun amount(cents: Int): String = String.format(Locale.ROOT, "%d.%02d", cents / 100, cents % 100)

    private fun launch(): Boolean = try {
        val intent = Intent(application, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            )
        }
        launchActivity(intent)
        true
    } catch (_: Exception) {
        false
    }

    private fun launchJson(requested: Boolean): JSONObject = JSONObject().put("screenState", "requested")
        .put("launchRequested", requested).put("foregroundGuaranteed", false)
        .put("warning", "Android may restrict background launches; review remains pending until the app opens.")

    private fun parse(raw: String): JSONObject = parseArguments(raw)

    private fun keys(json: JSONObject, allowed: Set<String>) {
        if (json.keys().asSequence().any { it !in allowed }) fail("INVALID_ARGUMENT", "Unknown argument key")
    }

    private fun text(json: JSONObject, key: String): String {
        val value = json.opt(key)
        if (value !is String || value.length !in 1..128 || value.isBlank()) {
            fail("INVALID_ARGUMENT", "$key must be a nonblank string of 1..128 characters")
        }
        return value
    }

    private fun number(json: JSONObject, key: String): Long {
        val value = json.opt(key)
        return when (value) {
            is Int -> value.toLong()
            is Long -> value
            else -> fail("INVALID_ARGUMENT", "$key must be an integer")
        }
    }

    private fun quantity(json: JSONObject): Int {
        val value = number(json, "quantity")
        if (value !in 1L..99L) fail("INVALID_ARGUMENT", "quantity must be 1..99")
        return value.toInt()
    }

    private fun version(json: JSONObject): Long = number(json, "expectedCartVersion").also {
        if (it < 0) fail("INVALID_ARGUMENT", "expectedCartVersion must be nonnegative")
    }

    private fun options(json: JSONObject): Map<String, String> {
        val value = json.opt("options") as? JSONObject ?: fail("INVALID_ARGUMENT", "options must be an object")
        return value.keys().asSequence().associateWith { key -> text(value, key) }
    }

    private fun array(json: JSONObject, key: String): JSONArray {
        val value = json.opt(key) as? JSONArray ?: fail("INVALID_ARGUMENT", "$key must be an array")
        if (value.length() > 100) fail("INVALID_ARGUMENT", "$key has more than 100 entries")
        return value
    }

    private fun checkVersion(expected: Long, actual: Long) {
        if (expected != actual) fail("CART_VERSION_CONFLICT", "Cart version changed; expected $expected, actual $actual")
    }

    private fun canonical(value: Any?): String = when (value) {
        null, JSONObject.NULL -> "null"
        is JSONObject -> value.keys().asSequence().toList().sorted().joinToString(",", "{", "}") {
            JSONObject.quote(it) + ":" + canonical(value.get(it))
        }
        is JSONArray -> (0 until value.length()).joinToString(",", "[", "]") { canonical(value.get(it)) }
        is String -> JSONObject.quote(value)
        else -> value.toString()
    }

    private fun <Result> onMain(action: () -> Result): Result {
        if (Looper.myLooper() == Looper.getMainLooper()) return action()
        val task = FutureTask<Result> { action() }
        if (!main.post(task)) fail("INTERNAL_ERROR", "Main thread is unavailable")
        var interrupted = false
        try {
            while (true) {
                try {
                    return task.get()
                } catch (_: InterruptedException) {
                    interrupted = true
                } catch (failure: ExecutionException) {
                    throw failure.cause ?: failure
                }
            }
        } finally {
            if (interrupted) Thread.currentThread().interrupt()
        }
    }

    private fun categoryCatalog(): Map<String, String> {
        val used = mutableSetOf<String>()
        return session.menu.distinctBy { it.category }.associate { item ->
            val base = item.categoryId.ifBlank {
                legacyCategoryIds[item.category] ?: "c" + item.category.hashCode().toUInt().toString(16)
            }
            var id = base
            var suffix = 2
            while (!used.add(id)) {
                id = "$base-$suffix"
                suffix++
            }
            id to item.category
        }
    }

    private fun storeCurrency(): String =
        session.menu.firstOrNull { it.currency.isNotBlank() }?.currency ?: "CNY"

    private fun storeOrder(args: JSONObject): JSONObject {
        keys(args, setOf("orderId"))
        val orderId = text(args, "orderId")
        val snapshot = storeOrderSnapshot(orderId)
            ?: fail("STORE_ORDER_UNAVAILABLE", "门店订单不存在或暂时无法查询")
        return base().put("storeOrder", snapshot)
    }

    private fun storeOrderSnapshot(orderId: String): JSONObject? {
        val gateway = gaapPosGateway as? com.sm.orderingagent.gateway.RealMaxIotGaapPosGateway ?: return null
        return try {
            val response = gateway.queryOrderInfo(orderId)
            if (com.sm.orderingagent.gateway.RealMaxIotGaapPosGateway.isBusinessSuccess(response)) {
                response.optJSONObject("data")
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun menuDetail(item: com.sm.orderingagent.domain.MenuItem): com.sm.orderingagent.domain.MenuItem {
        if (session.usesLocalDemoMenu || item.spuId.isBlank()) return item
        val gateway = gaapPosGateway as? com.sm.orderingagent.gateway.RealMaxIotGaapPosGateway ?: return item
        val response = try {
            gateway.queryMenuProductInfo(item.spuId)
        } catch (failure: Exception) {
            throw OrderingException(failure.message ?: "商品详情查询失败", "MENU_UNAVAILABLE")
        }
        if (!com.sm.orderingagent.gateway.RealMaxIotGaapPosGateway.isBusinessSuccess(response)) {
            fail("ITEM_NOT_FOUND", response.optString("msg", "商品详情不可用"))
        }
        return com.sm.orderingagent.gateway.MaxIotMenuConverter.convertDetail(response) ?: item
    }

    private fun ensureLiveMenu() {
        if (session.usesLocalDemoMenu) return
        try {
            val gateway = gaapPosGateway as? com.sm.orderingagent.gateway.RealMaxIotGaapPosGateway
                ?: throw OrderingException("未配置门店菜单网关", "MENU_UNAVAILABLE")
            session.ensureLiveMenu {
                com.sm.orderingagent.gateway.MaxIotMenuConverter.convert(gateway.fetchRealMenuProducts())
            }
        } catch (failure: OrderingException) {
            throw failure
        } catch (failure: Exception) {
            throw OrderingException(failure.message ?: "门店菜单加载失败", "MENU_UNAVAILABLE")
        }
    }

    private fun fail(code: String, message: String): Nothing = throw OperationFailure(code, message)
    private class OperationFailure(val code: String, message: String) : IllegalArgumentException(message)
    private data class PriceSpec(val itemId: String, val quantity: Int, val options: Map<String, String>)

    companion object {
        fun parseArguments(raw: String): JSONObject {
            if (raw.length > 65_536 || raw.toByteArray(Charsets.UTF_8).size > 65_536) {
                throw OrderingException("JSON exceeds 64 KiB", "INVALID_ARGUMENT")
            }
            try {
                android.util.JsonReader(java.io.StringReader(raw)).use { reader ->
                    reader.isLenient = false
                    reader.beginObject()
                    while (reader.hasNext()) {
                        reader.nextName()
                        reader.skipValue()
                    }
                    reader.endObject()
                    require(reader.peek() == android.util.JsonToken.END_DOCUMENT)
                }
                return JSONObject(raw)
            } catch (_: Exception) {
                throw OrderingException("Expected a strict JSON object without trailing content", "INVALID_ARGUMENT")
            }
        }

        private val writeKeys = setOf("requestId", "orderId")
        private val writeOperationIds = setOf("start-new-order", "set-dining-settings", "add-cart-item",
            "update-cart-item", "remove-cart-item", "set-allergy-requirements", "apply-cart-changes", "submit-demo-order")
        private const val disclaimer = "Demo allergen information is not a safety guarantee. Cross-contact may occur; consult restaurant staff."
        val operationIds: List<String> = listOf(
            "list-menu", "get-menu-item", "search-menu", "get-cart", "apply-cart-changes", "start-new-order", "set-dining-settings",
            "add-cart-item", "update-cart-item",
            "remove-cart-item", "set-allergy-requirements", "calculate-price", "open-checkout", "review-order", "submit-demo-order",
            "get-store-order", "request-staff-assistance", "show-menu",
        )
    }
}
