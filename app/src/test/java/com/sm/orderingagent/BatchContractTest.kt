package com.sm.orderingagent

import android.app.Application
import com.sm.orderingagent.domain.CommandReceipt
import com.sm.orderingagent.domain.DemoMenu
import com.sm.orderingagent.domain.OrderingEngine
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
class BatchContractTest {
    private val application: Application = RuntimeEnvironment.getApplication()

    @After fun cleanup() {
        OrderingSession.resetForTests()
        listOf("", ".bak", ".new").forEach { File(application.filesDir, "ordering-state.json$it").delete() }
    }

    @Test fun ordinaryWriteRejectsVersionAndBatchRejectsUnknownNestedKeysAtomically() {
        val operations = OrderingOperations(application, launchActivity = {})
        val session = OrderingSession.get(application)
        val orderId = session.state.currentOrderId
        val before = session.state
        val versioned = """{"requestId":"old-contract","orderId":"$orderId","expectedCartVersion":0,"partySize":3}"""
        assertEquals("INVALID_ARGUMENT", operations.execute("set-dining-settings", versioned).errorCode)
        val nested = """{"requestId":"bad-batch","orderId":"$orderId","changes":[{"operation":"set-dining-settings","partySize":3},{"operation":"remove-cart-item","cartItemId":"missing","unknown":true}]}"""
        assertEquals("INVALID_ARGUMENT", operations.execute("apply-cart-changes", nested).errorCode)
        assertEquals(before, session.state)
        assertEquals(before, StateStore(File(application.filesDir, "ordering-state.json")).load())
        val stale = nested.replace(orderId, "old-order")
        assertEquals("ORDER_ID_CONFLICT", operations.execute("apply-cart-changes", stale).errorCode)
        assertEquals(before, session.state)
    }

    @Test fun batchAllergyAndQuantityValidationRollBackEarlierChanges() {
        val operations = OrderingOperations(application, launchActivity = {})
        val session = OrderingSession.get(application)
        val orderId = session.state.currentOrderId
        val options = JSONObject(session.product("latte").defaults)
        val request = """{"requestId":"allergy","orderId":"$orderId","changes":[{"operation":"set-allergy-requirements","allergenIds":["milk"]},{"operation":"add-cart-item","itemId":"latte","quantity":1,"options":$options}]}"""
        assertEquals("ALLERGY_CONFLICT", operations.execute("apply-cart-changes", request).errorCode)
        assertTrue(session.state.allergens.isEmpty())
        assertTrue(session.state.receipts.isEmpty())
        assertEquals(0L, session.state.version)
    }

    @Test fun multipleDomainChangesPersistOnceAndFailureRestoresQuoteAndReceipt() {
        val engine = OrderingEngine(DemoMenu.items)
        engine.add("fries", 1)
        val quote = engine.quote(1000)
        val before = engine.state
        var saves = 0
        assertThrows(IllegalStateException::class.java) {
            engine.transaction({ saves++; throw IllegalStateException("disk full") }) {
                requireOrder(before.currentOrderId)
                setDining("打包", 5)
                add("fries", 1)
                recordReceipt(CommandReceipt("batch", "fingerprint", "{}"))
            }
        }
        assertEquals(1, saves)
        assertEquals(before, engine.state)
        assertEquals(quote, engine.checkout(quote.id, 2000))
        engine.transaction({ saves++ }) {
            requireOrder(before.currentOrderId)
            setDining("打包", 5)
            add("fries", 1)
            recordReceipt(CommandReceipt("batch", "fingerprint", "{}"))
        }
        assertEquals(2, saves)
        assertEquals(1, engine.state.receipts.size)
        assertEquals(before.currentOrderId, engine.state.currentOrderId)
    }
}