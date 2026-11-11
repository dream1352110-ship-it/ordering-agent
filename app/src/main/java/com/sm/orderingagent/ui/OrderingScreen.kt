package com.sm.orderingagent.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.PostAdd
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sm.orderingagent.OrderingViewModel
import com.sm.orderingagent.domain.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Forest = Color(0xFF245B43)
private val PriceRed = Color(0xFFB33832)
private val Page = Color(0xFFF4F6F4)
private val Corner = RoundedCornerShape(8.dp)
private val Disclosure = "演示菜单：配料、份量与过敏原信息仅供演示，未经实际厨房核验。共用设备可能造成交叉接触；资料未列出不代表不含。过敏者请先咨询工作人员。库存为本机演示数量。"

private data class ProductEdit(val product: MenuItem, val line: CartItem? = null)
private data class Review(val quote: CheckoutQuote, val state: OrderingState)

@Composable
fun OrderingScreen(vm: OrderingViewModel) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Forest, onPrimary = Color.White, primaryContainer = Color(0xFFE2EEE6),
            onPrimaryContainer = Color(0xFF163B29),
            secondary = Forest, onSecondary = Color.White,
            secondaryContainer = Color(0xFFDCECE1), onSecondaryContainer = Color(0xFF163B29),
            tertiary = Color(0xFF796B30), tertiaryContainer = Color(0xFFF2EBCB),
            outline = Color(0xFF7C887F), outlineVariant = Color(0xFFD4DCD6),
            background = Page, surface = Color.White,
            surfaceVariant = Color(0xFFEDF1ED), error = PriceRed,
            onBackground = Color(0xFF202A24), onSurface = Color(0xFF202A24),
        ),
        shapes = Shapes(Corner, Corner, Corner, Corner, Corner),
    ) {
        val state = vm.state
        val snackbars = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        var error by remember { mutableStateOf<String?>(null) }
        var category by rememberSaveable { mutableStateOf("全部") }
        var search by rememberSaveable { mutableStateOf("") }
        var edit by remember { mutableStateOf<ProductEdit?>(null) }
        var allergies by remember { mutableStateOf(false) }
        var history by remember { mutableStateOf(false) }
        var cartOpen by remember { mutableStateOf(false) }
        var clearConfirm by remember { mutableStateOf(false) }
        var newOrderConfirm by remember { mutableStateOf(false) }
        var review by remember { mutableStateOf<Review?>(null) }
        var receipt by remember { mutableStateOf<Order?>(null) }
        var justOrdered by remember { mutableStateOf(false) }

        LaunchedEffect(vm.orderGeneration) {
            edit = null
            review = null
            receipt = null
            cartOpen = false
            allergies = false
            history = false
            clearConfirm = false
            newOrderConfirm = false
            justOrdered = false
            search = ""
            category = "全部"
            error = null
        }
        LaunchedEffect(vm.pendingCheckout) {
            vm.pendingCheckout?.let { quote ->
                if (quote.version == vm.state.version) review = Review(quote, vm.state)
                vm.acknowledgeCheckout()
            }
        }
        LaunchedEffect(vm.latestRemoteOrder) {
            vm.latestRemoteOrder?.let { order ->
                edit = null
                review = null
                cartOpen = false
                justOrdered = true
                receipt = order
                vm.acknowledgeOrder()
            }
        }
        LaunchedEffect(review?.quote?.id, state.version) {
            val current = review ?: return@LaunchedEffect
            val remaining = current.quote.createdAt + 300_000L - System.currentTimeMillis()
            if (current.quote.version == state.version && remaining > 0) delay(remaining)
            if (review?.quote?.id == current.quote.id) {
                review = null
                error = "订单已变更或核对单已过期，请重新核对"
                snackbars.showSnackbar(error!!)
            }
        }

        fun command(action: () -> Unit): Boolean = try {
            action()
            error = null
            true
        } catch (failure: Exception) {
            val message = failure.message ?: "操作未完成，请重试"
            error = message
            scope.launch { snackbars.showSnackbar(message) }
            false
        }

        val checkout: () -> Unit = {
            command { review = Review(vm.quote(), vm.state) }
        }
        val openEdit: (CartItem) -> Unit = { line ->
            error = null
            edit = ProductEdit(vm.product(line.itemId), line)
        }
        val changeQuantity: (CartItem, Int) -> Unit = { line, quantity ->
            command { vm.update(line.id, quantity, line.options) }
        }
        val remove: (CartItem) -> Unit = { line -> command { vm.remove(line.id) }; Unit }
        val clear: () -> Unit = { clearConfirm = true }

        Scaffold(
            modifier = Modifier.fillMaxSize().safeDrawingPadding(),
            containerColor = Page,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbars) },
        ) { padding ->
            BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
                val wide = maxWidth >= 840.dp
                Column(Modifier.fillMaxSize()) {
                    Header(
                        state, wide,
                        onDining = { mode, people -> command { vm.setDining(mode, people) } },
                        onAllergies = { error = null; allergies = true },
                        onHistory = { error = null; history = true },
                        onNewOrder = { error = null; newOrderConfirm = true },
                    )
                    HorizontalDivider()
                    Row(Modifier.weight(1f)) {
                        if (wide) {
                            Column(
                                Modifier.width(92.dp).fillMaxHeight().background(Color.White)
                                    .verticalScroll(rememberScrollState()).padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                DemoMenu.categories.forEach { label ->
                                    FilterChip(
                                        selected = category == label,
                                        onClick = { category = label },
                                        label = { Text(label) },
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                            VerticalDivider()
                        }
                        Column(Modifier.weight(1f).fillMaxHeight()) {
                            OutlinedTextField(
                                value = search, onValueChange = { search = it },
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                placeholder = { Text("搜索餐品或配料") },
                                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                                trailingIcon = {
                                    if (search.isNotEmpty()) ToolIcon("清除搜索", Icons.Outlined.Close) { search = "" }
                                },
                                singleLine = true, shape = Corner,
                            )
                            if (!wide) {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    items(DemoMenu.categories) { label ->
                                        FilterChip(category == label, { category = label }, { Text(label) })
                                    }
                                }
                            }
                            val visible = vm.menu.filter {
                                (category == "全部" || it.category == category) &&
                                    (search.isBlank() || it.name.contains(search.trim(), true) ||
                                        it.ingredients.contains(search.trim(), true))
                            }
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(category, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("${visible.size} 款 · 演示菜单", style = MaterialTheme.typography.labelMedium)
                            }
                            if (visible.isEmpty()) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("没有找到相关餐品") }
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Adaptive(160.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    items(visible, key = { it.id }) { product ->
                                        ProductTile(product, vm.remainingStock(product.id), vm.risks(product)) {
                                            error = null
                                            edit = ProductEdit(product)
                                        }
                                    }
                                }
                            }
                        }
                        if (wide) {
                            VerticalDivider()
                            CartPanel(
                                vm, Modifier.width(320.dp).fillMaxHeight().background(Color.White),
                                openEdit, changeQuantity, remove, clear, checkout,
                            )
                        }
                    }
                    if (!wide) {
                        Surface(shadowElevation = 4.dp) {
                            Row(
                                Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                OutlinedButton(onClick = { error = null; cartOpen = true }, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Outlined.ShoppingBag, null, Modifier.size(20.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("购物车 ${state.cart.sumOf { it.quantity }}", maxLines = 1)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(money(vm.totalCents()), color = PriceRed, fontWeight = FontWeight.Bold)
                                    if (state.cart.isNotEmpty() && vm.blockers().isNotEmpty()) {
                                        Text("存在待处理项", color = PriceRed, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                Button(onClick = checkout, enabled = vm.blockers().isEmpty(), modifier = Modifier.testTag("checkout")) {
                                    Text("核对订单")
                                }
                            }
                        }
                    }
                }
            }
        }

        if (cartOpen) {
            Dialog(onDismissRequest = { cartOpen = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
                Surface(
                    modifier = Modifier.safeDrawingPadding().padding(12.dp).widthIn(max = 560.dp)
                        .fillMaxWidth().fillMaxHeight(0.94f), shape = Corner,
                ) {
                    Column {
                        DialogHeading("购物车", { cartOpen = false })
                        ErrorNotice(error)
                        CartPanel(vm, Modifier.weight(1f), openEdit, changeQuantity, remove, clear, checkout)
                    }
                }
            }
        }
        edit?.let { target ->
            key(vm.orderGeneration, target.product.id, target.line?.id) {
                ProductDialog(
                    target, vm.remainingStock(target.product.id), vm.risks(target.product), error,
                    onDismiss = { edit = null; error = null },
                    onSave = { quantity, options ->
                        val succeeded = command {
                            if (target.line == null) vm.add(target.product.id, quantity, options)
                            else vm.update(target.line.id, quantity, options)
                        }
                        if (succeeded) edit = null
                    },
                )
            }
        }
        if (allergies) {
            AllergyDialog(state.allergens, state.cart.flatMap { line ->
                vm.risks(vm.product(line.itemId)).map { "${vm.product(line.itemId).name}：$it" }
            }.distinct(), error, { allergies = false; error = null }) { selected ->
                if (command { vm.setAllergens(selected) }) allergies = false
            }
        }
        if (newOrderConfirm) {
            AlertDialog(
                onDismissRequest = { newOrderConfirm = false },
                title = { Text("新建订单？") },
                text = { Column { Text("清空当前餐品和过敏要求，恢复堂食、2人；历史订单保留。"); ErrorNotice(error) } },
                confirmButton = {
                    TextButton(onClick = {
                        if (command { vm.startNewOrder() }) newOrderConfirm = false
                    }) { Text("确认新建") }
                },
                dismissButton = { TextButton(onClick = { newOrderConfirm = false }) { Text("取消") } },
            )
        }
        if (clearConfirm) {
            AlertDialog(
                onDismissRequest = { clearConfirm = false },
                title = { Text("清空购物车？") },
                text = { Column { Text("已选餐品将全部移除，过敏原要求会保留。"); ErrorNotice(error) } },
                confirmButton = {
                    TextButton(onClick = { if (command { vm.clear() }) clearConfirm = false }) { Text("确认清空", color = PriceRed) }
                },
                dismissButton = { TextButton(onClick = { clearConfirm = false }) { Text("保留餐品") } },
            )
        }
        review?.let { current ->
            key(current.quote.id) {
                CheckoutDialog(current, vm.menu, error, { review = null; error = null }) { payment ->
                    command {
                        val order = vm.submit(current.quote.id, payment)
                        review = null
                        cartOpen = false
                        justOrdered = true
                        receipt = order
                    }
                }
            }
        }
        if (history) {
            Modal("历史订单", { history = false }) {
                if (state.orders.isEmpty()) Text("暂无订单", Modifier.padding(vertical = 24.dp))
                state.orders.forEach { order ->
                    Surface(onClick = { justOrdered = false; receipt = order }, color = Color.Transparent) {
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("订单 ${order.number}", fontWeight = FontWeight.Bold)
                                Text(orderTime(order.createdAt), style = MaterialTheme.typography.bodySmall)
                                Text("${order.serviceMode} · ${order.lines.sumOf { it.quantity }} 件 · 模拟订单", style = MaterialTheme.typography.bodySmall)
                            }
                            Text(money(order.totalCents), color = PriceRed)
                            Icon(Icons.Outlined.History, "查看订单${order.number}回执")
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
        receipt?.let { order -> ReceiptDialog(order, justOrdered) { receipt = null } }
    }
}

@Composable
private fun Header(
    state: OrderingState, wide: Boolean,
    onDining: (String, Int) -> Unit, onAllergies: () -> Unit, onHistory: () -> Unit,
    onNewOrder: () -> Unit,
) {
    Surface {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.Restaurant, null, tint = Forest, modifier = Modifier.size(28.dp))
                Text("食刻餐厅", style = if (wide) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (wide) DiningControls(state, onDining)
                ToolIcon("新建订单", Icons.Outlined.PostAdd, onClick = onNewOrder)
                ToolIcon("过敏原要求", Icons.Outlined.WarningAmber, selected = state.allergens.isNotEmpty(), onClick = onAllergies)
                ToolIcon("订单历史", Icons.Outlined.History, onClick = onHistory)
            }
            if (!wide) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    DiningControls(state, onDining)
                }
            }
        }
    }
}

@Composable
private fun DiningControls(state: OrderingState, onDining: (String, Int) -> Unit) {
    SingleChoiceSegmentedButtonRow {
        listOf("堂食", "打包").forEachIndexed { index, mode ->
            SegmentedButton(
                selected = state.serviceMode == mode,
                onClick = { onDining(mode, state.partySize) },
                shape = SegmentedButtonDefaults.itemShape(index, 2, Corner),
            ) { Text(if (mode == "打包") "外带" else mode) }
        }
    }
    Spacer(Modifier.width(8.dp))
    ToolIcon("减少用餐人数", Icons.Outlined.Remove, enabled = state.partySize > 1) { onDining(state.serviceMode, state.partySize - 1) }
    Text("${state.partySize} 人", style = MaterialTheme.typography.labelLarge)
    ToolIcon("增加用餐人数", Icons.Outlined.Add, enabled = state.partySize < 12) { onDining(state.serviceMode, state.partySize + 1) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToolIcon(
    label: String, icon: ImageVector, enabled: Boolean = true, selected: Boolean = false, onClick: () -> Unit,
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(label) } }, state = rememberTooltipState(),
    ) {
        IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(48.dp)) {
            Icon(icon, label, tint = if (!enabled) Color(0xFFB9BFBA) else if (selected) PriceRed else Forest)
        }
    }
}

@Composable
private fun ProductPhoto(product: MenuItem, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val identifier = remember(product.image, context) {
        context.resources.getIdentifier(product.image, "drawable", context.packageName)
    }
    Box(modifier.background(Color.White), contentAlignment = Alignment.Center) {
        if (identifier != 0) {
            Image(painterResource(identifier), product.name, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        } else {
            Icon(Icons.Outlined.Restaurant, product.name, Modifier.size(48.dp), tint = Forest)
        }
    }
}

@Composable
private fun ProductTile(product: MenuItem, stock: Int, risks: List<String>, onClick: () -> Unit) {
    Card(
        onClick = onClick, shape = Corner,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth().testTag("product-${product.id}")
            .semantics { contentDescription = "查看${product.name}" },
    ) {
        ProductPhoto(product, Modifier.fillMaxWidth().aspectRatio(1.35f))
        Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(product.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(money(product.priceCents), color = PriceRed, fontWeight = FontWeight.Bold)
                Text(if (stock == 0) "已售罄" else "余 $stock", style = MaterialTheme.typography.labelMedium,
                    color = if (stock == 0) PriceRed else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                if (risks.isNotEmpty()) "过敏原需留意" else product.portion,
                style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                color = if (risks.isNotEmpty()) PriceRed else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CartPanel(
    vm: OrderingViewModel, modifier: Modifier,
    onEdit: (CartItem) -> Unit, onQuantity: (CartItem, Int) -> Unit,
    onRemove: (CartItem) -> Unit, onClear: () -> Unit, onCheckout: () -> Unit,
) {
    val state = vm.state
    Column(modifier.padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("已选餐品", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("${state.cart.sumOf { it.quantity }} 件", style = MaterialTheme.typography.labelMedium)
            ToolIcon("清空购物车", Icons.Outlined.DeleteOutline, enabled = state.cart.isNotEmpty(), onClick = onClear)
        }
        Text("${state.serviceMode} · ${state.partySize} 人", style = MaterialTheme.typography.bodySmall)
        if (state.allergens.isNotEmpty()) Text("避开：${allergenNames(state.allergens)}", style = MaterialTheme.typography.bodySmall, color = Forest)
        Spacer(Modifier.height(8.dp))
        HorizontalDivider()
        LazyColumn(Modifier.weight(1f)) {
            if (state.cart.isEmpty()) item {
                Column(Modifier.fillMaxWidth().padding(vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.ShoppingBag, null, Modifier.size(36.dp), tint = Forest)
                    Spacer(Modifier.height(12.dp))
                    Text("购物车还是空的")
                }
            }
            items(state.cart, key = { it.id }) { line ->
                val product = vm.product(line.itemId)
                Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(product.name, fontWeight = FontWeight.SemiBold)
                            val options = product.optionText(line.options)
                            if (options.isNotEmpty()) Text(options, style = MaterialTheme.typography.bodySmall)
                            Text(money(product.unitPrice(line.options) * line.quantity), color = PriceRed)
                        }
                        ToolIcon("编辑${product.name}", Icons.Outlined.Edit) { onEdit(line) }
                    }
                    vm.risks(product).forEach { Text(it, color = PriceRed, style = MaterialTheme.typography.bodySmall) }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        ToolIcon("移除${product.name}", Icons.Outlined.DeleteOutline) { onRemove(line) }
                        Spacer(Modifier.weight(1f))
                        QuantityStepper(
                            line.quantity, "${product.name}", line.quantity > 1,
                            line.quantity < 99 && state.cart.filter { it.itemId == line.itemId }.sumOf { it.quantity } < vm.remainingStock(line.itemId),
                        ) { onQuantity(line, it) }
                    }
                }
                HorizontalDivider()
            }
        }
        val blockers = vm.blockers()
        if (state.cart.isNotEmpty() && blockers.isNotEmpty()) {
            Column(Modifier.heightIn(max = 104.dp).verticalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
                blockers.forEach { Text(it, color = PriceRed, style = MaterialTheme.typography.bodySmall) }
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("合计", fontWeight = FontWeight.Bold)
            Text(money(vm.totalCents()), color = PriceRed, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Button(onClick = onCheckout, enabled = blockers.isEmpty(), modifier = Modifier.fillMaxWidth().testTag("cart-checkout")) { Text("核对订单") }
    }
}

@Composable
private fun QuantityStepper(quantity: Int, name: String, canDecrease: Boolean = quantity > 1, canIncrease: Boolean = quantity < 99, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ToolIcon("减少$name", Icons.Outlined.Remove, enabled = canDecrease) { onChange(quantity - 1) }
        Box(Modifier.width(32.dp), contentAlignment = Alignment.Center) { Text("$quantity", fontWeight = FontWeight.Bold) }
        ToolIcon("增加$name", Icons.Outlined.Add, enabled = canIncrease) { onChange(quantity + 1) }
    }
}

@Composable
private fun ProductDialog(
    target: ProductEdit, stock: Int, risks: List<String>, error: String?,
    onDismiss: () -> Unit, onSave: (Int, Map<String, String>) -> Unit,
) {
    val product = target.product
    var quantity by rememberSaveable { mutableIntStateOf(target.line?.quantity ?: 1) }
    var options by remember { mutableStateOf(target.line?.options ?: product.defaults) }
    Modal(product.name, onDismiss, footer = {
        ErrorNotice(error)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            QuantityStepper(quantity, product.name, canIncrease = quantity < minOf(99, stock)) { quantity = it }
            Spacer(Modifier.weight(1f))
            Text(money(product.unitPrice(options) * quantity), color = PriceRed, fontWeight = FontWeight.Bold)
        }
        Button(
            onClick = { onSave(quantity, options) }, enabled = stock > 0 && risks.isEmpty(),
            modifier = Modifier.fillMaxWidth().testTag("add-to-cart"),
        ) { Text(if (stock == 0) "已售罄" else if (target.line == null) "加入购物车" else "保存修改") }
    }) {
        ProductPhoto(product, Modifier.fillMaxWidth().aspectRatio(1.7f))
        Text(product.description, style = MaterialTheme.typography.bodyLarge)
        Text("份量：${product.portion}")
        Text("配料：${product.ingredients}")
        Text("演示剩余：$stock 份", style = MaterialTheme.typography.bodySmall)
        HorizontalDivider()
        Text("过敏原信息", fontWeight = FontWeight.Bold)
        Text("已知含有：${allergenNames(product.contains)}")
        Text("可能接触：${allergenNames(product.mayContain)}")
        Text(if (product.allergenInfoComplete) "未知信息：未列出的成分仍需向工作人员确认" else "未知信息：本餐品的过敏原资料不完整", color = PriceRed)
        risks.forEach { Text("与当前要求冲突：$it", color = PriceRed) }
        Text(Disclosure, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        product.options.forEach { group ->
            HorizontalDivider()
            Text(group.label, fontWeight = FontWeight.Bold)
            group.choices.forEach { choice ->
                Surface(onClick = { options = options + (group.id to choice.id) }, color = Color.Transparent) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = options[group.id] == choice.id, onClick = { options = options + (group.id to choice.id) })
                        Text(choice.label, Modifier.weight(1f))
                        if (choice.extraCents != 0) Text("+${money(choice.extraCents)}", color = PriceRed)
                    }
                }
            }
        }
    }
}

@Composable
private fun AllergyDialog(
    initial: Set<Allergen>, conflicts: List<String>, error: String?, onDismiss: () -> Unit, onApply: (Set<Allergen>) -> Unit,
) {
    var selected by remember { mutableStateOf(initial) }
    Modal("过敏原要求", onDismiss, footer = {
        ErrorNotice(error)
        Button(onClick = { onApply(selected) }, modifier = Modifier.fillMaxWidth()) { Text("应用要求") }
    }) {
        Text("请选择需要避开的过敏原", fontWeight = FontWeight.Bold)
        Text("含有、可能接触所选过敏原的餐品，以及过敏原资料不完整的餐品，将无法加入或结算。", color = PriceRed)
        Allergen.entries.forEach { allergen ->
            val toggle = { selected = if (allergen in selected) selected - allergen else selected + allergen }
            Surface(onClick = toggle, color = Color.Transparent) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(allergen in selected, onCheckedChange = { toggle() })
                    Text(allergen.label)
                }
            }
        }
        if (conflicts.isNotEmpty()) {
            Text("当前购物车与已应用要求的冲突", fontWeight = FontWeight.Bold)
            conflicts.forEach { Text(it, color = PriceRed) }
        }
        HorizontalDivider()
        Text(Disclosure, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun CheckoutDialog(review: Review, menu: List<MenuItem>, error: String?, onDismiss: () -> Unit, onSubmit: (String) -> Boolean) {
    var payment by rememberSaveable { mutableStateOf("扫码支付") }
    var submitting by remember { mutableStateOf(false) }
    Modal("核对订单", onDismiss, footer = {
        ErrorNotice(error)
        Button(
            onClick = {
                if (!submitting) {
                    submitting = true
                    if (!onSubmit(payment)) submitting = false
                }
            }, enabled = !submitting, modifier = Modifier.fillMaxWidth().testTag("submit-order"),
        ) { Text(if (submitting) "正在提交" else "确认模拟下单 · ${money(review.quote.totalCents)}") }
    }) {
        Text("模拟下单，不会发起真实支付或扣款。", color = PriceRed, fontWeight = FontWeight.Bold)
        Text("${review.state.serviceMode} · ${review.state.partySize} 人")
        Text("过敏原要求：${allergenNames(review.state.allergens)}")
        HorizontalDivider()
        review.state.cart.forEach { line ->
            val product = menu.first { it.id == line.itemId }
            SummaryLine(product.name, line.quantity, product.optionText(line.options), product.unitPrice(line.options) * line.quantity)
        }
        HorizontalDivider()
        Text("模拟付款方式", fontWeight = FontWeight.Bold)
        listOf("扫码支付", "现金").forEach { method ->
            Surface(onClick = { payment = method }, color = Color.Transparent) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(payment == method, onClick = { payment = method })
                    Text("$method（模拟）")
                }
            }
        }
        Text("合计 ${money(review.quote.totalCents)}", style = MaterialTheme.typography.titleLarge, color = PriceRed, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ReceiptDialog(order: Order, success: Boolean, onDismiss: () -> Unit) {
    Modal(if (success) "下单成功" else "订单回执", onDismiss, footer = {
        Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("完成") }
    }) {
        Icon(Icons.Outlined.CheckCircle, null, Modifier.size(40.dp), tint = Forest)
        Text("订单 ${order.number}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(orderTime(order.createdAt), style = MaterialTheme.typography.bodySmall)
        Text("模拟订单 · 未产生真实扣款", color = PriceRed)
        Text("${order.serviceMode} · ${order.partySize} 人 · ${order.paymentMethod}（模拟）")
        Text("过敏原要求：${allergenNames(order.allergens)}")
        HorizontalDivider()
        order.lines.forEach { SummaryLine(it.name, it.quantity, it.options, it.unitPriceCents * it.quantity) }
        HorizontalDivider()
        Text("合计 ${money(order.totalCents)}", color = PriceRed, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SummaryLine(name: String, quantity: Int, options: String, total: Int) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            Text("$name × $quantity", fontWeight = FontWeight.Medium)
            if (options.isNotEmpty()) Text(options, style = MaterialTheme.typography.bodySmall)
        }
        Text(money(total))
    }
}

@Composable
private fun Modal(
    title: String, onDismiss: () -> Unit, footer: @Composable ColumnScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.safeDrawingPadding().padding(12.dp).widthIn(max = 560.dp)
                .fillMaxWidth().heightIn(max = 760.dp), shape = Corner,
        ) {
            Column {
                DialogHeading(title, onDismiss)
                HorizontalDivider()
                Column(
                    Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp), content = content,
                )
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), content = footer)
            }
        }
    }
}

@Composable
private fun DialogHeading(title: String, onDismiss: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        ToolIcon("关闭$title", Icons.Outlined.Close, onClick = onDismiss)
    }
}

@Composable
private fun ErrorNotice(error: String?) {
    if (error != null) Text(error, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), color = PriceRed, style = MaterialTheme.typography.bodySmall)
}

private fun allergenNames(values: Set<Allergen>): String = values.joinToString("、") { it.label }.ifEmpty { "未列出" }
private fun orderTime(time: Long): String = SimpleDateFormat("MM月dd日 HH:mm", Locale.CHINA).format(Date(time))