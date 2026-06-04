package com.labelhub.modules.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.common.security.CurrentUser;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.task.domain.Task;
import com.labelhub.modules.task.domain.TaskStatus;
import com.labelhub.modules.task.mapper.TaskMapper;
import com.labelhub.modules.template.domain.TemplateEntity;
import com.labelhub.modules.template.domain.TemplateVersionEntity;
import com.labelhub.modules.template.dto.CreateTemplateRequest;
import com.labelhub.modules.template.dto.ForkTemplateRequest;
import com.labelhub.modules.template.repository.TemplateMapper;
import com.labelhub.modules.template.repository.TemplateVersionRepositoryMapper;
import com.labelhub.modules.template.service.TemplateSchemaValidator;
import com.labelhub.modules.template.service.TemplateService;
import com.labelhub.modules.template.service.TemplateVersionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TemplateVersionServiceTest {

    private final TaskMapper taskMapper = mock(TaskMapper.class);
    private final TemplateMapper templateMapper = mock(TemplateMapper.class);
    private final TemplateVersionRepositoryMapper templateVersionMapper = mock(TemplateVersionRepositoryMapper.class);
    private final TemplateSchemaValidator schemaValidator = mock(TemplateSchemaValidator.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TemplateVersionService versionService = new TemplateVersionService(
            templateMapper,
            templateVersionMapper,
            objectMapper
    );
    private final TemplateService templateService = new TemplateService(
            taskMapper,
            templateMapper,
            templateVersionMapper,
            schemaValidator,
            versionService,
            objectMapper
    );

    @AfterEach
    void clearCurrentUser() {
        CurrentUserContext.clear();
    }

    @Test
    void ownerCreatesTemplateWithInitialVersion() {
        CurrentUserContext.set(new CurrentUser(10L, "owner", "owner@example.com", Set.of(RoleCode.OWNER), 1));
        stubTask(10L);
        stubInsertIds();

        var response = templateService.createTemplate(1L, new CreateTemplateRequest(
                "质检模板",
                Map.of("components", List.of(Map.of("type", "Input", "field", "answer"))),
                "初始版本"
        ));

        assertThat(response.templateId()).isEqualTo(100L);
        assertThat(response.currentVersionNo()).isEqualTo(1);
        assertThat(response.currentVersion().versionNo()).isEqualTo(1);
        verify(schemaValidator).validateSchema(any());
        ArgumentCaptor<TemplateEntity> templateCaptor = ArgumentCaptor.forClass(TemplateEntity.class);
        verify(templateMapper).insert(templateCaptor.capture());
        assertThat(templateCaptor.getValue().getOwnerId()).isEqualTo(10L);
        assertThat(templateCaptor.getValue().getCurrentVersionNo()).isEqualTo(1);
        ArgumentCaptor<TemplateVersionEntity> versionCaptor = ArgumentCaptor.forClass(TemplateVersionEntity.class);
        verify(templateVersionMapper).insert(versionCaptor.capture());
        assertThat(versionCaptor.getValue().getOwnerId()).isEqualTo(10L);
        assertThat(versionCaptor.getValue().getPublishedSnapshot()).isFalse();
    }

    @Test
    void ownerCreatesReusableTemplateWithoutTask() {
        CurrentUserContext.set(new CurrentUser(10L, "owner", "owner@example.com", Set.of(RoleCode.OWNER), 1));
        stubInsertIds();

        var response = templateService.createOwnerTemplate(new CreateTemplateRequest(
                "通用质检模板",
                Map.of("components", List.of(Map.of("type", "Input", "field", "answer"))),
                "初始版本"
        ));

        assertThat(response.templateId()).isEqualTo(100L);
        assertThat(response.ownerId()).isEqualTo(10L);
        assertThat(response.taskId()).isNull();
        assertThat(response.currentVersion().ownerId()).isEqualTo(10L);
        assertThat(response.currentVersion().taskId()).isNull();
        ArgumentCaptor<TemplateEntity> templateCaptor = ArgumentCaptor.forClass(TemplateEntity.class);
        verify(templateMapper).insert(templateCaptor.capture());
        assertThat(templateCaptor.getValue().getOwnerId()).isEqualTo(10L);
        assertThat(templateCaptor.getValue().getTaskId()).isNull();
        ArgumentCaptor<TemplateVersionEntity> versionCaptor = ArgumentCaptor.forClass(TemplateVersionEntity.class);
        verify(templateVersionMapper).insert(versionCaptor.capture());
        assertThat(versionCaptor.getValue().getTaskId()).isNull();
    }

    @Test
    void listsOnlyCurrentOwnersReusableTemplates() {
        CurrentUserContext.set(new CurrentUser(10L, "owner", "owner@example.com", Set.of(RoleCode.OWNER), 1));
        TemplateEntity template = ownerTemplate(100L, 10L, 2);
        TemplateVersionEntity current = ownerVersion(200L, 100L, 10L, 2, false);
        when(templateMapper.selectByOwnerId(10L)).thenReturn(List.of(template));
        when(templateVersionMapper.selectByTemplateIdAndVersionNo(100L, 2)).thenReturn(current);

        var response = templateService.listOwnerTemplates();

        assertThat(response).hasSize(1);
        assertThat(response.get(0).ownerId()).isEqualTo(10L);
        assertThat(response.get(0).taskId()).isNull();
        assertThat(response.get(0).currentVersion().ownerId()).isEqualTo(10L);
    }

    @Test
    void nonOwnerCannotCreateTemplate() {
        CurrentUserContext.set(new CurrentUser(20L, "other", "other@example.com", Set.of(RoleCode.OWNER), 1));
        stubTask(10L);

        assertThatThrownBy(() -> templateService.createTemplate(1L, new CreateTemplateRequest(
                "质检模板",
                Map.of("components", List.of()),
                "初始版本"
        )))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(403001);
    }

    @Test
    void invalidSchemaFailsBeforeInsert() {
        CurrentUserContext.set(new CurrentUser(10L, "owner", "owner@example.com", Set.of(RoleCode.OWNER), 1));
        stubTask(10L);
        doThrow(new BusinessException(409301, "Invalid schema"))
                .when(schemaValidator).validateSchema(any());

        assertThatThrownBy(() -> templateService.createTemplate(1L, new CreateTemplateRequest(
                "质检模板",
                Map.of("components", "not-array"),
                "初始版本"
        )))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(409301);
    }

    @Test
    void listTemplatesReturnsCurrentVersionSummary() {
        CurrentUserContext.set(new CurrentUser(10L, "owner", "owner@example.com", Set.of(RoleCode.OWNER), 1));
        stubTask(10L);
        TemplateEntity template = template(100L, 1L, 2);
        TemplateVersionEntity current = version(200L, 100L, 1L, 2, false);
        when(templateMapper.selectByTaskId(1L)).thenReturn(List.of(template));
        when(templateVersionMapper.selectByTemplateIdAndVersionNo(100L, 2)).thenReturn(current);

        var response = templateService.listTemplates(1L);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).currentVersion().versionId()).isEqualTo(200L);
        assertThat(response.get(0).currentVersion().versionNo()).isEqualTo(2);
    }

    @Test
    void getVersionValidatesTaskOwner() {
        CurrentUserContext.set(new CurrentUser(20L, "other", "other@example.com", Set.of(RoleCode.OWNER), 1));
        stubTask(10L);
        when(templateVersionMapper.selectById(200L)).thenReturn(version(200L, 100L, 1L, 1, false));

        assertThatThrownBy(() -> versionService.getVersion(200L))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(403001);
    }

    @Test
    void getOwnerVersionValidatesTemplateOwner() {
        CurrentUserContext.set(new CurrentUser(20L, "other", "other@example.com", Set.of(RoleCode.OWNER), 1));
        TemplateEntity template = ownerTemplate(100L, 10L, 1);
        when(templateVersionMapper.selectById(200L)).thenReturn(ownerVersion(200L, 100L, 10L, 1, false));
        when(templateMapper.selectById(100L)).thenReturn(template);

        assertThatThrownBy(() -> versionService.getVersion(200L))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(403001);
    }

    @Test
    void forkPublishedVersionCreatesNewVersionWithoutChangingOldVersion() {
        CurrentUserContext.set(new CurrentUser(10L, "owner", "owner@example.com", Set.of(RoleCode.OWNER), 1));
        stubTask(10L);
        TemplateEntity template = template(100L, 1L, 2);
        TemplateVersionEntity base = version(200L, 100L, 1L, 2, true);
        when(templateMapper.selectById(100L)).thenReturn(template);
        when(templateVersionMapper.selectById(200L)).thenReturn(base);
        stubInsertIds();

        var response = templateService.forkTemplate(100L, new ForkTemplateRequest(
                200L,
                Map.of("components", List.of(Map.of("type", "Input", "field", "answer2"))),
                "调整字段"
        ));

        assertThat(response.currentVersionNo()).isEqualTo(3);
        assertThat(response.currentVersion().publishedSnapshot()).isFalse();
        verify(schemaValidator).validateSchema(any());
        ArgumentCaptor<TemplateVersionEntity> versionCaptor = ArgumentCaptor.forClass(TemplateVersionEntity.class);
        verify(templateVersionMapper).insert(versionCaptor.capture());
        assertThat(versionCaptor.getValue().getOwnerId()).isEqualTo(10L);
        assertThat(versionCaptor.getValue().getVersionNo()).isEqualTo(3);
        verify(templateMapper).updateCurrentVersionNo(100L, 3);
    }

    @Test
    void getTemplateSchemaReturnsSchemaForBeA() {
        when(templateVersionMapper.selectById(200L)).thenReturn(ownerVersion(200L, 100L, 10L, 1, true));
        when(templateMapper.selectById(100L)).thenReturn(ownerTemplate(100L, 10L, 1));

        var response = versionService.getTemplateSchema(200L);

        assertThat(response.versionId()).isEqualTo(200L);
        assertThat(response.ownerId()).isEqualTo(10L);
        assertThat(response.schemaJson().get("components").isArray()).isTrue();
    }

    @Test
    void getTemplateSchemaFailsWhenVersionMissing() {
        when(templateVersionMapper.selectById(200L)).thenReturn(null);

        assertThatThrownBy(() -> versionService.getTemplateSchema(200L))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(400102);
    }

    private void stubTask(Long ownerId) {
        Task task = new Task();
        task.setId(1L);
        task.setOwnerId(ownerId);
        task.setStatus(TaskStatus.DRAFT);
        when(taskMapper.selectById(1L)).thenReturn(task);
    }

    private void stubInsertIds() {
        AtomicLong ids = new AtomicLong(100L);
        when(templateMapper.insert(any(TemplateEntity.class))).thenAnswer(invocation -> {
            TemplateEntity entity = invocation.getArgument(0);
            entity.setId(ids.getAndIncrement());
            return 1;
        });
        when(templateVersionMapper.insert(any(TemplateVersionEntity.class))).thenAnswer(invocation -> {
            TemplateVersionEntity entity = invocation.getArgument(0);
            entity.setId(ids.getAndIncrement());
            return 1;
        });
    }

    private TemplateEntity template(Long id, Long taskId, int currentVersionNo) {
        TemplateEntity template = new TemplateEntity();
        template.setId(id);
        template.setOwnerId(10L);
        template.setTaskId(taskId);
        template.setOwnerId(10L);
        template.setName("质检模板");
        template.setCurrentVersionNo(currentVersionNo);
        template.setCreatedBy(10L);
        return template;
    }

    private TemplateEntity ownerTemplate(Long id, Long ownerId, int currentVersionNo) {
        TemplateEntity template = new TemplateEntity();
        template.setId(id);
        template.setOwnerId(ownerId);
        template.setName("通用质检模板");
        template.setCurrentVersionNo(currentVersionNo);
        template.setCreatedBy(ownerId);
        return template;
    }

    private TemplateVersionEntity version(Long id, Long templateId, Long taskId, int versionNo, boolean publishedSnapshot) {
        TemplateVersionEntity version = new TemplateVersionEntity();
        version.setId(id);
        version.setTemplateId(templateId);
        version.setOwnerId(10L);
        version.setTaskId(taskId);
        version.setOwnerId(10L);
        version.setVersionNo(versionNo);
        version.setSchemaJson("{\"components\":[{\"type\":\"Input\",\"field\":\"answer\"}]}");
        version.setPublishedSnapshot(publishedSnapshot);
        version.setChangeNote("版本说明");
        version.setCreatedBy(10L);
        return version;
    }

    private TemplateVersionEntity ownerVersion(Long id, Long templateId, Long ownerId, int versionNo, boolean publishedSnapshot) {
        TemplateVersionEntity version = version(id, templateId, null, versionNo, publishedSnapshot);
        version.setOwnerId(ownerId);
        return version;
    }
}
