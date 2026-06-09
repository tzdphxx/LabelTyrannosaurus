package com.labelhub.modules.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.labelhub.modules.task.domain.Task;
import com.labelhub.modules.task.domain.TaskStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TaskMapper extends BaseMapper<Task> {

    @Select("""
            <script>
            SELECT t.*
            FROM tasks t
            WHERE t.status = 'PUBLISHED'
              AND t.deadline_at &gt; #{now}
              <if test="status != null">
                AND t.status = #{status}
              </if>
              <if test="keyword != null">
                AND (
                  t.title LIKE CONCAT('%', #{keyword}, '%')
                  OR t.description LIKE CONCAT('%', #{keyword}, '%')
                )
              </if>
              <if test="tag != null">
                AND EXISTS (
                  SELECT 1
                  FROM task_tags tt
                  WHERE tt.task_id = t.id
                    AND tt.tag_name = #{tag}
                )
              </if>
            ORDER BY t.published_at DESC, t.id DESC
            </script>
            """)
    List<Task> selectPublishedMarketTasks(@Param("keyword") String keyword,
                                          @Param("tag") String tag,
                                          @Param("status") String status,
                                          @Param("now") LocalDateTime now);

    @Select("""
            SELECT t.*
            FROM tasks t
            WHERE t.id = #{taskId}
              AND t.status = 'PUBLISHED'
              AND t.deadline_at > #{now}
            """)
    Task selectPublishedMarketTaskById(@Param("taskId") Long taskId,
                                       @Param("now") LocalDateTime now);

    @Select("""
            <script>
            SELECT t.*
            FROM tasks t
            WHERE 1 = 1
              <if test="ownerId != null">
                AND t.owner_id = #{ownerId}
              </if>
              <if test="status != null">
                AND t.status = #{status}
              </if>
              <if test="keyword != null">
                AND (
                  t.title LIKE CONCAT('%', #{keyword}, '%')
                  OR t.description LIKE CONCAT('%', #{keyword}, '%')
                )
              </if>
            ORDER BY t.updated_at DESC, t.id DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<Task> selectOwnerTasksPage(@Param("ownerId") Long ownerId,
                                    @Param("status") String status,
                                    @Param("keyword") String keyword,
                                    @Param("limit") int limit,
                                    @Param("offset") int offset);

    @Select("""
            <script>
            SELECT COUNT(1)
            FROM tasks t
            WHERE 1 = 1
              <if test="ownerId != null">
                AND t.owner_id = #{ownerId}
              </if>
              <if test="status != null">
                AND t.status = #{status}
              </if>
              <if test="keyword != null">
                AND (
                  t.title LIKE CONCAT('%', #{keyword}, '%')
                  OR t.description LIKE CONCAT('%', #{keyword}, '%')
                )
              </if>
            </script>
            """)
    long countOwnerTasks(@Param("ownerId") Long ownerId,
                         @Param("status") String status,
                         @Param("keyword") String keyword);

    @Update("""
            UPDATE tasks
            SET claimed_count = claimed_count + 1
            WHERE id = #{taskId}
              AND claimed_count < quota
            """)
    int tryIncrementClaimedCount(@Param("taskId") Long taskId);

    @Update("""
            UPDATE tasks
            SET claimed_count = GREATEST(0, claimed_count - 1)
            WHERE id = #{taskId}
            """)
    int decrementClaimedCount(@Param("taskId") Long taskId);

    @Update("""
            UPDATE tasks
            SET status = #{afterStatus},
                quota = #{quota},
                claimed_count = #{claimedCount},
                published_at = #{publishedAt},
                ended_at = #{endedAt},
                updated_at = NOW()
            WHERE id = #{taskId}
              AND owner_id = #{ownerId}
              AND status = #{beforeStatus}
            """)
    int updateStatusIfCurrent(@Param("taskId") Long taskId,
                              @Param("ownerId") Long ownerId,
                              @Param("beforeStatus") TaskStatus beforeStatus,
                              @Param("afterStatus") TaskStatus afterStatus,
                              @Param("publishedAt") LocalDateTime publishedAt,
                              @Param("endedAt") LocalDateTime endedAt,
                              @Param("quota") Integer quota,
                              @Param("claimedCount") Integer claimedCount);
}
