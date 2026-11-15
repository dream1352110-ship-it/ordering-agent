package com.sm.orderingagent

import android.app.Application
import android.util.AtomicFile
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.sm.orderingagent.domain.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.FutureTask

class OrderingViewModel(application: Application) : AndroidViewModel(application) {
    private val session = OrderingSession.get(application)
    val state: OrderingState get() = session.state
    val menu get() = session.menu
    val categories get() = session.categories
    val pendingCheckout get() = session.pendingCheckout
    val latestRemoteOrder get() = session.latestRemoteOrder
    val orderGeneration get() = session.orderGeneration
    fun refreshMenu() = session.refreshMenuFromCloud()
    fun acknowledgeCheckout() = session.acknowledgeCheckout()
    fun acknowledgeOrder() = session.acknowledgeOrder()
    fun product(id: String) = session.product(id)
    fun totalCents() = session.totalCents()
    fun risks(product: MenuItem) = session.risks(product)
    fun remainingStock(id: String) = session.remainingStock(id)
    fun blockers() = session.blockers()
    fun quote() = session.quote()
    fun add(itemId: String, quantity: Int, options: Map<String, String>) = session.mutate { add(itemId, quantity, options) }
    fun update(cartItemId: String, quantity: Int, options: Map<String, String>) = session.mutate { update(cartItemId, quantity, options) }
    fun remove(id: String) = session.mutate { remove(id) }
    fun clear() = session.mutate { clear() }
    fun startNewOrder() = session.startNewOrder()
    fun setAllergens(allergens: Set<Allergen>) = session.mutate { setAllergens(allergens) }
    fun setDining(mode: String, partySize: Int) = session.mutate { setDining(mode, partySize) }
    fun submit(quoteId: String, paymentMethod: String): Order = session.mutate { submit(quoteId, paymentMethod) }
}

class OrderingSession private constructor(application: Application) {
    private val store = StateStore(File(application.filesDir, "ordering-state.json"))
    private val menuLock = Any()
    val usesLocalDemoMenu: Boolean = isInstrumentation()
    private val initialMenu = if (usesLocalDemoMenu) DemoMenu.items else emptyList()
    private var engine = OrderingEngine(initialMenu, store.load())
    @Volatile private var liveMenuReady = usesLocalDemoMenu
    var state by mutableStateOf(engine.state)
        private set
    var menu by mutableStateOf(initialMenu)
        private set
    var categories by mutableStateOf(listOf("全部") + initialMenu.map { it.category }.distinct())
        private set
    var pendingCheckout by mutableStateOf<CheckoutQuote?>(null)
        private set
    var latestRemoteOrder by mutableStateOf<Order?>(null)
        private set
    var orderGeneration by mutableStateOf(0L)
        private set

    init {
        refreshMenuFromCloud()
    }

    fun refreshMenuFromCloud() {
        if (usesLocalDemoMenu) return
        Thread {
            try {
                ensureLiveMenu {
                    val gateway = com.sm.orderingagent.gateway.RealMaxIotGaapPosGateway()
                    val responseJson = gateway.fetchRealMenuProducts(pageSize = 50)
                    com.sm.orderingagent.gateway.MaxIotMenuConverter.convert(responseJson)
                }
            } catch (_: Exception) {
                // 保持未就绪，下一次 Function / CLI 调用会重试
            }
        }.start()
    }

    fun ensureLiveMenu(fetch: () -> List<MenuItem>) {
        if (liveMenuReady) return
        synchronized(menuLock) {
            if (liveMenuReady) return
            val items = MenuCatalog.acceptCloudMenu(fetch())
            val task = FutureTask { applyLiveMenu(items) }
            if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
                task.run()
            } else {
                if (!android.os.Handler(android.os.Looper.getMainLooper()).post(task)) {
                    throw OrderingException("主线程不可用", "INTERNAL_ERROR")
                }
                task.get()
            }
            liveMenuReady = true
        }
    }

    private fun applyLiveMenu(items: List<MenuItem>) {
        val validCart = engine.state.cart.filter { cartItem -> items.any { it.id == cartItem.itemId } }
        val sanitized = if (validCart.size == engine.state.cart.size) {
            engine.state
        } else {
            engine.state.copy(cart = validCart, version = engine.state.version + 1)
        }
        menu = items
        categories = listOf("全部") + items.map { it.category }.distinct()
        engine = OrderingEngine(items, sanitized)
        state = engine.state
        store.save(state)
    }
    fun clearTransientForNewOrder() {
        pendingCheckout = null
        latestRemoteOrder = null
        orderGeneration += 1
    }
    fun requestCheckout(quote: CheckoutQuote) { pendingCheckout = quote }
    fun acknowledgeCheckout() { pendingCheckout = null }
    fun showRemoteOrder(order: Order) { pendingCheckout = null; latestRemoteOrder = order }
    fun acknowledgeOrder() { latestRemoteOrder = null }
    fun <Result> inspect(action: OrderingEngine.() -> Result): Result {
        check(android.os.Looper.myLooper() == android.os.Looper.getMainLooper())
        return engine.action()
    }
    fun product(id: String) = engine.item(id)
    fun totalCents() = engine.totalCents()
    fun risks(product: MenuItem) = engine.risks(product)
    fun remainingStock(id: String) = engine.remainingStock(id)
    fun blockers() = engine.blockers()
    fun quote() = engine.quote()
    fun add(itemId: String, quantity: Int, options: Map<String, String>) = mutate { add(itemId, quantity, options) }
    fun update(cartItemId: String, quantity: Int, options: Map<String, String>) = mutate { update(cartItemId, quantity, options) }
    fun remove(id: String) = mutate { remove(id) }
    fun clear() = mutate { clear() }
    fun startNewOrder() {
        mutate { startNewOrder() }
        clearTransientForNewOrder()
    }
    fun setAllergens(allergens: Set<Allergen>) = mutate { setAllergens(allergens) }
    fun setDining(mode: String, partySize: Int) = mutate { setDining(mode, partySize) }
    fun submit(quoteId: String, paymentMethod: String): Order = mutate { submit(quoteId, paymentMethod) }

    fun <Result> mutate(action: OrderingEngine.() -> Result): Result {
        check(android.os.Looper.myLooper() == android.os.Looper.getMainLooper())
        val result = engine.transaction(store::save, action)
        state = engine.state
        return result
    }

    companion object {
        internal fun isInstrumentation(): Boolean =
            System.getProperty("robolectric.logging.enabled") != null || System.getProperty("android.specs") != null

        @Volatile private var instance: OrderingSession? = null
        fun get(application: Application): OrderingSession = instance ?: synchronized(this) {
            instance ?: OrderingSession(application).also { instance = it }
        }
        internal fun resetForTests() { instance = null }
    }
}

internal class StateStore(file: File) {
    private val atomicFile = AtomicFile(file)
    fun load(): OrderingState {
        if (!atomicFile.baseFile.exists() && !File(atomicFile.baseFile.path + ".bak").exists()) return OrderingState().also(::save)
        return try {
            val json = JSONObject(atomicFile.openRead().use { it.readBytes().toString(Charsets.UTF_8) })
            require(json.getInt("schema") == 1)
            val currentOrderId = if (json.has("currentOrderId")) {
                require(!json.isNull("currentOrderId"))
                val value = json.get("currentOrderId")
                require(value is String && value.isNotBlank())
                value
            } else java.util.UUID.randomUUID().toString()
            val state = OrderingState(
                currentOrderId = currentOrderId,
                cart = json.getJSONArray("cart").objects().map { line ->
                    val options = line.getJSONObject("options")
                    CartItem(line.getString("id"), line.getString("itemId"), line.getInt("quantity"), options.keys().asSequence().associateWith { options.getString(it) })
                },
                allergens = json.getJSONArray("allergens").allergens(),
                serviceMode = json.getString("serviceMode"), partySize = json.getInt("partySize"),
                version = json.getLong("version"),
                orders = json.getJSONArray("orders").objects().map { order ->
                    Order(order.getString("id"), order.getString("number"), order.getLong("createdAt"),
                        order.getJSONArray("lines").objects().map { line ->
                            OrderLine(
                                line.getString("name"),
                                line.getInt("quantity"),
                                line.getString("options"),
                                line.getInt("unitPriceCents"),
                                if (line.has("skuId") && !line.isNull("skuId")) line.getString("skuId") else "",
                            )
                        }, order.getInt("totalCents"), order.getString("serviceMode"), order.getInt("partySize"),
                        order.getJSONArray("allergens").allergens(), order.getString("paymentMethod"),
                        if (order.has("summaryId") && !order.isNull("summaryId")) order.getString("summaryId") else null)
                },
                receipts = (json.optJSONArray("receipts") ?: JSONArray()).objects().map {
                    CommandReceipt(it.getString("requestId"), it.getString("fingerprint"), it.getString("resultJson"))
                },
            )
            if (!json.has("currentOrderId")) save(state)
            state
        } catch (failure: Exception) {
            throw OrderingException("本地订单数据读取失败；数据已保留，请联系维护人员", "PERSISTENCE_ERROR").apply { initCause(failure) }
        }
    }

    fun save(state: OrderingState) {
        val json = JSONObject().put("schema", 1).put("version", state.version)
            .put("currentOrderId", state.currentOrderId)
            .put("serviceMode", state.serviceMode).put("partySize", state.partySize)
            .put("allergens", JSONArray(state.allergens.map { it.name }))
            .put("receipts", JSONArray(state.receipts.map {
                JSONObject().put("requestId", it.requestId).put("fingerprint", it.fingerprint).put("resultJson", it.resultJson)
            }))
            .put("cart", JSONArray(state.cart.map {
                JSONObject().put("id", it.id).put("itemId", it.itemId).put("quantity", it.quantity).put("options", JSONObject(it.options))
            }))
            .put("orders", JSONArray(state.orders.map { order ->
                JSONObject().put("id", order.id).put("number", order.number).put("createdAt", order.createdAt)
                    .put("summaryId", order.summaryId ?: JSONObject.NULL)
                    .put("totalCents", order.totalCents).put("serviceMode", order.serviceMode).put("partySize", order.partySize)
                    .put("allergens", JSONArray(order.allergens.map { it.name })).put("paymentMethod", order.paymentMethod)
                    .put("lines", JSONArray(order.lines.map {
                        JSONObject().put("name", it.name).put("quantity", it.quantity).put("options", it.options)
                            .put("unitPriceCents", it.unitPriceCents).put("skuId", it.skuId)
                    }))
            }))
        val output = try {
            atomicFile.startWrite()
        } catch (failure: Exception) {
            throw OrderingException("保存失败，操作未生效，请重试", "PERSISTENCE_ERROR").apply { initCause(failure) }
        }
        try {
            output.write(json.toString().toByteArray(Charsets.UTF_8))
            atomicFile.finishWrite(output)
        } catch (failure: Exception) {
            atomicFile.failWrite(output)
            throw OrderingException("保存失败，操作未生效，请重试", "PERSISTENCE_ERROR").apply { initCause(failure) }
        }
    }
    private fun JSONArray.objects() = (0 until length()).map { getJSONObject(it) }
    private fun JSONArray.allergens() = (0 until length()).map { Allergen.valueOf(getString(it)) }.toSet()
}
