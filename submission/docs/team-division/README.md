# 团队分工与开发过程材料

本文档集由 `docs/development-plans/` 中的前端、BE-A、BE-B 三份任务书筛选和修正后形成。修正口径以已完成交付为准，内容覆盖已进入主链路和已形成可调用能力的部分。

## 1. 分工总览

| 分工 | 主要责任 | 已完成交付重点 |
| --- | --- | --- |
| FE 前端 | 多角色 Web 工作台、动态表单 Designer/Renderer、页面状态与服务层编排 | Owner 建任务与模板、Labeler 领取作答、Reviewer 审核处理、角色首页与看板入口 |
| BE-A 审核智能业务引擎 | 任务生命周期、领取草稿提交、AI 预审、LLM 调用、人工审核、提交追溯 | `任务 -> 领取 -> 草稿 -> 提交 -> AI 预审 -> 人工审核 -> 追溯` 主链路 |
| BE-B 平台支撑与数据资产 | Auth/RBAC、数据集、模板、Schema 校验、文件存储、导出、奖励、看板、审计、通知 | `数据资产 -> 模板契约 -> 文件与导出 -> 奖励统计 -> 多角色看板` 支撑链路 |

## 2. 文档清单

| 文件 | 说明 |
| --- | --- |
| [01-fe-division.md](./01-fe-division.md) | 前端分工任务书，覆盖路由、布局、Owner、Labeler、Reviewer、动态表单和前后端边界 |
| [02-be-a-review-ai-division.md](./02-be-a-review-ai-division.md) | BE-A 分工任务书，覆盖任务状态、领取提交、LLM Provider、AI 预审、辅助触发、人工审核和追溯 |
| [03-be-b-platform-data-division.md](./03-be-b-platform-data-division.md) | BE-B 分工任务书，覆盖登录权限、数据集、模板、Schema、存储、导出、奖励、看板、审计和基础设施 |

## 3. 交付口径

当前提交材料采用单题单活跃标注员的标注闭环。Labeler 从任务市场领取题目，提交答案后进入 AI 预审，Reviewer 基于提交内容、AI 建议和历史记录进行人工审核。审核结果通过后进入导出、贡献统计和看板聚合；打回后由 Labeler 在提交记录中查看原因并修正。

LLM Provider 由 Admin 进行全局维护，Owner 在任务 AI 配置中选择启用项。AI 审核通过异步队列、AgentRun、AI 结果表、运行日志和重试恢复能力进行工程化落地。字段级 LLM 辅助和题目级预标注都复用统一的 LLM 任务执行基础设施，并保留运行追踪信息。

模板由 Owner 维护，前端 Designer 负责搭建和预览，后端模板模块负责版本保存和 Schema 校验。正式提交时，前端校验只作为交互提示，最终答案合法性以后端 `SchemaValidationService` 和 `AnswerSchemaValidator` 为准。

数据库迁移以 `backend/src/main/resources/db/migration` 为准，当前迁移序列包含 `V39__submission_review_claim_indexes.sql`。关键补强包括 Admin 全局 Provider、AI 可观测字段、审核领取表、AI 流转策略、提交创建人和审核领取索引。

## 4. 分工边界

FE 不直接决定业务状态，不在浏览器端生成审核结论，也不绕过后端权限。前端负责页面编排、输入保护、列表状态、动态表单渲染和服务调用。

BE-A 负责主业务状态流转和审核智能链路。它读取 BE-B 提供的数据、模板、权限、审计、文件和统计基础能力，但不维护原始数据文件和导出文件生成。

BE-B 负责平台数据资产和通用能力。它提供 Auth/RBAC、数据集、模板、Schema、对象存储、导出、奖励、看板、审计、通知、Redis/Redisson 和异步任务基础设施，不直接替代 BE-A 做提交审核结论。

## 5. 自测与验收口径

分工文档中的验收项均按已完成能力编写，主要对应以下实现范围：

- 前端：`frontend/src/app/router.tsx`、`frontend/src/pages`、`frontend/src/features/dynamic-form`、`frontend/src/services`、`frontend/src/stores`。
- BE-A：`modules/task`、`modules/assignment`、`modules/submission`、`modules/ai`、`modules/preannotation`、`modules/review`、`modules/agent`、`infrastructure/llmtask`。
- BE-B：`modules/auth`、`modules/admin`、`modules/dataset`、`modules/template`、`modules/export`、`modules/storage`、`modules/reward`、`modules/role/dashboard`、`modules/audit`、`modules/media`、`modules/notification`、`infrastructure/redis`、`infrastructure/async`。
- 测试：`backend/src/test/java` 下的任务、提交、AI、LLM、审核、数据集、模板、导出、文件、看板、迁移和契约测试。
