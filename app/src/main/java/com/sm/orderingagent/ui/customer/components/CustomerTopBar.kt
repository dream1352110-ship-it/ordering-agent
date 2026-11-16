package com.sm.orderingagent.ui.customer.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sm.orderingagent.domain.Allergen
import com.sm.orderingagent.ui.theme.*

@Composable
fun CustomerTopBar(
    laneNo: String = "01",
    statusBadgeText: String = "Listening",
    allergens: Set<Allergen> = emptySet(),
    onEditAllergens: () -> Unit = {},
    onCallStaff: () -> Unit,
    onResetOrder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Surface(
        color = DemoSurface,
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = DemoLine,
                shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
            ),
        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 左侧：标准顾客屏标题 Lane 01 (Section 6 截图标准)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Lane $laneNo",
                    color = DemoInk,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp
                )

                // 避忌过敏原胶囊标签（若已设置）
                if (allergens.isNotEmpty()) {
                    Surface(
                        color = DemoDangerSoft,
                        shape = RoundedCornerShape(999.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DemoDanger.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable(onClick = onEditAllergens)
                    ) {
                        Text(
                            text = "⚠️ 避忌: ${allergens.joinToString { it.label }}",
                            color = DemoDanger,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // 右侧：Section 6 顾客屏经典状态徽章 (Listening / In cart / Please confirm / Confirmed)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val isSuccessState = statusBadgeText in listOf("Confirmed", "Accepted", "Ready")
                val isWaitingState = statusBadgeText in listOf("Listening", "In cart", "Please confirm", "Choose an option")

                val badgeBg = when {
                    isSuccessState -> DemoGreenSoft
                    statusBadgeText == "Help requested" -> DemoDangerSoft
                    else -> Color(0xFFE8F5E9)
                }

                val badgeText = when {
                    isSuccessState -> DemoGreen
                    statusBadgeText == "Help requested" -> DemoDanger
                    else -> Color(0xFF2E7D32)
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(badgeBg)
                        .border(1.dp, badgeText.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isWaitingState) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(badgeText.copy(alpha = pulseAlpha))
                        )
                    }
                    Text(
                        text = statusBadgeText,
                        color = badgeText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // 呼叫店员与重置测试按钮（平滑辅助，不破坏顾客屏美感）
                IconButton(
                    onClick = onCallStaff,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SupportAgent,
                        contentDescription = "呼叫店员",
                        tint = DemoInkSoft,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onResetOrder,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.RestartAlt,
                        contentDescription = "重置点餐",
                        tint = DemoInkSoft,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
