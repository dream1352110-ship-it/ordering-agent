# AGENTS.md · 智能点餐 Agent 工程指南与技能规范

欢迎来到 **smartordering** (`com.sm.orderingagent`) 项目。本文档面向所有参与本项目的 AI 编码 Agent（包括 Antigravity、Claude Code、Codex、Cursor 等），定义了本项目的核心架构约束、已集成的工程技能库（Matt Pocock Skills）以及开发工作流准则。

---

## 1. 项目概况与架构核心

本项目是一个基于 **OSAgent 第三方 CapabilityProvider SDK (1.1.0)** 的 Android 智能自助点餐应用，支持语音/大模型 Agent 与触屏用户双向无缝交互。

### 核心技术栈
* **平台**：Android (minSdk 26, compileSdk 36, targetSdk 36)
* **语言与运行时**：Kotlin 2.2.10, Java 17 (SDK 类文件兼容 Java 8)
* **构建系统**：Gradle 9.6.0 (Kotlin DSL)
* **UI 框架**：Jetpack Compose (Compose BOM 2026.02.01, Material 3)
* **核心依赖**：
  * `libs/capability-contract-1.1.0.aar`
  * `libs/capability-provider-sdk-1.1.0.aar`

### 架构铁律（不可违反）
1. **纯业务领域模型独立 (Domain Engine)**：底层点餐逻辑（菜单、购物车、规格计价、库存、过敏原核对、核对单过期、状态机）必须是纯 Kotlin 编写，完全不依赖任何 Agent SDK 或 Android 视图层，保证 100% 独立可测。
2. **两阶段确认机制 (Confirmation & Execution)**：
   * `confirmation` 回调**严禁产生任何副作用**：不可修改购物车、不可写磁盘、不可递增版本号、不可预扣库存。它仅负责生成供用户二次确认的展示文案。
   * `function` 执行阶段**必须重新独立校验**所有参数、版本号与业务前置条件（切勿轻信准备阶段的检查，防止并发篡改）。
   * 严禁在参数 Schema 中定义 `confirmed: true` 试图绕过确认机制。
3. **跨进程线程安全与调度 (`onMain`)**：
   * Function 为同步 Binder 跨进程调用，运行于系统的 Binder 线程池中。
   * 所有涉及业务状态修改和 Compose UI 响应的操作，**必须通过调度器串行投递到主线程（`onMain`）执行**，彻底避免并发数据竞争。
4. **防御性入参校验**：
   * 严格校验入参 JSON，必须为单个严格 JSON 对象（不超过 64 KiB）。
   * **坚决拒绝未知字段（Unknown Fields）**，防止大模型幻觉污染底层业务。
5. **资产驱动一致性**：
   * `assets/osagent/capabilities.json` 与 `assets/osagent/skills/` 必须与代码事实（菜品、ID、默认规格）保持 100% 逐字一致。

---

## 2. 集成的 Agent Skills 技能库

本项目已将 [Matt Pocock Skills](https://github.com/mattpocock/skills) 完整集成至 `.agents/skills/` 目录中。这是一套凝聚了现代软件工程实践的 Agent 专用技能，旨在杜绝“凭感觉写代码（vibe coding）”，倡导严谨、可复现、高内聚的设计。

### 技能分类与存放路径
* **日常工程技能**：`.agents/skills/engineering/` (20 个)
* **生产力与沟通工具**：`.agents/skills/productivity/` (7 个)
* **实验与前沿技能**：`.agents/skills/in-progress/` (8 个)
* **辅助工具**：`.agents/skills/misc/` (4 个)

### 技能调用方式
* **用户触发 (User-invoked)**：由人类开发者显式输入 `/skill-name` 触发，主要负责整体流程编排与关键决策。
* **模型自主触发 (Model-invoked)**：当开发场景契合时，Agent 可自主读取并执行对应的技能指导，承载复用性的工程自律（如 TDD、代码审查、Bug 诊断）。

---

## 3. 核心研发工作流（Workflows）

Agent 在执行功能研发、重构或调试时，应自觉遵循以下流转闭环：

### 核心主线：从想法到交付 (Idea → Ship)

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

#### 各环节关键说明：
* **`/grill-with-docs`**：开始一项新功能开发前的必经步骤。通过有条理的追问挖掘所有分支细节，消除模糊地带，并同步沉淀到 `GLOSSARY.md`。
* **`/tdd`**：编写业务逻辑（如购物车计算、过敏原排除）时，必须坚持**先写失败的单元测试，再写最小实现代码，最后重构**。
* **`/code-review`**：提交前进行双轴审查：
  1. **Standards 轴**：是否符合 Clean Code、Kotlin 惯用写法、无异味；
  2. **Spec 轴**：是否精准实现需求，无多余私货，无遗漏分支。
* **`/retro`**：在一次复杂的重构或踩坑修复后，反思当前工程规范或配置是否有漏洞，提出改进建议。

---

### 常见支线工作流 (On-Ramps)

#### 1. 遇到复杂或棘手 Bug ➔ `/diagnosing-bugs`
* **严禁在未定位复现前凭空猜测或直接改业务代码**。
* 严格遵循闭环：
  1. **构建确定性测试**：编写一个能稳定红灯复现该 Bug 的最小测试用例；
  2. **最小化场景**：剔除无关上下文；
  3. **提出假设并插桩验证**：通过日志或断点验证假设；
  4. **修复与回归验证**：确保红灯变绿，且原有测试全量通过。

#### 2. 代码库日常维护与深层优化 ➔ `/improve-codebase-architecture`
* 定期审视项目模块划分，寻找“浅接口、深实现”的重构机会。
* 结合 `/codebase-design` 设计高内聚低耦合的业务接缝。

#### 3. 沟通与表达矫正 ➔ `/wait-what`
* 当与人类用户的对话出现偏差或理解不一致时，立即停下，使用大白话及领域词汇重新梳理共识。

---

## 4. 常用技能快速索引 (Skill Cheat Sheet)

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

## 5. Agent 编码规范与纪律

1. **坚持测试驱动**：修改核心计价、购物车与库存逻辑时，必须先有对应的单元测试用例。
2. **保持注释与文档严谨**：代码修改必须维持现有注释与 KDoc 的准确性；不要随意删除原有的防御性检查。
3. **保持文本整洁**：遵循 Matt Pocock 规则，文档与注释中避免滥用破折号（em-dash），使用清晰准确的标点与连接词。
4. **修改必须可验证**：每次关键代码编写完成后，执行 `./gradlew test` 或特定模块测试，确认构建通过后再进行下一步。
