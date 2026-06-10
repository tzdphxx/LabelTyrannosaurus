# LabelHub 前端基础技术文档

> 本文档基于当前仓库 `frontend/package.json`、`frontend/src` 目录、前端分工文档和提交材料编写。本文重点说明浏览器端工程设计、模块边界、状态管理、服务对接、动态表单、启动构建和测试口径；后端数据模型、事务和权限裁决以后端基础技术文档为准。

## 1. 项目概述

LabelHub 前端是基于 React、TypeScript 和 Vite 构建的单页应用，负责承载 Admin、Owner、Labeler、Reviewer 四类角色的浏览器工作台。前端围绕「任务创建 -> 模板设计 -> 标注作答 -> AI 预审展示 -> 人工审核 -> 导出反馈」主流程组织页面、导航、状态和服务调用。

前端的核心职责是：

- 提供多角色登录后入口、角色化导航和路由保护。
- 组织 Owner 任务管理、模板设计、数据导入预览、AI 配置和导出操作。
- 组织 Labeler 任务市场、领取、作答、草稿保存、提交和打回返修。
- 组织 Reviewer 领取待审、审核队列、审核详情、批量处理和 AI 预审队列。
- 组织 Admin 平台看板、Reviewer 创建、审核分配查询和 LLM Provider 管理。
- 实现动态表单 Designer / Renderer，使同一份 schema 能用于设计、作答和审核只读回看。
- 封装 mock / real 两种服务模式，支持本地演示和真实后端联调。

前端不承担最终业务裁决。用户权限、任务状态机、模板版本校验、答案合法性、AI 调用、人工审核结论、导出文件生成、奖励结算和审计落库均以后端接口和服务端状态为准。

## 2. 前端技术栈

| 技术 | 当前用途 |
| --- | --- |
| React 19 | 构建单页应用、角色页面和动态表单运行态组件 |
| TypeScript | 约束页面 props、服务响应、业务 DTO、动态表单 schema 和 store 状态 |
| Vite | 本地开发服务器、前端构建和资源打包 |
| React Router | 定义登录页、角色工作台和业务子路由 |
| Ant Design | 页面布局、表单、表格、抽屉、弹窗、消息提示和基础 UI |
| `@ant-design/plots` | 看板趋势、分布和统计图表展示 |
| Zustand | 管理登录态、页面状态、任务草稿、审核队列和模板设计器状态 |
| Formily | 将动态模板 schema 渲染为可编辑或只读表单 |
| dnd-kit | 支撑模板设计器中的拖拽、排序和嵌套交互 |
| axios | 统一 HTTP Client，接入后端 REST API |
| xlsx | Owner 数据集文件的本地解析和预览 |
| Vitest / Testing Library | 单元测试、组件测试和页面交互测试 |

## 3. 工程目录结构

前端源码位于 `frontend/src`，核心目录如下：

```text
frontend/src/
  app/          # 路由、导航、布局和路由守卫
  components/   # 跨页面复用组件，如导航、页面容器、状态占位
  features/     # 领域型前端能力，当前核心是 dynamic-form
  hooks/        # 复用 hooks，如角色导航
  mocks/        # mock 模式下的演示数据
  pages/        # Admin / Owner / Labeler / Reviewer / Auth 页面
  services/     # HTTP Client、业务 service、mock/real 服务封装
  stores/       # Zustand stores
  test/         # Vitest / Testing Library 测试初始化
  types/        # 业务类型、响应类型和前端 DTO
  utils/        # 角色、任务、看板、标注等纯工具函数
```

目录边界遵循以下原则：

- `pages` 负责页面编排、布局组合和用户交互入口。
- `services` 负责数据访问、接口映射、mock/real 分支和错误归一。
- `stores` 负责跨组件状态、加载状态、提交状态和局部业务流程状态。
- `features/dynamic-form` 承载动态表单的物料、Designer 工具、schema 转换和 Renderer。
- `components` 只放跨业务页面复用的 UI 组件，不承载业务状态机。
- `types` 统一声明前端消费的数据形状，避免页面直接依赖后端原始响应结构。

## 4. 路由、认证与权限

前端通过 `frontend/src/app/router.tsx` 组织公开入口和登录后业务入口。主要路由包括：

| 路由 | 说明 |
| --- | --- |
| `/login` | 登录与 Owner / Labeler 注册 |
| `/app/admin` | Admin 工作台 |
| `/app/admin/review-assignment` | 审核分配查询 |
| `/app/admin/llm-providers` | LLM Provider 管理 |
| `/app/owner` | Owner 工作台 |
| `/app/owner/tasks` | 任务管理 |
| `/app/owner/tasks/new` | 新建任务 |
| `/app/owner/tasks/:taskId/edit` | 编辑任务 |
| `/app/owner/templates` | 模板管理 |
| `/app/owner/templates/:templateId/designer` | 模板设计器 |
| `/app/labeler` | Labeler 工作台 |
| `/app/labeler/market` | 任务市场 |
| `/app/labeler/workbench/:taskId` | 作答工作台 |
| `/app/labeler/submissions` | 我的领取 / 提交 |
| `/app/reviewer` | Reviewer 工作台 |
| `/app/reviewer/claim` | 领取待审 |
| `/app/reviewer/queue` | 审核队列 |
| `/app/reviewer/ai-reviews` | AI 预审队列 |
| `/app/reviewer/tasks/:taskId` | 任务审核详情 |

认证状态由 `authStore` 管理。登录成功后，前端保存 access token、refresh token、token version、用户信息和角色信息。业务请求通过统一 HTTP Client 自动注入 `Authorization: Bearer <token>`。当普通业务接口返回 401 时，前端尝试使用 refresh token 刷新并重放原请求；刷新失败则清理本地登录态并回到登录流程。

前端路由守卫用于提升用户体验和减少误操作：

- 未登录访问 `/app` 下页面时跳转到 `/login`。
- 已登录访问 `/login` 时跳转到当前角色首页。
- Admin、Owner、Labeler、Reviewer 只能访问各自角色路径。
- 未匹配或未开放路径返回安全入口或展示占位状态。

这些守卫不能替代后端 RBAC。真实权限、数据归属和接口可访问性仍由后端 token、角色和服务层校验决定。

## 5. 页面模块

### 5.1 Admin

Admin 页面提供平台运维和全局配置入口：

- 平台看板：查看任务、用户、提交、审核、奖励和趋势概览。
- Reviewer 创建：创建审核员账号，补齐审核角色入口。
- 审核分配查询：查询可分配任务、可分配 Reviewer 和 Reviewer 进度。
- LLM Provider 管理：新增、编辑、启用、停用和测试 Provider，集中维护模型接入配置。

Admin 页面只展示和触发平台管理能力，不在前端保存 Provider 密钥明文，也不绕过后端执行权限判断。

### 5.2 Owner

Owner 页面围绕任务生产和任务配置展开：

- 工作台：查看任务进度、质量摘要和待处理入口。
- 任务列表：筛选任务、执行发布、暂停、恢复、结束、删除草稿和直接导出。
- 任务编辑：维护基础信息、奖励规则、分发策略、模板版本、数据集文件和 AI 审核配置。
- 数据集预览：前端解析 JSON、JSONL、XLSX 文件，提供字段映射、样例行、错误行和发布前提示。
- 模板管理：维护模板列表并进入模板设计器。
- 模板设计器：通过物料面板、画布、属性面板和 schema 管理构建动态标注表单。

Owner 侧前端校验用于阻止明显不完整的任务进入发布流程，真实状态迁移、模板版本冻结、数据集入库和导出文件生成以后端为准。

### 5.3 Labeler

Labeler 页面围绕领取、作答、提交和返修展开：

- 工作台：展示个人领取、提交、审核和奖励概览。
- 任务市场：查询可参与任务，按关键词、标签和状态筛选，并领取任务 item。
- 作答工作台：加载任务材料、题目列表、模板 schema、草稿和审核历史。
- 草稿保存：支持自动保存和手动保存，真实模式下携带 clientVersion 与后端草稿接口协作。
- 正式提交：提交前执行前端必填和表单校验，提交后进入 AI 预审和人工审核链路。
- 我的领取 / 提交：查看草稿状态、提交状态、AI 结果、人工审核状态和打回原因。

Labeler 端只负责收集和展示答案。答案是否符合模板契约、提交状态是否可推进、打回后是否允许重提，均以后端状态和 schema 校验为准。

### 5.4 Reviewer

Reviewer 页面围绕 AI 预审后的人工复核展开：

- 工作台：查看个人审核概览和关键入口。
- 领取待审：按任务领取待处理提交，形成 Reviewer 与任务范围的处理关系。
- 审核队列：查看已领取范围内的待审提交，支持筛选和批量通过 / 打回。
- 审核详情：查看题目、答案、AI 建议、版本历史和审核记录，并执行单条通过 / 打回。
- AI 预审队列：按 pending、passed、rejected、manual、failed 等状态查看 AI 审核记录，支持失败或转人工记录的重试入口。

Reviewer 页面只展示前端映射后的成功、失败和详情信息。批量操作事务、部分失败原因、审核权限和状态推进以后端为准。

## 6. 动态表单设计

动态表单是前端核心能力，主要位于 `frontend/src/features/dynamic-form`。

### 6.1 Designer

Designer 面向 Owner，用于创建和维护模板 schema。它由以下部分组成：

- 物料注册表：声明输入、选择、展示、文件、JSON、LLM 辅助和布局容器等组件。
- 物料面板：提供可拖入画布的组件入口。
- 设计画布：支持组件添加、排序、嵌套和删除。
- 属性面板：维护标题、字段 key、placeholder、options、required、条件显示、联动规则和 LLM 属性。
- schema 管理：支持导入、保存、回显和转换。

设计器产物必须是可序列化 schema，不保存函数、组件实例或拖拽运行态对象。这样后端可以保存版本，Labeler 可以用同一份 schema 作答，Reviewer 可以基于提交快照只读回看。

### 6.2 Renderer

Renderer 面向 Labeler 作答和 Reviewer 审核回看。前端将模板 schema 转换为 Formily 可渲染结构：

- input / textarea / radio / checkbox / select 渲染为对应 Ant Design 表单控件。
- showItem 渲染为题目原始字段展示。
- richText、fileUpload、jsonEditor、llmPrompt 渲染为定制字段。
- group、tabs、tabPane 渲染为布局容器。
- required、enum、长度限制、条件显示和选项联动映射为 Formily 校验或 reaction。
- readOnly 模式下用于审核详情或历史版本回看。

前端校验用于即时反馈，正式答案校验以后端 `SchemaValidationService` 和 `AnswerSchemaValidator` 为准。

### 6.3 LLM Trigger

`llmPrompt` 组件用于字段级 AI 辅助。Labeler 在作答时触发 LLM 后，前端只提交 assignment、当前答案和用户补充指令等上下文入口；模型选择、Prompt 构造、权限、限流、执行记录和结果生成由后端完成。

前端负责：

- 判断是否存在 assignmentId，缺少上下文时拒绝触发。
- 发起 trigger run。
- 轮询运行状态。
- 展示建议、patch、置信度、风险提示、耗时和 traceId。
- 用户确认后再把建议应用到目标字段。

## 7. 服务层与接口对接

前端通过 `frontend/src/services` 封装业务服务，避免页面直接散落 axios 细节。

### 7.1 HTTP Client

统一 HTTP Client 负责：

- 设置 API base URL，默认使用 `/api`。
- 注入 Authorization header。
- 解析后端统一响应包。
- 将网络错误、HTTP 错误和业务错误归一为前端可展示的错误。
- 在 401 时尝试 refresh token 并重放请求。

### 7.2 mock / real 模式

前端支持两种服务模式：

| 模式 | 用途 |
| --- | --- |
| `VITE_SERVICE_MODE=real` | 调用真实后端接口，适合联调、演示和部署 |
| `VITE_SERVICE_MODE=mock` 或未配置 | 使用本地 mock 数据，适合页面开发、离线演示和交互自测 |

mock 模式主要覆盖 Owner、Labeler 和部分 Admin Provider 场景。Review 页面优先按真实 Reviewer API 对接，不作为纯 mock 页面设计。真实模式下，部分页面仍可能保留 mock fallback 作为演示兜底，但最终交付口径以后端接口为准。

### 7.3 主要服务范围

| 服务范围 | 主要能力 |
| --- | --- |
| Auth | 登录、注册、刷新 token、退出 |
| Admin | 看板、Reviewer 创建、审核分配查询、LLM Provider 管理 |
| Owner | 任务 CRUD、生命周期、统计、Labeler 查询、模板、文件上传、数据项查询 / 追加、直接导出、Provider 查询 |
| Labeler | 任务市场、领取、claims、答案模板、草稿、提交、题目历史 |
| Reviewer | 审核任务、领取待审、审核队列、审核详情、单条 / 批量审核、AI 审核状态、AI 重试、提交版本、题目历史 |
| LLM | assignment 字段级 trigger、trigger run 查询 |

前端 service 的职责是完成请求参数组织、响应映射、错误展示所需信息整理和 mock/real 分流，不在前端复制后端业务规则。

## 8. 状态管理

前端使用 Zustand 管理跨页面和跨组件状态。主要 store 包括：

| Store | 职责 |
| --- | --- |
| `authStore` | 用户、角色、token、登录、注册、退出和本地恢复 |
| `navigationStore` / `pageUiStore` | 导航、页面 UI 状态和通用反馈 |
| `adminDashboardStore` | Admin 看板数据和加载状态 |
| `ownerDashboardStore` / `ownerTaskStore` / `ownerDraftStore` | Owner 看板、任务列表、任务编辑草稿和发布校验 |
| `templateDesignerStore` | 设计器 schema、选中节点、保存状态和未保存标记 |
| `labelerDashboardStore` / `labelingStore` | Labeler 看板、市场任务、作答草稿、提交状态和历史反馈 |
| `reviewerDashboardStore` / `reviewStore` | Reviewer 看板、领取任务、审核队列、审核详情、AI 审核记录和批量操作状态 |

Store 中的状态用于页面编排和交互反馈，不等于最终业务事实。真实模式下，状态刷新以后端响应为准；mock 模式下，部分状态会保存在内存或 localStorage，用于演示流程连续性。

## 9. 构建、启动与环境变量

前端目录为 `frontend/`。常用命令：

```bash
cd frontend
npm install
npm run dev
npm run build
npm run preview
```

脚本说明：

| 命令 | 说明 |
| --- | --- |
| `npm run dev` | 启动 Vite 本地开发服务 |
| `npm run build` | 执行 TypeScript 构建检查并生成生产包 |
| `npm run preview` | 本地预览生产构建结果 |
| `npm run lint` | 执行 ESLint 检查 |
| `npm run test` | 执行 Vitest 测试 |
| `npm run test:coverage` | 执行测试并生成覆盖率 |
| `npm run test:watch` | 以 watch 模式运行测试 |

关键环境变量：

| 变量 | 说明 |
| --- | --- |
| `VITE_SERVICE_MODE` | 服务模式，`real` 调真实接口，`mock` 或未配置使用 mock |
| `VITE_API_BASE_URL` | 后端 API 基地址，未配置时默认 `/api` |

Docker Compose 场景下，前端镜像由根目录 `compose.yml` 编排启动，并通过 Nginx 将 `/api/` 反向代理到 Compose 网络中的后端服务。评审或演示人员优先使用根目录 README 中的 Docker Compose 启动方式；前端单独开发时使用上述 npm 命令。

## 10. 测试与质量保障

前端测试使用 Vitest、Testing Library、jest-dom 和 jsdom。当前测试主要覆盖以下方向：

- 动态表单物料注册、Designer 工具、schema 树、后端 schema 映射和 Formily 转换。
- DynamicFormRenderer 的字段渲染、只读状态和提交行为。
- Labeler 作答工作台、草稿保存、提交和状态刷新。
- Reviewer 审核详情、队列映射和 AI 审核展示。
- Labeling store、review mapper 等关键状态和数据映射逻辑。
- Admin Provider 页面模块的样式或交互约束测试。

建议验证命令：

```bash
cd frontend
npm run lint
npm run test
npm run build
```

验收时同时关注人工流程：

- Admin、Owner、Labeler、Reviewer 四类账号能进入各自首页。
- 角色侧边栏、顶部栏、退出和路由守卫符合角色边界。
- Owner 能建任务、配模板、导入数据、配置 AI 和导出。
- Labeler 能领取、作答、保存草稿、提交和查看打回原因。
- Reviewer 能领取、筛选、查看详情、通过、打回、批量处理和查看 AI 队列。
- 所有关键页面在 loading、empty、error、disabled 和 submitting 状态下有明确反馈。

## 11. 安全与边界

前端安全设计遵循“体验保护在前端，可信裁决在后端”的原则：

- 前端不保存 Provider API Key 明文，不在页面展示后端脱敏字段以外的敏感信息。
- 前端不在浏览器中直接调用模型，不自行生成最终 AI 审核结论。
- 前端不信任本地角色状态作为最终权限依据，所有敏感接口以后端 RBAC 和数据归属校验为准。
- 前端校验不能替代后端 schema 校验，用户绕过前端也必须被后端拦截。
- 前端不决定任务状态机、领取并发、审核事务、奖励结算、导出范围和审计落库。
- token 存储和 refresh 只用于浏览器会话体验；token 是否有效以后端鉴权结果为准。

## 12. 前端设计总结

LabelHub 前端围绕多角色工作台和动态表单构建。工程上，它将页面、状态、服务、类型和动态表单能力拆分为清晰边界：页面负责交互编排，store 负责状态承载，service 负责接口和 mock/real 分流，dynamic-form 负责 schema 驱动的设计和运行态渲染。

该设计的价值在于：Owner 能快速配置任务和模板，Labeler 能按模板稳定作答，Reviewer 能基于 AI 结果和历史版本完成复核，Admin 能维护平台入口和 Provider 能力。前端保持对用户体验、流程反馈和可视化表达负责，同时把权限、状态、校验、AI、导出和审计等可信业务事实交给后端完成。
