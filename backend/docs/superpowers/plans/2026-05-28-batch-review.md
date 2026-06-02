# 6.2 审核员批量操作 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reviewer 可以批量通过/打回/标记人工复核/分配审核员，逐条处理，部分成功返回，冲突组不可批量通过。

**Architecture:** BatchReviewService 封装批量逻辑，内部逐条调用校验并收集结果；通过 SubmissionMapper 查询冲突状态（同 task 同 datasetItemId 多条 PENDING_FINAL）；Controller 暴露 4 个 batch 端点。

**Tech Stack:** Spring Boot 3, MyBatis Plus, Lombok, JUnit 5 + Mockito, AssertJ

---

## File Structure

| 操作 | 路径 | 职责 |
|------|------|------|
| Create | `review/dto/BatchApproveRequest.java` | 批量通过请求 |
| Create | `review/dto/BatchRejectRequest.java` | 批量打回请求 |
| Create | `review/dto/BatchMarkManualRequest.java` | 批量标记人工复核请求 |
| Create | `review/dto/BatchAssignRequest.java` | 批量分配审核员请求 |
| Create | `review/dto/BatchReviewResponse.java` | 批量操作响应 |
| Create | `review/dto/BatchReviewItemResult.java` | 单条操作结果 |
| Modify | `submission/mapper/SubmissionMapper.java` | 增加冲突检测查询 |
| Create | `review/service/BatchReviewService.java` | 批量审核服务 |
| Modify | `review/web/ReviewController.java` | 增加 batch 端点 |
| Create | `test/.../review/service/BatchReviewServiceTest.java` | 单元测试 |
| Create | `test/.../review/web/BatchReviewControllerTest.java` | Controller 测试 |

---
