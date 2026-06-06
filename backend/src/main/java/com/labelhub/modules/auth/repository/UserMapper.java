package com.labelhub.modules.auth.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labelhub.modules.auth.domain.UserEntity;
import com.labelhub.modules.task.dto.AssignableLabelerResponse;
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
            <script>
            SELECT COUNT(DISTINCT u.id)
            FROM users u
            JOIN user_roles ur ON ur.user_id = u.id AND ur.role_code = 'LABELER'
            WHERE u.user_type &lt;&gt; 'SYSTEM'
            <if test="enabledOnly">
              AND u.enabled = TRUE
              AND u.login_enabled = TRUE
            </if>
            <if test="keyword != null">
              AND (
                u.username LIKE CONCAT('%', #{keyword}, '%')
                OR u.email LIKE CONCAT('%', #{keyword}, '%')
                OR u.display_name LIKE CONCAT('%', #{keyword}, '%')
              )
            </if>
            </script>
            """)
    long countAssignableLabelers(@Param("keyword") String keyword,
                                 @Param("enabledOnly") boolean enabledOnly);

    @Select("""
            <script>
            SELECT DISTINCT
                   u.id AS labelerId,
                   u.username AS username,
                   u.email AS email,
                   u.display_name AS displayName,
                   u.avatar_url AS avatarUrl,
                   u.enabled AS enabled,
                   u.login_enabled AS loginEnabled,
                   COALESCE(lcs.claimed_count, 0) AS claimedCount,
                   COALESCE(lcs.submitted_count, 0) AS submittedCount,
                   COALESCE(lcs.pending_review_count, 0) AS pendingReviewCount,
                   COALESCE(lcs.approved_count, 0) AS approvedCount,
                   COALESCE(lcs.rejected_count, 0) AS rejectedCount,
                   COALESCE(lcs.total_reward, 0.00) AS totalReward,
                   COALESCE(lcs.today_submitted_count, 0) AS todaySubmittedCount,
                   lcs.last_submit_date AS lastSubmitDate,
                   lcs.updated_at AS statsUpdatedAt,
                   CASE
                     WHEN COALESCE(lcs.approved_count, 0) + COALESCE(lcs.rejected_count, 0) = 0 THEN 0.0000
                     ELSE ROUND(COALESCE(lcs.approved_count, 0) * 1.0 / (COALESCE(lcs.approved_count, 0) + COALESCE(lcs.rejected_count, 0)), 4)
                   END AS approvalRate
            FROM users u
            JOIN user_roles ur ON ur.user_id = u.id AND ur.role_code = 'LABELER'
            LEFT JOIN labeler_contribution_stats lcs ON lcs.labeler_id = u.id
            WHERE u.user_type &lt;&gt; 'SYSTEM'
            <if test="enabledOnly">
              AND u.enabled = TRUE
              AND u.login_enabled = TRUE
            </if>
            <if test="keyword != null">
              AND (
                u.username LIKE CONCAT('%', #{keyword}, '%')
                OR u.email LIKE CONCAT('%', #{keyword}, '%')
                OR u.display_name LIKE CONCAT('%', #{keyword}, '%')
              )
            </if>
            ORDER BY u.username ASC, u.id ASC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<AssignableLabelerResponse> selectAssignableLabelers(@Param("keyword") String keyword,
                                                             @Param("enabledOnly") boolean enabledOnly,
                                                             @Param("offset") int offset,
                                                             @Param("limit") int limit);

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
