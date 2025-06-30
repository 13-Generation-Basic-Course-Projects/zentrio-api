package org.example.zentrio.repository;

import org.apache.ibatis.annotations.*;
import org.example.zentrio.dto.request.AchievementRequest;
import org.example.zentrio.dto.response.AchievementResponse;
import org.example.zentrio.dto.response.BoardSummary;
import org.example.zentrio.dto.response.MemberResponseData;
import org.example.zentrio.model.Achievement;
import org.example.zentrio.utility.JsonbTypeHandler;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Mapper
public interface AchievementRepository {

    @Results(id = "achievementMapper", value = {
            @Result(property = "achievementId", column = "achievement_id"),
            @Result(property = "userId", column = "user_id"),
            @Result(property = "details", column = "details", typeHandler = JsonbTypeHandler.class),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "userDetails", column = "user_id",
                    one = @One(select = "getDetailsOfUserByUserId")),
    })

    @Select("""
                INSERT INTO achievements (details,user_id)
                VALUES(
                       #{req.details,jdbcType=OTHER, typeHandler = org.example.zentrio.utility.JsonbTypeHandler},
                       #{userId})
                RETURNING *
            """)
    Achievement createAchievement(@Param("req") AchievementRequest achievementRequest, UUID userId);

    @Select("""
                UPDATE achievements
                SET details = #{req.details,jdbcType=OTHER, typeHandler= org.example.zentrio.utility.JsonbTypeHandler},
                    updated_at = #{updatedAt}
                WHERE user_id = #{userId}
                RETURNING *
            """)
    @ResultMap("achievementMapper")
    Achievement updateAchievement(@Param("req") AchievementRequest achievementRequest, UUID userId, LocalDateTime updatedAt);

    @Select("""
                SELECT COUNT(*) FROM achievements WHERE user_id = #{userId}
            """)
    boolean isAchievementExist(UUID userId);

    @Select("""
                SELECT * FROM achievements WHERE user_id = #{userId}
            """)
    @ResultMap("achievementMapper")
    AchievementResponse getAllAchievementByCurrentUser(UUID userId);


    @Select("""
                SELECT * FROM achievements
                WHERE achievement_id = #{achievementId}
            """)
    Achievement getAchievementById(UUID achievementId);


    @Select("""
                SELECT u.username, u.profile_image, u.user_id FROM users u
                WHERE u.user_id = #{userId}
            """)
    @Results(id = "userMapper", value = {

            @Result(property = "userId", column = "user_id"),
            @Result(property = "username", column = "username"),
            @Result(property = "imageUrl", column = "profile_image")
    })
    MemberResponseData getDetailsOfUserByUserId(UUID userId);

    @Select("""
                DELETE FROM achievements 
                WHERE achievement_id = #{achievementId}
                AND user_id = #{userId}
            """)
    void deletedAchievement(UUID achievementId, UUID userId);

    @Select("""
            SELECT b.board_id     AS boardId,
                b.title        AS boardName,
                b.created_at   AS createdAt,
                b.updated_at   AS updatedAt,
                w.workspace_id AS workspaceId,
                w.title        AS workspaceName,
                r.role_name AS role
            FROM boards b
                INNER JOIN members m ON b.board_id = m.board_id
                INNER JOIN roles r ON m.role_id = r.role_id
                INNER JOIN workspaces w ON b.workspace_id = w.workspace_id
                WHERE m.user_id = #{userId}
            AND w.created_by = #{userId}
            """)
    List<BoardSummary> getOwnBoards(UUID userId);

    @Select("""
            SELECT b.board_id     AS boardId,
                   b.title        AS boardName,
                   b.created_at   AS createdAt,
                    b.updated_at   AS updatedAt,
                   w.workspace_id AS workspaceId,
                   w.title        AS workspaceName,
                   r.role_name AS role
            FROM boards b
                     INNER JOIN members m ON b.board_id = m.board_id
                     INNER JOIN roles r ON m.role_id = r.role_id
                     INNER JOIN workspaces w ON b.workspace_id = w.workspace_id
            WHERE m.user_id = #{userId}
              AND w.created_by != #{userId}
            """)
    List<BoardSummary> getJoinBoard(UUID userId);

    @Select("""
                SELECT * FROM achievements WHERE user_id = #{userId}
                LIMIT 1
            """)
    @Results(id = "myAchievementMapper", value = {
            @Result(property = "achievementId", column = "achievement_id"),
            @Result(property = "userId", column = "user_id"),
            @Result(property = "details", column = "details", typeHandler = JsonbTypeHandler.class),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at")
    })
    Achievement getMyAchievement(UUID userId);

}
