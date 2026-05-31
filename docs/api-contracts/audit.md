# Audit 审计日志接口

## 查询业务审计日志

- 接口名称：查询业务对象审计时间线
- URL：`/api/v1/audit-logs?bizType={bizType}&bizId={bizId}`
- Method：`GET`
- 权限角色：`ADMIN`、`OWNER`、`REVIEWER`
- Owner 模块：BE-B
- 前端使用页面：`AuditTimeline`、Reviewer 详情页、Owner 任务/数据变更历史

## 请求字段

|字段|类型|必填|说明|
|---|---|---|---|
|bizType|string|是|业务对象类型，例如 `SUBMISSION`、`AI_REVIEW`|
|bizId|number|是|业务对象 id|

## 响应字段

统一响应结构：

```json
{
  "code": 0,
  "message": "OK",
  "data": [],
  "traceId": null
}
```

`data` 数组元素：

|字段|类型|说明|
|---|---|---|
|auditLogId|number|审计日志 id|
|bizType|string|业务对象类型|
|bizId|number|业务对象 id|
|actorType|string|`USER` 或 `SYSTEM_AGENT`|
|actorId|number|null 允许，通常指向 `users.id`|
|action|string|审计动作|
|beforeJson|object|null 允许，操作前快照|
|afterJson|object|null 允许，操作后快照|
|traceId|string|链路追踪 id|
|agentRunId|number|null 允许，AI 审计时关联 BE-A 的 `agent_runs.id`|
|createdAt|string|创建时间|

## 错误码

|code|场景|
|---|---|
|401001|未登录或 token 失效|
|403001|无角色权限|
|400102|`bizType` 或 `bizId` 非法|
|500001|系统错误或历史审计 JSON 无法解析|

## 内部写入契约

BE-A/BE-B 只能通过 Java 接口 `AuditAppender.append(AuditCommand)` 写入审计，不允许直接更新或删除 `audit_logs`。

`AuditCommand` 字段：

|字段|类型|必填|说明|
|---|---|---|---|
|actorType|string|是|`USER` 或 `SYSTEM_AGENT`|
|actorId|number|否|审计主体 id|
|bizType|string|是|业务对象类型|
|bizId|number|是|业务对象 id|
|action|string|是|动作编码|
|beforeJson|object|否|操作前快照|
|afterJson|object|否|操作后快照|
|traceId|string|是|链路追踪 id，业务层强制必填|
|agentRunId|number|否|AI 审计关联运行 id|

影响的状态：无。审计日志只追加，不推进 BE-A 或 BE-B 业务状态。
