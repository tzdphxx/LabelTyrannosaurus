package com.labelhub.modules.template.service;

public interface AnswerSchemaValidator {

    void validateAnswer(Long templateVersionId, String answerJson);
}
