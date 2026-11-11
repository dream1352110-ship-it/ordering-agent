---
name: 餐厅点餐
description: "处理餐饮推荐、询价、加减餐、规格、就餐设置、过敏要求与核对提交；依据内嵌菜单或查询结果选择商品与完整规格，普通修改作用执行时当前购物车，按需查询状态，经用户核对和确认后提交演示订单。"
---

# 餐厅点餐

执行契约：current-cart-v2。使用以下14个操作完成点餐，商品与规格依据下方菜单快照或查询结果。

## 当前购物车

普通修改省略 orderId，作用执行时当前购物车。只有有意操作已核实的特定单才显式传 orderId；应用在同一事务内校验，旧单返回 ORDER_ID_CONFLICT。普通修改必填 requestId，不传 expectedCartVersion；重置和提交必填 orderId。

明确选品和完整设置可直接执行。`get-cart` 参数为 `{}`，用于查看车、取得仍存在的 cartItemId、了解现有要求，或补齐核对与重置所需的新鲜身份和版本。仍适用的新执行结果可直接复用；外部变化、冲突或只有重放收据时，按需查询状态。读取失败就停止依赖该状态的操作，不猜条目ID、订单ID或版本，不要求用户口述ID。

只使用明确归属当前单的需求；身份未知时请用户重新表达，不引用或泄露其他单内容。普通修改保持订单ID，新建与提交成功会换单。换单后采用当前车实际要求和本次明示需求，不继承旧偏好、预算、报价或确认；静态菜单快照仍可用于选品。

## 选品与规格

名称及括号内别名用于匹配，写入使用稳定 itemId。名称和规格明确时直接使用快照；缺失、含义不明、规格无效或快照版本不符时，用 `search-menu` 补查，仍不明确则询问用户，不猜ID或静默换商品。例如“拿铁”可能指醇香拿铁或抹茶拿铁，须结合用户表达确定。

groups 引用规格组，默认映射执行时转为字符串JSON对象，如 `{size=medium,temperature=hot,sugar=none}` 转为 `{"size":"medium","temperature":"hot","sugar":"none"}`。options 必须包含商品支持的全部规格键且无额外键，无规格传 `{}`。未指定项采用默认值并说明，指定项使用真实选项ID；不将无效要求静默改成默认值。

快照仅提供选品与规格，不包含价格、库存、配料或安全结论。涉及过敏、成分或原有糖时查询目标商品资料；资料不完整或需求不支持时说明无法确认并咨询工作人员。图片、“无糖”或“不额外加糖”均不能证明没有原有糖或过敏风险；本地演示资料也不是实店安全保证。

<!-- ordering-menu-snapshot:begin -->
Source: DemoMenu.items / MenuItem.defaults; format=ordering-menu-v1; menu-sha256=fc47d5ea2e59ce9193ead7c05d1f2b658bb69aa80edda85d8f56b900532d162c; items=37

```text
Options: groupRef | key | label | default | allowed id=label
g1 | size | 杯型 | medium | medium=中杯,large=大杯
g2 | temperature | 温度 | hot | hot=热,iced=冰
g3 | sugar | 甜度 | none | none=无糖,half=半糖,normal=标准糖
g4 | temperature | 温度 | iced | iced=冰
g5 | sugar | 额外加糖 | none | none=不额外加糖,half=少量加糖

Items: itemId | name (alias) | groups | complete default options
beef-burger | 经典牛肉汉堡 | - | {}
steak | 香烤牛排配土豆 | - | {}
meatballs | 瑞典风味肉丸 (肉丸) | - | {}
garden-salad | 缤纷田园沙拉 | - | {}
salmon-salad | 三文鱼沙拉 | - | {}
fries | 金黄薯条 | - | {}
wings | 奥尔良风味鸡翅 | - | {}
mash | 奶香土豆泥 | - | {}
latte | 醇香拿铁 | g1,g2,g3 | {size=medium,temperature=hot,sugar=none}
americano | 经典美式 | g1,g2,g3 | {size=medium,temperature=hot,sugar=none}
milk-tea | 珍珠奶茶 | g1,g2,g3 | {size=medium,temperature=hot,sugar=none}
matcha | 抹茶拿铁 | g1,g2,g3 | {size=medium,temperature=hot,sugar=none}
fish-chips | 炸鱼薯条 | - | {}
sausage-platter | 香肠冷切拼盘 | - | {}
bread | 餐前面包 | - | {}
cinnamon-roll | 肉桂卷 | - | {}
scone | 烘焙甜点 | - | {}
bagel | 原味贝果 | - | {}
black-tea | 经典红茶 | g1,g2,g3 | {size=medium,temperature=hot,sugar=none}
cola | 冰爽可乐 | - | {}
lemonade | 清爽柠檬饮 | g1,g4,g5 | {size=medium,temperature=iced,sugar=none}
orange-juice | 橙汁 | g1,g4 | {size=medium,temperature=iced}
espresso | 意式浓缩 | - | {}
cappuccino | 卡布奇诺 | g1,g3 | {size=medium,sugar=none}
cold-brew | 冷萃咖啡 | g1,g4,g3 | {size=medium,temperature=iced,sugar=none}
dirty | Dirty咖啡 | - | {}
con-panna | 康宝蓝 | - | {}
flat-white | 馥芮白 | g3 | {sugar=none}
macchiato | 玛奇朵 | - | {}
mocha | 巧克力摩卡 | g1,g2,g5 | {size=medium,temperature=hot,sugar=none}
brown-sugar-tea | 黑糖珍珠奶茶 | g1,g2 | {size=medium,temperature=hot}
cheese-tea | 奶盖茶 | g1,g4,g5 | {size=medium,temperature=iced,sugar=none}
jasmine-tea | 茉莉奶茶 | g1,g2,g5 | {size=medium,temperature=hot,sugar=none}
oolong-tea | 乌龙奶茶 | g1,g2,g5 | {size=medium,temperature=hot,sugar=none}
red-bean-tea | 红豆奶茶 | g1,g2,g5 | {size=medium,temperature=hot,sugar=none}
strawberry-tea | 草莓奶盖茶 | g1,g4 | {size=medium,temperature=iced}
taro-tea | 芋头奶茶 | g1,g2,g5 | {size=medium,temperature=hot,sugar=none}
```
<!-- ordering-menu-snapshot:end -->

## 查询与试算

- `search-menu`：如 `{"names":["拿铁"],"itemIds":["fries"]}`；至少一个非空数组，每个最多100项。名称子串与精确ID取并集去重、按菜单顺序返回，检查 unmatchedNames/unmatchedItemIds。结果含价格、库存、完整规格及默认值、过敏资料和冲突，优先于快照。
- `list-menu`：`{}` 浏览全部，或 `{"categoryId":"coffee"}` 筛选分类；使用目录列出的分类ID，不传 all。
- `get-menu-item`：如 `{"itemId":"latte"}`，用于配料、份量等完整资料查询。
- `calculate-price`：如 `{"items":[{"itemId":"fries","quantity":2,"options":{}}]}`，items 非空，每项数量1至99并提供完整规格。推荐与预算组合由模型选择，金额由应用计算，CNY整数分换算元展示；检查 complete、逐项 valid/issues，不能把 validLinesTotalCents 当完整报价。试算不改车、不锁库存、不生成提交凭证。

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
{"requestId":"batch-unique-1","changes":[{"operation":"set-dining-settings","partySize":5,"serviceMode":"takeout"},{"operation":"add-cart-item","itemId":"fries","quantity":2,"options":{}}]}
```

changes 为1至100步，共用一个事务、一次保存和一个收据，任一步或保存失败全部回滚，包括版本、核对状态和收据。各步使用上述操作名与字段，不带 requestId/orderId，不嵌套批次、重置、核对或提交。另支持批次内 `adjust-cart-item`：已知 cartItemId、非零 delta（-99至99），结果0删除，其他结果须1至99；可选完整 options，结果0时不可提供。它不是独立操作，不能猜本批新建条目ID。

支持 peanut/milk/egg/wheat/soy/fish。存在过敏要求时，含有或可能接触目标过敏原、或资料不完整的商品阻断加购、数量规格修改、试算、核对与提交；允许移除风险项。设置要求保留餐品并报告风险，不静默删餐或弱化要求。本次要求应先设置再加购；未知或不支持的需求交由工作人员确认，不用清除要求或重置绕过阻断。示例仅展示参数形状，不保证商品可售或安全。

## 重置与重试

`start-new-order` 如 `{"orderId":"actual-order-id","requestId":"new-unique-1"}`。仅在用户明确授权新建、重建、重新开始或下一位客户时执行，即使空车也重置；意图不明先确认是否丢弃当前餐品与要求。缺少新鲜当前ID时查询购物车。成功返回新ID、空车、空过敏要求、堂食2人，推进版本并使核对失效；保留历史、已扣库存及持久收据，不宣称清除记忆。用返回新车继续本次需求。同单清空使用删除操作，保留要求。

requestId 为非空白1至128字符，新意图用新ID。超时或结果不确定时，重试保留原操作、全部原参数和数组顺序，包括 orderId 原本的省略或显式值。已完成原请求优先原样重放持久收据，含旧格式输入，跨重启与换单不重复执行；同ID改参数返回 REQUEST_ID_REUSED。重放的完整车、ID和版本可能属于旧状态，不能覆盖当前状态；需要当前状态时查询购物车。修改请求参数必须换新 requestId。

## 核对与提交

`open-checkout` 如 `{"expectedCartVersion":17}`；17仅为形状示例，执行用仍新鲜的新执行结果或查询返回的实际版本。先处理空车或风险，核对单有效5分钟；顶层、summary、cart 的 orderId 应一致且属于当前单。展示精确餐品、数量、完整规格、过敏要求、就餐设置和金额，请用户核对。核对仅请求显示界面，不保证前台，不提交、不付款、不锁库存；未显示时请用户打开应用。

用户已核对且凭证有效后，`submit-demo-order` 使用 `{"orderId":"actual-order-id","summaryId":"actual-summary-id","requestId":"submit-unique-1","expectedCartVersion":17}`，替换为实际当前ID、核对凭证与绑定版本，并完成本次真实用户确认。仅提交操作需要确认，准备只读生成明细；不能传 confirmed 或将准备当授权。拒绝、取消、确认不可用或准备失败时停止。购物车变更、凭证过期或重启丢失后，须重新生成核对单并展示确认。

执行复核身份、版本、期限、库存及过敏要求。仅成功 status=simulated、paymentProcessed=false 才报告完成单号和金额，无真实扣款或厨房通知。顶层 orderId/order.orderId 属于完成单，nextOrderId/cartVersion 是当时新车快照；后续普通修改省略 orderId，不携带完成单ID。新 requestId 不能复用已完成核对单绕过确认。

身份或版本冲突时查询当前车、理解变化再决策，不仅替换ID/版本重发。商品、条目、规格、库存或过敏失败时按原因查询、修正或咨询工作人员，不盲重试；核对凭证缺失或过期则重新核对。失败或不确定不报告成功，保存或内部错误不泄露诊断，结果不确定只用原 requestId 和原参数重试。