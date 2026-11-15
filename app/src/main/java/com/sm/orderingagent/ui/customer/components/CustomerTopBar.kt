package com.sm.orderingagent.ui.customer.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.sm.orderingagent.ui.theme.*

@Composable
fun CustomerTopBar(
    laneNo: String = "01",
    aiStateText: String = "Ready to order",
    isListening: Boolean = true,
    onCallStaff: () -> Unit,
    onResetOrder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
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
                .padding(horizontal = 22.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 左侧：车道与品牌
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 车道芯片
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .background(DemoGreenSoft)
                        .border(1.dp, DemoGreen.copy(alpha = 0.4f), RoundedCornerShape(9.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(DemoGreen.copy(alpha = if (isListening) pulseAlpha else 1f))
                    )
                    Text(
                        text = "Lane $laneNo",
                        color = DemoGreen,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Column {
                    Text(
                        text = "FLEX / VOICE",
                        color = DemoInk,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Drive-through order journey",
                        color = DemoInkSoft,
                        fontSize = 11.sp
                    )
                }
            }

            // 中间：状态指示（如 Ready to order / AI 正在聆听中）
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(DemoSurfaceWarm)
                    .border(1.dp, DemoLine, RoundedCornerShape(999.dp))
                    .padding(horizontal = 16.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(DemoAccent.copy(alpha = pulseAlpha))
                )
                Text(
                    text = aiStateText,
                    color = DemoInk,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // 右侧：呼叫人工与重置操作
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalButton(
                    onClick = onCallStaff,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = DemoSurfaceWarm,
                        contentColor = DemoInk
                    ),
                    shape = RoundedCornerShape(9.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SupportAgent,
                        contentDescription = "呼叫人工",
                        tint = DemoAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "呼叫人工", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                IconButton(
                    onClick = onResetOrder,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.RestartAlt,
                        contentDescription = "重置点餐",
                        tint = DemoInkSoft
                    )
                }
            }
        }
    }
}
