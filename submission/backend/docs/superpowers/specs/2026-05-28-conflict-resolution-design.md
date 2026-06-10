# 6.3 冲突仲裁与金标选择 设计文档

## 目标

多人标注（overlapCount > 1）同一 datasetItem 时，如果答案不一致，系统生成冲突组，Reviewer 从中选择金标提交。

## 架构

ConflictGroup 作为持久化实体，记录冲突状态和仲裁结果。冲突检测在 submission 进入 PENDING_FINAL 时触发。

## 核心实体

### ConflictGroup

```
conflict_groups 表：
- id (BIGINT PK)
- task_id (BIGINT NOT NULL)
- dataset_item_id (BIGINT NOT NULL)
- status (VARCHAR: OPEN / RESOLVED)
- consensus_score (DECIMAL(5,4)) — 相同答案数/总数
- golden_submission_id (BIGINT, nullable)
- resolved_by (BIGINT, nullable)
- resolved_reason (TEXT, nullable)
- created_at (DATETIME(3))
- resolved_at (DATETIME(3), nullable)
- UNIQUE INDEX (task_id, dataset_item_id)
```

### 状态机

```
OPEN -> RESOLVED (via resolve)
```

## 冲突检测逻辑

触发时机：submission.status 变为 PENDING_FINAL 时。

算法：
1. 查询同 task_id + dataset_item_id 下所有 PENDING_FINAL 的 submissions
2. 如果数量 < 2，不生成冲突
3. 计算 consensusScore = 最大相同 answerHash 数量 / 总数
4. 如果 consensusScore < 1.0（即不完全一致），创建或更新冲突组（OPEN）
5. 如果 consensusScore = 1.0（全部一致），不生成冲突组

## API 端点

### GET /api/v1/reviewer/conflict-groups
查询所有 OPEN 冲突组列表。

### GET /api/v1/reviewer/conflict-groups/{groupId}
查询冲突组详情，包含该组下所有 submissions 的 answerJson 摘要。

### POST /api/v1/reviewer/conflict-groups/{groupId}/resolve
请求：`{ goldenSubmissionId, reason }`
逻辑：
1. 校验 groupId 存在且 status=OPEN
2. 校验 goldenSubmissionId 属于该冲突组（同 task_id + dataset_item_id）
3. 标记 goldenSubmission.isGolden = true
4. 更新 conflictGroup: status=RESOLVED, goldenSubmissionId, resolvedBy, resolvedReason, resolvedAt
5. 写 ReviewRecord (action=RESOLVE_CONFLICT)
6. 写审计
7. 发布 GoldenSelected 事件

## 事件扩展

SubmissionEventPublisher 增加：
```java
void publishGoldenSelected(Long submissionId, Long reviewerId);
```

## 验收标准

- overlapCount=1 不生成冲突
- overlapCount=2 且答案不一致生成冲突组
- 选择金标后只有一个 isGolden=true
- 选择金标后发出 GoldenSelected 事件
- 已 RESOLVED 的冲突组不能再次 resolve
