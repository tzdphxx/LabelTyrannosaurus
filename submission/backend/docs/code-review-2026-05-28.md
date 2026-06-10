# Code Review Report — 2026-05-28

> 分支：`tzdphxx` vs `main`  
> 范围：191 files changed, +16,763 lines  
> 审查方法：对照 `docs/` 契约文档 + `DEVELOPMENT_GUIDELINES.md` 逐模块比对

---

## 一、致命问题（P0 — 阻塞启动或核心功能）

### 1.1 Flyway 迁移版本号重复

| 项目 | 内容 |
|------|------|
| 文件 | `V2__seed_system_agent.sql`, `V2__create_llm_providers.sql` |
| 文档依据 | DEVELOPMENT_GUIDELINES §3.6 "数据库结构通过 Flyway migration 管理" |
| 问题 | 两个文件都使用版本号 V2，Flyway 启动时抛出 `FlywayException` |
| 影响 | **应用完全无法启动** |
| 修复建议 | 将 `V2__seed_system_agent.sql` 改为 `V2_1__seed_system_agent.sql` 或重新编号 |

### 1.2 分布式锁在事务内释放（领取竞态）

| 项目 | 内容 |
|------|------|
| 文件 | `AssignmentClaimService.java:56-83` |
| 文档依据 | DEVELOPMENT_GUIDELINES §3.5 "领取必须使用 Redis/Redisson 锁和数据库唯一约束兜底" |
| 问题 | `@Transactional` 方法内 `finally` 释放锁，但事务在方法返回后才提交 |
| 影响 | 锁释放 → 事务未提交 → 并发线程获取锁 → 读到未提交数据 → 同一数据条目被重复领取 |
| 修复建议 | 方案 A：将锁逻辑提到事务外层（非事务 facade 方法中加锁，内部调事务方法）；方案 B：使用 `TransactionSynchronizationManager.registerSynchronization(afterCommit)` 释放锁 |

### 1.3 冲突解决未拒绝其他提交

| 项目 | 内容 |
|------|------|
| 文件 | `ConflictResolveService.java:87-89` |
| 文档依据 | DEVELOPMENT_GUIDELINES §3.4 "冲突仲裁只有一个金标" |
| 问题 | `resolve()` 将选定提交设为 `APPROVED + isGolden=true`，但同组其他 `PENDING_FINAL` 提交未被更新 |
| 影响 | 其他提交仍出现在审核队列，可被单独批准，导致同一数据条目出现多个金标 |
| 修复建议 | resolve() 中对同组非金标提交执行 `status=REJECTED, isGolden=false`，并更新对应 assignment 为 RETURNED |

### 1.4 标记人工审核未更新提交状态

| 项目 | 内容 |
|------|------|
| 文件 | `BatchReviewService.java:107-127` |
| 文档依据 | DEVELOPMENT_GUIDELINES §3.4 "状态机由后端强制执行" |
| 问题 | `trySingleMarkManual()` 只插入 `ReviewRecord`，未修改 `submission.status` |
| 影响 | 提交仍为 `PENDING_FINAL`，可被其他审核员批量通过，绕过人工审核门控 |
| 修复建议 | 插入 ReviewRecord 的同时将 submission.status 更新为 `MANUAL_REQUIRED`（需在 SubmissionStatus 枚举中确认该值存在） |

### 1.5 领取接口缺少角色权限校验

| 项目 | 内容 |
|------|------|
| 文件 | `AssignmentController.java:23` |
| 文档依据 | assignment.md "权限：LABELER"；DEVELOPMENT_GUIDELINES §3.3 "每个接口必须校验角色权限" |
| 问题 | 控制器无 `@PreAuthorize` 或角色注解，任何用户都能调用领取接口 |
| 影响 | Owner/Reviewer 等非标注员角色可以领取任务 |
| 修复建议 | 添加 `@PreAuthorize("hasRole('LABELER')")` |

---

## 二、中等问题（P1 — API 契约不一致 / 功能桩阻塞）

### 2.1 错误码与文档不符

| 项目 | 内容 |
|------|------|
| 文件 | `AssignmentDraftService.java:26`, `SubmissionSubmitService.java:42` |
| 文档依据 | assignment.md "错误码：409101 draftVersion 冲突" |
| 问题 | 草稿保存用 `409301`，提交用 `409401`，前端按文档 `409101` 处理会失败 |
| 修复建议 | 统一为文档规定的 `409101` |

### 2.2 发布前检查全部硬编码返回 false

| 项目 | 内容 |
|------|------|
| 文件 | `DefaultTaskPublishDependencyChecker.java:17-38` |
| 文档依据 | task.md 发布前检查 "dataset exists / template version exists / reward rule exists" |
| 问题 | `datasetReady()`、`templateVersionExists()`、`rewardRuleExists()` 全部返回 false |
| 影响 | 任何任务都无法发布（BE-B 集成桩未实现） |
| 修复建议 | MVP 阶段暂时返回 true（或根据数据库记录判断），并加 TODO 标记待 BE-B 接入 |

### 2.3 冲突解决未校验提交当前状态

| 项目 | 内容 |
|------|------|
| 文件 | `ConflictResolveService.java:79-85` |
| 文档依据 | DEVELOPMENT_GUIDELINES §3.4 状态机正确性 |
| 问题 | 未检查被选提交的 `status`，已 REJECTED 的提交可被选为金标 |
| 影响 | 提交 APPROVED 但 assignment 仍为 RETURNED，状态不一致 |
| 修复建议 | 添加 `if (golden.getStatus() != SubmissionStatus.PENDING_FINAL) throw ...` |

### 2.4 迁移顺序：V3 引用 V5 的表

| 项目 | 内容 |
|------|------|
| 文件 | `V3__agent_runs.sql:5` |
| 文档依据 | DEVELOPMENT_GUIDELINES §3.6 |
| 问题 | `agent_runs.submission_id NOT NULL` 引用 `submissions` 表，但该表在 V5 才创建 |
| 影响 | 无法添加外键约束，运行时可能出现引用不存在的 submission_id |
| 修复建议 | 调整迁移顺序，或将 submissions 表创建移至 V3 之前 |

---

## 三、低等问题（P2 — 命名偏差 / 防御性不足）

### 3.1 请求字段命名与文档不符

| 项目 | 内容 |
|------|------|
| 文件 | `AssignmentDraftSaveRequest.java:7`, `SubmissionSubmitRequest.java:6` |
| 文档依据 | assignment.md "请求字段：clientVersion" |
| 问题 | 代码中字段名为 `clientDraftVersion`，文档为 `clientVersion` |
| 影响 | 前端按文档开发会传错字段名 |
| 修复建议 | 统一为文档规定的 `clientVersion`，或更新文档 |

### 3.2 ReviewService 缺少空值检查

| 项目 | 内容 |
|------|------|
| 文件 | `ReviewService.java:73,98` |
| 文档依据 | DEVELOPMENT_GUIDELINES §3.1 "不允许空 catch；异常必须转换为业务错误" |
| 问题 | `assignmentMapper.selectById()` 返回值未判空，孤儿数据导致 NPE |
| 影响 | 审核操作返回 500 而非业务错误码 |
| 修复建议 | 添加 `if (assignment == null) throw new BusinessException(...)` |

### 3.3 TaskLifecycleService NPE 风险

| 项目 | 内容 |
|------|------|
| 文件 | `TaskLifecycleService.java:258` |
| 问题 | `task.getOwnerId().equals(ownerId)` 未处理 ownerId 为 null 的情况 |
| 修复建议 | 改为 `!ownerId.equals(task.getOwnerId())` 或 `!Objects.equals(...)` |

### 3.4 LlmTriggerService.asLong() 缺少异常处理

| 项目 | 内容 |
|------|------|
| 文件 | `LlmTriggerService.java:342` |
| 文档依据 | DEVELOPMENT_GUIDELINES §3.3 "所有业务错误必须返回文档中的错误码" |
| 问题 | `Long.parseLong(text)` 无 try-catch，非数字字符串导致 500 |
| 修复建议 | 包裹 try-catch，抛出 `BusinessException(LLM_TRIGGER_INVALID, ...)` |

### 3.5 AiReviewConfig 阈值范围仅 DTO 层校验

| 项目 | 内容 |
|------|------|
| 文件 | `AiReviewConfigService.java:196` |
| 文档依据 | ai-review.md "Thresholds must be between 0.00 and 100.00" |
| 问题 | 范围校验仅在 DTO `@DecimalMin/@DecimalMax` 注解，Service 层未二次校验 |
| 影响 | 若 Service 被内部直接调用（绕过 Controller @Valid），可存入非法阈值 |
| 修复建议 | 在 `validateRequest()` 中增加范围检查 |

---

## 四、确认符合文档的实现

以下核心契约经审查确认正确实现：

| 模块 | 契约要求 | 实现状态 |
|------|----------|----------|
| Assignment | 领取响应包含 7 个字段 | ✅ 全部存在 |
| Assignment | 领取使用 Redis 锁 + 唯一约束兜底 | ✅ tryLock + DuplicateKeyException |
| Assignment | 草稿乐观锁 `WHERE draft_version = #{expected}` | ✅ |
| Assignment | 提交创建 `AI_REVIEWING` 状态 | ✅ |
| Assignment | 提交入队 AI 审核（创建 AgentRun PENDING） | ✅ |
| Assignment | 打回重提 `versionNo = previous + 1` | ✅ |
| Review | approve 设置 `isGolden=true` | ✅ |
| Review | approve 发布 `SubmissionApproved` 事件 | ✅ |
| Review | reject 设置 `submission=REJECTED, assignment=RETURNED` | ✅ |
| Review | 批量审核逐条处理，返回成功/失败列表 | ✅ |
| Review | 导出只含 `APPROVED + isGolden=true` | ✅ SQL WHERE 正确 |
| AI | LlmTrigger 限流后不调用 LlmGateway | ✅ |
| AI | AiReviewConfig 仅 DRAFT 可配置 | ✅ |
| AI | AiReviewConfig 保存后回填 `tasks.aiReviewConfigId` | ✅ |
| AI | `manualReviewThreshold <= passThreshold` 校验 | ✅ |
| AI | Provider 必须存在且 enabled | ✅ |
| AI | previewMode=true 校验 OWNER 角色 + 任务归属 | ✅ |
| Task | 状态机 DRAFT→PUBLISHED→PAUSED/ENDED | ✅ |
| Task | 发布前检查 quota>0, overlapCount>=1, deadlineAt>now | ✅ |
| Task | AI 不能直接设置 APPROVED 或 isGolden | ✅ |

---

## 五、架构与规范合规性

### 5.1 符合规范

- ✅ 包结构遵循 `modules/{module}/web|service|domain|dto|mapper` 分层
- ✅ Controller 只做参数接收和响应返回
- ✅ DTO 与 Entity 分离
- ✅ 状态字段存储枚举字符串
- ✅ 时间字段使用 `LocalDateTime`，命名 `xxxAt`
- ✅ 金额/分数使用 `BigDecimal`
- ✅ 审计表只追加
- ✅ `system_ai_agent` 为不可登录系统用户

### 5.2 待改进

- ⚠️ 部分 Controller 缺少 `@PreAuthorize` 角色注解（依赖 MockCurrentUserProvider 跳过）
- ⚠️ 异步任务（AI 审核）失败后无重试机制
- ⚠️ 批量审核 `@Transactional` 包裹整个方法，基础设施异常仍会全量回滚

---

## 六、修复优先级总览

| 优先级 | 编号 | 问题 | 修复工作量 |
|--------|------|------|-----------|
| **P0** | 1.1 | Flyway V2 重复 | 5 min |
| **P0** | 1.2 | 领取锁竞态 | 30 min |
| **P0** | 1.3 | 冲突解决遗漏 | 20 min |
| **P0** | 1.4 | 标记人工审核状态 | 15 min |
| **P0** | 1.5 | 领取接口权限 | 5 min |
| **P1** | 2.1 | 错误码不符 | 10 min |
| **P1** | 2.2 | 发布检查桩 | 15 min |
| **P1** | 2.3 | 冲突解决状态校验 | 10 min |
| **P1** | 2.4 | 迁移顺序 | 20 min |
| **P2** | 3.1 | 字段命名 | 5 min |
| **P2** | 3.2 | ReviewService null 检查 | 10 min |
| **P2** | 3.3 | TaskLifecycle NPE | 5 min |
| **P2** | 3.4 | asLong 异常处理 | 5 min |
| **P2** | 3.5 | 阈值二次校验 | 5 min |

**预估总修复时间：约 2.5 小时**

---

## 七、结论

代码整体架构清晰，核心业务流程（领取→草稿→提交→AI审核→人工审核→导出）的主路径实现正确。主要风险集中在：

1. **启动阻塞**：Flyway 版本冲突必须立即修复
2. **并发安全**：领取锁释放时机不对，需要调整事务边界
3. **数据一致性**：冲突解决和人工审核标记存在状态遗漏，可能产生多金标

建议先修复 P0 问题后再进行集成测试。
