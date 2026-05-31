package com.labelhub.modules.agent.domain;

public record SystemActorContext(Long agentId) {

    public static final String ACTOR_TYPE = "SYSTEM_AGENT";
}
