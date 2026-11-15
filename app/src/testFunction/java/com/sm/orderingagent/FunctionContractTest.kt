package com.sm.orderingagent

import android.app.Application
import com.sm.orderingagent.capability.OrderingFunctions
import com.sm.orderingagent.operations.OrderingOperations
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class FunctionContractTest {
    private val application: Application = RuntimeEnvironment.getApplication()

    @Test fun javaSdkHandlersPreserveSuccessAndFailureFields() {
        val handlers = OrderingFunctions(application, launchActivity = {}).handlers()
        val success = handlers.getValue("list-menu").function.execute("{}")
        assertTrue(success.success)
        assertTrue(JSONObject(requireNotNull(success.resultJson)).has("items"))
        assertNull(success.errorCode)
        assertNull(success.errorMessage)
        assertNull(handlers.getValue("list-menu").confirmation)
        assertNotNull(handlers.getValue("submit-demo-order").confirmation)

        val failure = handlers.getValue("get-menu-item").function.execute("{}")
        assertFalse(failure.success)
        assertNull(failure.resultJson)
        assertEquals("INVALID_ARGUMENT", failure.errorCode)
        assertTrue(requireNotNull(failure.errorMessage).isNotBlank())
    }

    @Test fun catalogueHandlersSchemasAndSkillAgree() {
        val catalogue = JSONObject(application.assets.open("osagent/capabilities.json").bufferedReader().use { it.readText() })
        val handlers = OrderingFunctions(application, launchActivity = {}).handlers()
        val functions = catalogue.getJSONArray("functions")
        assertEquals(15, functions.length())
        assertEquals(OrderingOperations.operationIds.toSet(), handlers.keys)
        val ids = (0 until functions.length()).map { functions.getJSONObject(it).getString("id") }.toSet()
        assertEquals(handlers.keys, ids)
        for (index in 0 until functions.length()) {
            val function = functions.getJSONObject(index)
            val id = function.getString("id")
            val parameters = function.getJSONObject("function").getJSONObject("parameters")
            val properties = parameters.getJSONObject("properties")
            val required = parameters.getJSONArray("required")
            val requiredKeys = (0 until required.length()).map(required::getString).toSet()
            if (properties.has("requestId")) {
                assertTrue("$id request", "requestId" in requiredKeys)
                assertTrue("$id identity property", properties.has("orderId"))
                assertEquals("$id identity requirement", id in setOf("start-new-order", "submit-demo-order"),
                    "orderId" in requiredKeys)
            }
            if (id !in setOf("submit-demo-order", "open-checkout")) assertFalse("$id version", properties.has("expectedCartVersion"))
            assertEquals(id == "submit-demo-order", handlers.getValue(id).confirmation != null)
        }
        val skill = catalogue.getJSONArray("skills").getJSONObject(0)
        val refs = skill.getJSONArray("functionIds")
        assertEquals(ids, (0 until refs.length()).map(refs::getString).toSet())
        val body = application.assets.open(skill.getString("path")).bufferedReader().use { it.readText() }
        assertTrue(body.startsWith("---"))
        assertEquals(1, Regex("name: 餐厅点餐").findAll(body).count())
        assertTrue(body.contains("description: \"${skill.getString("description")}\""))
        ids.forEach { assertTrue("Missing $it", body.contains("`$it")) }
        assertTrue(body.contains("普通修改省略 orderId，作用执行时当前购物车"))
        assertTrue(body.contains("普通修改必填 requestId，不传 expectedCartVersion"))
        assertTrue(body.contains("重置和提交必填 orderId"))
        assertTrue(body.contains("仍适用的新执行结果可直接复用"))
        assertTrue(body.contains("current-cart-v2"))
        assertTrue(body.contains("optionGroups[].id"))
        assertFalse(body.contains("ordering-menu-snapshot:begin"))
        assertFalse(body.contains("不向工具输入新增"))
    }
}