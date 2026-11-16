package com.sm.orderingagent.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.sm.orderingagent.OrderingViewModel
import com.sm.orderingagent.domain.MenuItem
import com.sm.orderingagent.ui.customer.CustomerStage
import com.sm.orderingagent.ui.customer.components.CustomerAllergyDialog
import com.sm.orderingagent.ui.customer.components.CustomerItemOptionsDialog
import com.sm.orderingagent.ui.customer.components.CustomerTopBar
import com.sm.orderingagent.ui.customer.stages.CustomerCartReviewStage
import com.sm.orderingagent.ui.customer.stages.CustomerClarifyStage
import com.sm.orderingagent.ui.customer.stages.CustomerHandoffStage
import com.sm.orderingagent.ui.customer.stages.CustomerMenuStage
import com.sm.orderingagent.ui.customer.stages.CustomerSubmittedStage
import com.sm.orderingagent.ui.theme.DemoPaper
import com.sm.orderingagent.ui.theme.SmartorderingTheme
import kotlinx.coroutines.launch

@Composable
fun CustomerDriveThroughScreen(
    vm: OrderingViewModel,
    modifier: Modifier = Modifier
) {
    SmartorderingTheme {
        val state = vm.state
        val menu = vm.menu
        val categories = remember(menu) { listOf("全部") + menu.map { it.category }.distinct() }
        val scope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }

        var currentStage by remember { mutableStateOf<CustomerStage>(CustomerStage.Menu) }
        var editingProduct by remember { mutableStateOf<MenuItem?>(null) }
        var showAllergyDialog by remember { mutableStateOf(false) }
        var activeQuote by remember { mutableStateOf<com.sm.orderingagent.domain.CheckoutQuote?>(null) }
        var aiListeningText by remember { mutableStateOf("Ready to order") }

        // 监听云端/跨进程新一单重置
        LaunchedEffect(vm.orderGeneration) {
            currentStage = CustomerStage.Menu
            editingProduct = null
            activeQuote = null
        }

        // 监听来自 OSAgent 语音大模型能力调用的核对单生成 (open-checkout)
        LaunchedEffect(vm.pendingCheckout) {
            vm.pendingCheckout?.let { quote ->
                if (quote.version == state.version) {
                    activeQuote = quote
                }
                editingProduct = null
                currentStage = CustomerStage.CartReview
                vm.acknowledgeCheckout()
            }
        }

        // 监听来自 OSAgent 语音大模型直接触发跳转核对/修改订单 (review-order)
        LaunchedEffect(vm.pendingReviewOrder) {
            if (vm.pendingReviewOrder) {
                editingProduct = null
                currentStage = CustomerStage.CartReview
                vm.acknowledgeReviewOrder()
            }
        }

        // 监听来自 OSAgent 语音大模型直接触发跳转点餐菜单全览 (show-menu / list-menu)
        LaunchedEffect(vm.pendingShowMenu) {
            if (vm.pendingShowMenu) {
                editingProduct = null
                showAllergyDialog = false
                activeQuote = null
                currentStage = CustomerStage.Menu
                vm.acknowledgeShowMenu()
            }
        }

        // 监听来自 OSAgent 语音大模型能力调用的订单生成 (submit-demo-order)
        LaunchedEffect(vm.latestRemoteOrder) {
            vm.latestRemoteOrder?.let { order ->
                activeQuote = null
                editingProduct = null
                currentStage = CustomerStage.Submitted(order)
                vm.acknowledgeOrder()
            }
        }

        // 监听来自 OSAgent 语音大模型呼叫人工客服 (request-staff-assistance)
        LaunchedEffect(vm.pendingHandoffReason) {
            vm.pendingHandoffReason?.let { reason ->
                activeQuote = null
                editingProduct = null
                currentStage = CustomerStage.Handoff(reason)
                vm.acknowledgeStaffAssistance()
            }
        }

        // 5分钟核对单生命周期与购物车版本失效倒计时
        LaunchedEffect(activeQuote?.id, state.version) {
            val current = activeQuote ?: return@LaunchedEffect
            val remaining = current.createdAt + 300_000L - System.currentTimeMillis()
            if (current.version == state.version && remaining > 0) {
                kotlinx.coroutines.delay(remaining)
            }
            if (activeQuote?.id == current.id) {
                activeQuote = null
            }
        }

        fun executeCommand(errorMessage: String = "操作未完成，请重试", action: () -> Unit) {
            try {
                action()
            } catch (ex: Exception) {
                scope.launch {
                    snackbarHostState.showSnackbar(ex.message ?: errorMessage)
                }
            }
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = DemoPaper,
            topBar = {
                CustomerTopBar(
                    laneNo = "01",
                    statusBadgeText = when (currentStage) {
                        is CustomerStage.Handoff -> "Help requested"
                        is CustomerStage.Submitted -> "Confirmed"
                        is CustomerStage.CartReview -> if (state.cart.isNotEmpty()) "Please confirm" else "In cart"
                        is CustomerStage.Clarify -> "Choose an option"
                        else -> if (state.cart.isNotEmpty()) "In cart" else "Listening"
                    },
                    allergens = state.allergens,
                    onEditAllergens = { showAllergyDialog = true },
                    onCallStaff = {
                        currentStage = CustomerStage.Handoff("Customer requested staff assistance.")
                    },
                    onResetOrder = {
                        executeCommand("重置订单失败") {
                            vm.startNewOrder()
                            currentStage = CustomerStage.Menu
                        }
                    }
                )
            },
            modifier = modifier.fillMaxSize()
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(DemoPaper)
            ) {
                Crossfade(targetState = currentStage, label = "stageCrossfade") { stage ->
                    when (stage) {
                        is CustomerStage.Menu -> {
                            CustomerMenuStage(
                                menuItems = menu,
                                categories = categories,
                                state = state,
                                totalCents = vm.totalCents(),
                                risksProvider = { vm.risks(it) },
                                blockers = vm.blockers(),
                                onItemClick = { item ->
                                    if (item.options.isNotEmpty()) {
                                        editingProduct = item
                                    } else {
                                        executeCommand("加购失败") {
                                            vm.add(item.id, 1, item.defaults)
                                        }
                                    }
                                },
                                onQuickAdd = { item ->
                                    executeCommand("加购失败") {
                                        vm.add(item.id, 1, item.defaults)
                                    }
                                },
                                onOpenCartReview = {
                                    executeCommand("核对订单失败") {
                                        val quote = vm.quote()
                                        activeQuote = quote
                                        currentStage = CustomerStage.CartReview
                                    }
                                }
                            )
                        }

                        is CustomerStage.CartReview -> {
                            CustomerCartReviewStage(
                                state = state,
                                menu = menu,
                                totalCents = vm.totalCents(),
                                allergens = state.allergens,
                                blockers = vm.blockers(),
                                onBackToMenu = {
                                    activeQuote = null
                                    currentStage = CustomerStage.Menu
                                },
                                onIncreaseItem = { line ->
                                    executeCommand("调整数量失败") {
                                        vm.update(line.id, line.quantity + 1, line.options)
                                    }
                                },
                                onDecreaseItem = { line ->
                                    executeCommand("调整数量失败") {
                                        vm.update(line.id, line.quantity - 1, line.options)
                                    }
                                },
                                onRemoveItem = { line ->
                                    executeCommand("移除失败") {
                                        vm.remove(line.id)
                                    }
                                },
                                onSubmitOrder = { paymentMethod ->
                                    executeCommand("提交订单失败") {
                                        val quote = activeQuote ?: vm.quote()
                                        val order = vm.submit(quote.id, paymentMethod)
                                        activeQuote = null
                                        currentStage = CustomerStage.Submitted(order)
                                    }
                                }
                            )
                        }

                        is CustomerStage.Clarify -> {
                            CustomerClarifyStage(
                                candidates = stage.candidates,
                                queryText = stage.queryText,
                                onSelectCandidate = { item ->
                                    executeCommand("选择失败") {
                                        vm.add(item.id, 1, item.defaults)
                                        currentStage = CustomerStage.CartReview
                                    }
                                },
                                onCancel = {
                                    currentStage = CustomerStage.Menu
                                }
                            )
                        }

                        is CustomerStage.Submitted -> {
                            CustomerSubmittedStage(
                                order = stage.order,
                                menu = menu,
                                onStartNewOrder = {
                                    executeCommand("新建订单失败") {
                                        vm.startNewOrder()
                                        activeQuote = null
                                        currentStage = CustomerStage.Menu
                                    }
                                }
                            )
                        }

                        is CustomerStage.Handoff -> {
                            CustomerHandoffStage(
                                reason = stage.reason,
                                onBackToMenu = {
                                    currentStage = CustomerStage.Menu
                                }
                            )
                        }
                    }
                }

                // 规格定制弹窗
                editingProduct?.let { product ->
                    CustomerItemOptionsDialog(
                        item = product,
                        remainingStock = vm.remainingStock(product.id),
                        risks = vm.risks(product),
                        onDismiss = { editingProduct = null },
                        onConfirm = { qty, options ->
                            executeCommand("加入购物车失败") {
                                vm.add(product.id, qty, options)
                                editingProduct = null
                            }
                        }
                    )
                }

                // 过敏原设置弹窗
                if (showAllergyDialog) {
                    CustomerAllergyDialog(
                        initialAllergens = state.allergens,
                        conflicts = vm.blockers(),
                        onDismiss = { showAllergyDialog = false },
                        onApply = { selected ->
                            executeCommand("更新过敏原要求失败") {
                                vm.setAllergens(selected)
                                showAllergyDialog = false
                            }
                        }
                    )
                }
            }
        }
    }
}
