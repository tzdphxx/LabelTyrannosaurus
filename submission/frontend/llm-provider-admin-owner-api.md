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
| 管理员 | 查看 LLM Provider 管理列表 | GET | `/api/v1/admin/llm-providers` | 已实现 |
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
| structuredOutputMode | String | 否 | 最大 20 | `NONE`、`JSON_OBJECT`、`JSON_SCHEMA`；未传或非法值保存为空，运行时按不强制结构化输出处理 |

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

## 8. 管理员查看 LLM Provider 管理列表

管理员后台用于展示全部 Provider 配置，包括已启用和已停用记录。

**GET** `/api/v1/admin/llm-providers`

权限：`ADMIN`

查询参数：无。

请求体：无。

当前查询规则：

- 返回全部 Provider，不按 `enabled` 过滤。
- 按 `updated_at DESC` 排序。
- 响应复用 `LlmProviderResponse[]`。
- 不返回 API Key 明文或密文；敏感 Header 继续显示为 `******`。

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
      "enabled": false,
      "platformRateLimitPerMinute": 100,
      "taskRateLimitPerMinute": 50,
      "userRateLimitPerMinute": 20,
      "supportVision": false,
      "supportMultiImage": false,
      "maxImageCount": 10,
      "visionModel": null,
      "structuredOutputMode": null,
      "apiKeyConfigured": true,
      "createdBy": 1,
      "createdAt": "2026-06-06T20:00:00",
      "updatedAt": "2026-06-06T20:00:00"
    }
  ],
  "traceId": null
}
```

前端对接建议：

- 管理后台列表使用 `enabled` 展示启用/停用状态。
- 不要把 `apiKeyConfigured` 当作密钥内容展示；它只是布尔状态。
- `structuredOutputMode = null` 表示该 Provider 不强制结构化输出。

---

## 9. 任务创建者查看可选 LLM Provider / 模型

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



## 10. 本轮未提交代码涉及的前端重新对接点

以下为当前未提交代码中已改变或补强的接口契约，前端需要按这些规则重新对接。

### 10.1 创建任务必须配置 AI 审核

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

### 10.2 任务创建/编辑新增 `aiFlowPolicy`

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

### 10.3 结构化输出解析失败纠错重试

涉及后端 LLM Gateway，不新增前端请求路径。

当前行为：

- Provider 配置的 `structuredOutputMode` 仅接受 `NONE`、`JSON_OBJECT`、`JSON_SCHEMA`。
- `structuredOutputMode` 未传或非法时保存为空；运行时等价于不强制结构化输出。
- 如果 Provider 调用成功但模型输出不是合法 JSON，后端会追加一条用户消息，携带解析错误和上一次输出片段，要求模型只返回合法 JSON 对象并重试一次。
- 第二次解析成功时返回正常结构化结果。
- 第二次仍解析失败时返回空 `structuredJson`，由下游 AI 审核、预标注或 LLM Trigger 流程按缺字段逻辑转人工或降级处理。

前端建议：

- 不要依赖非法 `structuredOutputMode` 被服务端转换成 `NONE`；前端下拉只提供 `NONE`、`JSON_OBJECT`、`JSON_SCHEMA`。
- 展示 AI 结果时仍需兼容空结构化结果，避免直接读取必有字段导致页面报错。
