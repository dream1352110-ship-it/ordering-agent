package com.sm.orderingagent.gateway

import com.sm.orderingagent.domain.Choice
import com.sm.orderingagent.domain.MenuItem
import com.sm.orderingagent.domain.OptionGroup
import org.json.JSONObject

/**
 * 将 MaxIoT 服务端返回的真实商品 JSON 列表转换为领域模型 MenuItem
 */
object MaxIotMenuConverter {

    fun convert(responseJson: JSONObject): List<MenuItem> {
        val listJson = responseJson.optJSONObject("data")?.optJSONArray("list") ?: return emptyList()
        val items = mutableListOf<MenuItem>()

        for (i in 0 until listJson.length()) {
            val spuObj = listJson.optJSONObject(i) ?: continue
            val spuId = spuObj.optString("spuId", "")
            val productName = spuObj.optString("productName", "未命名商品")
            val categoryName = spuObj.optString("categoryName", "热销")
            val status = spuObj.optString("status", "ONLINE")
            val sellOutFlag = spuObj.optBoolean("sellOutFlag", false)

            if (status != "ONLINE") continue

            val skus = spuObj.optJSONArray("skus") ?: continue
            if (skus.length() == 0) continue

            // 提取 SKU 规格选项组
            val optionGroups = mutableListOf<OptionGroup>()
            var primarySkuId = ""
            var basePriceCents = 0
            var totalStock = 0

            var currency = ""
            if (skus.length() == 1) {
                val sku = skus.getJSONObject(0)
                primarySkuId = sku.optString("skuId", spuId)
                val sellAmount = sku.optJSONObject("sellAmount")
                basePriceCents = sellAmount?.optInt("amount", 0) ?: 0
                currency = sellAmount?.optString("currency", "").orEmpty()
                totalStock = stockOf(sku, sellOutFlag)
            } else {
                val skuChoices = mutableListOf<Choice>()
                val skuPrices = mutableListOf<Int>()
                var defaultChoiceId = ""
                var defaultCount = 0
                for (j in 0 until skus.length()) {
                    val sku = skus.getJSONObject(j)
                    val skuId = sku.optString("skuId", "")
                    if (skuId.isBlank()) continue
                    val skuName = sku.optString("skuName", "规格${j + 1}")
                    val sellAmount = sku.optJSONObject("sellAmount")
                    val skuPrice = sellAmount?.optInt("amount", 0) ?: 0
                    val isDefault = sku.optBoolean("isDefault", false)
                    if (primarySkuId.isBlank()) {
                        basePriceCents = skuPrice
                        primarySkuId = skuId
                        currency = sellAmount?.optString("currency", "").orEmpty()
                    }
                    if (isDefault) {
                        defaultCount += 1
                        defaultChoiceId = skuId
                        primarySkuId = skuId
                        basePriceCents = skuPrice
                        currency = sellAmount?.optString("currency", "").orEmpty().ifBlank { currency }
                    }
                    skuChoices.add(Choice(skuId, skuName, 0))
                    skuPrices.add(skuPrice)
                    val stockVal = stockOf(sku, sellOutFlag)
                    if (stockVal > 0) totalStock += stockVal
                }
                if (skuChoices.isEmpty()) continue
                val priced = skuChoices.mapIndexed { index, choice ->
                    choice.copy(extraCents = skuPrices[index] - basePriceCents)
                }
                if (defaultCount != 1) defaultChoiceId = ""
                optionGroups.add(OptionGroup("spec", "规格", priced, defaultChoiceId, kind = "spec"))
            }
            if (primarySkuId.isBlank()) continue

            appendChoiceGroups(
                optionGroups,
                spuObj.optJSONObject("customizationGroup")?.optJSONArray("customizationGroupList"),
                "customizationGroupId",
                "customizationGroupName",
                "customizationList",
                "customizationId",
                "customizationName",
                "做法",
                "customization",
            )
            appendChoiceGroups(
                optionGroups,
                spuObj.optJSONObject("ingredientGroup")?.optJSONArray("ingredientGroupList"),
                "ingredientGroupId",
                "ingredientGroupName",
                "ingredientList",
                "ingredientId",
                "ingredientName",
                "加料",
                "ingredient",
            )

            // 图像与描述
            val image = "burger"
            val desc = "门店热销新鲜出品，支持堂食与外带。"
            val ingredients = "优质食材制作"

            items.add(
                MenuItem(
                    id = primarySkuId,
                    name = productName,
                    category = categoryName,
                    priceCents = basePriceCents,
                    image = image,
                    description = desc,
                    ingredients = ingredients,
                    portion = "单份",
                    stock = if (sellOutFlag) 0 else totalStock,
                    options = optionGroups,
                    currency = currency,
                    categoryId = spuObj.optString("categoryId", ""),
                    spuId = spuId,
                    allergenInfoComplete = false,
                ),
            )
        }
        return items
    }

    private fun stockOf(sku: JSONObject, productSellOut: Boolean): Int {
        if (productSellOut || sku.optBoolean("sellOutFlag", false)) return 0
        if (!sku.has("stock") || sku.isNull("stock")) return 99
        return sku.optInt("stock", 0).coerceAtLeast(0)
    }

    private fun appendChoiceGroups(
        optionGroups: MutableList<OptionGroup>,
        groups: org.json.JSONArray?,
        groupIdKey: String,
        groupNameKey: String,
        listKey: String,
        choiceIdKey: String,
        choiceNameKey: String,
        fallbackName: String,
        kind: String,
    ) {
        if (groups == null) return
        for (g in 0 until groups.length()) {
            val group = groups.optJSONObject(g) ?: continue
            val gid = group.optString(groupIdKey, "")
            if (gid.isBlank()) continue
            val gname = group.optString(groupNameKey, fallbackName)
            val list = group.optJSONArray(listKey) ?: continue
            val choices = mutableListOf<Choice>()
            for (c in 0 until list.length()) {
                val entry = list.optJSONObject(c) ?: continue
                if (entry.optBoolean("soldOut", false)) continue
                val cid = entry.optString(choiceIdKey, "")
                if (cid.isBlank()) continue
                choices.add(Choice(cid, entry.optString(choiceNameKey, fallbackName), 0))
            }
            if (choices.isNotEmpty()) optionGroups.add(OptionGroup(gid, gname, choices, "", kind = kind))
        }
    }
}
