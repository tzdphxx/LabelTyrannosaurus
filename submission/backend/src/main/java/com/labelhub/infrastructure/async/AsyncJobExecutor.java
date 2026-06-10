package com.labelhub.infrastructure.async;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.Executor;

@Component
public class AsyncJobExecutor {

    private final Executor executor;
    private final List<AsyncJobFailureHandler> failureHandlers;

    public AsyncJobExecutor(@Qualifier("asyncJobTaskExecutor") Executor executor,
                            List<AsyncJobFailureHandler> failureHandlers) {
        this.executor = executor;
        this.failureHandlers = failureHandlers;
    }

    public void execute(AsyncJobCommand command) {
        try {
            executor.execute(() -> runSafely(command));
        } catch (Throwable failure) {
            notifyFailure(command, failure);
        }
    }

    private void runSafely(AsyncJobCommand command) {
        try {
            command.task().run();
        } catch (Throwable failure) {
            notifyFailure(command, failure);
        }
    }

    private void notifyFailure(AsyncJobCommand command, Throwable failure) {
        for (AsyncJobFailureHandler failureHandler : failureHandlers) {
            failureHandler.onFailure(command, failure);
        }
    }
}
