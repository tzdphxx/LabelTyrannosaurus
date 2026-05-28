package com.labelhub.common.security;

import com.labelhub.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public class MockCurrentUserProvider implements CurrentUserProvider {

    private static final int INVALID_USER_CONTEXT = 401001;
    private static final long DEFAULT_OWNER_ID = 1L;

    private final ObjectProvider<HttpServletRequest> requestProvider;

    public MockCurrentUserProvider(ObjectProvider<HttpServletRequest> requestProvider) {
        this.requestProvider = requestProvider;
    }

    @Override
    public CurrentUser currentUser() {
        Long userId = resolveUserId();
        return new CurrentUser(userId, "mock-owner-" + userId, Set.of(RoleCode.OWNER), 1);
    }

    private Long resolveUserId() {
        HttpServletRequest request = requestProvider.getIfAvailable();
        if (request == null) {
            return DEFAULT_OWNER_ID;
        }
        String userIdHeader = request.getHeader("X-User-Id");
        if (userIdHeader == null || userIdHeader.isBlank()) {
            return DEFAULT_OWNER_ID;
        }
        try {
            return Long.parseLong(userIdHeader);
        } catch (NumberFormatException ex) {
            throw new BusinessException(INVALID_USER_CONTEXT, "Invalid current user context");
        }
    }
}
