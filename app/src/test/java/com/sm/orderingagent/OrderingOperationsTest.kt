package com.sm.orderingagent

import android.app.Application
import com.sm.orderingagent.operations.OrderingOperations
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class OrderingOperationsTest {
    private lateinit var application: Application
    private lateinit var operations: OrderingOperations
    private lateinit var orderId: String

    @Before fun setup() {
        application = RuntimeEnvironment.getApplication()
        reset()
        operations = OrderingOperations(application, launchActivity = {})
        orderId = ok("get-cart").getString("orderId")
    }

    @After fun reset() {
        OrderingSession.resetForTests()
        if (::application.isInitialized) {
            listOf("", ".bak", ".new").forEach { File(application.filesDir, "ordering-state.json$it").delete() }
        }
    }

    @Test fun sameOrderSequentialWritesAndCompletedReplayAfterIdentityRotation() {
        val args = write("add").put("itemId", "fries").put("quantity", 1).put("options", JSONObject())
        val first = ok("add-cart-item", args)
        val second = ok("set-dining-settings", write("dining").put("partySize", 5))
        assertEquals(orderId, second.getString("orderId"))
        assertEquals(2L, second.getLong("cartVersion"))
        val reset = ok("start-new-order", write("reset"))
        assertNotEquals(orderId, reset.getString("orderId"))
        error("add-cart-item", JSONObject(args.toString()).put("requestId", "stale"), "ORDER_ID_CONFLICT")
        assertEquals(OrderingSession.get(application).state, StateStore(File(application.filesDir, "ordering-state.json")).load())
        val afterReset = ok("get-cart").toString()
        OrderingSession.resetForTests()
        assertEquals(first.toString(), ok("add-cart-item", args).toString())
        assertEquals(afterReset, ok("get-cart").toString())
        error("add-cart-item", JSONObject(args.toString()).put("quantity", 2), "REQUEST_ID_REUSED")
    }

    @Test fun batchAtomicRollbackRestoresCartQuoteReceiptsAndDisk() {
        ok("add-cart-item", write("seed").put("itemId", "fries").put("quantity", 1).put("options", JSONObject()))
        val summary = ok("open-checkout", JSONObject().put("expectedCartVersion", 1)).getJSONObject("summary")
        val before = ok("get-cart").toString()
        val file = File(application.filesDir, "ordering-state.json")
        val bytes = file.readBytes()
        error("apply-cart-changes", batch("bad",
            change("set-dining-settings").put("partySize", 6),
            change("add-cart-item").put("itemId", "fries").put("quantity", 99).put("options", JSONObject())), "OUT_OF_STOCK")
        assertEquals(before, ok("get-cart").toString())
        assertArrayEquals(bytes, file.readBytes())
        assertNotNull(operations.prepareConfirmation(write("submit").put("expectedCartVersion", 1)
            .put("summaryId", summary.getString("summaryId")).toString()))
    }

    @Test fun batchUsesOneReceiptAndSupportsAddAdjustSetRemoveAndRequirements() {
        val seed = ok("add-cart-item", write("seed").put("itemId", "fries").put("quantity", 3).put("options", JSONObject()))
        val lineId = seed.getJSONArray("lines").getJSONObject(0).getString("cartItemId")
        val args = batch("batch",
            change("adjust-cart-item").put("cartItemId", lineId).put("delta", -1),
            change("update-cart-item").put("cartItemId", lineId).put("quantity", 4),
            change("adjust-cart-item").put("cartItemId", lineId).put("delta", 1),
            change("remove-cart-item").put("cartItemId", lineId),
            change("set-dining-settings").put("partySize", 4).put("serviceMode", "takeout"),
            change("set-allergy-requirements").put("allergenIds", JSONArray().put("milk")),
            change("add-cart-item").put("itemId", "americano").put("quantity", 2)
                .put("options", JSONObject(OrderingSession.get(application).product("americano").defaults)))
        val result = ok("apply-cart-changes", args)
        assertEquals(orderId, result.getString("orderId"))
        assertEquals(7, result.getInt("appliedChanges"))
        assertEquals(4, result.getInt("partySize"))
        assertEquals("takeout", result.getString("serviceModeId"))
        assertEquals(2, result.getJSONArray("lines").getJSONObject(0).getInt("quantity"))
        assertEquals(2, OrderingSession.get(application).state.receipts.size)
        OrderingSession.resetForTests()
        assertEquals(result.toString(), ok("apply-cart-changes", args).toString())
        assertEquals(2, OrderingSession.get(application).state.receipts.size)
    }

    @Test fun compactQueryDeduplicatesAndExposesOptionsStockAndSafetyWithoutWrites() {
        val before = ok("get-cart").toString()
        val query = ok("search-menu", JSONObject().put("names", JSONArray().put("醇香拿铁").put("不存在"))
            .put("itemIds", JSONArray().put("latte").put("fries").put("missing")))
        val items = query.getJSONArray("items")
        assertEquals(2, items.length())
        val latte = (0 until items.length()).map(items::getJSONObject).single { it.getString("itemId") == "latte" }
        assertTrue(latte.has("optionGroups"))
        assertTrue(latte.has("stock"))
        assertTrue(latte.has("contains"))
        assertTrue(latte.has("allergenInfoComplete"))
        assertFalse(latte.has("description"))
        assertEquals("missing", query.getJSONArray("unmatchedItemIds").getString(0))
        assertEquals("不存在", query.getJSONArray("unmatchedNames").getString(0))
        assertEquals(before, ok("get-cart").toString())
        error("search-menu", JSONObject(), "INVALID_ARGUMENT")
    }

    @Test fun currentCartWriteReplaysOriginalResultAfterRotationAndRestart() {
        val args = JSONObject().put("requestId", "current-add").put("itemId", "meatballs")
            .put("quantity", 1).put("options", JSONObject())
        val first = ok("add-cart-item", args)
        assertEquals(orderId, first.getString("orderId"))
        assertEquals("add-cart-item", first.getString("operation"))
        assertEquals(1, first.getJSONArray("lines").getJSONObject(0).getInt("quantity"))
        val next = ok("start-new-order", write("rotate"))
        val afterRotation = ok("get-cart").toString()
        OrderingSession.resetForTests()
        assertEquals(first.toString(), ok("add-cart-item", args).toString())
        assertEquals(afterRotation, ok("get-cart").toString())
        val fresh = ok("add-cart-item", JSONObject(args.toString()).put("requestId", "next-add"))
        assertEquals(next.getString("orderId"), fresh.getString("orderId"))
        assertEquals(1, fresh.getJSONArray("lines").getJSONObject(0).getInt("quantity"))
        error("add-cart-item", JSONObject(args.toString()).put("quantity", 2), "REQUEST_ID_REUSED")
        error("add-cart-item", JSONObject(args.toString()).put("requestId", "stale-explicit")
            .put("orderId", orderId), "ORDER_ID_CONFLICT")
    }

    @Test fun currentCartBatchRollsBackAndDestructiveOperationsStillRequireIdentity() {
        val args = batch("current-batch", change("set-dining-settings").put("partySize", 4),
            change("add-cart-item").put("itemId", "meatballs").put("quantity", 1).put("options", JSONObject()))
            .apply { remove("orderId") }
        val result = ok("apply-cart-changes", args)
        assertEquals(orderId, result.getString("orderId"))
        assertEquals("apply-cart-changes", result.getString("operation"))
        assertEquals(2, result.getInt("appliedChanges"))
        val before = ok("get-cart").toString()
        val file = File(application.filesDir, "ordering-state.json")
        val bytes = file.readBytes()
        error("apply-cart-changes", batch("current-bad", change("set-dining-settings").put("partySize", 6),
            change("add-cart-item").put("itemId", "meatballs").put("quantity", 99).put("options", JSONObject()))
            .apply { remove("orderId") }, "OUT_OF_STOCK")
        assertEquals(before, ok("get-cart").toString())
        assertArrayEquals(bytes, file.readBytes())
        error("start-new-order", JSONObject().put("requestId", "no-reset-id"), "INVALID_ARGUMENT")
        val quote = ok("open-checkout", JSONObject().put("expectedCartVersion", result.getLong("cartVersion")))
            .getJSONObject("summary")
        val submit = JSONObject().put("requestId", "no-submit-id").put("summaryId", quote.getString("summaryId"))
            .put("expectedCartVersion", result.getLong("cartVersion"))
        error("submit-demo-order", submit, "INVALID_ARGUMENT")
        error("submit-demo-order", JSONObject(submit.toString()).put("orderId", "wrong"), "ORDER_ID_CONFLICT")
        error("submit-demo-order", JSONObject(submit.toString()).put("orderId", orderId)
            .put("expectedCartVersion", 0), "CART_VERSION_CONFLICT")
    }

    @Test fun currentCartStillValidatesOptionsAllergyAndReceiptArguments() {
        val add = JSONObject().put("requestId", "safety-add").put("itemId", "meatballs")
            .put("quantity", 1).put("options", JSONObject())
        error("add-cart-item", JSONObject(add.toString()).put("orderId", JSONObject.NULL), "INVALID_ARGUMENT")
        error("add-cart-item", JSONObject(add.toString()).put("options", JSONObject().put("size", "large")), "INVALID_OPTIONS")
        error("add-cart-item", JSONObject(add.toString()).put("itemId", "missing"), "ITEM_NOT_FOUND")
        ok("set-allergy-requirements", JSONObject().put("requestId", "milk-requirement")
            .put("allergenIds", JSONArray().put("milk")))
        val before = ok("get-cart").toString()
        error("add-cart-item", add, "ALLERGY_CONFLICT")
        assertEquals(before, ok("get-cart").toString())
        val searched = ok("search-menu", JSONObject().put("itemIds", JSONArray().put("meatballs")))
        assertEquals("meatballs", searched.getJSONArray("items").getJSONObject(0).getString("itemId"))
        assertEquals(before, ok("get-cart").toString())
    }

    private fun write(requestId: String) = JSONObject().put("requestId", requestId).put("orderId", orderId)
    private fun change(operation: String) = JSONObject().put("operation", operation)
    private fun batch(requestId: String, vararg changes: JSONObject) = write(requestId).put("changes", JSONArray(changes.toList()))
    private fun ok(id: String, args: JSONObject = JSONObject()): JSONObject {
        val result = operations.execute(id, args.toString())
        assertTrue("$id: ${result.errorCode}: ${result.errorMessage}", result.success)
        return JSONObject(requireNotNull(result.resultJson))
    }
    private fun error(id: String, args: JSONObject, code: String) {
        val result = operations.execute(id, args.toString())
        assertFalse(result.success)
        assertEquals(result.errorMessage, code, result.errorCode)
    }
}
