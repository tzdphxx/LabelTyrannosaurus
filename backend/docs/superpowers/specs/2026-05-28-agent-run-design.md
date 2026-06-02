# Agent Run（4.2）设计文档

**日期：** 2026-05-28
**功能编号：** 4.2
**负责方：** BE-A
**分支：** tzdphxx

---

## 1. 背景与目标

每一次 AI 审核运行需要有独立的运行实例（agentRun），用于记录本次调用的完整上下文（Provider、模型、Prompt 版本、输入输出快照、状态和耗时），支持失败重试追踪和审计引用。

**验收标准：**
- 每次 AI 审核至少一条 agentRun
- 失败重试产生新的 agentRun（不复用旧实例）
- 最终有效结果能指向 effectiveRunId（由 ai_review_results 引用）
- audit_log 能引用 agentRunId（AuditAppender 已支持 agentRunId 参数）

---

## 2. 文件布局

```
backend/src/main/resources/db/migration/
  V3__agent_runs.sql

backend/src/main/java/com/labelhub/modules/agent/
  domain/
    AgentRunStatus.java       # enum: PENDING/RUNNING/SUCCESS/FAILED/RATE_LIMITED/MANUAL_REQUIRED
    AgentRun.java             # 实体，映射 agent_runs 表
  mapper/
    AgentRunMapper.java       # BaseMapper<AgentRun>
  service/
    AgentRunService.java      # create / start / complete / fail

backend/src/test/java/com/labelhub/modules/agent/service/
  AgentRunServiceTest.java    # 单元测试（Mockito）
```

---

## 3. 数据库表

```sql
CREATE TABLE agent_runs (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  agent_type    VARCHAR(64)  NOT NULL,
  submission_id BIGINT       NOT NULL,
  provider_id   BIGINT       NULL,
  model_name    VARCHAR(128) NULL,
  prompt_version VARCHAR(64) NULL,
  input_snapshot  JSON NULL,
  output_snapshot JSON NULL,
  status        VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
  error_message TEXT NULL,
  started_at    DATETIME(3) NULL,
  finished_at   DATETIME(3) NULL,
  created_at    DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  KEY idx_agent_runs_submission (submission_id),
  KEY idx_agent_runs_status (status),
  CONSTRAINT chk_agent_runs_status
    CHECK (status IN ('PENDING','RUNNING','SUCCESS','FAILED','RATE_LIMITED','MANUAL_REQUIRED'))
);
```

---

## 4. 组件设计

### 4.1 AgentRunStatus

```java
public enum AgentRunStatus {
    PENDING, RUNNING, SUCCESS, FAILED, RATE_LIMITED, MANUAL_REQUIRED
}
```

### 4.2 AgentRun 实体

字段：id、agentType、submissionId、providerId、modelName、promptVersion、inputSnapshot（String JSON）、outputSnapshot（String JSON）、status（AgentRunStatus）、errorMessage、startedAt、finishedAt、createdAt。

### 4.3 AgentRunService 方法

| 方法 | 作用 | 状态迁移 |
|------|------|---------|
| `create(agentType, submissionId, providerId, modelName, promptVersion, inputSnapshot): AgentRun` | 新建运行实例 | → PENDING |
| `start(agentRunId)` | 开始执行 | PENDING → RUNNING，写 startedAt |
| `complete(agentRunId, outputSnapshot)` | 成功完成 | RUNNING → SUCCESS，写 finishedAt |
| `fail(agentRunId, AgentRunStatus, errorMessage)` | 失败/限流/兜底 | RUNNING → FAILED/RATE_LIMITED/MANUAL_REQUIRED，写 finishedAt |

`fail()` 的 status 参数只允许 FAILED / RATE_LIMITED / MANUAL_REQUIRED，传入其他值抛 IllegalArgumentException。

---

## 5. 提交结构（按小功能）

1. `feat(agent-run): add V3 migration for agent_runs table`
2. `feat(agent-run): add AgentRunStatus enum and AgentRun entity`
3. `feat(agent-run): add AgentRunMapper`
4. `feat(agent-run): add AgentRunService with create and lifecycle`
5. `test(agent-run): add AgentRunService tests`

---

## 6. 不在本功能范围内

- ai_review_results 表（5.x）
- AgentRun 与 AuditAppender 的实际调用（在 AI 预审 5.3 中使用）
- LLM Provider 调用
