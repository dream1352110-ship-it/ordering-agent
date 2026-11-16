package com.sm.orderingagent.ui.customer.stages

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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

data class DemoFeaturedCombo(
    val key: String,
    val title: String,
    val contents: String,
    val priceText: String,
    val isBurger: Boolean
)

data class DemoMenuItemRow(
    val key: String,
    val name: String,
    val priceText: String,
    val sizeLine: String? = null
)

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
    // 数量映射 (通过 itemId 或名称匹配)
    val quantities = remember(state.cart, menuItems) {
        state.cart.groupBy { it.itemId }.mapValues { (_, lines) -> lines.sumOf { it.quantity } }
    }

    // 最新加购反馈条 (Section 6.2 详细需求)
    val latestLine = state.cart.lastOrNull()
    val latestProduct = remember(latestLine, menuItems) {
        latestLine?.let { line -> menuItems.firstOrNull { it.id == line.itemId } }
    }

    // 精选三套餐数据 (与 HTML 及图片 1:1 完全一致)
    val featuredCombos = listOf(
        DemoFeaturedCombo("chickenCombo", "鸡肉三明治套餐", "三明治+薯条+饮料", "R 99.00", isBurger = true),
        DemoFeaturedCombo("beefCombo", "牛肉汉堡套餐", "牛肉汉堡+薯条+饮料", "R 109.00", isBurger = true),
        DemoFeaturedCombo("wrapCombo", "鸡肉卷套餐", "卷饼+薯条+饮料", "R 95.00", isBurger = false)
    )

    // 左栏：三明治和卷饼 (与 HTML 及图片 1:1 完全一致)
    val sandwichesAndWraps = listOf(
        DemoMenuItemRow("classicChicken", "经典鸡肉三明治", "R 58.00"),
        DemoMenuItemRow("spicyChicken", "香辣鸡肉三明治", "R 62.00"),
        DemoMenuItemRow("crispyChicken", "香脆鸡肉三明治", "R 64.00"),
        DemoMenuItemRow("zingerBurger", "香辣鸡腿堡", "R 66.00"),
        DemoMenuItemRow("chickenWrap", "鸡肉卷", "R 49.00")
    )

    // 右栏：配菜和饮料 (与 HTML 及图片 1:1 完全一致)
    val sidesAndDrinks = listOf(
        DemoMenuItemRow("fries", "薯条", "小号/中号/大号", "小号 R 32.00 · 中号 R 42.00 · 大号 R 52.00"),
        DemoMenuItemRow("nuggets", "鸡块", "R 45.00"),
        DemoMenuItemRow("applePie", "苹果派", "R 30.00"),
        DemoMenuItemRow("coke", "可口可乐", "小号/中号/大号", "小号 R 24.00 · 中号 R 28.00 · 大号 R 34.00"),
        DemoMenuItemRow("sprite", "雪碧", "小号/中号/大号", "小号 R 24.00 · 中号 R 28.00 · 大号 R 34.00")
    )

    // 辅助匹配真实 MenuItem
    fun resolveProduct(rowName: String): MenuItem? {
        return menuItems.firstOrNull { it.name == rowName || it.name.contains(rowName.take(2)) }
            ?: menuItems.firstOrNull()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        // 主体：1:1 HTML 经典深红酒色菜单看板 (.menu-board)
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

                    // 如果有已点商品，在右上角提供一键查看购物车
                    if (state.cart.isNotEmpty()) {
                        Surface(
                            color = DemoMenuBoardGold,
                            shape = RoundedCornerShape(999.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .clickable(onClick = onOpenCartReview)
                        ) {
                            Text(
                                text = "🛒 已点 ${state.cart.sumOf { it.quantity }} 项 · R " + String.format("%.2f", totalCents / 100.0),
                                color = DemoMenuBoardDarkRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                // 2. 三个大号精选套餐卡片 (1, 2, 3)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    featuredCombos.forEachIndexed { index, combo ->
                        val targetItem = resolveProduct(combo.title)
                        val qty = targetItem?.let { quantities[it.id] } ?: 0
                        val isAdded = qty > 0

                        Surface(
                            color = DemoMenuBoardCardBg,
                            shape = RoundedCornerShape(9.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DemoMenuBoardCardBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .clickable {
                                    targetItem?.let { onItemClick(it) }
                                }
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

                                // 右上角已点数量徽章
                                if (isAdded) {
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
                                    // 居中精美食物矢量插图
                                    Box(
                                        modifier = Modifier.height(44.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (combo.isBurger) {
                                            BurgerArt()
                                        } else {
                                            WrapArt()
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = combo.title,
                                        color = DemoMenuBoardText,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(
                                        text = combo.contents,
                                        color = DemoMenuBoardText.copy(alpha = 0.75f),
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = combo.priceText,
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
                            text = "中份薯条 +10 · 大份薯条 +20",
                            color = DemoMenuBoardText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "饮用 M +4 · L +8",
                            color = DemoMenuBoardText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 4. 双栏菜单区 (左栏：三明治和卷饼；右栏：配菜和饮料)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // 左栏：三明治和卷饼
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "三明治和卷饼",
                            color = DemoMenuBoardGold,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                        HorizontalDivider(thickness = 1.dp, color = DemoMenuBoardLine)
                        Spacer(modifier = Modifier.height(4.dp))

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(sandwichesAndWraps.size) { i ->
                                val row = sandwichesAndWraps[i]
                                val item = resolveProduct(row.name)
                                val qty = item?.let { quantities[it.id] } ?: 0
                                MenuBoardRow(
                                    name = row.name,
                                    priceText = row.priceText,
                                    sizeLine = row.sizeLine,
                                    quantity = qty,
                                    onClick = { item?.let { onItemClick(it) } }
                                )
                            }
                        }
                    }

                    // 右栏：配菜和饮料
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "配菜和饮料",
                            color = DemoMenuBoardGold,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                        HorizontalDivider(thickness = 1.dp, color = DemoMenuBoardLine)
                        Spacer(modifier = Modifier.height(4.dp))

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(sidesAndDrinks.size) { i ->
                                val row = sidesAndDrinks[i]
                                val item = resolveProduct(row.name)
                                val qty = item?.let { quantities[it.id] } ?: 0
                                MenuBoardRow(
                                    name = row.name,
                                    priceText = row.priceText,
                                    sizeLine = row.sizeLine,
                                    quantity = qty,
                                    onClick = { item?.let { onItemClick(it) } }
                                )
                            }
                        }
                    }
                }

                // 5. 底部黄色提示横幅 (COMBOS INCLUDE SMALL FRIES + SMALL DRINK)
                Surface(
                    color = DemoMenuBoardGold,
                    shape = RoundedCornerShape(5.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "套餐包含小份薯条和小杯饮料",
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

        // 6. Section 6.2 浮动提示：商品加入反馈条 (ADDED TO ORDER)
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
                                text = "✓ ADDED TO ORDER",
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

        // 7. Section 6.4 浮动提示：语音识别失败就地提示 (We didn't catch that)
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
                            text = "We didn't catch that",
                            color = Color(0xFFB71C1C),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Please say your order again, or say “help”.",
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
private fun MenuBoardRow(
    name: String,
    priceText: String,
    sizeLine: String? = null,
    quantity: Int = 0,
    onClick: () -> Unit
) {
    val isAdded = quantity > 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f)
        ) {
            // 金色圆点或绿色对勾
            if (isAdded) {
                Text(
                    text = "✓",
                    color = Color(0xFF4CAF50),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(DemoMenuBoardGoldSoft)
                )
            }

            Column {
                Text(
                    text = name,
                    color = DemoMenuBoardText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                if (sizeLine != null) {
                    Text(
                        text = sizeLine,
                        color = DemoMenuBoardText.copy(alpha = 0.65f),
                        fontSize = 9.sp
                    )
                }
                if (isAdded) {
                    Text(
                        text = "$quantity in order",
                        color = Color(0xFF81C784),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Text(
            text = priceText,
            color = if (sizeLine != null) DemoMenuBoardGold else DemoMenuBoardText,
            fontSize = if (sizeLine != null) 11.sp else 13.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}
