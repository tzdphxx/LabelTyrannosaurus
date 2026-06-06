package com.labelhub.modules.role.dashboard.controller;

import com.labelhub.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Set;

final class DashboardRequestGuard {

    private static final Set<String> FORBIDDEN_USER_ID_PARAMS = Set.of(
            "ownerId", "labelerId", "reviewerId", "userId"
    );

    private DashboardRequestGuard() {
    }

    static void rejectUserIdParams(HttpServletRequest request) {
        if (request == null) {
            return;
        }
        for (String name : FORBIDDEN_USER_ID_PARAMS) {
            if (request.getParameterMap().containsKey(name)) {
                throw new BusinessException(400102, "看板接口不支持传入用户 ID 参数");
            }
        }
    }
}
