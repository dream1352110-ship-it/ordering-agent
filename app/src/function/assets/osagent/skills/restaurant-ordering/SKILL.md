---
name: 餐厅点餐
description: "处理餐饮推荐、询价、加减餐、规格、就餐设置、过敏要求与核对提交；选品必须使用list-menu或search-menu返回的门店商品和完整规格，普通修改作用执行时当前购物车，按需查询状态，经用户核对和确认后提交门店订单。"
---

# 餐厅点餐

执行契约：current-cart-v2。使用以下18个操作完成点餐。商品与规格只能来自本次 `list-menu` 或 `search-menu` 的返回。

## 核心交互规则（最高优先级，匹配意图必须直接调用操作，严禁纯文本回复）

1. **跳转订单核对修改页 (Review Order)**：
   当用户表达以下意图时，**绝对严禁纯文本回复，绝对严禁询问用户“您想修改什么”，必须立即调用 `review-order`（入参传 `{}`）**：
   - 点餐完成/确认：“我点完了”、“点完了”、“就这些了”、“好了”、“我要确认订单”、“我点完了，我要确认订单”、“确认订单”、“帮我下单”、“确认下单”、“结账”、“买单”；
   - 查看/修改订单：“我想修改订单”、“我想修改一下订单”、“修改订单”、“我想改一下”、“查看订单”、“看看我点了什么”、“看一下购物车”、“核对订单”。
   调用 `review-order` 会直接将顾客屏/车载屏切换到 Review order 订单核对与修改页面，展示当前全部菜品、数量与规格，供用户核对或修改。
   **特别警告**：用户说“我点完了”、“我要确认订单”、“确认订单”、“确认下单”时，用户意图是**完成点餐并进入核对页面**，**严禁直接调用 `submit-demo-order`**（因为此时没有核对单凭证 summaryId 会报错失败），**必须直接调用 `review-order`**！只有在屏幕已经进入 Review Order 页面且用户最终再次明确确认支付时，才调用 `submit-demo-order`。
2. **跳转呼叫人工客服页 (Staff Assistance)**：
   当用户表达“寻求帮助”、“人工客服”、“呼叫服务员”、“找店员”、“找人工”、“人工服务”等意图时，**必须立即调用 `request-staff-assistance`（入参传 `{}`）**，屏幕将切换到人工客服接管页面（A team member is joining）。
3. **跳转点餐应用菜单全览页 (Show Menu)**：
   当用户表达以下意图时，**绝对严禁纯文本回复，绝对严禁在语音助手界面上列出菜单，严禁调用 `list-menu`，必须立即调用 `show-menu`（入参传 `{}`）**：
   - 查看/返回菜单：“我要看菜单”、“看菜单”、“看看菜单”、“我想看菜单”、“打开菜单”、“显示菜单”、“浏览菜单”、“返回菜单”、“切换到菜单”、“回菜单页”、“看下有什么吃的”；
   调用 `show-menu` 会直接唤醒点餐应用并在屏幕内切换到点餐菜单主页（Menu Stage），关闭弹窗并展示全部菜品分类与商品明细，交由用户触屏查看。

## 当前购物车

普通修改省略 orderId，作用执行时当前购物车。只有有意操作已核实的特定单才显式传 orderId；应用在同一事务内校验，旧单返回 ORDER_ID_CONFLICT。普通修改必填 requestId，不传 expectedCartVersion；重置和提交必填 orderId。

明确选品和完整设置可直接执行。`get-cart` 参数为 `{}`，用于查看车、取得仍存在的 cartItemId、了解现有要求，或补齐核对与重置所需的新鲜身份和版本。仍适用的新执行结果可直接复用；外部变化、冲突或只有重放收据时，按需查询状态。读取失败就停止依赖该状态的操作，不猜条目ID、订单ID或版本，不要求用户口述ID。

只使用明确归属当前单的需求；身份未知时请用户重新表达，不引用或泄露其他单内容。普通修改保持订单ID，新建与提交成功会换单。换单后采用当前车实际要求和本次明示需求，不继承旧偏好、预算、报价或确认。

## 选品与规格

顾客说出菜名后，先用 `search-menu` 或 `list-menu` 查询。只使用本次返回的 `itemId` 和 `optionGroups`。名称不唯一或未命中时询问用户，不猜 ID，不用旧对话里的商品 ID。

`options` 的键是该商品 `optionGroups[].id`，值是 `choices[].id`。`required` 为 true 的组必须全部提供；没有规格组时传 `{}`。顾客没提到的做法和加料不要自行加入。默认值只用本次返回的 `defaultId`，并在回复里说明。规格不存在时询问，不换成别的选项。

价格、库存、配料和过敏结论以 `get-menu-item` 或 `search-menu` 为准。资料不完整或需求不支持时说明无法确认并咨询工作人员。

## 查询与试算

- `search-menu`：如 `{"names":["顾客说的菜名"],"itemIds":["<本次返回的itemId>"]}`；至少一个非空数组，每个最多100项。名称子串与精确ID取并集去重、按菜单顺序返回，检查 unmatchedNames/unmatchedItemIds。结果含价格、库存、完整规格及默认值、过敏资料和冲突，这是选品依据。
- `list-menu`：`{}` 浏览门店全部在售商品，或 `{"categoryId":"<本次返回的categoryId>"}` 筛选；分类ID以当次返回为准，不传 all。注意：当用户说“我要看菜单”时请优先调用 `show-menu` 跳转应用内界面。
- `get-menu-item`：`{"itemId":"<list-menu或search-menu返回的itemId>"}`，用于配料、份量等完整资料查询。
- `calculate-price`：如 `{"items":[{"itemId":"<本次返回的itemId>","quantity":2,"options":{}}]}`，items 非空，每项数量1至99并提供完整规格。推荐与预算组合由模型选择，金额由应用计算，按返回的 currency 以最小单位换算展示；检查 complete、逐项 valid/issues，不能把 validLinesTotalCents 当完整报价。试算不改车、不锁库存、不生成提交凭证。

独立只读查询可并行；依赖前一步结果的请求须等待，购物车修改按顺序执行。

## 修改与过敏要求

以下五个单操作和批次共六个普通写入口，均使用 requestId、可选 orderId，返回完整购物车、实际 orderId/cartVersion 及 requestId/operation：

- `add-cart-item`：itemId、quantity（增加量1至99）、完整 options。同商品同规格合并数量，库存按商品跨规格汇总。
- `update-cart-item`：已知 cartItemId，quantity（绝对量1至99）或完整 options 至少一项；省略字段保留，options 提供时完整替换，合并后使用返回条目ID。0不是删除。
- `remove-cart-item`：已知 cartItemId，删除整条餐品，保留过敏与就餐设置。cartItemId 来自当前车结果，不是 itemId 或行号。
- `set-dining-settings`：serviceMode/partySize 至少一项，方式仅 dine-in/takeout，人数1至12，省略项保留；返回 serviceMode 为显示文字，serviceModeId 为稳定ID。
- `set-allergy-requirements`：必填完整 allergenIds，整体替换而非追加，只有用户明确清除才传 `[]`。

多个明确修改使用 `apply-cart-changes`，例如：

```json
{"requestId":"batch-unique-1","changes":[{"operation":"set-dining-settings","partySize":5,"serviceMode":"takeout"},{"operation":"add-cart-item","itemId":"<本次返回的itemId>","quantity":2,"options":{}}]}
```

changes 为1至100步，共用一个事务、一次保存和一个收据，任一步或保存失败全部回滚，包括版本、核对状态和收据。各步使用上述操作名与字段，不带 requestId/orderId，不嵌套批次、重置、核对或提交。另支持批次内 `adjust-cart-item`：已知 cartItemId、非零 delta（-99至99），结果0删除，其他结果须1至99；可选完整 options，结果0时不可提供。它不是独立操作，不能猜本批新建条目ID。

支持 peanut/milk/egg/wheat/soy/fish。存在过敏要求时，含有或可能接触目标过敏原、或资料不完整的商品阻断加购、数量规格修改、试算、核对与提交；允许移除风险项。设置要求保留餐品并报告风险，不静默删餐或弱化要求。本次要求应先设置再加购；未知或不支持的需求交由工作人员确认，不用清除要求或重置绕过阻断。示例仅展示参数形状，不保证商品可售或安全。

## 重置与重试

`start-new-order` 如 `{"orderId":"actual-order-id","requestId":"new-unique-1"}`。仅在用户明确授权新建、重建、重新开始或下一位客户时执行，即使空车也重置；意图不明先确认是否丢弃当前餐品与要求。缺少新鲜当前ID时查询购物车。成功返回新ID、空车、空过敏要求、堂食2人，推进版本并使核对失效；保留历史、已扣库存及持久收据，不宣称清除记忆。用返回新车继续本次需求。同单清空使用删除操作，保留要求。

requestId 为非空白1至128字符，新意图用新ID。超时或结果不确定时，重试保留原操作、全部原参数和数组顺序，包括 orderId 原本的省略或显式值。已完成原请求优先原样重放持久收据，含旧格式输入，跨重启与换单不重复执行；同ID改参数返回 REQUEST_ID_REUSED。重放的完整车、ID和版本可能属于旧状态，不能覆盖当前状态；需要当前状态时查询购物车。修改请求参数必须换新 requestId。

## 查看、修改订单与核对提交

当用户表达以下意图时，必须立即调用 `review-order` 或 `open-checkout` 跳转到订单核对页面（Review order）：
1. **完成点餐**：“我点完了”、“我要确认订单”、“结账”、“买单”、“核对订单”、“就这些了”、“好了”、“帮我下单”等；
2. **查看与修改订单明细**：“我想修改订单”、“查看订单”、“修改订单”、“看看我点了什么”、“我想改一下”、“看一下购物车”等。

调用方法极简，入参直接传 `{}` 即可，系统将立即把前台屏幕平滑切换到 Review order 订单核对修改页面，展示当前全部菜品、数量与规格，供用户核对或修改。也可带上已知版本如 `{"expectedCartVersion":17}`。核对单有效5分钟；顶层、summary、cart 的 orderId 应一致且属于当前单。展示精确餐品、数量、完整规格、过敏要求、就餐设置和金额，请用户核对。核对仅请求显示界面，不保证前台，不提交、不付款、不锁库存；未显示时请用户打开应用。

用户已核对且凭证有效后，`submit-demo-order` 使用 `{"orderId":"actual-order-id","summaryId":"actual-summary-id","requestId":"submit-unique-1","expectedCartVersion":17}`，替换为实际当前ID、核对凭证与绑定版本，并完成本次真实用户确认。仅提交操作需要确认，准备只读生成明细；不能传 confirmed 或将准备当授权。拒绝、取消、确认不可用或准备失败时停止。购物车变更、凭证过期或重启丢失后，须重新生成核对单并展示确认。

执行复核身份、版本、期限、库存及过敏要求，并向门店创单。成功时读取 status、paymentProcessed、gaap 和 storeOrder；paymentProcessed=false 表示本机未扣款。gaap.gaapOrderId 是门店订单号。用 gaap.displayOrderNo 或 storeOrder.collectNo 报告取餐号，没有则说明尚未发号。顶层 orderId/order.orderId 属于完成单，nextOrderId/cartVersion 是当时新车快照；后续普通修改省略 orderId，不携带完成单ID。新 requestId 不能复用已完成核对单绕过确认。

`get-store-order`：`{"orderId":"<gaap.gaapOrderId>"}`。查询门店订单的状态、金额和明细。店员改单或结账后，用同一个门店订单号再次调用刷新屏幕，不要根据本地购物车猜测。

身份或版本冲突时查询当前车、理解变化再决策，不仅替换ID/版本重发。商品、条目、规格、库存或过敏失败时按原因查询、修正或咨询工作人员，不盲重试；核对凭证缺失或过期则重新核对。失败或不确定不报告成功，保存或内部错误不泄露诊断，结果不确定只用原 requestId 和原参数重试。

## 呼叫人工与寻求帮助

当用户表达“寻求帮助”、“人工客服”、“呼叫服务员”、“找店员”、“找人工”、“人工服务”等需要人工介入的意图时，必须立即调用 `request-staff-assistance`（可选参数 `reason` 如 `{"reason":"Customer requested a team member"}` 或 `{}`）。系统将立即跳转至人工客服接管界面（A team member is joining），并在车道屏顶部点亮 Help requested 警示状态。

## 查看与返回菜单主页

当用户表达“我要看菜单”、“看菜单”、“看看菜单”、“我想看菜单”、“打开菜单”、“显示菜单”、“浏览菜单”、“返回菜单”、“切换到菜单”、“回菜单页”等意图时，必须立即调用 `show-menu`（入参传 `{}`）。系统将立即把屏幕切换到点餐菜单全览页面（Menu Stage），关闭弹窗并展示全部菜品列表。也可以调用 `list-menu`，两者均会同步刷新屏幕回到菜单页。
