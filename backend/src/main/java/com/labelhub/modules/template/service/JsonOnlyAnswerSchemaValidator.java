package com.labelhub.modules.template.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelhub.common.exception.BusinessException;
import org.springframework.stereotype.Service;

@Service
public class JsonOnlyAnswerSchemaValidator implements AnswerSchemaValidator {

    private static final int INVALID_ANSWER_JSON = 400402;

    private final ObjectMapper objectMapper;

    public JsonOnlyAnswerSchemaValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void validateAnswer(Long templateVersionId, String answerJson) {
        try {
            objectMapper.readTree(answerJson);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(INVALID_ANSWER_JSON, "Answer JSON is invalid");
        }
    }
}
