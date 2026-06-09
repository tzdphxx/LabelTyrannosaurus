# LabelHub 前端技术文档

> 项目版本: 0.0.0 | 最后更新: 2026-06-09

---

## 目录

1. [项目概述](#1-项目概述)
2. [快速开始](#2-快速开始)
3. [技术栈](#3-技术栈)
4. [项目结构](#4-项目结构)
5. [架构分层](#5-架构分层)
6. [角色系统与路由](#6-角色系统与路由)
7. [数据流](#7-数据流)
8. [HTTP 客户端](#8-http-客户端)
9. [状态管理](#9-状态管理)
10. [动态表单系统](#10-动态表单系统)
11. [业务模块](#11-业务模块)
12. [类型系统](#12-类型系统)
13. [Mock 机制](#13-mock-机制)
14. [开发约定](#14-开发约定)

---

## 1. 项目概述

LabelHub 是一个 AI 辅助的数据标注平台，支持**任务管理 → 数据标注 → AI 预审 → 人工复核**全链路闭环。平台包含四种角色：

| 角色 | 标识 | 职责 |
|------|------|------|
| 管理员 | `ADMIN` | 平台运营看板、审核分配、LLM Provider 管理 |
| 任务负责人 | `OWNER` | 创建任务、搭建标注模板、发布分发、质量追踪、导出 |
| 标注员 | `LABELER` | 领取任务、Schema 表单作答、草稿保存、提交 |
| 审核员 | `REVIEWER` | 认领待审、人工复核、AI 审核结果回看、批量处理 |

核心能力：

- **Schema 驱动的动态表单** — 拖拽搭建标注模板，运行时按 Schema 渲染表单
- **AI 预审集成** — 展示 AI 评分、维度分析、风险提示，支持降级兜底
- **多级审核流** — 支持 AI 预审 + 多轮人工复核，审核链路可追溯

---


## 3. 技术栈

| 类别 | 技术 | 用途 |
|------|------|------|
| 框架 | React 19 + TypeScript 6 | 核心 UI 框架 |
| 构建 | Vite 8 | 开发服务器与打包 |
| UI 组件 | Ant Design 5 | 表格、表单、卡片、按钮等基础组件 |
| 图表 | @ant-design/plots | 数据看板趋势图、柱状图 |
| 表单引擎 | Formily (antd-v5) | 动态 Schema 表单渲染 |
| 状态管理 | Zustand 5 | 全局状态管理 |
| 路由 | react-router 7 | 前端路由 |
| 拖拽 | @dnd-kit | 模板设计器拖拽编排 |
| HTTP | Axios | HTTP 客户端 |
| 日期 | dayjs | 日期处理 |
| 表格导出 | xlsx | XLSX 格式导出 |

---

## 4. 项目结构

```
frontend/
├── public/
├── src/
│   ├── app/                     # 应用层
│   │   ├── guards/              # 路由守卫
│   │   │   ├── RequireAuth.tsx       # 认证守卫
│   │   │   └── PublicOnlyRoute.tsx   # 公开路由守卫（登录页）
│   │   ├── layout/
│   │   │   └── AppLayout.tsx         # 全局布局：TopNav + SideNav + Content
│   │   ├── navigation.tsx            # 角色导航配置
│   │   └── router.tsx                # 路由配置
│   ├── components/              # 共享组件
│   │   ├── navigation/
│   │   │   ├── SideNav.tsx
│   │   │   ├── TopNav.tsx
│   │   │   ├── BreadcrumbNav.tsx
│   │   │   └── RoleBadge.tsx
│   │   ├── page/
│   │   │   ├── ContentShell.tsx      # 页面内容容器
│   │   │   └── PageHeader.tsx        # 页面头部
│   │   └── states/
│   │       └── StatePlaceholder.tsx   # 四种状态占位（empty/error/loading/forbidden）
│   ├── features/               # 业务域模块
│   │   └── dynamic-form/        # 动态表单（核心能力）
│   │       ├── components/
│   │       │   ├── DynamicFormRenderer.tsx    # 运行时渲染器
│   │       │   ├── rendererFields.tsx         # 字段组件映射
│   │       │   └── designer/                  # 设计器组件
│   │       ├── utils/
│   │       │   ├── schemaTree.ts              # Schema 树操作
│   │       │   ├── formilySchema.ts           # Formily 适配
│   │       │   ├── backendSchema.ts           # 后端 Schema 适配
│   │       │   ├── designerFields.ts          # 设计器属性编辑
│   │       │   ├── designerDrag.ts            # 拖拽逻辑
│   │       │   └── designerScroll.ts          # 滚动逻辑
│   │       └── materialRegistry.ts            # 物料注册表
│   ├── hooks/                  # 通用 Hooks
│   │   └── useRoleNavigation.ts
│   ├── mocks/                  # Mock 数据
│   │   ├── index.ts
│   │   ├── labeling.mock.ts
│   │   ├── review.mock.ts
│   │   ├── tasks.mock.ts
│   │   ├── templates.mock.ts
│   │   ├── ownerDashboard.mock.ts
│   │   └── imports.mock.ts
│   ├── pages/                  # 页面组件
│   │   ├── admin/
│   │   │   ├── AdminDashboardPage.tsx
│   │   │   ├── AdminLlmProviderPage.tsx
│   │   │   └── AdminReviewAssignmentPage.tsx
│   │   ├── auth/
│   │   │   └── LoginPage.tsx
│   │   ├── labeler/
│   │   │   ├── LabelerDashboardPage.tsx
│   │   │   ├── LabelerMarketPage.tsx
│   │   │   ├── LabelerWorkbenchPage.tsx
│   │   │   └── LabelerSubmissionsPage.tsx
│   │   ├── owner/
│   │   │   ├── OwnerDashboardPage.tsx
│   │   │   ├── OwnerTasksPage.tsx
│   │   │   ├── OwnerTaskEditorPage.tsx
│   │   │   └── templates/
│   │   │       ├── OwnerTemplatesPage.tsx
│   │   │       └── OwnerTemplateDesignerPage.tsx
│   │   ├── reviewer/
│   │   │   ├── ReviewerDashboardPage.tsx
│   │   │   ├── ReviewerClaimPage.tsx
│   │   │   ├── ReviewerQueuePage.tsx
│   │   │   ├── ReviewerAiReviewQueuePage.tsx
│   │   │   └── ReviewerReviewDetailPage.tsx
│   │   └── roles/
│   │       └── RoleHomePage.tsx
│   ├── services/               # 服务层
│   │   ├── http/               # HTTP 客户端基础设施
│   │   │   ├── httpClient.ts        # Axios 封装
│   │   │   ├── httpTypes.ts         # 类型定义
│   │   │   ├── serviceMode.ts       # Mock/Real 模式切换
│   │   │   └── index.ts
│   │   ├── admin/              # 管理端服务
│   │   ├── auth/               # 认证服务
│   │   ├── labeler/            # 标注员服务（Mock + Real）
│   │   ├── owner/              # 任务负责人服务
│   │   ├── review/             # 审核服务
│   │   └── llm/                # LLM 服务
│   ├── stores/                 # 状态管理（Zustand）
│   │   ├── authStore.ts
│   │   ├── labelingStore.ts
│   │   ├── reviewStore.ts
│   │   ├── ownerTaskStore.ts
│   │   ├── ownerDraftStore.ts
│   │   ├── ownerDashboardStore.ts
│   │   ├── labelerDashboardStore.ts
│   │   ├── reviewerDashboardStore.ts
│   │   ├── adminDashboardStore.ts
│   │   ├── templateDesignerStore.ts
│   │   ├── navigationStore.ts
│   │   └── pageUiStore.ts
│   ├── types/                  # 领域类型
│   │   ├── auth.ts
│   │   ├── task.ts
│   │   ├── review.ts
│   │   ├── labeling.ts
│   │   ├── template.ts
│   │   ├── dynamicForm.ts
│   │   ├── dashboard.ts
│   │   ├── admin.ts
│   │   ├── adminReviewAssignment.ts
│   │   ├── import.ts
│   │   ├── llm.ts
│   │   ├── llmProvider.ts
│   │   ├── navigation.ts
│   │   └── page.ts
│   ├── utils/                  # 工具函数
│   │   ├── roles.ts
│   │   ├── labeling.ts
│   │   ├── ownerTasks.ts
│   │   └── dashboard.ts
│   ├── App.tsx
│   ├── main.tsx
│   └── index.css
├── .env                        # 环境变量
├── vite.config.ts
├── tsconfig.json
└── package.json
```

---

## 5. 架构分层

前端采用**四层架构**：

```
应用层 (app/)         路由、布局、守卫
业务层 (pages/ + features/)  页面组件 + 业务域模块
能力层 (components/ + hooks/)  共享组件 + 通用 Hooks
基础设施层 (services/ + stores/ + types/ + utils/)  服务、状态、类型、工具
```

### 应用层 — `app/`

负责路由初始化、布局装配、角色路由守卫。

- **`router.tsx`** — 定义所有路由，按角色分组（`/app/admin`、`/app/owner`、`/app/labeler`、`/app/reviewer`），入口自动跳转至角色首页
- **`AppLayout.tsx`** — 全局布局：顶部导航栏 + 面包屑 + 侧边栏 + 内容区域
- **`RequireAuth.tsx`** — 认证守卫，未登录重定向到 `/login`
- **`PublicOnlyRoute.tsx`** — 公开路由守卫，已登录跳转到角色首页

### 业务层 — `pages/` + `features/`

页面组件按角色分包，每个角色的页面独立目录：

```
pages/
  admin/        管理端
  auth/         认证
  labeler/      标注员
  owner/        任务负责人
  reviewer/     审核员
  roles/        角色通用
```

`features/` 存放跨页面复用的业务域模块，当前只有 `dynamic-form/`。

### 能力层 — `components/` + `hooks/`

按用途分类：

- `components/navigation/` — 导航相关组件
- `components/page/` — 页面结构组件（ContentShell、PageHeader）
- `components/states/` — 状态占位组件
- `hooks/useRoleNavigation.ts` — 角色导航 Hook

### 基础设施层

- `services/` — API 封装，支持 Mock/Real 双模式
- `stores/` — Zustand 全局状态
- `types/` — TypeScript 类型定义
- `utils/` — 纯工具函数

---

## 6. 角色系统与路由

### 角色定义

```typescript
type Role = 'ADMIN' | 'OWNER' | 'LABELER' | 'REVIEWER'
```

### 路由结构

```
/login                      # 登录/注册页
/app                        # 需认证，全局布局
  /admin                    # 管理员
    index                   → AdminDashboardPage
    review-assignment       → AdminReviewAssignmentPage
    llm-providers           → AdminLlmProviderPage
  /owner                    # 任务负责人
    index                   → OwnerDashboardPage
    tasks                   → OwnerTasksPage
    tasks/new               → OwnerTaskEditorPage（创建）
    tasks/:taskId/edit      → OwnerTaskEditorPage（编辑）
    templates               → OwnerTemplatesPage
    templates/:templateId/designer → OwnerTemplateDesignerPage
  /labeler                  # 标注员
    index                   → LabelerDashboardPage
    market                  → LabelerMarketPage
    workbench/:taskId       → LabelerWorkbenchPage
    submissions             → LabelerSubmissionsPage
  /reviewer                 # 审核员
    index                   → ReviewerDashboardPage
    claim                   → ReviewerClaimPage
    queue                   → ReviewerQueuePage
    ai-reviews              → ReviewerAiReviewQueuePage
    tasks/:taskId           → ReviewerReviewDetailPage
```

### 导航配置

导航项在 `src/app/navigation.tsx` 中集中定义，每个角色拥有独立的导航菜单。使用 `useRoleNavigation` Hook 获取当前角色的导航项和激活项：

```typescript
const { activeKey, items } = useRoleNavigation()
```

### 路由守卫

- **未登录** → 重定向到 `/login`
- **已登录但访问自身角色以外的路径** → 路由不匹配，兜底到 `RoleHomePage` 或重定向

---

## 7. 数据流

### 整体数据流

```
Page Component → Store (Zustand) → Service Layer → HTTP Client / Mock
                                                       ↓
                                                  Backend API
```

### 分层职责

1. **Page Component** — 调用 Store 的 action，订阅 Store 状态
2. **Store** — 管理加载态/错误态/数据，调用 Service 层，转换数据
3. **Service** — 封装 API 调用逻辑，包含 Mock 和 Real 两套实现
4. **HTTP Client** — Axios 封装，处理 Token 注入、响应解包、错误归一化

### 典型数据流示例（标注员领取任务）

```typescript
// Page: LabelerMarketPage.tsx
const loadMarket = useLabelingStore((state) => state.loadMarket)

useEffect(() => { void loadMarket() }, [])

// Store: labelingStore.ts
loadMarket: async () => {
  set({ isMarketLoading: true })
  const tasks = await labelingService.listMarketTasks(filters)
  set({ marketTasks: tasks, isMarketLoading: false })
}

// Service: labelingService.ts（Mock 模式）
listMarketTasks(query) → 过滤内存中的 tasks 数组 → 返回
```

### Service 双模式

通过 `.env` 中的 `VITE_SERVICE_MODE` 控制：

- **`mock`**：使用 `src/services/*/xxxService.ts` 中的内存 Mock 实现
- **`real`**：使用 `src/services/*/xxxRealService.ts` 或 API 文件中的真实 HTTP 调用

例（标注员服务）：

```typescript
// src/services/labeler/index.ts
export const labelingService = isRealServiceMode()
  ? realLabelingService    // 真实 API 调用
  : mockLabelingService    // 内存 Mock
```

---

## 8. HTTP 客户端

### 基础封装

基于 Axios 的二次封装，位于 `src/services/http/httpClient.ts`。

### 核心特性

- **Token 自动注入** — 从 localStorage 读取 Token，自动添加 `Authorization` 头
- **响应解包** — 自动解包 `ApiResponseEnvelope`（`{ code, message, data }`），仅返回 `data`
- **Token 刷新** — 401 时自动使用 refresh token 刷新，刷新失败清除登录态
- **错误归一化** — 网络错误、服务端异常、业务错误统一转换为 `ApiError`

### API 响应格式

后端标准响应格式：

```typescript
interface ApiResponseEnvelope<T> {
  code: number      // 0 或 200 表示成功
  message?: string
  data?: T
}
```

### 请求方法

```typescript
import { request } from '../services'

// GET
const data = await request.get<T>('/v1/resource')

// POST
const data = await request.post<T>('/v1/resource', payload)

// PUT / PATCH / DELETE
await request.put<T>('/v1/resource/:id', payload)
await request.patch<T>('/v1/resource/:id', payload)
await request.delete<T>('/v1/resource/:id')
```

### 错误处理

```typescript
import { ApiError } from '../services'

try {
  await request.get('/v1/resource')
} catch (error) {
  if (error instanceof ApiError) {
    console.log(error.code, error.message, error.status)
  }
}
```

---

## 9. 状态管理

### 选型

使用 **Zustand 5** 进行全局状态管理，共 12 个 Store。

### Store 模式

所有 Store 遵循统一的模式：

```typescript
interface ExampleStore {
  // 数据
  items: Item[]
  filters: Filter

  // 加载态
  isLoading: boolean
  error: string | null

  // Action
  loadItems: () => Promise<void>
  setFilters: (filters: Partial<Filter>) => void
}

export const useExampleStore = create<ExampleStore>((set, get) => ({
  items: [],
  filters: initialFilters,
  isLoading: false,
  error: null,

  setFilters: (filters) => {
    set((state) => ({ filters: { ...state.filters, ...filters } }))
  },

  loadItems: async () => {
    set({ isLoading: true, error: null })
    try {
      const items = await exampleService.list(filters)
      set({ items })
    } catch {
      set({ error: '加载失败' })
    } finally {
      set({ isLoading: false })
    }
  },
}))
```

### 约定

- **数据 + 加载态 + 错误态** — 每个数据字段都有对应的 `isLoading` 和 `error`
- **Action 统一 try/catch** — 所有异步 action 都包裹 try/catch，在 catch 中设置错误信息
- **`set` 和 `get` 分离** — 读取状态用 `get()`，更新用 `set()`
- **筛选条件独立** — `filters` 对象独立存储，修改后触发的异步加载在 Page 层调用

### Store 清单

| Store | 用途 | 状态量 |
|-------|------|--------|
| `authStore` | 登录态、角色、Token | ~15 |
| `labelingStore` | 标注工作台、任务广场、提交 | ~25 |
| `reviewStore` | 审核队列、详情、AI 审核 | ~35 |
| `ownerTaskStore` | 任务列表、详情、数据项 | ~20 |
| `ownerDraftStore` | 任务草稿 | ~10 |
| `templateDesignerStore` | 模板设计器 | ~15 |
| `adminDashboardStore` | 管理员看板 | ~8 |
| `ownerDashboardStore` | 任务负责人看板 | ~8 |
| `labelerDashboardStore` | 标注员看板 | ~8 |
| `reviewerDashboardStore` | 审核员看板 | ~8 |
| `navigationStore` | 导航折叠状态 | ~3 |
| `pageUiStore` | 页面 UI 状态 | ~3 |

---

## 10. 动态表单系统

这是 LabelHub 的核心能力，位于 `src/features/dynamic-form/`。

### 架构

```
materialRegistry.ts   物料注册表（13 种字段类型定义）
         │
         ▼
   Designer           拖拽编排、属性编辑、结构调整
   ─────────
   createSchemaNodeFromMaterial()
   insertSchemaNode() / deleteSchemaNode() / reorderSchemaNodes()
   updateSchemaNode()
         │
         ▼
   Renderer           按照 Schema 运行时渲染表单
   ─────────
   DynamicFormRenderer.tsx    → Formily 适配渲染
   rendererFields.tsx         → 字段组件映射
```

### Schema 模型

```typescript
interface DynamicFormSchema {
  id: string          // Schema ID
  version: string     // 版本号
  title: string       // 表单标题
  nodes: DynamicSchemaNode[]  // 字段节点树
}

interface DynamicSchemaNode {
  id: string
  key: string
  type: DynamicFieldType   // 字段类型
  title: string            // 字段标题
  defaultValue?: unknown
  props: Record<string, unknown>  // 属性
  rules?: DynamicValidationRule[]  // 校验规则
  visibleWhen?: DynamicVisibleRule  // 显隐条件
  linkage?: DynamicLinkageRule       // 联动规则
  children?: DynamicSchemaNode[]     // 子节点（group/tabs）
}
```

### 物料类型（13 种）

| 分组 | 类型 | 说明 |
|------|------|------|
| 文本 | `input` | 单行输入 |
| 文本 | `textarea` | 多行输入 |
| 文本 | `richText` | 富文本 |
| 选择 | `radio` | 单选 |
| 选择 | `checkbox` | 多选 |
| 选择 | `select` | 标签选择 |
| 展示 | `showItem` | 展示项（只读） |
| 展示 | `jsonEditor` | JSON 编辑器 |
| 媒体 | `fileUpload` | 文件/图片上传 |
| 智能 | `llmPrompt` | LLM 交互 |
| 结构 | `group` | 分组容器 |
| 结构 | `tabs` / `tabPane` | Tab 容器 |

### 渲染流程

1. 读取模板 Schema
2. 解析节点树，构建 Formily Schema（`formilySchema.ts`）
3. 应用字段联动与条件显隐（`visibleWhen` / `linkage`）
4. 渲染运行态表单（`DynamicFormRenderer.tsx`）
5. 执行运行时校验（`rules`）
6. 输出结构化提交结果（`DynamicFormSubmitResult`）

### 设计器

`OwnerTemplateDesignerPage.tsx` 提供可视化拖拽设计器：

- **物料面板** — 左侧可拖拽的字段类型列表
- **画布** — 拖拽放置节点、调整顺序、删除
- **属性面板** — 选中节点后编辑标题、属性、校验规则
- **保存** — 草稿模板直接创建，已有模板 fork 新版本

### 校验规则

```typescript
type DynamicValidationRule =
  | { type: 'required'; message?: string }
  | { type: 'minLength'; value: number; message?: string }
  | { type: 'maxLength'; value: number; message?: string }
  | { type: 'enum'; values: Array<string | number | boolean>; message?: string }
```

---

## 11. 业务模块

### 11.1 标注工作流（Labeler）

```
任务广场 → 领取任务 → 标注工作台 → 作答 → 提交 → 查看结果
                              ↓
                          自动保存草稿
                              ↓
                        打回 → 修改 → 重新提交
```

关键页面：

- `LabelerMarketPage.tsx` — 任务广场，支持搜索和领取
- `LabelerWorkbenchPage.tsx` — 标注工作台，题目导航 + 动态表单 + 草稿自动保存（30s 防抖）
- `LabelerSubmissionsPage.tsx` — 我的提交列表

关键服务：`labelingService.ts`（Mock）/ `labelingRealService.ts`（Real）

### 11.2 审核工作流（Reviewer）

```
认领待审 → 审核队列 → 查看详情 → AI 审核参考 → 人工复核
                                                ↓
                                         通过 / 打回
                                                ↓
                                         批量处理
```

关键页面：

- `ReviewerClaimPage.tsx` — 认领待审提交
- `ReviewerQueuePage.tsx` — 审核任务队列
- `ReviewerAiReviewQueuePage.tsx` — AI 审核日志队列
- `ReviewerReviewDetailPage.tsx` — 审核详情（答案展示、AI 结果、版本历史、审核操作）

关键服务：`reviewService.ts`，融合 API 调用和本地运行时（`reviewLocalRuntime.ts`）

### 11.3 任务管理（Owner）

```
创建任务 → 选择/搭建模板 → 配置规则 → 发布 → 追踪进度 → 导出数据
                                                          ↓
                                                    暂停 / 结束
```

关键页面：

- `OwnerTasksPage.tsx` — 任务列表，支持筛选、发布、暂停、结束、导出
- `OwnerTaskEditorPage.tsx` — 任务创建/编辑（基本信息 + AI 审核配置 + 数据导入）
- `OwnerTemplatesPage.tsx` — 模板列表
- `OwnerTemplateDesignerPage.tsx` — 模板设计器

### 11.4 数据看板

四种角色各有数据看板，展示 KPI、趋势图、排行等：

- `AdminDashboardPage.tsx` — 平台总览（活跃任务、提交趋势、标注员排行）
- `OwnerDashboardPage.tsx` — 任务负责人看板
- `LabelerDashboardPage.tsx` — 标注员看板
- `ReviewerDashboardPage.tsx` — 审核员看板

### 11.5 管理后台（Admin）

- `AdminReviewAssignmentPage.tsx` — 审核任务分配（查看审核员负载、指派任务）
- `AdminLlmProviderPage.tsx` — LLM Provider 管理（增删改查、连接测试）
- `AdminDashboardPage.tsx` — 管理端包含创建审核员账号功能

---

## 12. 类型系统

### 核心领域类型

| 文件 | 核心类型 | 说明 |
|------|----------|------|
| `auth.ts` | `Role`, `User`, `AuthState` | 认证与角色 |
| `task.ts` | `OwnerTask`, `TaskDraft`, `TaskProgress` | 任务领域 |
| `review.ts` | `ReviewDetail`, `ReviewQueueItem`, `SubmissionReviewStatus` | 审核领域 |
| `labeling.ts` | `LabelerTaskSummary`, `LabelingQuestion`, `LabelingSubmission` | 标注领域 |
| `template.ts` | `TemplateDetail`, `TemplateVersionSnapshot` | 模板领域 |
| `dynamicForm.ts` | `DynamicFormSchema`, `DynamicSchemaNode`, `DynamicFieldType` | 动态表单核心模型 |
| `dashboard.ts` | `OwnerDashboardOverview`, `LabelerDashboardOverview`, `ReviewerDashboardOverview` | 看板数据 |
| `admin.ts` | `AdminDashboardOverview`, `AdminCreateReviewerRequest` | 管理员数据 |
| `adminReviewAssignment.ts` | `AssignableTask`, `AssignableReviewer`, `ReviewerProgress` | 审核分配 |
| `import.ts` | `ImportPreview`, `ImportIssue` | 数据导入 |
| `llm.ts` | `LlmTriggerRunRequest`, `LlmTriggerRunResponse` | LLM 调用 |
| `llmProvider.ts` | `LlmProviderResponse`, `LlmProviderUpsertRequest` | LLM 提供商配置 |
| `navigation.ts` | `NavItem` | 导航项 |
| `page.ts` | `PageStateKind`, `PageState` | 页面状态 |

### 状态枚举

标注提交审核状态流转：

```
ai_pending → ai_passed → manual_pending → manual_approved
           → ai_rejected                  → manual_rejected
```

```typescript
type SubmissionReviewStatus =
  | 'ai_pending'     // AI 审核中
  | 'ai_passed'      // AI 通过
  | 'ai_rejected'    // AI 打回
  | 'manual_pending' // 待人工审核
  | 'manual_approved' // 人工通过
  | 'manual_rejected' // 人工打回
```

---

## 13. Mock 机制

### 目的

开发时无需启动后端，使用内存 Mock 数据完成功能开发。

### 目录结构

```
src/mocks/
  index.ts                   统一导出
  labeling.mock.ts           标注相关 Mock 数据
  review.mock.ts             审核相关 Mock 数据
  tasks.mock.ts              任务 Mock 数据
  templates.mock.ts          模板 Mock 数据
  ownerDashboard.mock.ts     任务负责人看板 Mock
  imports.mock.ts            导入 Mock 数据
```

### 实现方式

Mock 服务与真实服务共享相同的接口签名：

```typescript
// 接口定义
interface LabelingService {
  listMarketTasks(query: LabelerTaskListQuery): Promise<LabelerTaskSummary[]>
  claimTask(taskId: string, options: LabelerClaimOptions): Promise<...>
  // ...
}

// Mock 实现（内存操作）
const mockLabelingService: LabelingService = { ... }

// Real 实现（HTTP 调用）
const realLabelingService: LabelingService = { ... }

// 选择器
export const labelingService = isRealServiceMode()
  ? realLabelingService
  : mockLabelingService
```

### 跨服务联动

Mock 模式下审核系统和标注系统可以联动。例如审核员打回一个提交时，标注员的 Mock 数据也会同步更新：

```typescript
// reviewService.ts
reviewService.registerReviewOutcomeSync(applyReviewOutcomeToLabelingState)
```

---

## 14. 开发约定

### 文件命名

- React 组件：`PascalCase.tsx`（如 `LabelerMarketPage.tsx`）
- 工具函数/非组件：`camelCase.ts`（如 `schemaTree.ts`）
- CSS Modules：与组件同名，后缀 `.module.css`
- 类型文件：`camelCase.ts`（如 `auth.ts`）

### 导入顺序

1. 外部库（react, antd, axios 等）
2. 项目内部模块（相对路径导入）
3. 类型导入（使用 `import type`）

### 组件约定

- **函数组件**，不使用 class 组件
- **命名导出**，不使用 default export（`App.tsx` 除外）
- **Props 类型**定义在组件文件内，复杂类型抽到 `types/`

### Store 约定

- 每个 Store 定义 `interface StoreName { ... }` 类型
- 创建时使用 `create<StoreName>((set, get) => ({ ... }))`
- 所有异步 action 遵循 `set(loading) → try/await → catch(error) → finally(loading)`

### 服务层约定

- 每个业务域独立目录（`services/admin/`、`services/labeler/` 等）
- 统一的 barrel export（`index.ts`）
- Mock 和 Real 实现通过 `isRealServiceMode()` 切换

### CSS 约定

- 使用 CSS Modules（`.module.css`）
- 全局样式在 `src/index.css`
- 页面级样式使用 CSS Modules 隔离
