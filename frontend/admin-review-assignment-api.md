# Admin 审核分配查询接口对接文档

## 1. 概览

本文档用于对接 Admin 端审核分配查询能力，覆盖以下三个只读接口：

| 功能 | 方法 | 路径 |
|------|------|------|
| 获取当前可以分配的任务 | GET | `/api/v1/admin/review/tasks/assignable` |
| 获取当前可以分配的人工审核员 | GET | `/api/v1/admin/review/reviewers/assignable` |
| 获取当前人工审核员任务进度、工作状态 | GET | `/api/v1/admin/review/reviewers/progress` |

统一响应结构：

```json
{
  "code": 0,
  "message": "OK",
  "data": {},
  "traceId": null
}
```

权限要求：

- 调用方必须具备 `ADMIN` 角色。
- 非 Admin 调用会返回业务错误 `403001 Forbidden`。

业务口径：

- 本组接口只做查询，不会分配任务，不会修改审核员归属，不写审计。
- 当前实际分配/归属模型为 `review_task_claims + submissions.assigned_reviewer_id`。
- 本次接口不使用 `task_reviewers` 表作为判断依据。

---

## 2. 获取当前可以分配的任务

Admin 查看当前待终审池中可以分配给审核员的任务级别。

**GET** `/api/v1/admin/review/tasks/assignable`

### 查询参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| taskId | Long | 否 | - | 按任务 ID 精确筛选 |
| keyword | String | 否 | - | 按任务标题或描述模糊搜索 |
| reviewLevel | Integer | 否 | - | 按审核级别筛选 |
| includeClaimed | boolean | 否 | `false` | 是否包含已被整任务认领的任务级别 |
| page | int | 否 | `1` | 页码，小于 1 时按 1 处理 |
| size | int | 否 | `20` | 每页数量，最大 100 |

### 可分配任务定义

一条可分配任务记录按 `task_id + current_review_level` 聚合，必须满足：

- 存在 `submissions.status = 'PENDING_FINAL'` 的待终审提交。
- 默认 `includeClaimed=false` 时，对应 `(task_id, review_level)` 不存在于 `review_task_claims`。

排序：

```text
pendingCount DESC, taskId DESC
```

### 响应字段

`data` 为分页对象：

| 字段 | 类型 | 说明 |
|------|------|------|
| items | Array | 当前页任务列表 |
| page | int | 当前页码 |
| pageSize | int | 当前每页数量 |
| total | long | 总记录数 |

`items[]` 字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| taskId | Long | 任务 ID |
| title | String | 任务标题 |
| status | String | 任务状态：`DRAFT` / `PUBLISHED` / `PAUSED` / `ENDED` |
| deadlineAt | String | 截止时间，ISO 日期时间 |
| reviewLevel | Integer | 审核级别 |
| pendingCount | Long | 当前任务级别下待终审提交数量 |
| claimed | Boolean | 是否已被整任务认领 |
| claimedReviewerId | Long | 已认领审核员 ID，未认领为 `null` |
| claimedReviewerName | String | 已认领审核员用户名，未认领为 `null` |
| available | Boolean | 是否当前可分配 |

### 响应示例

```json
{
  "code": 0,
  "message": "OK",
  "data": {
    "items": [
      {
        "taskId": 10,
        "title": "图片质量审核",
        "status": "PUBLISHED",
        "deadlineAt": "2026-06-10T10:00:00",
        "reviewLevel": 1,
        "pendingCount": 8,
        "claimed": false,
        "claimedReviewerId": null,
        "claimedReviewerName": null,
        "available": true
      }
    ],
    "page": 1,
    "pageSize": 20,
    "total": 1
  },
  "traceId": null
}
```

---

## 3. 获取当前可以分配的人工审核员

Admin 查看当前可用于分配的人工审核员及其负载。

**GET** `/api/v1/admin/review/reviewers/assignable`

### 查询参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| keyword | String | 否 | - | 按用户名或邮箱模糊搜索 |
| enabledOnly | boolean | 否 | `true` | 是否只返回启用且允许登录的审核员 |
| page | int | 否 | `1` | 页码，小于 1 时按 1 处理 |
| size | int | 否 | `20` | 每页数量，最大 100 |

### 可分配审核员定义

默认 `enabledOnly=true` 时，审核员必须满足：

- 用户拥有 `REVIEWER` 角色。
- `users.enabled = true`。
- `users.login_enabled = true`。

排序：

```text
pendingCount ASC, todayReviewedCount DESC, reviewerId ASC
```

### 响应字段

`data` 为分页对象。

`items[]` 字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| reviewerId | Long | 审核员用户 ID |
| username | String | 用户名 |
| email | String | 邮箱 |
| enabled | Boolean | 账号是否启用 |
| loginEnabled | Boolean | 是否允许登录 |
| pendingCount | Long | 当前分配给该审核员的待终审数量 |
| todayReviewedCount | Long | 今日已审核数量，包含通过和驳回 |
| totalApprovedCount | Long | 历史通过数量 |
| totalRejectedCount | Long | 历史驳回数量 |
| approvalRate | Decimal | 历史通过率，百分比，保留 2 位 |

### 响应示例

```json
{
  "code": 0,
  "message": "OK",
  "data": {
    "items": [
      {
        "reviewerId": 7,
        "username": "reviewer-a",
        "email": "reviewer-a@example.com",
        "enabled": true,
        "loginEnabled": true,
        "pendingCount": 4,
        "todayReviewedCount": 5,
        "totalApprovedCount": 60,
        "totalRejectedCount": 40,
        "approvalRate": 60.00
      }
    ],
    "page": 1,
    "pageSize": 20,
    "total": 1
  },
  "traceId": null
}
```

---

## 4. 获取当前人工审核员任务进度、工作状态

Admin 查看所有人工审核员的审核进度、负载和已认领任务。

**GET** `/api/v1/admin/review/reviewers/progress`

### 查询参数

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| keyword | String | 否 | - | 按用户名或邮箱模糊搜索 |
| enabledOnly | boolean | 否 | `true` | 是否只返回启用且允许登录的审核员 |

### 统计口径

| 指标 | 来源 | 说明 |
|------|------|------|
| pendingCount | `submissions` | `status = 'PENDING_FINAL'` 且 `assigned_reviewer_id = reviewerId` |
| todayReviewedCount | `review_records` | 今日 `action IN ('APPROVE', 'REJECT')` |
| totalReviewedCount | `review_records` | 历史 `action IN ('APPROVE', 'REJECT')` |
| approvalRate | `review_records` | `APPROVE / (APPROVE + REJECT) * 100` |
| claimedTasks | `review_task_claims` | 该审核员整任务认领的任务级别 |

### 响应字段

`data` 为数组。

| 字段 | 类型 | 说明 |
|------|------|------|
| reviewerId | Long | 审核员用户 ID |
| username | String | 用户名 |
| email | String | 邮箱 |
| enabled | Boolean | 账号是否启用 |
| loginEnabled | Boolean | 是否允许登录 |
| pendingCount | Long | 当前待审数量 |
| todayReviewedCount | Long | 今日已审数量 |
| totalReviewedCount | Long | 历史已审数量 |
| approvalRate | Decimal | 历史通过率，百分比，保留 2 位 |
| claimedTaskCount | Long | 已认领任务级别数量 |
| claimedTasks | Array | 已认领任务级别列表 |

`claimedTasks[]` 字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| taskId | Long | 任务 ID |
| title | String | 任务标题 |
| reviewLevel | Integer | 审核级别 |
| pendingCount | Long | 该任务级别下当前待终审数量 |
| claimedAt | String | 认领时间 |

### 响应示例

```json
{
  "code": 0,
  "message": "OK",
  "data": [
    {
      "reviewerId": 7,
      "username": "reviewer-a",
      "email": "reviewer-a@example.com",
      "enabled": true,
      "loginEnabled": true,
      "pendingCount": 4,
      "todayReviewedCount": 5,
      "totalReviewedCount": 100,
      "approvalRate": 60.00,
      "claimedTaskCount": 1,
      "claimedTasks": [
        {
          "taskId": 10,
          "title": "图片质量审核",
          "reviewLevel": 1,
          "pendingCount": 4,
          "claimedAt": "2026-06-04T09:30:00"
        }
      ]
    }
  ],
  "traceId": null
}
```

---

## 5. 前端推荐使用方式

### 分配任务页面

1. 调用 `/api/v1/admin/review/tasks/assignable` 获取可分配任务级别。
2. 调用 `/api/v1/admin/review/reviewers/assignable` 获取可选审核员和当前负载。
3. 前端展示时建议优先显示：
   - 任务标题
   - 审核级别
   - 待审数量
   - 是否已被认领
   - 审核员 pending 数
   - 今日已审数量

### 审核员工作状态页面

调用 `/api/v1/admin/review/reviewers/progress`，展示：

- 当前待审量 `pendingCount`
- 今日产出 `todayReviewedCount`
- 历史已审量 `totalReviewedCount`
- 通过率 `approvalRate`
- 已认领任务 `claimedTasks`

---

## 6. 注意事项

- 这三个接口都是查询接口，不提供“管理员代分配”动作。
- 如果后续需要 Admin 代审核员认领任务，应新增独立写接口，并补充审计记录。
- `available=false` 通常表示该任务级别已被某个审核员整任务认领。
- `pendingCount=0` 的任务级别不会出现在可分配任务列表中。
- 当 `approvalRate` 无历史通过/驳回记录时返回 `0.00`。
