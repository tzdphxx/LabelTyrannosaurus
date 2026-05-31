package com.labelhub.modules.agent.port;

public interface SystemAgentIdentityPort {

    /**
     * 读取 users 表中 system_ai_agent 的主键 ID。
     * 未找到时抛 IllegalStateException。
     */
    Long loadSystemAgentId();
}
