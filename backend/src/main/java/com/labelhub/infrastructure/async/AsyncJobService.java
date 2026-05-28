package com.labelhub.infrastructure.async;

import org.springframework.stereotype.Service;

@Service
public class AsyncJobService {

    private final AsyncJobExecutor asyncJobExecutor;

    public AsyncJobService(AsyncJobExecutor asyncJobExecutor) {
        this.asyncJobExecutor = asyncJobExecutor;
    }

    public void submit(AsyncJobCommand command) {
        asyncJobExecutor.execute(command);
    }
}
