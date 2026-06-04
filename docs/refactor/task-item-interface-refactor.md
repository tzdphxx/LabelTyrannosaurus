# 后端 "任务(Task)" 与 "题目(Item)" 接口重构方案

> 版本: v1.1  
> 日期: 2026-06-04  
> 状态: 待实施  
> 
> **v1.1 修正**: 
> - 删除 Owner 预分配审核员接口（与自领取+超时自动分配模型冲突）
> - Labeler 领取路径改为 `POST /tasks/{taskId}/items/claim`（选项A）
> - Batch 端点保留 `batch-append/batch-update/batch-delete` 命名
> - 补全 Reviewer 已有接口（工作台/dashboard/AI状态/批量操作/scope参数）
> - 补全 Owner 导出接口
> - 补全 AI 重试接口
> - **所有接口必须包含完整的 Swagger `@Operation` 和 `@Schema` 注解**

---

## 目录

1. [问题诊断](#1-问题诊断)
2. [决策汇总](#2-决策汇总)
3. [Domain 层重构](#3-domain-层重构)
4. [DTO 层重构](#4-dto-层重构)
5. [API 路径重构](#5-api-路径重构)
6. [各角色接口全景](#6-各角色接口全景)
7. [实施阶段](#7-实施阶段)

---

## 1. 问题诊断

### 1.1 同一张表存在两个 Java Entity 类

| 数据库表 | 类名 | 位置 | 字段数 | 使用方 |
|----------|------|------|--------|--------|
| `tasks` | `Task` | `modules/task/domain/` | 17 (Lombok) | BE-A: TaskLifecycleService, TaskManagementService |
| `tasks` | `TaskEntity` | `modules/task/domain/` | 10 (手写getter) | BE-B: DatasetItemService |
| `dataset_items` | `DatasetItem` | `modules/dataset/domain/` | 9 (Lombok) | BE-A: TaskMarketService |
| `dataset_items` | `DatasetItemEntity` | `modules/dataset/domain/` | 11 (含 transient) | BE-B: DatasetItemService |

**根因**: BE-A / BE-B 模块拆分后双方各自建了 ORM 模型，但模块化单体不需要在 ORM 层隔离。

### 1.2 Mapper 双重身份

每张表存在两套查询层，职责边界无文档:

```
tasks → TaskMapper + TaskRepositoryMapper
dataset_items → DatasetItemMapper + DatasetItemRepositoryMapper
```

### 1.3 DTO 爆炸

同一任务概念对应 6 个 Response DTO，同一题目概念对应 3 个 Response DTO:

| DTO | 用在哪里 | 字段数 |
|-----|----------|--------|
| `OwnerTaskSummaryResponse` | Owner 任务列表 | 13 |
| `OwnerTaskPageResponse` | 分页包装 | - |
| `TaskDetailResponse` | 任务详情 | 22 |
| `TaskLifecycleResponse` | 状态变更返回 | 2 |
| `CreateTaskResponse` | 创建返回 | 3 |
| `MarketTaskResponse` | Labeler 市场 | 14 |

### 1.4 API 路径命名不一致

| 问题 | 示例 |
|------|------|
| 角色前缀分散 | `/owner/tasks`, `/market/tasks`, `/labeler/claimed-tasks`, `/reviewer/tasks/{id}/claim` |
| 题目路径冗余 `dataset` | `/tasks/{taskId}/dataset/items` 而非 `/tasks/{taskId}/items` |
| 同一个资源的操作路径不统一 | 领取: `POST /tasks/{taskId}/assignments/claim`，详情: `GET /assignments/{id}` |
| `assignment` vs `claim` 用词不一致 | ClaimStrategy 叫 FCFS，API 路径叫 assignment |

### 1.5 ReviewTask 命名冲突

`ReviewTask` (表 `review_tasks`) 是审核员对一条 submission 的审核记录，**不是**业务 Task。名字里的 "Task" 和业务 Task 冲突。

---

## 2. 决策汇总

| # | 决策点 | 决定 |
|---|--------|------|
| 1 | Model 合并 | **合并**: `Task` + `TaskEntity` → `Task`; `DatasetItem` + `DatasetItemEntity` → `DatasetItem` |
| 2 | Mapper 合并 | **合并**: 每张表一个 Mapper，删掉 RepositoryMapper |
| 3 | DTO 策略 | **方案 B**: 扁平为主，字段重复可接受；组合视图用嵌套 |
| 4 | API 路径 | **保留角色前缀**，去掉 `/dataset/` 冗余段，`assignment` → `claim` 改名 |
| 5 | 路径扁平 vs 嵌套 | **两者共存**: 创建时嵌套（需要父资源 ID），资源有独立 ID 后支持扁平路径 |
| 6 | Batch 端点 | **不合并**: 保留 `append/update/delete` 三个独立端点 |
| 7 | ReviewTask 重命名 | `ReviewTask` → `ReviewRecord`，`review_tasks` 表 → `review_records` |
| 8 | 角色路由 | `GET /api/v1/tasks` 按角色分流，Owner 和 Labeler 不重叠 |

---

## 3. Domain 层重构

### 3.1 Task — 合并为单一模型

**删除**: `TaskEntity.java`

**保留**: `Task.java`，合并 `TaskEntity` 中缺少的字段（实际对比后 `TaskEntity` 是 `Task` 的子集，无需补充）。

```java
// modules/task/domain/Task.java — 唯一模型，BE-A 和 BE-B 共用
@Getter
@Setter
@TableName("tasks")
public class Task {

    @TableId(type = IdType.AUTO)
    private Long id;                          // 原 Task.id
    private Long ownerId;                     // 原 Task.ownerId
    private String title;                     // 原 Task.title
    private String description;               // 原 Task.description
    private String instructionRichText;       // 原 Task.instructionRichText
    private TaskStatus status;                // 原 Task.status
    private Integer quota;                    // 原 Task.quota
    private Integer claimedCount;             // 原 Task.claimedCount
    private Integer overlapCount;             // 原 Task.overlapCount
    private ClaimStrategy strategy;           // 原 Task.strategy
    private Integer maxClaimsPerLabeler;      // 原 Task.maxClaimsPerLabeler
    private LocalDateTime deadlineAt;         // 原 Task.deadlineAt
    private Long publishedTemplateVersionId;  // 原 Task.publishedTemplateVersionId
    private Long aiReviewConfigId;            // 原 Task.aiReviewConfigId
    private Integer reviewLevelCount;         // 原 Task.reviewLevelCount
    private Boolean rewardVisible;            // 原 Task.rewardVisible
    private LocalDateTime publishedAt;        // 原 Task.publishedAt
    private LocalDateTime endedAt;            // 原 Task.endedAt
    private LocalDateTime createdAt;          // 原 Task.createdAt
    private LocalDateTime updatedAt;          // 原 Task.updatedAt
}
```

**受影响文件**:
- `DatasetItemService.java` — `TaskEntity` → `Task`，`TaskRepositoryMapper` → `TaskMapper`
- `TaskRepositoryMapper.java` — 删除
- 全部引用 `TaskEntity` 的 import 改为 `Task`

### 3.2 DatasetItem — 合并为单一模型

**删除**: `DatasetItemEntity.java`

**保留**: `DatasetItem.java`，把 `DatasetItemEntity` 的两个 transient 计算字段合并进来:

```java
// modules/dataset/domain/DatasetItem.java — 唯一模型
@Getter
@Setter
@TableName("dataset_items")
public class DatasetItem {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private String externalId;
    private String itemJson;
    private String metadataJson;
    private Integer assignedCount;
    private Integer submittedCount;
    private Integer approvedCount;
    private Boolean deleted;

    // === 以下两个是 transient 计算字段（不在表中），合并自 DatasetItemEntity ===
    @TableField(exist = false)
    private Long labelerId;                  // 当前有效领取人，列表查询时 join 计算
    @TableField(exist = false)
    private String assignmentStatus;         // 当前领取状态，列表查询时 join 计算

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

**受影响文件**:
- `DatasetItemService.java` — `DatasetItemEntity` → `DatasetItem`
- `DatasetItemRepositoryMapper.java` — 删除，方法合并到 `DatasetItemMapper`
- 全部引用 `DatasetItemEntity` 的 import 改为 `DatasetItem`

### 3.3 ReviewTask → ReviewRecord 重命名

| 旧名称 | 新名称 | 影响 |
|--------|--------|------|
| `ReviewTask.java` | `ReviewRecord.java` | domain 类 |
| `ReviewTaskStatus.java` | `ReviewRecordStatus.java` | 枚举 |
| `ReviewTaskMapper.java` | `ReviewRecordMapper.java` | Mapper |
| `review_tasks` 表 | `review_records` 表 | DB migration |

`ReviewTaskClaim` **不改名** — 它表示审核员领取 (Task, Level)，"Task" 语义正确。

### 3.4 Mapper 合并

| 删除 | 方法迁移到 | 
|------|-----------|
| `TaskRepositoryMapper.java` | `TaskMapper.java` |
| `DatasetItemRepositoryMapper.java` | `DatasetItemMapper.java` |

合并后 Mapper 结构:

```
modules/task/mapper/
  TaskMapper.java          ← 所有 tasks 表查询
  TaskTagMapper.java       ← 不变

modules/dataset/mapper/
  DatasetItemMapper.java   ← 所有 dataset_items 表查询
  DatasetItemChangeLogMapper.java ← 不变
```

**删除**: `TaskReviewerMapper.java`（随 `task_reviewers` 表废弃）

---

## 4. DTO 层重构

### 4.1 设计原则

1. **扁平优先**: 摘要和详情级别 DTO 保持字段扁平，接受少量字段重复
2. **组合复用**: 跨角色共享的字段集合独立定义，详情 DTO 的子集 = 摘要 DTO
3. **命名统一**: Task 相关 DTO 以 `Task` 为前缀（非 `OwnerTask`/`MarketTask`），Item 相关以 `Item` 为前缀

### 4.2 DTO 对照表

```
┌─────────────────────────────────────────────────────────────────────┐
│                          删除的 DTO                                  │
├──────────────────────────────────────┬──────────────────────────────┤
│ OwnerTaskSummaryResponse             │ → TaskSummaryResponse        │
│ OwnerTaskPageResponse                │ → PageResponse<T>            │
│ TaskDetailResponse                   │ → TaskResponse               │
│ TaskLifecycleResponse                │ → TaskStatusResponse（改名）   │
│ CreateTaskResponse                   │ → 保留（特殊返回）             │
│ MarketTaskResponse                   │ → TaskMarketResponse（重构）   │
│ MarketTaskQueryRequest               │ → TaskMarketQuery            │
│ DatasetItemResponse                  │ → ItemResponse（改名）         │
│ MarketDatasetItemResponse            │ → ItemSummaryResponse（改名）  │
│ LabelerClaimedItemResponse           │ → ClaimedItemResponse（改名）  │
│ LabelerClaimedTaskResponse           │ → ClaimedTaskResponse（改名）  │
│ ReviewerTaskSummary                  │ → 保留（改名后语义正确）       │
│ ReviewTaskClaimResponse              │ → 保留                        │
│ TaskLabelerResponse                  │ → 保留                        │
│ TaskStatisticsResponse               │ → 保留                        │
│ CreateTaskRequest                    │ → 保留                        │
│ UpdateTaskRequest                    │ → 保留                        │
│ BatchAppendItemsRequest              │ → BatchAppendItemsRequest     │
│ BatchUpdateItemsRequest              │ → BatchUpdateItemsRequest     │
│ BatchDeleteItemsRequest              │ → BatchDeleteItemsRequest     │
│ BatchItemResult                      │ → BatchItemResult             │
└──────────────────────────────────────┴──────────────────────────────┘
```

### 4.3 新 DTO 定义

#### 4.3.1 Task 通用 DTO

```java
// ===== 任务摘要 — 所有列表场景统一使用 =====
public record TaskSummaryResponse(
    Long taskId,
    String title,
    TaskStatus status,
    List<String> tags,
    Integer quota,
    Integer claimedCount,
    Integer overlapCount,
    ClaimStrategy strategy,
    LocalDateTime deadlineAt,
    LocalDateTime publishedAt,
    LocalDateTime endedAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}

// ===== 任务详情 — Owner 查看/编辑单个任务 =====
// 字段包含 TaskSummaryResponse 的全部字段 + 详情独有字段（方案B：扁平重复）
public record TaskResponse(
    // --- 摘要字段（和 TaskSummaryResponse 相同，接受重复）---
    Long taskId,
    String title,
    TaskStatus status,
    List<String> tags,
    Integer quota,
    Integer claimedCount,
    Integer overlapCount,
    ClaimStrategy strategy,
    LocalDateTime deadlineAt,
    LocalDateTime publishedAt,
    LocalDateTime endedAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    // --- 详情独有字段 ---
    Long ownerId,
    String description,
    String instructionRichText,
    Integer maxClaimsPerLabeler,
    Long publishedTemplateVersionId,
    Long aiReviewConfigId,
    Integer reviewLevelCount,
    Boolean rewardVisible,
    RewardRuleResponse rewardRule
) {}

// ===== 状态变更返回 — 生命周期操作统一返回 =====
public record TaskStatusResponse(
    Long taskId,
    TaskStatus status
) {}

// ===== 创建返回 — 特殊返回，含副作用结果 =====
public record CreateTaskResponse(
    Long taskId,
    TaskStatus status,
    DatasetImportJobResponse datasetImportJob,  // 可为 null
    RewardRuleResponse rewardRule               // 可为 null
) {}
```

#### 4.3.2 Market 视图 DTO（组合嵌套）

```java
// ===== 任务市场视图 — Labeler 看到的任务 =====
// 采用组合模式：内嵌 TaskSummaryResponse + 市场计算字段
public record TaskMarketResponse(
    TaskSummaryResponse task,              // 任务摘要（嵌套复用）
    Integer availableCount,                // 当前可领取题目数
    Integer currentUserClaimedCount,       // 当前用户已领取数
    RewardSummaryResponse rewardSummary,   // 奖励摘要
    List<ItemSummaryResponse> itemsPreview // 可领取题目预览（前 N 条）
) {}
```

> **为什么这里用嵌套**: Market 视图有明确的三层语义（任务本身 + 用户状态 + 题目预览），嵌套让 JSON 结构表达了这种组合关系。

#### 4.3.3 已领取任务视图 DTO（组合嵌套）

```java
// ===== 已领取任务视图 — Labeler 看自己领取过的任务 =====
public record ClaimedTaskResponse(
    TaskSummaryResponse task,              // 任务摘要（嵌套复用）
    Integer myClaimedCount,                // 我在此任务下的领取数
    Integer mySubmittedCount,              // 我在此任务下的已提交数
    Integer myApprovedCount,               // 我在此任务下的已通过数
    List<ClaimedItemResponse> items        // 当前页的已领取题目
) {}
```

#### 4.3.4 Item 相关 DTO

```java
// ===== 题目摘要 — 市场/预览场景 =====
public record ItemSummaryResponse(
    Long itemId,
    String externalId,
    JsonNode itemJson,
    JsonNode metadataJson
) {}

// ===== 题目详情 — Owner 管理场景 =====
public record ItemResponse(
    Long itemId,
    Long taskId,
    String externalId,
    JsonNode itemJson,
    JsonNode metadataJson,
    Integer assignedCount,
    Integer submittedCount,
    Integer approvedCount,
    ItemStatus itemStatus,      // 派生字段: UNCLAIMED/CLAIMED/DRAFT/SUBMITTED/RETURNED/APPROVED
    Long labelerId,             // 派生字段: 当前有效领取人
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}

// ===== 已领取题目 — Labeler 视角 =====
public record ClaimedItemResponse(
    Long claimId,                    // assignment id
    Long itemId,                     // dataset item id
    ClaimStatus claimStatus,         // assignment status
    JsonNode itemJson,
    JsonNode metadataJson,
    Integer draftVersion,
    String latestSubmissionStatus,
    LocalDateTime updatedAt
) {}
```

#### 4.3.5 通用分页包装

```java
// ===== 通用分页响应 — 可复用到所有分页场景 =====
public record PageResponse<T>(
    List<T> items,
    int page,
    int pageSize,
    long total
) {}
```

#### 4.3.6 状态枚举改名

```java
// ItemStatus — 统一"题目状态"枚举名
public enum ItemStatus {
    UNCLAIMED,   // 未领取
    CLAIMED,     // 已领取待标
    DRAFT,       // 草稿中
    SUBMITTED,   // 已提交
    RETURNED,    // 已打回
    APPROVED     // 已通过
}
// 旧名: DatasetItemStatus → 新名: ItemStatus
```

---

## 5. API 路径重构

### 5.1 路径设计原则

1. **保留角色前缀**: `/owner/`、`/labeler/`、`/reviewer/` 各司其职
2. **去 `dataset` 冗余**: `/tasks/{id}/dataset/items` → `/tasks/{id}/items`
3. **`assignment` → `claim`**: 与业务术语（ClaimStrategy）对齐
4. **领取题目命名**: Labeler `POST /tasks/{id}/items/claim`（领题目），Reviewer `POST /reviewer/tasks/{id}/claims`（领整任务审核权），语义清晰不冲突
5. **Batch 端点保留 `batch-` 前缀**: append 传 List 新增、update 传 List（按 itemId 定位修改）、delete 传 List\<itemId\>
6. **导入用 fileId**: 不传题目 list，传已上传文件的 fileId
7. **创建时嵌套，有 ID 后扁平**: 资源创建需要父资源上下文时路径嵌套；资源有独立 ID 后提供扁平访问

### 5.2 新路径全景

```
/api/v1/
├── tasks/                                    # ★ 任务生命周期（Owner）
│   ├── POST                          创建任务草稿
│   ├── GET  /{taskId}                任务详情（Owner 视角）
│   ├── PUT  /{taskId}                编辑草稿
│   ├── DELETE /{taskId}              删除草稿
│   ├── POST /{taskId}/publish        发布
│   ├── POST /{taskId}/pause          暂停
│   ├── POST /{taskId}/resume         恢复
│   ├── POST /{taskId}/end            结束
│   ├── GET  /{taskId}/statistics     任务统计
│   ├── GET  /{taskId}/labelers       标注员列表
│   │
│   ├── GET  /{taskId}/items                      题目列表 ★ 改名
│   ├── POST /{taskId}/items/batch-append         批量追加 ★ 改名
│   ├── POST /{taskId}/items/batch-update         批量更新 ★ 改名
│   ├── POST /{taskId}/items/batch-delete         批量删除 ★ 改名
│   ├── POST /{taskId}/items/claim                领取一个题目 ★ 新增（原 /tasks/{id}/assignments/claim）
│   │
│   ├── POST /{taskId}/imports         创建追加导入 ★ 改名
│   ├── POST /{taskId}/imports/overwrite 创建覆盖导入 ★ 改名
│   ├── GET  /{taskId}/imports/{jobId}   查询导入任务 ★ 改名
│   │
│   ├── POST /{taskId}/exports           创建导出任务
│   ├── GET  /{taskId}/exports           导出历史列表
│   └── GET  /{taskId}/exports/{jobId}   导出任务详情
│
├── claims/                                   # ★ 已领取题目操作（扁平路径）
│   ├── GET                             标注员自己的领取列表（原 /labeler/claimed-tasks）
│   ├── GET  /{claimId}                 领取详情（原 /assignments/{id}）
│   ├── PUT  /{claimId}/draft           保存草稿（原 /assignments/{id}/draft）
│   └── POST /{claimId}/submit          提交答案（原 /assignments/{id}/submit）
│
├── owner/tasks/                              # ★ Owner 任务列表（保留前缀）
│   └── GET                             Owner 分页任务列表（原 /owner/tasks）
│
├── market/tasks/                             # ★ Labeler 任务市场（保留前缀）
│   ├── GET                             可领取任务市场列表
│   └── GET  /{taskId}                  市场任务详情 + 可领题目预览
│
├── labeler/submissions/                      # ★ 标注员提交历史（保留前缀）
│   ├── GET                             我的提交列表
│   └── GET  /{submissionId}            提交详情
│
├── reviewer/                                 # ★ 审核员（保留前缀）
│   ├── GET  /tasks                      审核员工作台任务导航
│   ├── GET  /dashboard                  工作统计概览
│   ├── GET  /ai-review-status           AI 预审状态列表
│   ├── POST /tasks/{taskId}/claims     领取整任务审核权
│   ├── DELETE /tasks/{taskId}/claims   释放整任务
│   │
│   ├── GET  /submissions              待审列表（scope=AVAILABLE 广场 / CLAIMED 我的 / 不传 全部）
│   ├── GET  /submissions/{id}          审核详情（答案+AI评分+审核历史+prompt）
│   ├── POST /submissions/{id}/approve  通过
│   ├── POST /submissions/{id}/reject   驳回
│   ├── POST /submissions/batch/approve   批量通过
│   ├── POST /submissions/batch/reject    批量驳回
│   └── POST /submissions/batch/mark-manual 批量转人工
│
├── submissions/                              # ★ AI 重试（共享路径）
│   └── POST /{submissionId}/ai-review/retry  AI 审核失败重试
│
└── review/                                   # ★ 冲突仲裁（保留）
    ├── GET  /conflict-groups
    ├── GET  /conflict-groups/{id}
    └── POST /conflict-groups/{id}/resolve
```

### 5.3 路径对照表

| 旧路径 | 新路径 | Controller |
|--------|--------|------------|
| `GET /owner/tasks` | `GET /owner/tasks` | OwnerTaskController |
| `POST /tasks` | `POST /tasks` | TaskController |
| `GET /tasks/{id}` | `GET /tasks/{id}` | TaskController |
| `PUT /tasks/{id}` | `PUT /tasks/{id}` | TaskController |
| `DELETE /tasks/{id}` | `DELETE /tasks/{id}` | TaskController |
| `POST /tasks/{id}/publish` | 不变 | TaskController |
| `POST /tasks/{id}/pause` | 不变 | TaskController |
| `POST /tasks/{id}/resume` | 不变 | TaskController |
| `POST /tasks/{id}/end` | 不变 | TaskController |
| `GET /tasks/{id}/statistics` | 不变 | TaskController |
| `GET /tasks/{id}/labelers` | 不变 | TaskController |
| `POST /tasks/{id}/reviewers` | ❌ 删除（与自领取模型冲突） | — |
| `GET /tasks/{id}/reviewers` | ❌ 删除（与自领取模型冲突） | — |
| `GET /tasks/{id}/dataset/items` | `GET /tasks/{id}/items` | DatasetItemController |
| `POST /tasks/{id}/dataset/items/batch-append` | `POST /tasks/{id}/items/batch-append` | DatasetItemController |
| `POST /tasks/{id}/dataset/items/batch-update` | `POST /tasks/{id}/items/batch-update` | DatasetItemController |
| `POST /tasks/{id}/dataset/items/batch-delete` | `POST /tasks/{id}/items/batch-delete` | DatasetItemController |
| `POST /tasks/{id}/dataset/import` | `POST /tasks/{id}/imports` | DatasetImportController |
| `POST /tasks/{id}/dataset/import/overwrite` | `POST /tasks/{id}/imports/overwrite` | DatasetImportController |
| `GET /tasks/{id}/dataset/import-jobs/{jobId}` | `GET /tasks/{id}/imports/{jobId}` | DatasetImportController |
| `POST /tasks/{id}/assignments/claim` | `POST /tasks/{id}/items/claim` | ClaimController |
| `GET /assignments/{id}` | `GET /claims/{id}` | ClaimController |
| `PUT /assignments/{id}/draft` | `PUT /claims/{id}/draft` | ClaimController |
| `POST /assignments/{id}/submit` | `POST /claims/{id}/submit` | ClaimController |
| `GET /labeler/claimed-tasks` | `GET /claims` | ClaimController |
| `GET /labeler/claimed-tasks/{taskId}` | `GET /claims?taskId={taskId}` | ClaimController |
| `GET /market/tasks` | 不变 | MarketTaskController |
| `GET /market/tasks/{id}` | 不变 | MarketTaskController |
| `GET /labeler/submissions` | 不变 | LabelerSubmissionController |
| `GET /labeler/submissions/{id}` | 不变 | LabelerSubmissionController |
| `GET /reviewer/tasks` | 不变 | ReviewerWorkspaceController |
| `GET /reviewer/dashboard` | 不变 | ReviewerWorkspaceController |
| `GET /reviewer/ai-review-status` | 不变 | ReviewerWorkspaceController |
| `POST /reviewer/tasks/{id}/claim` | 不变 | ReviewTaskClaimController |
| `DELETE /reviewer/tasks/{id}/claim` | 不变 | ReviewTaskClaimController |
| `GET /reviewer/submissions` | 不变（含 scope 参数） | ReviewController |
| `GET /reviewer/submissions/{id}` | 不变 | ReviewController |
| `POST /reviewer/submissions/{id}/approve` | 不变 | ReviewController |
| `POST /reviewer/submissions/{id}/reject` | 不变 | ReviewController |
| `POST /reviewer/submissions/batch/approve` | 不变 | ReviewController |
| `POST /reviewer/submissions/batch/reject` | 不变 | ReviewController |
| `POST /reviewer/submissions/batch/mark-manual` | 不变 | ReviewController |
| `POST /submissions/{id}/ai-review` | 不变（只保留 /retry） | AiReviewController |
| `POST /tasks/{id}/exports` | 不变 | ExportController |
| `GET /tasks/{id}/exports` | 不变 | ExportController |
| `GET /tasks/{id}/exports/{jobId}` | 不变 | ExportController |

### 5.4 Controller 合并

| 旧 Controller | 合并到 | 说明 |
|---------------|--------|------|
| `OwnerTaskController` (TaskController 内部类) | `OwnerTaskController` (独立类) | 独立出来 |
| `AssignmentController` | `ClaimController` | 合并 |
| `AssignmentDetailController` | `ClaimController` | 合并 |
| `AssignmentDraftController` | `ClaimController` | 合并 |
| `AssignmentSubmitController` | `ClaimController` | 合并 |
| `LabelerClaimedTaskController` | `ClaimController` | 合并（列表查询用 `GET /claims`） |
| `DatasetItemController` | 保留 | 去 `/dataset/` 前缀 |
| `DatasetImportController` | 保留 | 去 `/dataset/` 前缀 |
| `MarketTaskController` | 保留 | 路径不变 |
| `LabelerSubmissionController` | 保留 | 路径不变 |
| `ReviewerWorkspaceController` | 保留 | 路径不变 |
| `ReviewTaskClaimController` | 保留 | 路径不变 |
| `ReviewController` | 保留 | 路径不变 |
| `AiReviewController` | 保留 | 路径不变 |
| `ExportController` | 保留 | 路径不变 |

最终 Controller 数量: **14 → 10 个**（与 Task/Item 直接相关的）

---

## 6. 各角色接口全景

### 6.1 Owner 角色 — 任务管理

```
POST   /api/v1/tasks                         创建任务草稿
GET    /api/v1/owner/tasks                   我的任务列表（分页）
GET    /api/v1/tasks/{taskId}                任务详情
PUT    /api/v1/tasks/{taskId}                编辑草稿
DELETE /api/v1/tasks/{taskId}                删除草稿
POST   /api/v1/tasks/{taskId}/publish        发布任务
POST   /api/v1/tasks/{taskId}/pause          暂停任务
POST   /api/v1/tasks/{taskId}/resume         恢复任务
POST   /api/v1/tasks/{taskId}/end            结束任务
GET    /api/v1/tasks/{taskId}/statistics     任务统计（领取/提交/通过/驳回数）
GET    /api/v1/tasks/{taskId}/labelers       标注员列表及进度
```

### 6.2 Owner 角色 — 题目管理 & 导出

```
GET    /api/v1/tasks/{taskId}/items                      题目列表（分页）
POST   /api/v1/tasks/{taskId}/items/batch-append         批量追加题目（传 List）
POST   /api/v1/tasks/{taskId}/items/batch-update         批量更新题目（传 List，按 itemId 定位修改）
POST   /api/v1/tasks/{taskId}/items/batch-delete         批量删除题目（传 List<itemId>）
POST   /api/v1/tasks/{taskId}/imports                    创建追加导入（用 fileId）
POST   /api/v1/tasks/{taskId}/imports/overwrite          创建覆盖导入（用 fileId）
GET    /api/v1/tasks/{taskId}/imports/{jobId}            查询导入任务
POST   /api/v1/tasks/{taskId}/exports                    创建导出任务（导出金标数据）
GET    /api/v1/tasks/{taskId}/exports                    导出历史列表
GET    /api/v1/tasks/{taskId}/exports/{jobId}            导出任务详情/下载
```

### 6.3 Owner — 返回的 DTO

| 接口 | 请求 | 响应 |
|------|------|------|
| 创建任务 | `CreateTaskRequest` | `CreateTaskResponse` |
| 任务列表 | - | `PageResponse<TaskSummaryResponse>` |
| 任务详情 | - | `TaskResponse` |
| 编辑任务 | `UpdateTaskRequest` | `TaskStatusResponse` |
| 删除任务 | - | `void` |
| 发布/暂停/恢复/结束 | - | `TaskStatusResponse` |
| 任务统计 | - | `TaskStatisticsResponse` |
| 标注员列表 | - | `List<TaskLabelerResponse>` |
| 题目列表 | - | `PageResponse<ItemResponse>` |
| 批量追加 | `BatchAppendItemsRequest` | `List<BatchItemResult>` |
| 批量更新 | `BatchUpdateItemsRequest` | `List<BatchItemResult>` |
| 批量删除 | `BatchDeleteItemsRequest` | `List<BatchItemResult>` |
| 创建导入 | `DatasetImportRequest` | `DatasetImportJobResponse` |
| 查询导入 | - | `DatasetImportJobResponse` |

### 6.4 Labeler 角色 — 领取与提交

```
GET    /api/v1/market/tasks                        可领取任务市场
GET    /api/v1/market/tasks/{taskId}               市场任务详情 + 可领题目预览
POST   /api/v1/tasks/{taskId}/items/claim          领取一个题目
GET    /api/v1/claims                              我的领取列表
GET    /api/v1/claims/{claimId}                    领取详情（题目 + 模板 + 草稿状态）
PUT    /api/v1/claims/{claimId}/draft              保存草稿
POST   /api/v1/claims/{claimId}/submit             提交答案
GET    /api/v1/labeler/submissions                 我的提交历史
GET    /api/v1/labeler/submissions/{id}            提交详情（含 AI 审核结果）
```

### 6.5 Labeler — 返回的 DTO

| 接口 | 请求 | 响应 |
|------|------|------|
| 任务市场列表 | - | `List<TaskMarketResponse>` |
| 市场任务详情 | - | `TaskMarketResponse` |
| 领取题目 | - | `ClaimedItemResponse` |
| 我的领取列表 | - | `PageResponse<ClaimedItemResponse>` |
| 领取详情 | - | `ClaimedItemResponse`（含更多详情） |
| 保存草稿 | `DraftSaveRequest` | `void` |
| 提交答案 | `SubmitRequest` | `SubmitResponse` |
| 提交列表 | - | `PageResponse<SubmissionListItem>` |
| 提交详情 | - | `SubmissionDetailResponse` |

### 6.6 Reviewer 角色 — 审核

```
# 工作台
GET    /api/v1/reviewer/tasks                      待审任务导航列表
GET    /api/v1/reviewer/dashboard                  工作统计概览
GET    /api/v1/reviewer/ai-review-status           AI 预审状态总览

# 领取
POST   /api/v1/reviewer/tasks/{taskId}/claims     领取整任务审核权
DELETE /api/v1/reviewer/tasks/{taskId}/claims     释放整任务

# 审核广场 & 我的待审（scope 参数区分）
GET    /api/v1/reviewer/submissions?scope=AVAILABLE   可领取的待审（广场）
GET    /api/v1/reviewer/submissions?scope=CLAIMED     我已领的待审
GET    /api/v1/reviewer/submissions                   全部待审
GET    /api/v1/reviewer/submissions/{id}              审核详情（答案+AI评分+审核历史+prompt模板）

# 审核操作
POST   /api/v1/reviewer/submissions/{id}/approve        通过
POST   /api/v1/reviewer/submissions/{id}/reject         驳回
POST   /api/v1/reviewer/submissions/batch/approve       批量通过
POST   /api/v1/reviewer/submissions/batch/reject        批量驳回
POST   /api/v1/reviewer/submissions/batch/mark-manual   批量转人工

# AI 重试
POST   /api/v1/submissions/{submissionId}/ai-review/retry   AI 审核失败重试
```

### 6.7 Reviewer — 返回的 DTO

| 接口 | 请求 | 响应 |
|------|------|------|
| 任务导航 | - | `List<ReviewerTaskSummary>` |
| 工作统计 | - | `ReviewerDashboardResponse` |
| AI预审状态 | - | `List<ReviewerAiReviewStatusItem>` |
| 领取整任务 | - | `ReviewTaskClaimResponse` |
| 释放 | - | `void` |
| 待审列表 | - | `PageResponse<ReviewerSubmissionListItem>` |
| 审核详情 | - | `ReviewerSubmissionDetailResponse` |
| 通过 | `ApproveRequest` | `ReviewActionResponse` |
| 驳回 | `RejectRequest` | `ReviewActionResponse` |
| 批量通过 | `BatchApproveRequest` | `BatchReviewResponse` |
| 批量驳回 | `BatchRejectRequest` | `BatchReviewResponse` |
| 批量转人工 | `BatchMarkManualRequest` | `BatchReviewResponse` |
| AI重试 | - | `AiReviewResultResponse` |

### 6.8 Task 生命周期状态流转

```
                    Owner: POST /tasks/{id}/publish
    DRAFT ────────────────────────────────────→ PUBLISHED
                                                    │
                    Owner: POST /tasks/{id}/pause    │
                                              ←─────┘
                                                    ↓
                                                  PAUSED
                                                    │
                    Owner: POST /tasks/{id}/resume   │
                                              ←─────┘
                                                    ↓
                    Owner: POST /tasks/{id}/end    ENDED
    PUBLISHED ────────────────────────────────→  (终态)
    PAUSED ──────────────────────────────────→  (终态)
```

### 6.9 Item 生命周期状态流转

```
                           Labeler: POST /tasks/{id}/items/claim
    UNCLAIMED ────────────────────────────────────────→ CLAIMED
                                                            │
                           Labeler: PUT /claims/{id}/draft   │
                              ←──────────────────────────────┘
                                                            ↓
                                                          DRAFT
                                                            │
                           Labeler: POST /claims/{id}/submit │
                              ←──────────────────────────────┘
                                                            ↓
                                                        SUBMITTED
                                                            │
                           Reviewer: reject                  │
                              ←──────────────────────────────┘
                           ↓                              │
                        RETURNED                           │
                           │                              │
                           │   Reviewer: approve           │
                           │      ←────────────────────────┘
                           │                              ↓
                           └──────────────────────→   APPROVED
                              (Labeler 重新提交)         (终态)
```

---

## 7. 实施阶段

### Phase 1: Model & Mapper 层（P0，基础依赖）

| 步骤 | 操作 | 文件 |
|------|------|------|
| 1.1 | 合并 `Task` + `TaskEntity` → `Task` | `Task.java` |
| 1.2 | 更新所有引用 `TaskEntity` 的代码 | `DatasetItemService.java` 等 |
| 1.3 | 删除 `TaskEntity.java`、`TaskRepositoryMapper.java` | 2 个文件 |
| 1.4 | 合并 `DatasetItem` + `DatasetItemEntity` → `DatasetItem` | `DatasetItem.java` |
| 1.5 | 更新所有引用 `DatasetItemEntity` 的代码 | 关联文件 |
| 1.6 | 删除 `DatasetItemEntity.java`、`DatasetItemRepositoryMapper.java` | 2 个文件 |
| 1.7 | 合并 Mapper 方法到 `DatasetItemMapper` | `DatasetItemMapper.java` |

### Phase 2: DTO 层（P1）

| 步骤 | 操作 |
|------|------|
| 2.1 | 新建 `TaskSummaryResponse`、`TaskResponse`、`TaskStatusResponse` |
| 2.2 | 新建 `TaskMarketResponse`（嵌套组合） |
| 2.3 | 新建 `ClaimedTaskResponse`（嵌套组合） |
| 2.4 | 新建 `ItemSummaryResponse`、`ItemResponse`、`ClaimedItemResponse` |
| 2.5 | 新建 `PageResponse<T>` |
| 2.6 | 重命名 `DatasetItemStatus` → `ItemStatus` |
| 2.7 | 更新所有 Service 和 Controller 的返回类型 |
| 2.8 | 删除旧 DTO 类 |

### Phase 3: ReviewTask 重命名（P2，独立模块）

| 步骤 | 操作 |
|------|------|
| 3.1 | `ReviewTask.java` → `ReviewRecord.java` |
| 3.2 | `ReviewTaskStatus.java` → `ReviewRecordStatus.java` |
| 3.3 | `ReviewTaskMapper.java` → `ReviewRecordMapper.java` |
| 3.4 | 新增 DB migration: `review_tasks` → `review_records` 表重命名 |

### Phase 4: API 路径 & Controller 重构（P1）

| 步骤 | 操作 |
|------|------|
| 4.1 | 新建 `ClaimController`，处理 `POST /tasks/{id}/items/claim` + 扁平 `GET/PUT/POST /claims/**`，合并原有 5 个 Controller |
| 4.2 | 修改 `DatasetItemController` 路径: `/dataset/items` → `/items`，保留 `batch-append/batch-update/batch-delete` |
| 4.3 | 修改 `DatasetImportController` 路径: `/dataset/import*` → `/imports*` |
| 4.4 | 删除 `POST /tasks/{id}/reviewers`、`GET /tasks/{id}/reviewers`，废弃 `TaskReviewerService` |
| 4.5 | 删除 `AssignmentController`、`AssignmentDetailController`、`AssignmentDraftController`、`AssignmentSubmitController`、`LabelerClaimedTaskController` |
| 4.6 | `OwnerTaskController` 从 TaskController 内部类提取为独立类 |
| 4.7 | 更新 Swagger `@Tag` 注解 |

### Phase 5: 测试 & 验证

| 步骤 | 操作 |
|------|------|
| 5.1 | 更新单元测试引用 |
| 5.2 | 运行全量测试 |
| 5.3 | 更新 API 契约文档 |

---

## 附录 A: 文件变更清单

### 删除 (12 个文件)

```
backend/src/main/java/com/labelhub/modules/task/domain/TaskEntity.java
backend/src/main/java/com/labelhub/modules/task/domain/TaskReviewer.java
backend/src/main/java/com/labelhub/modules/task/repository/TaskRepositoryMapper.java
backend/src/main/java/com/labelhub/modules/task/mapper/TaskReviewerMapper.java
backend/src/main/java/com/labelhub/modules/task/service/TaskReviewerService.java
backend/src/main/java/com/labelhub/modules/task/dto/TaskReviewerResponse.java
backend/src/main/java/com/labelhub/modules/task/dto/AssignTaskReviewersRequest.java
backend/src/main/java/com/labelhub/modules/dataset/domain/DatasetItemEntity.java
backend/src/main/java/com/labelhub/modules/dataset/repository/DatasetItemRepositoryMapper.java
backend/src/main/java/com/labelhub/modules/task/dto/OwnerTaskSummaryResponse.java
backend/src/main/java/com/labelhub/modules/task/dto/OwnerTaskPageResponse.java
backend/src/main/java/com/labelhub/modules/task/dto/TaskDetailResponse.java
backend/src/main/java/com/labelhub/modules/task/dto/TaskLifecycleResponse.java
```

### 新增 (12+ 个文件)

```
backend/src/main/java/com/labelhub/common/api/PageResponse.java
backend/src/main/java/com/labelhub/modules/task/dto/TaskSummaryResponse.java
backend/src/main/java/com/labelhub/modules/task/dto/TaskResponse.java
backend/src/main/java/com/labelhub/modules/task/dto/TaskStatusResponse.java
backend/src/main/java/com/labelhub/modules/assignment/dto/TaskMarketResponse.java     (重命名+重构)
backend/src/main/java/com/labelhub/modules/assignment/dto/ClaimedTaskResponse.java    (重命名+重构)
backend/src/main/java/com/labelhub/modules/dataset/dto/ItemSummaryResponse.java       (重命名)
backend/src/main/java/com/labelhub/modules/dataset/dto/ItemResponse.java              (重命名)
backend/src/main/java/com/labelhub/modules/assignment/dto/ClaimedItemResponse.java    (重命名)
backend/src/main/java/com/labelhub/modules/assignment/web/ClaimController.java        (合并 5 个 Controller)
backend/src/main/java/com/labelhub/modules/review/domain/ReviewRecord.java            (重命名)
backend/src/main/java/com/labelhub/modules/review/domain/ReviewRecordStatus.java      (重命名)
```

### 重命名 (8+ 个文件)

```
ReviewTask.java           → ReviewRecord.java
ReviewTaskStatus.java     → ReviewRecordStatus.java
ReviewTaskMapper.java     → ReviewRecordMapper.java
DatasetItemStatus.java    → ItemStatus.java
MarketTaskResponse.java   → TaskMarketResponse.java
MarketDatasetItemResponse.java → ItemSummaryResponse.java
DatasetItemResponse.java  → ItemResponse.java
LabelerClaimedItemResponse.java → ClaimedItemResponse.java
LabelerClaimedTaskResponse.java → ClaimedTaskResponse.java
```

### 新增 DB migration

```
V36__rename_review_tasks_to_review_records.sql
```

---

## 附录 B: 角色-接口速查表

| 角色 | 可访问的路径前缀 | 能力 |
|------|-----------------|------|
| **OWNER** | `/api/v1/tasks`, `/api/v1/owner/tasks` | 创建/编辑/发布/暂停/恢复/结束任务；管理题目和导入；查看统计和标注员 |
| **LABELER** | `/api/v1/market/tasks`, `/api/v1/tasks/{id}/items/claim`, `/api/v1/claims`, `/api/v1/labeler/submissions` | 浏览任务市场；领取题目；保存草稿；提交答案；查看提交历史 |
| **REVIEWER** | `/api/v1/reviewer/tasks`, `/api/v1/reviewer/submissions`, `/api/v1/review/conflict-groups` | 领取整任务审核权；审核提交；处理冲突 |
| **ADMIN** | 所有路径 | 全局管理 |
