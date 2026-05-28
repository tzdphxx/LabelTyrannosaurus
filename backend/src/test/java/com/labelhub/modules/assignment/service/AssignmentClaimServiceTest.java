package com.labelhub.modules.assignment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.labelhub.common.audit.AuditAppender;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.infrastructure.redis.RedisLockService;
import com.labelhub.modules.assignment.domain.Assignment;
import com.labelhub.modules.assignment.domain.AssignmentStatus;
import com.labelhub.modules.assignment.dto.AssignmentClaimResponse;
import com.labelhub.modules.assignment.mapper.AssignmentMapper;
import com.labelhub.modules.dataset.service.DatasetClaimService;
import com.labelhub.modules.dataset.service.DatasetItemSnapshot;
import com.labelhub.modules.task.domain.Task;
import com.labelhub.modules.task.domain.TaskStatus;
import com.labelhub.modules.task.mapper.TaskMapper;
import com.labelhub.modules.template.service.TemplateSchemaService;
import com.labelhub.modules.template.service.TemplateSchemaSnapshot;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

@ExtendWith(MockitoExtension.class)
class AssignmentClaimServiceTest {

    private static final Long TASK_ID = 10L;
    private static final Long LABELER_ID = 20L;
    private static final Long ITEM_ID = 30L;
    private static final Long TEMPLATE_VERSION_ID = 40L;
    private static final Long ASSIGNMENT_ID = 50L;

    @Mock
    private TaskMapper taskMapper;

    @Mock
    private DatasetClaimService datasetClaimService;

    @Mock
    private TemplateSchemaService templateSchemaService;

    @Mock
    private AssignmentMapper assignmentMapper;

    @Mock
    private RedisLockService redisLockService;

    @Mock
    private AuditAppender auditAppender;

    private AssignmentClaimService assignmentClaimService;

    @BeforeEach
    void setUp() {
        assignmentClaimService = new AssignmentClaimService(
                taskMapper,
                datasetClaimService,
                templateSchemaService,
                assignmentMapper,
                redisLockService,
                auditAppender
        );
    }

    @Test
    void claimsItemWithLockAssignmentAndAudit() {
        when(taskMapper.selectById(TASK_ID)).thenReturn(publishedTask(1));
        when(redisLockService.tryLock("lock:claim:task:10", 2000, 10000)).thenReturn(true);
        when(datasetClaimService.reserveClaimableItem(TASK_ID, LABELER_ID, 1))
                .thenReturn(Optional.of(new DatasetItemSnapshot(ITEM_ID, "{\"text\":\"hello\"}")));
        when(templateSchemaService.getTemplateSchema(TEMPLATE_VERSION_ID))
                .thenReturn(new TemplateSchemaSnapshot(TEMPLATE_VERSION_ID, "{\"type\":\"object\"}"));
        when(assignmentMapper.insert(any(Assignment.class))).thenAnswer(invocation -> {
            Assignment assignment = invocation.getArgument(0);
            assignment.setId(ASSIGNMENT_ID);
            return 1;
        });

        AssignmentClaimResponse response = assignmentClaimService.claim(TASK_ID, LABELER_ID);

        assertThat(response.assignmentId()).isEqualTo(ASSIGNMENT_ID);
        assertThat(response.datasetItemId()).isEqualTo(ITEM_ID);
        assertThat(response.templateVersionId()).isEqualTo(TEMPLATE_VERSION_ID);
        assertThat(response.schemaJson()).isEqualTo("{\"type\":\"object\"}");
        assertThat(response.itemJson()).isEqualTo("{\"text\":\"hello\"}");
        assertThat(response.draftAnswerJson()).isNull();
        assertThat(response.draftVersion()).isEqualTo(1);
        ArgumentCaptor<Assignment> assignmentCaptor = ArgumentCaptor.forClass(Assignment.class);
        verify(assignmentMapper).insert(assignmentCaptor.capture());
        assertThat(assignmentCaptor.getValue().getStatus()).isEqualTo(AssignmentStatus.CLAIMED);
        verify(auditAppender).append(eq("ASSIGNMENT"), eq(ASSIGNMENT_ID), eq("USER"), eq(LABELER_ID),
                eq("ASSIGNMENT_CLAIMED"), eq(null), any(), eq(null), eq(null));
        verify(redisLockService).unlock("lock:claim:task:10");
    }

    @Test
    void rejectsWhenLockCannotBeAcquired() {
        when(taskMapper.selectById(TASK_ID)).thenReturn(publishedTask(1));
        when(redisLockService.tryLock("lock:claim:task:10", 2000, 10000)).thenReturn(false);

        assertThatThrownBy(() -> assignmentClaimService.claim(TASK_ID, LABELER_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(409201));

        verify(datasetClaimService, never()).reserveClaimableItem(any(), any(), any());
        verify(redisLockService, never()).unlock(any());
    }

    @Test
    void rejectsWhenNoClaimableItemExists() {
        when(taskMapper.selectById(TASK_ID)).thenReturn(publishedTask(1));
        when(redisLockService.tryLock("lock:claim:task:10", 2000, 10000)).thenReturn(true);
        when(datasetClaimService.reserveClaimableItem(TASK_ID, LABELER_ID, 1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assignmentClaimService.claim(TASK_ID, LABELER_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(409201));

        verify(assignmentMapper, never()).insert(any(Assignment.class));
        verify(auditAppender, never()).append(any(), any(), any(), any(), any(), any(), any(), any(), any());
        verify(redisLockService).unlock("lock:claim:task:10");
    }

    @Test
    void rejectsDuplicateClaimAndLetsTransactionRollbackReservation() {
        when(taskMapper.selectById(TASK_ID)).thenReturn(publishedTask(1));
        when(redisLockService.tryLock("lock:claim:task:10", 2000, 10000)).thenReturn(true);
        when(datasetClaimService.reserveClaimableItem(TASK_ID, LABELER_ID, 1))
                .thenReturn(Optional.of(new DatasetItemSnapshot(ITEM_ID, "{\"text\":\"hello\"}")));
        when(templateSchemaService.getTemplateSchema(TEMPLATE_VERSION_ID))
                .thenReturn(new TemplateSchemaSnapshot(TEMPLATE_VERSION_ID, "{\"type\":\"object\"}"));
        when(assignmentMapper.insert(any(Assignment.class))).thenThrow(new DuplicateKeyException("duplicate"));

        assertThatThrownBy(() -> assignmentClaimService.claim(TASK_ID, LABELER_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(409201));

        verify(auditAppender, never()).append(any(), any(), any(), any(), any(), any(), any(), any(), any());
        verify(redisLockService).unlock("lock:claim:task:10");
    }

    @Test
    void rejectsNonPublishedTask() {
        Task task = publishedTask(1);
        task.setStatus(TaskStatus.PAUSED);
        when(taskMapper.selectById(TASK_ID)).thenReturn(task);

        assertThatThrownBy(() -> assignmentClaimService.claim(TASK_ID, LABELER_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(400101));

        verify(redisLockService, never()).tryLock(any(), any(Long.class), any(Long.class));
    }

    @Test
    void rejectsExpiredTask() {
        Task task = publishedTask(1);
        task.setDeadlineAt(LocalDateTime.now().minusMinutes(1));
        when(taskMapper.selectById(TASK_ID)).thenReturn(task);

        assertThatThrownBy(() -> assignmentClaimService.claim(TASK_ID, LABELER_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(400101));

        verify(redisLockService, never()).tryLock(any(), any(Long.class), any(Long.class));
    }

    @Test
    void concurrentClaimsDoNotOverAssignWhenOnlyOneReservationCanWin() throws InterruptedException {
        InMemoryDatasetClaimService inMemoryDatasetClaimService = new InMemoryDatasetClaimService();
        AssignmentClaimService concurrentService = new AssignmentClaimService(
                taskMapper,
                inMemoryDatasetClaimService,
                templateSchemaService,
                assignmentMapper,
                redisLockService,
                auditAppender
        );
        when(taskMapper.selectById(TASK_ID)).thenReturn(publishedTask(1));
        when(redisLockService.tryLock("lock:claim:task:10", 2000, 10000)).thenReturn(true);
        when(templateSchemaService.getTemplateSchema(TEMPLATE_VERSION_ID))
                .thenReturn(new TemplateSchemaSnapshot(TEMPLATE_VERSION_ID, "{\"type\":\"object\"}"));
        when(assignmentMapper.insert(any(Assignment.class))).thenAnswer(invocation -> {
            Assignment assignment = invocation.getArgument(0);
            assignment.setId(ASSIGNMENT_ID + assignment.getLabelerId());
            return 1;
        });

        AtomicInteger successCount = new AtomicInteger();
        Thread[] threads = new Thread[20];
        for (int i = 0; i < threads.length; i++) {
            long labelerId = 100L + i;
            threads[i] = new Thread(() -> {
                try {
                    concurrentService.claim(TASK_ID, labelerId);
                    successCount.incrementAndGet();
                } catch (BusinessException ignored) {
                }
            });
        }
        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(inMemoryDatasetClaimService.assignedCount()).isEqualTo(1);
    }

    private Task publishedTask(int overlapCount) {
        Task task = new Task();
        task.setId(TASK_ID);
        task.setStatus(TaskStatus.PUBLISHED);
        task.setOverlapCount(overlapCount);
        task.setPublishedTemplateVersionId(TEMPLATE_VERSION_ID);
        task.setDeadlineAt(LocalDateTime.now().plusDays(1));
        return task;
    }

    private static final class InMemoryDatasetClaimService implements DatasetClaimService {

        private final AtomicInteger assignedCount = new AtomicInteger();

        @Override
        public Optional<DatasetItemSnapshot> reserveClaimableItem(Long taskId, Long labelerId, Integer overlapCount) {
            while (true) {
                int current = assignedCount.get();
                if (current >= overlapCount) {
                    return Optional.empty();
                }
                if (assignedCount.compareAndSet(current, current + 1)) {
                    return Optional.of(new DatasetItemSnapshot(ITEM_ID, "{\"text\":\"hello\"}"));
                }
            }
        }

        int assignedCount() {
            return assignedCount.get();
        }
    }
}
