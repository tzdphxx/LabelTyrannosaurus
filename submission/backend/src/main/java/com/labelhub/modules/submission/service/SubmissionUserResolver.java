package com.labelhub.modules.submission.service;

import com.labelhub.modules.auth.domain.UserEntity;
import com.labelhub.modules.auth.repository.UserMapper;
import com.labelhub.modules.submission.domain.Submission;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

/**
 * Resolves user display names for submission versions.
 * <p>
 * For each submission version, determines the effective creator:
 * if {@code createdBy} is set (reviewer revision), uses that ID;
 * otherwise falls back to {@code labelerId} (original labeler).
 * </p>
 */
@Component
public class SubmissionUserResolver {

    private final UserMapper userMapper;

    public SubmissionUserResolver(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /**
     * Build a map from user ID to display name for all creators
     * of the given submissions. Collects both {@code createdBy} and
     * {@code labelerId} so both labeler-created and reviewer-revised
     * versions have non-null creator names.
     */
    public Map<Long, String> resolveCreatorNames(List<Submission> versions) {
        Set<Long> userIds = versions.stream()
                .flatMap(s -> Stream.of(s.getCreatedBy(), s.getLabelerId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<UserEntity> users = userMapper.selectBatchIds(userIds);
        // displayName 可空（本系统大量用户未设置），Collectors.toMap 遇到 null value 会抛 NPE，
        // 因此手动构建并回退到 username（非空唯一），保证每个 creator 都有可显示名称。
        Map<Long, String> names = new HashMap<>();
        for (UserEntity user : users) {
            String displayName = user.getDisplayName() != null ? user.getDisplayName() : user.getUsername();
            names.put(user.getId(), displayName);
        }
        return names;
    }

    /**
     * Return the display name for the creator of a single submission version.
     * Falls back to labelerId when createdBy is null.
     */
    public String resolveCreatorName(Submission submission, Map<Long, String> userNames) {
        Long creatorId = submission.getCreatedBy() != null
                ? submission.getCreatedBy()
                : submission.getLabelerId();
        return creatorId != null ? userNames.get(creatorId) : null;
    }

    /**
     * Return the effective creator ID for a submission version
     * (createdBy if set, otherwise labelerId).
     */
    public Long effectiveCreatorId(Submission submission) {
        return submission.getCreatedBy() != null
                ? submission.getCreatedBy()
                : submission.getLabelerId();
    }
}
