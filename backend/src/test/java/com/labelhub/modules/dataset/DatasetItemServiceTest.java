package com.labelhub.modules.dataset;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.common.security.CurrentUser;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.dataset.domain.DatasetItemChangeLogEntity;
import com.labelhub.modules.dataset.domain.DatasetItemEntity;
import com.labelhub.modules.dataset.dto.BatchAppendItemsRequest;
import com.labelhub.modules.dataset.dto.BatchDeleteItemsRequest;
import com.labelhub.modules.dataset.dto.BatchUpdateItemsRequest;
import com.labelhub.modules.dataset.dto.DatasetItemAppendRequest;
import com.labelhub.modules.dataset.dto.DatasetItemQuery;
import com.labelhub.modules.dataset.dto.DatasetItemStatus;
import com.labelhub.modules.dataset.dto.DatasetItemUpdateRequest;
import com.labelhub.modules.dataset.repository.DatasetItemChangeLogMapper;
import com.labelhub.modules.dataset.repository.DatasetItemRepositoryMapper;
import com.labelhub.modules.dataset.service.DatasetItemService;
import com.labelhub.modules.dataset.service.DatasetSnapshotService;
import com.labelhub.modules.task.domain.TaskEntity;
import com.labelhub.modules.task.domain.TaskStatus;
import com.labelhub.modules.task.repository.TaskRepositoryMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DatasetItemServiceTest {

    private final TaskRepositoryMapper taskMapper = mock(TaskRepositoryMapper.class);
    private final DatasetItemRepositoryMapper datasetItemMapper = mock(DatasetItemRepositoryMapper.class);
    private final DatasetItemChangeLogMapper changeLogMapper = mock(DatasetItemChangeLogMapper.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DatasetItemService itemService = new DatasetItemService(
            taskMapper,
            datasetItemMapper,
            changeLogMapper,
            objectMapper
    );
    private final DatasetSnapshotService snapshotService = new DatasetSnapshotService(
            taskMapper,
            datasetItemMapper,
            objectMapper
    );

    @AfterEach
    void clearCurrentUser() {
        CurrentUserContext.clear();
    }

    @Test
    void ownerCanListActiveItemsWithPagination() throws Exception {
        CurrentUserContext.set(new CurrentUser(10L, "owner", "owner@example.com", Set.of(RoleCode.OWNER), 1));
        stubTask(10L, TaskStatus.DRAFT, 2);
        DatasetItemEntity item = item(100L, "q1", 0, 0, false);
        when(datasetItemMapper.selectActivePage(1L, "q", 20, 0)).thenReturn(List.of(item));
        when(datasetItemMapper.countActivePage(1L, "q")).thenReturn(1L);

        var response = itemService.listItems(1L, new DatasetItemQuery(1, 20, "q"));

        assertThat(response.total()).isEqualTo(1L);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).itemJson().get("question").asText()).isEqualTo("one");
    }

    @Test
    void ownerListItemsReturnsDerivedItemStatusAndLabeler() {
        CurrentUserContext.set(new CurrentUser(10L, "owner", "owner@example.com", Set.of(RoleCode.OWNER), 1));
        stubTask(10L, TaskStatus.DRAFT, 1);
        DatasetItemEntity unclaimed = item(100L, "q1", 0, 0, false);
        DatasetItemEntity claimed = itemWithAssignment(101L, "q2", 20L, "CLAIMED");
        DatasetItemEntity draft = itemWithAssignment(102L, "q3", 21L, "DRAFTING");
        DatasetItemEntity submitted = itemWithAssignment(103L, "q4", 22L, "SUBMITTED");
        DatasetItemEntity returned = itemWithAssignment(104L, "q5", 23L, "RETURNED");
        DatasetItemEntity approved = itemWithAssignment(105L, "q6", 24L, "APPROVED");
        when(datasetItemMapper.selectActivePage(1L, null, 20, 0))
                .thenReturn(List.of(unclaimed, claimed, draft, submitted, returned, approved));
        when(datasetItemMapper.countActivePage(1L, null)).thenReturn(6L);

        var response = itemService.listItems(1L, new DatasetItemQuery(1, 20, null));

        assertThat(response.items()).extracting("itemStatus").containsExactly(
                DatasetItemStatus.UNCLAIMED,
                DatasetItemStatus.CLAIMED,
                DatasetItemStatus.DRAFT,
                DatasetItemStatus.SUBMITTED,
                DatasetItemStatus.RETURNED,
                DatasetItemStatus.APPROVED
        );
        assertThat(response.items()).extracting("labelerId").containsExactly(
                null, 20L, 21L, 22L, 23L, 24L
        );
    }

    @Test
    void nonOwnerCannotBatchAppend() {
        CurrentUserContext.set(new CurrentUser(20L, "other", "other@example.com", Set.of(RoleCode.OWNER), 1));
        stubTask(10L, TaskStatus.DRAFT, 2);

        assertThatThrownBy(() -> itemService.batchAppend(1L, new BatchAppendItemsRequest(List.of(
                new DatasetItemAppendRequest("q1", Map.of("question", "one"), Map.of())
        ))))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(403001);
    }

    @Test
    void batchAppendReturnsPerItemResultForDuplicateExternalId() {
        CurrentUserContext.set(new CurrentUser(10L, "owner", "owner@example.com", Set.of(RoleCode.OWNER), 1));
        stubTask(10L, TaskStatus.DRAFT, 2);
        when(datasetItemMapper.selectActiveByTaskIdAndExternalId(1L, "q1")).thenReturn(null);
        when(datasetItemMapper.selectActiveByTaskIdAndExternalId(1L, "q2")).thenReturn(item(101L, "q2", 0, 0, false));

        var results = itemService.batchAppend(1L, new BatchAppendItemsRequest(List.of(
                new DatasetItemAppendRequest("q1", Map.of("question", "one"), Map.of("source", "manual")),
                new DatasetItemAppendRequest("q2", Map.of("question", "two"), Map.of())
        )));

        assertThat(results).extracting("success").containsExactly(true, false);
        assertThat(results.get(1).errorCode()).isEqualTo(400102);
        verify(datasetItemMapper).insert(any(DatasetItemEntity.class));
        verify(changeLogMapper).insert(any(DatasetItemChangeLogEntity.class));
    }

    @Test
    void batchUpdateRejectsClaimedItemAndUpdatesUnclaimedItem() {
        CurrentUserContext.set(new CurrentUser(10L, "owner", "owner@example.com", Set.of(RoleCode.OWNER), 1));
        stubTask(10L, TaskStatus.DRAFT, 2);
        when(datasetItemMapper.selectById(100L)).thenReturn(item(100L, "q1", 1, 0, false));
        when(datasetItemMapper.selectById(101L)).thenReturn(item(101L, "q2", 0, 0, false));
        when(datasetItemMapper.updateEditableJsonById(any(), any(), any(), any())).thenReturn(1);

        var results = itemService.batchUpdate(1L, new BatchUpdateItemsRequest(List.of(
                new DatasetItemUpdateRequest(100L, Map.of("question", "blocked"), Map.of()),
                new DatasetItemUpdateRequest(101L, Map.of("question", "updated"), Map.of("source", "manual"))
        )));

        assertThat(results).extracting("success").containsExactly(false, true);
        assertThat(results.get(0).errorCode()).isEqualTo(400101);
        verify(datasetItemMapper).updateEditableJsonById(101L, 1L, "{\"question\":\"updated\"}", "{\"source\":\"manual\"}");
        verify(changeLogMapper).insert(any(DatasetItemChangeLogEntity.class));
    }

    @Test
    void batchUpdateReturnsFailureWhenEditableItemIsClaimedBeforeAtomicUpdate() {
        CurrentUserContext.set(new CurrentUser(10L, "owner", "owner@example.com", Set.of(RoleCode.OWNER), 1));
        stubTask(10L, TaskStatus.DRAFT, 2);
        when(datasetItemMapper.selectById(101L)).thenReturn(item(101L, "q2", 0, 0, false));
        when(datasetItemMapper.updateEditableJsonById(any(), any(), any(), any())).thenReturn(0);

        var results = itemService.batchUpdate(1L, new BatchUpdateItemsRequest(List.of(
                new DatasetItemUpdateRequest(101L, Map.of("question", "updated"), Map.of("source", "manual"))
        )));

        assertThat(results).extracting("success").containsExactly(false);
        assertThat(results.get(0).errorCode()).isEqualTo(400101);
        verify(changeLogMapper, never()).insert(any(DatasetItemChangeLogEntity.class));
    }

    @Test
    void batchDeleteSoftDeletesOnlyEditableItems() {
        CurrentUserContext.set(new CurrentUser(10L, "owner", "owner@example.com", Set.of(RoleCode.OWNER), 1));
        stubTask(10L, TaskStatus.DRAFT, 2);
        when(datasetItemMapper.selectById(100L)).thenReturn(item(100L, "q1", 0, 1, false));
        when(datasetItemMapper.selectById(101L)).thenReturn(item(101L, "q2", 0, 0, false));
        when(datasetItemMapper.softDeleteById(101L)).thenReturn(1);

        var results = itemService.batchDelete(1L, new BatchDeleteItemsRequest(List.of(100L, 101L)));

        assertThat(results).extracting("success").containsExactly(false, true);
        assertThat(results.get(0).errorCode()).isEqualTo(400101);
        verify(datasetItemMapper).softDeleteById(101L);
        verify(changeLogMapper).insert(any(DatasetItemChangeLogEntity.class));
    }

    @Test
    void batchDeleteReturnsFailureWhenEditableItemIsClaimedBeforeSoftDelete() {
        CurrentUserContext.set(new CurrentUser(10L, "owner", "owner@example.com", Set.of(RoleCode.OWNER), 1));
        stubTask(10L, TaskStatus.DRAFT, 2);
        when(datasetItemMapper.selectById(101L)).thenReturn(item(101L, "q2", 0, 0, false));
        when(datasetItemMapper.softDeleteById(101L)).thenReturn(0);

        var results = itemService.batchDelete(1L, new BatchDeleteItemsRequest(List.of(101L)));

        assertThat(results).extracting("success").containsExactly(false);
        assertThat(results.get(0).errorCode()).isEqualTo(400101);
        verify(changeLogMapper, never()).insert(any(DatasetItemChangeLogEntity.class));
    }

    @Test
    void snapshotDoesNotExposeDeletedItem() {
        when(datasetItemMapper.selectById(100L)).thenReturn(item(100L, "q1", 0, 0, true));

        assertThatThrownBy(() -> snapshotService.getDatasetItemSnapshot(100L))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(400102);
    }

    @Test
    void reserveClaimableItemUsesSingleActiveAssignmentAndIncreasesAssignedCount() {
        stubTask(10L, TaskStatus.PUBLISHED, 2);
        DatasetItemEntity claimable = item(100L, "q1", 0, 0, false);
        when(datasetItemMapper.selectClaimableItem(1L)).thenReturn(claimable);
        when(datasetItemMapper.markAssignedIfUnassigned(100L)).thenReturn(1);

        var snapshot = snapshotService.reserveClaimableItem(1L, 30L);

        assertThat(snapshot.itemId()).isEqualTo(100L);
        verify(datasetItemMapper).markAssignedIfUnassigned(100L);
    }

    @Test
    void increaseSubmittedAndApprovedCountsUseExplicitAtomicEntrypoints() {
        when(datasetItemMapper.increaseSubmittedCount(100L)).thenReturn(1);
        when(datasetItemMapper.increaseApprovedCount(100L)).thenReturn(1);

        snapshotService.increaseSubmittedCount(100L);
        snapshotService.increaseApprovedCount(100L);

        verify(datasetItemMapper).increaseSubmittedCount(100L);
        verify(datasetItemMapper).increaseApprovedCount(100L);
    }

    @Test
    void countIncreaseFailsWhenItemMissing() {
        when(datasetItemMapper.increaseSubmittedCount(100L)).thenReturn(0);

        assertThatThrownBy(() -> snapshotService.increaseSubmittedCount(100L))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(400102);
        verify(datasetItemMapper, never()).increaseApprovedCount(100L);
    }

    private void stubTask(Long ownerId, TaskStatus status, int overlapCount) {
        TaskEntity task = new TaskEntity();
        task.setId(1L);
        task.setOwnerId(ownerId);
        task.setStatus(status);
        task.setOverlapCount(overlapCount);
        when(taskMapper.selectById(1L)).thenReturn(task);
    }

    private DatasetItemEntity item(Long id, String externalId, int assignedCount, int submittedCount, boolean deleted) {
        DatasetItemEntity item = new DatasetItemEntity();
        item.setId(id);
        item.setTaskId(1L);
        item.setExternalId(externalId);
        item.setItemJson("{\"question\":\"one\"}");
        item.setMetadataJson("{\"source\":\"seed\"}");
        item.setAssignedCount(assignedCount);
        item.setSubmittedCount(submittedCount);
        item.setApprovedCount(0);
        item.setDeleted(deleted);
        return item;
    }

    private DatasetItemEntity itemWithAssignment(Long id, String externalId, Long labelerId, String assignmentStatus) {
        DatasetItemEntity item = item(id, externalId, 1, 0, false);
        item.setLabelerId(labelerId);
        item.setAssignmentStatus(assignmentStatus);
        return item;
    }
}
