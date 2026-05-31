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
