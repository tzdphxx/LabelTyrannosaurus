package com.labelhub.modules.admin.service;

import com.labelhub.common.api.PageResponse;
import com.labelhub.modules.admin.dto.AdminClaimedReviewTaskRow;
import com.labelhub.modules.admin.dto.AssignableReviewTaskResponse;
import com.labelhub.modules.admin.dto.AssignableReviewerResponse;
import com.labelhub.modules.admin.dto.ClaimedReviewTaskResponse;
import com.labelhub.modules.admin.dto.ReviewerProgressResponse;
import com.labelhub.modules.admin.mapper.AdminReviewAssignmentMapper;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class AdminReviewAssignmentQueryService {

    private static final int MAX_PAGE_SIZE = 100;

    private final AdminReviewAssignmentMapper mapper;

    public AdminReviewAssignmentQueryService(AdminReviewAssignmentMapper mapper) {
        this.mapper = mapper;
    }

    public PageResponse<AssignableReviewTaskResponse> listAssignableTasks(Long taskId,
                                                                          String keyword,
                                                                          Integer reviewLevel,
                                                                          boolean includeClaimed,
                                                                          int page,
                                                                          int size) {
        int safePage = safePage(page);
        int safeSize = safeSize(size);
        int offset = (safePage - 1) * safeSize;
        String normalizedKeyword = normalize(keyword);
        long total = mapper.countAssignableTasks(taskId, normalizedKeyword, reviewLevel, includeClaimed);
        List<AssignableReviewTaskResponse> items = mapper.selectAssignableTasks(
                taskId, normalizedKeyword, reviewLevel, includeClaimed, offset, safeSize);
        return new PageResponse<>(items, safePage, safeSize, total);
    }

    public PageResponse<AssignableReviewerResponse> listAssignableReviewers(String keyword,
                                                                            boolean enabledOnly,
                                                                            int page,
                                                                            int size) {
        int safePage = safePage(page);
        int safeSize = safeSize(size);
        int offset = (safePage - 1) * safeSize;
        String normalizedKeyword = normalize(keyword);
        long total = mapper.countAssignableReviewers(normalizedKeyword, enabledOnly);
        List<AssignableReviewerResponse> items = mapper.selectAssignableReviewers(
                normalizedKeyword, enabledOnly, offset, safeSize);
        return new PageResponse<>(items, safePage, safeSize, total);
    }

    public List<ReviewerProgressResponse> listReviewerProgress(String keyword, boolean enabledOnly) {
        String normalizedKeyword = normalize(keyword);
        List<ReviewerProgressResponse> reviewers = mapper.selectReviewerProgress(normalizedKeyword, enabledOnly);
        if (reviewers.isEmpty()) {
            return reviewers;
        }
        List<Long> reviewerIds = reviewers.stream()
                .map(ReviewerProgressResponse::reviewerId)
                .toList();
        Map<Long, List<ClaimedReviewTaskResponse>> tasksByReviewer = mapper.selectClaimedTasksByReviewerIds(reviewerIds)
                .stream()
                .collect(Collectors.groupingBy(
                        AdminClaimedReviewTaskRow::reviewerId,
                        Collectors.mapping(AdminClaimedReviewTaskRow::toResponse, Collectors.toList())));
        return reviewers.stream()
                .map(reviewer -> reviewer.withClaimedTasks(
                        tasksByReviewer.getOrDefault(reviewer.reviewerId(), List.of())))
                .toList();
    }

    private int safePage(int page) {
        return Math.max(1, page);
    }

    private int safeSize(int size) {
        return Math.max(1, Math.min(size, MAX_PAGE_SIZE));
    }

    private String normalize(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim();
    }
}
