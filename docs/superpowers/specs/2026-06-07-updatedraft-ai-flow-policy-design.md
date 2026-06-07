# 编辑草稿任务时支持修改 AI 流转策略（aiFlowPolicy）

- 日期：2026-06-07
- 状态：已设计，待实现
- 范围：后端 `updateDraft` + `UpdateTaskRequest`，前端编辑草稿表单

## 背景

任务创建（`POST /api/v1/tasks`）已支持内联 AI 配置，且上一轮已为
`CreateTaskRequest` 加入 `aiFlowPolicy` 字段：创建时不填则默认
`MANUAL_FIRST`（只提建议），允许自定义为 `AI_PASS_ONLY` /
`AI_REJECT_ONLY` / `AI_PASS_AND_REJECT` / `ALWAYS_MANUAL`。
`TaskLifecycleService` 在内联构造 `AiReviewConfigRequest` 时按 policy
派生 `allowAiDirectApprove` / `allowAiDirectReject` 两个开关。

但编辑草稿任务（`PUT /api/v1/tasks/{taskId}`，`updateDraft`）目前没有
任何修改 AI 配置的能力——它只有 `task.setAiReviewConfigId(...)`，即只能
引用一个已存在的配置，无法调整已有配置的流转策略。

因此用户在创建任务后，无法再通过编辑草稿修改“AI 直接过审 / 只提建议”
这一策略，只能走独立的 `PUT /tasks/{taskId}/ai-review-configs` 接口。

## 目标

1. 创建任务时强制配置 AI 审核：`aiProviderId` + `aiPrompt` +
   `aiScoringDimensions` 为必填项，不允许创建“无 AI 审核”的任务。
2. 创建时 `aiFlowPolicy` 可自定义；不传则默认 `MANUAL_FIRST`（AI 只提建议）。
   其余可默认字段（阈值、maxRetry、strategy 等）不传则补默认。
3. 编辑草稿任务时，能够修改该任务 AI 配置的流转策略（`aiFlowPolicy`），
   兼顾现有 `updateDraft` 逻辑、不引入大改。

## 非目标

- 不把整套内联 AI 配置（provider/prompt/维度/阈值）搬进 `updateDraft`；
  编辑侧只允许改 `aiFlowPolicy`，不改 provider/prompt/维度。
- 不新增或改动任何 REST 端点的 URL。
- 不提供“系统级默认 provider/prompt 兜底自动生成配置”的能力（B2 方案，已否决）。

## 设计

### 创建侧：强制 AI 配置（B1）

`POST /api/v1/tasks` 的内联 AI 配置由“可选”改为“必填”：

- 校验规则：`aiProviderId` + `aiPrompt`（非空）+ `aiScoringDimensions`
  （非空）三者必填，缺任一则拒绝创建（返回参数校验错误）。
- `aiFlowPolicy` 不传 → 默认 `MANUAL_FIRST`（AI 只提建议）；可自定义。
- 其余可默认字段（`aiPassThreshold` / `aiManualReviewThreshold` /
  `maxRetry` / `aiReviewStrategy` 等）不传则由 `save()` 现有逻辑补默认。
- 现有 `hasAiInlineConfig`（“全有才触发”的可选判定）改为强制校验：
  不再是“凑齐才建配置”，而是“必须凑齐，否则建任务失败”。

> 影响：所有当前不带 AI 字段创建任务的代码/测试将被拒，需同步改造
> （见“影响面”）。这是 B1 的直接后果，已与用户确认接受。

### 整体形态

编辑侧仅针对 `aiFlowPolicy` 做最小改动，不引入整套内联 AI 配置：

- `UpdateTaskRequest` 只新增一个可选字段 `aiFlowPolicy`。
- `updateDraft` 中加一小段逻辑：当传入了 `aiFlowPolicy` 且该草稿任务
  已存在一份 AI 配置时，更新该配置的流转策略字段；否则不动。
- 不走 `AiReviewConfigService.save()` 的整体校验路径（该路径要求
  provider/prompt/维度齐全），而是新增一个轻量方法只更新策略相关字段，
  从而做到“只传 policy 即可改”，且不破坏现有 `updateDraft` 行为。

### 数据流

```
PUT /api/v1/tasks/{taskId}
  body: { ...原有字段, aiFlowPolicy? }
        │
        └─→ updateDraft()
              ├─ 原有任务字段更新（不变）
              └─ if (aiFlowPolicy != null):
                    config = findByTaskId(taskId)
                    if (config != null):
                        aiReviewConfigService.updateFlowPolicy(ownerId, taskId, aiFlowPolicy)
                    else:
                        忽略（该草稿未配置 AI，无策略可调）
```

### 新增 service 方法

`AiReviewConfigService.updateFlowPolicy(Long ownerId, Long taskId, String aiFlowPolicy)`：

- 校验 owner + 草稿状态（复用现有 `loadOwnedDraftTask`）。
- 调用契约：本方法假定配置已存在（由 `updateDraft` 在调用前用
  `findByTaskId` 判存在性，无配置时根本不调用本方法，从而实现“无 AI 草稿
  传 policy = no-op”的边界行为）。作为防御，若进入本方法时配置仍不存在，
  抛 `AI_REVIEW_CONFIG_NOT_FOUND`（正常流程不会触发）。
- 更新三个字段并持久化：
  - `aiFlowPolicy`
  - `allowAiDirectApprove`（由 policy 派生）
  - `allowAiDirectReject`（由 policy 派生）
- 派生规则（与创建侧 `TaskLifecycleService` 完全一致）：
  - `AI_PASS_ONLY` / `AI_PASS_AND_REJECT` → approve = true
  - `AI_REJECT_ONLY` / `AI_PASS_AND_REJECT` → reject = true
  - 其余（含 `MANUAL_FIRST` / `ALWAYS_MANUAL`）→ 两者皆 false
- 追加审计（复用现有 `AI_REVIEW_CONFIG_UPDATED` 审计动作）。

### 接口契约

端点不变，仅 `PUT /api/v1/tasks/{taskId}` 请求体新增一个可选字段：

```jsonc
PUT /api/v1/tasks/{taskId}
{
  "title": "...", "deadlineAt": "...",   // 原有字段不变
  "aiFlowPolicy": "AI_PASS_AND_REJECT"   // 新增，可选；枚举见下
}
```

- 枚举值：`MANUAL_FIRST` | `AI_PASS_ONLY` | `AI_REJECT_ONLY`
  | `AI_PASS_AND_REJECT` | `ALWAYS_MANUAL`
- 不传 = 不改动现有策略（向后兼容，老前端无感）。
- 响应 `TaskStatusResponse` 结构不变。

### 边界行为（兼容、不抛异常给最终用户）

| 场景 | 行为 |
|---|---|
| 不传 `aiFlowPolicy` | 不触碰 AI 配置（现有 `updateDraft` 行为完全不变） |
| 传了，且草稿已有 AI 配置 | 更新该配置的 policy + 派生开关 |
| 传了，但草稿无 AI 配置 | 忽略（no-op），不报错——无配置即无策略可调 |
| 非草稿任务 | 沿用现有 `updateDraft` 的状态校验（非 DRAFT 直接拒） |

### 错误处理

- `updateFlowPolicy` 中无效枚举值：与创建侧一致，按未知值落到“两开关皆 false”
  的安全默认（不抛错），或在 DTO 层加 `@Pattern` 校验提前拒绝。实现时二选一，
  推荐 DTO 层 `@Pattern` 提前拒绝，给前端明确反馈。
- owner 不匹配 / 任务不存在：复用 `loadOwnedDraftTask` 现有异常。

### 测试

- `updateDraft` 传 policy 且已有配置 → 配置 policy 与两开关被正确更新。
- 传 policy 但无配置 → 不报错、配置表无变化。
- 不传 policy → 现有 `updateDraft` 行为不回归（原有测试保持通过）。
- 派生规则单测：5 个枚举值 → 正确的 approve/reject 组合。

### 影响面

- 后端：
  - `CreateTaskRequest` 校验：provider/prompt/维度 由可选改为必填
    （DTO 层 `@NotNull`/`@NotBlank`/`@NotEmpty` 或 service 层显式校验）。
  - `TaskLifecycleService.hasAiInlineConfig`：从“凑齐才建配置”的可选判定，
    反转为“必须凑齐，否则建任务失败”的强制前置校验。
  - `UpdateTaskRequest`（+`aiFlowPolicy` 字段）、
    `TaskLifecycleService.updateDraft`（+一小段调用 `updateFlowPolicy`）、
    `AiReviewConfigService`（+`updateFlowPolicy` 方法）。
- 测试改造（B1 的直接后果）：
  - `TaskLifecycleServiceTest`：3 处 `new CreateTaskRequest` 当前不带 AI
    字段，需补全 provider/prompt/维度，否则创建被拒。
  - `ApiContractMappingTest`：涉及建任务契约的用例需同步带上 AI 字段。
  - 新增用例：缺 AI 字段建任务 → 返回校验错误；editDraft 改 policy 的正反例。
- 前端：
  - 建任务表单：provider/prompt/维度 标记为必填；AI 流转策略下拉可选
    （不填默认 MANUAL_FIRST）。
  - 编辑草稿表单：新增“AI 流转策略”下拉，随 `PUT /tasks/{id}` 一并提交。
- 数据库：无 schema 变更（复用现有 `ai_review_configs` 字段）。
- 兼容性：**破坏性变更**——不再允许创建无 AI 审核的任务；所有现存
  “无 AI 建任务”的调用方（含前端、测试、脚本）必须改造。已与用户确认接受。


