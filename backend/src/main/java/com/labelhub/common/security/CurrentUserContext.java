package com.labelhub.common.security;

import org.springframework.stereotype.Component;

@Component
public class CurrentUserContext {

    private final CurrentUserProvider currentUserProvider;

    public CurrentUserContext(CurrentUserProvider currentUserProvider) {
        this.currentUserProvider = currentUserProvider;
    }

    public CurrentUser currentUser() {
        return currentUserProvider.currentUser();
    }

    public Long currentUserId() {
        return currentUser().userId();
    }
}
