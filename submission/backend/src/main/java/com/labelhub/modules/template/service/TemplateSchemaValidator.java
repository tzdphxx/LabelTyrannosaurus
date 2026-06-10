package com.labelhub.modules.template.service;

/**
 * 模板 schema 校验端口。
 *
 * <p>Task7 先通过该端口保证保存前存在校验动作；Task8 会把完整布局、组件和答案规则校验落到这里。</p>
 */
public interface TemplateSchemaValidator {

    /**
     * 校验待保存的 schema JSON 字符串。
     */
    void validateSchema(String schemaJson);
}
