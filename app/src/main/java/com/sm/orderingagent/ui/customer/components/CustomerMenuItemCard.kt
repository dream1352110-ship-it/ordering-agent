package com.sm.orderingagent.ui.customer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sm.orderingagent.domain.MenuItem
import com.sm.orderingagent.ui.theme.*

@Composable
fun CustomerMenuItemCard(
    item: MenuItem,
    index: Int,
    quantityInCart: Int,
    onCardClick: () -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isAdded = quantityInCart > 0

    Surface(
        color = if (isAdded) DemoSurfaceWarm else DemoSurface,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (isAdded) 1.5.dp else 1.dp,
                color = if (isAdded) DemoAccent else DemoLine,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onCardClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 顶部：序号与已选 Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 序号徽章 (1, 2, 3...)
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (isAdded) DemoAccentSoft else DemoSurfaceWarm)
                        .border(1.dp, if (isAdded) DemoAccent else DemoLine, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$index",
                        color = if (isAdded) DemoAccent else DemoInkSoft,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 已点数量角标
                if (isAdded) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(DemoGreenSoft)
                            .border(1.dp, DemoGreen.copy(alpha = 0.4f), RoundedCornerShape(999.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            tint = DemoGreen,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "$quantityInCart",
                            color = DemoGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 中部：菜品名称与描述/规格摘要
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = item.name,
                    color = DemoInk,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.description.ifBlank {
                        if (item.options.isNotEmpty()) {
                            item.options.joinToString(" · ") { opt ->
                                val defaultVal = opt.choices.firstOrNull { it.id == opt.defaultId }?.label ?: ""
                                "${opt.label}: $defaultVal"
                            }
                        } else {
                            item.category
                        }
                    },
                    color = DemoInkSoft,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 底部：价格与加号触控按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "¥",
                        color = DemoDanger,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = String.format("%.2f", item.priceCents / 100.0),
                        color = DemoDanger,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // 加号按钮
                FilledIconButton(
                    onClick = onAddClick,
                    modifier = Modifier.size(34.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isAdded) DemoAccent else DemoSurfaceWarm,
                        contentColor = if (isAdded) Color.White else DemoInk
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = "加一份",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
