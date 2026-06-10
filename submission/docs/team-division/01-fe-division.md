# FE 前端分工任务书

FE 负责 LabelHub 的浏览器端工作台建设，交付身份化路由、统一布局、账号认证、任务发布、标注作答、动态表单、AI 预审展示、人工审核、Admin 运维入口和前后端接口适配能力。

前端需要同时支持两类运行方式：一类是接入后端接口的真实服务模式，另一类是用于联调前演示和页面自测的 Mock 模式。真实服务模式通过统一 HTTP Client 调用后端 `/v1` 接口；Mock 模式用于保障页面和交互流程在后端接口未就绪时也能演示。

## 0. FE 目标

FE 的交付目标包括：

- 提供 Admin、Owner、Labeler、Reviewer 四类身份的登录后工作入口。
- 按登录用户身份限制可访问路由，防止不同身份工作台互相串页。
- 支持账号登录、注册、token 本地保存、请求鉴权头注入、401 refresh token 重试和退出清理。
- Owner 支持任务管理、任务创建编辑、数据集文件解析预览、模板管理、模板设计器、任务直接导出、AI 审核配置和指派标注员选择。
- Labeler 支持任务广场、任务领取、作答工作台、草稿保存、提交、我的领取/提交状态、题目审核历史查看。
- Reviewer 支持领取待审任务、审核队列、任务详情审核、批量审核、AI 自动预审队列、AI 重试和提交历史追溯展示。
- Admin 支持平台看板、创建审核员、审核分配查询、LLM Provider 管理和连通性测试。
- 动态表单支持 Designer 可视化搭建、后端 schema 映射、Formily 渲染、字段级 LLM Trigger、规则联动和只读预览。
- 页面统一处理 loading、empty、error、禁用态、确认弹窗、表单校验和操作反馈。

FE 不负责后端事务、最终权限裁决、任务状态机、数据入库、对象存储、AI 模型执行、审核结论持久化、奖励结算和审计日志落库。上述结果以后端接口返回为准，前端负责调用、展示、交互保护和失败反馈。

## 1. 基础工程、身份与 HTTP 能力

### 1.1 大功能：身份化路由与访问保护

交付效果：

- 用户访问根路径时，根据登录状态进入登录页或登录身份首页。
- 登录页只对未登录用户开放，已登录用户访问登录页会跳转到对应身份首页。
- `/app` 下所有业务页需要登录，并且按登录身份限制路由范围。
- 用户不能直接进入其他身份的业务路径。

小功能：

1. 身份首页
   - `ADMIN` 进入 `/app/admin`。
   - `OWNER` 进入 `/app/owner`。
   - `LABELER` 进入 `/app/labeler`。
   - `REVIEWER` 进入 `/app/reviewer`。

2. 路由范围
   - `/login`：登录/注册页。
   - `/app/admin`：管理员数据看板。
   - `/app/admin/review-assignment`：审核分配查询。
   - `/app/admin/llm-providers`：LLM Provider 管理。
   - `/app/owner`：Owner 工作台。
   - `/app/owner/tasks`：任务管理。
   - `/app/owner/tasks/new`：新建任务。
   - `/app/owner/tasks/:taskId/edit`：任务编辑。
   - `/app/owner/templates`：模板管理。
   - `/app/owner/templates/:templateId/designer`：模板设计器。
   - `/app/labeler`：Labeler 工作台。
   - `/app/labeler/market`：任务广场。
   - `/app/labeler/workbench/:taskId`：作答工作台。
   - `/app/labeler/submissions`：我的领取/提交。
   - `/app/reviewer`：Reviewer 工作台。
   - `/app/reviewer/claim`：领取待审。
   - `/app/reviewer/queue`：审核队列。
   - `/app/reviewer/ai-reviews`：AI 审核队列。
   - `/app/reviewer/tasks/:taskId`：任务审核详情。

3. 路由兜底
   - 各身份未开放的子路径展示“入口预留”占位页。
   - 未匹配路径回到登录页。

交付内容：

- 应用路由。
- 登录页公开访问控制。
- 业务页登录保护。
- 身份路径白名单校验。
- 身份首页跳转。
- 预留入口占位状态。

边界：

- 前端路由保护提升体验，但不能替代后端 RBAC。
- 预留占位页只代表路由兜底，不代表对应业务页面纳入本阶段交付。

验收：

```Plaintext
未登录访问 /app 下任意页面会跳转 /login。
已登录访问 /login 会跳转登录身份首页。
Admin、Owner、Labeler、Reviewer 分别只能访问自己的 /app 子路径。
未开放子路径展示预留占位，不出现空白页。
```

### 1.2 大功能：登录、注册与 token 管理

交付效果：

- 用户可以通过账号密码登录。
- 用户可以注册 Owner 或 Labeler 账号。
- 登录成功后保存 accessToken、refreshToken、tokenVersion、登录身份和用户信息。
- 页面刷新后可以从 localStorage 恢复登录态。
- HTTP 请求自动带 Authorization header，401 时尝试 refresh token。

小功能：

1. 登录表单
   - 登录模式输入用户名或邮箱、密码。
   - 登录成功后根据后端返回角色进入对应首页。
   - 认证失败展示错误消息。

2. 注册表单
   - 注册模式输入用户名、邮箱、身份、密码和确认密码。
   - 注册身份开放 Owner 和 Labeler。
   - 前端校验邮箱格式和两次密码一致。

3. token 存储
   - accessToken、refreshToken、tokenVersion 存入 localStorage。
   - role 和 user 信息存入 localStorage。
   - 退出时清理 token 与身份信息。

4. refresh 机制
   - 普通业务接口返回 401 时，用 refreshToken 调用刷新接口。
   - 刷新成功后重放原请求。
   - 刷新失败时清理本地登录态。

对接接口：

```Plaintext
POST /v1/auth/login
POST /v1/auth/register
POST /v1/auth/refresh
```

交付内容：

- Auth store。
- 登录/注册页。
- Auth service。
- HTTP Client token 注入。
- 401 refresh token 重试。
- 本地登录态恢复。
- 退出清理。

边界：

- Admin 和 Reviewer 不开放自助注册，Reviewer 通过 Admin 创建。
- 前端保存用户展示信息，真实身份和权限仍以后端 token 和接口鉴权为准。

验收：

```Plaintext
账号登录成功后进入后端返回角色对应首页。
注册 Owner 或 Labeler 后进入对应首页。
刷新页面后，存在有效本地 token 和 role 时保持已登录状态。
业务请求自动携带 Bearer token。
401 后会尝试 refresh，失败后清理登录态。
```

### 1.3 大功能：统一 HTTP 与服务模式

交付效果：

- 前端有统一 request 封装，业务 service 不直接散落 axios 细节。
- 支持按环境变量选择真实接口模式或 Mock 模式。
- 后端统一响应包会被自动拆包，非成功 code 转为统一 ApiError。

小功能：

1. HTTP Client
   - 统一 baseURL，默认 `/api`。
   - 默认超时 15 秒。
   - 自动注入 Authorization。
   - 自动 unwrap `{ code, data, message }` 响应。
   - code 为 `0` 或 `200` 时视为成功。
   - 网络错误、业务错误、HTTP 错误统一转为 ApiError。

2. 服务模式
   - `VITE_SERVICE_MODE=real` 时调用真实后端接口。
   - `VITE_SERVICE_MODE=mock` 或未配置时走 Mock 数据。
   - Owner、Labeler、Admin LLM Provider 等模块支持模式切换。

3. API 基地址
   - `VITE_API_BASE_URL` 可配置后端地址。
   - 未配置时默认 `/api`。

交付内容：

- HTTP 类型定义。
- request get/post/put/patch/delete 封装。
- 服务模式判断。
- ApiError 归一化。
- token refresh 与重试。

边界：

- 并非每个 service 都需要完整 Mock/Real 双实现；核心业务页面优先保障真实接口对接。
- 前端只做错误消息归一化，具体业务错误码语义以后端定义为准。

验收：

```Plaintext
真实模式下 service 调用 /v1 开头后端接口。
Mock 模式下核心演示页面仍能工作。
后端响应包能被拆出 data。
业务错误能在页面显示明确 message。
```

## 2. 应用布局、导航与页面状态

### 2.1 大功能：身份化导航与统一布局

交付效果：

- 四类身份共用应用布局，但侧边导航按登录身份切换。
- 顶部栏展示用户和退出入口。
- 页面标题、说明、操作区、内容容器和空状态风格统一。

小功能：

1. Admin 导航
   - 工作台。
   - 审核分配。
   - LLM Provider。

2. Owner 导航
   - 工作台。
   - 任务管理。
   - 模板管理。

3. Labeler 导航
   - 工作台。
   - 任务广场。
   - 我的领取。

4. Reviewer 导航
   - 工作台。
   - 领取待审。
   - 审核队列。
   - AI 审核队列。

交付内容：

- 应用布局。
- 侧边导航。
- 顶部导航。
- 身份徽标。
- 页面标题组件。
- 内容容器。
- 空/错/加载占位组件。
- 面包屑导航组件。

边界：

- 导航项以本阶段开放页面为准。
- Owner 导出与审计不纳入导航交付范围，导出能力在任务列表中以直接导出动作交付。

验收：

```Plaintext
不同身份登录后看到不同侧边菜单。
登录身份对应菜单高亮。
退出后清理登录态并回到登录页。
页面 loading、empty、error 风格一致。
```

### 2.2 大功能：状态 Store

交付效果：

- 跨页面状态由 Zustand store 管理。
- 页面组件负责展示和交互，数据加载、错误状态、提交状态集中在 store 或 service 中。

小功能：

1. 身份状态
   - 用户。
   - 角色。
   - token 信息。
   - 登录/注册/退出动作。

2. 看板状态
   - Admin 看板。
   - Owner 看板。
   - Labeler 看板。
   - Reviewer 看板。

3. Owner 状态
   - 任务列表。
   - 任务编辑草稿。
   - 模板设计器 schema、选中节点、保存状态、错误状态和未保存标记。

4. Labeler 状态
   - 市场任务。
   - 作答任务。
   - 题目列表。
   - 草稿。
   - 提交校验。
   - 我的领取/提交统计。

5. Reviewer 状态
   - 审核队列。
   - 审核任务。
   - AI 审核日志。
   - 审核详情。
   - 提交版本。
   - 题目历史。
   - 批量审核状态。

交付内容：

- Auth store。
- Admin dashboard store。
- Owner dashboard/task/draft store。
- Labeler dashboard/labeling store。
- Reviewer dashboard/review store。
- Template designer store。
- Page UI/navigation store。

边界：

- Store 中的状态不等于最终业务状态，真实模式下以后端返回刷新为准。
- Mock 模式下部分状态存于内存或 localStorage，仅用于演示和页面自测。

验收：

```Plaintext
页面能展示加载中、加载失败、空状态和提交中。
复杂页面刷新数据后能更新列表和详情。
Labeler 草稿、Reviewer 审核、Designer 未保存状态都有前端反馈。
```

## 3. Admin 工作台

### 3.1 大功能：管理员数据看板

交付效果：

- Admin 能查看平台级任务、用户、提交、审核、奖励和趋势概览。
- Admin 能按时间范围切换看板数据。
- Admin 能创建 Reviewer 账号。

小功能：

1. KPI 展示
   - 活跃任务。
   - 已领取。
   - 已提交。
   - 待审核。
   - 平台用户。
   - 通过率。
   - 打回率。
   - 奖励金额。

2. 图表展示
   - 平台提交趋势折线图。
   - 任务状态分布柱状图。
   - 审核质量仪表。
   - 标注员排行表。

3. 时间范围
   - 7 天。
   - 14 天。
   - 30 天。

4. 创建审核员
   - 输入用户名、邮箱、初始密码。
   - 创建成功后展示最近创建的审核员信息。

对接接口：

```Plaintext
GET /v1/admin/dashboard/overview
POST /v1/admin/users/reviewers
```

边界：

- 看板只读，不修改任务、提交或审核状态。
- 创建审核员只负责调用后端接口，不在前端维护完整用户管理列表。

验收：

```Plaintext
Admin 能打开数据看板。
Admin 能切换 7/14/30 天范围并刷新。
看板能展示 KPI、趋势、状态分布和排行。
Admin 能创建审核员账号并看到创建结果。
```

### 3.2 大功能：审核分配查询

交付效果：

- Admin 能查看可分配审核任务、可分配审核员和审核员进度。
- 页面定位为查询和运维视图，不直接执行分配动作。

小功能：

1. 可分配任务
   - 按关键词搜索。
   - 按任务 ID 查询。
   - 按审核级别查询。
   - 可选择是否包含已认领。
   - 展示任务状态、待审量、认领状态和截止时间。

2. 可分配审核员
   - 按用户名或邮箱搜索。
   - 支持仅启用筛选。
   - 展示待审负载、今日已审和通过率。

3. 审核员进度
   - 展示审核员可用状态。
   - 展示待审、今日已审、历史已审和通过率。
   - 展示已认领任务列表。

对接接口：

```Plaintext
GET /v1/admin/review/tasks/assignable
GET /v1/admin/review/reviewers/assignable
GET /v1/admin/review/reviewers/progress
```

边界：

- 本页面不执行任务分配，只查询分配相关数据。
- 分配、领取和释放等写操作由 Reviewer 领取待审或后端能力承担。

验收：

```Plaintext
Admin 能查询可分配任务。
Admin 能查询可分配 Reviewer。
Admin 能查看 Reviewer 进度。
筛选条件变化后列表刷新。
接口失败时展示错误提示。
```

### 3.3 大功能：LLM Provider 管理

交付效果：

- Admin 能维护全局 LLM Provider。
- Admin 能新增、编辑、启用、停用和测试 Provider。
- Provider 能力信息供 Owner 任务 AI 配置选择。

小功能：

1. Provider 列表
   - 展示 Provider 名称、编码、默认模型、baseUrl。
   - 展示启用/停用状态。
   - 展示视觉、多图、结构化输出能力。
   - 展示平台/任务/用户限流。
   - 展示 API Key 是否已配置。

2. 新增/编辑
   - Provider 编码。
   - 展示名称。
   - Base URL。
   - API Key。
   - 默认模型。
   - 支持视觉、多图、最大图片数、视觉模型。
   - 结构化输出模式。
   - 限流配置。
   - Custom Headers。

3. 密钥处理
   - 编辑时 API Key 留空表示保留原密钥。
   - 响应中的敏感 Header 按脱敏值展示，不当作明文密钥。

4. 连通性测试
   - 可输入临时 API Key、模型名和 Headers。
   - 显示测试成功/失败、消息和耗时。

对接接口：

```Plaintext
GET /v1/admin/llm-providers
POST /v1/admin/llm-providers
PUT /v1/admin/llm-providers/{providerId}
POST /v1/admin/llm-providers/{providerId}/enable
POST /v1/admin/llm-providers/{providerId}/disable
POST /v1/admin/llm-providers/{providerId}/test
```

边界：

- Mock 模式提供 Provider 示例数据。
- 密钥加密、连通性测试执行和 Provider 可用性以后端为准。

验收：

```Plaintext
Admin 能查看 Provider 列表。
Admin 能新增和编辑 Provider。
Admin 能启用或停用 Provider。
Admin 能发起 Provider 测试并看到结果。
敏感字段不会在前端被当作明文展示。
```

## 4. Owner 工作台

### 4.1 大功能：Owner 看板

交付效果：

- Owner 能查看自己任务范围内的进度、质量和待处理摘要。
- 看板作为入口页，提供进入任务、模板和重点任务的导航。

小功能：

- 任务数。
- 进行中任务。
- 待处理事项。
- 进度和质量摘要。
- 任务管理入口。
- 新建任务入口。
- 模板管理入口。
- 重点任务编辑入口。

对接接口：

```Plaintext
GET /v1/owner/dashboard/overview
```

边界：

- 看板只读，不直接推进任务状态。
- Mock 模式使用本地看板数据。

验收：

```Plaintext
Owner 能打开工作台。
Owner 能看到任务进度和质量摘要。
Owner 能从首页进入任务和模板页面。
加载失败时展示错误状态。
```

### 4.2 大功能：任务管理与直接导出

交付效果：

- Owner 能分页查看任务列表，按关键词和状态筛选。
- Owner 能对任务执行发布、暂停、恢复、结束、删除草稿等生命周期动作。
- Owner 能对任务发起直接导出。

小功能：

1. 列表查询
   - 关键词搜索。
   - 状态筛选。
   - 分页参数映射。
   - 后端状态 `DRAFT/PUBLISHED/PAUSED/ENDED` 与前端展示状态互转。

2. 任务动作
   - 新建任务。
   - 编辑任务。
   - 发布任务。
   - 暂停任务。
   - 恢复任务。
   - 结束任务。
   - 删除草稿任务。

3. 进度统计
   - 读取任务统计。
   - 映射总题量、领取量、提交量、待审量、通过量、打回量和通过率。

4. 直接导出
   - 支持选择导出格式。
   - 调用任务直接导出接口。
   - Mock 模式生成可下载结果。

对接接口：

```Plaintext
GET /v1/owner/tasks
GET /v1/tasks/{taskId}
POST /v1/tasks
PUT /v1/tasks/{taskId}
DELETE /v1/tasks/{taskId}
POST /v1/tasks/{taskId}/publish
POST /v1/tasks/{taskId}/pause
POST /v1/tasks/{taskId}/resume
POST /v1/tasks/{taskId}/end
GET /v1/tasks/{taskId}/statistics
POST /v1/tasks/{taskId}/exports/direct
```

边界：

- 真实生命周期合法性以后端状态机为准。
- 直接导出的文件生成、权限和数据范围以后端为准。

验收：

```Plaintext
Owner 能查看和筛选任务列表。
Owner 能进入新建和编辑页面。
Owner 能触发发布、暂停、恢复、结束等动作。
Owner 能触发直接导出并得到下载结果或错误反馈。
```

### 4.3 大功能：任务创建与编辑

交付效果：

- Owner 能在任务编辑页维护任务基础信息、分发策略、奖励规则、模板版本、数据集文件、AI 审核配置和发布前校验。
- 页面把复杂任务配置拆成基础信息、数据导入、预览校验等区域。

小功能：

1. 基础信息
   - 标题。
   - 描述。
   - 富文本任务说明。
   - 标签。
   - 截止时间。
   - 任务配额。
   - 审核级别数。
   - overlapCount。
   - maxClaimsPerLabeler。

2. 奖励规则
   - 奖励模式。
   - 单题奖励。
   - 奖励货币。
   - 是否对标注员可见。

3. 分发策略
   - 先到先得。
   - 配额分发。
   - 指派。
   - 指派模式下可打开标注员选择抽屉，查询可分配 Labeler。

4. 模板绑定
   - 选择已存在模板版本。
   - 任务保存时提交 publishedTemplateVersionId。
   - 模板版本作为发布前必要条件。

5. 数据集文件
   - 上传文件到后端文件接口。
   - 前端解析 JSON、JSONL、XLSX 形成导入预览。
   - 展示总行数、有效行、错误行、字段映射、样例和阻塞问题。
   - 支持任务数据项查询和批量追加 JSON 数据。

6. AI 审核配置
   - 选择启用的 LLM Provider。
   - 回填默认模型。
   - 配置 Prompt。
   - 配置评分维度。
   - 配置通过阈值和人工复核阈值。
   - 配置审核策略：LIGHTWEIGHT、PARALLEL_VOTE、DEEP_DIMENSION、AGENT_DEBATE。
   - 配置 AI 流转策略。

7. 发布校验
   - 要求先保存任务草稿。
   - 校验标题、标签长度、配额、截止时间、模板版本、数据集文件、审核级别、奖励、分发策略、AI Provider、Prompt、模型名、评分维度和阈值。
   - 只有草稿任务可发布。

对接接口：

```Plaintext
GET /v1/owner/labelers/assignable
GET /v1/llm-providers
POST /v1/files/upload
GET /v1/tasks/{taskId}/dataset/items
POST /v1/tasks/{taskId}/dataset/items/batch-append-json
```

边界：

- 前端解析文件用于交互预览，真实导入入库和数据集状态以后端为准。
- AI Prompt 测试入口不纳入任务编辑页交付。
- 发布前校验是前端补充保护，最终发布结果以后端返回为准。

验收：

```Plaintext
Owner 能维护任务基础字段、奖励、分发策略和 AI 配置。
Owner 能上传数据集文件并看到 JSON/JSONL/XLSX 解析预览。
Owner 能选择模板版本和 LLM Provider。
发布前缺少模板、数据集、AI Provider 或 Prompt 会被拦截。
指派策略下能查询并选择可分配标注员。
```

### 4.4 大功能：模板管理与版本保存

交付效果：

- Owner 能查看模板列表、创建模板、进入模板设计器。
- 模板保存时将前端 schema 转换为后端模板 schema。
- 支持基于模板版本 fork 新版本。

小功能：

1. 模板列表
   - 展示模板名称、描述、版本、状态和字段数。
   - 支持空状态和错误状态。

2. 模板创建
   - 输入模板名称和描述。
   - 创建空 schema。
   - 真实模式下调用 Owner 模板创建接口。

3. 版本查询
   - 查询模板版本列表。
   - 查询模板详情时读取版本 schema。

4. 保存 schema
   - Designer 保存时调用 fork 版本逻辑。
   - 前端 `llmPrompt` 映射为后端 `LlmTrigger`。
   - 前端 `showItem` 映射为后端 `ShowItem`。

对接接口：

```Plaintext
GET /v1/owner/templates
POST /v1/owner/templates
GET /v1/templates/{templateId}/versions
POST /v1/templates/{templateId}/fork
```

边界：

- 模板 schema 的最终合法性以后端 Schema 校验为准。
- 模板版本差异对比页面不纳入本阶段交付。

验收：

```Plaintext
Owner 能查看模板列表。
Owner 能创建模板。
Owner 能进入设计器编辑模板。
保存时能生成新的模板版本或 Mock 版本。
后端 schema 与前端 schema 能互相转换。
```

## 5. 动态表单 Designer、Renderer 与 LLM Trigger

### 5.1 大功能：Designer 可视化搭建

交付效果：

- Owner 能通过物料区、画布区、属性区和预览区搭建动态标注模板。
- 设计树支持拖拽、选中、删除、容器嵌套、属性编辑和 schema 管理。

小功能：

1. 物料类型
   - input。
   - textarea。
   - radio。
   - checkbox。
   - select。
   - showItem。
   - richText。
   - fileUpload。
   - jsonEditor。
   - llmPrompt。
   - group。
   - tabs。
   - tabPane。

2. 物料分组
   - 文本。
   - 选择。
   - 展示。
   - 媒体。
   - 智能。
   - 结构。

3. 容器规则
   - 根节点不能直接添加 tabPane。
   - group 和 tabPane 能承载普通字段。
   - tabs 只接收 tabPane。
   - 新增 tabs 时默认生成“基础信息”和“补充信息”两个 tabPane。

4. 属性编辑
   - 标题。
   - 字段 key。
   - placeholder。
   - options。
   - required。
   - 条件显示。
   - required/disabled 联动。
   - 选项联动。
   - LLM Provider、模型、Prompt、目标字段等智能组件属性。

交付内容：

- 物料注册表。
- 物料面板。
- 设计画布。
- 节点卡片。
- Drop Zone。
- 拖拽预览。
- 属性面板。
- 条件规则编辑器。
- 联动规则编辑器。
- Schema 管理面板。
- Designer store。

边界：

- 设计器前端规则是交互表达，不是最终后端 schema 安全校验。
- 复杂联动按 Formily reactions 转换能力执行，不作为完整规则引擎交付。

验收：

```Plaintext
Owner 能向画布添加所有物料。
Owner 能拖拽调整节点位置。
Owner 能编辑节点属性和规则。
Tabs、Group、TabPane 的嵌套规则符合约束。
Designer 能保存和导入 schema。
```

### 5.2 大功能：后端 Schema 映射与 Formily 渲染

交付效果：

- 前端动态 schema 可以转换为后端模板 schema。
- 后端返回的模板 schema 可以转换回前端设计器 schema。
- Renderer 基于 Formily 渲染可编辑或只读表单。

小功能：

1. 后端映射
   - 前端 `llmPrompt` 转后端 `LlmTrigger`。
   - 前端 `showItem` 转后端 `ShowItem`。
   - 后端 `LlmTrigger` 转回 `llmPrompt`。
   - 后端 `ShowItem` 转回 `showItem`。
   - required、enum、providerId、modelName、promptTemplate、targetFields 等字段参与映射。

2. Formily 转换
   - input 渲染为 Input。
   - textarea 渲染为 Input.TextArea。
   - radio 渲染为 Radio.Group。
   - checkbox 渲染为 Checkbox.Group。
   - select 渲染为 Select。
   - showItem 渲染为 ShowItem。
   - richText 渲染为 RichTextEditor。
   - fileUpload 渲染为 FileUploadField。
   - jsonEditor 渲染为 JsonEditorField。
   - llmPrompt 渲染为 LlmPromptBlock。
   - group/tabs/tabPane 渲染为布局容器。

3. 校验与联动
   - required 转 Formily required validator。
   - minLength/maxLength 转长度校验。
   - enum 转枚举校验。
   - visibleWhen 转 visible reaction。
   - requiredWhen 转 required reaction。
   - disabledWhen 转 disabled reaction。
   - linkedOptions 转 options reaction。

4. Renderer 运行参数
   - initialValues。
   - readOnly。
   - submitText。
   - answerFieldKeys。
   - getCurrentValues。
   - llmContext。
   - onValuesChange。
   - onSubmit。
   - onApplyLlmValues。

边界：

- 前端校验用于即时反馈，正式答案校验以后端为准。
- fileUpload 字段在 Renderer 中表达文件列表值，真实上传需结合后端文件服务。

验收：

```Plaintext
Designer schema 能转为后端 components schema。
后端 schema 能回显到 Designer。
Renderer 能渲染所有注册字段类型。
readOnly 模式下审核页不能编辑答案。
条件显示和联动规则能在 Formily 中执行。
```

### 5.3 大功能：字段级 LLM Trigger

交付效果：

- `llmPrompt` 组件在运行时可以触发 assignment 维度 LLM 辅助。
- 前端轮询 trigger run 状态，完成后展示建议、patch、风险提示和可填充值。
- 用户确认后再把 LLM 结果应用到表单值。

小功能：

1. 触发条件
   - 需要 assignmentId。
   - 携带答案 JSON。
   - 携带 datasetItemId。
   - 携带用户补充指令。

2. 运行轮询
   - 提交触发请求。
   - 若返回 triggerRunId，则按 1500ms 间隔轮询。
   - 最多轮询 50 次。
   - PENDING/RUNNING 持续超时后返回失败提示。

3. 结果展示
   - suggestionJson。
   - patch。
   - displayText。
   - targetFields。
   - rawModelSummary。
   - confidence。
   - warnings。
   - traceId。
   - latencyMs。
   - errorCode/errorMessage。

对接接口：

```Plaintext
POST /v1/assignments/{assignmentId}/llm-triggers
GET /v1/llm/triggers/runs/{triggerRunId}
```

边界：

- 前端只负责触发、轮询、展示和应用建议；模型调用、运行记录、权限和限流以后端为准。
- 缺少 assignmentId 时前端拒绝触发。

验收：

```Plaintext
Labeler 作答时 llmPrompt 能携带上下文触发 LLM。
运行中状态会被轮询。
成功结果能展示并应用到目标字段。
失败或超时会给出错误信息。
```

## 6. Labeler 工作台

### 6.1 大功能：Labeler 看板

交付效果：

- Labeler 能查看个人参与、领取、提交、审核和奖励类概览。
- 看板提供任务广场、我的领取和继续作答入口。

对接接口：

```Plaintext
GET /v1/labeler/dashboard/overview
```

边界：

- 看板只读，不修改领取、草稿或提交状态。

验收：

```Plaintext
Labeler 能进入个人工作台。
Labeler 能查看个人概览。
Labeler 能从工作台进入任务广场和我的领取。
```

### 6.2 大功能：任务广场与领取

交付效果：

- Labeler 能查看可参与任务，按关键词、标签和状态筛选。
- Labeler 能领取任务 item，并进入作答工作台。

小功能：

1. 市场查询
   - 真实模式调用市场任务接口。
   - 根据返回任务快照构造前端任务卡片。
   - 展示标题、说明、标签、截止时间、奖励摘要、剩余数量、个人已领取数量和每人上限。

2. 筛选
   - 关键词。
   - 标签。
   - 状态。

3. 领取
   - 调用任务 item 领取接口。
   - 携带领取数量等选项。
   - 领取后缓存 assignment 上下文，便于进入作答工作台。

对接接口：

```Plaintext
GET /v1/market/tasks
POST /v1/tasks/{taskId}/items/claim
GET /v1/claims
```

边界：

- 真实领取数量、并发控制和 item 分配以后端为准。
- Mock 模式下领取只修改本地任务状态。

验收：

```Plaintext
Labeler 能查看任务广场。
Labeler 能筛选任务。
Labeler 能领取可参与任务。
领取成功后能进入作答工作台。
```

### 6.3 大功能：作答工作台、草稿与提交

交付效果：

- Labeler 能查看已领取任务的题目材料、模板表单和历史反馈。
- 作答时自动保存草稿，也可手动保存。
- 提交时先校验题目或任务草稿，再调用提交接口。

小功能：

1. 工作台加载
   - 加载任务详情。
   - 加载题目列表。
   - 加载审核摘要。
   - 加载题目审核历史。
   - 加载答案模板。

2. 表单作答
   - 使用 DynamicFormRenderer 渲染后端模板 schema。
   - showItem 展示题目原始数据。
   - 用户输入写入草稿。
   - LLM Trigger 可在 assignment 场景运行。

3. 草稿保存
   - 输入变化后约 1200ms 防抖自动保存。
   - 支持手动保存。
   - 真实模式调用 claim 草稿接口并携带 clientVersion。
   - Mock 模式保存到 localStorage。
   - 草稿版本冲突等后端错误会显示明确提示。

4. 提交
   - 支持提交单题草稿。
   - 支持提交任务草稿。
   - 提交前执行前端必填校验。
   - 真实模式调用 claim 提交接口并携带 clientVersion。
   - 提交后刷新市场和我的领取/提交数据。

对接接口：

```Plaintext
GET /v1/labeler/tasks/{taskId}/answer-template
GET /v1/claims
GET /v1/claims/{claimId}/draft
PUT /v1/claims/{claimId}/draft
POST /v1/claims/{claimId}/submit
GET /v1/submissions/{submissionId}/item-history
```

边界：

- 真实模式下我的提交列表和统计当前由前端 Mock fallback 提供；后端已提供 `/api/v1/labeler/submissions` 列表和详情接口。
- 前端校验不能替代后端 Schema 校验。

验收：

```Plaintext
Labeler 能进入作答工作台。
题目材料和模板表单能展示。
答案变化后能自动保存草稿。
草稿提交时携带版本信息。
提交失败时保留输入并展示原因。
题目历史能按 submissionId 查询并展示。
```

### 6.4 大功能：我的领取/提交

交付效果：

- Labeler 能查看自己领取的任务、草稿状态、提交状态和审核结果。
- 被打回的数据可以回到作答工作台继续修改。

小功能：

1. 领取统计
   - total。
   - claimed。
   - drafting。
   - submitted。
   - returned。
   - approved。
   - cancelled。

2. 列表展示
   - taskId。
   - taskTitle。
   - assignmentId。
   - datasetItemId。
   - draftVersion。
   - claimedAt。
   - returnedAt。
   - updatedAt。
   - myClaimedCount、mySubmittedCount、myApprovedCount。

3. 提交反馈
   - AI 决策。
   - 人工审核状态。
   - 打回原因。
   - 审核意见。
   - 答案 JSON 预览。

边界：

- 领取列表来自 `/v1/claims`；提交统计和提交列表当前由前端 Mock fallback 提供，后端已提供 `/api/v1/labeler/submissions` 列表和详情接口。
- 提交版本 diff 页面不纳入 Labeler 独立页面交付。

验收：

```Plaintext
Labeler 能看到我的领取。
Labeler 能看到领取统计。
Labeler 能查看提交状态和打回原因。
被打回任务能返回工作台处理。
```

## 7. Reviewer 工作台

### 7.1 大功能：Reviewer 看板

交付效果：

- Reviewer 能查看个人审核概览。
- 看板提供进入领取待审、审核队列和 AI 审核队列的入口。

对接接口：

```Plaintext
GET /v1/reviewer/dashboard/overview
```

验收：

```Plaintext
Reviewer 能进入工作台。
Reviewer 能查看审核概览。
Reviewer 能进入领取待审、审核队列和 AI 审核队列。
```

### 7.2 大功能：领取待审

交付效果：

- Reviewer 能查看可领取或已领取的审核任务。
- Reviewer 能按任务领取待审提交。
- 领取后可进入任务审核详情页查看 item。

小功能：

1. 任务卡片
   - 展示 taskId、任务标题、待领取数、我的待审数、已审数。
   - 展示任务是否 Mine、Claimed 或 Open。
   - 展示处理进度。

2. 领取动作
   - 对待审数大于 0 的任务开放 Claim。
   - 领取成功后展示 claimedCount。
   - 领取结果保存在 review store 中用于页面提示。

对接接口：

```Plaintext
GET /v1/reviewer/tasks
POST /v1/reviewer/tasks/{taskId}/claim
```

边界：

- 释放领取不纳入本页面交付。
- 领取级别按 reviewLevel=1 提交。

验收：

```Plaintext
Reviewer 能查看可领取任务。
Reviewer 能领取待审提交。
领取成功后显示领取数量。
Reviewer 能进入任务详情查看 item。
```

### 7.3 大功能：审核队列与批量审核

交付效果：

- Reviewer 能查看自己已领取范围内的待审核提交。
- Reviewer 能按关键词、风险、状态等条件筛选。
- Reviewer 能执行批量通过或批量打回。

小功能：

1. 队列来源
   - 调用 Reviewer submissions 接口。
   - scope 使用 CLAIMED。
   - 映射为前端队列 item。

2. 批量操作
   - 批量通过调用 batch approve。
   - 批量打回调用 batch reject。
   - 批量打回携带 reason。
   - 响应中的失败项映射为前端失败列表。

对接接口：

```Plaintext
GET /v1/reviewer/submissions
POST /v1/reviewer/submissions/batch/approve
POST /v1/reviewer/submissions/batch/reject
```

边界：

- 批量操作的事务语义、部分失败原因和权限以后端为准。
- 页面只展示前端映射后的成功/失败结果。

验收：

```Plaintext
Reviewer 能查看审核队列。
Reviewer 能筛选队列。
Reviewer 能批量通过。
Reviewer 能批量打回并填写原因。
部分失败能在前端展示失败原因。
```

### 7.4 大功能：任务审核详情与单条审核

交付效果：

- Reviewer 能按任务查看待审 item，选择提交后查看 AI 建议、答案、历史版本和审核历史。
- Reviewer 能执行通过或打回。
- 页面支持在同一任务内按 AI 决策过滤 item，并执行批量审核。

小功能：

1. 任务 item
   - 查询任务下待处理 item。
   - 展示 AI 决策分布。
   - 支持 all/pass/reject/manual 过滤。

2. 提交详情
   - 查询 Reviewer submission detail。
   - 映射为前端 ReviewDetail。
   - 展示题目、答案、AI 结果和人工审核记录。

3. 版本与历史
   - 查询 submission versions。
   - 查询 submission item history。
   - 展示 AI 预审轮次和人工审核轮次。

4. 单条审核
   - approve 支持 reviewComment 和 revisedAnswerJson payload 结构。
   - 本页面不提供答案编辑器，revisedAnswerJson 不作为可视化编辑能力交付。
   - reject 要求 reason。
   - 审核后刷新详情。

对接接口：

```Plaintext
GET /v1/reviewer/tasks/{taskId}/items
GET /v1/reviewer/submissions/{submissionId}
POST /v1/reviewer/submissions/{submissionId}/approve
POST /v1/reviewer/submissions/{submissionId}/reject
GET /v1/submissions/{submissionId}/versions
GET /v1/submissions/{submissionId}/item-history
```

边界：

- 详情路由参数是 taskId，页面内部再选择具体 submission。
- approve 的 revisedAnswerJson 只保留接口 payload 支持，不交付答案编辑器。

验收：

```Plaintext
Reviewer 能打开任务审核详情。
Reviewer 能筛选任务 item。
Reviewer 能查看提交详情、版本和历史。
Reviewer 能通过单条提交。
Reviewer 打回时必须填写原因。
审核后详情和队列状态刷新。
```

### 7.5 大功能：AI 自动预审队列

交付效果：

- Reviewer 能查看 AI 自动预审记录。
- Reviewer 能按状态查看待审核、已通过、已打回、转人工和失败记录。
- Reviewer 能查看 AI 评分、风险标记、Prompt 快照、提交内容和处理日志。
- 对失败或需要人工的 AI 记录，Reviewer 能触发重试。

小功能：

1. 列表筛选
   - pending。
   - passed。
   - rejected。
   - manual。
   - failed。
   - 分页。

2. 详情展示
   - submissionId。
   - agentRunId。
   - taskTitle。
   - aiReviewStatus。
   - decision。
   - averageScore。
   - dimensions。
   - riskFlags。
   - suggestion。
   - promptSnapshot/rawPrompt。
   - answerJson。

3. 审计/历史
   - 读取题目历史。
   - 将 AI 预审和人工审核轮次组合为时间线。

4. 重试
   - 当记录状态为 FAILED 或 MANUAL_REQUIRED 且存在 submissionId 时显示重试按钮。
   - 重试成功后提示并刷新记录。

对接接口：

```Plaintext
GET /v1/reviewer/ai-review-status
GET /v1/tasks/{taskId}/ai-review-logs
GET /v1/submissions/{submissionId}/ai-review
POST /v1/submissions/{submissionId}/ai-review/retry
GET /v1/submissions/{submissionId}/item-history
```

边界：

- 页面中的“平均耗时”“重试率”等 UI 摘要不作为后端真实指标交付项。
- AI 审核执行、重试调度和状态推进以后端为准。

验收：

```Plaintext
Reviewer 能打开 AI 审核队列。
Reviewer 能按 AI 状态筛选记录。
Reviewer 能查看 AI 评分、风险、Prompt 和答案。
Reviewer 能查看提交历史时间线。
失败或转人工记录能触发 AI 重试。
```

## 8. FE 与后端协作边界

### 8.1 FE 负责范围

- 身份化路由、布局和导航。
- 登录、注册、token 保存、refresh 重试和退出。
- 统一 HTTP Client、ApiError 和服务模式切换。
- Admin 看板、审核分配查询、LLM Provider 管理。
- Owner 任务管理、任务编辑、文件解析预览、模板管理、Designer、直接导出。
- Labeler 市场、领取、作答、草稿、提交、我的领取/提交。
- Reviewer 领取待审、审核队列、任务详情审核、AI 审核队列。
- 动态表单 schema 映射、Designer、Renderer、LLM Trigger 触发与轮询。
- 页面级 loading、empty、error、confirm、message、表单校验和只读态。

### 8.2 FE 不做最终裁决

- 用户身份和权限以后端 Auth/RBAC 为准。
- token 是否有效以后端鉴权和 refresh 接口为准。
- 任务生命周期以后端状态机为准。
- 数据集导入入库、题目状态和并发领取以后端为准。
- 模板版本、schema 校验和答案校验以后端为准。
- 文件存储和下载 URL 以后端文件服务为准。
- AI Provider 密钥、模型调用、限流和运行记录以后端为准。
- AI 自动预审和 LLM Trigger 结果以后端 AI 服务为准。
- 人工审核结论、批量审核事务和历史追溯以后端为准。
- 导出文件生成、奖励结算、审计日志和通知以后端平台能力为准。

### 8.3 接口覆盖范围

前端需要对接以下后端能力：

```Plaintext
Auth：登录、注册、刷新 token。
Admin：平台看板、创建审核员、审核分配查询、LLM Provider 管理。
Owner：任务 CRUD、生命周期、统计、可分配标注员、模板、文件上传、数据项查询/追加、直接导出、LLM Provider 查询。
Labeler：任务市场、领取、claims、答案模板、草稿、提交、题目历史。
Reviewer：审核任务、领取待审、审核队列、审核详情、单条/批量审核、AI 审核状态、AI 重试、提交版本、题目历史。
LLM：assignment 字段级 trigger、trigger run 查询。
```

### 8.4 Mock / 本地能力边界

- `VITE_SERVICE_MODE` 默认走 mock。
- Mock 模式用于 Owner、Labeler 和部分 Admin LLM Provider 的本地演示。
- Mock Labeler 草稿使用 localStorage。
- Mock Labeler 提交会触发本地 AI 审核模拟并同步回提交状态。
- 真实模式下 Labeler 我的提交统计和列表当前由前端 Mock fallback 提供；后端已提供 `/api/v1/labeler/submissions` 列表和详情接口。
- Review 页面优先按真实 Reviewer API 对接，不作为纯 Mock 页面设计。

## 9. FE 自测清单

```Plaintext
应用能进入 /login。
登录成功后按后端返回角色进入对应首页。
注册 Owner 或 Labeler 成功后进入对应首页。
刷新页面后能从 localStorage 恢复 token、role 和 user。
非登录身份访问其他身份路径会被重定向回自己的首页。
HTTP 请求能携带 Bearer token，401 时会尝试 refresh。
Admin 能查看平台看板并创建审核员。
Admin 能查看审核分配查询页。
Admin 能新增、编辑、启用、停用和测试 LLM Provider。
Owner 能查看任务列表，执行任务生命周期动作。
Owner 能新建/编辑任务，配置分发策略、奖励、模板、数据集和 AI 审核。
Owner 能解析 JSON、JSONL、XLSX 数据集文件预览。
Owner 能查询可分配标注员并用于指派策略。
Owner 能创建模板并进入模板设计器。
Designer 能添加、拖拽、嵌套、编辑和保存动态表单组件。
Renderer 能渲染所有字段类型并支持 readOnly。
llmPrompt 能在有 assignmentId 时触发 LLM Trigger 并轮询结果。
Labeler 能查看任务广场并领取任务。
Labeler 能进入作答工作台，加载模板、题目材料、草稿和题目历史。
Labeler 能自动保存草稿、手动保存、提交单题或任务。
Labeler 能查看我的领取/提交状态和打回反馈。
Reviewer 能领取待审任务。
Reviewer 能查看审核队列并批量通过/打回。
Reviewer 能进入任务审核详情，查看提交、版本、历史并单条审核。
Reviewer 能查看 AI 自动预审队列、详情和触发重试。
所有关键页面在 loading、empty、error 状态下有可见反馈。
```
