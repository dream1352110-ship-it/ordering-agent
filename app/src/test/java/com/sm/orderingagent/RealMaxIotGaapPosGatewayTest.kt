package com.sm.orderingagent.gateway

import com.sm.orderingagent.domain.Order
import com.sm.orderingagent.domain.OrderLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RealMaxIotGaapPosGatewayTest {

    @Test
    fun `test fetchRealMenuProducts from MaxIot sandbox`() {
        val gateway = RealMaxIotGaapPosGateway()
        val result = gateway.fetchRealMenuProducts(pageSize = 10)
        assertEquals(1, result.optInt("code"))
        assertTrue(result.optBoolean("success"))
        val data = result.optJSONObject("data")
        assertTrue(data != null)
        val list = data?.optJSONArray("list")
        assertTrue(list != null && list.length() > 0)
    }

    @Test
    fun `test submitOrder to MaxIot createOrderV3 sandbox`() {
        val gateway = RealMaxIotGaapPosGateway()
        val order = Order(
            id = "ORD-TEST-" + System.currentTimeMillis(),
            number = "001",
            createdAt = System.currentTimeMillis(),
            lines = listOf(
                OrderLine(
                    name = "矿泉水",
                    quantity = 1,
                    options = "",
                    unitPriceCents = 200000,
                    skuId = "S202605151521389815040725",
                    currency = "HKD",
                ),
            ),
            totalCents = 200000,
            serviceMode = "打包",
            partySize = 1,
            allergens = emptySet(),
            paymentMethod = "扫码支付",
        )

        val submitResult = gateway.submitOrder(order, "SUMM-TEST", "takeout")
        assertTrue(submitResult.isSuccess)
        assertEquals("CREATE", submitResult.status)
        assertTrue(submitResult.gaapOrderId.isNotEmpty())
        assertTrue(submitResult.displayOrderNo.isNotEmpty())
    }

    @Test
    fun `create order body uses the cart line sku`() {
        val body = RealMaxIotGaapPosGateway.orderBizBody(
            storeId = "S002605126786036896",
            serviceMode = "堂食",
            lines = listOf(
                OrderLine(
                    "茶",
                    2,
                    "少冰 / 芝士",
                    1800,
                    skuId = "SKU-TEA",
                    currency = "HKD",
                    customizationIds = listOf("CUST_LESS_ICE"),
                    ingredientIds = listOf("ING_CHEESE"),
                ),
            ),
        )
        val detail = body.getJSONArray("orderDetail").getJSONObject(0)
        assertEquals("SKU-TEA", detail.getString("skuId"))
        assertEquals(2, detail.getInt("quantity"))
        assertEquals(1800, detail.getJSONObject("price").getInt("amount"))
        assertEquals("HKD", detail.getJSONObject("price").getString("currency"))
        assertEquals("CUST_LESS_ICE", detail.getJSONArray("customizations").getJSONObject(0).getString("customizationId"))
        assertEquals("ING_CHEESE", detail.getJSONArray("ingredients").getJSONObject(0).getString("ingredientId"))
        assertEquals("DINEIN", body.getString("serviceType"))
        assertTrue(body.getBoolean("checkStock"))
    }
}
