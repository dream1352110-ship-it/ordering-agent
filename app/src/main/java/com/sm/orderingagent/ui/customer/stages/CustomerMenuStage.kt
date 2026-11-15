package com.sm.orderingagent.ui.customer.stages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sm.orderingagent.domain.MenuItem
import com.sm.orderingagent.domain.OrderingState
import com.sm.orderingagent.ui.customer.components.CustomerBottomCartBar
import com.sm.orderingagent.ui.customer.components.CustomerVoicePromptBar
import com.sm.orderingagent.ui.theme.*

@Composable
fun CustomerMenuStage(
    menuItems: List<MenuItem>,
    categories: List<String>,
    state: OrderingState,
    totalCents: Int,
    onItemClick: (MenuItem) -> Unit,
    onQuickAdd: (MenuItem) -> Unit,
    onOpenCartReview: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf("全部") }

    val totalCount = remember(state.cart) {
        state.cart.sumOf { it.quantity }
    }

    val quantitiesByItem = remember(state.cart) {
        state.cart.groupBy { it.itemId }
            .mapValues { (_, lines) -> lines.sumOf { it.quantity } }
    }

    // 最新加购的商品（用于展示 Demo 原版 ✓ ADDED TO ORDER 反馈条）
    val latestCartItem = state.cart.lastOrNull()
    val latestProduct = remember(latestCartItem, menuItems) {
        latestCartItem?.let { line -> menuItems.firstOrNull { it.id == line.itemId } }
    }

    // 筛选前 3 款作为 Demo 原版 3 个大号精选套餐卡片
    val featuredItems = remember(menuItems) {
        menuItems.take(3)
    }

    // 左栏：主食与汉堡；右栏：小食与饮品
    val mainItems = remember(menuItems, selectedCategory) {
        val base = menuItems.drop(3)
        if (selectedCategory == "全部") base.filter { it.category in listOf("主食", "咖啡") || it.id in listOf("burger", "fish_chips", "latte", "americano") }
        else base.filter { it.category == selectedCategory }
    }

    val sideItems = remember(menuItems, selectedCategory) {
        val base = menuItems.drop(3)
        if (selectedCategory == "全部") base.filter { it.category in listOf("小食", "饮品", "特调与茶", "烘焙") || it.id in listOf("fries", "cola", "wings", "bagel", "salad", "milk_tea") }
        else emptyList()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DemoPaper)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 12.dp)
        ) {
            // 1. 看板大标题与套餐升级说明条
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MENU",
                    color = DemoInk,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )

                Surface(
                    color = DemoSurfaceWarm,
                    shape = RoundedCornerShape(999.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DemoLine)
                ) {
                    Text(
                        text = "COMBO SIZE UP: 大薯升级 +¥4 · 大饮升级 +¥2",
                        color = DemoInkSoft,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Demo 原版 3 张精选套餐卡片 (menu-featured-row)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                featuredItems.forEachIndexed { index, item ->
                    val qty = quantitiesByItem[item.id] ?: 0
                    val isAdded = qty > 0

                    Surface(
                        color = if (isAdded) DemoSurfaceWarm else DemoSurface,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                width = if (isAdded) 1.5.dp else 1.dp,
                                color = if (isAdded) DemoAccent else DemoLine,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { onItemClick(item) }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(if (isAdded) DemoAccentSoft else DemoSurfaceWarm)
                                        .border(1.dp, if (isAdded) DemoAccent else DemoLine, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        color = if (isAdded) DemoAccent else DemoInkSoft,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (isAdded) {
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(DemoGreenSoft)
                                            .border(1.dp, DemoGreen.copy(alpha = 0.4f), RoundedCornerShape(999.dp))
                                            .padding(horizontal = 7.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Check,
                                            contentDescription = null,
                                            tint = DemoGreen,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "$qty",
                                            color = DemoGreen,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = item.name,
                                color = DemoInk,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = item.description.ifBlank { "主食 + 薯条 + 饮品" },
                                color = DemoInkSoft,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "¥" + String.format("%.2f", item.priceCents / 100.0),
                                    color = DemoDanger,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )

                                FilledIconButton(
                                    onClick = { onQuickAdd(item) },
                                    modifier = Modifier.size(30.dp),
                                    shape = CircleShape,
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = if (isAdded) DemoAccent else DemoSurfaceWarm,
                                        contentColor = if (isAdded) Color.White else DemoInk
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Add,
                                        contentDescription = "加一份",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Demo 原版双栏菜单 (menu-board-columns: 左栏主食, 右栏小食与饮品)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 左栏
                Surface(
                    color = DemoSurface,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DemoLine),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "主食与汉堡 · Sandwiches & Mains",
                            color = DemoInk,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = DemoLine)
                        Spacer(modifier = Modifier.height(8.dp))

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(mainItems) { item ->
                                val qty = quantitiesByItem[item.id] ?: 0
                                MenuRowItem(
                                    item = item,
                                    quantity = qty,
                                    onClick = { onItemClick(item) },
                                    onAdd = { onQuickAdd(item) }
                                )
                            }
                        }
                    }
                }

                // 右栏
                Surface(
                    color = DemoSurface,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DemoLine),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "小食与特调饮品 · Sides & Drinks",
                            color = DemoInk,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = DemoLine)
                        Spacer(modifier = Modifier.height(8.dp))

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(sideItems.ifEmpty { mainItems }) { item ->
                                val qty = quantitiesByItem[item.id] ?: 0
                                MenuRowItem(
                                    item = item,
                                    quantity = qty,
                                    onClick = { onItemClick(item) },
                                    onAdd = { onQuickAdd(item) }
                                )
                            }
                        }
                    }
                }
            }

            // 4. Demo 原版加购反馈气泡 (menu-order-feedback)
            AnimatedVisibility(
                visible = latestProduct != null && totalCount > 0,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
            ) {
                latestProduct?.let { p ->
                    Surface(
                        color = DemoGreenSoft,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DemoGreen.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "✓ ADDED TO ORDER",
                                color = DemoGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = p.name,
                                color = DemoInk,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "· ¥" + String.format("%.2f", p.priceCents / 100.0),
                                color = DemoDanger,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 5. 底部常驻语音提示条
            CustomerVoicePromptBar(
                promptText = "Start once. Speak naturally. 直接说话即可点餐，如：“来份汉堡套餐和一杯拿铁”",
                modifier = Modifier.padding(top = 4.dp, bottom = if (totalCount > 0) 70.dp else 0.dp)
            )
        }

        // 6. 底部常驻微型购物车胶囊
        CustomerBottomCartBar(
            totalCount = totalCount,
            totalCents = totalCents,
            onOpenCartReview = onOpenCartReview,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun MenuRowItem(
    item: MenuItem,
    quantity: Int,
    onClick: () -> Unit,
    onAdd: () -> Unit
) {
    val isAdded = quantity > 0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isAdded) DemoSurfaceWarm else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(if (isAdded) DemoGreenSoft else DemoLine.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                if (isAdded) {
                    Text(text = "✓", color = DemoGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Column {
                Text(
                    text = item.name,
                    color = DemoInk,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isAdded) "$quantity in order" else item.description.ifBlank { item.category },
                    color = if (isAdded) DemoGreen else DemoInkSoft,
                    fontSize = 11.sp
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "¥" + String.format("%.2f", item.priceCents / 100.0),
                color = DemoDanger,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            FilledIconButton(
                onClick = onAdd,
                modifier = Modifier.size(26.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (isAdded) DemoAccent else DemoLine.copy(alpha = 0.3f),
                    contentColor = if (isAdded) Color.White else DemoInk
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "加一份",
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
