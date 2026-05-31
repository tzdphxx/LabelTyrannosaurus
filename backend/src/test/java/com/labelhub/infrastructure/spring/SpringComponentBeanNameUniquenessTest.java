package com.labelhub.infrastructure.spring;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SpringComponentBeanNameUniquenessTest {

    @Test
    void springComponentBeanNamesAreUniqueAcrossApplicationPackages() {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(true);

        AnnotationBeanNameGenerator beanNameGenerator = new AnnotationBeanNameGenerator();
        DefaultListableBeanFactory registry = new DefaultListableBeanFactory();
        Map<String, String> beanNames = new LinkedHashMap<>();
        List<String> duplicates = new ArrayList<>();

        for (BeanDefinition candidate : scanner.findCandidateComponents("com.labelhub")) {
            String beanName = beanNameGenerator.generateBeanName(candidate, registry);
            String beanClassName = candidate.getBeanClassName();
            String previousClassName = beanNames.putIfAbsent(beanName, beanClassName);
            if (previousClassName != null && !previousClassName.equals(beanClassName)) {
                duplicates.add(beanName + ": " + previousClassName + " <-> " + beanClassName);
            }
        }

        assertThat(duplicates).isEmpty();
    }
}
