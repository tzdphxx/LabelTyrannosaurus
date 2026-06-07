package com.labelhub.infrastructure.async;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AsyncJobServiceTest {

    @Test
    void submitDelegatesCommandToExecutor() {
        AsyncJobExecutor asyncJobExecutor = mock(AsyncJobExecutor.class);
        AsyncJobService asyncJobService = new AsyncJobService(asyncJobExecutor);
        AsyncJobCommand command = new AsyncJobCommand(
                AsyncJobType.DATASET_IMPORT,
                101L,
                "trace-1",
                () -> {
                }
        );

        asyncJobService.submit(command);

        verify(asyncJobExecutor).execute(command);
    }

    @Test
    void executorCapturesFailureWithoutThrowingToSubmitter() {
        Executor directExecutor = Runnable::run;
        AsyncJobFailureHandler failureHandler = mock(AsyncJobFailureHandler.class);
        AsyncJobExecutor asyncJobExecutor = new AsyncJobExecutor(directExecutor, List.of(failureHandler));
        IllegalStateException failure = new IllegalStateException("import failed");
        AsyncJobCommand command = new AsyncJobCommand(
                AsyncJobType.DATASET_IMPORT,
                101L,
                "trace-1",
                () -> {
                    throw failure;
                }
        );

        assertThatCode(() -> asyncJobExecutor.execute(command)).doesNotThrowAnyException();

        verify(failureHandler).onFailure(command, failure);
    }
}
