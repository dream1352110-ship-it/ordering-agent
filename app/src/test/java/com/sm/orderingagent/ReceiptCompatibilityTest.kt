package com.sm.orderingagent

import android.app.Application
import com.sm.orderingagent.domain.CommandReceipt
import com.sm.orderingagent.operations.OrderingOperations
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ReceiptCompatibilityTest {
    private val application: Application = RuntimeEnvironment.getApplication()

    @After fun cleanup() {
        OrderingSession.resetForTests()
        listOf("", ".bak", ".new").forEach { File(application.filesDir, "ordering-state.json$it").delete() }
    }

    @Test fun legacyCompletedReceiptReplaysBeforeNewIdentityValidation() {
        val fingerprint = """{"arguments":{"expectedCartVersion":0,"itemId":"fries","options":{},"quantity":1},"function":"add-cart-item"}"""
        OrderingSession.get(application).mutate {
            recordReceipt(CommandReceipt("legacy", fingerprint, """{"orderId":"old-order","cartVersion":1,"requestId":"legacy"}"""))
            startNewOrder()
        }
        OrderingSession.resetForTests()
        val operations = OrderingOperations(application, launchActivity = {})
        val result = operations.execute("add-cart-item", """{"requestId":"legacy","expectedCartVersion":0,"itemId":"fries","quantity":1,"options":{}}""")
        assertTrue(result.errorMessage, result.success)
        assertEquals("old-order", JSONObject(result.resultJson!!).getString("orderId"))
        assertTrue(OrderingSession.get(application).state.cart.isEmpty())
        assertEquals(1, OrderingSession.get(application).state.receipts.size)
    }

    @Test fun submitRequiresCurrentIdentityAndFreshQuoteButCompletedReplayStillWorks() {
        val operations = OrderingOperations(application, launchActivity = {})
        val orderId = OrderingSession.get(application).state.currentOrderId
        fun execute(id: String, json: String): JSONObject {
            val result = operations.execute(id, json)
            assertTrue("${result.errorCode}: ${result.errorMessage}", result.success)
            return JSONObject(result.resultJson!!)
        }
        execute("add-cart-item", """{"requestId":"add","orderId":"$orderId","itemId":"fries","quantity":1,"options":{}}""")
        val summary = execute("open-checkout", """{"expectedCartVersion":1}""").getJSONObject("summary").getString("summaryId")
        val args = """{"requestId":"submit","orderId":"$orderId","expectedCartVersion":1,"summaryId":"$summary"}"""
        val first = execute("submit-demo-order", args)
        assertEquals(first.toString(), execute("submit-demo-order", args).toString())
        assertEquals("ORDER_ID_CONFLICT", operations.execute("submit-demo-order", args.replace("\"submit\"", "\"fresh-request\"")).errorCode)
        val nextId = first.getString("nextOrderId")
        val staleSummary = JSONObject(args).put("requestId", "old-summary").put("orderId", nextId).put("expectedCartVersion", 2)
        assertEquals("SUMMARY_NOT_FOUND", operations.execute("submit-demo-order", staleSummary.toString()).errorCode)
        assertEquals(1, OrderingSession.get(application).state.orders.size)
    }
}