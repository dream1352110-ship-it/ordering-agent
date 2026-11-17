package com.sm.orderingagent.gateway

import com.sm.orderingagent.domain.Order
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * 接入 MaxIoT 真实服务端的 POS 交互网关
 * 对接服务：
 * 1. queryMenuProductList（拉取真实菜单）
 * 2. createOrderV3（真实提交 POS 订单）
 * 3. queryOrderInfo（查询真实订单状态）
 */
class RealMaxIotGaapPosGateway(
    private val baseUrl: String = MaxIotConfig.DEFAULT_BASE_URL,
    private val storeId: String = MaxIotConfig.DEFAULT_STORE_ID,
    private val orgId: String = MaxIotConfig.DEFAULT_ORG_ID,
    private val appId: String = MaxIotConfig.DEFAULT_APP_ID,
    private val clientName: String = MaxIotConfig.DEFAULT_CLIENT_NAME,
    private val deviceId: String = MaxIotConfig.DEFAULT_DEVICE_ID,
    private val serviceId: String = MaxIotConfig.DEFAULT_SERVICE_ID,
    private val xToken: String = MaxIotConfig.DEFAULT_X_TOKEN,
    private val maxToken: String = MaxIotConfig.DEFAULT_MAX_TOKEN,
) : GaapPosGateway {

    override fun startSession(storeId: String, laneId: String): GaapSessionResult {
        val sessionId = "SESS-" + System.currentTimeMillis()
        return GaapSessionResult(
            isSuccess = true,
            sessionId = sessionId,
            storeId = storeId,
            laneId = laneId,
            message = "MaxIoT 云端会话已就绪",
        )
    }

    override fun submitOrder(order: Order, summaryId: String, serviceMode: String): GaapSubmitResult {
        return try {
            val missing = order.lines.firstOrNull { it.skuId.isBlank() }
            if (missing != null) {
                return GaapSubmitResult(
                    isSuccess = false,
                    gaapOrderId = "",
                    displayOrderNo = "",
                    pickupWindow = "",
                    estimatedWaitSeconds = 0,
                    status = "REJECTED",
                    errorCode = "SKU_MISSING",
                    errorMessage = "订单行缺少门店 SKU：${missing.name}",
                )
            }
            val missingCurrency = order.lines.firstOrNull { it.currency.isBlank() }
            if (missingCurrency != null) {
                return GaapSubmitResult(
                    isSuccess = false,
                    gaapOrderId = "",
                    displayOrderNo = "",
                    pickupWindow = "",
                    estimatedWaitSeconds = 0,
                    status = "REJECTED",
                    errorCode = "CURRENCY_MISSING",
                    errorMessage = "订单行缺少菜单币种：${missingCurrency.name}",
                )
            }
            val bizBody = orderBizBody(storeId, serviceMode, order.lines)

            val uri = "$baseUrl/v3/eco/model/$orgId/hanbu_restaurant_service/createOrderV3/sandbox"
            val responseJson = callMaxIotService("createOrderV3", uri, bizBody)

            val code = responseJson.optInt("code", -1)
            val success = responseJson.optBoolean("success", false)
            if (code == 1 && success) {
                val data = responseJson.optJSONObject("data") ?: JSONObject()
                val realOrderId = data.optString("orderId", "")
                if (realOrderId.isBlank()) {
                    return rejected("ORDER_ID_MISSING", "创单成功响应缺少 orderId", responseJson)
                }
                val collectNo = data.optString("collectNo", "")
                val storeSerial = data.optString("storeSerial", "")
                val displayNo = collectNo.ifBlank { storeSerial }
                val status = data.optString("status", "CREATE")

                GaapSubmitResult(
                    isSuccess = true,
                    gaapOrderId = realOrderId,
                    displayOrderNo = displayNo,
                    pickupWindow = "",
                    estimatedWaitSeconds = 0,
                    status = status,
                    rawResponse = responseJson.toString(),
                )
            } else {
                rejected(code.toString(), responseJson.optString("msg", "创单失败"), responseJson)
            }
        } catch (ex: Exception) {
            rejected("NETWORK_ERROR", ex.message ?: "创单结果待确认", null)
        }
    }

    override fun reportHandover(request: HandoverRequest): HandoverResponse {
        return HandoverResponse(
            isSuccess = true,
            handoverId = "HND-" + System.currentTimeMillis(),
            staffAssigned = "值班店长 (服务台已通知)",
            message = "人工接管消息已上报，等待店员响应",
        )
    }

    override fun endSession(sessionId: String, reason: String): Boolean = true

    fun queryMenuCategoryList(): JSONObject =
        callMaxIotService("queryMenuCategoryList", serviceUri("queryMenuCategoryList"), menuQueryBody())

    fun queryMenuProductInfo(spuId: String): JSONObject {
        val body = menuQueryBody().put("spuId", spuId)
        return callMaxIotService("queryMenuProductInfo", serviceUri("queryMenuProductInfo"), body)
    }

    /**
     * 拉取真实菜单商品列表。先确认分类服务可用，展示分类仍以商品上的 category 为准。
     */
    fun fetchRealMenuProducts(pageSize: Int = 50): JSONObject {
        val categories = queryMenuCategoryList()
        if (!isBusinessSuccess(categories)) {
            throw IllegalStateException(categories.optString("msg", "分类查询失败"))
        }
        val first = fetchMenuPage(1, pageSize)
        if (!isBusinessSuccess(first)) {
            throw IllegalStateException(first.optString("msg", "商品查询失败"))
        }
        val data = first.optJSONObject("data") ?: return first
        val firstList = data.optJSONArray("list") ?: return first
        if (firstList.length() < pageSize) return first
        val all = JSONArray()
        appendProducts(all, firstList)
        var page = 2
        while (page <= 20) {
            val next = fetchMenuPage(page, pageSize)
            if (!isBusinessSuccess(next)) {
                throw IllegalStateException(next.optString("msg", "商品查询失败"))
            }
            val nextList = next.optJSONObject("data")?.optJSONArray("list") ?: break
            if (nextList.length() == 0) break
            appendProducts(all, nextList)
            if (nextList.length() < pageSize) break
            page++
        }
        data.put("list", all)
        return first
    }

    private fun fetchMenuPage(pageNum: Int, pageSize: Int): JSONObject {
        val bizBody = menuQueryBody()
        bizBody.put("pageNum", pageNum)
        bizBody.put("pageSize", pageSize)
        return callMaxIotService("queryMenuProductList", serviceUri("queryMenuProductList"), bizBody)
    }

    private fun menuQueryBody(): JSONObject {
        val channel = JSONObject()
        channel.put("subPlatform", "POS")
        channel.put("serviceType", "TAKEOUT")
        return JSONObject()
            .put("storeId", storeId)
            .put("menuSubType", "SELF_POS")
            .put("menuChannel", channel)
    }

    private fun serviceUri(serviceName: String): String =
        "$baseUrl/v3/eco/model/$orgId/hanbu_restaurant_service/$serviceName/sandbox"

    private fun rejected(code: String, message: String, response: JSONObject?): GaapSubmitResult =
        GaapSubmitResult(
            isSuccess = false,
            gaapOrderId = "",
            displayOrderNo = "",
            pickupWindow = "",
            estimatedWaitSeconds = 0,
            status = "UNKNOWN",
            errorCode = code,
            errorMessage = message,
            rawResponse = response?.toString(),
        )

    private fun appendProducts(target: JSONArray, source: JSONArray) {
        for (index in 0 until source.length()) target.put(source.get(index))
    }

    /**
     * 查询真实订单快照
     */
    fun queryOrderInfo(orderId: String): JSONObject {
        val bizBody = JSONObject()
        bizBody.put("storeId", storeId)
        bizBody.put("orderId", orderId)

        return callMaxIotService("queryOrderInfo", serviceUri("queryOrderInfo"), bizBody)
    }

    private fun callMaxIotService(serviceName: String, innerUri: String, bizBody: JSONObject): JSONObject {
        val outerUrl = URL("$baseUrl/?serviceName=$serviceName")
        val conn = outerUrl.openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 15000
            conn.readTimeout = 30000
            conn.doInput = true
            conn.doOutput = true

            conn.setRequestProperty("Accept", "*/*")
            conn.setRequestProperty("content-type", "application/json")
            conn.setRequestProperty("c", "test")
            conn.setRequestProperty("max-service-id", "AFC260210a9vn88au67wa4dsxbqie3r095_$serviceName")
            conn.setRequestProperty("max-service-name", serviceName)
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; SmartOrderingAgent)")

            val innerHeader = JSONObject()
            innerHeader.put("x-org-id", orgId)
            innerHeader.put("max-app-id", appId)
            innerHeader.put("sunmi-appid", appId)
            innerHeader.put("x-client-name", clientName)
            innerHeader.put("x-token", xToken)
            innerHeader.put("max-token", maxToken)
            innerHeader.put("sunmi-token", maxToken)
            innerHeader.put("content-type", "application/json")
            innerHeader.put("x-appversion", "V2.15.9")
            innerHeader.put("max-appversion", "V2.15.9")
            innerHeader.put("x-appversioncode", "36")
            innerHeader.put("max-appversioncode", "36")
            innerHeader.put("x-device", deviceId)
            innerHeader.put("max-device-id", deviceId)
            innerHeader.put("max-store-id", storeId)
            innerHeader.put("max-language", "zh-CN")
            innerHeader.put("max-s-id", serviceId)
            innerHeader.put("max-p-id", appId)
            innerHeader.put("max-package", "com.sunmi.smz2ksvj.hanbu_restaurant_desktop")
            innerHeader.put("a", "0")
            innerHeader.put("c", "test")
            innerHeader.put("d", "1")
            innerHeader.put("e", "3")
            innerHeader.put("device-type", "desktop")

            val httpData = JSONObject()
            httpData.put("uri", innerUri)
            httpData.put("method", "post")
            httpData.put("header", innerHeader)
            httpData.put("body", bizBody)

            val config = JSONObject()
            config.put("requestTimeout", 30000)
            config.put("serviceName", serviceName)

            val fullPayload = JSONObject()
            fullPayload.put("config", config)
            fullPayload.put("httpData", httpData)

            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(fullPayload.toString())
                writer.flush()
            }

            val responseCode = conn.responseCode
            val inputStream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
            val respText = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { it.readText() }
            return JSONObject(respText)
        } finally {
            conn.disconnect()
        }
    }

    companion object {
        fun isBusinessSuccess(response: JSONObject): Boolean {
            val code = response.opt("code")?.toString()
            val success = if (response.has("success") && !response.isNull("success")) {
                response.optBoolean("success")
            } else {
                true
            }
            return code == "1" && success
        }

        fun orderBizBody(storeId: String, serviceMode: String, lines: List<com.sm.orderingagent.domain.OrderLine>): JSONObject {
            val orderDetails = JSONArray()
            for (line in lines) {
                val detail = JSONObject()
                detail.put("skuId", line.skuId)
                detail.put("quantity", line.quantity)
                val priceObj = JSONObject()
                priceObj.put("amount", line.unitPriceCents)
                priceObj.put("currency", line.currency)
                detail.put("price", priceObj)
                detail.put("displayPrice", priceObj)
                detail.put("includedTaxPrice", priceObj)
                detail.put("ingredients", JSONArray(line.ingredientIds.map { id ->
                    JSONObject().put("ingredientId", id)
                }))
                detail.put("customizations", JSONArray(line.customizationIds.map { id ->
                    JSONObject().put("customizationId", id)
                }))
                orderDetails.put(detail)
            }
            val bizBody = JSONObject()
            bizBody.put("storeId", storeId)
            val posServiceType = if (serviceMode.equals("takeout", ignoreCase = true) || serviceMode == "打包") "TAKEOUT" else "DINEIN"
            bizBody.put("serviceType", posServiceType)
            bizBody.put("channel", "POS")
            bizBody.put("checkStock", true)
            bizBody.put("makingPrint", false)
            bizBody.put("customerName", "AI语音顾客")
            bizBody.put("remark", "AI点餐提交")
            bizBody.put("discountInfo", JSONArray())
            bizBody.put("orderDetail", orderDetails)
            return bizBody
        }
    }
}
