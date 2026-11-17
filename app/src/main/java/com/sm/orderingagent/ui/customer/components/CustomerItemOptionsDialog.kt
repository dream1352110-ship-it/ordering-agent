package com.sm.orderingagent.ui.customer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sm.orderingagent.domain.MenuItem
import com.sm.orderingagent.ui.theme.*

@Composable
fun CustomerItemOptionsDialog(
    item: MenuItem,
    initialQuantity: Int = 1,
    initialOptions: Map<String, String> = item.defaults,
    remainingStock: Int,
    risks: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (quantity: Int, options: Map<String, String>) -> Unit
) {
    var quantity by rememberSaveable { mutableIntStateOf(initialQuantity) }
    var selectedOptions by remember { mutableStateOf(initialOptions) }

    val requiredGroups = remember(item) {
        item.options.filter { it.kind == "option" || it.kind == "spec" }
    }
    val hasAllRequiredOptions = remember(selectedOptions, requiredGroups) {
        requiredGroups.all { group ->
            val choiceId = selectedOptions[group.id]
            choiceId != null && group.choices.any { it.id == choiceId }
        }
    }
    val currentUnitPriceCents = remember(selectedOptions, item) {
        try {
            item.unitPrice(selectedOptions)
        } catch (_: Exception) {
            item.priceCents + selectedOptions.entries.sumOf { (groupId, choiceId) ->
                val group = item.options.firstOrNull { it.id == groupId }
                group?.choices?.firstOrNull { it.id == choiceId }?.extraCents ?: 0
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = DemoSurface,
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, DemoLineStrong),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 顶部标题与关闭按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = item.name,
                            color = DemoInk,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (remainingStock > 0) "库存充足 (余 $remainingStock 份)" else "已售罄",
                            color = if (remainingStock > 0) DemoGreen else DemoDanger,
                            fontSize = 13.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "关闭",
                            tint = DemoInkSoft
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 中间可滚动区域：菜品介绍与规格选择
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (item.description.isNotBlank()) {
                        Text(
                            text = item.description,
                            color = DemoInkSoft,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }

                    // 过敏原与配料资料卡片
                    Surface(
                        color = if (risks.isNotEmpty()) DemoDangerSoft else DemoSurfaceWarm,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (risks.isNotEmpty()) DemoDanger.copy(alpha = 0.5f) else DemoLine
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (risks.isNotEmpty()) {
                                Text(
                                    text = "⚠️ 过敏原风险：${risks.joinToString("、")}",
                                    color = DemoDanger,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (item.contains.isNotEmpty()) {
                                Text(
                                    text = "已知含有：${item.contains.joinToString("、") { it.label }}",
                                    color = if (risks.isNotEmpty()) DemoDanger else DemoInk,
                                    fontSize = 12.sp
                                )
                            }
                            if (item.mayContain.isNotEmpty()) {
                                Text(
                                    text = "可能接触：${item.mayContain.joinToString("、") { it.label }}",
                                    color = DemoInkSoft,
                                    fontSize = 12.sp
                                )
                            }
                            if (item.ingredients.isNotBlank()) {
                                Text(
                                    text = "主要配料：${item.ingredients}",
                                    color = DemoInkSoft,
                                    fontSize = 12.sp
                                )
                            }
                            if (!item.allergenInfoComplete) {
                                Text(
                                    text = "提示：本餐品过敏原资料尚不完整，如设置避忌将自动阻断。",
                                    color = DemoInkSoft,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // 规格选项组
                    item.options.forEach { group ->
                        val isRequiredGroup = group.kind == "option" || group.kind == "spec"
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = group.label + if (!isRequiredGroup) "（可选）" else "",
                                color = DemoInk,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                group.choices.forEach { choice ->
                                    val isSelected = selectedOptions[group.id] == choice.id
                                    val delta = choice.extraCents
                                    val priceText = when {
                                        delta > 0 -> " +¥${String.format("%.2f", delta / 100.0)}"
                                        delta < 0 -> " -¥${String.format("%.2f", -delta / 100.0)}"
                                        else -> ""
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) DemoAccentSoft else DemoSurfaceWarm)
                                            .border(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) DemoAccent else DemoLine,
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .clickable {
                                                selectedOptions = if (!isRequiredGroup && isSelected) {
                                                    selectedOptions - group.id
                                                } else {
                                                    selectedOptions + (group.id to choice.id)
                                                }
                                            }
                                            .padding(vertical = 12.dp, horizontal = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = choice.label + priceText,
                                            color = if (isSelected) DemoAccent else DemoInk,
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 底部：数量加减与加入购物车按钮
                Surface(
                    color = DemoSurfaceWarm,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DemoLine),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 数量调节器
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilledTonalIconButton(
                                onClick = { if (quantity > 1) quantity-- },
                                enabled = quantity > 1,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.size(36.dp),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = DemoSurface,
                                    contentColor = DemoInk
                                )
                            ) {
                                Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }

                            Text(
                                text = "$quantity",
                                color = DemoInk,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )

                            FilledTonalIconButton(
                                onClick = { if (quantity < minOf(99, remainingStock)) quantity++ },
                                enabled = quantity < minOf(99, remainingStock),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.size(36.dp),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = DemoSurface,
                                    contentColor = DemoInk
                                )
                            ) {
                                Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // 加入购物车
                        Button(
                            onClick = { onConfirm(quantity, selectedOptions) },
                            enabled = remainingStock > 0 && risks.isEmpty() && hasAllRequiredOptions,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (risks.isNotEmpty() || !hasAllRequiredOptions) DemoLineStrong else DemoAccent,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = when {
                                    risks.isNotEmpty() -> "过敏冲突无法加购"
                                    !hasAllRequiredOptions -> "请选择完整规格"
                                    else -> "确认 · ¥" + String.format("%.2f", (currentUnitPriceCents * quantity) / 100.0)
                                },
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
