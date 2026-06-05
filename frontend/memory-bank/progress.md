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

## 2026-06-01 - Owner 模板 Designer 画布体验与 CSS Module 迁移

### 已实现

- 调整 Owner 模板 Designer 页面顶部区域：
  - 为顶部 `ContentShell` 增加页面专属 CSS Module 样式。
  - 移除较长描述文案并压缩标题区域高度，使页面顶部更紧凑。
- 优化物料面板展示：
  - `MaterialPalette` 改为图标 + 标题 + 简短描述的紧凑物料项。
  - 物料列不再使用内部滚动条，桌面端通过紧凑布局展示物料。
  - 保持 dnd-kit 物料拖拽数据结构不变。
- 将模板 Designer 页面相关样式从 `src/index.css` 迁移到 `OwnerTemplateDesignerPage.module.css`：
  - 页面布局、三列面板、物料面板、画布、属性面板、拖拽浮层、Schema 面板等样式集中到页面 CSS Module。
  - `src/index.css` 不再保留 `.designer-*` 样式。
- 完善画布区域渲染器：
  - 新增 `CanvasFieldPreview`，按 `DynamicSchemaNode.type` 在画布中渲染接近实际预览的字段外观。
  - `CanvasNodeCard` 不再以 schema key/tag 为主展示节点，改为显示表单控件预览。
  - 画布与实际预览的主要差异保留为设计态操作条、拖拽按钮、删除按钮、选中态和 drop 区域分割线。
  - 容器类节点仍支持子节点拖放、排序和删除。
- 迁移运行态表单渲染器样式：
  - 新增 `DynamicFormRenderer.module.css`。
  - `DynamicFormRenderer.tsx` 和 `rendererFields.tsx` 改用 CSS Module class。
  - `src/index.css` 不再保留 `.dynamic-renderer*` 样式。

### 当前约束

- 未处理与本次需求无关的既有 TypeScript / lint 问题。
- 工作区中存在无关修改，例如 `src/app/navigation.tsx` 和若干未跟踪目录，本次没有回退或修改它们。
- 画布渲染器采用轻量 Ant Design 预览控件，不直接嵌入 Formily 运行态表单，以避免破坏节点级拖拽、选中和删除交互。

### 已验证

- 已执行 `nvm list`，当前 Node 为 `22.14.0`。
- 已执行 `npm exec vite build`，通过。
- 已执行 `npm run build`，失败于既有 TypeScript 错误：
  - `src/app/navigation.tsx` 未使用导入。
  - `RoleBadge.tsx` role key 类型不匹配。
  - `designerDrag.ts` 与 `OwnerTemplateDesignerPage.tsx` 中已有 `never` 类型收窄问题。
- 已执行 `npm run lint`，失败于既有 lint / React hooks 规则问题；新增 `CanvasFieldPreview` 未产生 lint 报错。
## 2026-06-02 - Owner 真实接口接入启动

### 已实现

- 接入 OWNER 模板库真实创建流程：
  - 新增 BE-B 模板创建/响应 DTO。
  - `ownerTemplateService` 支持 `VITE_SERVICE_MODE=real` 时调用 `POST /v1/owner/templates`。
  - 创建模板请求补齐 `schemaJson` 和 `changeNote`。
  - 前端创建弹窗继续使用 `description` 输入，并映射为后端 `changeNote`。
  - `schemaJson` 使用当前 Designer 可识别的 `DynamicFormSchema` 空 schema。
  - 模板列表/详情 real 模式读取 `GET /v1/owner/templates`，并映射 `currentVersion.versionId` 为 `currentVersionId`。
- 对接 OWNER 任务管理真实接口骨架：
  - 新增任务分页、创建、编辑、详情、生命周期、统计 DTO。
  - `ownerTaskService` 支持 mock/real 双模式。
  - real 模式任务列表调用分页版 `GET /v1/owner/tasks`。
  - real 模式创建任务调用 `POST /v1/tasks`。
  - real 模式编辑草稿调用 `PUT /v1/tasks/{taskId}`。
  - real 模式详情调用 `GET /v1/tasks/{taskId}`。
  - real 模式发布、暂停、恢复、结束分别调用 `/publish`、`/pause`、`/resume`、`/end`。
  - real 模式删除草稿调用 `DELETE /v1/tasks/{taskId}`。
  - real 模式任务进度改为调用 `GET /v1/tasks/{taskId}/statistics`。
- 调整任务字段模型：
  - `id` 映射 `taskId`。
  - `instruction` 映射 `instructionRichText`。
  - `deadline` 映射 `deadlineAt`。
  - `templateId` 调整为 `publishedTemplateVersionId`，使用模板当前版本 ID。
  - 新增 `quota`、`claimedCount`、`reviewLevelCount`。
  - AI 审核配置调整为 `prompt`、`model`、`rating`。
  - `reward` 使用奖励单价字符串。
  - `strategy` 固定为 `先到先得`、`配额分发`、`指派`。
  - 状态在服务层处理 `DRAFT/PUBLISHED/PAUSED/ENDED` 与前端小写状态映射。
- 调整 Owner 任务页面：
  - 任务列表支持分页。
  - 草稿任务增加删除入口。
  - 创建/编辑页补充任务配额、审核级别数、AI 审核配置字段。
  - 模板选择改为提交模板当前版本 ID。
  - 发布前保留前端基础校验。
- 接入真实文件上传接口：
  - 新增 `FileUploadResponse`。
  - `ownerImportService.uploadDatasetFile(file)` real 模式调用 `POST /v1/files/upload`。
  - 使用 `FormData`，字段名为 `file`。
  - 上传成功后将 `fileId` 写入草稿 `datasetFileId`。
  - 创建任务时 `datasetFileId` 会随 `POST /v1/tasks` 提交。
  - 创建页上传成功后展示文件名、大小、类型和文件 ID。
  - mock 模式继续保留当前 mock 导入预览。

### 当前约束

- 数据集上传接口只返回文件元数据，不返回字段映射、样本预览或异常行；real 模式下暂不展示真实预览。
- `GET /v1/tasks/{taskId}` 文档未返回 `datasetFileId`，刷新已保存草稿后前端无法从详情恢复上传文件元数据。
- 任务详情接口未返回模板名称，当前 real 映射只能展示模板版本 ID 或占位名称。
- `src/app/navigation.tsx` 存在非本次任务产生的未提交改动，未处理也未回退。

### 已验证

- 已执行针对本次任务相关文件的 `npx eslint`，通过。
- 已执行 `git diff --check`，未发现空白错误，仅有 Git 行尾转换提示。
- 已执行 `npm run build`，仍失败于既有问题：
  - `src/app/navigation.tsx` 未使用导入和角色 key 不匹配。
  - `src/components/navigation/RoleBadge.tsx` 角色 key 不匹配。
  - dynamic-form Designer 相关 `never` 推断错误。
  - 本次新增的模板、任务和上传接口改动未再产生新的 TypeScript 错误。

## 2026-06-02 - Owner 创建任务 AI 字段与模板列表对接

### 已实现

- 调整创建任务 AI 审核配置字段，前端草稿、类型和请求组装统一使用后端新字段：
  - `aiProviderId`
  - `aiModelName`
  - `aiPrompt`
  - `aiScoringDimensions`
  - `aiPassThreshold`
  - `aiManualReviewThreshold`
- 创建任务页补充 AI 审核配置能力：
  - 大模型下拉继续调用 `GET /v1/llm-providers`，展示 `defaultModel`。
  - 选择模型后保存 provider ID 和模型名。
  - 评分维度改为可添加的标签式输入，并以字符串数组保存。
  - 新增“通过阈值”和“人工复核阈值”两个 0-100 数值输入。
- 更新创建/编辑任务请求：
  - `POST /v1/tasks` 和 `PUT /v1/tasks/{taskId}` 不再提交旧的 `providerId`、`model`、`prompt`、`rating`。
  - 请求 payload 改为提交后端要求的 AI 字段。
  - 模板字段继续使用 `publishedTemplateVersionId`，值来自模板当前版本 ID。
- 更新发布前校验：
  - 校验 AI 模型、Prompt、评分维度和两个阈值。
  - 校验 Prompt、模型名、评分维度长度，以及阈值范围。
- 调整 OWNER 模板列表映射：
  - `GET /v1/owner/templates` 继续通过现有 request 层自动解包 `ApiResponse.data`。
  - `templateId` 映射为模板 ID。
  - `currentVersion.versionId` 映射为 `currentVersionId`，用于创建任务时提交版本 ID。
  - 支持 `PUBLISHED_SNAPSHOT -> ready`，未知或空状态按 `draft` 处理。
- 同步更新 mock 任务数据中的 AI 审核配置结构，保持 mock mode 可用。

### 当前约束

- 工作区仍包含此前任务产生的 owner 上传、文件解析、CSS 迁移等未提交改动，本次没有回滚。
- 完整 TypeScript build 仍被无关既有错误阻塞：
  - `src/app/navigation.tsx`
  - `src/components/navigation/RoleBadge.tsx`
  - dynamic-form Designer 相关 `never` 推断错误。
- `npm run lint` 仍被无关既有 lint 错误阻塞：
  - `src/app/navigation.tsx`
  - `LinkageRuleEditor.tsx`
  - `SchemaManagerPanel.tsx`

### 已验证

- 已执行 `nvm list`，当前 Node 为 `22.14.0`。
- 已执行 `npm exec vite build`，通过，仅有 chunk size warning。
- 已执行 `npm run lint`，失败于无关既有 lint 错误。
- 已执行 `npm run build`，失败于无关既有 TypeScript 错误。
## 2026-06-05 - Owner 真实接口与页面交互补充

### 已实现

- 接入 Admin 审核分配查询业务：
  - 新增 `ADMIN` 角色入口、导航与 `/app/admin` 路由。
  - 新增 Admin 审核分配页面，展示可分配任务、可分配审核员和审核员进度。
  - 新增 `adminReviewAssignmentService`，对接：
    - `GET /v1/admin/review/tasks/assignable`
    - `GET /v1/admin/review/reviewers/assignable`
    - `GET /v1/admin/review/reviewers/progress`
- 完善 Owner 任务详情题目渲染：
  - 接入 `GET /v1/tasks/{taskId}/dataset/items` 分页查询题目。
  - 接入 `POST /v1/tasks/{taskId}/dataset/items/batch-append-json`，支持在任务详情中手动追加题目。
  - 题目展示改为 Ant Design Table，动态字段列来自 `itemJson` keys。
  - 点击加号可在表格中新增一条可编辑空白行，确认后组装 `externalId`、`itemJson`、`metadataJson` 调用接口。
- 调整 Owner 创建任务请求体：
  - `POST /v1/tasks` 请求体改为新版接口结构。
  - 新增并提交 `overlapCount`、`maxClaimsPerLabeler`、`aiReviewStrategy`。
  - 奖励字段从旧 `reward` 字符串改为 `rewardRule` 对象：
    - `rewardMode`
    - `unitReward`
    - `rewardCurrency`
    - `rewardVisible`
  - 分发策略从前端中文值映射为后端编码：
    - `先到先得 -> FCFS`
    - `配额分发 -> QUOTA`
    - `指派 -> ASSIGN`
  - `aiReviewConfigId` 按当前决策暂不传，继续使用内联 AI 字段。
  - 创建任务页面新增一致性次数、每人最大领取数、AI 审核策略、奖励模式、奖励币种、奖励展示开关。
- 优化 Owner 模板详情 Designer 页面：
  - 模板详情页 `<main>` 增加页面级 CSS Module class。
  - 页面高度锁定在 app 内容区内，外层 `overflow: hidden`，避免浏览器页面级滚动条。
  - Designer 主体区改为 flex 剩余空间布局，移除原先撑开页面的固定 `min-height`。
  - 保留画布和属性面板的局部滚动。
- 维护 mock 模式：
  - mock 任务补齐新增创建任务字段默认值。
  - mock 题目追加逻辑同步更新本地 mock 数据集样本。

### 当前约束

- `aiReviewConfigId` 暂无前端来源，本阶段不传。
- `aiReviewStrategy` 当前只提供接口示例中的 `LIGHTWEIGHT`。
- `rewardMode` 当前只提供 `APPROVED_ITEM`，`rewardCurrency` 当前只提供 `POINT`。
- 完整 TypeScript build 仍被既有 dynamic-form Designer `never` 类型错误阻塞。
- Vite dev server 曾因本地 `spawn EPERM` 无法在沙箱内启动，未强制继续启动。

### 已验证

- 已执行 `nvm list`，当前 Node 为 `22.14.0`。
- 已多次执行 `npm exec vite build`，通过，仅有 chunk size warning。
- 已执行 `npm run build`，失败于既有 TypeScript 错误：
  - `src/features/dynamic-form/utils/designerDrag.ts` 的 `never` 属性访问。
  - `src/pages/owner/templates/OwnerTemplateDesignerPage.tsx` 的 `never` 属性访问。
