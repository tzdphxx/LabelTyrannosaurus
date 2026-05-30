# 阶段一：基础可跑

## 当前状态

- `P0` 已完成
- `P1` 已完成
- `P2` 未执行

当前实现已经包含登录入口、三类默认账号、角色专属导航、基础工作台壳、核心领域类型、API Client 骨架、认证状态和共享组件。构建与 lint 属于 `P2`，尚未执行。

## 当前实现文件

- 登录入口：`src/pages/auth/LoginPage.tsx`
- 默认账号：`src/services/auth/demoAccounts.ts`
- 应用壳：`src/app/AppShell.tsx`、`src/app/layout/AppLayout.tsx`
- 导航配置：`src/app/navigation.ts`
- 全局状态：`src/stores/appStore.tsx`
- 领域类型：`src/types/domain.ts`、`src/types/api.ts`
- 服务层：`src/services/api/client.ts`、`src/services/task/taskService.ts`、`src/services/template/templateService.ts`、`src/services/review/reviewService.ts`、`src/services/export/exportService.ts`
- 共享组件：`src/components/page/PageHeader.tsx`、`src/components/states/PageState.tsx`、`src/components/feedback/ResultNotice.tsx`、`src/components/shell/DataTableShell.tsx`、`src/components/shell/PanelDrawer.tsx`

## 目标

建立项目的前端基础设施，让应用先具备“能登录、能识别角色、能进入对应工作台、能承接后续业务模块”的最小可用形态。

## 覆盖任务

- 任务 1：初始化前端目录结构
- 任务 2：引入核心依赖并保留渐进接入空间
- 任务 3：建立登录入口、全局布局、角色导航、权限守卫、通用状态
- 任务 4：定义核心领域类型
- 任务 5：建立 API Client 与服务层骨架

## P0 - 必须先完成

### P0.1 目录与工程骨架

建立并固定以下目录：

```text
src/
  app/
  pages/
  features/
  components/
  services/
  stores/
  schemas/
  hooks/
  types/
  utils/
```

同时补齐基础工程配置：

- 路由依赖已进入项目依赖，但阶段一暂未强制接入路由实现
- 状态管理使用 Zustand 承载登录态、导航状态和页面状态
- UI 组件依赖已进入项目依赖，但阶段一先使用原生组件和 CSS 保持轻量
- TypeScript 类型配置
- 统一样式入口

### P0.2 登录入口与应用壳

实现最小可运行的登录和应用框架：

- 登录页
- 三个默认演示账号
- 顶部栏
- 左侧导航
- 主内容区域
- 退出登录
- 登录账号决定当前角色
- 路由守卫
- 页面级加载态、空态、错误态、无权限态

### P0.3 核心领域类型

先定义后续模块共用的基础类型：

- `Role`
- `Task`
- `TaskStatus`
- `Template`
- `SchemaNode`
- `Submission`
- `ReviewRecord`
- `AIReviewResult`
- `ExportJob`
- `AuditEvent`

### P0.4 服务层骨架

只建立接口层，不接 Mock 业务数据：

- API Client 封装
- 请求与响应类型定义
- 错误封装
- 任务、模板、审核、导出分域 service 文件
- 演示账号配置
- 统一的加载/失败处理入口

## P1 - 稳定基础

### P1.1 全局状态

补齐全局状态切片：

- 当前登录用户
- 当前角色
- 是否已登录
- 登录错误
- 当前导航项
- 页面级 UI 状态
- 通用通知状态
- 退出登录动作

### P1.2 共享组件

已抽出后续会复用的基础组件：

- 页面标题
- 状态占位组件
- 结果提示组件
- 通用表格壳
- 通用抽屉壳

### P1.3 主题与交互规范

统一：

- 字体与间距
- 表单间距
- 表格密度
- 按钮层级
- 错误提示样式
- 登录页账号卡片
- 三角色工作台信息结构

## P2 - 验收收尾

### P2.1 角色页烟囱验证

为三类角色准备最小页面入口，确认：

- Owner 登录后进入任务负责人工作台
- Labeler 登录后进入标注员工作台
- Reviewer 登录后进入审核员工作台
- 退出登录后回到登录页

### P2.2 构建与质量检查

待完成：

- `npm run build`
- `npm run lint`
- TypeScript 类型检查

### P2.3 文档同步

补齐：

- 目录说明
- 运行说明
- 角色入口说明
- 默认账号说明

## 产出

- 项目可启动
- 默认进入登录页
- 三类账号可登录到对应工作台
- 基础布局可访问
- 角色导航按账号权限展示
- 核心类型文件完整
- API Client 与 service 骨架就位

## 验收标准

- 应用默认展示登录页
- 三个默认账号可进入对应角色工作台
- 不同角色看到不同导航和页面内容
- 退出登录可回到登录页
- 空态、加载态、错误态、无权限态统一
- TypeScript 类型无明显缺口
- 构建和 lint 通过

## 风险

- 过早引入复杂抽象会拖慢后续开发
- 当前认证仅为前端演示态，后续需要替换为真实认证接口
- 权限模型当前按账号角色控制，后续再细化到按钮级和字段级
- 这一阶段不做 Mock 业务数据，避免和后续真实接口对接时产生偏差
