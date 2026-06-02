# 6.1 人工终审 (Manual Final Review) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现人工终审模块，让 Reviewer 可以通过 REST API 查询待审列表、对 PENDING_FINAL 提交执行通过/打回操作，并写入审核记录、推进状态机、发布事件。

**Architecture:** ReviewService 封装 approve/reject 核心逻辑，通过 SubmissionEventPublisher 端口（no-op 默认实现）与 BE-B 解耦；ReviewSubmissionMapper 负责带 LEFT JOIN ai_review_results 的复杂待审查询；Controller 暴露 `/api/v1/reviewer/submissions` 三个端点。

**Tech Stack:** Spring Boot 3, MyBatis Plus, Lombok, JUnit 5 + Mockito, AssertJ

---

## 依赖说明

- `ai_review_results` 表由 5.3（AI自动预审）负责创建；本计划在 SELECT 时使用 LEFT JOIN，字段 `ai_decision` 可为 null，不影响终审功能。
- `AuditAppender` 已实现，直接注入。
- `AssignmentStatus.RETURNED` 和 `AssignmentStatus.APPROVED` 枚举值已存在。
- `SubmissionStatus.PENDING_FINAL`、`APPROVED`、`REJECTED` 枚举值已存在。
- `ReviewAction` 枚举（APPROVE/REJECT）已存在于 `com.labelhub.modules.review.domain`。

---

## File Structure

| 操作 | 路径 | 职责 |
|------|------|------|
| Modify | `submission/domain/Submission.java` | 增加 `isGolden` 字段 |
| Create | `review/domain/ReviewRecord.java` | 审核记录实体（review_records 表） |
| Create | `review/mapper/ReviewRecordMapper.java` | ReviewRecord CRUD |
| Create | `review/mapper/ReviewSubmissionMapper.java` | 待审列表复杂查询（LEFT JOIN） |
| Create | `review/dto/SubmissionReviewItem.java` | 待审列表响应 DTO |
| Create | `review/dto/ApproveRequest.java` | 通过请求 DTO |
| Create | `review/dto/RejectRequest.java` | 打回请求 DTO |
| Create | `review/dto/ReviewActionResponse.java` | 审核操作响应 DTO |
| Create | `review/port/SubmissionEventPublisher.java` | 事件发布端口接口 |
| Create | `review/port/NoOpSubmissionEventPublisher.java` | 事件发布默认空实现 |
| Create | `review/service/ReviewService.java` | 终审核心服务 |
| Create | `review/web/ReviewController.java` | REST Controller |
| Create | `test/.../review/service/ReviewServiceTest.java` | ReviewService 单元测试 |
| Create | `test/.../review/web/ReviewControllerTest.java` | ReviewController 单元测试 |

所有 Java 源码的根路径：`backend/src/main/java/com/labelhub/`  
所有测试的根路径：`backend/src/test/java/com/labelhub/`

---

## Task 1: 为 Submission 实体增加 isGolden 字段

**Files:**
- Modify: `backend/src/main/java/com/labelhub/modules/submission/domain/Submission.java`

- [ ] **Step 1: 在 Submission.java 中增加 `isGolden` 字段**

在 `updatedAt` 字段之前插入：

```java
    private Boolean isGolden;
```

修改后 `Submission.java` 完整内容：

```java
package com.labelhub.modules.submission.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("submissions")
public class Submission {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long assignmentId;

    private Long taskId;

    private Long datasetItemId;

    private Long labelerId;

    private Long templateVersionId;

    private Integer versionNo;

    private String answerJson;

    private String answerHash;

    private SubmissionStatus status;

    private Boolean isGolden;

    private LocalDateTime submittedAt;

    private LocalDateTime updatedAt;
}
```

- [ ] **Step 2: 编译验证**

```bash
cd backend && mvn compile -q
```

Expected: BUILD SUCCESS，无编译错误。

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/labelhub/modules/submission/domain/Submission.java
git commit -m "feat(review): add isGolden field to Submission entity"
```

---

## Task 2: 创建 ReviewRecord 实体与 Mapper

**Files:**
- Create: `backend/src/main/java/com/labelhub/modules/review/domain/ReviewRecord.java`
- Create: `backend/src/main/java/com/labelhub/modules/review/mapper/ReviewRecordMapper.java`

- [ ] **Step 1: 创建 ReviewRecord 实体**

```java
// backend/src/main/java/com/labelhub/modules/review/domain/ReviewRecord.java
package com.labelhub.modules.review.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("review_records")
public class ReviewRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long submissionId;

    private Long reviewerId;

    private ReviewAction action;

    private Integer reviewLevel;

    private String reason;

    private String reviewComment;

    private LocalDateTime createdAt;
}
```

- [ ] **Step 2: 创建 ReviewRecordMapper**

```java
// backend/src/main/java/com/labelhub/modules/review/mapper/ReviewRecordMapper.java
package com.labelhub.modules.review.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labelhub.modules.review.domain.ReviewRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ReviewRecordMapper extends BaseMapper<ReviewRecord> {
}
```

- [ ] **Step 3: 编译验证**

```bash
cd backend && mvn compile -q
```

Expected: BUILD SUCCESS。

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/labelhub/modules/review/domain/ReviewRecord.java \
        backend/src/main/java/com/labelhub/modules/review/mapper/ReviewRecordMapper.java
git commit -m "feat(review): add ReviewRecord entity and mapper"
```

---

## Task 3: 创建 DTOs

**Files:**
- Create: `backend/src/main/java/com/labelhub/modules/review/dto/SubmissionReviewItem.java`
- Create: `backend/src/main/java/com/labelhub/modules/review/dto/ApproveRequest.java`
- Create: `backend/src/main/java/com/labelhub/modules/review/dto/RejectRequest.java`
- Create: `backend/src/main/java/com/labelhub/modules/review/dto/ReviewActionResponse.java`

- [ ] **Step 1: 创建 SubmissionReviewItem**

```java
// backend/src/main/java/com/labelhub/modules/review/dto/SubmissionReviewItem.java
package com.labelhub.modules.review.dto;

import com.labelhub.modules.ai.domain.AiDecision;
import com.labelhub.modules.submission.domain.SubmissionStatus;

public record SubmissionReviewItem(
        Long submissionId,
        Long taskId,
        Long datasetItemId,
        Long labelerId,
        SubmissionStatus submissionStatus,
        AiDecision aiDecision,
        String conflictStatus,
        int reviewLevel
) {
}
```

- [ ] **Step 2: 创建 ApproveRequest**

```java
// backend/src/main/java/com/labelhub/modules/review/dto/ApproveRequest.java
package com.labelhub.modules.review.dto;

import jakarta.validation.constraints.Min;

public record ApproveRequest(
        String reviewComment,
        @Min(1) int reviewLevel
) {
}
```

- [ ] **Step 3: 创建 RejectRequest**

```java
// backend/src/main/java/com/labelhub/modules/review/dto/RejectRequest.java
package com.labelhub.modules.review.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record RejectRequest(
        @NotBlank String reason,
        @Min(1) int reviewLevel
) {
}
```

- [ ] **Step 4: 创建 ReviewActionResponse**

```java
// backend/src/main/java/com/labelhub/modules/review/dto/ReviewActionResponse.java
package com.labelhub.modules.review.dto;

import com.labelhub.modules.submission.domain.SubmissionStatus;

public record ReviewActionResponse(
        Long submissionId,
        SubmissionStatus submissionStatus,
        Long reviewRecordId
) {
}
```

- [ ] **Step 5: 编译验证**

```bash
cd backend && mvn compile -q
```

Expected: BUILD SUCCESS。

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/labelhub/modules/review/dto/
git commit -m "feat(review): add review DTOs (SubmissionReviewItem, ApproveRequest, RejectRequest, ReviewActionResponse)"
```

---

## Task 4: 创建 SubmissionEventPublisher 端口

**Files:**
- Create: `backend/src/main/java/com/labelhub/modules/review/port/SubmissionEventPublisher.java`
- Create: `backend/src/main/java/com/labelhub/modules/review/port/NoOpSubmissionEventPublisher.java`

- [ ] **Step 1: 创建端口接口**

```java
// backend/src/main/java/com/labelhub/modules/review/port/SubmissionEventPublisher.java
package com.labelhub.modules.review.port;

public interface SubmissionEventPublisher {

    void publishApproved(Long submissionId, Long reviewerId);
}
```

- [ ] **Step 2: 创建空实现（BE-B 未接入前使用）**

```java
// backend/src/main/java/com/labelhub/modules/review/port/NoOpSubmissionEventPublisher.java
package com.labelhub.modules.review.port;

import org.springframework.stereotype.Component;

@Component
public class NoOpSubmissionEventPublisher implements SubmissionEventPublisher {

    @Override
    public void publishApproved(Long submissionId, Long reviewerId) {
        // BE-B 接入后替换为真实实现
    }
}
```

- [ ] **Step 3: 编译验证**

```bash
cd backend && mvn compile -q
```

Expected: BUILD SUCCESS。

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/labelhub/modules/review/port/
git commit -m "feat(review): add SubmissionEventPublisher port and no-op adapter"
```

---

## Task 5: 创建 ReviewSubmissionMapper（待审列表查询）

**Files:**
- Create: `backend/src/main/java/com/labelhub/modules/review/mapper/ReviewSubmissionMapper.java`

- [ ] **Step 1: 创建复杂查询 Mapper**

```java
// backend/src/main/java/com/labelhub/modules/review/mapper/ReviewSubmissionMapper.java
package com.labelhub.modules.review.mapper;

import com.labelhub.modules.review.dto.SubmissionReviewItem;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ReviewSubmissionMapper {

    @Results({
            @Result(property = "submissionId",    column = "submission_id"),
            @Result(property = "taskId",          column = "task_id"),
            @Result(property = "datasetItemId",   column = "dataset_item_id"),
            @Result(property = "labelerId",       column = "labeler_id"),
            @Result(property = "submissionStatus",column = "submission_status"),
            @Result(property = "aiDecision",      column = "ai_decision"),
            @Result(property = "conflictStatus",  column = "conflict_status"),
            @Result(property = "reviewLevel",     column = "review_level")
    })
    @Select("""
            SELECT s.id              AS submission_id,
                   s.task_id         AS task_id,
                   s.dataset_item_id AS dataset_item_id,
                   s.labeler_id      AS labeler_id,
                   s.status          AS submission_status,
                   arr.ai_decision   AS ai_decision,
                   NULL              AS conflict_status,
                   1                 AS review_level
            FROM submissions s
            LEFT JOIN ai_review_results arr ON arr.submission_id = s.id
            WHERE s.status = 'PENDING_FINAL'
            ORDER BY s.submitted_at ASC
            """)
    List<SubmissionReviewItem> selectPendingFinalItems();
}
```

> 注意：`ai_review_results` 表由 5.3（AI自动预审）创建。LEFT JOIN 保证该表不存在或无记录时 `aiDecision` 为 null，不影响查询执行。`conflict_status` 固定返回 NULL，留给 6.3 扩展。

- [ ] **Step 2: 编译验证**

```bash
cd backend && mvn compile -q
```

Expected: BUILD SUCCESS。

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/labelhub/modules/review/mapper/ReviewSubmissionMapper.java
git commit -m "feat(review): add ReviewSubmissionMapper for pending final list query"
```

---

## Task 6: 编写 ReviewServiceTest（TDD：先写测试）

**Files:**
- Create: `backend/src/test/java/com/labelhub/modules/review/service/ReviewServiceTest.java`

- [ ] **Step 1: 创建测试文件（此时测试会因类不存在而编译失败，这是预期行为）**

```java
// backend/src/test/java/com/labelhub/modules/review/service/ReviewServiceTest.java
package com.labelhub.modules.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.labelhub.common.audit.AuditAppender;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.modules.assignment.domain.Assignment;
import com.labelhub.modules.assignment.domain.AssignmentStatus;
import com.labelhub.modules.assignment.mapper.AssignmentMapper;
import com.labelhub.modules.review.domain.ReviewAction;
import com.labelhub.modules.review.domain.ReviewRecord;
import com.labelhub.modules.review.dto.ApproveRequest;
import com.labelhub.modules.review.dto.RejectRequest;
import com.labelhub.modules.review.dto.ReviewActionResponse;
import com.labelhub.modules.review.dto.SubmissionReviewItem;
import com.labelhub.modules.review.mapper.ReviewRecordMapper;
import com.labelhub.modules.review.mapper.ReviewSubmissionMapper;
import com.labelhub.modules.review.port.SubmissionEventPublisher;
import com.labelhub.modules.submission.domain.Submission;
import com.labelhub.modules.submission.domain.SubmissionStatus;
import com.labelhub.modules.submission.mapper.SubmissionMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    private static final Long REVIEWER_ID = 1L;
    private static final Long SUBMISSION_ID = 100L;
    private static final Long ASSIGNMENT_ID = 10L;

    @Mock private SubmissionMapper submissionMapper;
    @Mock private AssignmentMapper assignmentMapper;
    @Mock private ReviewRecordMapper reviewRecordMapper;
    @Mock private ReviewSubmissionMapper reviewSubmissionMapper;
    @Mock private SubmissionEventPublisher eventPublisher;
    @Mock private AuditAppender auditAppender;

    private ReviewService reviewService;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewService(
                submissionMapper, assignmentMapper, reviewRecordMapper,
                reviewSubmissionMapper, eventPublisher, auditAppender);
    }

    // --- approve ---

    @Test
    void approveSetsStatusApprovedAndGolden() {
        Submission submission = pendingFinalSubmission();
        when(submissionMapper.selectById(SUBMISSION_ID)).thenReturn(submission);
        when(assignmentMapper.selectById(ASSIGNMENT_ID)).thenReturn(submittedAssignment());

        ReviewActionResponse response = reviewService.approve(
                SUBMISSION_ID, REVIEWER_ID, new ApproveRequest("Looks good", 1));

        assertThat(response.submissionId()).isEqualTo(SUBMISSION_ID);
        assertThat(response.submissionStatus()).isEqualTo(SubmissionStatus.APPROVED);
        assertThat(submission.getStatus()).isEqualTo(SubmissionStatus.APPROVED);
        assertThat(submission.getIsGolden()).isTrue();
    }

    @Test
    void approveSetsAssignmentStatusApproved() {
        Assignment assignment = submittedAssignment();
        when(submissionMapper.selectById(SUBMISSION_ID)).thenReturn(pendingFinalSubmission());
        when(assignmentMapper.selectById(ASSIGNMENT_ID)).thenReturn(assignment);

        reviewService.approve(SUBMISSION_ID, REVIEWER_ID, new ApproveRequest("ok", 1));

        assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.APPROVED);
        verify(assignmentMapper).updateById(assignment);
    }

    @Test
    void approveCreatesReviewRecordWithLevel() {
        when(submissionMapper.selectById(SUBMISSION_ID)).thenReturn(pendingFinalSubmission());
        when(assignmentMapper.selectById(ASSIGNMENT_ID)).thenReturn(submittedAssignment());

        reviewService.approve(SUBMISSION_ID, REVIEWER_ID, new ApproveRequest("good", 1));

        ArgumentCaptor<ReviewRecord> captor = ArgumentCaptor.forClass(ReviewRecord.class);
        verify(reviewRecordMapper).insert(captor.capture());
        ReviewRecord record = captor.getValue();
        assertThat(record.getAction()).isEqualTo(ReviewAction.APPROVE);
        assertThat(record.getReviewLevel()).isEqualTo(1);
        assertThat(record.getReviewerId()).isEqualTo(REVIEWER_ID);
        assertThat(record.getSubmissionId()).isEqualTo(SUBMISSION_ID);
    }

    @Test
    void approvePublishesEvent() {
        when(submissionMapper.selectById(SUBMISSION_ID)).thenReturn(pendingFinalSubmission());
        when(assignmentMapper.selectById(ASSIGNMENT_ID)).thenReturn(submittedAssignment());

        reviewService.approve(SUBMISSION_ID, REVIEWER_ID, new ApproveRequest("ok", 1));

        verify(eventPublisher).publishApproved(SUBMISSION_ID, REVIEWER_ID);
    }

    @Test
    void approveWritesAudit() {
        when(submissionMapper.selectById(SUBMISSION_ID)).thenReturn(pendingFinalSubmission());
        when(assignmentMapper.selectById(ASSIGNMENT_ID)).thenReturn(submittedAssignment());

        reviewService.approve(SUBMISSION_ID, REVIEWER_ID, new ApproveRequest("ok", 1));

        verify(auditAppender).append(
                eq("SUBMISSION"), eq(SUBMISSION_ID),
                eq("USER"), eq(REVIEWER_ID),
                eq("SUBMISSION_APPROVED"),
                any(), any(), isNull(), any());
    }

    @Test
    void approveNonPendingFinalThrows() {
        Submission submission = pendingFinalSubmission();
        submission.setStatus(SubmissionStatus.APPROVED);
        when(submissionMapper.selectById(SUBMISSION_ID)).thenReturn(submission);

        assertThatThrownBy(() -> reviewService.approve(
                SUBMISSION_ID, REVIEWER_ID, new ApproveRequest("ok", 1)))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(400601));
        verify(eventPublisher, never()).publishApproved(any(), any());
    }

    @Test
    void approveNotFoundThrows() {
        when(submissionMapper.selectById(SUBMISSION_ID)).thenReturn(null);

        assertThatThrownBy(() -> reviewService.approve(
                SUBMISSION_ID, REVIEWER_ID, new ApproveRequest("ok", 1)))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(404601));
    }

    // --- reject ---

    @Test
    void rejectSetsStatusRejected() {
        Submission submission = pendingFinalSubmission();
        when(submissionMapper.selectById(SUBMISSION_ID)).thenReturn(submission);
        when(assignmentMapper.selectById(ASSIGNMENT_ID)).thenReturn(submittedAssignment());

        ReviewActionResponse response = reviewService.reject(
                SUBMISSION_ID, REVIEWER_ID, new RejectRequest("Missing label on item 3", 1));

        assertThat(response.submissionStatus()).isEqualTo(SubmissionStatus.REJECTED);
        assertThat(submission.getStatus()).isEqualTo(SubmissionStatus.REJECTED);
    }

    @Test
    void rejectSetsAssignmentStatusReturned() {
        Assignment assignment = submittedAssignment();
        when(submissionMapper.selectById(SUBMISSION_ID)).thenReturn(pendingFinalSubmission());
        when(assignmentMapper.selectById(ASSIGNMENT_ID)).thenReturn(assignment);

        reviewService.reject(SUBMISSION_ID, REVIEWER_ID, new RejectRequest("Bad label", 1));

        assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.RETURNED);
        verify(assignmentMapper).updateById(assignment);
    }

    @Test
    void rejectCreatesReviewRecordWithReason() {
        when(submissionMapper.selectById(SUBMISSION_ID)).thenReturn(pendingFinalSubmission());
        when(assignmentMapper.selectById(ASSIGNMENT_ID)).thenReturn(submittedAssignment());

        reviewService.reject(SUBMISSION_ID, REVIEWER_ID, new RejectRequest("Bad label", 1));

        ArgumentCaptor<ReviewRecord> captor = ArgumentCaptor.forClass(ReviewRecord.class);
        verify(reviewRecordMapper).insert(captor.capture());
        ReviewRecord record = captor.getValue();
        assertThat(record.getAction()).isEqualTo(ReviewAction.REJECT);
        assertThat(record.getReason()).isEqualTo("Bad label");
        assertThat(record.getReviewLevel()).isEqualTo(1);
    }

    @Test
    void rejectBlankReasonThrows() {
        assertThatThrownBy(() -> reviewService.reject(
                SUBMISSION_ID, REVIEWER_ID, new RejectRequest("", 1)))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(400602));
        verify(submissionMapper, never()).selectById(any());
    }

    @Test
    void rejectNonPendingFinalThrows() {
        Submission submission = pendingFinalSubmission();
        submission.setStatus(SubmissionStatus.REJECTED);
        when(submissionMapper.selectById(SUBMISSION_ID)).thenReturn(submission);

        assertThatThrownBy(() -> reviewService.reject(
                SUBMISSION_ID, REVIEWER_ID, new RejectRequest("again", 1)))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(400601));
    }

    @Test
    void rejectDoesNotPublishEvent() {
        when(submissionMapper.selectById(SUBMISSION_ID)).thenReturn(pendingFinalSubmission());
        when(assignmentMapper.selectById(ASSIGNMENT_ID)).thenReturn(submittedAssignment());

        reviewService.reject(SUBMISSION_ID, REVIEWER_ID, new RejectRequest("Bad label", 1));

        verify(eventPublisher, never()).publishApproved(any(), any());
    }

    // --- listPendingFinal ---

    @Test
    void listPendingFinalDelegatesMapper() {
        SubmissionReviewItem item = new SubmissionReviewItem(
                SUBMISSION_ID, 1L, 1L, 1L, SubmissionStatus.PENDING_FINAL, null, null, 1);
        when(reviewSubmissionMapper.selectPendingFinalItems()).thenReturn(List.of(item));

        List<SubmissionReviewItem> result = reviewService.listPendingFinal();

        assertThat(result).containsExactly(item);
    }

    // --- helpers ---

    private Submission pendingFinalSubmission() {
        Submission s = new Submission();
        s.setId(SUBMISSION_ID);
        s.setAssignmentId(ASSIGNMENT_ID);
        s.setStatus(SubmissionStatus.PENDING_FINAL);
        return s;
    }

    private Assignment submittedAssignment() {
        Assignment a = new Assignment();
        a.setId(ASSIGNMENT_ID);
        a.setStatus(AssignmentStatus.SUBMITTED);
        return a;
    }
}
```

- [ ] **Step 2: 运行测试，确认编译失败（ReviewService 不存在）**

```bash
cd backend && mvn test -pl . -Dtest=ReviewServiceTest -q 2>&1 | tail -20
```

Expected: 编译错误 `cannot find symbol: class ReviewService`，这是预期的 RED 状态。

- [ ] **Step 3: Commit 测试文件**

```bash
git add backend/src/test/java/com/labelhub/modules/review/service/ReviewServiceTest.java
git commit -m "test(review): add ReviewServiceTest (RED - ReviewService not yet implemented)"
```

---

## Task 7: 实现 ReviewService（让测试变绿）

**Files:**
- Create: `backend/src/main/java/com/labelhub/modules/review/service/ReviewService.java`

- [ ] **Step 1: 创建 ReviewService**

```java
// backend/src/main/java/com/labelhub/modules/review/service/ReviewService.java
package com.labelhub.modules.review.service;

import com.labelhub.common.audit.AuditAppender;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.modules.assignment.domain.Assignment;
import com.labelhub.modules.assignment.domain.AssignmentStatus;
import com.labelhub.modules.assignment.mapper.AssignmentMapper;
import com.labelhub.modules.review.domain.ReviewAction;
import com.labelhub.modules.review.domain.ReviewRecord;
import com.labelhub.modules.review.dto.ApproveRequest;
import com.labelhub.modules.review.dto.RejectRequest;
import com.labelhub.modules.review.dto.ReviewActionResponse;
import com.labelhub.modules.review.dto.SubmissionReviewItem;
import com.labelhub.modules.review.mapper.ReviewRecordMapper;
import com.labelhub.modules.review.mapper.ReviewSubmissionMapper;
import com.labelhub.modules.review.port.SubmissionEventPublisher;
import com.labelhub.modules.submission.domain.Submission;
import com.labelhub.modules.submission.domain.SubmissionStatus;
import com.labelhub.modules.submission.mapper.SubmissionMapper;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {

    private static final int SUBMISSION_NOT_FOUND = 404601;
    private static final int SUBMISSION_STATUS_NOT_REVIEWABLE = 400601;
    private static final int REJECT_REASON_REQUIRED = 400602;
    private static final String SUBMISSION_BIZ_TYPE = "SUBMISSION";
    private static final String USER_ACTOR_TYPE = "USER";

    private final SubmissionMapper submissionMapper;
    private final AssignmentMapper assignmentMapper;
    private final ReviewRecordMapper reviewRecordMapper;
    private final ReviewSubmissionMapper reviewSubmissionMapper;
    private final SubmissionEventPublisher eventPublisher;
    private final AuditAppender auditAppender;

    public ReviewService(SubmissionMapper submissionMapper,
                         AssignmentMapper assignmentMapper,
                         ReviewRecordMapper reviewRecordMapper,
                         ReviewSubmissionMapper reviewSubmissionMapper,
                         SubmissionEventPublisher eventPublisher,
                         AuditAppender auditAppender) {
        this.submissionMapper = submissionMapper;
        this.assignmentMapper = assignmentMapper;
        this.reviewRecordMapper = reviewRecordMapper;
        this.reviewSubmissionMapper = reviewSubmissionMapper;
        this.eventPublisher = eventPublisher;
        this.auditAppender = auditAppender;
    }

    public List<SubmissionReviewItem> listPendingFinal() {
        return reviewSubmissionMapper.selectPendingFinalItems();
    }

    @Transactional
    public ReviewActionResponse approve(Long submissionId, Long reviewerId, ApproveRequest request) {
        Submission submission = requirePendingFinal(submissionId);

        ReviewRecord record = createReviewRecord(
                submissionId, reviewerId, ReviewAction.APPROVE,
                request.reviewLevel(), null, request.reviewComment());

        submission.setStatus(SubmissionStatus.APPROVED);
        submission.setIsGolden(true);
        submissionMapper.updateById(submission);

        Assignment assignment = assignmentMapper.selectById(submission.getAssignmentId());
        assignment.setStatus(AssignmentStatus.APPROVED);
        assignment.setApprovedAt(LocalDateTime.now());
        assignmentMapper.updateById(assignment);

        eventPublisher.publishApproved(submissionId, reviewerId);

        appendAudit(submission, reviewerId, "SUBMISSION_APPROVED", record.getId());

        return new ReviewActionResponse(submissionId, SubmissionStatus.APPROVED, record.getId());
    }

    @Transactional
    public ReviewActionResponse reject(Long submissionId, Long reviewerId, RejectRequest request) {
        if (request.reason() == null || request.reason().isBlank()) {
            throw new BusinessException(REJECT_REASON_REQUIRED, "Reject reason is required");
        }
        Submission submission = requirePendingFinal(submissionId);

        ReviewRecord record = createReviewRecord(
                submissionId, reviewerId, ReviewAction.REJECT,
                request.reviewLevel(), request.reason(), null);

        submission.setStatus(SubmissionStatus.REJECTED);
        submissionMapper.updateById(submission);

        Assignment assignment = assignmentMapper.selectById(submission.getAssignmentId());
        assignment.setStatus(AssignmentStatus.RETURNED);
        assignment.setReturnedAt(LocalDateTime.now());
        assignmentMapper.updateById(assignment);

        appendAudit(submission, reviewerId, "SUBMISSION_REJECTED", record.getId());

        return new ReviewActionResponse(submissionId, SubmissionStatus.REJECTED, record.getId());
    }

    private Submission requirePendingFinal(Long submissionId) {
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            throw new BusinessException(SUBMISSION_NOT_FOUND, "Submission not found");
        }
        if (submission.getStatus() != SubmissionStatus.PENDING_FINAL) {
            throw new BusinessException(SUBMISSION_STATUS_NOT_REVIEWABLE,
                    "Submission is not in PENDING_FINAL status");
        }
        return submission;
    }

    private ReviewRecord createReviewRecord(Long submissionId, Long reviewerId,
                                            ReviewAction action, int reviewLevel,
                                            String reason, String reviewComment) {
        ReviewRecord record = new ReviewRecord();
        record.setSubmissionId(submissionId);
        record.setReviewerId(reviewerId);
        record.setAction(action);
        record.setReviewLevel(reviewLevel);
        record.setReason(reason);
        record.setReviewComment(reviewComment);
        record.setCreatedAt(LocalDateTime.now());
        reviewRecordMapper.insert(record);
        return record;
    }

    private void appendAudit(Submission submission, Long reviewerId,
                              String action, Long reviewRecordId) {
        Map<String, Object> before = new LinkedHashMap<>();
        before.put("submissionId", submission.getId());
        before.put("status", SubmissionStatus.PENDING_FINAL);

        Map<String, Object> after = new LinkedHashMap<>();
        after.put("submissionId", submission.getId());
        after.put("status", submission.getStatus());
        after.put("isGolden", submission.getIsGolden());
        after.put("reviewRecordId", reviewRecordId);

        auditAppender.append(SUBMISSION_BIZ_TYPE, submission.getId(),
                USER_ACTOR_TYPE, reviewerId,
                action, before, after, null, null);
    }
}
```

- [ ] **Step 2: 运行测试，确认全部通过**

```bash
cd backend && mvn test -pl . -Dtest=ReviewServiceTest -q
```

Expected: 全部测试通过，BUILD SUCCESS。

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/labelhub/modules/review/service/ReviewService.java
git commit -m "feat(review): implement ReviewService (approve, reject, listPendingFinal)"
```

---

## Task 8: 编写 ReviewControllerTest（TDD：先写测试）

**Files:**
- Create: `backend/src/test/java/com/labelhub/modules/review/web/ReviewControllerTest.java`

- [ ] **Step 1: 创建 Controller 测试文件**

参考 `AssignmentDraftControllerTest` 或 `AssignmentSubmitControllerTest` 的 MockMvc 写法：

```java
// backend/src/test/java/com/labelhub/modules/review/web/ReviewControllerTest.java
package com.labelhub.modules.review.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.modules.review.dto.ApproveRequest;
import com.labelhub.modules.review.dto.RejectRequest;
import com.labelhub.modules.review.dto.ReviewActionResponse;
import com.labelhub.modules.review.dto.SubmissionReviewItem;
import com.labelhub.modules.review.service.ReviewService;
import com.labelhub.modules.submission.domain.SubmissionStatus;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReviewController.class)
class ReviewControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean ReviewService reviewService;
    @MockBean CurrentUserContext currentUserContext;

    @Test
    void listPendingFinalReturnsItems() throws Exception {
        SubmissionReviewItem item = new SubmissionReviewItem(
                100L, 1L, 1L, 1L, SubmissionStatus.PENDING_FINAL, null, null, 1);
        when(reviewService.listPendingFinal()).thenReturn(List.of(item));

        mockMvc.perform(get("/api/v1/reviewer/submissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].submissionId").value(100))
                .andExpect(jsonPath("$.data[0].submissionStatus").value("PENDING_FINAL"));
    }

    @Test
    void approveReturnsApprovedStatus() throws Exception {
        when(currentUserContext.currentUserId()).thenReturn(1L);
        when(reviewService.approve(eq(100L), eq(1L), any(ApproveRequest.class)))
                .thenReturn(new ReviewActionResponse(100L, SubmissionStatus.APPROVED, 200L));

        mockMvc.perform(post("/api/v1/reviewer/submissions/100/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ApproveRequest("Looks good", 1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.submissionId").value(100))
                .andExpect(jsonPath("$.data.submissionStatus").value("APPROVED"));
    }

    @Test
    void rejectReturnsRejectedStatus() throws Exception {
        when(currentUserContext.currentUserId()).thenReturn(1L);
        when(reviewService.reject(eq(100L), eq(1L), any(RejectRequest.class)))
                .thenReturn(new ReviewActionResponse(100L, SubmissionStatus.REJECTED, 201L));

        mockMvc.perform(post("/api/v1/reviewer/submissions/100/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RejectRequest("Missing label", 1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.submissionStatus").value("REJECTED"));
    }

    @Test
    void rejectBlankReasonReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/reviewer/submissions/100/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RejectRequest("", 1))))
                .andExpect(status().isBadRequest());
    }
}
```

- [ ] **Step 2: 运行测试，确认编译失败（ReviewController 不存在）**

```bash
cd backend && mvn test -pl . -Dtest=ReviewControllerTest -q 2>&1 | tail -20
```

Expected: 编译错误 `cannot find symbol: class ReviewController`。

- [ ] **Step 3: Commit 测试文件**

```bash
git add backend/src/test/java/com/labelhub/modules/review/web/ReviewControllerTest.java
git commit -m "test(review): add ReviewControllerTest (RED - ReviewController not yet implemented)"
```

---

## Task 9: 实现 ReviewController（让测试变绿）

**Files:**
- Create: `backend/src/main/java/com/labelhub/modules/review/web/ReviewController.java`

- [ ] **Step 1: 创建 ReviewController**

```java
// backend/src/main/java/com/labelhub/modules/review/web/ReviewController.java
package com.labelhub.modules.review.web;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.modules.review.dto.ApproveRequest;
import com.labelhub.modules.review.dto.RejectRequest;
import com.labelhub.modules.review.dto.ReviewActionResponse;
import com.labelhub.modules.review.dto.SubmissionReviewItem;
import com.labelhub.modules.review.service.ReviewService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reviewer/submissions")
public class ReviewController {

    private final ReviewService reviewService;
    private final CurrentUserContext currentUserContext;

    public ReviewController(ReviewService reviewService, CurrentUserContext currentUserContext) {
        this.reviewService = reviewService;
        this.currentUserContext = currentUserContext;
    }

    @GetMapping
    public ApiResponse<List<SubmissionReviewItem>> listPendingFinal() {
        return ApiResponse.ok(reviewService.listPendingFinal());
    }

    @PostMapping("/{submissionId}/approve")
    public ApiResponse<ReviewActionResponse> approve(@PathVariable Long submissionId,
                                                      @Valid @RequestBody ApproveRequest request) {
        return ApiResponse.ok(reviewService.approve(
                submissionId, currentUserContext.currentUserId(), request));
    }

    @PostMapping("/{submissionId}/reject")
    public ApiResponse<ReviewActionResponse> reject(@PathVariable Long submissionId,
                                                     @Valid @RequestBody RejectRequest request) {
        return ApiResponse.ok(reviewService.reject(
                submissionId, currentUserContext.currentUserId(), request));
    }
}
```

- [ ] **Step 2: 运行 Controller 测试**

```bash
cd backend && mvn test -pl . -Dtest=ReviewControllerTest -q
```

Expected: 全部测试通过，BUILD SUCCESS。

- [ ] **Step 3: 运行全量测试确认没有回归**

```bash
cd backend && mvn test -q
```

Expected: BUILD SUCCESS，所有已有测试仍然通过。

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/labelhub/modules/review/web/ReviewController.java
git commit -m "feat(review): implement ReviewController (GET pending list, POST approve, POST reject)"
```

---

## 验收核查

| 验收条件 | 对应 Task | 验证方式 |
|---------|-----------|---------|
| PENDING_FINAL 可通过 | Task 7 | `ReviewServiceTest#approveSetsStatusApprovedAndGolden` |
| 通过后 APPROVED | Task 7 | `ReviewServiceTest#approveSetsStatusApprovedAndGolden` |
| 打回后 submission.status=REJECTED | Task 7 | `ReviewServiceTest#rejectSetsStatusRejected` |
| 打回后 assignment.status=RETURNED | Task 7 | `ReviewServiceTest#rejectSetsAssignmentStatusReturned` |
| 打回理由 Labeler 可见（存入 review_records.reason） | Task 7 | `ReviewServiceTest#rejectCreatesReviewRecordWithReason` |
| 通过后发出 SubmissionApproved 事件 | Task 7 | `ReviewServiceTest#approvePublishesEvent` |
| reviewRecord 写入 reviewLevel | Task 7 | `ReviewServiceTest#approveCreatesReviewRecordWithLevel` |
| 打回必须有理由 | Task 7 | `ReviewServiceTest#rejectBlankReasonThrows` |
| 非 PENDING_FINAL 不能终审 | Task 7 | `ReviewServiceTest#approveNonPendingFinalThrows` |

---

## 注：数据库 DDL（供参考，需与 BE-B 协商建表时机）

```sql
CREATE TABLE review_records (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    submission_id BIGINT       NOT NULL,
    reviewer_id   BIGINT       NOT NULL,
    action        VARCHAR(32)  NOT NULL COMMENT 'APPROVE / REJECT',
    review_level  INT          NOT NULL DEFAULT 1,
    reason        TEXT,
    review_comment TEXT,
    created_at    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    INDEX idx_submission_id (submission_id)
) COMMENT = '人工终审记录';

ALTER TABLE submissions ADD COLUMN is_golden TINYINT(1) NOT NULL DEFAULT 0;
```
