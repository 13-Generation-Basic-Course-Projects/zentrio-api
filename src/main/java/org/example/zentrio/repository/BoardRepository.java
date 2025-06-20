package org.example.zentrio.repository;

import org.apache.ibatis.annotations.*;
import org.example.zentrio.dto.request.BoardRequest;
import org.example.zentrio.dto.response.BoardResponse;
import org.example.zentrio.dto.response.MemberResponse;
import org.example.zentrio.dto.response.MemberResponseData;
import org.example.zentrio.model.Board;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Mapper
public interface BoardRepository {

    @Select("""
            INSERT INTO members(user_id,board_id,role_id)
            VALUES(#{userId},#{boardId},#{roleId})
            """)
    void insertMember(UUID userId, UUID boardId, UUID roleId);

    @Select("""
                INSERT INTO boards(title,description,cover,workspace_id)
                VALUES(#{req.title},#{req.description},#{req.cover}, #{workspaceId})
                RETURNING *
            """)
    @Results(id = "boardMapper", value = {
            @Result(property = "boardId", column = "board_id"),
            @Result(property = "title", column = "title"),
            @Result(property = "description", column = "description"),
            @Result(property = "cover", column = "cover"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "createdBy", column = "workspace_id",
            one = @One(select = "getPmByWorkspaceId")),
            @Result(property = "isFavourite", column = "is_favourite"),
            @Result(property = "workspaceId", column = "workspace_id")
    })
    Board createBoard(@Param("req") BoardRequest boardRequest, UUID workspaceId);

    @Select("""
                SELECT * FROM boards WHERE workspace_id = #{workspaceId}
                LIMIT #{limit} OFFSET #{offset}
            """)
    @ResultMap("boardMapper")
    List<Board> getAllBoardsByWorkspaceId(UUID workspaceId, Integer limit, Integer offset);


    @Select("""
                UPDATE boards SET title = #{request.title},
                                  description = #{request.description},
                                  cover = #{request.cover},
                                  updated_at= #{updatedAt}
                WHERE board_id = #{boardId}
                RETURNING *
            """)
    @ResultMap("boardMapper")
    Board updateBoardByBoardId(@Param("request") BoardRequest boardRequest, UUID boardId, LocalDateTime updatedAt);


    @Select("""
                DELETE FROM boards
                WHERE board_id = #{boardId}
                AND workspace_id = #{workspaceId}
            """)

    void deleteBoardByBoardId(UUID boardId, UUID workspaceId);


    @Select("""
                SELECT * FROM boards WHERE board_id = #{boardId}
            """)
    @ResultMap("boardMapper")
    Board getBoardByBoardId(UUID boardId);

    @Select("""
                SELECT * FROM boards WHERE title ILIKE '%' || #{boardTitle} || '%'
            """)
    @ResultMap("boardMapper")
    List<Board> getBoardByTitle(String boardTitle);


    @Select("""
                SELECT COUNT(*) FROM boards WHERE workspace_id = #{workspaceId}
            """)
    Integer getBoardCountByWorkspaceId(UUID workspaceId);


    @Select("""
               SELECT DISTINCT u.user_id, u.username, u.email, u.profile_image, u.gender, m.board_id AS board_id FROM users u
                INNER JOIN members m ON u.user_id = m.user_id
                WHERE board_id = #{boardId}
            """)
    @Results(id = "boardWithMembersMap", value = {
            @Result(property = "userId", column = "user_id"),
            @Result(property = "username", column = "username"),
            @Result(property = "profileImage",column = "profile_image"),
            @Result(property = "email", column = "email"),
            @Result(property = "tasks", column = "{userId=user_id, boardId=board_id}",
            many = @Many(select = "getAllTaskNames")),
            @Result(property = "roles", column = "{userId=user_id, boardId=board_id}",
                    many = @Many(select = "org.example.zentrio.repository.RoleRepository.getRolesNameByUserId")),

    })
    HashSet<MemberResponse> getBoardByBoardIdWithMember(UUID boardId);


    @Select("""
        
        SELECT DISTINCT ta.title
        
        FROM tasks ta
                 JOIN boards b ON b.board_id = ta.board_id
                 JOIN members m ON m.board_id = ta.board_id AND m.user_id = #{userId}
            JOIN roles r ON m.role_id = r.role_id
        
            -- Left joins for filtering based on role
            LEFT JOIN task_assignments tsa ON ta.task_id = tsa.task_id
            LEFT JOIN checklists ch ON ch.task_id = ta.task_id
            LEFT JOIN checklist_assignments ca ON ch.checklist_id = ca.checklist_id
        
        WHERE ta.board_id = #{boardId}
          AND (
        -- PM gets all tasks
            r.role_name = 'ROLE_MANAGER'
        
        -- Team Lead gets tasks where he is the leader (assigned_by)
           OR (r.role_name = 'ROLE_LEADER' AND tsa.assigned_to = m.member_id)
        
        -- Member gets tasks via checklist assignment
           OR (r.role_name = 'ROLE_MEMBER' AND ca.member_id = m.member_id)
            )
    """)
    HashSet<String> getAllTaskNames(UUID userId, UUID boardId);


    @Select("""
                SELECT member_id FROM members
                WHERE user_id = #{userId}
                AND board_id = #{boardId}
            """)
    UUID getMemberIdByUserIdAndBoardId(UUID userId, UUID boardId);


    @Select("""
                SELECT m.member_id FROM members m
                JOIN roles r ON m.role_id = r.role_id
                WHERE m.user_id = #{userId}
                  AND m.board_id = #{boardId}
                  AND r.role_name = 'ROLE_LEADER'
                LIMIT 1
            """)
    UUID getTeamLeaderMemberIdByUserIdAndBoardId(UUID userId, UUID boardId);

    @Select("""
                SELECT m.member_id FROM members m
                JOIN roles r ON m.role_id = r.role_id
                WHERE m.user_id = #{userId}
                  AND m.board_id = #{boardId}
                  AND r.role_name = 'ROLE_MANAGER'
                LIMIT 1
            """)
    UUID getManagerMemberIdByUserIdAndBoardId(UUID userId, UUID boardId);

    @Select("""
                UPDATE boards SET image_url = #{imageUrl}
                WHERE board_id = #{boardId}
            """)
    void updateCover(UUID boardId, String imageUrl);


    @Select("""
                UPDATE boards SET is_favourite = #{isFavourite}
                WHERE board_id = #{boardId}
            """)
    void updateIsFavourite(UUID boardId, Boolean isFavourite);

    @Select("""
                SELECT member_id
                FROM members m
                INNER JOIN roles r ON m.role_id = r.role_id
                WHERE board_id = #{boardId}
                AND role_name = 'ROLE_MANAGER'
                LIMIT 1
            """)
    UUID getManagerMemberIdByBoardId(UUID boardId);


    @Select("""
                SELECT boards.* FROM boards
                INNER JOIN members m ON boards.board_id = m.board_id
                WHERE m.board_id = #{boardId}
                AND m.user_id = #{userId}
            """)
    @ResultMap("boardMapper")
    Board getBoardByIdWithCurrentUserId(UUID boardId, UUID userId);

    @Select("""
                SELECT boards.* FROM boards
                INNER JOIN members m ON boards.board_id = m.board_id
                WHERE m.user_id = #{userId}
            """)
    @ResultMap("boardMapper")
    HashSet<Board> getAllBoardsForCurrentUser(UUID userId);

    @Select("""
        SELECT * FROM boards
    """)
    @ResultMap("boardMapper")
    HashSet<Board> getAllBoards();

    @Select("""
        
            SELECT EXISTS(
            SELECT  1 FROM  members m
                      where m.user_id= #{assigneeId}
                      AND m.board_id= #{boardId}
        )
        """)
    Boolean getExistUserInBoard(UUID assigneeId, UUID boardId);

//    @Select("""
//        SELECT * FROM boards WHERE board_id= #{boardId}
//        """)
//    @Results(id = "boarAllDataMapper", value = {
//            @Result(property = "boardId", column = "board_id"),
//            @Result(property = "title", column = "title"),
//            @Result(property = "description", column = "description"),
//            @Result(property = "cover", column = "cover"),
//            @Result(property = "createdAt", column = "created_at"),
//            @Result(property = "updatedAt", column = "updated_at"),
//            @Result(property = "isFavourite", column = "is_favourite"),
//            @Result(property = "workspaceId", column = "workspace_id"),
//            @Result(property = "allTasks", column = "board_id",
//            many = @Many(select = "org.example.zentrio.repository.TaskRepository.getAllDataInTaskByBoardId"))
//    })
//    BoardResponse getAllDataInBoard(UUID boardId);

    @Select("""
        SELECT * FROM boards WHERE board_id= #{boardId}
        """)
    @Results(id = "boarAllDataMapper", value = {
            @Result(property = "boardId", column = "board_id"),
            @Result(property = "title", column = "title"),
            @Result(property = "description", column = "description"),
            @Result(property = "cover", column = "cover"),
            @Result(property = "createdAt", column = "created_at"),
            @Result(property = "updatedAt", column = "updated_at"),
            @Result(property = "isFavourite", column = "is_favourite"),
            @Result(property = "workspaceId", column = "workspace_id"),
            @Result(property = "ganttChart", column = "board_id",
                    one = @One(select = "org.example.zentrio.repository.GanttChartRepository.gatAGanttChartByBoardId"))
    })
    BoardResponse getAllDataInBoard(UUID boardId);

    @Select("""
        DELETE FROM members m
        WHERE m.board_id = #{boardId}
        AND m.user_id = #{userId}
    """)
    void deletedMember(UUID boardId, UUID userId);


    @Select("""
        SELECT b.*
        FROM boards b
        INNER JOIN workspaces w ON b.workspace_id = w.workspace_id
        WHERE b.is_favourite = TRUE
        AND w.created_by = #{userId}
    """)
    @ResultMap("boardMapper")
    HashSet<Board> getFavouriteBoardsByUserId(UUID userId);


    @Select("""
             
                   SELECT u.profile_image AS image , u.username AS name, u.user_id, u.gender FROM users u
                        inner join workspaces wk on u.user_id = wk.created_by
                WHERE wk.workspace_id= #{workspaceId}
        """)
    @Results(id = "userWorkspaceMapper", value = {
            @Result(property = "userId", column = "user_id"),
            @Result(property = "gender", column = "gender"),
            @Result(property = "imageUrl", column = "image"),
            @Result(property = "username", column = "name"),
    })
    MemberResponseData getPmByWorkspaceId(UUID workspaceId);
}
