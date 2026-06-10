package com.labelhub.modules.template.service;

import com.labelhub.common.exception.BusinessException;
import com.labelhub.infrastructure.redis.RedisKeyBuilder;
import com.labelhub.modules.template.domain.TemplateVersion;
import com.labelhub.modules.template.mapper.TemplateVersionMapper;
import java.util.concurrent.TimeUnit;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DefaultTemplateSchemaService implements TemplateSchemaService {

    private static final int TEMPLATE_VERSION_NOT_FOUND = 404201;
    private static final long TEMPLATE_SCHEMA_CACHE_TTL_HOURS = 6L;

    private final TemplateVersionMapper templateVersionMapper;
    private final RedissonClient redissonClient;

    @Autowired
    public DefaultTemplateSchemaService(TemplateVersionMapper templateVersionMapper,
                                        RedissonClient redissonClient) {
        this.templateVersionMapper = templateVersionMapper;
        this.redissonClient = redissonClient;
    }

    public DefaultTemplateSchemaService(TemplateVersionMapper templateVersionMapper) {
        this(templateVersionMapper, null);
    }

    @Override
    public TemplateSchemaSnapshot getTemplateSchema(Long templateVersionId) {
        if (redissonClient == null) {
            return loadTemplateSchema(templateVersionId);
        }
        RBucket<TemplateSchemaSnapshot> bucket = redissonClient.getBucket(
                RedisKeyBuilder.templateSchema(templateVersionId));
        TemplateSchemaSnapshot cached = bucket.get();
        if (cached != null) {
            return cached;
        }
        TemplateSchemaSnapshot snapshot = loadTemplateSchema(templateVersionId);
        bucket.set(snapshot, TEMPLATE_SCHEMA_CACHE_TTL_HOURS, TimeUnit.HOURS);
        return snapshot;
    }

    private TemplateSchemaSnapshot loadTemplateSchema(Long templateVersionId) {
        TemplateVersion templateVersion = templateVersionMapper.selectById(templateVersionId);
        if (templateVersion == null) {
            throw new BusinessException(TEMPLATE_VERSION_NOT_FOUND, "模板版本不存在");
        }
        return new TemplateSchemaSnapshot(templateVersion.getId(), templateVersion.getSchemaJson());
    }
}
