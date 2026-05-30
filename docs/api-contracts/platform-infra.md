# 平台基础设施契约

## 适用范围

- Owner 模块：BE-B。
- 覆盖内容：Redis/Redisson、AsyncJobService、RateLimitService、AI 预审队列、幂等键和补偿扫描。

## Redis Key

```text
lock:claim:task:{taskId}:item:{itemId}
draft:assignment:{assignmentId}
llm:rate:platform:{providerCode}
llm:rate:task:{taskId}
llm:rate:user:{userId}
ai:review:dedup:{submissionId}
ai:review:stream:task:{taskId}
event:dedup:{eventType}:{bizId}
```

说明：
- `lock:claim:*` 用于领取题目并发控制。
- `draft:assignment:*` 用于草稿缓存。
- `llm:rate:*` 用于模型调用限流。
- `ai:review:dedup:*` 用于 AI 预审幂等。
- `ai:review:stream:task:*` 用于按任务隔离的 AI 预审 FIFO 队列。
- `event:dedup:*` 用于奖励和其他域事件消费幂等。

## AI 预审队列

- 队列按 `taskId` 分流。
- 消费者组名称固定为 `ai-review-workers`。
- 消费确认必须在数据库状态、AI 结果和审计全部落库后再 ACK。
- 如果入队失败，必须依赖补偿扫描重投，不允许直接丢弃。

## 限流契约

- `RateLimitService.tryAcquire(key, permits)` 返回是否允许、以及建议等待时间。
- 限流维度至少支持平台、任务、用户三层。
- 调用方不得把 429 当作系统错误，应该展示可重试态。

## 异步任务

- `AsyncJobService.submit(command)` 仅负责编排和派发，不直接暴露实现细节。
- 导入、导出、AI 预审可以共用同一执行器模型，但状态仍分别写回各自 job 表或结果表。
- 异步失败必须保存错误信息和 traceId。

## 错误码

|code|场景|
|---|---|
|`409101`|并发冲突、锁竞争失败。|
|`429001`|限流命中。|
|`500001`|基础设施执行失败。|
