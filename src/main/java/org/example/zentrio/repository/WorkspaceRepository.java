package org.example.zentrio.repository;

import org.apache.ibatis.annotations.*;
import org.example.zentrio.dto.request.WorkspaceRequest;
import org.example.zentrio.dto.response.AppUserResponse;
import org.example.zentrio.dto.response.JoinBoardResponse;
import org.example.zentrio.dto.response.OtherWorkspaceResponse;
import org.example.zentrio.model.Board;
import org.example.zentrio.model.Workspace;


import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Mapper
public interface WorkspaceRepository {

    @Select("""
                INSERT INTO workspaces(title, description, created_by) VALUES (#{request.title}, #{request.description}, #{userId})
                RETURNING *
            """)
    @Results(id = "workspaceMapper", value = {
            @Result(property = "workspaceId", column = "workspace_id"),
            @Result(property = "title", column = "title"),
            @Result(property = "description", column = "description"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "createdBy", column = "created_by"),
    })
    Workspace createWorkspace(@Param("request") WorkspaceRequest workspaceRequest, UUID userId);

    @Select("""
                SELECT * FROM workspaces
                WHERE created_by = #{userId}
                LIMIT #{limit} OFFSET #{offset}
            """)
    @ResultMap("workspaceMapper")
    List<Workspace> getAllWorkspaces(UUID userId, Integer limit, Integer offset);

    @Select("""
                SELECT * FROM workspaces
                WHERE workspace_id = #{workspaceId}
            """)
    Workspace getWorkspaceByWorkspaceId(UUID workspaceId);

    @Select("""
                 SELECT COUNT(*) FROM workspaces WHERE created_by = #{userId}
            """)
    Integer countWorkspacesByUserId(UUID userId);

    @Select("""
                SELECT * FROM workspaces WHERE workspace_id = #{workspaceId} AND created_by = #{userId}
            """)
    @ResultMap("workspaceMapper")
    Workspace getWorkspaceById(UUID workspaceId, UUID userId);

    @Select("""
                SELECT * FROM workspaces WHERE title ILIKE '%'|| #{title} ||'%'
            """)
    @ResultMap("workspaceMapper")
    List<Workspace> getWorkspaceByTitle(String title);

    @Select("""
                UPDATE workspaces SET title = #{request.title}, description = #{request.description},
                                  updated_at = #{updatedAt}
                WHERE workspace_id = #{workspaceId} AND created_by = #{userId}
                RETURNING *
            """)
    @ResultMap("workspaceMapper")
    Workspace updateWorkspaceById(UUID workspaceId, @Param("request") WorkspaceRequest workspaceRequest, UUID userId, LocalDateTime updatedAt);

    @Select("""
                DELETE FROM workspaces WHERE workspace_id = #{workspaceId} AND created_by = #{userId}
            """)
    @ResultMap("workspaceMapper")
    void deleteWorkspaceByWorkspaceId(UUID workspaceId, UUID userId);

    @Select("""
                SELECT * FROM workspaces WHERE workspace_id = #{workspaceId}
            """)
    @ResultMap("workspaceMapper")
    Workspace getWorkspaceByWorkspaceIdForAllUsers(UUID workspaceId);

    @Select("""
                SELECT * FROM workspaces
            """)
    @ResultMap("workspaceMapper")
    HashSet<Workspace> getAllWorkspacesForAllUsers();


    @Select("""
                SELECT DISTINCT r.role_name FROM members m
                INNER JOIN roles r ON r.role_id = m.role_id
                WHERE m.board_id = #{boardId} AND m.user_id = #{userId}
            """)
    List<String> getRolesNameByBoardIdAndUserId(UUID boardId, UUID userId);

    // Get boards the user joined within the workspace
    @Select("""
                SELECT b.board_id, b.title, b.description, b.is_favourite,
                STRING_AGG(r.role_name, ', ') AS role_name,
                b.created_at, b.updated_at, b.workspace_id, m.user_id
                FROM boards b
                INNER JOIN members m ON b.board_id = m.board_id
                INNER JOIN roles r ON m.role_id = r.role_id
                WHERE m.user_id = #{userId}
                AND b.workspace_id = #{workspaceId}
                GROUP BY b.board_id, b.title, b.description, b.is_favourite,
                b.created_at, b.updated_at, b.workspace_id, m.user_id
                ORDER BY b.board_id;
            """)
    @Results(id = "joinBoardMapper", value = {
            @Result(property = "boardId", column = "board_id"),
            @Result(property = "title", column = "title"),
            @Result(property = "description", column = "description"),
            @Result(property = "isFavourite", column = "is_favourite"),
            @Result(property = "role", column = "{userId=user_id,boardId=board_id}",
            many = @Many(select = "getRolesNameByBoardIdAndUserId")),
    })
    List<JoinBoardResponse> getJoinBoardByUserAndWorkspace(UUID userId, UUID workspaceId);


    // Main query to get all other workspaces the user joined
    @Select("""
        SELECT DISTINCT w.workspace_id, w.title, w.description, w.created_by, r.role_name, m.user_id
        FROM boards b
        INNER JOIN members m ON b.board_id = m.board_id
        INNER JOIN roles r ON m.role_id = r.role_id
        INNER JOIN workspaces w ON b.workspace_id = w.workspace_id
        WHERE m.user_id = #{userId}
          AND w.created_by != #{userId}
    """)
    @Results(id = "otherWorkspaceMapper", value = {
            @Result(property = "workspaceId", column = "workspace_id"),
            @Result(property = "title", column = "title"),
            @Result(property = "description", column = "description"),
            @Result(property = "createdByDetails", column = "user_id",
                    one = @One(select = "org.example.zentrio.repository.AchievementRepository.getDetailsOfUserByUserId")),
            @Result(property = "otherBoards", column = "{userId=user_id,workspaceId=workspace_id}",
                    many = @Many(select = "getJoinBoardByUserAndWorkspace")),
    })
    HashSet<OtherWorkspaceResponse> getOtherWorkspaceForUser(UUID userId);





}


