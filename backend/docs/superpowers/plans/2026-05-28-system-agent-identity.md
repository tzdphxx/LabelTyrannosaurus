# System Agent 身份（4.1）Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让 BE-A 能读取 `system_ai_agent` 用户 ID 并缓存，构造 `SystemActorContext`，为后续 AI 审核写 `actorType=SYSTEM_AGENT` 的审计日志做好基础。

**Architecture:** Port/Adapter 模式——BE-A 定义 `SystemAgentIdentityPort` 接口，`DefaultSystemAgentIdentityAdapter` 通过 `UserMapper` 读取 `users` 表，`SystemAgentProvider` 用 volatile + DCL 懒加载缓存 `agentId`，对外暴露不可变值对象 `SystemActorContext`。

**Tech Stack:** Spring Boot 3.5, MyBatis-Plus 3.5.15, Lombok, JUnit 5 + Mockito（via spring-boot-starter-test），Flyway migration。

---

## 文件布局

| 操作 | 路径 |
|------|------|
| 新建 | `backend/src/main/resources/db/migration/V2__seed_system_agent.sql` |
| 新建 | `backend/src/main/java/com/labelhub/common/user/User.java` |
| 新建 | `backend/src/main/java/com/labelhub/common/user/UserMapper.java` |
| 新建 | `backend/src/main/java/com/labelhub/modules/agent/domain/SystemActorContext.java` |
| 新建 | `backend/src/main/java/com/labelhub/modules/agent/port/SystemAgentIdentityPort.java` |
| 新建 | `backend/src/main/java/com/labelhub/modules/agent/adapter/DefaultSystemAgentIdentityAdapter.java` |
| 新建 | `backend/src/main/java/com/labelhub/modules/agent/service/SystemAgentProvider.java` |
| 新建 | `backend/src/test/java/com/labelhub/modules/agent/adapter/DefaultSystemAgentIdentityAdapterTest.java` |
| 新建 | `backend/src/test/java/com/labelhub/modules/agent/service/SystemAgentProviderTest.java` |

---

## Task 1：V2 Migration — 种子数据

**Files:**
- Create: `backend/src/main/resources/db/migration/V2__seed_system_agent.sql`

- [ ] **Step 1: 创建 migration 文件**

```sql
-- V2__seed_system_agent.sql
INSERT INTO users (username, email, user_type, login_enabled, display_name)
VALUES ('system_ai_agent', 'system_ai_agent@internal', 'SYSTEM', 0, 'System AI Agent');
```

> `login_enabled=0` 禁止登录；`user_type='SYSTEM'` 满足 `chk_users_user_type` CHECK 约束。

- [ ] **Step 2: 提交**

```bash
git add backend/src/main/resources/db/migration/V2__seed_system_agent.sql
git commit -m "feat(agent): seed system_ai_agent user"
```

---

## Task 2：共享用户实体与 Mapper

**Files:**
- Create: `backend/src/main/java/com/labelhub/common/user/User.java`
- Create: `backend/src/main/java/com/labelhub/common/user/UserMapper.java`

- [ ] **Step 1: 创建 `User` 实体**

```java
// backend/src/main/java/com/labelhub/common/user/User.java
package com.labelhub.common.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("users")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String userType;

    private Boolean loginEnabled;

    private Boolean enabled;
}
```

- [ ] **Step 2: 创建 `UserMapper`**

```java
// backend/src/main/java/com/labelhub/common/user/UserMapper.java
package com.labelhub.common.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
```

- [ ] **Step 3: 提交**

```bash
git add backend/src/main/java/com/labelhub/common/user/
git commit -m "feat(agent): add shared User entity and UserMapper"
```

---

## Task 3：值对象 `SystemActorContext` 与端口接口

**Files:**
- Create: `backend/src/main/java/com/labelhub/modules/agent/domain/SystemActorContext.java`
- Create: `backend/src/main/java/com/labelhub/modules/agent/port/SystemAgentIdentityPort.java`

- [ ] **Step 1: 创建 `SystemActorContext`**

```java
// backend/src/main/java/com/labelhub/modules/agent/domain/SystemActorContext.java
package com.labelhub.modules.agent.domain;

public record SystemActorContext(Long agentId) {

    public static final String ACTOR_TYPE = "SYSTEM_AGENT";
}
```

> `ACTOR_TYPE` 的值必须与 `audit_logs.actor_type` CHECK 约束 `('USER', 'SYSTEM_AGENT')` 一致。

- [ ] **Step 2: 创建端口接口**

```java
// backend/src/main/java/com/labelhub/modules/agent/port/SystemAgentIdentityPort.java
package com.labelhub.modules.agent.port;

public interface SystemAgentIdentityPort {

    /**
     * 读取 users 表中 system_ai_agent 的主键 ID。
     * 未找到时抛 IllegalStateException。
     */
    Long loadSystemAgentId();
}
```

- [ ] **Step 3: 提交**

```bash
git add backend/src/main/java/com/labelhub/modules/agent/
git commit -m "feat(agent): add SystemActorContext and SystemAgentIdentityPort"
```

---

## Task 4：Adapter — TDD

**Files:**
- Create: `backend/src/main/java/com/labelhub/modules/agent/adapter/DefaultSystemAgentIdentityAdapter.java`
- Create: `backend/src/test/java/com/labelhub/modules/agent/adapter/DefaultSystemAgentIdentityAdapterTest.java`

- [ ] **Step 1: 先写测试（失败）**

```java
// backend/src/test/java/com/labelhub/modules/agent/adapter/DefaultSystemAgentIdentityAdapterTest.java
package com.labelhub.modules.agent.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.labelhub.common.user.User;
import com.labelhub.common.user.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultSystemAgentIdentityAdapterTest {

    @Mock
    private UserMapper userMapper;

    private DefaultSystemAgentIdentityAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new DefaultSystemAgentIdentityAdapter(userMapper);
    }

    @Test
    void returnsAgentIdWhenSystemUserFound() {
        User user = new User();
        user.setId(99L);
        when(userMapper.selectOne(any())).thenReturn(user);

        assertThat(adapter.loadSystemAgentId()).isEqualTo(99L);
    }

    @Test
    void throwsWhenSystemAgentUserNotFound() {
        when(userMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> adapter.loadSystemAgentId())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("system_ai_agent");
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

```bash
cd backend && mvn test -Dtest=DefaultSystemAgentIdentityAdapterTest -q 2>&1 | tail -5
```

期望输出包含：`COMPILATION FAILURE` 或 `ClassNotFoundException`（类尚未创建）。

- [ ] **Step 3: 实现 Adapter**

```java
// backend/src/main/java/com/labelhub/modules/agent/adapter/DefaultSystemAgentIdentityAdapter.java
package com.labelhub.modules.agent.adapter;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.labelhub.common.user.User;
import com.labelhub.common.user.UserMapper;
import com.labelhub.modules.agent.port.SystemAgentIdentityPort;
import org.springframework.stereotype.Component;

@Component
class DefaultSystemAgentIdentityAdapter implements SystemAgentIdentityPort {

    private final UserMapper userMapper;

    DefaultSystemAgentIdentityAdapter(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public Long loadSystemAgentId() {
        User user = userMapper.selectOne(
                new QueryWrapper<User>()
                        .eq("username", "system_ai_agent")
                        .eq("user_type", "SYSTEM")
        );
        if (user == null) {
            throw new IllegalStateException("system_ai_agent user not found — ensure V2 migration has run");
        }
        return user.getId();
    }
}
```

- [ ] **Step 4: 运行测试，确认通过**

```bash
cd backend && mvn test -Dtest=DefaultSystemAgentIdentityAdapterTest -q 2>&1 | tail -5
```

期望输出：`BUILD SUCCESS`。

- [ ] **Step 5: 提交**

```bash
git add backend/src/main/java/com/labelhub/modules/agent/adapter/ \
        backend/src/test/java/com/labelhub/modules/agent/adapter/
git commit -m "feat(agent): add DefaultSystemAgentIdentityAdapter with tests"
```

---

## Task 5：Provider — TDD

**Files:**
- Create: `backend/src/main/java/com/labelhub/modules/agent/service/SystemAgentProvider.java`
- Create: `backend/src/test/java/com/labelhub/modules/agent/service/SystemAgentProviderTest.java`

- [ ] **Step 1: 先写测试（失败）**

```java
// backend/src/test/java/com/labelhub/modules/agent/service/SystemAgentProviderTest.java
package com.labelhub.modules.agent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.labelhub.modules.agent.domain.SystemActorContext;
import com.labelhub.modules.agent.port.SystemAgentIdentityPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SystemAgentProviderTest {

    private static final Long AGENT_ID = 42L;

    @Mock
    private SystemAgentIdentityPort identityPort;

    private SystemAgentProvider provider;

    @BeforeEach
    void setUp() {
        provider = new SystemAgentProvider(identityPort);
    }

    @Test
    void returnsContextWithCorrectAgentId() {
        when(identityPort.loadSystemAgentId()).thenReturn(AGENT_ID);

        SystemActorContext ctx = provider.get();

        assertThat(ctx.agentId()).isEqualTo(AGENT_ID);
    }

    @Test
    void actorTypeConstantIsSystemAgent() {
        assertThat(SystemActorContext.ACTOR_TYPE).isEqualTo("SYSTEM_AGENT");
    }

    @Test
    void cachesAgentIdAfterFirstLoad() {
        when(identityPort.loadSystemAgentId()).thenReturn(AGENT_ID);

        provider.get();
        provider.get();

        verify(identityPort, times(1)).loadSystemAgentId();
    }

    @Test
    void eachCallReturnsFreshContextWithSameAgentId() {
        when(identityPort.loadSystemAgentId()).thenReturn(AGENT_ID);

        SystemActorContext first = provider.get();
        SystemActorContext second = provider.get();

        assertThat(first.agentId()).isEqualTo(second.agentId());
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

```bash
cd backend && mvn test -Dtest=SystemAgentProviderTest -q 2>&1 | tail -5
```

期望输出：`COMPILATION FAILURE`（类尚未创建）。

- [ ] **Step 3: 实现 `SystemAgentProvider`**

```java
// backend/src/main/java/com/labelhub/modules/agent/service/SystemAgentProvider.java
package com.labelhub.modules.agent.service;

import com.labelhub.modules.agent.domain.SystemActorContext;
import com.labelhub.modules.agent.port.SystemAgentIdentityPort;
import org.springframework.stereotype.Component;

@Component
public class SystemAgentProvider {

    private final SystemAgentIdentityPort identityPort;
    private volatile Long cachedAgentId;

    public SystemAgentProvider(SystemAgentIdentityPort identityPort) {
        this.identityPort = identityPort;
    }

    public SystemActorContext get() {
        if (cachedAgentId == null) {
            synchronized (this) {
                if (cachedAgentId == null) {
                    cachedAgentId = identityPort.loadSystemAgentId();
                }
            }
        }
        return new SystemActorContext(cachedAgentId);
    }
}
```

- [ ] **Step 4: 运行测试，确认通过**

```bash
cd backend && mvn test -Dtest=SystemAgentProviderTest -q 2>&1 | tail -5
```

期望输出：`BUILD SUCCESS`。

- [ ] **Step 5: 运行全量测试，确认无回归**

```bash
cd backend && mvn test -q 2>&1 | tail -10
```

期望输出：`BUILD SUCCESS`，无 `FAILURE` 或 `ERROR`。

- [ ] **Step 6: 提交**

```bash
git add backend/src/main/java/com/labelhub/modules/agent/service/ \
        backend/src/test/java/com/labelhub/modules/agent/service/
git commit -m "feat(agent): add SystemAgentProvider with caching and tests"
```

---

## 验收检查

完成以上 5 个 Task 后，验证以下三条验收标准均满足：

| 验收标准 | 如何验证 |
|---------|---------|
| BE-B 创建的 `system_ai_agent` 可被 BE-A 读取 | `DefaultSystemAgentIdentityAdapterTest` 通过 |
| AI 审核写审计时 `actorType=SYSTEM_AGENT` | `SystemActorContext.ACTOR_TYPE` 值为 `"SYSTEM_AGENT"`（由 `actorTypeConstantIsSystemAgent` 测试覆盖） |
| `actorId` 指向 `system_ai_agent` | `returnsContextWithCorrectAgentId` 测试覆盖 |

运行全量测试确认：

```bash
cd backend && mvn test -q 2>&1 | tail -5
```

期望：`BUILD SUCCESS`。
