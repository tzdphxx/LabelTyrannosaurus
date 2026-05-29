package com.labelhub.modules.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.common.security.CurrentUser;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.task.domain.TaskEntity;
import com.labelhub.modules.task.domain.TaskStatus;
import com.labelhub.modules.task.repository.TaskMapper;
import com.labelhub.modules.template.domain.TemplateVersionEntity;
import com.labelhub.modules.template.dto.SchemaValidationError;
import com.labelhub.modules.template.repository.TemplateMapper;
import com.labelhub.modules.template.repository.TemplateVersionMapper;
import com.labelhub.modules.template.service.SchemaValidationService;
import com.labelhub.modules.template.service.TemplateVersionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SchemaValidationServiceTest {

    private final TaskMapper taskMapper = mock(TaskMapper.class);
    private final TemplateMapper templateMapper = mock(TemplateMapper.class);
    private final TemplateVersionMapper templateVersionMapper = mock(TemplateVersionMapper.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TemplateVersionService templateVersionService = new TemplateVersionService(
            taskMapper,
            templateMapper,
            templateVersionMapper,
            objectMapper
    );
    private final SchemaValidationService schemaValidationService = new SchemaValidationService(
            objectMapper,
            templateVersionService
    );

    @BeforeEach
    void setCurrentUser() {
        CurrentUserContext.set(new CurrentUser(10L, "owner", "owner@example.com", Set.of(RoleCode.OWNER), 1));
    }

    @AfterEach
    void clearCurrentUser() {
        CurrentUserContext.clear();
    }

    @Test
    void validateAnswerReturnsRequiredErrorWithPath() {
        stubVersion("""
                {"components":[{"type":"Input","field":"answer","required":true}]}
                """);

        var errors = schemaValidationService.validateAnswer(200L, Map.of());

        assertThat(errors)
                .extracting(SchemaValidationError::path)
                .containsExactly("/answer");
        assertThat(errors)
                .extracting(SchemaValidationError::errorCode)
                .containsExactly(409301);
    }

    @Test
    void validateAnswerRejectsEnumMismatch() {
        stubVersion("""
                {"components":[{"type":"Input","field":"answer","enum":["A","B"]}]}
                """);

        var errors = schemaValidationService.validateAnswer(200L, Map.of("answer", "C"));

        assertThat(errors)
                .extracting(SchemaValidationError::path)
                .containsExactly("/answer");
        assertThat(errors.get(0).errorMessage()).contains("enum");
    }

    @Test
    void validateAnswerRejectsRegexMismatch() {
        stubVersion("""
                {"components":[{"type":"Input","field":"phone","regex":"^1\\\\d{10}$"}]}
                """);

        var errors = schemaValidationService.validateAnswer(200L, Map.of("phone", "abc"));

        assertThat(errors)
                .extracting(SchemaValidationError::path)
                .containsExactly("/phone");
        assertThat(errors.get(0).errorMessage()).contains("regex");
    }

    @Test
    void validateAnswerRejectsShowItemField() {
        stubVersion("""
                {"components":[
                  {"type":"ShowItem","field":"preview"},
                  {"type":"Input","field":"answer"}
                ]}
                """);

        var errors = schemaValidationService.validateAnswer(200L, Map.of(
                "preview", "value",
                "answer", "A"
        ));

        assertThat(errors)
                .extracting(SchemaValidationError::path)
                .containsExactly("/preview");
        assertThat(errors.get(0).errorMessage()).contains("ShowItem");
    }

    @Test
    void validateAnswerRejectsSchemaVersionOwnedByAnotherOwner() {
        stubVersion("""
                {"components":[{"type":"Input","field":"secret","required":true}]}
                """);
        CurrentUserContext.set(new CurrentUser(20L, "other", "other@example.com", Set.of(RoleCode.OWNER), 1));

        assertThatThrownBy(() -> schemaValidationService.validateAnswer(200L, Map.of()))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(403001);
    }

    @Test
    void validateSchemaAllowsFieldlessContainersWithNestedAnswerFields() {
        schemaValidationService.validateSchema("""
                {"components":[
                  {"type":"Group","children":[
                    {"type":"Input","field":"answer","required":true}
                  ]},
                  {"type":"Grid","components":[
                    {"type":"Input","field":"comment"}
                  ]}
                ]}
                """);
    }

    @Test
    void validateSchemaRejectsDuplicateField() {
        assertThatThrownBy(() -> schemaValidationService.validateSchema("""
                {"components":[
                  {"type":"Input","field":"answer"},
                  {"type":"Input","field":"answer"}
                ]}
                """))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(409301);
    }

    @Test
    void validateAnswerFailsWhenVersionMissing() {
        when(templateVersionMapper.selectById(200L)).thenReturn(null);

        assertThatThrownBy(() -> schemaValidationService.validateAnswer(200L, Map.of()))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(400102);
    }

    private TemplateVersionEntity version(String schemaJson) {
        TemplateVersionEntity version = new TemplateVersionEntity();
        version.setId(200L);
        version.setTemplateId(100L);
        version.setTaskId(1L);
        version.setVersionNo(1);
        version.setSchemaJson(schemaJson);
        version.setPublishedSnapshot(true);
        version.setCreatedBy(10L);
        return version;
    }

    private void stubVersion(String schemaJson) {
        when(templateVersionMapper.selectById(anyLong())).thenReturn(version(schemaJson));
        stubTask(10L);
    }

    private void stubTask(Long ownerId) {
        TaskEntity task = new TaskEntity();
        task.setId(1L);
        task.setOwnerId(ownerId);
        task.setStatus(TaskStatus.DRAFT);
        when(taskMapper.selectById(1L)).thenReturn(task);
    }
}
