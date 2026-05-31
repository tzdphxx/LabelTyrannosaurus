package com.labelhub.modules.agent.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SystemActorContextTest {

    @Test
    void actorTypeConstantIsSystemAgent() {
        assertThat(SystemActorContext.ACTOR_TYPE).isEqualTo("SYSTEM_AGENT");
    }
}
