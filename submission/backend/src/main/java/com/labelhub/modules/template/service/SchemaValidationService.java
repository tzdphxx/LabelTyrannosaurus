package com.labelhub.modules.template.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.modules.template.dto.SchemaValidationError;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 模板 schema 与答案 JSON 的统一校验服务。
 *
 * <p>保存模板版本时通过 {@link TemplateSchemaValidator} 校验 schema；BE-A 提交答案时通过
 * {@link AnswerSchemaValidator} 入口复用同一套字段级规则。</p>
 */
@Service
@Primary
public class SchemaValidationService implements TemplateSchemaValidator, AnswerSchemaValidator {

    private static final int INVALID_ANSWER_JSON = 400402;

    private final ObjectMapper objectMapper;
    private final TemplateVersionService templateVersionService;

    public SchemaValidationService(ObjectMapper objectMapper, TemplateVersionService templateVersionService) {
        this.objectMapper = objectMapper;
        this.templateVersionService = templateVersionService;
    }

    /**
     * 校验待保存 schema 的结构和组件规则。
     */
    @Override
    public void validateSchema(String schemaJson) {
        SchemaRules rules = parseSchema(readSchema(schemaJson));
        if (!rules.errors().isEmpty()) {
            SchemaValidationError first = rules.errors().get(0);
            throw new BusinessException(409301, first.path() + " " + first.errorMessage());
        }
    }

    /**
     * 校验指定模板版本下的答案字段，返回所有可定位的错误。
     */
    public List<SchemaValidationError> validateAnswer(Long schemaVersionId, Map<String, Object> answerJson) {
        JsonNode schema = templateVersionService.getVersion(schemaVersionId).schemaJson();
        return validateAnswerAgainstSchema(schema, answerJson);
    }

    /**
     * BE-A 提交主链路使用的校验入口。
     *
     * <p>提交链路已经通过 assignment 校验了 labeler 对题目的访问权限，因此这里读取内部
     * schema 快照，避免复用 Owner 侧 schema 查询权限导致标注员提交被误拒绝。</p>
     */
    @Override
    public void validateAnswer(Long templateVersionId, String answerJson) {
        Map<String, Object> parsedAnswer = readAnswerObject(answerJson);
        JsonNode schema = templateVersionService.getTemplateSchema(templateVersionId).schemaJson();
        List<SchemaValidationError> errors = validateAnswerAgainstSchema(schema, parsedAnswer);
        if (!errors.isEmpty()) {
            SchemaValidationError first = errors.get(0);
            throw new BusinessException(INVALID_ANSWER_JSON,
                    first.path() + " " + first.errorMessage());
        }
    }

    private List<SchemaValidationError> validateAnswerAgainstSchema(JsonNode schema, Map<String, Object> answerJson) {
        SchemaRules rules = parseSchema(schema);
        if (!rules.errors().isEmpty()) {
            SchemaValidationError first = rules.errors().get(0);
            throw new BusinessException(409301, first.path() + " " + first.errorMessage());
        }

        JsonNode answer = objectMapper.valueToTree(answerJson == null ? Map.of() : answerJson);
        List<SchemaValidationError> errors = new ArrayList<>();
        if (!answer.isObject()) {
            errors.add(SchemaValidationError.of("/", "answerJson 必须是 JSON 对象"));
            return errors;
        }

        for (String field : rules.showItemFields()) {
            if (answer.has(field)) {
                errors.add(SchemaValidationError.of(answerPath(field), "ShowItem 字段不允许出现在作答内容中"));
            }
        }
        for (ComponentRule rule : rules.answerRules()) {
            validateFieldAnswer(rule, answer, errors);
        }
        return errors;
    }

    private Map<String, Object> readAnswerObject(String answerJson) {
        try {
            Map<String, Object> parsed = objectMapper.readValue(answerJson,
                    new TypeReference<Map<String, Object>>() {
                    });
            return parsed == null ? Map.of() : parsed;
        } catch (JsonProcessingException ex) {
            throw new BusinessException(INVALID_ANSWER_JSON, "作答 JSON 格式不合法");
        }
    }

    private void validateFieldAnswer(ComponentRule rule, JsonNode answer, List<SchemaValidationError> errors) {
        JsonNode value = answer.get(rule.field());
        if (isMissing(value)) {
            if (rule.required()) {
                errors.add(SchemaValidationError.of(answerPath(rule.field()), "必填答案缺失"));
            }
            return;
        }
        if (!rule.enumValues().isEmpty() && rule.enumValues().stream().noneMatch(option -> option.equals(value))) {
            errors.add(SchemaValidationError.of(answerPath(rule.field()), "答案不在可选值范围内"));
        }
        if (rule.regexPattern() != null) {
            if (!value.isTextual() || !rule.regexPattern().matcher(value.asText()).matches()) {
                errors.add(SchemaValidationError.of(answerPath(rule.field()), "答案不符合正则规则"));
            }
        }
    }

    private boolean isMissing(JsonNode value) {
        return value == null
                || value.isNull()
                || (value.isTextual() && !StringUtils.hasText(value.asText()));
    }

    private JsonNode readSchema(String schemaJson) {
        try {
            JsonNode schema = objectMapper.readTree(schemaJson);
            if (!schema.isObject()) {
                throw new BusinessException(409301, "Schema 必须是 JSON 对象");
            }
            return schema;
        } catch (JsonProcessingException ex) {
            throw new BusinessException(409301, "Schema JSON 格式不合法");
        }
    }

    private SchemaRules parseSchema(JsonNode schema) {
        List<SchemaValidationError> errors = new ArrayList<>();
        List<ComponentRule> answerRules = new ArrayList<>();
        Set<String> showItemFields = new HashSet<>();
        Map<String, String> fieldPaths = new HashMap<>();

        JsonNode components = schema.get("components");
        if (components == null || !components.isArray()) {
            errors.add(SchemaValidationError.of("/components", "components 必须是 JSON 数组"));
            return new SchemaRules(answerRules, showItemFields, errors);
        }

        collectComponents(components, "/components", fieldPaths, answerRules, showItemFields, errors);
        return new SchemaRules(answerRules, showItemFields, errors);
    }

    private void collectComponents(JsonNode components,
                                   String componentsPath,
                                   Map<String, String> fieldPaths,
                                   List<ComponentRule> answerRules,
                                   Set<String> showItemFields,
                                   List<SchemaValidationError> errors) {
        for (int i = 0; i < components.size(); i++) {
            JsonNode component = components.get(i);
            String componentPath = componentsPath + "/" + i;
            if (!component.isObject()) {
                errors.add(SchemaValidationError.of(componentPath, "component 必须是 JSON 对象"));
                continue;
            }
            collectComponent(component, componentPath, fieldPaths, answerRules, showItemFields, errors);
        }
    }

    private void collectComponent(JsonNode component,
                                  String componentPath,
                                  Map<String, String> fieldPaths,
                                  List<ComponentRule> answerRules,
                                  Set<String> showItemFields,
                                  List<SchemaValidationError> errors) {
        String type = textValue(component.get("type"));
        if (!StringUtils.hasText(type)) {
            errors.add(SchemaValidationError.of(componentPath + "/type", "component type 不能为空"));
        }

        String field = textValue(component.get("field"));
        boolean showItem = "ShowItem".equals(type);
        if (!showItem && !StringUtils.hasText(field) && !hasNestedComponents(component)) {
            errors.add(SchemaValidationError.of(componentPath + "/field", "component field 不能为空"));
        }
        if (StringUtils.hasText(field)) {
            detectDuplicateField(field, componentPath, fieldPaths, errors);
            if (showItem) {
                showItemFields.add(field);
            } else {
                answerRules.add(componentRule(component, componentPath, type, field, errors));
            }
        }

        validateNestedComponents(component, componentPath, "children", fieldPaths, answerRules, showItemFields, errors);
        validateNestedComponents(component, componentPath, "components", fieldPaths, answerRules, showItemFields, errors);
    }

    private boolean hasNestedComponents(JsonNode component) {
        return component.has("children") || component.has("components");
    }

    private void detectDuplicateField(String field,
                                      String componentPath,
                                      Map<String, String> fieldPaths,
                                      List<SchemaValidationError> errors) {
        String previousPath = fieldPaths.putIfAbsent(field, componentPath + "/field");
        if (previousPath != null) {
            errors.add(SchemaValidationError.of(componentPath + "/field", "字段重复：" + field));
        }
    }

    private ComponentRule componentRule(JsonNode component,
                                        String componentPath,
                                        String type,
                                        String field,
                                        List<SchemaValidationError> errors) {
        boolean required = false;
        JsonNode requiredNode = component.get("required");
        if (requiredNode != null) {
            if (requiredNode.isBoolean()) {
                required = requiredNode.asBoolean();
            } else {
                errors.add(SchemaValidationError.of(componentPath + "/required", "required 必须是布尔值"));
            }
        }

        List<JsonNode> enumValues = new ArrayList<>();
        JsonNode enumNode = component.get("enum");
        if (enumNode != null) {
            if (enumNode.isArray()) {
                enumNode.forEach(value -> enumValues.add(value.deepCopy()));
            } else {
                errors.add(SchemaValidationError.of(componentPath + "/enum", "enum 必须是 JSON 数组"));
            }
        }

        Pattern regexPattern = null;
        JsonNode regexNode = component.get("regex");
        if (regexNode != null) {
            if (regexNode.isTextual()) {
                try {
                    regexPattern = Pattern.compile(regexNode.asText());
                } catch (PatternSyntaxException ex) {
                    errors.add(SchemaValidationError.of(componentPath + "/regex", "regex 不合法"));
                }
            } else {
                errors.add(SchemaValidationError.of(componentPath + "/regex", "regex 必须是字符串"));
            }
        }
        return new ComponentRule(type, field, required, enumValues, regexPattern);
    }

    private void validateNestedComponents(JsonNode component,
                                          String componentPath,
                                          String childKey,
                                          Map<String, String> fieldPaths,
                                          List<ComponentRule> answerRules,
                                          Set<String> showItemFields,
                                          List<SchemaValidationError> errors) {
        JsonNode children = component.get(childKey);
        if (children == null) {
            return;
        }
        String childPath = componentPath + "/" + childKey;
        if (!children.isArray()) {
            errors.add(SchemaValidationError.of(childPath, childKey + " 必须是 JSON 数组"));
            return;
        }
        collectComponents(children, childPath, fieldPaths, answerRules, showItemFields, errors);
    }

    private String textValue(JsonNode node) {
        return node != null && node.isTextual() ? node.asText() : null;
    }

    private String answerPath(String field) {
        return "/" + field.replace("~", "~0").replace("/", "~1");
    }

    private record SchemaRules(List<ComponentRule> answerRules,
                               Set<String> showItemFields,
                               List<SchemaValidationError> errors) {
    }

    private record ComponentRule(String type,
                                 String field,
                                 boolean required,
                                 List<JsonNode> enumValues,
                                 Pattern regexPattern) {
    }
}
