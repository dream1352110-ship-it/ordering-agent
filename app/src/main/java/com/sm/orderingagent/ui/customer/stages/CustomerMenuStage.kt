package com.sm.orderingagent.ui.customer.stages

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sm.orderingagent.domain.MenuItem
import com.sm.orderingagent.domain.OrderingState
import com.sm.orderingagent.ui.customer.components.BurgerArt
import com.sm.orderingagent.ui.customer.components.WrapArt
import com.sm.orderingagent.ui.theme.*

@Composable
fun CustomerMenuStage(
    menuItems: List<MenuItem>,
    categories: List<String>,
    state: OrderingState,
    totalCents: Int,
    risksProvider: (MenuItem) -> List<String> = { emptyList() },
    blockers: List<String> = emptyList(),
    hasVoiceError: Boolean = false,
    onItemClick: (MenuItem) -> Unit,
    onQuickAdd: (MenuItem) -> Unit,
    onOpenCartReview: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 购物车商品数量映射
    val quantities = remember(state.cart) {
        state.cart.groupBy { it.itemId }.mapValues { (_, lines) -> lines.sumOf { it.quantity } }
    }

    // 最新加入购物车的商品（用于浮动提示条反馈）
    val latestLine = state.cart.lastOrNull()
    val latestProduct = remember(latestLine, menuItems) {
        latestLine?.let { line -> menuItems.firstOrNull { it.id == line.itemId } }
    }

    // 动态提取本地 Mock 菜单中的前 3 个招牌主食作为顶部 Featured Combos
    val featuredItems = remember(menuItems) {
        val preferred = listOf("beef-burger", "steak", "meatballs")
        preferred.mapNotNull { id -> menuItems.firstOrNull { it.id == id } }
            .ifEmpty { menuItems.filter { it.category == "主食" }.take(3) }
            .ifEmpty { menuItems.take(3) }
    }

    // 左栏分类：主食、轻食、小食、烘焙（全量 14 款，分类分组展示并支持平滑滑动）
    val leftCategories = remember { listOf("主食", "轻食", "小食", "烘焙") }
    val leftGrouped = remember(menuItems) {
        leftCategories.mapNotNull { cat ->
            val list = menuItems.filter { it.category == cat }
            if (list.isNotEmpty()) cat to list else null
        }
    }

    // 右栏分类：咖啡、茶饮、冷饮（全量 23 款，分类分组展示并支持平滑滑动）
    val rightCategories = remember { listOf("咖啡", "茶饮", "冷饮") }
    val rightGrouped = remember(menuItems) {
        rightCategories.mapNotNull { cat ->
            val list = menuItems.filter { it.category == cat }
            if (list.isNotEmpty()) cat to list else null
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        // 主体：1:1 HTML 经典深酒红菜单看板 (.menu-board)
        Surface(
            color = DemoMenuBoardBg,
            shape = RoundedCornerShape(13.dp),
            border = androidx.compose.foundation.BorderStroke(2.dp, DemoMenuBoardBorder),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. 顶部大标题：菜单 (MENU)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "菜单",
                        color = DemoMenuBoardText,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )

                    // 右上角购物车小部件
                    if (state.cart.isNotEmpty()) {
                        Surface(
                            color = DemoMenuBoardGold,
                            shape = RoundedCornerShape(999.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .clickable(onClick = onOpenCartReview)
                        ) {
                            Text(
                                text = "🛒 已点 ${state.cart.sumOf { it.quantity }} 项 · ¥ " + String.format("%.2f", totalCents / 100.0),
                                color = DemoMenuBoardDarkRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                // 2. 三个大号精选套餐/招牌主食卡片 (1, 2, 3) - 绑定真实本地 Mock 数据
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    featuredItems.forEachIndexed { index, item ->
                        val qty = quantities[item.id] ?: 0
                        val isAdded = qty > 0
                        val risks = risksProvider(item)

                        Surface(
                            color = DemoMenuBoardCardBg,
                            shape = RoundedCornerShape(9.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (risks.isNotEmpty()) Color(0xFFEF5350) else DemoMenuBoardCardBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .clickable { onItemClick(item) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                // 左上角黄色序号徽章
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(DemoMenuBoardGold)
                                        .align(Alignment.TopStart),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        color = DemoMenuBoardDarkRed,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }

                                // 右上角已点数量徽章或过敏原警示
                                if (risks.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(Color(0xFFD32F2F))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                            .align(Alignment.TopEnd)
                                    ) {
                                        Text(
                                            text = "⚠️ 冲突",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                } else if (isAdded) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(Color(0xFF2E7D32))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                            .align(Alignment.TopEnd)
                                    ) {
                                        Text(
                                            text = "✓ $qty",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // 卡片居中内容
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // 矢量食物插画
                                    Box(
                                        modifier = Modifier.height(44.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (index == 2) {
                                            WrapArt()
                                        } else {
                                            BurgerArt()
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = item.name,
                                        color = DemoMenuBoardText,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(
                                        text = item.portion.ifBlank { item.description.take(14) },
                                        color = DemoMenuBoardText.copy(alpha = 0.75f),
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = "¥ " + String.format("%.2f", item.priceCents / 100.0),
                                        color = DemoMenuBoardGold,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. 组合加码条 (COMBO SIZE UP)
                Surface(
                    color = Color(0x1AFFF3D6),
                    shape = RoundedCornerShape(5.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x2EFFE6A8)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "组合加码",
                            color = DemoMenuBoardGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "加配金黄薯条 +¥16.00 · 奥尔良风味鸡翅 +¥22.00",
                            color = DemoMenuBoardText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "咖啡升级大杯 +¥4.00",
                            color = DemoMenuBoardText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 4. 双栏菜单区 (绑定真实本地 Mock 菜品)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // 左栏：主食、轻食、小食、烘焙（全量 14 款，支持触控滑动）
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "精选主食与小食烘焙",
                                color = DemoMenuBoardGold,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "共 14 款",
                                color = DemoMenuBoardGoldSoft.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        HorizontalDivider(thickness = 1.dp, color = DemoMenuBoardLine)
                        Spacer(modifier = Modifier.height(4.dp))

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            leftGrouped.forEach { (cat, items) ->
                                item(key = "hdr_$cat") {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(width = 3.dp, height = 11.dp)
                                                .background(DemoMenuBoardGold, RoundedCornerShape(2.dp))
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "$cat (${items.size})",
                                            color = DemoMenuBoardGoldSoft,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                items(items, key = { it.id }) { item ->
                                    val qty = quantities[item.id] ?: 0
                                    val risks = risksProvider(item)
                                    RealMenuBoardRow(
                                        item = item,
                                        quantity = qty,
                                        risks = risks,
                                        onClick = { onItemClick(item) }
                                    )
                                }
                            }
                        }
                    }

                    // 右栏：咖啡、茶饮、冷饮（全量 23 款，支持触控滑动）
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "现磨咖啡与醇香饮品",
                                color = DemoMenuBoardGold,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "共 23 款",
                                color = DemoMenuBoardGoldSoft.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        HorizontalDivider(thickness = 1.dp, color = DemoMenuBoardLine)
                        Spacer(modifier = Modifier.height(4.dp))

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            rightGrouped.forEach { (cat, items) ->
                                item(key = "hdr_$cat") {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(width = 3.dp, height = 11.dp)
                                                .background(DemoMenuBoardGold, RoundedCornerShape(2.dp))
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "$cat (${items.size})",
                                            color = DemoMenuBoardGoldSoft,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                items(items, key = { it.id }) { item ->
                                    val qty = quantities[item.id] ?: 0
                                    val risks = risksProvider(item)
                                    RealMenuBoardRow(
                                        item = item,
                                        quantity = qty,
                                        risks = risks,
                                        onClick = { onItemClick(item) }
                                    )
                                }
                            }
                        }
                    }
                }

                // 5. 底部黄色提示横幅
                Surface(
                    color = DemoMenuBoardGold,
                    shape = RoundedCornerShape(5.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "套餐包含小份薯条和小杯饮料 · 智能语音点餐进行中",
                        color = DemoMenuBoardDarkRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                    )
                }
            }
        }

        // 6. 浮动提示：商品加入反馈条 (ADDED TO ORDER)
        AnimatedVisibility(
            visible = latestProduct != null && !hasVoiceError,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 14.dp)
        ) {
            latestProduct?.let { product ->
                Surface(
                    color = Color(0xFFF9F6EE),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD9D2C3)),
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .clickable(onClick = onOpenCartReview)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "✓ 已加入购物车",
                                color = Color(0xFF2E7D32),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = product.name,
                                color = DemoInk,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (latestLine?.options?.isNotEmpty() == true) {
                                Text(
                                    text = product.optionText(latestLine.options),
                                    color = DemoInkSoft,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Text(
                            text = "查看订单 ›",
                            color = DemoAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 7. 浮动提示：语音识别失败就地提示 (没听清楚)
        AnimatedVisibility(
            visible = hasVoiceError,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 14.dp)
        ) {
            Surface(
                color = Color(0xFFFFF2EE),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD5C8)),
                shadowElevation = 6.dp,
                modifier = Modifier.fillMaxWidth(0.92f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE53935)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "!",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Column {
                        Text(
                            text = "没听清楚",
                            color = Color(0xFFB71C1C),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "请再说一遍，或说“帮帮我”。",
                            color = DemoInkSoft,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RealMenuBoardRow(
    item: MenuItem,
    quantity: Int = 0,
    risks: List<String> = emptyList(),
    onClick: () -> Unit
) {
    val isAdded = quantity > 0
    val sizeGroup = item.options.firstOrNull { it.id == "size" }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                // 金色圆点或绿色对勾或过敏原警示
                if (risks.isNotEmpty()) {
                    Text(
                        text = "⚠️",
                        color = Color(0xFFEF5350),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                } else if (isAdded) {
                    Text(
                        text = "✓",
                        color = Color(0xFF4CAF50),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(DemoMenuBoardGoldSoft)
                    )
                }

                Text(
                    text = item.name,
                    color = DemoMenuBoardText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (risks.isNotEmpty()) {
                    Text(
                        text = "⚠️ " + risks.first(),
                        color = Color(0xFFEF5350),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                } else if (isAdded) {
                    Text(
                        text = "已选 $quantity",
                        color = Color(0xFF81C784),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }

            if (sizeGroup != null) {
                Text(
                    text = sizeGroup.choices.joinToString("/") { it.label },
                    color = DemoMenuBoardGold,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    text = "¥ " + String.format("%.2f", item.priceCents / 100.0),
                    color = DemoMenuBoardGold,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        // 下方规格与单价细节（对齐 HTML 菜单板格式）
        if (sizeGroup != null) {
            val sizePricing = sizeGroup.choices.joinToString(" · ") { choice ->
                val price = (item.priceCents + choice.extraCents) / 100.0
                "${choice.label} ¥ " + String.format("%.2f", price)
            }
            Text(
                text = sizePricing,
                color = DemoMenuBoardText.copy(alpha = 0.70f),
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 11.dp, top = 2.dp)
            )
        } else if (item.portion.isNotBlank()) {
            Text(
                text = item.portion,
                color = DemoMenuBoardText.copy(alpha = 0.65f),
                fontSize = 10.sp,
                modifier = Modifier.padding(start = 11.dp, top = 2.dp)
            )
        }
    }
}
