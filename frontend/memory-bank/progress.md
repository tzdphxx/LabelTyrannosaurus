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

### 未验证

- 按要求未运行 `npm run build`。
- 按要求未运行 `npm run lint`。

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

### 未验证

- `npm run build` 因当前 shell 找不到 `npm` 未能执行。
- `npm run lint` 因当前 shell 找不到 `npm` 未能执行。
- `node_modules/.bin/tsc.cmd -b` 因当前 shell 找不到 `node` 未能执行。
- `node_modules/.bin/eslint.cmd .` 因当前 shell 找不到 `node` 未能执行。
- 已执行 `git diff --check`，未发现空白错误，仅有 Git 行尾转换提示。

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

### 未验证

- 按要求未运行 `npm run build`。
- 按要求未运行 `npm run lint`。

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

### 未验证

- 本阶段验证流程被中断，尚未完成 `npm run build` 和 `npm run lint`。
- 当前环境此前多次出现 `nvm`、`npm`、`node` 不可用，后续需要在可用 Node 环境中重新执行验证。

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

### 未验证

- 按用户要求，本阶段未运行 `npm run build`。
- 按用户要求，本阶段未运行 `npm run lint`。

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

### 未验证

- 本阶段未运行 `npm run build`。
- 本阶段未运行 `npm run lint`。
- 此前执行 `nvm list` 时当前 shell 找不到 `nvm`。
- 此前尝试 `npm run build` 时被用户中断，后续按用户意图未继续执行 npm 命令。
- 已执行 `git diff --check -- .\src`，未发现空白错误，仅有 Git 行尾转换提示。

## 2026-06-01 - 标注工作台题目级提交与状态流程调整

### 已实现

- 扩展标注题目状态，题目导航支持展示：待标注、进行中、已打回、已提交、草稿。
- 新增题目状态文案与颜色映射，工作台左侧题目导航和右侧状态展示复用统一映射。
- 将工作台提交逻辑从任务级提交调整为题目级提交：
  - 新增题目级草稿校验。
  - 新增 `submitQuestionDraft` 服务层方法。
  - 新增 `labelingStore.submitQuestionDraft` action。
  - 当前题提交成功后只更新当前题为已提交，不再把整任务所有题目置为已提交。
- 调整标注工作台布局：
  - 上一题/下一题移动到中间表单区域左下方。
  - 保存草稿/提交当前题移动到中间表单区域右下方。
  - 右侧栏改为“题目状态与流程”，展示当前题状态、回填来源、保存状态、最近保存和本题流程时间线。
- Mock 数据中将返修任务题目标记为已打回，用于覆盖已打回状态展示。
- 优化工作台页面内部表单值处理，避免在 effect 中同步重置 state 导致 ESLint `react-hooks/set-state-in-effect` 命中。

### 当前约束

- 本次仍基于前端 Mock 服务层实现，不接真实后端接口。
- 题目级提交生成的提交记录仍复用现有 `LabelingSubmission` 结构，后续接后端时可进一步拆分为题目级提交实体。
- “进行中”当前作为页面临时编辑态展示：当前题有未保存修改时显示为进行中，保存后显示为草稿。
- 全量构建和 lint 仍受既有问题阻塞，阻塞点不在本次工作改动文件中。

### 已验证

- 已执行 `nvm list`，当前可用 Node 版本为 `22.14.0`。
- 已执行 `npm ci` 安装 lockfile 依赖。
- 已执行本次修改文件的局部 ESLint： 
  - `npx eslint src\pages\labeler\LabelerWorkbenchPage.tsx src\stores\labelingStore.ts src\services\labeler\labelingService.ts src\services\labeler\labelingServiceHelpers.ts src\types\labeling.ts src\utils\labeling.ts src\mocks\labeling.mock.ts`
  - 结果通过。
- 已执行 `git diff --check -- src\pages\labeler\LabelerWorkbenchPage.tsx src\stores\labelingStore.ts src\services\labeler\labelingService.ts src\services\labeler\labelingServiceHelpers.ts src\types\labeling.ts src\utils\labeling.ts src\mocks\labeling.mock.ts src\index.css`，未发现空白错误，仅有 Git LF/CRLF 提示。
- 已执行 `npm run build`，失败于既有 TypeScript 错误：
  - `src/app/navigation.tsx`
  - `src/components/navigation/RoleBadge.tsx`
  - `src/features/dynamic-form/utils/designerDrag.ts`
  - `src/pages/owner/templates/OwnerTemplateDesignerPage.tsx`
- 已执行全量 `npm run lint`，失败于既有 lint 错误：
  - `src/app/navigation.tsx`
  - `src/features/dynamic-form/components/DynamicFormRenderer.tsx`
  - `src/features/dynamic-form/components/designer/LinkageRuleEditor.tsx`
  - `src/features/dynamic-form/components/designer/SchemaManagerPanel.tsx`
  - `src/features/dynamic-form/materialRegistry.ts`
  - `src/pages/owner/templates/OwnerTemplatesPage.ts`
## 2026-06-02 - 标注员市场领取真实服务接入

### 已实现

- 阅读并对齐标注市场与领取接口文档，按文档契约接入真实服务。
- 新增 `src/services/labeler/labelingRealService.ts`：
  - 接入 `GET /v1/market/tasks` 查询标注市场任务。
  - 接入 `POST /v1/tasks/{taskId}/assignments/claim` 领取 assignment。
  - 接入 `GET /v1/assignments/{assignmentId}` 加载 assignment 详情、题目材料、schema 和草稿。
  - 接入 `GET /v1/labeler/assignments` 查询当前标注员已领取 assignment，用于市场页找回已领取任务。
  - 接入 assignment 草稿读取、保存和题目级提交接口。
- 将原 mock 标注服务改名为 `mockLabelingService`，并在 `src/services/labeler/index.ts` 按 `VITE_SERVICE_MODE` 切换：
  - `mock` 模式继续使用原 mock 行为。
  - `real` 模式使用真实接口服务。
- 保持市场页和工作台现有调用方式不变：
  - 领取后仍跳转 `/app/labeler/workbench/:taskId`。
  - 真实服务内部维护 `taskId -> assignmentId` 映射。
- 完成字段降级策略：
  - 进度使用 `quota - remainingQuota` 和 `quota` 计算。
  - `instruction` 使用 `description` 兜底。
  - `templateName`、`templateId`、审核详情等接口缺失字段使用空值或占位展示。
  - assignment 题目标题使用 `题目 #datasetItemId` 生成。
  - `itemList` / `itemJson` 转换为工作台材料区可展示的键值结构。
- 增加真实状态到前端状态的映射：
  - `CLAIMED`、`DRAFTING`、`RETURNED`、`SUBMITTED`、`APPROVED`、`CANCELLED` 映射到现有任务/题目状态。

### 当前约束

- 本次不调整工作台路由，仍以 `taskId` 作为 URL 参数。
- 本次不实现取消领取接口。
- 真实审核历史、上一轮答案、AI/人工审核详情接口当前仍未接入，相关字段保留为空或占位。
- 如果 `GET /v1/labeler/assignments` 暂不可用，市场列表仍可通过 `GET /v1/market/tasks` 展示可领取任务，但已领取任务找回会受限。

### 已验证

- 已执行 `nvm list`，当前 Node 版本为 `22.14.0`；项目无 `.nvmrc`，`package.json` 未声明 `engines.node`。
- 已执行本次相关文件的局部 ESLint：
  - `npx eslint src\services\labeler\labelingService.ts src\services\labeler\labelingRealService.ts src\services\labeler\index.ts src\stores\labelingStore.ts src\pages\labeler\LabelerMarketPage.tsx src\pages\labeler\LabelerWorkbenchPage.tsx`
  - 结果通过。
- 已执行相关文件 `git diff --check`，未发现空白错误，仅有 Git LF/CRLF 提示。
- 已执行 `npm run build`，失败于既有无关 TypeScript 错误：
  - `src/app/navigation.tsx`
  - `src/components/navigation/RoleBadge.tsx`
  - `src/features/dynamic-form/utils/designerDrag.ts`
  - `src/pages/owner/templates/OwnerTemplateDesignerPage.tsx`
