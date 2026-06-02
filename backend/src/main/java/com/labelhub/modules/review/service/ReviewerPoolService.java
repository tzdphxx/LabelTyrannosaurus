package com.labelhub.modules.review.service;

import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.auth.domain.UserEntity;
import com.labelhub.modules.auth.domain.UserRoleEntity;
import com.labelhub.modules.auth.repository.UserMapper;
import com.labelhub.modules.auth.repository.UserRoleMapper;
import com.labelhub.modules.review.service.ReviewAutoAssignScheduler.ReviewerLoad;
import com.labelhub.modules.submission.mapper.SubmissionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ReviewerPoolService {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final SubmissionMapper submissionMapper;

    public ReviewerPoolService(UserMapper userMapper,
                               UserRoleMapper userRoleMapper,
                               SubmissionMapper submissionMapper) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.submissionMapper = submissionMapper;
    }

    public List<ReviewerLoad> getActiveReviewersWithLoad() {
        LambdaQueryWrapper<UserRoleEntity> roleQuery = new LambdaQueryWrapper<UserRoleEntity>()
                .eq(UserRoleEntity::getRoleCode, RoleCode.REVIEWER);
        List<UserRoleEntity> reviewerRoles = userRoleMapper.selectList(roleQuery);

        return reviewerRoles.stream()
                .map(UserRoleEntity::getUserId)
                .distinct()
                .filter(this::isUserEnabled)
                .map(uid -> new ReviewerLoad(uid, submissionMapper.countPendingByReviewer(uid)))
                .collect(Collectors.toList());
    }

    private boolean isUserEnabled(Long userId) {
        UserEntity user = userMapper.selectById(userId);
        return user != null && Boolean.TRUE.equals(user.getEnabled());
    }
}
