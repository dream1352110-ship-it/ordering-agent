package com.sm.orderingagent.ui.customer.stages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sm.orderingagent.domain.MenuItem
import com.sm.orderingagent.domain.OrderingState
import com.sm.orderingagent.ui.customer.components.CustomerBottomCartBar
import com.sm.orderingagent.ui.customer.components.CustomerMenuItemCard
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

    val filteredItems = remember(menuItems, selectedCategory) {
        if (selectedCategory == "全部") menuItems
        else menuItems.filter { it.category == selectedCategory }
    }

    val totalCount = remember(state.cart) {
        state.cart.sumOf { it.quantity }
    }

    val quantitiesByItem = remember(state.cart) {
        state.cart.groupBy { it.itemId }
            .mapValues { (_, lines) -> lines.sumOf { it.quantity } }
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
            // 看板大标题
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MENU",
                    color = DemoInk,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )

                // 套餐升级说明条 (Demo 中的 combo-size-note)
                Surface(
                    color = DemoSurfaceWarm,
                    shape = RoundedCornerShape(999.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DemoLine)
                ) {
                    Text(
                        text = "COMBO SIZE UP: 大薯 +¥6 · 大饮 +¥4",
                        color = DemoInkSoft,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 分类筛选横滑栏 (采用 Demo 风格的胶囊按钮)
            if (categories.size > 1) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { cat ->
                        val isSelected = cat == selectedCategory
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (isSelected) DemoInk else DemoSurface)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) DemoInk else DemoLine,
                                    shape = RoundedCornerShape(999.dp)
                                )
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 16.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = cat,
                                color = if (isSelected) Color.White else DemoInk,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // 菜品网格列表 (自适应 2/3/4 列)
            BoxWithConstraints(modifier = Modifier.weight(1f)) {
                val columnCount = when {
                    maxWidth > 900.dp -> 4
                    maxWidth > 600.dp -> 3
                    else -> 2
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(columnCount),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = if (totalCount > 0) 90.dp else 60.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(
                        items = filteredItems,
                        key = { _, item -> item.id }
                    ) { index, item ->
                        CustomerMenuItemCard(
                            item = item,
                            index = index + 1,
                            quantityInCart = quantitiesByItem[item.id] ?: 0,
                            onCardClick = { onItemClick(item) },
                            onAddClick = { onQuickAdd(item) }
                        )
                    }
                }
            }

            // 底部常驻语音提示 (Demo 风格的提示框)
            CustomerVoicePromptBar(
                promptText = "Start once. Speak naturally. 直接说话即可点餐，如：“来份鸡腿堡套餐，可乐去冰”",
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // 底部常驻微型购物车胶囊
        CustomerBottomCartBar(
            totalCount = totalCount,
            totalCents = totalCents,
            onOpenCartReview = onOpenCartReview,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
