# Contribution 标注员贡献接口

## 贡献总览

- URL: `/api/v1/labeler/contribution/overview`
- Method: `GET`
- 权限角色: `LABELER`、`ADMIN`
- Owner 模块: BE-B

响应体:

```json
{
  "code": 0,
  "message": "OK",
  "data": {
    "labelerId": 20,
    "claimedCount": 12,
    "submittedCount": 10,
    "pendingReviewCount": 6,
    "approvedCount": 3,
    "rejectedCount": 1,
    "totalReward": 9.00,
    "approvalRate": 0.7500
  },
  "traceId": null
}
```

说明:
- 当前接口只读取 JWT 中的当前用户，不接受前端传入 `labelerId`。
- `approvalRate = approvedCount / (approvedCount + rejectedCount)`，待审核不进入分母。

## 贡献趋势

- URL: `/api/v1/labeler/contribution/trend`
- Method: `GET`
- Query: `days`，默认 7，最大 31

响应体:

```json
{
  "code": 0,
  "message": "OK",
  "data": [
    {
      "statDate": "2026-05-23",
      "submittedCount": 0,
      "approvedCount": 0,
      "rejectedCount": 0,
      "rewardAmount": 0.00
    }
  ],
  "traceId": null
}
```

说明:
- 查询区间内无数据的日期补零。

## 任务贡献

- URL: `/api/v1/labeler/contribution/tasks`
- Method: `GET`
- Query: `limit` 默认 20，最大 100；`offset` 默认 0

响应体:

```json
{
  "code": 0,
  "message": "OK",
  "data": [
    {
      "taskId": 1,
      "taskTitle": "问答质检任务",
      "submittedCount": 10,
      "approvedCount": 3,
      "rejectedCount": 1,
      "totalReward": 9.00
    }
  ],
  "traceId": null
}
```

## 奖励流水

- URL: `/api/v1/labeler/rewards/ledger`
- Method: `GET`
- Query: `limit` 默认 20，最大 100；`offset` 默认 0

响应体:

```json
{
  "code": 0,
  "message": "OK",
  "data": [
    {
      "ledgerId": 300,
      "taskId": 1,
      "submissionId": 200,
      "assignmentId": 100,
      "amount": 3.00,
      "direction": "CREDIT",
      "reason": "审核通过奖励",
      "sourceEventId": "evt-1",
      "rewardType": "SUBMISSION_APPROVED",
      "createdAt": "2026-05-29T10:00:00"
    }
  ],
  "traceId": null
}
```

说明:
- `direction=CREDIT` 表示正向发放。
- `direction=DEBIT` 表示冲正扣回。
- 流水 append-only，不提供删除或修改接口。

## 错误码

| code | 场景 |
|---|---|
| `401001` | 未登录或 token 失效 |
| `403001` | 当前角色无权访问标注员贡献接口 |
| `500001` | 系统错误 |
