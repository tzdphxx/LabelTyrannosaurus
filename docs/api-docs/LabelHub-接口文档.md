# LabelHub 数据标注平台 — 接口文档

> 基于当前后端实现生成，版本日期：2026-06-01

## 目录

- [通用约定](#通用约定)
- [1. 认证模块](#1-认证模块)
- [2. 管理员用户管理](#2-管理员用户管理)
- [3. 任务管理](#3-任务管理)
- [4. 数据集管理](#4-数据集管理)
- [5. 模板管理](#5-模板管理)
- [6. 标注市场与领取](#6-标注市场与领取)
- [7. 草稿与提交](#7-草稿与提交)
- [8. 标注员提交记录](#8-标注员提交记录)
- [9. 审核模块](#9-审核模块)
- [10. 冲突仲裁](#10-冲突仲裁)
- [11. AI 审核](#11-ai-审核)
- [12. 大模型供应商配置](#12-大模型供应商配置)
- [13. LLM 触发器](#13-llm-触发器)
- [14. 预标注](#14-预标注)
- [15. Agent 运行记录](#15-agent-运行记录)
- [16. 奖励与贡献](#16-奖励与贡献)
- [17. 导出模块](#17-导出模块)
- [18. 文件存储](#18-文件存储)
- [19. 审计日志](#19-审计日志)
- [20. 媒体处理](#20-媒体处理)
- [附录 A：状态机枚举](#附录-a状态机枚举)
- [附录 B：统一错误码](#附录-b统一错误码)
- [附录 C：缺失接口清单](#附录-c缺失接口清单)
- [附录 E：AI 场景接入指导](#附录-eai-场景接入指导)

### D.11 环境变量联调注意事项

- LLM Provider 会加密保存 API Key；生产/联调环境建议显式配置 `LABELHUB_LLM_KEY_ENCRYPTION_SECRET`。`local` profile 在未配置该变量时提供本地 fallback，便于开发环境跑通接口，但正式部署不应依赖默认值。

---

## 通用约定

### 统一响应格式

成功响应 HTTP 200：

```json
{
  "code": 0,
  "message": "ok",
  "data": {},
  "traceId": "trace-id"
}
```

业务错误响应（HTTP 状态码根据错误类型动态映射）：

| 错误码前缀 | HTTP 状态码 | 说明 |
|------|------|------|
| 400xxx | 400 Bad Request | 参数校验失败、状态不允许操作 |
| 401xxx | 401 Unauthorized | 认证失败、令牌无效 |
| 403xxx | 403 Forbidden | 权限不足、无权访问资源 |
| 404xxx | 404 Not Found | 资源不存在 |
| 409xxx | 409 Conflict | 并发冲突、状态冲突 |
| 429xxx | 429 Too Many Requests | 限流 |
| 500xxx | 500 Internal Server Error | 系统内部错误 |

### 分页响应格式

```json
{
  "items": [],
  "page": 1,
  "pageSize": 20,
  "total": 0
}
```

### 认证方式

所有需要认证的接口在请求头中携带：

```
Authorization: Bearer <accessToken>
```

### 角色说明

| 角色 | 说明 |
|------|------|
| ADMIN | 系统管理员，管理用户和全局配置 |
| OWNER | 任务创建者，管理任务、数据集、模板、AI 配置 |
| REVIEWER | 审核员，审核提交和解决冲突 |
| LABELER | 标注员，领取任务、提交标注 |

---

## 1. 认证模块

### 1.1 POST /api/v1/auth/register

**作用**：注册新用户账号。注册成功后自动签发登录令牌，无需再次登录。仅允许注册 LABELER 或 OWNER 角色，REVIEWER/ADMIN/SYSTEM_AGENT 角色不可自注册。

**权限**：公开接口，无需认证

**请求体**：

| 字段 | 类型 | 必填 | 约束 | 说明 |
|------|------|------|------|------|
| username | String | 是 | 非空，最大 64 字符 | 用户名，全局唯一 |
| email | String | 是 | 非空，合法邮箱格式，最大 255 字符 | 邮箱地址，全局唯一 |
| password | String | 是 | 非空，8~128 字符 | 登录密码，服务端以 BCrypt 存储 |
| role | String | 是 | 非空，最大 32 字符，可选值：`LABELER`、`OWNER` | 注册身份角色 |

**响应体** `TokenResponse`：

| 字段 | 类型 | 说明 |
|------|------|------|
| accessToken | String | 访问令牌，默认 120 分钟过期 |
| refreshToken | String | 刷新令牌，默认 14 天过期 |
| tokenVersion | Integer | 令牌版本号，角色或状态变更后递增 |
| role | String | 用户角色：LABELER / OWNER |

**错误码**：400102（用户名或邮箱已存在 / 角色不允许注册）

---

### 1.2 POST /api/v1/auth/login

**作用**：用户登录。支持用户名或邮箱作为账号。仅允许 `enabled=true` 且 `loginEnabled=true` 的普通用户登录，系统用户（SYSTEM 类型）不可通过此接口登录。

**权限**：公开接口，无需认证

**请求体**：

| 字段 | 类型 | 必填 | 约束 | 说明 |
|------|------|------|------|------|
| account | String | 是 | 非空 | 用户名或邮箱 |
| password | String | 是 | 非空 | 登录密码 |

**响应体**：同 `TokenResponse`

**错误码**：401001（账号密码错误 / 账号被禁用 / 系统用户不可登录）；400102（用户拥有多个角色，状态异常）

---

### 1.3 POST /api/v1/auth/refresh

**作用**：使用刷新令牌换取新的令牌对。服务端校验 refreshToken 中的 tokenVersion 是否与数据库一致，角色变更或账号禁用后旧令牌自动失效。

**权限**：公开接口（需提供有效 refreshToken）

**请求体**：

| 字段 | 类型 | 必填 | 约束 | 说明 |
|------|------|------|------|------|
| refreshToken | String | 是 | 非空 | 上次登录或刷新获得的刷新令牌 |

**响应体**：同 `TokenResponse`

**错误码**：401001（refreshToken 无效或 tokenVersion 过期）

---

### 1.4 GET /api/v1/users/me

**作用**：获取当前已认证用户的基本信息，用于前端恢复登录态和渲染权限菜单。不返回密码、令牌等敏感字段。

**权限**：需认证（任意角色）

**请求参数**：无

**响应体** `UserProfileResponse`：

| 字段 | 类型 | 说明 |
|------|------|------|
| userId | Long | 用户 ID |
| username | String | 用户名 |
| email | String | 邮箱 |
| role | String | 当前角色 |

---

### 1.5 PUT /api/v1/users/me/password

**作用**：修改当前用户密码。校验旧密码后更新为新密码，成功后 tokenVersion 递增，旧令牌失效需重新登录。

**权限**：需认证（任意角色）

**请求体** `ChangePasswordRequest`：

| 字段 | 类型 | 必填 | 约束 | 说明 |
|------|------|------|------|------|
| oldPassword | String | 是 | 非空 | 当前密码 |
| newPassword | String | 是 | 8~128 字符 | 新密码 |

**响应体**：空（code=0 表示成功）

**错误码**：401001（旧密码错误）

---

### 1.6 PUT /api/v1/users/me/profile

**作用**：更新当前用户的个人信息。支持修改显示名称和邮箱，邮箱需全局唯一。

**权限**：需认证（任意角色）

**请求体** `UpdateProfileRequest`：

| 字段 | 类型 | 必填 | 约束 | 说明 |
|------|------|------|------|------|
| displayName | String | 否 | 最大 64 字符 | 显示名称 |
| email | String | 否 | 合法邮箱格式，最大 255 字符 | 新邮箱（需全局唯一） |

**响应体**：空（code=0 表示成功）

**错误码**：400102（邮箱已被占用）

---

## 2. 管理员用户管理

### 2.1 GET /api/v1/admin/users

**作用**：管理员查询后台用户列表。默认排除 `userType=SYSTEM` 的系统用户（如 system_ai_agent），避免系统主体暴露给普通管理流程。

**权限**：ADMIN

**请求参数**：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| includeSystem | boolean | 否 | false | 是否包含系统用户 |

**响应体** `List<AdminUserResponse>`：

| 字段 | 类型 | 说明 |
|------|------|------|
| userId | Long | 用户 ID |
| username | String | 用户名 |
| email | String | 邮箱 |
| roles | List&lt;String&gt; | 角色列表 |
| enabled | Boolean | 是否启用 |
| userType | String | 用户类型：USER / SYSTEM |
| createdAt | LocalDateTime | 创建时间 |

---

### 2.2 POST /api/v1/admin/users/{userId}/enable

**作用**：启用指定用户账号。启用后用户可重新登录，同时递增 tokenVersion 使旧令牌失效。

**权限**：ADMIN

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| userId | Long | 目标用户 ID |

**响应体**：空（code=0 表示成功）

---

### 2.3 POST /api/v1/admin/users/{userId}/disable

**作用**：禁用指定用户账号。禁用后用户不能登录，已有 token 因 tokenVersion 变化而失效。

**权限**：ADMIN

**路径参数**：同上

**响应体**：空

---

### 2.4 PUT /api/v1/admin/users/{userId}/roles

**作用**：替换指定用户的角色。每个用户只能拥有一个角色，变更后递增 tokenVersion 使旧令牌失效。

**权限**：ADMIN

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| userId | Long | 目标用户 ID |

**请求体** `UpdateUserRolesRequest`：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| role | String | 是 | 新角色，可选：LABELER / OWNER / REVIEWER / ADMIN |

**响应体**：空

---

### 2.5 POST /api/v1/admin/users/reviewers

**作用**：管理员直接创建审核员账号。审核员不可自注册，必须由管理员创建。

**权限**：ADMIN

**请求体** `CreateReviewerRequest`：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | String | 是 | 审核员用户名 |
| email | String | 是 | 审核员邮箱 |
| password | String | 是 | 初始密码 |

**响应体**：`AdminUserResponse`（同 2.1 列表项）

---

## 3. 任务管理

### 3.1 GET /api/v1/owner/tasks

**作用**：查询当前 OWNER 用户创建的所有任务摘要列表，用于任务管理首页展示。

**权限**：OWNER（通过 JWT 上下文自动获取 ownerId）

**请求参数**：无

**响应体** `List<OwnerTaskSummaryResponse>`：

| 字段 | 类型 | 说明 |
|------|------|------|
| taskId | Long | 任务 ID |
| title | String | 任务标题 |
| status | String | 任务状态：DRAFT / PUBLISHED / PAUSED / ENDED |
| quota | Integer | 任务配额 |
| claimedCount | Integer | 已领取数量 |
| createdAt | LocalDateTime | 创建时间 |

---

### 3.2 POST /api/v1/tasks

**作用**：创建草稿任务。任务归属当前 OWNER 用户。可同时指定 datasetFileId 来触发数据集导入。

**权限**：OWNER

**请求体** `CreateTaskRequest`：

| 字段 | 类型 | 必填 | 约束 | 说明 |
|------|------|------|------|------|
| title | String | 是 | 非空，最大 200 字符 | 任务标题 |
| description | String | 否 | - | 任务描述 |
| instructionRichText | String | 否 | - | 富文本标注说明 |
| tags | List&lt;String&gt; | 否 | 每个标签最大 64 字符 | 任务标签 |
| quota | Integer | 是 | ≥ 1 | 任务配额（可领取总数） |
| deadlineAt | LocalDateTime | 是 | 必须为未来时间 | 截止时间 |
| overlapCount | Integer | 是 | ≥ 1 | 每条数据需要的标注份数 |
| publishedTemplateVersionId | Long | 否 | - | 关联的模板版本 ID |
| **── AI 审核（引用已有 或 内联创建，二选一）──** |
| aiReviewConfigId | Long | 否 | - | 引用已创建的 AI 配置 ID |
| aiProviderId | Long | 否* | - | AI 模型供应商 ID（内联时必填） |
| aiModelName | String | 否 | 最大 128 字符 | 模型名（可选，须匹配 Provider defaultModel） |
| aiPrompt | String | 否* | 最大 10000 字符 | AI 审核 Prompt（内联时必填） |
| aiScoringDimensions | List&lt;String&gt; | 否* | 每项最大 64 字符 | 评分维度（内联时必填） |
| aiPassThreshold | BigDecimal | 否* | 0.00~100.00 | 通过阈值（内联时必填） |
| aiManualReviewThreshold | BigDecimal | 否* | 0.00~100.00 | 人工复核阈值（内联时必填） |
| **── 通用 ──** |
| reviewLevelCount | Integer | 否 | ≥ 1，默认 1 | 审核级别数 |
| datasetFileId | Long | 否 | - | 已上传的数据集文件 ID |

**响应体** `CreateTaskResponse`：

| 字段 | 类型 | 说明 |
|------|------|------|
| taskId | Long | 新建任务 ID |
| status | String | 任务状态，固定为 DRAFT |
| datasetImportJob | Object | 数据集导入任务信息（无 datasetFileId 时为 null） |

> **内联 AI 配置示例** — 一步创建任务 + AI 审核：
> ```json
> {
>   "title": "图像分类标注", "quota": 100, "overlapCount": 1,
>   "deadlineAt": "2026-07-01T23:59:59",
>   "aiProviderId": 50,
>   "aiPrompt": "请评估以下标注答案的质量...",
>   "aiScoringDimensions": ["准确性", "完整性"],
>   "aiPassThreshold": 80.00,
>   "aiManualReviewThreshold": 60.00,
>   "datasetFileId": 99
> }
> ```

---

### 3.3 GET /api/v1/tasks/{taskId}

**作用**：查询当前用户拥有的任务详情，包含完整配置信息。

**权限**：OWNER（仅能查看自己创建的任务）

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| taskId | Long | 任务 ID |

**响应体** `TaskDetailResponse`：

| 字段 | 类型 | 说明 |
|------|------|------|
| taskId | Long | 任务 ID |
| ownerId | Long | 创建者用户 ID |
| title | String | 任务标题 |
| description | String | 任务描述 |
| instructionRichText | String | 富文本标注说明 |
| status | String | 任务状态 |
| tags | List&lt;String&gt; | 标签列表 |
| quota | Integer | 配额 |
| claimedCount | Integer | 已领取数量 |
| overlapCount | Integer | 重叠标注数 |
| deadlineAt | LocalDateTime | 截止时间 |
| publishedTemplateVersionId | Long | 模板版本 ID |
| aiReviewConfigId | Long | AI 审核配置 ID |
| reviewLevelCount | Integer | 审核级别数（1=单级，2=初审+终审，3=初审+复审+终审） |
| rewardVisible | Boolean | 奖励是否对标注员可见 |
| publishedAt | LocalDateTime | 发布时间 |
| endedAt | LocalDateTime | 结束时间 |
| createdAt | LocalDateTime | 创建时间 |
| updatedAt | LocalDateTime | 更新时间 |

---

### 3.4 PUT /api/v1/tasks/{taskId}

**作用**：编辑草稿任务。仅允许编辑 DRAFT 状态的任务，已发布任务不可修改。

**权限**：OWNER

**请求体** `UpdateTaskRequest`：字段同 CreateTaskRequest（不含 datasetFileId）

**响应体** `TaskLifecycleResponse`：

| 字段 | 类型 | 说明 |
|------|------|------|
| taskId | Long | 任务 ID |
| status | String | 当前状态 |

**错误码**：400101（任务状态不允许编辑）

---

### 3.5 POST /api/v1/tasks/{taskId}/publish

**作用**：发布任务。发布前自动校验：数据集是否存在、模板版本是否存在、奖励规则是否配置、截止时间是否合理、配额和重叠数是否合法。校验通过后任务状态变为 PUBLISHED。

**权限**：OWNER

**响应体**：`TaskLifecycleResponse`

**错误码**：400101（状态不允许发布）；400102（校验不通过，缺少必要配置）

---

### 3.6 POST /api/v1/tasks/{taskId}/pause

**作用**：暂停已发布任务。暂停后标注员不可继续领取新题目，但已领取的可继续提交。

**权限**：OWNER

**状态迁移**：PUBLISHED → PAUSED

---

### 3.7 POST /api/v1/tasks/{taskId}/resume

**作用**：恢复已暂停任务为发布状态。

**权限**：OWNER

**状态迁移**：PAUSED → PUBLISHED

---

### 3.8 POST /api/v1/tasks/{taskId}/end

**作用**：结束任务。结束后不可再领取或提交，已有提交继续走审核流程。

**权限**：OWNER

**状态迁移**：PUBLISHED / PAUSED → ENDED

---

### 3.9 DELETE /api/v1/tasks/{taskId}

**作用**：删除草稿任务。仅允许删除 DRAFT 状态的任务，已发布/暂停/结束的任务不可删除。删除时同时清除关联的标签数据。

**权限**：OWNER（仅能删除自己创建的任务）

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| taskId | Long | 任务 ID |

**响应体**：空（code=0 表示成功）

**错误码**：404001（任务不存在）；400101（任务状态不允许删除）

---

### 3.10 GET /api/v1/owner/tasks（分页版）

**作用**：分页查询当前 OWNER 用户创建的任务列表，支持按状态和关键词筛选。替代原有无分页列表接口。

**权限**：OWNER

**请求参数**：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| status | String | 否 | - | 按状态筛选：DRAFT / PUBLISHED / PAUSED / ENDED |
| keyword | String | 否 | - | 按标题或描述模糊搜索 |
| page | int | 否 | 1 | 页码，从 1 开始 |
| size | int | 否 | 20 | 每页条数，最大 100 |

**响应体** `OwnerTaskPageResponse`：

| 字段 | 类型 | 说明 |
|------|------|------|
| items | List&lt;OwnerTaskSummaryResponse&gt; | 当前页任务列表 |
| page | int | 当前页码 |
| pageSize | int | 每页条数 |
| total | long | 总记录数 |

---

### 3.11 GET /api/v1/tasks/{taskId}/statistics

**作用**：查询任务的提交统计数据，包含总题目数、已领取、已提交、通过、驳回、待审核数量和通过率。用于 OWNER 任务管理仪表盘。

**权限**：OWNER（仅能查看自己创建的任务）

**响应体** `TaskStatisticsResponse`：

| 字段 | 类型 | 说明 |
|------|------|------|
| taskId | Long | 任务 ID |
| totalItems | int | 数据集总题目数 |
| claimedCount | int | 已领取数量 |
| submittedCount | int | 已提交总数（含通过和驳回） |
| approvedCount | int | 已通过数量 |
| rejectedCount | int | 已驳回数量 |
| pendingReviewCount | int | 待审核数量 |
| passRate | String | 通过率（如 "85.71%"） |

---

### 3.12 GET /api/v1/tasks/{taskId}/labelers

**作用**：查询任务下参与的标注员列表及其进度统计。用于 OWNER 查看任务参与情况。

**权限**：OWNER（仅能查看自己创建的任务）

**响应体** `List<TaskLabelerResponse>`：

| 字段 | 类型 | 说明 |
|------|------|------|
| labelerId | Long | 标注员 ID |
| username | String | 用户名 |
| displayName | String | 显示名称 |
| claimedCount | int | 已领取/编辑中数量 |
| submittedCount | int | 已提交数量 |
| approvedCount | int | 已通过数量 |
| rejectedCount | int | 已退回数量 |
| cancelledCount | int | 已放弃数量 |
| firstClaimedAt | DateTime | 首次领取时间 |
| lastActivityAt | DateTime | 最后活动时间 |

---

### 3.13 POST /api/v1/tasks/{taskId}/reviewers

**作用**：Owner 将审核员预分配到任务级别。替换已有分配（全量覆盖）。预分配后，自动分配调度器优先将该任务的提交分配给预分配的审核员。

**权限**：OWNER（仅能操作自己创建的任务）

**请求体** `AssignTaskReviewersRequest`：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| reviewerIds | List&lt;Long&gt; | 是 | 审核员 ID 列表 |

**响应体**：空（code=0 表示成功）

**错误码**：404001（任务不存在）；400103（指定用户不是审核员角色）

---

### 3.14 GET /api/v1/tasks/{taskId}/reviewers

**作用**：查询任务预分配的审核员列表。

**权限**：OWNER（仅能查看自己创建的任务）

**响应体** `List<TaskReviewerResponse>`：

| 字段 | 类型 | 说明 |
|------|------|------|
| reviewerId | Long | 审核员 ID |
| username | String | 用户名 |
| displayName | String | 显示名称 |
| assignedAt | DateTime | 分配时间 |

---

## 4. 数据集管理

### 4.1 GET /api/v1/tasks/{taskId}/dataset/items

**作用**：分页查询任务下未删除的数据项列表。支持按 externalId 精确筛选。

**权限**：ADMIN 或 OWNER

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| taskId | Long | 任务 ID |

**查询参数**：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| page | Integer | 否 | 1 | 页码，从 1 开始 |
| pageSize | Integer | 否 | 20 | 每页条数 |
| externalId | String | 否 | - | 按外部 ID 精确筛选 |

**响应体** `DatasetItemPageResponse`：

| 字段 | 类型 | 说明 |
|------|------|------|
| items | List | 数据项列表 |
| page | Integer | 当前页码 |
| pageSize | Integer | 每页条数 |
| total | Long | 总记录数 |

每个数据项包含：itemId、taskId、externalId、itemJson、assignedCount、submittedCount、approvedCount

---

### 4.2 POST /api/v1/tasks/{taskId}/dataset/items/batch-append

**作用**：向任务数据集批量追加数据项。同任务内 externalId 重复的项会进入错误报告。

**权限**：ADMIN 或 OWNER

**请求体** `BatchAppendItemsRequest`：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| items | List | 是 | 待追加的数据项列表 |

每个 item：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| externalId | String | 是 | 外部唯一标识 |
| itemJson | String | 是 | 题目内容 JSON |
| metadataJson | String | 否 | 元数据 JSON |

**响应体** `List<BatchItemResult>`：

| 字段 | 类型 | 说明 |
|------|------|------|
| itemId | Long | 数据项 ID（成功时） |
| externalId | String | 外部 ID |
| success | Boolean | 是否成功 |
| errorCode | String | 错误码（失败时） |
| errorMessage | String | 错误信息（失败时） |

---

### 4.3 POST /api/v1/tasks/{taskId}/dataset/items/batch-update

**作用**：批量更新任务数据项内容。已领取或已提交的题目不允许修改。

**权限**：ADMIN 或 OWNER

**请求体** `BatchUpdateItemsRequest`：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| items | List | 是 | 待更新的数据项列表 |

每个 item：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| itemId | Long | 是 | 数据项 ID |
| itemJson | String | 是 | 新的题目内容 JSON |
| metadataJson | String | 否 | 新的元数据 JSON |

**响应体**：同 `List<BatchItemResult>`

**错误码**：400101（题目已被领取，不可修改）

---

### 4.4 POST /api/v1/tasks/{taskId}/dataset/items/batch-delete

**作用**：批量软删除任务数据项。已领取或已提交的题目不允许删除。

**权限**：ADMIN 或 OWNER

**请求体** `BatchDeleteItemsRequest`：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| itemIds | List&lt;Long&gt; | 是 | 待删除的数据项 ID 列表 |

**响应体**：同 `List<BatchItemResult>`

---

### 4.5 POST /api/v1/tasks/{taskId}/dataset/import

**作用**：基于已上传文件创建追加导入任务。追加导入不覆盖已有题目，重复 externalId 进入错误报告。支持 JSON / JSONL / Excel 格式。

**权限**：ADMIN 或 OWNER

**请求体** `DatasetImportRequest`：

| 字段 | 类型 | 必填 | 约束 | 说明 |
|------|------|------|------|------|
| fileId | Long | 是 | 非空 | 已上传到对象存储的文件 ID |

**响应体** `DatasetImportJobResponse`：

| 字段 | 类型 | 说明 |
|------|------|------|
| jobId | Long | 导入任务 ID |
| taskId | Long | 关联任务 ID |
| status | String | 任务状态：PENDING / RUNNING / SUCCESS / FAILED |
| totalCount | Integer | 总行数 |
| successCount | Integer | 成功导入数 |
| failedCount | Integer | 失败数 |
| errorReportUrl | String | 错误报告下载地址（失败时） |
| createdAt | LocalDateTime | 创建时间 |

---

### 4.6 POST /api/v1/tasks/{taskId}/dataset/import/overwrite

**作用**：创建覆盖导入任务。仅允许任务处于 DRAFT 状态时使用，避免修改已发布、已领取或已提交的题目。

**权限**：ADMIN 或 OWNER

**请求体**：同 `DatasetImportRequest`

**响应体**：同 `DatasetImportJobResponse`

**错误码**：400101（任务非 DRAFT 状态，不允许覆盖导入）

---

### 4.7 GET /api/v1/tasks/{taskId}/dataset/import-jobs/{jobId}

**作用**：查询导入任务状态和错误报告。如果任务已生成错误报告，响应中包含短期签名下载地址。

**权限**：ADMIN 或 OWNER

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| taskId | Long | 任务 ID |
| jobId | Long | 导入任务 ID |

**响应体**：同 `DatasetImportJobResponse`

---

## 5. 模板管理

### 5.1 POST /api/v1/tasks/{taskId}/templates

**作用**：为任务创建模板并生成首个版本。模板定义了标注界面的字段结构（schema），标注员根据模板填写答案。

**权限**：ADMIN 或 OWNER

**请求体** `CreateTemplateRequest`：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | String | 是 | 模板名称 |
| schemaJson | String | 是 | 模板 Schema JSON，定义标注字段结构 |
| changeNote | String | 否 | 版本变更说明 |

**响应体** `TemplateResponse`：

| 字段 | 类型 | 说明 |
|------|------|------|
| templateId | Long | 模板 ID |
| taskId | Long | 所属任务 ID |
| name | String | 模板名称 |
| currentVersionNo | Integer | 当前版本号 |
| currentVersion | Object | 当前版本详情 |

---

### 5.2 GET /api/v1/tasks/{taskId}/templates

**作用**：查询任务下的模板列表及其当前版本信息。

**权限**：ADMIN 或 OWNER

**响应体**：`List<TemplateResponse>`

---

### 5.3 GET /api/v1/template-versions/{versionId}

**作用**：查询指定模板版本的详细信息，包含完整的 Schema JSON。

**权限**：ADMIN 或 OWNER

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| versionId | Long | 模板版本 ID |

**响应体** `TemplateVersionResponse`：

| 字段 | 类型 | 说明 |
|------|------|------|
| versionId | Long | 版本 ID |
| templateId | Long | 所属模板 ID |
| versionNo | Integer | 版本号 |
| schemaJson | String | Schema JSON |
| publishedSnapshot | Boolean | 是否已发布快照（发布后不可修改） |
| changeNote | String | 变更说明 |
| createdAt | LocalDateTime | 创建时间 |

---

### 5.4 POST /api/v1/templates/{templateId}/fork

**作用**：基于已有模板版本派生新版本。用于迭代模板设计，保留历史版本不变。

**权限**：ADMIN 或 OWNER

**请求体** `ForkTemplateRequest`：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| baseVersionId | Long | 是 | 基础版本 ID |
| schemaJson | String | 是 | 新版本的 Schema JSON |
| changeNote | String | 否 | 变更说明 |

**响应体**：`TemplateResponse`

---

### 5.5 POST /api/v1/schema/validate-answer

**作用**：按指定模板版本的 Schema 校验答案 JSON 是否合法。不修改任何业务数据，纯校验用途。供前端预校验和 BE-A 提交编排复用。

**权限**：ADMIN 或 OWNER

**请求体** `ValidateAnswerRequest`：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| schemaVersionId | Long | 是 | 模板版本 ID |
| answerJson | String | 是 | 待校验的答案 JSON |

**响应体** `List<SchemaValidationError>`：

| 字段 | 类型 | 说明 |
|------|------|------|
| field | String | 出错字段路径 |
| errorType | String | 错误类型 |
| message | String | 错误描述 |

**错误码**：409301（Schema 校验失败）

---

## 6. 标注市场与领取

### 6.1 GET /api/v1/market/tasks

**作用**：查询当前标注员可领取的已发布任务列表。返回任务详情字段，并在每个任务下附带可领取题目预览 `itemsPreview`，用于任务大厅直接展示“任务 + 题目”。只展示 PUBLISHED 且未过截止时间的任务。

**权限**：LABELER（通过 JWT 上下文获取用户 ID）

**查询参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | String | 否 | 按任务标题或描述关键词搜索 |
| tag | String | 否 | 按标签筛选 |
| status | String | 否 | 按任务状态筛选；非 PUBLISHED 返回空列表 |

**响应体** `List<MarketTaskResponse>`：

| 字段 | 类型 | 说明 |
|------|------|------|
| taskId | Long | 任务 ID |
| title | String | 任务标题 |
| description | String | 任务描述 |
| instructionRichText | String | 富文本标注说明 |
| status | String | 任务状态，通常为 PUBLISHED |
| tags | List&lt;String&gt; | 标签列表 |
| quota | Integer | 总配额 |
| overlapCount | Integer | 重叠标注数 |
| publishedTemplateVersionId | Long | 已发布模板版本 ID |
| deadlineAt | LocalDateTime | 截止时间 |
| availableCount | Integer | 当前标注员可领取题目数 |
| currentUserClaimedCount | Integer | 当前标注员已领取数量 |
| rewardSummary | Object | 奖励摘要；任务未开放奖励时为 null |
| itemsPreview | List&lt;MarketDatasetItemResponse&gt; | 当前标注员可领取题目预览 |

`itemsPreview` 每项：

| 字段 | 类型 | 说明 |
|------|------|------|
| datasetItemId | Long | 题目/数据项 ID |
| externalId | String | 外部题目 ID |
| itemJson | String | 题目内容 JSON |
| metadataJson | String | 题目元数据 JSON |

---

### 6.2 GET /api/v1/market/tasks/{taskId}

**作用**：查询任务大厅中某个任务的详情和可领取题目分页列表。用于进入任务大厅详情页后展示任务说明及其下面的题目。

**权限**：LABELER

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| taskId | Long | 任务 ID |

**查询参数**：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| itemPage | int | 否 | 1 | 题目页码，从 1 开始 |
| itemSize | int | 否 | 20 | 每页题目数，最大 100 |

**响应体**：同 `MarketTaskResponse`，其中 `itemsPreview` 为当前页可领取题目。

**错误码**：404501（任务不存在、未发布或已过截止时间）

---

### 6.3 POST /api/v1/tasks/{taskId}/assignments/claim

**作用**：标注员领取一个可标注的数据项。系统通过 Redis 分布式锁保证并发安全，同一标注员对同一数据项不可重复领取。领取后创建 assignment 记录，状态为 CLAIMED。

**权限**：LABELER

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| taskId | Long | 任务 ID |

**请求参数**：无

**响应体** `AssignmentClaimResponse`：

| 字段 | 类型 | 说明 |
|------|------|------|
| assignmentId | Long | 领取记录 ID |
| taskId | Long | 任务 ID |
| datasetItemId | Long | 数据项 ID |
| templateVersionId | Long | 模板版本 ID |
| status | String | 领取状态：CLAIMED |

**错误码**：409201（领取冲突，无可用题目或并发竞争失败）

---

### 6.4 GET /api/v1/labeler/claimed-tasks

**作用**：按任务聚合查询当前标注员已领取的内容。返回任务详情和当前标注员领取的题目预览，用于标注员页面展示“我领取的任务”。不会返回其他标注员领取的题目。

**权限**：LABELER

**查询参数**：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| page | int | 否 | 1 | 任务页码，从 1 开始 |
| size | int | 否 | 20 | 每页任务数，最大 100 |

**响应体** `List<LabelerClaimedTaskResponse>`：

| 字段 | 类型 | 说明 |
|------|------|------|
| taskId | Long | 任务 ID |
| title | String | 任务标题 |
| description | String | 任务描述 |
| instructionRichText | String | 富文本标注说明 |
| status | String | 任务状态 |
| quota | Integer | 总配额 |
| overlapCount | Integer | 重叠标注数 |
| deadlineAt | LocalDateTime | 截止时间 |
| publishedTemplateVersionId | Long | 已发布模板版本 ID |
| claimedItemCount | Long | 当前标注员在该任务下领取的题目数 |
| updatedAt | LocalDateTime | 当前标注员在该任务下最后活动时间 |
| itemsPreview | List&lt;LabelerClaimedItemResponse&gt; | 当前标注员领取的题目预览 |

---

### 6.5 GET /api/v1/labeler/claimed-tasks/{taskId}

**作用**：查询当前标注员在某个任务下已领取的题目分页列表，并返回任务详情。用于标注员进入某个已领取任务页面。

**权限**：LABELER（仅能查看自己的领取记录）

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| taskId | Long | 任务 ID |

**查询参数**：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| status | String | 否 | - | 按领取状态筛选：CLAIMED / DRAFTING / SUBMITTED / RETURNED / APPROVED / CANCELLED |
| page | int | 否 | 1 | 题目页码，从 1 开始 |
| size | int | 否 | 20 | 每页题目数，最大 100 |

**响应体**：同 `LabelerClaimedTaskResponse`，其中 `itemsPreview` 为当前页已领取题目。

`itemsPreview` 每项：

| 字段 | 类型 | 说明 |
|------|------|------|
| assignmentId | Long | 领取记录 ID |
| datasetItemId | Long | 题目/数据项 ID |
| assignmentStatus | String | 领取状态 |
| itemJson | String | 题目内容 JSON |
| metadataJson | String | 题目元数据 JSON |
| draftVersion | Integer | 草稿版本号 |
| latestSubmissionStatus | String | 最近一次有效提交状态；未提交时为 null |
| updatedAt | LocalDateTime | 最后更新时间 |

**错误码**：404402（当前标注员未领取该任务）

---

### 6.6 GET /api/v1/assignments/{assignmentId}

**作用**：查询标注员已领取的单个 assignment 详情，包含题目数据、模板信息、当前草稿和提交状态。用于进入具体标注页。该接口是单题详情，不是任务聚合查询。

**权限**：LABELER（仅能查看自己的 assignment）

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| assignmentId | Long | Assignment ID |

**响应体** `AssignmentDetailResponse`：

| 字段 | 类型 | 说明 |
|------|------|------|
| assignmentId | Long | 领取记录 ID |
| taskId | Long | 任务 ID |
| datasetItemId | Long | 数据项 ID |
| itemJson | String | 题目内容 JSON |
| templateVersionId | Long | 模板版本 ID |
| schemaJson | String | 模板 Schema JSON |
| assignmentStatus | String | 当前领取状态 |
| draftAnswerJson | String | 草稿答案 JSON |
| draftVersion | Integer | 草稿版本号 |
| latestSubmissionId | Long | 最近一次有效提交 ID |
| latestSubmissionStatus | String | 最近一次有效提交状态 |

---

### 6.7 GET /api/v1/labeler/assignments

**作用**：分页查询当前标注员的所有 assignment 记录，支持按任务和状态筛选。该接口保留为兼容的“已领取题目扁平列表”，适合轻量工作台列表；如需任务详情 + 已领取题目，请使用 `GET /api/v1/labeler/claimed-tasks`。

**权限**：LABELER

**请求参数**：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| taskId | Long | 否 | - | 按任务 ID 筛选 |
| status | String | 否 | - | 按状态筛选：CLAIMED / DRAFTING / SUBMITTED / RETURNED / APPROVED / CANCELLED |
| page | int | 否 | 1 | 页码，从 1 开始 |
| size | int | 否 | 20 | 每页条数，最大 100 |

**响应体** `List<LabelerAssignmentListItem>`：

| 字段 | 类型 | 说明 |
|------|------|------|
| assignmentId | Long | Assignment ID |
| taskId | Long | 任务 ID |
| taskTitle | String | 任务标题 |
| datasetItemId | Long | 数据项 ID |
| status | String | 当前状态 |
| draftVersion | Integer | 草稿版本号 |
| claimedAt | DateTime | 领取时间 |
| returnedAt | DateTime | 退回时间（未退回为 null） |
| updatedAt | DateTime | 最后更新时间 |

---

### 6.8 POST /api/v1/labeler/assignments/{assignmentId}/cancel

**作用**：标注员放弃已领取的 assignment，释放数据项回市场池。仅 CLAIMED / DRAFTING / RETURNED 状态可放弃。放弃后 assignment 状态变为 CANCELLED，对应数据项的 assigned_count 减 1，其他标注员可重新领取。

**权限**：LABELER（仅能操作自己的 assignment）

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| assignmentId | Long | Assignment ID |

**响应体**：空（code=0 表示成功）

**错误码**：
- 404401：Assignment 不存在
- 403401：无权操作（非本人 assignment）
- 409401：当前状态不可放弃（已提交/已通过等终态）

---
## 7. 草稿与提交

### 7.1 PUT /api/v1/assignments/{assignmentId}/draft

**作用**：保存当前标注任务的答案草稿。草稿使用乐观锁（draftVersion）防止并发覆盖。每次保存成功后 draftVersion 递增。

**权限**：LABELER（仅能操作自己的 assignment）

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| assignmentId | Long | Assignment ID |

**请求体** `AssignmentDraftSaveRequest`：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| answerJson | String | 是 | 草稿答案 JSON |
| clientVersion | Integer | 是 | 客户端持有的版本号，用于乐观锁校验 |

**响应体** `AssignmentDraftResponse`：

| 字段 | 类型 | 说明 |
|------|------|------|
| assignmentId | Long | Assignment ID |
| draftAnswerJson | String | 当前草稿内容 |
| draftVersion | Integer | 最新版本号 |
| updatedAt | LocalDateTime | 更新时间 |

**错误码**：409101（版本冲突，需刷新后重试）

---

### 7.2 GET /api/v1/assignments/{assignmentId}/draft

**作用**：读取当前标注任务的草稿内容。用于页面恢复或断点续标。

**权限**：LABELER

**响应体**：同 `AssignmentDraftResponse`

---

### 7.3 POST /api/v1/assignments/{assignmentId}/submit

**作用**：提交当前 assignment 的最终答案。提交后触发 Schema 校验，校验通过后创建 submission 记录并自动进入 AI 预审流程。提交后 assignment 状态变为 SUBMITTED。

**权限**：LABELER

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| assignmentId | Long | Assignment ID |

**请求体** `SubmissionSubmitRequest`：

| 字段 | 类型 | 必填 | 约束 | 说明 |
|------|------|------|------|------|
| answerJson | String | 是 | 非空 | 最终答案 JSON |
| clientVersion | Integer | 是 | 非空 | 客户端版本号，防止并发提交 |

**响应体** `SubmissionSubmitResponse`：

| 字段 | 类型 | 说明 |
|------|------|------|
| submissionId | Long | 提交记录 ID |
| assignmentId | Long | Assignment ID |
| status | String | 提交状态：SUBMITTED / AI_REVIEWING |
| versionNo | Integer | 提交版本号 |

**错误码**：400101（状态不允许提交）；409301（Schema 校验失败）；409101（版本冲突）

---

## 8. 标注员提交记录

### 8.1 GET /api/v1/labeler/submissions

**作用**：标注员查看自己的提交记录列表。支持按任务、提交状态、领取状态筛选，分页返回。

**权限**：LABELER

**查询参数**：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| taskId | Long | 否 | - | 按任务 ID 筛选 |
| submissionStatus | String | 否 | - | 按提交状态筛选：AI_REVIEWING / PENDING_FINAL / APPROVED / REJECTED |
| assignmentStatus | String | 否 | - | 按领取状态筛选：CLAIMED / SUBMITTED / RETURNED / APPROVED |
| page | int | 否 | 1 | 页码，从 1 开始 |
| size | int | 否 | 20 | 每页条数 |

**响应体** `List<LabelerSubmissionListItem>`：

| 字段 | 类型 | 说明 |
|------|------|------|
| submissionId | Long | 提交 ID |
| assignmentId | Long | Assignment ID |
| taskId | Long | 任务 ID |
| taskTitle | String | 任务标题 |
| submissionStatus | String | 提交状态 |
| assignmentStatus | String | 领取状态 |
| versionNo | Integer | 提交版本号 |
| submittedAt | LocalDateTime | 提交时间 |

---

### 8.2 GET /api/v1/labeler/submissions/{submissionId}

**作用**：查看单条提交的详细信息，包含答案内容、AI 审核结果、审核状态等。

**权限**：LABELER（仅能查看自己的提交）

**响应体** `LabelerSubmissionDetailResponse`：

| 字段 | 类型 | 说明 |
|------|------|------|
| submissionId | Long | 提交 ID |
| assignmentId | Long | Assignment ID |
| answerJson | String | 答案内容 |
| status | String | 提交状态 |
| aiDecision | String | AI 结论 |
| aiAverageScore | BigDecimal | AI 平均分 |
| reviewComment | String | 审核评语 |
| submittedAt | LocalDateTime | 提交时间 |

---

## 9. 审核模块

> **多级审核流程**：任务创建时通过 `reviewLevelCount` 设置审核级别数（默认 1）。设置为 3 时流程为：初审(level 1) → 复审(level 2) → 终审(level 3)。每级通过后自动升级到下一级并清空分配，等待新的审核员认领/分配。**同一审核员不能审同一条提交的多个级别**（403601 错误码）。

### 9.1 POST /api/v1/reviewer/submissions/claim

**作用**：审核员主动从未分配池中领取待审提交。使用 Redis 锁防止同一审核员并发领取，数据库行锁（`FOR UPDATE SKIP LOCKED`）保证多审核员同时领取不会拿到同一条。领取后自动更新 `submission.assigned_reviewer_id`，写入 ReviewRecord 和审计日志。

**权限**：REVIEWER

**查询参数**：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| count | int | 否 | 10 | 领取数量，最大 50 |
| taskId | Long | 否 | - | 按任务 ID 筛选（只领取某个任务的待审提交） |

**响应体** `ReviewClaimResponse`：

| 字段 | 类型 | 说明 |
|------|------|------|
| claimedSubmissionIds | List&lt;Long&gt; | 成功领取的提交 ID 列表 |
| claimedCount | int | 实际领取数量（可能小于请求数量） |

**错误码**：409201（领取繁忙，请重试）

**使用场景**：
- 审核员上班后主动领取一批待审提交到自己名下
- 领取后通过 `GET /api/v1/reviewer/submissions?assignedReviewerId=<自己ID>` 查看自己的待审列表
- 审核完一批后可再次 claim 领取下一批

**并发安全**：
- 多个审核员同时 claim 不会拿到同一条提交
- 同一审核员短时间内重复 claim 会被 Redis 锁拦截

---

### 9.2 GET /api/v1/reviewer/tasks

**作用**：审核员查看有待审提交的任务列表，作为工作台入口导航。返回每个任务的待审总数和分配给当前审核员的待审数。

**权限**：REVIEWER

**请求参数**：无

**响应体** `List<ReviewerTaskSummary>`：

| 字段 | 类型 | 说明 |
|------|------|------|
| taskId | Long | 任务 ID |
| taskTitle | String | 任务标题 |
| pendingCount | int | 该任务待审总数 |
| myPendingCount | int | 分配给当前审核员的待审数 |
| totalReviewedCount | int | 该任务已审总数 |

---

### 9.3 GET /api/v1/reviewer/dashboard

**作用**：返回当前审核员的工作统计概览，用于审核工作台首页展示。

**权限**：REVIEWER

**请求参数**：无

**响应体** `ReviewerDashboardResponse`：

| 字段 | 类型 | 说明 |
|------|------|------|
| pendingCount | int | 当前待审数（分配给自己的 PENDING_FINAL 提交） |
| todayReviewedCount | int | 今日已审数 |
| totalApproved | int | 累计通过数 |
| totalRejected | int | 累计驳回数 |
| approvalRate | BigDecimal | 通过率（百分比，如 85.50） |

---

### 9.4 GET /api/v1/reviewer/submissions

**作用**：审核员查询可处理的提交列表。支持多维筛选：按任务、提交状态、AI 结论、AI 审核状态、冲突状态、审核级别和指定审核员过滤。

**权限**：REVIEWER

**查询参数**：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| taskId | Long | 否 | - | 按任务 ID 筛选 |
| submissionStatus | String | 否 | - | 按提交状态筛选 |
| aiDecision | String | 否 | - | 按 AI 结论筛选：PASS / REJECT / MANUAL_REVIEW |
| aiReviewStatus | String | 否 | - | 按 AI 审核状态筛选 |
| conflictStatus | String | 否 | - | 按冲突状态筛选 |
| reviewLevel | Integer | 否 | - | 按审核级别筛选 |
| assignedReviewerId | Long | 否 | - | 按分配的审核员 ID 筛选 |
| page | int | 否 | 1 | 页码，从 1 开始 |
| size | int | 否 | 20 | 每页条数，最大 100 |

**响应体** `List<ReviewerSubmissionListItem>`：

| 字段 | 类型 | 说明 |
|------|------|------|
| submissionId | Long | 提交 ID |
| taskId | Long | 任务 ID |
| labelerId | Long | 标注员 ID |
| submissionStatus | String | 提交状态 |
| aiDecision | String | AI 结论 |
| aiReviewStatus | String | AI 审核状态 |
| conflictStatus | String | 冲突状态 |
| reviewLevel | Integer | 审核级别 |
| assignedReviewerId | Long | 分配的审核员 |

---

### 9.5 GET /api/v1/reviewer/submissions/{submissionId}

**作用**：查询指定提交的审核详情，包含标注答案、AI 评分、审核历史、冲突信息等。用于审核员工作台渲染。

**权限**：REVIEWER

**响应体** `ReviewerSubmissionDetailResponse`：

| 字段 | 类型 | 说明 |
|------|------|------|
| submissionId | Long | 提交 ID |
| taskId | Long | 任务 ID |
| assignmentId | Long | 分配 ID |
| datasetItemId | Long | 数据项 ID |
| labelerId | Long | 标注员 ID |
| versionNo | Integer | 版本号 |
| submissionStatus | String | 提交状态 |
| answerJson | String | 标注答案 JSON |
| itemJson | String | 题目数据 JSON |
| templateVersionId | Long | 模板版本 ID |
| schemaJson | String | 模板 Schema JSON |
| aiReviewResult | AiReviewSummary | AI 审核结果摘要 |
| agentRunSummary | AgentRunSummary | Agent 运行摘要 |
| reviewRecords | List&lt;ReviewRecordItem&gt; | 审核记录列表 |
| versionHistory | List&lt;VersionHistoryItem&gt; | 版本历史 |
| latestPreAnnotation | LatestPreAnnotationSummary | 最新预标注摘要 |

**嵌套对象 `AiReviewSummary`**：

| 字段 | 类型 | 说明 |
|------|------|------|
| aiReviewResultId | Long | AI 审核结果 ID |
| agentRunId | Long | Agent 运行 ID |
| status | String | AI 审核状态 |
| decision | String | AI 结论 |
| averageScore | String | 平均分 |
| riskFlags | String | 风险标记 |
| suggestion | String | AI 建议 |
| errorCode | String | 错误码 |
| promptMode | String | Prompt 模式 |
| degraded | Boolean | 是否降级 |
| limitations | String | 限制说明 |

**嵌套对象 `AgentRunSummary`**：

| 字段 | 类型 | 说明 |
|------|------|------|
| agentRunId | Long | Agent 运行 ID |
| agentType | String | Agent 类型 |
| modelName | String | 模型名称 |
| status | String | 运行状态 |
| startedAt | DateTime | 开始时间 |
| finishedAt | DateTime | 结束时间 |

**嵌套对象 `ReviewRecordItem`**：

| 字段 | 类型 | 说明 |
|------|------|------|
| reviewRecordId | Long | 审核记录 ID |
| reviewerId | Long | 审核员 ID |
| action | String | 审核动作 |
| reviewLevel | Integer | 审核级别 |
| reason | String | 原因 |
| reviewComment | String | 审核评语 |
| createdAt | DateTime | 创建时间 |

**嵌套对象 `VersionHistoryItem`**：

| 字段 | 类型 | 说明 |
|------|------|------|
| submissionId | Long | 提交 ID |
| versionNo | Integer | 版本号 |
| status | String | 提交状态 |
| isGolden | Boolean | 是否为黄金标注 |
| createdAt | DateTime | 创建时间 |

**嵌套对象 `LatestPreAnnotationSummary`**：

| 字段 | 类型 | 说明 |
|------|------|------|
| preAnnotationId | Long | 预标注 ID |
| agentRunId | Long | Agent 运行 ID |
| status | String | 状态 |
| suggestedAnswerJson | String | 建议答案 JSON |
| fieldSuggestions | String | 字段建议 |
| riskFlags | String | 风险标记 |
| overallConfidence | String | 整体置信度 |
| limitations | String | 限制说明 |
| promptMode | String | Prompt 模式 |
| degraded | Boolean | 是否降级 |
| ignoredFields | String | 忽略字段 |
| mediaUnderstanding | String | 媒体理解 |
| finalDiff | String | 最终差异 |

---

### 9.6 POST /api/v1/reviewer/submissions/{submissionId}/approve

**作用**：审核通过指定提交。通过后 submission 状态变为 APPROVED，触发 SubmissionApproved 事件用于奖励结算。

**权限**：REVIEWER

**请求体** `ApproveRequest`：

| 字段 | 类型 | 必填 | 约束 | 说明 |
|------|------|------|------|------|
| reviewComment | String | 否 | - | 审核评语 |
| reviewLevel | int | 是 | ≥ 1 | 审核级别（MVP 固定传 1） |

**响应体** `ReviewActionResponse`：

| 字段 | 类型 | 说明 |
|------|------|------|
| submissionId | Long | 提交 ID |
| action | String | 执行的操作：APPROVE |
| newStatus | String | 变更后状态 |

---

### 9.7 POST /api/v1/reviewer/submissions/{submissionId}/reject

**作用**：审核驳回指定提交。驳回后 submission 状态变为 REJECTED，assignment 状态变为 RETURNED，标注员可重新修改后提交。

**权限**：REVIEWER

**请求体** `RejectRequest`：

| 字段 | 类型 | 必填 | 约束 | 说明 |
|------|------|------|------|------|
| reason | String | 是 | 非空 | 驳回原因 |
| reviewLevel | int | 是 | ≥ 1 | 审核级别（MVP 固定传 1） |

**响应体**：同 `ReviewActionResponse`（action=REJECT）

---

### 9.8 POST /api/v1/reviewer/submissions/batch/approve

**作用**：批量审核通过多个提交。

**权限**：REVIEWER

**请求体** `BatchApproveRequest`：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| submissionIds | List&lt;Long&gt; | 是 | 待通过的提交 ID 列表 |

**响应体** `BatchReviewResponse`：

| 字段 | 类型 | 说明 |
|------|------|------|
| successCount | Integer | 成功数 |
| failedCount | Integer | 失败数 |
| failures | List | 失败明细列表 |

---

### 9.9 POST /api/v1/reviewer/submissions/batch/reject

**作用**：批量审核驳回多个提交。

**权限**：REVIEWER

**请求体** `BatchRejectRequest`：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| submissionIds | List&lt;Long&gt; | 是 | 待驳回的提交 ID 列表 |
| reason | String | 是 | 统一驳回原因 |

**响应体**：同 `BatchReviewResponse`

---

### 9.10 POST /api/v1/reviewer/submissions/batch/mark-manual

**作用**：将多个提交批量标记为需要人工处理（转人工复核）。用于 AI 预审结论不确定时由审核员手动标记。

**权限**：REVIEWER

**请求体** `BatchMarkManualRequest`：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| submissionIds | List&lt;Long&gt; | 是 | 待标记的提交 ID 列表 |

**响应体**：同 `BatchReviewResponse`

---

### 9.11 POST /api/v1/reviewer/submissions/batch/assign

**作用**：批量将提交分配给指定审核员。用于审核任务调度。

**权限**：REVIEWER

**请求体** `BatchAssignRequest`：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| submissionIds | List&lt;Long&gt; | 是 | 待分配的提交 ID 列表 |
| reviewerId | Long | 是 | 目标审核员 ID |

**响应体**：同 `BatchReviewResponse`

> **注**：以上批量接口同时支持 `/batch/xxx` 和 `/batch-xxx` 两种路径（兼容契约）。

---

### 9.12 GET /api/v1/reviewer/ai-review-status

**作用**：获取当前审查员所有负责提交的 AI 预审状态（轻量接口，专注 AI 审核信息）。

**权限**：REVIEWER

**响应体** `List<ReviewerAiReviewStatusItem>`：

| 字段 | 类型 | 说明 |
|------|------|------|
| submissionId | Long | 提交 ID |
| taskId | Long | 任务 ID |
| taskTitle | String | 任务标题 |
| submissionStatus | SubmissionStatus | 提交状态 |
| aiReviewStatus | AiReviewStatus | AI 审核状态（PENDING / SUCCESS / FAILED 等） |
| aiDecision | String | AI 决策（PASS / REJECT / MANUAL_REVIEW） |
| averageScore | String | AI 平均评分 |
| assignedToMe | Boolean | 是否直接分配给当前审查员 |
| submittedAt | LocalDateTime | 提交时间 |

**查询范围**：`submissionStatus IN ('PENDING_FINAL', 'AI_REVIEWING')` 且（`assigned_reviewer_id = 当前用户` 或 `review_tasks` 中有分配给当前用户的待审记录）。

---

## 10. 冲突仲裁

### 10.1 GET /api/v1/reviewer/conflict-groups

**作用**：查询待解决的冲突组列表。当同一数据项有多个标注员提交了不同答案（overlapCount > 1），系统自动检测冲突并生成冲突组，等待审核员仲裁选择金标答案。

**权限**：REVIEWER

**查询参数**：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| limit | int | 否 | 100 | 返回条数上限，最大 500 |

**响应体** `List<ConflictGroupResponse>`：

| 字段 | 类型 | 说明 |
|------|------|------|
| groupId | Long | 冲突组 ID |
| taskId | Long | 任务 ID |
| datasetItemId | Long | 数据项 ID |
| status | String | 冲突状态：CONFLICTED / RESOLVED |
| submissions | List | 冲突组内的提交列表 |
| resolvedAt | LocalDateTime | 解决时间 |

---

### 10.2 GET /api/v1/reviewer/conflict-groups/{groupId}

**作用**：查询冲突组详情，包含所有冲突提交的答案内容和 AI 评分，供审核员对比选择。

**权限**：REVIEWER

**响应体**：同 `ConflictGroupResponse`（含完整提交详情）

---

### 10.3 POST /api/v1/reviewer/conflict-groups/{groupId}/resolve

**作用**：选择最终金标提交并完成冲突仲裁。被选中的提交标记为 isGolden=true，触发 GoldenSelected 事件用于奖励结算。

**权限**：REVIEWER

**请求体** `ConflictResolveRequest`：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| goldenSubmissionId | Long | 是 | 选定为金标的提交 ID |
| resolveComment | String | 否 | 仲裁说明 |

**响应体** `ConflictResolveResponse`：

| 字段 | 类型 | 说明 |
|------|------|------|
| groupId | Long | 冲突组 ID |
| goldenSubmissionId | Long | 金标提交 ID |
| status | String | 变更后状态：RESOLVED |

---

## 11. AI 审核

### 11.1 POST /api/v1/tasks/{taskId}/ai-review-configs

**作用**：创建或保存任务的 AI 审核配置。配置包含 LLM 供应商、模型、Prompt 模板、评分维度、通过阈值等。任务发布前必须配置 AI 审核。

**权限**：OWNER

**请求体** `AiReviewConfigRequest`：

| 字段 | 类型 | 必填 | 约束 | 说明 |
|------|------|------|------|------|
| providerId | Long | 是 | 非空 | Admin 启用的模型 ID |
| modelName | String | 否 | 最大 128 字符 | 模型名（可选，如提供则必须等于 Provider defaultModel，否则 400402） |
| promptTemplate | String | 是 | 非空，最大 10000 字符 | Prompt 模板 |
| scoringDimensions | List&lt;String&gt; | 是 | 非空列表，每项最大 64 字符 | 评分维度列表 |
| passThreshold | BigDecimal | 是 | 0.00~100.00 | 通过阈值 |
| manualReviewThreshold | BigDecimal | 是 | 0.00~100.00 | 人工复核阈值 |
| maxRetry | Integer | 否 | 0~10 | 最大重试次数 |
| aiFlowPolicy | String | 否 | - | AI 流程策略 |
| allowAiDirectApprove | Boolean | 否 | - | 是否允许 AI 直接通过 |
| allowAiDirectReject | Boolean | 否 | - | 是否允许 AI 直接驳回 |
| rejectThreshold | BigDecimal | 否 | 0.00~100.00 | 驳回阈值 |
| confidenceThreshold | BigDecimal | 否 | 0.00~1.00 | 置信度阈值 |
| riskFlagsForceManual | List&lt;String&gt; | 否 | - | 触发强制人工的风险标记列表 |
| multimodalEnabled | Boolean | 否 | 默认 true | 是否启用多模态 |
| degradationPenalty | BigDecimal | 否 | 0.00~1.00，默认 0.20 | 降级惩罚系数 |
| visionDetail | String | 否 | 最大 20 字符，默认 "auto" | 视觉细节级别 |
| maxImagesPerRequest | Integer | 否 | 0~20，默认 5 | 每次请求最大图片数 |
| allowAiDirectApproveWhenDegraded | Boolean | 否 | 默认 false | 降级时是否允许 AI 直接通过 |

**响应体** `AiReviewConfigResponse`：包含 configId 和完整配置字段

---

### 11.2 PUT /api/v1/tasks/{taskId}/ai-review-configs/{configId}

**作用**：更新指定 AI 审核配置。

**权限**：OWNER

**请求体**：同 `AiReviewConfigRequest`

---

### 11.3 GET /api/v1/tasks/{taskId}/ai-review-configs

**作用**：查询任务当前 AI 审核配置。

**权限**：OWNER

**响应体**：`AiReviewConfigResponse`

---

### 11.4 POST /api/v1/tasks/{taskId}/ai-review-configs/{configId}/test

**作用**：用样例输入测试 AI 审核提示词和输出结构。不影响实际业务数据，用于配置调试。

**权限**：OWNER

**请求体** `AiReviewPromptTestRequest`：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| sampleAnswerJson | String | 是 | 样例答案 JSON |
| sampleItemJson | String | 否 | 样例题目 JSON |

**响应体** `AiReviewPromptTestResponse`：

| 字段 | 类型 | 说明 |
|------|------|------|
| success | Boolean | 是否成功 |
| rawResponse | String | LLM 原始响应 |
| parsedResult | Object | 解析后的结构化结果 |
| latencyMs | Long | 耗时毫秒 |
| errorMessage | String | 错误信息 |

---

### 11.5 GET /api/v1/submissions/{submissionId}/ai-review

**作用**：获取指定提交的 AI 预审结果，包含各维度评分、结论、置信度、风险标记和原始 Prompt/响应。

**权限**：需认证（REVIEWER / OWNER / LABELER 均可查看相关提交）

**响应体** `AiReviewResultResponse`：

| 字段 | 类型 | 说明 |
|------|------|------|
| aiReviewStatus | String | AI 审核状态：PENDING / RUNNING / SUCCESS / FAILED / MANUAL_REQUIRED |
| decision | String | AI 结论：PASS / REJECT / MANUAL_REVIEW |
| averageScore | BigDecimal | 平均分 |
| dimensionScores | Map | 各维度评分 |
| riskFlags | List&lt;String&gt; | 风险标记列表 |
| suggestion | String | AI 建议 |
| agentRunId | Long | 关联的 Agent 运行 ID |
| promptSnapshot | String | Prompt 快照 |
| rawResponse | String | LLM 原始响应 |
| retryCount | Integer | 重试次数 |

---

### 11.6 GET /api/v1/submissions/{submissionId}/ai-review-result

**作用**：查询指定提交的 AI 审核结果摘要（与 11.5 返回相同结构，提供兼容路径）。

**权限**：需认证

**响应体**：同 `AiReviewResultResponse`

---

### 11.7 POST /api/v1/submissions/{submissionId}/ai-review/retry

**作用**：审核员手动触发 AI 预审重试。适用于 AI 审核失败或需要重新评估的场景。每次重试产生新的 AgentRun 记录。

**权限**：REVIEWER

**响应体**：同 `AiReviewResultResponse`

---

### 11.8 GET /api/v1/tasks/{taskId}/ai-review-logs

**作用**：分页查询指定任务下的所有 AI 审核结果记录，支持按状态、决策、时间范围筛选。用于管理员和审核员查看任务维度的 AI 审核日志。

**权限**：ADMIN / OWNER（仅自己的任务） / REVIEWER

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | Integer | 否 | 页码，从 1 开始，默认 1 |
| pageSize | Integer | 否 | 每页条数，默认 20，最大 100 |
| status | String | 否 | 筛选状态：PENDING / RUNNING / SUCCESS / FAILED / RATE_LIMITED / MANUAL_REQUIRED |
| decision | String | 否 | 筛选决策：PASS / REJECT / MANUAL_REVIEW |
| startTime | DateTime | 否 | 起始时间（ISO 8601 格式） |
| endTime | DateTime | 否 | 结束时间（ISO 8601 格式） |

**响应体** `AiReviewResultPageResponse`：

| 字段 | 类型 | 说明 |
|------|------|------|
| items | List&lt;AiReviewResultResponse&gt; | 当前页数据 |
| page | int | 当前页码 |
| pageSize | int | 每页条数 |
| total | long | 总记录数 |

---

## 12. 大模型供应商配置

> **权限边界**：ADMIN 统一创建和维护加密 API Key 的平台模型；OWNER 只从 Admin 启用的模型列表中选择。

### 12.1 GET /api/v1/llm-providers（OWNER 只读）

**作用**：查询 Admin 已启用的模型选项列表。仅返回选择和展示必要字段。

**权限**：OWNER

**响应体** `List<OwnerModelOptionResponse>`：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 供应商 ID（即 Owner 选择模型时的 providerId） |
| providerCode | String | 供应商编码 |
| providerName | String | 供应商名称 |
| defaultModel | String | 默认模型名称（实际调用的模型名） |
| supportVision | Boolean | 是否支持视觉识别 |
| supportMultiImage | Boolean | 是否支持多图识别 |
| maxImageCount | Integer | 最大图片数量 |
| visionModel | String | 视觉模型名称 |
| structuredOutputMode | String | 结构化输出模式 |

> **安全**：OWNER 响应永不含 API Key、baseUrl、customHeaders、限流值。

---

### 12.2 GET /api/v1/admin/llm-providers（ADMIN 管理列表）

**权限**：ADMIN。返回全部 Provider，含管理字段。

**响应体** `List<LlmProviderResponse>`：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 供应商 ID |
| providerCode | String | 供应商编码 |
| providerName | String | 供应商名称 |
| baseUrl | String | API 基础地址 |
| defaultModel | String | 默认模型名称 |
| customHeaders | Map | 自定义请求头（敏感值脱敏） |
| enabled | Boolean | 是否启用 |
| platformRateLimitPerMinute | Integer | 平台级限流 |
| taskRateLimitPerMinute | Integer | 任务级限流 |
| userRateLimitPerMinute | Integer | 用户级限流 |
| supportVision | Boolean | 是否支持视觉 |
| supportMultiImage | Boolean | 是否支持多图 |
| maxImageCount | Integer | 最大图片数 |
| visionModel | String | 视觉模型名称 |
| structuredOutputMode | String | 结构化输出模式 |
| outputSchema | String | JSON Schema 输出结构（Admin 管理） |
| apiKeyConfigured | Boolean | 是否已配置 API Key |
| createdBy | Long | 创建者 |
| createdAt | LocalDateTime | 创建时间 |
| updatedAt | LocalDateTime | 更新时间 |

---

### 12.3 POST /api/v1/admin/llm-providers（ADMIN 创建）

**权限**：ADMIN。API Key 使用 AES-GCM 加密存储。

**请求体** `CreateLlmProviderRequest`：

| 字段 | 类型 | 必填 | 约束 | 说明 |
|------|------|:--:|------|------|
| providerCode | String | 是 | 非空，最大 64 字符 | 供应商唯一编码 |
| providerName | String | 是 | 非空，最大 100 字符 | 供应商显示名称 |
| baseUrl | String | 是 | 非空，最大 500 字符 | API 基础地址 |
| apiKey | String | 是 | 非空，最大 4096 字符 | API 密钥（加密存储） |
| defaultModel | String | 是 | 非空，最大 128 字符 | 默认模型 |
| customHeaders | Map | 否 | - | 自定义请求头 |
| platformRateLimitPerMinute | Integer | 否 | ≥ 0 | 平台级限流 |
| taskRateLimitPerMinute | Integer | 否 | ≥ 0 | 任务级限流 |
| userRateLimitPerMinute | Integer | 否 | ≥ 0 | 用户级限流 |
| supportVision | Boolean | 否 | 默认 false | 是否支持视觉 |
| supportMultiImage | Boolean | 否 | 默认 false | 是否支持多图 |
| maxImageCount | Integer | 否 | ≥ 0，默认 10 | 最大图片数 |
| visionModel | String | 否 | 最大 100 字符 | 视觉模型名 |
| structuredOutputMode | String | 否 | 最大 20 字符 | NONE / JSON_OBJECT / JSON_SCHEMA |
| outputSchema | String | 否 | 最大 10000 字符 | JSON Schema 输出结构 |

**响应体**：同 `LlmProviderResponse`

**错误码**：500301（密钥未配置）；500302（加密/解密失败）

---

### 12.4 PUT /api/v1/admin/llm-providers/{providerId}（ADMIN 更新）

**权限**：ADMIN。apiKey 可选：不传保留原密钥，传新值则替换。

**请求体** `UpdateLlmProviderRequest`：字段同 CreateLlmProviderRequest，apiKey 可选。

**错误码**：404301（供应商不存在）

---

### 12.5 POST /api/v1/admin/llm-providers/{providerId}/enable（ADMIN 启用）

**权限**：ADMIN。启用后 OWNER 可在 AI 审核配置中选用。

---

### 12.6 POST /api/v1/admin/llm-providers/{providerId}/disable（ADMIN 禁用）

**权限**：ADMIN。禁用后不可被新配置选用，已运行任务不受影响。

---

### 12.7 POST /api/v1/admin/llm-providers/{providerId}/test（ADMIN 测试）

**权限**：ADMIN。发送 OpenAI 兼容请求验证连通性。

**请求体** `TestLlmProviderRequest`：

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| apiKey | String | 否 | 临时密钥（不传用已存储的） |
| modelName | String | 否 | 测试模型名 |
| customHeaders | Map | 否 | 临时自定义头 |

**响应体** `LlmProviderTestResponse`：success / latencyMs / message

**错误码**：400301（Header 无效）；500303（Header JSON 格式错误）

---

## 13. LlmTrigger（字段级 AI 辅助）

LlmTrigger 是标注员在作答过程中点击某个模板组件触发的 AI 辅助能力。前端只传点击的 `componentId`、当前草稿 `currentAnswerJson` 和可选 `userInstruction`；后端自动读取 assignment、任务、题目、模板组件、任务 AI 审核配置和评分维度，调用 LLM 后返回可直接合并到 `answerJson` 的结构化 `patch`。

### 13.1 POST /api/v1/assignments/{assignmentId}/llm-triggers（标注时触发）

**作用**：标注员在作答过程中点击组件 AI 按钮，触发 LLM 为该组件生成结构化建议。后端从 assignment 自动获取 taskId、templateVersionId、datasetItemId、template schema、AI 审核配置等上下文。

**权限**：LABELER（必须是该 assignment 的持有者）

**请求体** `LlmTriggerRunRequest`：

| 字段 | 类型 | 必填 | 约束 | 说明 |
|------|------|:--:|------|------|
| componentId | String | 是 | 最大 128 字符 | 被点击的模板组件 ID，必须存在于当前 template schema |
| currentAnswerJson | Map | 否 | - | 当前已填写的草稿答案，后端默认空对象 |
| userInstruction | String | 否 | 最大 1000 字符 | 用户补充指令，仅作为附加要求 |
| providerId | Long | 否 | - | 历史兼容字段，标注端主链路不再使用 |
| modelName | String | 否 | 最大 128 字符 | 历史兼容字段，标注端主链路不再使用 |
| promptTemplate | String | 否 | 最大 10000 字符 | 历史兼容字段，标注端主链路不再使用 |
| targetFields | List&lt;String&gt; | 否 | - | 历史兼容字段，目标字段由后端从组件解析 |

**示例**：
```json
{
  "componentId": "summary",
  "currentAnswerJson": {
    "summary": "",
    "label": "产品咨询"
  },
  "userInstruction": "请生成更简洁的摘要"
}
```

### 13.2 POST /api/v1/tasks/{taskId}/llm-triggers/test（Owner 预览测试）

**作用**：Owner 搭模板时用指定题目和组件测试 LlmTrigger 效果。不创建 submission，不写入标注员草稿。

**权限**：OWNER（必须是该任务的 Owner）

**请求体** `LlmTriggerRunRequest`（比标注模式多传 `datasetItemId`）：

| 字段 | 类型 | 必填 | 说明 |
|------|------|:--:|------|
| datasetItemId | Long | 是 | 测试用的题目 ID |
| componentId | String | 是 | 测试的模板组件 ID |
| currentAnswerJson | Map | 否 | 草稿答案 |
| userInstruction | String | 否 | 用户补充指令 |

**示例**：
```json
{
  "datasetItemId": 70,
  "componentId": "summary",
  "currentAnswerJson": {
    "summary": ""
  }
}
```

### 13.3 GET /api/v1/llm/triggers/runs/{triggerRunId}

**作用**：轮询异步 LlmTrigger 的运行状态和结果。

**权限**：OWNER（自己的任务）/ LABELER（自己的 assignment）

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| triggerRunId | Long | 运行记录 ID |

**响应体** `LlmTriggerRunResponse`（三个接口通用）：

| 字段 | 类型 | 说明 |
|------|------|------|
| triggerRunId | Long | 运行记录 ID |
| agentRunId | Long | 关联的 Agent 运行 ID |
| componentId | String | 触发的组件 ID |
| suggestionJson | Map | 完整结构化建议，包含 patch、confidence、warnings 等 |
| patch | Map | 可直接合并到 answerJson 的字段补丁 |
| displayText | String | 展示文本 |
| targetFields | List&lt;String&gt; | 目标字段列表 |
| rawModelSummary | String | 原始输出摘要 |
| confidence | BigDecimal | AI 对建议的置信度 |
| warnings | List&lt;String&gt; | 警告信息，如模型输出包含非目标字段 |
| traceId | String | 本次 AI 调用链路 ID |
| status | String | SUCCESS / FAILED / RUNNING |
| latencyMs | Long | 耗时毫秒 |
| errorCode | String | 错误码 |
| errorMessage | String | 错误信息 |

---

### 13.4 GET /api/v1/tasks/{taskId}/llm-trigger-runs

**作用**：分页查询指定任务下的所有 LlmTrigger 运行记录，支持按状态、组件 ID、时间范围筛选。用于管理员和任务 Owner 查看 LLM 调用日志。

**权限**：ADMIN / OWNER（仅自己的任务） / REVIEWER

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | Integer | 否 | 页码，从 1 开始，默认 1 |
| pageSize | Integer | 否 | 每页条数，默认 20，最大 100 |
| status | String | 否 | 筛选状态：RUNNING / SUCCESS / FAILED / RATE_LIMITED |
| componentId | String | 否 | 筛选触发器组件 ID |
| startTime | DateTime | 否 | 起始时间（ISO 8601 格式） |
| endTime | DateTime | 否 | 结束时间（ISO 8601 格式） |

**响应体** `LlmTriggerRunPageResponse`：

| 字段 | 类型 | 说明 |
|------|------|------|
| items | List&lt;LlmTriggerRunResponse&gt; | 当前页数据 |
| page | int | 当前页码 |
| pageSize | int | 每页条数 |
| total | long | 总记录数 |

---

## 14. 预标注

### 14.1 POST /api/v1/assignments/{assignmentId}/pre-annotations/run

**作用**：触发 AI 为当前 assignment 生成整题建议答案。复用任务的 AI 审核配置中的 Provider 和 Prompt。一个 assignment 同时只能有一个预标注在运行（通过 Redis 锁保证）。预标注结果不会自动写入草稿，需标注员确认后手动采纳。

**权限**：LABELER

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| assignmentId | Long | Assignment ID |

**请求体** `PreAnnotationRunRequest`（可选）：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| hint | String | 否 | 用户提示，附加到 Prompt 中 |

**响应体** `PreAnnotationResponse`：

| 字段 | 类型 | 说明 |
|------|------|------|
| preAnnotationId | Long | 预标注记录 ID |
| assignmentId | Long | Assignment ID |
| status | String | 状态：RUNNING / SUCCESS / FAILED |
| suggestedAnswerJson | String | 建议答案 JSON |
| fieldSuggestions | List | 字段级建议列表 |
| overallConfidence | BigDecimal | 整体置信度 |
| riskFlags | List&lt;String&gt; | 风险标记 |
| ignoredFieldsJson | String | 被过滤的非法字段 |
| mediaUnderstandingJson | String | 多模态理解信息 |
| agentRunId | Long | 关联的 Agent 运行 ID |

**错误码**：403801（非本人 assignment）；409（已有预标注在运行）

---

### 14.2 GET /api/v1/assignments/{assignmentId}/pre-annotations/latest

**作用**：获取当前 assignment 最新一次预标注的结果。

**权限**：LABELER

**响应体**：同 `PreAnnotationResponse`

---

### 14.3 GET /api/v1/pre-annotations/{preAnnotationId}

**作用**：查询指定预标注记录的完整信息。LABELER 只能查看自己 assignment 的预标注，OWNER 和 REVIEWER 可查看任意预标注。

**权限**：LABELER / OWNER / REVIEWER

**响应体**：同 `PreAnnotationResponse`

---

## 15. Agent 运行记录

### 15.1 GET /api/v1/agent-runs/{agentRunId}

**作用**：查询单次 AI Agent 运行的完整信息，包括输入 Prompt 快照、LLM 输出、运行状态和耗时。用于调试和审计 AI 行为。

**权限**：需认证（根据关联业务对象权限校验）

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| agentRunId | Long | Agent 运行记录 ID |

**响应体** `AgentRunDetailResponse`：

| 字段 | 类型 | 说明 |
|------|------|------|
| agentRunId | Long | 运行记录 ID |
| agentType | String | Agent 类型：AI_REVIEW / PRE_ANNOTATION / LLM_TRIGGER |
| submissionId | Long | 关联提交 ID（AI 审核时） |
| assignmentId | Long | 关联 Assignment ID（预标注时） |
| providerId | Long | 使用的供应商 ID |
| modelName | String | 使用的模型名 |
| promptVersion | String | Prompt 版本 |
| inputSnapshot | String | 输入 Prompt 快照 |
| outputSnapshot | String | LLM 输出快照 |
| status | String | 状态：PENDING / RUNNING / SUCCESS / FAILED |
| errorMessage | String | 错误信息 |
| startedAt | LocalDateTime | 开始时间 |
| finishedAt | LocalDateTime | 结束时间 |
| latencyMs | Long | 耗时毫秒 |

---

## 16. 奖励与贡献

### 16.1 POST /api/v1/tasks/{taskId}/reward-rule

**作用**：保存任务奖励规则的新版本。规则以版本追加保存，历史流水不因本次配置变更重算。用于 Owner 配置任务的标注奖励方案。

**权限**：ADMIN 或 OWNER

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| taskId | Long | 任务 ID |

**请求体** `RewardRuleRequest`：

| 字段 | 类型 | 必填 | 约束 | 说明 |
|------|------|------|------|------|
| rewardMode | String | 否 | - | 奖励模式（如 PER_ITEM） |
| unitReward | BigDecimal | 是 | ≥ 0.00 | 单条奖励金额 |
| rewardCurrency | String | 否 | - | 奖励货币/积分类型 |
| rewardVisible | Boolean | 否 | - | 奖励是否对标注员可见 |

**响应体** `RewardRuleResponse`：

| 字段 | 类型 | 说明 |
|------|------|------|
| ruleId | Long | 规则 ID |
| taskId | Long | 任务 ID |
| effectiveVersion | Integer | 生效版本号 |
| rewardMode | String | 奖励模式 |
| unitReward | BigDecimal | 单条奖励 |
| rewardCurrency | String | 货币类型 |
| rewardVisible | Boolean | 是否可见 |
| effectiveAt | LocalDateTime | 生效时间 |

---

### 16.2 GET /api/v1/tasks/{taskId}/reward-rule

**作用**：查询任务最新奖励规则，用于 Owner 配置页回显。

**权限**：ADMIN 或 OWNER

**响应体**：同 `RewardRuleResponse`

---

### 16.3 GET /api/v1/labeler/contribution/overview

**作用**：查询当前标注员贡献总览。待审核提交不进入通过率分母。所有查询基于 JWT 用户，不接受前端传入 labelerId。

**权限**：LABELER 或 ADMIN

**响应体** `ContributionOverviewResponse`：

| 字段 | 类型 | 说明 |
|------|------|------|
| labelerId | Long | 标注员 ID |
| claimedCount | Integer | 已领取数 |
| submittedCount | Integer | 已提交数 |
| approvedCount | Integer | 已通过数 |
| rejectedCount | Integer | 已驳回数 |
| totalReward | BigDecimal | 累计奖励 |
| approvalRate | BigDecimal | 通过率 |

---

### 16.4 GET /api/v1/labeler/contribution/trend

**作用**：查询最近 N 天贡献趋势。缺失日期由服务层补零，保证前端图表连续。

**权限**：LABELER 或 ADMIN

**查询参数**：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| days | Integer | 否 | 30 | 查询天数 |

**响应体** `List<DailyContributionPoint>`：

| 字段 | 类型 | 说明 |
|------|------|------|
| date | LocalDate | 日期 |
| submittedCount | Integer | 当日提交数 |
| approvedCount | Integer | 当日通过数 |
| reward | BigDecimal | 当日奖励 |

---

### 16.5 GET /api/v1/labeler/contribution/tasks

**作用**：按任务聚合查询当前标注员贡献统计。

**权限**：LABELER 或 ADMIN

**查询参数**：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| limit | Integer | 否 | 20 | 返回条数 |
| offset | Integer | 否 | 0 | 偏移量 |

**响应体** `List<TaskContributionResponse>`：

| 字段 | 类型 | 说明 |
|------|------|------|
| taskId | Long | 任务 ID |
| taskTitle | String | 任务标题 |
| submittedCount | Integer | 提交数 |
| approvedCount | Integer | 通过数 |
| totalReward | BigDecimal | 该任务累计奖励 |

---

### 16.6 GET /api/v1/labeler/rewards/ledger

**作用**：查询当前标注员奖励流水，包含正向奖励和冲正（撤销）记录。

**权限**：LABELER 或 ADMIN

**查询参数**：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| limit | Integer | 否 | 20 | 返回条数 |
| offset | Integer | 否 | 0 | 偏移量 |

**响应体** `List<RewardLedgerResponse>`：

| 字段 | 类型 | 说明 |
|------|------|------|
| ledgerId | Long | 流水 ID |
| taskId | Long | 任务 ID |
| submissionId | Long | 关联提交 ID |
| amount | BigDecimal | 金额（正向为正，冲正为负） |
| direction | String | 方向：CREDIT / DEBIT |
| reason | String | 原因 |
| createdAt | LocalDateTime | 创建时间 |

---

## 17. 导出模块

### 17.1 POST /api/v1/tasks/{taskId}/exports

**作用**：创建异步导出任务。导出范围为 submission.status=APPROVED 且 isGolden=true 的金标提交数据。支持配置导出格式、附加字段和字段映射。

**权限**：ADMIN 或 OWNER

**请求体** `CreateExportRequest`：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| exportFormat | String | 否 | 导出格式：JSON / JSONL / CSV / EXCEL |
| includeAiReview | Boolean | 否 | 是否包含 AI 审核结果 |
| includeAuditTrail | Boolean | 否 | 是否包含审计轨迹 |
| includeReviewComment | Boolean | 否 | 是否包含审核评语 |
| includeLabelerInfo | Boolean | 否 | 是否包含标注员信息 |
| fieldMappings | List | 否 | 字段映射配置列表 |

**响应体** `ExportJobResponse`：

| 字段 | 类型 | 说明 |
|------|------|------|
| exportJobId | Long | 导出任务 ID |
| taskId | Long | 关联任务 ID |
| status | String | 状态：PENDING / RUNNING / SUCCESS / FAILED |
| exportFormat | String | 导出格式 |
| resultFileId | Long | 结果文件 ID（成功时） |
| downloadUrl | String | 下载地址（成功时） |
| errorMessage | String | 错误信息（失败时） |
| createdAt | LocalDateTime | 创建时间 |
| completedAt | LocalDateTime | 完成时间 |

---

### 17.2 GET /api/v1/tasks/{taskId}/exports

**作用**：分页查询任务的导出历史列表。

**权限**：ADMIN 或 OWNER

**查询参数**：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| page | Integer | 否 | 1 | 页码 |
| pageSize | Integer | 否 | 20 | 每页条数 |

**响应体** `ExportJobPageResponse`：分页包装的 ExportJobResponse 列表

---

### 17.3 GET /api/v1/tasks/{taskId}/exports/{exportJobId}

**作用**：查询导出任务详情和下载信息。导出完成后包含签名下载地址。

**权限**：ADMIN 或 OWNER

**响应体**：同 `ExportJobResponse`

---

## 18. 文件存储

### 18.1 POST /api/v1/files/upload

**作用**：上传文件到对象存储（MinIO）并记录文件元数据。文件归属用户由 JWT 上下文决定，不信任前端传入。用于数据集导入文件、导出结果等场景。

**权限**：需认证（任意角色）

**请求格式**：`multipart/form-data`

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | MultipartFile | 是 | 上传的文件 |
| businessType | String | 是 | 业务类型标识（如 DATASET_IMPORT、EXPORT） |

**响应体** `FileUploadResponse`：

| 字段 | 类型 | 说明 |
|------|------|------|
| fileId | Long | 文件记录 ID |
| fileName | String | 原始文件名 |
| fileSize | Long | 文件大小（字节） |
| contentType | String | MIME 类型 |
| businessType | String | 业务类型 |
| uploadedAt | LocalDateTime | 上传时间 |

---

### 18.2 GET /api/v1/files/{fileId}/signed-url

**作用**：获取文件的短期签名下载地址。权限由服务层按 object_files.owner_id 和 ADMIN 角色校验，非文件所有者且非管理员不可获取。

**权限**：需认证（文件所有者或 ADMIN）

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| fileId | Long | 文件 ID |

**响应体** `SignedUrlResponse`：

| 字段 | 类型 | 说明 |
|------|------|------|
| fileId | Long | 文件 ID |
| signedUrl | String | 短期有效的签名下载地址 |
| expiresAt | LocalDateTime | 签名过期时间 |

---

## 19. 审计日志

### 19.1 GET /api/v1/audit-logs

**作用**：按业务类型和业务 ID 查询审计时间线。用于前端审计时间线组件展示提交、AI 审核、人工审核、打回等操作历史。审计日志只追加不修改不删除。

**权限**：ADMIN、OWNER 或 REVIEWER

**查询参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| bizType | String | 是 | 业务对象类型（如 SUBMISSION、AI_REVIEW、ASSIGNMENT） |
| bizId | Long | 是 | 业务对象 ID |

**响应体** `List<AuditLogResponse>`：

| 字段 | 类型 | 说明 |
|------|------|------|
| auditLogId | Long | 审计日志 ID |
| bizType | String | 业务类型 |
| bizId | Long | 业务 ID |
| actorType | String | 操作者类型：USER / SYSTEM_AGENT |
| actorId | Long | 操作者 ID |
| action | String | 操作动作 |
| beforeJson | String | 变更前快照 |
| afterJson | String | 变更后快照 |
| agentRunId | Long | 关联 Agent 运行 ID（AI 操作时） |
| traceId | String | 链路追踪 ID |
| createdAt | LocalDateTime | 创建时间 |

---

## 20. 媒体处理

### 20.1 POST /api/v1/dataset-items/{itemId}/media/process

**作用**：触发数据项的多模态媒体处理。解析题目中的媒体资源（图片、视频等），生成 AI 可用的上下文信息。

**权限**：ADMIN 或 OWNER

**路径参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| itemId | Long | 数据项 ID |

**响应体** `MediaProcessingJobResponse`：

| 字段 | 类型 | 说明 |
|------|------|------|
| jobId | Long | 处理任务 ID |
| itemId | Long | 数据项 ID |
| status | String | 状态：PENDING / PROCESSING / COMPLETED / FAILED |
| createdAt | LocalDateTime | 创建时间 |

---

### 20.2 GET /api/v1/dataset-items/{itemId}/media-context

**作用**：获取数据项的媒体上下文信息。包含已处理的媒体资源列表和 AI 可用的结构化描述。

**权限**：ADMIN、OWNER、REVIEWER 或 LABELER

**响应体** `MediaContextResponse`：

| 字段 | 类型 | 说明 |
|------|------|------|
| itemId | Long | 数据项 ID |
| mediaAssets | List | 媒体资源列表 |
| contextReady | Boolean | 上下文是否就绪 |

---

### 20.3 GET /api/v1/media-processing/jobs/{jobId}

**作用**：查询媒体处理任务状态。

**权限**：ADMIN、OWNER 或 REVIEWER

**响应体**：同 `MediaProcessingJobResponse`

---

## 附录 A：状态机枚举

### TaskStatus（任务状态）

| 值 | 说明 | 合法迁移 |
|------|------|------|
| DRAFT | 草稿 | → PUBLISHED |
| PUBLISHED | 已发布 | → PAUSED / ENDED |
| PAUSED | 已暂停 | → PUBLISHED / ENDED |
| ENDED | 已结束 | 终态 |

### AssignmentStatus（领取状态）

| 值 | 说明 |
|------|------|
| CLAIMED | 已领取 |
| DRAFTING | 编辑中 |
| SUBMITTED | 已提交 |
| RETURNED | 已打回 |
| APPROVED | 已通过 |
| CANCELLED | 已取消 |

### SubmissionStatus（提交状态）

| 值 | 说明 |
|------|------|
| SUBMITTED | 已提交 |
| AI_REVIEWING | AI 审核中 |
| PENDING_FINAL | 待终审 |
| APPROVED | 已通过 |
| REJECTED | 已驳回 |
| SUPERSEDED | 已被新版本取代 |

### AiReviewStatus（AI 审核状态）

| 值 | 说明 |
|------|------|
| PENDING | 等待中 |
| RUNNING | 运行中 |
| SUCCESS | 成功 |
| FAILED | 失败 |
| RATE_LIMITED | 被限流 |
| MANUAL_REQUIRED | 需人工处理 |

### AiDecision（AI 结论）

| 值 | 说明 |
|------|------|
| PASS | 通过 |
| REJECT | 驳回 |
| MANUAL_REVIEW | 转人工 |

### ConflictStatus（冲突状态）

| 值 | 说明 |
|------|------|
| NONE | 无冲突 |
| CONSENSUS_REACHED | 达成共识 |
| CONFLICTED | 存在冲突 |
| RESOLVED | 已解决 |

---

## 附录 B：统一错误码

| code | 含义 | 前端处理建议 |
|------|------|------|
| 400101 | 状态不允许当前操作 | 展示业务错误提示 |
| 400102 | 参数非法或不满足约束 | 表单字段级提示 |
| 400301 | LLM Provider Header 名无效 | 提示修正 Header |
| 401001 | 未登录或令牌失效 | 跳转登录页 |
| 403001 | 无权限 | 展示无权限页 |
| 403801 | 非本人资源 | 提示无权访问 |
| 404001 | 资源不存在 | 提示资源不存在或已删除 |
| 404301 | LLM Provider 不存在 | 提示资源不存在 |
| 404401 | Assignment 不存在 | 提示资源不存在 |
| 403601 | 同一审核员不能审同一条提交的多个级别 | 提示已被审核过 |
| 409101 | 版本冲突（乐观锁） | 提示刷新后重试 |
| 409201 | 领取冲突（无可用题目） | 提示重新领取 |
| 409301 | Schema 校验失败 | 定位字段错误 |
| 409401 | Assignment 状态不可放弃 | 提示当前状态不允许操作 |
| 429001 | LLM 限流 | 展示等待或重试中 |
| 500001 | 系统内部错误 | 展示 traceId 供排查 |
| 500301 | LLM 加密密钥未配置 | 联系管理员 |
| 500302 | LLM 密钥加解密失败 | 联系管理员 |
| 500303 | LLM Header JSON 格式错误 | 检查配置 |

---

## 附录 C：缺失接口清单

以下为基于完整业务流程审查发现的功能缺口，按优先级和模块分类。

### C.1 审核流程缺口

| 建议接口 | 说明 | 状态 |
|------|------|------|
| `POST /api/v1/reviewer/submissions/claim` | 审核员自领取未分配的提交 | ✅ 已实现 |
| `GET /api/v1/reviewer/tasks` | 审核员可审核的任务列表入口 | ✅ 已实现 |
| `GET /api/v1/reviewer/dashboard` | 审核员工作台概览统计 | ✅ 已实现 |
| 定时自动分配（`ReviewAutoAssignScheduler`） | 按负载均衡将未分配提交自动分配给审核员 | ✅ 已实现 |
| `POST /api/v1/tasks/{taskId}/reviewers` | Owner 将审核员预分配到任务级别 | ✅ 已实现（3.13） |
| `GET /api/v1/admin/reviewers/workload` | 管理员查看各审核员工作量和审核效率统计 | ❌ 低 |

**当前审核流程**（已实现）：
- 提交进入 AI 预审后，状态变为 `PENDING_FINAL`（待终审）
- ✅ 审核员通过 `GET /api/v1/reviewer/tasks` 查看有待审提交的任务列表
- ✅ 审核员通过 `GET /api/v1/reviewer/dashboard` 查看个人工作统计
- ✅ 审核员通过 `POST /api/v1/reviewer/submissions/claim` 主动领取待审提交
- ✅ 未领取的提交由 `ReviewAutoAssignScheduler` 定时按负载均衡自动分配
- ✅ 审核员通过 `GET /api/v1/reviewer/submissions?assignedReviewerId=<自己ID>` 查看自己的待审列表
- ✅ 也可通过 `POST /api/v1/reviewer/submissions/batch/assign` 手动分配给其他审核员

---

### C.2 标注员工作流缺口（高优先级）

| 建议接口 | 说明 | 状态 |
|------|------|------|
| `GET /api/v1/market/tasks` 响应增加任务详情和 `itemsPreview` | 任务大厅列表直接返回任务详情及其下面可领取题目预览 | ✅ 已实现（6.1） |
| `GET /api/v1/market/tasks/{taskId}` | 任务大厅详情页查询任务详情及可领取题目分页列表 | ✅ 已实现（6.2） |
| `GET /api/v1/labeler/claimed-tasks` | 标注员按任务聚合查看自己已领取的任务及题目预览 | ✅ 已实现（6.4） |
| `GET /api/v1/labeler/claimed-tasks/{taskId}` | 标注员查看某个任务下自己领取的题目分页列表 | ✅ 已实现（6.5） |
| `GET /api/v1/labeler/assignments` | 兼容的已领取题目扁平列表，可按 taskId/status/page/size 筛选 | ✅ 已实现（6.7） |
| `POST /api/v1/labeler/assignments/{assignmentId}/cancel` | 标注员主动放弃已领取的 assignment | ✅ 已实现（6.8） |
| AI 审核完成通知（轮询/WebSocket） | 提交后 AI 审核异步执行，前端只能轮询 `GET /submissions/{id}/ai-review-result`，无推送机制 | ❌ 中 |

**说明**：`GET /api/v1/labeler/assignments` 是兼容保留的扁平 assignment 列表；标注员页面需要“任务详情 + 自己领取的题目”时，应优先使用 `GET /api/v1/labeler/claimed-tasks` 或 `GET /api/v1/labeler/claimed-tasks/{taskId}`。

---
### C.3 任务管理缺口（中优先级）

| 建议接口 | 说明 | 优先级 |
|------|------|------|
| `GET /api/v1/owner/tasks` 增加筛选参数 | 当前返回全量列表，不支持按 status / keyword / tag 筛选。任务多时无法快速定位 | ✅ 已实现（3.10） |
| `DELETE /api/v1/tasks/{taskId}` | 删除草稿任务。当前只能 end 不能删除，DRAFT 状态的废弃任务会一直存在 | ✅ 已实现（3.9） |
| `GET /api/v1/tasks/{taskId}/statistics` | Owner 查看任务统计看板：提交总数、通过数、驳回数、通过率、标注完成进度 | ✅ 已实现（3.11） |
| `GET /api/v1/tasks/{taskId}/labelers` | Owner 查看任务下参与的标注员列表及其进度 | ✅ 已实现（3.12） |
| `GET /api/v1/owner/tasks/{taskId}/review-progress` | Owner 查看任务审核进度：待审/已审/通过/驳回数量 | ✅ 已合并至 3.11 统计接口 |

---

### C.4 分页与列表缺口

| 改进项 | 说明 | 状态 |
|------|------|------|
| `GET /api/v1/reviewer/submissions` 响应增加 total 字段 | 返回 `PageResponse(items, page, pageSize, total)` | ✅ 已实现 |
| `GET /api/v1/labeler/submissions` 响应增加 total 字段 | 同上 | ✅ 已实现 |
| 批量操作接口 size 限制 | batch-approve/reject/assign 无硬性上限，建议前端控制在 100 条以内 | ⚠️ 无硬限制 |

---

### C.5 用户账号管理缺口

| 接口 | 说明 | 状态 |
|------|------|------|
| `PUT /api/v1/users/me/password` | 用户修改密码（校验旧密码，成功后旧令牌失效） | ✅ 已实现 |
| `PUT /api/v1/users/me/profile` | 用户更新个人信息（displayName、email） | ✅ 已实现 |

---

### C.6 通知与状态查询（轮询方案）

| 接口 | 说明 | 状态 |
|------|------|------|
| `GET /api/v1/submissions/{id}/ai-review` | 轮询 AI 审核结果（提交后前端定时查询） | ✅ 已有 |
| `GET /api/v1/tasks/{taskId}/dataset/import-jobs/{jobId}` | 轮询数据集导入进度 | ✅ 已有 |
| `GET /api/v1/tasks/{taskId}/exports/{exportJobId}` | 轮询导出任务状态 | ✅ 已有 |
| `GET /api/v1/labeler/submissions?submissionStatus=REJECTED` | 标注员轮询被驳回的提交 | ✅ 已有 |

> 说明：不实现 WebSocket 推送，所有异步状态变更通过前端定时轮询对应接口获取。轮询间隔建议：AI 审核 3-5 秒，导入/导出 5-10 秒。

---

### C.7 已实现但未纳入主文档的接口

| 接口 | 说明 | 状态 |
|------|------|------|
| `GET /api/v1/submissions/{submissionId}/diff?baseVersionNo=N` | 答案 Diff 对比，对比指定提交与基准版本的字段级差异。权限：OWNER / REVIEWER | ✅ 已实现 |
| `GET /api/v1/submissions/{submissionId}/versions` | 查询提交所属 assignment 的所有版本历史。权限：OWNER / REVIEWER / LABELER | ✅ 已实现 |
| `GET /api/v1/owner/export/golden-submissions?taskId=X&lastId=Y&limit=Z` | Owner 分页查询可导出的优质提交快照（游标分页） | ✅ 已实现 |

---

### C.8 业务流程完整性总结

```
标注员完整工作流：
  [✅] 浏览任务市场（含任务详情和可领取题目） → [✅] 领取任务 → [✅] 查看单题详情
  → [✅] 按任务聚合查看已领取题目 → [✅] 保存草稿 → [✅] 提交答案 → [✅] 查看提交状态
  → [✅] 查看兼容的扁平领取列表（回到工作台）
  → [✅] 放弃已领取任务
  → [✅] 被驳回后重新提交（隐式，再次调用 submit）
  → [❌] 收到驳回通知

审核员完整工作流：
  [✅] 查看可审核的任务列表
  [✅] 查看待审提交列表（支持多维筛选）
  [✅] 自领取未分配的提交
  [✅] 被分配提交（batch/assign + 自动分配）
  [✅] 查看提交详情 → [✅] 通过/驳回
  [✅] 批量通过/驳回/转人工/分配
  [✅] 查看冲突组 → [✅] 仲裁选金标
  [✅] 手动重试 AI 审核
  [✅] 查看个人审核统计

Owner 完整工作流：
  [✅] 创建任务 → [✅] 导入数据集 → [✅] 配置模板
  → [✅] 配置 AI 审核 → [✅] 配置奖励规则 → [✅] 发布任务
  → [✅] 暂停/恢复/结束任务
  → [✅] 删除草稿任务
  → [✅] 搜索/筛选自己的任务
  → [✅] 查看任务统计（提交数、通过率、进度）
  → [✅] 查看任务下的标注员列表
  → [✅] 将审核员预分配到任务
  → [✅] 导出金标数据
```

---

## 附录 D：主流程接口调用链

以下为平台核心业务流程（建任务 → 搭模板 → 发布 → Labeler 作答 → 提交 → AI 预审 → 人工审核 → 导出）中各环节实际调用的接口和使用顺序。每个阶段的接口串行依赖——前一步的返回值（taskId、assignmentId、submissionId 等）是下一步的输入参数。

### D.1 Owner 建任务

```text
1. POST /api/v1/files/upload
   请求: multipart/form-data { file: <数据集文件>, businessType: "DATASET_IMPORT" }
   响应: { fileId: 99 }
   说明: 上传数据集文件（支持 JSON / JSONL / Excel 格式）

2. POST /api/v1/tasks
   请求: {
     title: "图像分类标注",
     quota: 100,
     deadlineAt: "2026-07-01T23:59:59",
     overlapCount: 3,
     datasetFileId: 99
   }
   响应: { taskId: 1, status: "DRAFT", datasetImportJob: { jobId: 10, status: "PENDING" } }
   说明: 创建草稿任务，传 datasetFileId 自动创建导入任务

3. GET /api/v1/tasks/1/dataset/import-jobs/10
   响应: { status: "SUCCESS", totalCount: 500, successCount: 498, failedCount: 2 }
   说明: 轮询导入进度，直到 status 为 SUCCESS 或 FAILED
```

如果不在创建时传 datasetFileId，也可以单独导入：
```text
POST /api/v1/tasks/1/dataset/import    请求: { fileId: 99 }
```

---

### D.2 搭模板

```text
4. POST /api/v1/tasks/1/templates
   请求: {
     name: "图像分类模板",
     schemaJson: "{\"components\":[{\"field\":\"label\",\"type\":\"Select\",\"options\":[\"猫\",\"狗\"]}]}",
     changeNote: "初始版本"
   }
   响应: { templateId: 10, currentVersionNo: 1, currentVersion: { versionId: 20 } }
   说明: 创建模板，Schema 定义标注界面的字段结构

5. PUT /api/v1/tasks/1
   请求: { ..., publishedTemplateVersionId: 20 }
   说明: 将模板版本绑定到任务
```

如果需要迭代模板：
```text
POST /api/v1/templates/10/fork
请求: { baseVersionId: 20, schemaJson: "<新Schema>", changeNote: "v2 增加置信度字段" }
```

---

### D.3 配置 AI 审核 + 奖励 → 发布

```text
6. POST /api/v1/llm-providers
   请求: {
     providerCode: "qwen",
     providerName: "通义千问",
     baseUrl: "https://dashscope.aliyuncs.com/compatible-mode/v1",
     apiKey: "sk-xxx",
     defaultModel: "qwen-max"
   }
   响应: { id: 50 }
   说明: 配置大模型供应商（首次使用时创建，后续复用）

7. POST /api/v1/llm-providers/50/test
   请求: {}
   响应: { success: true, latencyMs: 320, message: "连接成功" }
   说明: 验证供应商连通性

8. POST /api/v1/tasks/1/ai-review-configs
   请求: {
     providerId: 50,
     modelName: "qwen-max",
     promptTemplate: "请评估以下标注答案的质量...",
     scoringDimensions: ["准确性", "完整性", "一致性"],
     passThreshold: 80.00,
     manualReviewThreshold: 60.00,
     outputSchema: { type: "object", properties: { ... } },
     maxRetry: 3
   }
   响应: { configId: 40 }
   说明: 配置 AI 审核规则

9. POST /api/v1/tasks/1/reward-rule
   请求: { rewardMode: "PER_ITEM", unitReward: 2.00, rewardCurrency: "积分", rewardVisible: true }
   说明: 配置奖励规则

10. POST /api/v1/tasks/1/publish
    响应: { taskId: 1, status: "PUBLISHED" }
    说明: 发布任务（系统自动校验：数据集✓ 模板✓ 奖励✓ AI配置✓ 截止时间✓）
```

---

### D.4 Labeler 领取 + 作答

```text
11. GET /api/v1/market/tasks
    响应: [{ taskId: 1, title: "图像分类标注", description: "...", itemsPreview: [{ datasetItemId: 70, itemJson: "{...}" }], ... }]
    说明: 标注员浏览任务市场；列表已包含任务详情和可领取题目预览。

11a. GET /api/v1/market/tasks/1?itemPage=1&itemSize=20
     响应: { taskId: 1, title: "图像分类标注", instructionRichText: "...", itemsPreview: [...] }
     说明: 进入任务大厅详情页，查看任务详情及该任务下可领取题目。

12. POST /api/v1/tasks/1/assignments/claim
    响应: { assignmentId: 100, datasetItemId: 70, templateVersionId: 20, status: "CLAIMED" }
    说明: 领取一个题目（Redis 锁保证并发安全）

12a. GET /api/v1/labeler/claimed-tasks
     响应: [{ taskId: 1, title: "图像分类标注", claimedItemCount: 2, itemsPreview: [{ assignmentId: 100, datasetItemId: 70, assignmentStatus: "CLAIMED", itemJson: "{...}" }] }]
     说明: 标注员页面按任务查看自己已领取的任务及题目；只返回当前标注员领取的题目。

12b. GET /api/v1/labeler/claimed-tasks/1?status=CLAIMED&page=1&size=20
     响应: { taskId: 1, title: "图像分类标注", itemsPreview: [{ assignmentId: 100, datasetItemId: 70, itemJson: "{...}", draftVersion: 1 }] }
     说明: 查看某个任务下当前标注员领取的题目分页列表，可按状态筛选。

12c. GET /api/v1/labeler/assignments?status=CLAIMED
     响应: [{ assignmentId: 100, taskId: 1, taskTitle: "图像分类标注", datasetItemId: 70, status: "CLAIMED", draftVersion: 1, claimedAt: "...", updatedAt: "..." }]
     说明: 兼容的扁平 assignment 列表；适合轻量工作台列表，也可按 taskId、status、page、size 过滤。

13. GET /api/v1/assignments/100
    响应: { itemJson: "{...}", schemaJson: "{...}", draftAnswerJson: null, assignmentStatus: "CLAIMED" }
    说明: 进入具体标注页，获取单个已领取题目的完整详情和模板结构。

14. PUT /api/v1/assignments/100/draft
    请求: { answerJson: "{\"label\":\"猫\"}", clientVersion: 0 }
    响应: { draftVersion: 1 }
    说明: 保存草稿（可多次调用，每次 clientVersion 递增）

    ── 标注过程中可触发 LlmTrigger（字段级 AI 辅助）──

15. POST /api/v1/assignments/100/llm-triggers
    请求: {
      providerId: 50,
      modelName: "qwen-plus",
      promptTemplate: "根据以下内容生成摘要：",
      targetFields: ["summary"],
      currentAnswerJson: { "summary": "当前草稿" }
    }
    响应: { triggerRunId: 500, agentRunId: 600, status: "RUNNING", targetFields: ["summary"] }
    说明: 前端全量传参，无需解析模板；后端根据 assignment 自动获取上下文

16. GET /api/v1/llm/triggers/runs/500  ← 轮询
    响应: { status: "SUCCESS", suggestionJson: { "summary": "AI建议..." }, targetFields: ["summary"] }
    说明: 标注员参考 AI 建议，自行决定是否采纳

    ──（可选）AI 预标注辅助（整题级别）──
```
```text
15a. POST /api/v1/assignments/100/pre-annotations/run
     响应: { preAnnotationId: 200, status: "RUNNING" }

15b. GET /api/v1/assignments/100/pre-annotations/latest  ← 轮询结果
     响应: { status: "SUCCESS", suggestedAnswerJson: "{\"label\":\"猫\"}", overallConfidence: 0.92 }
     说明: 标注员参考建议后手动确认采纳
```

---
### D.5 提交 → AI 预审

```text
16. POST /api/v1/assignments/100/submit
    请求: { answerJson: "{\"label\":\"猫\"}", clientVersion: 1 }
    响应: { submissionId: 300, status: "AI_REVIEWING", versionNo: 1 }
    说明: 提交最终答案，系统自动触发 AI 预审（异步执行）

    ↓ 系统后台异步执行 AI 预审

17. GET /api/v1/submissions/300/ai-review  ← 轮询 AI 审核结果
    响应: {
      aiReviewStatus: "SUCCESS",
      decision: "PASS",
      averageScore: 85.5,
      dimensionScores: { "准确性": 90, "完整性": 82, "一致性": 84.5 },
      riskFlags: [],
      suggestion: "标注质量良好"
    }
    说明: AI 审核完成后 submission 状态自动变为 PENDING_FINAL
```

**AI 结论与后续流转**：

| AI decision | 含义 | 后续 |
|------|------|------|
| PASS | AI 认为质量合格 | 进入人工终审队列 |
| REJECT | AI 认为质量不合格 | 进入人工终审队列（由人工最终决定） |
| MANUAL_REVIEW | AI 不确定 | 进入人工终审队列 |

> 注意：MVP 阶段 AI 结论仅作为参考，不直接决定最终结果。所有提交最终都需要人工终审。

---

### D.6 人工审核

```text
18a. GET /api/v1/reviewer/ai-review-status
     响应: [{ submissionId: 300, taskTitle: "图像分类标注", aiReviewStatus: "SUCCESS",
              aiDecision: "PASS", averageScore: "85.50", assignedToMe: true, ... }]
     说明: 审查员查看自己所有负责提交的 AI 预审状态（专用轻量接口）

18b. GET /api/v1/reviewer/submissions?submissionStatus=PENDING_FINAL&page=1&size=20
     响应: [{ submissionId: 300, aiDecision: "PASS", aiReviewStatus: "SUCCESS", ... }]
     说明: 审核员查看待审提交列表（含更多筛选维度）

19. GET /api/v1/reviewer/submissions/300
    响应: { 完整提交详情：答案内容、AI 各维度评分、历史审核记录、冲突信息 }
    说明: 查看单条提交的审核详情页

20a. 通过：
     POST /api/v1/reviewer/submissions/300/approve
     请求: { reviewComment: "标注准确", reviewLevel: 1 }
     效果: submission → APPROVED，触发 SubmissionApproved 事件 → 奖励自动结算

20b. 驳回：
     POST /api/v1/reviewer/submissions/300/reject
     请求: { reason: "标签选择错误，应为'狗'", reviewLevel: 1 }
     效果: submission → REJECTED，assignment → RETURNED
     后续: Labeler 可修改后再次调用 POST /assignments/100/submit（生成 versionNo=2）
```

**批量操作**：
```text
POST /api/v1/reviewer/submissions/batch/approve
请求: { submissionIds: [300, 301, 302] }

POST /api/v1/reviewer/submissions/batch/reject
请求: { submissionIds: [303, 304], reason: "标注不完整" }

POST /api/v1/reviewer/submissions/batch/assign
请求: { submissionIds: [305, 306, 307], reviewerId: 5 }
说明: 将提交分配给指定审核员
```

**冲突仲裁**（overlapCount > 1 时，同一题目多人标注结果不一致）：
```text
21. GET /api/v1/reviewer/conflict-groups
    响应: [{ groupId: 500, datasetItemId: 70, status: "CONFLICTED", submissions: [...] }]

22. GET /api/v1/reviewer/conflict-groups/500
    响应: { 冲突组详情：包含各标注员的答案对比 }

23. POST /api/v1/reviewer/conflict-groups/500/resolve
    请求: { goldenSubmissionId: 300, resolveComment: "该标注员答案更准确" }
    效果: 选定金标，触发 GoldenSelected 事件 → 金标奖励结算
```

---

### D.7 导出

```text
24. POST /api/v1/tasks/1/exports
    请求: { exportFormat: "JSONL", includeAiReview: true, includeReviewComment: true }
    响应: { exportJobId: 600, status: "PENDING" }
    说明: 创建异步导出任务（导出 APPROVED 且 isGolden=true 的金标数据）

25. GET /api/v1/tasks/1/exports/600  ← 轮询导出状态
    响应: { status: "SUCCESS", downloadUrl: "https://minio.../export.jsonl?signature=..." }

26. GET /api/v1/files/{fileId}/signed-url  ← 或通过文件 ID 获取下载地址
    响应: { signedUrl: "https://...", expiresAt: "2026-06-01T16:30:00" }
```

---

### D.8 流程总览图

```text
┌─────────────────────────────────────────────────────────────────────┐
│ Owner 配置阶段                                                       │
│                                                                     │
│  upload file → create task → import dataset → create template       │
│  → bind template → config LLM provider → config AI review           │
│  → config reward → publish                                          │
└──────────────────────────────────┬──────────────────────────────────┘
                                   ↓ 任务上线
┌──────────────────────────────────┴──────────────────────────────────┐
│ Labeler 标注阶段                                                     │
│                                                                     │
│  browse market → claim → view assignment                            │
│  → save draft (N次) → [optional: pre-annotation] → submit           │
└──────────────────────────────────┬──────────────────────────────────┘
                                   ↓ 提交触发
┌──────────────────────────────────┴──────────────────────────────────┐
│ AI 预审阶段（系统自动，异步）                                          │
│                                                                     │
│  AI review → decision: PASS / REJECT / MANUAL_REVIEW                │
│  → submission 状态变为 PENDING_FINAL                                 │
└──────────────────────────────────┬──────────────────────────────────┘
                                   ↓ 进入审核队列
┌──────────────────────────────────┴──────────────────────────────────┐
│ Reviewer 人工审核阶段                                                 │
│                                                                     │
│  list pending → view detail (含 AI 评分参考)                         │
│  → approve / reject / batch操作                                      │
│  → [如有冲突] conflict-groups → resolve (选金标)                      │
│                                                                     │
│  驳回 → Labeler RETURNED → 修改 → 重新 submit (versionNo+1)         │
└──────────────────────────────────┬──────────────────────────────────┘
                                   ↓ 审核通过
┌──────────────────────────────────┴──────────────────────────────────┐
│ 导出阶段                                                             │
│                                                                     │
│  create export job → poll status → download file                    │
└─────────────────────────────────────────────────────────────────────┘
```

### D.9 各阶段接口依赖关系

| 阶段 | 输入依赖 | 产出 |
|------|------|------|
| 上传文件 | 无 | fileId |
| 创建任务 | fileId（可选） | taskId, jobId |
| 创建模板 | taskId | templateId, versionId |
| 配置供应商 | 无 | providerId |
| 配置 AI 审核 | taskId, providerId | configId |
| 配置奖励 | taskId | ruleId |
| 发布任务 | taskId（需已配置模板+AI+奖励） | status=PUBLISHED |
| 领取题目 | taskId | assignmentId, datasetItemId |
| 保存草稿 | assignmentId | draftVersion |
| 提交答案 | assignmentId | submissionId |
| AI 预审 | submissionId（系统自动） | aiDecision, averageScore |
| 人工审核 | submissionId | APPROVED / REJECTED |
| 冲突仲裁 | groupId, goldenSubmissionId | RESOLVED |
| 导出 | taskId | exportJobId, downloadUrl |

### D.10 LlmTrigger 详解

#### 设计原则

**标注员点击组件触发，后端自动聚合上下文**。LlmTrigger 现在依赖当前 assignment、任务、题目、模板组件、当前草稿和任务 AI 审核配置来构造 LLM 输入；前端只需要告诉后端点击了哪个组件，并传入当前草稿。

#### 标注时触发

```
POST /api/v1/assignments/{assignmentId}/llm-triggers
```

权限：LABELER（必须是 assignment 持有者）。后端从 assignment 自动获取 taskId、datasetItemId、templateVersionId、任务 AI 审核配置和组件 schema。

请求示例：

```json
{
  "componentId": "summary",
  "currentAnswerJson": {
    "summary": "",
    "label": "产品咨询"
  },
  "userInstruction": "请生成更简洁的摘要"
}
```

响应成功后关注：

```json
{
  "triggerRunId": 500,
  "agentRunId": 600,
  "componentId": "summary",
  "patch": {
    "summary": "用户正在咨询产品使用方式。"
  },
  "displayText": "用户正在咨询产品使用方式。",
  "targetFields": ["summary"],
  "confidence": 0.87,
  "warnings": [],
  "traceId": "trace-id",
  "status": "SUCCESS"
}
```

前端点击“采纳”后，将 `patch` 合并到 `answerJson`，再调用草稿保存接口。

#### Owner 预览测试

```
POST /api/v1/tasks/{taskId}/llm-triggers/test
```

权限：OWNER（必须是任务所有者）。需传 `datasetItemId` 指定测试题目，传 `componentId` 指定测试组件。模型和评分维度同样来自任务 AI 审核配置。

#### 与 AI 审核、预标注的区别

| | LlmTrigger | 预标注 | AI 审核 |
|---|:--:|:--:|:--:|
| 触发 | **手动点击** | 手动 | 提交后**自动** |
| 粒度 | 字段级 | 整题 | submission |
| 参数来源 | assignment + template component + ai-review-configs | API 调用 | ai-review-configs |
| 结果影响 | 前端展示，标注员决定 | 前端展示 | 写入 ai_review_result |

#### 数据流

```text
前端点击组件 AI 按钮
  ↓
POST /api/v1/assignments/{id}/llm-triggers
  { componentId, currentAnswerJson, userInstruction }
  → 校验：assignment 属于当前 LABELER
  → 读取：task + datasetItem + template component + ai-review-config
  → 校验：Provider 已启用、componentId 存在
  → 构造 inputSnapshot（task + itemJson + component + scoringDimensions + currentAnswerJson）
  → 进入异步队列
  ↓
Worker 消费
  → LlmGateway.review()
  → 写入 llm_trigger_runs
  ↓
前端轮询 GET /api/v1/llm/triggers/runs/{id}
  → 拿到 patch
  → 标注员点击采纳后覆盖组件字段
```

### D.11 联调注意事项

- 普通注册只允许 `LABELER` 和 `OWNER`；`REVIEWER` 需由管理员创建或调整角色
- 每个普通用户只能拥有一个角色；用户无角色或多角色时登录/刷新会返回 `400102`
- 大模型供应商管理：`/api/v1/admin/llm-providers`（ADMIN）；OWNER 通过 `GET /api/v1/llm-providers` 查看
- LlmTrigger 标注端只传 `componentId/currentAnswerJson/userInstruction`，模型、评分维度和组件上下文由后端聚合
- 所有 LLM 调用均通过异步队列执行，前端需轮询结果

---

## 附录 E：AI 场景接入指导

本节按真实业务场景说明 AI 能力如何接入。AI 结果默认都是辅助建议：AI 审核不直接通过提交，预标注和 LlmTrigger 不直接写入答案，前端必须由用户确认后再采纳。

### E.1 AI 能力总览

| 场景 | 触发角色 | 触发方式 | 粒度 | 结果入口 | 主要用途 |
|------|------|------|------|------|------|
| 模型供应商配置 | OWNER | 手动配置 | Provider | `/api/v1/llm-providers` | 接入 DashScope、OpenAI-compatible 等模型服务 |
| AI 审核配置 | OWNER | 手动配置 | Task | `/api/v1/tasks/{taskId}/ai-review-configs` | 定义审核 prompt、评分维度、阈值和输出 schema |
| AI Prompt 测试 | OWNER | 手动测试 | Config | `/api/v1/tasks/{taskId}/ai-review-configs/{configId}/test` | 发布前验证 prompt 和结构化输出 |
| 预标注 | LABELER | 手动触发 | Assignment | `/api/v1/assignments/{assignmentId}/pre-annotations/latest` | 整题建议答案、字段建议、置信度和风险提示 |
| LlmTrigger | LABELER / OWNER | 手动触发 | 字段级 | `/api/v1/llm/triggers/runs/{triggerRunId}` | 作答过程中的字段级 AI 辅助 |
| AI 自动审核 | 系统 | 提交后异步触发 | Submission | `/api/v1/submissions/{submissionId}/ai-review` | 给 Reviewer 提供评分、结论和风险建议 |
| AgentRun 追踪 | 有权访问业务对象的用户 | 查询 | 单次 AI 调用 | `/api/v1/agent-runs/{agentRunId}` | 查看输入快照、输出快照、traceId、耗时和错误 |
| AI 指标 | 已认证用户，按 Actuator 权限 | 查询 | 服务指标 | `/actuator/metrics/labelhub.ai.requests` | 查看成功、失败、限流、超时、耗时等埋点 |

### E.2 场景一：OWNER 接入模型供应商

适用页面：Owner AI 配置页、任务创建页的模型选择器。

调用链：

```text
1. POST /api/v1/llm-providers
2. POST /api/v1/llm-providers/{id}/test
3. POST /api/v1/llm-providers/{id}/enable
4. GET  /api/v1/llm-providers
```

创建 Provider 示例：

```json
{
  "providerCode": "dashscope",
  "providerName": "DashScope",
  "baseUrl": "https://dashscope.aliyuncs.com/compatible-mode/v1",
  "apiKey": "sk-xxx",
  "defaultModel": "qwen-plus",
  "customHeaders": {},
  "platformRateLimitPerMinute": 60,
  "taskRateLimitPerMinute": 30,
  "userRateLimitPerMinute": 10
}
```

前端接入要点：

- `apiKey` 只在创建或更新时提交，后端永不返回明文。
- 列表响应里的 `apiKeyConfigured=true` 表示密钥已配置。
- `customHeaders` 中的敏感字段会脱敏返回。
- 只有 `enabled=true` 的 provider 才应出现在 AI 审核、预标注、LlmTrigger 的可选模型列表中。

### E.3 场景二：OWNER 配置 AI 审核并发布任务

适用页面：任务创建/编辑页、AI 审核配置页、发布前校验页。

调用链：

```text
1. GET  /api/v1/llm-providers
2. POST /api/v1/tasks/{taskId}/ai-review-configs
3. POST /api/v1/tasks/{taskId}/ai-review-configs/{configId}/test
4. POST /api/v1/tasks/{taskId}/publish
```

保存 AI 审核配置示例：

```json
{
  "providerId": 50,
  "modelName": "qwen-plus",
  "promptTemplate": "请根据题目、标注答案和任务说明进行审核，输出 JSON。",
  "scoringDimensions": ["准确性", "完整性", "一致性"],
  "passThreshold": 80.00,
  "manualReviewThreshold": 60.00,
  "outputSchema": {
    "type": "object",
    "required": ["decision", "averageScore", "dimensionScores", "suggestion"],
    "properties": {
      "decision": { "type": "string" },
      "averageScore": { "type": "number" },
      "dimensionScores": { "type": "object" },
      "riskFlags": { "type": "array" },
      "suggestion": { "type": "string" }
    }
  }
}
```

Prompt 测试示例：

```json
{
  "itemSnapshot": {
    "text": "待标注文本或图片元数据"
  },
  "answerJson": {
    "label": "正向",
    "reason": "内容表达积极"
  }
}
```

前端接入要点：

- `manualReviewThreshold` 不能大于 `passThreshold`。
- Prompt 测试会创建 `agentRunId`，但不会创建 submission 或 AI 审核结果。
- 发布任务前应确保数据集、模板、奖励规则、AI 审核配置都已完成。

### E.4 场景三：LABELER 使用预标注

适用页面：标注工作台进入单题后，点击“AI 预标注/生成建议”。

调用链：

```text
1. GET  /api/v1/assignments/{assignmentId}
2. POST /api/v1/assignments/{assignmentId}/pre-annotations/run
3. GET  /api/v1/assignments/{assignmentId}/pre-annotations/latest
4. GET  /api/v1/agent-runs/{agentRunId}
```

触发预标注：

```text
POST /api/v1/assignments/{assignmentId}/pre-annotations/run
```

响应示例：

```json
{
  "preAnnotationId": 200,
  "assignmentId": 100,
  "agentRunId": 600,
  "status": "RUNNING",
  "suggestedAnswerJson": null,
  "fieldSuggestions": [],
  "riskFlags": [],
  "overallConfidence": null,
  "errorCode": null,
  "errorMessage": null
}
```

轮询最新结果：

```text
GET /api/v1/assignments/{assignmentId}/pre-annotations/latest
```

成功响应关注字段：

| 字段 | 说明 |
|------|------|
| preAnnotationId | 预标注记录 ID |
| agentRunId | 对应 AgentRun，可继续查链路详情 |
| status | `RUNNING` / `SUCCESS` / `FAILED` / `RATE_LIMITED` 等 |
| suggestedAnswerJson | 整题建议答案 |
| fieldSuggestions | 字段级建议 |
| overallConfidence | 整体置信度 |
| riskFlags | 风险标记 |
| limitations | 模型能力限制或降级说明 |
| finalDiff | 当前草稿与建议答案的差异 |

前端接入要点：

- 只有当前 assignment 的 LABELER 能触发自己的预标注。
- 同一个 assignment 同时只允许一个预标注运行。
- `SUCCESS` 后也不要自动覆盖草稿，必须展示差异并让用户确认采纳。
- 如果需要完整链路，取响应中的 `agentRunId` 调用 AgentRun 详情。

### E.5 场景四：LABELER 作答时使用字段级 LlmTrigger

适用页面：标注表单中的“AI 生成摘要”“AI 改写”“AI 推荐标签”等按钮。

调用链：

```text
1. POST /api/v1/assignments/{assignmentId}/llm-triggers
2. GET  /api/v1/llm/triggers/runs/{triggerRunId}
3. PUT  /api/v1/assignments/{assignmentId}/draft
```

触发示例：

```json
{
  "componentId": "summary",
  "currentAnswerJson": {
    "summary": "",
    "label": "产品咨询"
  },
  "userInstruction": "生成一句更简洁的摘要"
}
```

响应示例：

```json
{
  "triggerRunId": 500,
  "agentRunId": 601,
  "componentId": "summary",
  "suggestionJson": null,
  "patch": {},
  "displayText": null,
  "targetFields": ["summary"],
  "rawModelSummary": null,
  "confidence": null,
  "warnings": [],
  "traceId": "trace-id",
  "status": "RUNNING",
  "latencyMs": null,
  "errorCode": null,
  "errorMessage": null
}
```

轮询成功后：

```json
{
  "triggerRunId": 500,
  "agentRunId": 601,
  "componentId": "summary",
  "suggestionJson": {
    "componentId": "summary",
    "targetFields": ["summary"],
    "patch": {
      "summary": "用户正在咨询产品使用方式。"
    },
    "displayText": "用户正在咨询产品使用方式。",
    "confidence": 0.87,
    "warnings": []
  },
  "patch": {
    "summary": "用户正在咨询产品使用方式。"
  },
  "displayText": "用户正在咨询产品使用方式。",
  "targetFields": ["summary"],
  "confidence": 0.87,
  "warnings": [],
  "traceId": "trace-id",
  "status": "SUCCESS",
  "latencyMs": 1280
}
```

前端接入要点：

- LlmTrigger 标注端只传 `componentId`、`currentAnswerJson` 和可选 `userInstruction`。
- 后端自动读取 assignment、题目、模板组件、任务 AI 审核配置和评分维度。
- `patch` 是可直接合并到 `answerJson` 的结构化结果，`targetFields` 用于告诉前端建议写入哪些字段。
- 用户点击采纳后，再调用 `PUT /api/v1/assignments/{assignmentId}/draft` 保存草稿。
- `RATE_LIMITED` 表示限流，不应继续高频轮询或立即重试。

### E.6 场景五：OWNER 测试 LlmTrigger

适用页面：模板设计器、任务配置页中的 LlmTrigger 调试面板。

调用链：

```text
1. POST /api/v1/tasks/{taskId}/llm-triggers/test
2. GET  /api/v1/llm/triggers/runs/{triggerRunId}
3. GET  /api/v1/tasks/{taskId}/llm-trigger-runs
```

请求体与标注端 LlmTrigger 基本一致，但 OWNER 测试必须额外传 `datasetItemId` 指定测试题目；模型、prompt 基础信息和评分维度仍来自任务 AI 审核配置，不产生 submission，也不写入 labeler 草稿。

前端接入要点：

- 只允许任务 OWNER 测试自己的任务。
- 请求体至少包含 `datasetItemId`、`componentId`、`currentAnswerJson`。
- 测试结果用于调 prompt，不应展示给真实标注员作为正式建议。
- 任务维度日志可用于查看某个 prompt 的运行效果和失败原因。

### E.7 场景六：提交后自动 AI 审核

适用页面：标注提交页、Reviewer 审核列表、Reviewer 提交详情页。

调用链：

```text
1. POST /api/v1/assignments/{assignmentId}/submit
2. GET  /api/v1/submissions/{submissionId}/ai-review
3. GET  /api/v1/agent-runs/{agentRunId}
4. POST /api/v1/submissions/{submissionId}/ai-review/retry
```

提交后响应通常会返回 submission 状态：

```json
{
  "submissionId": 300,
  "status": "AI_REVIEWING",
  "versionNo": 1
}
```

轮询 AI 审核：

```text
GET /api/v1/submissions/{submissionId}/ai-review
```

成功响应关注字段：

| 字段 | 说明 |
|------|------|
| id | AI 审核结果 ID |
| submissionId | 提交 ID |
| agentRunId | 对应 AgentRun |
| providerId | 使用的 provider |
| modelName | 使用的模型 |
| status | `PENDING` / `RUNNING` / `SUCCESS` / `FAILED` / `RATE_LIMITED` / `MANUAL_REQUIRED` |
| decision | AI 结论，如 `PASS` / `REJECT` / `MANUAL_REVIEW` |
| averageScore | 平均分 |
| dimensionScores | 各维度评分 |
| riskFlags | 风险标记 |
| suggestion | 给 Reviewer 的审核建议 |
| confidence | 置信度 |
| flowAction | 建议流转动作 |
| promptMode | Prompt 模式 |
| degraded | 是否降级 |
| limitations | 限制说明 |
| errorCode | 错误码 |
| errorMessage | 错误信息 |

前端接入要点：

- AI 审核完成后，submission 会进入人工终审路径。
- AI 结论仅供 Reviewer 参考，不会自动把 submission 改成 `APPROVED`。
- `FAILED`、`RATE_LIMITED`、`MANUAL_REQUIRED` 都应允许 Reviewer 继续人工处理。
- Reviewer 可调用 `/retry` 触发重试，每次重试会产生新的 AgentRun。

### E.8 场景七：任务级 AI 日志查询

适用页面：Owner 任务监控页、Reviewer 质检页、AI 运维排查页。

AI 审核日志：

```text
GET /api/v1/tasks/{taskId}/ai-review-logs?page=1&pageSize=20&status=SUCCESS&decision=PASS
```

LlmTrigger 日志：

```text
GET /api/v1/tasks/{taskId}/llm-trigger-runs?page=1&pageSize=20&status=SUCCESS&componentId=summary
```

筛选参数：

| 参数 | 说明 |
|------|------|
| page | 页码 |
| pageSize | 每页数量 |
| status | 运行状态 |
| decision | AI 审核结论，仅 AI 审核日志支持 |
| componentId | LlmTrigger 组件/按钮标识，仅 LlmTrigger 日志支持 |
| startTime | 开始时间，ISO_DATE_TIME |
| endTime | 结束时间，ISO_DATE_TIME |

前端接入要点：

- OWNER/REVIEWER/ADMIN 可查看任务维度日志。
- 日志列表用于定位批量失败、限流、模型异常和 prompt 效果。
- 单条日志中的 `agentRunId` 可继续跳转到 AgentRun 详情。

### E.9 场景八：AgentRun 完整链路追踪

适用页面：AI 结果详情弹窗、运维排查页、Reviewer 详情页中的“查看 AI 调用详情”。

调用：

```text
GET /api/v1/agent-runs/{agentRunId}
```

响应关注字段：

| 字段 | 说明 |
|------|------|
| agentRunId | AgentRun ID |
| agentType | `AI_REVIEW` / `PRE_ANNOTATION` / `LLM_TRIGGER` / `AI_REVIEW_CONFIG_TEST` 等 |
| submissionId | 关联 submission，可为空 |
| assignmentId | 关联 assignment，可为空 |
| providerId | Provider ID |
| modelName | 模型名 |
| promptVersion | Prompt 版本 |
| status | 运行状态 |
| inputSnapshot | 输入快照，包括 prompt、题目、当前答案等 |
| outputSnapshot | 输出快照，包括模型结构化结果或原始摘要 |
| errorMessage | 错误信息 |
| traceId | 本次链路追踪 ID |
| queuedAt | 入队时间 |
| startedAt | 开始执行时间 |
| finishedAt | 结束时间 |
| latencyMs | 调用耗时 |
| redacted | 是否因为权限做了脱敏 |

前端接入要点：

- LABELER 只能查看自己 assignment 相关的 AgentRun。
- OWNER/REVIEWER 按任务/提交权限查看。
- `redacted=true` 时表示后端已对敏感输入或输出做权限脱敏。
- Provider API Key 和敏感 header 不会出现在响应中。

### E.10 场景九：AI 性能与可用性指标

适用页面：运维监控、健康检查、AI 调用质量看板。

指标入口：

```text
GET /actuator/metrics
GET /actuator/metrics/labelhub.ai.requests
GET /actuator/metrics/labelhub.ai.latency
```

指标含义：

| 指标 | 类型 | 说明 |
|------|------|------|
| labelhub.ai.requests | Counter | AI 请求结果计数，覆盖成功、失败、限流、超时、无效 JSON、Provider 异常等 |
| labelhub.ai.latency | Timer | AI 调用耗时，单位按 Actuator 返回 |

标签：

| 标签 | 示例 | 说明 |
|------|------|------|
| biz_type | `LLM_GATEWAY` / `AI_REVIEW` / `PRE_ANNOTATION` / `LLM_TRIGGER` | 业务类型 |
| provider_id | `50` / `unknown` | Provider ID |
| model_name | `qwen-plus` / `unknown` | 模型名 |
| status | `SUCCESS` / `FAILED` / `RATE_LIMITED` / `TIMEOUT` | 结果状态 |
| error_code | `none` / `PROVIDER_ERROR` | 错误码 |

前端/运维接入要点：

- 指标只有在当前应用进程发生过 AI 调用后才会出现在 `/actuator/metrics` 中。
- 生产环境建议由 Prometheus 或监控平台抓取 Actuator 指标。
- 可按 `biz_type + status` 统计 AI 成功率、失败率和限流率。
- 可按 `provider_id + model_name` 统计不同模型的延迟和可用性。

### E.11 AI 状态与轮询建议

| 状态 | 含义 | 前端处理 |
|------|------|------|
| PENDING | 已创建，等待执行 | 展示排队中，短间隔轮询 |
| RUNNING | 正在调用模型 | 展示生成中，继续轮询 |
| SUCCESS | 成功 | 展示结果，允许用户采纳或 Reviewer 查看 |
| FAILED | 调用失败 | 展示错误，允许人工处理或按权限重试 |
| RATE_LIMITED | 被限流 | 展示稍后重试，降低轮询频率 |
| MANUAL_REQUIRED | 需要人工处理 | 进入人工审核/人工判断流程 |
| INVALID_JSON | 模型输出无法解析 | 展示结构化失败，建议重试或调整 prompt |
| TIMEOUT | 模型调用超时 | 展示超时，允许重试 |

轮询建议：

```text
前 10 秒：每 1~2 秒轮询一次
10 秒后：每 3~5 秒轮询一次
超过 60 秒：提示用户可稍后回来查看，保留后台任务
遇到 SUCCESS / FAILED / RATE_LIMITED / MANUAL_REQUIRED：停止轮询
```

### E.12 AI 接入检查清单

Owner 配置阶段：

- 已创建并启用 LLM Provider。
- Provider 测试通过，`apiKeyConfigured=true`。
- 任务已配置 AI 审核 prompt、评分维度、阈值和 outputSchema。
- 发布前完成 Prompt 测试。

Labeler 作答阶段：

- 预标注按钮只在 assignment 可编辑状态展示。
- LlmTrigger 按钮传入 `componentId`、`currentAnswerJson` 和可选 `userInstruction`；Provider、模型、评分维度由后端从任务 AI 审核配置读取。
- AI 建议不自动写草稿，必须由用户确认。

Reviewer 审核阶段：

- 审核列表展示 AI 状态、decision、averageScore 和风险标记。
- 审核详情可跳转 AgentRun 查看调用链路。
- AI 失败或限流时仍允许人工审核。
- REVIEWER 可对异常 AI 审核执行 retry。

可观测性阶段：

- AgentRun 详情展示 `traceId`、`queuedAt`、`startedAt`、`finishedAt`、`latencyMs`。
- Actuator 可查询 `labelhub.ai.requests` 和 `labelhub.ai.latency`。
- 监控侧按 `biz_type`、`provider_id`、`model_name`、`status`、`error_code` 聚合。

---

> 文档结束。如有接口变更请同步更新本文件。
