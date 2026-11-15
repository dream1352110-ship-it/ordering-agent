package com.sm.orderingagent.gateway

import com.sm.orderingagent.domain.CartItem
import com.sm.orderingagent.domain.Order

/**
 * GAAP POS 订单提交结果
 */
data class GaapSubmitResult(
    val isSuccess: Boolean,
    val gaapOrderId: String,
    val displayOrderNo: String,
    val pickupWindow: String,
    val estimatedWaitSeconds: Int,
    val status: String, // "PREPARING", "ACCEPTED"
    val rawResponse: String? = null,
    val errorCode: String? = null,
    val errorMessage: String? = null,
)

/**
 * GAAP POS 会话启停结果
 */
data class GaapSessionResult(
    val isSuccess: Boolean,
    val sessionId: String,
    val storeId: String,
    val laneId: String,
    val message: String,
)

/**
 * 人工接管请求报文（时序图步骤 21）
 */
data class HandoverRequest(
    val sessionId: String,
    val currentOrderId: String,
    val cartItems: List<CartItem>,
    val customerUtterance: String,
    val failureReason: String,
    val source: String = "AI_VOICE_AGENT",
)

/**
 * 人工接管响应报文
 */
data class HandoverResponse(
    val isSuccess: Boolean,
    val handoverId: String,
    val staffAssigned: String? = null,
    val message: String,
)

/**
 * GAAP POS 交互网关抽象契约
 * 对应架构中【菜单与订单规则 -> GAAP POS】的系统集成层
 */
interface GaapPosGateway {
    /**
     * 开始点单会话（时序图步骤 1）
     */
    fun startSession(storeId: String, laneId: String): GaapSessionResult

    /**
     * 提交标准订单至 GAAP POS（时序图步骤 13/14）
     */
    fun submitOrder(order: Order, summaryId: String, serviceMode: String): GaapSubmitResult

    /**
     * 上报人工接管通知（时序图步骤 21）
     */
    fun reportHandover(request: HandoverRequest): HandoverResponse

    /**
     * 结束点单会话或超时关闭（时序图步骤 26）
     */
    fun endSession(sessionId: String, reason: String): Boolean
}

/**
 * 模拟 GAAP POS 网关实现（Demo 演示与单元测试基线）
 */
class MockGaapPosGateway(
    private val defaultPickupWindow: String = "2号取餐窗口",
    private val defaultWaitSeconds: Int = 120,
) : GaapPosGateway {

    override fun startSession(storeId: String, laneId: String): GaapSessionResult {
        val sessionId = "SESS-" + System.currentTimeMillis()
        return GaapSessionResult(
            isSuccess = true,
            sessionId = sessionId,
            storeId = storeId,
            laneId = laneId,
            message = "会话已启动，Flex 3 屏幕就绪",
        )
    }

    override fun submitOrder(order: Order, summaryId: String, serviceMode: String): GaapSubmitResult {
        // 生成确定的取餐号（例如 #A-108）
        val shortSuffix = (Math.abs(order.id.hashCode()) % 900) + 100
        val displayNo = "#A-$shortSuffix"
        return GaapSubmitResult(
            isSuccess = true,
            gaapOrderId = "GAAP-${order.id}",
            displayOrderNo = displayNo,
            pickupWindow = defaultPickupWindow,
            estimatedWaitSeconds = defaultWaitSeconds,
            status = "PREPARING",
            rawResponse = """{"status":"ACCEPTED","displayNo":"$displayNo"}""",
        )
    }

    override fun reportHandover(request: HandoverRequest): HandoverResponse {
        return HandoverResponse(
            isSuccess = true,
            handoverId = "HND-" + System.currentTimeMillis(),
            staffAssigned = "店员-01 (已在收银台接管)",
            message = "人工接管消息已下发至 GAAP POS 终端，AI 自动提单已冻结",
        )
    }

    override fun endSession(sessionId: String, reason: String): Boolean {
        return true
    }
}
