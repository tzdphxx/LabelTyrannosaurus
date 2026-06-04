# 审核员修订 & 多版本对比 — 设计文档

**日期**: 2026-06-04
**分支**: tzdphxx
**状态**: 已确认

---

## 需求概述

1. **审核员直接修订**：审核员审核时可以修改标注内容，修改后直接生效（创建新版本并审批通过）
2. **部分通过/打回**：保持现状（按 submission 粒度，BatchReviewService 已支持）
3. **提交记录 & 多版本 diff**：标注员入口查看版本历史，支持多版本并排字段级对比

---

## 一、审核员直接修订

### 1.1 流程

```
审核员提交 ApproveRequest（含可选 revisedAnswerJson）
    │
    ├─ 无 revisedAnswerJson → 走原有审批逻辑
    │
    └─ 有 revisedAnswerJson
        │
        ├─ 校验 JSON 格式，规范化
        ├─ 计算 SHA-256 哈希
        ├─ 若哈希与当前提交相同 → 跳过创建新版本，继续原有审批
        │
        └─ 若哈希不同：
            ├─ supersede 当前 assignment 的所有活跃版本
            ├─ 创建新 Submission（version_no + 1）
            │   - status = PENDING_FINAL（为后续审批流一致）
            │   - labelerId = 原标注员
            │   - createdBy = 审核员 ID
            │   - assignedReviewerId = 当前审核员
            │   - currentReviewLevel = 当前审核级别
            ├─ 以新版本为对象继续审批流
            │
        └─ 创建 ReviewRecord（action=APPROVE）
        └─ 多级审核判定 / 最终审批
```

### 1.2 API 改动

**`POST /api/v1/reviewer/submissions/{id}/approve`** — 请求体增加可选字段：

```java
public record ApproveRequest(
    String reviewComment,
    @Min(1) int reviewLevel,
    String revisedAnswerJson  // 新增，可选
) {}
```

### 1.3 数据库改动

**V35 迁移** — `submissions` 表新增列：

```sql
ALTER TABLE submissions ADD COLUMN created_by BIGINT NULL;
-- NULL = 标注员创建（向后兼容）
-- 非 NULL = 审核员/其他用户修订时创建
```

### 1.4 实体改动

`Submission.java` 新增字段：
```java
private Long createdBy;
```

### 1.5 Service 改动

`ReviewService.approve()` 方法 — 在 `requireNotReviewedAtOtherLevel` 之后、`createReviewRecord` 之前插入修订逻辑：

- 调用新私有方法 `maybeApplyRevision(submission, request.revisedAnswerJson(), reviewerId)`
- 如果修订了，返回新的 submission 对象；否则返回原 submission

### 1.6 影响范围

| 层级 | 文件 | 改动 |
|------|------|------|
| DTO | `ApproveRequest.java` | 加 `revisedAnswerJson` |
| Service | `ReviewService.java` | `approve()` 加修订分支 |
| Domain | `Submission.java` | 加 `createdBy` 字段 |
| DB | `V35__submission_created_by.sql` | 加列 |
| Mapper | `SubmissionMapper.java` | MyBatis-Plus 自动映射，无需改 |

---

## 二、多版本对比

### 2.1 现状

| 功能 | 端点 | 权限 |
|------|------|------|
| 版本列表 | `GET /api/v1/submissions/{id}/versions` | OWNER, REVIEWER, LABELER ✅ |
| 两两 diff | `GET /api/v1/submissions/{id}/diff?baseVersionNo=N` | OWNER, REVIEWER ✅ |
| 标注员详情含版本历史 | `GET /api/v1/labeler/submissions/{id}` | LABELER ✅ |

### 2.2 新增

**`GET /api/v1/submissions/compare?ids=101,102,103`**

- 权限：OWNER, REVIEWER, LABELER
- 校验：所有 id 属于同一 assignment
- 返回：多版本字段级并排对比

### 2.3 响应结构

```java
public record MultiVersionCompareResponse(
    List<VersionInfo> versions,
    List<FieldComparison> fields
) {}

public record VersionInfo(
    Long submissionId,
    Integer versionNo,
    LocalDateTime submittedAt,
    Long createdBy,        // NULL = 标注员
    String creatorName     // 用户名
) {}

public record FieldComparison(
    String fieldPath,
    Map<Integer, Object> valuesByVersion,  // versionNo → 值
    boolean hasDifference
) {}
```

### 2.4 JSON 示例

```json
{
  "versions": [
    {"submissionId": 101, "versionNo": 1, "submittedAt": "...", "createdBy": null, "creatorName": "标注员张三"},
    {"submissionId": 102, "versionNo": 2, "submittedAt": "...", "createdBy": 5, "creatorName": "审核员李四"}
  ],
  "fields": [
    {"fieldPath": "title", "values": {"1": "原始标题", "2": "修订标题"}, "hasDifference": true},
    {"fieldPath": "category", "values": {"1": "A", "2": "A"}, "hasDifference": false}
  ]
}
```

### 2.4 Service 改动

`AnswerDiffService` 新增方法：
```java
public MultiVersionCompareResponse multiCompare(List<Long> submissionIds)
```

逻辑：
1. 加载所有 Submission，校验同一 assignment
2. 对每个版本的 answerJson 解析为 `Map<String, Object>`
3. 收集所有字段路径（递归展平）
4. 构建字段对比表：每个字段在每个版本的值
5. 标记 `hasDifference`

### 2.5 影响范围

| 层级 | 文件 | 改动 |
|------|------|------|
| DTO | 新增 `MultiVersionCompareResponse.java` | 响应结构 |
| Service | `AnswerDiffService.java` | 新增 `multiCompare()` |
| Controller | `SubmissionTraceController.java` | 新增 `GET /compare` |

---

## 三、版本历史 DTO 增强

为让版本历史和 diff 结果能展示"谁创建的"，增强现有 DTO：

### 3.1 `VersionHistoryItem` 增加字段

```java
Long createdBy,      // 新增
String creatorName   // 新增
```

### 3.2 `VersionSummary`（LabelerSubmissionDetailResponse 内部 record）增加字段

```java
Long createdBy,      // 新增
String creatorName   // 新增
```

### 3.3 `SubmissionVersionService.toHistoryItem()` 

需要 JOIN 查询用户表获取 `creatorName`，或者通过 `UserMapper` 单独查询。

---

## 四、不需要改动的

- 审批流多级审核（`ReviewLevelEscalationService`）：无影响
- CAS 乐观锁（`casUpdateStatus`）：无影响，新版本会走同样的 CAS 路径
- 批量审核（`BatchReviewService`）：不涉及修订，保持原样
- 打回逻辑（`reject`）：不涉及修订
- 现有的 pairwise diff 和版本列表接口：仅增强返回字段，不改签名

---

## 五、测试要点

1. **审核员修订-基本流程**：审核员 approve 带 `revisedAnswerJson` → 新版本创建 → 审批通过
2. **审核员修订-内容未变**：`revisedAnswerJson` 与原 answer 相同 → 跳过创建新版本，正常通过
3. **审核员修订-多级审核**：level 1 修订通过 → 正确升级到 level 2，且 level 2 看到的是修订后的内容
4. **审核员修订-无效 JSON**：传非法 JSON → 返回错误
5. **审核员修订-非指定审核员**：被拒绝
6. **多版本对比-两版本**：正常返回字段对比表
7. **多版本对比-三版本以上**：正确处理
8. **多版本对比-跨 assignment**：返回错误
9. **多版本对比-含审核员修订版本**：`createdBy` 正确显示审核员信息
10. **向后兼容**：不带 `revisedAnswerJson` 的 approve 行为不变
