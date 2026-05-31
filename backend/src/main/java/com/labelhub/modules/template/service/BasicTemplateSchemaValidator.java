package com.labelhub.modules.template.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelhub.common.exception.BusinessException;
import org.springframework.stereotype.Service;

/**
 * Task7 的最小 schema 校验实现。
 *
 * <p>当前只保证 schema 是合法 JSON object，避免模板版本保存不可解析内容；字段级规则由 Task8 扩展。</p>
 */
@Service
public class BasicTemplateSchemaValidator implements TemplateSchemaValidator {

    private final ObjectMapper objectMapper;

    public BasicTemplateSchemaValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void validateSchema(String schemaJson) {
        try {
            if (!objectMapper.readTree(schemaJson).isObject()) {
                throw new BusinessException(409301, "Schema must be a JSON object");
            }
        } catch (JsonProcessingException ex) {
            throw new BusinessException(409301, "Invalid schema JSON");
        }
    }
}
