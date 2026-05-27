package com.labelhub.modules.auth.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labelhub.modules.auth.domain.UserEntity;
import org.apache.ibatis.annotations.Mapper;
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
}
