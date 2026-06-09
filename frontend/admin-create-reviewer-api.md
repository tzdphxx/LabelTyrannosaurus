# Admin 创建审核员账号接口对接文档

## 1. 接口概览

管理员创建审核员账号。该接口创建出的账号角色固定为 `REVIEWER`，账号默认启用并允许登录。

| 项目 | 说明 |
| --- | --- |
| 接口名称 | Admin 创建审核员账号 |
| 请求方法 | `POST` |
| 接口路径 | `/api/v1/admin/users/reviewers` |

## 2. 鉴权要求

该接口需要当前登录用户具备 `ADMIN` 角色。

请求头：

```http
Authorization: Bearer <accessToken>
Content-Type: application/json
```

鉴权失败时：

| 场景 | HTTP 状态码 | 说明 |
| --- | --- | --- |
| 未登录或 Token 无效 | `401 Unauthorized` | 需要重新登录或刷新 Token |
| 已登录但非 `ADMIN` 角色 | `403 Forbidden` | 当前用户无权创建审核员账号 |

## 3. 请求参数

### 3.1 请求体

```json
{
  "username": "reviewer01",
  "email": "reviewer01@example.com",
  "password": "Password123"
}
```

### 3.2 字段说明

| 字段 | 类型 | 必填 | 约束 | 说明 |
| --- | --- | --- | --- | --- |
| `username` | `String` | 是 | 非空，最大 64 字符 | 审核员用户名，全局唯一 |
| `email` | `String` | 是 | 非空，合法邮箱格式，最大 255 字符 | 审核员邮箱，全局唯一 |
| `password` | `String` | 是 | 非空，8 到 128 字符 | 初始登录密码，服务端使用 BCrypt 加密保存 |

## 4. 成功响应

HTTP 状态码：`200 OK`

```json
{
  "code": 0,
  "message": "OK",
  "data": {
    "userId": 12,
    "username": "reviewer01",
    "email": "reviewer01@example.com",
    "userType": "USER",
    "enabled": true,
    "loginEnabled": true,
    "tokenVersion": 1,
    "role": "REVIEWER"
  },
  "traceId": null
}
```

### 4.1 外层响应字段

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `code` | `Integer` | 业务码，`0` 表示成功 |
| `message` | `String` | 响应消息，成功时为 `OK` |
| `data` | `AdminUserResponse` | 新创建的审核员账号信息 |
| `traceId` | `String/null` | 请求追踪 ID，通常来自请求头 `X-Trace-Id` |

### 4.2 data 字段

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `userId` | `Long` | 新创建用户 ID |
| `username` | `String` | 用户名 |
| `email` | `String` | 邮箱 |
| `userType` | `String` | 用户类型，当前为 `USER` |
| `enabled` | `Boolean` | 账号是否启用，创建后为 `true` |
| `loginEnabled` | `Boolean` | 是否允许登录，创建后为 `true` |
| `tokenVersion` | `Integer` | Token 版本，创建后为 `1` |
| `role` | `String` | 用户角色，固定为 `REVIEWER` |

## 5. 错误响应

系统统一错误响应格式：

```json
{
  "code": 400102,
  "message": "错误信息",
  "data": null,
  "traceId": "trace-001"
}
```

## 管理端看板总览


**接口地址**:`/api/v1/admin/dashboard/overview`


**请求方式**:`GET`


**请求数据类型**:`application/x-www-form-urlencoded`


**响应数据类型**:`*/*`


**接口描述**:<p>返回 ADMIN 首页需要的 KPI、趋势、排行和异常提醒</p>



**请求参数**:


**请求参数**:


| 参数名称 | 参数说明 | 请求类型    | 是否必须 | 数据类型 | schema |
| -------- | -------- | ----- | -------- | -------- | ------ |
|range||query|false|string||


**响应状态**:


| 状态码 | 说明 | schema |
| -------- | -------- | ----- | 
|200|OK|ApiResponseAdminDashboardOverviewResponse|


**响应示例**:
```javascript
{
	"code": 0,
	"message": "",
	"data": {
		"range": "7d",
		"kpis": {
			"activeTaskCount": 12,
			"claimedCount": 340,
			"submittedCount": 286,
			"pendingReviewCount": 31,
			"approvalRate": 0.82,
			"rejectionRate": 0.18,
			"rewardAmount": 1280.5
		},
		"userSummary": {
			"totalUserCount": 126,
			"roleCounts": {},
			"disabledUserCount": 3,
			"newUserCount": 8
		},
		"trend": [
			{
				"date": "2026-06-01",
				"submittedCount": 42,
				"approvedCount": 35,
				"rejectedCount": 7,
				"rewardAmount": 188
			}
		],
		"taskStatusDistribution": {},
		"topLabelers": [
			{
				"labelerId": 20,
				"displayName": "labeler-a",
				"submittedCount": 46,
				"approvedCount": 39,
				"rewardAmount": 210
			}
		],
		"topTasks": [
			{
				"taskId": 1001,
				"title": "商品质检任务",
				"submittedCount": 120,
				"approvedCount": 98,
				"rejectedCount": 22
			}
		],
		"alerts": [
			{
				"type": "REVIEW_BACKLOG",
				"level": "WARNING",
				"title": "审核积压",
				"description": "当前有 31 条提交待审核",
				"targetPath": "/app/reviewer/queue"
			}
		],
		"generatedAt": "2026-06-03T21:30:00"
	},
	"traceId": ""
}
```