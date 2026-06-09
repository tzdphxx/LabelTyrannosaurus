# LabelHub 前端技术文档

> 项目版本: 0.0.0 | 最后更新: 2026-06-09

---

## 目录

1. [总体架构](#1-总体架构)
2. [技术栈](#2-技术栈)
3. [核心技术难点和挑战](#3-核心技术难点和挑战)

---

## 1. 总体架构

### 1.1 项目定位

LabelHub 是一个 **AI 辅助的数据标注平台**，覆盖 **任务管理 → 动态表单搭建 → 数据标注 → AI 预审 → 人工复核 → 数据导出** 全链路闭环。

平台定义四种角色，每种角色拥有独立的工作台和权限边界：

| 角色 | 标识 | 核心职责 |
|------|------|----------|
| 管理员 | `ADMIN` | 平台运营看板、审核分配、LLM Provider 管理 |
| 任务负责人 | `OWNER` | 创建任务、搭建标注模板、配置 AI 审核规则、发布分发、导出 |
| 标注员 | `LABELER` | 领取任务、Schema 表单作答、草稿保存与提交、打回修改 |
| 审核员 | `REVIEWER` | 认领待审、AI 审核结果参考、人工复核、批量处理 |

核心能力特征：

- **Schema 驱动** — 标注表单由 JSON Schema 定义，支持拖拽可视化搭建和运行时动态渲染
- **AI 原生集成** — 后端 AI 审核结果在前端以可解释面板展示，包含评分维度、风险标记和降级兜底
- **全链路可追溯** — 草稿自动保存、多版本提交记录、审核操作审计留痕

---

### 1.2 系统边界

#### 前端负责

- 登录认证与角色识别，路由级别的权限守卫
- 任务/模板/标注/审核/看板的全部交互界面
- 动态表单的设计器（拖拽编排）与渲染器（运行时表单）
- 草稿的本地缓存与自动保存
- AI 审核结果的结构化展示与状态提示
- 错误态、空态、加载态、无权限态的用户体验

---

### 1.3 四层架构

前端采用 **应用层 / 业务层 / 能力层 / 基础设施层** 的四层架构，层间单向依赖：

```
┌─────────────────────────────────────────────────────┐
│                    应用层 (app/)                      │
│            路由初始化 · 布局装配 · 权限守卫            │
├─────────────────────────────────────────────────────┤
│                    业务层 (pages/ + features/)        │
│             页面组件 · 业务域模块（动态表单）           │
├─────────────────────────────────────────────────────┤
│                能力层 (components/ + hooks/)          │
│           共享组件 · 通用 Hooks · 导航/页面/状态组件   │
├─────────────────────────────────────────────────────┤
│             基础设施层 (services/ + stores/ + utils/)  │
│            API 封装 · 状态管理 · 工具函数 · 类型定义    │
└─────────────────────────────────────────────────────┘
```

**应用层** — `src/app/`

- `router.tsx`：集中定义全部路由，按角色分组（`/app/admin`、`/app/owner`、`/app/labeler`、`/app/reviewer`）
- `AppLayout.tsx`：全局布局（顶部导航 + 面包屑 + 侧边栏 + 内容区）
- `RequireAuth.tsx` / `PublicOnlyRoute.tsx`：认证与公开路由守卫

**业务层** — `src/pages/` + `src/features/`

页面按角色分包，职责单一：

```
pages/
  admin/      管理员页面（看板 · LLM Provider · 审核分配）
  auth/       登录/注册
  labeler/    标注员页面（看板 · 任务广场 · 工作台 · 我的提交）
  owner/      任务负责人页面（看板 · 任务列表 · 任务编辑 · 模板管理 · 模板设计器）
  reviewer/   审核员页面（看板 · 认领 · 队列 · AI 审核 · 审核详情）
```

`features/dynamic-form/` 是跨角色复用的核心业务模块，独立为 Feature。

**能力层** — `src/components/` + `src/hooks/`

- `components/navigation/` — 导航相关组件（SideNav、TopNav、BreadcrumbNav、RoleBadge）
- `components/page/` — 页面骨架组件（ContentShell、PageHeader）
- `components/states/` — 四种状态占位组件（StatePlaceholder）
- `hooks/useRoleNavigation.ts` — 角色导航 Hook

**基础设施层** — `src/services/` + `src/stores/` + `src/types/` + `src/utils/`

- `services/` — API 封装层，支持 Mock/Real 双模式切换
- `stores/` — 12 个 Zustand Store，统一 loading/error 模式
- `types/` — 14 个领域类型文件
- `utils/` — 纯工具函数

---

### 1.4 项目目录结构

```
frontend/
├── src/
│   ├── app/                     # 应用层
│   │   ├── guards/              # 路由守卫
│   │   │   ├── RequireAuth.tsx
│   │   │   └── PublicOnlyRoute.tsx
│   │   ├── layout/
│   │   │   └── AppLayout.tsx
│   │   ├── navigation.tsx       # 四角色导航配置
│   │   └── router.tsx           # 路由表
│   ├── components/              # 能力层
│   │   ├── navigation/          # SideNav, TopNav, BreadcrumbNav, RoleBadge
│   │   ├── page/                # ContentShell, PageHeader
│   │   └── states/              # StatePlaceholder
│   ├── features/                # 业务层
│   │   └── dynamic-form/        # 核心：动态表单（设计器 + 渲染器）
│   ├── hooks/
│   │   └── useRoleNavigation.ts
│   ├── mocks/                   # 7 个 Mock 数据文件
│   ├── pages/                   # 业务层（按角色分包，共 15 个页面）
│   │   ├── admin/               # 3 pages
│   │   ├── auth/                # 1 page
│   │   ├── labeler/             # 4 pages
│   │   ├── owner/               # 5 pages
│   │   ├── reviewer/            # 5 pages
│   │   └── roles/               # 1 page
│   ├── services/                # 基础设施层
│   │   ├── http/                # Axios 封装基础设施
│   │   ├── admin/               # 5 个服务文件
│   │   ├── auth/                # 认证服务
│   │   ├── labeler/             # 标注员服务（Mock + Real 双实现）
│   │   ├── owner/               # 7 个服务文件
│   │   ├── review/              # 审核服务（API + 本地运行时）
│   │   └── llm/                 # LLM 服务
│   ├── stores/                  # 基础设施层（12 个 Zustand Store）
│   ├── types/                   # 基础设施层（14 个类型文件）
│   └── utils/                   # 基础设施层（4 个工具文件）
├── .env
├── vite.config.ts
├── tsconfig.json
└── package.json
```

---

### 1.5 角色系统与路由架构

路由按角色分组，同一角色下的页面共享全局布局（侧边栏 + 顶部导航）：

```
/login                              # 登录/注册（公开）
/app                                # 全局布局（需认证）
  /admin                            # 管理员
    ├── /                           → AdminDashboardPage
    ├── review-assignment           → AdminReviewAssignmentPage
    └── llm-providers               → AdminLlmProviderPage
  /owner                            # 任务负责人
    ├── /                           → OwnerDashboardPage
    ├── tasks                       → OwnerTasksPage
    ├── tasks/new                   → OwnerTaskEditorPage（创建）
    ├── tasks/:id/edit              → OwnerTaskEditorPage（编辑）
    ├── templates                   → OwnerTemplatesPage
    └── templates/:id/designer      → OwnerTemplateDesignerPage
  /labeler                          # 标注员
    ├── /                           → LabelerDashboardPage
    ├── market                      → LabelerMarketPage
    ├── workbench/:id               → LabelerWorkbenchPage
    └── submissions                 → LabelerSubmissionsPage
  /reviewer                         # 审核员
    ├── /                           → ReviewerDashboardPage
    ├── claim                       → ReviewerClaimPage
    ├── queue                       → ReviewerQueuePage
    ├── ai-reviews                  → ReviewerAiReviewQueuePage
    └── tasks/:id                   → ReviewerReviewDetailPage
```

导航项在 `src/app/navigation.tsx` 中集中配置，每个角色拥有独立的菜单列表。`useRoleNavigation` Hook 自动根据当前角色返回对应的导航项和激活态。

路由守卫链：未登录 → `/login`；已登录 → 角色首页；访问越权路径 → 兜底重定向。

---

### 1.6 数据流架构

数据流动方向为 **单向数据流**：

```
Page Component ──调用──▶ Store (Zustand) ──调用──▶ Service Layer
      ▲                        │                         │
      │                        │                         ├── Real: HTTP Client → Backend API
      │                        │                         └── Mock: 内存操作
      │                        ▼
      └──── 订阅状态 ──── state + loading + error
```

**分层职责：**

| 层 | 职责 |
|----|------|
| Page Component | 调用 Store Action，通过 selector 订阅状态 |
| Store (Zustand) | 管理数据/加载态/错误态，调用 Service，转换数据格式 |
| Service | 封装业务逻辑，决定走 Mock 还是 Real API |
| HTTP Client | Axios 封装，处理 Token、响应解包、错误归一化 |

**典型数据流示例**（标注员领取任务）：

```typescript
// 1. Page 调用 Store Action
const loadMarket = useLabelingStore((state) => state.loadMarket)
useEffect(() => { void loadMarket() }, [])

// 2. Store 管理加载态并调用 Service
loadMarket: async () => {
  set({ isMarketLoading: true })
  try {
    const tasks = await labelingService.listMarketTasks(filters)
    set({ marketTasks: tasks })
  } catch {
    set({ error: '任务广场加载失败' })
  } finally {
    set({ isMarketLoading: false })
  }
}

// 3. Service 根据环境变量选择实现
export const labelingService = isRealServiceMode()
  ? realLabelingService    // HTTP 调用
  : mockLabelingService    // 内存 Mock
```

---

### 1.7 业务模块划分

项目分为五条核心业务线：

```
┌─────────────────────────────────────────────────────────────┐
│                      数据看板（Dashboard）                    │
│  ADMIN 平台看板 │ OWNER 任务看板 │ LABELER 标注看板 │ REVIEWER 审核看板 │
├─────────────────────────────────────────────────────────────┤
│                       任务管理（Owner）                       │
│      创建 → 配置模板 → 配置 AI 审核 → 导入数据 → 发布         │
├─────────────────────────────────────────────────────────────┤
│                     标注工作流（Labeler）                      │
│     领取任务 → 动态表单作答 → 草稿自动保存 → 提交 → 修改      │
├─────────────────────────────────────────────────────────────┤
│                     审核工作流（Reviewer）                     │
│   认领 → AI 审核参考 → 人工复核 → 通过/打回 → 批量处理       │
├─────────────────────────────────────────────────────────────┤
│                     管理后台（Admin）                         │
│    审核分配 · LLM Provider 管理 · 审核员账号 · 平台看板       │
└─────────────────────────────────────────────────────────────┘
```

| 模块 | 核心页面 | 涉及角色 |
|------|----------|----------|
| 数据看板 | 4 个 DashboardPage | 全部 |
| 任务管理 | TasksPage / TaskEditorPage | OWNER |
| 模板管理 | TemplatesPage / TemplateDesignerPage | OWNER |
| 标注工作台 | MarketPage / WorkbenchPage / SubmissionsPage | LABELER |
| 审核管理 | ClaimPage / QueuePage / AiReviewQueuePage / ReviewDetailPage | REVIEWER |
| 管理后台 | DashboardPage / LlmProviderPage / ReviewAssignmentPage | ADMIN |

---

## 2. 技术栈

### 2.1 技术选型总览

| 类别 | 技术 | 版本 | 用途 |
|------|------|------|------|
| 框架 | React | 19 | 组件化 UI 构建 |
| 语言 | TypeScript | ~6.0 | 类型安全 |
| 构建工具 | Vite | 8 | 极速开发服务器与生产打包 |
| UI 组件库 | Ant Design | 5 | 企业级组件体系 |
| 图表库 | @ant-design/plots | 2.6 | 看板 KPI 趋势图与分布图 |
| 表单引擎 | Formily (antd-v5) | 1.2 | Schema 驱动的动态表单渲染 |
| 状态管理 | Zustand | 5 | 轻量级全局状态 |
| 路由 | react-router | 7 | 声明式路由 |
| 拖拽 | @dnd-kit | 6/10 | 模板设计器拖拽编排 |
| HTTP | Axios | 1.16 | HTTP 客户端 |
| 日期处理 | dayjs | 1.11 | 日期格式化与计算 |
| 表格导出 | xlsx | 0.18 | XLSX 生成 |

### 2.2 核心框架选型理由

| 决策 | 选择 | 替代方案 | 理由 |
|------|------|----------|------|
| UI 框架 | React 19 | Vue 3, Svelte | 团队熟悉度优先；React 19 的并发特性为大表单场景提供更好的渲染控制 |
| 语言 | TypeScript 6 | JavaScript | 动态表单 Schema 模型复杂（13 种字段类型 × 多层嵌套），TS 的类型系统可以在编译期捕获结构错误；14 个类型文件构成的领域模型需要严格类型约束 |
| 构建工具 | Vite 8 | Webpack, Turbopack | Vite 基于 ESM 的 HMR 在大型项目中仍保持毫秒级响应；配置简洁，生态成熟 |
| 组件库 | Ant Design 5 | Material UI, Arco | 面向企业后台场景，数据表格/表单/筛选器等能力开箱即用；中文文档完善 |

### 2.3 UI 体系

UI 方案采用 **Ant Design 5 + CSS Modules**：

- **Ant Design 5** 提供表格（Table）、表单（Form）、卡片（Card）、选择器（Select）、消息提示（message）等企业级组件，覆盖 90% 的页面构建需求
- **CSS Modules** 用于页面级和组件级样式隔离，避免类名冲突；全局样式（Reset、CSS 变量）放在 `src/index.css`
- 不引入 Tailwind CSS 的原因：项目以数据表格和表单为主，页面布局高度结构化，Tailwind 的原子化类名在复杂表单场景下可读性差且难以维护

### 2.4 状态管理

使用 **Zustand 5**，共 12 个 Store。

**为什么选择 Zustand 而非 Redux 或 Context：**

| 对比项 | Zustand | Redux Toolkit | React Context |
|--------|---------|---------------|---------------|
| 模板代码 | 极少 | 较多（slice + reducer + action） | 较少 |
| TypeScript 支持 | 天然良好 | 良好 | 需额外类型定义 |
| 异步 Action | 直接 async/await | createAsyncThunk | 无内置方案 |
| 性能 | 按 selector 订阅 | 按 selector 订阅 | 整个子树重渲染 |
| Bundle 体积 | ~1KB | ~11KB | 0 |

**Store 统一模式：**

每个 Store 遵循相同的契约结构：

```
interface XxxStore {
  // 数据
  items: Xxx[]
  filters: XxxFilter

  // 加载态 + 错误态
  isLoading: boolean
  error: string | null

  // Action
  loadItems: () => Promise<void>
  setFilters: (filters: Partial<XxxFilter>) => void
}
```

所有异步 Action 统一遵循 `set(loading) → try/catch → finally(!loading)` 模式：

```typescript
loadItems: async () => {
  set({ isLoading: true, error: null })
  try {
    const items = await service.list(filters)
    set({ items })
  } catch {
    set({ error: '加载失败' })
  } finally {
    set({ isLoading: false })
  }
}
```

### 2.5 HTTP 客户端封装

基于 Axios 的二次封装（`src/services/http/httpClient.ts`），解决以下通用问题：

**响应自动解包** — 后端统一包装为 `ApiResponseEnvelope`：

```typescript
interface ApiResponseEnvelope<T> {
  code: number       // 0 或 200 表示成功
  message?: string
  data?: T
}

// 响应拦截器自动解包，Page 层直接拿到 T
const result = await request.get<OwnerTask>('/v1/tasks/1')
// result 的类型是 OwnerTask，而非 ApiResponseEnvelope<OwnerTask>
```

**Token 自动管理** — 请求拦截器注入 `Authorization` 头；响应 401 时自动使用 refresh token 重试，重试失败清除登录态。

**错误归一化** — 网络错误、HTTP 错误、业务错误统一转换为 `ApiError` 类型：

```typescript
class ApiError extends Error {
  code?: number | string
  status?: number
  url?: string
  method?: string
  details?: unknown
}
```

### 2.6 表单引擎

动态表单渲染选用 **Formily**（`@formily/core` + `@formily/react` + `@formily/antd-v5`）。

选型理由：

- **Schema 驱动** — Formily 原生支持 JSON Schema 到表单控件的映射，天然适合 "设计器产出 Schema → 渲染器消费 Schema" 的架构
- **联动能力** — 内置 `visible`、`reactions` 等联动机制，能满足条件显隐、选项级联、跨字段校验等需求
- **开箱即用的 Ant Design 桥接** — `@formily/antd-v5` 提供了与 Ant Design 5 的完整适配

Formily 在整个项目中的定位是**运行时渲染引擎**，而上层的 Schema 模型由项目自定义的 `DynamicFormSchema` 定义。两者通过 `formilySchema.ts` 适配层转换。

### 2.7 其他关键依赖

| 依赖 | 使用场景 |
|------|----------|
| `@dnd-kit/core` + `@dnd-kit/sortable` | 模板设计器中字段节点的拖拽排序与结构调整 |
| `@ant-design/plots` | 四角色看板中的折线图（提交趋势）、柱状图（任务分布） |
| `xlsx` | 任务数据导出为 XLSX 格式 |
| `dayjs` | 日期展示、时间范围计算 |

---

## 3. 核心技术难点和挑战

### 3.1 Schema 驱动的动态表单系统

**这是整个平台最核心的技术挑战。** 标注表单不能是硬编码的，必须支持业务方（任务负责人）通过拖拽自行定义字段结构，然后标注员在运行时看到对应的表单。

#### 挑战一：一套 Schema 模型兼顾设计与渲染

我们设计了 `DynamicFormSchema` 作为设计器和渲染器的**唯一契约**，确保两端对同一份数据的理解一致：

```typescript
interface DynamicFormSchema {
  id: string
  version: string
  title: string
  nodes: DynamicSchemaNode[]
}

interface DynamicSchemaNode {
  id: string
  key: string                    // 数据字段标识
  type: DynamicFieldType         // 字段类型
  title: string                  // 字段标题
  props: Record<string, unknown> // 属性（placeholder、options 等）
  rules?: DynamicValidationRule[] // 校验规则
  visibleWhen?: DynamicVisibleRule // 条件显隐
  linkage?: DynamicLinkageRule   // 联动规则
  children?: DynamicSchemaNode[]  // 子节点（group/tabs）
}
```

#### 挑战二：13 种字段类型的注册与管理

字段类型分为 6 组 13 种，通过 `materialRegistry.ts` 集中注册：

| 分组 | 类型 | 用途 |
|------|------|------|
| 文本 | `input` / `textarea` / `richText` | 短文本、长文本、富文本输入 |
| 选择 | `radio` / `checkbox` / `select` | 单选、多选、标签选择 |
| 展示 | `showItem` / `jsonEditor` | 只读展示、JSON 调试 |
| 媒体 | `fileUpload` | 图片/文件上传 |
| 智能 | `llmPrompt` | 字段级 LLM 交互（AI 辅助填充） |
| 结构 | `group` / `tabs` / `tabPane` | 容器布局 |

每种类型注册为 `MaterialDefinition`，包含默认属性、校验规则、父子约束。扩展新字段类型只需在注册表中新增一条记录。

#### 挑战三：Designer / Renderer 分离

```
物料注册表 (materialRegistry.ts)
       │
       ▼
  ┌───── 设计器 (Designer) ─────┐
  │  拖拽编排 · 属性编辑 · 结构调整  │
  │  create/insert/delete/reorder  │
  └───────────┬──────────────────┘
              │ 输出 DynamicFormSchema
              ▼
  ┌───── 渲染器 (Renderer) ──────┐
  │    schema → Formily Schema   │
  │    → 运行时表单 → 校验 → 提交  │
  └──────────────────────────────┘
```

设计器和渲染器是**两个独立的应用**，设计器面向任务负责人（`OwnerTemplateDesignerPage`），渲染器嵌入标注员工作台（`LabelerWorkbenchPage`），但它们共享同一套 Schema 模型和物料注册表。

#### 挑战四：Formily 适配层

Formily 的 Schema 结构与项目自定义的 `DynamicFormSchema` 不同，需要适配层（`formilySchema.ts`）做双向转换：

```
DynamicSchemaNode  ──→  Formily ISchema
    {                       {
      type: 'input',           type: 'string',
      key: 'name',             name: 'name',
      title: '姓名',            title: '姓名',
      props: {                  'x-component': 'Input',
        placeholder: '请输入'     'x-component-props': {
      }                           placeholder: '请输入'
      rules: [ ... ]           },
    }                          'x-validator': [ ... ]
                             }
```

#### 挑战五：Schema 树操作

`schemaTree.ts` 实现了对嵌套节点树的核心操作：

- **查找节点** — `findSchemaNode()` 递归遍历
- **更新节点** — `updateSchemaNode()` 不可变方式更新（返回新 Schema 对象）
- **插入/删除** — `insertSchemaNode()` / `deleteSchemaNode()`
- **拖拽重排** — `reorderSchemaNodes()` 同级节点排序
- **校验** — `validateDynamicSchema()` 校验字段 key 唯一性、必填 title 等

所有操作都遵循**不可变数据**原则，每次操作返回新的 `DynamicFormSchema` 引用，确保 Zustand 的状态比较正确触发重渲染。

---


### 3.3 复杂审核状态流转

#### 挑战

标注提交的审核状态涉及 **AI 预审 + 多轮人工复核**，状态组合复杂：

```
                    ┌──────────┐
                    │  AI 审核  │
                    └────┬─────┘
                    ┌────┴────┐
                    ▼         ▼
              ┌─────────┐ ┌──────────┐
              │ AI 通过  │ │ AI 打回  │
              └────┬────┘ └────┬─────┘
                   │           │
                   ▼           ▼
              ┌──────────┐ ┌──────────┐
              │ 人工待审  │ │ 已打回    │
              └────┬─────┘ └──────────┘
              ┌────┴────┐
              ▼         ▼
        ┌─────────┐ ┌─────────┐
        │ 人工通过 │ │ 人工打回 │
        └─────────┘ └─────────┘
```

对应的类型定义：

```typescript
type SubmissionReviewStatus =
  | 'ai_pending'       // AI 审核中
  | 'ai_passed'        // AI 通过 → 进入人工待审
  | 'ai_rejected'      // AI 打回
  | 'manual_pending'   // 待人工审核
  | 'manual_approved'  // 人工通过
  | 'manual_rejected'  // 人工打回
```

#### 方案

- 前端以 `SubmissionReviewStatus` 作为**单一状态源**，所有组件消费这个枚举做界面展示
- 审核详情页合并展示 AI 审核结果和人工复核记录，通过 `ReviewDetail` 类型统一承载
- 审核历史通过 `versionHistory`（版本历史）+ `reviewRecords`（审核操作记录）+ `aiReviewResult`（AI 结果）三条时间线完整呈现
- 状态展示使用不同的 Tag 颜色和徽标，让审核员一眼识别当前状态


---

### 3.5 标注草稿自动保存机制

#### 挑战

标注员在工作台上可能花费较长时间填写表单，需要**自动保存**防止数据丢失，但保存频率不能太高（避免性能开销和内存写入频繁）。

#### 方案

```typescript
// LabelerWorkbenchPage.tsx — 30 秒防抖自动保存
useEffect(() => {
  if (!hasUnsavedChanges) return

  const timer = window.setTimeout(() => {
    saveDraft({ taskId, questionId, userId, values })
  }, 30000)  // 30 秒无操作后自动保存

  return () => window.clearTimeout(timer)
}, [values, hasUnsavedChanges])
```

**签名对比优化** — 使用 `stringifyDraftValues()` 将 draft 对象序列化后做字符串签名比对，避免冗余保存：

```typescript
const savedSignature = stringifyDraftValues(draft.values)
savedValuesSignatureRef.current = savedSignature
// 只有签名变化时才触发保存
if (valuesSignature !== savedValuesSignatureRef.current) {
  // 执行保存
}
```

这个机制确保：标注员修改表单 → 30 秒静默 → 自动保存；立即点击"保存草稿"按钮 → 立即保存；没有实际修改 → 跳过保存。

---

### 3.6 AI 审核结果展示与降级策略

#### 挑战

AI 审核是异步、不可靠的（模型可能超时、返回格式异常、评分缺失），前端不能假设 AI 结果一定可用。需要设计一套**稳健的展示方案**，让审核员在任何情况下都能完成工作。

#### 方案

**结构化结果面板** — AI 审核结果以可解释面板展示，而非简单的"通过/打回"标签：

| 面板区域 | 内容 |
|----------|------|
| 总体结论 | 通过 / 人工审核 / 打回 |
| 评分维度 | 每个维度的名称、分值、评语 |
| 风险标记 | 高风险 / 中风险 / 低风险 |
| 建议 | AI 给出的审核建议 |
| 原始返回 | Prompt 快照 + 原始 Response（调试用） |

**降级策略**：

| 场景 | 降级行为 |
|------|----------|
| AI 未返回结果 | 展示"AI 审核暂不可用"，直接开放人工审核入口 |
| AI 评分缺失 | 降级为普通审核视图，隐藏评分面板 |
| AI 超时/异常 | 提示"结果暂不可用"，不阻塞审核流程 |
| 模型降级（degraded=true） | 标记"使用降级模型"，展示 limitations 信息 |

```typescript
interface AiReviewResult {
  status: 'pending' | 'completed' | 'failed'
  decision: 'pass' | 'manual_review' | 'reject'
  riskLevel: 'low' | 'medium' | 'high'
  degraded?: boolean     // 是否降级
  limitations?: string   // 降级原因
  // ...
}
```

---

### 3.7 类型系统复杂度管理

#### 挑战

项目涉及 4 种角色、5 条业务线、多种审核状态。14 个类型文件之间存在复杂的引用关系，管理不当会导致类型冗余和循环依赖。

#### 方案

**领域驱动分层** — 类型按业务域划分，每个文件聚焦一个领域：

```
types/
  auth.ts              # 认证与角色（Role, User, AuthState）
  task.ts              # 任务领域（OwnerTask, TaskDraft, TaskProgress）
  review.ts            # 审核领域（ReviewDetail, SubmissionReviewStatus）
  labeling.ts          # 标注领域（LabelerTaskSummary, LabelingQuestion）
  template.ts          # 模板领域（TemplateDetail, TemplateVersion）
  dynamicForm.ts       # 动态表单核心模型（核心中的核心）
  dashboard.ts         # 四角色看板数据
  admin.ts             # 管理员模块
  adminReviewAssignment.ts  # 审核分配
  import.ts            # 数据导入
  llm.ts / llmProvider.ts   # LLM 相关
  navigation.ts / page.ts   # UI 基础设施
```

**核心约束** — `dynamicForm.ts` 不依赖任何业务类型，只依赖 TypeScript 基础类型；业务类型（`task.ts`、`review.ts`、`labeling.ts`）依赖 `dynamicForm.ts`，但不相互依赖，形成有向无环图。

**重复类型的归一化** — 标注系统和审核系统都涉及 `SubmissionReviewStatus`，统一在 `review.ts` 中定义，`labeling.ts` 引用：

```typescript
// review.ts
export type SubmissionReviewStatus = 'ai_pending' | 'ai_passed' | ...

// labeling.ts
import type { SubmissionReviewStatus } from './review'
```

这避免了同一状态在多个文件中重复定义导致的不一致。

---

## 设计原则总结

| 原则 | 体现 |
|------|------|
| **单一职责** | 每个 Service 聚焦一个业务域，Store 聚焦一个数据实体 |
| **接口隔离** | Mock 和 Real 服务共享同一接口签名，可无缝切换 |
| **依赖倒置** | 应用层依赖抽象（Service 接口），不依赖具体实现 |
| **不可变数据** | Schema 树操作每次返回新引用，确保状态变更可追踪 |
| **关注点分离** | Designer 只管 Schema 产出，Renderer 只管 Schema 消费 |
| **降级优先** | AI 审核不可用时不影响核心审核流程 |
