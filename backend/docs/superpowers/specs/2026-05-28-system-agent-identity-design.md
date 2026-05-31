# System Agent 身份（4.1）设计文档

**日期：** 2026-05-28  
**功能编号：** 4.1  
**负责方：** BE-A  
**分支：** tzdphxx

---

## 1. 背景与目标

AI Agent 在执行 AI 预审时，需要作为独立系统主体写入审计日志（`audit_logs.actor_type = 'SYSTEM_AGENT'`）。  
`users` 表中的 `system_ai_agent` 系统用户由 BE-B 创建维护，BE-A 只读取并使用其 ID。

**交付效果：**
- BE-A 能读取 BE-B 提供的 `system_ai_agent` profile 并缓存 `agentId`
- AI 审核写审计时 `actorType = SYSTEM_AGENT`，`actorId` 指向 `system_ai_agent`

---

## 2. 架构

### 2.1 文件布局

```
backend/src/main/java/com/labelhub/
├── common/user/
│   ├── User.java                              # 共享用户实体（id, username, userType）
│   └── UserMapper.java                        # MyBatis BaseMapper<User>
└── modules/agent/
    ├── domain/
    │   └── SystemActorContext.java            # 值对象：agentId + ACTOR_TYPE 常量
    ├── port/
    │   └── SystemAgentIdentityPort.java       # BE-A 定义的读取端口接口
    ├── adapter/
    │   └── DefaultSystemAgentIdentityAdapter.java  # 查询 UserMapper 实现
    └── service/
        └── SystemAgentProvider.java           # 缓存 agentId，暴露 SystemActorContext

backend/src/main/resources/db/migration/
└── V2__seed_system_agent.sql                  # 插入 system_ai_agent 种子用户

backend/src/test/java/com/labelhub/modules/agent/service/
└── SystemAgentProviderTest.java               # 单元测试
```

### 2.2 依赖方向

```
SystemAgentProvider
  → SystemAgentIdentityPort (interface, BE-A 定义)
      ← DefaultSystemAgentIdentityAdapter (实现，查询 common/user/UserMapper)
```

BE-A 不直接依赖 users 表结构，仅依赖端口接口。

---

## 3. 组件详细设计

### 3.1 `SystemActorContext`

```java
package com.labelhub.modules.agent.domain;

public record SystemActorContext(Long agentId) {
    public static final String ACTOR_TYPE = "SYSTEM_AGENT";
}
```

- 不可变值对象
- `ACTOR_TYPE` 与 `audit_logs.actor_type` CHECK 约束中的值一致

### 3.2 `SystemAgentIdentityPort`

```java
package com.labelhub.modules.agent.port;

public interface SystemAgentIdentityPort {
    Long loadSystemAgentId();
}
```

- BE-A 定义，BE-B adapter 实现
- 返回 `users` 表中 `username='system_ai_agent'` 且 `user_type='SYSTEM'` 的记录 ID
- 找不到时抛 `IllegalStateException`，防止启动后静默失败

### 3.3 `DefaultSystemAgentIdentityAdapter`

- 注入 `UserMapper`
- 使用 MyBatis-Plus `QueryWrapper` 查询 `username='system_ai_agent'` AND `user_type='SYSTEM'`
- 若结果为 null，抛出 `IllegalStateException("system_ai_agent user not found")`

### 3.4 `SystemAgentProvider`

- Spring `@Component`
- `volatile Long cachedAgentId` + 双重检查锁（DCL）实现懒加载单次缓存
- 公开方法 `get(): SystemActorContext`
- agentId 全应用生命周期内只查询一次（系统用户不会变更）

### 3.5 `common/user/User`

仅映射 AI 身份读取所需字段：`id`、`username`、`userType`、`loginEnabled`、`enabled`。  
MyBatis-Plus `@TableName("users")`，不与其他模块耦合。

### 3.6 V2 Migration

```sql
INSERT INTO users (username, email, user_type, login_enabled, display_name)
VALUES ('system_ai_agent', 'system_ai_agent@internal', 'SYSTEM', 0, 'System AI Agent');
```

- `login_enabled=0`：禁止登录，纯系统主体
- `user_type='SYSTEM'`：满足 CHECK 约束

---

## 4. 数据流

```
SystemAgentProvider.get()
  ├─ [缓存命中] 直接返回 SystemActorContext(cachedAgentId)
  └─ [未命中] SystemAgentIdentityPort.loadSystemAgentId()
                └─ DefaultSystemAgentIdentityAdapter
                     └─ UserMapper.selectOne(username='system_ai_agent', userType='SYSTEM')
                          └─ 写入 cachedAgentId
                          └─ 返回 SystemActorContext(agentId)
```

**后续 AI 审核调用示例（4.2/5.x 中使用）：**

```java
SystemActorContext ctx = systemAgentProvider.get();
auditAppender.append(
    bizType, bizId,
    SystemActorContext.ACTOR_TYPE,  // "SYSTEM_AGENT"
    ctx.agentId(),
    action, before, after, traceId, agentRunId
);
```

---

## 5. 错误处理

| 场景 | 处理方式 |
|------|---------|
| `system_ai_agent` 用户不存在 | `IllegalStateException`，快速失败，防止静默错误 |
| 数据库查询失败 | 异常向上传播，不吞异常 |
| 缓存中 agentId 已有值 | DCL 保证只加载一次，并发安全 |

---

## 6. 测试覆盖

| 测试用例 | 类型 | 验收标准 |
|---------|------|---------|
| provider 首次调用触发 port 查询，返回正确 SystemActorContext | 单元（mock port） | agentId 正确 |
| provider 第二次调用不再触发 port 查询（缓存命中） | 单元 | loadSystemAgentId 只被调用 1 次 |
| `SystemActorContext.ACTOR_TYPE` 值为 `"SYSTEM_AGENT"` | 单元 | actorType 正确 |
| adapter 未找到用户时抛 `IllegalStateException` | 单元（mock mapper） | 快速失败 |

---

## 7. 验收标准（来自任务书）

```
BE-B 创建的 system_ai_agent 可被 BE-A 读取。
AI 审核写审计时 actorType=SYSTEM_AGENT。
actorId 指向 system_ai_agent。
```

---

## 8. 不在本功能范围内

- Agent Run（4.2）
- AI 自动预审调用 SystemAgentProvider（5.3）
- BE-B 创建 system_ai_agent 的接口（BE-B 职责）
