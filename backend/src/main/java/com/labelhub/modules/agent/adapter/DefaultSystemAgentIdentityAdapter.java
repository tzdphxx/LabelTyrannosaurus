package com.labelhub.modules.agent.adapter;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.labelhub.common.user.SystemUserMapper;
import com.labelhub.common.user.User;
import com.labelhub.modules.agent.port.SystemAgentIdentityPort;
import org.springframework.stereotype.Component;

@Component
class DefaultSystemAgentIdentityAdapter implements SystemAgentIdentityPort {

    private final SystemUserMapper userMapper;

    DefaultSystemAgentIdentityAdapter(SystemUserMapper userMapper) {
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
