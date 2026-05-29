package com.labelhub.modules.template.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.common.security.CurrentUser;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.task.domain.TaskEntity;
import com.labelhub.modules.task.repository.TaskMapper;
import com.labelhub.modules.template.domain.TemplateEntity;
import com.labelhub.modules.template.domain.TemplateVersionEntity;
import com.labelhub.modules.template.dto.CreateTemplateRequest;
import com.labelhub.modules.template.dto.ForkTemplateRequest;
import com.labelhub.modules.template.dto.TemplateResponse;
import com.labelhub.modules.template.dto.TemplateVersionResponse;
import com.labelhub.modules.template.repository.TemplateMapper;
import com.labelhub.modules.template.repository.TemplateVersionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 模板管理应用服务。
 *
 * <p>BE-B 负责 schema 存储和版本递增。发布任务时冻结哪个 templateVersionId 属于 BE-A，
 * 因此本服务不会写 {@code tasks.published_template_version_id}。</p>
 */
@Service
public class TemplateService {

    private final TaskMapper taskMapper;
    private final TemplateMapper templateMapper;
    private final TemplateVersionMapper templateVersionMapper;
    private final TemplateSchemaValidator schemaValidator;
    private final TemplateVersionService templateVersionService;
    private final ObjectMapper objectMapper;

    public TemplateService(TaskMapper taskMapper,
                           TemplateMapper templateMapper,
                           TemplateVersionMapper templateVersionMapper,
                           TemplateSchemaValidator schemaValidator,
                           TemplateVersionService templateVersionService,
                           ObjectMapper objectMapper) {
        this.taskMapper = taskMapper;
        this.templateMapper = templateMapper;
        this.templateVersionMapper = templateVersionMapper;
        this.schemaValidator = schemaValidator;
        this.templateVersionService = templateVersionService;
        this.objectMapper = objectMapper;
    }

    /**
     * 创建模板并生成首个版本。
     */
    @Transactional
    public TemplateResponse createTemplate(Long taskId, CreateTemplateRequest request) {
        TaskEntity task = requireOwnedTask(taskId);
        CurrentUser actor = CurrentUserContext.requireCurrentUser();
        String schemaJson = writeJson(request.schemaJson());
        schemaValidator.validateSchema(schemaJson);

        TemplateEntity template = new TemplateEntity();
        template.setTaskId(task.getId());
        template.setName(request.name());
        template.setCurrentVersionNo(1);
        template.setCreatedBy(actor.userId());
        templateMapper.insert(template);

        TemplateVersionEntity version = newVersion(template.getId(), task.getId(), 1, schemaJson,
                request.changeNote(), actor.userId());
        templateVersionMapper.insert(version);
        return toResponse(template, version);
    }

    /**
     * 查询任务下模板列表，并附带每个模板当前版本概要。
     */
    public List<TemplateResponse> listTemplates(Long taskId) {
        requireOwnedTask(taskId);
        return templateMapper.selectByTaskId(taskId).stream()
                .map(template -> toResponse(template, currentVersion(template)))
                .toList();
    }

    /**
     * 基于已有版本 fork 新版本。旧版本始终保持不可变。
     */
    @Transactional
    public TemplateResponse forkTemplate(Long templateId, ForkTemplateRequest request) {
        TemplateEntity template = requireTemplate(templateId);
        requireOwnedTask(template.getTaskId());
        CurrentUser actor = CurrentUserContext.requireCurrentUser();
        TemplateVersionEntity baseVersion = resolveBaseVersion(template, request.baseVersionId());
        String schemaJson = request.schemaJson() == null ? baseVersion.getSchemaJson() : writeJson(request.schemaJson());
        schemaValidator.validateSchema(schemaJson);

        int nextVersionNo = template.getCurrentVersionNo() + 1;
        TemplateVersionEntity newVersion = newVersion(template.getId(), template.getTaskId(), nextVersionNo,
                schemaJson, request.changeNote(), actor.userId());
        templateVersionMapper.insert(newVersion);
        templateMapper.updateCurrentVersionNo(template.getId(), nextVersionNo);
        template.setCurrentVersionNo(nextVersionNo);
        return toResponse(template, newVersion);
    }

    private TemplateVersionEntity resolveBaseVersion(TemplateEntity template, Long baseVersionId) {
        TemplateVersionEntity baseVersion = baseVersionId == null
                ? currentVersion(template)
                : templateVersionMapper.selectById(baseVersionId);
        if (baseVersion == null || !template.getId().equals(baseVersion.getTemplateId())) {
            throw new BusinessException(400102, "Template version not found");
        }
        return baseVersion;
    }

    private TemplateVersionEntity currentVersion(TemplateEntity template) {
        TemplateVersionEntity version = templateVersionMapper.selectByTemplateIdAndVersionNo(
                template.getId(), template.getCurrentVersionNo());
        if (version == null) {
            throw new BusinessException(400102, "Template current version not found");
        }
        return version;
    }

    private TemplateEntity requireTemplate(Long templateId) {
        TemplateEntity template = templateMapper.selectById(templateId);
        if (template == null) {
            throw new BusinessException(400102, "Template not found");
        }
        return template;
    }

    private TaskEntity requireOwnedTask(Long taskId) {
        CurrentUser currentUser = CurrentUserContext.requireCurrentUser();
        TaskEntity task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(400102, "Task not found");
        }
        if (!currentUser.roles().contains(RoleCode.ADMIN) && !currentUser.userId().equals(task.getOwnerId())) {
            throw new BusinessException(403001, "Forbidden");
        }
        return task;
    }

    private TemplateVersionEntity newVersion(Long templateId,
                                             Long taskId,
                                             Integer versionNo,
                                             String schemaJson,
                                             String changeNote,
                                             Long actorId) {
        TemplateVersionEntity version = new TemplateVersionEntity();
        version.setTemplateId(templateId);
        version.setTaskId(taskId);
        version.setVersionNo(versionNo);
        version.setSchemaJson(schemaJson);
        version.setPublishedSnapshot(false);
        version.setChangeNote(changeNote);
        version.setCreatedBy(actorId);
        return version;
    }

    private TemplateResponse toResponse(TemplateEntity template, TemplateVersionEntity currentVersion) {
        TemplateVersionResponse versionResponse = templateVersionService.toResponse(currentVersion);
        return new TemplateResponse(
                template.getId(),
                template.getTaskId(),
                template.getName(),
                template.getCurrentVersionNo(),
                versionResponse,
                template.getCreatedBy(),
                template.getCreatedAt(),
                template.getUpdatedAt()
        );
    }

    private String writeJson(Map<String, Object> schemaJson) {
        try {
            return objectMapper.writeValueAsString(schemaJson == null ? Map.of() : schemaJson);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(400102, "Invalid schema payload");
        }
    }
}
