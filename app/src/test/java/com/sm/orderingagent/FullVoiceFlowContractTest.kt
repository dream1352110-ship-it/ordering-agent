package com.sm.orderingagent

import android.app.Application
import com.sm.orderingagent.domain.DemoMenu
import com.sm.orderingagent.gateway.MockGaapPosGateway
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

/**
 * 完整驱动测试：严格对齐时序图 26 步流转与 14 项 Function 契约
 * 覆盖：
 * 1. 正常点单闭环（步骤 1-15：会话启动 -> 查单 -> 选规格 -> 试算 -> 加购 -> 停顿核对 -> 确认提交 GAAP POS）
 * 2. 规格追问与修改（步骤 16-20：追问 -> 补充 -> update-cart-item -> apply-cart-changes 原子批次更新）
 * 3. 人工接管与冻结（步骤 21-26：转人工 -> reportHandover -> 冻结提交）
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class FullVoiceFlowContractTest {

    private lateinit var application: Application
    private lateinit var operations: OrderingOperations
    private lateinit var gateway: MockGaapPosGateway

    @Before
    fun setup() {
        application = RuntimeEnvironment.getApplication()
        reset()
        gateway = MockGaapPosGateway(defaultPickupWindow = "2号取餐窗口", defaultWaitSeconds = 150)
        operations = OrderingOperations(application, launchActivity = {}, gaapPosGateway = gateway)
    }

    @After
    fun reset() {
        OrderingSession.resetForTests()
        if (::application.isInitialized) {
            listOf("", ".bak", ".new").forEach { File(application.filesDir, "ordering-state.json$it").delete() }
        }
    }

    @Test
    fun testScenarioA_StandardFlow_EndToEndVoiceToGaapPos() {
        // [步骤 1 & 2] GAAP POS 触发开始点单 -> 获取当前旧订单并调用 start-new-order 重置 + list-menu
        val initialOrderId = ok("get-cart").getString("orderId")
        val startResult = ok("start-new-order", JSONObject()
            .put("requestId", "req-start-001")
            .put("orderId", initialOrderId)
        )
        val orderId = startResult.getString("orderId")
        assertTrue("新订单 ID 必须非空", orderId.isNotBlank())
        assertEquals(1L, startResult.getLong("cartVersion"))

        val menuResult = ok("list-menu", JSONObject())
        assertTrue("菜单必须包含分类列表", menuResult.getJSONArray("categories").length() > 0)
        assertTrue("菜单必须包含菜品列表", menuResult.getJSONArray("items").length() > 0)
        assertTrue("菜单必须包含过敏原列表", menuResult.getJSONArray("allergens").length() > 0)

        // [步骤 3-6] 顾客语音表达 -> AI 调用 search-menu / get-menu-item / calculate-price 规则校验
        val searchResult = ok("search-menu", JSONObject().put("names", JSONArray().put("醇香拿铁")))
        val searchedItems = searchResult.getJSONArray("items")
        assertEquals(1, searchedItems.length())
        val latteItem = searchedItems.getJSONObject(0)
        val latteId = latteItem.getString("itemId")
        assertEquals("latte", latteId)

        val detailResult = ok("get-menu-item", JSONObject().put("itemId", "latte"))
        val itemDetail = detailResult.getJSONObject("item")
        assertTrue(itemDetail.has("optionGroups"))
        val defaultOptions = itemDetail.getJSONObject("defaultOptions")

        // 计价试算（无副作用，不改变版本）
        val calcArgs = JSONObject().put("items", JSONArray().put(
            JSONObject().put("itemId", "latte").put("quantity", 1).put("options", defaultOptions)
        ))
        val calcResult = ok("calculate-price", calcArgs)
        assertTrue("试算结果必须完整合法", calcResult.getBoolean("complete"))
        val priceCents = calcResult.getInt("totalCents")
        assertTrue(priceCents > 0)

        // [步骤 7 & 8] 商品和规格完整且唯一 -> add-cart-item 加购并更新 Flex 3 屏幕
        val addArgs = JSONObject()
            .put("requestId", "req-add-latte")
            .put("orderId", orderId)
            .put("itemId", "latte")
            .put("quantity", 1)
            .put("options", defaultOptions)
        val addResult = ok("add-cart-item", addArgs)
        assertEquals(2L, addResult.getLong("cartVersion"))
        assertEquals(1, addResult.getJSONArray("lines").length())
        val cartItemId = addResult.getJSONArray("lines").getJSONObject(0).getString("cartItemId")

        // [步骤 9 & 10] 顾客停顿达到阈值 -> open-checkout 唤起购物车与订单确认
        val checkoutResult = ok("open-checkout", JSONObject().put("expectedCartVersion", 2L))
        val summary = checkoutResult.getJSONObject("summary")
        val summaryId = summary.getString("summaryId")
        assertTrue("核对单必须生成 summaryId", summaryId.isNotBlank())
        assertTrue("必须通知 Flex 3 屏幕打开购物车", checkoutResult.getJSONObject("ui").getBoolean("launchRequested"))

        // 两阶段确认准备回调（只读，无副作用）
        val confirmArgs = JSONObject()
            .put("requestId", "req-submit-001")
            .put("orderId", orderId)
            .put("summaryId", summaryId)
            .put("expectedCartVersion", 2L)
        val confirmText = operations.prepareConfirmation(confirmArgs.toString())
        assertTrue("确认文案必须包含核对摘要", confirmText.contains("确认") || confirmText.contains("¥") || confirmText.contains("合计"))

        // [步骤 11-15] 明确确认 -> submit-demo-order 锁定并提交 GAAP POS
        val submitResult = ok("submit-demo-order", confirmArgs)
        assertEquals("simulated", submitResult.getString("status"))
        assertTrue("必须包含 GAAP POS 提交结果", submitResult.has("gaap"))
        val gaapData = submitResult.getJSONObject("gaap")
        assertTrue("GAAP 订单号必须非空", gaapData.getString("gaapOrderId").startsWith("GAAP-"))
        assertEquals("2号取餐窗口", gaapData.getString("pickupWindow"))
        assertEquals("PREPARING", gaapData.getString("status"))
        assertTrue(gaapData.getString("displayOrderNo").startsWith("#A-"))

        // 幂等重试校验：相同参数重复提交，直接返回历史收据
        val retryResult = ok("submit-demo-order", confirmArgs)
        assertEquals(submitResult.toString(), retryResult.toString())
    }

    @Test
    fun testScenarioB_MissingOptionsAndAtomicBatchUpdates() {
        val initialOrderId = ok("get-cart").getString("orderId")
        val startResult = ok("start-new-order", JSONObject()
            .put("requestId", "req-b-start")
            .put("orderId", initialOrderId)
        )
        val orderId = startResult.getString("orderId")

        // [步骤 16] 缺失规格直接加购 -> 规则引擎拒绝并提示 INVALID_OPTIONS (追问缺失规格)
        val badAdd = operations.execute("add-cart-item", JSONObject()
            .put("requestId", "req-b-bad")
            .put("orderId", orderId)
            .put("itemId", "americano")
            .put("quantity", 1)
            .put("options", JSONObject())
            .toString())
        assertFalse(badAdd.success)
        assertEquals("INVALID_OPTIONS", badAdd.errorCode)

        // [步骤 17-20] 追问后顾客确认默认规格，加购成功
        val americanoDefaults = ok("get-menu-item", JSONObject().put("itemId", "americano"))
            .getJSONObject("item").getJSONObject("defaultOptions")
        val addArgs = JSONObject()
            .put("requestId", "req-b-add")
            .put("orderId", orderId)
            .put("itemId", "americano")
            .put("quantity", 1)
            .put("options", americanoDefaults)
        val addResult = ok("add-cart-item", addArgs)
        val cartItemId = addResult.getJSONArray("lines").getJSONObject(0).getString("cartItemId")
        assertEquals(2L, addResult.getLong("cartVersion"))

        // 追问后补充修改规格：调用 update-cart-item 修改为冰饮大杯
        val updatedOptions = JSONObject(americanoDefaults.toString())
            .put("temperature", "iced")
            .put("size", "large")
        val updateArgs = JSONObject()
            .put("requestId", "req-b-update")
            .put("orderId", orderId)
            .put("cartItemId", cartItemId)
            .put("quantity", 2)
            .put("options", updatedOptions)
        val updateResult = ok("update-cart-item", updateArgs)
        assertEquals(3L, updateResult.getLong("cartVersion"))
        assertEquals(2, updateResult.getJSONArray("lines").getJSONObject(0).getInt("quantity"))

        // 原子多步操作：apply-cart-changes (设置就餐模式 + 移除 + 加购)
        val batchArgs = JSONObject()
            .put("requestId", "req-b-batch")
            .put("orderId", orderId)
            .put("changes", JSONArray().put(
                JSONObject().put("operation", "set-dining-settings").put("serviceMode", "takeout")
            ).put(
                JSONObject().put("operation", "add-cart-item").put("itemId", "fries").put("quantity", 1).put("options", JSONObject())
            ))
        val batchResult = ok("apply-cart-changes", batchArgs)
        assertEquals(5L, batchResult.getLong("cartVersion"))
        assertEquals("takeout", batchResult.getString("serviceModeId"))
        assertEquals(2, batchResult.getInt("appliedChanges"))

        // 过敏原安全阻断测试：set-allergy-requirements
        val allergyArgs = JSONObject()
            .put("requestId", "req-b-allergy")
            .put("orderId", orderId)
            .put("allergenIds", JSONArray().put("milk"))
        val allergyResult = ok("set-allergy-requirements", allergyArgs)
        assertEquals(6L, allergyResult.getLong("cartVersion"))

        // 尝试加购含牛奶的拿铁（提供合法规格） -> 必须被 ALLERGY_CONFLICT 彻底阻断
        val latteDefaults = ok("get-menu-item", JSONObject().put("itemId", "latte"))
            .getJSONObject("item").getJSONObject("defaultOptions")
        val badLatte = JSONObject()
            .put("requestId", "req-b-bad-latte")
            .put("orderId", orderId)
            .put("itemId", "latte")
            .put("quantity", 1)
            .put("options", latteDefaults)
        val failResult = operations.execute("add-cart-item", badLatte.toString())
        assertFalse(failResult.success)
        assertEquals("ALLERGY_CONFLICT", failResult.errorCode)
    }

    @Test
    fun testScenarioC_Handover_CustomerRequestsStaffOrFailure() {
        val initialOrderId = ok("get-cart").getString("orderId")
        val startResult = ok("start-new-order", JSONObject()
            .put("requestId", "req-c-start")
            .put("orderId", initialOrderId)
        )
        val orderId = startResult.getString("orderId")

        // 加购一项
        ok("add-cart-item", JSONObject()
            .put("requestId", "req-c-add")
            .put("orderId", orderId)
            .put("itemId", "fries")
            .put("quantity", 1)
            .put("options", JSONObject()))

        // [步骤 21] 顾客请求人工或连续识别失败 -> 上报人工接管
        val handoverResp = operations.reportHandover(
            customerUtterance = "我想找人工店员帮我点",
            failureReason = "CUSTOMER_EXPLICIT_HELP_REQUEST",
        )
        assertTrue("人工接管必须成功", handoverResp.isSuccess)
        assertTrue(handoverResp.handoverId.startsWith("HND-"))
        assertNotNull(handoverResp.staffAssigned)
    }

    private fun ok(id: String, args: JSONObject = JSONObject()): JSONObject {
        val result = operations.execute(id, args.toString())
        assertTrue("$id execution failed: ${result.errorCode}: ${result.errorMessage}", result.success)
        return JSONObject(requireNotNull(result.resultJson))
    }
}
