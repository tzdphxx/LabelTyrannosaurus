package com.labelhub.infrastructure.async;

public record AsyncJobCommand(AsyncJobType jobType, Long jobId, String traceId, Runnable task) {
}
