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
        assertThat(first).isNotSameAs(second);
    }
}
