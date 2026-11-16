package com.sm.orderingagent.ui.customer.stages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sm.orderingagent.domain.Allergen
import com.sm.orderingagent.domain.CartItem
import com.sm.orderingagent.domain.MenuItem
import com.sm.orderingagent.domain.OrderingState
import com.sm.orderingagent.ui.theme.*

@Composable
fun CustomerCartReviewStage(
    state: OrderingState,
    menu: List<MenuItem>,
    totalCents: Int,
    isConfirmed: Boolean = false,
    orderIdText: String? = null,
    allergens: Set<Allergen> = state.allergens,
    blockers: List<String> = emptyList(),
    onBackToMenu: () -> Unit,
    onIncreaseItem: (CartItem) -> Unit,
    onDecreaseItem: (CartItem) -> Unit,
    onRemoveItem: (CartItem) -> Unit,
    onSubmitOrder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalCount = state.cart.sumOf { it.quantity }

    val pageTitle = when {
        isConfirmed -> "Order ${orderIdText ?: state.currentOrderId.take(7)}"
        blockers.isEmpty() && state.cart.isNotEmpty() -> "Review order"
        else -> "Your order"
    }

    val pageBadge = when {
        isConfirmed -> "CONFIRMED"
        blockers.isEmpty() && state.cart.isNotEmpty() -> "PLEASE CONFIRM"
        else -> "$totalCount ITEM${if (totalCount > 1) "S" else ""}"
    }

    val voicePromptText = when {
        isConfirmed -> "Thank you. Please proceed to the next window."
        blockers.isEmpty() && state.cart.isNotEmpty() -> "Say “confirm” to place the order or “change” to edit it."
        else -> "Say “add fries”, “that’s all”, or “remove the Coke”."
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DemoPaper)
            .padding(horizontal = 24.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // 1. 顶部标题与徽章 (Section 6.2 / 6.3 1:1 标准)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilledTonalIconButton(
                        onClick = onBackToMenu,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = DemoSurface,
                            contentColor = DemoInk
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "返回菜单",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Text(
                        text = pageTitle,
                        color = DemoInk,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    )
                }

                Surface(
                    color = DemoSurfaceWarm,
                    shape = RoundedCornerShape(999.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DemoLine)
                ) {
                    Text(
                        text = pageBadge,
                        color = DemoInkSoft,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // 2. 如果已出单，展示 Section 6.3 绿色 Order sent to kitchen 横幅
            if (isConfirmed) {
                Surface(
                    color = Color(0xFFDFF1E6),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBE5CB)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Order sent to kitchen",
                            color = Color(0xFF1B5E20),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "ACCEPTED",
                            color = Color(0xFF1B5E20),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            // 3. 过敏原冲突风险提示卡片（若存在）
            if (blockers.isNotEmpty()) {
                Surface(
                    color = DemoDangerSoft,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DemoDanger.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "⚠️ 存在过敏原风险阻断，无法提交订单：",
                            color = DemoDanger,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        blockers.forEach { blocker ->
                            Text(text = "• $blocker", color = DemoDanger, fontSize = 12.sp)
                        }
                    }
                }
            }

            // 4. 订单商品明细卡片 (Section 6.2 经典白色大卡片)
            Surface(
                color = DemoSurface,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DemoLine),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
            ) {
                if (state.cart.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "No items in the order.", color = DemoInkSoft, fontSize = 14.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(state.cart, key = { it.id }) { line ->
                            val product = menu.firstOrNull { it.id == line.itemId }
                            val unitPriceCents = product?.let { p ->
                                try { p.unitPrice(line.options) } catch (_: Exception) { p.priceCents }
                            } ?: 0

                            val lineSubtotalCents = unitPriceCents * line.quantity

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = product?.name ?: line.itemId,
                                        color = DemoInk,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = product?.optionText(line.options) ?: "标准规格",
                                        color = DemoInkSoft,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // 数量微调器
                                    if (!isConfirmed) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(DemoSurfaceWarm)
                                                .border(1.dp, DemoLine, RoundedCornerShape(999.dp))
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    if (line.quantity <= 1) onRemoveItem(line)
                                                    else onDecreaseItem(line)
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (line.quantity <= 1) Icons.Outlined.DeleteOutline else Icons.Outlined.Remove,
                                                    contentDescription = "减少",
                                                    tint = if (line.quantity <= 1) DemoDanger else DemoInkSoft,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }

                                            Text(
                                                text = "${line.quantity}",
                                                color = DemoInk,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )

                                            IconButton(
                                                onClick = { onIncreaseItem(line) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Add,
                                                    contentDescription = "增加",
                                                    tint = DemoAccent,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    } else {
                                        Text(
                                            text = "x${line.quantity}",
                                            color = DemoInkSoft,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // 金额
                                    Text(
                                        text = "¥ " + String.format("%.2f", lineSubtotalCents / 100.0),
                                        color = DemoInk,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. 底部金额汇总与 Section 6.2 语音指令提示框 (review-order-footer)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HorizontalDivider(color = DemoLine)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Subtotal", color = DemoInkSoft, fontSize = 13.sp)
                Text(text = "¥ " + String.format("%.2f", totalCents / 100.0), color = DemoInk, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Total", color = DemoInk, fontSize = 26.sp, fontWeight = FontWeight.Black)
                Text(
                    text = "¥ " + String.format("%.2f", totalCents / 100.0),
                    color = DemoInk,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black
                )
            }

            // Section 6 虚线框语音提示条 (.voice-prompt)
            Surface(
                color = Color(0x73FFFFFF),
                shape = RoundedCornerShape(11.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DemoLineStrong),
                modifier = Modifier.fillMaxWidth()
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
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(DemoAccent)
                    )
                    Text(
                        text = voicePromptText,
                        color = DemoInkSoft,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // 触屏确认提交与加餐大按钮
            if (!isConfirmed && state.cart.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onBackToMenu,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DemoLine)
                    ) {
                        Text("继续加餐", color = DemoInk)
                    }

                    Button(
                        onClick = onSubmitOrder,
                        enabled = blockers.isEmpty(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1.6f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (blockers.isEmpty()) DemoAccent else DemoLineStrong,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = if (blockers.isNotEmpty()) "存在过敏风险无法提交" else "确认下单",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
