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
