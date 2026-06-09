# LabelTyrannosaurus / LabelHub

LabelHub 是一个 AI 数据标注平台，覆盖「任务创建 -> 动态模板 -> 标注提交 -> AI 预审 -> 人工终审 -> 多格式导出」的主流程。仓库采用 monorepo 组织，包含 React 前端、Spring Boot 后端和项目交付文档。

## 当前提交材料入口

最终答辩和仓库评审请优先阅读：

| 文档 | 说明 |
| --- | --- |
| [最终交付文档索引](docs/final-delivery/README.md) | 当前提交材料的入口 |
| [基础技术文档](docs/final-delivery/basic-technical-doc.md) | 架构图、关键技术点、状态机、测试索引 |
| [AI Coding 过程记录](docs/final-delivery/ai-coding-process-record.md) | 开发思路、过程文件、关键调整 |
| [Demo 脚本与截图清单](docs/final-delivery/demo-script-and-screenshot-list.md) | 录屏脚本和截图项 |
| [后端亮点候选方案](backend/docs/backend-highlight-options-for-review.md) | 不改前端接口前提下的后端亮点方向 |

`docs/development-plans/` 保留为过程和早期设计记录。若历史计划与当前代码或 `docs/final-delivery/` 不一致，以当前代码和最终交付文档为准。

## 仓库结构

```text
frontend/   # React + Vite frontend
backend/    # Spring Boot backend
docs/       # course requirements, API contracts, final delivery docs
datasets/   # sample datasets
```

## 当前后端能力

- Auth/RBAC：注册、登录、刷新 token、Admin 用户与角色管理。
- Task/Dataset/Template：任务生命周期、数据导入、模板版本、schema 校验。
- Assignment/Submission：任务市场、领取、草稿、提交版本、answerHash 幂等。
- AI Review：AI 审核配置、异步队列、结构化输出、重试、失败人工兜底、AgentRun 追溯。
- LLM：ADMIN 全局 Provider 管理，Owner 查询已启用 Provider，OpenAI-compatible Gateway。
- LLM Assist：字段级 LlmTrigger、题目级 PreAnnotation、SupervisorAgent 后端能力。
- Review/Export：Reviewer 终审、批量操作、审计日志、JSON/JSONL/CSV/Excel 异步导出。
- Storage/Observability：Tencent COS 文件存储、签名 URL、traceId、Actuator health/info/metrics。

## 当前实现边界

- 当前主 Demo 是单人标注链路。`V28__single_labeler_dataset_item_status.sql` 将 `overlap_count` 归一为 `1`，并增加单活跃 assignment 约束。
- 冲突组/金标相关表、接口和测试保留为后端扩展能力或历史设计材料，不作为当前前端演示必选环节。
- AI Flow Policy 支持可配置直通/直拒；答辩建议默认使用人工终审兜底策略。
- 前端服务层仍存在 mock 数据，最终录屏应按实际联调范围选择前后端联合演示或后端 API 补充演示。

## 后端启动

```powershell
cd backend
mvn spring-boot:run
```

关键环境变量：

| 变量 | 说明 |
| --- | --- |
| `DB_HOST` / `DB_PORT` / `DB_NAME` | MySQL 连接 |
| `DB_USERNAME` / `DB_PASSWORD` | MySQL 账号 |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | Redis 连接 |
| `JWT_SECRET` | JWT 签名密钥 |
| `COS_SECRET_ID` / `COS_SECRET_KEY` / `COS_REGION` / `COS_BUCKET` | Tencent COS |
| `LABELHUB_LLM_KEY_ENCRYPTION_SECRET` | LLM API Key 加密密钥 |
| `AI_DASHSCOPE_API_KEY` | DashScope 默认配置 |

## 前端启动

```powershell
cd frontend
npm install
npm run dev
```

## 测试

后端定向测试示例：

```powershell
cd backend
mvn "-Dtest=SchemaValidationServiceTest,SubmissionSubmitServiceTest" test
```

接口映射守护：

```powershell
cd backend
mvn "-Dtest=ApiContractMappingTest" test
```

全量后端回归以 `mvn test` 为准。
