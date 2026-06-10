package com.labelhub.modules.review.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 审核员对某个 (任务, 审核级别) 的整任务领取记录。
 *
 * <p>一个 (taskId, reviewLevel) 组合只能被一名审核员领取，由数据库唯一约束
 * {@code uk_review_task_claim} 保证排他。领取后，该任务该级别下当前及后续进入
 * 待审池的提交都会归属给该审核员。</p>
 */
@Getter
@Setter
@TableName("review_task_claims")
public class ReviewTaskClaim {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private Integer reviewLevel;

    private Long reviewerId;

    private LocalDateTime claimedAt;
}
