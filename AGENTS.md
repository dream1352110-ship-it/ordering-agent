# AGENTS.md · 智能点餐 Agent 工程指南与技能规范

欢迎来到 **smartordering** (`com.sm.orderingagent`) 项目。本文档面向所有参与本项目的 AI 编码 Agent（包括 Antigravity、Claude Code、Codex、Cursor 等），定义了本项目的核心架构约束、已集成的工程技能库（Matt Pocock Skills）、14 项标准点餐能力契约、SDK 物理边界以及高质量开发工作流准则。

---

## 1. 项目概况与架构全景

本项目是一个基于 **OSAgent 第三方 CapabilityProvider SDK (1.1.0)** 的 Android 智能自助点餐应用，支持语音大模型 Agent 与触屏用户双向无缝交互。

### 1.1 核心技术栈
* **平台**：Android (minSdk 26, compileSdk 36, targetSdk 36)
* **语言与运行时**：Kotlin 2.2.10, Java 17 (SDK 类文件兼容 Java 8)
* **构建系统**：Gradle 9.6.0 (Kotlin DSL)
* **UI 框架**：Jetpack Compose (Compose BOM 2026.02.01, Material 3)
* **核心依赖**：
  * `libs/capability-contract-1.1.0.aar`
  * `libs/capability-provider-sdk-1.1.0.aar`

### 1.2 Gradle Flavor 架构分层
为了与官方验收标准完全对齐并兼顾商业演进，工程采用 `productFlavors` 架构（共享相同的 `applicationId = "com.sm.orderingagent"`）：

```
                  ┌─────────────────────────────────────────┐
                  │          OSAgent / 语音大模型 / ADB       │
                  └────────────────────┬────────────────────┘
                                       │
            ┌──────────────────────────┴──────────────────────────┐
            │                                                     │
            ▼                                                     ▼
┌───────────────────────┐                             ┌───────────────────────┐
│ flavor: function      │                             │ flavor: cli           │
│ 14 个结构化 Function  │                             │ label: "ordering"     │
│ + restaurant-ordering │                             │ + CLI Skill           │
└───────────┬───────────┘                             └───────────┬───────────┘
            │                                                     │
            └──────────────────────────┬──────────────────────────┘
                                       ▼
                       ┌───────────────────────────────┐
                       │  OrderingOperations (Facade)   │ ◀── 严格 JSON 解析 / onMain 调度
                       └───────────────┬───────────────┘
                                       ▼
                       ┌───────────────────────────────┐
                       │   OrderingEngine (Domain)     │ ◀── 纯 Kotlin 状态机 / 计价 / 校验
                       └───────────────┬───────────────┘
                                       ▼
                       ┌───────────────────────────────┐
                       │  Repository / Persistence     │ ◀── 会话状态 / AtomicFile 持久化
                       │  (可平滑切换为商用 App 数据源)  │
                       └───────────────────────────────┘
```

---

## 2. 五大架构铁律（不可违反）

1. **纯业务领域模型独立 (Domain Engine)**：
   * 点餐底层业务规则（菜品、购物车、规格计价、库存、过敏原核对、核对单过期、状态机）必须是**纯 Kotlin 编写**，完全不依赖任何 Agent SDK、Android 上下文或视图层，保证 100% 独立可测。
2. **两阶段确认机制 (Confirmation & Execution)**：
   * `confirmation` 回调**严禁产生任何副作用**：不可修改购物车、不可写磁盘、不可递增版本号、不可预扣库存。它仅负责根据当前入参生成供用户二次确认的展示文案。
   * `function` 执行阶段**必须重新独立校验**所有参数、版本号与业务前置条件（切勿轻信准备阶段的检查，防止并发篡改）。
   * 严禁在参数 Schema 中定义 `confirmed: true` 试图绕过确认机制。
3. **跨进程线程安全与调度 (`onMain`)**：
   * Function 为同步 Binder 跨进程调用，运行于系统的 Binder 线程池中。
   * 所有涉及业务状态修改和 Compose UI 响应的操作，**必须通过调度器串行投递到主线程（`onMain`）执行**，彻底避免并发数据竞争。
4. **防御性入参校验**：
   * 严格校验入参 JSON，必须为单个严格 JSON 对象（不超过 64 KiB）。
   * **坚决拒绝未知字段（Unknown Fields）**，防止大模型幻觉污染底层业务。
5. **资产驱动与菜单快照一致性**：
   * `assets/osagent/capabilities.json` 与 `assets/osagent/skills/` 必须与代码事实（菜品、ID、默认规格）保持 100% 逐字一致。
   * 必须通过 `MenuSkillSnapshotTest` 自动化测试进行代码事实与 Skill 文档的持续比对。

---

## 3. 标准 14 项点餐能力契约 (Capabilities Contract)

所有操作均以严格 JSON 对象交互，统一金额单位为 **CNY 整数分 (cents)**，执行超时均为 15000 ms。

| 操作 ID | 模式 | 必填入参 | 可选入参与商用扩展 | 核心返回字段与业务约束 |
| :--- | :--- | :--- | :--- | :--- |
| **`list-menu`** | 只读 (auto) | 无 | `categoryId` | `categories`, `allergens`, `items`。分类过滤不缩减全部分类列表。 |
| **`get-menu-item`** | 只读 (auto) | `itemId` | 无 | 菜品完整资料：配料、份量、价格、库存、规格组及默认值、过敏原。 |
| **`search-menu`** | 只读 (auto) | 无 | `names`, `itemIds` (至少一个非空) | 菜品并集列表、`unmatchedNames`, `unmatchedItemIds`。 |
| **`get-cart`** | 只读 (auto) | 无 | (入参传 `{}`) | 完整购物车：`orderId`, `cartVersion`, `lines`, `totalCents`, `blockers`, `serviceMode`, `partySize`。 |
| **`apply-cart-changes`**| 修改 (auto) | `requestId`, `changes` | `orderId` | 1 至 100 步原子批次变更，支持 add/update/remove/adjust/settings。单步失败全量回滚。 |
| **`start-new-order`** | 修改 (auto) | `requestId`, `orderId` | 无 | 明确清空并重置当前车，重置为堂食 2 人，生成新 `orderId` 并推进版本。 |
| **`set-dining-settings`**| 修改 (auto) | `requestId` | `orderId`, `serviceMode`, `partySize`, `tableNo`(扩展), `remarks`(扩展) | 设置堂食/打包、人数、桌号及备注，设置项至少提供一个。 |
| **`add-cart-item`** | 修改 (auto) | `requestId`, `itemId`, `quantity`, `options` | `orderId` | 同菜品同规格自动合并数量，全车过敏原与总库存阻断校验。 |
| **`update-cart-item`** | 修改 (auto) | `requestId`, `cartItemId` | `orderId`, `quantity`, `options` | 按购物车行 ID 修改，数量或规格至少提供一项，规格提供时整体替换。 |
| **`remove-cart-item`** | 修改 (auto) | `requestId`, `cartItemId` | `orderId` | 删除整条购物车行，不使用 quantity=0 表达删除。 |
| **`set-allergy-requirements`**| 修改 (auto) | `requestId`, `allergenIds` | `orderId` | 整体替换避忌过敏原清单。仅支持 6 大过敏原，冲突时强阻断但不自动删餐。 |
| **`calculate-price`** | 试算 (auto) | `items` | 无 | 候选餐品试算，返回每行单价、小计、有效性及 issues，全部合法才返回 `totalCents`。不改车、不扣库存。 |
| **`open-checkout`** | 核对 (auto) | `expectedCartVersion` | 无 | 生成 5 分钟有效期的核对单（`summary`），请求前台 UI 唤起（`ui.launchRequested`）。不推进版本、不扣库存。 |
| **`submit-demo-order`** | 提交 (**confirm**) | `requestId`, `orderId`, `summaryId`, `expectedCartVersion` | 无 | **需用户二次确认**。复核有效核对单与版本，生成模拟订单 `order`，清空购物车并生成 `nextOrderId`。 |

---

## 4. 业务领域规则规范 (Domain Rules)

1. **金额计算法则**：
   * 所有金额字段均为非负 Int 整数分（CNY cents，例如 3200 代表 ¥32.00），严禁使用浮点数 Float/Double。
   * 行单价 = 基础单价 + 规格加价；行小计 = 行单价 × 数量；总金额 = 各行小计累加。
2. **并发控制与版本机制**：
   * 购物车维护 64 位无损版本号 `cartVersion`（初始为 1）。
   * 任何导致购物车商品、数量、规格、就餐模式或过敏要求的写操作，均推进版本号。
   * 核对（`open-checkout`）与提交（`submit-demo-order`）强制要求传入 `expectedCartVersion`，版本不一致直接拒绝（`CART_VERSION_CONFLICT`）。
3. **过敏原安全阻断体系**：
   * 仅支持 6 种标准过敏原：`peanut`（花生）、`milk`（牛奶）、`egg`（鸡蛋）、`wheat`（小麦）、`soy`（大豆）、`fish`（鱼）。
   * 一旦设置过敏原避忌，任何含有（`contains`）或可能含有（`mayContain`）该成分的商品，或过敏资料不全的商品，**必须被彻底阻断加购、核对与提交**。
4. **幂等性与请求去重**：
   * 8 类写操作强制携带客户端生成的 `requestId`。
   * 相同 `requestId` 且参数一致时，重放历史收据结果；相同 `requestId` 但参数不一致时，坚决拒绝（`REQUEST_ID_CONFLICT`）。
5. **核对单有效期 (Checkout Summary Lifecycle)**：
   * 核对单具有严格的时效性：`0 <= (now - createdAt) <= 300,000 ms`（5 分钟）。
   * 超时未提交、购物车发生任何新变更或应用进程重启，旧核对单自动彻底失效，必须重新调用 `open-checkout`。

---

## 5. SDK 物理规范与接入细节 (SDK Physical Boundaries)

严格遵循 OSAgent CapabilityProvider SDK 1.1.0 物理约束：

### 5.1 资产物理限制 (Catalogue & Skills)
* **Catalogue 约束**：`src/main/assets/osagent/capabilities.json` 编码必须为 UTF-8，文件最大不能超过 **256 KiB**。单个 Provider 最多声明 **100 个 Function**。
* **参数 Schema 大小**：原始 Schema 的 UTF-8 编码大小不超过 **64 KiB**，嵌套深度不得超过 16 层。
* **Skill 文档规范**：`SKILL.md` 正文最大不超过 **256 KiB**，文件第一行必须严格以 `---` 开头 frontmatter（包含 `name` 和 `description`），不要在前面插入任何注释或标题。
* **路径格式要求**：`path` 必须为至少两段式的相对路径（如 `osagent/skills/<目录>/SKILL.md`），禁止以 `/` 开头或包含 `..`。
* **欢迎页建议 (`promptSuggestions`)**：每个 Skill 最多 64 项；建议 `id` 必须匹配正则 `^[a-z][a-z0-9-]{0,63}$`；`priority` 仅允许取整数 `0` 或 `1`。

### 5.2 错误码隔离铁律
* **业务错误码与 SDK 系统错误码严格隔离**：
  * SDK 会自动返回框架层错误码（如 `CALLER_NOT_ALLOWED`、`PROTOCOL_VERSION_UNSUPPORTED`、`FUNCTION_NOT_FOUND`、`HANDLER_EXEC_ERROR` 等）。
  * **Provider 严禁在业务失败时借用 SDK 系统错误码**！业务校验不通过时，必须返回 Provider 专属的业务错误码，例如：`INVALID_ARGUMENTS`、`ITEM_NOT_FOUND`、`OUT_OF_STOCK`、`ALLERGEN_CONFLICT`、`CART_VERSION_CONFLICT`、`ORDER_ID_CONFLICT`、`REQUEST_ID_CONFLICT`、`CHECKOUT_EXPIRED`。

### 5.3 CLI 运行时输出规范
* **退出码映射**：Provider 的 `CapabilityCliHandler` 执行完成后只能返回 `CapabilityCliStatus.SUCCESS` 或 `CapabilityCliStatus.FAILURE`，由 SDK 分别映射为进程退出码 `0` 或 `1`。**严禁在代码中调用 `System.exit()`**。
* **通道职责划分**：业务成功数据必须原样写 `output.stdout(json)`（紧凑 JSON，不混入多余日志）；业务失败或错误诊断信息写 `output.stderr(text)`。
* **流管理**：Handler 必须在 `finally` 块中调用 `output.flush()`，但**严禁调用 `output.close()`**（通道由 SDK 在调用结束后统一关闭）。

### 5.4 超时与生命周期窗口
* **实际等待窗口**：系统端 App 绑定与调用等待窗口为 **`5000 ms + execution.timeoutMs`**。
* **超时非取消原则**：本地等待超时或命令取消**不等于**远端 Provider 业务已停止。针对可能超时或网络抖动的修改操作，必须依赖 `requestId` 幂等重试或主动状态查询，不可直接当成未执行而盲目新建重复请求。

---

## 6. 集成的 Agent Skills 技能库与工作流

本项目已将 [Matt Pocock Skills](https://github.com/mattpocock/skills) 完整集成至 `.agents/skills/` 目录中。

### 6.1 核心主线：从想法到交付 (Idea → Ship)

```
[ 需求/想法 ]
     │
     ▼
1. /grill-with-docs ───────────▶ 审视需求，建立统一语言 (GLOSSARY.md & ADR)
     │
     ├─▶ [设计存疑/需探索] ──▶ /prototype (快速原型验证) ──▶ /handoff
     │
     ▼
2. /to-spec ───────────────────▶ 提炼完整技术规格文档
     │
     ▼
3. /to-tickets ────────────────▶ 拆解为带阻塞拓扑依赖的独立任务卡片
     │
     ▼
4. /implement ─────────────────▶ 推进编码：严格驱动 /tdd (红-绿-重构循环)
   (或 /implement-spec)           每个 Seam 完成后调用 /code-review
     │
     ▼
5. /pr ────────────────────────▶ 整理 Pull Request 变更说明与风险评估
     │
     ▼
6. /retro ─────────────────────▶ 复盘会话，沉淀经验并改进工程环境
```

### 6.2 常用技能快速索引 (Skill Cheat Sheet)

| 技能名称 | 路径 | 触发方式 | 何时使用 |
| :--- | :--- | :--- | :--- |
| **ask-matt** | `.agents/skills/engineering/ask-matt/` | User | 不确定当前场景该用哪个技能时，担任技能总路由 |
| **grill-with-docs** | `.agents/skills/engineering/grill-with-docs/` | User | 需求讨论阶段：深度提问面试，沉淀领域模型与 ADR |
| **grill-me** | `.agents/skills/productivity/grill-me/` | User | 纯提问面试，不落本地文档（快速理清思路） |
| **to-spec** | `.agents/skills/engineering/to-spec/` | User | 将对话和结论整理为正式的技术规范 Specification |
| **to-tickets** | `.agents/skills/engineering/to-tickets/` | User | 将 Spec 拆解为可独立执行的子任务（声明前置依赖） |
| **implement** | `.agents/skills/engineering/implement/` | User | 逐步实施任务，强制触发 `/tdd` 和 `/code-review` |
| **implement-spec** | `.agents/skills/engineering/implement-spec/` | User | 启动子智能体并发完成全量 Spec |
| **tdd** | `.agents/skills/engineering/tdd/` | Model | 测试驱动开发：严格红-绿-重构循环 |
| **diagnosing-bugs**| `.agents/skills/engineering/diagnosing-bugs/` | Model | 疑难 Bug 诊断：先写红灯测试再修复 |
| **code-review** | `.agents/skills/engineering/code-review/` | Model | 双轴审查：代码规范检查 + Spec 一致性核对 |
| **domain-modeling**| `.agents/skills/engineering/domain-modeling/` | Model | 统一领域词汇，维护 ADR 和 GLOSSARY |
| **codebase-design**| `.agents/skills/engineering/codebase-design/` | Model | 设计深模块（Deep Module）与清晰接缝 |
| **improve-codebase-architecture** | `.agents/skills/engineering/improve-codebase-architecture/` | User | 扫描代码架构瓶颈并输出可视化改善报告 |
| **prototype** | `.agents/skills/engineering/prototype/` | Model | 快速构建抛弃型原型验证技术可行性 |
| **research** | `.agents/skills/engineering/research/` | Model | 后台查阅权威一手资料并输出调研报告 |
| **handoff** | `.agents/skills/productivity/handoff/` | User | 生成紧凑会话交接记录，便于切换窗口或跨 Agent 协作 |
| **pr** | `.agents/skills/engineering/pr/` | Model | 编写高质量 Pull Request 描述 |
| **retro** | `.agents/skills/engineering/retro/` | User | 复盘开发会话，改进 Agent 工具链与工程约束 |

---

## 7. 测试与质量门禁规范

1. **红-绿-重构 (TDD) 铁律**：
   * 任何涉及计价、购物车修改、库存扣减、核对单过期或过敏校验的改动，**必须先写失败的单元测试**。
   * 保持纯业务测试完全脱离 Android Framework，确保可以在毫秒级完成全量回归。
2. **菜单快照自动化测试 (`MenuSkillSnapshotTest`)**：
   * 当增加、删除菜品或调整规格默认值时，必须运行快照校验测试，确保代码中的菜单与 `SKILL.md` 的描述 100% 逐字对应。
3. **双轴代码审查门禁 (`/code-review`)**：
   * **Standards 轴**：Kotlin 惯用写法、异常处理完备、无内存泄露风险、无未知字段宽松解析。
   * **Spec 轴**：与第 3 节 14 项契约逐字核对，无多余未声明参数，无遗漏错误分支。
4. **文字整洁原则**：
   * 遵循 Matt Pocock 规则，文档与注释中避免滥用破折号（em-dash），使用清晰准确的标点与连接词。

---

## 8. 设备联调与发布检查表

### 8.1 系统双向信任配置命令 (ADB)
```sh
# 1. 允许系统发现本 Provider 应用
adb shell settings put system sm_os_cap_providers com.sm.orderingagent

# 2. 允许调用者（OSAgent 语音助手 + 调试 Shell）调用本应用
adb shell settings put system sm_os_cap_callers com.sm.osagent:com.android.shell
```

### 8.2 发布前必核检查清单 (Checklist)
* [ ] 两个 SDK AAR（`contract` 与 `provider-sdk`）版本一致且未手写重复 Service。
* [ ] Merged Manifest 中包含自动合并的 `SmCapabilityProviderService`。
* [ ] `Application.onCreate` 中已完成 Handler 集合安装（不得为空 Map）。
* [ ] `capabilities.json` 合法无语法错误，协议版本精确匹配，大小小于 256 KiB。
* [ ] 所有 Function 均具备同名 Handler 映射，超时设定在 15000–60000 ms 之间。
* [ ] 敏感提交操作配置为 `confirm` 模式，且确认回调**无任何副作用**。
* [ ] 跨进程请求已通过 `onMain` 调度至主线程，防止并发竞态。
* [ ] 防御性 JSON 解析对未知字段严格拒绝。
* [ ] 单元测试全量通过（`./gradlew test`）。

---

## 9. Git 提交规范

为了保证版本提交历史的清晰、可回溯与自动化处理能力，所有代码提交必须遵循本节规范：

* **语言与格式**：Git commit message 一律使用**简体中文**，并采用 **Conventional Commits** 标准格式：
  ```
  <type>(<scope>): <中文说明>
  ```
* **常用 type 类型**：
  * `feat`：新增功能（特性）
  * `fix`：修复缺陷（Bug）
  * `docs`：文档变更（如更新 AGENTS.md、SKILL.md、注释等）
  * `refactor`：代码重构（既不修复 bug 也不添加新功能的代码变更）
  * `perf`：性能优化
  * `test`：增加或修改测试用例
  * `build`：构建系统或外部依赖项变更（如 Gradle 配置、AAR 引入等）
  * `ci`：持续集成配置与脚本变更
  * `chore`：其他杂项或辅助工具的变动
  * `revert`：撤销先前的提交
* **scope 范围约定**：使用实际组件或业务领域，例如 `pos`、`login`、`api`、`device`、`printer` 或 `domain`；不适用时可以省略括号。
* **subject 规则**：使用简洁的祈使句说明本次提交做了什么，建议不超过 **50 个字符**。
* **body 与 footer（可选）**：
  * 可选 `body` 用于说明改动动机、上下文和关键实现，每条要点独占一行，建议每行不超过 **72 个字符**；
  * `footer` 用于标识 `BREAKING CHANGE:` 或关联关联任务/Issue（如 `Closes #123`）。
* **原子化提交原则**：**一次提交只包含一个可说明、可独立验证的逻辑变更**，严禁混入用户无关改动、本机环境差异、临时配置、私有密钥或构建缓存产物。
