package com.labelhub.contracts;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ApiContractMappingTest {

    @Test
    void p0AndP1ContractEndpointsHaveControllerMappings() {
        Set<Endpoint> actual = mappedEndpoints();

        assertThat(actual).containsAll(List.of(
                endpoint("POST", "/api/v1/auth/register"),
                endpoint("POST", "/api/v1/auth/login"),
                endpoint("POST", "/api/v1/auth/refresh"),
                endpoint("GET", "/api/v1/users/me"),
                endpoint("PUT", "/api/v1/users/me/password"),
                endpoint("PUT", "/api/v1/users/me/profile"),
                endpoint("GET", "/api/v1/admin/users"),
                endpoint("PUT", "/api/v1/admin/users/{userId}/roles"),
                endpoint("POST", "/api/v1/admin/users/{userId}/enable"),
                endpoint("POST", "/api/v1/admin/users/{userId}/disable"),
                endpoint("GET", "/api/v1/audit-logs"),
                endpoint("GET", "/api/v1/owner/labelers/assignable"),
                endpoint("GET", "/api/v1/owner/tasks"),
                endpoint("POST", "/api/v1/tasks"),
                endpoint("GET", "/api/v1/tasks/{taskId}"),
                endpoint("PUT", "/api/v1/tasks/{taskId}"),
                endpoint("POST", "/api/v1/tasks/{taskId}/publish"),
                endpoint("POST", "/api/v1/tasks/{taskId}/pause"),
                endpoint("POST", "/api/v1/tasks/{taskId}/resume"),
                endpoint("POST", "/api/v1/tasks/{taskId}/end"),
                endpoint("POST", "/api/v1/tasks/{taskId}/imports"),
                endpoint("POST", "/api/v1/tasks/{taskId}/imports/overwrite"),
                endpoint("GET", "/api/v1/tasks/{taskId}/imports/{jobId}"),
                endpoint("GET", "/api/v1/tasks/{taskId}/items"),
                endpoint("POST", "/api/v1/tasks/{taskId}/items/batch-append"),
                endpoint("POST", "/api/v1/tasks/{taskId}/items/batch-update"),
                endpoint("POST", "/api/v1/tasks/{taskId}/items/batch-delete"),
                endpoint("POST", "/api/v1/tasks/{taskId}/templates"),
                endpoint("GET", "/api/v1/tasks/{taskId}/templates"),
                endpoint("GET", "/api/v1/templates/{templateId}/versions"),
                endpoint("GET", "/api/v1/template-versions/{versionId}"),
                endpoint("POST", "/api/v1/templates/{templateId}/fork"),
                endpoint("POST", "/api/v1/schema/validate-answer"),
                endpoint("GET", "/api/v1/market/tasks"),
                endpoint("GET", "/api/v1/market/tasks/{taskId}"),
                endpoint("POST", "/api/v1/tasks/{taskId}/items/claim"),
                endpoint("GET", "/api/v1/claims/{claimId}"),
                endpoint("PUT", "/api/v1/claims/{claimId}/draft"),
                endpoint("POST", "/api/v1/claims/{claimId}/submit"),
                endpoint("GET", "/api/v1/claims"),
                endpoint("GET", "/api/v1/labeler/assignments"),
                endpoint("POST", "/api/v1/labeler/assignments/{assignmentId}/cancel"),
                endpoint("GET", "/api/v1/labeler/submissions"),
                endpoint("GET", "/api/v1/llm-providers"),
                endpoint("POST", "/api/v1/admin/llm-providers"),
                endpoint("PUT", "/api/v1/admin/llm-providers/{providerId}"),
                endpoint("POST", "/api/v1/admin/llm-providers/{providerId}/enable"),
                endpoint("POST", "/api/v1/admin/llm-providers/{providerId}/disable"),
                endpoint("POST", "/api/v1/admin/llm-providers/{providerId}/test"),
                endpoint("POST", "/api/v1/assignments/{assignmentId}/llm-triggers"),
                endpoint("GET", "/api/v1/llm/triggers/runs/{triggerRunId}"),
                endpoint("POST", "/api/v1/tasks/{taskId}/ai-review-configs"),
                endpoint("PUT", "/api/v1/tasks/{taskId}/ai-review-configs/{configId}"),
                endpoint("GET", "/api/v1/tasks/{taskId}/ai-review-configs"),
                endpoint("POST", "/api/v1/tasks/{taskId}/ai-review-configs/{configId}/test"),
                endpoint("GET", "/api/v1/submissions/{submissionId}/ai-review"),
                endpoint("GET", "/api/v1/submissions/{submissionId}/ai-review-result"),
                endpoint("POST", "/api/v1/submissions/{submissionId}/ai-review/retry"),
                endpoint("GET", "/api/v1/agent-runs/{agentRunId}"),
                endpoint("GET", "/api/v1/reviewer/submissions"),
                endpoint("GET", "/api/v1/reviewer/submissions/{submissionId}"),
                endpoint("POST", "/api/v1/reviewer/submissions/{submissionId}/approve"),
                endpoint("POST", "/api/v1/reviewer/submissions/{submissionId}/reject"),
                endpoint("POST", "/api/v1/reviewer/submissions/batch/approve"),
                endpoint("POST", "/api/v1/reviewer/submissions/batch/mark-manual"),
                endpoint("POST", "/api/v1/reviewer/submissions/batch-approve"),
                endpoint("POST", "/api/v1/reviewer/submissions/batch-reject"),
                endpoint("POST", "/api/v1/reviewer/submissions/batch-mark-manual"),
                endpoint("POST", "/api/v1/reviewer/tasks/{taskId}/claim"),
                endpoint("DELETE", "/api/v1/reviewer/tasks/{taskId}/claim"),
                endpoint("GET", "/api/v1/reviewer/tasks"),
                endpoint("GET", "/api/v1/reviewer/dashboard"),
                endpoint("GET", "/api/v1/reviewer/ai-review-status"),
                endpoint("GET", "/api/v1/reviewer/conflict-groups"),
                endpoint("GET", "/api/v1/reviewer/conflict-groups/{groupId}"),
                endpoint("POST", "/api/v1/reviewer/conflict-groups/{groupId}/resolve"),
                endpoint("POST", "/api/v1/tasks/{taskId}/reward-rule"),
                endpoint("GET", "/api/v1/tasks/{taskId}/reward-rule"),
                endpoint("GET", "/api/v1/labeler/contribution/overview"),
                endpoint("GET", "/api/v1/labeler/contribution/trend"),
                endpoint("GET", "/api/v1/labeler/contribution/tasks"),
                endpoint("GET", "/api/v1/labeler/rewards/ledger"),
                endpoint("POST", "/api/v1/tasks/{taskId}/exports"),
                endpoint("GET", "/api/v1/tasks/{taskId}/exports"),
                endpoint("GET", "/api/v1/tasks/{taskId}/exports/{exportJobId}"),
                endpoint("POST", "/api/v1/files/upload"),
                endpoint("GET", "/api/v1/files/{fileId}/signed-url")
        ));
    }

    private static Set<Endpoint> mappedEndpoints() {
        Set<Endpoint> endpoints = new LinkedHashSet<>();
        for (String className : controllerClassNames()) {
            Class<?> controller = loadClass(className);
            String[] basePaths = requestMappingPaths(controller.getAnnotation(RequestMapping.class));
            if (basePaths.length == 0) {
                basePaths = new String[] {""};
            }
            for (Method method : controller.getDeclaredMethods()) {
                add(endpoints, "GET", basePaths, getPaths(method));
                add(endpoints, "POST", basePaths, postPaths(method));
                add(endpoints, "PUT", basePaths, putPaths(method));
                add(endpoints, "DELETE", basePaths, deletePaths(method));
            }
        }
        return endpoints;
    }

    private static void add(Set<Endpoint> endpoints, String verb, String[] basePaths, String[] methodPaths) {
        for (String basePath : basePaths) {
            for (String methodPath : methodPaths) {
                endpoints.add(endpoint(verb, join(basePath, methodPath)));
            }
        }
    }

    private static String[] getPaths(Method method) {
        GetMapping mapping = method.getAnnotation(GetMapping.class);
        return mapping == null ? new String[0] : paths(mapping.value(), mapping.path());
    }

    private static String[] postPaths(Method method) {
        PostMapping mapping = method.getAnnotation(PostMapping.class);
        return mapping == null ? new String[0] : paths(mapping.value(), mapping.path());
    }

    private static String[] putPaths(Method method) {
        PutMapping mapping = method.getAnnotation(PutMapping.class);
        return mapping == null ? new String[0] : paths(mapping.value(), mapping.path());
    }

    private static String[] deletePaths(Method method) {
        DeleteMapping mapping = method.getAnnotation(DeleteMapping.class);
        return mapping == null ? new String[0] : paths(mapping.value(), mapping.path());
    }

    private static String[] requestMappingPaths(RequestMapping mapping) {
        return mapping == null ? new String[0] : paths(mapping.value(), mapping.path());
    }

    private static String[] paths(String[] values, String[] paths) {
        String[] selected = values.length > 0 ? values : paths;
        return selected.length > 0 ? selected : new String[] {""};
    }

    private static String join(String basePath, String methodPath) {
        if (basePath.isBlank()) {
            return normalize(methodPath);
        }
        if (methodPath.isBlank()) {
            return normalize(basePath);
        }
        return normalize(basePath + "/" + methodPath);
    }

    private static String normalize(String path) {
        String normalized = path.replace("\\", "").replaceAll("/{2,}", "/");
        return normalized.startsWith("/") ? normalized : "/" + normalized;
    }

    private static Endpoint endpoint(String method, String path) {
        return new Endpoint(method, path);
    }

    private static Class<?> loadClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("Controller class is missing: " + className, ex);
        }
    }

    private static List<String> controllerClassNames() {
        return Arrays.asList(
                "com.labelhub.modules.admin.controller.AdminUserController",
                "com.labelhub.modules.agent.web.AgentRunController",
                "com.labelhub.modules.ai.web.AiReviewConfigController",
                "com.labelhub.modules.ai.web.AiReviewController",
                "com.labelhub.modules.ai.web.AiReviewResultController",
                "com.labelhub.modules.ai.web.AdminLlmProviderController",
                "com.labelhub.modules.ai.web.LlmProviderController",
                "com.labelhub.modules.ai.web.LlmTriggerController",
                "com.labelhub.modules.assignment.web.ClaimController",
                "com.labelhub.modules.assignment.web.LabelerAssignmentController",
                "com.labelhub.modules.assignment.web.MarketTaskController",
                "com.labelhub.modules.audit.controller.AuditLogController",
                "com.labelhub.modules.auth.controller.AuthController",
                "com.labelhub.modules.dataset.controller.DatasetImportController",
                "com.labelhub.modules.dataset.controller.DatasetItemController",
                "com.labelhub.modules.export.controller.ExportController",
                "com.labelhub.modules.review.web.ConflictController",
                "com.labelhub.modules.review.web.ReviewController",
                "com.labelhub.modules.review.web.ReviewTaskClaimController",
                "com.labelhub.modules.review.web.ReviewerWorkspaceController",
                "com.labelhub.modules.reward.controller.ContributionController",
                "com.labelhub.modules.reward.controller.RewardRuleController",
                "com.labelhub.modules.storage.controller.FileController",
                "com.labelhub.modules.submission.web.LabelerSubmissionController",
                "com.labelhub.modules.task.web.OwnerLabelerController",
                "com.labelhub.modules.task.web.OwnerTaskController",
                "com.labelhub.modules.task.web.TaskController",
                "com.labelhub.modules.template.controller.SchemaValidationController",
                "com.labelhub.modules.template.controller.TemplateController"
        );
    }

    private record Endpoint(String method, String path) {
    }
}
