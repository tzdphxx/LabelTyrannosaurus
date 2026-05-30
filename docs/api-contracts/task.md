# task

# Task API Contract

Owner：BE\-A

## POST /api/v1/tasks

权限：OWNER。

请求字段：

```Plaintext
title
description
instructionRichText
tags
quota
deadlineAt
overlapCount
```

响应字段：

```Plaintext
taskId
status = DRAFT
```

## POST /api/v1/tasks/\{taskId\}/publish

权限：OWNER。

发布前检查：

```Plaintext
BE-B dataset exists
BE-B template version exists
BE-B reward rule exists
BE-A ai review config exists
deadlineAt > now
quota > 0
overlapCount >= 1
```

状态影响：

```Plaintext
DRAFT -> PUBLISHED
```

错误码：

```Plaintext
400101 状态不允许
400102 发布条件缺失
```

## POST /api/v1/tasks/\{taskId\}/pause

状态影响：

```Plaintext
PUBLISHED -> PAUSED
```

## POST /api/v1/tasks/\{taskId\}/resume

状态影响：

```Plaintext
PAUSED -> PUBLISHED
```

## POST /api/v1/tasks/\{taskId\}/end

状态影响：

```Plaintext
PUBLISHED/PAUSED -> ENDED
```

