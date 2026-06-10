package com.labelhub.modules.assignment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.labelhub.common.exception.BusinessException;
import com.labelhub.modules.assignment.dto.RewardSummaryResponse;
import com.labelhub.modules.dataset.domain.DatasetItem;
import com.labelhub.modules.dataset.mapper.DatasetItemMapper;
import com.labelhub.modules.dataset.service.DatasetMarketStatsService;
import com.labelhub.modules.reward.service.RewardSummaryService;
import com.labelhub.modules.task.domain.ClaimStrategy;
import com.labelhub.modules.task.domain.Task;
import com.labelhub.modules.task.domain.TaskStatus;
import com.labelhub.modules.task.domain.TaskTag;
import com.labelhub.modules.task.mapper.TaskMapper;
import com.labelhub.modules.task.mapper.TaskTagMapper;
import com.labelhub.modules.template.domain.TemplateVersion;
import com.labelhub.modules.template.mapper.TemplateVersionMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class LabelerTaskWorkspaceServiceTest {

    private static final Long LABELER_ID = 20L;
    private static final Long TASK_ID = 10L;
    private static final Long TEMPLATE_VERSION_ID = 40L;

    private final TaskMapper taskMapper = org.mockito.Mockito.mock(TaskMapper.class);
    private final TaskTagMapper taskTagMapper = org.mockito.Mockito.mock(TaskTagMapper.class);
    private final DatasetItemMapper datasetItemMapper = org.mockito.Mockito.mock(DatasetItemMapper.class);
    private final DatasetMarketStatsService datasetMarketStatsService =
            org.mockito.Mockito.mock(DatasetMarketStatsService.class);
    private final AssignmentMarketStatsService assignmentMarketStatsService =
            org.mockito.Mockito.mock(AssignmentMarketStatsService.class);
    private final RewardSummaryService rewardSummaryService = org.mockito.Mockito.mock(RewardSummaryService.class);
    private final TemplateVersionMapper templateVersionMapper = org.mockito.Mockito.mock(TemplateVersionMapper.class);

    private final LabelerTaskWorkspaceService service = new LabelerTaskWorkspaceService(
            taskMapper,
            taskTagMapper,
            datasetItemMapper,
            datasetMarketStatsService,
            assignmentMarketStatsService,
            rewardSummaryService,
            templateVersionMapper);

    @Test
    void getsPublishedTaskDetailWithClaimableItemDetails() {
        Task task = publishedTask();
        when(taskMapper.selectPublishedMarketTaskById(taskIdArg(), nowArg())).thenReturn(task);
        when(taskTagMapper.selectList(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(tag("image")));
        when(datasetMarketStatsService.countAvailableItems(TASK_ID, LABELER_ID, 1)).thenReturn(6);
        when(assignmentMarketStatsService.countClaimedByLabeler(TASK_ID, LABELER_ID)).thenReturn(2);
        RewardSummaryResponse reward = new RewardSummaryResponse("PIECE", new BigDecimal("0.20"), "CNY");
        when(rewardSummaryService.findRewardSummary(TASK_ID, true)).thenReturn(reward);
        when(datasetItemMapper.selectClaimableItems(TASK_ID, LABELER_ID, 1, 100, 0))
                .thenReturn(List.of(item(100L, "q-100")));

        var response = service.getTaskDetail(LABELER_ID, TASK_ID, 0, 500);

        assertThat(response.task().taskId()).isEqualTo(TASK_ID);
        assertThat(response.templateVersionId()).isEqualTo(TEMPLATE_VERSION_ID);
        assertThat(response.availableCount()).isEqualTo(6);
        assertThat(response.currentUserClaimedCount()).isEqualTo(2);
        assertThat(response.rewardSummary()).isEqualTo(reward);
        assertThat(response.items()).singleElement()
                .satisfies(item -> {
                    assertThat(item.itemId()).isEqualTo(100L);
                    assertThat(item.externalId()).isEqualTo("q-100");
                    assertThat(item.itemJson()).isEqualTo("{\"text\":\"hello\"}");
                    assertThat(item.metadataJson()).isEqualTo("{\"source\":\"unit\"}");
                });
        verify(datasetItemMapper).selectClaimableItems(TASK_ID, LABELER_ID, 1, 100, 0);
    }

    @Test
    void rejectsInvisibleTaskDetail() {
        when(taskMapper.selectPublishedMarketTaskById(taskIdArg(), nowArg())).thenReturn(null);

        assertThatThrownBy(() -> service.getTaskDetail(LABELER_ID, TASK_ID, 1, 20))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(404501));
    }

    @Test
    void getsPublishedAnswerTemplateForLabeler() {
        Task task = publishedTask();
        TemplateVersion version = new TemplateVersion();
        version.setId(TEMPLATE_VERSION_ID);
        version.setSchemaJson("{\"type\":\"object\"}");
        when(taskMapper.selectPublishedMarketTaskById(taskIdArg(), nowArg())).thenReturn(task);
        when(templateVersionMapper.selectById(TEMPLATE_VERSION_ID)).thenReturn(version);

        var response = service.getAnswerTemplate(LABELER_ID, TASK_ID);

        assertThat(response.taskId()).isEqualTo(TASK_ID);
        assertThat(response.templateVersionId()).isEqualTo(TEMPLATE_VERSION_ID);
        assertThat(response.schemaJson()).isEqualTo("{\"type\":\"object\"}");
    }

    @Test
    void rejectsTaskWithoutPublishedTemplate() {
        Task task = publishedTask();
        task.setPublishedTemplateVersionId(null);
        when(taskMapper.selectPublishedMarketTaskById(taskIdArg(), nowArg())).thenReturn(task);

        assertThatThrownBy(() -> service.getAnswerTemplate(LABELER_ID, TASK_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(404502));
    }

    private Task publishedTask() {
        Task task = new Task();
        task.setId(TASK_ID);
        task.setTitle("Image Task");
        task.setDescription("desc");
        task.setInstructionRichText("<p>label carefully</p>");
        task.setStatus(TaskStatus.PUBLISHED);
        task.setQuota(10);
        task.setClaimedCount(3);
        task.setOverlapCount(1);
        task.setStrategy(ClaimStrategy.FCFS);
        task.setDeadlineAt(LocalDateTime.now().plusDays(1));
        task.setPublishedTemplateVersionId(TEMPLATE_VERSION_ID);
        task.setRewardVisible(true);
        return task;
    }

    private TaskTag tag(String name) {
        TaskTag tag = new TaskTag();
        tag.setTaskId(TASK_ID);
        tag.setTagName(name);
        return tag;
    }

    private DatasetItem item(Long id, String externalId) {
        DatasetItem item = new DatasetItem();
        item.setId(id);
        item.setExternalId(externalId);
        item.setItemJson("{\"text\":\"hello\"}");
        item.setMetadataJson("{\"source\":\"unit\"}");
        return item;
    }

    private LocalDateTime nowArg() {
        return org.mockito.ArgumentMatchers.any(LocalDateTime.class);
    }

    private Long taskIdArg() {
        return org.mockito.ArgumentMatchers.eq(TASK_ID);
    }
}
