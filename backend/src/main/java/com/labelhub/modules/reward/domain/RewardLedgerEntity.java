package com.labelhub.modules.reward.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 奖励流水。该表只追加不修改，冲正通过负向流水表达，不删除历史正向流水。
 */
@TableName("reward_ledger")
public class RewardLedgerEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long labelerId;
    private Long submissionId;
    private Long assignmentId;
    private BigDecimal amount;
    private RewardDirection direction;
    private String reason;
    private String sourceEventId;
    private String rewardType;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Long getLabelerId() {
        return labelerId;
    }

    public void setLabelerId(Long labelerId) {
        this.labelerId = labelerId;
    }

    public Long getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(Long submissionId) {
        this.submissionId = submissionId;
    }

    public Long getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(Long assignmentId) {
        this.assignmentId = assignmentId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public RewardDirection getDirection() {
        return direction;
    }

    public void setDirection(RewardDirection direction) {
        this.direction = direction;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getSourceEventId() {
        return sourceEventId;
    }

    public void setSourceEventId(String sourceEventId) {
        this.sourceEventId = sourceEventId;
    }

    public String getRewardType() {
        return rewardType;
    }

    public void setRewardType(String rewardType) {
        this.rewardType = rewardType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
