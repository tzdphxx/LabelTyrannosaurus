# Task & DatasetItem Lifecycle Redesign

> 日期：2026-06-02
> 状态：已确认

## 一、DatasetItem 状态枚举

```java
public enum DatasetItemStatus {
    AVAILABLE,   // 可领取 (assigned_count < overlapCount)
    FULL,        // 名额已满 (assigned_count >= overlapCount)
}
```

- 题目状态只表达**是否还能被领取**，不表达标注/审核进度（进度看 Assignment + Submission）
- `overlapCount = 1` 时，被领取即 `FULL`

## 二、Task 生命周期（不变）

```
DRAFT → PUBLISHED → PAUSED → ENDED
              ↑        │
              └─resume─┘
```

- DRAFT：Owner 可 CRUD 题目、编辑任务、删除
- PUBLISHED：标注员可领取，审核员可审核
- PAUSED：已领的可继续，不可新领
- ENDED：不可再领取或提交

## 三、DatasetItem 生命周期

```
Over 操作 (仅 DRAFT 状态)：
  - 批量追加 (batch-append)
  - 批量更新 (batch-update)
  - 批量删除 (batch-delete)
  - 🆕 单条新增 (POST .../items/single)
  - 🆕 单条编辑 (PUT .../items/{itemId})
  - 🆕 单条删除 (DELETE .../items/{itemId})
  - 文件导入 (import / import/overwrite)

PUBLISHED 后标注流转：

   AVAILABLE ──claim──→ (assigned_count +1)
      │                    │
      │               assigned_count < overlapCount? ──YES──→ 仍 AVAILABLE
      │                    │
      │                    └──NO──→ FULL
      │
      └── 题目 FULL 后其他标注员不可再领


标注员侧 (Assignment 驱动)：

   CLAIMED ──submit──→ SUBMITTED ──approve──→ APPROVED
      │                    │    ──reject───→ RETURNED ──resubmit──→ SUBMITTED
      └──cancel──→ AVAILABLE (释放，assigned_count -1)
```

## 四、overlapCount 恢复

| 层级 | 变更 |
|------|------|
| `CreateTaskRequest` | ➕ `overlapCount` (Integer, ≥1, 必填) |
| `UpdateTaskRequest` | ➕ `overlapCount` (Integer, ≥1, 必填) |
| `Task.java` 实体 | ➕ `overlapCount` |
| DB `tasks` 表 | ➕ `overlap_count INT NOT NULL DEFAULT 1` |
| Flyway | V30 迁移：加回 overlap_count + 加 dataset_items.status |
| `DatasetItemMapper` | SQL 恢复 `#{overlapCount}` 参数 |
| `DatasetClaimService` | 接口恢复 `Integer overlapCount` 参数 |
| `validatePublishRequirements` | 恢复 `overlapCount > 0` 校验 |

## 五、策略 × overlapCount 交互

| 策略 | claim 行为 | 单条最多标注人数 |
|------|-----------|:--:|
| FCFS | 任何人主动领 | overlapCount |
| ASSIGNED | Owner 指派，不可主动领 | overlapCount |
| QUOTA_CLAIM | 配额内主动领 | overlapCount × 每人限 bonusThreshold 条 |

ASSIGNED 下同一条题目可指派多个标注员（≤ overlapCount）。

## 六、接口变更清单

### 6.1 恢复 / 数据库

| # | 内容 | 说明 |
|---|------|------|
| V30 | `tasks` 表加回 `overlap_count`；`dataset_items` 加 `status` | Flyway |
| D30 | `DatasetItemStatus` 枚举 | 新增 |

### 6.2 任务创建/编辑（恢复 overlapCount）

| 文件 | 变更 |
|------|------|
| `CreateTaskRequest` | ➕ `overlapCount` |
| `UpdateTaskRequest` | ➕ `overlapCount` |
| `Task.java` | ➕ `overlapCount` |
| `TaskLifecycleService` | create / updateDraft 恢复 overlapCount |
| `validatePublishRequirements` | 恢复 overlapCount 校验 |

### 6.3 题目单条 CRUD（🆕）

| 接口 | 权限 | 说明 |
|------|------|------|
| `POST /api/v1/tasks/{id}/dataset/items/single` | Owner (DRAFT only) | 新增单条 |
| `PUT /api/v1/tasks/{id}/dataset/items/{itemId}` | Owner (DRAFT only) | 编辑单条 |
| `DELETE /api/v1/tasks/{id}/dataset/items/{itemId}` | Owner (DRAFT only) | 软删单条 |

### 6.4 题目列表增强

| 接口 | 变更 |
|------|------|
| `GET .../dataset/items` | 响应加 `status` 字段 |

### 6.5 批量领取（🆕）

| 接口 | 权限 | 说明 |
|------|------|------|
| `POST /api/v1/tasks/{id}/assignments/batch-claim` | Labeler | 请求 `{count:N}`，一次领 N 条 |

行为：
- FCFS：循环 claim 直到 count 或库存或配额满
- QUOTA_CLAIM：循环 claim 直到 count 或配额满或库存空
- ASSIGNED：返回 403

### 6.6 指派接口（🆕）

| 接口 | 权限 | 说明 |
|------|------|------|
| `POST /api/v1/tasks/{id}/assignments/assign` | Owner | 请求 `{labelerId, datasetItemIds:[]}` |

### 6.7 标注市场响应增强

`MarketTaskResponse` ➕：

| 字段 | 类型 | 说明 |
|------|------|------|
| `strategy` | Strategy | 分发策略 |
| `taskStatus` | String | 派生状态：CAN_CLAIM / CLAIMED_SOME / UNAVAILABLE |

### 6.8 Assignment 详情增强

`AssignmentDetailResponse` ➕：

| 字段 | 类型 | 说明 |
|------|------|------|
| `rewardSummary` | RewardSummaryResponse | 奖励摘要 |
| `datasetItemStatus` | DatasetItemStatus | 题目状态 |

## 七、完整接口矩阵

| # | 接口 | 角色 | 说明 |
|---|------|------|------|
| 3.1 | `GET /api/v1/owner/tasks` | Owner | 任务列表 |
| 3.2 | `POST /api/v1/tasks` | Owner | 创建（含 overlapCount） |
| 3.3 | `GET /api/v1/tasks/{id}` | Owner | 详情 |
| 3.4 | `PUT /api/v1/tasks/{id}` | Owner | 编辑（含 overlapCount） |
| 3.5~3.9 | publish/pause/resume/end/delete | Owner | 状态流转 |
| 4.1 | `GET /api/v1/tasks/{id}/dataset/items` | Owner | 题目列表（+status） |
| 🆕4.2 | `POST .../items/single` | Owner | 单条新增 |
| 🆕4.3 | `PUT .../items/{itemId}` | Owner | 单条编辑 |
| 🆕4.4 | `DELETE .../items/{itemId}` | Owner | 单条删除 |
| 4.5~4.7 | batch-append/update/delete | Owner | 批量操作 |
| 4.8~4.9 | import / import/overwrite | Owner | 文件导入 |
| 6.1 | `GET /api/v1/market/tasks` | Labeler | 市场列表（+strategy+taskStatus） |
| 6.2 | `POST .../assignments/claim` | Labeler | 领取（单条） |
| 🆕6.3 | `POST .../assignments/batch-claim` | Labeler | 批量领取 |
| 🆕6.4 | `POST .../assignments/assign` | Owner | 指派题目 |
| 6.5 | `GET /api/v1/assignments/{id}` | Labeler | 详情（+reward+itemStatus） |

## 八、不动模块

| 模块 | 原因 |
|------|------|
| 冲突仲裁 (Conflict) | overlapCount>1 可触发，保留不动 |
| Diff 对比 | 保留不动 |
| 版本追溯 (versions) | 保留不动 |
| AI 审核配置 | 另一个 Agent 负责 |
| 奖励结算 (RewardSettlement) | 保留不动 |

## 九、DB 迁移 V30

```sql
-- 恢复 overlap_count
ALTER TABLE tasks ADD COLUMN overlap_count INT NOT NULL DEFAULT 1 AFTER quota;

-- 题目加显式状态
ALTER TABLE dataset_items ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE' AFTER assigned_count;
UPDATE dataset_items SET status = 'FULL' WHERE assigned_count >= 1 AND deleted = 0;
```

## 十、测试要点

- overlapCount=1 单标注员正常流转
- overlapCount=3 三个标注员各领同一条，分别提交
- FCFS claim 通过，ASSIGNED claim 403，QUOTA_CLAIM 超配额拒绝
- 批量领取一次领 N 条，返回实际领取数
- Owner 指派指定的题目给指定的标注员
- 题目 CRUD 仅 DRAFT 可用
- 释放题目 assigned_count 递减，AVAILABLE 恢复
