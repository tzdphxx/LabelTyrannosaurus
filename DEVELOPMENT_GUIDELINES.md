# LabelHub Development Guidelines

> 本规范面向团队成员和 AI 开发工具。任何实现前，请先阅读 `docs/development-plan/04-对接契约与数据结构协作手册.md`、对应 `docs/api-contracts/` 文件，以及本文件。

## 1. Project Baseline

LabelHub 是一个前后端分离的模块化单体项目，核心链路是：

```text
Owner 建任务 -> 导入数据 -> 搭模板 -> 配置 AI -> 发布
Labeler 领取 -> 草稿 -> 提交
AI Agent 预审 -> Reviewer 终审/仲裁 -> 金标
Owner 导出
```

已确认技术栈：

- Backend：Java、Spring Boot 3、Spring Security、MyBatis-Plus、Flyway、MySQL、Redis/Redisson、Spring AI Alibaba、Tencent Cloud COS。
- Frontend：React 18、TypeScript，建议使用 Ant Design 或 Semi Design、Formily、TanStack Query、Zustand/Jotai/Redux Toolkit、dnd-kit 或 react-dnd。
- API：统一 `/api/v1` 前缀，统一 `ApiResponse` 响应结构。
- Docs：接口、状态、字段、事件以 `docs/api-contracts/` 和 `docs/development-plan/04-对接契约与数据结构协作手册.md` 为准。

## 2. Collaboration Rules

1. 接口字段、状态枚举、事件字段、错误码变更必须先改文档，再改代码。
2. 前端不得绕过契约字段临时适配；后端不得临时改字段名让前端猜。
3. BE-A、BE-B、FE 的模块边界必须遵守协作手册，不因为实现方便跨边界直接写对方负责的数据状态。
4. 所有关键信息都要可追溯：任务状态迁移、领取、提交、AI 审核、人工审核、仲裁、奖励、导出必须写审计或事件。
5. AI 生成代码前必须先读取相关文档和现有代码；禁止凭空新增与项目风格冲突的目录、框架和状态值。
6. MVP 优先跑通端到端闭环；高级能力可以后置，但不能破坏核心状态机正确性。

## 3. Backend Standards

### 3.1 Java Code Style

后端代码必须遵循《阿里巴巴 Java 开发手册》的基础规范，并结合本项目约定：

- 包名全部小写，使用 `com.labelhub` 根包。
- 类名使用 UpperCamelCase，方法、变量、参数使用 lowerCamelCase，常量使用 UPPER_SNAKE_CASE。
- 禁止魔法值。状态、角色、动作、错误码应使用 enum 或常量集中管理。
- 单个方法只做一层清晰业务意图，复杂流程拆为私有方法或领域服务。
- 不允许空 `catch`；异常必须转换为业务错误、重试、审计或日志。
- 金额、奖励、分数等需要精度的字段使用 `BigDecimal`，禁止用 `float`/`double` 表示业务金额。
- 时间字段使用 `LocalDateTime`/`Instant`，数据库和接口命名统一为 `xxxAt`。
- 集合判空使用工具或 `isEmpty()`，禁止靠异常控制流程。
- 禁止在日志中输出密码、JWT、API Key、COS Secret、LLM 原始密钥。

### 3.2 Package Structure

后端按模块组织，优先沿用现有结构：

```text
backend/src/main/java/com/labelhub/
  common/
    api/
    audit/
    exception/
    security/
  infrastructure/
    llm/
    redis/
    storage/
  modules/
    auth/
    admin/
    task/
    dataset/
    template/
    assignment/
    submission/
    ai/
    review/
    reward/
    export/
    audit/
```

模块内部推荐分层：

```text
controller/
service/
domain/
repository/
dto/
event/
```

要求：

- Controller 只做参数接收、权限入口和响应返回，不写核心业务。
- Service 编排用例和事务边界。
- Domain 放状态机、枚举、业务规则和领域判断。
- Repository/Mapper 只处理持久化。
- DTO 与 Entity 分离，接口响应不得直接暴露数据库实体。

### 3.3 API Rules

统一响应结构：

```json
{
  "code": 0,
  "message": "OK",
  "data": {},
  "traceId": "trace-id"
}
```

接口规范：

- URL 使用名词复数和资源层级，例如 `/api/v1/tasks/{taskId}/exports`。
- 状态迁移动作使用 `POST`，例如 `/publish`、`/pause`、`/submit`、`/approve`。
- 查询使用 `GET`，创建使用 `POST`，整体更新使用 `PUT`。
- 分页响应统一包含 `items`、`page`、`pageSize`、`total`。
- 所有业务错误必须返回文档中的错误码，例如 `400101`、`409101`、`429001`。
- 每个接口必须校验角色权限，禁止只依赖前端隐藏入口。

### 3.4 State Machine Rules

状态机由后端强制执行，前端只展示状态，不推断业务合法性。

关键约束：

- `TaskStatus` 只能按 `DRAFT -> PUBLISHED -> PAUSED/ENDED` 等文档路径迁移。
- `AssignmentStatus`、`SubmissionStatus`、`AiReviewStatus`、`ConflictStatus` 不得私自新增。
- AI 可以建议 `PASS`、`REJECT`、`MANUAL_REVIEW`，但不能直接设置 `APPROVED` 或 `isGolden=true`。
- 打回后重新提交必须生成新的 `submission.versionNo`，不能覆盖历史。
- 导出默认只允许 `submission.status=APPROVED` 且 `isGolden=true` 的金标数据。

### 3.5 Transactions and Concurrency

- 领取、提交、审核通过、冲突仲裁、奖励结算必须有明确事务边界。
- 领取题目必须使用 Redis/Redisson 锁和数据库唯一约束兜底，避免超发。
- 草稿保存、审核提交、批量审核必须使用版本号做乐观锁，冲突返回 `409101`。
- 批量操作采用逐条处理，返回成功列表和失败原因，不做“一条失败全回滚”的大事务。
- 异步任务必须可查询状态，失败必须记录错误信息和 traceId。

### 3.6 Database and Migration

- 数据库结构通过 Flyway migration 管理，禁止直接手改线上结构。
- 表名、字段名使用 snake_case；Java 属性使用 camelCase，依赖 MyBatis-Plus 映射。
- 状态字段存储稳定枚举字符串，不存中文展示文案。
- JSON 字段只存结构化业务快照，不存文件二进制。
- 文件、导入错误报告、导出文件统一走对象存储。
- 审计表只追加，不更新、不删除。

### 3.7 Security

- 密码只能存 BCrypt hash。
- JWT 必须包含 `userId`、`roles`、`tokenVersion`，角色变更后旧 token 失效。
- `system_ai_agent` 是不可登录系统用户，只用于 AI 审计主体。
- Admin 不能移除最后一个 Admin。
- API Key、COS Secret、JWT、密码不得出现在响应、日志、异常信息和审计明文中。
- 文件上传必须校验类型、大小、归属权限和对象存储 key。

### 3.8 Backend Testing

后端至少覆盖：

- 状态机非法迁移。
- 发布前跨模块检查。
- 并发领取不超发。
- 草稿版本冲突。
- Schema 校验失败不能提交。
- AI 审核成功、限流、失败重试和人工兜底。
- 人工审核通过/打回。
- 批量审核部分成功。
- 冲突仲裁只有一个金标。
- 奖励事件幂等。
- 导出只包含 APPROVED 金标。

## 4. Frontend Standards

### 4.1 TypeScript and React

- 使用 React 18 + TypeScript，业务代码禁止大量 `any`。
- API 请求、响应、状态枚举必须有显式类型。
- 状态枚举值与后端完全一致，展示文案通过前端映射层处理。
- 组件使用函数组件和 Hooks。
- 表单、列表、详情页必须处理 loading、empty、error、success 状态。
- 长流程表单离开页面前必须提示未保存更改。
- 断网或接口超时不能导致用户输入丢失。

### 4.2 Frontend Directory Structure

建议结构：

```text
frontend/src/
  api/
  app/
  components/
  features/
    designer/
    renderer/
    assignment/
    review/
    export/
  pages/
    admin/
    owner/
    labeler/
    reviewer/
  stores/
  types/
  utils/
```

要求：

- `api/` 封装请求客户端和接口函数，不在页面里散写 URL。
- `types/` 放接口 DTO、枚举和公共类型。
- `features/` 放可复用业务能力，例如 Designer、Renderer、Review Detail。
- `pages/` 负责路由级组合。
- 通用 UI 组件放 `components/`，业务组件放对应 `features/`。

### 4.3 UI and UX Rules

- 使用统一组件库，优先 Ant Design 或 Semi Design，不混用多个主 UI 库。
- 所有列表页使用统一 `DataTable` 风格：loading、empty、pagination、row selection。
- 所有状态展示使用统一 `StatusTag` 映射。
- 批量操作结果使用统一 `ResultPanel` 展示成功数、失败数和失败原因。
- 审计历史使用统一 `AuditTimeline`。
- JSON 展示使用格式化和折叠，避免纯文本大段堆叠。
- 1280x800 和 1920x1080 必须可用；移动端响应式兼容是加分项。

### 4.4 API Client Rules

- 所有请求自动附带 accessToken。
- 401001 清理登录态并跳转 `/login`。
- 403001 展示无权限页或无权限提示。
- 409101 版本冲突必须提示刷新、覆盖或合并。
- 429001 展示限流、排队或重试中状态。
- 500001 必须展示 traceId，便于后端排查。
- 不在页面里硬编码接口响应结构，统一通过 API Client 解包 `ApiResponse`。

### 4.5 Dynamic Form Designer and Renderer

- Designer 负责编辑 schema，Renderer 只消费 schema。
- 同一份 schema 必须同时支持 Owner 预览和 Labeler 作答。
- `ShowItem` 只读取 `itemJson`，不得写入 `answerJson`。
- 有 `field` 的组件才写入 `answerJson`。
- `LlmTrigger` 输出只作为参考或预填建议，必须由用户确认后才写入答案。
- 发布后的模板版本不可原地修改，前端必须走 fork 流程。
- 前端校验只能提升体验，后端必须再次校验。
- 禁止执行 schema 中的任意 JS 字符串；条件显示和校验只能使用受控表达式。

### 4.6 Draft and Offline Rules

- Labeler 作答字段变化后 debounce 保存草稿。
- 页面空闲时做补偿保存。
- 保存失败时保留本地答案，不清空表单。
- 断网时写 localStorage，网络恢复后根据 `clientVersion` 和 `serverVersion` 同步。
- 提交失败后必须保留用户答案，并展示错误原因。

### 4.7 Frontend Testing

前端至少覆盖：

- 登录态恢复和权限菜单。
- Owner 建任务、发布前检查。
- Designer 拖拽、属性编辑、预览、fork。
- Renderer 全部物料渲染。
- 草稿保存、刷新恢复、断网恢复。
- 提交校验错误定位。
- Reviewer 审核详情、diff、通过、打回。
- 批量操作部分成功展示。
- 冲突仲裁选择金标。
- 导出任务状态刷新和下载入口。

## 5. Contract and Documentation Rules

每个接口文档必须包含：

```text
接口名称
URL
Method
权限角色
Owner 模块
请求字段
响应字段
错误码
影响的状态
前端使用页面
```

变更流程：

1. 修改 `docs/development-plan/04-对接契约与数据结构协作手册.md` 或 `docs/api-contracts/`。
2. 同步字段、状态、错误码、事件影响。
3. 后端修改 DTO、状态机和测试。
4. 前端修改类型、页面和错误处理。
5. 联调验证。

## 6. AI Agent Coding Rules

给 AI 分配任务时，请明确：

```text
角色：FE / BE-A / BE-B
目标页面或接口
必须读取的文档
允许修改的目录
不可修改的契约
验收命令或自测清单
```

AI 必须遵守：

- 先读文档和现有代码，再实现。
- 不擅自新增状态、错误码、角色、数据库字段。
- 不跨模块直接写不属于本模块的状态。
- 不删除用户已有改动。
- 生成代码后同步补测试或说明未补测试原因。
- 修改接口时同步更新对应文档。
- 提交前至少运行相关构建或测试命令；无法运行必须说明原因。

## 7. Definition of Done

一个功能完成必须同时满足：

- 与文档契约一致。
- 权限校验完整。
- 状态机合法。
- 错误码可被前端展示。
- loading、empty、error、success 状态完整。
- 关键动作有审计或事件。
- 核心路径有测试或清晰自测记录。
- 不泄露密钥和敏感信息。
- README、API 文档或任务文档已同步更新。

