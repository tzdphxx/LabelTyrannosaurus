# Reward 奖励规则与结算契约

## 保存奖励规则

Description: Saves the latest reward rule for a task and creates a new effective version.

Description: Saves the latest reward rule for a task and creates a new effective version.

- URL: `/api/v1/tasks/{taskId}/reward-rule`
- Method: `POST`
- 权限角色: `ADMIN`、任务 `OWNER`
- Owner 模块: BE-B

请求体:

```json
{
  "rewardMode": "APPROVED_ITEM",
  "unitReward": 2.50,
  "rewardCurrency": "POINT",
  "rewardVisible": true
}
```

响应体:

```json
{
  "code": 0,
  "message": "OK",
  "data": {
    "ruleId": 100,
    "taskId": 1,
    "effectiveVersion": 3,
    "rewardMode": "APPROVED_ITEM",
    "unitReward": 2.50,
    "rewardCurrency": "POINT",
    "rewardVisible": true,
    "effectiveAt": "2026-05-29T10:00:00",
    "createdBy": 10,
    "createdAt": "2026-05-29T10:00:00"
  },
  "traceId": null
}
```

说明:
- 当前仅支持 `rewardMode=APPROVED_ITEM`。
- `unitReward >= 0`。
- 每次保存都会追加 `effectiveVersion`，历史 `reward_ledger` 不重算。

## 查询奖励规则

Description: Reads the current effective reward rule for a task.

Description: Reads the current effective reward rule for a task.

- URL: `/api/v1/tasks/{taskId}/reward-rule`
- Method: `GET`
- 权限角色: `ADMIN`、任务 `OWNER`

响应体同保存接口，返回任务最新规则。

## GET /api/v1/labeler/contribution/overview

Description: 查询当前标注员贡献总览（已领取、已提交、通过、驳回、累计奖励、通过率）。

- 权限角色: `LABELER`、`ADMIN`
- Owner 模块: BE-B

说明: 待审核提交不进入通过率分母。

## GET /api/v1/labeler/contribution/trend

Description: 查询最近 N 天贡献趋势，缺失日期自动补零。

- 权限角色: `LABELER`、`ADMIN`
- 查询参数: `days`（可选，默认 7，最大值 365）

## GET /api/v1/labeler/contribution/tasks

Description: 按任务聚合查询当前标注员贡献统计，按最近活跃时间排序。

- 权限角色: `LABELER`、`ADMIN`
- 查询参数: `limit`（可选，默认 20）、`offset`（可选，默认 0）

## GET /api/v1/labeler/rewards/ledger

Description: 查询当前标注员奖励流水，包含正向奖励（CREDIT）和冲正记录（DEBIT），按时间倒序。

- 权限角色: `LABELER`、`ADMIN`
- 查询参数: `limit`（可选，默认 20）、`offset`（可选，默认 0）

响应字段:

```text
ledgerId      流水 ID
taskId        所属任务 ID
submissionId  关联提交 ID
assignmentId  关联分配 ID
amount        奖励金额
direction     资金方向: CREDIT（正向奖励）/ DEBIT（冲正扣除）
reason        操作原因或备注
sourceEventId 来源事件 ID（幂等去重用）
rewardType    奖励类型: SUBMISSION_APPROVED / REWARD_REVERSED 等
createdAt     创建时间
```

## 内部奖励结算能力

BE-B 提供 Java Service 供 BE-A 事件消费链路调用:

```text
RewardSettlementService.settleSubmissionApproved(event)
RewardSettlementService.settleGoldenSelected(event)
RewardSettlementService.reverseReward(event)
```

### SubmissionApproved

字段:

```text
eventId
taskId
datasetItemId
assignmentId
submissionId
labelerId
reviewerId
approvedAt
traceId
```

效果:
- 追加 `reward_ledger` 正向流水，`direction=CREDIT`，`reward_type=SUBMISSION_APPROVED`。
- 更新 `labeler_contribution_stats`、`labeler_daily_stats`、`labeler_task_stats`。
- 显式调用 `DatasetSnapshotService.increaseApprovedCount(datasetItemId)`。
- 追加审计日志。

### GoldenSelected

字段:

```text
eventId
goldenSubmissionId
conflictGroupId
reviewerId
resolvedAt
traceId
```

说明:
- BE-B 只读 `submissions` 快照补齐 `assignmentId`、`taskId`、`labelerId`。
- 如果 `goldenSubmissionId` 已经产生正向奖励，不重复计奖。

### RewardReversed

字段:

```text
eventId
taskId
submissionId
labelerId
reason
operatorId
createdAt
traceId
```

效果:
- 追加 `reward_ledger` 负向流水，`direction=DEBIT`，`reward_type=REWARD_REVERSED`。
- 扣减贡献统计。
- 不删除、不修改原正向流水。

## 幂等约束

- `reward_ledger.source_event_id` 唯一，防止同一事件重复消费。
- `reward_ledger.positive_submission_id` 唯一，防止同一 `submissionId` 多次正向发奖。
- `reward_ledger.positive_assignment_id` 唯一，防止同一 `assignmentId` 多版本重复发有效正向奖励。

## 错误码

| code | 场景 |
|---|---|
| `400102` | 任务、奖励规则、提交快照或正向奖励不存在；请求参数非法；不支持的奖励模式 |
| `401001` | 未登录或 token 失效 |
| `403001` | 非管理员且不是任务 Owner |
| `500001` | 系统错误 |
