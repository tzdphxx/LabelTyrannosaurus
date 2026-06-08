package com.labelhub.modules.task.service;

import com.labelhub.common.api.PageResponse;
import com.labelhub.modules.auth.repository.UserMapper;
import com.labelhub.modules.task.dto.AssignableLabelerResponse;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OwnerAssignableLabelerService {

    private static final int MAX_PAGE_SIZE = 100;

    private final UserMapper userMapper;

    public OwnerAssignableLabelerService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public PageResponse<AssignableLabelerResponse> listAssignableLabelers(String keyword,
                                                                          boolean enabledOnly,
                                                                          int page,
                                                                          int size) {
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        int offset = (safePage - 1) * safeSize;
        String normalizedKeyword = normalize(keyword);
        long total = userMapper.countAssignableLabelers(normalizedKeyword, enabledOnly);
        List<AssignableLabelerResponse> items = userMapper.selectAssignableLabelers(
                normalizedKeyword, enabledOnly, offset, safeSize);
        return new PageResponse<>(items, safePage, safeSize, total);
    }

    private String normalize(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim();
    }
}
