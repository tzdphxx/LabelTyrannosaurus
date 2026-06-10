package com.labelhub.modules.admin.mapper;

import com.labelhub.modules.admin.dto.AdminClaimedReviewTaskRow;
import com.labelhub.modules.admin.dto.AssignableReviewTaskResponse;
import com.labelhub.modules.admin.dto.AssignableReviewerResponse;
import com.labelhub.modules.admin.dto.ReviewerProgressResponse;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AdminReviewAssignmentMapper {

    @Select("""
            <script>
            SELECT COUNT(1)
            FROM (
                SELECT s.task_id, COALESCE(s.current_review_level, 1) AS review_level
                FROM submissions s
                JOIN tasks t ON t.id = s.task_id
                LEFT JOIN review_task_claims rtc
                  ON rtc.task_id = s.task_id
                 AND rtc.review_level = COALESCE(s.current_review_level, 1)
                WHERE s.status = 'PENDING_FINAL'
                <if test="taskId != null"> AND s.task_id = #{taskId}</if>
                <if test="reviewLevel != null"> AND COALESCE(s.current_review_level, 1) = #{reviewLevel}</if>
                <if test="keyword != null">
                  AND (t.title LIKE CONCAT('%', #{keyword}, '%')
                       OR t.description LIKE CONCAT('%', #{keyword}, '%'))
                </if>
                <if test="includeClaimed == false"> AND rtc.id IS NULL</if>
                GROUP BY s.task_id, COALESCE(s.current_review_level, 1)
            ) x
            </script>
            """)
    long countAssignableTasks(@Param("taskId") Long taskId,
                              @Param("keyword") String keyword,
                              @Param("reviewLevel") Integer reviewLevel,
                              @Param("includeClaimed") boolean includeClaimed);

    @Select("""
            <script>
            SELECT s.task_id AS taskId,
                   t.title AS title,
                   t.status AS status,
                   t.deadline_at AS deadlineAt,
                   COALESCE(s.current_review_level, 1) AS reviewLevel,
                   COUNT(1) AS pendingCount,
                   CASE WHEN rtc.id IS NULL THEN FALSE ELSE TRUE END AS claimed,
                   rtc.reviewer_id AS claimedReviewerId,
                   ru.username AS claimedReviewerName,
                   CASE WHEN rtc.id IS NULL THEN TRUE ELSE FALSE END AS available
            FROM submissions s
            JOIN tasks t ON t.id = s.task_id
            LEFT JOIN review_task_claims rtc
              ON rtc.task_id = s.task_id
             AND rtc.review_level = COALESCE(s.current_review_level, 1)
            LEFT JOIN users ru ON ru.id = rtc.reviewer_id
            WHERE s.status = 'PENDING_FINAL'
            <if test="taskId != null"> AND s.task_id = #{taskId}</if>
            <if test="reviewLevel != null"> AND COALESCE(s.current_review_level, 1) = #{reviewLevel}</if>
            <if test="keyword != null">
              AND (t.title LIKE CONCAT('%', #{keyword}, '%')
                   OR t.description LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="includeClaimed == false"> AND rtc.id IS NULL</if>
            GROUP BY s.task_id, t.title, t.status, t.deadline_at,
                     COALESCE(s.current_review_level, 1), rtc.id, rtc.reviewer_id, ru.username
            ORDER BY pendingCount DESC, s.task_id DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<AssignableReviewTaskResponse> selectAssignableTasks(@Param("taskId") Long taskId,
                                                             @Param("keyword") String keyword,
                                                             @Param("reviewLevel") Integer reviewLevel,
                                                             @Param("includeClaimed") boolean includeClaimed,
                                                             @Param("offset") int offset,
                                                             @Param("limit") int limit);

    @Select("""
            <script>
            SELECT COUNT(DISTINCT u.id)
            FROM users u
            JOIN user_roles ur ON ur.user_id = u.id AND ur.role_code = 'REVIEWER'
            WHERE 1 = 1
            <if test="enabledOnly"> AND u.enabled = TRUE AND u.login_enabled = TRUE</if>
            <if test="keyword != null">
              AND (u.username LIKE CONCAT('%', #{keyword}, '%')
                   OR u.email LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            </script>
            """)
    long countAssignableReviewers(@Param("keyword") String keyword,
                                  @Param("enabledOnly") boolean enabledOnly);

    @Select("""
            <script>
            SELECT u.id AS reviewerId,
                   u.username AS username,
                   u.email AS email,
                   u.enabled AS enabled,
                   u.login_enabled AS loginEnabled,
                   COALESCE(p.pending_count, 0) AS pendingCount,
                   COALESCE(td.today_reviewed_count, 0) AS todayReviewedCount,
                   COALESCE(ap.approved_count, 0) AS totalApprovedCount,
                   COALESCE(rj.rejected_count, 0) AS totalRejectedCount,
                   CASE
                     WHEN COALESCE(ap.approved_count, 0) + COALESCE(rj.rejected_count, 0) = 0 THEN 0.00
                     ELSE ROUND(COALESCE(ap.approved_count, 0) * 100.0
                                / (COALESCE(ap.approved_count, 0) + COALESCE(rj.rejected_count, 0)), 2)
                   END AS approvalRate
            FROM users u
            JOIN user_roles ur ON ur.user_id = u.id AND ur.role_code = 'REVIEWER'
            LEFT JOIN (
                SELECT assigned_reviewer_id AS reviewer_id, COUNT(1) AS pending_count
                FROM submissions
                WHERE status = 'PENDING_FINAL' AND assigned_reviewer_id IS NOT NULL
                GROUP BY assigned_reviewer_id
            ) p ON p.reviewer_id = u.id
            LEFT JOIN (
                SELECT reviewer_id, COUNT(1) AS today_reviewed_count
                FROM review_records
                WHERE action IN ('APPROVE', 'REJECT')
                  AND created_at >= CURRENT_DATE()
                  AND created_at &lt; CURRENT_DATE() + INTERVAL 1 DAY
                GROUP BY reviewer_id
            ) td ON td.reviewer_id = u.id
            LEFT JOIN (
                SELECT reviewer_id, COUNT(1) AS approved_count
                FROM review_records
                WHERE action = 'APPROVE'
                GROUP BY reviewer_id
            ) ap ON ap.reviewer_id = u.id
            LEFT JOIN (
                SELECT reviewer_id, COUNT(1) AS rejected_count
                FROM review_records
                WHERE action = 'REJECT'
                GROUP BY reviewer_id
            ) rj ON rj.reviewer_id = u.id
            WHERE 1 = 1
            <if test="enabledOnly"> AND u.enabled = TRUE AND u.login_enabled = TRUE</if>
            <if test="keyword != null">
              AND (u.username LIKE CONCAT('%', #{keyword}, '%')
                   OR u.email LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            ORDER BY pendingCount ASC, todayReviewedCount DESC, reviewerId ASC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<AssignableReviewerResponse> selectAssignableReviewers(@Param("keyword") String keyword,
                                                               @Param("enabledOnly") boolean enabledOnly,
                                                               @Param("offset") int offset,
                                                               @Param("limit") int limit);

    @Select("""
            <script>
            SELECT u.id AS reviewerId,
                   u.username AS username,
                   u.email AS email,
                   u.enabled AS enabled,
                   u.login_enabled AS loginEnabled,
                   COALESCE(p.pending_count, 0) AS pendingCount,
                   COALESCE(td.today_reviewed_count, 0) AS todayReviewedCount,
                   COALESCE(rv.total_reviewed_count, 0) AS totalReviewedCount,
                   CASE
                     WHEN COALESCE(ap.approved_count, 0) + COALESCE(rj.rejected_count, 0) = 0 THEN 0.00
                     ELSE ROUND(COALESCE(ap.approved_count, 0) * 100.0
                                / (COALESCE(ap.approved_count, 0) + COALESCE(rj.rejected_count, 0)), 2)
                   END AS approvalRate,
                   0 AS claimedTaskCount,
                   NULL AS claimedTasks
            FROM users u
            JOIN user_roles ur ON ur.user_id = u.id AND ur.role_code = 'REVIEWER'
            LEFT JOIN (
                SELECT assigned_reviewer_id AS reviewer_id, COUNT(1) AS pending_count
                FROM submissions
                WHERE status = 'PENDING_FINAL' AND assigned_reviewer_id IS NOT NULL
                GROUP BY assigned_reviewer_id
            ) p ON p.reviewer_id = u.id
            LEFT JOIN (
                SELECT reviewer_id, COUNT(1) AS today_reviewed_count
                FROM review_records
                WHERE action IN ('APPROVE', 'REJECT')
                  AND created_at >= CURRENT_DATE()
                  AND created_at &lt; CURRENT_DATE() + INTERVAL 1 DAY
                GROUP BY reviewer_id
            ) td ON td.reviewer_id = u.id
            LEFT JOIN (
                SELECT reviewer_id, COUNT(1) AS total_reviewed_count
                FROM review_records
                WHERE action IN ('APPROVE', 'REJECT')
                GROUP BY reviewer_id
            ) rv ON rv.reviewer_id = u.id
            LEFT JOIN (
                SELECT reviewer_id, COUNT(1) AS approved_count
                FROM review_records
                WHERE action = 'APPROVE'
                GROUP BY reviewer_id
            ) ap ON ap.reviewer_id = u.id
            LEFT JOIN (
                SELECT reviewer_id, COUNT(1) AS rejected_count
                FROM review_records
                WHERE action = 'REJECT'
                GROUP BY reviewer_id
            ) rj ON rj.reviewer_id = u.id
            WHERE 1 = 1
            <if test="enabledOnly"> AND u.enabled = TRUE AND u.login_enabled = TRUE</if>
            <if test="keyword != null">
              AND (u.username LIKE CONCAT('%', #{keyword}, '%')
                   OR u.email LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            ORDER BY pendingCount ASC, todayReviewedCount DESC, reviewerId ASC
            </script>
            """)
    List<ReviewerProgressResponse> selectReviewerProgress(@Param("keyword") String keyword,
                                                          @Param("enabledOnly") boolean enabledOnly);

    @Select("""
            <script>
            SELECT rtc.reviewer_id AS reviewerId,
                   rtc.task_id AS taskId,
                   t.title AS title,
                   rtc.review_level AS reviewLevel,
                   COALESCE(p.pending_count, 0) AS pendingCount,
                   rtc.claimed_at AS claimedAt
            FROM review_task_claims rtc
            JOIN tasks t ON t.id = rtc.task_id
            LEFT JOIN (
                SELECT task_id, COALESCE(current_review_level, 1) AS review_level, COUNT(1) AS pending_count
                FROM submissions
                WHERE status = 'PENDING_FINAL'
                GROUP BY task_id, COALESCE(current_review_level, 1)
            ) p ON p.task_id = rtc.task_id AND p.review_level = rtc.review_level
            <choose>
            <when test="reviewerIds != null and reviewerIds.size() > 0">
            WHERE rtc.reviewer_id IN
            <foreach collection="reviewerIds" item="reviewerId" open="(" separator="," close=")">
                #{reviewerId}
            </foreach>
            </when>
            <otherwise>
            WHERE 1 = 0
            </otherwise>
            </choose>
            ORDER BY rtc.reviewer_id ASC, pendingCount DESC, rtc.task_id DESC
            </script>
            """)
    List<AdminClaimedReviewTaskRow> selectClaimedTasksByReviewerIds(@Param("reviewerIds") List<Long> reviewerIds);
}
