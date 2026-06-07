# LLM Provider 管理与任务创建者模型查看接口文档

## 1. 当前代码结论

本文档按当前后端代码整理，覆盖管理员设置 LLM Provider，以及任务创建者查看可选 LLM Provider / 模型。

| 使用方 | 能力 | 方法 | 路径 | 当前状态 |
| --- | --- | --- | --- | --- |
| 管理员 | 创建 LLM Provider | POST | `/api/v1/admin/llm-providers` | 已实现 |
| 管理员 | 更新 LLM Provider | PUT | `/api/v1/admin/llm-providers/{providerId}` | 已实现 |
| 管理员 | 启用 LLM Provider | POST | `/api/v1/admin/llm-providers/{providerId}/enable` | 已实现 |
| 管理员 | 停用 LLM Provider | POST | `/api/v1/admin/llm-providers/{providerId}/disable` | 已实现 |
| 管理员 | 测试 LLM Provider | POST | `/api/v1/admin/llm-providers/{providerId}/test` | 已实现 |
| 管理员 | 查看 LLM Provider 管理列表 | GET | `/api/v1/admin/llm-providers` | 当前未实现 |
| 任务创建者 | 查看可选 LLM Provider / 模型 | GET | `/api/v1/llm-providers` | 已实现 |

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

| 接口 | 要求角色 | 代码依据 |
| --- | --- | --- |
| `/api/v1/admin/llm-providers/**` | `ADMIN` | `CurrentUserContext.requireRole(RoleCode.ADMIN)` |
| `/api/v1/llm-providers` | `OWNER` | `CurrentUserContext.requireRole(RoleCode.OWNER)` |

安全规则：

- `apiKey` 仅在管理员创建、更新、测试请求中提交。
- 创建和更新时，已保存的 `apiKey` 由服务端 AES-GCM 加密落库。
- 所有接口响应都不会返回 `apiKey` 明文或密文。
- 响应中的敏感 Header 值会被替换成 `******`。
- 任务创建者查看接口当前复用完整 `LlmProviderResponse`，不是精简 DTO；因此会返回 `baseUrl`、masked `customHeaders`、限流字段、`apiKeyConfigured` 等字段。

---

## 2. 通用响应对象

管理员写接口和任务创建者查看接口当前都返回 `LlmProviderResponse`，字段如下。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | Long | Provider ID；任务 AI 配置中使用该值作为 `providerId` |
| providerCode | String | Provider 编码 |
| providerName | String | Provider 展示名称 |
| baseUrl | String | OpenAI-compatible Base URL |
| defaultModel | String | 默认模型名；当前每条 Provider 记录代表一个可选模型配置 |
| customHeaders | Object | 已保存的请求头；敏感值会被屏蔽 |
| enabled | Boolean | 是否启用 |
| platformRateLimitPerMinute | Integer | 平台级每分钟限流 |
| taskRateLimitPerMinute | Integer | 任务级每分钟限流 |
| userRateLimitPerMinute | Integer | 用户级每分钟限流 |
| supportVision | Boolean | 是否支持视觉输入 |
| supportMultiImage | Boolean | 是否支持多图输入 |
| maxImageCount | Integer | 单次请求最大图片数 |
| visionModel | String | 视觉模型名 |
| structuredOutputMode | String | 结构化输出模式 |
| apiKeyConfigured | Boolean | 是否已配置 API Key |
| createdBy | Long | 创建该 Provider 的管理员用户 ID |
| createdAt | String | 创建时间 |
| updatedAt | String | 更新时间 |

响应示例：

```json
{
  "code": 0,
  "message": "OK",
  "data": {
    "id": 30,
    "providerCode": "dashscope",
    "providerName": "DashScope Qwen Plus",
    "baseUrl": "https://dashscope.aliyuncs.com/compatible-mode/v1",
    "defaultModel": "qwen-plus",
    "customHeaders": {
      "Authorization": "******",
      "X-Trace-Source": "labelhub"
    },
    "enabled": true,
    "platformRateLimitPerMinute": 100,
    "taskRateLimitPerMinute": 50,
    "userRateLimitPerMinute": 20,
    "supportVision": false,
    "supportMultiImage": false,
    "maxImageCount": 10,
    "visionModel": null,
    "structuredOutputMode": "JSON_OBJECT",
    "apiKeyConfigured": true,
    "createdBy": 1,
    "createdAt": "2026-06-06T20:00:00",
    "updatedAt": "2026-06-06T20:00:00"
  },
  "traceId": null
}
```

---

## 3. 管理员创建 LLM Provider

管理员创建一个全局可用的 LLM Provider。创建成功后服务端默认设置 `enabled=true`。

**POST** `/api/v1/admin/llm-providers`

权限：`ADMIN`

### 请求体

| 字段 | 类型 | 必填 | 约束 | 说明 |
| --- | --- | --- | --- | --- |
| providerCode | String | 是 | 非空，最大 64 | Provider 编码，例如 `dashscope` |
| providerName | String | 是 | 非空，最大 100 | Provider 展示名称 |
| baseUrl | String | 是 | 非空，最大 500 | OpenAI-compatible Base URL；服务端会去掉末尾 `/` |
| apiKey | String | 是 | 非空，最大 4096 | 管理员配置的 API Key；服务端加密保存 |
| defaultModel | String | 是 | 非空，最大 128 | 默认模型名，也是 Owner 选择后实际使用的模型名 |
| customHeaders | Object | 否 | key/value 字符串 | 额外请求头；空 key 或空 value 会被忽略 |
| platformRateLimitPerMinute | Integer | 否 | `>= 0` | 平台级每分钟限流 |
| taskRateLimitPerMinute | Integer | 否 | `>= 0` | 任务级每分钟限流 |
| userRateLimitPerMinute | Integer | 否 | `>= 0` | 用户级每分钟限流 |
| supportVision | Boolean | 否 | - | 是否支持视觉输入；未传按 `false` 处理 |
| supportMultiImage | Boolean | 否 | - | 是否支持多图输入；未传按 `false` 处理 |
| maxImageCount | Integer | 否 | `>= 0` | 单次请求最大图片数；未传按 `10` 处理 |
| visionModel | String | 否 | 最大 100 | 视觉模型名 |
| structuredOutputMode | String | 否 | 最大 20 | `NONE`、`JSON_OBJECT`、`JSON_SCHEMA`；未传或非法值按 `NONE` 处理 |

请求示例：

```json
{
  "providerCode": "dashscope",
  "providerName": "DashScope Qwen Plus",
  "baseUrl": "https://dashscope.aliyuncs.com/compatible-mode/v1",
  "apiKey": "sk-***",
  "defaultModel": "qwen-plus",
  "customHeaders": {
    "Authorization": "Bearer custom-token",
    "X-Trace-Source": "labelhub"
  },
  "platformRateLimitPerMinute": 100,
  "taskRateLimitPerMinute": 50,
  "userRateLimitPerMinute": 20,
  "supportVision": false,
  "supportMultiImage": false,
  "maxImageCount": 10,
  "visionModel": null,
  "structuredOutputMode": "JSON_OBJECT"
}
```

响应：`data` 为 `LlmProviderResponse`。

---

## 4. 管理员更新 LLM Provider

管理员更新全局 LLM Provider。

**PUT** `/api/v1/admin/llm-providers/{providerId}`

权限：`ADMIN`

路径参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| providerId | Long | 是 | Provider ID |

请求体字段与创建接口一致，但 `apiKey` 可选：

- `apiKey` 为 `null`、缺省或空白字符串：保留原有加密 API Key。
- `apiKey` 非空：服务端加密后替换原有 API Key。

请求示例：

```json
{
  "providerCode": "dashscope",
  "providerName": "DashScope Qwen Max",
  "baseUrl": "https://dashscope.aliyuncs.com/compatible-mode/v1",
  "apiKey": null,
  "defaultModel": "qwen-max",
  "customHeaders": {},
  "platformRateLimitPerMinute": 120,
  "taskRateLimitPerMinute": 60,
  "userRateLimitPerMinute": 30,
  "supportVision": false,
  "supportMultiImage": false,
  "maxImageCount": 10,
  "visionModel": null,
  "structuredOutputMode": "JSON_OBJECT"
}
```

响应：`data` 为 `LlmProviderResponse`。

---

## 5. 管理员启用 LLM Provider

启用后，该 Provider 会出现在任务创建者模型列表中。

**POST** `/api/v1/admin/llm-providers/{providerId}/enable`

权限：`ADMIN`

请求体：无。

响应：`data` 为 `LlmProviderResponse`，其中 `enabled=true`。

---

## 6. 管理员停用 LLM Provider

停用后，该 Provider 不再出现在任务创建者模型列表中。

**POST** `/api/v1/admin/llm-providers/{providerId}/disable`

权限：`ADMIN`

请求体：无。

响应：`data` 为 `LlmProviderResponse`，其中 `enabled=false`。

---

## 7. 管理员测试 LLM Provider

用于测试 Provider 的 OpenAI-compatible 连通性。测试时可以临时覆盖 API Key、模型名和 Header；临时值不会保存。

**POST** `/api/v1/admin/llm-providers/{providerId}/test`

权限：`ADMIN`

路径参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| providerId | Long | 是 | Provider ID |

请求体：

| 字段 | 类型 | 必填 | 约束 | 说明 |
| --- | --- | --- | --- | --- |
| apiKey | String | 否 | 最大 4096 | 临时测试 API Key；未传则使用已保存 API Key |
| modelName | String | 否 | 最大 128 | 临时测试模型名；未传则使用 `defaultModel` |
| customHeaders | Object | 否 | key/value 字符串 | 临时附加请求头，会与已保存 Header 合并 |

请求示例：

```json
{
  "apiKey": null,
  "modelName": "qwen-plus",
  "customHeaders": {
    "X-Test": "true"
  }
}
```

响应 `data`：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| success | Boolean | 是否测试成功 |
| latencyMs | Long | 请求耗时，毫秒 |
| message | String | 测试结果描述；不会包含 API Key 明文 |

响应示例：

```json
{
  "code": 0,
  "message": "OK",
  "data": {
    "success": true,
    "latencyMs": 328,
    "message": "OK"
  },
  "traceId": null
}
```

---

## 8. 任务创建者查看可选 LLM Provider / 模型

任务创建者创建或编辑任务 AI 设置时调用该接口，获取管理员已启用的 Provider 列表。

**GET** `/api/v1/llm-providers`

权限：`OWNER`

查询参数：无。

请求体：无。

当前查询规则：

- 只返回 `enabled = true` 的 Provider。
- 按 `updated_at DESC` 排序。
- 当前未按 Owner 过滤。
- 当前未分页。

响应：`data` 为 `LlmProviderResponse[]`。

前端对接建议：

- 使用 `data[].id` 作为任务 AI 配置接口中的 `providerId`。
- 使用 `data[].providerName` 和 `data[].defaultModel` 展示模型选项。
- 不要展示 `apiKeyConfigured` 作为密钥内容；它只是布尔状态。
- 当前接口会返回 `baseUrl` 和 masked `customHeaders`，如前端只做模型选择，应忽略这些字段。

响应示例：

```json
{
  "code": 0,
  "message": "OK",
  "data": [
    {
      "id": 30,
      "providerCode": "dashscope",
      "providerName": "DashScope Qwen Plus",
      "baseUrl": "https://dashscope.aliyuncs.com/compatible-mode/v1",
      "defaultModel": "qwen-plus",
      "customHeaders": {
        "Authorization": "******"
      },
      "enabled": true,
      "platformRateLimitPerMinute": 100,
      "taskRateLimitPerMinute": 50,
      "userRateLimitPerMinute": 20,
      "supportVision": false,
      "supportMultiImage": false,
      "maxImageCount": 10,
      "visionModel": null,
      "structuredOutputMode": "JSON_OBJECT",
      "apiKeyConfigured": true,
      "createdBy": 1,
      "createdAt": "2026-06-06T20:00:00",
      "updatedAt": "2026-06-06T20:00:00"
    }
  ],
  "traceId": null
}
```

---

## 9. 管理员数据看板接口

管理员数据看板当前有一个聚合接口，返回管理后台首页需要的 KPI、趋势、分布、排行榜和异常提醒。

**GET** `/api/v1/admin/dashboard/overview`

权限：`ADMIN`

代码依据：

- Controller 路径：`AdminDashboardController`
- 权限注解：`@PreAuthorize("hasRole('ADMIN')")`
- 返回类型：`AdminDashboardOverviewResponse`

查询参数：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- | --- |
| range | String | 否 | `7d` | 统计周期。仅支持 `7d` 和 `30d`。非法值返回 `400102` |

响应 `data` 字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| range | String | 当前统计周期：`7d` 或 `30d` |
| kpis | Object | 平台核心 KPI |
| userSummary | Object | 用户总量、角色分布、新增/禁用用户摘要 |
| trend | Array | 按自然日补齐的趋势数据，长度等于 `range` 对应天数 |
| taskStatusDistribution | Object | 任务状态分布，固定包含 `DRAFT`、`PUBLISHED`、`PAUSED`、`ENDED` |
| topLabelers | Array | 周期内提交量靠前的标注员，最多 5 条 |
| topTasks | Array | 周期内提交量靠前的任务，最多 5 条 |
| alerts | Array | 看板异常提醒，仅用于展示，不自动处理业务状态 |
| generatedAt | String | 本次看板数据生成时间 |

`kpis` 字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| activeTaskCount | Long | 活跃任务数，包含当前已发布任务以及周期内产生领取或提交的任务 |
| claimedCount | Long | 周期内标注员领取次数 |
| submittedCount | Long | 周期内有效提交数，不包含 `SUPERSEDED` 提交 |
| pendingReviewCount | Long | 当前待终审提交数 |
| approvalRate | Decimal | 周期内审核通过率：`approved / (approved + rejected)`，分母为 0 时返回 0 |
| rejectionRate | Decimal | 周期内审核打回率：`rejected / (approved + rejected)`，分母为 0 时返回 0 |
| rewardAmount | Decimal | 周期内正向奖励支出汇总金额 |

`userSummary` 字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| totalUserCount | Long | 非 SYSTEM 用户总数 |
| roleCounts | Object | 角色人数分布，固定包含 `ADMIN`、`OWNER`、`LABELER`、`REVIEWER` |
| disabledUserCount | Long | 被禁用或禁止登录的非 SYSTEM 用户数 |
| newUserCount | Long | 统计周期内新增的非 SYSTEM 用户数 |

`trend[]` 字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| date | String | 自然日，格式 `yyyy-MM-dd` |
| submittedCount | Long | 当日有效提交数 |
| approvedCount | Long | 当日审核通过数 |
| rejectedCount | Long | 当日审核打回数 |
| rewardAmount | Decimal | 当日正向奖励支出金额 |

`topLabelers[]` 字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| labelerId | Long | 标注员用户 ID |
| displayName | String | 标注员展示名，优先 displayName，缺失时使用 username |
| submittedCount | Long | 周期内有效提交数 |
| approvedCount | Long | 周期内审核通过数 |
| rewardAmount | Decimal | 周期内获得的正向奖励金额 |

`topTasks[]` 字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| taskId | Long | 任务 ID |
| title | String | 任务标题 |
| submittedCount | Long | 周期内有效提交数 |
| approvedCount | Long | 周期内审核通过数 |
| rejectedCount | Long | 周期内审核打回数 |

`alerts[]` 字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| type | String | 提醒类型：`REVIEW_BACKLOG`、`HIGH_REJECTION_RATE_TASK`、`ZERO_SUBMISSION_ACTIVE_TASK`、`DISABLED_USER` |
| level | String | 提醒级别：`INFO`、`WARNING`、`CRITICAL` |
| title | String | 提醒标题 |
| description | String | 提醒说明 |
| targetPath | String | 前端可跳转的目标路径 |

响应示例：

```json
{
  "code": 0,
  "message": "OK",
  "data": {
    "range": "7d",
    "kpis": {
      "activeTaskCount": 12,
      "claimedCount": 340,
      "submittedCount": 286,
      "pendingReviewCount": 31,
      "approvalRate": 0.8200,
      "rejectionRate": 0.1800,
      "rewardAmount": 1280.50
    },
    "userSummary": {
      "totalUserCount": 126,
      "roleCounts": {
        "ADMIN": 2,
        "OWNER": 10,
        "LABELER": 100,
        "REVIEWER": 14
      },
      "disabledUserCount": 3,
      "newUserCount": 8
    },
    "trend": [
      {
        "date": "2026-06-01",
        "submittedCount": 42,
        "approvedCount": 35,
        "rejectedCount": 7,
        "rewardAmount": 188.00
      }
    ],
    "taskStatusDistribution": {
      "DRAFT": 4,
      "PUBLISHED": 8,
      "PAUSED": 1,
      "ENDED": 20
    },
    "topLabelers": [
      {
        "labelerId": 20,
        "displayName": "labeler-a",
        "submittedCount": 46,
        "approvedCount": 39,
        "rewardAmount": 210.00
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
    "generatedAt": "2026-06-07T10:00:00"
  },
  "traceId": null
}
```

---

## 11. 本轮未提交代码涉及的前端重新对接点

以下为当前未提交代码中已改变或补强的接口契约，前端需要按这些规则重新对接。

### 12.1 创建任务必须配置 AI 审核

**POST** `/api/v1/tasks`

当前创建任务时，后端会强制要求内联创建 AI 审核配置。前端至少需要传：

- `aiProviderId`
- `aiPrompt`
- `aiScoringDimensions`

不要在创建新任务时只传 `aiReviewConfigId`：AI 审核配置接口本身依赖已存在的 `taskId`，新任务创建阶段没有可合法引用的同任务配置。创建成功后，后端会把新建的 AI 配置 ID 回写到任务的 `aiReviewConfigId`。

阈值与策略可缺省：

| 字段 | 缺省规则 |
| --- | --- |
| `aiPassThreshold` | 未传时后端按 `80.00` 创建 |
| `aiManualReviewThreshold` | 未传时后端按 `60.00` 创建 |
| `aiFlowPolicy` | 未传时 AI 配置服务按 `MANUAL_FIRST` 创建 |
| `aiModelName` | 通过 AI 配置接口保存时可缺省，缺省使用 Provider `defaultModel` |

如果缺少内联 AI 配置，返回：

```json
{
  "code": 400104,
  "message": "创建任务必须配置 AI 审核：缺少 AI 模型供应商",
  "data": null,
  "traceId": "..."
}
```

前端建议：

- 建任务表单应把“选择 AI Provider / 填 Prompt / 配评分维度”做成必填步骤。
- 如果在创建任务弹窗中一次性配置 AI 审核，则传内联字段，不需要额外先调 AI 配置接口。
- 如果需要分步配置，建议先创建带最小 AI 配置的草稿任务，再通过 `/api/v1/tasks/{taskId}/ai-review-configs` 更新完整配置。

### 12.2 任务创建/编辑新增 `aiFlowPolicy`

涉及接口：

| 方法 | 路径 | 影响 |
| --- | --- | --- |
| POST | `/api/v1/tasks` | 内联创建 AI 配置时可传，后端会派生直接过审/直接打回开关 |
| PUT | `/api/v1/tasks/{taskId}` | 仅当草稿任务已有 AI 配置时生效，用于调整流转策略 |
| POST | `/api/v1/tasks/{taskId}/ai-review-configs` | 可传，用于保存完整 AI 审核配置 |
| PUT | `/api/v1/tasks/{taskId}/ai-review-configs/{configId}` | 可传，用于更新完整 AI 审核配置 |

可选值：

| 值 | 含义 | 后端派生开关 |
| --- | --- | --- |
| `MANUAL_FIRST` | AI 只提建议，结果一律转人工 | `allowAiDirectApprove=false`，`allowAiDirectReject=false` |
| `AI_PASS_ONLY` | 允许 AI 直接过审，打回仍转人工 | `allowAiDirectApprove=true`，`allowAiDirectReject=false` |
| `AI_REJECT_ONLY` | 允许 AI 直接打回，通过仍转人工 | `allowAiDirectApprove=false`，`allowAiDirectReject=true` |
| `AI_PASS_AND_REJECT` | 允许 AI 直接过审与直接打回 | `allowAiDirectApprove=true`，`allowAiDirectReject=true` |
| `ALWAYS_MANUAL` | 始终转人工 | `allowAiDirectApprove=false`，`allowAiDirectReject=false` |

前端建议：

- 任务创建页只展示一个“AI 流转策略”下拉，不要同时让用户手动编辑 `allowAiDirectApprove` 和 `allowAiDirectReject`，避免矛盾配置。
- 任务编辑页如果没有 AI 配置，传 `aiFlowPolicy` 不会创建配置；应先引导用户配置 AI 审核。
- 如果使用完整 AI 配置接口，仍可传 `allowAiDirectApprove` / `allowAiDirectReject`，但建议与 `aiFlowPolicy` 保持一致。

### 12.3 AI 审核配置 `modelName` 改为可选

涉及接口：

| 方法 | 路径 |
| --- | --- |
| POST | `/api/v1/tasks/{taskId}/ai-review-configs` |
| PUT | `/api/v1/tasks/{taskId}/ai-review-configs/{configId}` |

`modelName` 当前可缺省或传空白；后端会使用所选 LLM Provider 的 `defaultModel`。这与 Owner 查询 Provider 列表中的 `data[].defaultModel` 对应。

前端建议：

- 模型下拉默认选中 Provider `defaultModel`。
- 如果前端没有提供模型切换能力，可以只传 `providerId`，不传 `modelName`。
- 如果传了 `modelName`，应优先使用 `/api/v1/llm-providers` 返回的 `defaultModel`，避免与 Provider 能力不匹配。

### 12.4 导出创建不再要求前端透传 `X-Trace-Id`

**POST** `/api/v1/tasks/{taskId}/exports`

控制器已不再从 `HttpServletRequest` 读取 `X-Trace-Id` 参数传入服务。服务端会通过 `TraceIdProvider` 解析或生成 traceId。

前端对接不变：

- 请求路径不变。
- 请求体不变。
- 前端可以继续传 `X-Trace-Id`，但不应依赖它作为必填字段。

### 12.5 Reviewer 提交详情读取权限收紧

**GET** `/api/v1/reviewer/submissions/{submissionId}`

当前 Reviewer 只有在 `submission.assignedReviewerId` 等于当前用户 ID 时才能读取详情；Admin 可读取全部。

未分配 Reviewer 访问会返回：

```json
{
  "code": 403601,
  "message": "Reviewer is not assigned to this submission",
  "data": null,
  "traceId": "..."
}
```

前端建议：

- Reviewer 工作台列表应只展示分配给自己的提交。
- 详情页遇到 `403601` 时跳回审核列表或显示“该提交未分配给你”。
- 不要通过手动拼接 submissionId 直接进入详情页绕过列表。

### 12.6 LLM 异步任务失败兜底

涉及后端异步任务，不新增前端请求路径：

| 类型 | 影响 |
| --- | --- |
| AI 审核任务 | Worker 捕获异常后会写入 `FAILED` 的 AI 审核结果，并把提交转入待人工终审 |
| LLM Trigger | Worker 捕获异常后会把 Trigger Run 标记为 `FAILED` |
| AI 预标注 | Worker 捕获异常后会把预标注任务标记为 `FAILED` |

前端建议：

- 轮询 AI 审核、预标注、LLM Trigger 结果时，需要处理 `FAILED`，不要只等待 `SUCCESS`。
- `FAILED` 时展示 `errorMessage` 的脱敏摘要，并提供“重试”或“转人工处理”的入口。

---

## 13. 与代码存在的产品缺口

以下不是文档错误，而是当前代码实现与理想产品接口之间的差异：

- 当前没有 `GET /api/v1/admin/llm-providers` 管理员列表接口。管理员后台如果需要展示全部 Provider，需要补充该接口。

- 当前 Owner 查看接口返回完整 `LlmProviderResponse`，不是精简模型选项 DTO。它不会泄漏 API Key，但会返回 `baseUrl`、masked `customHeaders`、限流和 `apiKeyConfigured`。

- 当前 `structuredOutputMode` 非法值不会报错，而是静默归一化为 `NONE`。

- 当前没有独立的 `GET /api/v1/owner/dashboard` 聚合接口；Owner 看板需要由任务列表和单任务统计组合。

  
