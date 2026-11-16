package com.sm.orderingagent.ui.customer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sm.orderingagent.domain.Allergen
import com.sm.orderingagent.ui.theme.*

@Composable
fun CustomerAllergyDialog(
    initialAllergens: Set<Allergen>,
    conflicts: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onApply: (Set<Allergen>) -> Unit,
) {
    var selected by remember { mutableStateOf(initialAllergens) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = DemoSurface,
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, DemoLineStrong),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 顶部标题与关闭
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Shield,
                            contentDescription = null,
                            tint = DemoAccent,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "过敏原避忌设置",
                            color = DemoInk,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "关闭",
                            tint = DemoInkSoft
                        )
                    }
                }

                Text(
                    text = "请勾选您需要避开的过敏原。系统将自动阻断含有、可能接触该成分或资料不完整的餐品加购与结算。",
                    color = DemoInkSoft,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                // 6 大过敏原网格选择 (2 列)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val entries = Allergen.entries.toList()
                    val chunked = entries.chunked(2)

                    chunked.forEach { rowEntries ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowEntries.forEach { allergen ->
                                val isChecked = allergen in selected
                                val toggle = {
                                    selected = if (isChecked) selected - allergen else selected + allergen
                                }

                                Surface(
                                    color = if (isChecked) DemoAccentSoft else DemoSurfaceWarm,
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = if (isChecked) 1.5.dp else 1.dp,
                                        color = if (isChecked) DemoAccent else DemoLine
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable(onClick = toggle)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = allergen.label,
                                            color = if (isChecked) DemoAccent else DemoInk,
                                            fontSize = 15.sp,
                                            fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Medium
                                        )
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = { toggle() },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = DemoAccent,
                                                checkmarkColor = Color.White
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 若有当前冲突，展示醒目提示
                if (conflicts.isNotEmpty()) {
                    Surface(
                        color = DemoDangerSoft,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DemoDanger.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "⚠️ 当前购物车存在以下过敏原冲突：",
                                color = DemoDanger,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            conflicts.forEach { conflict ->
                                Text(
                                    text = "• $conflict",
                                    color = DemoDanger,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                // 底部操作按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { selected = emptySet() },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DemoLine),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DemoInkSoft)
                    ) {
                        Text("清空避忌", fontSize = 14.sp)
                    }

                    Button(
                        onClick = { onApply(selected) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.5f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DemoAccent,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = if (selected.isEmpty()) "保存设置" else "确认避开 (${selected.size}项)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
