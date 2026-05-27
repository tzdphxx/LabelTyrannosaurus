package com.labelhub.modules.auth.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.labelhub.common.security.RoleCode;

@TableName("user_roles")
public class UserRoleEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private RoleCode roleCode;

    public UserRoleEntity() {
    }

    public UserRoleEntity(Long userId, RoleCode roleCode) {
        this.userId = userId;
        this.roleCode = roleCode;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public RoleCode getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(RoleCode roleCode) {
        this.roleCode = roleCode;
    }
}
