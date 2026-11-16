package com.sm.orderingagent.ui.customer.stages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
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
import com.sm.orderingagent.ui.theme.*

@Composable
fun CustomerClarifyStage(
    candidates: List<MenuItem>,
    queryText: String,
    onSelectCandidate: (MenuItem) -> Unit,
    onCancel: () -> Unit,
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
            // 顶部导航与 Section 6.4 标题
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilledTonalIconButton(
                        onClick = onCancel,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = DemoSurface,
                            contentColor = DemoInk
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "返回",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Text(
                        text = "Choose your sandwich",
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
                        text = "OPTIONS",
                        color = DemoInkSoft,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // 选项列表 (Section 6.4 1:1 单行卡片)
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(candidates, key = { _, item -> item.id }) { index, item ->
                    Surface(
                        color = DemoSurface,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DemoLine),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectCandidate(item) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                // 黑色方圆编号徽章 (Section 6.4 1:1)
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF1E2822)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        color = Color(0xFFC9EF6B),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }

                                Column {
                                    Text(
                                        text = item.name,
                                        color = DemoInk,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = item.description.ifBlank { "Single · No extras" },
                                        color = DemoInkSoft,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Text(
                                text = "¥ " + String.format("%.2f", item.priceCents / 100.0),
                                color = DemoInk,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }

        // Section 6 虚线框语音提示条 (.voice-prompt)
        Surface(
            color = Color(0x73FFFFFF),
            shape = RoundedCornerShape(11.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DemoLineStrong),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
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
                    text = "Say “one”, “two”, “three”, or the item name.",
                    color = DemoInkSoft,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
