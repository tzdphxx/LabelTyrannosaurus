package com.labelhub.modules.auth.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labelhub.common.security.RoleCode;
import com.labelhub.modules.auth.domain.UserRoleEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Set;

@Mapper
public interface UserRoleMapper extends BaseMapper<UserRoleEntity> {

    @Select("select role_code from user_roles where user_id = #{userId}")
    Set<RoleCode> selectRoleCodesByUserId(Long userId);

    @Select("select count(distinct user_id) from user_roles where role_code = #{roleCode}")
    Long countUsersWithRole(RoleCode roleCode);

    @Delete("delete from user_roles where user_id = #{userId}")
    int deleteByUserId(Long userId);

    default void replaceRoles(Long userId, Set<RoleCode> roles) {
        deleteByUserId(userId);
        for (RoleCode role : roles) {
            insert(new UserRoleEntity(userId, role));
        }
    }
}
