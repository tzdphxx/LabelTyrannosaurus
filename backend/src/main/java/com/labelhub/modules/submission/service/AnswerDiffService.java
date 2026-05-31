package com.labelhub.modules.submission.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.modules.submission.domain.Submission;
import com.labelhub.modules.submission.dto.AnswerDiffResponse;
import com.labelhub.modules.submission.dto.FieldDiff;
import com.labelhub.modules.submission.mapper.SubmissionMapper;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class AnswerDiffService {

    private static final int NOT_FOUND = 404801;
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private final SubmissionMapper submissionMapper;
    private final ObjectMapper objectMapper;

    public AnswerDiffService(SubmissionMapper submissionMapper, ObjectMapper objectMapper) {
        this.submissionMapper = submissionMapper;
        this.objectMapper = objectMapper;
    }

    public AnswerDiffResponse diff(Long submissionId, Integer baseVersionNo) {
        Submission target = submissionMapper.selectById(submissionId);
        if (target == null) {
            throw new BusinessException(NOT_FOUND, "Submission not found");
        }
        List<Submission> versions = submissionMapper.selectByAssignmentId(target.getAssignmentId());
        Submission base = versions.stream()
                .filter(s -> s.getVersionNo().equals(baseVersionNo))
                .findFirst()
                .orElse(null);
        if (base == null) {
            throw new BusinessException(NOT_FOUND, "Base version not found");
        }

        Map<String, Object> baseMap = parseAnswer(base.getAnswerJson());
        Map<String, Object> targetMap = parseAnswer(target.getAnswerJson());
        List<FieldDiff> diffs = computeDiffs("", baseMap, targetMap);

        return new AnswerDiffResponse(
                base.getId(), base.getVersionNo(),
                target.getId(), target.getVersionNo(),
                diffs);
    }

    @SuppressWarnings("unchecked")
    private List<FieldDiff> computeDiffs(String prefix, Map<String, Object> base, Map<String, Object> target) {
        List<FieldDiff> diffs = new ArrayList<>();
        Set<String> allKeys = new LinkedHashSet<>();
        allKeys.addAll(base.keySet());
        allKeys.addAll(target.keySet());

        for (String key : allKeys) {
            String path = prefix.isEmpty() ? key : prefix + "." + key;
            boolean inBase = base.containsKey(key);
            boolean inTarget = target.containsKey(key);

            if (!inBase) {
                diffs.add(new FieldDiff(path, null, target.get(key), FieldDiff.ChangeType.ADDED));
            } else if (!inTarget) {
                diffs.add(new FieldDiff(path, base.get(key), null, FieldDiff.ChangeType.REMOVED));
            } else {
                Object bVal = base.get(key);
                Object tVal = target.get(key);
                if (bVal instanceof Map && tVal instanceof Map) {
                    diffs.addAll(computeDiffs(path, (Map<String, Object>) bVal, (Map<String, Object>) tVal));
                } else if (!Objects.equals(bVal, tVal)) {
                    diffs.add(new FieldDiff(path, bVal, tVal, FieldDiff.ChangeType.MODIFIED));
                }
            }
        }
        return diffs;
    }

    private Map<String, Object> parseAnswer(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception e) {
            return Map.of();
        }
    }
}