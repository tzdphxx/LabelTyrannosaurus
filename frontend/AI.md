# AI 审核结果接口 reviewTrace 对接说明

## 接口

`GET /api/v1/submissions/{submissionId}/ai-review`

该接口仍然用于查询某条提交的 AI 预审结果。现有入参不变，原有响应字段不变，本次只新增一个响应字段：`reviewTrace`。

## 新增字段

字段名：`reviewTrace`

类型：Object，可为空。

用途：描述本次 AI 审核实际采用了什么策略，以及该策略如何得到最终结果。它用于区分单模型审核、多模型投票、维度专项审核、Agent 辩论 / Supervisor 审核等场景。

## 返回结构

```json
{
  "reviewTrace": {
    "strategy": "PARALLEL_VOTE",
    "strategyLabel": "Parallel model vote",
    "summary": "3 model branches reviewed in parallel; 2 branch(es) supported the final decision; consensus threshold was met.",
    "steps": [
      {
        "name": "qwen-plus",
        "role": "voter",
        "decision": "PASS",
        "score": "90",
        "confidence": "0.9",
        "status": "SUCCESS",
        "reason": "The submitted answer matches the task requirements."
      }
    ],
    "metrics": {
      "voteCount": 3,
      "topVotes": 2,
      "hasConsensus": true,
      "minAgreement": 2
    }
  }
}
```

## 字段说明

| 字段 | 类型 | 说明 |
|---|---|---|
| `strategy` | String | 审核策略编码，例如 `LIGHTWEIGHT`、`PARALLEL_VOTE`、`DEEP_DIMENSION`、`AGENT_DEBATE` |
| `strategyLabel` | String | 策略展示名称 |
| `summary` | String | 后端生成的策略过程摘要，可直接展示 |
| `steps` | Array | 策略执行步骤列表，例如每个投票模型、每个维度 reviewer、Supervisor 步骤 |
| `metrics` | Object | 策略聚合指标，例如投票数、一致票数、阈值、工具数 |

`steps` 每项字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| `name` | String | 模型名、维度名或 Agent 名 |
| `role` | String | 步骤角色，例如 `single_reviewer`、`voter`、`dimension_reviewer`、`supervisor` |
| `decision` | String | 该步骤给出的结论 |
| `score` | String | 该步骤评分，可能为空 |
| `confidence` | String | 该步骤置信度，可能为空 |
| `status` | String | 步骤执行状态，例如 `SUCCESS`、`FAILED` |
| `reason` | String | 该步骤的简短原因或建议 |

## 对接建议

1. 保持原有字段展示逻辑不变：`decision`、`averageScore`、`dimensionScores`、`riskFlags`、`suggestion` 仍然是最终审核结论。
2. 新增一个“AI 审核策略”展示区，优先展示 `reviewTrace.strategyLabel` 和 `reviewTrace.summary`。
3. 如果 `reviewTrace.steps` 不为空，可以用时间线或列表展示每个模型 / 维度 / Agent 的步骤结果。
4. 如果 `reviewTrace.metrics` 不为空，可以展示为标签，例如 `voteCount: 3`、`topVotes: 2`、`hasConsensus: true`。
5. 兼容旧数据：当 `reviewTrace` 为 `null` 时，隐藏策略区或展示“暂无策略过程”。不要影响原有 AI 审核结果展示。

## 不同策略的表现

### 单模型审核

`strategy = LIGHTWEIGHT`

特点：只有一个步骤，`role = single_reviewer`，表示单个模型直接生成最终审核结果。

### 多模型投票

`strategy = PARALLEL_VOTE`

特点：`steps` 中通常有多个 `voter`，`metrics` 中会包含：

- `voteCount`：参与投票的成功分支数量
- `topVotes`：支持最终结论的最高票数
- `hasConsensus`：是否达到一致性要求
- `minAgreement`：最小一致票数阈值

### 维度专项审核

`strategy = DEEP_DIMENSION`

特点：`steps` 中每项通常对应一个维度 reviewer，`metrics` 中会包含：

- `dimensionCount`
- `minAgreement`
- `passThreshold`
- `manualReviewThreshold`

### Agent 辩论 / Supervisor 审核

`strategy = AGENT_DEBATE` 或 Supervisor 相关策略

特点：`steps` 中会包含 supervisor 步骤，`metrics` 中可能包含：

- `maxIterations`
- `toolCount`
- `agentMode`

## 最小前端兼容示例

```ts
const trace = aiReviewResult.reviewTrace

if (trace) {
  console.log(trace.strategyLabel ?? trace.strategy)
  console.log(trace.summary)
  console.table(trace.steps ?? [])
  console.log(trace.metrics ?? {})
}
```

## 注意事项

- `reviewTrace` 是展示和追溯字段，不参与前端本地决策。
- 最终审核结论仍以顶层 `decision` 为准。
- 最终分数仍以顶层 `averageScore` 为准。
- 各维度评分仍以顶层 `dimensionScores` 为准。
- 调用方必须兼容 `reviewTrace = null`。
