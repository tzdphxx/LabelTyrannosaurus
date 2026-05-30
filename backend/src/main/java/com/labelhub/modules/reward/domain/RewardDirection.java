package com.labelhub.modules.reward.domain;

/**
 * 奖励流水方向。正向发放使用 {@code CREDIT}，冲正只允许追加 {@code DEBIT} 流水。
 */
public enum RewardDirection {
    CREDIT,
    DEBIT;

    public boolean credit() {
        return this == CREDIT;
    }
}
