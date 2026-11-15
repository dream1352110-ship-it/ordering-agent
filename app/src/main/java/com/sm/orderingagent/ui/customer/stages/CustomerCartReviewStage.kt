package com.sm.orderingagent.ui.customer.stages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sm.orderingagent.domain.CartItem
import com.sm.orderingagent.domain.MenuItem
import com.sm.orderingagent.domain.OrderingState
import com.sm.orderingagent.ui.customer.components.CustomerVoicePromptBar
import com.sm.orderingagent.ui.theme.*

@Composable
fun CustomerCartReviewStage(
    state: OrderingState,
    menu: List<MenuItem>,
    totalCents: Int,
    onBackToMenu: () -> Unit,
    onIncreaseItem: (CartItem) -> Unit,
    onDecreaseItem: (CartItem) -> Unit,
    onRemoveItem: (CartItem) -> Unit,
    onSubmitOrder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalCount = state.cart.sumOf { it.quantity }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DemoPaper)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 顶部导航与标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    FilledTonalIconButton(
                        onClick = onBackToMenu,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = DemoSurface,
                            contentColor = DemoInk
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "返回菜单"
                        )
                    }

                    Column {
                        Text(
                            text = "Review order",
                            color = DemoInk,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = if (totalCount > 0) "$totalCount items in order" else "No items",
                            color = DemoInkSoft,
                            fontSize = 13.sp
                        )
                    }
                }

                // 堂食/外带标识
                Surface(
                    color = DemoSurfaceWarm,
                    shape = RoundedCornerShape(999.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DemoLine)
                ) {
                    Text(
                        text = if (state.serviceMode == "takeout" || state.serviceMode == "外带") "打包带走" else "堂食就餐 (${state.partySize}人)",
                        color = DemoInk,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 中部：购物车商品列表或空状态
            if (state.cart.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ShoppingCart,
                            contentDescription = null,
                            tint = DemoLineStrong,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "Your order is empty",
                            color = DemoInk,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Say what you would like to order.",
                            color = DemoInkSoft,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onBackToMenu,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DemoAccent,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("返回选购菜单", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(state.cart, key = { it.id }) { line ->
                        val product = menu.firstOrNull { it.id == line.itemId }
                        val unitPriceCents = product?.let { p ->
                            try {
                                p.unitPrice(line.options)
                            } catch (_: Exception) {
                                p.priceCents
                            }
                        } ?: 0

                        val lineSubtotalCents = unitPriceCents * line.quantity

                        Surface(
                            color = DemoSurface,
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DemoLine),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 左侧：名称与规格详情
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = product?.name ?: line.itemId,
                                        color = DemoInk,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = product?.optionText(line.options) ?: "标准规格",
                                        color = DemoInkSoft,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "单价: ¥" + String.format("%.2f", unitPriceCents / 100.0),
                                        color = DemoInkSoft,
                                        fontSize = 12.sp
                                    )
                                }

                                // 右侧：加减按钮与小计
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // 数量加减微调器
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(DemoSurfaceWarm)
                                            .border(1.dp, DemoLine, RoundedCornerShape(999.dp))
                                            .padding(horizontal = 6.dp, vertical = 4.dp)
                                    ) {
                                        IconButton(
                                            onClick = {
                                                if (line.quantity <= 1) onRemoveItem(line)
                                                else onDecreaseItem(line)
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (line.quantity <= 1) Icons.Outlined.DeleteOutline else Icons.Outlined.Remove,
                                                contentDescription = "减少",
                                                tint = if (line.quantity <= 1) DemoDanger else DemoInkSoft,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        Text(
                                            text = "${line.quantity}",
                                            color = DemoInk,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )

                                        IconButton(
                                            onClick = { onIncreaseItem(line) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Add,
                                                contentDescription = "增加",
                                                tint = DemoAccent,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    // 单行小计金额
                                    Text(
                                        text = "¥" + String.format("%.2f", lineSubtotalCents / 100.0),
                                        color = DemoDanger,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 底部核对汇总与大按钮
            if (state.cart.isNotEmpty()) {
                Surface(
                    color = DemoSurface,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, DemoLineStrong),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // 金额统计明细
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text(
                                    text = "应付总金额 Total",
                                    color = DemoInkSoft,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "含税 · 价格均已计算规格加价",
                                    color = DemoLineStrong,
                                    fontSize = 11.sp
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "¥",
                                    color = DemoDanger,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = String.format("%.2f", totalCents / 100.0),
                                    color = DemoDanger,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        // 确认下单大按钮与返回继续选购
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            OutlinedButton(
                                onClick = onBackToMenu,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, DemoLine),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = DemoInk)
                            ) {
                                Text("继续加餐", fontSize = 16.sp)
                            }

                            Button(
                                onClick = onSubmitOrder,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1.8f)
                                    .height(52.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DemoAccent,
                                    contentColor = Color.White
                                )
                            ) {
                                Text(
                                    text = "确认下单 (¥" + String.format("%.2f", totalCents / 100.0) + ")",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 底部语音提示
            CustomerVoicePromptBar(
                promptText = "Say “confirm” to place the order or “add fries” to add items."
            )
        }
    }
}
