# Supervisor Agent 审核架构设计

## Context

当前 AI 审核是单次直接调用模式（prompt → JSON → done），无法利用多步推理和工具调用来提高审核质量。本设计将 AI 审核升级为 Supervisor Agent 模式：LLM 作为编排者，动态决定调用哪些 tools 收集信息，最终做出审核决策。

同时保留扩展性，未来可复用同一架构支持 AI 预标注功能。

## 设计决策

- **Agent 模式**：Supervisor（LLM 编排决策）
- **Tool 选择**：LLM 通过 OpenAI function calling 协议动态选择
- **编排策略**：按任务配置动态决定可用 tools
- **迭代限制**：不限轮数，设 max_iterations 上限兜底（默认 10）
- **Sub-agent**：渐进式，初期为纯代码 tool，后续可升级为带 LLM 的 sub-agent
- **向后兼容**：通过 `agent_mode` 字段区分 DIRECT（现有）和 SUPERVISOR（新）模式

## 整体架构

```
AiAutoReviewService.executeAttempt()
  │
  ├─ agent_mode=DIRECT → 现有单次调用逻辑（不变）
  │
  └─ agent_mode=SUPERVISOR → SupervisorAgent.execute()
       │
       │  Loop (max N iterations):
       │    1. 组装 messages + available tools schema
       │    2. 调用 LLM (function calling)
       │    3. 解析响应:
       │       - tool_calls → ToolRegistry 执行 → observation
       │       - content (final answer) → 解析结构化结果
       │
       └─ ToolRegistry
            ├─ query_submission_history
            ├─ query_item_submissions
            ├─ query_task_guidelines
            ├─ compare_with_golden
            ├─ compute_similarity
            ├─ detect_anomaly_pattern
            ├─ verify_format
            └─ check_content_safety
```

## 核心接口

### ReviewTool 接口

```java
public interface ReviewTool {
    String name();
    String description();
    Map<String, Object> parametersSchema(); // JSON Schema
    ToolResult execute(Map<String, Object> arguments, ToolContext context);
}

public record ToolContext(Long submissionId, Long taskId, Long datasetItemId,
                          String answerJson, String itemJson) {}

public record ToolResult(boolean success, String output, String errorMessage) {}
```

### ToolRegistry

- 自动发现所有实现 ReviewTool 接口的 Spring Bean
- `getToolsForTask(Long taskId, List<String> enabledTools)` 按配置过滤
- `generateToolDefinitions(...)` 生成 OpenAI function calling 格式的 tools JSON

### SupervisorAgent

```java
public class SupervisorAgent {
    static final int DEFAULT_MAX_ITERATIONS = 10;
    static final int MAX_TOOL_CALLS_PER_TURN = 5;

    SupervisorResult execute(SupervisorRequest request);
}

public record SupervisorRequest(Long submissionId, Long taskId,
    String systemPrompt, String userPrompt,
    List<ToolDefinition> tools, ToolContext toolContext,
    int maxIterations, Long providerId, String modelName) {}

public record SupervisorResult(boolean success, String decision,
    BigDecimal averageScore, Map<String, Object> dimensionScores,
    List<String> riskFlags, String suggestion,
    String rawConversation, String errorCode, String errorMessage) {}
```

## LLM 协议扩展

### LlmMessage 扩展

```java
public record LlmMessage(
    String role,              // "system" | "user" | "assistant" | "tool"
    String content,
    List<ToolCall> toolCalls, // assistant 消息中的 tool 调用
    String toolCallId,        // tool 响应消息的 call id
    String name               // tool 响应消息的 tool name
) {
    // 保留现有双参数构造器向后兼容
    public LlmMessage(String role, String content) {
        this(role, content, null, null, null);
    }
}

public record ToolCall(String id, String type, FunctionCall function) {}
public record FunctionCall(String name, String arguments) {}
public record ToolDefinition(String type, FunctionDef function) {}
public record FunctionDef(String name, String description, Map<String, Object> parameters) {}
```

### OpenAiCompatibleAdapter 扩展

- 新增 `chat(LlmProviderRuntimeConfig, List<LlmMessage>, List<ToolDefinition>)` 方法
- 请求体增加 `"tools": [...]` 字段
- 响应解析增加 `choices[0].message.tool_calls` 提取
- 支持 `finish_reason: "tool_calls"` 判断是否需要继续循环

## Tool 集合（初始）

| Tool | 类别 | 功能 | 参数 |
|------|------|------|------|
| query_submission_history | 数据查询 | 查询 labeler 历史提交和通过率 | labeler_id, limit |
| query_item_submissions | 数据查询 | 查询同一 item 其他标注者答案 | dataset_item_id |
| query_task_guidelines | 数据查询 | 获取任务标注规范 | task_id |
| compare_with_golden | 对比分析 | 对比答案与 golden answer | submission_id |
| compute_similarity | 对比分析 | 计算两个答案的相似度 | answer_a, answer_b |
| detect_anomaly_pattern | 对比分析 | 检测异常模式 | answer_json |
| verify_format | 外部验证 | 校验答案格式 | answer_json, schema |
| check_content_safety | 外部验证 | 内容安全审核 | text_content |

## 数据库变更

```sql
ALTER TABLE ai_review_configs
  ADD COLUMN agent_mode VARCHAR(24) NOT NULL DEFAULT 'DIRECT'
    COMMENT 'DIRECT | SUPERVISOR',
  ADD COLUMN enabled_tools JSON NULL
    COMMENT '启用的 tool 名称列表，null=全部',
  ADD COLUMN max_iterations INT NOT NULL DEFAULT 10,
  ADD CONSTRAINT chk_agent_mode CHECK (agent_mode IN ('DIRECT', 'SUPERVISOR'));
```

## 与现有代码集成

### AiAutoReviewService 修改

`executeAttempt()` 根据 `config.getAgentMode()` 分流：
- `DIRECT`：现有逻辑不变
- `SUPERVISOR`：调用 `SupervisorAgent.execute()`，将结果映射为 `AttemptOutcome`

### AgentRun 记录

- `inputSnapshot`：初始 messages + tools 配置
- `outputSnapshot`：完整多轮对话历史（含 tool_calls 和 tool results）
- 一次 Supervisor 执行 = 一条 AgentRun

### 重试逻辑（5.4）

对 Supervisor 透明。Supervisor 执行失败（LLM 调用失败、超时）触发现有重试机制。Tool 执行失败不触发重试，由 Supervisor LLM 自行决定如何处理。

## 安全机制

- `max_iterations`：防止无限循环（默认 10，可配置）
- `MAX_TOOL_CALLS_PER_TURN = 5`：单轮最多 5 个 tool 调用
- 单个 tool 执行超时：5 秒
- 总执行时间上限：通过 LLM gateway timeout 控制
- Tool 权限隔离：tool 只能访问当前 submission 相关数据

## 未来扩展

- **AI 预标注**：复用 ToolRegistry + SupervisorAgent，换 system prompt 和 tool 集合
- **Sub-agent 升级**：tool 内部可启动 mini SupervisorAgent
- **新增 tool**：只需实现 ReviewTool 接口 + 注册为 Spring Bean
- **多模型支持**：不同 tool 可配置不同 LLM provider
