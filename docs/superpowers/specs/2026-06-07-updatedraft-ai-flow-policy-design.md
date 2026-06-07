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

让用户在编辑草稿任务时，能够修改该任务 AI 配置的流转策略
（`aiFlowPolicy`），且尽量兼顾现有 `updateDraft` 逻辑、不引入大改。

## 非目标

- 不把整套内联 AI 配置（provider/prompt/维度/阈值）搬进 `updateDraft`。
  那是更重的“完全对称”方案，本设计明确不做。
- 不改变任务创建侧的行为（创建侧已完成，保持不变）。
- 不新增或改动任何 REST 端点的 URL。
- 不强制所有任务都必须配置 AI 审核（保持创建侧的向后兼容）。

## 设计

### 整体形态

仅针对 `aiFlowPolicy` 做最小改动，不引入整套内联 AI 配置：

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
- 读取该任务的 AI 配置；不存在则抛 `AI_REVIEW_CONFIG_NOT_FOUND`
  （由 `updateDraft` 在调用前先判存在性，避免对“无 AI 草稿”报错）。
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

- 后端：`UpdateTaskRequest`（+1 字段）、`TaskLifecycleService.updateDraft`
  （+一小段）、`AiReviewConfigService`（+`updateFlowPolicy` 方法）。
- 前端：编辑草稿表单新增一个“AI 流转策略”下拉，仅在该任务已有 AI 配置时显示；
  保存时随 `PUT /tasks/{id}` 一并提交。
- 数据库：无 schema 变更（复用现有 `ai_review_configs` 字段）。
- 兼容性：纯增量，不破坏任何现有调用与测试。


