package com.labelhub.modules.reward.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 标注员总体贡献统计。该表由奖励结算链路维护，查询接口只读取汇总结果。
 */
@TableName("labeler_contribution_stats")
public class LabelerContributionStatsEntity {

    @TableId
    private Long labelerId;
    private Integer claimedCount;
    private Integer submittedCount;
    private Integer pendingReviewCount;
    private Integer approvedCount;
    private Integer rejectedCount;
    private BigDecimal totalReward;
    private Integer todaySubmittedCount;
    private LocalDate lastSubmitDate;
    private LocalDateTime updatedAt;

    public Long getLabelerId() {
        return labelerId;
    }

    public void setLabelerId(Long labelerId) {
        this.labelerId = labelerId;
    }

    public Integer getClaimedCount() {
        return claimedCount;
    }

    public void setClaimedCount(Integer claimedCount) {
        this.claimedCount = claimedCount;
    }

    public Integer getSubmittedCount() {
        return submittedCount;
    }

    public void setSubmittedCount(Integer submittedCount) {
        this.submittedCount = submittedCount;
    }

    public Integer getPendingReviewCount() {
        return pendingReviewCount;
    }

    public void setPendingReviewCount(Integer pendingReviewCount) {
        this.pendingReviewCount = pendingReviewCount;
    }

    public Integer getApprovedCount() {
        return approvedCount;
    }

    public void setApprovedCount(Integer approvedCount) {
        this.approvedCount = approvedCount;
    }

    public Integer getRejectedCount() {
        return rejectedCount;
    }

    public void setRejectedCount(Integer rejectedCount) {
        this.rejectedCount = rejectedCount;
    }

    public BigDecimal getTotalReward() {
        return totalReward;
    }

    public void setTotalReward(BigDecimal totalReward) {
        this.totalReward = totalReward;
    }

    public Integer getTodaySubmittedCount() {
        return todaySubmittedCount;
    }

    public void setTodaySubmittedCount(Integer todaySubmittedCount) {
        this.todaySubmittedCount = todaySubmittedCount;
    }

    public LocalDate getLastSubmitDate() {
        return lastSubmitDate;
    }

    public void setLastSubmitDate(LocalDate lastSubmitDate) {
        this.lastSubmitDate = lastSubmitDate;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
