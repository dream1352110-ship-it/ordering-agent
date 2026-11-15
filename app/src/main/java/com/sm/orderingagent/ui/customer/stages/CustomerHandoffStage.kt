package com.sm.orderingagent.ui.customer.stages

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sm.orderingagent.ui.customer.components.CustomerVoicePromptBar
import com.sm.orderingagent.ui.theme.*

@Composable
fun CustomerHandoffStage(
    reason: String,
    onBackToMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DemoPaper),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // 中间大号呼叫人工卡片 (Demo 的 screen-centered-message)
            Surface(
                color = DemoSurface,
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, DemoLineStrong),
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(DemoAccentSoft)
                            .border(1.5.dp, DemoAccent.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "···",
                            color = DemoAccent,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Text(
                        text = "A team member is joining",
                        color = DemoInk,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = reason.ifBlank { "We are connecting your order to the store team." },
                        color = DemoAccent,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Please stay in lane.\nStaff will speak with you shortly.",
                        color = DemoInkSoft,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = onBackToMenu,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = DemoInk
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DemoLine),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("返回菜单", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 底部语音提示
            CustomerVoicePromptBar(
                promptText = "Order context sent to store. Screen shows connection status."
            )
        }
    }
}
