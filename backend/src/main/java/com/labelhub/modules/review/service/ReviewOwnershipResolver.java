package com.labelhub.modules.review.service;

import com.labelhub.modules.review.mapper.ReviewTaskClaimMapper;
import com.labelhub.modules.submission.domain.Submission;
import com.labelhub.modules.submission.mapper.SubmissionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 解析单条提交在进入待审池时的审核员归属。
 *
 * <p>当提交进入 {@code PENDING_FINAL}（首次进入或多级审核升级到下一级）时，
 * 若其所属 (任务, 当前审核级别) 已被某审核员整任务领取，则把该提交的
 * {@code assigned_reviewer_id} 归属给该审核员，实现"后续提交自动归属"。</p>
 */
@Service
public class ReviewOwnershipResolver {

    private static final Logger log = LoggerFactory.getLogger(ReviewOwnershipResolver.class);

    private final ReviewTaskClaimMapper claimMapper;
    private final SubmissionMapper submissionMapper;

    public ReviewOwnershipResolver(ReviewTaskClaimMapper claimMapper,
                                   SubmissionMapper submissionMapper) {
        this.claimMapper = claimMapper;
        this.submissionMapper = submissionMapper;
    }

    /**
     * 若该提交所属 (任务, 级别) 已被领取，则把它归属给领取的审核员。
     * 仅在归属为空时设置，不覆盖既有归属。
     */
    public void assignToClaimant(Submission submission) {
        if (submission == null || submission.getAssignedReviewerId() != null) {
            return;
        }
        int level = submission.getCurrentReviewLevel() != null
                ? submission.getCurrentReviewLevel() : 1;
        Long reviewerId = claimMapper.selectReviewerForTaskLevel(submission.getTaskId(), level);
        if (reviewerId == null) {
            return;
        }
        submission.setAssignedReviewerId(reviewerId);
        submissionMapper.updateById(submission);
        log.debug("Submission {} auto-assigned to claimant reviewer {} (task {}, level {})",
                submission.getId(), reviewerId, submission.getTaskId(), level);
    }
}
