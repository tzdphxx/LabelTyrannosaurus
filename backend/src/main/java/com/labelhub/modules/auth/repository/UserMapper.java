package com.labelhub.modules.auth.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labelhub.modules.auth.domain.UserEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {

    @Select("select * from users where username = #{username} limit 1")
    UserEntity selectByUsername(String username);

    @Select("select * from users where email = #{email} limit 1")
    UserEntity selectByEmail(String email);

    @Select("select * from users where username = #{account} or email = #{account} limit 1")
    UserEntity selectByUsernameOrEmail(String account);

    @Select("""
            select * from users
            where (#{includeSystem} = true or user_type <> 'SYSTEM')
            order by created_at desc
            """)
    List<UserEntity> selectAdminUsers(boolean includeSystem);

    @Update("update users set token_version = token_version + 1 where id = #{userId}")
    int incrementTokenVersion(Long userId);

    @Update("update users set enabled = #{enabled}, token_version = token_version + 1 where id = #{userId}")
    int setEnabled(Long userId, boolean enabled);

    @Update("update users set last_login_at = current_timestamp(3) where id = #{userId}")
    int updateLastLoginAt(Long userId);

    /**
     * 修复系统 AI 主体的固定字段，显式清空 password_hash。
     *
     * <p>MyBatis-Plus 的 updateById 默认会跳过 null 字段，因此不能依赖实体更新来清空密码哈希。</p>
     */
    @Update("""
            update users
            set email = #{email},
                password_hash = null,
                user_type = 'SYSTEM',
                login_enabled = false,
                enabled = true,
                display_name = #{displayName}
            where id = #{userId}
            """)
    int repairSystemPrincipal(@Param("userId") Long userId,
                              @Param("email") String email,
                              @Param("displayName") String displayName);
}
