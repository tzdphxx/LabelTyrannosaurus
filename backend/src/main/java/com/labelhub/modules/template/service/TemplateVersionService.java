package com.labelhub.modules.template.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.common.security.CurrentUser;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.template.domain.TemplateEntity;
import com.labelhub.modules.template.domain.TemplateVersionEntity;
import com.labelhub.modules.template.dto.TemplateSchemaResponse;
import com.labelhub.modules.template.dto.TemplateVersionResponse;
import com.labelhub.modules.template.repository.TemplateMapper;
import com.labelhub.modules.template.repository.TemplateVersionRepositoryMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 模板版本读取服务。
 *
 * <p>该服务承接 HTTP 查询和 BE-A 内部读取 schema 的需求。模板版本归属于 OWNER；
 * BE-B 不在这里修改任务发布引用，只按 versionId 返回稳定 schema 快照。</p>
 */
@Service
public class TemplateVersionService {

    private final TemplateMapper templateMapper;
    private final TemplateVersionRepositoryMapper templateVersionMapper;
    private final ObjectMapper objectMapper;

    public TemplateVersionService(TemplateMapper templateMapper,
                                  TemplateVersionRepositoryMapper templateVersionMapper,
                                  ObjectMapper objectMapper) {
        this.templateMapper = templateMapper;
        this.templateVersionMapper = templateVersionMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 查询模板版本详情，并校验当前用户对任务的访问权限。
     */
    public TemplateVersionResponse getVersion(Long versionId) {
        TemplateVersionEntity version = requireVersion(versionId);
        requireOwnedTemplate(version);
        return toResponse(version);
    }

    /**
     * 给 BE-A 使用的 schema 快照读取入口，不依赖当前登录态。
     */
    public TemplateSchemaResponse getTemplateSchema(Long templateVersionId) {
        TemplateVersionEntity version = requireVersion(templateVersionId);
        Long ownerId = resolveOwnerId(version);
        return new TemplateSchemaResponse(
                version.getId(),
                version.getTemplateId(),
                ownerId,
                version.getVersionNo(),
                readJson(version.getSchemaJson()),
                version.getPublishedSnapshot()
        );
    }

    /**
     * 任务发布后冻结该模板版本的发布快照标记。
     */
    @Transactional
    public void markPublishedSnapshot(Long templateVersionId) {
        requireVersion(templateVersionId);
        templateVersionMapper.markPublishedSnapshot(templateVersionId);
    }

    TemplateVersionResponse toResponse(TemplateVersionEntity version) {
        return new TemplateVersionResponse(
                version.getId(),
                version.getTemplateId(),
                version.getTaskId(),
                resolveOwnerId(version),
                version.getVersionNo(),
                readJson(version.getSchemaJson()),
                version.getPublishedSnapshot(),
                version.state(),
                version.getChangeNote(),
                version.getCreatedBy(),
                version.getCreatedAt()
        );
    }

    private TemplateVersionEntity requireVersion(Long versionId) {
        TemplateVersionEntity version = templateVersionMapper.selectById(versionId);
        if (version == null) {
            throw new BusinessException(400102, "模板版本不存在");
        }
        return version;
    }

    private void requireOwnedTemplate(TemplateVersionEntity version) {
        CurrentUser currentUser = CurrentUserContext.requireCurrentUser();
        Long ownerId = resolveOwnerId(version);
        if (!currentUser.roles().contains(RoleCode.ADMIN) && !currentUser.userId().equals(ownerId)) {
            throw new BusinessException(403001, "当前账号没有权限执行该操作");
        }
    }

    private Long resolveOwnerId(TemplateVersionEntity version) {
        if (version.getOwnerId() != null) {
            return version.getOwnerId();
        }
        TemplateEntity template = templateMapper.selectById(version.getTemplateId());
        if (template == null) {
            throw new BusinessException(400102, "模板不存在");
        }
        return template.getOwnerId();
    }

    private JsonNode readJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(500001, "已存储的模板 Schema 不合法");
        }
    }
}
