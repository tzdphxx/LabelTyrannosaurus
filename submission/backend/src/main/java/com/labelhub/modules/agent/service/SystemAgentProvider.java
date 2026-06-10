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
