package com.labelhub.modules.task.dto;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.reflect.RecordComponent;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class AssignableLabelerResponseSchemaTest {

    @Test
    void allResponseFieldsHaveChineseSchemaDescriptions() {
        Map<String, String> descriptions = java.util.Arrays.stream(AssignableLabelerResponse.class.getRecordComponents())
                .collect(Collectors.toMap(
                        RecordComponent::getName,
                        component -> component.getAccessor().getAnnotation(Schema.class).description()));

        assertThat(descriptions).containsExactlyInAnyOrderEntriesOf(Map.ofEntries(
                Map.entry("labelerId", "标注员用户 ID"),
                Map.entry("username", "用户名"),
                Map.entry("email", "邮箱"),
                Map.entry("displayName", "显示名称"),
                Map.entry("avatarUrl", "头像 URL"),
                Map.entry("enabled", "账号是否启用"),
                Map.entry("loginEnabled", "是否允许登录"),
                Map.entry("claimedCount", "已领取题目总数量"),
                Map.entry("submittedCount", "已提交题目总数量"),
                Map.entry("pendingReviewCount", "待审核题目数量"),
                Map.entry("approvedCount", "已通过题目数量"),
                Map.entry("rejectedCount", "已驳回题目数量"),
                Map.entry("totalReward", "累计获得奖励"),
                Map.entry("todaySubmittedCount", "今日提交题目数量"),
                Map.entry("lastSubmitDate", "最近提交日期"),
                Map.entry("statsUpdatedAt", "统计数据更新时间"),
                Map.entry("approvalRate", "通过率")
        ));
    }
}
