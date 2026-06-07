package com.labelhub.modules.ai.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.labelhub.modules.ai.domain.AiFlowAction;
import com.labelhub.modules.ai.domain.AiReviewConfig;
import com.labelhub.modules.ai.domain.AiReviewResult;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * E2E 验证用例（阶段 2）：AI 流转决策真值表。
 * 覆盖「AI 能否直接过审 / 直接打回 / 只给建议（转人工）」的核心逻辑。
 */
class AiFlowDecisionServiceE2ETest {

    private final AiFlowDecisionService service = new AiFlowDecisionService();

    private AiReviewResult result(String decision, String conf, String avg) {
        AiReviewResult r = new AiReviewResult();
        r.setDecision(decision);
        if (conf != null) r.setConfidence(new BigDecimal(conf));
        if (avg != null) r.setAverageScore(new BigDecimal(avg));
        return r;
    }

    private AiReviewConfig config(String policy, Boolean allowApprove, Boolean allowReject) {
        AiReviewConfig c = new AiReviewConfig();
        c.setAiFlowPolicy(policy);
        c.setAllowAiDirectApprove(allowApprove);
        c.setAllowAiDirectReject(allowReject);
        c.setPassThreshold(new BigDecimal("80"));
        c.setRejectThreshold(new BigDecimal("40"));
        return c;
    }

    @Test
    void manualFirstAlwaysGoesManualEvenForConfidentPass() {
        AiFlowAction action = service.decide(
                result("PASS", "0.99", "95"), config("MANUAL_FIRST", true, true));
        assertThat(action).isEqualTo(AiFlowAction.AI_ASSIGN_MANUAL_REVIEW);
    }

    @Test
    void alwaysManualGoesManual() {
        assertThat(service.decide(result("PASS", "0.99", "95"), config("ALWAYS_MANUAL", true, true)))
                .isEqualTo(AiFlowAction.AI_ASSIGN_MANUAL_REVIEW);
    }

    @Test
    void passAndRejectWithApproveAllowedDirectApproves() {
        assertThat(service.decide(result("PASS", "0.90", "95"), config("AI_PASS_AND_REJECT", true, true)))
                .isEqualTo(AiFlowAction.AI_DIRECT_APPROVE);
    }

    @Test
    void passOnlyWithApproveAllowedDirectApproves() {
        assertThat(service.decide(result("PASS", "0.90", "95"), config("AI_PASS_ONLY", true, false)))
                .isEqualTo(AiFlowAction.AI_DIRECT_APPROVE);
    }

    @Test
    void passButApproveSwitchOffGoesManual() {
        assertThat(service.decide(result("PASS", "0.90", "95"), config("AI_PASS_ONLY", false, false)))
                .isEqualTo(AiFlowAction.AI_ASSIGN_MANUAL_REVIEW);
    }

    @Test
    void passButPolicyOnlyAllowsRejectGoesManual() {
        assertThat(service.decide(result("PASS", "0.90", "95"), config("AI_REJECT_ONLY", true, true)))
                .isEqualTo(AiFlowAction.AI_ASSIGN_MANUAL_REVIEW);
    }

    @Test
    void passBelowScoreThresholdGoesManual() {
        assertThat(service.decide(result("PASS", "0.90", "70"), config("AI_PASS_ONLY", true, false)))
                .isEqualTo(AiFlowAction.AI_ASSIGN_MANUAL_REVIEW);
    }

    @Test
    void lowConfidenceGoesManual() {
        assertThat(service.decide(result("PASS", "0.50", "95"), config("AI_PASS_ONLY", true, false)))
                .isEqualTo(AiFlowAction.AI_ASSIGN_MANUAL_REVIEW);
    }

    @Test
    void rejectWithRejectAllowedDirectRejects() {
        assertThat(service.decide(result("REJECT", "0.90", "20"), config("AI_PASS_AND_REJECT", true, true)))
                .isEqualTo(AiFlowAction.AI_DIRECT_REJECT);
    }

    @Test
    void uncertainDecisionGoesManual() {
        assertThat(service.decide(result("UNCERTAIN", "0.99", "95"), config("AI_PASS_AND_REJECT", true, true)))
                .isEqualTo(AiFlowAction.AI_ASSIGN_MANUAL_REVIEW);
    }

    @Test
    void riskFlagsForceManualEvenWhenPass() {
        AiReviewResult r = result("PASS", "0.99", "95");
        r.setRiskFlags("[\"SUSPECTED_FRAUD\"]");
        assertThat(service.decide(r, config("AI_PASS_AND_REJECT", true, true)))
                .isEqualTo(AiFlowAction.AI_ASSIGN_MANUAL_REVIEW);
    }

    @Test
    void degradedResultGoesManual() {
        AiReviewResult r = result("PASS", "0.99", "95");
        r.setDegraded(true);
        assertThat(service.decide(r, config("AI_PASS_AND_REJECT", true, true)))
                .isEqualTo(AiFlowAction.AI_ASSIGN_MANUAL_REVIEW);
    }
}
