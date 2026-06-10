package com.labelhub.modules.reward.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("reward_rules")
public class RewardRule {

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
}
