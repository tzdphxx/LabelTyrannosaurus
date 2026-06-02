# Task API Field Remodel Design

> 日期：2026-06-02
> 状态：已确认

## 目标

重构任务管理 API 的字段体系：补全响应字段、引入分发策略、嵌入奖励规则、删除废弃字段。

## 一、新增枚举：Strategy

```java
public enum Strategy {
    FCFS,          // 先到先得
    ASSIGNED,      // 指派
    QUOTA_CLAIM    // 配额抢单
}
```

## 二、内嵌 Reward 字段

奖励规则不再独立建表，直接嵌入 `tasks` 表。字段：

| 字段 | SQL 类型 | Java 类型 | 说明 | 可空 |
|------|---------|-----------|------|------|
| `reward_per_approval` | DECIMAL(10,2) | BigDecimal | 每条通过奖励积分 | YES |
| `penalty_per_rejection` | DECIMAL(10,2) | BigDecimal | 每条驳回扣分 | YES |
| `bonus_threshold` | INT | Integer | 额外奖励通过数阈值 | YES |
| `bonus_points` | DECIMAL(10,2) | BigDecimal | 达标后额外奖励积分 | YES |

## 三、数据库迁移

### tasks 表变更

```sql
-- 新增
ALTER TABLE tasks ADD COLUMN strategy VARCHAR(20) NOT NULL DEFAULT 'FCFS';
ALTER TABLE tasks ADD COLUMN reward_per_approval DECIMAL(10,2);
ALTER TABLE tasks ADD COLUMN penalty_per_rejection DECIMAL(10,2);
ALTER TABLE tasks ADD COLUMN bonus_threshold INT;
ALTER TABLE tasks ADD COLUMN bonus_points DECIMAL(10,2);

-- 删除
ALTER TABLE tasks DROP COLUMN overlap_count;
ALTER TABLE tasks DROP COLUMN reward_visible;

-- 删除奖励规则表
DROP TABLE IF EXISTS reward_rules;
```

## 四、Entity 变更

`Task.java`：
- 删除字段：`overlapCount`、`rewardVisible`
- 新增字段：`strategy`（Strategy 枚举）、`rewardPerApproval`、`penaltyPerRejection`、`bonusThreshold`、`bonusPoints`

## 五、DTO 变更

### CreateTaskRequest

| 操作 | 字段 | 类型 | 说明 |
|------|------|------|------|
| ➕ | `strategy` | Strategy | 分发策略，默认 FCFS |
| ➕ | `rewardPerApproval` | BigDecimal | 通过奖励 |
| ➕ | `penaltyPerRejection` | BigDecimal | 驳回扣分 |
| ➕ | `bonusThreshold` | Integer | 额外奖励阈值 |
| ➕ | `bonusPoints` | BigDecimal | 额外奖励积分 |
| ➖ | `overlapCount` | — | 删除 |

### UpdateTaskRequest

同上（一致）。

### TaskDetailResponse

| 操作 | 字段 |
|------|------|
| ➕ | `strategy` |
| ➕ | `rewardPerApproval`、`penaltyPerRejection`、`bonusThreshold`、`bonusPoints` |
| ➖ | `overlapCount`、`rewardVisible` |

### OwnerTaskSummaryResponse

| 操作 | 字段 |
|------|------|
| ➕ | `description`、`strategy` |
| ➕ | `rewardPerApproval`、`penaltyPerRejection`、`bonusThreshold`、`bonusPoints` |
| ➖ | `overlapCount` |

### TaskLifecycleResponse

| 操作 | 字段 |
|------|------|
| ➕ | `description`、`tags` |

### CreateTaskResponse

| 操作 | 字段 |
|------|------|
| ➕ | `description`、`tags` |

## 六、Service 层变更

### TaskLifecycleService

- `create()` — 保存 strategy + reward 字段到 Task；删除 overlapCount/rewardVisible 设置
- `createWithDataset()` — 透传
- `updateDraft()` — 更新 strategy + reward 字段；删除 overlapCount 更新
- `toDetailResponse()` — 映射新字段，移除 overlapCount/rewardVisible
- `snapshot()` — 删除 overlapCount，新增 strategy + reward
- `validatePublishRequirements()` — 删除 rewardRuleExists 校验（不再需要），可选增加 reward 字段非空校验

### TaskManagementService

- `listOwnerTasksPage()` — `OwnerTaskSummaryResponse` 构造加 description/strategy/reward，删 overlapCount

### AssignmentClaimService

- 根据 `task.strategy` 控制领取行为：

| Strategy | 行为 |
|----------|------|
| `FCFS` | 维持现有 claim 逻辑（数据库行锁） |
| `ASSIGNED` | 不允许主动 claim，由 Owner 指派后才可标注（返回错误） |
| `QUOTA_CLAIM` | FCFS + 单人限领数量校验（基于 reward.bonusThreshold 或独立配置） |

## 七、清理

删除以下文件（RewardRule 已嵌入 Task，不再需要独立模块）：

- `modules/reward/domain/RewardRule.java`
- `modules/reward/dto/RewardRuleRequest.java`
- `modules/reward/dto/RewardRuleResponse.java`
- `modules/reward/controller/RewardRuleController.java`
- `modules/reward/service/RewardRuleService.java`
- `modules/reward/mapper/RewardRuleMapper.java`

## 八、AI 配置

AI 配置（prompt/model/rating/阈值等）由另外的 Agent 负责修改创建流程：Owner 创建任务时配置提示词、评分维度、模型选择和阈值；AI 模型的创建和管理由 Admin 管理。本设计不覆盖。

## 九、测试要点

- 创建任务时 strategy 默认值 FCFS 生效
- reward 字段可空（任务可以不设置奖励）
- overlapCount 从所有 API 请求/响应中移除
- rewardVisible 从所有响应中移除
- FCFS 下领取行为不变
- ASSIGNED 下主动 claim 返回错误
- QUOTA_CLAIM 下超限领取返回错误
- 旧 reward_rules 表已删除，RewardRuleController 接口不可用
