package com.sm.orderingagent.ui.customer.stages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sm.orderingagent.domain.MenuItem
import com.sm.orderingagent.domain.Order
import com.sm.orderingagent.ui.theme.*

@Composable
fun CustomerSubmittedStage(
    order: Order,
    menu: List<MenuItem>,
    onStartNewOrder: () -> Unit,
    modifier: Modifier = Modifier
) {
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
            // 1. 顶部标题与 CONFIRMED 徽章 (Section 6.3 1:1)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Order ${order.number}",
                    color = DemoInk,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp
                )

                Surface(
                    color = DemoSurfaceWarm,
                    shape = RoundedCornerShape(999.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DemoLine)
                ) {
                    Text(
                        text = "CONFIRMED",
                        color = DemoInkSoft,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // 2. 绿色横幅 Order sent to kitchen ACCEPTED (Section 6.3 1:1)
            Surface(
                color = Color(0xFFDFF1E6),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBE5CB)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
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

            // 提示与模拟信息 (对齐 fcdad020 ReceiptDialog 标准)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "模拟订单 · 未产生真实扣款",
                    color = Color(0xFFB33832),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${order.serviceMode} · ${order.partySize} 人 · ${order.paymentMethod}（模拟）",
                    color = DemoInkSoft,
                    fontSize = 12.sp
                )
            }

            // 3. 订单内容卡片
            Surface(
                color = DemoSurface,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DemoLine),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(order.lines) { line ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = line.name,
                                    color = DemoInk,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (line.options.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = line.options,
                                        color = DemoInkSoft,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = "¥ " + String.format("%.2f", (line.unitPriceCents * line.quantity) / 100.0),
                                    color = DemoInk,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "x${line.quantity}",
                                    color = DemoInkSoft,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. 底部金额与 Section 6.3 语音指引
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
                Text(
                    text = "¥ " + String.format("%.2f", order.totalCents / 100.0),
                    color = DemoInk,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Total", color = DemoInk, fontSize = 26.sp, fontWeight = FontWeight.Black)
                Text(
                    text = "¥ " + String.format("%.2f", order.totalCents / 100.0),
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
                        text = "Thank you. Please proceed to the next window.",
                        color = DemoInkSoft,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // 开始新订单按钮
            Button(
                onClick = onStartNewOrder,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DemoAccent,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("开始新点餐 / Next Order", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
