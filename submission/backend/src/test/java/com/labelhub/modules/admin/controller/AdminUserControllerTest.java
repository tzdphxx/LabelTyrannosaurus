package com.labelhub.modules.admin.controller;

import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.admin.dto.UpdateUserRolesRequest;
import com.labelhub.modules.admin.service.AdminUserService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AdminUserControllerTest {

    private final AdminUserService adminUserService = mock(AdminUserService.class);
    private final AdminUserController controller = new AdminUserController(adminUserService);

    @Test
    void updateRoleDelegatesToAdminUserService() {
        controller.updateRole(10L, new UpdateUserRolesRequest(RoleCode.REVIEWER));

        verify(adminUserService).changeRole(10L, RoleCode.REVIEWER);
    }
}
