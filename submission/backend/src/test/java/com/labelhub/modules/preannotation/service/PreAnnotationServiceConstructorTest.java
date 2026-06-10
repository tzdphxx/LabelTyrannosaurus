package com.labelhub.modules.preannotation.service;

import com.labelhub.infrastructure.llmtask.LlmTaskQueueService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Constructor;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class PreAnnotationServiceConstructorTest {

    @Test
    void serviceHasSingleAutowiredConstructorForSpringRuntime() {
        Constructor<?>[] autowiredConstructors = Arrays.stream(PreAnnotationService.class.getConstructors())
                .filter(constructor -> constructor.isAnnotationPresent(Autowired.class))
                .toArray(Constructor[]::new);

        assertThat(autowiredConstructors).hasSize(1);
        assertThat(Arrays.asList(autowiredConstructors[0].getParameterTypes()))
                .contains(LlmTaskQueueService.class);
    }
}
