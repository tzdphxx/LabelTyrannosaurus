package com.labelhub.modules.reward.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 任务奖励规则版本。BE-B 只维护奖励配置，历史奖励流水按结算时命中的规则快照保留。
 */
@TableName("reward_rules")
public class RewardRuleEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Integer effectiveVersion;
    private String rewardMode;
    private BigDecimal unitReward;
    private String rewardCurrency;
    private Boolean rewardVisible;
    private LocalDateTime effectiveAt;
    private Long createdBy;
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

    public Integer getEffectiveVersion() {
        return effectiveVersion;
    }

    public void setEffectiveVersion(Integer effectiveVersion) {
        this.effectiveVersion = effectiveVersion;
    }

    public String getRewardMode() {
        return rewardMode;
    }

    public void setRewardMode(String rewardMode) {
        this.rewardMode = rewardMode;
    }

    public BigDecimal getUnitReward() {
        return unitReward;
    }

    public void setUnitReward(BigDecimal unitReward) {
        this.unitReward = unitReward;
    }

    public String getRewardCurrency() {
        return rewardCurrency;
    }

    public void setRewardCurrency(String rewardCurrency) {
        this.rewardCurrency = rewardCurrency;
    }

    public Boolean getRewardVisible() {
        return rewardVisible;
    }

    public void setRewardVisible(Boolean rewardVisible) {
        this.rewardVisible = rewardVisible;
    }

    public LocalDateTime getEffectiveAt() {
        return effectiveAt;
    }

    public void setEffectiveAt(LocalDateTime effectiveAt) {
        this.effectiveAt = effectiveAt;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
