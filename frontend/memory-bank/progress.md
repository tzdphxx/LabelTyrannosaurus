# Progress

## 2026-05-30 - 计划一：基础可跑

### 已实现

- 完成基础目录拆分：`app`、`pages`、`components`、`stores`、`types`、`hooks`、`utils`。
- 接入 React Router，建立 `/login`、`/app/owner`、`/app/labeler`、`/app/reviewer` 基础路由。
- 实现登录页，支持任务负责人、标注员、审核员三种演示身份登录。
- 实现登录态与角色路由守卫：
  - 未登录访问 `/app/*` 跳转 `/login`。
  - 已登录访问 `/login` 跳转当前角色首页。
  - 已登录访问非当前角色页面时跳回自己的角色首页。
- 抽离公共布局：
  - `TopNav`：顶部导航栏。
  - `SideNav`：侧边导航栏。
  - `AppLayout`：应用主布局。
- 抽离 P1 共享基础组件：
  - `PageHeader`：页面标题区。
  - `RoleBadge`：角色标签。
  - `ContentShell`：主内容容器。
- 使用 Zustand 承载基础状态：
  - `authStore`：当前用户、当前角色、登录态、登录/退出。
  - `navigationStore`：侧边栏折叠、当前路径、当前菜单项。
  - `pageUiStore`：页面基础状态。
- 建立阶段一核心类型：
  - `Role`
  - `User`
  - `AuthState`
  - `NavItem`
  - `PageState`
- 配置三类角色导航与首页占位内容。
- 接入 Ant Design 基础组件与全局样式。

### 当前约束

- 阶段一没有建立 `services` 服务层骨架。
- 阶段一没有实现 API Client。
- 阶段一没有接入 Mock 业务数据。
- 当前认证仅为前端演示态，刷新后不会持久化登录态。



## 2026-05-30 - 计划三：动态表单核心 P0

### 已实现

- 完成动态表单 P0 的基础类型与 schema 契约：
  - `DynamicFormSchema`
  - `DynamicSchemaNode`
  - `DynamicValidationRule`
  - `DynamicVisibleRule`
  - `DynamicFormSubmitResult`
- 新增 dnd-kit + Formily 动态表单能力目录：
  - 物料注册中心
  - schema tree 增删改查与排序工具
  - schema 到 Formily schema 的转换层
  - Formily Renderer 组件
- 完成 P0 8 类基础物料：
  - 单行输入
  - 多行输入
  - 单选
  - 多选
  - 标签选择
  - ShowItem 展示项
  - 分组容器
  - Tab 容器
- 扩展模板 Mock 数据，模板现在包含可读写的动态表单 schema。
- 扩展 Owner 模板服务层：
  - 查询模板列表
  - 查询模板详情
  - 查询模板 schema
  - 保存模板 schema
- 新增 `templateDesignerStore`，承载模板 Designer 的 schema、选中节点、保存状态、加载状态、错误状态和未保存状态。
- 新增 Owner 模板管理页面：
  - `/app/owner/templates`
- 新增 Owner 模板 Designer 页面：
  - `/app/owner/templates/:templateId/designer`
- Designer P0 支持：
  - 从物料区拖拽添加字段
  - 画布内字段排序
  - 容器内字段承载
  - 字段选择
  - 字段标题、key、占位提示、选项、必填、基础显隐条件编辑
  - 字段删除二次确认
  - schema 保存
  - Formily 运行态预览
  - 提交预览输出结构化结果
- 补充 Designer、Renderer、画布、属性面板和预览区样式。

### 当前约束

- 当前环境中 `nvm`、`npm`、`node` 均不可用，无法实际执行 npm 脚本或本地 node_modules 可执行文件。
- 真实后端接口未接入，模板 schema 仍通过 Mock 服务层读写。
- P0 未实现富文本、文件/图片上传、JSON 编辑器和 LLM 交互组件。
- P0 未加入虚拟化、缓存、懒加载等性能优化手段。



## 2026-05-30 - 计划二：Owner 闭环 P0

### 已实现

- 完成计划二 P0 的文档拆分与 Owner 闭环边界梳理，明确 `P0 / P1` 分层。
- 新增 Owner 业务类型：
  - 任务、任务进度、任务草稿、发布校验。
  - 模板、导入预览、导入异常。
- 新增 `src/mocks/` 目录并按类型拆分 Mock 数据：
  - `tasks.mock.ts`
  - `templates.mock.ts`
  - `imports.mock.ts`
  - `ownerDashboard.mock.ts`
- 新增 Owner 服务层：
  - `ownerTaskService`
  - `ownerImportService`
  - `ownerDashboardService`
  - `ownerTemplateService`
- 新增 Owner 状态管理 store：
  - `ownerTaskStore`：任务列表、详情、进度、筛选、状态变更。
  - `ownerDraftStore`：草稿编辑、导入预览、步骤、未保存状态、发布校验。
  - `ownerDashboardStore`：仪表盘统计、重点任务、最近任务。
- 新增 Owner P0 页面：
  - 仪表盘页：任务总览、重点任务进度、最近任务。
  - 任务管理页：搜索、筛选、列表、进度、状态操作入口。
  - 任务创建/编辑页：基础信息、模板选择、导入预览、校验与发布。
- 接入 Owner 路由：
  - `/app/owner`
  - `/app/owner/tasks`
  - `/app/owner/tasks/new`
  - `/app/owner/tasks/:taskId/edit`
- 补充 Owner 页面样式，覆盖统计卡片、表格、表单、进度区和导入预览区。

### 当前约束

- 真实文件导入未实现，P0 仍以 Mock 导入预览驱动页面。
- `npm` 与 `nvm` 在当前环境中不可用，无法实际执行构建和 lint 验证。
- 目前任务发布仍依赖前端 Mock 服务层和状态流转，不接真实后端接口。



## 2026-05-31 - 计划三：动态表单核心修复与 P1 启动

### 已实现

- 修复并增强动态表单 P0 Designer 体验：
  - 新增模板创建入口，Owner 可在模板管理页创建草稿模板并进入 Designer。
  - 新增画布节点删除按钮，拖入画布后的物料可直接从画布删除。
  - 增加 dnd-kit `DragOverlay`，拖拽过程中有可见的跟随反馈。
  - 物料、画布、属性三个面板调整为等高工作台布局。
  - 画布变为独立滚动容器，拖入新物料后会滚动到新节点位置。
- 拆分过大的 `OwnerTemplateDesignerPage.tsx`：
  - 页面文件从约 700 行降到约 230 行。
  - 低层 Designer 组件拆到 `src/features/dynamic-form/components/designer/`。
  - 拖拽状态解析、字段选项解析、画布滚动等工具拆到 `src/features/dynamic-form/utils/`。
  - 页面文件保留路由参数、store 编排、拖拽流程和页面布局。
- 更新 `agent.md` 编码限制：
  - 手写源码文件接近 300 行时必须评估拆分。
  - 超过 500 行原则上不得继续堆叠。
  - 页面文件只保留编排逻辑，复杂 UI、工具、类型和配置应拆分到合适模块。
- 启动计划三 P1 动态表单增强：
  - 扩展动态表单 schema 类型，新增多条件显隐、联动规则和 P1 物料类型。
  - 新增 P1 物料类型：
    - 富文本
    - 文件/图片上传
    - JSON 编辑器
    - LLM 交互占位
  - 扩展物料注册中心，P1 物料可出现在物料面板并可放入分组或 Tab 面板。
  - 扩展 schema 到 Formily schema 的转换层：
    - 多条件显隐转换为 Formily reactions。
    - 条件必填转换为 required reaction。
    - 条件禁用转换为 disabled reaction。
    - 联动选项初步转换为 options reaction。
  - 新增 Renderer P1 组件：
    - `RichTextEditor`
    - `FileUploadField`
    - `JsonEditorField`
    - `LlmPromptBlock`
  - `DynamicFormRenderer` 支持 `readOnly` 模式和初始值回填。
  - 新增属性面板子组件：
    - `ConditionRuleEditor`
    - `LinkageRuleEditor`
  - 属性面板支持编辑多条件显隐、条件必填、条件禁用和选择类字段的联动选项。
  - 新增 `SchemaManagerPanel`，支持 schema JSON 预览、导入和复制导出。
  - `templateDesignerStore` 新增 `replaceSchema`，用于导入 JSON 后整体替换当前 schema。

### 当前约束

- P1 仍然只使用前端 Mock 服务层，不接真实后端接口。
- 文件/图片上传组件当前只做前端占位，不上传到后端。
- LLM 交互组件当前只做 UI 与 schema 占位，不调用真实模型。
- 联动选项当前实现为单条命中规则的基础版本，后续可扩展多 case 配置。
- 没有加入虚拟化、缓存、懒加载等性能优化手段。



## 2026-05-31 - 计划四：标注闭环 P0-P7

### 已实现

- 细化 `implementation-plans/04-labeler-workflow.md`，将计划四拆分为 P0-P7。
- 新增 Labeler 领域类型，覆盖任务广场任务、题目、草稿、提交、审核意见、提交统计和提交校验结果。
- 新增 Labeler Mock 数据：
  - 任务广场任务。
  - 标注题目。
  - 草稿。
  - 历史提交。
  - 打回审核意见。
- 新增 Labeler 服务层：
  - 查询任务广场。
  - 查询标签。
  - 领取任务。
  - 查询工作台任务和题目。
  - 查询、保存和持久化草稿。
  - 提交前校验。
  - 基于草稿提交任务。
  - 查询我的数据统计和提交记录。
- 新增 `labelingStore`，承载任务广场、工作台、草稿、提交、提交统计和历史记录状态。
- 接入 Labeler 路由：
  - `/app/labeler/market`
  - `/app/labeler/workbench/:taskId`
  - `/app/labeler/submissions`
- 实现任务广场页面：
  - 搜索。
  - 标签筛选。
  - 状态筛选。
  - 表格列表展示。
  - 任务领取。
  - 已领取、进行中、待修改任务进入工作台。
- 实现标注工作台基础流程：
  - 左侧题目导航。
  - 中间 schema 表单作答。
  - 右侧任务信息、保存区和提交区。
  - 上一题、下一题、点击跳题。
  - 手动保存草稿。
- 扩展 `DynamicFormRenderer`：
  - 支持表单值变化回调。
  - 支持提交按钮文案配置。
- 实现草稿自动保存与恢复：
  - 表单变化后延迟保存。
  - 草稿写入 `localStorage`。
  - 刷新后从本地草稿恢复。
  - 保存状态展示。
- 实现提交前校验和提交反馈：
  - 检查未保存草稿题目。
  - 检查必填字段。
  - 校验失败跳转到第一道错误题。
  - 提交成功后更新任务和题目状态。
- 实现打回修改流程：
  - 展示打回原因。
  - 展示上一轮审核意见。
  - 回填上一轮答案。
  - 展示上一轮答案快照。
  - 重新提交生成新提交记录，不覆盖旧记录。
- 实现我的数据页面：
  - 已提交、已通过、已打回、待修改、进行中统计。
  - 历史提交列表。
  - 状态筛选。
  - 关键词搜索。
  - 提交详情查看。
  - 待修改任务返回工作台。
- 补充 Labeler 页面样式，覆盖任务广场、工作台和我的数据。

### 当前约束

- 计划四仍使用前端 Mock 服务层，不接真实后端。
- 草稿持久化当前使用浏览器 `localStorage`，后续接后端时需要替换服务层实现。
- 提交前校验当前覆盖静态必填规则，复杂条件显隐下的跨题整单校验后续可增强。
- 我的数据页面当前展示 Mock/前端内存提交记录，不接真实审核结果接口。



## 2026-05-31 - 计划五：AI 前置审核与人工复核闭环 P0-P7

### 已实现

- 重写并细化 `implementation-plans/05-review-workflow.md`，将计划五调整为 AI 前置审核流程：
  - 标注员提交后先进入 AI 审核。
  - AI 结果分为通过、人工复核、打回。
  - 只有 AI 判定为人工复核的提交进入审核员队列。
- 新增 Review 领域类型：
  - `AiReviewDecision`
  - `AiReviewStatus`
  - `ManualReviewStatus`
  - `SubmissionReviewStatus`
  - `AiReviewResult`
  - `ManualReviewRecord`
  - `ReviewQueueItem`
  - `ReviewDetail`
  - `ReviewAuditEvent`
  - `BatchManualReviewResult`
- 新增 Review Mock 数据：
  - AI 通过提交。
  - AI 打回提交。
  - AI 人工复核提交。
  - AI 异常降级人工复核提交。
  - 人工复核历史。
  - 审计时间线。
- 新增 Review 服务层与 store：
  - 查询人工复核队列。
  - 查询审核详情。
  - 查询审核历史。
  - 模拟 AI 审核分流。
  - 提交单条人工复核动作。
  - 提交批量人工复核动作。
  - 同步人工审核结果回标注员侧状态。
- 扩展 Labeler 提交流程：
  - `submitAnswers` 和 `submitTaskDrafts` 提交后调用 AI 审核分流。
  - AI 通过直接更新为已通过。
  - AI 打回直接更新为待修改。
  - AI 人工复核保持待人工复核状态，并进入审核员队列。
- 拆分冗长的 `labelingService.ts`：
  - 新增 `labelingServiceHelpers.ts` 承载 clone、草稿持久化、提交校验和 AI 结果映射。
  - `labelingService.ts` 降为服务编排层，保留外部 API 不变。
- 接入 Reviewer 路由：
  - `/app/reviewer/queue`
  - `/app/reviewer/tasks/:reviewId`
  - `/app/reviewer/history`
- 实现人工复核队列页面：
  - 只展示 AI 人工复核项。
  - 支持关键词、风险等级、人工状态筛选。
  - 支持待复核项选择。
  - 支持批量人工通过。
  - 支持批量人工打回，打回必须填写统一原因。
  - 支持分页。
- 实现人工复核详情页：
  - 展示 AI 审核结果、风险等级、命中原因和异常降级原因。
  - 展示提交快照、原始数据和 schema 版本。
  - 使用 `DynamicFormRenderer` 的 `readOnly` 模式渲染提交答案。
  - 支持人工通过、人工打回、修订建议。
  - 人工打回和修订建议必须填写原因或建议。
  - 操作前二次确认。
  - 操作后写入人工审核记录和审计时间线。
  - 人工完成后提供下一条待复核入口。
- 实现审核历史页面：
  - 只读查看 AI 通过、AI 打回、AI 转人工和人工完成记录。
  - 支持关键词和 AI 结论筛选。
  - 支持进入详情回看。
- 扩展标注员“我的数据”页面：
  - 展示 AI 审核结果。
  - 展示审核来源，区分 AI 审核和人工审核。
  - 人工打回结果可回流为待修改状态。
- 补充 Reviewer 页面样式，覆盖队列、详情、只读表单、操作区和历史页。

### 当前约束

- 计划五仍使用前端 Mock 服务层，不接真实后端。
- AI 审核只通过 Mock 规则模拟，不调用真实模型。
- AI 异常当前按 Mock 兜底进入人工复核。
- 人工审核结果同步到标注员侧依赖前端内存回调，刷新页面后仍回到初始 Mock 数据。
- 批量人工复核当前使用统一原因，不支持逐条填写不同原因。
- 审核历史、审计时间线和提交快照都来自前端 Mock，不具备真实持久化能力。



## 2026-06-02 - Reviewer 真实接口接入与 AI 审核队列页

### 已实现

- 改造 Reviewer 审核详情页为真实接口驱动：
  - `GET /api/v1/reviewer/submissions/{submissionId}` 作为提交基础详情来源。
  - `GET /api/v1/submissions/{submissionId}/versions` 作为历史提交记录来源。
  - `POST /api/v1/reviewer/submissions/{submissionId}/approve` 提交通过。
  - `POST /api/v1/reviewer/submissions/{submissionId}/reject` 提交打回。
  - 页面调整为三列工作台：左侧题目状态，中间详情、历史版本、AI 预审和人工意见，右侧今日工作状态和审计日志替代展示。
  - 只保留“通过”和“打回”两个人工操作按钮，打回原因必填。
- 扩展 Review 真实接口类型：
  - `ReviewerSubmissionListItem`
  - `SubmissionVersion`
  - `ReviewActionResponse`
  - `BatchReviewResponse`
  - `AiReviewResultResponse`
  - `AiReviewResultPageResponse`
  - `AiReviewLogQuery`
  - `AiReviewQueueStatusFilter`
- 扩展 Review 服务层：
  - 人工审核队列、审核详情、通过、打回和批量操作改为调用真实 reviewer 接口。
  - 新增 AI 审核接口：
    - `GET /api/v1/tasks/ai-review-logs`
    - `GET /api/v1/tasks/{taskId}/ai-review-logs`
    - `GET /api/v1/submissions/{submissionId}/ai-review`
    - `POST /api/v1/submissions/{submissionId}/ai-review/retry`
- 扩展 `reviewStore`：
  - 增加历史版本状态。
  - 增加 AI 审核日志列表、选中记录、分页、加载状态和重试状态。
  - 增加今日审核数量的前端会话内统计。
- 新增 Reviewer AI 审核队列页：
  - 页面文件：`frontend/src/pages/reviewer/ReviewerAiReviewQueuePage.tsx`
  - 路由：`/app/reviewer/ai-reviews`
  - 导航入口：审核员侧边栏新增“AI审核队列”。
  - 左侧展示 AI 审核题目队列，支持按全部、待审核、已通过、已打回、转人工、失败切换。
  - 右侧展示选中记录的 AI 评语、评分维度、风险标记、处理日志、Prompt 快照和 LLM 原始响应。
  - 当记录包含 `submissionId` 且状态为失败或需人工时，支持触发 AI 审核重试。
- 补充 Reviewer 页面样式：
  - 新增详情页三列布局样式。
  - 新增 AI 审核队列左右分栏、队列选中态、评分行、代码块和响应式单列布局。

### 当前约束

- 当前 `GET /api/v1/reviewer/submissions/{submissionId}` 仅按已确认字段展示基础信息，不包含完整标注答案和审核模板。
- AI 审核队列中的“标注内容”和“审核模板”区域按用户决策显示空态。
- `GET /api/v1/tasks/ai-review-logs` 为新增约定接口，前端按与按任务查询相同的分页响应结构接入。
- AI 审核日志项如果不返回 `submissionId`，前端只能以 `agentRunId` 作为队列 key，无法触发单提交详情刷新和 AI 重试。
- Review 服务层仍保留部分 Mock 编排能力，用于标注员提交流程中的前端模拟 AI 分流。

### 已验证

- 已执行 `nvm list`，当前 Node 版本为 `22.14.0`。
- 已执行 `npm run build`，构建未通过。
- 构建失败点为既有类型问题，未出现在新增 AI 审核队列页：
  - `src/components/navigation/RoleBadge.tsx` 中角色 key 大小写与 `Role` 类型不匹配。
  - `src/features/dynamic-form/utils/designerDrag.ts` 中若干 `never` 类型访问。
  - `src/pages/owner/templates/OwnerTemplateDesignerPage.tsx` 中若干 `never` 类型访问。



## 2026-06-02 - Reviewer queue empty state and AI review status API update

### Implemented

- Fixed reviewer manual queue empty-data behavior:
  - When `queue` is empty, not loading, and has no error, the page keeps the header context and shows `暂无数据`.
  - Empty state hides batch actions, refresh action, filters, table, and batch reject modal entry.
  - `reviewService` now accepts both `[]` and `{ items: [] }` list responses for reviewer submissions to avoid `.map` failures on empty paged responses.
- Changed reviewer AI review queue list API:
  - New endpoint: `GET /api/v1/reviewer/ai-review-status`.
  - Frontend request path is `/v1/reviewer/ai-review-status` because the API base URL is `/api`.
- Added/extended review types:
  - Added `ReviewerAiReviewStatusItem`.
  - Extended `AiReviewResultResponse` with `taskTitle`, `submissionStatus`, and `submittedAt`.
- Updated `reviewService.listAllAiReviewLogs`:
  - Maps `aiDecision` to the existing `decision` field.
  - Applies status/decision filtering on the client.
  - Applies page/pageSize pagination on the client.
  - Still returns `{ items, page, pageSize, total }` so the store/page contract stays stable.
- Updated `reviewStore` AI review queue state handling:
  - Reloading, filtering, or paging clears stale selected records when they are no longer in the current list.
  - Detail and retry responses are merged with lightweight list fields so `taskTitle`, `submissionStatus`, and `submittedAt` are preserved.
- Updated `ReviewerAiReviewQueuePage` display:
  - Queue items show task title, submission ID, AI status, AI decision, average score, and submitted time/status.
  - Detail summary now includes task title, submission status, and submitted time.

### Current Constraints

- `GET /api/v1/reviewer/ai-review-status` is a lightweight list API and does not directly provide `dimensionScores`, `riskFlags`, `promptSnapshot`, or `rawResponse`.
- Selecting a record still depends on `GET /api/v1/submissions/{submissionId}/ai-review` to load detailed AI review fields.
- Status filtering and pagination are currently client-side because the new endpoint spec does not define query parameters.

### Verification

- Ran `nvm list`; current Node version is `22.14.0`.
- Ran `npm run build`; build still fails due to existing unrelated TypeScript errors outside the changed reviewer files:
  - `src/components/navigation/RoleBadge.tsx` role key mismatch.
  - `src/features/dynamic-form/utils/designerDrag.ts` `never` type property access.
  - `src/pages/owner/templates/OwnerTemplateDesignerPage.tsx` `never` type property access.
