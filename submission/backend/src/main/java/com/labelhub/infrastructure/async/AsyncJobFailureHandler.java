package com.labelhub.infrastructure.async;

public interface AsyncJobFailureHandler {

    void onFailure(AsyncJobCommand command, Throwable failure);
}
