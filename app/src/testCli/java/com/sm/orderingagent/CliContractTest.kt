package com.sm.orderingagent

import android.app.Application
import com.sm.orderingagent.capability.OrderingCliHandler
import com.sm.orderingagent.operations.OrderingOperations
import com.sm.osagent.capability.provider.CapabilityCliOutput
import com.sm.osagent.capability.provider.CapabilityCliStatus
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
class CliContractTest {
    private val application: Application = RuntimeEnvironment.getApplication()
    private val handler = OrderingCliHandler(application, OrderingOperations(application, launchActivity = {}))

    @After fun cleanup() {
        OrderingSession.resetForTests()
        listOf("", ".bak", ".new").forEach { File(application.filesDir, "ordering-state.json$it").delete() }
    }

    @Test fun helpContainsAllFourteenExamplesAndSkillMatchesCatalogue() {
        val output = Output()
        assertEquals(CapabilityCliStatus.SUCCESS, handler.execute(listOf("--help"), output))
        assertEquals(15, OrderingOperations.operationIds.size)
        OrderingOperations.operationIds.forEach { assertTrue(output.stdout.contains("ordering $it --json '")) }
        assertEquals(1, output.flushes)
        val catalogue = JSONObject(application.assets.open("osagent/capabilities.json").bufferedReader().use { it.readText() })
        assertFalse(catalogue.has("functions"))
        val skill = catalogue.getJSONObject("cli").getJSONObject("skill")
        val body = application.assets.open(skill.getString("path")).bufferedReader().use { it.readText() }
        assertTrue(body.contains("description: \"${skill.getString("description")}\""))
        assertEquals(1, Regex("name: 餐厅点餐").findAll(body).count())
        OrderingOperations.operationIds.forEach { assertTrue(body.contains("sm-oscap-cli ordering $it")) }
        assertTrue(body.contains("普通修改省略 orderId，作用执行时当前购物车"))
        assertTrue(body.contains("普通修改必填 requestId，不传 expectedCartVersion"))
        assertTrue(body.contains("重置和提交必填 orderId"))
        assertTrue(body.contains("仍适用的新执行结果可直接复用"))
        assertTrue(body.contains("current-cart-v2"))
        assertTrue(body.contains("ordering-menu-snapshot:begin"))
        assertFalse(body.contains("不向 JSON 参数新增"))
    }

    @Test fun batchPrepareIsSyntaxOnlyAndExecuteIsAtomicWithOneReceipt() {
        val session = OrderingSession.get(application)
        val orderId = session.state.currentOrderId
        val before = session.state
        val json = """{"requestId":"batch","changes":[{"operation":"set-dining-settings","partySize":4},{"operation":"add-cart-item","itemId":"fries","quantity":2,"options":{}}]}"""
        val args = listOf("apply-cart-changes", "--json", json)
        val prepared = Output()
        assertEquals(CapabilityCliStatus.SUCCESS, handler.prepareConfirmation(args, prepared))
        assertEquals("", prepared.stdout)
        assertEquals(before, session.state)
        val executed = Output()
        assertEquals(CapabilityCliStatus.SUCCESS, handler.execute(args, executed))
        assertEquals(2, JSONObject(executed.stdout).getInt("appliedChanges"))
        assertEquals(orderId, JSONObject(executed.stdout).getString("orderId"))
        assertEquals("apply-cart-changes", JSONObject(executed.stdout).getString("operation"))
        assertEquals(1, session.state.receipts.size)
        val replay = Output()
        assertEquals(CapabilityCliStatus.SUCCESS, handler.execute(args, replay))
        assertEquals(executed.stdout, replay.stdout)
        assertEquals(1, session.state.receipts.size)
    }

    @Test fun directMeatballsAddNeedsNoCartQueryAndReturnsActualTarget() {
        val args = listOf("add-cart-item", "--json",
            """{"requestId":"meatballs-direct","itemId":"meatballs","quantity":1,"options":{}}""")
        val prepared = Output()
        assertEquals(CapabilityCliStatus.SUCCESS, handler.prepareConfirmation(args, prepared))
        assertEquals("", prepared.stdout)
        assertTrue(OrderingSession.get(application).state.cart.isEmpty())
        val first = Output()
        assertEquals(CapabilityCliStatus.SUCCESS, handler.execute(args, first))
        val result = JSONObject(first.stdout)
        assertEquals(OrderingSession.get(application).state.currentOrderId, result.getString("orderId"))
        assertEquals("add-cart-item", result.getString("operation"))
        assertEquals(1, result.getJSONArray("lines").getJSONObject(0).getInt("quantity"))
        val second = Output()
        assertEquals(CapabilityCliStatus.SUCCESS, handler.execute(listOf("add-cart-item", "--json",
            args[2].replace("meatballs-direct", "meatballs-more")), second))
        assertEquals(2, JSONObject(second.stdout).getJSONArray("lines").getJSONObject(0).getInt("quantity"))
    }

    @Test fun submitPrepareIsReadOnlyAndExecutesOnlyInExistingExecutePhase() {
        val session = OrderingSession.get(application)
        session.add("fries", 1, emptyMap())
        val quote = session.quote()
        val json = JSONObject().put("orderId", session.state.currentOrderId).put("requestId", "submit")
            .put("expectedCartVersion", session.state.version).put("summaryId", quote.id).toString()
        val before = session.state
        val args = listOf("submit-demo-order", "--json", json)
        val prepared = Output()
        assertEquals(CapabilityCliStatus.SUCCESS, handler.prepareConfirmation(args, prepared))
        assertTrue(prepared.stdout.contains(quote.id))
        assertEquals(before, session.state)
        assertTrue(session.state.orders.isEmpty())
        val executed = Output()
        assertEquals(CapabilityCliStatus.SUCCESS, handler.execute(args, executed))
        assertEquals(1, session.state.orders.size)
        val replayPrepare = Output()
        assertEquals(CapabilityCliStatus.SUCCESS, handler.prepareConfirmation(args, replayPrepare))
        assertTrue(replayPrepare.stdout.contains("已提交"))
        assertEquals(1, session.state.orders.size)
    }

    private class Output : CapabilityCliOutput {
        var stdout = ""
        var stderr = ""
        var flushes = 0
        override fun stdout(text: String) { stdout += text }
        override fun stderr(text: String) { stderr += text }
        override fun flush() { flushes++ }
        override fun close() { error("Handler must not close SDK output") }
    }
}