# 03\-BE\-B后端平台支撑与数据资产开发任务书

# BE\-B 后端平台支撑与数据资产开发任务书

> 面向 AI 开发工具：本文件只描述 BE\-B 负责范围。BE\-B 负责用户权限、数据资产、模板资源、奖励统计、导出、存储、Redis/Redisson 和通用异步基础设施。

## 0\. BE\-B 目标

BE\-B 负责 LabelHub 的平台支撑与数据资产服务：

- Auth/RBAC。

- Admin 用户与角色管理。

- 数据集导入。

- 题目批量编辑。

- 模板版本管理。

- Schema 存储与 Schema 校验服务。

- 奖励规则。

- 奖励流水。

- Labeler 贡献统计。

- 导出任务。

- 字段映射。

- 多格式文件生成。

- 对象存储。

- 文件上传。

- 错误报告。

- Redis/Redisson lock 与 rate limit。

- 通用异步任务执行器。

- System Principal 创建与维护。

- AuditAppender 审计追加能力。

边界规则：

- BE\-B 不直接修改 submission\.status。

- BE\-B 不直接修改 review\.status。

- BE\-B 不直接决定 isGolden。

- BE\-B 根据 BE\-A 事件结算奖励和统计。

- BE\-B 提供 schema 校验能力，最终是否允许提交由 BE\-A 决定。

- BE\-B 提供 Redis/Redisson 和异步执行基础能力，业务触发由对应模块决定。

- BE\-B 提供 JWT 解析、tokenVersion 校验和 AuthContext 能力，BE\-A 复用该能力鉴权。

- BE\-B 提供 AuditAppender，BE\-A/BE\-B 都通过追加接口写审计，不直接操作审计表。

## 1\. Auth/RBAC 与 Admin 模块

### 1\.1 大功能：开放注册与登录

交付效果：

- 用户可以注册登录，系统能按角色鉴权。

小功能：

1. 用户注册

    - username 唯一。

    - email 唯一。

    - 密码 BCrypt。

    - 默认角色 Labeler。

2. 用户登录

    - accessToken。

    - refreshToken。

    - tokenVersion。

3. 当前用户信息。

4. token 刷新。

5. 登出。

验收：

```Plaintext
注册用户默认 Labeler。
错误密码不能登录。
token 过期后需要刷新。
未登录访问业务接口返回 401。
```

### 1\.2 大功能：Admin 用户管理

交付效果：

- Admin 可以管理用户角色。

小功能：

1. 用户列表。

2. 用户搜索。

3. 修改角色。

4. 启用用户。

5. 禁用用户。

6. 禁止移除最后一个 Admin。

7. System 用户过滤。

验收：

```Plaintext
非 Admin 不能改角色。
Admin 能授权 Owner/Reviewer。
system_ai_agent 不作为普通用户展示。
禁用用户不能登录。
```

### 1\.3 大功能：Auth Propagation

交付效果：

- BE\-A 的任务、审核、LLM Provider 接口可以复用统一登录态和角色鉴权。

小功能：

1. JWT 签发。

2. JWT 解析。

3. userId 提取。

4. roles 提取。

5. tokenVersion 校验。

6. `CurrentUserContext`。

7. 角色变更后旧 token 失效。

规则：

- MVP 采用模块化单体，共享 JWT 密钥和用户表。

- BE\-A 不实现第二套登录。

- BE\-A 接口通过统一 Auth Filter 获取当前用户。

验收：

```Plaintext
BE-A 接口能读取 userId 和 roles。
角色变更后旧 token 访问 BE-A 接口失败。
BE-A 无权限接口返回 403。
```

### 1\.4 大功能：System Principal

交付效果：

- 平台存在不可登录的 `system\_ai\_agent`，供 BE\-A AI 审核链路使用。

小功能：

1. 初始化 `system\_ai\_agent`。

2. `userType=SYSTEM`。

3. `loginEnabled=false`。

4. `role=SYSTEM\_AGENT`。

5. 提供 `getSystemAgentProfile\(\)`。

6. Admin 用户列表过滤 system 用户。

验收：

```Plaintext
system_ai_agent 启动时存在。
system_ai_agent 不能登录。
BE-A 能读取 system_ai_agent 的 actorId。
前端用户列表不展示 system_ai_agent。
```

### 1\.5 大功能：AuditAppender

交付效果：

- 所有模块通过统一追加接口写审计，避免直接耦合审计表。

小功能：

1. `AuditAppender\.append\(command\)`。

2. actorType。

3. actorId。

4. bizType。

5. bizId。

6. action。

7. beforeJson。

8. afterJson。

9. traceId。

10. agentRunId 可选。

规则：

- BE\-A 和 BE\-B 都只能通过 AuditAppender 写审计。

- 审计日志只追加，不更新，不删除。

- 审计字段变更由 BE\-B 维护兼容。

验收：

```Plaintext
BE-A 调用 AuditAppender 能写审计。
BE-B 调用 AuditAppender 能写审计。
AI 审计能带 agentRunId。
审计查询能按 bizType + bizId 返回时间线。
```

## 2\. 数据资产模块

### 2\.1 大功能：数据集导入

交付效果：

- Owner 能把原始数据导入为平台题目资产。

小功能：

1. JSON 导入。

2. JSONL 导入。

3. Excel 导入。

4. `QA\_QUALITY` 解析。

5. `PREFERENCE\_COMPARE` 解析。

6. 原始文件上传对象存储。

7. 失败行记录。

8. 错误报告生成。

9. 导入进度查询。

规则：

- 同任务 externalId 唯一。

- 单行失败不影响其他行。

- 文件不存数据库二进制。

验收：

```Plaintext
qa_quality.jsonl 可导入。
preference_compare.jsonl 可导入。
重复 externalId 返回明确错误。
错误报告可下载。
```

### 2\.2 大功能：题目批量编辑

交付效果：

- Owner 能安全维护题目资产。

小功能：

1. DRAFT 批量编辑。

2. DRAFT 批量删除。

3. DRAFT 覆盖导入。

4. DRAFT/PUBLISHED 批量追加。

5. PUBLISHED 修正未领取题目。

6. 逐条校验。

7. 逐条结果返回。

8. JSON Patch 审计。

9. 软删除。

规则：

- 已领取题目不可改原始数据。

- 已提交题目不可改原始数据。

- 覆盖导入只允许 DRAFT。

验收：

```Plaintext
DRAFT 可覆盖导入。
PUBLISHED 可追加新题。
PUBLISHED 修改已领取题失败。
批量操作返回成功数、失败数、失败原因。
```

### 2\.3 大功能：题目快照查询能力

交付效果：

- BE\-A 可以在领取、提交、审核时读取稳定题目快照。

小功能：

1. 按 task 查询可领取 item。

2. 按 itemId 查询 itemJson。

3. 按 task 聚合 assigned/submitted/approved 计数。

4. 判断 item 是否可编辑。

5. 判断 item 是否可领取。

6. `reserveClaimableItem\(taskId, labelerId\)`。

7. `increaseSubmittedCount\(itemId\)`。

8. `increaseApprovedCount\(itemId\)`。

验收：

```Plaintext
BE-A 能查询可领取 item。
BE-A 能查询 itemJson 快照。
已领取 item 返回不可编辑。
模块化单体下 reserveClaimableItem 参与 BE-A 的本地事务。
提交成功后 submittedCount 有明确更新入口。
审核通过或金标选择后 approvedCount 有明确更新入口。
```

## 3\. 模板资源与 Schema 校验模块

### 3\.1 大功能：模板版本管理

交付效果：

- 模板作为可复用资源由平台统一管理。

小功能：

1. 创建模板。

2. 保存模板版本。

3. 查询版本列表。

4. 查询版本详情。

5. fork 模板版本。

6. 发布快照标记。

规则：

- 已发布版本不可原地修改。

- `versionNo` 单调递增。

- schemaJson 入库前必须校验结构。

验收：

```Plaintext
Owner 可保存模板。
已发布版本修改失败。
fork 后生成新版本。
BE-A 能按 versionId 读取 schema。
```

### 3\.2 大功能：Schema 校验服务

交付效果：

- BE\-A 提交答案时，可以调用统一校验能力。

小功能：

1. `validateSchema\(schemaJson\)`。

2. `validateAnswer\(schemaVersionId, answerJson\)`。

3. required 校验。

4. enum 校验。

5. regex 校验。

6. JSON 字段校验。

7. ShowItem 排除提交。

8. field 重复检测。

9. 校验错误路径返回。

验收：

```Plaintext
必填缺失返回字段路径。
非法 enum 返回字段路径。
ShowItem 字段不允许作为答案字段。
schema 非法时模板保存失败。
```

## 4\. 奖励与贡献统计模块

### 4\.1 大功能：奖励规则

交付效果：

- Owner 能配置虚拟奖励规则。

小功能：

1. 保存奖励规则。

2. 查询奖励规则。

3. 发布时冻结规则快照。

4. 对 Labeler 是否可见。

5. 规则版本。

规则：

- 只做虚拟奖励。

- 不涉及支付。

- 不涉及提现。

- 不涉及真实账单。

验收：

```Plaintext
Owner 能配置单条通过奖励。
发布后规则快照可查。
规则变更不影响历史流水。
```

### 4\.2 大功能：奖励结算

交付效果：

- BE\-A 确认通过或金标后，BE\-B 生成奖励流水。

事件来源：

```Plaintext
SubmissionApproved
GoldenSelected
RewardReversed
```

小功能：

1. 消费 SubmissionApproved。

2. 消费 GoldenSelected。

3. 生成正向 rewardLedger。

4. 幂等检查。

5. 负向冲正。

6. 写审计。

7. 更新 approvedCount。

规则：

- 同一 submission 只能有一条正向奖励。

- 正向奖励最终幂等键以 submissionId 为准。

- 打回不计奖励。

- 冲突金标只奖励金标提交者。

- 如果同一 submission 先通过、后又被选为金标，GoldenSelected 不得重复计奖。

验收：

```Plaintext
通过事件生成奖励流水。
重复事件不重复发奖励。
同一 submission 收到 SubmissionApproved 和 GoldenSelected 不重复发奖励。
冲正生成负向流水。
```

### 4\.3 大功能：贡献统计

交付效果：

- Labeler 可以查看个人贡献和奖励。

小功能：

1. 已领取数。

2. 已提交数。

3. 待审核数。

4. 通过数。

5. 打回数。

6. 通过率。

7. 累计奖励。

8. 今日提交。

9. 近 7 日趋势。

10. 各任务贡献明细。

11. 奖励流水。

验收：

```Plaintext
通过后通过数增加。
打回后打回数增加。
待审核不进入通过率分母。
近 7 日无数据日期补零。
累计奖励等于奖励流水汇总。
```

## 5\. 导出模块

### 5\.1 大功能：导出任务

交付效果：

- Owner 能异步导出 APPROVED 金标数据。

小功能：

1. 创建导出任务。

2. 查询导出状态。

3. 查询导出历史。

4. 字段映射。

5. JSON。

6. JSONL。

7. CSV。

8. Excel。

9. 包含 aiReview。

10. 包含 auditTrail。

11. 下载链接。

规则：

- 默认只导出 `APPROVED \+ isGolden=true`。

- submission 状态、isGolden、AI 结果由 BE\-A 的导出快照查询提供。

- 大文件分页读取。

- 文件上传对象存储。

依赖 BE\-A：

```Plaintext
queryExportableGoldenSubmissions(taskId, pageRequest)
```

验收：

```Plaintext
导出任务状态可查询。
导出文件可下载。
默认不导出打回数据。
包含 AI 审核时字段正确。
导出数据来自 BE-A 快照查询。
```

## 6\. 平台基础设施模块

### 6\.1 大功能：对象存储

交付效果：

- 所有文件统一走对象存储。

小功能：

1. MinIO client。

2. 上传文件。

3. 下载文件。

4. 预签名 URL。

5. object key 生成。

6. 文件元数据。

7. 文件类型限制。

使用场景：

- 数据集原始文件。

- 导入错误报告。

- 标注证据文件。

- 导出文件。

验收：

```Plaintext
文件可上传。
文件可下载。
预签名 URL 可访问。
数据库不存文件二进制。
```

### 6\.2 大功能：Redis/Redisson 基础能力

交付效果：

- BE\-A 可以复用锁、限流和缓存能力。

小功能：

1. Redisson 配置。

2. LockService。

3. RateLimitService。

4. Redis key 规范。

5. 草稿缓存能力。

验收：

```Plaintext
LockService 可用于领取题目。
锁超时自动释放。
RRateLimiter 能限制 QPS。
草稿缓存可读写。
```

### 6\.3 大功能：通用异步任务执行器

交付效果：

- 导入、AI 审核、导出都可以使用统一异步基础设施。

小功能：

1. 任务提交。

2. 任务执行。

3. 任务状态。

4. 错误记录。

5. traceId 传递。

6. 重试策略。

验收：

```Plaintext
异步任务失败有错误信息。
任务状态可查询。
后台任务日志包含 traceId。
任务失败不会卡住主事务。
```

## 7\. BE\-B 自测清单

```Plaintext
Auth/RBAC 测试通过。
Admin 用户管理测试通过。
Auth Propagation 测试通过。
System Principal 初始化测试通过。
AuditAppender 追加测试通过。
数据集导入样例测试通过。
批量编辑状态限制测试通过。
模板版本冻结测试通过。
Schema 校验服务测试通过。
奖励事件幂等测试通过。
贡献统计口径测试通过。
导出范围测试通过。
导出调用 BE-A 快照查询测试通过。
MinIO 上传下载测试通过。
Redisson lock 测试通过。
RRateLimiter 测试通过。
异步任务状态测试通过。
```

